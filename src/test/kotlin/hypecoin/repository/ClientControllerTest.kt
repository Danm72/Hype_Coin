package hypecoin.repository


import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import hypecoin.controllers.BlockchainHandler
import hypecoin.controllers.ClientController
import hypecoin.getKeyPair
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

/**
 * Created by dan.
 */

class ClientControllerTest {

    val session: WebSocketSession = mock()
    val message = TextMessage("{\n" +
            "  \"type\": \"message\",\n" +
            "  \"data\": {\n" +
            "    \"data\": \"Hello!\",\n" +
            "    \"timestamp\": \"Fri Jun 22 15:56:03 IST 2018\",\n" +
            "    \"signedDigest\": \"test\",\n" +
            "    \"id\": -1\n" +
            "  }\n" +
            "}")

    val block = TextMessage("{\n" +
            "  \"type\": \"block\",\n" +
            "  \"data\": {\n" +
            "  }\n" +
            "}")

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(mBlockchainHandler.processMessage(any(), (any()))).thenReturn(true)
        clientHandler.sessionList[session] = getKeyPair().public
    }

    @Mock
    lateinit var mBlockchainHandler: BlockchainHandler

    @InjectMocks
    var clientHandler = ClientController()

    @Test
    fun `test that MESSAGES calls correct Handler method`() {
        clientHandler.handleTextMessage(session, message)

        verify(mBlockchainHandler, times(1)).processMessage(any(), any())
    }

    @Test
    fun testBlocksCallsCorrectMessageHandler() {

        val message = TextMessage("{ \"type\": \"blocks\", \"data\": {}}")

        clientHandler.handleTextMessage(session, message)

        verify(mBlockchainHandler, times(1)).getNumberOfCompletedBlocks()
    }

    @Test
    fun testBlocksallsCorrectMessageHandler() {
        clientHandler.handleTextMessage(session, block)

        verify(mBlockchainHandler, times(1)).getBlock(any())
    }

}
