# Original BoomerangAustralia.java against rules 1 to 12: test evidence

The original breaks rules 1, 5 (depending on Q3), 7, 9, 10b, 10c, 10d and 12; it fulfils 2, 3, 4, 6, 8, 10a, 10e (as the brief states it) and 11. 19 tagged tests fail against it, each for the stated reason, and 36 tests pass. The Java file is untouched (sha1 `2f678238bcd465d0028b26c70a57dbe3edf94cb8` before and after). Nothing is committed.

One decision is yours: 14 of the 55 tests reach the code through a subclass seam (see section 6). If subclassing does not count as "without modifying the original", those claims fall back to "confirmed by reading only".

All line numbers refer to `app/src/main/java/BoomerangAustralia.java`.

## 1. Section 7, row by row

| Row | Verdict | Evidence |
|---|---|---|
| R1: server adds its own player; "0 2" gives 3, "0 4" gives 5; negatives pass | Confirmed by test, with a correction | `rule 1 - 0 players and 4 bots must not become a game of five players`: "players in the game: 5; the program printed: Index 0 out of bounds for length 0". `rule 1 - a negative number of players is refused` (args "-2 4"): "expected: <0> but was: <5>". Passing characterisation: "0 2" gives 3, "-1 3" gives 4. Correction: the cause is line 443 alone; lines 445 to 455 count correctly, so "1 1", "1 2" and "1 3" give 2, 3 and 4. A game of five never plays: the deal runs out of cards (line 331), line 421 catches it, and only the exception text is printed. |
| R5, R9: Throw and Catch never shown to others | Confirmed by test | `rule 9 - the Catch card is added to the cards shown to the other players`: "nothing told to seat 1, apart from its own hands, names [M], the Catch card of seat 0". `rule 5 - when the round has ended the Throw card is revealed to the other players`: "no message to seat 1 in the whole round names [E], the Throw card of seat 0". Both have an in-test control that passes (cards 2 and 6 did reach seat 1). Lines 374 (`subList(1, ...)`), 396, 398. The R9 breach is firm (brief rule 9); the R5 one is a breach only if Q3 is answered yes. |
| R7: unmatched input drafts nothing, hand passed with one card too many, round fails later | Confirmed by test, with a correction | `rule 7 - a player who types something that is not in the hand still selects one card in that step`: "cards selected, cards passed on ==> expected: <(1, 6)> but was: <(0, 7)>" (lines 90 to 96). `rule 7 - one wrong input must not end the game for everyone`: "round 1 was never scored; the game ended with ==> expected: <stopped by the test> but was: <Index 6 out of bounds for length 6>" (line 159). Correction: the whole game ends, not the round, because the catch on line 421 is outside the round loop; no winner is announced. There is a second death point: if a player's draft is still empty after step 2, `subList(1, 0)` on line 374 throws "fromIndex(1) > toIndex(0)" (passing test `R7 row - a player who never answers...`). |
| R10b: "Wester Australia" | Confirmed by test | `rule 10b - the first player to visit A, B, C and D scores 3 for Western Australia`: "region points told to seat 0 ==> expected: <[3]> but was: <[0]>". Line 323. A passing test shows `checkRegionComplete("Western Australia")` itself returns true (lines 99 to 114), so the fault is only the name in the list. |
| R10b: regions checked before the Catch joins | Confirmed by test | `rule 10b - a region completed by the Catch card scores its 3 points in that round`: "expected: <[3]> but was: <[0]>". `rule 10b - a region completed by the Catch card in round 4 counts in the final total`: "regions of seat 0 at the end of the game ==> expected: <[Northern Territory]> but was: <[]>". Lines 381 to 393 run before line 396. Passing tests confirm "one round late" (told 0, then 3). The control with the same cards and H selected, not caught, passes with 3. |
| R10c: twice the number of collection cards | Confirmed by test, with a refinement | Four tests: Leaves x3 + Shells "expected: <12> but was: <8>"; Souvenirs + Wildflowers "expected: <14> but was: <4>"; Souvenirs + Shells "expected: <8> but was: <4>"; 2 Wildflowers + Souvenirs "expected: <9> but was: <6>". Lines 178 to 183. Refinement: the limit does exist (`nr<8`, line 180) but is applied per collection type to a card count that never exceeds 5, so it never triggers. The original is right in two cases only: no collections, and Leaves alone (value 1). |
| R10d: only exactly two score | Confirmed by test | Three Kangaroos "expected: <3> but was: <0>"; four "expected: <6> but was: <0>"; five "expected: <6> but was: <0>". Lines 193 to 208. |
| R10e: asked one by one in no defined order; zero-card activity can be scored and used up | Corrected | The behaviour is as described (passing tests, see Q6 and Q9). It does not break rule 10e as the brief words it: the table, one per round, once per game and "optional" all hold (four passing tests). It departs only from AC10e.6 and AC10e.7, which rest on Q6 and Q7. The order is not random: on the JDK 21 toolchain it is always Indigenous Culture, Swimming, Sightseeing, Bushwalking (HashMap of line 217, iterated on line 230), which differs from the source order on line 215. |
| R12: total leaves out Tourist sites | Confirmed by test | `rule 12 - the final total is the sum of all categories, Tourist sites included`: "total for the categories {Throw and Catch score=10, Tourist sites score=28, Collections score=26, Animals score=12} ==> expected: <76> but was: <48>". Lines 270 and 272. |
| R12: first player with the highest total wins; no tie-break | Confirmed by test | `rule 12a - of two players tied on the total, the one with more Throw and catch points wins`: "The winner is player: 0 with 48 points ==> expected: <true> but was: <false>". Totals tied (48 each as the original counts, 55 each with sites), Throw and catch 0 against 12. Lines 412 to 417. |
| S2: exceptions swallowed in three places | Corrected: four, plus one that is only half reported | Empty catch blocks at 54, 62, 64 and 463 to 465. Line 64 (console input ended, answer becomes "") was missed. Line 422 prints only `e.getMessage()` on the server console; remote clients are told nothing. Passing tests cover 54, 62 and 64; the rule 1 test below covers 464. |
| S5: no rule can be tested in isolation | Largely refuted | All of rule 10 except the region bonus, and the rule 12 total, are tested in isolation: build a `Player` (constructor line 45), fill the public `draft`, call `roundScore`. The console is replaceable with `System.setIn/setOut` before the Player exists (Scanner created on line 34). Even the "online" player works over in-memory object streams (passing test). What remains true: the region bonus loop, passing, showing and the winner live inside `initGame` (293 to 424), with the locals `regions` and `finishedRegions` (323, 324) and unseeded shuffles (327, 404). These are reachable only through a whole game. Line 337 (the thread pool) is not an obstacle. |

