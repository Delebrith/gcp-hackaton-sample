package hello

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.util.Collections
import kotlin.math.abs

@SpringBootApplication
class KotlinApplication {

    val moves = Collections.synchronizedList(mutableListOf<String>()) as MutableList

    @Bean
    fun routes() = router {
        GET {
            ServerResponse.ok().body(Mono.just("Let the battle begin!"))
        }

        POST("/**", accept(APPLICATION_JSON)) { request ->
            request.bodyToMono(ArenaUpdate::class.java).flatMap { arenaUpdate ->
                println(arenaUpdate)

                val me = arenaUpdate.arena.state[arenaUpdate._links.self.href]!!

                val players = arenaUpdate.arena
                    .state.entries
                    .map { state -> Coords(state.value.x, state.value.y) to state.value }

                val couldBeHit =  players
                    .filter { player -> player.first.x == me.x || player.first.y == me.y }
                    .filter { player -> abs(player.first.x - me.x) < 3 || abs(player.first.y - me.y) < 3 }


                val canBeHit = couldBeHit.filter { it.second.canBeHitBy(me) }

                val canHitMe = couldBeHit.filter { it.second.faces(me) }


//                ServerResponse.ok().body(Mono.just(listOf("F", "R", "L", "T").random()))
                val result = if (canBeHit.isNotEmpty()) {
                    if (me.wasHit && moves.take(2).all { it.equals("T") })
                        listOf("R", "F", "F", "F").random()
                    else
                        "T"
                } else if (couldBeHit.isNotEmpty()) {
                    if (me.wasHit && canHitMe.isNotEmpty())
                        listOf("R", "F", "F", "F").random()
                    else
                        listOf("R", "R", "R", "R", "R", "R", "R", "R", "R", "F").random()
                }
                else {
                    listOf("R", "R", "R", "R", "F").random()
                }

                moves.add(0, result)
                if (moves.size >= 5) moves.removeLast()
                println("Past moves: $moves")
                ServerResponse.ok().body(Mono.just(result))
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinApplication>(*args)
}

data class ArenaUpdate(val _links: Links, val arena: Arena)
data class PlayerState(val x: Int, val y: Int, val direction: String, val score: Int, val wasHit: Boolean) {
    fun canBeHitBy(me: PlayerState): Boolean {
        return when (me.direction) {
            "N" -> x == me.x && y < me.y
            "S" -> x == me.x && y > me.y
            "E" -> x > me.x && y == me.y
            "W" -> x < me.x && y == me.y
            else -> false
        }
    }

    fun faces(me: PlayerState): Boolean {
        return when (direction) {
            "N" -> x == me.x && y > me.y
            "S" -> x == me.x && y < me.y
            "E" -> x < me.x && y == me.y
            "W" -> x > me.x && y == me.y
            else -> false
        }
    }
}
data class Links(val self: Self)
data class Self(val href: String)
data class Arena(val dims: List<Int>, val state: Map<String, PlayerState>)

data class Coords(val x: Int, val y: Int)
