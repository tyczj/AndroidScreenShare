package com.tyczj.screenshare

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR
import android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import kotlin.coroutines.CoroutineContext

class RemoteService: AccessibilityService(), WebRtcClient.WebRtcClientListener, CoroutineScope {

    private val supervisor = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main.immediate + supervisor
    private var job: Job? = null

    private val gson = Gson()
    private val signalServer = MessagingClient.instance
    private val webRtcClient = WebRtcClient.instance

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {

    }

    override fun onInterrupt() {

    }

    override fun onUnbind(intent: Intent?): Boolean {
        job?.cancel()
        return super.onUnbind(intent)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        webRtcClient.listener = this
        job = launch {
            signalServer.connect()
            signalServer.socketListener.collect {socketEvent ->
                when(socketEvent){
                    is SocketEvent.ConnectRequest -> {
                        val intent = Intent(this@RemoteService, AcknowledgeConnectionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    is SocketEvent.Closed -> {

                    }
                    is SocketEvent.Answer -> {
                        webRtcClient.handleAnswer(socketEvent.sdp)
                    }
                    is SocketEvent.IceCandidate -> {
                        val iceCandidate = IceCandidate(socketEvent.sdpMid,socketEvent.sdpMLineIndex,socketEvent.sdp)
                        webRtcClient.addIceCandidate(iceCandidate)
                    }
                    is SocketEvent.MouseEvent -> {
                        when(val mouseEvent = socketEvent.event){
                            is Event.ClickEvent -> {
                                val path = Path()
                                path.moveTo(mouseEvent.x.toFloat(), mouseEvent.y.toFloat())

                                val clickStroke = StrokeDescription(path, 0, 1)
                                val clickBuilder = GestureDescription.Builder()
                                clickBuilder.addStroke(clickStroke);
                                dispatchGesture(clickBuilder.build(), null, null)
                            }
                            is Event.Gesture -> {
                                val path = Path()
                                path.moveTo(mouseEvent.startX.toFloat(), mouseEvent.startY.toFloat())
                                path.lineTo(mouseEvent.endX.toFloat(), mouseEvent.endY.toFloat())
                                val sd = StrokeDescription(path, 0, mouseEvent.duration)
                                val gd = GestureDescription.Builder().addStroke(sd).build()
                                dispatchGesture(gd, null, null)
                            }
                            is Event.LongPress -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val path = Path()
                                    path.moveTo(mouseEvent.x.toFloat(), mouseEvent.y.toFloat())
                                    val gd = GestureDescription.Builder().addStroke(StrokeDescription(path, 0, 1, true)).build()
                                    dispatchGesture(gd, null, null)
                                }
                            }
                            Event.RightClick -> {
                                performGlobalAction(GLOBAL_ACTION_BACK)
                            }
                        }
                    }
                    is SocketEvent.Offer -> {
                        webRtcClient.handleOffer(socketEvent.sdp)
                    }
                }
            }
        }

        val info = AccessibilityServiceInfo()
        if (Build.VERSION.SDK_INT >= 33) {
            info.flags = FLAG_INPUT_METHOD_EDITOR or FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        } else {
            info.flags = FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        setServiceInfo(info)
    }

    override fun onIceCandidateAdded(candidate: IceCandidate) {
        launch {
            val json = JSONObject()
            json.put("type", "ice-candidate")
            json.put("candidate", gson.toJson(candidate))
            json.put("sdpMLineIndex", candidate.sdpMLineIndex)
            json.put("sdpMid", candidate.sdpMid)
            json.put("sdp", candidate.sdp)
            signalServer.sendMessage(json.toString())
        }
    }

    override fun sendAnswer(desc: SessionDescription) {
        launch {
            val json = JSONObject()
            json.put("type", "answer")
            json.put("sdp", gson.toJson(desc))
            signalServer.sendMessage(json.toString())
        }
    }

    override fun sendOffer(desc: SessionDescription) {
        launch {
            val json = JSONObject()
            json.put("type", "offer")
            json.put("sdp", gson.toJson(desc))
            signalServer.sendMessage(json.toString())
        }
    }
}