package com.tyczj.screenshare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import kotlin.coroutines.CoroutineContext

class MediaService: Service(), WebRtcClient.WebRtcClientListener, MessagingClient.WebRtcMessageListener, CoroutineScope {

    private val supervisor = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main.immediate + supervisor

    private val webRtcClient = WebRtcClient.instance
    private val signalServer = MessagingClient.instance
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        webRtcClient.listener = this
        signalServer.listener = this

//        launch {
//            signalServer.socketListener.collect{socketEvent ->
//                when(socketEvent){
//                    is SocketEvent.Closed -> {
//
//                    }
//                    is SocketEvent.Answer -> {
//                        webRtcClient.handleAnswer(socketEvent.sdp)
//                    }
//                    is SocketEvent.IceCandidate -> {
//                        val iceCandidate = IceCandidate(socketEvent.sdpMid,socketEvent.sdpMLineIndex,socketEvent.sdp)
//                        webRtcClient.addIceCandidate(iceCandidate)
//                    }
//                    is SocketEvent.Offer -> {
//                        webRtcClient.handleOffer(socketEvent.sdp)
//                    }
//                    else -> {}
//                }
//            }
//        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val channel = NotificationChannel("1", "ScreenCapture", NotificationManager.IMPORTANCE_LOW)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, "1")
            .setContentTitle("ScreenRecord")
            .setContentText("Screen Recording")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(123456, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        if(intent?.hasExtra(Intent.EXTRA_INTENT) == true){
            val intentData: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)
            intentData?.let {
                webRtcClient.initializePeerConnection(this)
                webRtcClient.startScreenCapture(this@MediaService, intentData)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MediaService", "onDestroy")
//        cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onIceCandidateAdded(candidate: IceCandidate) {
//        launch {
            val json = JSONObject()
            json.put("type", "ice-candidate")
            json.put("candidate", gson.toJson(candidate))
            json.put("sdpMLineIndex", candidate.sdpMLineIndex)
            json.put("sdpMid", candidate.sdpMid)
            json.put("sdp", candidate.sdp)
            signalServer.sendMessage(json.toString())
//        }
    }

    override fun sendAnswer(desc: SessionDescription) {
//        launch {
            val json = JSONObject()
            json.put("type", "answer")
            json.put("sdp", gson.toJson(desc))
            signalServer.sendMessage(json.toString())

//            webRtcClient.addVideo(this@MediaService)
//        }
    }

    override fun sendOffer(desc: SessionDescription) {
//        launch {
            val json = JSONObject()
            json.put("type", "offer")
            json.put("sdp", gson.toJson(desc))
            signalServer.sendMessage(json.toString())
//        }
    }

    override fun onMessage(socketEvent: SocketEvent) {
        Log.d("MediaService", "New Event")
        when(socketEvent){
            is SocketEvent.Closed -> {

            }
            is SocketEvent.Answer -> {
                webRtcClient.handleAnswer(socketEvent.sdp)
            }
            is SocketEvent.IceCandidate -> {
                val iceCandidate = IceCandidate(socketEvent.sdpMid,socketEvent.sdpMLineIndex,socketEvent.sdp)
                webRtcClient.addIceCandidate(iceCandidate)
            }
            is SocketEvent.Offer -> {
                webRtcClient.handleOffer(socketEvent.sdp)
            }
            else -> {}
        }
    }
}