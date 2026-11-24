# Code review: Boomerang Australia in Kotlin

Reviewed on 24 November 2026 at commit `0acea54` (working tree clean). Read: `AGENTS.md`,
`docs/requirements.md`, `docs/ARCHITECTURE.md`, all 22 Kotlin source files, the test sources, the build
file, the README and the original `BoomerangAustralia.java`. Nothing was changed except this file.

What was run:

- `./gradlew test --rerun-tasks`: 244 tests pass (208 against the new code, 36 against the original).
- `./gradlew legacyBugs`: 19 tests fail against the original, as intended.
- `git diff --stat tests-locked-2 HEAD -- app/src/main`: 2 files, 35 insertions, 7 deletions
  (`App.kt` +28 -7, `AlternatingPassingRule.kt` +14). The figures of D6 are correct.

What was not run: the program itself (`./gradlew run`), because the review was limited to the test tasks.
Finding H1 and the network findings M1 and M2 are therefore established by reading, not by a run. They
should be confirmed with one manual game before hand-in.

Path prefix for all Kotlin files below: `app/src/main/kotlin/boomerang/`.

## 1. Summary

The game logic is sound. I found no fault in rules 1 to 12, no dependency that crosses a package boundary
the wrong way, and no signature in the code that differs from section 3 of the architecture. Every
acceptance criterion AC1.1 to AC12.7 has at least one test that names it.

The faults are at the edges: how the program is started, what a client is told when the server gives up,
how the lobby reacts to a bad connection, and several sentences of `ARCHITECTURE.md` that the code does not
bear out. None needs a change of design. One is serious for a hand-in because it is the first thing an
examiner meets (H1).

## 2. Findings by severity

### High

**H1. The documented way to run the game cannot play a human seat.**
`app/build.gradle.kts` lines 22 to 24, `README.md` line 15.
The README gives `./gradlew run` as the command to run the game. Without arguments that prints the usage
text and exits with status 1, which Gradle reports as a failed build. With `--args="localhost"` the client
starts, but Gradle's `run` task gives the process an empty standard input unless the build says otherwise,
and the build does not. `readlnOrNull()` then returns null at the first question, `ConsolePlayer` throws
`PlayerLost` (`players/ConsolePlayer.kt` line 64), the client prints "A player can no longer be reached"
and the server aborts the game. Only a game of bots ("0 4") works through Gradle.
Why it matters: criterion f is judged by running the program, and the README has no other instruction.
Smallest fix: in `build.gradle.kts` add `tasks.named<JavaExec>("run") { standardInput = System.`in` }`, and
in the README give the real commands, for example `./gradlew run -q --console=plain --args="1 1"` in one
terminal and `--args="localhost"` in another. Alternative without a build change: document
`./gradlew installDist` and the script under `app/build/install/app/bin/`.
Status: by reading; not run. Verify first.

### Medium

**M1. A client can end silently, with status 0, when the game never finished.**
`App.kt` lines 103 to 105 and 126 to 133, `net/ClientSession.kt` line 14.
`runServer` closes every remote player in a `finally`, and `RemotePlayer.close` sends `Bye`. The client
treats `Bye` as the normal end and returns without a word. On the normal path it has seen `GameOver` or
`GameAborted` first. On two other paths it has seen nothing: (a) the lobby fails after this client was
seated (`ConnectionLost` from `listener.accept()`, `App.kt` line 113); (b) the server crashes on a broken
invariant (`require`, `check`, an index error), which D10 says is allowed to crash with a stack trace on
the server. In both the person at the client sees the program stop with no message.
Why it matters: S2 says no failure is ignored silently and a failure is reported to the people affected.
Smallest fix: one line in `runClient` after `ClientSession(...).run()`: print "The server has closed the
session." It is harmless after a normal end. A cleaner fix is a reason carried by `Bye`.

