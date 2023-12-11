package es.unizar.urlshortener.infrastructure.delivery

import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import java.util.*

/*
class SenderWS (private val session: Session, private val url: String) : Runnable{
    override fun run() {
        val logger = LoggerFactory.getLogger(SenderWS::class.java)
        logger.info("recipient: ${session.id} message: $url")
        with(session.basicRemote) {
            sendTextSafe("Soy un runnable")
        }
    }
}

class InterstitialWS() {
    private lateinit var taskExecutor: TaskExecutor

    fun ejecutarPrueba(session: Session) {
        val prueba = SenderWS(session, "url")
        taskExecutor.execute(prueba)
    }
}

@Configuration(proxyBeanMethods = false)
class WebSocketConfig {
    @Bean
    fun serverEndpoint() = ServerEndpointExporter()
}
 */

/**
 * If the websocket connection underlying this [RemoteEndpoint] is busy sending a message when a call is made to send
 * another one, for example if two threads attempt to call a send method concurrently, or if a developer attempts to
 * send a new message while in the middle of sending an existing one, the send method called while the connection
 * is already busy may throw an [IllegalStateException].
 *
 * This method wraps the call to [RemoteEndpoint.Basic.sendText] in a synchronized block to avoid this exception.
 */
fun RemoteEndpoint.Basic.sendTextSafe(message: String) {
    synchronized(this) {
        sendText(message)
    }
}

@ServerEndpoint("/prueba")
@Component
class WSEndpoint(){
    val logger = LoggerFactory.getLogger(WSEndpoint::class.java)

    /**
     * Successful connection
     *
     * @param session
     */
    @OnOpen
    fun onOpen(session: Session) {
        logger.info ("Server Connected ... Session ${session.id}")
        with(session.basicRemote){
            sendTextSafe("Hola")
        }
    }

    /**
     * Connection closure
     *
     * @param session
     */
    @OnClose
    fun onClose(session: Session, closeReason: CloseReason) {
        logger.info("Session ${session.id} closed because of $closeReason")
    }

    /**
     * Message received
     *
     * @param message
     */
    @OnMessage
    fun onMsg(message: String, session: Session) {
        logger.info("Server Message ... Session ${session.id}")
        val currentLine = Scanner(message.lowercase(Locale.getDefault()))
        if (currentLine.findInLine("bye") == null) {
            logger.info("Server received \"${message}\"")
            runCatching {
                if (session.isOpen) {
                    with(session.basicRemote) {
                        sendTextSafe("---")
                    }
                }
            }.onFailure {
                logger.error("$it while sending message")
                session.close(CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "I'm sorry, I didn't understand that."))
            }
        } else {
            session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Alright then, goodbye!"))
        }
    }

    @OnError
    fun onError(session: Session, errorReason: Throwable) {
        logger.error("$errorReason: Session ${session.id} closed because of ${errorReason.javaClass.name}")
    }
}


