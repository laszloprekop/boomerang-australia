package boomerang.players

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer

/** S4: selects any card of the hand at random and takes the first score offered. Ignores events. */
class BotPlayer(private val random: kotlin.random.Random) : IPlayer {
    /** The answer is ready at once: the site of a card drawn from the hand with the bot's random source. */
    override fun selectCard(request: CardRequest): IAnswer<Char> {
        val site = request.hand.random(random).site
        return IAnswer { site }
    }

    /** Index 0 is the first score offered. */
    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> = IAnswer { 0 }

    /** A bot decides without looking at what happened, so there is nothing to do. */
    override fun show(event: GameEvent) {
    }
}