**M2. One connection that is not a game client stops the lobby or ends the server.**
`net/SocketConnection.kt` lines 14 to 23 (line 18), `net/SocketConnection.kt` lines 77 to 84, `App.kt`
lines 111 to 114.
`SocketListener.accept` builds a `SocketConnection`, whose constructor blocks in `ObjectInputStream(...)`
until the peer has sent a stream header. There is no time limit, and the accept loop is serial. A peer
that connects and stays silent (a port scan, a telnet session) holds the lobby for ever. A peer that sends
other bytes (a browser) causes `StreamCorruptedException`, which becomes `ConnectionLost`, which leaves
`acceptHumans`, ends the server and dismisses the clients already seated (and by M1 they are told
nothing).
Why it matters: S2, and the brief names a more robust network layer as a coming change. This is the
first thing that change would have to repair, and it sits in the constructor, not behind a seam.
Smallest fix: set `socket.soTimeout` for the handshake in the `init` block and clear it afterwards; in
`acceptHumans` catch `ConnectionLost` for the one connection, print it and keep waiting for the seat.

**M3. D3 describes a console and a use of `Symbol.kind` that do not exist.**
`docs/ARCHITECTURE.md` lines 479 to 485 against `players/ConsolePlayer.kt` lines 115 to 118 and
`game/Cards.kt` line 6.
D3 says the console layout is "one row per kind found on the cards" and that "`kind` only groups symbols
for display". `cardLine` prints one line per card with the symbol names joined by commas. No main source
file reads `kind` (checked with grep); only the R2 tests do (AC2.5).
Why it matters: criterion i, and the oral exam. The defence of D3 rests on a property the code lacks.
Smallest fix: correct the two sentences: `kind` is card data that AC2.5 checks; the console prints the
symbols of a card on the card's line.

