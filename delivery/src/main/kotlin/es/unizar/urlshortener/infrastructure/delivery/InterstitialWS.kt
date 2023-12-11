package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler


class MyHandler() : TextWebSocketHandler() {
    @Autowired
    lateinit var redirectUseCase: RedirectUseCase

    val logger = LoggerFactory.getLogger(MyHandler::class.java)

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.info("WS-Received: ${message.payload}")
        session.sendMessage(TextMessage(redirectUseCase.redirectTo(message.payload).value.target))
        session.close()
    }
}

@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(myHandler(), "/ws/")
    }

    @Bean
    fun myHandler(): WebSocketHandler {
        return MyHandler()
    }
}
