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

    private val client = HttpClient(CIO) {
        install(WebSockets){
            pingInterval = 30000
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val mutableSocketListener: MutableSharedFlow<SocketEvent> = MutableSharedFlow(0, 50, BufferOverflow.SUSPEND)

    val socketListener = mutableSocketListener.asSharedFlow()

    private suspend fun connect() {

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

                            }
                            "answer" -> {

                            }
                            "ice-candidate" -> {

                            }
                            "mouseEvent" -> {

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