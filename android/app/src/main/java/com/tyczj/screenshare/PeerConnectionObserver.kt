package com.tyczj.screenshare

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

open class PeerConnectionObserver: PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.d("PeerConnectionObserver", "onSignalingChange $p0")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d("PeerConnectionObserver", "onIceConnectionChange $p0")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d("PeerConnectionObserver", "onIceConnectionReceivingChange")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.d("PeerConnectionObserver", "onIceGatheringChange $p0")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d("PeerConnectionObserver", "onIceCandidate ${candidate.toString()}")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.d("PeerConnectionObserver", "onIceCandidatesRemoved")
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.d("PeerConnectionObserver", "onAddStream ${p0.toString()}")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.d("PeerConnectionObserver", "onRemoveStream ${p0.toString()}")
    }

    override fun onDataChannel(p0: DataChannel?) {
        Log.d("PeerConnectionObserver", "onDataChannel ${p0.toString()}")
    }

    override fun onRenegotiationNeeded() {
        Log.d("PeerConnectionObserver", "onRenegotiationNeeded")
    }
}