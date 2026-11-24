# Requirements: open questions, with options

From the requirements analyst. "Rec." is the analyst's recommendation. Not yet decided.

"Rec." is my recommendation and is what Part A assumes.

**Q1. Where does the last card (the Catch) go?**
- Sources: brief rule 9, lines 129 to 130, says the previous player. Rulebook lines 99 to 103 says "passed to the left", the same direction as every other pass. The original follows the brief (line 346).
- A (Rec.): previous player. This is the graded specification. With 3 or 4 players, your Catch is one of the two cards you handed on a step earlier, and your neighbour picks which.
- B: next player, as in the rulebook. Your Catch then comes from a hand you saw several steps earlier. R9 and its tests change.
- With 2 players, A and B are identical.

**Q2. Leftover cards with 2 or 3 players.**
- Sources: brief rule 11, lines 159 to 160, says "start a new round from step 2", so all 28 are reshuffled. Rulebook lines 269 to 274 puts last round's undealt cards on top of the new deck so they are certain to be dealt. Rulebook lines 61 and 66 to 67 say "shuffle the 28 cards", so the rulebook contradicts itself. The original reshuffles all 28 (lines 403 to 404).
- A (Rec.): full reshuffle each round. Rounds are independent and nothing is carried over.
- B: the rulebook note. The deal depends on the previous round, so R3, R4 and R11 gain state and more tests. With 2 players, the 14 cards unused last round would be exactly the next deal.

**Q3. Is the Throw card shown to everyone at the end of the round?**
- Sources: brief line 120 says "not revealed to other players until the round ends". Rule 9, line 130, puts the Catch among the "shown cards". Rulebook lines 74 to 77 agree. The original never shows another player's Throw or Catch (lines 369 to 377, 395 to 401).
- A (Rec.): at round end, all seven cards of every player are shown to all.
- B: keep the original. Throw cards stay private for good, and only the hiding during the round is tested.

**Q4. What does "first to complete a region" mean within one round?**
- Sources: brief lines 140 to 141 say "at the same time". Rulebook lines 167 to 168 say "at the end of the same round".
- A (Rec.): completion is judged once per round on all seven cards. Everyone completing a region in the same round gets 3.
- B: by draft step. The player whose completing card was taken earlier in the round wins alone. This needs per-step tracking and contradicts the rulebook.
- Either way, I assume the Catch card counts towards completion in the round it is received. The original misses this.

**Q5. Do Throw and Catch cards count in all categories?**
- Sources: brief lines 137 ("played cards this round"), 144, 148 ("cards you drafted") and 152 ("your seven cards") vary in wording. Rulebook lines 109 to 111 say the score is "determined by the seven cards".
- A (Rec.): all seven cards count for sites, collections, animals and activities.
- B: only the five chosen face-up cards count for some categories. Nothing in either source supports this.

**Q6. Can an activity be scored with no card, or with one?**
- Sources: the brief's table (lines 155 to 156) starts at 1 card for 0 points. Rulebook lines 232 to 236 and 258 to 261 are silent. The Europe rulebook (lines 268 to 270) requires at least one icon for its equivalent. The original offers every unscored activity, including those with no card (lines 219 to 233).
- A (Rec.): at least one card is needed. One card scores 0 and still uses up the activity. This matches the table's range.
- B: at least two cards, so only choices worth points are offered. A player can never waste an activity.
- C: any unscored activity, as in the original. The table then needs a value for 0 cards (0).

**Q7. How is the activity choice asked?**
- Sources: brief lines 269 to 270 allow changing the output. The original asks yes or no per activity in hash-map order and stops at the first yes, so a "no" cannot be taken back and the order is arbitrary (lines 230 to 247).
- A (Rec.): one question that lists the scorable activities with their points, plus "none".
- B: keep the yes and no sequence, in a fixed order.

**Q8. Who wins if the totals and the Throw and catch totals are both equal?**
- Sources: brief line 163 and rulebook lines 284 to 285 stop at the first tie-break. The original gives the win to the lowest player number (lines 412 to 417).
- A (Rec.): shared win. All still-tied players are named.
- B: seat order decides, as the original does by accident.
- C: a further tie-break of your choice, such as most region bonuses.

**Q9. Are bots kept, and how do they decide?**
- Sources: bots are not in rules 1 to 12. The run syntax in brief lines 249 to 250 has a bot count, and line 257 says "general functionality remains the same".
- A (Rec.): keep them. A bot picks a card uniformly at random. For activities, either:
  - A1: take the first one offered, even for 0 points, as the original does (lines 237 to 239); or
  - A2 (Rec.): take the offered activity worth the most points, and none if nothing scores. This is a small behaviour change.
- B: drop bots. There is less to build, but a lone tester needs two or more clients, and the run syntax changes.
- The original's bot can never pick the last card of its hand (`nextInt(size-1)`, line 88). I treat that as a bug.

