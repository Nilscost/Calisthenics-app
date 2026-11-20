// Web backup files (Phase 4b): export = a download of the same backup JSON Android writes; import = a hidden
// <input type=file> whose text goes through the common backup code (validation, merge, safety copy).
package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@JsFun("""(name, text) => {
  const blob = new Blob([text], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = name; a.style.display = 'none';
  document.body.appendChild(a); a.click();
  setTimeout(() => { a.remove(); URL.revokeObjectURL(url); }, 30000);
}""")
private external fun downloadText(name: String, text: String)

/** Opens the file chooser; [onText] gets the file's text ("" when it cannot be read or is over 20 MB, as on Android). Must run inside the tap. */
@JsFun("""(onText) => {
  const input = document.createElement('input');
  input.type = 'file'; input.accept = '.json,application/json,text/*'; input.style.display = 'none';
  document.body.appendChild(input);
  input.addEventListener('change', async () => {
    let text = '';
    try { const f = input.files && input.files[0]; if (f && f.size <= 20000000) text = await f.text(); } catch (e) {}
    input.remove();
    onText(text);
  });
  input.addEventListener('cancel', () => input.remove());
  input.click();
}""")
private external fun pickFileText(onText: (JsString) -> Unit)

/** Keeps only the newest safety copy (browser storage is small); false when the browser refuses (quota). */
@JsFun("""(name, text) => {
  try {
    const prefix = 'cal_safety_backup:';
    for (let i = localStorage.length - 1; i >= 0; i--) { const k = localStorage.key(i); if (k && k.startsWith(prefix)) localStorage.removeItem(k); }
    localStorage.setItem(prefix + name, text);
    return true;
  } catch (e) { return false; }
}""")
private external fun storeSafetyCopy(name: String, text: String): Boolean

@Composable
actual fun rememberBackupFiles(): BackupFileLauncher {
    val scope = rememberCoroutineScope()
    return remember {
        object : BackupFileLauncher {
            override fun export(suggestedName: String, write: suspend (store: (String) -> Unit) -> Unit) {
                scope.launch { write { text -> downloadText(suggestedName, text) } }
            }
            override fun import(onText: (String) -> Unit) = pickFileText { onText(it.toString()) }
        }
    }
}

/** Web: the browser has no app-private folder, so the pre-restore copy sits in localStorage (newest only). */
actual fun PlatformContext.writeSafetyBackup(name: String, text: String) {
    if (!storeSafetyCopy(name, text)) error("The safety copy did not fit into the browser's storage.")
}
