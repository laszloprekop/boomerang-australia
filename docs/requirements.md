# Requirements: Boomerang Australia

Status: approved on 24 November 2026, with the decisions of section 8.
Section 7 was verified by tests on 24 November 2026, and all questions of section 8 are decided.

Sources, in order of authority:

1. The exam brief, `40516-HomeExam2025.txt` (cited as "brief", with line numbers of the text file).
2. The Boomerang Australia rulebook, `38060-boomerang-australia-rulebook.txt` (cited as "rulebook").
3. The original program, `app/src/main/java/BoomerangAustralia.java` (cited as "original"), for
   behaviour the brief does not describe.

Where the brief and the rulebook disagree, the brief wins unless a decision in section 8 says otherwise.
A tag such as `[Q4]` marks a statement that rests on a provisional answer to an open question in section 8.

## 1. Terms

| Term | Meaning |
|---|---|
| Seat order | The fixed order of the players for the whole game: player 1 to player n. |
| Next player | The following player in seat order; the next player of the last player is the first. |
| Previous player | The preceding player in seat order; the previous player of the first player is the last. |
| Hand | The cards a player currently holds and can select from. |
| Draft | The cards a player has kept in the current round: seven by the end of the round. |
| Throw card | The first card a player keeps in a round. Hidden from the other players during the round. |
| Shown cards | The cards of a draft that all players can see: everything except the Throw card. |
| Catch card | The seventh card of a draft: the last card of a neighbour's hand, received without a choice. |
| Site | The tourist site on a card, identified by one character: A to Z, `*` or `-`. |
| Region | A group of four sites. |
| Round | One deal, one draft and one scoring. |
| Game | Four rounds. |

## 2. Card data

The deck is exactly these 28 cards (brief lines 86 to 114). The region is spelled "New South Wales" `[Q14]`.

| Site | Name | Region | Number | Collection | Animal | Activity |
|---|---|---|---|---|---|---|
| A | The Bungle Bungles | Western Australia | 1 | Leaves | | Indigenous Culture |
| B | The Pinnacles | Western Australia | 1 | | Kangaroos | Sightseeing |
| C | Margaret River | Western Australia | 1 | Shells | Kangaroos | |
| D | Kalbarri National Park | Western Australia | 1 | Wildflowers | | Bushwalking |
| E | Uluru | Northern Territory | 4 | | Emus | Indigenous Culture |
| F | Kakadu National Park | Northern Territory | 4 | | Wombats | Sightseeing |
| G | Nitmiluk National Park | Northern Territory | 4 | Shells | Platypuses | |
| H | King's Canyon | Northern Territory | 4 | | Koalas | Swimming |
| I | The Great Barrier Reef | Queensland | 6 | Wildflowers | | Sightseeing |
| J | The Whitsundays | Queensland | 6 | | Kangaroos | Indigenous Culture |
| K | Daintree Rainforest | Queensland | 6 | Souvenirs | | Bushwalking |
| L | Surfers Paradise | Queensland | 6 | Wildflowers | | Swimming |
| M | Barossa Valley | South Australia | 3 | | Koalas | Bushwalking |
| N | Lake Eyre | South Australia | 3 | | Emus | Swimming |
| O | Kangaroo Island | South Australia | 3 | | Kangaroos | Bushwalking |
| P | Mount Gambier | South Australia | 3 | Wildflowers | | Sightseeing |
| Q | Blue Mountains | New South Wales | 5 | | Wombats | Indigenous Culture |
| R | Sydney Harbour | New South Wales | 5 | | Emus | Sightseeing |
| S | Bondi Beach | New South Wales | 5 | | Wombats | Swimming |
| T | Hunter Valley | New South Wales | 5 | | Emus | Bushwalking |
| U | Melbourne | Victoria | 2 | | Wombats | Bushwalking |
| V | The MCG | Victoria | 2 | Leaves | | Indigenous Culture |
| W | Twelve Apostles | Victoria | 2 | Shells | | Swimming |
| X | Royal Exhibition Building | Victoria | 2 | Leaves | Platypuses | |
| Y | Salamanca Markets | Tasmania | 7 | Leaves | Emus | |
| Z | Mount Wellington | Tasmania | 7 | | Koalas | Sightseeing |
| * | Port Arthur | Tasmania | 7 | Leaves | | Indigenous Culture |
| - | Richmond | Tasmania | 7 | | Kangaroos | Swimming |

