# Console screens

Status: specification, approved in scope by the student on 25 Nov 2026. Beyond the brief: no rule of the
game changes. The aim is a console a person can play from: one screen per question, 80 columns wide, with
a map of the sites, a score sheet and the reason for every score.

This document is the input for the tests and the implementation. The target screens in section 4 show the
layout that is wanted. Wording and exact spacing are free (S7 still holds); what must be on a screen is
fixed by the criteria in section 3.

## 1. Decisions

| # | Decision | Reason |
|---|---|---|
| D1 | `ConsolePlayer` remembers what it was shown (seat, names, round, visited sites, cards shown, its score lines per round). | A screen with a score sheet needs more than the one event that triggers it. The memory is display state of one console, not game state: the game never reads it. |
| D2 | The console learns the cards of the edition through its constructor, `cards: List<Card> = emptyList()`. `App.kt` passes `australia().cards` on the server and on the client. | The map lists sites nobody has drafted yet. No event carries the deck, and it is the same on both sides, so it does not need to travel. With no cards given the map is left out. |
| D3 | Colour is a constructor flag, `colour: Boolean = false`. `App.kt` sets it: on when `System.console()` is not null and the environment variable `NO_COLOR` is not set. | Escape codes are garbage in a file or a pipe. The tests run without colour, so they read plain text. |
| D4 | `GameStarted` gets `names: List<String>`, by seat, default "Player 1", "Player 2" and so on. `Game` takes the names as a constructor argument with that default. `App.kt` names humans "Player n" and bots "Bot n", n being seat + 1. | The console can then say "Bot 3". The game stays ignorant of what a bot is: it passes names on. |
| D5 | `RoundStarted` gets `passing: Passing`, default the standard passing. `Passing` becomes `Serializable`. | Review finding L7: with `alternate` on, a person could not see who passes to whom. |
| D6 | `Score` gets `explanation: String? = null`. Each of the six scoring classes fills it with one short line of facts (which cards, which numbers). | Only the rule knows why the points are what they are. Known cost: a sentence is now written inside a scoring class, as `announcement` already is. The console decides where and whether to print it. |
| D7 | Taking no activity stays a numbered option. There is no "press Enter to skip" and no pause between rounds. | Both would be new kinds of question in `IPlayer`. The numbered option already exists and is tested. |
| D8 | The console knows no edition. Regions, their order and the symbol kinds (the column heads) are read from the cards. A region code is made from the name: the initials of a name of several words (WA), else its first three letters in capitals (QUE). Region colours are given in order of first appearance from a fixed list of seven. | A second edition must not need a second console. |
| D9 | The top-level `describe` functions stay, with their signatures. A `ConsolePlayer` that has been shown nothing else prints for an event what `describe(event)` returns. | The locked tests call them, and compare. |

## 2. Out of scope

- Clearing the terminal or moving the cursor. Screens are printed one after the other.
- Any change to a rule, to `IPlayer`, to the bot, or to what travels when (only the three new fields).
- Review finding L5.

## 3. Criteria

Colour off unless said otherwise. "Screen" is everything one call of `selectCard`, `chooseScore` or `show`
prints.

**CS1 Width.** No line of any screen is longer than 80 characters, for any card of the edition and for
four players.

**CS2 Header.** After `GameStarted` and `RoundStarted`, a card question starts with a header that has the
edition, the round, the number of the pick (cards in the draft plus one) and the player's own name.

**CS3 Map.** With the edition's cards given, a card question shows every region once and every site of the
edition once. A site visited in an earlier round, a site in the draft of this round and a site not visited
are marked in three different ways. Each region shows how many of its sites the player has (visited or in
this draft) out of how many. A region whose sites the player all has is marked as complete. Without cards
given there is no map and nothing fails.

**CS4 Card table.** The hand and the draft are tables with one card per line: site character, name,
number, region code and one column per symbol kind of the edition, a dash where a card has no symbol of
that kind. All information the locked tests look for (full region name included) is still somewhere on the
screen.

**CS5 Throw card.** With at least one card in the draft, the card question says which card is the Throw
card.

**CS6 The others.** After `CardsShown`, the next card question shows for every other player the name and
the shown cards, and does not show a Throw card of another player. The player's own row is not repeated
there.

**CS7 Passing.** A card question from the second pick on names the player the hand came from, by the
`passing` of the round: with hands `TO_NEXT` seat 0 of three receives from seat 2, with `TO_PREVIOUS` from
seat 1.

**CS8 Score sheet.** After at least one `RoundScored`, the card question and the final screen show a
sheet: one row per category in the order received, one column per round played, the row total. The final
screen also has a row of round totals and the grand total.

**CS9 Refusal.** A card question with `refused` set says first which answer was refused and that nothing
was drafted. A choice question with `refused` set says which number was not offered.

**CS10 Choice.** A choice question lists the options numbered from 1, each with what it uses up (or that
it uses nothing) and its points, and the explanation of the option when it has one.

**CS11 Round scored.** `RoundScored` prints one line per category with title, explanation when there is
one, and points; then the total of the round; then what is announced of the other players, by name.

**CS12 Drafts revealed.** `DraftsRevealed` prints for every player the name and all seven cards, and marks
the Throw card and the Catch card.

**CS13 Result.** `GameOver` prints the players ranked by total, highest first, each with name and total,
the receiver marked as "you", and every winner marked. One winner and a shared win read differently.

**CS14 Names.** Wherever a player is named, the name from `GameStarted.names` is used. A console that never
saw `GameStarted` says "Player n".

