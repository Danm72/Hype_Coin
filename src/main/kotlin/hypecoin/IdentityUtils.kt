package hypecoin

import java.nio.charset.StandardCharsets.UTF_8
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


/**
 * Created by dan.
 */

private const val AES = "AES"
private const val RSA = "RSA"
private const val SHA1WithRSA = "SHA1WithRSA"
private const val SHA_256 = "SHA-256"
private const val UTF8 = "UTF-8"
private const val RSA_KEY_SIZE = 1024
private const val AES_KEY_SIZE = 128

@Throws(NoSuchAlgorithmException::class)
fun getKeyPair(): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(RSA_KEY_SIZE, SecureRandom())
    return kpg.generateKeyPair()
}

fun generateSecretKey(): SecretKey {
    val generator = KeyGenerator.getInstance(AES)
    generator.init(AES_KEY_SIZE) // The AES key size in number of bits
    return generator.generateKey()
}

fun generateHash(string: String): String {
    val bytes = string.toByteArray()
    val md = MessageDigest.getInstance(SHA_256)
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

fun verifySignedString(original: String, signedString: String, publicKey: PublicKey): Boolean {
    val byteString = Base64.getDecoder().decode(signedString)
    val originalByteString = original.toByteArray(charset(UTF8))

    val signature = Signature.getInstance(SHA1WithRSA)
    signature.initVerify(publicKey)
    signature.update(originalByteString)

    return signature.verify(byteString)
}

fun decryptEncryptedString(signedString: String, key: Key): String {
    val deco = Base64.getDecoder().decode(signedString)

    val cipher: Cipher = if (key is PublicKey || key is PrivateKey) {
        Cipher.getInstance(RSA)
    } else {
        Cipher.getInstance(AES)
    }

    cipher.init(Cipher.DECRYPT_MODE, key)

    return String(cipher.doFinal(deco), UTF_8)
}

fun encryptString(stringToEncrypt: String, key: Key): String {
    val data = stringToEncrypt.toByteArray(charset(UTF8))

    val cipher: Cipher = if (key is PublicKey || key is PrivateKey) {
        Cipher.getInstance(RSA)
    } else {
        Cipher.getInstance(AES)
    }

    cipher.init(Cipher.ENCRYPT_MODE, key)

    val encrypted = cipher.doFinal(data)

    return String(Base64.getEncoder().encode(encrypted), charset(UTF8))
}

fun signString(stringToSign: String, privateKey: PrivateKey): String {
    val data = stringToSign.toByteArray(charset(UTF8))

    val sig = Signature.getInstance(SHA1WithRSA)
    sig.initSign(privateKey)
    sig.update(data)

    return String(Base64.getEncoder().encode(sig.sign()), charset(UTF8))
}

fun convertStringToPublicKey(key: String): PublicKey {
    val decoded = Base64.getDecoder().decode(key)
    val keySpec = X509EncodedKeySpec(decoded)

    return KeyFactory.getInstance(RSA).generatePublic(keySpec)
}

fun convertStringToSecretKey(key: String): SecretKey {
    val decoded = Base64.getDecoder().decode(key)

    return SecretKeySpec(decoded, 0, decoded.size, AES)
}

fun provideKeyAsString(key: Key): String {
    return String(Base64.getEncoder().encode(key.encoded))
}