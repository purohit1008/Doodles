package client;

import ui.Dialogs;
import ui.WelcomeScreen;
import ui.Widgets;

import javax.swing.*;
import java.awt.*;

/** The in-game frame. All the game logic lives in {@link GameScreen}. */
public class GameWindow extends JFrame {

    /** How long a server may stay silent before we give up on it. */
    private static final int GREETING_TIMEOUT_MS = 6000;

    private final GameScreen screen;
    private final String playerName;
    private final WelcomeScreen origin;      // start screen that is hidden (not closed) while we connect
    private boolean originAlive;
    private boolean leaving;

    public GameWindow(String playerName) {
        this(playerName, "QUICK", "", null);
    }

    public GameWindow(String playerName, String roomAction, String roomCode) {
        this(playerName, roomAction, roomCode, null);
    }

    public GameWindow(String playerName, String roomAction, String roomCode, WelcomeScreen origin) {
        this.playerName = playerName;
        this.origin = origin;
        this.originAlive = origin != null;

        Widgets.installDefaults();
        setTitle("DOODLES - " + playerName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1240, 780));
        setSize(1420, 880);
        setLocationRelativeTo(null);

        screen = new GameScreen(playerName, roomAction, roomCode);
        setContentPane(screen.root());
        setGlassPane(screen.toasts());
        screen.toasts().setVisible(true);
        screen.setHost(new GameScreen.Host() {
            @Override public void roomEntered() { showGame(); }
            @Override public void exitToMenu() { backToMenu(); }
        });

        String problem = screen.connect();
        if (problem != null) {
            Dialogs.message(originAlive ? origin : null, "Can't connect", problem, Dialogs.Kind.ERROR);
            backToMenu();
            return;
        }

        // Something accepted the connection but it is not (or not yet) a Doodles server, for example
        // a different program on the same port. Don't leave the player in front of a dead window:
        // say what is wrong and go back to the menu.
        javax.swing.Timer watchdog = new javax.swing.Timer(GREETING_TIMEOUT_MS, e -> {
            if (leaving || screen.serverGreeted()) return;
            Dialogs.message(originAlive ? origin : null, "No answer from the server",
                    "Something is listening on " + GameScreen.serverAddress() + " but it is not the Doodles server.\n\n"
                            + "Make sure GameServer is running and that the server and the game use the same port.",
                    Dialogs.Kind.ERROR);
            backToMenu();
        });
        watchdog.setRepeats(false);
        watchdog.start();
    }

    /** The game window stays hidden until we really are inside a room. */
    private void showGame() {
        if (!isVisible()) setVisible(true);
        if (originAlive) {
            originAlive = false;
            origin.dispose();
        }
    }

    private void backToMenu() {
        SwingUtilities.invokeLater(() -> {
            if (leaving) return;
            leaving = true;
            if (originAlive) {
                originAlive = false;
                origin.reveal();
            } else {
                new WelcomeScreen(playerName).setVisible(true);
            }
            dispose();
        });
    }

    @Override
    public void dispose() {
        if (screen != null) screen.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow("TestPlayer");
            window.setVisible(true);
        });
    }
}
