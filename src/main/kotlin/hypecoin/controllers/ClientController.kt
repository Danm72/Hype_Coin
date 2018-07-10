package hypecoin.controllers;

/**
 * Created by dan
 */

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hypecoin.convertStringToPublicKey
import hypecoin.decryptEncryptedString
import hypecoin.encryptString
import hypecoin.models.ServerMessage
import hypecoin.provideKeyAsString
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.security.PublicKey
import java.util.*

private const val REGISTER = "register"
private const val MESSAGE = "message"
private const val BLOCKS = "blocks"
private const val BLOCK = "block"
private const val HEAD = "head"
private const val PREVIOUS = "previous"
private const val HANDSHAKE_1 = "handshake_step_1"
private const val HANDSHAKE_2 = "handshake_step_2"

class ClientController : TextWebSocketHandler() {
    private val logger = LogFactory.getLog(javaClass)

    val sessionList = HashMap<WebSocketSession, PublicKey>()

    @Autowired
    lateinit var mBlockchainHandler: BlockchainHandler

    @Throws(Exception::class)
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessionList -= session
    }

    public override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val json = try {
            jacksonObjectMapper().readTree(message.payload)
        } catch (e: Exception) {
            val decryptedMessage = decryptEncryptedString(message.payload, mBlockchainHandler.nodeSecretKey)
            jacksonObjectMapper().readTree(decryptedMessage)
        }

        logger.debug(String.format("Message received: %s", json))

        val jsonData = json.get("data").asText()
        when (json.get("type").asText()) {
            REGISTER -> {
                sessionList[session] = convertStringToPublicKey(jsonData)

                provideNodePublicKeyToClient(session)
                provideSharedSecretKeyToClient(session)

                logger.debug(String.format("New client registered #%s", sessionList.size))
            }
            MESSAGE -> {
                try {
                    val processSuccessful = mBlockchainHandler.processMessage(json = jsonData, publicKey = getClientKey(session))
                    if (processSuccessful) {
                        logger.debug("Processed message")
                    } else {
                        logger.debug("Failed to processed message")
                    }
                } catch (e: Exception) {
                    logger.debug(String.format("Exception processing message %s", e.message))
                }
            }
            BLOCKS -> {
                val numberOfCompletedBlocks = mBlockchainHandler.getNumberOfCompletedBlocks()
                emit(session, String.format("%s", numberOfCompletedBlocks))

                logger.debug(String.format("Blocks completed: %s", numberOfCompletedBlocks))
            }
            BLOCK -> {
                val index = json.get("data").asInt()
                val block = mBlockchainHandler.getBlock(index = index)
                val blockAsJson = jacksonObjectMapper().writeValueAsString(block)

                emit(session, String.format("%s", blockAsJson))
                logger.debug(String.format("Block requested: %s", blockAsJson))
            }
            HEAD -> {
                val block = mBlockchainHandler.getHead()
                block?.let {
                    val blockAsJson = jacksonObjectMapper().writeValueAsString(block)

                    emit(session, String.format("%s", blockAsJson))
                }
            }
            PREVIOUS -> {
                val blockHash = json.get("data").asText()
                val block = mBlockchainHandler.getPreviousBlock(hash = blockHash)
                val blockAsJson = jacksonObjectMapper().writeValueAsString(block)

                emit(session, String.format("%s", blockAsJson))
                logger.debug(String.format("Block requested: %s", blockAsJson))
            }
        }
    }

    private fun provideNodePublicKeyToClient(session: WebSocketSession) {
        val nodePublicKey = provideKeyAsString(key = mBlockchainHandler.getNodePublicKey())
        val registrationSuccessful = ServerMessage(HANDSHAKE_1, nodePublicKey)
        emit(session, jacksonObjectMapper().writeValueAsString(registrationSuccessful))
    }

    private fun provideSharedSecretKeyToClient(session: WebSocketSession) {
        val nodeSecretKey = provideKeyAsString(key = mBlockchainHandler.nodeSecretKey)
        val encryptedNodeSecret = encryptString(nodeSecretKey, getClientKey(session))
        val registrationSuccessfulSecret = ServerMessage(HANDSHAKE_2, encryptedNodeSecret)
        emit(session, jacksonObjectMapper().writeValueAsString(registrationSuccessfulSecret))
    }

    private fun getClientKey(session: WebSocketSession) = sessionList[session]!!

    fun emit(session: WebSocketSession, msg: String) = session.sendMessage(TextMessage(msg))
}