Nothing in rules 1 to 12 turned out untestable. Only `client()` (426 to 440) and the accept loop (448 to 457) are out of reach: `new Socket(ip, 2048)`, `new ServerSocket(2048)` and `new Scanner(System.in)` are hard-coded. So no test uses more than one console player or the one-argument form.

## 2. Departures section 7 missed

- **R1, non-numeric arguments (lines 282, 464).** `Integer.valueOf` throws, `main` swallows it, the program exits silently. Test `rule 1 - arguments that are not numbers are refused with a message`: "the program printed: ''". This is AC1.2 of requirements.md; the brief's rule 1 says nothing about messages.
- **R10b knock-on (lines 384 to 391).** When a Catch-completed region is paid a round late, a player who completes the same region in that later round is paid too, though the rule gives them nothing. Passing test `R10b row - and then a player completing the region a round later is paid as well` (both told 0, then 3).
- **R12 knock-on, by reading only.** Because the winner is chosen on totals without site points (lines 272, 399, 414), the wrong player can win outright, not only in ties. I did not build a scenario for it.
- **AC12.1, fifth deal (lines 403 to 410).** After round 4 the original shuffles and deals again before naming the winner. Passing test `R12 - after the fourth round...` (hands of 7, 14 cards left in the deck). Invisible to players.
- **R11, by reading only.** A player is shown category scores each round (lines 160, 171, 184, 211, 242) but never a round total: the value returned on line 276 is stored in `finalScore` (line 399), overwritten each round and never printed. I judged this not a breach of the brief's wording.
- **Bots:** see Q9.

