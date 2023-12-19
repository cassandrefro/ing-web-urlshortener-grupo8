package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler

const val MS_TO_SEND : Long = 5000

class SenderWS (private val session: WebSocketSession, private val url: String) : Runnable{
    val logger = LoggerFactory.getLogger(SenderWS::class.java)

    override fun run() {
        logger.info("recipient: ${session.id} message: $url")
        Thread.sleep(MS_TO_SEND)
        if (session.isOpen){
            session.sendMessage(TextMessage(url))
            session.close()
        }
    }
}

class InterstitialWSHandler : TextWebSocketHandler() {
    @Autowired
    lateinit var redirectUseCase: RedirectUseCase
    @Autowired
    private lateinit var taskExecutor: TaskExecutor

    val logger = LoggerFactory.getLogger(InterstitialWSHandler::class.java)

    /**
     * Handles incoming text messages in the WebSocket session.
     *
     * @param session The WebSocket session associated with the received message.
     * @param message The received [TextMessage] containing the payload.
     *
     * This function logs the received message payload using the [logger.info] method.
     * It then executes a [SenderWS] task on the [taskExecutor], passing the WebSocket [session]
     * and the target URL obtained by redirecting the payload using [redirectUseCase.redirectTo].
     * The redirection result is expected to contain a [Redirect] object, and the target URL is extracted
     * from its [value] property.
     */
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.info("WS-Received: ${message.payload}")
        taskExecutor.execute(SenderWS(session, redirectUseCase.redirectTo(message.payload).value.target))
    }
}

@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(interstitialWSHandler(), "/ws/")
    }

    @Bean
    fun interstitialWSHandler(): WebSocketHandler {
        return InterstitialWSHandler()
    }
}
