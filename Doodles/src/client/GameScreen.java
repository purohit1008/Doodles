package client;

import common.Config;
import common.Emotes;
import common.PowerUp;
import ui.Dialogs;
import ui.EmojiPicker;
import ui.EmoteArt;
import ui.Icons;
import ui.Leaderboard;
import ui.LobbyPanel;
import ui.PowerShop;
import ui.RoomBrowser;
import ui.ResultsDialog;
import ui.Sfx;
import ui.Theme;
import ui.ToastLayer;
import ui.TopBar;
import ui.Widgets;
import ui.WordChoiceOverlay;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything on the in-game screen: networking, canvas, tools, leaderboard, power-up shop, chat,
 * word choice, lobby and results. GameWindow is only the frame that hosts it.
 */
public class GameScreen {

    private static final String SERVER_HOST = Config.HOST;
    private static final int SERVER_PORT = Config.PORT;
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final Pattern TOKEN = Pattern.compile(":([a-z0-9]{2,12}):");

    // ---- network ----
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // ---- player / room ----
    private final String playerName;
    private final String roomAction;
    private final String requestedRoomCode;
    private int playerId = -1;
    private int hostId = -1;
    private String roomCode = "";
    private final Map<Integer, PlayerInfo> roomPlayers = new LinkedHashMap<>();

    // ---- game state ----
    private String currentRole = "WAITING";
    private String currentCategory = "";
    private String currentDrawer = "";
    private boolean roundActive;
    private boolean choosingWord;
    private boolean powerUsed;
    private boolean frozen;
    private int myScore;
    private int totalRounds = 3;
    private int roundSeconds = 60;
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    private final Map<String, Integer> roundStartScores = new LinkedHashMap<>();
    private final Set<String> guessedNames = new HashSet<>();
    private boolean showingResults;
    private boolean lobbyWaiting;
    private long lastStickerAt;

    // ---- timer ----
    private int timeLeft = 60;
    private int timeTotal = 60;
    private javax.swing.Timer gameTimer;

    // ---- UI ----
    private JPanel root;
    private DrawingPanel drawingPanel;
    private JPanel canvasStack;
    private WordChoiceOverlay overlay;
    private ToolDock dock;
    private TopBar topBar;
    private Leaderboard leaderboard;
    private PowerShop powerShop;
    private ToastLayer toasts;
    private JTextPane chatPane;
    private ChatField chatField;
    private Widgets.IconButton sendButton;
    private EmojiPicker picker;
    private EmojiPicker.Button emojiButton;
    private JPopupMenu emojiPopup;
    private KeyEventDispatcher shortcuts;

    // ---- room entry ----
    private Host host;
    private RoomBrowser roomBrowser;
    private boolean awaitingRoomList;
    private volatile boolean closing;
    private volatile boolean serverGreeted;     // true once a real Doodles server said "SERVER READY"

    // ---- lobby ----
    private JDialog lobbyDialog;
    private LobbyPanel lobbyPanel;

    // =========================================================================
    // SETUP
    // =========================================================================

    public GameScreen(String playerName, String roomAction, String roomCode) {
        this.playerName = playerName;
        this.roomAction = roomAction == null ? "QUICK" : roomAction;
        this.requestedRoomCode = roomCode == null ? "" : roomCode.trim().toUpperCase(Locale.ROOT);
        Widgets.installDefaults();
        buildUI();
        installShortcuts();
    }

    public JPanel root() { return root; }

    public ToastLayer toasts() { return toasts; }

    public LobbyPanel lobbyPanel() { return buildLobbyPanelIfNeeded(); }

    public EmojiPicker picker() { return picker; }

    /** The window that hosts this screen reacts to these events. */
    public interface Host {
        /** We are inside a room: time to show the game window. */
        void roomEntered();

        /** Go back to the start screen. */
        void exitToMenu();
    }

    public void setHost(Host h) { this.host = h; }

