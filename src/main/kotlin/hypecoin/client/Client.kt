package hypecoin.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hypecoin.*
import hypecoin.models.Message
import hypecoin.models.ServerMessage
import org.apache.commons.logging.LogFactory
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.security.PublicKey
import java.util.*
import javax.crypto.SecretKey
import javax.persistence.GeneratedValue

private const val HYPE_URL = "ws://localhost:8081/hype/websocket"
private const val HANDSHAKE_1 = "handshake_step_1"
private const val HANDSHAKE_2 = "handshake_step_2"

/**
 * Created by dan.
 */

data class Client(@GeneratedValue
                  val id: Long = -1) : ClientInterface {
    private val logger = LogFactory.getLog(javaClass)

    private val keypair = getKeyPair()
    private var nodePublicKey: PublicKey? = null
    private var nodeSecretKey: SecretKey? = null

    override fun openConnectionWithNode(): WebSocketSession? {
        val client = StandardWebSocketClient()
        return client.doHandshake(object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                val json = jacksonObjectMapper().readTree(message.payload)
                logger.debug(String.format("Message received: %s", json))

                val jsonData = json.get("data").asText()

                when (json.get("type").asText()) {
                    HANDSHAKE_1 -> {
                        nodePublicKey = convertStringToPublicKey(jsonData)
                    }

                    HANDSHAKE_2 -> {
                        val decryptedSecret = decryptEncryptedString(jsonData, keypair.private)
                        nodeSecretKey = convertStringToSecretKey(decryptedSecret)
                    }
                }
            }

            override fun afterConnectionEstablished(session: WebSocketSession) {
                super.afterConnectionEstablished(session)

                registerWithNode(session)
            }

            override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
                super.handleTransportError(session, exception)

                logger.error(exception)
            }

            override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
                super.afterConnectionClosed(session, status)

                logger.info("Closed" + status.reason)
            }

        }, HYPE_URL).get()
    }

    override fun registerWithNode(session: WebSocketSession) {
        logger.info(String.format("Registering with node"))

        session.sendMessage(TextMessage((generateRegistrationMessage())))
    }

    override fun sendMessage(session: WebSocketSession, message: String) {
        logger.info(String.format("Sending Message %s", message))

        nodePublicKey?.let {
            val encryptedMessage = encryptString(generateMessage(message), nodeSecretKey!!)
            session.sendMessage(TextMessage(encryptedMessage))
        }

    }

    private fun generateRegistrationMessage(): String {
        val wrappedRegister = ServerMessage(type = "register", data = provideKeyAsString(key = keypair.public))

        return jacksonObjectMapper().writeValueAsString(wrappedRegister)
    }

    private fun generateMessage(content: String): String {
        val signedDigest = signString(content, keypair.private)

        val message = Message(data = content, timestamp = Date().time, signedDigest = signedDigest)
        val messageJson = jacksonObjectMapper().writeValueAsString(message)
        val wrappedMessage = ServerMessage(type = "message", data = messageJson)

        return jacksonObjectMapper().writeValueAsString(wrappedMessage)
    }

}
