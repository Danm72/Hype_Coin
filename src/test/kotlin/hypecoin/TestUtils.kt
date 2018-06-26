package hypecoin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hypecoin.models.Message
import java.security.PrivateKey

/**
 * Created by dan on 26/06/2018.
 */

class TestUtils {
    companion object {
        fun generateMessageAsString(content: String, privateKey: PrivateKey): String {
            val signedDigest = signString(content, privateKey)

            val message = Message(data = content, signedDigest = signedDigest)

            return jacksonObjectMapper().writeValueAsString(message)
        }
    }
}