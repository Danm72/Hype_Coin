package hypecoin.models


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hibernate.cfg.Configuration
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase
import org.hibernate.testing.transaction.TransactionUtil.doInHibernate
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.IOException
import java.util.*

/**
 * Created by dan.
 * This class tests the hibernate hypecoin.models
 */
internal class HibernateModelTests : BaseCoreFunctionalTestCase() {

    private val properties: Properties
        @Throws(IOException::class)
        get() {
            val properties = Properties()
            properties.load(javaClass.classLoader.getResourceAsStream("hibernate.properties"))
            return properties
        }

    override fun getAnnotatedClasses(): Array<Class<*>> {
        return arrayOf(Message::class.java, Block::class.java)
    }

    override fun configure(configuration: Configuration) {
        super.configure(configuration)
        configuration.properties = properties
    }

    @Test
    fun givenMessagesVerifySaveToBlock() {
        doInHibernate(({ this.sessionFactory() })) { session ->
            val mess1 = Message(data = "1", signedDigest = "1")
            val mess2 = Message(data = "2", signedDigest = "2")
            val mess3 = Message(data = "3", signedDigest = "3")
            val mess4 = Message(data = "4", signedDigest = "4")
            val mess5 = Message(data = "5", signedDigest = "5")

            val messageList = listOf(mess1, mess2, mess3, mess4, mess5)

            messageList.forEach { message -> session.save(message) }

            val messageJson = jacksonObjectMapper().writeValueAsString(messageList)

            val blockToSave = Block(
                    signedHash = "sig",
                    data = messageJson, previousHash = "0")

            session.save(blockToSave)

            val blockFound = session.find(Block::class.java, blockToSave.id)

            assertTrue(blockToSave == blockFound)
        }
    }

}