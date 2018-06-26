package hypecoin.repository

import hypecoin.models.Block
import hypecoin.models.Message
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface MessageRepository : CrudRepository<Message, Long>

@Repository
interface BlockRepository : CrudRepository<Block, Long> {
    fun findFirstByOrderByIdDesc(): Optional<Block>

}