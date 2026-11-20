import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO

// Web app (Phase 3c): wasmJs entry point hosting the shared Compose UI. Run `:web:wasmJsBrowserDistribution`
// for a static folder (build/dist/wasmJs/productionExecutable) that any HTTP server can serve.
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        outputModuleName.set("calisthenics-web")
        browser { commonWebpackConfig { outputFileName = "calisthenics-web.js" } }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain {
            // Catalog and privacy text are copied from their single source in the Android assets (nothing committed twice).
            resources.srcDir(layout.buildDirectory.dir("generated/webAssets"))
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
    }
}

// Everything below is copied from its single source (nothing is committed twice): Android assets for the catalog,
// privacy text, exercise thumbnails and demo clips; THIRD_PARTY_NOTICES.md for the licence screen.
val copyWebAssets = tasks.register<Copy>("copyWebAssets") {
    val assets = rootProject.layout.projectDirectory.dir("app/src/main/assets")
    from(assets) { include("catalog.json", "catalog_meta.json", "privacy.md") }
    from(assets.dir("thumbs")) { into("thumbs") }
    from(assets.dir("demos")) { into("demos") }
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")) { rename { "licenses.md" } }
    into(layout.buildDirectory.dir("generated/webAssets/data"))
}
tasks.matching { it.name == "wasmJsProcessResources" }.configureEach { dependsOn(copyWebAssets) }

// PWA icons: one source, the Android launcher artwork (app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png: the black
// tile with the athlete inside a white margin). The tile is cropped out and scaled to 180 (apple-touch-icon), 192, 512 and a
// maskable 512 (the figure sits inside the 80% safe circle). Nothing is committed; PNGs are written at build time.
val generatePwaIcons = tasks.register("generatePwaIcons") {
    val src = rootProject.layout.projectDirectory.file("app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png")
    val out = layout.buildDirectory.dir("generated/webAssets/icons")
    inputs.file(src)
    outputs.dir(out)
    doLast {
        System.setProperty("java.awt.headless", "true")
        val img = ImageIO.read(src.asFile)
        var x0 = img.width; var y0 = img.height; var x1 = -1; var y1 = -1
        for (y in 0 until img.height) for (x in 0 until img.width) {
            val rgb = img.getRGB(x, y)
            if ((rgb and 0xFF) < 64 && ((rgb shr 8) and 0xFF) < 64 && ((rgb shr 16) and 0xFF) < 64) {
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
            }
        }
        val side = minOf(x1 - x0 + 1, y1 - y0 + 1)
        val tile = img.getSubimage(x0, y0, side, side)
        fun scaled(size: Int): BufferedImage {
            var cur = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
            cur.createGraphics().apply { drawImage(tile, 0, 0, null); dispose() }
            var w = side
            while (w / 2 >= size) { // halve step by step for a clean downscale
                w /= 2
                val next = BufferedImage(w, w, BufferedImage.TYPE_INT_RGB)
                next.createGraphics().apply {
                    setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                    drawImage(cur, 0, 0, w, w, null); dispose()
                }
                cur = next
            }
            val fin = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
            fin.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                drawImage(cur, 0, 0, size, size, null); dispose()
            }
            return fin
        }
        val dir = out.get().asFile.also { it.mkdirs() }
        for ((name, size) in listOf("apple-touch-icon.png" to 180, "icon-192.png" to 192, "icon-512.png" to 512, "icon-maskable-512.png" to 512)) {
            ImageIO.write(scaled(size), "png", File(dir, name))
        }
    }
}
tasks.matching { it.name == "wasmJsProcessResources" }.configureEach { dependsOn(generatePwaIcons) }

// Service worker: written into the finished distribution (the wasm file names are only known after the webpack step). The shell
// cache name is the SHA-256 of every precached file (name + bytes), the media cache name that of the clips and thumbnails.
val generateServiceWorker = tasks.register("generateServiceWorker") {
    val template = layout.projectDirectory.file("pwa/sw.template.js")
    val dist = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    inputs.file(template)
    doLast {
        val root = dist.get().asFile
        fun sha(files: List<File>): String {
            val md = MessageDigest.getInstance("SHA-256")
            for (f in files) { md.update(f.relativeTo(root).invariantSeparatorsPath.toByteArray()); md.update(f.readBytes()) }
            return md.digest().joinToString("") { "%02x".format(it) }.take(16)
        }
        val all = root.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(root).invariantSeparatorsPath }.toList()
        val media = all.filter { it.relativeTo(root).invariantSeparatorsPath.let { p -> p.startsWith("data/demos/") || p.startsWith("data/thumbs/") } }
        val bundle = root.resolve("calisthenics-web.js").readText()
        val shell = all.filter { f ->
            val p = f.relativeTo(root).invariantSeparatorsPath
            // a stale *.wasm of an earlier build in the same folder is not precached: only wasm files the bundle names
            f !in media && p != "sw.js" && !p.endsWith(".map") && !p.endsWith(".LICENSE.txt") &&
                (!p.endsWith(".wasm") || bundle.contains(p))
        }
        val names = shell.map { it.relativeTo(root).invariantSeparatorsPath }
        root.resolve("sw.js").writeText(template.asFile.readText()
            .replace("__BUILD_ID__", sha(shell))
            .replace("__MEDIA_ID__", sha(media))
            .replace("__SHELL_FILES__", (listOf("./") + names).joinToString("', '")))
    }
}
tasks.matching { it.name == "wasmJsBrowserDistribution" }.configureEach { finalizedBy(generateServiceWorker) }

// The Binaryen distribution repository is declared in settings.gradle.kts (project repositories are not allowed).
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
    extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
}