Symbol values:

| Kind | Symbols and values |
|---|---|
| Collections | Leaves 1, Wildflowers 2, Shells 3, Souvenirs 5 |
| Animals (per pair) | Kangaroos 3, Emus 4, Wombats 5, Koalas 7, Platypuses 9 |
| Activities (by count) | 1 card: 0, 2: 2, 3: 4, 4: 7, 5: 10, 6: 15 |

## 3. Rules 1 to 12

Each rule is one statement followed by its acceptance criteria (AC). The numbering follows the brief.

### R1. Player count

A game has at least 2 and at most 4 players, counting humans and bots together `[Q9]`, and any other
count is refused before a game starts. (Brief line 76.)

- AC1.1 Counts of 2, 3 and 4 are accepted and produce a game with exactly that many players.
- AC1.2 Counts of 0, 1 and 5, negative numbers and input that is not a number are refused with a message
  that states the allowed range. No cards are dealt.
- AC1.3 The check applies to the total of humans and bots, whatever the mix `[Q10]`.

### R2. The deck

The deck consists of exactly the 28 cards of section 2. (Brief lines 77 to 114.)

- AC2.1 The deck has 28 cards and 28 distinct site characters: A to Z, `*` and `-`.
- AC2.2 Every card equals its row in section 2: name, site, region, number and symbols.
- AC2.3 There are 7 regions with exactly 4 sites each.
- AC2.4 All four cards of a region carry the same number: Western Australia 1, Victoria 2,
  South Australia 3, Northern Territory 4, New South Wales 5, Queensland 6, Tasmania 7.
- AC2.5 Every card carries exactly two symbols, of two different kinds out of Collection, Animal and
  Activity.
- AC2.6 Symbol counts over the deck: Leaves 5, Wildflowers 4, Shells 3, Souvenirs 1; Kangaroos 5, Emus 5,
  Wombats 4, Koalas 3, Platypuses 2; Indigenous Culture, Sightseeing, Bushwalking and Swimming 6 each.

### R3. Shuffle

At the start of every round all 28 cards are put in a random order. (Brief line 118.)

- AC3.1 After the shuffle the deck holds the same 28 cards, each exactly once.
- AC3.2 The order is taken from a source of randomness that a test can replace: the same source state
  gives the same order, so a test can fix the deal `[Q17]`.
- AC3.3 With the production source, the order is not the fixed order of section 2. This is tested with a
  seeded source whose result is known to differ.

### R4. Deal

Each player is dealt 7 cards from the shuffled deck. (Brief line 119.)

- AC4.1 After the deal every player holds exactly 7 cards.
- AC4.2 No card is in two hands.
- AC4.3 With 2, 3 and 4 players, 14, 7 and 0 cards stay undealt. Undealt cards take no part in the round
  and are shown to nobody.

### R5. Throw card

Each player selects one card of the dealt hand as the Throw card, and no other player learns which card
it is before the round ends. (Brief lines 120 to 123.)

- AC5.1 Only a card that is in the player's own hand can be selected. Anything else is refused and the
  player is asked again `[Q12]`.
- AC5.2 After the selection the hand has 6 cards and the Throw card is the first card of the draft.
- AC5.3 Nothing sent or shown to another player before the end of the round identifies the Throw card.
- AC5.4 A player can see their own Throw card at any time.
- AC5.5 No hand is passed before every player has selected a Throw card.
- AC5.6 When the round ends, every player's Throw card is shown to all players `[Q3]`.

### R6. Passing

After every selection that leaves more than one card in the hands, each player's remaining hand passes to
the next player, and the last player's hand passes to the first. (Brief lines 124 to 125.)

