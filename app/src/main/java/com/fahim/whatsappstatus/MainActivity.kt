package com.fahim.whatsappstatus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.fahim.whatsappstatus.ui.theme.WhatsappStatusTheme
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.util.*


class MainActivity : ComponentActivity() {
    private fun hasStoragePermission() =
        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasAllFilesPermission() = Environment.isExternalStorageManager()

    private val TAG = MainActivity::class.java.name
    var statusFiles = emptyList<File>()
    override fun onCreate(savedInstanceState: Bundle?) {
        val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!hasAllFilesPermission())
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        uri
                    )
                )
        }
        if (!hasStoragePermission()) {
            Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) { /* ... */
                        statusFiles =
                            getFilesList("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses")

                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) { /* ... */
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) { /* ... */
                    }
                }).check()
        }
        super.onCreate(savedInstanceState)




        val PATH =
            Environment.getExternalStorageDirectory().absolutePath +
                    File.separator + "Android" +
                    File.separator + "media" +
                    File.separator + "com.whatsapp" +
                    File.separator + "WhatsApp" +
                    File.separator + "Media" +
                    File.separator + ".Statuses"
        /* val PATH =
             Environment.getExternalStorageDirectory().absolutePath + File.separator + "Download"
         */
        val destdir =
            Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "WhatsAppStatus"
        if (!File(destdir).exists()) {
            File(destdir).mkdir()
        }

        Log.e(TAG, "onCreate: " + PATH)
        statusFiles =
            getFilesList(PATH)
        for (file in statusFiles)
            Log.e("TAG", "onCreate: " + file.name)
        setContent {
            WhatsappStatusTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting(
                        statusFiles
                    ) { sourceFile: File -> copyFile(sourceFile) }
                }
            }
        }
    }

    fun copyFile(sourceFile: File): File? {
        // Make sure the source file exists and is a file (not a directory)
        if (!sourceFile.exists() || !sourceFile.isFile) {
            return null
        }

        val destdir =
            Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "WhatsAppStatus"

        val destDir = File(destdir)
        // Make sure the destination directory exists and is a directory
        if (!destDir.exists() || !destDir.isDirectory) {
            return null
        }

        // Construct the destination file path by appending the source file name to the destination directory path
        val destFilePath = "${destDir.absolutePath}/${sourceFile.name}"

        // Create a new file object for the destination file
        val destFile = File(destFilePath)

        // Copy the source file to the destination file
        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(this, "Successfully saved!", Toast.LENGTH_SHORT).show()

        // Return the destination file object
        return destFile
    }


    fun getFilesList(path: String): List<File> {
        val directory = File(path)
        if (!directory.isDirectory) {
            throw IllegalArgumentException("Path is not a directory")
        }
        return directory.listFiles { file -> file.isFile || file.isHidden }?.toList() ?: emptyList()
    }

}

@Composable
fun Greeting(statusFiles: List<File>, copyFile: (File) -> File?) {

    LazyColumn {
        items(statusFiles.size) { index ->
            val file = statusFiles[index]

            ImageTitleSubtitleDownloadRow(
                file.absolutePath,
                file.name,
                Date(file.lastModified()).toString().substring(0, 20),
                { copyFile(file) },
            )
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImageTitleSubtitleDownloadRow(
    imageUrl: String,
    title: String,
    subtitle: String,
    onDownloadClicked: () -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlideImage(
            model = imageUrl, contentDescription = null,
            Modifier
                .height(64.dp)
                .width(48.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title, fontWeight = FontWeight.Bold,
                maxLines = 1, // Set maximum number of lines
                overflow = TextOverflow.Ellipsis // Add ellipsis to overflowing text)
            )
            Text(text = subtitle)
        }
        Button(onClick = onDownloadClicked) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WhatsappStatusTheme {

//        Greeting(statusFiles = status)
    }
}