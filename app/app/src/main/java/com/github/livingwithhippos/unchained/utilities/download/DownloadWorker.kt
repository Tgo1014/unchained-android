package com.github.livingwithhippos.unchained.utilities.download

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedApplication
import com.github.livingwithhippos.unchained.data.service.DownloadStatus
import com.github.livingwithhippos.unchained.data.service.ForegroundDownloadService
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.URLConnection

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private var job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override suspend fun doWork(): Result {

        val sourceUrl: String = inputData.getString(MainActivityViewModel.KEY_DOWNLOAD_SOURCE)!!
        val folderUri: Uri = Uri.parse(inputData.getString(MainActivityViewModel.KEY_FOLDER_URI)!!)
        val fileName: String = inputData.getString(MainActivityViewModel.KEY_DOWNLOAD_NAME)!!

        val newFile: DocumentFile? = getFileDocument(sourceUrl, folderUri, fileName)

        if (newFile == null) {
            Timber.e("Error getting download location file")
            return Result.failure()
        }


        val outputStream = applicationContext.contentResolver.openOutputStream(newFile.uri)
        if (outputStream == null) {
            Timber.e("Error getting download uri")
            return Result.failure()
        }

        val notificationID = newFile.hashCode()

        // todo: customize this
        val client = OkHttpClient()
        val writer = FileWriter(
            outputStream
        )
        val downloader = Downloader(
            client,
            writer
        )
        scope.launch {
            writer.state.collect {
                // todo: manage progress

                when (it) {
                    DownloadStatus.Completed -> {
                        makeStatusNotification(
                            notificationID,
                            fileName,
                            applicationContext.getString(R.string.download_complete),
                            applicationContext
                        )
                    }
                    is DownloadStatus.Error -> {
                        makeStatusNotification(
                            notificationID,
                            fileName,
                            applicationContext.getString(R.string.error),
                            applicationContext
                        )
                    }
                    DownloadStatus.Paused -> {
                        // todo: add this, it also needs to be onGoing
                        makeStatusNotification(
                            notificationID,
                            fileName,
                            applicationContext.getString(R.string.paused),
                            applicationContext
                        )
                    }
                    DownloadStatus.Queued -> {
                        makeStatusNotification(
                            notificationID,
                            fileName,
                            applicationContext.getString(R.string.queued),
                            applicationContext
                        )
                    }
                    DownloadStatus.Stopped -> {
                        makeStatusNotification(
                            notificationID,
                            fileName,
                            applicationContext.getString(R.string.stopped),
                            applicationContext
                        )
                    }
                    is DownloadStatus.Running -> {

                        makeProgressStatusNotification(
                            notificationID,
                            fileName,
                            applicationContext.getString(
                                R.string.download_in_progress_format,
                                it.percent
                            ),
                            it.percent,
                            applicationContext
                        )
                    }
                }
            }
        }

        // this needs to be blocking, see https://developer.android.com/topic/libraries/architecture/workmanager/advanced/coroutineworker
        val downloadedSize: Long = downloader.download(sourceUrl)

        // todo: get whole size and check if it correspond
        return if (downloadedSize > 0)
            Result.success()
        else
            Result.failure()

    }


    private fun getFileDocument(
        sourceUrl: String,
        destinationFolder: Uri,
        fileName: String
    ): DocumentFile? {

        val folderUri: DocumentFile? =
            DocumentFile.fromTreeUri(applicationContext, destinationFolder)
        if (folderUri != null) {
            val extension: String = MimeTypeMap.getFileExtensionFromUrl(sourceUrl)
            var mime: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime == null) {
                mime = URLConnection.guessContentTypeFromName(sourceUrl)
                /*
                if (mime == null) {
                    val connection: URLConnection = URL(link).openConnection()
                    mime= connection.contentType
                }
                 */
                if (mime == null) {
                    // todo: use other checks or a random mime type
                    mime = "*/*"
                }
            }
            // todo: check if the extension needs to be removed as the docs say (it does not seem to)
            return folderUri.createFile(mime, fileName)
        } else {
            Timber.e("folderUri was null")
            return null
        }
    }
}

fun makeStatusNotification(
    id: Int,
    filename: String,
    title: String,
    context: Context)
{
    // Create the notification
    val builder = NotificationCompat.Builder(context, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo_no_background)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(ForegroundDownloadService.GROUP_KEY_DOWNLOADS)
        .setGroupSummary(false)
        .setOngoing(false)
        .setContentTitle(title)
        .setStyle(NotificationCompat.BigTextStyle().bigText(filename))

    // Show the notification
    NotificationManagerCompat.from(context).notify(id, builder.build())
}

fun makeProgressStatusNotification(
    id: Int,
    filename: String,
    title: String,
    progress: Int,
    context: Context
) {
    // Create the notification
    val builder = NotificationCompat.Builder(context, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo_no_background)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(ForegroundDownloadService.GROUP_KEY_DOWNLOADS)
        .setGroupSummary(true)
        .setOngoing(true)
        .setProgress(100, progress, false)
        .setContentTitle(title)
        .setStyle(NotificationCompat.BigTextStyle().bigText(filename))

    // Show the notification
    NotificationManagerCompat.from(context).notify(id, builder.build())
}