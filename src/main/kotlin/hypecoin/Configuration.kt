package hypecoin

/**
 * Created by dan.
 */

import hypecoin.controllers.ClientController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WSConfig : WebSocketConfigurer {

    @Bean
    fun clientHandler(): ClientController {
        return ClientController()
    }

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(clientHandler(), "/hype")
                .setAllowedOrigins("*") //todo remove before prod
                .withSockJS()
    }
}