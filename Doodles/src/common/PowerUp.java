package common;

import java.util.Locale;

/**
 * Single source of truth for every power-up in the game.
 * Used by the server (rules, cost, duration) and the client (shop, icons, text),
 * so the two can never disagree.
 */
public enum PowerUp {

    // ---- Drawer power-ups (aimed at one active guesser) ----
    BLUR("Blur", "Smudges a guesser's canvas.", Who.DRAWER, true, 300, 5),
    MIRROR("Flip", "Flips a guesser's canvas upside-down.", Who.DRAWER, true, 300, 6),
    SPOTLIGHT("Spotlight", "Guesser sees only a small circle.", Who.DRAWER, true, 350, 6),

    // ---- Guesser power-ups ----
    FREEZE("Freeze", "Another guesser can't type.", Who.GUESSER, true, 300, 5),
    HINT("Hint", "Reveals extra letters, just for you.", Who.GUESSER, false, 200, 0),
    DOUBLE("Double", "Next correct guess = 2x points.", Who.GUESSER, false, 250, 0),
    SHIELD("Shield", "Blocks the next power-up at you.", Who.GUESSER, false, 150, 0);

    public enum Who { DRAWER, GUESSER }

    private final String label;
    private final String tagline;
    private final Who who;
    private final boolean targeted;
    private final int cost;
    private final int seconds;

    PowerUp(String label, String tagline, Who who, boolean targeted, int cost, int seconds) {
        this.label = label;
        this.tagline = tagline;
        this.who = who;
        this.targeted = targeted;
        this.cost = cost;
        this.seconds = seconds;
    }

    public String label() { return label; }
    public String tagline() { return tagline; }
    public Who who() { return who; }
    public boolean isTargeted() { return targeted; }
    public int cost() { return cost; }
    /** Duration of a timed effect in seconds (0 = instant / lasts until used). */
    public int seconds() { return seconds; }
    /** Timed effects that land on another player's screen. */
    public boolean isTimedEffect() { return targeted; }

    public static PowerUp parse(String text) {
        if (text == null) return null;
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
