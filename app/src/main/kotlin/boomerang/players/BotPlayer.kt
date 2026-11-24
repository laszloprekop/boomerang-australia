package boomerang.players

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer

/** S4: selects any card of the hand at random and takes the first score offered. Ignores events. */
class BotPlayer(private val random: kotlin.random.Random) : IPlayer {
    override fun selectCard(request: CardRequest): IAnswer<Char> = TODO("not implemented")
    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> = TODO("not implemented")
    override fun show(event: GameEvent): Unit = TODO("not implemented")
}
