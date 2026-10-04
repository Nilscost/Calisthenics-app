// Debug-only G1 runtime test screen (M0). Not shipped in release builds.
package io.github.gonbei774.calisthenicsmemory.spike

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File

class G1TestActivity : ComponentActivity() {
    private fun dir() = File(getExternalFilesDir(null), SpikeSessionService.LOG_DIR_NAME).also { it.mkdirs() }

    private fun start(blocks: Int, workMs: Long, restMs: Long, tag: String, speak: Boolean, duck: Boolean) {
        File(dir(), SpikeSessionService.DONE_MARKER).delete()
        val i = Intent(this, SpikeSessionService::class.java).setAction(SpikeSessionService.ACTION_RUN)
            .putExtra(SpikeSessionService.EXTRA_BLOCKS, blocks)
            .putExtra(SpikeSessionService.EXTRA_WORK_MS, workMs)
            .putExtra(SpikeSessionService.EXTRA_REST_MS, restMs)
            .putExtra(SpikeSessionService.EXTRA_TAG, tag)
            .putExtra(SpikeSessionService.EXTRA_SPEAK, speak)
            .putExtra(SpikeSessionService.EXTRA_DUCK, duck)
        ContextCompat.startForegroundService(this, i)
    }

    private fun stop() {
        startService(Intent(this, SpikeSessionService::class.java).setAction(SpikeSessionService.ACTION_STOP))
    }

    private fun share() {
        val files = dir().listFiles { f -> f.isFile }?.sortedBy { it.lastModified() }.orEmpty()
        if (files.isEmpty()) return
        val uris = ArrayList(files.map { FileProvider.getUriForFile(this, "$packageName.g1share", it) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).setType("text/plain")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .putExtra(Intent.EXTRA_SUBJECT, "G1 runtime test results")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, "Share G1 results"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var speak by remember { mutableStateOf(true) }
                    var duck by remember { mutableStateOf(true) }
                    var summary by remember { mutableStateOf("") }
                    var checks by remember { mutableStateOf("") }
                    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    fun refresh() {
                        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                        val notifOn = NotificationManagerCompat.from(this@G1TestActivity).areNotificationsEnabled()
                        val airplane = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
                        checks = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                            "Notifications allowed: $notifOn\n" +
                            "Battery optimisation ignored: ${pm.isIgnoringBatteryOptimizations(packageName)}\n" +
                            "Airplane mode on: $airplane"
                        val m = File(dir(), SpikeSessionService.DONE_MARKER)
                        summary = if (m.exists()) m.readText() else "No finished run yet."
                    }
                    LaunchedEffect(Unit) { while (true) { refresh(); delay(2000) } }
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).systemBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("G1 runtime test (debug build)", style = MaterialTheme.typography.titleLarge)
                        Text(checks)
                        if (Build.VERSION.SDK_INT >= 33) {
                            OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow notifications") }
                        }
                        OutlinedButton(onClick = {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }) { Text("Open battery optimisation settings") }
                        Row { Checkbox(speak, { speak = it }); Text("Speak each cue (offline English TTS)", Modifier.padding(top = 12.dp)) }
                        Row { Checkbox(duck, { duck = it }); Text("Duck other audio (play music to test)", Modifier.padding(top = 12.dp)) }
                        Button(onClick = { start(6, 1500, 1000, "smoke15s", speak, duck) }) { Text("Smoke test (about 15 s)") }
                        Button(onClick = { start(110, 15_000, 10_000, "full46min", speak, duck) }) { Text("Full test (45 min 40 s)") }
                        OutlinedButton(onClick = { stop() }) { Text("Stop") }
                        Button(onClick = { share() }) { Text("Share results (CSV + summary)") }
                        Text("Last finished run", style = MaterialTheme.typography.titleMedium)
                        Text(summary)
                        Text("Pass rule (spec A02): every cue at most 1 s late, total within 2 s. Only the owner decides G1.")
                    }
                }
            }
        }
    }
}
