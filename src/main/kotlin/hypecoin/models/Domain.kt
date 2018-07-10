package hypecoin.models

import hypecoin.generateHash
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction.*
import java.util.*
import javax.persistence.*


@Entity
data class Message(
        var data: String,
        var timestamp: Long = Date().time,
        var signedDigest: String,
        @Id @GeneratedValue
        @NotFound(action = IGNORE)
        var id: Long = -1)

@Entity
data class Block(
        @Lob
        @Column(length = 100000)
        var data: String,
        val previousHash: String,
        val hash: String = generateHash(data + previousHash),
        var signedHash: String = "",
        @Id @GeneratedValue
        val id: Long = -1)

@Entity
data class ServerMessage(
        val type: String,
        val data: String,
        @Id @GeneratedValue
        val id: Long = -1)