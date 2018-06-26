package hypecoin.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import hypecoin.Queue.Companion.QUEUE_LIMIT
import hypecoin.TestUtils.Companion.generateMessageAsString
import hypecoin.controllers.BlockchainHandlerImpl
import hypecoin.getKeyPair
import hypecoin.models.Block
import hypecoin.models.Message
import hypecoin.signString
import junit.framework.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations


/**
 * Created by dan.
 */
class BlockchainHandlerTest {

    val keypair = getKeyPair()
    val message = generateMessageAsString("Hello", privateKey = keypair.private)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val signedDigest = signString("Hello", keypair.private)

        val message = Message(data = "Hello", signedDigest = signedDigest)

        `when`(messageRepo.save<Message>(any())).thenReturn(message)

    }

    @Mock
    lateinit var messageRepo: MessageRepository

    @Mock
    lateinit var blockRepo: BlockRepository

    @InjectMocks
    var messageHandler = BlockchainHandlerImpl()


    @Test
    fun `when a message is correctly structured and signed then it should be processed`() {
        val processedSuccessfully = messageHandler.processMessage(message, keypair.public)

        assert(processedSuccessfully)
        assertEquals(messageHandler.queue.getQueueSize(), 1)
    }

    @Test
    fun `when a message is NOT correctly signed then it should NOT be processed`() {
        val invalidPrivKeyMessage = jacksonObjectMapper().readTree(generateMessageAsString("Hello", privateKey = getKeyPair().private))

        val processedSuccessfully = messageHandler.processMessage(invalidPrivKeyMessage.asText(), keypair.public)

        assertFalse(processedSuccessfully)
        assertEquals(messageHandler.queue.getQueueSize(), 0)
    }

    @Test
    fun `when the queue is full process messages and write to block`() {
        repeat(QUEUE_LIMIT) {
            messageHandler.processMessage(message, keypair.public)
        }

        assertEquals(messageHandler.queue.getQueueSize(), 0)
        verify(blockRepo).save<Block>(any())
    }


}