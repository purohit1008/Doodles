package client;

import ui.Icons;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * Every brush / stroke style in the game.
 *
 * Rendering is fully deterministic (no hidden state, randomness is seeded from the
 * pixel position), so the drawer, every guesser and every undo-redraw produce
 * exactly the same picture from the same list of points.
 */
public enum Brush {

    PEN(0, "Pen", Icons.PEN, "Clean, smooth line"),
    MARKER(1, "Marker", Icons.MARKER, "Chunky square-tip marker"),
    CRAYON(2, "Crayon", Icons.CRAYON, "Waxy, grainy texture"),
    SPRAY(3, "Spray", Icons.SPRAY, "Soft airbrush spray"),
    CALLIGRAPHY(4, "Calligraphy", Icons.CALLIGRAPHY, "Angled nib, thick & thin strokes"),
    NEON(5, "Neon", Icons.NEON, "Glowing light tube"),
    RAINBOW(6, "Rainbow", Icons.RAINBOW, "Colour shifts as you draw"),
    DASHED(7, "Dashed", Icons.DASHED, "Dashed line stroke"),
    DOTTED(8, "Dotted", Icons.DOTTED, "Dotted trail stroke");

    private final int id;
    private final String label;
    private final String iconId;
    private final String tip;

    Brush(int id, String label, String iconId, String tip) {
        this.id = id;
        this.label = label;
        this.iconId = iconId;
        this.tip = tip;
    }

    public int id() { return id; }
    public String label() { return label; }
    public String iconId() { return iconId; }
    public String tip() { return tip; }

    public static Brush byId(int id) {
        for (Brush b : values()) if (b.id == id) return b;
        return PEN;
    }

    // =========================================================================
    // RENDERING
    // =========================================================================

    /**
     * Draws one piece of a stroke.
     *
     * @param prev  previous point of the stroke, or null for the first dab
     * @param dist  distance already travelled along this stroke (drives dashes, dots, rainbow)
     * @return the new travelled distance
     */
    public double draw(Graphics2D g, Point prev, Point cur, Color color, int size, double dist) {
        double len = prev == null ? 0 : prev.distance(cur);
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        switch (this) {
            case PEN -> pen(g, prev, cur, color, size);
            case MARKER -> marker(g, prev, cur, color, size);
            case CRAYON -> crayon(g, prev, cur, color, size);
            case SPRAY -> spray(g, prev, cur, color, size);
            case CALLIGRAPHY -> calligraphy(g, prev, cur, color, size);
            case NEON -> neon(g, prev, cur, color, size);
            case RAINBOW -> {
                float hue = (float) (((dist + len / 2.0) * 0.0045) % 1.0);
                pen(g, prev, cur, Color.getHSBColor(hue, 0.82f, 1.0f), size);
            }
            case DASHED -> dashed(g, prev, cur, color, size, dist, len);
            case DOTTED -> dotted(g, prev, cur, color, size, dist, len);
        }

        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
        if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        return dist + len;
    }

    // ---------------------------------------------------------------- PEN