## 3. The three open questions

**Q3: No, never.** Test `Q3 - over a whole game no player is ever told another player's Throw or Catch card` passes over four rounds. Others are only ever sent "Player n has drafted" with draft cards 2 to 6 (line 374). The full draft goes to its owner only (line 398).

**Q6: Yes.** Test `Q6 - an activity with no card is offered, scores 0 when accepted and is used up for the game` passes: the prompt is "Want to keep Swimming(0) [0 points]? (Y/N)", and after Y Swimming is stored as 0 (line 241). The next round, with six Swimming cards, does not offer it (line 220). Line 232 gives 0 for zero cards.

**Q9:**
- **Card choice (line 88).** `rnd.nextInt(hand.size()-1)` is uniform over all cards except the last. In 700 drafts from a hand of 7 the bot picked exactly A to F, never G. From a hand of two it always takes the first card. A hand of one would throw `IllegalArgumentException`, but normal play never asks for that (hand sizes are 7 down to 2).
- **Activity choice (lines 237 to 239).** The bot answers Y to the first question, whatever the count. It therefore uses activities in the fixed order Indigenous Culture, Swimming, Sightseeing, Bushwalking, one per round. With six Bushwalking cards three rounds running it had used up the other three at 0 points and Bushwalking (15) was still waiting.
- **Consequence for the Q9 lean.** "The first activity offered, as in the original" means this HashMap order, not the order of line 215.

## 4. Passing tests: what the original gets right

In `LegacyRulesHoldTest.kt`, 20 tests:
- **Rule 1:** "1 1", "1 2", "1 3" give 2, 3, 4 players; totals of 1 and 5 and a wrong argument count are refused with the "2-4 players" message and no deck.
- **Rule 2:** the 28 cards equal the brief's table field by field (including "New South Whales").
- **Rule 3:** the same 28 cards, in an order that is not the table order and differs between games.
- **Rule 4:** 7 cards each, no duplicates, 14, 7 and 0 undealt for 2, 3 and 4 players.
- **Rule 5:** during the round no other player is told the Throw card; the owner sees it.
- **Rule 6:** the remaining hand goes to the next player, last to first (2, 3, 4 players).
- **Rule 7:** after each later step, everyone is told exactly the cards selected so far, minus the Throw.
- **Rule 8:** selections at hand sizes 7, 6, 5, 4, 3, 2.
- **Rule 9:** the last card goes to the previous player and is seventh in the draft; the drafts together equal the deal.
- **Rule 10a:** the absolute difference (3, 3, 0, 6).
- **Rule 10b:** site points 7, 3, 0 across rounds; first completer of a region gets 3, a later-round completer 0; `checkRegionComplete` is correct.
- **Rule 10c:** only the two control cases (none, Leaves alone).
- **Rule 10d:** exactly one pair per animal, all five values.
- **Rule 10e:** the table 0, 2, 4, 7, 10, 15; one per round; once per game; none allowed.
- **Rules 11 and 12:** four rounds, a new deal each, sites remembered, highest total wins, all are told.
- **Rule 12, unmodified path:** real constructor "1 1", real `server()`, scripted keyboard, plays to "The winner is player:".

`LegacyBehaviourTest.kt` holds 15 passing characterisation tests (rows R1, R7, R10b, S2, and Q3, Q6, Q7, Q9, AC12.1).

## 5. Files and Gradle output

Created in `/Users/laszloprekop/dev/LTU/HomeExam/boomerang/app/src/test/kotlin/legacy/`:
- `LegacyHarness.kt`
- `LegacyRulesHoldTest.kt`
- `LegacyBugsTest.kt`
- `LegacyBehaviourTest.kt`

