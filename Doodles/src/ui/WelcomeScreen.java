package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Start window. All the visuals live in {@link WelcomePanel}; this class only hosts it and
 * opens the game window with the chosen way of playing.
 */
public class WelcomeScreen extends JFrame {

    private final WelcomePanel panel;

    public WelcomeScreen() {
        this("");
    }

    public WelcomeScreen(String nickname) {
        Widgets.installDefaults();
        setTitle("DOODLES");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(740, 800);
        setMinimumSize(new Dimension(640, 740));
        setLocationRelativeTo(null);

        panel = new WelcomePanel(this::startGame, () -> createRoom(false), () -> createRoom(true), this::joinRoom);
        if (nickname != null && !nickname.isEmpty()) panel.setNickname(nickname);
        setContentPane(panel);
    }

    /** Called when the game window sends the player back here. */
    public void reveal() {
        setVisible(true);
        toFront();
        panel.focusName();
    }

    private String requireName(String message) {
        String name = panel.getNickname();
        if (name.isEmpty()) {
            Dialogs.message(this, "Nickname needed", message, Dialogs.Kind.WARN);
            panel.focusName();
            return null;
        }
        return name;
    }

    /** Hides this window (it comes back if joining fails) and opens the game window. */
    private void open(String name, String action, String code) {
        setVisible(false);
        SwingUtilities.invokeLater(() -> new client.GameWindow(name, action, code, this));
    }

    private void startGame() {
        String name = requireName("Please enter a nickname to play.");
        if (name != null) open(name, "QUICK", "");
    }

    private void createRoom(boolean privateRoom) {
        String name = requireName("Please enter a nickname first.");
        if (name != null) open(name, privateRoom ? "CREATE_PRIVATE" : "CREATE_PUBLIC", "");
    }

    private void joinRoom() {
        String name = requireName("Please enter a nickname first.");
        if (name == null) return;

        int mode = Dialogs.choose(this, "Join a room", "What kind of room do you want to join?",
                new String[]{"Public room", "Private room"}, new Color[]{Theme.ACCENT, Theme.ACCENT_PINK});
        if (mode < 0) return;

        if (mode == 0) {
            open(name, "LIST", "");
        } else {
            String code = Dialogs.input(this, "Private room", "Enter the 5-character room code your friend shared with you.",
                    "A7K2Q", 5, 5, "[A-Za-z0-9]", true, true);
            if (code == null) return;
            open(name, "JOIN", code.trim().toUpperCase());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WelcomeScreen().setVisible(true));
    }
}
