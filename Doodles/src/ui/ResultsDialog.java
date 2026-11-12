package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.List;

/** Final results screen: podium for the top three plus the rest of the table. */
public final class ResultsDialog {

    private ResultsDialog() {}

    /** rows: {name, score} already sorted best-first. */
    public static void show(Component parent, List<String[]> rows, String me) {
        JDialog[] dd = new JDialog[1];
        JPanel content = buildContent(rows, me, () -> dd[0].dispose());
        dd[0] = Dialogs.framed(parent, "Final results", content, true, Dialogs.Style.PASTEL);
        dd[0].setVisible(true);
    }

    /** The whole results screen as a panel (also used for offscreen previews). */
    public static JPanel buildContent(List<String[]> rows, String me, Runnable onClose) {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                paintResults(g, getWidth(), rows, me);
                g.dispose();
            }
        };
        root.setOpaque(false);
        root.setPreferredSize(new Dimension(580, 700));

        Widgets.PillButton close = new Widgets.PillButton("Back to lobby", Theme.ACCENT);
        close.addActionListener(e -> onClose.run());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        south.setOpaque(false);
        close.setPreferredSize(new Dimension(220, 46));
        south.add(close);
        south.setPreferredSize(new Dimension(0, 78));
        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    private static void paintResults(Graphics2D g, int w, List<String[]> rows, String me) {
        NumberFormat fmt = NumberFormat.getIntegerInstance();

        Icons.paint(g, Icons.CROWN, w / 2.0 - 22, 24, 44, Theme.GOLD);
        g.setFont(Theme.font(Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        String title = "FINAL RESULTS";
        g.setColor(Theme.TEXT);
        g.drawString(title, (w - fm.stringWidth(title)) / 2, 92);
        if (!rows.isEmpty()) {
            g.setFont(Theme.font(Font.PLAIN, 16));
            String sub = rows.get(0)[0] + (rows.get(0)[0].equalsIgnoreCase(me) ? ", you win!" : " wins the game!");
            fm = g.getFontMetrics();
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(sub, (w - fm.stringWidth(sub)) / 2, 122);
        }

        // podium: 2nd | 1st | 3rd
        int[] order = {1, 0, 2};
        int[] heights = {112, 150, 84};
        Color[] colors = {Theme.SILVER, Theme.GOLD, Theme.BRONZE};
        int barW = 140, gap = 14;
        int startX = (w - (barW * 3 + gap * 2)) / 2;
        int baseY = 372;
        for (int slot = 0; slot < 3; slot++) {
            int rank = order[slot];
            if (rank >= rows.size()) continue;
            String[] r = rows.get(rank);
            int x = startX + slot * (barW + gap);
            int h = heights[slot];
            Color c = colors[slot];
            g.setPaint(new GradientPaint(0, baseY - h, Theme.mix(c, Color.WHITE, 0.25), 0, baseY, c));
            g.fill(new RoundRectangle2D.Double(x, baseY - h, barW, h, 18, 18));
            g.setColor(Theme.alpha(Color.WHITE, 200));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(x + 1, baseY - h + 1, barW - 2, h - 2, 18, 18));

            Widgets.avatar(g, r[0], x + (barW - 58) / 2.0, baseY - h - 34, 58);
            g.setFont(Theme.font(Font.BOLD, 34));
            g.setColor(Theme.alpha(new Color(40, 28, 10), 190));
            String rk = String.valueOf(rank + 1);
            g.drawString(rk, x + (barW - g.getFontMetrics().stringWidth(rk)) / 2, baseY - h + 70 - (rank == 0 ? 0 : 6) + 6 - 6);

            g.setFont(Theme.font(Font.BOLD, 14));
            String name = fit(r[0] + (r[0].equalsIgnoreCase(me) ? " (You)" : ""), g.getFontMetrics(), barW - 12);
            g.setColor(Theme.TEXT);
            g.drawString(name, x + (barW - g.getFontMetrics().stringWidth(name)) / 2, baseY + 22);
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(Theme.ink(Theme.YELLOW));
            String sc = fmt.format(parse(r[1])) + " pts";
            g.drawString(sc, x + (barW - g.getFontMetrics().stringWidth(sc)) / 2, baseY + 40);
        }

        // everyone else
        int y = baseY + 62;
        for (int i = 3; i < Math.min(rows.size(), 8); i++) {
            String[] r = rows.get(i);
            RoundRectangle2D row = new RoundRectangle2D.Double(60, y, w - 120, 30, 14, 14);
            boolean you = r[0].equalsIgnoreCase(me);
            g.setColor(you ? Theme.ACCENT_SOFT : Theme.alpha(Theme.PANEL_LIGHT, 220));
            g.fill(row);
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(String.valueOf(i + 1), 78, y + 20);
            g.setColor(Theme.TEXT);
            g.drawString(fit(r[0] + (you ? " (You)" : ""), g.getFontMetrics(), 250), 108, y + 20);
            g.setColor(Theme.ink(Theme.YELLOW));
            String sc = fmt.format(parse(r[1])) + " pts";
            g.drawString(sc, w - 78 - g.getFontMetrics().stringWidth(sc), y + 20);
            y += 36;
        }
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String fit(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "...") > max) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
