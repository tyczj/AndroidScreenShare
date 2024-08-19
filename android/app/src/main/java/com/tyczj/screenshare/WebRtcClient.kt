package com.tyczj.screenshare

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Display
import androidx.core.hardware.display.DisplayManagerCompat
import org.json.JSONObject
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoTrack
import java.util.UUID

class WebRtcClient private constructor() {

    companion object{
        val instance: WebRtcClient = WebRtcClient()
    }

    interface WebRtcClientListener{
        fun onIceCandidateAdded(candidate: IceCandidate)
        fun sendAnswer(desc: SessionDescription)
        fun sendOffer(desc: SessionDescription)
    }

    var listener: WebRtcClientListener? = null

    private val eglBaseContext = EglBase.create().eglBaseContext

    private var peerConnection: PeerConnection? = null
    private var localStream: MediaStream?=null
    private var capturer: ScreenCapturerAndroid? = null
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
    }

    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun2.1.google.com:19302").createIceServer()
    )

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder().setVideoDecoderFactory(
            DefaultVideoDecoderFactory(eglBaseContext)
        ).setVideoEncoderFactory(
            DefaultVideoEncoderFactory(
                eglBaseContext, true, true
            )
        ).setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = false
        }).createPeerConnectionFactory()
    }

    fun startScreenCapture(context: Context, permissionIntent: Intent){
        if(peerConnection == null){

            capturer = ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d("TAG", "onStop: stopped screen casting permission")
                }
            })

            peerConnection = peerConnectionFactory.createPeerConnection(iceServer, object: PeerConnectionObserver(){

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    super.onIceConnectionChange(p0)
                    if(p0 == PeerConnection.IceConnectionState.DISCONNECTED){
                        close()
                    }
                }

                override fun onIceCandidate(candidate: IceCandidate?) {
                    super.onIceCandidate(candidate)
                    candidate?.let {
                        sendIceCandidate(it)
                    }
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    Log.d("TAG", "onAddStream: $p0")
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)
                    if(newState == PeerConnection.PeerConnectionState.CONNECTED){

                    }
                }

                override fun onRenegotiationNeeded() {
                    super.onRenegotiationNeeded()
                    createOffer()
                }

            })

            val defaultDisplay = DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext = context.createDisplayContext(defaultDisplay!!)

            val screenWidthPixels = displayContext.resources.displayMetrics.widthPixels
            val screenHeightPixels = displayContext.resources.displayMetrics.heightPixels

            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread",eglBaseContext)

            val localVideoSource = peerConnectionFactory.createVideoSource(capturer!!.isScreencast)

            capturer!!.initialize(surfaceTextureHelper,context,localVideoSource.capturerObserver)
            capturer!!.startCapture(screenWidthPixels,screenHeightPixels,30)

            val localVideoTrack: VideoTrack = peerConnectionFactory.createVideoTrack(UUID.randomUUID().toString(),localVideoSource)
            try{
                peerConnection?.addTrack(localVideoTrack, listOf(UUID.randomUUID().toString()))
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun createOffer() {
        peerConnection?.createOffer(object : SessionObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : SessionObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.sendOffer(desc!!)
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    fun handleAnswer(spd: String){
        val json = JSONObject(spd)
        val s = json.getString("sdp")
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, s)
        peerConnection?.setRemoteDescription(object: SessionObserver(){
            override fun onSetSuccess() {
                super.onSetSuccess()
            }
        }, sessionDescription)
    }

    fun answer() {
        peerConnection?.createAnswer(object : SessionObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : SessionObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.sendAnswer(desc!!)
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    fun handleOffer(spd: String){
        val json = JSONObject(spd)
        val s = json.getString("sdp")
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, s)
        peerConnection?.setRemoteDescription(object: SessionObserver(){

            override fun onSetSuccess() {
                super.onSetSuccess()
                answer()
            }
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate){
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(candidate: IceCandidate){
        listener?.onIceCandidateAdded(candidate)
    }

    fun close(){
        try {
            capturer?.stopCapture()
            capturer?.dispose()
            localStream?.dispose()
            peerConnection?.close()
            peerConnection = null
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}