- AC6.1 For 2, 3 and 4 players: after a pass, player i+1 holds exactly the cards player i held before
  it, and player 1 holds what player n held.
- AC6.2 A pass neither loses nor duplicates a card.
- AC6.3 A pass happens only after all players have made the selection of the current step.

### R7. Selecting and showing a card

From each hand received, the player selects one card, which joins their draft and becomes visible to all
players. (Brief line 126.)

- AC7.1 Only a card in the received hand can be selected `[Q12]`.
- AC7.2 The selected card leaves the hand and is appended to the draft.
- AC7.3 After all players have selected in a step, every player is shown every player's shown cards.
  The Throw cards are not among them.
- AC7.4 No player sees another player's selection of the current step before making their own `[Q16]`.

### R8. Repeating until one card remains

Passing (R6) and selecting (R7) repeat until every hand holds exactly one card. (Brief lines 127 to 128.)

- AC8.1 In a round a player selects at hand sizes 7 (the Throw card), 6, 5, 4, 3 and 2.
- AC8.2 After the last selection every player has a draft of 6 cards (the Throw card and 5 shown cards)
  and a hand of 1 card.
- AC8.3 A round has exactly 5 passes to the next player.

### R9. Catch card

The single remaining card of each hand passes to the previous player (the first player's card to the
last player) and joins that player's shown cards as the Catch card. (Brief lines 129 to 130.) `[Q1]`

- AC9.1 For 2, 3 and 4 players: player i-1 receives the last card of player i, and player n receives the
  last card of player 1.
- AC9.2 Every player ends the round with a draft of exactly 7 cards: the Throw card first, the Catch card
  last.
- AC9.3 The drafts of all players together are exactly the cards dealt in the round.
- AC9.4 The Catch card is visible to all players. Receiving it involves no choice.

### R10. Scoring a round

When all drafts are complete, each player is scored on their seven cards (Throw and Catch cards
included `[Q5]`) in the five categories below. (Brief lines 131 to 158.)

#### R10a. Throw and catch

The score is the absolute difference between the numbers on the Throw card and the Catch card.

- AC10a.1 Throw 4 and Catch 1 score 3. Throw 1 and Catch 4 score 3.
- AC10a.2 Equal numbers score 0 (for example two cards of the same region).
- AC10a.3 The maximum is 6 (numbers 1 and 7).

#### R10b. Tourist sites and region bonus

A player scores 1 point for every site on their seven cards that they had not visited in an earlier
round, and 3 bonus points for every region they complete in the first round in which any player
completes it.

- AC10b.1 Seven sites never visited before score 7.
- AC10b.2 A site visited in an earlier round scores 0 when drafted again.
- AC10b.3 The visited sites of a player are kept for the whole game, per player.
- AC10b.4 A region is complete for a player when all four of its sites are visited, counting earlier
  rounds and all seven cards of this round. A and B in round 1 with C and D in round 2 complete
  Western Australia in round 2.
- AC10b.5 The first player to complete a region scores 3 bonus points for it in that round.
- AC10b.6 All players who complete the same region in the same round score 3 each `[Q4]`.
- AC10b.7 A player who completes a region in a later round than another player scores no bonus for it.
- AC10b.8 A player who completes two regions in one round, both for the first time by anyone, scores 6.
- AC10b.9 A region bonus is scored at most once per player and region.

#### R10c. Collections

The collection values on the seven cards are added up; a sum of 1 to 7 scores double, a sum above 7
scores the sum.

- AC10c.1 No collection symbol scores 0.
- AC10c.2 Three Leaves and one Shells (A, V, X, C) sum to 6 and score 12.
- AC10c.3 Souvenirs and Wildflowers (K, D) sum to 7 and score 14.
- AC10c.4 Souvenirs and Shells (K, C) sum to 8 and score 8.
- AC10c.5 Two Wildflowers and Souvenirs (D, I, K) sum to 9 and score 9.

