# The recording in the README

`docs/demo.gif` shows the first round of a game at a real terminal: the logo, the map as a board, hands and
drafts as cards, the score of the round. Nobody types: `DemoGame.kt` in the test sources plays a console
against two bots in one process, without the network, and gives the answers itself after a pause. The deal
and the bots are seeded, so every run shows the same game.

## Make it again

Needs [asciinema](https://asciinema.org) (records what a program prints in a terminal) and
[agg](https://github.com/asciinema/agg) (turns such a recording into a GIF).

```sh
./gradlew testClasses installDist
asciinema rec --overwrite --headless --window-size 86x46 \
  --command "java -cp 'app/build/classes/kotlin/test:app/build/install/app/lib/*' boomerang.support.DemoGameKt 46 1300" \
  /tmp/demo.cast
agg --font-size 13 --fps-cap 6 --idle-time-limit 2 --last-frame-duration 4 \
  --font-family "JetBrainsMono Nerd Font,JetBrains Mono,Apple Braille" /tmp/demo.cast docs/demo.gif
```

The two numbers behind `DemoGameKt` are the height of the terminal in lines (the same as in
`--window-size`) and the pause before an answer in milliseconds. The font is JetBrains Mono, which must be
installed; Apple Braille, a font of macOS, has the braille characters of the logo. Elsewhere name a
fixed-width font and one that has those characters.

## See it live

The same without a recording, in a terminal at least 86 columns wide and 46 lines high:

```sh
java -cp 'app/build/classes/kotlin/test:app/build/install/app/lib/*' boomerang.support.DemoGameKt 46 1300
```
