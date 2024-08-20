package com.tyczj.screenshare

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class AcknowledgeConnectionActivity: AppCompatActivity() {

    private lateinit var webRtcClient:WebRtcClient
    private lateinit var signalingServer: MessagingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webRtcClient = WebRtcClient.instance
        signalingServer = MessagingClient.instance
        startScreenCapture()
    }

    private fun startScreenCapture(){
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        ActivityCompat.startActivityForResult(this, mediaProjectionManager.createScreenCaptureIntent(), 1, null)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK){
//            lifecycleScope.launch {
                val intent = Intent(this@AcknowledgeConnectionActivity, MediaService::class.java)
                intent.putExtra(Intent.EXTRA_INTENT, data)
                startForegroundService(intent)
                val defaultDisplay = DisplayManagerCompat.getInstance(this@AcknowledgeConnectionActivity).getDisplay(Display.DEFAULT_DISPLAY)
                val displayContext = createDisplayContext(defaultDisplay!!)

                val screenWidthPixels = displayContext.resources.displayMetrics.widthPixels
                val screenHeightPixels = displayContext.resources.displayMetrics.heightPixels

                val json = JSONObject()
                json.put("type", "connectConfirm")
                json.put("screenWidth", screenWidthPixels)
                json.put("screenHeight", screenHeightPixels)
                signalingServer.sendMessage(json.toString())
//            }
        }

        finish()
    }
}