#### R10d. Animals

For every animal, each complete pair of that animal on the seven cards scores the animal's value.

- AC10d.1 One Kangaroos card scores 0.
- AC10d.2 Two Kangaroos (B, C) score 3.
- AC10d.3 Three Kangaroos (B, C, J) score 3.
- AC10d.4 Four Kangaroos (B, C, J, O) score 6. Five (with `-`) also score 6.
- AC10d.5 A pair of Koalas and a pair of Kangaroos score 10.
- AC10d.6 Pairs of Emus, Wombats and Platypuses score 4, 5 and 9.

#### R10e. Activities

In each round a player may choose at most one activity they have not scored earlier in the game, and
scores it by the number of cards with that activity among their seven cards; choosing none is allowed.

- AC10e.1 Counts of 1, 2, 3, 4, 5 and 6 score 0, 2, 4, 7, 10 and 15.
- AC10e.2 Three Bushwalking cards (D, K, M), with Bushwalking chosen, score 4.
- AC10e.3 A player scores at most one activity per round.
- AC10e.4 An activity scored in an earlier round is not offered and cannot be chosen again in the game.
- AC10e.5 Choosing no activity scores 0 and uses up no activity.
- AC10e.6 Only activities with at least one card in the draft are offered. One card scores 0 and still
  uses up the activity `[Q6]`.
- AC10e.7 The player makes the choice after seeing all seven cards of the draft, in a single question
  that lists the possible activities with their points `[Q7]`.

### R11. Next round

After every player has been scored and has been shown the score, a new round starts with the complete
deck of 28 cards, shuffled and dealt as in R2 to R4. (Brief lines 159 to 160.)

- AC11.1 Each player is shown their own score per category for the round before the next round starts.
  Completed regions and the final result are shown to all players `[Q15]`.
- AC11.2 The new round uses all 28 cards, whichever cards were dealt or left over in the round before `[Q2]`.
- AC11.3 Hands, drafts, Throw cards and Catch cards of the finished round are cleared.
- AC11.4 Visited sites, scored activities, awarded region bonuses and the scores of earlier rounds are kept.
- AC11.5 Seat order and passing direction are the same in every round.

### R12. End of the game

The game ends after the fourth round is scored; each player's total is the sum of all category scores of
all rounds including region bonuses, the highest total wins, and among players tied on the total the one
with the most Throw and catch points wins. (Brief lines 161 to 163.)

- AC12.1 Exactly four rounds are played. There is no fifth deal.
- AC12.2 The total is the sum of Throw and catch, Tourist sites, Collections, Animals and Activities
  over four rounds, plus all region bonuses.
- AC12.3 Tourist site points are part of the total.
- AC12.4 The player with the highest total is the winner.
- AC12.5 Two players with equal totals and different Throw and catch totals: the one with the higher
  Throw and catch total wins.
- AC12.6 Players equal on both total and Throw and catch total share the win `[Q8]`.
- AC12.7 All players are told the final scores and the winner or winners.

## 4. Supporting requirements

These are not numbered rules in the brief. They come from the rest of the brief and from behaviour of the
original that "remains the same" (brief line 257).

| Id | Requirement | Source |
|---|---|---|
| S1 | A selection that is not allowed (unknown site character, card not in hand, activity not offered) changes nothing, and the same player is asked again with a message saying what is allowed. Site characters are accepted in upper and lower case. `[Q12]` | Brief line 239; original lines 90 to 94 |
| S2 | Failures (bad start arguments, port in use, server not reachable, a player lost during the game) are reported in words to the people affected. No failure is ignored silently. A lost player ends the game for everyone `[Q13]`. | Brief line 239; original lines 54, 62, 464 |
| S3 | The game is played over a network: one server, started first, waits for the human players' clients to connect and then runs the game. The server hosts no player of its own `[Q10]`. The command-line shape of the original (players and bots for the server, server address for the client) and port 2048 are kept `[Q11]`. | Brief lines 248 to 257 |
| S4 | A seat can be taken by a bot. A bot selects a card of its hand at random and takes the first activity it is offered, in the order the edition lists its activities `[Q9]`. | Brief lines 249 to 250; original lines 86 to 89, 237 to 239 |
| S5 | Every rule R1 to R12 is covered by automated tests that pass or fail, run with `./gradlew test`, and need no network, no console and no real randomness. The tests do not shape the design of the production code. | Brief lines 228 to 231, 259 to 263 |
| S6 | The game rules can be exercised without console, network or storage. | Brief line 172; AGENTS.md |
| S7 | The exact text and layout of the console output are free. The information given to each player is not. | Brief lines 269 to 270 |

