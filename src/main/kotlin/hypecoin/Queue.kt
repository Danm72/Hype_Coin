package hypecoin

import hypecoin.models.Message
import java.util.ArrayList
import hypecoin.repository.*
/**
 * Created by dan on 26/06/2018.
 */


data class Queue(val queueLimit: Int = QUEUE_LIMIT) {
    companion object {
        const val QUEUE_LIMIT = 100
    }

    /*
    * This is a simple queue abstraction, should be swapped out for Kafka/Redis etc in prod.
    */

    var queue = ArrayList<Message>()

    fun getQueueSize() = queue.size

    fun addToQueue(messageObject: Message) {
        queue.add(messageObject)
    }

    fun pluckBlockOfMessages(): List<Message> {
        return queue.take(queueLimit)
    }

    fun removeBlockOfMessages(messages: List<Message>, messageRepo: MessageRepository) {
        messages.forEach {
            messageRepo.deleteById(it.id)
            queue.remove(it)
        }
    }
}