**CS15 Colour.** With `colour = true`, a screen contains escape sequences (the character with code 27).
With `colour = false` it contains none. With the escape sequences removed, the two are the same text.

**CS16 Game: names.** `Game` sends every player a `GameStarted` with the names it was given; without
names, "Player 1" to "Player n". Names of another length than the players are refused with
`IllegalArgumentException`.

**CS17 Game: passing.** `RoundStarted` carries what the passing rule answered for that round. With
`AlternatingPassingRule` and three players, rounds 2 and 4 carry the mirrored passing.

**CS18 Over the wire.** `GameStarted` with names and `RoundStarted` with passing survive serialization.
A whole game still passes `GameOverTheWireTest`.

**CS19 Explanations.** Each of the six scoring classes gives an explanation with every score it returns,
and the explanation contains the facts the points come from:

| Class | The explanation contains |
|---|---|
| `ThrowCatchDifference` | the site and number of the Throw card and of the Catch card |
| `NewSites` | the sites that are new, or that none is |
| `RegionBonus` | nothing more than the announcement; `null` when the bonus is 0 |
| `SymbolSum` | each symbol counted with its value, the sum, and whether it was doubled |
| `SymbolPairs` | each symbol with its number of pairs and the points; a symbol held once is named as no pair |
| `ChosenSymbolCount` | the symbol and how many cards carry it; for the option that takes nothing, that nothing is used |

**CS20 Start-up.** `App.kt` names bots "Bot n" and humans "Player n". Colour is on only with a console and
without `NO_COLOR`. The decision is a function that can be tested without a terminal:
`useColour(hasConsole: Boolean, noColor: String?): Boolean`.

## 4. Target screens

Example numbers; they follow the rules but come from no real game.

### 4.1 A card question

```
BOOMERANG AUSTRALIA               Round 2, pick 3 of 7          You are Player 1
================================================================================
MAP   [A] visited earlier   (D) drafted this round    E  not visited
   Western Australia   [A] [B] [C] (D) 4/4  complete
   Northern Territory   E  [F]  G   H  1/4
   Queensland           I   J  [K]  L  1/4
   South Australia     (M)  N   O   P  1/4
   New South Wales     [Q]  R   S   T  1/4
   Victoria            [U]  V   W   X  1/4
   Tasmania             Y   Z   *   -  0/4

YOUR DRAFT THIS ROUND   2 of 7 cards
      Site                     No Reg  Collections Animals    Activities
  [D] Kalbarri National Park    1 WA   Wildflowers -          Bushwalking
  [M] Barossa Valley            3 SA   -           Koalas     Bushwalking
  The first card, D, is your Throw card. The others see it when the round ends.

THE OTHERS THIS ROUND
  Player 2   Throw: hidden   [S] Bondi Beach
  Bot 3      Throw: hidden   [T] Hunter Valley

SCORE SHEET
                         R1   Total
  Throw and catch         3       3
  Tourist sites           7       7
  Region bonus            0       0
  Collections             9       9
  Animals                 8       8
  Activities              2       2
--------------------------------------------------------------------------------
YOUR HAND   5 cards, passed to you by Bot 3
      Site                     No Reg  Collections Animals    Activities
  [E] Uluru                     4 NT   -           Emus       Indigenous Culture
  [J] The Whitsundays           6 QUE  -           Kangaroos  Indigenous Culture
  [N] Lake Eyre                 3 SA   -           Emus       Swimming
  [O] Kangaroo Island           3 SA   -           Kangaroos  Bushwalking
  [P] Mount Gambier             3 SA   Wildflowers -          Sightseeing
  Regions: WA Western Australia, NT Northern Territory, QUE Queensland, ...

Type the letter of the card to keep (E J N O P):
```

### 4.2 After a refused answer

```
Q is not in your hand. Nothing was drafted and no cards were passed.
```

followed by the card question again.

### 4.3 The end of a round

`DraftsRevealed`:

```
THE DRAFTS OF ROUND 2   Throw first, Catch last
  Player 1 (you)
  [D] Kalbarri National Park    1 WA   Wildflowers -          Bushwalking   Throw
  ...
  [P] Mount Gambier             3 SA   Wildflowers -          Sightseeing   Catch
  Player 2
  ...
```

The choice question:

```
ACTIVITIES   choose one
  1  Swimming             2 cards    2 points
  2  Indigenous Culture   1 card     0 points
  3  nothing                         0 points
Type the number of your choice:
```

`RoundScored`:

```
ROUND 2 SCORE
  Throw and catch   Throw D is 1, Catch P is 3                             2
  Tourist sites     7 new sites: D H J M N O P                             7
  Region bonus      Western Australia, South Australia                     6
  Collections       Wildflowers 2 + 2 = 4, doubled                         8
  Animals           Koalas 1 pair 7, Kangaroos 1 pair 3, Emus no pair     10
  Activities        Swimming, 2 cards                                      2
                                                                      ------
  Round 2 total                                                           35
  Player 2: Region bonus 3 (Tasmania)
```

### 4.4 The end of the game

```
BOOMERANG AUSTRALIA                 Final score                 You are Player 1
================================================================================
YOUR SCORE SHEET
                         R1   R2   R3   R4   Total
  Throw and catch         3    2    4    1      10
  ...
  Round total            29   35   24   33     121

RESULT
  1  Player 1 (you)   121   winner
  2  Bot 3            114
  3  Player 2          98
```
