package com.tyczj.screenshare

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import org.json.JSONObject

class MessagingClient {

    companion object{
        val instance = MessagingClient()
    }

    private val client = HttpClient(CIO) {
        install(WebSockets){
            pingInterval = 30000
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val mutableSocketListener: MutableSharedFlow<SocketEvent> = MutableSharedFlow(0, 50, BufferOverflow.SUSPEND)

    val socketListener = mutableSocketListener.asSharedFlow()

    suspend fun connect() {

        client.webSocket(host = "127.0.0.1", port = 8080, path = "/"){
            session = this
            while (isActive){
                when(val frame = this.incoming.receive()){
                    is Frame.Text -> {
                        val text = frame.readText()
                        val json = JSONObject(text)
                        when(json.getString("type")){
                            "connectRequest" -> {
                                mutableSocketListener.emit(SocketEvent.ConnectRequest)
                            }
                            "offer" -> {
                                mutableSocketListener.emit(SocketEvent.Offer(json.getString("sdp")))
                            }
                            "answer" -> {
                                mutableSocketListener.emit(SocketEvent.Answer(json.getString("sdp")))
                            }
                            "ice-candidate" -> {
                                mutableSocketListener.emit(SocketEvent.IceCandidate(json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("sdp")))
                            }
                            "mouseEvent" -> {
                                val event = json.getJSONObject("event")
                                when(event.getString("type")){
                                    "click" -> {
                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.ClickEvent(event.getInt("x"), event.getInt("y"))))
                                    }
                                    "gesture" -> {
                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.Gesture(event.getInt("startX"), event.getInt("startY"), event.getInt("endX"), event.getInt("endY"), event.getLong("duration"))))
                                    }
                                    "longPress" -> {
                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.LongPress(event.getInt("x"), event.getInt("y"))))
                                    }
                                    "rightClick" -> {
                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.RightClick))
                                    }
                                }
                            }
                        }
                    }
                    is Frame.Binary -> {}
                    is Frame.Close -> {
                        session = null
                        mutableSocketListener.emit(SocketEvent.Closed(frame.readReason()))
                    }
                    is Frame.Ping -> {}
                    is Frame.Pong -> {}
                }
            }

            session = null
        }
    }

    suspend fun sendMessage(message: String){
        session?.send(Frame.Text(message))
    }
}