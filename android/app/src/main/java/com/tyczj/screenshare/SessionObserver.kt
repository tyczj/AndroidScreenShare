package com.tyczj.screenshare

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SessionObserver: SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {
        Log.d("SessionObserver", "onCreateSuccess ${desc.toString()}")
    }

    override fun onSetSuccess() {
        Log.d("SessionObserver", "onSetSuccess")
    }

    override fun onCreateFailure(p0: String?) {
        Log.d("SessionObserver", "onCreateFailure $p0")
    }

    override fun onSetFailure(p0: String?) {
        Log.d("SessionObserver", "onSetFailure $p0")
    }
}