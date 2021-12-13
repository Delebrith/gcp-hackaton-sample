package hello

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import kotlin.math.abs

@SpringBootApplication
class KotlinApplication {

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
                    .filter { player -> abs(player.first.x - me.x) <= 3 || abs(player.first.y - me.y) <= 3 }


                val canBeHit = couldBeHit
                    .filter { player -> player.second.canBeHit(me) }


//                ServerResponse.ok().body(Mono.just(listOf("F", "R", "L", "T").random()))
                if (canBeHit.isNotEmpty())
                    return@flatMap ServerResponse.ok().body(Mono.just("T"))
                //else if (couldBeHit.isNotEmpty()) {
                //    return@flatMap ServerResponse.ok().body(Mono.just("R"))
                // } 
                else
                    return@flatMap ServerResponse.ok().body(Mono.just(listOf("F", "F", "F", "F", "R", "L").random()))

            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinApplication>(*args)
}

data class ArenaUpdate(val _links: Links, val arena: Arena)
data class PlayerState(val x: Int, val y: Int, val direction: String, val score: Int, val wasHit: Boolean) {
    fun canBeHit(me: PlayerState): Boolean {
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
            "N" -> x == me.x && y >= me.y
            "S" -> x == me.x && y <= me.y
            "E" -> x <= me.x && y == me.y
            "W" -> x >= me.x && y == me.y
            else -> false
        }
    }
}
data class Links(val self: Self)
data class Self(val href: String)
data class Arena(val dims: List<Int>, val state: Map<String, PlayerState>)

data class Coords(val x: Int, val y: Int)
