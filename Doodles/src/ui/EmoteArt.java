package ui;

import common.Emotes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Vector-drawn emoji and stickers (no font or image files needed, identical on every machine).
 * Everything is painted in a unit square and scaled, so it stays crisp at any size.
 */
public final class EmoteArt {

    private EmoteArt() {}

    private static final Color INK = new Color(0x4A3410);
    private static final Color SKIN_LIGHT = new Color(0xFFE88A);
    private static final Color SKIN_DARK = new Color(0xFFC63A);
    private static final Color SKIN_EDGE = new Color(0xE7A400);

    // ------------------------------------------------------------------ icon wrapper

    public static Icon icon(String id, int size) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = Theme.smooth(g);
                paintEmoji(g2, id, x, y, size);
                g2.dispose();
            }

            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon stickerIcon(String id, int w, int h) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = Theme.smooth(g);
                paintSticker(g2, id, x, y, w, h, true);
                g2.dispose();
            }

            @Override public int getIconWidth() { return w; }
            @Override public int getIconHeight() { return h; }
        };
    }

    // ------------------------------------------------------------------ emoji

    public static void paintEmoji(Graphics2D g0, String id, double x, double y, double size) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(size, size);
        switch (id) {
            case "heart" -> heart(g);
            case "star" -> star(g);
            case "fire" -> fire(g);
            case "thumbsup" -> thumb(g, false);
            case "thumbsdown" -> thumb(g, true);
            case "trophy" -> trophy(g);
            case "crown" -> crown(g);
            case "bulb" -> bulb(g);
            case "rocket" -> rocket(g);
            case "gift" -> gift(g);
            case "hundred" -> hundred(g);
            case "palette" -> palette(g);
            case "skull" -> skull(g);
            case "ghost" -> ghost(g);
            case "sparkle" -> sparkle(g);
            case "check" -> check(g);
            default -> face(g, id);
        }
        g.dispose();
    }

    private static Stroke line(double w) {
        return new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static void arc(Graphics2D g, double cx, double cy, double w, double h, double start, double ext, double sw, Color c) {
        g.setColor(c);
        g.setStroke(line(sw));
        g.draw(new Arc2D.Double(cx - w / 2, cy - h / 2, w, h, start, ext, Arc2D.OPEN));
    }

    private static void dot(Graphics2D g, double cx, double cy, double rx, double ry, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
    }

    private static void miniHeart(Graphics2D g, double cx, double cy, double s, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy + s * 0.9);
        p.curveTo(cx - s * 1.5, cy - s * 0.1, cx - s * 0.7, cy - s * 1.1, cx, cy - s * 0.3);
        p.curveTo(cx + s * 0.7, cy - s * 1.1, cx + s * 1.5, cy - s * 0.1, cx, cy + s * 0.9);
        g.setColor(c);
        g.fill(p);
    }

    // ---- faces ----

    private static void face(Graphics2D g, String id) {
        Color top = SKIN_LIGHT, bottom = SKIN_DARK, edge = SKIN_EDGE;
        if (id.equals("angry")) { top = new Color(0xFFB27A); bottom = new Color(0xFF6A4D); edge = new Color(0xD93F2B); }
        if (id.equals("scream")) { top = new Color(0xC9E6FF); bottom = new Color(0x86BFFF); edge = new Color(0x4D8FE0); }
        if (id.equals("sick")) { top = new Color(0xE5F5A6); bottom = new Color(0xB7DB5A); }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(0.4, 0.32), 0.7f, new float[]{0f, 1f}, new Color[]{top, bottom}));
        g.fill(new Ellipse2D.Double(0.03, 0.03, 0.94, 0.94));
        g.setColor(edge);
        g.setStroke(line(0.035));
        g.draw(new Ellipse2D.Double(0.04, 0.04, 0.92, 0.92));

        double lx = 0.34, rx = 0.66, ey = 0.42;
        switch (id) {
            case "smile" -> { eyes(g, lx, rx, ey); arc(g, 0.5, 0.52, 0.42, 0.34, 200, 140, 0.05, INK); }
            case "laugh" -> {
                arc(g, lx, ey + 0.03, 0.16, 0.14, 20, 140, 0.05, INK);
                arc(g, rx, ey + 0.03, 0.16, 0.14, 20, 140, 0.05, INK);
                openMouth(g, 0.5, 0.62, 0.52, 0.34, true);
            }
            case "wink" -> {
                arc(g, lx, ey + 0.03, 0.16, 0.12, 20, 140, 0.05, INK);
                dot(g, rx, ey, 0.045, 0.06, INK);
                arc(g, 0.5, 0.52, 0.42, 0.34, 200, 140, 0.05, INK);
            }
            case "love" -> {
                miniHeart(g, lx, ey, 0.1, new Color(0xFF3B6B));
                miniHeart(g, rx, ey, 0.1, new Color(0xFF3B6B));
                arc(g, 0.5, 0.52, 0.42, 0.34, 200, 140, 0.05, INK);
            }
            case "cool" -> {
                g.setColor(new Color(0x22283A));
                g.fill(new RoundRectangle2D.Double(0.2, 0.34, 0.27, 0.18, 0.1, 0.1));
                g.fill(new RoundRectangle2D.Double(0.53, 0.34, 0.27, 0.18, 0.1, 0.1));
                g.setStroke(line(0.04));
                g.draw(new Line2D.Double(0.46, 0.38, 0.54, 0.38));
                g.setColor(new Color(255, 255, 255, 120));
                g.fill(new Ellipse2D.Double(0.24, 0.36, 0.08, 0.04));
                g.fill(new Ellipse2D.Double(0.57, 0.36, 0.08, 0.04));
                arc(g, 0.52, 0.6, 0.34, 0.2, 200, 140, 0.05, INK);
            }
            case "cry" -> {
                eyes(g, lx, rx, ey);
                arc(g, 0.5, 0.72, 0.34, 0.24, 20, 140, 0.05, INK);
                tear(g, lx - 0.03, 0.52); tear(g, rx + 0.03, 0.52);
            }
            case "angry" -> {
                eyes(g, lx, rx, ey + 0.03);
                g.setColor(INK); g.setStroke(line(0.06));
                g.draw(new Line2D.Double(0.2, 0.26, 0.4, 0.34));
                g.draw(new Line2D.Double(0.8, 0.26, 0.6, 0.34));
                arc(g, 0.5, 0.74, 0.34, 0.22, 20, 140, 0.05, INK);
            }
            case "wow" -> {
                eyes(g, lx, rx, ey);
                arc(g, lx, ey - 0.12, 0.18, 0.1, 30, 120, 0.035, INK);
                arc(g, rx, ey - 0.12, 0.18, 0.1, 30, 120, 0.035, INK);
                g.setColor(new Color(0x7A2B1E));
                g.fill(new Ellipse2D.Double(0.42, 0.56, 0.16, 0.22));
            }
            case "think" -> {
                eyes(g, lx, rx, ey);
                g.setColor(INK); g.setStroke(line(0.05));
                g.draw(new Line2D.Double(0.62, 0.26, 0.82, 0.22));
                g.draw(new Path2D.Double(new Line2D.Double(0.36, 0.7, 0.62, 0.66)));
                dot(g, 0.88, 0.1, 0.05, 0.05, new Color(255, 255, 255, 200));
            }
            case "party" -> {
                eyes(g, lx, rx, ey + 0.04);
                arc(g, 0.5, 0.58, 0.42, 0.32, 200, 140, 0.05, INK);
                Path2D hat = new Path2D.Double();
                hat.moveTo(0.28, 0.24); hat.lineTo(0.5, -0.08); hat.lineTo(0.72, 0.24); hat.closePath();
                g.setPaint(new GradientPaint(0.3f, 0f, new Color(0xFF7EB6), 0.7f, 0.25f, new Color(0x7CC6FF)));
                g.fill(hat);
                dot(g, 0.5, -0.06, 0.05, 0.05, new Color(0xFFD447));
                dot(g, 0.12, 0.62, 0.025, 0.025, new Color(0xFF5C8A));
                dot(g, 0.9, 0.55, 0.025, 0.025, new Color(0x4DA3FF));
                dot(g, 0.86, 0.3, 0.02, 0.02, new Color(0x4DD6A0));
            }
            case "sleepy" -> {
                arc(g, lx, ey + 0.04, 0.16, 0.12, 200, 140, 0.05, INK);
                arc(g, rx, ey + 0.04, 0.16, 0.12, 200, 140, 0.05, INK);
                g.setColor(INK);
                g.fill(new Ellipse2D.Double(0.45, 0.62, 0.1, 0.1));
                g.setColor(new Color(0x4D8FE0));
                g.setFont(Theme.font(Font.BOLD, 1).deriveFont(0.26f));
                g.drawString("z", 0.74f, 0.26f);
                g.setFont(Theme.font(Font.BOLD, 1).deriveFont(0.18f));
                g.drawString("z", 0.86f, 0.14f);
            }
            case "tongue" -> {
                dot(g, lx, ey, 0.045, 0.06, INK);
                arc(g, rx, ey + 0.03, 0.16, 0.12, 20, 140, 0.05, INK);
                g.setColor(new Color(0x7A2B1E));
                g.fill(new RoundRectangle2D.Double(0.3, 0.58, 0.4, 0.12, 0.1, 0.1));
                g.setColor(new Color(0xFF6F91));
                g.fill(new RoundRectangle2D.Double(0.44, 0.62, 0.17, 0.22, 0.12, 0.12));
            }
            case "blush" -> {
                arc(g, lx, ey + 0.03, 0.16, 0.14, 20, 140, 0.05, INK);
                arc(g, rx, ey + 0.03, 0.16, 0.14, 20, 140, 0.05, INK);
                dot(g, 0.22, 0.58, 0.07, 0.045, new Color(255, 111, 145, 190));
                dot(g, 0.78, 0.58, 0.07, 0.045, new Color(255, 111, 145, 190));
                arc(g, 0.5, 0.54, 0.34, 0.26, 200, 140, 0.05, INK);
            }
            case "sweat" -> {
                eyes(g, lx, rx, ey);
                g.setColor(INK); g.setStroke(line(0.05));
                g.draw(new Line2D.Double(0.36, 0.7, 0.64, 0.7));
                tear(g, 0.84, 0.2);
            }
            case "scream" -> {
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(0.22, 0.3, 0.22, 0.26));
                g.fill(new Ellipse2D.Double(0.56, 0.3, 0.22, 0.26));
                dot(g, 0.33, 0.43, 0.04, 0.05, INK);
                dot(g, 0.67, 0.43, 0.04, 0.05, INK);
                g.setColor(new Color(0x2A2A55));
                g.fill(new Ellipse2D.Double(0.36, 0.6, 0.28, 0.3));
            }
            case "nerd" -> {
                eyes(g, lx, rx, ey);
                g.setColor(new Color(0x3A3A55)); g.setStroke(line(0.04));
                g.draw(new Ellipse2D.Double(0.2, 0.3, 0.26, 0.22));
                g.draw(new Ellipse2D.Double(0.54, 0.3, 0.26, 0.22));
                g.draw(new Line2D.Double(0.46, 0.41, 0.54, 0.41));
                arc(g, 0.5, 0.56, 0.4, 0.3, 200, 140, 0.05, INK);
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(0.46, 0.66, 0.08, 0.1, 0.02, 0.02));
            }
            default -> { eyes(g, lx, rx, ey); arc(g, 0.5, 0.52, 0.42, 0.34, 200, 140, 0.05, INK); }
        }
    }

    private static void eyes(Graphics2D g, double lx, double rx, double y) {
        dot(g, lx, y, 0.05, 0.065, INK);
        dot(g, rx, y, 0.05, 0.065, INK);
        dot(g, lx + 0.015, y - 0.02, 0.015, 0.018, Color.WHITE);
        dot(g, rx + 0.015, y - 0.02, 0.015, 0.018, Color.WHITE);
    }

    private static void openMouth(Graphics2D g, double cx, double cy, double w, double h, boolean tongue) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx - w / 2, cy - h / 2);
        p.curveTo(cx - w / 4, cy - h / 2 + 0.04, cx + w / 4, cy - h / 2 + 0.04, cx + w / 2, cy - h / 2);
        p.curveTo(cx + w / 2, cy + h / 2, cx - w / 2, cy + h / 2, cx - w / 2, cy - h / 2);
        g.setColor(new Color(0x6B1F1A));
        g.fill(p);
        if (tongue) {
            Shape old = g.getClip();
            g.clip(p);
            g.setColor(new Color(0xFF6F91));
            g.fill(new Ellipse2D.Double(cx - w * 0.28, cy + h * 0.12, w * 0.56, h * 0.6));
            g.setClip(old);
        }
    }

    private static void tear(Graphics2D g, double cx, double cy) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - 0.08);
        p.curveTo(cx + 0.07, cy + 0.02, cx + 0.06, cy + 0.1, cx, cy + 0.1);
        p.curveTo(cx - 0.06, cy + 0.1, cx - 0.07, cy + 0.02, cx, cy - 0.08);
        g.setColor(new Color(0x5CB8FF));
        g.fill(p);
    }

    // ---- objects ----

    private static void heart(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(0.5, 0.9);
        p.curveTo(0.0, 0.58, 0.04, 0.16, 0.28, 0.16);
        p.curveTo(0.4, 0.16, 0.47, 0.24, 0.5, 0.32);
        p.curveTo(0.53, 0.24, 0.6, 0.16, 0.72, 0.16);
        p.curveTo(0.96, 0.16, 1.0, 0.58, 0.5, 0.9);
        g.setPaint(new GradientPaint(0.2f, 0.1f, new Color(0xFF8FB0), 0.7f, 0.95f, new Color(0xFF3B6B)));
        g.fill(p);
        g.setColor(new Color(255, 255, 255, 150));
        g.fill(new Ellipse2D.Double(0.2, 0.26, 0.14, 0.09));
    }

    private static Path2D starPath(double cx, double cy, double ro, double ri, int pts) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < pts * 2; i++) {
            double r = i % 2 == 0 ? ro : ri;
            double a = -Math.PI / 2 + i * Math.PI / pts;
            double px = cx + Math.cos(a) * r, py = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    private static void star(Graphics2D g) {
        Path2D p = starPath(0.5, 0.54, 0.47, 0.2, 5);
        g.setPaint(new GradientPaint(0.3f, 0.1f, new Color(0xFFE770), 0.7f, 0.95f, new Color(0xFFB800)));
        g.fill(p);
        g.setColor(new Color(0xE79A00));
        g.setStroke(line(0.03));
        g.draw(p);
    }

    private static void fire(Graphics2D g) {
        Path2D o = new Path2D.Double();
        o.moveTo(0.5, 0.04);
        o.curveTo(0.55, 0.28, 0.9, 0.38, 0.84, 0.66);
        o.curveTo(0.8, 0.9, 0.6, 0.96, 0.5, 0.96);
        o.curveTo(0.3, 0.96, 0.14, 0.86, 0.16, 0.62);
        o.curveTo(0.18, 0.46, 0.3, 0.42, 0.34, 0.28);
        o.curveTo(0.4, 0.34, 0.42, 0.4, 0.44, 0.44);
        o.curveTo(0.5, 0.3, 0.46, 0.16, 0.5, 0.04);
        g.setPaint(new GradientPaint(0f, 0.1f, new Color(0xFFB347), 0f, 0.96f, new Color(0xFF4D4D)));
        g.fill(o);
        Path2D i = new Path2D.Double();
        i.moveTo(0.5, 0.48);
        i.curveTo(0.6, 0.6, 0.7, 0.66, 0.66, 0.8);
        i.curveTo(0.62, 0.92, 0.38, 0.92, 0.34, 0.8);
        i.curveTo(0.32, 0.68, 0.44, 0.62, 0.5, 0.48);
        g.setPaint(new GradientPaint(0f, 0.5f, new Color(0xFFF08A), 0f, 0.92f, new Color(0xFFC23D)));
        g.fill(i);
    }

    private static void thumb(Graphics2D g, boolean down) {
        if (down) { g.translate(0, 1); g.scale(1, -1); }
        g.setColor(new Color(0x5C8DFF));
        g.fill(new RoundRectangle2D.Double(0.06, 0.44, 0.2, 0.46, 0.06, 0.06));
        g.setPaint(new GradientPaint(0f, 0.2f, SKIN_LIGHT, 0f, 0.9f, SKIN_DARK));
        Path2D hand = new Path2D.Double();
        hand.moveTo(0.3, 0.46);
        hand.lineTo(0.46, 0.46);
        hand.curveTo(0.58, 0.34, 0.5, 0.2, 0.54, 0.1);
        hand.curveTo(0.66, 0.08, 0.7, 0.24, 0.64, 0.4);
        hand.lineTo(0.86, 0.4);
        hand.curveTo(0.96, 0.42, 0.96, 0.54, 0.9, 0.58);
        hand.curveTo(0.96, 0.62, 0.94, 0.72, 0.88, 0.74);
        hand.curveTo(0.92, 0.8, 0.86, 0.88, 0.8, 0.88);
        hand.lineTo(0.3, 0.88);
        hand.closePath();
        g.fill(hand);
        g.setColor(SKIN_EDGE);
        g.setStroke(line(0.025));
        g.draw(hand);
    }

    private static void trophy(Graphics2D g) {
        g.setColor(new Color(0xE79A00)); g.setStroke(line(0.06));
        g.draw(new Arc2D.Double(0.04, 0.16, 0.26, 0.3, 90, 180, Arc2D.OPEN));
        g.draw(new Arc2D.Double(0.7, 0.16, 0.26, 0.3, 90, -180, Arc2D.OPEN));
        Path2D cup = new Path2D.Double();
        cup.moveTo(0.2, 0.08); cup.lineTo(0.8, 0.08); cup.curveTo(0.8, 0.5, 0.7, 0.62, 0.5, 0.64);
        cup.curveTo(0.3, 0.62, 0.2, 0.5, 0.2, 0.08);
        g.setPaint(new GradientPaint(0.2f, 0f, new Color(0xFFE770), 0.8f, 0.7f, new Color(0xFFB800)));
        g.fill(cup);
        g.setColor(new Color(0xE79A00)); g.fill(new Rectangle2D.Double(0.44, 0.62, 0.12, 0.16));
        g.setPaint(new GradientPaint(0f, 0.78f, new Color(0x7C86A8), 0f, 0.94f, new Color(0x4D577A)));
        g.fill(new RoundRectangle2D.Double(0.28, 0.78, 0.44, 0.14, 0.05, 0.05));
        g.setColor(new Color(255, 255, 255, 140));
        g.fill(new RoundRectangle2D.Double(0.28, 0.14, 0.06, 0.3, 0.05, 0.05));
    }

    private static void crown(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(0.08, 0.76); p.lineTo(0.04, 0.26); p.lineTo(0.28, 0.5); p.lineTo(0.5, 0.14);
        p.lineTo(0.72, 0.5); p.lineTo(0.96, 0.26); p.lineTo(0.92, 0.76); p.closePath();
        g.setPaint(new GradientPaint(0f, 0.1f, new Color(0xFFE770), 0f, 0.8f, new Color(0xFFB800)));
        g.fill(p);
        g.setColor(new Color(0xE79A00)); g.setStroke(line(0.03)); g.draw(p);
        g.setColor(new Color(0xE79A00)); g.fill(new RoundRectangle2D.Double(0.08, 0.76, 0.84, 0.12, 0.05, 0.05));
        dot(g, 0.5, 0.6, 0.06, 0.06, new Color(0xFF3B6B));
        dot(g, 0.26, 0.68, 0.04, 0.04, new Color(0x4DA3FF));
        dot(g, 0.74, 0.68, 0.04, 0.04, new Color(0x4DD6A0));
    }

    private static void bulb(Graphics2D g) {
        g.setColor(new Color(0xFFD447)); g.setStroke(line(0.05));
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(-160 + i * 35);
            g.draw(new Line2D.Double(0.5 + Math.cos(a) * 0.42, 0.4 + Math.sin(a) * 0.42, 0.5 + Math.cos(a) * 0.5, 0.4 + Math.sin(a) * 0.5));
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(0.44, 0.3), 0.4f, new float[]{0f, 1f}, new Color[]{new Color(0xFFF6A8), new Color(0xFFC933)}));
        g.fill(new Ellipse2D.Double(0.22, 0.12, 0.56, 0.56));
        g.fill(new RoundRectangle2D.Double(0.36, 0.56, 0.28, 0.16, 0.05, 0.05));
        g.setColor(new Color(0x8A93B5));
        g.fill(new RoundRectangle2D.Double(0.36, 0.72, 0.28, 0.07, 0.03, 0.03));
        g.fill(new RoundRectangle2D.Double(0.38, 0.81, 0.24, 0.07, 0.03, 0.03));
    }

    private static void rocket(Graphics2D g) {
        g.translate(0.5, 0.5); g.rotate(Math.toRadians(40)); g.translate(-0.5, -0.5);
        Path2D flame = new Path2D.Double();
        flame.moveTo(0.4, 0.74); flame.curveTo(0.4, 0.92, 0.5, 1.0, 0.5, 1.0); flame.curveTo(0.5, 1.0, 0.6, 0.92, 0.6, 0.74);
        g.setPaint(new GradientPaint(0f, 0.74f, new Color(0xFFD447), 0f, 1f, new Color(0xFF6A4D)));
        g.fill(flame);
        g.setColor(new Color(0xFF5C7A));
        Path2D finL = new Path2D.Double(); finL.moveTo(0.36, 0.5); finL.lineTo(0.2, 0.72); finL.lineTo(0.38, 0.7); finL.closePath(); g.fill(finL);
        Path2D finR = new Path2D.Double(); finR.moveTo(0.64, 0.5); finR.lineTo(0.8, 0.72); finR.lineTo(0.62, 0.7); finR.closePath(); g.fill(finR);
        Path2D body = new Path2D.Double();
        body.moveTo(0.5, 0.02); body.curveTo(0.72, 0.18, 0.7, 0.5, 0.64, 0.76); body.lineTo(0.36, 0.76); body.curveTo(0.3, 0.5, 0.28, 0.18, 0.5, 0.02);
        g.setPaint(new GradientPaint(0.3f, 0f, Color.WHITE, 0.7f, 0f, new Color(0xCFD8F0)));
        g.fill(body);
        g.setColor(new Color(0xFF5C7A));
        Path2D nose = new Path2D.Double(); nose.moveTo(0.5, 0.02); nose.curveTo(0.6, 0.08, 0.66, 0.18, 0.68, 0.26); nose.lineTo(0.32, 0.26); nose.curveTo(0.34, 0.18, 0.4, 0.08, 0.5, 0.02);
        g.fill(nose);
        dot(g, 0.5, 0.44, 0.09, 0.09, new Color(0x5CB8FF));
        dot(g, 0.47, 0.41, 0.025, 0.025, new Color(255, 255, 255, 200));
    }

    private static void gift(Graphics2D g) {
        g.setPaint(new GradientPaint(0f, 0.4f, new Color(0xFF9EC7), 0f, 0.92f, new Color(0xFF6FAE)));
        g.fill(new RoundRectangle2D.Double(0.12, 0.4, 0.76, 0.5, 0.06, 0.06));
        g.setPaint(new GradientPaint(0f, 0.28f, new Color(0xFFB3D5), 0f, 0.44f, new Color(0xFF86C0)));
        g.fill(new RoundRectangle2D.Double(0.06, 0.28, 0.88, 0.16, 0.05, 0.05));
        g.setColor(new Color(0x7CC6FF));
        g.fill(new Rectangle2D.Double(0.45, 0.28, 0.1, 0.62));
        g.setColor(new Color(0x7CC6FF)); g.setStroke(line(0.055));
        g.draw(new Arc2D.Double(0.26, 0.08, 0.26, 0.24, 0, 200, Arc2D.OPEN));
        g.draw(new Arc2D.Double(0.48, 0.08, 0.26, 0.24, -20, 200, Arc2D.OPEN));
    }

    private static void hundred(Graphics2D g) {
        g.setColor(new Color(0xFF4D6A));
        Font f = Theme.font(Font.BOLD, 1).deriveFont(0.58f);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String s = "100";
        g.drawString(s, (float) (0.5 - fm.stringWidth(s) / 2.0), 0.58f);
        g.setStroke(line(0.05));
        g.draw(new Line2D.Double(0.14, 0.72, 0.86, 0.72));
        g.draw(new Line2D.Double(0.22, 0.84, 0.78, 0.84));
    }

    private static void palette(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(0.5, 0.1);
        p.curveTo(0.88, 0.1, 0.98, 0.44, 0.88, 0.64);
        p.curveTo(0.8, 0.8, 0.66, 0.66, 0.6, 0.76);
        p.curveTo(0.56, 0.9, 0.4, 0.92, 0.26, 0.84);
        p.curveTo(0.04, 0.72, 0.02, 0.1, 0.5, 0.1);
        g.setPaint(new GradientPaint(0f, 0.1f, new Color(0xFFE2B8), 0f, 0.9f, new Color(0xE8B87C)));
        g.fill(p);
        g.setColor(new Color(0xC48B4D)); g.setStroke(line(0.03)); g.draw(p);
        dot(g, 0.3, 0.34, 0.07, 0.07, new Color(0xFF5C7A));
        dot(g, 0.5, 0.24, 0.07, 0.07, new Color(0xFFD447));
        dot(g, 0.7, 0.34, 0.07, 0.07, new Color(0x4DD6A0));
        dot(g, 0.24, 0.56, 0.07, 0.07, new Color(0x4DA3FF));
        dot(g, 0.42, 0.7, 0.05, 0.05, new Color(255, 255, 255, 220));
    }

    private static void skull(Graphics2D g) {
        g.setPaint(new GradientPaint(0f, 0.1f, Color.WHITE, 0f, 0.9f, new Color(0xD3DAEE)));
        g.fill(new Ellipse2D.Double(0.14, 0.08, 0.72, 0.66));
        g.fill(new RoundRectangle2D.Double(0.3, 0.6, 0.4, 0.3, 0.08, 0.08));
        g.setColor(new Color(0x9AA3C4)); g.setStroke(line(0.025));
        g.draw(new Ellipse2D.Double(0.14, 0.08, 0.72, 0.66));
        dot(g, 0.35, 0.42, 0.1, 0.12, new Color(0x2A2F4A));
        dot(g, 0.65, 0.42, 0.1, 0.12, new Color(0x2A2F4A));
        Path2D nose = new Path2D.Double(); nose.moveTo(0.5, 0.54); nose.lineTo(0.45, 0.64); nose.lineTo(0.55, 0.64); nose.closePath();
        g.setColor(new Color(0x2A2F4A)); g.fill(nose);
        g.setStroke(line(0.025));
        for (double x : new double[]{0.4, 0.5, 0.6}) g.draw(new Line2D.Double(x, 0.74, x, 0.88));
    }

    private static void ghost(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(0.16, 0.9); p.lineTo(0.16, 0.42);
        p.curveTo(0.16, 0.0, 0.84, 0.0, 0.84, 0.42);
        p.lineTo(0.84, 0.9);
        p.lineTo(0.72, 0.8); p.lineTo(0.6, 0.9); p.lineTo(0.5, 0.8); p.lineTo(0.4, 0.9); p.lineTo(0.28, 0.8);
        p.closePath();
        g.setPaint(new GradientPaint(0f, 0.1f, Color.WHITE, 0f, 0.9f, new Color(0xD9DFF5)));
        g.fill(p);
        g.setColor(new Color(0xA9B2D6)); g.setStroke(line(0.025)); g.draw(p);
        dot(g, 0.38, 0.4, 0.06, 0.08, new Color(0x2A2F4A));
        dot(g, 0.62, 0.4, 0.06, 0.08, new Color(0x2A2F4A));
        g.setColor(new Color(0x2A2F4A));
        g.fill(new Ellipse2D.Double(0.45, 0.54, 0.1, 0.12));
    }

    private static void sparkle(Graphics2D g) {
        g.setPaint(new GradientPaint(0.2f, 0.1f, new Color(0x9EE4FF), 0.8f, 0.9f, new Color(0x4DA3FF)));
        g.fill(starPath(0.42, 0.56, 0.4, 0.09, 4));
        g.setPaint(new GradientPaint(0.6f, 0f, new Color(0xFFD1E6), 0.9f, 0.4f, new Color(0xFF6FAE)));
        g.fill(starPath(0.78, 0.22, 0.2, 0.05, 4));
        g.setColor(new Color(0xFFD447));
        g.fill(starPath(0.8, 0.8, 0.13, 0.035, 4));
    }

    private static void check(Graphics2D g) {
        g.setPaint(new GradientPaint(0f, 0f, new Color(0x7CE8B8), 0f, 1f, new Color(0x2FBF86)));
        g.fill(new Ellipse2D.Double(0.04, 0.04, 0.92, 0.92));
        g.setColor(Color.WHITE); g.setStroke(line(0.1));
        Path2D p = new Path2D.Double(); p.moveTo(0.27, 0.52); p.lineTo(0.43, 0.68); p.lineTo(0.74, 0.34);
        g.draw(p);
    }

    // ------------------------------------------------------------------ stickers

    private static String stickerFace(String id) {
        return switch (id) {
            case "gg" -> "trophy"; case "wow" -> "wow"; case "lol" -> "laugh"; case "nice" -> "thumbsup";
            case "oops" -> "sweat"; case "help" -> "scream"; case "genius" -> "bulb"; case "close" -> "cry";
            case "bravo" -> "party"; case "hmm" -> "think"; case "hi" -> "wink"; case "bye" -> "sleepy";
            case "love" -> "love"; case "epic" -> "fire"; default -> "star";
        };
    }

    private static Color[] stickerColors(String id) {
        return switch (id) {
            case "gg" -> new Color[]{new Color(0x9ED4FF), new Color(0x4DA3FF)};
            case "wow" -> new Color[]{new Color(0xFFB3D5), new Color(0xFF6FAE)};
            case "lol" -> new Color[]{new Color(0xFFE88A), new Color(0xFFB84D)};
            case "nice" -> new Color[]{new Color(0xA6F2CF), new Color(0x4DD6A0)};
            case "oops" -> new Color[]{new Color(0xD3C6FF), new Color(0x9C86FF)};
            case "help" -> new Color[]{new Color(0xFFB3B3), new Color(0xFF6B8A)};
            case "genius" -> new Color[]{new Color(0xB3EBFF), new Color(0x5CC8FF)};
            case "close" -> new Color[]{new Color(0xFFD3B3), new Color(0xFF9F6B)};
            case "bravo" -> new Color[]{new Color(0xFFC2E0), new Color(0xFF86C0)};
            case "hmm" -> new Color[]{new Color(0xCFDBFF), new Color(0x8FA8FF)};
            case "hi" -> new Color[]{new Color(0xB5F5E0), new Color(0x5EDCB5)};
            case "bye" -> new Color[]{new Color(0xE0D2FF), new Color(0xA98BFF)};
            case "love" -> new Color[]{new Color(0xFFC0D4), new Color(0xFF6F9C)};
            case "epic" -> new Color[]{new Color(0xFFD1A0), new Color(0xFF8A4D)};
            default -> new Color[]{new Color(0xCFE8FF), new Color(0x7CC6FF)};
        };
    }

    /** A die-cut style sticker: white outline, soft shadow, slight tilt (when {@code tilt} is true). */
    public static void paintSticker(Graphics2D g0, String id, double x, double y, double w, double h, boolean tilt) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (tilt) g.rotate(Math.toRadians(Math.abs(id.hashCode()) % 2 == 0 ? -3 : 3), x + w / 2, y + h / 2);

        double m = Math.max(2, h * 0.07);                       // white border thickness
        RoundRectangle2D outer = new RoundRectangle2D.Double(x + 1, y + 1, w - 2 - 2, h - 2 - 3, h * 0.42, h * 0.42);
        g.setColor(new Color(40, 60, 110, 38));
        g.fill(new RoundRectangle2D.Double(outer.getX() + 1, outer.getY() + 3, outer.getWidth(), outer.getHeight(), h * 0.42, h * 0.42));
        g.setColor(Color.WHITE);
        g.fill(outer);

        Color[] c = stickerColors(id);
        RoundRectangle2D inner = new RoundRectangle2D.Double(outer.getX() + m, outer.getY() + m, outer.getWidth() - 2 * m, outer.getHeight() - 2 * m, h * 0.34, h * 0.34);
        g.setPaint(new GradientPaint(0f, (float) inner.getY(), c[0], 0f, (float) (inner.getY() + inner.getHeight()), c[1]));
        g.fill(inner);
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new RoundRectangle2D.Double(inner.getX() + 4, inner.getY() + 3, inner.getWidth() - 8, inner.getHeight() * 0.38, h * 0.3, h * 0.3));

        double face = inner.getHeight() * 0.86;
        paintEmoji(g, stickerFace(id), inner.getX() + inner.getHeight() * 0.1, inner.getY() + (inner.getHeight() - face) / 2, face);

        String text = Emotes.stickerCaption(id);
        Color ink = Theme.mix(c[1], new Color(0x1E2747), 0.62);
        float fs = (float) (inner.getHeight() * 0.42);
        Font f = Theme.font(Font.BOLD, 1).deriveFont(fs);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        double avail = inner.getMaxX() - (inner.getX() + inner.getHeight() * 1.05) - h * 0.1;
        while (fm.stringWidth(text) > avail && fs > 7) {
            fs -= 0.5f;
            g.setFont(f.deriveFont(fs));
            fm = g.getFontMetrics();
        }
        double tx = inner.getX() + inner.getHeight() * 1.05 + (avail - fm.stringWidth(text)) / 2;
        double ty = inner.getCenterY() + (fm.getAscent() - fm.getDescent()) / 2.0;
        g.setColor(new Color(255, 255, 255, 140));
        g.drawString(text, (float) tx, (float) (ty + 1.2));
        g.setColor(ink);
        g.drawString(text, (float) tx, (float) ty);
        g.dispose();
    }
}
