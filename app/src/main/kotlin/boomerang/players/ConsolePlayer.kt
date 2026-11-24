package boomerang.players

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer

/** A human at a text console. Prints every event and question; repeats a question locally until a line can
 *  be read as an answer (one character; a number). Throws PlayerLost when the input ends. */
class ConsolePlayer(private val readLine: () -> String?, private val print: (String) -> Unit) : IPlayer {
    override fun selectCard(request: CardRequest): IAnswer<Char> = TODO("not implemented")
    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> = TODO("not implemented")
    override fun show(event: GameEvent): Unit = TODO("not implemented")
}

/** The only place that lays out text for a player. */
internal fun describe(event: GameEvent): String = TODO("not implemented")
internal fun describe(request: CardRequest): String = TODO("not implemented")
internal fun describe(request: ChoiceRequest): String = TODO("not implemented")