## 5. Future changes (not implemented now)

The design must make these possible. No code and no tests are written for them in this hand-in.

### FC13. Alternating passing direction (brief lines 166 to 167, 226 to 227)

An optional rule for games of three or four players: the direction in which hands are passed alternates
from round to round. `[Q18]`

- It is optional: a game is played with or without it, decided before the game starts.
- It does not apply to two players.
- Accepted when: the rule can be added and switched on without changing how cards are selected, how
  rounds are scored, or how players are reached over the network.
- Decided on 24 November 2026, after rules 1 to 12 were implemented: FC13 is implemented, as the proof
  that the design takes this change. The brief does not ask for it (line 165), and asks for unit tests of
  any future modification that is implemented (brief lines 260 to 261).
  - AC13.1 With the rule on, in a game of three or four players, odd rounds pass as rules 6 and 9 say
    (hands to the next player, Catch card to the previous player) and even rounds are mirrored completely
    (hands to the previous player, Catch card to the next player). `[Q18]`
  - AC13.2 With the rule on, a game of two players passes in every round as rules 6 and 9 say.
  - AC13.3 The rule is off unless it is asked for when the server is started: a third argument
    `alternate` after `<humans> <bots>`. Any other third argument is refused with the usage text.
  - AC13.4 Without the third argument the game behaves exactly as before.

### FC14. Boomerang Europe and Boomerang USA (brief lines 168 to 170, 220 to 227)

The other two editions can be offered beside Australia: other cards, sites and regions, other symbols,
and other scoring. `[Q19]`

- Accepted when: an edition can be added by adding its cards and its scoring, without changing the
  drafting sequence (R3 to R9, R11), the network code, or the Australia scoring and its tests.

## 6. Changes the design must survive

### 6.1 Another edition

What stays the same in all three rulebooks: 2 to 4 players, 28 cards numbered 1 to 7, 7 cards dealt, a
hidden Throw card, passing and selecting, a Catch card, 4 rounds, a fresh shuffle each round, scoring on
the seven cards, highest total wins.

What differs, and so must not be fixed in the parts shared by all editions:

| Aspect | Australia | Europe | USA |
|---|---|---|---|
| Throw and catch | Absolute difference | Absolute difference | The Throw number if Catch is equal or higher, else 0 |
| Sites | 28 sites, 1 point each | Countries, 1 point each | Towns, 1 point each |
| Regions | 7 regions of 4, bonus 3 | Bonus of 2 to 5 depending on the region; an example shows a region of 3 | Regions of 4, bonus 3 |
| Extra site bonus | None | Passport: first to visit a country in every region, 7, 3, 1 by order of arrival, once per player | West to east route over connected towns, 7, 3, 1 by order of arrival, once per player |
| Green symbols | Collections 1, 2, 3, 5: sum of 1 to 7 doubled, above 7 as is | Cuisine 1, 2, 3, 5: sum of 1 to 7 as is, 8 or more halved and rounded up | Americana 1, 2, 3, 5: the sum, but 0 unless it exceeds the previous round's; after a 0, any sum of 1 or more scores |
| Yellow symbols | Animals in pairs: 3, 4, 5, 7, 9 | Transport in pairs: 3, 4, 5, 7, 9 | Animals in pairs: 3, 4, 5, 7, 9 |
| Blue symbols | Activities by own count, one per round, each once per game | National Treasures: needs one own icon, scores 4, 3 or 2 (for 2, 3 or 4 players) per matching icon on the other players' cards, one per round, each once per game | Activities by own count, one per round, each once per game |
| Tie-break | Throw and catch | Passport bonus, then Throw and catch | West to east bonus, then Throw and catch |

