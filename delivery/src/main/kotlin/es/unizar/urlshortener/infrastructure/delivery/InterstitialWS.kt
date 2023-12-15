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
        session.sendMessage(TextMessage(url))
        session.close()
    }
}

class InterstitialWSHandler : TextWebSocketHandler() {
    @Autowired
    lateinit var redirectUseCase: RedirectUseCase
    @Autowired
    private lateinit var taskExecutor: TaskExecutor

    val logger = LoggerFactory.getLogger(InterstitialWSHandler::class.java)

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
