package moe.styx.styx2m.misc

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.styx.common.compose.AppContextImpl
import moe.styx.common.compose.extensions.getPathAndIDFromAndroidURI
import moe.styx.common.compose.files.Stores
import moe.styx.common.compose.files.updateList
import moe.styx.common.compose.threads.DownloadedEntry
import moe.styx.common.util.Log
import moe.styx.common.util.launchThreaded
import moe.styx.styx2m.fetchDeviceAppConfig
import java.io.File

class DownloadCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.DOWNLOAD_COMPLETE") {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != -1L) {
                val dlManager = AppContextImpl.get().getSystemService(DownloadManager::class.java)
                val results = dlManager.query(DownloadManager.Query().setFilterById(id))
                val statusCol = results.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val uriCol = results.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val reasonCol = results.getColumnIndex(DownloadManager.COLUMN_REASON)
                if (statusCol != -1 && uriCol != -1 && results != null && results.moveToNext()) {
                    val status = results.getInt(statusCol)
                    val uri = results.getString(uriCol) ?: return
                    val (path, entryID) = uri.getPathAndIDFromAndroidURI()
                    val file = File(path)
                    if (status != DownloadManager.STATUS_SUCCESSFUL) {
                        val reason = if (reasonCol == -1) "Unknown" else results.getInt(reasonCol)
                        Log.e { "Download failed for file: $uri\nReason: $reason" }
                        if (file.exists())
                            runCatching { file.delete() }.onFailure {
                                Log.e(exception = it) { "Failed to delete download: ${file.absolutePath}" }
                            }.getOrNull()
                        return
                    }
                    if (file.exists()) {
                        Log.i { "Download completed and added to list: ${file.name}" }
                        launchThreaded {
                            try {
                                if (AppContextImpl.appConfig().appStoragePath.isBlank()) {
                                    AppContextImpl.appConfig = {
                                        fetchDeviceAppConfig(AppContextImpl.get())
                                    }
                                }
                                Stores.downloadedStore.updateList {
                                    it.add(DownloadedEntry(entryID, path))
                                }
                            } catch (ex: Exception) {
                                Log.e(exception = ex) { "Failed to save downloaded entry to list." }
                                runCatching { file.delete() }.onFailure {
                                    Log.e(exception = it) { "Failed to delete download: ${file.absolutePath}" }
                                }.getOrNull()
                            }
                        }
                    }
                }
            }
        }
    }
}