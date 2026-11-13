// Routines to and from a chatbot: the guide the person gives the chatbot (built from the loaded catalog, so it is never stale) and the
// routine format v1 the chatbot writes back. Pure code: the screen only moves text between files, the text field and the saved routines.
package app.calisthenics.domain.routine

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.missingFor
import app.calisthenics.domain.model.Area
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.ExerciseVariation
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.model.ProgressionRule
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.model.RoutineSlot
import app.calisthenics.domain.model.SideMode
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.planner.MAX_EXPLICIT_ROUNDS
import app.calisthenics.domain.planner.MIN_TRAIN_ROUNDS
import app.calisthenics.domain.planner.PlanEdits
import app.calisthenics.domain.planner.TrainSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

const val ROUTINE_FORMAT_VERSION = 1
const val GUIDE_FILE_NAME = "calisthenics-guide.md"
const val MAX_ROUTINE_NAME = 40
const val MAX_ROUTINE_EXERCISES = 30
const val MAX_EXCHANGE_REPS = 100
const val MAX_EXCHANGE_SECONDS = 600
private const val DEFAULT_IMPORT_SETS = 3
private const val ROUTINE_KEY = "calisthenicsRoutine"

/** One exercise of a routine in the exchange format. [reps] / [seconds] null = the person's current level; given = a fixed number (per side for sided exercises). */
data class ExchangeExercise(val id: String, val reps: Int? = null, val seconds: Int? = null)

data class ExchangeRoutine(val name: String, val sets: Int, val format: WorkoutFormat, val exercises: List<ExchangeExercise>)

/** What was found in pasted text or a file. Only [canSave] results may be saved; [warnings] never block. */
data class RoutineImportResult(val routines: List<ExchangeRoutine>, val errors: List<String>, val warnings: List<String>) {
    val canSave: Boolean get() = routines.isNotEmpty() && errors.isEmpty()
}

/** An exercise the exchange accepts: strength reps or holds (stretches are added by the app between sets, never listed in a routine). */
fun isRoutineExercise(v: ExerciseVariation) = v.kind == Kind.REPS || v.kind == Kind.HOLD

// ---------- import ----------

fun parseRoutines(text: String, catalog: Catalog, profile: EquipmentProfile? = null): RoutineImportResult {
    val root = findRoutineJson(straightenQuotes(text))
        ?: return RoutineImportResult(emptyList(), listOf("No routine found in this text. Paste the JSON block the chatbot wrote, with \"$ROUTINE_KEY\": $ROUTINE_FORMAT_VERSION at the top."), emptyList())
    val items = if (root is JsonArray) root.toList() else listOf(root)
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val routines = items.mapIndexedNotNull { i, item -> parseOne(item, if (items.size > 1) "Routine ${i + 1}" else "Routine", catalog, profile, errors, warnings) }
    return RoutineImportResult(routines, errors, warnings)
}

