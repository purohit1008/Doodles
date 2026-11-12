# Doodles - Real-time Multiplayer Drawing & Guessing Game

A Java Swing game where players take turns drawing a secret word while the others guess it in
the chat. Built for the **Advanced Programming Practice** course.

- **Language / UI:** Java 21, Swing
- **Networking:** TCP sockets, authoritative server (clients are never trusted for scores, roles or power-ups)
- **Database:** MySQL via JDBC (players, game sessions, drawings, chat)
- **Players:** 2 to 8 per room

## Features

- **Rooms:** quick play, public rooms, private rooms with a 5-character code, lobby with host controls, host transfer if the host leaves
- **Turns:** the drawer changes after every turn; every player draws once per round; host picks rounds (1-10) and drawing time (30-180 s)
- **Words:** 700+ words in 17 categories; the drawer picks from 3 choices; progressive hints during the round
- **Scoring:** speed-based points, drawer bonus, a scoreboard for each round plus an overall leaderboard next to the chat, podium at the end
- **Chat:** guess/chat box with 32 emoji, 14 stickers and custom messages
- **Power-ups (unique feature):** spend points to attack others or help yourself
  - Drawer: Blur, Flip, Spotlight
  - Guesser: Freeze, Hint, Double, Shield
- **Drawing tools:** 9 brushes, eraser, fill, line, rectangle, ellipse, undo, clear, colour picker, save drawing as PNG
- **UI:** playful, colourful "candy" theme, animations, confetti, synthesized sound effects

## Project structure

```
src/
  client/    game screen, drawing panel, tool dock, game window
  server/    GameServer (rooms, rounds, scoring, power-ups), WordBank
  common/    shared classes: Config, Player, Message, PowerUp, Emotes, DrawingCommand
  database/  JDBC access: connection, player / game / drawing / chat DAOs
  ui/        welcome screen, lobby, dialogs, leaderboard, power shop, theme, icons
database/    doodles_database.sql  (schema)
lib/         mysql-connector-j-26.10.0.jar  (JDBC driver)
screenshots/ project output
```

## Requirements

- JDK 21 or newer
- MySQL Server 8+ (running on `localhost:3306`)
- The MySQL driver jar is already in `lib/`

## Setup and execution

### 1. Create the database

```bash
mysql -u root -p < database/doodles_database.sql
```

The code expects MySQL user `root` with password `Doodles@123`. To use a different password, either
edit `src/database/DataBaseConnection.java` or pass `-Ddoodles.db.password=YOURPASSWORD` to the server.

### 2. Start the server (first)

```bash
./run-server.sh          # macOS / Linux
run-server.bat           # Windows
```

Or in IntelliJ: run `server.GameServer`. The console prints `Database: READY` when MySQL is connected.
If the database is not available the game still works, but nothing is saved.

### 3. Start the game (one per player)

```bash
./run-client.sh          # macOS / Linux
run-client.bat           # Windows
```

Or in IntelliJ: run `ui.WelcomeScreen`. Start it once per player, then use **Quick Play**,
**Create Public**, **Create Private** or **Join a Room**. The host starts the game once 2+ players are in the lobby.

### Playing on two computers

Start the clients with `-Ddoodles.host=<IP of the server computer>`. The default port is `5051`;
change it with `-Ddoodles.port=NNNN` on the server and every client.

## Troubleshooting

| Message | Fix |
|---|---|
| Could not connect to the Doodles server | Start `GameServer` first |
| No answer from the server | Another program uses the port; change it with `-Ddoodles.port` |
| MySQL driver is not on the classpath | Make sure the jar is in `lib/` (IntelliJ: right-click `lib` > Add as Library) |
| wrong MySQL user or password | Fix the password as described in step 1 |
| database / table is missing | Run `database/doodles_database.sql` |

## Screenshots

| Screen | Image |
|---|---|
| Welcome screen | ![Welcome](screenshots/01-welcome.png) |
| Lobby with room code | ![Lobby](screenshots/02-lobby.png) |
| Drawing round | ![Drawing](screenshots/03-drawing.png) |
| Guessing, chat and emoji | ![Chat](screenshots/04-chat.png) |
| Power-ups | ![Power-ups](screenshots/05-powerups.png) |
| Scoreboard and results | ![Results](screenshots/06-results.png) |
| Server console (database ready) | ![Server](screenshots/07-server-console.png) |
| MySQL tables with saved data | ![Database](screenshots/08-database.png) |

## Team

Name: Krsna - GitHub: [purohit1008](https://github.com/purohit1008)
