package common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The emoji and sticker catalogue shared by the client (drawing/picker) and the server (validation).
 * Emoji travel inside chat text as :id: tokens, stickers travel as their own STICKER command.
 */
public final class Emotes {

    private Emotes() {}

    public static final String[] EMOJI = {
            "smile", "laugh", "wink", "love", "cool", "cry", "angry", "wow",
            "think", "party", "sleepy", "tongue", "blush", "sweat", "scream", "nerd",
            "heart", "star", "fire", "thumbsup", "thumbsdown", "trophy", "crown", "bulb",
            "rocket", "gift", "hundred", "palette", "skull", "ghost", "sparkle", "check"
    };

    /** id, caption (also the text shown on the sticker). */
    public static final String[][] STICKERS = {
            {"gg", "GG"}, {"wow", "WOW!"}, {"lol", "LOL"}, {"nice", "NICE!"},
            {"oops", "OOPS"}, {"help", "HELP!"}, {"genius", "GENIUS"}, {"close", "SO CLOSE"},
            {"bravo", "BRAVO"}, {"hmm", "HMMM?"}, {"hi", "HI!"}, {"bye", "BYE"},
            {"love", "LOVE IT"}, {"epic", "EPIC"}
    };

    private static final Pattern TOKEN = Pattern.compile(":([a-z0-9]{2,12}):");

    public static boolean isEmoji(String id) {
        for (String e : EMOJI) if (e.equals(id)) return true;
        return false;
    }

    public static boolean isSticker(String id) {
        for (String[] s : STICKERS) if (s[0].equals(id)) return true;
        return false;
    }

    public static String stickerCaption(String id) {
        for (String[] s : STICKERS) if (s[0].equals(id)) return s[1];
        return id;
    }

    /** Removes every valid :emoji: token (used before checking a chat line as a guess). */
    public static String stripTokens(String text) {
        Matcher m = TOKEN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, isEmoji(m.group(1)) ? " " : Matcher.quoteReplacement(m.group()));
        m.appendTail(sb);
        return sb.toString().trim();
    }

    /** Turns typed smileys like :) or <3 into :emoji: tokens. */
    public static String applyShortcuts(String text) {
        String t = " " + text + " ";
        String[][] map = {
                {":)", ":smile:"}, {":-)", ":smile:"}, {":D", ":laugh:"}, {"xD", ":laugh:"}, {";)", ":wink:"},
                {"<3", ":heart:"}, {":(", ":cry:"}, {":'(", ":cry:"}, {":O", ":wow:"}, {":o", ":wow:"},
                {"B)", ":cool:"}, {":P", ":tongue:"}, {":p", ":tongue:"}, {">:(", ":angry:"}, {":/", ":think:"}
        };
        for (String[] m : map) t = t.replace(" " + m[0] + " ", " " + m[1] + " ");
        return t.trim();
    }
}
