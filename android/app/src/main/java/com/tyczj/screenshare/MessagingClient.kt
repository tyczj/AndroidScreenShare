package com.tyczj.screenshare

import android.util.Log
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MessagingClient {

    companion object{
        val instance = MessagingClient()
    }

    interface WebRtcMessageListener{
        fun onMessage(socketEvent: SocketEvent)
    }

    interface MouseListener{
        fun onMouseEvent(event: Event)
        fun onConnect()
    }

    private val _client = OkHttpClient()
    private var _signalingServer: WebSocket? = null

//    private val client = HttpClient(CIO) {
//        install(WebSockets){
//            pingInterval = 30000
//        }
//    }

    private var session: DefaultClientWebSocketSession? = null
    private val mutableSocketListener: MutableSharedFlow<SocketEvent> = MutableSharedFlow(0, 50, BufferOverflow.SUSPEND)

    val socketListener = mutableSocketListener.asSharedFlow()

    var listener: WebRtcMessageListener? = null
    var mouseListener: MouseListener? = null

    init {
        connect()
    }

    fun connect() {

        val request = Request.Builder().url("ws://192.168.44.63:8888/").build()
        _signalingServer = _client.newWebSocket(request, object: WebSocketListener(){

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                t.printStackTrace()
                connect()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                Log.d("MessagingClient", "Message received: $text")
                val json = JSONObject(text)
                when(json.getString("type")){
                    "connectRequest" -> {
                        mouseListener?.onConnect()
//                                listener?.onMessage(SocketEvent.ConnectRequest)
//                                mutableSocketListener.emit(SocketEvent.ConnectRequest)
                    }
                    "offer" -> {
                        listener?.onMessage(SocketEvent.Offer(json.getString("sdp")))
//                                mutableSocketListener.emit(SocketEvent.Offer(json.getString("sdp")))
                    }
                    "answer" -> {
                        listener?.onMessage(SocketEvent.Answer(json.getString("sdp")))
//                                mutableSocketListener.emit(SocketEvent.Answer(json.getString("sdp")))
                    }
                    "ice-candidate" -> {
                        listener?.onMessage(SocketEvent.IceCandidate(json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("sdp")))
//                                mutableSocketListener.emit(SocketEvent.IceCandidate(json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("sdp")))
                    }
                    "mouseEvent" -> {
                        val event = json.getJSONObject("event")
                        when(event.getString("type")){
                            "click" -> {
                                mouseListener?.onMouseEvent(Event.ClickEvent(event.getInt("x"), event.getInt("y")))
//
//                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.ClickEvent(event.getInt("x"), event.getInt("y"))))
                            }
                            "gesture" -> {
                                mouseListener?.onMouseEvent(Event.Gesture(event.getInt("startX"), event.getInt("startY"), event.getInt("endX"), event.getInt("endY"), event.getLong("duration")))
//
//                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.Gesture(event.getInt("startX"), event.getInt("startY"), event.getInt("endX"), event.getInt("endY"), event.getLong("duration"))))
                            }
                            "longPress" -> {
                                mouseListener?.onMouseEvent(Event.LongPress(event.getInt("x"), event.getInt("y")))
//
//                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.LongPress(event.getInt("x"), event.getInt("y"))))
                            }
                            "rightClick" -> {
                                mouseListener?.onMouseEvent(Event.RightClick)
//
//                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.RightClick))
                            }
                        }
                    }
                }
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Log.d("MessagingClient", "Connected")
            }

        })
//        client.webSocket(host = "192.168.44.63", port = 8888, path = "/"){
//            session = this
//            while (isActive){
//                when(val frame = this.incoming.receive()){
//                    is Frame.Text -> {
//                        val text = frame.readText()
//                        Log.d("MessagingClient", "Message: $text")
//                        val json = JSONObject(text)
//                        when(json.getString("type")){
//                            "connectRequest" -> {
//                                mouseListener?.onConnect()
////                                listener?.onMessage(SocketEvent.ConnectRequest)
////                                mutableSocketListener.emit(SocketEvent.ConnectRequest)
//                            }
//                            "offer" -> {
//                                listener?.onMessage(SocketEvent.Offer(json.getString("sdp")))
////                                mutableSocketListener.emit(SocketEvent.Offer(json.getString("sdp")))
//                            }
//                            "answer" -> {
//                                listener?.onMessage(SocketEvent.Answer(json.getString("sdp")))
////                                mutableSocketListener.emit(SocketEvent.Answer(json.getString("sdp")))
//                            }
//                            "ice-candidate" -> {
//                                listener?.onMessage(SocketEvent.IceCandidate(json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("sdp")))
////                                mutableSocketListener.emit(SocketEvent.IceCandidate(json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("sdp")))
//                            }
//                            "mouseEvent" -> {
//                                val event = json.getJSONObject("event")
//                                when(event.getString("type")){
//                                    "click" -> {
//                                        mouseListener?.onMouseEvent(Event.ClickEvent(event.getInt("x"), event.getInt("y")))
////
////                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.ClickEvent(event.getInt("x"), event.getInt("y"))))
//                                    }
//                                    "gesture" -> {
//                                        mouseListener?.onMouseEvent(Event.Gesture(event.getInt("startX"), event.getInt("startY"), event.getInt("endX"), event.getInt("endY"), event.getLong("duration")))
////
////                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.Gesture(event.getInt("startX"), event.getInt("startY"), event.getInt("endX"), event.getInt("endY"), event.getLong("duration"))))
//                                    }
//                                    "longPress" -> {
//                                        mouseListener?.onMouseEvent(Event.LongPress(event.getInt("x"), event.getInt("y")))
////
////                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.LongPress(event.getInt("x"), event.getInt("y"))))
//                                    }
//                                    "rightClick" -> {
//                                        mouseListener?.onMouseEvent(Event.RightClick)
////
////                                        mutableSocketListener.emit(SocketEvent.MouseEvent(Event.RightClick))
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    is Frame.Binary -> {}
//                    is Frame.Close -> {
//                        session = null
////                        mutableSocketListener.emit(SocketEvent.Closed(frame.readReason()))
//                    }
//                    is Frame.Ping -> {}
//                    is Frame.Pong -> {}
//                }
//            }
//
//            session = null
//        }
    }

    fun sendMessage(message: String){
        Log.d("MessagingClient", "Sending message: $message")
        _signalingServer?.let {
            val sent = it.send(message)
            if(!sent){
                Log.d("MessagingClient","Message not sent")
            }else{
                Log.d("MessagingClient","Message sent")
            }
        }
//        Log.d("MessagingClient", "Sending message: $message")
//        session?.send(Frame.Text(message))
    }
}