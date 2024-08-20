package com.tyczj.signalserver

import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.web.socket.*

class WebSocketHandler: TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.simpleName)

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        super.afterConnectionEstablished(session)
        logger.info("Session established: ${session.id}")
        sessions[session.id] = session
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        super.afterConnectionClosed(session, status)
        val savedSession = sessions.filter { entry -> entry.value.id == session.id  }.entries
        if(savedSession.isNotEmpty()){
            sessions.remove(savedSession.first().key)
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        super.handleTextMessage(session, message)
        logger.info(message.payload)
        try{
            val json = JSONObject(message.payload)
            when (json.getString("type")) {
                else -> {
                    // handles "ice-candidate", "offer", "answer", "connect", "connectConfirm", "keyEvent", "mouseEvent"
                    sessions.forEach { (_, s) ->
                        if(s.id != session.id){
                            s.sendMessage(message)
                        }
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
            logger.error(e.toString(), e)
        }
    }
}