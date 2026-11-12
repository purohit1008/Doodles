package ui;

import common.PowerUp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The power-up shop: shows the power-ups for your current role, with live affordability. */
public class PowerShop extends JPanel {

    private static final int HEADER_H = 58;
    private static final int CARD_H = 66;
    private static final int GAP = 6;

    private String role = "WAITING";
    private int score;
    private boolean roundActive;
    private boolean usedThisRound;
    private boolean locked;
    private boolean shieldArmed, doubleArmed;
    private int hover = -1;
    private Consumer<PowerUp> onBuy = p -> { };
    private final NumberFormat fmt = NumberFormat.getIntegerInstance();

    public PowerShop() {
        setOpaque(false);
        setPreferredSize(new Dimension(250, HEADER_H + 4 * (CARD_H + GAP) + 34));
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getY());
                if (i != hover) {
                    hover = i;
                    setCursor(Cursor.getPredefinedCursor(i >= 0 && canBuy(powers().get(i)) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getY());
                if (i >= 0 && canBuy(powers().get(i))) onBuy.accept(powers().get(i));
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setOnBuy(Consumer<PowerUp> c) { onBuy = c; }

    public void setState(String role, int score, boolean roundActive, boolean usedThisRound, boolean locked) {
        this.role = role;
        this.score = score;
        this.roundActive = roundActive;
        this.usedThisRound = usedThisRound;
        this.locked = locked;
        repaint();
    }

    public void setArmed(boolean shield, boolean dbl) {
        shieldArmed = shield;
        doubleArmed = dbl;
        repaint();
    }

    private boolean isDrawer() { return role.equals("DRAWER"); }

    private List<PowerUp> powers() {
        List<PowerUp> l = new ArrayList<>();
        PowerUp.Who who = isDrawer() ? PowerUp.Who.DRAWER : PowerUp.Who.GUESSER;
        for (PowerUp p : PowerUp.values()) if (p.who() == who) l.add(p);
        return l;
    }

    private boolean inRound() {
        return roundActive && (role.equals("DRAWER") || role.equals("GUESSER"));
    }

    private boolean canBuy(PowerUp p) {
        return inRound() && !usedThisRound && !locked && score >= p.cost();
    }

    private String reason(PowerUp p) {
        if (!inRound()) return "Unlocks in a round";
        if (locked) return "You're frozen";
        if (usedThisRound) return "Used this round";
        if (score < p.cost()) return "Need " + (p.cost() - score) + " more";
        return null;
    }

    private int indexAt(int y) {
        int rel = y - HEADER_H;
        if (rel < 0) return -1;
        int i = rel / (CARD_H + GAP);
        if (rel % (CARD_H + GAP) >= CARD_H) return -1;
        return i < powers().size() ? i : -1;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth();

        Icons.paint(g, Icons.BOLT, 16, 14, 22, Theme.YELLOW);
        g.setFont(Theme.font(Font.BOLD, 13));
        g.setColor(Theme.TEXT);
        g.drawString("POWER-UPS", 46, 31);

        String pts = fmt.format(score) + " pts";
        g.setFont(Theme.font(Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.ink(Theme.YELLOW));
        g.drawString(pts, w - 16 - fm.stringWidth(pts), 31);

        g.setFont(Theme.font(Font.PLAIN, 11));
        g.setColor(Theme.TEXT_MUTED);
        String sub = !inRound() ? "Spend points once per round" : isDrawer() ? "You're drawing: hit a guesser" : "Spend points once per round";
        g.drawString(sub, 16, 49);

        List<PowerUp> items = powers();
        for (int i = 0; i < items.size(); i++) {
            PowerUp p = items.get(i);
            int y = HEADER_H + i * (CARD_H + GAP);
            boolean ok = canBuy(p);
            boolean hot = ok && i == hover;
            Color c = Widgets.powerColor(p);

            RoundRectangle2D card = new RoundRectangle2D.Double(10, y, w - 20, CARD_H, 16, 16);
            g.setColor(hot ? Theme.PANEL_RAISED : Theme.alpha(Theme.PANEL_LIGHT, ok ? 255 : 170));
            g.fill(card);
            g.setColor(hot ? c : Theme.alpha(ok ? c : Theme.BORDER, ok ? 140 : 150));
            g.setStroke(new BasicStroke(hot ? 2f : 1.2f));
            g.draw(card);

            // icon medallion
            double d = 38, ix = 19, iy = y + (CARD_H - d) / 2.0;
            g.setColor(Theme.alpha(c, ok ? 50 : 22));
            g.fill(new Ellipse2D.Double(ix, iy, d, d));
            Icons.paint(g, p.name(), ix + 7, iy + 7, 24, ok ? c : Theme.alpha(c, 110));

            // texts
            int tx = (int) (ix + d + 10);
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(ok ? Theme.TEXT : Theme.alpha(Theme.TEXT, 150));
            g.drawString(p.label(), tx, y + 22);
            if (p.isTargeted()) {
                int lw = g.getFontMetrics().stringWidth(p.label());
                Icons.paint(g, Icons.TARGET, tx + lw + 6, y + 11, 14, ok ? Theme.ink(c) : Theme.alpha(Theme.TEXT_MUTED, 160));
            }
            g.setFont(Theme.font(Font.PLAIN, 10));
            String r = reason(p);
            g.setColor(r != null && !(r.startsWith("Need")) ? Theme.alpha(Theme.TEXT_MUTED, 200) : Theme.alpha(Theme.TEXT_MUTED, ok ? 255 : 170));
            String line = r != null ? r : p.tagline();
            int maxW = w - 20 - (tx - 10) - 62;
            java.util.List<String> lines = wrap(line, g.getFontMetrics(), maxW, 2);
            for (int li = 0; li < lines.size(); li++) g.drawString(lines.get(li), tx, y + 38 + li * 12);

            // cost pill
            String cost = String.valueOf(p.cost());
            g.setFont(Theme.font(Font.BOLD, 12));
            FontMetrics cf = g.getFontMetrics();
            int pw = cf.stringWidth(cost) + 14, ph = 22;
            double px = w - 20 - pw, py = y + (CARD_H - ph) / 2.0;
            g.setColor(ok ? Theme.alpha(c, 60) : Theme.alpha(Theme.BORDER, 120));
            g.fill(new RoundRectangle2D.Double(px, py, pw, ph, ph, ph));
            g.setColor(ok ? Theme.ink(c) : Theme.alpha(Theme.TEXT_MUTED, 190));
            g.drawString(cost, (float) (px + 7), (float) (py + 15));
        }

        // armed self-powers
        int ay = HEADER_H + items.size() * (CARD_H + GAP) + 4;
        int ax = 14;
        if (shieldArmed) ax = chip(g, ax, ay, "SHIELD ACTIVE", Icons.SHIELD, Widgets.powerColor(PowerUp.SHIELD));
        if (doubleArmed) chip(g, ax, ay, "x2 READY", Icons.DOUBLE, Widgets.powerColor(PowerUp.DOUBLE));
        g.dispose();
    }

    private int chip(Graphics2D g, int x, int y, String text, String icon, Color c) {
        g.setFont(Theme.font(Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 34, h = 24;
        g.setColor(Theme.alpha(c, 45));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g.setColor(Theme.alpha(c, 200));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, h, h));
        Icons.paint(g, icon, x + 6, y + 4, 16, c);
        g.setColor(Theme.ink(c));
        g.drawString(text, x + 26, y + 16);
        return x + w + 6;
    }

    private static java.util.List<String> wrap(String text, FontMetrics fm, int max, int maxLines) {
        java.util.List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(test) <= max || cur.length() == 0) {
                cur = new StringBuilder(test);
            } else {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.size() > maxLines) {
            StringBuilder rest = new StringBuilder(lines.get(maxLines - 1));
            for (int i = maxLines; i < lines.size(); i++) rest.append(' ').append(lines.get(i));
            lines = new ArrayList<>(lines.subList(0, maxLines - 1));
            lines.add(ellipsize(rest.toString(), fm, max));
        }
        return lines;
    }

    private static String ellipsize(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "...") > max) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
