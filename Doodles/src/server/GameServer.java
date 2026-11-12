package server;

import database.ChatMessageDAO;
import database.DataBaseConnection;
import database.DrawingDAO;
import database.GameSessionDAO;
import common.Config;
import common.Emotes;
import common.PowerUp;
import database.PlayerDAO;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOODLES authoritative multiplayer server.
 *
 * Protocol is line based. The server owns room membership, rounds, scores,
 * word selection, guesses and power-up validation. Clients are never trusted
 * for score/role/target validation.
 */
public class GameServer {

    private static final int PORT = Config.PORT;
    private static final int DEFAULT_ROUNDS = 3;
    private static final int DEFAULT_ROUND_SECONDS = 60;
    private static final int MAX_ROUNDS_LIMIT = 10;
    private static final int MIN_SECONDS = 30, MAX_SECONDS = 180;
    private static final int CHOOSE_SECONDS = 15;     // time the drawer has to pick a word
    private static final int WORD_CHOICES = 3;
    private static final long RECAP_MILLIS = 4800;    // pause between turns (round recap on the clients)
    private static final int DEFAULT_MAX_PLAYERS = 8;
    private static final int MIN_PLAYERS = 2;

    private static final Random RANDOM = new Random();
    private static final AtomicInteger PLAYER_COUNTER = new AtomicInteger();

