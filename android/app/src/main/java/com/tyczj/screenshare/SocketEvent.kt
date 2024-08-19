package com.tyczj.screenshare

import io.ktor.websocket.CloseReason

sealed class SocketEvent {
    class Closed(val reason: CloseReason?): SocketEvent()
    class MouseEvent(val event: Event): SocketEvent()
    class IceCandidate(val sdpMid: String, val sdpMLineIndex: Int, val sdp: String): SocketEvent()
    class Offer(val sdp: String): SocketEvent()
    class Answer(val sdp: String): SocketEvent()
    data object ConnectRequest: SocketEvent()
}

sealed class Event{
    class ClickEvent(val x: Int, val y: Int): Event()
    class Gesture(val startX: Int, val startY: Int, val endX: Int, val endY: Int, val duration: Long): Event()
    class LongPress(val x: Int, val y: Int): Event()
    data object RightClick: Event()
}