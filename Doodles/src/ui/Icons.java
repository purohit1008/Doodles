package ui;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Hand-drawn vector icon set (24x24 design grid, scaled to any size).
 * No image files needed: every icon is pure Java2D, so it stays sharp at any size.
 */
public final class Icons {

    private Icons() {}

    // ---- Brush / tool ids ----
    public static final String PEN = "PEN", MARKER = "MARKER", CRAYON = "CRAYON", SPRAY = "SPRAY",
            CALLIGRAPHY = "CALLIGRAPHY", NEON = "NEON", RAINBOW = "RAINBOW", DASHED = "DASHED",
            DOTTED = "DOTTED", ERASER = "ERASER", FILL = "FILL", UNDO = "UNDO", CLEAR = "CLEAR",
            LINE = "LINE", RECT = "RECT", ELLIPSE = "ELLIPSE", PALETTE = "PALETTE";

    // ---- Power-up ids (match common.PowerUp names) ----
    public static final String BLUR = "BLUR", MIRROR = "MIRROR", SPOTLIGHT = "SPOTLIGHT",
            FREEZE = "FREEZE", HINT = "HINT", DOUBLE = "DOUBLE", SHIELD = "SHIELD", BOLT = "BOLT";

    // ---- UI ids ----
    public static final String GEAR = "GEAR", CLOCK = "CLOCK", CROWN = "CROWN", SEND = "SEND", CHAT = "CHAT",
            TARGET = "TARGET", USERS = "USERS", COPY = "COPY", CAMERA = "CAMERA", SOUND = "SOUND", MUTE = "MUTE",
            MINUS = "MINUS", PLUS = "PLUS", PLAY = "PLAY", HOME = "HOME";

    public static final String[] ALL = {
            PEN, MARKER, CRAYON, SPRAY, CALLIGRAPHY, NEON, RAINBOW, DASHED, DOTTED,
            ERASER, FILL, UNDO, CLEAR, LINE, RECT, ELLIPSE, PALETTE,
            BLUR, MIRROR, SPOTLIGHT, FREEZE, HINT, DOUBLE, SHIELD, BOLT,
            GEAR, CLOCK, CROWN, SEND, CHAT, TARGET, USERS, COPY, CAMERA, SOUND, MUTE, MINUS, PLUS, PLAY, HOME
    };

    public static ImageIcon icon(String id, int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paint(g, id, 0, 0, size, color);
        g.dispose();
        return new ImageIcon(img);
    }

    /** Paints icon {@code id} into the square (x, y, size, size). */
    public static void paint(Graphics2D g0, String id, double x, double y, double size, Color c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(c);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color soft = Theme.alpha(c, 70);
        Color mid = Theme.alpha(c, 130);

        switch (id) {

            case PEN -> {
                g.rotate(Math.toRadians(45), 12, 12);
                g.draw(new RoundRectangle2D.Double(9.5, 2.5, 5, 13, 1.5, 1.5));
                Path2D tip = poly(9.5, 15.5, 14.5, 15.5, 12, 21);
                g.fill(tip);
                ln(g, 9, 6, 15, 6);
            }

            case MARKER -> {
                g.rotate(Math.toRadians(45), 12, 12);
                g.setColor(soft);
                g.fill(new RoundRectangle2D.Double(8, 2.5, 8, 11, 2, 2));
                g.setColor(c);
                g.draw(new RoundRectangle2D.Double(8, 2.5, 8, 11, 2, 2));
                g.fill(poly(8.8, 13.5, 15.2, 13.5, 14, 19.5, 10, 19.5));
                ln(g, 8, 7, 16, 7);
            }

            case CRAYON -> {
                g.rotate(Math.toRadians(45), 12, 12);
                g.setColor(soft);
                g.fill(new Rectangle2D.Double(9.5, 3, 5, 12.5));
                g.setColor(c);
                g.draw(new Rectangle2D.Double(9.5, 3, 5, 12.5));
                g.fill(poly(9.5, 15.5, 14.5, 15.5, 12, 21));
                ln(g, 9.5, 7, 14.5, 7);
                ln(g, 9.5, 11, 14.5, 11);
            }

            case SPRAY -> {
                g.setColor(soft);
                g.fill(new RoundRectangle2D.Double(5.5, 10, 9, 12, 3, 3));
                g.setColor(c);
                g.draw(new RoundRectangle2D.Double(5.5, 10, 9, 12, 3, 3));
                g.draw(new Rectangle2D.Double(7.5, 6.5, 5, 3.5));
                ln(g, 8, 15, 12, 15);
                dot(g, 17.5, 6, 1.1);
                dot(g, 20.5, 9, 1.1);
                dot(g, 17, 10.5, 1.1);
                dot(g, 20, 4, 1.1);
                dot(g, 15.5, 3.5, 1.1);
                dot(g, 21, 13, 1.1);
            }

            case CALLIGRAPHY -> {
                Path2D nib = new Path2D.Double();
                nib.moveTo(12, 2.5);
                nib.lineTo(18, 10);
                nib.quadTo(18, 16, 12, 22);
                nib.quadTo(6, 16, 6, 10);
                nib.closePath();
                g.setColor(soft);
                g.fill(nib);
                g.setColor(c);
                g.draw(nib);
                ln(g, 12, 12, 12, 21);
                g.fill(new Ellipse2D.Double(10.4, 9.4, 3.2, 3.2));
            }

            case NEON -> {
                Path2D wave = new Path2D.Double();
                wave.moveTo(3, 17);
                wave.curveTo(7, 3, 10, 21, 14, 11);
                wave.curveTo(16, 6, 19, 6, 21, 8);
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.alpha(c, 40));
                g.draw(wave);
                g.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.alpha(c, 90));
                g.draw(wave);
                g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(c);
                g.draw(wave);
            }