    private static final Map<Socket, PrintWriter> OUTPUTS = new ConcurrentHashMap<>();
    private static final Map<Socket, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<Socket, Integer> DB_IDS = new ConcurrentHashMap<>();
    private static final Map<Socket, Integer> PLAYER_IDS = new ConcurrentHashMap<>();
    private static final Map<Socket, Long> LAST_STICKER = new ConcurrentHashMap<>();
    private static final Map<Socket, Room> PLAYER_ROOMS = new ConcurrentHashMap<>();
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4);

    private static final class Room {
        final String code;
        final boolean privateRoom;
        final int maxPlayers;
        volatile int gameId;
        final Set<Socket> members = ConcurrentHashMap.newKeySet();
        final Map<Socket, Integer> scores = new ConcurrentHashMap<>();
        final Set<Socket> guessedThisRound = ConcurrentHashMap.newKeySet();
        final Map<Socket, Long> blurUntil = new ConcurrentHashMap<>();
        final Map<Socket, Long> freezeUntil = new ConcurrentHashMap<>();
        final Map<Socket, Long> mirrorUntil = new ConcurrentHashMap<>();
        final Map<Socket, Long> spotlightUntil = new ConcurrentHashMap<>();
        final Set<Socket> shielded = ConcurrentHashMap.newKeySet();
        final Set<Socket> doubleNext = ConcurrentHashMap.newKeySet();
        final Map<Socket, Integer> extraHints = new ConcurrentHashMap<>();
        final Map<Socket, Boolean> powerUsed = new ConcurrentHashMap<>(); // one power-up per player per round
        final Map<Socket, Integer> hintPositions = new ConcurrentHashMap<>(); // unused compatibility holder

        volatile int hostPlayerId;
        volatile int totalRounds = DEFAULT_ROUNDS;
        volatile int roundSeconds = DEFAULT_ROUND_SECONDS;
        volatile int gamesPlayed;
        volatile boolean choosing;
        volatile long chooseEnd;
        volatile List<WordBank.Choice> choices;
        final Deque<Socket> turnQueue = new java.util.concurrent.ConcurrentLinkedDeque<>();
        final LinkedHashSet<String> recentWords = new LinkedHashSet<>();
        volatile boolean started;
        volatile boolean roundActive;
        volatile int round;
        volatile Socket drawer;
        volatile String category = "";
        volatile String word = "";
        volatile int hintLevel;
        volatile long roundEnd;
        volatile long nextRoundAt;
        volatile int lastHintBroadcastLevel = -1;
        final List<Integer> revealPositions = new ArrayList<>();

        Room(String code, boolean privateRoom, int maxPlayers, int gameId) {
            this.code = code;
            this.privateRoom = privateRoom;
            this.maxPlayers = maxPlayers;
            this.gameId = gameId;
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("       DOODLES GAME SERVER");
        System.out.println("=================================");
        System.out.println("Port: " + PORT);

        // Report the database state right away instead of waiting for the first player to find out.
        if (DataBaseConnection.selfTest()) {
            System.out.println("Database: READY - players, chat and drawings will be saved.");
        } else {
            System.out.println("Database: NOT AVAILABLE - the game still works, but nothing is saved to MySQL.");
            System.out.println("          Fix the problem above and restart the server to enable saving.");
        }

        Thread manager = new Thread(GameServer::roomLoop, "Doodles-RoomManager");
        manager.setDaemon(true);
        manager.start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for players on port " + PORT + "...");
            while (true) {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> connectPlayer(socket), "Doodles-Client-" + socket.getPort());
                t.setDaemon(true);
                t.start();
            }
        } catch (java.net.BindException e) {
            System.out.println("ERROR: port " + PORT + " is already in use.");
            System.out.println("       Another GameServer is probably still running (stop it with the red square),");
            System.out.println("       or another program uses this port. Change it with -Ddoodles.port=NNNN on the server AND the clients.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SCHEDULER.shutdownNow();
        }
    }

    private static void connectPlayer(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            String first = in.readLine();
            if (first == null || !first.startsWith("NAME ")) {
                out.println("ERROR Player name required.");
                socket.close();
                return;
            }

            String name = first.substring(5).trim();
            if (name.isEmpty() || name.length() > 20 || name.contains("|")) {
                out.println("ERROR Invalid player name.");
                socket.close();
                return;
            }

            // -1 means "no database record": the player can still play, nothing is saved for them
            int dbId = PlayerDAO.getOrCreatePlayer(name);

            int playerId = PLAYER_COUNTER.incrementAndGet();
            OUTPUTS.put(socket, out);
            NAMES.put(socket, name);
            DB_IDS.put(socket, dbId);
            PLAYER_IDS.put(socket, playerId);
            System.out.println("Player connected: " + name + " (#" + playerId + ")");

            out.println("PLAYER " + playerId);
            out.println("NAME " + name);
            out.println("SCORE 0");
            out.println("POWERSTATUS AVAILABLE");
            out.println("SERVER READY");
            handlePlayer(socket, in);
        } catch (IOException e) {
            // Connection failed or closed before the handshake completed.
        } finally {
            removePlayer(socket);
        }
    }

    private static void handlePlayer(Socket socket, BufferedReader in) throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (!message.trim().isEmpty()) processMessage(socket, message.trim());
        }
    }

    private static void processMessage(Socket socket, String message) {
        if (!OUTPUTS.containsKey(socket)) return;

        try {
            if (message.equals("LIST_ROOMS")) {
                sendPublicRooms(socket);
                return;
            }
            if (message.startsWith("CREATE_ROOM ")) {
                String[] p = message.split("\\s+");
                boolean privateRoom = p.length >= 2 && p[1].equalsIgnoreCase("PRIVATE");
                int max = parseInt(p.length >= 3 ? p[2] : "8", DEFAULT_MAX_PLAYERS);
                createRoom(socket, privateRoom, max);
                return;
            }
            if (message.startsWith("JOIN_ROOM ")) {
                joinRoom(socket, message.substring(10).trim().toUpperCase(Locale.ROOT));
                return;
            }
            if (message.equals("QUICK_PLAY")) {
                joinOrCreatePublic(socket);
                return;
            }
            if (message.equals("START_GAME")) {
                startGame(socket);
                return;
            }

            Room room = PLAYER_ROOMS.get(socket);
            if (room == null) {
                OUTPUTS.get(socket).println("ERROR Join or create a room first.");
                return;
            }

            if (message.startsWith("DRAW ") || message.startsWith("FILL ")) {
                if (room.roundActive && socket == room.drawer) {
                    int dbId = DB_IDS.getOrDefault(socket, -1);
                    if (dbId != -1) DrawingDAO.saveDrawing(room.gameId, dbId, message);
                    broadcast(room, message, socket);
                }
                return;
            }

            if (message.equals("UP") || message.equals("UNDO") || message.equals("CANVAS_CLEAR")) {
                if (room.roundActive && socket == room.drawer) {
                    int dbId = DB_IDS.getOrDefault(socket, -1);
                    if (dbId != -1 && message.equals("UP")) DrawingDAO.saveDrawing(room.gameId, dbId, "UP");
                    broadcast(room, message, socket);
                }
                return;
            }

            if (message.startsWith("CHAT ")) {
                handleChat(socket, room, message.substring(5).trim());
                return;
            }

            if (message.startsWith("STICKER ")) {
                handleSticker(socket, room, message.substring(8).trim());
                return;
            }

            if (message.startsWith("SET_SETTINGS ")) {
                handleSettings(socket, room, message.substring(13).trim());
                return;
            }

            if (message.startsWith("CHOOSE_WORD ")) {
                handleChooseWord(socket, room, parseInt(message.substring(12).trim(), -1));
                return;
            }

            if (message.startsWith("BUY_POWER ")) {
                handlePower(socket, room, message.substring(10).trim());
                return;
            }
        } catch (Exception e) {
            PrintWriter out = OUTPUTS.get(socket);
            if (out != null) out.println("ERROR Server rejected that action.");
            e.printStackTrace();
        }
    }

    private static void createRoom(Socket socket, boolean privateRoom, int maxPlayers) {
        if (PLAYER_ROOMS.containsKey(socket)) {
            OUTPUTS.get(socket).println("ERROR You are already in a room.");
            return;
        }
        maxPlayers = Math.max(2, Math.min(8, maxPlayers));
        String code;
        do code = generateRoomCode(); while (ROOMS.containsKey(code));

        int gameId = GameSessionDAO.createGame();      // -1 = database unavailable, the game just isn't recorded

        Room room = new Room(code, privateRoom, maxPlayers, gameId);
        room.hostPlayerId = PLAYER_IDS.get(socket);
        room.members.add(socket);
        room.scores.put(socket, 0);
        room.powerUsed.put(socket, false);
        ROOMS.put(code, room);
        PLAYER_ROOMS.put(socket, room);

        send(socket, "ROOM_CREATED " + code + " " + (privateRoom ? "PRIVATE" : "PUBLIC") + " " + maxPlayers);
        sendRoomLobby(room);
        sendPublicRoomsToAll();
    }

    private static void joinRoom(Socket socket, String code) {
        if (code.isEmpty()) {
            OUTPUTS.get(socket).println("ERROR Room code required.");
            return;
        }
        Room room = ROOMS.get(code);
        if (room == null) {
            OUTPUTS.get(socket).println("ERROR Room not found.");
            return;
        }
        if (room.started) {
            OUTPUTS.get(socket).println("ERROR That game has already started.");
            return;
        }
        if (room.members.size() >= room.maxPlayers) {
            OUTPUTS.get(socket).println("ERROR Room is full.");
            return;
        }
        if (PLAYER_ROOMS.containsKey(socket)) {
            OUTPUTS.get(socket).println("ERROR You are already in a room.");
            return;
        }
        for (Socket member : room.members) {
            if (NAMES.getOrDefault(member, "").equalsIgnoreCase(NAMES.getOrDefault(socket, ""))) {
                OUTPUTS.get(socket).println("ERROR That nickname is already in the room.");
                return;
            }
        }

        room.members.add(socket);
        room.scores.put(socket, 0);
        room.powerUsed.put(socket, false);
        PLAYER_ROOMS.put(socket, room);
        send(socket, "ROOM_JOINED " + room.code + " " + (room.privateRoom ? "PRIVATE" : "PUBLIC") + " " + room.maxPlayers);
        sendRoomLobby(room);
        broadcast(room, "SYSTEM " + safeName(socket) + " joined the room.", null);
        sendPublicRoomsToAll();
    }

    private static void joinOrCreatePublic(Socket socket) {
        if (PLAYER_ROOMS.containsKey(socket)) {
            send(socket, "ERROR You are already in a room.");
            return;
        }
        Room best = ROOMS.values().stream()
                .filter(r -> !r.privateRoom && !r.started && r.members.size() < r.maxPlayers)
                .min(Comparator.comparingInt(r -> r.members.size()))
                .orElse(null);
        if (best == null) createRoom(socket, false, 8);
        else joinRoom(socket, best.code);
    }

    private static void startGame(Socket socket) {
        Room room = PLAYER_ROOMS.get(socket);
        if (room == null) { send(socket, "ERROR Create or join a room first."); return; }
        if (PLAYER_IDS.get(socket) != room.hostPlayerId) { send(socket, "ERROR Only the host can start the game."); return; }
        if (room.members.size() < MIN_PLAYERS) { send(socket, "ERROR At least 2 players are required."); return; }
        synchronized (GameServer.class) {
            if (room.started) return;
            if (room.gamesPlayed > 0) {
                room.gameId = GameSessionDAO.createGame();   // every game gets its own session record (-1 = not recorded)
            }
            room.started = true;
            room.round = 0;
            room.drawer = null;
            room.roundActive = false;
            room.choosing = false;
            room.turnQueue.clear();
            room.recentWords.clear();
            room.guessedThisRound.clear();
            room.shielded.clear();
            room.doubleNext.clear();
            room.extraHints.clear();
            room.nextRoundAt = System.currentTimeMillis() + 1800;
            for (Socket member : room.members) {
                room.scores.put(member, 0);                  // the scoreboard starts from zero in every new game
                room.powerUsed.put(member, false);
                room.blurUntil.remove(member);
                room.freezeUntil.remove(member);
                room.mirrorUntil.remove(member);
                room.spotlightUntil.remove(member);
            }
        }
        broadcast(room, "GAME_START", null);
        for (Socket member : room.members) send(member, "SCORE 0");
        broadcastScoreboard(room);
        sendRoomLobby(room);
        broadcast(room, "SYSTEM New game: " + room.totalRounds + " rounds, " + room.roundSeconds + "s per drawing.", null);
    }

    private static void handleSettings(Socket socket, Room room, String data) {
        if (PLAYER_IDS.get(socket) != room.hostPlayerId) { send(socket, "ERROR Only the host can change the settings."); return; }
        if (room.started) { send(socket, "ERROR Settings are locked while a game is running."); return; }
        String[] p = data.split("\\s+");
        room.totalRounds = Math.max(1, Math.min(MAX_ROUNDS_LIMIT, parseInt(p.length > 0 ? p[0] : "", room.totalRounds)));
        room.roundSeconds = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, parseInt(p.length > 1 ? p[1] : "", room.roundSeconds)));
        broadcast(room, "ROOM_SETTINGS " + room.totalRounds + " " + room.roundSeconds, null);
    }

    private static void handleSticker(Socket socket, Room room, String id) {
        if (!Emotes.isSticker(id)) return;
        long now = System.currentTimeMillis();
        Long last = LAST_STICKER.get(socket);
        if (last != null && now - last < 1500) return;       // simple flood control
        LAST_STICKER.put(socket, now);
        broadcast(room, "STICKER " + safeName(socket) + " " + id, null);
    }

    private static void handleChooseWord(Socket socket, Room room, int index) {
        synchronized (GameServer.class) {
            if (!room.choosing || socket != room.drawer) return;
            List<WordBank.Choice> list = room.choices;
            if (list == null || index < 0 || index >= list.size()) return;
            beginRound(room, list.get(index));
        }
    }

    private static void handleChat(Socket socket, Room room, String raw) {
        String text = raw.replaceAll("\\p{Cntrl}", " ").trim();
        if (text.isEmpty()) return;
        if (text.length() > 120) text = text.substring(0, 120);
        if (room.freezeUntil.getOrDefault(socket, 0L) > System.currentTimeMillis()) {
            send(socket, "POWER_ERROR You are frozen and cannot submit a guess yet.");
            return;
        }
        int dbId = DB_IDS.getOrDefault(socket, -1);
        if (dbId != -1) ChatMessageDAO.saveMessage(room.gameId, dbId, text);

        String guessText = Emotes.stripTokens(text);
        boolean correct = room.roundActive && socket != room.drawer && !guessText.isEmpty() && checkGuess(socket, room, guessText);
        if (correct) return;

        String line = safeName(socket) + ": " + text;
        boolean knowsWord = room.roundActive && (socket == room.drawer || room.guessedThisRound.contains(socket));
        if (knowsWord) {
            // players who already know the word chat among themselves so nobody can spoil it
            for (Socket m : room.members) {
                if (m == room.drawer || room.guessedThisRound.contains(m)) send(m, "CHAT_PRIVATE " + line);
            }
        } else {
            broadcast(room, "CHAT " + line, null);
        }
    }

    private static synchronized boolean checkGuess(Socket socket, Room room, String guess) {
        if (!room.roundActive || socket == room.drawer || room.guessedThisRound.contains(socket)) return false;
        if (!normalize(guess).equals(normalize(room.word))) return false;

        int secondsLeft = secondsRemaining(room);
        int points = Math.max(100, Math.min(500, 100 + (int)Math.round(400.0 * secondsLeft / room.roundSeconds)));
        boolean doubled = room.doubleNext.remove(socket);
        if (doubled) points *= 2;
        room.guessedThisRound.add(socket);
        room.scores.merge(socket, points, Integer::sum);
        if (room.drawer != null) room.scores.merge(room.drawer, 100, Integer::sum);

        String name = safeName(socket);
        broadcast(room, "CORRECT " + name + " " + points, null);
        broadcast(room, "CHAT 🎯 " + name + " guessed correctly! +" + points + " points" + (doubled ? " (DOUBLE!)" : ""), null);
        send(room.drawer, "CHAT 🎨 You earned +100 drawer points!");
        broadcastScoreboard(room);
        broadcastPlayers(room);
        send(socket, "HINT " + room.word);

        if (allGuessersFinished(room)) endRound(room, "Everyone guessed the word!");
        return true;
    }

    private static void handlePower(Socket socket, Room room, String data) {
        String[] p = data.split("\\s+");
        PowerUp power = PowerUp.parse(p.length > 0 ? p[0] : null);
        if (power == null) { send(socket, "POWER_ERROR Unknown power-up."); return; }
        long now = System.currentTimeMillis();

        if (!room.started || !room.roundActive) { send(socket, "POWER_ERROR Power-ups are available only during a round."); return; }
        if (room.freezeUntil.getOrDefault(socket, 0L) > now) { send(socket, "POWER_ERROR You are frozen."); return; }
        if (Boolean.TRUE.equals(room.powerUsed.get(socket))) { send(socket, "POWER_ERROR You already used a power-up this round."); return; }
        if (room.scores.getOrDefault(socket, 0) < power.cost()) { send(socket, "POWER_ERROR You need " + power.cost() + " points for " + power.label() + "."); return; }

        boolean drawer = socket == room.drawer;
        if (power.who() == PowerUp.Who.DRAWER && !drawer) { send(socket, "POWER_ERROR " + power.label() + " can only be used by the drawer."); return; }
        if (power.who() == PowerUp.Who.GUESSER && drawer) { send(socket, "POWER_ERROR " + power.label() + " can only be used by guessers."); return; }

        Socket target = null;
        if (power.isTargeted()) {
            int targetId = p.length > 1 ? parseInt(p[1], -1) : -1;
            target = findByPlayerId(room, targetId);
            if (target == null || target == socket) { send(socket, "POWER_ERROR Invalid target."); return; }
            if (target == room.drawer || room.guessedThisRound.contains(target)) { send(socket, "POWER_ERROR Target must be an active guesser."); return; }
        } else {
            // self powers are only useful to a guesser who is still guessing
            if (room.guessedThisRound.contains(socket)) { send(socket, "POWER_ERROR You already guessed the word."); return; }
            if (power == PowerUp.HINT && room.extraHints.getOrDefault(socket, 0) >= 3) { send(socket, "POWER_ERROR No more letters to reveal."); return; }
            if (power == PowerUp.DOUBLE && room.doubleNext.contains(socket)) { send(socket, "POWER_ERROR Double is already active."); return; }
            if (power == PowerUp.SHIELD && room.shielded.contains(socket)) { send(socket, "POWER_ERROR Shield is already active."); return; }
        }
        activatePower(room, socket, target, power);
    }

    private static void activatePower(Room room, Socket user, Socket target, PowerUp power) {
        room.scores.merge(user, -power.cost(), Integer::sum);
        room.powerUsed.put(user, true);
        String userName = safeName(user);

        if (power.isTargeted()) {
            String targetName = safeName(target);
            if (room.shielded.remove(target)) {
                // the shield soaks it up: points are spent, no effect
                send(user, "POWER_USED " + power.name() + " " + targetName + " 0");
                send(user, "SHIELD_BLOCK_USER " + targetName);
                send(target, "SHIELD_BLOCK_TARGET " + userName);
                broadcast(room, "CHAT 🛡 " + targetName + "'s shield blocked " + userName + "'s " + power.label() + "!", null);
            } else {
                long until = System.currentTimeMillis() + power.seconds() * 1000L;
                switch (power) {
                    case BLUR -> room.blurUntil.put(target, until);
                    case MIRROR -> room.mirrorUntil.put(target, until);
                    case SPOTLIGHT -> room.spotlightUntil.put(target, until);
                    case FREEZE -> room.freezeUntil.put(target, until);
                    default -> { }
                }
                send(user, "POWER_USED " + power.name() + " " + targetName + " " + power.seconds());
                send(target, "EFFECT " + power.name() + " " + power.seconds());
                broadcast(room, "CHAT ⚡ " + userName + " used " + power.label() + " on " + targetName + "!", null);
            }
        } else {
            switch (power) {
                case HINT -> {
                    int extra = room.extraHints.merge(user, 2, Integer::sum);
                    room.extraHints.put(user, Math.min(extra, room.revealPositions.size()));
                    send(user, "HINT " + createHintCount(room, baseRevealCount(room) + room.extraHints.get(user)));
                    send(user, "POWER_USED HINT you 0");
                    send(user, "CHAT 💡 Extra letters revealed just for you.");
                }
                case DOUBLE -> {
                    room.doubleNext.add(user);
                    send(user, "POWER_USED DOUBLE you 0");
                    send(user, "CHAT Double armed: your next correct guess is worth 2x points!");
                }
                case SHIELD -> {
                    room.shielded.add(user);
                    send(user, "POWER_USED SHIELD you 0");
                    send(user, "CHAT 🛡 Shield up: the next power-up aimed at you is blocked.");
                }
                default -> { }
            }
        }
        broadcastScoreboard(room);
        broadcastPlayers(room);
        send(user, "POWERSTATUS USED");
        sendPowerState(room, user);
    }

    /** Tells one player which self-powers are currently armed (shown in the UI). */
    private static void sendPowerState(Room room, Socket s) {
        send(s, "POWERSTATE " + (room.shielded.contains(s) ? "SHIELD " : "") + (room.doubleNext.contains(s) ? "DOUBLE" : ""));
    }

    private static Socket findByPlayerId(Room room, int id) {
        for (Socket s : room.members) if (PLAYER_IDS.getOrDefault(s, -1) == id) return s;
        return null;
    }

    private static void roomLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            long now = System.currentTimeMillis();
            for (Room room : new ArrayList<>(ROOMS.values())) {
                try {
                    expireEffects(room, now);
                    if (!room.started) continue;
                    if (room.members.size() < MIN_PLAYERS) {
                        synchronized (GameServer.class) {
                            if (room.started) {
                                broadcast(room, "SYSTEM Not enough players left - the game is over.", null);
                                finishGame(room);
                            }
                        }
                        continue;
                    }
                    if (room.choosing) {
                        if (now >= room.chooseEnd) autoPickWord(room);
                    } else if (!room.roundActive) {
                        if (now >= room.nextRoundAt) startRound(room);
                    } else if (now >= room.roundEnd) {
                        endRound(room, "Time is up!");
                    } else {
                        updateHints(room);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /** Next drawing turn: pick the drawer and offer them three words. A round = everybody draws once. */
    private static synchronized void startRound(Room room) {
        if (!room.started || room.roundActive || room.choosing || room.members.size() < MIN_PLAYERS) return;
        List<Socket> connected = new ArrayList<>(room.members);
        connected.removeIf(s -> !OUTPUTS.containsKey(s));
        connected.sort(Comparator.comparingInt(s -> PLAYER_IDS.getOrDefault(s, Integer.MAX_VALUE)));
        if (connected.size() < MIN_PLAYERS) return;

        room.turnQueue.removeIf(s -> !connected.contains(s));
        if (room.turnQueue.isEmpty()) {
            if (room.round >= room.totalRounds) {            // every round has been played
                finishGame(room);
                return;
            }
            room.round++;
            room.turnQueue.addAll(connected);
        }
        room.drawer = room.turnQueue.poll();

        List<WordBank.Choice> choices = WordBank.pick(WORD_CHOICES, RANDOM, room.recentWords);
        room.choices = choices;
        room.choosing = true;
        room.chooseEnd = System.currentTimeMillis() + CHOOSE_SECONDS * 1000L;
        room.word = "";
        room.category = "";
        room.guessedThisRound.clear();
        room.hintLevel = 0;
        room.lastHintBroadcastLevel = -1;
        room.revealPositions.clear();

        room.shielded.clear();
        room.doubleNext.clear();
        room.extraHints.clear();
        for (Socket s : connected) {
            room.blurUntil.remove(s);
            room.freezeUntil.remove(s);
            room.mirrorUntil.remove(s);
            room.spotlightUntil.remove(s);
            room.powerUsed.put(s, false);
            send(s, "EFFECT_CLEAR");
            send(s, "POWERSTATE ");
        }
        broadcast(room, "CLEAR", null);
        broadcast(room, "ROUNDS " + room.round + " " + room.totalRounds, null);
        broadcast(room, "DRAWER " + safeName(room.drawer), null);

        StringBuilder offer = new StringBuilder("WORD_CHOICES ").append(CHOOSE_SECONDS);
        for (WordBank.Choice c : choices) offer.append('|').append(c.category).append(':').append(c.word).append(':').append(WordBank.difficulty(c.word));
        for (Socket s : connected) {
            if (s == room.drawer) {
                send(s, "ROLE DRAWER");
                send(s, offer.toString());
            } else {
                send(s, "ROLE GUESSER");
                send(s, "CHOOSING " + escape(safeName(room.drawer)) + " " + CHOOSE_SECONDS);
            }
            send(s, "POWERSTATUS " + (Boolean.TRUE.equals(room.powerUsed.get(s)) ? "USED" : "AVAILABLE"));
        }
        broadcastPlayers(room);
        broadcastScoreboard(room);
        broadcast(room, "SYSTEM Round " + room.round + " of " + room.totalRounds + ": " + safeName(room.drawer) + " is choosing a word.", null);
    }

    private static synchronized void autoPickWord(Room room) {
        List<WordBank.Choice> list = room.choices;
        if (!room.choosing || list == null || list.isEmpty()) return;
        send(room.drawer, "CHAT Too slow - a word was picked for you.");
        beginRound(room, list.get(RANDOM.nextInt(list.size())));
    }

    /** The drawer has chosen (or the timer picked for them): the guessing clock starts now. */
    private static synchronized void beginRound(Room room, WordBank.Choice choice) {
        if (!room.choosing) return;
        room.choosing = false;
        room.choices = null;
        room.word = choice.word;
        room.category = choice.category;
        room.recentWords.add(choice.word);
        while (room.recentWords.size() > 60) room.recentWords.remove(room.recentWords.iterator().next());

        room.roundActive = true;
        room.guessedThisRound.clear();
        room.hintLevel = 0;
        room.lastHintBroadcastLevel = 0;
        room.revealPositions.clear();
        prepareHints(room);
        room.roundEnd = System.currentTimeMillis() + room.roundSeconds * 1000L;

        broadcast(room, "ROUND " + room.round + " " + room.roundSeconds, null);
        broadcast(room, "CATEGORY " + room.category, null);
        for (Socket s : new ArrayList<>(room.members)) {
            if (!OUTPUTS.containsKey(s)) continue;
            if (s == room.drawer) send(s, "WORD " + room.word);
            else send(s, "HINT " + createHint(room, 0));
        }
        broadcastPlayers(room);
        broadcast(room, "SYSTEM " + safeName(room.drawer) + " is drawing now!", null);
    }

    private static synchronized void endRound(Room room, String reason) {
        if (!room.roundActive) return;
        room.roundActive = false;
        String word = room.word;
        broadcast(room, "ROUND_END " + word + " " + reason, null);
        broadcastScoreboard(room);
        room.word = "";
        room.category = "";
        room.guessedThisRound.clear();
        room.hintLevel = 0;
        room.lastHintBroadcastLevel = -1;
        room.revealPositions.clear();
        room.nextRoundAt = System.currentTimeMillis() + RECAP_MILLIS;
        sendRoomLobby(room);
    }

    /** Sends the final table and sends everybody back to the lobby (scores reset when the next game starts). */
    private static synchronized void finishGame(Room room) {
        if (!room.started) return;
        room.started = false;
        room.roundActive = false;
        room.choosing = false;
        room.choices = null;
        room.nextRoundAt = 0;
        room.turnQueue.clear();
        room.gamesPlayed++;
        broadcastFinalResults(room);
        GameSessionDAO.endGame(room.gameId);
        sendRoomLobby(room);
    }

    private static void broadcastFinalResults(Room room) {
        List<Map.Entry<Socket,Integer>> list = new ArrayList<>(room.scores.entrySet());
        list.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        StringBuilder sb = new StringBuilder("GAME_OVER");
        for (Map.Entry<Socket,Integer> e : list) sb.append('|').append(PLAYER_IDS.getOrDefault(e.getKey(), -1)).append(':').append(escape(safeName(e.getKey()))).append(':').append(e.getValue());
        broadcast(room, sb.toString(), null);
    }

    private static void prepareHints(Room room) {
        List<Integer> positions = new ArrayList<>();
        for (int i=0;i<room.word.length();i++) if (Character.isLetterOrDigit(room.word.charAt(i))) positions.add(i);
        Collections.shuffle(positions, RANDOM);
        room.revealPositions.addAll(positions);
    }

    private static int levelToCount(Room room, int level) {
        return level == 1 ? 1 : level == 2 ? Math.min(2, room.revealPositions.size()) : level >= 3 ? Math.min(3, room.revealPositions.size()) : 0;
    }

    private static int baseRevealCount(Room room) { return levelToCount(room, room.hintLevel); }

    private static String createHint(Room room, int level) {
        return createHintCount(room, levelToCount(room, level));
    }

    private static String createHintCount(Room room, int count) {
        if (room.word == null || room.word.isEmpty()) return "";
        count = Math.max(0, Math.min(count, room.revealPositions.size()));
        Set<Integer> revealed = new HashSet<>();
        for (int i=0;i<count;i++) revealed.add(room.revealPositions.get(i));
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<room.word.length();i++) {
            char c=room.word.charAt(i);
            if (c==' ') sb.append("  ");
            else if (!Character.isLetterOrDigit(c)) sb.append(c).append(' ');
            else if (revealed.contains(i)) sb.append(c).append(' ');
            else sb.append("_ ");
        }
        return sb.toString().trim();
    }

    private static void updateHints(Room room) {
        double used = (room.roundSeconds - secondsRemaining(room)) / (double) room.roundSeconds;
        int level = used >= 0.92 ? 3 : used >= 0.75 ? 2 : used >= 0.5 ? 1 : 0;
        if (level <= room.lastHintBroadcastLevel) return;
        room.lastHintBroadcastLevel = level;
        room.hintLevel = level;
        for (Socket s : room.members) {
            if (s == room.drawer) continue;
            send(s, "HINT " + createHintCount(room, levelToCount(room, level) + room.extraHints.getOrDefault(s, 0)));
        }
    }

    private static void expireEffects(Room room, long now) {
        expire(room.blurUntil, now, "BLUR");
        expire(room.freezeUntil, now, "FREEZE");
        expire(room.mirrorUntil, now, "MIRROR");
        expire(room.spotlightUntil, now, "SPOTLIGHT");
    }

    private static void expire(Map<Socket, Long> map, long now, String type) {
        for (Socket s : new ArrayList<>(map.keySet())) {
            if (map.getOrDefault(s, 0L) <= now) {
                map.remove(s);
                send(s, "EFFECT_END " + type);
            }
        }
    }

    private static boolean allGuessersFinished(Room room) {
        int guessers=0;
        for (Socket s : room.members) {
            if (s==room.drawer) continue;
            guessers++;
            if (!room.guessedThisRound.contains(s)) return false;
        }
        return guessers>0;
    }

    private static int secondsRemaining(Room room) {
        return Math.max(0, (int)Math.ceil((room.roundEnd - System.currentTimeMillis()) / 1000.0));
    }

    private static void sendCurrentState(Socket socket, Room room) {
        if (!room.roundActive) return;
        send(socket, "ROUND " + room.round + " " + secondsRemaining(room));
        send(socket, "ROUNDS " + room.round + " " + room.totalRounds);
        send(socket, "CATEGORY " + room.category);
        send(socket, "DRAWER " + safeName(room.drawer));
        if (socket == room.drawer) {
            send(socket, "ROLE DRAWER");
            send(socket, "WORD " + room.word);
        } else {
            send(socket, "ROLE GUESSER");
            send(socket, "HINT " + createHintCount(room, baseRevealCount(room) + room.extraHints.getOrDefault(socket, 0)));
        }
        send(socket, "SCORE " + room.scores.getOrDefault(socket,0));
        send(socket, "POWERSTATUS " + (Boolean.TRUE.equals(room.powerUsed.get(socket)) ? "USED" : "AVAILABLE"));
        broadcastPlayers(room);
    }

    private static void sendRoomLobby(Room room) {
        StringBuilder sb = new StringBuilder("ROOM_LOBBY|").append(room.code).append('|').append(room.hostPlayerId).append('|').append(room.started ? "PLAYING" : "WAITING").append('|').append(room.members.size()).append('|').append(room.maxPlayers);
        for (Socket s : room.members) sb.append('|').append(PLAYER_IDS.getOrDefault(s,-1)).append(':').append(escape(safeName(s)));
        broadcast(room, sb.toString(), null);
        broadcast(room, "ROOM_SETTINGS " + room.totalRounds + " " + room.roundSeconds, null);
        broadcastPlayers(room);
    }

    private static void broadcastPlayers(Room room) {
        StringBuilder sb = new StringBuilder("PLAYERS");
        List<Socket> list = new ArrayList<>(room.members);
        list.sort(Comparator.comparingInt(s -> PLAYER_IDS.getOrDefault(s, Integer.MAX_VALUE)));
        for (Socket s : list) {
            String role = room.started && (room.roundActive || room.choosing) ? (s==room.drawer ? "DRAWER" : "GUESSER") : "LOBBY";
            sb.append('|').append(PLAYER_IDS.getOrDefault(s,-1)).append(':').append(escape(safeName(s))).append(':').append(room.scores.getOrDefault(s,0)).append(':').append(role);
        }
        broadcast(room, sb.toString(), null);
    }

    private static void broadcastScoreboard(Room room) {
        List<Map.Entry<Socket,Integer>> entries = new ArrayList<>(room.scores.entrySet());
        entries.sort((a,b)->Integer.compare(b.getValue(),a.getValue()));
        StringBuilder sb = new StringBuilder("SCORES");
        for (Map.Entry<Socket,Integer> e : entries) sb.append('|').append(escape(safeName(e.getKey()))).append('=').append(e.getValue());
        broadcast(room, sb.toString(), null);
    }

    private static void removePlayer(Socket socket) {
        Room room = PLAYER_ROOMS.remove(socket);
        if (room != null) {
            boolean wasDrawer = room.drawer == socket;
            room.members.remove(socket);
            room.scores.remove(socket);
            room.powerUsed.remove(socket);
            room.guessedThisRound.remove(socket);
            room.blurUntil.remove(socket);
            room.freezeUntil.remove(socket);
            room.mirrorUntil.remove(socket);
            room.spotlightUntil.remove(socket);
            room.shielded.remove(socket);
            room.doubleNext.remove(socket);
            room.extraHints.remove(socket);
            room.turnQueue.remove(socket);
            LAST_STICKER.remove(socket);

            if (room.members.isEmpty()) {
                ROOMS.remove(room.code);
                if (room.started) GameSessionDAO.endGame(room.gameId);
            } else {
                if (PLAYER_IDS.getOrDefault(socket,-1) == room.hostPlayerId) {
                    room.hostPlayerId = PLAYER_IDS.get(room.members.iterator().next());
                }
                if (wasDrawer && room.choosing) {
                    synchronized (GameServer.class) {
                        room.choosing = false;
                        room.choices = null;
                        room.drawer = null;
                        room.nextRoundAt = System.currentTimeMillis() + 1200;
                    }
                    broadcast(room, "CHOOSE_CANCEL", null);
                } else if (wasDrawer && room.roundActive) {
                    endRound(room, "The drawer disconnected.");
                    room.drawer = null;
                } else if (room.roundActive && allGuessersFinished(room)) {
                    endRound(room, "Everyone guessed the word!");
                }
                broadcast(room, "SYSTEM " + safeName(socket) + " left the room.", null);
                sendRoomLobby(room);
            }
            sendPublicRoomsToAll();
        }

        OUTPUTS.remove(socket);
        NAMES.remove(socket);
        DB_IDS.remove(socket);
        PLAYER_IDS.remove(socket);
        try { socket.close(); } catch (IOException ignored) {}
    }

    private static void sendPublicRooms(Socket socket) {
        StringBuilder sb = new StringBuilder("ROOMS");
        List<Room> list = new ArrayList<>(ROOMS.values());
        list.sort(Comparator.comparing(r -> r.code));
        for (Room r : list) {
            if (!r.privateRoom && !r.started && r.members.size() < r.maxPlayers) {
                sb.append('|').append(r.code).append(':').append(r.members.size()).append('/').append(r.maxPlayers).append(':').append(escape(safeName(findByPlayerId(r,r.hostPlayerId))));
            }
        }
        send(socket, sb.toString());
    }

    private static void sendPublicRoomsToAll() {
        // only players who are still browsing (not inside a room) care about the list;
        // everybody else must never receive it, or their client would open the room picker again
        for (Socket s : OUTPUTS.keySet()) if (!PLAYER_ROOMS.containsKey(s)) sendPublicRooms(s);
    }

    private static void send(Socket socket, String message) {
        PrintWriter out = OUTPUTS.get(socket);
        if (out != null) out.println(message);
    }

    private static void broadcast(Room room, String message, Socket except) {
        for (Socket s : new ArrayList<>(room.members)) {
            if (s != except) send(s, message);
        }
    }

    private static String safeName(Socket socket) {
        return NAMES.getOrDefault(socket, "Player").replace('|', ' ').replace(':',' ');
    }

    private static String escape(String value) { return value.replace('|',' ').replace(':',' '); }

    private static String generateRoomCode() {
        final String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb=new StringBuilder(5);
        for(int i=0;i<5;i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    private static int parseInt(String s,int fallback){try{return Integer.parseInt(s);}catch(Exception e){return fallback;}}

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public static int getPlayerCount() { return OUTPUTS.size(); }
}