What this demands of the design, stated as needs and not as a solution:

1. Cards, sites, regions, region sizes and region bonuses are data of an edition.
2. The kinds of symbols on a card are not limited to Collection, Animal and Activity (brief lines 169 to 170).
3. The set of scoring categories, and the rule of each, belongs to an edition.
4. A scoring rule may need more than the player's seven cards:
   - which card is the Throw and which the Catch (all editions);
   - the player's own history: visited sites, choices already used, the previous round's value (USA Americana);
   - the other players' cards of the round and the number of players (Europe National Treasures);
   - what other players have already claimed, with an order of arrival and shared places in the same
     round (region bonuses, passport, west to east route);
   - relations between sites that are not regions (USA pathways and coasts);
   - a decision by the player (Activities, National Treasures).
5. The chain of tie-breaks belongs to an edition.
6. Which edition is played is decided when a game is set up.

The card lists of Europe and USA are not in the rulebook texts and were not examined for this document.

### 6.2 An optional rule

Optional rules (brief lines 64, 69 to 72, 226 to 227) are switched per game. FC13 is the named one. A rule
of this kind changes one step of a round (who receives a hand) and must be addable without editing the
steps it does not concern.

### 6.3 A different network layer

The way server and clients talk may be replaced to make it "more flexible and robust, as well as easier
to understand" (brief lines 222 to 224). Examples: another transport or message format, reconnecting, a
lobby, a client other than the console.

- The rules, the scoring and their tests do not change when the network layer changes.
- The rules never depend on the wording of a message. The original client recognises a question by the
  words "Type" and "keep" and the end of the game by "winner" (original lines 432 to 435); nothing in the
  new design may rest on that.
- What a player is told and asked (own hand, shown cards, a card to select, an activity to choose,
  scores, the result) is defined independently of how it is carried.
- A human at a console, a human over the network and a bot are interchangeable from the point of view of
  the rules.

## 7. Where the original departs from rules 1 to 12

Verified on 24 November 2026 by tests against the untouched original: `./gradlew legacyBugs` runs 19 tests
that each state a rule and fail. The full evidence, with failure messages, is in `legacy-findings.md`.

"Direct" means the test calls the original's own classes. "Subclass" means the test needs `RiggedGame`, a
subclass that replaces the public `server()` method to fix the deal, and that swaps the hands of later rounds
while the game runs. The original file is not changed in either case. Decision: the answer to exam question 1
states both, the assert and that it is reachable only through such a subclass.

| Rule | Departure | Original lines | Shown by |
|---|---|---|---|
| R1 | The server always adds a player of its own (line 443), so "0 4" gives 5 players. A game of five never plays: the deal runs out of cards and only the exception text is printed. | 282, 443 | Direct |
| R1 | Negative numbers pass the check ("-2 4" gives 5 players). Arguments that are not numbers end the program without a message. Both rest on AC1.2, not on the brief's wording. | 282, 464 | Direct |
| R5 | A Throw card is never shown to another player, not even when the round ends. A departure only if Q3 is answered yes. | 374, 398 | Subclass |
| R9 | A Catch card is never shown to the other players. | 374, 396, 398 | Subclass |
| R7 | An input that matches no card drafts nothing, and the hand is passed on with one card too many. | 90 to 96 | Direct |
| R7 | That one wrong input later ends the whole game, not only the round, and no winner is announced. | 159, 374, 421 | Subclass |
| R10b | The region list says "Wester Australia", so Western Australia never gives its bonus. | 323 | Subclass |
| R10b | Regions are checked before the Catch card joins the draft: a region completed by the Catch card is paid a round late, or never in round 4. A player who completes it in that later round is then paid too. | 381 to 396 | Subclass |
| R10c | Collections score twice the number of cards per collection. The values 1, 2, 3 and 5 are not used, and the limit of 7 is applied to a count that never reaches it. | 177 to 183 | Direct |
| R10d | Only exactly two of an animal score; three, four or five score 0. | 193 to 208 | Direct |
| R12 | The final total leaves out the Tourist sites points, so the wrong player can win outright. | 270, 272 | Direct |
| R12 | The first player with the highest total wins; there is no tie-break. | 412 to 417 | Subclass |