Changed: `/Users/laszloprekop/dev/LTU/HomeExam/boomerang/app/build.gradle.kts`. `test` now excludes the tag `legacy-bug`; the new task `legacyBugs` includes only that tag and is not wired into `check` or `build`.

`./gradlew test --rerun`: 36 tests, 0 failures (result files: 15 + 20 + 1). `./gradlew test build`, last lines:
```
> Task :app:check UP-TO-DATE
> Task :app:build UP-TO-DATE

BUILD SUCCESSFUL in 746ms
8 actionable tasks: 8 up-to-date
```

`./gradlew legacyBugs`, last lines (it exits non-zero by design):
```
LegacyBugsTest > rule 1 - 0 players and 4 bots must not become a game of five players() FAILED
    org.opentest4j.AssertionFailedError: players in the game: 5; the program printed: Index 0 out of bounds for length 0 ==> expected: <true> but was: <false>
19 tests completed, 19 failed
> Task :app:legacyBugs FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:legacyBugs'.
> There were failing tests. See the report at: file:///Users/laszloprekop/dev/LTU/HomeExam/boomerang/app/build/reports/tests/legacyBugs/index.html
BUILD FAILED in 12s
```

## 6. Doubts and traps

**The seam.**
- **Round 1 deal.** 14 whole-game tests use `RiggedGame`, a Kotlin subclass that overrides the public `server()` (line 442). `initGame` calls it on line 328, after the shuffle (327) and before the deal (330 to 332), so the subclass can reorder the public `deck`.
- **Player I/O.** It also overrides `Player.sendMessage` and `readMessage` (lines 52, 59) through an inner subclass, which gives each seat its own inbox.
- **Rounds 2 to 4.** There is no gap between shuffle and deal (403 to 410), so the harness replaces the public `hand` lists at the first message of the round, under a lock. Affected: the round-4 region test, the tie-break test, the later-round region tests and the Q3 test.
- **What does not need it.** Rules 1, 10a, 10c, 10d, 10e, the rule 12 total, the unit-level rule 7 test and the Q6 and Q9 tests use only direct calls or the real constructor.

**Tag choices that are judgement calls.**
- The rule 5 test presumes Q3 is "yes".
- The two rule 1 tests for negatives and non-numbers rest on AC1.2, not on the brief's wording.
- "0 2 gives 3" is kept as a passing characterisation, because 3 players is within rule 1.

**Traps.**
- **Swallowed exceptions hide harness faults.** Line 421 catches everything and prints only the message; exceptions in the pool threads (line 352) vanish to stderr. Every whole-game test therefore calls `assertRanAsPlanned()`: no harness exceptions, the realised drafts equal the plan, and the game ended the expected way.
- **A wrong scenario passes silently.** My first round-4 Catch test passed, because the filler rounds had already given seat 0 two Northern Territory sites, so the region completed without the Catch. Sites accumulate across rounds; the test now has a precondition on `sites`.
- **The HashMap order is easy to guess wrong.** I did, twice. The asserted order is observed on the JDK 21 toolchain and could differ on another JVM.
- **The Scanner binds early.** It is created when a `Player` is constructed (line 34), so `System.in` must be replaced before construction, not before the read. Once it sees end of input, every later answer is "".
- **A whole game is slow.** It costs about 2.4 s, from `Thread.sleep(100)` per step on line 361. The harness stops single-round tests early by throwing from the last message of the round (line 400).
- **Catch cards pass through other hands.** Every player has held another player's Catch card on the way round, so "was shown the Catch" must exclude the viewer's own hand displays. Throw cards never leave their owner.
- **Package-private types.** `Card` and `Player` are package-private, so Kotlin declarations that expose them must be `internal`, and the tests must stay in the default package.
- **Port 2048.** Never pass a first argument above 1, or a single argument: both open it.
- **Flakiness.** The shuffle test has a negligible chance (about 1 in 28!) of failing by chance.
