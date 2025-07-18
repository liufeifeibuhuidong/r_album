package com.rhyme.r_album

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
import android.net.Uri
import android.os.Environment
import android.os.Handler
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import kotlin.concurrent.thread

/** RAlbumPlugin */
class RAlbumPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private var context: Context? = null
    private val handler: Handler = Handler()

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.rhyme_lph/r_album")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        context = null
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "createAlbum" -> createAlbum(call, result)
            "saveAlbum" -> saveAlbum(call, result)
            else -> result.notImplemented()
        }
    }

    private fun createAlbum(call: MethodCall, result: Result) {
        val albumName = call.argument<String>("albumName")
        if (albumName == null) {
            result.success(false)
            return
        }
        thread {
            val rootFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), albumName)
            if (!rootFile.exists()) {
                rootFile.mkdirs()
            }
            handler.post {
                result.success(true)
            }
        }
    }

    private fun saveAlbum(call: MethodCall, result: Result) {
        val albumName = call.argument<String>("albumName")
        val filePaths = call.argument<List<String>>("filePaths")

        if (albumName == null) {
            result.error("100", "albumName is not null", null)
            return
        }
        if (filePaths == null) {
            result.error("101", "filePaths is not null", null)
            return
        }

        thread {
            val rootFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), albumName)
            if (!rootFile.exists()) {
                rootFile.mkdirs()
            }

            try {
                for (path in filePaths) {
                    val fileName = if (path.lastIndexOf('.') == -1) {
                        "${System.currentTimeMillis()}"
                    } else {
                        val suffix = path.substring(path.lastIndexOf(".") + 1)
                        "${System.currentTimeMillis()}.$suffix"
                    }
                    val itemFile = File(rootFile, fileName)
                    if (!itemFile.exists()) itemFile.createNewFile()
                    val output = itemFile.outputStream()
                    val input = FileInputStream(path)
                    val buf = ByteArray(1024)
                    var len: Int
                    while (input.read(buf).also { len = it } != -1) {
                        output.write(buf, 0, len)
                    }
                    output.flush()
                    output.close()
                    input.close()

                    handler.post {
                        context?.sendBroadcast(Intent(ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(itemFile)))
                    }
                }
                handler.post { result.success(true) }
            } catch (e: Exception) {
                handler.post { result.success(false) }
            }
        }
    }
}