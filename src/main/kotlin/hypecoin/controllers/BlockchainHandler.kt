package hypecoin.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hypecoin.*
import hypecoin.Queue
import hypecoin.Queue.Companion.QUEUE_LIMIT
import hypecoin.models.Block
import hypecoin.models.Message
import hypecoin.repository.BlockRepository
import hypecoin.repository.MessageRepository
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import javax.crypto.SecretKey


/**
 * Created by dan.
 */

interface BlockchainHandler {
    fun processMessage(json: String, publicKey: PublicKey): Boolean
    fun getNumberOfCompletedBlocks(): Int
    fun getBlock(index: Int): Block?
    fun getPreviousBlock(hash: String): Block?
    fun getHead(): Block?
    fun getNodePublicKey(): PublicKey
    fun getNodePrivateKey(): PrivateKey

    val nodeSecretKey: SecretKey
}

@Component
class BlockchainHandlerImpl : BlockchainHandler {

    private val logger = LogFactory.getLog(javaClass)

    @Autowired
    lateinit var messageRepo: MessageRepository

    @Autowired
    lateinit var blockRepo: BlockRepository

    val nodeIdentity = getKeyPair()
    override val nodeSecretKey = generateSecretKey()

    val queue = Queue()

    var chain: List<Block> = LinkedList()


    override fun processMessage(json: String, publicKey: PublicKey): Boolean {
        val message: Message? = validateMessageSchema(json)

        message?.let {
            val isValid = verifySignedString(it.data, it.signedDigest, publicKey)
            if (!isValid) {
                logger.debug("Signature or message invalid")
                return false
            }

            val savedMessage = messageRepo.save(it)
            queue.addToQueue(savedMessage)
            logger.info(String.format("Message #%s added to Queue", savedMessage.id))

            if (queue.getQueueSize() >= QUEUE_LIMIT) {
                processBlock()
            }

            return true
        } ?: run {
            return false
        }
    }

    override fun getNumberOfCompletedBlocks(): Int {
        return chain.size
    }

    override fun getBlock(index: Int): Block? {
        return if (chain.isEmpty() || index < 0) null else chain[index]
    }

    override fun getPreviousBlock(hash: String): Block? {
        chain.forEachIndexed { index, block ->
            if (block.hash.equals(hash, true)) {
                if ((index - 1 >= 0) && chain[index - 1].hash == block.previousHash) {
                    return chain[index - 1]
                }
            }
        }

        return null
    }

    override fun getHead(): Block? {
        return if (chain.isNotEmpty()) chain.last() else null
    }

    override fun getNodePublicKey(): PublicKey {
        return nodeIdentity.public
    }

    override fun getNodePrivateKey(): PrivateKey {
        return nodeIdentity.private
    }

    private fun validateMessageSchema(jsonString: String): Message? {
        return try {
            val message = ObjectMapper().readValue(jsonString, Message::class.java)

            if (message.data.isNotEmpty() && message.signedDigest.isNotEmpty()) {
                message
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug(String.format("Failed to decode message %s", e.localizedMessage))
            null
        }
    }

    private fun processBlock() {
        logger.info("Processing block")

        val blockOfMessages = queue.pluckBlockOfMessages()
        val messagesAsJson = jacksonObjectMapper().writeValueAsString(blockOfMessages)
        queue.removeBlockOfMessages(blockOfMessages, messageRepo)
        logger.info(String.format("Emptying queue"))


        val previousBlock = blockRepo.findFirstByOrderByIdDesc()//todo optimise as query
        val previousHash = if (previousBlock.isPresent) previousBlock.get().hash else "0"

        val block = Block(data = messagesAsJson, previousHash = previousHash)
        block.signedHash = signString(block.hash, nodeIdentity.private)

        val savedBlock = blockRepo.save(block)
        chain += savedBlock

        logger.info(String.format("Processed block #%s", chain.size))
    }
}