**Q10. Does the server host a player, and which mixes are allowed?**
- Sources: brief lines 255 to 257 allow the server not to host a player. The original always adds a console player on the server (line 443), which breaks the count when the first argument is 0.
- A (Rec.): the server hosts nobody. Every human is a client, and the arguments mean "humans who will connect" and "bots". The cost: playing alone against bots needs a server and a client in two terminals.
- B: the server keeps one local player. Single-terminal play remains, but the server process stays tied to a console.
- Sub-question: is a game of bots only (0 humans) allowed? Rec. yes, since it gives an end-to-end run without input. The alternative is to require at least one human.

**Q11. Are the command line and port kept?**
- Sources: brief lines 248 to 253 give `[#Players] [#Bots]` for the server and `[IP]` for the client. The port is 2048 in the original (lines 428, 449).
- A (Rec.): keep the same argument shape and port, with the meaning adjusted by Q10.
- B: free redesign, such as named options or a configurable port, documented in the README.

**Q12. What happens on an invalid selection?**
- Sources: brief line 239 grades error handling. The original ignores the input, drafts nothing and fails later (lines 90 to 94, 159).
- A (Rec.): nothing changes and the same player is asked again with a message. Letters are accepted in either case, and `*` and `-` are typed as they are.
- B: a random valid card is taken after an invalid input. The game cannot stall, but the player loses control.
- C: ask again a limited number of times, then B.

**Q13. What happens when a client disappears during a game?**
- Sources: brief lines 222 to 224 put robustness in the future network change. The original swallows the error and carries on with empty input (line 62).
- A (Rec.): the game ends for everyone with a clear message.
- B: a bot takes over the seat. This is more logic and touches rules, bots and network at once.
- C: wait for a reconnect. This belongs to the future network layer.

**Q14. Spelling and site characters.**
- Sources: the card table says "New South Whales" (lines 103 to 106) and rule 10b says "New South Wales" (line 136). The original uses "Whales" and has "Wester Australia" in its region list (line 323). Site letters are "A-Z" on line 78 and "A-Ö" on line 134, while the table uses A to Z plus `*` and `-`.
- A (Rec.): "New South Wales". The valid sites are the 28 characters of the table.
- B: keep "New South Whales" verbatim from the table, consistently everywhere.

**Q15. Which scores are shown, and to whom?**
- Sources: brief lines 159 to 160 say "the score is shown to the player". In the rulebook, region bonuses are announced to the group (lines 155 to 159). The original shows each player only their own breakdown, plus the completed regions and the winner to all.
- A (Rec.): after each round, everyone sees every player's score per category and running total. Nothing is secret once the cards are revealed (Q3).
- B: each player sees only their own breakdown. Completed regions and the final result go to all, as in the original.

**Q16. Are the picks of one step simultaneous?**
- Sources: brief line 126; rulebook lines 90 to 92; the original's comment on line 348 and its reveal after all have chosen (lines 364 to 377).
- A (Rec.): a pick is shown only after all players have picked in that step.
- B: each pick is shown immediately. Slower players gain information; I see no support for this in the sources.

**Q17. What counts as a test of "shuffle" (rule 3)?**
- A (Rec.): two properties. The same 28 cards come out, and the order comes from a random source the test can fix. This is deterministic and fast.
- B: a statistical test over many shuffles. It adds little and can fail by chance.

**Q18. Requirement 13: what exactly alternates? (Future, but it fixes what must be variable.)**
- Sources: brief lines 166 to 167 say "to previous and then to the next player each round". Rulebook lines 293 to 296 say "to the left and then to the right". Rule 6 makes "next" the normal direction, so the brief's word order is the reverse of the rulebook's. Neither says what happens to the Catch.
- A (Rec.): rounds 1 and 3 are as rules 6 and 9. Rounds 2 and 4 are mirrored completely: hands go to the previous player and the Catch to the next.
- B: the brief read literally: round 1 passes to the previous player, round 2 to the next.
- C: only the hand passing alternates; the Catch always goes to the previous player.
- In all cases it is optional, off by default, and only for 3 or 4 players.

**Q19. Requirement 14: how far do we go now?**
- Sources: brief lines 70 to 72 and 165 say "you do not need to implement these".
- A (Rec.): design only. Section 6.1 of the document lists what the design must leave open.
- B: also implement one edition, or a tiny made-up edition used only in tests, as evidence for the extensibility claim. This is more work and more to explain at the oral exam. Brief lines 260 to 261 require tests for any future modification that is implemented.

**Q20. Are all departures listed in section 7 bugs to fix?**
- Sources: AGENTS.md says "No change of behaviour beyond what the requirements ask for", so each fix needs your approval as a required change. Exam question 1 (brief lines 178 to 185) asks which requirements the code fails.
- A (Rec.): yes, all of them.
- B: name any you want kept as they are.
- The list was made by reading the source. A characterisation run in the test stage would confirm each one.

