@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package app.calisthenics.storageprobe

import app.calisthenics.data.history.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsString
import kotlin.js.toJsString

@JsFun("(key) => new URLSearchParams(location.search).get(key) || ''")
private external fun parameter(key: JsString): JsString
@JsFun("(status, text) => { document.body.dataset.status = status; document.body.textContent = text; }")
private external fun report(status: JsString, text: JsString)
@JsFun("""(name, value, success, failure) => {
  const req = indexedDB.open(name, 1);
  req.onerror = () => failure(String(req.error));
  req.onsuccess = () => { const db = req.result; const tx = db.transaction('history', 'readwrite');
    tx.objectStore('history').put(value, 'state');
    tx.oncomplete = () => { db.close(); success(); }; tx.onabort = () => { db.close(); failure(String(tx.error)); };
  };
}""")
private external fun writeRaw(name: JsString, value: JsString, success: () -> Unit, failure: (JsString) -> Unit)

@JsFun("""(name, success, failure) => {
  const req = indexedDB.open(name, 1);
  req.onerror = () => failure(String(req.error));
  req.onsuccess = () => { const db = req.result; const tx = db.transaction('history', 'readwrite');
    tx.objectStore('history').delete('state');
    tx.oncomplete = () => { db.close(); success(); }; tx.onabort = () => { db.close(); failure(String(tx.error)); };
  };
}""")
private external fun deleteRaw(name: JsString, success: () -> Unit, failure: (JsString) -> Unit)

private suspend fun deleteDocument(name: String) = suspendCancellableCoroutine<Unit> { c ->
    deleteRaw(name.toJsString(), { if (c.isActive) c.resume(Unit) }, { if (c.isActive) c.resumeWithException(IllegalStateException(it.toString())) })
}

private suspend fun corrupt(name: String, text: String) = suspendCancellableCoroutine<Unit> { c ->
    writeRaw(name.toJsString(), text.toJsString(), { if (c.isActive) c.resume(Unit) }, { if (c.isActive) c.resumeWithException(IllegalStateException(it.toString())) })
}

fun main() {
    val mode = parameter("mode".toJsString()).toString().ifEmpty { "contract" }
    val id = parameter("id".toJsString()).toString().ifEmpty { "default" }
    val name = "calisthenics-storage-test-$id"
    val repo = IndexedDbHistoryRepository(name)
    report("running".toJsString(), "{}".toJsString())
    MainScope().launch {
        try {
            when (mode) {
                "settingsPrepare" -> settingsPrepare()
                "settingsVerify" -> settingsVerify()
                "settingsAtomic" -> settingsAtomic()
                "contract" -> { exerciseHistoryContract(repo); check(repo.sessions().size == 2) }
                "prepare" -> repo.saveFinishedSession(PlanRecord("p", 9_007_199_254_740_993L, "r", 1, 1, "home", "{}"), SessionRow("s", "p", 9_007_199_254_740_993L, null, "COMPLETED"), emptyList())
                "revise" -> { repeat(10) { repo.reviseFeedback("s", "v", "MET", false, false, it.toLong()) } }
                "verify" -> {
                    check(repo.plan("p")?.createdAtEpochMs == 9_007_199_254_740_993L)
                    check(repo.sessions().single().startedAtEpochMs == 9_007_199_254_740_993L)
                    check(repo.feedbackHistory("s", "v").map { it.revision } == (1..20).toList())
                }
                "abort" -> {
                    val before = repo.sessions()
                    try { repo.saveFinishedSession(PlanRecord("new", 0, "r", 1, 1, "home", "{}"), SessionRow("bad", "missing", 0, null, "COMPLETED"), emptyList()); error("expected rejection") } catch (_: IllegalArgumentException) { }
                    check(repo.sessions() == before && repo.plan("new") == null)
                }
                "corrupt" -> {
                    repo.sessions()
                    corrupt(name, "not-json")
                    try { repo.sessions(); error("corruption accepted") } catch (_: kotlinx.serialization.SerializationException) { }
                }
                "missing" -> {
                    repo.sessions()
                    deleteDocument(name)
                    try { repo.sessions(); error("missing document accepted") } catch (_: IllegalArgumentException) { }
                }
                "version" -> {
                    repo.sessions()
                    corrupt(name, "{\"version\":99,\"state\":{}}")
                    try { repo.sessions(); error("unsupported version accepted") } catch (_: IllegalArgumentException) { }
                }
                else -> error("Unknown mode")
            }
            report("passed".toJsString(), ("{\"mode\":\"$mode\",\"id\":\"$id\"}").toJsString())
        } catch (e: Throwable) {
            report("failed".toJsString(), ("{\"error\":\"" + (e.message ?: e.toString()).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"}").toJsString())
        }
    }
}
