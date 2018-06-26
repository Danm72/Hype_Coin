package hypecoin

import hypecoin.TestUtils.Companion.generateMessageAsString
import junit.framework.Assert.assertEquals
import org.junit.Test

/**
 * Created by dan.
 */
internal class IdentityUtilsTest {


    @Test
    fun verifySignedStringTest() {
        val keypair = getKeyPair()

        val plainText = "TestString"
        val signedString = signString(stringToSign = plainText, privateKey = keypair.private)

        val signedCorrectly = verifySignedString(original = plainText, signedString = signedString, publicKey = keypair.public)

        assert(signedCorrectly)
    }

    @Test
    fun `Verify encrypted string is decrypted correctly`() {
        val keypair = getKeyPair()

        val plainText = "TestString"
        val encryptedString = encryptString(stringToEncrypt = plainText, key = keypair.private)

        val decrypted = decryptEncryptedString(encryptedString, key = keypair.public)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `Test secret key encrypted and decryption`() {
        val clientKeyPair = getKeyPair()
        val secretKey = generateSecretKey()

        val generatedMessage = generateMessageAsString("Hello", clientKeyPair.private)
        val encryptedMessage = encryptString(generatedMessage, secretKey)

        val encryptedSecret = encryptString(provideKeyAsString(secretKey), clientKeyPair.public)
        val decryptedSecret = decryptEncryptedString(encryptedSecret, clientKeyPair.private)
        val secret = convertStringToSecretKey(decryptedSecret)

        val decryptedMessage = decryptEncryptedString(encryptedMessage, key = secret)

        assertEquals(secretKey, secret)
        assertEquals(generatedMessage, decryptedMessage)
    }

    @Test
    fun stringToPublicKeyTest() {
    }

    @Test
    fun stringToPrivateKeyTest() {
    }

    @Test
    fun generateHashTest() {
    }

    @Test
    fun signStringTest() {
    }
}