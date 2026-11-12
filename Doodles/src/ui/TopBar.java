package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Header of the game screen: round, word tiles, countdown ring, connection state, settings. */
public class TopBar extends JPanel {

    private final RoundChip roundChip = new RoundChip();
    private final WordDisplay word = new WordDisplay();
    private final TimerRing timer = new TimerRing();
    private final StatusPill status = new StatusPill();

    public TopBar(Runnable onSettings) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(0, 96));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 31));
        left.setOpaque(false);
        left.add(roundChip);
        left.setPreferredSize(new Dimension(250, 96));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        right.setOpaque(false);
        Widgets.IconButton gear = new Widgets.IconButton(Icons.GEAR, "Settings & shortcuts", 40);
        gear.addActionListener(e -> onSettings.run());
        right.add(status);
        right.add(timer);
        right.add(gear);
        right.setPreferredSize(new Dimension(330, 96));

        add(left, BorderLayout.WEST);
        add(word, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public WordDisplay word() { return word; }

    public void setRound(int round, int totalRounds) {
        roundChip.round = round;
        roundChip.total = totalRounds;
        roundChip.repaint();
    }

    public void setTime(int left, int total) {
        timer.left = left;
        timer.total = Math.max(1, total);
        timer.repaint();
    }

    public void setOnline(boolean online) {
        status.online = online;
        status.repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        g.setColor(Theme.alpha(Theme.SHADOW, 22));
        g.fillRect(0, getHeight() - 1, getWidth(), 4);
        g.setColor(Theme.alpha(Color.WHITE, 232));
        g.fillRect(0, 0, getWidth(), getHeight() - 3);
        g.setPaint(new GradientPaint(0, 0, Theme.SKY, getWidth(), 0, Theme.PINK));
        g.fillRect(0, getHeight() - 4, getWidth(), 4);
        g.dispose();
    }

    // =========================================================================

    private static class RoundChip extends JComponent {
        int round = 1, total = 3;

        RoundChip() { setPreferredSize(new Dimension(150, 34)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.setColor(Theme.PANEL_LIGHT);
            g.fill(r);
            g.setColor(Theme.BORDER);
            g.draw(r);
            Icons.paint(g, Icons.CLOCK, 10, 7, 20, Theme.ORANGE);
            g.setFont(Theme.SMALL_BOLD);
            g.setColor(Theme.TEXT);
            g.drawString("ROUND " + round + " / " + total, 38, 22);
            g.dispose();
        }
    }

    private static class StatusPill extends JComponent {
        boolean online = true;

        StatusPill() { setPreferredSize(new Dimension(84, 26)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            Color c = online ? Theme.MINT : Theme.DANGER;
            int y = (getHeight() - 24) / 2;
            g.setColor(Theme.alpha(c, 38));
            g.fill(new RoundRectangle2D.Double(0, y, getWidth() - 1, 24, 24, 24));
            g.setColor(c);
            g.fill(new Ellipse2D.Double(10, y + 8, 8, 8));
            g.setFont(Theme.TINY_BOLD);
            g.drawString(online ? "ONLINE" : "OFFLINE", 24, y + 16);
            g.dispose();
        }
    }

    private static class TimerRing extends JComponent {
        int left = 60, total = 60;

        TimerRing() { setPreferredSize(new Dimension(66, 66)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            double d = Math.min(getWidth(), getHeight()) - 6;
            double x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
            double frac = Math.max(0, Math.min(1, left / (double) total));
            Color c = left <= 10 ? Theme.DANGER : frac < 0.4 ? Theme.ORANGE : Theme.MINT;
            g.setColor(Theme.alpha(Theme.SHADOW, 30));
            g.fill(new Ellipse2D.Double(x, y + 2, d, d));
            g.setColor(Theme.PANEL);
            g.fill(new Ellipse2D.Double(x, y, d, d));
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Theme.BORDER);
            g.draw(new Ellipse2D.Double(x + 4, y + 4, d - 8, d - 8));
            g.setColor(c);
            g.draw(new Arc2D.Double(x + 4, y + 4, d - 8, d - 8, 90, -360 * frac, Arc2D.OPEN));
            g.setFont(Theme.font(Font.BOLD, 20));
            String s = String.valueOf(left);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(left <= 10 ? Theme.DANGER : Theme.TEXT);
            g.drawString(s, (float) (getWidth() / 2.0 - fm.stringWidth(s) / 2.0), (float) (getHeight() / 2.0 + fm.getAscent() / 2.0 - 3));
            g.dispose();
        }
    }

    // =========================================================================
    // WORD DISPLAY
    // =========================================================================

    /** Letter tiles in the centre of the top bar. */
    public static class WordDisplay extends JComponent {

        private enum Kind { MESSAGE, PLAIN, HINT }

        private String title = "";
        private Color titleColor = Theme.TEXT_MUTED;
        private String text = "waiting...";
        private Kind kind = Kind.MESSAGE;
        private Color tint = Theme.TEXT;

        public WordDisplay() { setPreferredSize(new Dimension(300, 96)); }

        public void setTitle(String t, Color c) {
            title = t;
            titleColor = c;
            repaint();
        }

        public void setMessage(String t) {
            text = t;
            kind = Kind.MESSAGE;
            tint = Theme.TEXT_MUTED;
            repaint();
        }

        /** The real word (drawer view, or the reveal at the end of a round). */
        public void setPlain(String t, Color c) {
            text = t;
            kind = Kind.PLAIN;
            tint = c;
            repaint();
        }

        /** A hint like "_ _ A _ _" as sent by the server. */
        public void setHint(String t) {
            text = t;
            kind = Kind.HINT;
            tint = Theme.TEXT;
            repaint();
        }

        public String getText() { return text; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(titleColor);
            FontMetrics tf = g.getFontMetrics();
            g.drawString(title, (getWidth() - tf.stringWidth(title)) / 2, 24);

            if (kind == Kind.MESSAGE) {
                g.setFont(Theme.font(Font.ITALIC, 22));
                g.setColor(tint);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, 62);
                g.dispose();
                return;
            }

            // ---- tokenise into tiles ----
            java.util.List<String> tiles = new java.util.ArrayList<>(); // "" = blank, " " = gap
            if (kind == Kind.PLAIN) {
                for (char ch : text.toCharArray()) tiles.add(ch == ' ' ? " " : String.valueOf(ch));
            } else {
                int i = 0;
                while (i < text.length()) {
                    char ch = text.charAt(i);
                    if (ch == ' ') {
                        int run = 0;
                        while (i < text.length() && text.charAt(i) == ' ') { run++; i++; }
                        if (run >= 2) tiles.add(" ");
                    } else {
                        tiles.add(ch == '_' ? "" : String.valueOf(ch));
                        i++;
                    }
                }
            }
            if (tiles.isEmpty()) { g.dispose(); return; }

            double tw = 34, th = 44, gap = 6, wordGap = 20;
            double total = 0;
            for (String t : tiles) total += t.equals(" ") ? wordGap : tw + gap;
            double scale = Math.min(1.0, (getWidth() - 24) / Math.max(1, total));
            g.translate((getWidth() - total * scale) / 2.0, 36);
            g.scale(scale, scale);

            double x = 0;
            for (String t : tiles) {
                if (t.equals(" ")) { x += wordGap; continue; }
                RoundRectangle2D r = new RoundRectangle2D.Double(x, 0, tw, th, 11, 11);
                boolean blank = t.isEmpty();
                g.setColor(Theme.alpha(Theme.SHADOW, 40));
                g.fill(new RoundRectangle2D.Double(x, 3, tw, th, 11, 11));
                g.setColor(blank ? Theme.PANEL_LIGHT : Theme.PANEL);
                g.fill(r);
                g.setColor(blank ? Theme.BORDER : Theme.alpha(tint, 200));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(r);
                if (blank) {
                    g.setColor(Theme.alpha(Theme.TEXT_MUTED, 190));
                    g.fill(new RoundRectangle2D.Double(x + 9, th - 12, tw - 18, 3, 3, 3));
                } else {
                    g.setFont(Theme.font(Font.BOLD, 26));
                    FontMetrics fm = g.getFontMetrics();
                    g.setColor(Theme.ink(tint));
                    g.drawString(t, (float) (x + (tw - fm.stringWidth(t)) / 2.0), (float) (th / 2.0 + fm.getAscent() / 2.0 - 3));
                }
                x += tw + gap;
            }
            g.dispose();
        }
    }
}
