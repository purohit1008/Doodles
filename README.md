[README_FINAL_BUILD.txt](https://github.com/user-attachments/files/33830410/README_FINAL_BUILD.txt)
DOODLES FINAL BUILD
===================

Core:
- Java 21 / Swing
- TCP sockets, server authoritative
- MySQL/JDBC
- Server port 5000

Room system:
- Create public room
- Create private room with 5-character code
- Join public room from room list
- Join private room by code
- Lobby with host and player list
- Host-only Start Game
- No joining once a game starts
- Host transfer if host disconnects

Game:
- 2-8 players per room
- The host chooses the number of ROUNDS (1-10) and the DRAWING TIME (30-180s) in the lobby
- A round = every player draws once (so 4 players x 3 rounds = 12 drawings)
- The drawer is offered THREE words (from three different categories) and has 15s to pick;
  if they are too slow, one is picked for them (keys 1/2/3 also work)
- 700+ words in 17 categories (server/WordBank.java), recently used words are not repeated
- Progressive hints at 50% / 75% / 92% of the drawing time
- Server-side guess validation, speed-based scoring, drawer bonus
- When a game ends everybody returns to the lobby; the scoreboard resets to zero when the next
  game starts (a new game session is also created in the database)
- Players who already guessed chat privately with the drawer (no spoilers)

Chat extras:
- 32 vector emoji (type :) or <3 or pick from the smiley button) and 14 stickers
- Stickers pop up over the canvas for everybody and land in the chat
- All emoji/stickers are drawn in code (ui/EmoteArt.java), no image files needed

Power-ups (one per player per round, server validated):
- Drawer (target one active guesser): Blur 300, Flip 300, Spotlight 350
- Guesser: Freeze 300 (target), Hint 200, Double 250, Shield 150
- Shield blocks the next targeted power-up (the attacker still pays)
- Defined once in common/PowerUp.java (cost, duration, role)

Brushes: Pen, Marker, Crayon, Spray, Calligraphy, Neon, Rainbow, Dashed, Dotted
Tools: Eraser, Fill, Line, Rectangle, Ellipse, Undo, Clear, 20 colours + picker
Shortcuts (drawer): 1-9 brush, B/E/F/L/R/O tools, [ ] size, Ctrl+Z undo

UI: light-blue / light-pink "candy" theme (ui/Theme), vector icons (ui/Icons), animated welcome
screen with live avatar + random nickname dice, word-choice cards, round recap card, confetti,
podium results, synthesized sound effects (ui/Sfx, toggle in Settings), "Save drawing" as PNG.

Important:
1. Run the SQL schema in database/doodles_database.sql.
2. Verify database credentials in src/database/DataBaseConnection.java.
3. Run server.GameServer first.
4. Run ui.WelcomeScreen on each client.
5. Use CREATE PUBLIC / CREATE PRIVATE / JOIN ROOM / QUICK PLAY.

Network additions (client -> server): SET_SETTINGS rounds seconds, CHOOSE_WORD index, STICKER id
(server -> client): ROOM_SETTINGS, WORD_CHOICES, CHOOSING, CHOOSE_CANCEL, STICKER, CHAT_PRIVATE
Drawing network format: DRAW x y r g b size brushId | FILL x y r g b | UNDO | UP | CANVAS_CLEAR.

Rooms & dialogs (v4):
- Creating or joining a room no longer pops up a "no public rooms" box: the server only sends the
  public room list to players who are still browsing, and the client only shows it when asked.
- "Join a room > Public room" opens a live room browser (auto-refreshing, with a Create button).
- Wrong room code / full room / duplicate nickname: a friendly message, then back to the menu.
- Settings has a "Leave room" button; the game window stays hidden until you are really in a room.
- Every pop-up is now frameless and drawn by the game (ui/Dialogs.java), so nothing depends on the
  Windows theme or accent colour any more. Includes a themed colour picker (ui/ColorPickerPanel.java).
