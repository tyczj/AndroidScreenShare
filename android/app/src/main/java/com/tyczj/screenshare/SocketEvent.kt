package com.tyczj.screenshare

import io.ktor.websocket.CloseReason

sealed class SocketEvent {
    class Closed(reason: CloseReason?): SocketEvent()
    class MouseEvent(event: Event): SocketEvent()
    class IceCandidate(): SocketEvent()
    class Offer(): SocketEvent()
    class Answer(): SocketEvent()
    data object ConnectRequest: SocketEvent()
}

sealed class Event{
    class ClickEvent(x: Int, y: Int): Event()
    class Gesture(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Event()
    class LongPress(x: Int, y: Int): Event()
    data object RightClick: Event()
}