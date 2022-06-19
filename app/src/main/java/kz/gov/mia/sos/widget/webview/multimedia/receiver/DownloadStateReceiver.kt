package kz.gov.mia.sos.widget.webview.multimedia.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull

internal class DownloadStateReceiver constructor(
    private val listener: Listener
) : BroadcastReceiver() {

    companion object {
        private val TAG = DownloadStateReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive() -> context: $context, intent: $intent")

        if (context == null) return
        val action = intent?.action
        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId < 0) return

            val downloadManager =
                ContextCompat.getSystemService(context, DownloadManager::class.java) ?: return

            val cursor: Cursor = downloadManager.query(
                DownloadManager.Query()
                    .setFilterById(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1))
            )

            Log.d(TAG, "onReceive() -> cursor: $cursor")

            if (cursor.moveToFirst()) {
                if (cursor.count > 0) {
                    val status =
                        cursor.getIntOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                    Log.d(TAG, "onReceive() -> status: $status")

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val localUri: String? =
                            cursor.getStringOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        val mimeType: String? =
                            downloadManager.getMimeTypeForDownloadedFile(downloadId)
                        Log.d(TAG, "onReceive() -> downloadId: $downloadId, localUri: $localUri")
                        listener.onFileUriReady(downloadId, Uri.parse(localUri), mimeType)
                    }
                }
            }

            cursor.close()
        }
    }

    fun interface Listener {
        fun onFileUriReady(downloadId: Long, uri: Uri?, mimeType: String?)
    }

}
