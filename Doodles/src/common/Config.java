package common;

/**
 * Network settings shared by the server and every client, so they can never disagree.
 *
 * Defaults work for "everything on one computer". To play across two computers start the
 * clients with  -Ddoodles.host=<server IP>  (or set the DOODLES_HOST environment variable).
 * The port can be changed on BOTH sides with -Ddoodles.port=NNNN (or DOODLES_PORT).
 *
 * Port 5051 (not 5000): on macOS port 5000 is taken by the AirPlay Receiver, which accepts
 * connections and then never answers, so a client would wait forever.
 */
public final class Config {

    public static final String HOST = text("doodles.host", "DOODLES_HOST", "localhost");
    public static final int PORT = number("doodles.port", "DOODLES_PORT", 5051);

    private Config() {}

    private static String text(String property, String env, String fallback) {
        String v = System.getProperty(property);
        if (v == null || v.isBlank()) v = System.getenv(env);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static int number(String property, String env, int fallback) {
        try {
            return Integer.parseInt(text(property, env, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
