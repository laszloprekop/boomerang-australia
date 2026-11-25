package boomerang.support

import boomerang.australia.AUSTRALIA_LOGO
import boomerang.australia.AUSTRALIA_MAP
import boomerang.australia.australia
import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.Game
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer
import boomerang.game.RandomShuffler
import boomerang.game.RoundStarted
import boomerang.players.BotPlayer
import boomerang.players.ConsolePlayer
import boomerang.players.TerminalFrame
import boomerang.players.TerminalLook
import kotlin.random.Random

/** Plays the first round of a game on this terminal by itself, for a recording (docs/demo.gif, see
 *  docs/demo.md): a console with the whole terminal look against two bots, in one process and without the
 *  network. The answers are "typed" after a pause. Arguments: the height of the terminal in lines, and
 *  the pause before an answer in milliseconds. Not a test; nothing calls it but a person. */
fun main(args: Array<String>) {
    val rows = args.getOrNull(0)?.toIntOrNull() ?: 50
    val pause = args.getOrNull(1)?.toLongOrNull() ?: 1500
    val typist = Typist(pause)
    val console = ConsolePlayer(
        readLine = typist::type, print = ::println, cards = australia().cards, colour = true,
        terminal = TerminalLook(TerminalFrame(rows, colour = true), cardFan = true, map = AUSTRALIA_MAP, logo = AUSTRALIA_LOGO),
    )
    val players = listOf(typist.watching(console), BotPlayer(Random(7)), BotPlayer(Random(8)))
    try {
        Game(australia(), players, RandomShuffler(Random(42))).play()
    } catch (_: DemoOver) {
        Thread.sleep(pause * 2)
    }
}

private class DemoOver : RuntimeException()

/** Knows a good answer to the question the console was last asked, and gives it as a typed line. */
private class Typist(private val pause: Long) {
    private var answer = ""

    fun type(): String {
        Thread.sleep(pause)
        println(answer)
        Thread.sleep(pause / 3)
        return answer
    }

    /** The console as the game sees it: every question is looked at before it is passed on, and the demo
     *  ends where the second round would start. */
    fun watching(console: IPlayer): IPlayer = object : IPlayer {
        override fun selectCard(request: CardRequest): IAnswer<Char> {
            answer = request.hand.first().site.toString()
            return console.selectCard(request)
        }

        override fun chooseScore(request: ChoiceRequest): IAnswer<Int> {
            answer = (request.scores.indices.maxBy { request.scores[it].points } + 1).toString()
            return console.chooseScore(request)
        }

        override fun show(event: GameEvent) {
            if (event is RoundStarted && event.round == 2) throw DemoOver()
            console.show(event)
            Thread.sleep(pause)
        }
    }
}
