package hypecoin

import hypecoin.client.Client
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@SpringBootApplication
class NodeApplication

private const val RUN_TIME = 60

fun main(args: Array<String>) {
    runApplication<NodeApplication>(*args)

    var scheduledFutures: List<ScheduledFuture<*>> = arrayListOf()
    val clients = arrayOf(Client(), Client(), Client(), Client(), Client())

    val startTime = Date().time

    clients.forEachIndexed { index, client ->
        val session = client.openConnectionWithNode()
        session?.let {
            scheduledFutures += ConcurrentTaskScheduler().scheduleAtFixedRate({
                val secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(Date().time) - TimeUnit.MILLISECONDS.toSeconds(startTime)

                if (secondsElapsed <= RUN_TIME) {
                    client.sendMessage(session, "hello")
                } else {
                    print("\n SHUTTING DOWN CLIENT $index \n")
                    scheduledFutures[index].cancel(true)
                }
            }, 1000)
        }
    }
}