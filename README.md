# Minute Rush

Minute Rush is a Fabric 26.3 minigame. Everyone receives the same randomly selected task, and the first player to complete it wins the round. The first player to reach 10 round wins takes the match. A round times out after 60 seconds and immediately starts a new task.

## Running

1. Install Java 25.
2. Run `./gradlew build`.
3. Use the generated Fabric server/client run configuration.

Anyone can use:

- `/minute-rush config` - open the settings book: difficulty, PvP, points to win and time per round
- `/minute-rush config score <value>` - set the points needed to win
- `/minute-rush config duration <seconds>` - set the time per round
- `/minute-rush config difficulty <easy|medium|hard|progressive>` - set the match difficulty
- `/minute-rush config pvp <on|off>` - on: players can hurt each other all match; off: only during tasks that target another player
- `/minute-rush status` - inspect match state
- `/voteskip` - vote to skip the current round

Settings changes are announced to everyone. Settings are locked for everyone, operators included, while a match is running; the book still opens to show them.

Operators can also use:

- `/minute-rush start` - start a match (requires at least two players)
- `/minute-rush solo-start` - start an isolated one-player testing match in a singleplayer world
- `/minute-rush solo-exit` - stop singleplayer testing and reset its score
- `/minute-rush skip` - skip the current round (useful while testing)
- `/minute-rush test <basic|movement|crafting|combat|interactive|death|quickfire|maths>` - cycle through a task category
- `/minute-rush test skip` - skip the current category-test task
- `/minute-rush reset` - reset the match and scores

The task catalog now covers placing, collecting, mining, jumping, traveling, sprinting, swimming, walking, repeated jumps, hold-item actions, player interaction, mob interaction, and interactable-block interaction. The game avoids the previous three task kinds when the pool has alternatives, so the same task family does not repeat immediately. Singleplayer deliberately excludes the player-player task because it has no second participant; the other task families use the same round lifecycle as multiplayer. Placement tasks no longer require coordinates: place the requested block anywhere, and the game detects the placement from your item-use stat. Tasks use per-player baselines so pre-existing inventory/stat progress does not falsely complete a round. Settings default to progressive difficulty, PvP on, 10 points and 60 seconds, and can be changed from the settings book or the config commands.

Task selection combines each catalog rarity with its action multiplier: crafting 1.50, movement/exploration 1.40, interactive/combat 1.25, mine/place/collect 0.80, and hold 0.40. Collection and mining tasks no longer grant starter items; players must obtain the required resources normally.

Quickfire and maths answers are sent as ordinary chat messages. Generated letter and alphanumeric targets ignore letter case; maths division accepts equivalent decimal forms, not fractions. Death tasks enable keep-inventory while active and for the configured grace period afterward.

Wood crafting includes both any-type and specific-type tasks. Poplar and shelf mushroom tasks require a Dappled Forest in the arena; pale oak requires a Pale Garden. The overworld arena survey cannot establish Nether access, so specific crimson and warped tasks remain available in category tests but are excluded from normal selection. The warden and breeze tasks require a predicted ancient city or trial chambers inside the border, respectively. As with village prediction, the seed-based survey does not guarantee that the structure actually generated; the warden check is conservative and does not detect every deep-dark area.

Each task also has a difficulty based on what it needs: **Simple** (no items, e.g. walking or hitting a mob), **Easy** (wood, stone, copper and common drops), **Medium** (iron, gold, redstone, string, and rare drops such as apples) and **Hard** (diamonds and emeralds).

The match difficulty (`/minute-rush config difficulty <mode>`, default `progressive`) sets how often each tier comes up:

| Mode | Simple + Easy | Medium | Hard |
| --- | --- | --- | --- |
| `easy` | 60% | 30% | 10% |
| `medium` | 40% | 40% | 20% |
| `hard` | 30% | 45% | 25% |

`progressive` plays as `easy` for 5 minutes or 3N completed tasks, then `medium` for 7 minutes or 4N completed tasks (whichever comes first each time), then `hard` for the rest of the match. In every mode tasks are Simple or Easy only for 5 minutes or until 3N have been completed (whichever comes first), so players can gather resources first. A task counts as completed when someone finishes its round; rounds that time out or are skipped don't count. N is the number of players in the match: spectators are not counted, but players who disconnect and can still rejoin are. If a tier has no task possible in the arena, the other tiers share its chance.

Every round begins with a short countdown. The task is shown in colored chat/title text, and the round winner and score are shown the same way without title fade-in. A sidebar scoreboard displays the current task, seconds remaining, and each player's match score. A bossbar mirrors the current task and timer. New rounds, countdowns, round wins, match wins, and timeouts provide sound feedback. Mob tasks accept either right-click/use or left-click/attack; the wording says "Use or hit any mob" to make that behavior explicit. `/minute-rush solo-start` uses this same countdown, task announcement, timeout, scoring, and reset lifecycle; it only relaxes the normal two-player start requirement.

Sound effects are sent directly to every active participant using vanilla Minecraft sound events and the master sound channel. Players must have their master volume enabled; client-side sound settings, mute controls, accessibility settings, or resource packs can still suppress vanilla sounds.

Singleplayer testing is explicit and does not change normal multiplayer rules. Run `/minute-rush solo-start` as the player, test the tasks normally, and use `/minute-rush solo-exit` to stop. A regular `/minute-rush start` still requires at least two players.

For parity with the older mod, the most useful input is its source repository or a zip containing source, `fabric.mod.json`, Gradle files, resources, data/config files, and any test world. If source is unavailable, provide a decompiled source archive plus configs and screenshots or a short video showing the intended round flow and task behavior. The JAR alone is enough for bytecode inspection, but it cannot reliably recover all labels, resource-driven task data, or intended UX.
