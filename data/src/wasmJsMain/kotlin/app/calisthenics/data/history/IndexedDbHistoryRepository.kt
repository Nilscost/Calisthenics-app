@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package app.calisthenics.data.history

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.toJsString

// The transform executes synchronously inside the get request callback while the readwrite transaction is active.
@JsFun("""(name, transform, success, failure) => {
  let blocked = false;
  const request = indexedDB.open(name, 1);
  request.onupgradeneeded = () => { if (!request.result.objectStoreNames.contains('history')) request.result.createObjectStore('history').put('{\"version\":1,\"state\":{}}', 'state'); };
  request.onerror = () => { if (!blocked) failure(String(request.error)); };
  request.onblocked = () => { if (!blocked) { blocked = true; failure('database open blocked'); } };
  request.onsuccess = () => {
    const db = request.result; if (blocked) { db.close(); return; } db.onversionchange = () => db.close(); let result = null, cause = null;
    try { const tx = db.transaction('history', 'readwrite');
    const store = tx.objectStore('history'); const get = store.get('state');
    get.onerror = () => { cause = String(get.error); tx.abort(); };
    get.onsuccess = () => {
      try { result = transform(get.result === undefined ? null : get.result); if (result === null) { cause = 'history transformation failed'; tx.abort(); } else store.put(result, 'state'); }
      catch (e) { cause = String(e); tx.abort(); }
    };
    tx.oncomplete = () => { db.close(); success(); };
    tx.onabort = () => { db.close(); failure(cause || String(tx.error || 'transaction aborted')); };
    tx.onerror = () => {};
    } catch (e) { db.close(); failure(String(e)); }
  };
}""")
private external fun updateDocument(name: JsString, transform: (JsString?) -> JsString?, success: () -> Unit, failure: (JsString) -> Unit)

@JsFun("""(name, success, failure) => {
  let blocked = false;
  const request = indexedDB.open(name, 1);
  request.onupgradeneeded = () => { if (!request.result.objectStoreNames.contains('history')) request.result.createObjectStore('history').put('{\"version\":1,\"state\":{}}', 'state'); };
  request.onerror = () => { if (!blocked) failure(String(request.error)); };
  request.onblocked = () => { if (!blocked) { blocked = true; failure('database open blocked'); } };
  request.onsuccess = () => { const db = request.result; if (blocked) { db.close(); return; } db.onversionchange = () => db.close(); let value = null; try { const tx = db.transaction('history', 'readonly');
    const get = tx.objectStore('history').get('state'); get.onsuccess = () => { value = get.result === undefined ? null : get.result; };
    tx.oncomplete = () => { db.close(); success(value); }; tx.onabort = () => { db.close(); failure(String(tx.error || 'transaction aborted')); };
    } catch (e) { db.close(); failure(String(e)); }
  };
}""")
private external fun readDocument(name: JsString, success: (JsString?) -> Unit, failure: (JsString) -> Unit)

@Serializable
private data class HistoryDocument(val version: Int, val state: HistoryState)

class IndexedDbHistoryRepository(private val databaseName: String = "calisthenics-history") : StateHistoryRepository() {
    private val json = Json { encodeDefaults = true }
    private fun decode(value: JsString?): HistoryState {
        require(value != null) { "History document is missing" }
        val document = json.decodeFromString(HistoryDocument.serializer(), value.toString())
        require(document.version == 1) { "Unsupported history storage version ${document.version}" }
        return document.state
    }
    override suspend fun readState(): HistoryState = suspendCancellableCoroutine { cont ->
        readDocument(databaseName.toJsString(), { value ->
            try { val state = decode(value); if (cont.isActive) cont.resume(state) }
            catch (e: Throwable) { if (cont.isActive) cont.resumeWithException(e) }
        }, { error -> if (cont.isActive) cont.resumeWithException(IllegalStateException(error.toString())) })
    }
    override suspend fun <T> updateState(transform: (HistoryState) -> Pair<HistoryState, T>): T = suspendCoroutine { cont ->
        var outcome: T? = null
        var transformError: Throwable? = null
        updateDocument(databaseName.toJsString(), { value ->
            try { val state = decode(value); val (next, result) = transform(state); outcome = result; json.encodeToString(HistoryDocument.serializer(), HistoryDocument(version = 1, state = next)).toJsString() }
            catch (e: Throwable) { transformError = e; null }
        }, { @Suppress("UNCHECKED_CAST") cont.resume(outcome as T) }, { error -> cont.resumeWithException(transformError ?: IllegalStateException(error.toString())) })
    }
}
