package ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/** Ranked player list with medals, avatars, drawer pencil and "guessed it" tick. */
public class Leaderboard extends JPanel {

    public static final class Entry {
        public final String name;
        public final int score;
        public final boolean you;
        public final boolean drawer;
        public final boolean guessed;

        public Entry(String name, int score, boolean you, boolean drawer, boolean guessed) {
            this.name = name;
            this.score = score;
            this.you = you;
            this.drawer = drawer;
            this.guessed = guessed;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final NumberFormat fmt = NumberFormat.getIntegerInstance();

    public Leaderboard() {
        setOpaque(false);
        setPreferredSize(new Dimension(250, 300));
    }

    public void setEntries(List<Entry> list) {
        entries.clear();
        entries.addAll(list);
        setPreferredSize(new Dimension(250, 56 + Math.max(1, entries.size()) * 46));
        revalidate();
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth();

        Icons.paint(g, Icons.CROWN, 16, 14, 22, Theme.GOLD);
        g.setFont(Theme.font(Font.BOLD, 13));
        g.setColor(Theme.TEXT);
        g.drawString("LEADERBOARD", 46, 31);

        int top = 50;
        if (entries.isEmpty()) {
            g.setFont(Theme.font(Font.ITALIC, 12));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("Waiting for players...", 18, top + 20);
            g.dispose();
            return;
        }

        int avail = getHeight() - top - 6;
        int rowH = Math.max(40, Math.min(60, avail / Math.max(1, entries.size())));
        int pad = 10;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int y = top + i * rowH;
            RoundRectangle2D row = new RoundRectangle2D.Double(pad, y, w - pad * 2, rowH - 6, 16, 16);
            g.setColor(e.you ? Theme.ACCENT_SOFT : Theme.alpha(Theme.PANEL_LIGHT, 200));
            g.fill(row);
            g.setColor(e.you ? Theme.ACCENT_HOVER : i == 0 && e.score > 0 ? Theme.alpha(Theme.GOLD, 200) : Theme.alpha(Theme.BORDER, 200));
            g.setStroke(new BasicStroke(e.you ? 1.8f : 1.1f));
            g.draw(row);

            int cy = y + (rowH - 6) / 2;

            // rank medal
            Color medal = i == 0 ? Theme.GOLD : i == 1 ? Theme.SILVER : i == 2 ? Theme.BRONZE : null;
            double md = 24;
            double mx = pad + 9, my = cy - md / 2;
            if (medal != null) {
                g.setColor(medal);
                g.fill(new Ellipse2D.Double(mx, my, md, md));
                g.setColor(Theme.alpha(Color.WHITE, 140));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Ellipse2D.Double(mx + 2, my + 2, md - 4, md - 4));
            }
            g.setFont(Theme.font(Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            String rank = String.valueOf(i + 1);
            g.setColor(medal != null ? new Color(60, 40, 10) : Theme.TEXT_MUTED);
            g.drawString(rank, (float) (mx + (md - fm.stringWidth(rank)) / 2), (float) (cy + fm.getAscent() / 2.0 - 2));

            // avatar
            double ad = Math.min(38, rowH - 14);
            double ax = pad + 40, ay = cy - ad / 2;
            Widgets.avatar(g, e.name, ax, ay, ad);
            if (e.drawer) {
                double bd = 17;
                g.setColor(Theme.YELLOW);
                g.fill(new Ellipse2D.Double(ax + ad - bd + 4, ay + ad - bd + 3, bd, bd));
                g.setColor(Theme.PANEL);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Ellipse2D.Double(ax + ad - bd + 4, ay + ad - bd + 3, bd, bd));
                Icons.paint(g, Icons.PEN, ax + ad - bd + 6, ay + ad - bd + 5, 13, new Color(60, 40, 10));
            }

            // text
            int tx = (int) (ax + ad + 12);
            int maxName = w - pad - tx - 34;
            g.setFont(Theme.font(Font.BOLD, 13));
            fm = g.getFontMetrics();
            String name = ellipsize(e.name, fm, maxName);
            g.setColor(Theme.TEXT);
            g.drawString(name, tx, cy - 3);
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.ink(Theme.YELLOW));
            String pts = fmt.format(e.score) + " pts";
            g.drawString(pts, tx, cy + 13);
            if (e.you) {
                int pw = g.getFontMetrics().stringWidth(pts);
                g.setFont(Theme.font(Font.BOLD, 9));
                FontMetrics bf = g.getFontMetrics();
                int bw = bf.stringWidth("YOU") + 12;
                g.setColor(Theme.ACCENT_PINK);
                g.fill(new java.awt.geom.RoundRectangle2D.Double(tx + pw + 8, cy + 2, bw, 15, 15, 15));
                g.setColor(Color.WHITE);
                g.drawString("YOU", tx + pw + 14, cy + 13);
            }

            // guessed tick
            if (e.guessed) {
                double td = 20, tcx = w - pad - 14 - td / 2, tcy = cy - td / 2;
                g.setColor(Theme.MINT);
                g.fill(new Ellipse2D.Double(tcx, tcy, td, td));
                Path2D tick = new Path2D.Double();
                tick.moveTo(tcx + 5.5, tcy + 10.5);
                tick.lineTo(tcx + 8.8, tcy + 13.8);
                tick.lineTo(tcx + 14.5, tcy + 6.8);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(tick);
            }
        }
        g.dispose();
    }

    private static String ellipsize(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "...") > max) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