private fun parseOne(item: JsonElement, label: String, catalog: Catalog, profile: EquipmentProfile?, errors: MutableList<String>, warnings: MutableList<String>): ExchangeRoutine? {
    val obj = item as? JsonObject ?: run { errors += "$label is not a routine object."; return null }
    val version = (obj[ROUTINE_KEY] as? JsonPrimitive)?.intOrNull
    if (version != ROUTINE_FORMAT_VERSION) {
        errors += if (obj[ROUTINE_KEY] == null) "$label has no \"$ROUTINE_KEY\": $ROUTINE_FORMAT_VERSION." else "$label: format version ${obj[ROUTINE_KEY]} is not supported (this app reads version $ROUTINE_FORMAT_VERSION)."
        return null
    }
    val rawName = (obj["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim().orEmpty()
    val name = rawName.ifBlank { "Imported routine" }.take(MAX_ROUTINE_NAME)
    if (rawName.isBlank()) warnings += "$label has no name; it is called \"$name\"."
    if (rawName.length > MAX_ROUTINE_NAME) warnings += "$label: the name was shortened to $MAX_ROUTINE_NAME characters."
    val where = if (label == "Routine") "\"$name\"" else "$label (\"$name\")"
    var ok = true
    fun fail(msg: String) { errors += "$where: $msg"; ok = false }

    val sets = when (val s = obj["sets"]) {
        null -> DEFAULT_IMPORT_SETS
        else -> (s as? JsonPrimitive)?.intOrNull?.takeIf { it in MIN_TRAIN_ROUNDS..MAX_EXPLICIT_ROUNDS } ?: run { fail("sets must be a number from $MIN_TRAIN_ROUNDS to $MAX_EXPLICIT_ROUNDS."); DEFAULT_IMPORT_SETS }
    }
    val format = when (val f = obj["format"]) {
        null -> WorkoutFormat.CIRCUIT
        else -> (f as? JsonPrimitive)?.content?.trim()?.let { c -> WorkoutFormat.entries.firstOrNull { it.name.equals(c, ignoreCase = true) } }
            ?: run { fail("format must be one of ${WorkoutFormat.entries.joinToString(", ")}."); WorkoutFormat.CIRCUIT }
    }
    val list = obj["exercises"] as? JsonArray
    if (list == null || list.isEmpty()) { fail("exercises must be a list with at least one exercise."); return null }
    if (list.size > MAX_ROUTINE_EXERCISES) fail("at most $MAX_ROUTINE_EXERCISES exercises.")
    val exercises = list.mapIndexedNotNull { i, e -> parseExercise(e, "$where, exercise ${i + 1}", catalog, profile, errors, warnings) ?: run { ok = false; null } }
    return if (ok) ExchangeRoutine(name, sets, format, exercises) else null
}

private fun parseExercise(e: JsonElement, label: String, catalog: Catalog, profile: EquipmentProfile?, errors: MutableList<String>, warnings: MutableList<String>): ExchangeExercise? {
    val obj = e as? JsonObject ?: run { errors += "$label is not an object like {\"id\": \"pullup-full\"}."; return null }
    val id = (obj["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim().orEmpty()
    if (id.isEmpty()) { errors += "$label has no \"id\"."; return null }
    val v = catalog.variation(id)
    if (v == null) {
        val close = closestExercises(id, catalog)
        errors += "$label: unknown id \"$id\"." + if (close.isEmpty()) "" else " Closest: " + close.joinToString("; ") { "${it.id} (${it.name})" } + "."
        return null
    }
    if (!isRoutineExercise(v)) { errors += "$label: \"$id\" is a stretch. The app adds stretches itself; leave it out of the routine."; return null }
    val reps = obj["reps"]?.let { (it as? JsonPrimitive)?.intOrNull ?: run { errors += "$label ($id): reps must be a whole number."; return null } }
    val seconds = obj["seconds"]?.let { (it as? JsonPrimitive)?.intOrNull ?: run { errors += "$label ($id): seconds must be a whole number."; return null } }
    val hold = v.kind == Kind.HOLD
    var ok = true
    fun fail(msg: String) { errors += "$label ($id): $msg"; ok = false }
    when {
        reps != null && seconds != null -> fail("give reps or seconds, not both.")
        reps != null && hold -> fail("${v.name} is a hold; use \"seconds\", not \"reps\".")
        seconds != null && !hold -> fail("${v.name} counts reps; use \"reps\", not \"seconds\".")
        reps != null -> {
            val cap = catalog.policyFor(v)?.maxRepsPerSet ?: MAX_EXCHANGE_REPS
            if (reps !in 1..minOf(cap, MAX_EXCHANGE_REPS)) fail("reps $reps is out of range (1 to ${minOf(cap, MAX_EXCHANGE_REPS)}).")
        }
        seconds != null -> if (seconds !in 1..MAX_EXCHANGE_SECONDS) fail("seconds $seconds is out of range (1 to $MAX_EXCHANGE_SECONDS).")
    }
    if (!ok) return null
    if (profile != null && !isAvailable(v, profile)) warnings += "$label: ${v.name} needs ${missingFor(v, profile)}, which the profile \"${profile.name}\" does not have."
    return ExchangeExercise(id, reps, seconds)
}

/** Up to three exercises whose id or name is closest to [query]: shared words first, then edit distance. */
fun closestExercises(query: String, catalog: Catalog, limit: Int = 3): List<ExerciseVariation> {
    val q = query.lowercase().trim()
    val words = q.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
    return catalog.variations.filter(::isRoutineExercise).map { v ->
        val own = (v.id.lowercase().split('-') + v.name.lowercase().split(Regex("[^a-z0-9]+"))).filter { it.isNotEmpty() }.toSet()
        Triple(v, words.count { it in own }, minOf(editDistance(q, v.id.lowercase()), editDistance(q, v.name.lowercase())))
    }.sortedWith(compareBy({ -it.second }, { it.third }, { it.first.id })).take(limit).map { it.first }
}

private fun editDistance(a: String, b: String): Int {
    var prev = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val cur = IntArray(b.length + 1)
        cur[0] = i
        for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        prev = cur
    }
    return prev[b.length]
}

/** Chat apps turn straight quotes into curly ones; JSON needs the straight ones. */
private fun straightenQuotes(s: String) = s.replace('“', '"').replace('”', '"').replace('„', '"').replace('‘', '\'').replace('’', '\'')

private val fence = Regex("```[A-Za-z0-9]*[ \\t]*\\r?\\n?(.*?)```", RegexOption.DOT_MATCHES_ALL)

/** The first fenced block or bare JSON object / array in [text] that is a routine; failing that, the first JSON value at all (so the error can say what is wrong). */
private fun findRoutineJson(text: String): JsonElement? {
    val found = mutableListOf<JsonElement>()
    fun add(s: String) { runCatching { Json.parseToJsonElement(s.trim()) }.getOrNull()?.takeIf { it is JsonObject || it is JsonArray }?.let { found += it } }
    fence.findAll(text).forEach { add(it.groupValues[1]) }
    var i = 0
    var tries = 0
    while (i < text.length && tries < 40) {
        val c = text[i]
        if (c == '{' || c == '[') {
            tries++
            val end = balancedEnd(text, i)
            if (end != null) { val before = found.size; add(text.substring(i, end + 1)); i = if (found.size > before) end + 1 else i + 1; continue }
        }
        i++
    }
    fun isRoutine(e: JsonElement) = (e is JsonObject && ROUTINE_KEY in e) || (e is JsonArray && e.isNotEmpty() && e.all { it is JsonObject && ROUTINE_KEY in it })
    return found.firstOrNull(::isRoutine) ?: found.firstOrNull()
}

private fun balancedEnd(s: String, start: Int): Int? {
    var depth = 0
    var inString = false
    var i = start
    while (i < s.length) {
        val c = s[i]
        if (inString) { if (c == '\\') i++ else if (c == '"') inString = false }
        else when (c) { '"' -> inString = true; '{', '[' -> depth++; '}', ']' -> { depth--; if (depth == 0) return i } }
        i++
    }
    return null
}

// ---------- routine <-> SavedRoutine ----------

/** Slot ids of an imported routine; a fixed number is stored as a free target on its slot, the exercise it belongs to as an override variation. */
private fun slotId(index: Int) = "x${index + 1}"

/**
 * The routine as the app stores it: one slot per exercise in order, fixed numbers as free targets (per side for sided exercises),
 * sets and format in the settings. [base] carries the person's own defaults (rest type, warm-up, ...).
 */
fun savedFromExchange(r: ExchangeRoutine, catalog: Catalog, id: String, now: Long, base: TrainSettings = TrainSettings()): SavedRoutine {
    val slots = r.exercises.mapIndexed { i, e ->
        val v = catalog.variation(e.id) ?: error("unknown exercise ${e.id}")
        RoutineSlot(slotId(i), v.patterns.first(), v.areas.firstOrNull() ?: Area.UPPER_BODY, v.id)
    }
    val fixed = r.exercises.mapIndexedNotNull { i, e -> (e.reps ?: e.seconds)?.let { slotId(i) to it } }.toMap()
    val edits = PlanEdits(freeTargets = fixed, overrideVariations = r.exercises.indices.filter { slotId(it) in fixed }.associate { slotId(it) to r.exercises[it].id })
    val title = r.name.trim().ifBlank { "Imported routine" }
    return savedFrom(id, title, 1, Routine(id, 1, title, slots), edits, base.copy(rounds = r.sets, format = r.format), ProgressionRule(), now)
}

/** The exchange form of a saved routine (a routine made from a program has no slots and gives an empty list). */
fun exchangeOf(saved: SavedRoutine, catalog: Catalog): ExchangeRoutine = ExchangeRoutine(saved.name, saved.sets, saved.format, saved.routine.slots.map { s ->
    val fixed = saved.edits.freeTargets[s.id]
    val hold = catalog.variation(s.preferredVariationId)?.kind == Kind.HOLD
    ExchangeExercise(s.preferredVariationId, reps = fixed.takeIf { !hold }, seconds = fixed.takeIf { hold })
})

// ---------- JSON out ----------

private fun ExchangeRoutine.toJson(): JsonObject = buildJsonObject {
    put(ROUTINE_KEY, ROUTINE_FORMAT_VERSION)
    put("name", name)
    put("sets", sets)
    put("format", format.name)
    putJsonArray("exercises") {
        exercises.forEach { e -> add(buildJsonObject { put("id", e.id); e.reps?.let { put("reps", it) }; e.seconds?.let { put("seconds", it) } }) }
    }
}

/** One routine as a JSON object, several as an array. */
fun routinesToJson(routines: List<ExchangeRoutine>): String =
    if (routines.size == 1) routines.single().toJson().toString() else buildJsonArray { routines.forEach { add(it.toJson()) } }.toString()

// ---------- the guide ----------

private fun equipmentLabel(v: ExerciseVariation): String {
    val alts = v.equipmentAlternatives.map { set ->
        (set.needs.map { if (it.quantity > 1) "${it.equipmentId} x${it.quantity}" else it.equipmentId } + set.capabilities.toList()).joinToString(" + ")
    }
    return if (alts.isEmpty() || alts.any { it.isEmpty() }) "none" else alts.joinToString(" or ")
}

private fun sidesLabel(v: ExerciseVariation) = when (v.sideMode) {
    SideMode.NONE -> "total"
    SideMode.SPLIT -> "per side (left, then right)"
    SideMode.ALTERNATING -> "per side (alternating)"
}

private fun areaLabel(v: ExerciseVariation): String {
    val names = if (v.areas.isNotEmpty()) v.areas.map { it.name } else if (v.stretchAreas.isNotEmpty()) v.stretchAreas.map { it.name } else v.patterns.map { it.name }
    return names.joinToString(", ") { it.lowercase().replace('_', ' ') }
}

private fun targetLabel(t: Target) = if (t.max != null && t.max != t.value) "${t.value}-${t.max}" else "${t.value}"

private fun cell(s: String) = s.replace('|', '/').replace('\n', ' ')

private fun strengthTable(catalog: Catalog, list: List<ExerciseVariation>) = buildString {
    appendLine("| id | name | unit | sides | equipment | area | L1 | L2 | L3 | L4 | L5 |")
    appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
    for (v in list) {
        val tiers = catalog.policyFor(v)?.tiers.orEmpty().associate { it.index to targetLabel(it.target) }
        val unit = if (v.kind == Kind.HOLD) "seconds (hold)" else "reps"
        appendLine("| `${v.id}` | ${cell(v.name)} | $unit | ${sidesLabel(v)} | ${cell(equipmentLabel(v))} | ${areaLabel(v)} | " + (1..5).joinToString(" | ") { tiers[it] ?: "-" } + " |")
    }
}

/** A small valid routine from the catalog itself (bodyweight, no sides), so the guide's example always uses ids that exist. */
fun exampleRoutine(catalog: Catalog): ExchangeRoutine {
    val pool = catalog.variations.filter { isRoutineExercise(it) && !it.routineOnly && it.sideMode == SideMode.NONE && it.equipmentAlternatives.any { s -> s.needs.isEmpty() } }
    val reps = pool.filter { it.kind == Kind.REPS }
    val holds = pool.filter { it.kind == Kind.HOLD }
    val picks = listOfNotNull(
        reps.firstOrNull()?.let { ExchangeExercise(it.id) },
        holds.firstOrNull()?.let { ExchangeExercise(it.id, seconds = catalog.policyFor(it)?.tiers?.minByOrNull { t -> t.index }?.target?.value ?: 30) },
        reps.getOrNull(1)?.let { ExchangeExercise(it.id, reps = 8) },
    )
    return ExchangeRoutine("Example day", 3, WorkoutFormat.CIRCUIT, picks)
}

/** The Markdown file the person gives to a chatbot: what to produce, the format with one example, and the exercise library as tables. */
fun exportGuide(catalog: Catalog): String {
    val main = catalog.variations.filter { isRoutineExercise(it) && !it.routineOnly }
    val routineOnly = catalog.variations.filter { isRoutineExercise(it) && it.routineOnly }
    val stretches = catalog.variations.filter { !isRoutineExercise(it) }
    val example = exampleRoutine(catalog)
    return buildString {
        appendLine("# Calisthenics app: guide for a chatbot")
        appendLine()
        appendLine("The person uses a calisthenics app (bodyweight training, no gym needed). You help them write workout routines the app can import.")
        appendLine("Write a routine only with exercises from the library below, using their `id`, and answer with one JSON block in the format below.")
        appendLine("The app checks every id, so do not invent exercises, and do not list stretches: the app adds stretches between sets itself.")
        appendLine("Ask the person about their goal, time and equipment if you do not know them yet, then keep the routine short (usually 4 to 8 exercises).")
        appendLine("Put the JSON in a code block and add a sentence or two of explanation outside it.")
        appendLine()
        appendLine("## Routine format (version $ROUTINE_FORMAT_VERSION)")
        appendLine()
        appendLine("```json")
        appendLine(example.toJson().toString())
        appendLine("```")
        appendLine()
        appendLine("- `calisthenicsRoutine`: always $ROUTINE_FORMAT_VERSION. `name`: up to $MAX_ROUTINE_NAME characters.")
        appendLine("- `sets`: $MIN_TRAIN_ROUNDS to $MAX_EXPLICIT_ROUNDS (default $DEFAULT_IMPORT_SETS). `format`: ${WorkoutFormat.entries.joinToString(", ") { "`${it.name}`" }} " +
            "(CIRCUIT = all exercises, then the next set; PAIRS = two exercises alternate through all their sets; STRAIGHT = all sets of one exercise, then the next).")
        appendLine("- `exercises`: 1 to $MAX_ROUTINE_EXERCISES items, each `{\"id\": \"...\"}`. Add `reps` (for exercises counted in reps) or `seconds` (for holds) only when you want a fixed number " +
            "(reps 1 to $MAX_EXCHANGE_REPS, seconds 1 to $MAX_EXCHANGE_SECONDS). Left out, the app uses the person's current level for that exercise and keeps progressing it.")
        appendLine("- A fixed number is per side for exercises whose sides column says \"per side\" (left then right, or alternating within one set), and the total otherwise.")
        appendLine("- Several routines: put them in one JSON array.")
        appendLine("- Each library row below has five levels (L1 easiest to L5 hardest) with the number the app asks for at that level; the unit column says reps or seconds.")
        appendLine()
        appendLine("## Exercise library")
        appendLine()
        append(strengthTable(catalog, main))
        appendLine()
        appendLine("## Routine-only exercises")
        appendLine()
        appendLine("Drill and conditioning exercises that fit a routine but are never picked automatically. Same format as above.")
        appendLine()
        append(strengthTable(catalog, routineOnly))
        appendLine()
        appendLine("## Stretches and mobility (for information, not for routines)")
        appendLine()
        appendLine("| id | name | seconds | sides | equipment | area |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        for (v in stretches) appendLine("| `${v.id}` | ${cell(v.name)} | ${v.defaultSeconds ?: "-"} | ${if (v.sideMode == SideMode.NONE) "total" else "per side"} | ${cell(equipmentLabel(v))} | ${areaLabel(v)} |")
    }
}
