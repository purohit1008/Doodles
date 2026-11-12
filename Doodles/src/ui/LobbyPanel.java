package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Pre-game lobby: room code, players, and (for the host) the number of rounds and drawing time. */
public class LobbyPanel extends JPanel {

    public interface Listener {
        void onStart();
        void onSettings(int rounds, int seconds);
        void onCopy();
        void onClose();
    }

    public static final int[] TIMES = {30, 45, 60, 90, 120, 180};
    public static final int MIN_ROUNDS = 1, MAX_ROUNDS = 10;

    private final Listener listener;
    private final Board board = new Board();
    private final Widgets.PillButton startButton = new Widgets.PillButton("START GAME", Theme.SUCCESS);
    private final Widgets.PillButton closeButton = new Widgets.PillButton("Hide", Theme.PANEL_RAISED);

    private String code = "-----";
    private int hostId = -1, meId = -1, maxPlayers = 8;
    private String me = "";
    private List<String[]> people = new ArrayList<>();   // {id, name}
    private int rounds = 3, seconds = 60;
    private boolean copied;

    public LobbyPanel(Listener l) {
        this.listener = l;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(board, BorderLayout.CENTER);

        closeButton.setForeground(Theme.TEXT);
        closeButton.addActionListener(e -> listener.onClose());
        startButton.addActionListener(e -> listener.onStart());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));
        south.add(closeButton);
        south.add(startButton);
        add(south, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(520, 706));
    }

    public void update(String code, int hostId, List<String[]> people, String me, int meId, int max, boolean waiting) {
        this.code = code;
        this.hostId = hostId;
        this.people = people;
        this.me = me;
        this.meId = meId;
        this.maxPlayers = max;
        boolean host = isHost();
        startButton.setVisible(host && waiting);
        boolean ready = people.size() >= 2;
        startButton.setEnabled(host && ready);
        startButton.setText(ready ? "START GAME" : "Need 2+ players");
        board.repaint();
    }

    public void setSettings(int rounds, int seconds) {
        this.rounds = rounds;
        this.seconds = seconds;
        board.repaint();
    }

    public void setCopied(boolean c) {
        copied = c;
        board.repaint();
    }

    public int getRounds() { return rounds; }

    public int getSeconds() { return seconds; }

    private boolean isHost() { return meId >= 0 && meId == hostId; }

    // =========================================================================

    private final class Board extends JComponent {
        private Rectangle copyRect = new Rectangle(), minusRect = new Rectangle(), plusRect = new Rectangle();
        private final Rectangle[] timeRects = new Rectangle[TIMES.length];
        private int hover = -1;                    // 0 copy, 1 minus, 2 plus, 10+i time chips

        Board() {
            MouseAdapter m = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int h = hit(e.getPoint());
                    if (h != hover) {
                        hover = h;
                        setCursor(Cursor.getPredefinedCursor(h >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }

                @Override public void mouseClicked(MouseEvent e) {
                    int h = hit(e.getPoint());
                    if (h == 0) { listener.onCopy(); return; }
                    if (!isHost() || h < 0) return;
                    if (h == 1 && rounds > MIN_ROUNDS) listener.onSettings(rounds - 1, seconds);
                    else if (h == 2 && rounds < MAX_ROUNDS) listener.onSettings(rounds + 1, seconds);
                    else if (h >= 10) listener.onSettings(rounds, TIMES[h - 10]);
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private int hit(Point p) {
            if (copyRect.contains(p)) return 0;
            if (!isHost()) return -1;
            if (minusRect.contains(p)) return 1;
            if (plusRect.contains(p)) return 2;
            for (int i = 0; i < timeRects.length; i++) if (timeRects[i] != null && timeRects[i].contains(p)) return 10 + i;
            return -1;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            int w = getWidth();
            int pad = 26;

            // ---- header ----
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("ROOM CODE", pad + 4, 36);
            g.setFont(Theme.font(Font.BOLD, 34));
            FontMetrics fm = g.getFontMetrics();
            int codeW = fm.stringWidth(code) + 40;
            RoundRectangle2D pill = new RoundRectangle2D.Double(pad, 46, codeW, 56, 56, 56);
            g.setColor(Theme.alpha(Theme.SHADOW, 40));
            g.fill(new RoundRectangle2D.Double(pad, 49, codeW, 56, 56, 56));
            g.setPaint(new GradientPaint(pad, 46, Theme.SKY, pad + codeW, 102, Theme.PINK));
            g.fill(pill);
            g.setColor(Color.WHITE);
            g.drawString(code, pad + 20, 46 + (56 + fm.getAscent() - fm.getDescent()) / 2);

            copyRect = new Rectangle(pad + codeW + 12, 58, 98, 32);
            boolean hc = hover == 0;
            g.setColor(hc ? Theme.PANEL_RAISED : Theme.PANEL_LIGHT);
            g.fill(new RoundRectangle2D.Double(copyRect.x, copyRect.y, copyRect.width, copyRect.height, 32, 32));
            g.setColor(hc ? Theme.ACCENT_HOVER : Theme.BORDER);
            g.setStroke(new BasicStroke(1.3f));
            g.draw(new RoundRectangle2D.Double(copyRect.x + 0.5, copyRect.y + 0.5, copyRect.width - 1, copyRect.height - 1, 32, 32));
            Icons.paint(g, Icons.COPY, copyRect.x + 10, copyRect.y + 7, 18, copied ? Theme.SUCCESS : Theme.TEXT);
            g.setFont(Theme.font(Font.BOLD, 12));
            g.setColor(copied ? Theme.SUCCESS : Theme.TEXT);
            g.drawString(copied ? "Copied!" : "Copy", copyRect.x + 34, copyRect.y + 21);

            // player count
            String cnt = people.size() + " / " + maxPlayers;
            g.setFont(Theme.font(Font.BOLD, 13));
            fm = g.getFontMetrics();
            int cw = fm.stringWidth(cnt) + 42;
            double cx = w - pad - cw;
            g.setColor(Theme.PANEL_LIGHT);
            g.fill(new RoundRectangle2D.Double(cx, 62, cw, 30, 30, 30));
            Icons.paint(g, Icons.USERS, cx + 10, 68, 18, Theme.ACCENT);
            g.setColor(Theme.TEXT);
            g.drawString(cnt, (float) (cx + 32), 82);

            // ---- players (two columns) ----
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("PLAYERS", pad + 4, 134);
            int colW = (w - pad * 2 - 12) / 2;
            for (int i = 0; i < Math.max(people.size(), 1) && i < 8; i++) {
                int px = pad + (i % 2) * (colW + 12);
                int py = 146 + (i / 2) * 58;
                if (people.isEmpty()) {
                    g.setFont(Theme.font(Font.ITALIC, 14));
                    g.setColor(Theme.TEXT_MUTED);
                    g.drawString("Waiting for players...", px + 6, py + 30);
                    break;
                }
                String[] pl = people.get(i);
                boolean host = pl[0].equals(String.valueOf(hostId));
                boolean you = pl[1].equals(me);
                RoundRectangle2D card = new RoundRectangle2D.Double(px, py, colW, 50, 20, 20);
                g.setColor(Theme.alpha(Theme.SHADOW, 22));
                g.fill(new RoundRectangle2D.Double(px, py + 3, colW, 50, 20, 20));
                g.setColor(you ? new Color(0xEAF4FF) : Color.WHITE);
                g.fill(card);
                g.setColor(you ? Theme.ACCENT_HOVER : Theme.BORDER);
                g.setStroke(new BasicStroke(you ? 1.8f : 1.2f));
                g.draw(card);
                Widgets.avatar(g, pl[1], px + 8, py + 7, 36);
                g.setFont(Theme.font(Font.BOLD, 14));
                fm = g.getFontMetrics();
                String nm = ellipsize(pl[1], fm, colW - 64 - (host ? 26 : 0));
                g.setColor(Theme.TEXT);
                g.drawString(nm, px + 52, py + (you ? 24 : 30));
                if (you) {
                    g.setFont(Theme.font(Font.BOLD, 10));
                    g.setColor(Theme.ACCENT);
                    g.drawString("YOU", px + 52, py + 39);
                }
                if (host) Icons.paint(g, Icons.CROWN, px + colW - 30, py + 14, 22, Theme.GOLD);
            }

            // ---- settings card ----
            int sy = 146 + 4 * 58 + 10;
            int sh = 208;
            RoundRectangle2D card = new RoundRectangle2D.Double(pad, sy, w - pad * 2, sh, 26, 26);
            g.setColor(Theme.alpha(Theme.SHADOW, 22));
            g.fill(new RoundRectangle2D.Double(pad, sy + 4, w - pad * 2, sh, 26, 26));
            g.setColor(Color.WHITE);
            g.fill(card);
            g.setColor(Theme.BORDER);
            g.setStroke(new BasicStroke(1.3f));
            g.draw(card);

            Icons.paint(g, Icons.GEAR, pad + 18, sy + 16, 20, Theme.ACCENT);
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(Theme.TEXT);
            g.drawString("GAME SETTINGS", pad + 46, sy + 31);
            g.setFont(Theme.font(Font.PLAIN, 11));
            g.setColor(Theme.TEXT_MUTED);
            String who = isHost() ? "You are the host" : "Only the host can change these";
            fm = g.getFontMetrics();
            g.drawString(who, w - pad - 18 - fm.stringWidth(who), sy + 30);

            // rounds stepper
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(Theme.TEXT);
            g.drawString("Rounds", pad + 20, sy + 76);
            g.setFont(Theme.font(Font.PLAIN, 11));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("everyone draws once per round", pad + 20, sy + 92);
            int bx = w - pad - 20 - 152;
            minusRect = new Rectangle(bx, sy + 58, 38, 38);
            plusRect = new Rectangle(bx + 114, sy + 58, 38, 38);
            stepBtn(g, minusRect, Icons.MINUS, hover == 1, isHost() && rounds > MIN_ROUNDS);
            stepBtn(g, plusRect, Icons.PLUS, hover == 2, isHost() && rounds < MAX_ROUNDS);
            g.setFont(Theme.font(Font.BOLD, 24));
            fm = g.getFontMetrics();
            String rs = String.valueOf(rounds);
            g.setColor(Theme.TEXT);
            g.drawString(rs, bx + 76 - fm.stringWidth(rs) / 2, sy + 87);

            // time chips
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(Theme.TEXT);
            g.drawString("Drawing time", pad + 20, sy + 128);
            int chipW = (w - pad * 2 - 40 - (TIMES.length - 1) * 6) / TIMES.length;
            for (int i = 0; i < TIMES.length; i++) {
                Rectangle r = new Rectangle(pad + 20 + i * (chipW + 6), sy + 138, chipW, 30);
                timeRects[i] = r;
                boolean on = TIMES[i] == seconds;
                boolean hv = hover == 10 + i && isHost();
                RoundRectangle2D rr = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 30, 30);
                if (on) g.setPaint(new GradientPaint(r.x, r.y, Theme.SKY, r.x + r.width, r.y, Theme.PINK));
                else g.setColor(hv ? Theme.PANEL_RAISED : Theme.PANEL_LIGHT);
                g.fill(rr);
                if (!on) {
                    g.setColor(hv ? Theme.ACCENT_HOVER : Theme.BORDER);
                    g.draw(new RoundRectangle2D.Double(r.x + 0.5, r.y + 0.5, r.width - 1, r.height - 1, 30, 30));
                }
                g.setFont(Theme.font(Font.BOLD, 12));
                fm = g.getFontMetrics();
                String t = TIMES[i] + "s";
                g.setColor(on ? Color.WHITE : Theme.TEXT_MUTED);
                g.drawString(t, r.x + (r.width - fm.stringWidth(t)) / 2, r.y + 20);
            }

            // estimate
            int turns = Math.max(1, people.size()) * rounds;
            int minutes = (int) Math.round(turns * (seconds + 20) / 60.0);
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(turns + " drawings  -  about " + Math.max(1, minutes) + " min", pad + 20, sy + sh - 14);
            g.dispose();
        }

        private void stepBtn(Graphics2D g, Rectangle r, String icon, boolean hot, boolean active) {
            RoundRectangle2D rr = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14);
            g.setColor(active ? (hot ? Theme.PANEL_RAISED : Theme.PANEL_LIGHT) : Theme.alpha(Theme.PANEL_LIGHT, 130));
            g.fill(rr);
            g.setColor(active && hot ? Theme.ACCENT_HOVER : Theme.BORDER);
            g.setStroke(new BasicStroke(1.3f));
            g.draw(new RoundRectangle2D.Double(r.x + 0.5, r.y + 0.5, r.width - 1, r.height - 1, 14, 14));
            Icons.paint(g, icon, r.x + 9, r.y + 9, 20, active ? Theme.TEXT : Theme.alpha(Theme.TEXT_MUTED, 120));
        }
    }

    private static String ellipsize(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "...") > max) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