    private static void pen(Graphics2D g, Point a, Point b, Color c, int size) {
        g.setColor(c);
        if (a == null) {
            dab(g, b.x, b.y, size);
        } else {
            g.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    // ---------------------------------------------------------------- MARKER

    private static void marker(Graphics2D g, Point a, Point b, Color c, int size) {
        float w = Math.max(3f, size * 1.5f);
        g.setColor(c);
        if (a == null) {
            g.fill(new java.awt.geom.Rectangle2D.Double(b.x - w / 2.0, b.y - w / 2.0, w, w));
        } else {
            g.setStroke(new BasicStroke(w, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    // ---------------------------------------------------------------- CRAYON

    private static void crayon(Graphics2D g, Point a, Point b, Color c, int size) {
        // faint waxy core so the stroke never looks empty
        g.setColor(Theme_alpha(c, 105));
        if (a == null) {
            dab(g, b.x, b.y, Math.max(2, Math.round(size * 0.6f)));
        } else {
            g.setStroke(new BasicStroke(Math.max(2f, size * 0.6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(a.x, a.y, b.x, b.y);
        }
        double step = Math.max(1.0, size / 4.0);
        int n = a == null ? 1 : Math.max(1, (int) Math.ceil(a.distance(b) / step));
        int grain = size > 12 ? 2 : 1;
        for (int i = 0; i <= n; i++) {
            double t = a == null ? 1 : i / (double) n;
            int px = a == null ? b.x : (int) Math.round(a.x + (b.x - a.x) * t);
            int py = a == null ? b.y : (int) Math.round(a.y + (b.y - a.y) * t);
            Random r = new Random(seed(px, py, size, 11));
            int count = size * 2 + 5;
            for (int k = 0; k < count; k++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double rad = Math.sqrt(r.nextDouble()) * size * 0.55;
                int alpha = 120 + r.nextInt(120);
                g.setColor(Theme_alpha(c, alpha));
                g.fillRect((int) Math.round(px + Math.cos(ang) * rad), (int) Math.round(py + Math.sin(ang) * rad), grain, grain);
            }
        }
    }

    // ---------------------------------------------------------------- SPRAY

    private static void spray(Graphics2D g, Point a, Point b, Color c, int size) {
        double step = Math.max(1.0, size / 5.0);
        int n = a == null ? 1 : Math.max(1, (int) Math.ceil(a.distance(b) / step));
        double radius = Math.max(4, size * 1.5);
        for (int i = 0; i <= n; i++) {
            double t = a == null ? 1 : i / (double) n;
            int px = a == null ? b.x : (int) Math.round(a.x + (b.x - a.x) * t);
            int py = a == null ? b.y : (int) Math.round(a.y + (b.y - a.y) * t);
            Random r = new Random(seed(px, py, size, 29));
            int count = 12 + size * 2;
            for (int k = 0; k < count; k++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double rad = Math.min(radius, Math.abs(r.nextGaussian()) * radius * 0.5);
                g.setColor(Theme_alpha(c, 70 + r.nextInt(150)));
                g.fillRect((int) Math.round(px + Math.cos(ang) * rad), (int) Math.round(py + Math.sin(ang) * rad), 1, 1);
            }
        }
    }

    // ---------------------------------------------------------------- CALLIGRAPHY

    private static void calligraphy(Graphics2D g, Point a, Point b, Color c, int size) {
        double half = Math.max(2.0, size * 0.75);
        double dx = Math.cos(Math.toRadians(-45)) * half;
        double dy = Math.sin(Math.toRadians(-45)) * half;
        g.setColor(c);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (a == null) {
            g.draw(new Line2D.Double(b.x - dx, b.y - dy, b.x + dx, b.y + dy));
            return;
        }
        Path2D p = new Path2D.Double();
        p.moveTo(a.x + dx, a.y + dy);
        p.lineTo(a.x - dx, a.y - dy);
        p.lineTo(b.x - dx, b.y - dy);
        p.lineTo(b.x + dx, b.y + dy);
        p.closePath();
        g.fill(p);
        g.draw(p);
    }

    // ---------------------------------------------------------------- NEON

    private static void neon(Graphics2D g, Point a, Point b, Color c, int size) {
        int[] widths = {Math.round(size * 2.8f) + 2, Math.round(size * 1.8f) + 1, Math.round(size * 1.0f)};
        int[] alphas = {26, 55, 120};
        for (int i = 0; i < widths.length; i++) {
            g.setColor(Theme_alpha(c, alphas[i]));
            if (a == null) {
                dab(g, b.x, b.y, widths[i]);
            } else {
                g.setStroke(new BasicStroke(widths[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }
        g.setColor(c);
        int core = Math.max(1, Math.round(size * 0.5f));
        if (a == null) {
            dab(g, b.x, b.y, core);
        } else {
            g.setStroke(new BasicStroke(core, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    // ---------------------------------------------------------------- DASHED

    private static void dashed(Graphics2D g, Point a, Point b, Color c, int size, double dist, double len) {
        g.setColor(c);
        if (a == null) {
            dab(g, b.x, b.y, size);
            return;
        }
        double dash = size * 2.6 + 5;
        double gap = size * 1.7 + 4;
        double period = dash + gap;
        g.setStroke(new BasicStroke(size, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        double s = 0;
        while (s < len) {
            double phase = (dist + s) % period;
            if (phase < dash) {
                double end = Math.min(len, s + (dash - phase));
                g.draw(new Line2D.Double(
                        a.x + (b.x - a.x) * (s / len), a.y + (b.y - a.y) * (s / len),
                        a.x + (b.x - a.x) * (end / len), a.y + (b.y - a.y) * (end / len)));
                s = end;
            } else {
                s += (period - phase);
            }
            if (s <= 0) break;
        }
    }

    // ---------------------------------------------------------------- DOTTED

    private static void dotted(Graphics2D g, Point a, Point b, Color c, int size, double dist, double len) {
        g.setColor(c);
        double diameter = Math.max(3, size);
        if (a == null) {
            dab(g, b.x, b.y, (int) diameter);
            return;
        }
        double spacing = size * 2.0 + 5;
        long before = (long) Math.floor(dist / spacing);
        long after = (long) Math.floor((dist + len) / spacing);
        for (long k = before + 1; k <= after; k++) {
            double t = len == 0 ? 0 : (k * spacing - dist) / len;
            double x = a.x + (b.x - a.x) * t;
            double y = a.y + (b.y - a.y) * t;
            g.fill(new java.awt.geom.Ellipse2D.Double(x - diameter / 2, y - diameter / 2, diameter, diameter));
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void dab(Graphics2D g, double x, double y, int size) {
        double d = Math.max(1, size);
        g.fill(new java.awt.geom.Ellipse2D.Double(x - d / 2.0, y - d / 2.0, d, d));
    }

    private static long seed(int x, int y, int size, int salt) {
        return (x * 73856093L) ^ (y * 19349663L) ^ (size * 83492791L) ^ (salt * 2654435761L);
    }

    private static Color Theme_alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