Not a departure from the brief, but behaviour to decide about:

| Topic | What the original does | Original lines | Question |
|---|---|---|---|
| R10e | Activities are asked one by one, in an order that is fixed but accidental (Indigenous Culture, Swimming, Sightseeing, Bushwalking on JDK 21). An activity with no card is offered, scores 0 and is used up. | 217 to 247 | Q6, Q7 |
| Bots | A bot never picks the last card of its hand, and says yes to the first activity asked. | 88, 237 to 239 | Q9 |
| R12 | A fifth deal happens after round 4, before the winner is named. Nobody sees it. | 403 to 410 | none: not carried over |
| S2 | Errors are swallowed in four places, and one more prints only the exception text on the server. | 54, 62, 64, 421, 463 | none: fixed by S2 |

What the original gets right, shown by 36 passing tests: rules 2, 3, 4, 6, 8, 10a and 11, the parts of rules
1, 5, 7, 9, 10b, 10e and 12 not listed above.

What cannot be tested: the client (lines 426 to 440) and the accept loop (448 to 457), because the socket, the
port and the keyboard reader are created inside them.

## 8. Open questions and provisional answers

Decided answers are final for this hand-in. Open ones show the answer the requirements above assume for
now. The full questions with their options are in `requirements-questions.md`.

| Id | Question | Answer | Status |
|---|---|---|---|
| Q1 | Does the Catch card go to the previous player (brief) or onward in the passing direction (rulebook)? | Previous player | decided |
| Q2 | Is the whole deck reshuffled each round, or are last round's undealt cards guaranteed to be dealt? | Whole deck reshuffled | decided |
| Q3 | Are Throw cards shown to everyone when the round ends? | Yes | decided |
| Q4 | Is "first to complete a region" decided per round or per card within a round? | Per round | decided |
| Q5 | Do Throw and Catch cards count in every scoring category? | Yes | decided |
| Q6 | Can an activity with no card, or one card, be scored? | At least one card needed | decided |
| Q7 | One question for the activity, or yes and no per activity? | One question | decided |
| Q8 | Who wins if the tie-break is also tied? | Shared win | decided |
| Q9 | Are bots kept, and how do they decide? | Kept; any card of the hand at random; the first activity offered, in the order the edition lists its activities | decided |
| Q10 | Does the server host a player? Is a game of bots only allowed? | No; yes | decided |
| Q11 | Are the original command-line arguments and port 2048 kept? | Yes | decided |
| Q12 | What happens on an invalid selection? | Ask again | decided |
| Q13 | What happens when a player is lost during a game? | The game ends for all, with a message | decided |
| Q14 | "New South Whales" or "New South Wales"? Which site characters are valid? | "Wales"; the 28 of the table | decided |
| Q15 | Which scores does a player see, and whose? | Their own, per category, each round; completed regions and the result go to all | decided |
| Q16 | Are the selections of one step hidden until all have selected? | Yes | decided |
| Q17 | What counts as a test of the shuffle? | Same cards, order from a replaceable random source | decided |
| Q18 | FC13: which rounds are reversed, and does the Catch direction reverse too? | Even rounds mirrored completely | decided |
| Q19 | FC14: design only, or one edition implemented as proof? | Design only | decided |
| Q20 | Are all departures of section 7 bugs to fix? | Yes, the twelve rows of its first table | decided |

## 9. Out of scope

- Implementing Europe or USA.
- Reconnecting, saving a game, a graphical client.
- Reproducing the console layout of the original.
