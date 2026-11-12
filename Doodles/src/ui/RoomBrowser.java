package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Live list of public rooms: pick one to join, or create your own. */
public final class RoomBrowser {

    public interface Listener {
        void onJoin(String code);
        void onCreate();
        void onBack();
        void onRefresh();
    }

    private final Listener listener;
    private final RoomList list = new RoomList();
    private JDialog dialog;
    private javax.swing.Timer poll;
    private boolean closing;

    private JPanel body;

    public RoomBrowser(Component parent, Listener l) {
        this(l);
        dialog = Dialogs.framed(parent, "Public rooms", body, false, Dialogs.Style.WHITE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                poll.stop();
                if (!closing) listener.onBack();
            }
        });
        poll = new javax.swing.Timer(2500, e -> listener.onRefresh());
    }

    /** Builds only the content (no window). */
    private RoomBrowser(Listener l) {
        this.listener = l;
        body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 26, 24, 26));

        JLabel sub = Widgets.label("Pick a room to jump in - the list updates by itself", Theme.font(Font.PLAIN, 13), Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(sub);
        body.add(Box.createVerticalStrut(12));

        JScrollPane scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(470, 300));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(scroll);
        body.add(Box.createVerticalStrut(18));

        Widgets.PillButton back = Dialogs.button("Back", new Color(0x9AA6C6), 120);
        Widgets.PillButton create = Dialogs.button("Create a public room", Theme.ACCENT, 200).icon(Icons.PLUS);
        back.addActionListener(e -> { if (dialog != null) dialog.dispose(); });
        create.addActionListener(e -> listener.onCreate());
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        row.add(back);
        row.add(create);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(row);
    }

    /** The browser's look without a window (used for screenshots). */
    public static JComponent preview(List<String[]> rooms) {
        RoomBrowser rb = new RoomBrowser(new Listener() {
            @Override public void onJoin(String code) { }
            @Override public void onCreate() { }
            @Override public void onBack() { }
            @Override public void onRefresh() { }
        });
        rb.update(rooms);
        return Dialogs.framePreview("Public rooms", rb.body, Dialogs.Style.WHITE);
    }

    /** rooms: {code, "players/max", hostName}. */
    public void update(List<String[]> rooms) { list.setRooms(rooms); }

    public void show() {
        dialog.setVisible(true);
        poll.start();
    }

    public void close() {
        closing = true;
        poll.stop();
        dialog.dispose();
    }

    public boolean isOpen() { return dialog.isDisplayable() && !closing; }

    // =========================================================================

    private final class RoomList extends JComponent implements Scrollable {
        private List<String[]> rooms = new ArrayList<>();
        private int hover = -1;
        private static final int ROW = 66, GAP = 8;

        RoomList() {
            MouseAdapter m = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int h = joinAt(e.getPoint());
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
                    int h = joinAt(e.getPoint());
                    if (h >= 0 && h < rooms.size()) listener.onJoin(rooms.get(h)[0]);
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void setRooms(List<String[]> r) {
            rooms = new ArrayList<>(r);
            hover = -1;
            revalidate();
            repaint();
        }

        private Rectangle joinRect(int i) { return new Rectangle(getWidth() - 12 - 96 - 6, i * (ROW + GAP) + (ROW - 38) / 2, 96, 38); }

        private int joinAt(Point p) {
            for (int i = 0; i < rooms.size(); i++) if (joinRect(i).contains(p)) return i;
            return -1;
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(470, Math.max(300, rooms.size() * (ROW + GAP)));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            int w = getWidth() - 12;
            if (rooms.isEmpty()) {
                EmoteArt.paintEmoji(g, "think", getWidth() / 2.0 - 36, 52, 72);
                g.setFont(Theme.font(Font.BOLD, 18));
                FontMetrics fm = g.getFontMetrics();
                String t = "No public rooms right now";
                g.setColor(Theme.TEXT);
                g.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, 164);
                g.setFont(Theme.font(Font.PLAIN, 13));
                fm = g.getFontMetrics();
                String s = "Create one and invite your friends!";
                g.setColor(Theme.TEXT_MUTED);
                g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, 190);
                g.dispose();
                return;
            }
            for (int i = 0; i < rooms.size(); i++) {
                String[] r = rooms.get(i);
                int y = i * (ROW + GAP);
                RoundRectangle2D card = new RoundRectangle2D.Double(2, y + 2, w - 4, ROW - 4, 22, 22);
                g.setColor(Theme.alpha(Theme.SHADOW, 20));
                g.fill(new RoundRectangle2D.Double(2, y + 5, w - 4, ROW - 4, 22, 22));
                g.setColor(Color.WHITE);
                g.fill(card);
                g.setColor(Theme.BORDER);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(card);

                Widgets.avatar(g, r[2], 14, y + (ROW - 40) / 2.0, 40);
                g.setFont(Theme.font(Font.BOLD, 15));
                FontMetrics fm = g.getFontMetrics();
                String host = ellipsize(r[2], fm, w - 14 - 40 - 12 - 96 - 90 - 40);
                g.setColor(Theme.TEXT);
                g.drawString(host, 66, y + 28);
                g.setFont(Theme.font(Font.BOLD, 11));
                g.setColor(Theme.TEXT_MUTED);
                g.drawString("ROOM " + r[0], 66, y + 47);

                // players pill
                String pl = r[1];
                g.setFont(Theme.font(Font.BOLD, 12));
                fm = g.getFontMetrics();
                int pw = fm.stringWidth(pl) + 40;
                double px = joinRect(i).x - 12 - pw;
                g.setColor(Theme.PANEL_LIGHT);
                g.fill(new RoundRectangle2D.Double(px, y + (ROW - 28) / 2.0, pw, 28, 28, 28));
                Icons.paint(g, Icons.USERS, px + 10, y + (ROW - 16) / 2.0, 16, Theme.ACCENT);
                g.setColor(Theme.TEXT);
                g.drawString(pl, (float) (px + 30), (float) (y + ROW / 2.0 + 4.5));

                // join pill
                Rectangle jr = joinRect(i);
                boolean hot = hover == i;
                g.setColor(Theme.alpha(Theme.SHADOW, 40));
                g.fill(new RoundRectangle2D.Double(jr.x, jr.y + 3, jr.width, jr.height, jr.height, jr.height));
                g.setPaint(new GradientPaint(jr.x, jr.y, hot ? Theme.ACCENT_HOVER : Theme.ACCENT, jr.x + jr.width, jr.y, hot ? Theme.PINK : Theme.ACCENT_PINK));
                g.fill(new RoundRectangle2D.Double(jr.x, jr.y, jr.width, jr.height, jr.height, jr.height));
                g.setFont(Theme.font(Font.BOLD, 14));
                fm = g.getFontMetrics();
                g.setColor(Color.WHITE);
                g.drawString("Join", jr.x + (jr.width - fm.stringWidth("Join")) / 2, jr.y + (jr.height + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }

        private String ellipsize(String s, FontMetrics fm, int max) {
            if (fm.stringWidth(s) <= max) return s;
            String t = s;
            while (t.length() > 1 && fm.stringWidth(t + "...") > max) t = t.substring(0, t.length() - 1);
            return t + "...";
        }

        // Scrollable: follow the viewport width
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(470, 300); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
