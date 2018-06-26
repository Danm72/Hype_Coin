/**
 * Created by dan.
 */
package hypecoin.client

import org.springframework.web.socket.WebSocketSession

interface ClientInterface {
    fun registerWithNode(session: WebSocketSession)
    fun openConnectionWithNode(): WebSocketSession?
    fun sendMessage(session: WebSocketSession, message: String)
}