            case RAINBOW -> {
                Color[] rc = {new Color(255, 82, 96), new Color(255, 200, 60), new Color(70, 214, 150), new Color(70, 180, 255)};
                g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < rc.length; i++) {
                    double r = 10 - i * 2.4;
                    g.setColor(rc[i]);
                    g.draw(new Arc2D.Double(12 - r, 18 - r, r * 2, r * 2, 0, 180, Arc2D.OPEN));
                }
            }

            case DASHED -> {
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{0.1f, 4.6f}, 0f));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{4.2f, 3.4f}, 0f));
                g.draw(new Line2D.Double(3, 20, 21, 4));
            }

            case DOTTED -> {
                for (int i = 0; i < 6; i++) {
                    double t = i / 5.0;
                    dot(g, 4 + 16 * t, 19.5 - 15 * t, 1.7);
                }
            }

            case ERASER -> {
                g.rotate(Math.toRadians(-45), 12, 12);
                RoundRectangle2D body = new RoundRectangle2D.Double(3.5, 7.5, 17, 9, 2.5, 2.5);
                g.setColor(soft);
                g.fill(body);
                g.setColor(mid);
                g.fill(new Rectangle2D.Double(12, 7.5, 8.5, 9));
                g.setColor(c);
                g.draw(body);
                ln(g, 12, 7, 12, 17);
            }

            case FILL -> {
                Path2D bucket = poly(4.5, 12, 11, 5.5, 18.5, 13, 12, 19.5);
                g.setColor(soft);
                g.fill(bucket);
                g.setColor(c);
                g.draw(bucket);
                Path2D handle = new Path2D.Double();
                handle.moveTo(11, 5.5);
                handle.quadTo(9, 2.5, 6.5, 4.5);
                g.draw(handle);
                ln(g, 6.5, 10.5, 15.5, 14.5);
                Path2D drop = new Path2D.Double();
                drop.moveTo(20.5, 14);
                drop.curveTo(20.5, 14, 18.7, 17, 18.7, 18.3);
                drop.curveTo(18.7, 19.4, 19.5, 20.2, 20.5, 20.2);
                drop.curveTo(21.5, 20.2, 22.3, 19.4, 22.3, 18.3);
                drop.curveTo(22.3, 17, 20.5, 14, 20.5, 14);
                g.fill(drop);
            }

            case UNDO -> {
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(9, 5.5);
                arrow.lineTo(4, 10.5);
                arrow.lineTo(9, 15.5);
                g.draw(arrow);
                Path2D body = new Path2D.Double();
                body.moveTo(4.5, 10.5);
                body.lineTo(14, 10.5);
                body.curveTo(21, 10.5, 21, 19.5, 14, 19.5);
                body.lineTo(8.5, 19.5);
                g.draw(body);
            }

            case CLEAR -> {
                ln(g, 4, 7, 20, 7);
                Path2D lid = new Path2D.Double();
                lid.moveTo(9, 7);
                lid.lineTo(9, 4);
                lid.lineTo(15, 4);
                lid.lineTo(15, 7);
                g.draw(lid);
                Path2D bin = new Path2D.Double();
                bin.moveTo(6, 7);
                bin.lineTo(7, 20);
                bin.quadTo(7.1, 21, 8.3, 21);
                bin.lineTo(15.7, 21);
                bin.quadTo(16.9, 21, 17, 20);
                bin.lineTo(18, 7);
                g.setColor(soft);
                g.fill(bin);
                g.setColor(c);
                g.draw(bin);
                ln(g, 10, 10.5, 10, 17.5);
                ln(g, 14, 10.5, 14, 17.5);
            }

            case LINE -> {
                g.draw(new Line2D.Double(5, 19, 19, 5));
                g.fill(new Ellipse2D.Double(2.2, 16.2, 5.6, 5.6));
                g.fill(new Ellipse2D.Double(16.2, 2.2, 5.6, 5.6));
            }

            case RECT -> {
                RoundRectangle2D r = new RoundRectangle2D.Double(3.5, 6, 17, 12, 2.5, 2.5);
                g.setColor(soft);
                g.fill(r);
                g.setColor(c);
                g.draw(r);
            }

            case ELLIPSE -> {
                Ellipse2D e = new Ellipse2D.Double(3, 5.5, 18, 13);
                g.setColor(soft);
                g.fill(e);
                g.setColor(c);
                g.draw(e);
            }

            case PALETTE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 3);
                p.curveTo(5.5, 3, 2.5, 8, 3, 12.5);
                p.curveTo(3.6, 18, 8, 21, 12.5, 21);
                p.curveTo(15, 21, 15.5, 19, 14.2, 17.5);
                p.curveTo(13, 16, 14, 14.5, 16, 14.5);
                p.lineTo(18, 14.5);
                p.curveTo(20, 14.5, 21.5, 13.4, 21.4, 11.5);
                p.curveTo(21, 6.5, 17, 3, 12, 3);
                p.closePath();
                g.setColor(soft);
                g.fill(p);
                g.setColor(c);
                g.draw(p);
                dot(g, 8, 11, 1.5);
                dot(g, 11.5, 7.5, 1.5);
                dot(g, 16, 8.5, 1.5);
                dot(g, 8.5, 15.5, 1.5);
            }

            // ------------------------------------------------ power-ups

            case BLUR -> {
                Path2D drop = new Path2D.Double();
                drop.moveTo(12, 2.5);
                drop.curveTo(15.5, 7.5, 19, 11.5, 19, 15);
                drop.curveTo(19, 18.9, 15.9, 21.5, 12, 21.5);
                drop.curveTo(8.1, 21.5, 5, 18.9, 5, 15);
                drop.curveTo(5, 11.5, 8.5, 7.5, 12, 2.5);
                drop.closePath();
                g.setColor(soft);
                g.fill(drop);
                g.setColor(c);
                g.draw(drop);
                g.setColor(mid);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(8, 12, 8, 6, 200, 140, Arc2D.OPEN));
                g.setColor(c);
                g.draw(new Arc2D.Double(8.3, 11.5, 4, 4, 130, 90, Arc2D.OPEN));
            }

            case FREEZE -> {
                for (int k = 0; k < 3; k++) {
                    Graphics2D r = (Graphics2D) g.create();
                    r.rotate(Math.toRadians(60 * k), 12, 12);
                    r.setColor(c);
                    ln(r, 12, 3, 12, 21);
                    ln(r, 12, 6, 9.4, 3.8);
                    ln(r, 12, 6, 14.6, 3.8);
                    ln(r, 12, 18, 9.4, 20.2);
                    ln(r, 12, 18, 14.6, 20.2);
                    r.dispose();
                }
            }

            case MIRROR -> {
                ln(g, 8, 20, 8, 5);
                ln(g, 4.5, 8.5, 8, 5);
                ln(g, 11.5, 8.5, 8, 5);
                ln(g, 16, 4, 16, 19);
                ln(g, 12.5, 15.5, 16, 19);
                ln(g, 19.5, 15.5, 16, 19);
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{1.2f, 2.2f}, 0f));
                g.setColor(mid);
                ln(g, 12, 2, 12, 22);
            }

            case SPOTLIGHT -> {
                g.rotate(Math.toRadians(-38), 12, 12);
                Path2D beam = poly(9, 7.5, 15, 7.5, 21, 21, 3, 21);
                g.setColor(Theme.alpha(c, 45));
                g.fill(beam);
                g.setColor(c);
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2.2f, 2.6f}, 0f));
                ln(g, 9, 8, 3.3, 20.6);
                ln(g, 15, 8, 20.7, 20.6);
                g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Theme.alpha(c, 110));
                g.fill(new Ellipse2D.Double(3, 18.5, 18, 5));
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(8, 1.5, 8, 6.5, 2.5, 2.5));
                g.setColor(Theme.alpha(Color.WHITE, 200));
                g.fill(new Ellipse2D.Double(10.3, 3, 3.4, 3.4));
            }

            case HINT -> {
                g.setColor(soft);
                g.fill(new Ellipse2D.Double(5.5, 3, 13, 13));
                g.setColor(c);
                Path2D bulb = new Path2D.Double();
                bulb.moveTo(9.3, 15.5);
                bulb.curveTo(9.3, 13.5, 5.5, 12.5, 5.5, 9);
                bulb.curveTo(5.5, 5.4, 8.4, 3, 12, 3);
                bulb.curveTo(15.6, 3, 18.5, 5.4, 18.5, 9);
                bulb.curveTo(18.5, 12.5, 14.7, 13.5, 14.7, 15.5);
                g.draw(bulb);
                ln(g, 9.3, 15.5, 14.7, 15.5);
                ln(g, 9.8, 18.2, 14.2, 18.2);
                ln(g, 11, 21, 13, 21);
                Path2D fil = new Path2D.Double();
                fil.moveTo(9.8, 9.5);
                fil.lineTo(12, 12.5);
                fil.lineTo(14.2, 9.5);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(fil);
            }

            case DOUBLE -> {
                g.setColor(soft);
                g.fill(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.setColor(c);
                g.draw(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.setFont(Theme.font(Font.BOLD, 11));
                FontMetrics fm = g.getFontMetrics();
                String s = "x2";
                g.drawString(s, (float) (12 - fm.stringWidth(s) / 2.0), (float) (12 + fm.getAscent() / 2.0 - 1.2));
            }

            case SHIELD -> {
                Path2D s = new Path2D.Double();
                s.moveTo(12, 2.5);
                s.lineTo(20, 5.5);
                s.lineTo(20, 11.5);
                s.curveTo(20, 16.5, 16.5, 20, 12, 21.5);
                s.curveTo(7.5, 20, 4, 16.5, 4, 11.5);
                s.lineTo(4, 5.5);
                s.closePath();
                g.setColor(soft);
                g.fill(s);
                g.setColor(c);
                g.draw(s);
                Path2D ok = new Path2D.Double();
                ok.moveTo(8.3, 12);
                ok.lineTo(11, 14.7);
                ok.lineTo(15.7, 9.3);
                g.draw(ok);
            }

            case BOLT -> {
                g.fill(poly(13.5, 1.8, 5, 13.6, 10.6, 13.6, 9.4, 22.2, 19, 9.6, 13.2, 9.6));
            }

            // ------------------------------------------------ UI

            case GEAR -> {
                for (int k = 0; k < 8; k++) {
                    Graphics2D r = (Graphics2D) g.create();
                    r.rotate(Math.toRadians(45 * k), 12, 12);
                    r.fill(new RoundRectangle2D.Double(10.2, 1.8, 3.6, 5, 1.2, 1.2));
                    r.dispose();
                }
                g.draw(new Ellipse2D.Double(5.8, 5.8, 12.4, 12.4));
                g.draw(new Ellipse2D.Double(9.4, 9.4, 5.2, 5.2));
            }

            case CLOCK -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                Path2D h = new Path2D.Double();
                h.moveTo(12, 7);
                h.lineTo(12, 12);
                h.lineTo(15.5, 14);
                g.draw(h);
            }

            case CROWN -> {
                Path2D cr = poly(3, 19, 2.5, 7.5, 8, 12, 12, 4.5, 16, 12, 21.5, 7.5, 21, 19);
                g.setColor(soft);
                g.fill(cr);
                g.setColor(c);
                g.draw(cr);
                ln(g, 3.5, 21.5, 20.5, 21.5);
            }

            case SEND -> {
                Path2D pl = poly(3, 11, 21, 3, 14.5, 21, 11.2, 13.2);
                g.setColor(soft);
                g.fill(pl);
                g.setColor(c);
                g.draw(pl);
                ln(g, 11.2, 13.2, 21, 3);
            }

            case CHAT -> {
                Path2D b = new Path2D.Double();
                b.moveTo(6, 4);
                b.lineTo(18, 4);
                b.quadTo(21, 4, 21, 7);
                b.lineTo(21, 13);
                b.quadTo(21, 16, 18, 16);
                b.lineTo(11, 16);
                b.lineTo(6.5, 20);
                b.lineTo(6.5, 16);
                b.quadTo(3, 16, 3, 13);
                b.lineTo(3, 7);
                b.quadTo(3, 4, 6, 4);
                b.closePath();
                g.setColor(soft);
                g.fill(b);
                g.setColor(c);
                g.draw(b);
            }

            case TARGET -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                ln(g, 12, 1.5, 12, 6);
                ln(g, 12, 18, 12, 22.5);
                ln(g, 1.5, 12, 6, 12);
                ln(g, 18, 12, 22.5, 12);
            }

            case USERS -> {
                g.setColor(soft);
                g.fill(new Ellipse2D.Double(7.5, 3.5, 7, 7));
                g.fill(new RoundRectangle2D.Double(4, 13, 14, 8, 8, 8));
                g.setColor(c);
                g.draw(new Ellipse2D.Double(7.5, 3.5, 7, 7));
                g.draw(new Arc2D.Double(4, 13, 14, 14, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(15, 6, 5.5, 5.5, 60, 240, Arc2D.OPEN));
            }

            case COPY -> {
                g.setColor(soft);
                g.fill(new RoundRectangle2D.Double(8.5, 8.5, 12, 12, 3, 3));
                g.setColor(c);
                g.draw(new RoundRectangle2D.Double(8.5, 8.5, 12, 12, 3, 3));
                Path2D back = new Path2D.Double();
                back.moveTo(5.5, 15.5);
                back.lineTo(5.5, 5.5);
                back.lineTo(15.5, 5.5);
                g.draw(back);
            }

            case CAMERA -> {
                g.setColor(soft);
                g.fill(new RoundRectangle2D.Double(2.5, 7, 19, 13, 4, 4));
                g.setColor(c);
                g.draw(new RoundRectangle2D.Double(2.5, 7, 19, 13, 4, 4));
                g.draw(new Ellipse2D.Double(8, 9.8, 8, 8));
                g.draw(poly(8, 7, 9.6, 4, 14.4, 4, 16, 7));
            }

            case SOUND -> {
                Path2D sp = poly(3.5, 9.5, 8, 9.5, 13, 5, 13, 19, 8, 14.5, 3.5, 14.5);
                g.setColor(soft);
                g.fill(sp);
                g.setColor(c);
                g.draw(sp);
                g.draw(new Arc2D.Double(11, 8, 7, 8, -60, 120, Arc2D.OPEN));
                g.draw(new Arc2D.Double(11, 4.5, 11, 15, -60, 120, Arc2D.OPEN));
            }

            case MUTE -> {
                Path2D sp = poly(3.5, 9.5, 8, 9.5, 13, 5, 13, 19, 8, 14.5, 3.5, 14.5);
                g.setColor(soft);
                g.fill(sp);
                g.setColor(c);
                g.draw(sp);
                ln(g, 16, 9.5, 21.5, 15);
                ln(g, 21.5, 9.5, 16, 15);
            }

            case MINUS -> ln(g, 5.5, 12, 18.5, 12);

            case PLUS -> {
                ln(g, 5.5, 12, 18.5, 12);
                ln(g, 12, 5.5, 12, 18.5);
            }

            case PLAY -> {
                g.setColor(c);
                g.fill(poly(7, 4.5, 19.5, 12, 7, 19.5));
            }

            case HOME -> {
                Path2D h = poly(3, 11.5, 12, 3.5, 21, 11.5, 18.5, 11.5, 18.5, 20.5, 5.5, 20.5, 5.5, 11.5);
                g.setColor(soft);
                g.fill(h);
                g.setColor(c);
                g.draw(h);
                g.draw(new RoundRectangle2D.Double(10, 14, 4, 6.5, 1, 1));
            }

            default -> g.draw(new Rectangle2D.Double(4, 4, 16, 16));
        }
        g.dispose();
    }

    // ---------------------------------------------------------------- helpers

    private static void ln(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void dot(Graphics2D g, double cx, double cy, double r) {
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }
}