    /** Connects and starts listening. Returns null on success, otherwise a message for the player. */
    public String connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), CONNECT_TIMEOUT_MS);
            in =new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
            drawingPanel.attach(out);
            out.println("NAME " + playerName);
            switch (roomAction.toUpperCase(Locale.ROOT)) {
                case "CREATE_PRIVATE" -> out.println("CREATE_ROOM PRIVATE 8");
                case "CREATE_PUBLIC" -> out.println("CREATE_ROOM PUBLIC 8");
                case "JOIN" -> out.println("JOIN_ROOM " + requestedRoomCode);
                case "LIST" -> {
                    awaitingRoomList = true;
                    out.println("LIST_ROOMS");
                }
                default -> out.println("QUICK_PLAY");
            }
            startReceiverThread();
            return null;
        } catch (IOException e) {
            return "Could not connect to the Doodles server at " + SERVER_HOST + ":" + SERVER_PORT
                    + ".\n\nStart GameServer first, then try again.";
        }
    }

    /** True once the server has answered with the Doodles greeting (so it really is our server). */
    public boolean serverGreeted() { return serverGreeted; }

    public static String serverAddress() { return SERVER_HOST + ":" + SERVER_PORT; }

    public void shutdown() {
        closing = true;
        stopServerTimer();
        closeRoomBrowser();
        if (lobbyDialog != null) lobbyDialog.dispose();
        if (shortcuts != null) KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(shortcuts);
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    // =========================================================================
    // UI
    // =========================================================================

    private void buildUI() {
        Backdrop bg = new Backdrop();
        bg.setLayout(new BorderLayout());
        root = bg;

        topBar = new TopBar(this::showSettings);
        topBar.setRound(1, totalRounds);
        topBar.setTime(timeLeft, timeTotal);
        topBar.setOnline(true);
        topBar.word().setTitle("WAITING FOR THE GAME", Theme.TEXT_MUTED);
        topBar.word().setMessage("get ready...");
        bg.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 14, 14, 14));

        // ---- left: leaderboard + power-ups ----
        leaderboard = new Leaderboard();
        List<Leaderboard.Entry> first = new ArrayList<>();
        first.add(new Leaderboard.Entry(playerName, 0, true, false, false));
        leaderboard.setEntries(first);
        Widgets.RoundPanel lbCard = card();
        lbCard.setLayout(new BorderLayout());
        JScrollPane lbScroll = slim(leaderboard);
        lbScroll.setBorder(new EmptyBorder(0, 0, 8, 0));
        lbCard.add(lbScroll, BorderLayout.CENTER);

        powerShop = new PowerShop();
        powerShop.setOnBuy(this::onBuyPower);
        Widgets.RoundPanel shopCard = card();
        shopCard.setLayout(new BorderLayout());
        shopCard.add(powerShop, BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(262, 0));
        left.add(lbCard, BorderLayout.CENTER);
        left.add(shopCard, BorderLayout.SOUTH);

        // ---- centre: canvas (with the word-choice overlay on top) + tool dock ----
        drawingPanel = new DrawingPanel(null);
        drawingPanel.setCanDraw(false);
        dock = new ToolDock(drawingPanel, this::confirmClear);
        dock.setDrawerMode(false);

        overlay = new WordChoiceOverlay();
        overlay.setOnPick(i -> { if (out != null) out.println("CHOOSE_WORD " + i); Sfx.play(Sfx.Sound.SELECT); });
        overlay.setAlignmentX(0.5f);
        overlay.setAlignmentY(0.5f);
        drawingPanel.setAlignmentX(0.5f);
        drawingPanel.setAlignmentY(0.5f);
        canvasStack = new JPanel();
        canvasStack.setLayout(new OverlayLayout(canvasStack));
        canvasStack.setOpaque(false);
        canvasStack.add(overlay);
        canvasStack.add(drawingPanel);

        Widgets.RoundPanel center = card();
        center.setLayout(new BorderLayout());
        center.add(canvasStack, BorderLayout.CENTER);
        center.add(dock, BorderLayout.SOUTH);
        center.setBorder(new EmptyBorder(4, 4, 0, 4));

        // ---- right: chat ----
        Widgets.RoundPanel chatCard = card();
        chatCard.setLayout(new BorderLayout());
        chatCard.setPreferredSize(new Dimension(318, 0));
        chatCard.add(buildChatPanel(), BorderLayout.CENTER);

        body.add(left, BorderLayout.WEST);
        body.add(center, BorderLayout.CENTER);
        body.add(chatCard, BorderLayout.EAST);
        bg.add(body, BorderLayout.CENTER);

        toasts = new ToastLayer();
        refreshShop();
        updateInputState();
    }

    private static Widgets.RoundPanel card() {
        return new Widgets.RoundPanel(Theme.PANEL, 26).border(Theme.BORDER);
    }

    private JPanel buildChatPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel head = new JPanel(null) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                Icons.paint(g, Icons.CHAT, 16, 14, 22, Theme.ACCENT);
                g.setFont(Theme.font(Font.BOLD, 13));
                g.setColor(Theme.TEXT);
                g.drawString("CHAT & GUESSES", 46, 31);
                g.dispose();
            }
        };
        head.setOpaque(false);
        head.setPreferredSize(new Dimension(0, 48));
        p.add(head, BorderLayout.NORTH);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setOpaque(false);
        chatPane.setForeground(Theme.TEXT);
        chatPane.setFont(Theme.NORMAL_FONT);
        chatPane.setBorder(new EmptyBorder(4, 16, 4, 12));
        ((DefaultCaret) chatPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        p.add(slim(chatPane), BorderLayout.CENTER);

        chatField = new ChatField();
        chatField.addActionListener(e -> sendChatMessage());
        sendButton = new Widgets.IconButton(Icons.SEND, "Send", 44);
        sendButton.accent(Theme.ACCENT);
        sendButton.setSelected(true);
        sendButton.addActionListener(e -> sendChatMessage());

        picker = new EmojiPicker();
        picker.setListener(new EmojiPicker.Listener() {
            @Override public void onEmoji(String id) { insertEmoji(id); }
            @Override public void onSticker(String id) { sendSticker(id); }
        });
        emojiPopup = new JPopupMenu();
        emojiPopup.setBorder(BorderFactory.createEmptyBorder());
        emojiPopup.setLightWeightPopupEnabled(true);
        emojiPopup.add(picker);
        emojiButton = new EmojiPicker.Button(this::toggleEmojiPopup);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(emojiButton);
        buttons.add(sendButton);

        JPanel input = new JPanel(new BorderLayout(8, 0));
        input.setOpaque(false);
        input.setBorder(new EmptyBorder(8, 12, 14, 8));
        input.add(chatField, BorderLayout.CENTER);
        input.add(buttons, BorderLayout.EAST);
        p.add(input, BorderLayout.SOUTH);
        return p;
    }

    private void toggleEmojiPopup() {
        if (emojiPopup.isVisible()) {
            emojiPopup.setVisible(false);
            return;
        }
        picker.setEmojiEnabled(chatField.isEnabled());
        picker.setStickerTab(!chatField.isEnabled());
        emojiPopup.show(emojiButton, emojiButton.getWidth() - EmojiPicker.W + 6, -EmojiPicker.H - 8);
    }

    private void insertEmoji(String id) {
        if (!chatField.isEnabled()) return;
        String token = ":" + id + ":";
        int pos = chatField.getCaretPosition();
        String text = chatField.getText();
        String before = text.substring(0, pos), after = text.substring(pos);
        String pad1 = before.isEmpty() || before.endsWith(" ") ? "" : " ";
        chatField.setText(before + pad1 + token + " " + after);
        chatField.setCaretPosition((before + pad1 + token + " ").length());
        chatField.requestFocusInWindow();
    }

    private void sendSticker(String id) {
        if (out == null) return;
        long now = System.currentTimeMillis();
        if (now - lastStickerAt < 1500) {
            toasts.show("Slow down", "One sticker every second or so.", Icons.CLOCK, Theme.ORANGE, 1400);
            return;
        }
        lastStickerAt = now;
        emojiPopup.setVisible(false);
        out.println("STICKER " + id);
    }

    // =========================================================================
    // KEYBOARD SHORTCUTS
    // =========================================================================

    private void installShortcuts() {
        shortcuts = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            Window w = SwingUtilities.getWindowAncestor(root);
            if (w == null || !w.isActive()) return false;
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (owner instanceof javax.swing.text.JTextComponent && ((javax.swing.text.JTextComponent) owner).isEditable()) return false;
            if (owner != null && !SwingUtilities.isDescendingFrom(owner, root)) return false;

            int k = e.getKeyCode();
            if (overlay.isShowingChoices()) {
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_3) { overlay.pick(k - KeyEvent.VK_1); return true; }
                return false;
            }
            if (!currentRole.equals("DRAWER") || !roundActive) return false;
            if (e.isControlDown() && k == KeyEvent.VK_Z) { dock.undoNow(); return true; }
            if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) return false;
            switch (k) {
                case KeyEvent.VK_B -> dock.selectTool(DrawingPanel.Tool.BRUSH);
                case KeyEvent.VK_E -> dock.selectTool(DrawingPanel.Tool.ERASER);
                case KeyEvent.VK_F -> dock.selectTool(DrawingPanel.Tool.FILL);
                case KeyEvent.VK_L -> dock.selectTool(DrawingPanel.Tool.LINE);
                case KeyEvent.VK_R -> dock.selectTool(DrawingPanel.Tool.RECT);
                case KeyEvent.VK_O -> dock.selectTool(DrawingPanel.Tool.ELLIPSE);
                case KeyEvent.VK_OPEN_BRACKET -> dock.adjustSize(-2);
                case KeyEvent.VK_CLOSE_BRACKET -> dock.adjustSize(2);
                default -> {
                    if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) {
                        Brush[] all = Brush.values();
                        int idx = k - KeyEvent.VK_1;
                        if (idx < all.length) dock.selectBrush(all[idx]);
                    } else {
                        return false;
                    }
                }
            }
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcuts);
    }

    // =========================================================================
    // SETTINGS / HELP
    // =========================================================================

    private Window window() { return SwingUtilities.getWindowAncestor(root); }

    private void showSettings() {
        JDialog[] dd = new JDialog[1];
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(4, 30, 26, 30));

        // sound + save
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        opts.setOpaque(false);
        opts.setAlignmentX(Component.LEFT_ALIGNMENT);
        Widgets.PillButton sound = new Widgets.PillButton(Sfx.isEnabled() ? "Sound: ON" : "Sound: OFF", Sfx.isEnabled() ? Theme.SUCCESS : Theme.TEXT_MUTED).icon(Sfx.isEnabled() ? Icons.SOUND : Icons.MUTE);
        sound.addActionListener(e -> {
            Sfx.setEnabled(!Sfx.isEnabled());
            sound.setText(Sfx.isEnabled() ? "Sound: ON" : "Sound: OFF");
            sound.repaint();
            if (Sfx.isEnabled()) Sfx.play(Sfx.Sound.POP);
        });
        Widgets.PillButton save = new Widgets.PillButton("Save drawing", Theme.ACCENT_PINK).icon(Icons.CAMERA);
        save.addActionListener(e -> saveDrawing());
        opts.add(sound);
        opts.add(save);
        opts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        p.add(opts);
        p.add(Box.createVerticalStrut(18));

        p.add(Widgets.label("SHORTCUTS (when you are drawing)", Theme.SMALL_BOLD, Theme.ACCENT));
        p.add(Box.createVerticalStrut(10));
        String[][] rows = {
                {"1 - 9", "Pick a brush"}, {"B", "Brush"}, {"E", "Eraser"}, {"F", "Fill bucket"},
                {"L / R / O", "Line / Rectangle / Ellipse"}, {"[  ]", "Smaller / bigger brush"}, {"Ctrl + Z", "Undo"},
                {"1 / 2 / 3", "Pick one of the three words"}
        };
        for (String[] r : rows) {
            JPanel line = new JPanel(new BorderLayout());
            line.setOpaque(false);
            JLabel k = Widgets.label(r[0], new Font(Font.MONOSPACED, Font.BOLD, 13), Theme.ink(Theme.PINK));
            k.setPreferredSize(new Dimension(110, 24));
            line.add(k, BorderLayout.WEST);
            line.add(Widgets.label(r[1], Theme.NORMAL_FONT, Theme.TEXT), BorderLayout.CENTER);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(line);
        }
        p.add(Box.createVerticalStrut(16));
        p.add(Widgets.label("POWER-UPS", Theme.SMALL_BOLD, Theme.ACCENT));
        p.add(Box.createVerticalStrut(8));
        for (PowerUp pu : PowerUp.values()) {
            JPanel line = new JPanel(new BorderLayout(10, 0));
            line.setOpaque(false);
            line.add(new JLabel(Icons.icon(pu.name(), 22, Widgets.powerColor(pu))), BorderLayout.WEST);
            line.add(Widgets.label(pu.label() + " (" + pu.cost() + ") - " + pu.tagline(), Theme.font(Font.PLAIN, 12), Theme.TEXT), BorderLayout.CENTER);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(line);
        }
        p.add(Box.createVerticalStrut(8));
        JLabel note = Widgets.label("One power-up per round. Drawer powers hit one guesser; guesser powers help you or hit another guesser.", Theme.font(Font.ITALIC, 11), Theme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(note);
        p.add(Box.createVerticalStrut(20));

        Widgets.PillButton leave = Dialogs.button("Leave room", Theme.DANGER, 150).icon(Icons.HOME);
        leave.addActionListener(e -> {
            if (Dialogs.confirm(root, "Leave room", "Leave this game and go back to the main menu?", "Leave", "Stay", Dialogs.Kind.WARN)) {
                dd[0].dispose();
                if (host != null) host.exitToMenu();
            }
        });
        Widgets.PillButton ok = Dialogs.button("Got it", Theme.ACCENT, 130);
        ok.addActionListener(e -> dd[0].dispose());
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        row.add(leave);
        row.add(ok);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(row);

        dd[0] = Dialogs.framed(root, "Settings & shortcuts", p, true, Dialogs.Style.PASTEL);
        dd[0].getRootPane().setDefaultButton(ok);
        dd[0].setVisible(true);
    }

    private void saveDrawing() {
        Window w = window();
        FileDialog fd = new FileDialog(w instanceof Frame f ? f : (Frame) null, "Save drawing", FileDialog.SAVE);
        fd.setFile("doodle.png");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        File f = new File(fd.getDirectory(), fd.getFile());
        if (!f.getName().toLowerCase(Locale.ROOT).endsWith(".png")) f = new File(f.getParentFile(), f.getName() + ".png");
        try {
            ImageIO.write(drawingPanel.snapshot(), "png", f);
            toasts.show("Saved!", f.getName(), Icons.CAMERA, Theme.SUCCESS);
        } catch (IOException ex) {
            toasts.show("Could not save", ex.getMessage(), Icons.CAMERA, Theme.DANGER, 3500);
        }
    }

    private void confirmClear() {
        if (Dialogs.confirm(root, "Clear canvas", "Clear the entire drawing?\nEveryone will see the canvas wiped.", "Clear it", "Keep drawing", Dialogs.Kind.WARN)) {
            drawingPanel.clearCanvas();
        }
    }

    // =========================================================================
    // CHAT
    // =========================================================================

    private void sendChatMessage() {
        if (out == null || !chatField.isEnabled()) return;
        String message = Emotes.applyShortcuts(chatField.getText().trim());
        if (message.isEmpty()) return;
        out.println("CHAT " + message);   // the server echoes wrong guesses back to everybody
        chatField.setText("");
        chatField.requestFocusInWindow();
    }

    private static SimpleAttributeSet style(Color color, boolean bold, boolean italic, int size) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, Theme.FAMILY);
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, bold);
        StyleConstants.setItalic(a, italic);
        StyleConstants.setFontSize(a, size);
        StyleConstants.setSpaceBelow(a, 6);
        return a;
    }

    private void appendChat(String raw) {
        if (chatPane == null || raw == null) return;
        SwingUtilities.invokeLater(() -> {
            String text = raw;
            Color color = Theme.TEXT;
            boolean italic = false, bold = false;
            String name = null;

            if (raw.startsWith("System: ")) {
                text = raw.substring(8);
                color = Theme.TEXT_MUTED;
                italic = true;
            } else if (raw.startsWith("\uD83C\uDFAF") || raw.contains("guessed correctly")) {
                color = Theme.ink(Theme.MINT);
                bold = true;
            } else if (raw.startsWith("\u26A1") || raw.startsWith("\u2744")) {
                color = Theme.ink(Theme.ORANGE);
                bold = true;
            } else if (raw.startsWith("\uD83D\uDEE1") || raw.startsWith("\uD83D\uDCA1") || raw.startsWith("Double armed")) {
                color = Theme.ACCENT;
                bold = true;
            } else if (raw.startsWith("\uD83C\uDFA8")) {
                color = Theme.ink(Theme.PINK);
                bold = true;
            } else {
                int c = raw.indexOf(": ");
                if (c > 0 && c < 26) {
                    name = raw.substring(0, c);
                    text = raw.substring(c + 2);
                }
            }
            // emoji glyphs are not reliable in Swing fonts, the colour already carries the meaning
            text = text.replaceAll("^[\\p{So}\\p{Cs}\\uFE0F\\u200D\\u2700-\\u27BF\\s]+", "");
            writeChat(name, text, color, bold, italic, null);
        });
    }

    private void appendPrivateChat(String line) {
        SwingUtilities.invokeLater(() -> {
            int c = line.indexOf(": ");
            String name = c > 0 ? line.substring(0, c) : null;
            String text = c > 0 ? line.substring(c + 2) : line;
            writeChat(name == null ? null : name + " (guessed)", text, Theme.ink(Theme.MINT), false, true, null);
        });
    }

    private void appendSticker(String who, String id) {
        SwingUtilities.invokeLater(() -> writeChat(who, null, Theme.TEXT, false, false, id));
    }

    private void writeChat(String name, String text, Color color, boolean bold, boolean italic, String stickerId) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            if (name != null) doc.insertString(doc.getLength(), name + (stickerId != null ? "\n" : "  "), style(Theme.ink(Theme.avatarColor(name.replace(" (guessed)", ""))), true, false, 13));
            if (stickerId != null) {
                SimpleAttributeSet ia = style(color, false, false, 13);
                StyleConstants.setIcon(ia, EmoteArt.stickerIcon(stickerId, 112, 44));
                doc.insertString(doc.getLength(), " ", ia);
                doc.insertString(doc.getLength(), "\n", style(color, false, false, 13));
            } else {
                insertRich(doc, text, style(color, bold, italic, 13));
                doc.insertString(doc.getLength(), "\n", style(color, bold, italic, 13));
            }
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    /** Inserts text, turning :emoji: tokens into small painted icons. */
    private void insertRich(StyledDocument doc, String text, SimpleAttributeSet base) throws BadLocationException {
        Matcher m = TOKEN.matcher(text);
        int last = 0;
        while (m.find()) {
            if (!Emotes.isEmoji(m.group(1))) continue;
            doc.insertString(doc.getLength(), text.substring(last, m.start()), base);
            SimpleAttributeSet ia = new SimpleAttributeSet(base);
            StyleConstants.setIcon(ia, EmoteArt.icon(m.group(1), 20));
            doc.insertString(doc.getLength(), " ", ia);
            last = m.end();
        }
        doc.insertString(doc.getLength(), text.substring(last), base);
    }

    private void updateInputState() {
        if (chatField == null) return;
        boolean drawer = currentRole.equals("DRAWER");
        boolean canGuess = roundActive && currentRole.equals("GUESSER") && !frozen && !guessedNames.contains(playerName);
        boolean chatting = (!roundActive && !drawer) || (roundActive && guessedNames.contains(playerName) && !drawer);
        boolean typingOk = canGuess || chatting;
        chatField.setEnabled(typingOk);
        sendButton.setEnabled(typingOk);
        if (frozen) chatField.setHint("You are frozen...");
        else if (drawer && (roundActive || choosingWord)) chatField.setHint("You're drawing!");
        else if (roundActive && guessedNames.contains(playerName)) chatField.setHint("You got it! Chat here...");
        else if (roundActive) chatField.setHint("Type your guess...");
        else chatField.setHint("Chat...");
        picker.setEmojiEnabled(typingOk);
        chatField.repaint();
    }

    // =========================================================================
    // RECEIVER
    // =========================================================================

    private void startReceiverThread() {
        Thread receiver = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) handleServerMessage(message);
            } catch (IOException ignored) {
            }
            SwingUtilities.invokeLater(() -> {
                if (closing) return;
                topBar.setOnline(false);
                appendChat("System: Server disconnected.");
                onConnectionLost();
            });
        });
        receiver.setDaemon(true);
        receiver.start();
    }

    // =========================================================================
    // SERVER MESSAGES
    // =========================================================================

    public void handleServerMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        if (message.equals("SERVER READY")) {
            serverGreeted = true;
            return;
        }

        if (message.startsWith("DRAW ") || message.startsWith("FILL ") || message.equals("UP")
                || message.equals("UNDO") || message.equals("CANVAS_CLEAR")) {
            drawingPanel.receiveDrawing(message);
            return;
        }

        // ---- rooms / lobby ----
        if (message.startsWith("ROOM_CREATED ") || message.startsWith("ROOM_JOINED ")) {
            String[] p = message.split("\\s+");
            roomCode = p.length > 1 ? p[1] : "";
            SwingUtilities.invokeLater(() -> {
                awaitingRoomList = false;
                closeRoomBrowser();
                appendChat("System: Room " + roomCode + " ready.");
                if (host != null) host.roomEntered();
            });
            return;
        }
        if (message.equals("ROOMS") || message.startsWith("ROOMS|")) {
            SwingUtilities.invokeLater(() -> onRoomList(message));
            return;
        }
        if (message.startsWith("ROOM_SETTINGS ")) {
            String[] p = message.split("\\s+");
            if (p.length >= 3) {
                int r = parseIntSafe(p[1], totalRounds), s = parseIntSafe(p[2], roundSeconds);
                SwingUtilities.invokeLater(() -> {
                    totalRounds = r;
                    roundSeconds = s;
                    if (lobbyPanel != null) lobbyPanel.setSettings(r, s);
                    if (!roundActive && !choosingWord) topBar.setRound(1, r);
                });
            }
            return;
        }
        if (message.startsWith("ROOM_LOBBY|")) {
            SwingUtilities.invokeLater(() -> updateLobby(message));
            return;
        }
        if (message.equals("GAME_START")) {
            SwingUtilities.invokeLater(this::onGameStart);
            return;
        }

        // ---- word choice ----
        if (message.startsWith("WORD_CHOICES ")) {
            String[] parts = message.split("\\|");
            int secs = parseIntSafe(parts[0].substring(13).trim(), 15);
            List<WordChoiceOverlay.Card> cards = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                String[] f = parts[i].split(":", 3);
                if (f.length >= 2) cards.add(new WordChoiceOverlay.Card(f[0], f[1], f.length > 2 ? parseIntSafe(f[2], 2) : 2));
            }
            SwingUtilities.invokeLater(() -> {
                choosingWord = true;
                setServerTimer(secs);
                topBar.word().setTitle("YOUR TURN", Theme.ACCENT);
                topBar.word().setMessage("pick a word below");
                overlay.showChoices(cards, secs);
                Sfx.play(Sfx.Sound.ROUND_START);
                updateInputState();
            });
            return;
        }
        if (message.startsWith("CHOOSING ")) {
            String data = message.substring(9).trim();
            int split = data.lastIndexOf(' ');
            String name = split > 0 ? data.substring(0, split) : data;
            int secs = split > 0 ? parseIntSafe(data.substring(split + 1), 15) : 15;
            SwingUtilities.invokeLater(() -> {
                choosingWord = true;
                setServerTimer(secs);
                topBar.word().setTitle("GET READY", Theme.TEXT_MUTED);
                topBar.word().setMessage(name + " is choosing...");
                overlay.showWaiting(name, secs);
                updateInputState();
            });
            return;
        }
        if (message.equals("CHOOSE_CANCEL")) {
            SwingUtilities.invokeLater(() -> {
                choosingWord = false;
                overlay.hideOverlay();
                stopServerTimer();
                topBar.word().setMessage("picking a new drawer...");
            });
            return;
        }

        // ---- power-ups ----
        if (message.startsWith("POWERSTATUS ")) {
            powerUsed = message.substring(12).trim().equalsIgnoreCase("USED");
            SwingUtilities.invokeLater(this::refreshShop);
            return;
        }
        if (message.startsWith("POWERSTATE")) {
            String s = message.substring(10);
            boolean shield = s.contains("SHIELD"), dbl = s.contains("DOUBLE");
            SwingUtilities.invokeLater(() -> powerShop.setArmed(shield, dbl));
            return;
        }
        if (message.startsWith("POWER_USED ")) {
            String[] p = message.split("\\s+");
            PowerUp pu = p.length > 1 ? PowerUp.parse(p[1]) : null;
            String target = p.length > 2 ? p[2] : "";
            if (pu != null) SwingUtilities.invokeLater(() -> {
                Sfx.play(Sfx.Sound.POWER);
                if (pu.isTargeted()) toasts.show(pu.label() + " launched!", "Aimed at " + target, pu.name(), Widgets.powerColor(pu));
                else toasts.show(pu.label() + " activated", pu.tagline(), pu.name(), Widgets.powerColor(pu));
            });
            return;
        }
        if (message.startsWith("POWER_ERROR ")) {
            String text = message.substring(12);
            SwingUtilities.invokeLater(() -> toasts.show("Can't do that", text, Icons.BOLT, Theme.DANGER, 3200));
            return;
        }
        if (message.startsWith("SHIELD_BLOCK_USER ")) {
            String who = message.substring(18).trim();
            SwingUtilities.invokeLater(() -> toasts.show("Blocked!", who + "'s shield absorbed it.", Icons.SHIELD, Theme.DANGER));
            return;
        }
        if (message.startsWith("SHIELD_BLOCK_TARGET ")) {
            String who = message.substring(20).trim();
            SwingUtilities.invokeLater(() -> toasts.show("Shield saved you", who + "'s power-up was blocked.", Icons.SHIELD, Theme.MINT));
            return;
        }
        if (message.equals("EFFECT_CLEAR")) {
            SwingUtilities.invokeLater(() -> {
                drawingPanel.clearEffects();
                frozen = false;
                powerShop.setArmed(false, false);
                refreshShop();
                updateInputState();
            });
            return;
        }
        if (message.startsWith("EFFECT ")) {
            String[] p = message.split("\\s+");
            PowerUp pu = p.length > 1 ? PowerUp.parse(p[1]) : null;
            int secs = p.length > 2 ? parseIntSafe(p[2], 5) : 5;
            if (pu != null) SwingUtilities.invokeLater(() -> {
                drawingPanel.startEffect(pu, secs);
                if (pu == PowerUp.FREEZE) {
                    frozen = true;
                    refreshShop();
                    updateInputState();
                }
                Sfx.play(Sfx.Sound.HIT);
                toasts.show("You got hit: " + pu.label(), secs + " seconds", pu.name(), Widgets.powerColor(pu));
            });
            return;
        }
        if (message.startsWith("EFFECT_END ")) {
            PowerUp pu = PowerUp.parse(message.substring(11));
            if (pu != null) SwingUtilities.invokeLater(() -> {
                drawingPanel.endEffect(pu);
                if (pu == PowerUp.FREEZE) {
                    frozen = false;
                    refreshShop();
                    updateInputState();
                }
            });
            return;
        }

        // ---- players / scores ----
        if (message.startsWith("PLAYERS")) {
            updatePlayers(message);
            return;
        }
        if (message.startsWith("SCORES")) {
            updateScoreboard(message);
            return;
        }
        if (message.startsWith("SCORE ")) {
            myScore = parseIntSafe(message.substring(6).trim(), myScore);
            SwingUtilities.invokeLater(this::refreshShop);
            return;
        }
        if (message.startsWith("GAME_OVER")) {
            SwingUtilities.invokeLater(() -> showGameOver(message));
            return;
        }
        if (message.startsWith("PLAYER ")) {
            playerId = parseIntSafe(message.substring(7).trim(), playerId);
            return;
        }
        if (message.startsWith("GAMEID ") || message.startsWith("GAME ")) return;

        // ---- round flow ----
        if (message.startsWith("ROUNDS ")) {
            String[] p = message.split("\\s+");
            if (p.length >= 3) {
                int cur = parseIntSafe(p[1], 1);
                totalRounds = parseIntSafe(p[2], 3);
                SwingUtilities.invokeLater(() -> topBar.setRound(cur, totalRounds));
            }
            return;
        }
        if (message.startsWith("ROUND ")) {
            String[] p = message.split("\\s+");
            if (p.length >= 3) {
                int round = parseIntSafe(p[1], 1);
                int seconds = parseIntSafe(p[2], 60);
                SwingUtilities.invokeLater(() -> onRoundStart(round, seconds));
            }
            return;
        }
        if (message.startsWith("CATEGORY ")) {
            currentCategory = message.substring(9).trim();
            SwingUtilities.invokeLater(this::updateWordTitle);
            return;
        }
        if (message.startsWith("DRAWER ")) {
            currentDrawer = message.substring(7).trim();
            SwingUtilities.invokeLater(this::refreshLeaderboard);
            return;
        }
        if (message.startsWith("ROLE ")) {
            currentRole = message.substring(5).trim().toUpperCase(Locale.ROOT);
            SwingUtilities.invokeLater(() -> {
                boolean drawer = currentRole.equals("DRAWER");
                drawingPanel.setCanDraw(drawer && roundActive);
                dock.setDrawerMode(drawer && roundActive);
                updateWordTitle();
                refreshShop();
                updateInputState();
                if (drawer) toasts.show("Your turn to draw!", "Pick one of three words.", Icons.PEN, Theme.YELLOW);
            });
            return;
        }
        if (message.startsWith("WORD ")) {
            String word = message.substring(5).trim();
            SwingUtilities.invokeLater(() -> topBar.word().setPlain(word, Theme.MINT));
            return;
        }
        if (message.startsWith("HINT ")) {
            String hint = message.substring(5).trim();
            SwingUtilities.invokeLater(() -> {
                if (!currentRole.equals("DRAWER")) topBar.word().setHint(hint);
            });
            return;
        }
        if (message.equals("CLEAR")) {
            SwingUtilities.invokeLater(() -> drawingPanel.clearDrawing());
            return;
        }
        if (message.startsWith("CORRECT ")) {
            String data = message.substring(8).trim();
            int split = data.lastIndexOf(' ');
            String name = split > 0 ? data.substring(0, split) : data;
            String points = split > 0 ? data.substring(split + 1) : "0";
            SwingUtilities.invokeLater(() -> {
                guessedNames.add(name);
                refreshLeaderboard();
                updateInputState();
                if (name.equals(playerName)) {
                    Sfx.play(Sfx.Sound.CORRECT);
                    toasts.show("Correct!", "+" + points + " points", Icons.CROWN, Theme.MINT);
                    toasts.confetti(canvasArea(), 70);
                } else {
                    Sfx.play(Sfx.Sound.OTHER_CORRECT);
                    toasts.show(name + " got it!", "+" + points + " points", Icons.CROWN, Theme.GOLD, 1800);
                }
            });
            return;
        }
        if (message.startsWith("ROUND_END ")) {
            String data = message.substring(10).trim();
            int split = data.indexOf(' ');
            String word = split > 0 ? data.substring(0, split) : data;
            String reason = split > 0 ? data.substring(split + 1) : "Round complete.";
            SwingUtilities.invokeLater(() -> onRoundEnd(word, reason));
            return;
        }

        // ---- chat / stickers ----
        if (message.startsWith("CHAT_PRIVATE ")) {
            appendPrivateChat(message.substring(13));
            return;
        }
        if (message.startsWith("CHAT ")) {
            appendChat(message.substring(5));
            return;
        }
        if (message.startsWith("STICKER ")) {
            String data = message.substring(8).trim();
            int split = data.lastIndexOf(' ');
            if (split > 0) {
                String who = data.substring(0, split), id = data.substring(split + 1);
                if (Emotes.isSticker(id)) {
                    appendSticker(who, id);
                    SwingUtilities.invokeLater(() -> {
                        Sfx.play(Sfx.Sound.POP);
                        toasts.pop(id, who, canvasArea());
                    });
                }
            }
            return;
        }
        if (message.startsWith("SYSTEM ")) {
            appendChat("System: " + message.substring(7));
            return;
        }
        if (message.startsWith("ERROR ")) {
            String text = message.substring(6);
            SwingUtilities.invokeLater(() -> onServerError(text));
        }
    }

    // ---- round / game transitions (all on the EDT) ----

    private void onGameStart() {
        // a fresh game always starts from zero
        scores.clear();
        roundStartScores.clear();
        guessedNames.clear();
        myScore = 0;
        powerUsed = false;
        frozen = false;
        roundActive = false;
        choosingWord = false;
        currentRole = "WAITING";
        currentDrawer = "";
        overlay.hideOverlay();
        toasts.hideRecap();
        drawingPanel.clearDrawing();
        drawingPanel.clearEffects();
        drawingPanel.setCanDraw(false);
        dock.setDrawerMode(false);
        powerShop.setArmed(false, false);
        stopServerTimer();
        topBar.setRound(1, totalRounds);
        topBar.setTime(roundSeconds, roundSeconds);
        topBar.word().setTitle("NEW GAME", Theme.ACCENT);
        topBar.word().setMessage("get ready...");
        if (lobbyDialog != null) lobbyDialog.setVisible(false);
        refreshLeaderboard();
        refreshShop();
        updateInputState();
        toasts.show("Game on!", "Scores start from zero.", Icons.BOLT, Theme.ACCENT);
        Sfx.play(Sfx.Sound.ROUND_START);
    }

    private void onRoundStart(int round, int seconds) {
        choosingWord = false;
        roundActive = true;
        guessedNames.clear();
        roundStartScores.clear();
        roundStartScores.putAll(scores);
        overlay.hideOverlay();
        toasts.hideRecap();
        topBar.setRound(round, totalRounds);
        setServerTimer(seconds);
        drawingPanel.clearDrawing();
        boolean drawer = currentRole.equals("DRAWER");
        drawingPanel.setCanDraw(drawer);
        dock.setDrawerMode(drawer);
        updateWordTitle();
        refreshLeaderboard();
        refreshShop();
        updateInputState();
        Sfx.play(Sfx.Sound.ROUND_START);
        if (drawer) toasts.show("Draw it!", "Press 1-9 to swap brushes.", Icons.PEN, Theme.YELLOW);
        else toasts.show(currentDrawer + " is drawing", "Type your guess below.", Icons.PEN, Theme.SKY);
    }

    private void onRoundEnd(String word, String reason) {
        stopServerTimer();
        boolean wasRound = roundActive;
        roundActive = false;
        choosingWord = false;
        drawingPanel.setCanDraw(false);
        drawingPanel.clearEffects();
        dock.setDrawerMode(false);
        frozen = false;
        topBar.word().setTitle("THE WORD WAS", Theme.TEXT_MUTED);
        topBar.word().setPlain(word, Theme.YELLOW);
        appendChat("System: " + reason + " The word was " + word + ".");
        refreshShop();
        updateInputState();
        if (!wasRound) return;
        Sfx.play(Sfx.Sound.ROUND_END);

        List<ToastLayer.RecapRow> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            rows.add(new ToastLayer.RecapRow(e.getKey(), e.getValue() - roundStartScores.getOrDefault(e.getKey(), 0), e.getKey().equals(currentDrawer)));
        }
        rows.sort((a, b) -> Integer.compare(b.gained, a.gained));
        toasts.showRecap(word, rows, canvasArea(), 4400);
    }

    /** Where the canvas is, in the glass pane's coordinates (for stickers, confetti and the recap card). */
    private Rectangle canvasArea() {
        try {
            if (toasts.getRootPane() != null && canvasStack.getWidth() > 0) {
                Point p = SwingUtilities.convertPoint(canvasStack, 0, 0, toasts);
                return new Rectangle(p.x, p.y, canvasStack.getWidth(), canvasStack.getHeight());
            }
            Point p = SwingUtilities.convertPoint(canvasStack, 0, 0, root);
            if (canvasStack.getWidth() > 0) return new Rectangle(p.x, p.y, canvasStack.getWidth(), canvasStack.getHeight());
        } catch (RuntimeException ignored) {
        }
        return new Rectangle(300, 110, 760, 560);
    }

    // =========================================================================
    // POWER-UP PURCHASE
    // =========================================================================

    private void onBuyPower(PowerUp p) {
        if (out == null) return;
        if (!p.isTargeted()) {
            out.println("BUY_POWER " + p.name());
            return;
        }
        List<PlayerInfo> targets = new ArrayList<>();
        synchronized (roomPlayers) {
            for (PlayerInfo pi : roomPlayers.values()) {
                if (pi.id != playerId && pi.role.equals("GUESSER") && !guessedNames.contains(pi.name)) targets.add(pi);
            }
        }
        if (targets.isEmpty()) {
            toasts.show("No target", "Everyone else already guessed.", p.name(), Theme.DANGER);
            return;
        }
        PlayerInfo choice = chooseTarget(p, targets);
        if (choice != null) out.println("BUY_POWER " + p.name() + " " + choice.id);
    }

    private PlayerInfo chooseTarget(PowerUp p, List<PlayerInfo> targets) {
        JDialog[] dd = new JDialog[1];
        final PlayerInfo[] picked = {null};
        Color c = Widgets.powerColor(p);

        JPanel box = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                g.setColor(Theme.alpha(c, 40));
                g.fillOval(4, 2, 56, 56);
                Icons.paint(g, p.name(), 13, 11, 38, c);
                g.setFont(Theme.font(Font.BOLD, 17));
                g.setColor(Theme.TEXT);
                g.drawString("Choose a target", 74, 28);
                g.setFont(Theme.font(Font.PLAIN, 13));
                g.setColor(Theme.TEXT_MUTED);
                g.drawString(p.label() + "  -  " + p.cost() + " pts", 74, 48);
                g.dispose();
            }
        };
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(78, 30, 26, 30));
        for (PlayerInfo pi : targets) {
            Widgets.PillButton b = new Widgets.PillButton(pi.name + "   (" + pi.score + " pts)", Theme.mix(c, new Color(0x1E2747), 0.12));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            b.addActionListener(e -> {
                picked[0] = pi;
                dd[0].dispose();
            });
            box.add(b);
            box.add(Box.createVerticalStrut(8));
        }
        Widgets.PillButton cancel = new Widgets.PillButton("Cancel", new Color(0x9AA6C6));
        cancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        cancel.addActionListener(e -> dd[0].dispose());
        box.add(cancel);
        box.setPreferredSize(new Dimension(390, 78 + targets.size() * 54 + 46 + 26));

        dd[0] = Dialogs.framed(root, "Aim " + p.label(), box, true, Dialogs.Style.WHITE);
        dd[0].setVisible(true);
        return picked[0];
    }

    private void refreshShop() {
        if (powerShop == null) return;
        powerShop.setState(currentRole, myScore, roundActive, powerUsed, frozen);
    }

    // =========================================================================
    // ROOMS / LOBBY
    // =========================================================================

    /** The server answered a LIST_ROOMS request. Only react if this player actually asked for the list. */
    private void onRoomList(String message) {
        if (!roomCode.isEmpty() || !awaitingRoomList || closing) return;
        List<String[]> rooms = new ArrayList<>();
        String[] parts = message.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] p = parts[i].split(":", 3);
            if (p.length >= 2) rooms.add(new String[]{p[0], p[1], p.length == 3 ? p[2] : "?"});
        }
        if (roomBrowser != null && roomBrowser.isOpen()) {
            roomBrowser.update(rooms);
            return;
        }
        roomBrowser = new RoomBrowser(root, new RoomBrowser.Listener() {
            @Override public void onJoin(String code) { if (out != null) out.println("JOIN_ROOM " + code); }
            @Override public void onCreate() { if (out != null) out.println("CREATE_ROOM PUBLIC 8"); }
            @Override public void onRefresh() { if (out != null && roomCode.isEmpty()) out.println("LIST_ROOMS"); }
            @Override public void onBack() {
                awaitingRoomList = false;
                if (host != null) host.exitToMenu();
            }
        });
        roomBrowser.update(rooms);
        roomBrowser.show();
    }

    private void closeRoomBrowser() {
        if (roomBrowser != null) {
            roomBrowser.close();
            roomBrowser = null;
        }
    }

    private void onServerError(String text) {
        if (!roomCode.isEmpty()) {                       // inside a room: a small toast is enough
            toasts.show("Oops", text, Icons.BOLT, Theme.DANGER, 3400);
            return;
        }
        Dialogs.message(root, "Can't join", text, Dialogs.Kind.ERROR);
        if (roomBrowser == null || !roomBrowser.isOpen()) {
            if (host != null) host.exitToMenu();         // nothing left to do here: back to the start screen
        }
    }

    private void onConnectionLost() {
        closeRoomBrowser();
        if (host == null) {
            toasts.show("Disconnected", "The server connection was lost.", Icons.CLOCK, Theme.DANGER, 4000);
            return;
        }
        if (roomCode.isEmpty()) {
            Dialogs.message(root, "Disconnected", "The connection to the server was lost.", Dialogs.Kind.ERROR);
            host.exitToMenu();
        } else if (Dialogs.confirm(root, "Disconnected", "The connection to the server was lost.\nGo back to the main menu?", "Main menu", "Stay here", Dialogs.Kind.ERROR)) {
            host.exitToMenu();
        }
    }

    private LobbyPanel buildLobbyPanelIfNeeded() {
        if (lobbyPanel == null) {
            lobbyPanel = new LobbyPanel(new LobbyPanel.Listener() {
                @Override public void onStart() { if (out != null) out.println("START_GAME"); }
                @Override public void onSettings(int rounds, int seconds) { if (out != null) out.println("SET_SETTINGS " + rounds + " " + seconds); }
                @Override public void onCopy() {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(roomCode), null);
                    lobbyPanel.setCopied(true);
                }
                @Override public void onClose() { if (lobbyDialog != null) lobbyDialog.setVisible(false); }
            });
            lobbyPanel.setSettings(totalRounds, roundSeconds);
        }
        return lobbyPanel;
    }

    private void updateLobby(String message) {
        String[] p = message.split("\\|");
        if (p.length < 6) return;
        roomCode = p[1];
        hostId = parseIntSafe(p[2], -1);
        String status = p[3];
        int max = parseIntSafe(p[5], 8);
        List<String[]> people = new ArrayList<>();
        for (int i = 6; i < p.length; i++) {
            String[] person = p[i].split(":", 2);
            if (person.length == 2) people.add(new String[]{person[0], person[1]});
        }
        lobbyWaiting = status.equals("WAITING");

        LobbyPanel lp = buildLobbyPanelIfNeeded();
        lp.update(roomCode, hostId, people, playerName, playerId, max, lobbyWaiting);
        if (lobbyDialog == null || !lobbyDialog.isDisplayable()) {
            lobbyDialog = Dialogs.framed(root, "Game lobby", lp, false, Dialogs.Style.PASTEL);
        }
        if (!lobbyDialog.isVisible() && lobbyWaiting && !showingResults) lobbyDialog.setVisible(true);
    }

    // =========================================================================
    // PLAYERS / SCOREBOARD / RESULTS
    // =========================================================================

    private void updatePlayers(String message) {
        String[] entries = message.split("\\|");
        synchronized (roomPlayers) {
            roomPlayers.clear();
            for (int i = 1; i < entries.length; i++) {
                String[] p = entries[i].split(":", 4);
                if (p.length >= 4) {
                    int id = parseIntSafe(p[0], -1);
                    if (id >= 0) roomPlayers.put(id, new PlayerInfo(id, p[1], parseIntSafe(p[2], 0), p[3]));
                }
            }
            PlayerInfo me = roomPlayers.get(playerId);
            if (me != null) myScore = me.score;
        }
        SwingUtilities.invokeLater(this::refreshShop);
    }

    private void updateScoreboard(String message) {
        String[] entries = message.split("\\|");
        SwingUtilities.invokeLater(() -> {
            scores.clear();
            for (int i = 1; i < entries.length; i++) {
                int eq = entries[i].lastIndexOf('=');
                if (eq <= 0) continue;
                scores.put(entries[i].substring(0, eq), parseIntSafe(entries[i].substring(eq + 1), 0));
            }
            Integer mine = scores.get(playerName);
            if (mine != null) myScore = mine;
            refreshLeaderboard();
            refreshShop();
        });
    }

    private void refreshLeaderboard() {
        if (leaderboard == null) return;
        List<Leaderboard.Entry> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            String n = e.getKey();
            list.add(new Leaderboard.Entry(n, e.getValue(), n.equalsIgnoreCase(playerName),
                    (roundActive || choosingWord) && n.equals(currentDrawer), roundActive && guessedNames.contains(n)));
        }
        if (list.isEmpty()) list.add(new Leaderboard.Entry(playerName, 0, true, false, false));
        leaderboard.setEntries(list);
    }

    private void showGameOver(String message) {
        String[] entries = message.split("\\|");
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < entries.length; i++) {
            String[] p = entries[i].split(":", 3);
            if (p.length == 3) rows.add(new String[]{p[1], p[2]});
        }
        appendChat("System: Game finished - final results are in!");
        stopServerTimer();
        roundActive = false;
        choosingWord = false;
        currentRole = "WAITING";
        overlay.hideOverlay();
        toasts.hideRecap();
        topBar.word().setTitle("GAME OVER", Theme.ACCENT);
        topBar.word().setMessage("thanks for playing!");
        refreshShop();
        updateInputState();
        Sfx.play(Sfx.Sound.FANFARE);
        toasts.confetti(canvasArea(), 140);
        showingResults = true;
        ResultsDialog.show(root, rows, playerName);
        showingResults = false;
        if (lobbyDialog != null && lobbyWaiting) lobbyDialog.setVisible(true);
    }

    private void updateWordTitle() {
        boolean drawer = currentRole.equals("DRAWER");
        String prefix = drawer ? "YOUR WORD" : "GUESS THIS";
        String t = currentCategory.isEmpty() ? prefix : prefix + "  -  " + currentCategory.toUpperCase(Locale.ROOT);
        topBar.word().setTitle(t, drawer ? Theme.ACCENT : Theme.TEXT_MUTED);
    }

    // =========================================================================
    // TIMER
    // =========================================================================

    private void setServerTimer(int seconds) {
        stopServerTimer();
        timeLeft = Math.max(0, seconds);
        timeTotal = Math.max(1, seconds);
        topBar.setTime(timeLeft, timeTotal);
        gameTimer = new javax.swing.Timer(1000, e -> {
            if (timeLeft > 0) {
                timeLeft--;
                topBar.setTime(timeLeft, timeTotal);
                if (roundActive && timeLeft > 0 && timeLeft <= 5) Sfx.play(Sfx.Sound.TICK);
            }
        });
        gameTimer.start();
    }

    private void stopServerTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }
    }

    private int parseIntSafe(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return fallback; }
    }

    private static final class PlayerInfo {
        final int id;
        final String name;
        final int score;
        final String role;

        PlayerInfo(int id, String name, int score, String role) {
            this.id = id;
            this.name = name;
            this.score = score;
            this.role = role;
        }
    }

    // =========================================================================
    // SMALL CUSTOM COMPONENTS
    // =========================================================================

    private static final class Backdrop extends JPanel {
        Backdrop() { setOpaque(true); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            Theme.paintBackdrop(g, getWidth(), getHeight());
            g.dispose();
        }
    }

    /** Rounded input with placeholder text. */
    private static final class ChatField extends JTextField {
        private String hint = "";

        ChatField() {
            setOpaque(false);
            setForeground(Theme.TEXT);
            setCaretColor(Theme.TEXT);
            setDisabledTextColor(Theme.TEXT_MUTED);
            setFont(Theme.font(Font.PLAIN, 14));
            setBorder(new EmptyBorder(10, 16, 10, 12));
        }

        void setHint(String h) { hint = h; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.setColor(isEnabled() ? Theme.PANEL_LIGHT : Theme.alpha(Theme.PANEL_LIGHT, 150));
            g.fill(r);
            g.setColor(isFocusOwner() ? Theme.ACCENT_HOVER : Theme.BORDER);
            g.setStroke(new BasicStroke(isFocusOwner() ? 1.8f : 1.2f));
            g.draw(r);
            g.dispose();
            super.paintComponent(g0);
            if (getText().isEmpty()) {
                Graphics2D h = Theme.smooth(g0);
                h.setFont(getFont());
                h.setColor(Theme.alpha(Theme.TEXT_MUTED, isEnabled() ? 255 : 170));
                h.drawString(hint, getInsets().left, (getHeight() + h.getFontMetrics().getAscent() - h.getFontMetrics().getDescent()) / 2);
                h.dispose();
            }
        }
    }

    private static JScrollPane slim(JComponent c) {
        JScrollPane sp = new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = Theme.PANEL_RAISED;
                trackColor = new Color(0, 0, 0, 0);
            }

            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }

            private JButton zero() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override protected void paintTrack(Graphics g, JComponent comp, Rectangle r) { }

            @Override protected void paintThumb(Graphics g0, JComponent comp, Rectangle r) {
                Graphics2D g = Theme.smooth(g0);
                g.setColor(Theme.alpha(Theme.ACCENT, 90));
                g.fill(new RoundRectangle2D.Double(r.x + 1, r.y + 2, r.width - 2, r.height - 4, 8, 8));
                g.dispose();
            }
        });
        return sp;
    }
}