**M4. Dependency rule 7 is broken as written, and D10 contradicts it.**
`docs/ARCHITECTURE.md` lines 138 to 139 against `net/SocketConnection.kt` lines 21, 31, 38, 40, 42, 57,
70 and 81, `net/ClientSession.kt` line 11, `scoring/SiteCategories.kt` line 26.
Rule 7: "Text for people is produced only in `players/ConsolePlayer.kt` and `App.kt`". The `net` package
writes the sentences that `App.kt` prints unchanged to the person (lines 76 and 130), and D10 says so
itself ("each with its own sentence", lines 576 to 577). `RegionBonus` builds the announcement text by
joining names with ", ". `DependencyRulesTest` checks rules 1 to 6 only, so nothing holds rule 7.
Why it matters: the document claims a rule the code does not keep. Rewording a failure message means
editing `net`, which rule 7 says cannot happen.
Smallest fix: reword rule 7 to what is true ("text for players during a game is produced only in
`ConsolePlayer.kt`; the reasons of `ConnectionLost` are written in `net` and printed by `App.kt`"). The
larger fix, a reason value in `ConnectionLost` and a list of names in `Score`, is not needed.

**M5. The start-up path and two failures named by S2 have no test.**
`App.kt` lines 65 to 141; `net/SocketConnection.kt` lines 53 to 60 and 66 to 71.
`main`, `runServer`, `acceptHumans`, `closeAll`, `printResult` and `runClient` are run by no test. That
leaves unpinned: humans take the first seats (line 100), every connection is closed on every path (lines
103 to 105), the two failure sentences and the exit status. S2 names "port in use" and "server not
reachable"; both are implemented and neither is tested, although each needs five lines
(`SocketListener(0)` and a second listener on its port; `SocketConnection.open` to a closed port).
The start-up code is also not shaped for a test: `fail` calls `exitProcess` (line 83), the port is the
constant (line 96), output is `println`.
Why it matters: criteria c, d and g. This is the one part of the program built the way the original was.
Smallest fix: the two tests above. Optional: give `runServer` the listener and a print function as
parameters so that a test can run a whole server with scripted clients.

### Low

**L1. `Draft` depends on `Game`, against the order section 8 promises.**
`game/Cards.kt` lines 25 and 26, `game/DraftPhase.kt` lines 6, 31 and 33; `docs/ARCHITECTURE.md` lines 747
to 749.
`Draft.catchCard` reads `Game.HAND_SIZE`. The lowest value type of the package depends on the class that
runs the game, which closes a cycle `Game` to `DraftPhase` to `Draft` to `Game`. Section 8 says a file
uses only files above it, with one stated exception; this is a second and third one.
Smallest fix: declare the size beside `Draft` (for example `Draft.SIZE` in `Cards.kt`) and let
`Game.HAND_SIZE` refer to it, or name the exception in section 8.

**L2. `ChosenSymbolCount` indexes `pointsByCount` without a check.**
`scoring/SymbolCategories.kt` line 38.
Australia is safe (at most six cards per activity, seven entries). The class is offered as a reusable
mechanism: an edition with a shorter list, or seven cards of one symbol, fails with an index error in the
middle of a round.
Smallest fix: `require(pointsByCount.isNotEmpty())` in an `init`, and
`pointsByCount[minOf(count, pointsByCount.lastIndex)]`.

**L3. `Edition` does not check that the deck can be dealt.**
`game/Edition.kt` lines 10 to 22, `game/DraftPhase.kt` line 33, `game/Game.kt` line 43.
A deck of fewer than 28 cards passes the constructor and fails later in `subList` for four players. The
contract of `IShuffler` ("the same cards, each once") is also taken on trust.
Smallest fix: `require(cards.size >= Game.PLAYERS.last * Game.HAND_SIZE)` in `Edition`.

**L4. A category that returns no score fails without a hint.**
`game/ScoringPhase.kt` lines 21 and 29. The contract "never empty" is broken by `first()` with a bare
`NoSuchElementException`. Smallest fix: `check(offered.isNotEmpty()) { "${category.name} offered no
score" }` after line 21.

**L5. `GameAborted(-1)` is possible.**
`game/Game.kt` line 86. `players.indexOf(lost.player)` is -1 when the exception names an object that is
not at the table (a wrapper around a player, for instance). The others are then told that "Player 0 was
lost". Not reachable with today's three player classes. Smallest fix: `check(lostSeat >= 0)`.

**L6. The object streams are used for a whole session without `reset`, and without a filter.**
`net/SocketConnection.kt` lines 28, 29 and 38.
`ObjectOutputStream` remembers every object it has written by identity. That is correct only as long as
every value sent is immutable and never sent again after a change; nothing enforces it, and both ends keep
every message of the game in memory. The test double `SerializingWire` opens a new stream per message, so
no test exercises the long-lived stream. Reading objects from port 2048 without an `ObjectInputFilter`
accepts any serializable class on the classpath. The data classes carry no `serialVersionUID`, so client
and server must be the same build.
Smallest fix: `output.reset()` after `flush()`; a filter that admits `boomerang.*`, `kotlin.*` and
`java.util.*`.

**L7. A player is never told which way the cards travel.**
`game/IPlayer.kt` line 21, `game/Game.kt` lines 59 to 63.
`RoundStarted` carries the round and the visited sites. With `alternate` on, a person cannot know from the
screen who receives the hand in this round, nor that the option is on. No AC of FC13 asks for it, so no
test notices. Smallest fix: a `passing` field in `RoundStarted` with one sentence in `describe`.

**L8. Messages on the server and client console are rough.**
`App.kt` line 74 (does not say which seat or why, although `PlayerLost` has the cause), line 89 (prints
the `toString` of an exception), line 120 (prints Kotlin lists, "Winning player: [1, 2]"), line 83
(failures go to standard output). On a client whose own input ended, line 74 says "A player can no longer
be reached", which is about itself.

**L9. README.**
`README.md` lines 7 and 13 have typing errors ("downliad", "testes"). It does not say how to start a
server and clients, what the arguments mean, or that `./gradlew legacyBugs` exists and fails on purpose.

**L10. Smaller differences between `ARCHITECTURE.md` and the code.**

| Where in the document | What it says | What the code shows |
|---|---|---|
| Lines 739 to 740 (7.4, ISP) | "No implementation carries a method or parameter it does not use." | `BotPlayer.show` is empty (`players/BotPlayer.kt` line 21); `StandardPassingRule.passing` ignores both parameters (`game/IPassingRule.kt` line 23); `Bye.deliverTo` ignores the player (`net/IConnection.kt` line 53). |
| Lines 608 to 610 (D13) | Three doubles and two helpers in `support/`. | There is a sixth file, `SerializingConnection.kt` (`SerializingWire`), and `GameOverTheWireTest` is not mentioned. |
| Lines 615 to 616 (D14) | Six open interfaces, "each with operations and more than one implementation". | `IConnection` and `IShuffler` have one implementation each in the main sources; the others are test doubles. `IAnswer` has only lambdas. |
| Lines 12 to 14 | `game` talks to the outside through three interfaces. | Four are implemented outside `game`: `IScoringCategory` is implemented in `scoring`. |
| Lines 72 to 98 (diagram 2.1) | The arrows drawn. | Missing: `Game` to `IPlayer` (`Game.kt` lines 54, 61, 70, 80, 90), `ScoringPhase` to `History`, the messages to `IPlayer` (`deliverTo`). |
| Lines 685 and 703 to 706 | A second edition "adds one line to `App.kt`". | `App.kt` line 101 names `australia()` with no switch. D6 reports that the same estimate for FC13 was wrong by 28 lines; the estimate for FC14 was not corrected. |
| Lines 129 to 130 (rule 2) | No value appears in `scoring` as a literal. | The test checks strings of three or more characters only. The pair size `2` (`SymbolCategories.kt` line 23) is a literal; it is mechanism, so the rule is kept in spirit. |

Everything else I compared matches: the 22 files and their order, every signature of section 3, the
sequence of section 4, the seven events, the type tests listed in D15, the four files with
`Serializable`, the measured figures of D6 and the 19 tests of FC13.

### Matters of taste, not faults

- The `I` prefix is not a Kotlin convention, and D14 calls the naming "idiomatic Kotlin". The rule itself
  (open interface with prefix, sealed family without) is consistent and is kept everywhere.
- Files named after an interface hold much else: `IPlayer.kt` has the seven events, two requests and
  `PlayerLost`; `IConnection.kt` has the whole protocol; `History.kt` has `GameResult`. Section 8 gives the
  reason. A reader looking for `GameEvent` has to know.
- Fully qualified names where an import would do (`net/SocketConnection.kt` lines 9, 10 and 55,
  `game/IShuffler.kt` line 7, `players/BotPlayer.kt` line 10), and a constructor parameter copied to a
  field by hand.
- Loops that fill mutable lists where `map` would do (`DraftPhase.deal`, `ScoringPhase.askChoosers`). It
  reads easily aloud, which may be the point.
- KDoc on private one-liners that repeats the code ("Prints the event.").
- `History.previousPoints` and `ScoringContext.otherDrafts` have no caller in the main sources. They exist
  for FC14, which is a named future change, so the rule of `AGENTS.md` allows them. They are still code
  for an edition that is not written.
- The original Java file is in the main source set, so the built jar holds a second `main`. The exam asks
  for the file to stay.
- Q10: the server hosts no player. Playing alone against bots now needs two processes where the original
  needed one. It is a decided requirement, not a fault, but expect the question.

## 3. The seven questions

**1. Requirements.** R1 to R12: satisfied; I traced each AC to code and to a test. FC13: satisfied
(AC13.1 to AC13.4). S1, S4, S5, S6, S7: satisfied. S2: satisfied inside a game (lost player, second loss,
refusals); not fully at the edges (M1, M2, and L8 for wording). S3: implemented, and by H1 not reachable
through the documented command for a human. Behaviour no test pins: all of M5, the long-lived object
stream (L6), deck size (L3), the bound in L2, what a client sees on `Bye` without a result (M1), the
direction never being told (L7).

**2. The documented architecture.** Obeyed in structure. The differences are M3, M4, L1 and the table in
L10. No difference is in a signature or a package boundary.

**3. Direction of dependencies.** Between packages: correct, all inward, and `DependencyRulesTest` holds
it. `net` and `players` do not know each other; `scoring` names no edition; only `App.kt` knows several
outer packages. Inside `game`: one wrong direction, L1. Nothing else.

**4. Separation of responsibilities.** Good. `DraftPhase`, `ScoringPhase`, `Game` and `History` each do
one thing and are tested alone. The weakest spot is `App.kt`, which parses, composes, runs the lobby, runs
the client and words failures in 144 lines; it is small enough, but it is the part without tests (M5).
`ConsolePlayer.kt` mixes the input loop with all layout; the layout is in free functions and is tested as
text, which is acceptable.

**5. Unnecessary coupling.** Two points. (a) The wire format is Java serialization of the classes of
`game`: renaming a field of `Card` or moving `Score` changes the protocol, and a client that is not this
build cannot connect. D9 states this as a chosen cost; it is the real limit of "another transport is one
new file". (b) `ServerMessage.deliverTo(IPlayer)` puts behaviour into the transported values; it removes a
`when` on the client and is a fair trade. Nothing else: no shared mutable state, no singleton with state,
no lookup by string.

**6. Unnecessary abstraction.** Nothing serious. Every open interface is tied to a requirement or a named
change: `IPlayer` (S3, S4, 6.3), `IScoringCategory` (FC14), `IPassingRule` (FC13), `IShuffler` (AC3.2),
`IConnection` (6.3), `IAnswer` (AC7.4 without threads). Candidates for removal are small: `Symbol.kind`
has no reader in production (M3); `previousPoints` and `otherDrafts` serve only FC14. There is no factory,
registry, base class or event bus that nothing needs.

**7. Maintainability that tests do not show.** L6 (identity cache of the stream), M4 (wording in `net`),
the implicit contract of `IPlayer` that a question is put at once and the answer is read later, on one
connection, in strict request and reply order (a time limit or a heartbeat needs a reader thread behind
`IConnection`, as D8 says), and the fact that `Score.chosen` can only be a `Symbol` (section 9 of the
architecture admits it).

## 4. Verdicts on the grading criteria

| | Criterion | Verdict | Weakest point |
|---|---|---|---|
| a | Extension with new editions | High. An edition is a value built from categories; new mechanisms are new classes; `game` stays closed. | Not proven by code: FC14 is design only. `App.kt` line 101 has no way to choose an edition, and a choice can only be a `Symbol`. |
| b | Modification: network, optional rules | Good. FC13 is implemented and measured: one new file and the start-up file. The network sits behind `IConnection` and `IPlayer`. | The seam for optional rules covers directions only. The network is blocking request and reply over Java serialization of `game` values, with no time limit (M2, L6). |
| c | Designed for testability | Very high for rules, scoring, players and protocol: no thread, socket, console or chance on any rule path. | The composition root: `exitProcess`, the fixed port and `println` in `App.kt` (M5). |
| d | Unit tests for rules 1 to 12 | Very high. Every AC from 1.1 to 12.7 has a test that names it; the twelve legacy faults are retested against the new code. | Outside the twelve rules: S2 at start-up and `main` are untested (M5). |
| e | Structure, standards, naming | Good. Small files, one vocabulary, constants named, no dead code. | README (L9); the `I` prefix against Kotlin convention; files that hold more than their name says. |
| f | Correct functionality | Rules 1 to 12 and FC13 are correct as far as reading and 208 tests can show. | Running it: H1. Until that is confirmed or fixed, a human cannot play by the documented command. |
| g | Errors and exceptions | Good inside a game: refusals are values, a lost player ends the game with a message, no empty catch. | The edges: silent client exit (M1), the lobby (M2), rough operator messages (L8). |
| h | Documentation | Very thorough: KDoc on every type and function, with rule ids; decisions with rejected options. | The README, and a few statements in `ARCHITECTURE.md` that are not true of the code (M3, M4, L10). |
| i | True to the design document | Largely. Packages, files, signatures, sequence and patterns are as written. | D3 on `kind` and the console (M3), rule 7 (M4), section 8 order (L1), the ISP sentence (L10). |

## 5. Improvements over the original

Lines are those of `app/src/main/java/BoomerangAustralia.java`. New files are under
`app/src/main/kotlin/boomerang/`.

| # | Improvement | SOLID | Booch metric | Quality attribute | Original lines | New file |
|---|---|---|---|---|---|---|
| 1 | One class with everything split into rules, scoring, edition data, players, network and start-up | SRP | Cohesion | Modifiability | 6 to 468 (`Player` 26 to 278, `initGame` 293 to 424) | `game/Game.kt`, `game/DraftPhase.kt`, `game/ScoringPhase.kt` |
| 2 | Kinds of player are classes behind one interface, not the flags `isBot` and `online` | OCP, LSP | Coupling | Extensibility | 28 to 29, 47 to 50, 53 to 57, 61 to 64, 84 to 89, 235 to 239, 443 to 446 | `game/IPlayer.kt`, `players/BotPlayer.kt`, `players/ConsolePlayer.kt`, `net/RemotePlayer.kt` |
| 3 | A card holds a list of symbols; a new kind of symbol is data | OCP | Sufficiency | Extensibility | 12 to 19, 116 to 139, 140 to 152 | `game/Cards.kt`, `australia/Australia.kt` |
| 4 | Scoring is one stateless class per mechanism; computing is apart from telling and asking | SRP, OCP | Primitiveness | Testability, extensibility | 154 to 277 (copied block 193 to 208) | `game/IScoringCategory.kt`, `scoring/SymbolCategories.kt`, `scoring/SiteCategories.kt`, `scoring/ThrowCatchDifference.kt` |
| 5 | All score state in one immutable `History`; the region rule in one class | SRP | Cohesion | Testability | 33, 38 to 42, 99 to 114, 323 to 324, 381 to 393 | `game/History.kt`, `scoring/SiteCategories.kt` |
| 6 | Collaborators are handed in: shuffle, random source, console, connection | DIP | Coupling | Testability | 34, 87, 327, 404, 428 to 430, 449 to 453 | `game/IShuffler.kt`, `players/ConsolePlayer.kt` (two lambdas), `App.kt` (composition root) |
| 7 | Values are immutable and state is private; no object writes another's fields | SRP (information hiding) | Coupling | Modifiability | 7 to 10, 27 to 42, 95 to 96, 365 to 366, 385 to 386, 396, 407 to 409 | `game/Cards.kt` (`Draft`), `game/DraftPhase.kt` |
| 8 | Client and server agree on typed messages, not on words in a text | OCP | Coupling | Modifiability | 432 to 435, 52 to 66 | `net/IConnection.kt`, `net/ClientSession.kt` |
| 9 | The transport is one adapter behind an interface | DIP | Coupling | Modifiability | 426 to 458 | `net/SocketConnection.kt`, `net/RemotePlayer.kt` |
| 10 | Passing directions are a rule object; FC13 is a new file | OCP | Primitiveness | Modifiability | 341 to 347 | `game/IPassingRule.kt`, `game/AlternatingPassingRule.kt` |
| 11 | "At the same time" by asking all and then awaiting all; no thread pool, no polling sleep | SRP | Primitiveness | Testability | 336 to 362 | `game/IPlayer.kt` (`IAnswer`), `game/DraftPhase.kt` |
| 12 | Edition facts are data of an `Edition`: cards, regions, bonus, tables, tie-break | OCP | Completeness | Extensibility | 110, 177, 190, 215, 229, 269, 294 to 323, 386 | `game/Edition.kt`, `australia/Australia.kt` |
| 13 | Small interfaces by client: three, two and one operation in place of one `Player` with all of it | ISP | Primitiveness | Testability | 26 to 278 | `game/IPlayer.kt`, `net/IConnection.kt`, `game/IScoringCategory.kt` |
| 14 | Rule 1 has one definition and the command line asks it | SRP | Cohesion | Modifiability | 282, 443 to 450 | `game/Game.kt` (lines 12 to 16 and 99), `App.kt` (line 46) |
| 15 | Failures are typed and reported; a wrong answer is a value and is asked again; the constructor no longer runs the game | SRP | Completeness | Testability | 54, 62, 64, 90 to 94, 280 to 291, 421 to 423, 460 to 466 | `game/Game.kt` (lines 19 to 26, 85 to 95), `net/SocketConnection.kt`, `App.kt` (lines 65 to 78) |
| 16 | `Card` is a data class: `equals` and `hashCode` agree, no unchecked cast, no unused field | LSP (contract of `Object`) | Completeness | Testability | 15, 21 to 23 | `game/Cards.kt` |

## 6. What I would fix before hand-in, in this order

1. H1: make a human game startable by the documented command, rewrite the README commands, and play one
   whole game by hand with two clients and a bot. Half an hour, and it protects criterion f.
2. M3, M4, L1 (document side) and L10: correct the sentences of `ARCHITECTURE.md` that the code does not
   bear out. Document changes only; they protect criterion i and the oral exam.
3. M1: one line so that a client never ends without a word.
4. M5: the two tests for "port in use" and "no server answers".
5. If time is left: M2 (handshake time limit and a lobby that survives one bad connection), then L2 to L4
   (three one-line guards).

Each code change above needs the full test run again and, by `AGENTS.md`, approval first where it touches
the architecture.
