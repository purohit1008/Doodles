package ui;

import common.Emotes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;

/** Emoji + sticker panel (shown in a popup above the chat box). */
public class EmojiPicker extends JPanel {

    public interface Listener {
        void onEmoji(String id);
        void onSticker(String id);
    }

    public static final int W = 316, H = 304;
    private static final int PAD = 12, TAB_H = 32;
    private static final int E_COLS = 8, E_CELL = 34;
    private static final int S_COLS = 3, S_W = 92, S_H = 44;

    private Listener listener = new Listener() {
        @Override public void onEmoji(String id) { }
        @Override public void onSticker(String id) { }
    };
    private boolean stickerTab;
    private boolean emojiEnabled = true;
    private int hover = -1;                  // index in the grid, or 1000+i for the recent row
    private final Deque<String> recent = new ArrayDeque<>();

    public EmojiPicker() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(W, H));
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int h = hit(e.getX(), e.getY());
                if (h != hover) {
                    hover = h;
                    setCursor(Cursor.getPredefinedCursor(h != -1 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override public void mouseClicked(MouseEvent e) {
                int tab = tabAt(e.getX(), e.getY());
                if (tab >= 0) {
                    stickerTab = tab == 1;
                    hover = -1;
                    repaint();
                    return;
                }
                int h = hit(e.getX(), e.getY());
                if (h < 0) return;
                if (stickerTab) {
                    if (h < Emotes.STICKERS.length) listener.onSticker(Emotes.STICKERS[h][0]);
                } else if (emojiEnabled) {
                    String id = h >= 1000 ? recentList()[h - 1000] : Emotes.EMOJI[h];
                    remember(id);
                    listener.onEmoji(id);
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setListener(Listener l) { listener = l; }

    public void setEmojiEnabled(boolean on) {
        emojiEnabled = on;
        if (!on) stickerTab = true;
        repaint();
    }

    public void setStickerTab(boolean on) {
        stickerTab = on;
        repaint();
    }

    private void remember(String id) {
        recent.remove(id);
        recent.addFirst(id);
        while (recent.size() > E_COLS) recent.removeLast();
    }

    private String[] recentList() { return recent.toArray(new String[0]); }

    // ---- geometry ----

    private Rectangle tabRect(int i) {
        int tw = (W - PAD * 2 - 8) / 2;
        return new Rectangle(PAD + i * (tw + 8), PAD, tw, TAB_H);
    }

    private int tabAt(int x, int y) {
        for (int i = 0; i < 2; i++) if (tabRect(i).contains(x, y)) return i;
        return -1;
    }

    private Rectangle emojiCell(int i) {
        int x0 = (W - E_COLS * E_CELL) / 2;
        return new Rectangle(x0 + (i % E_COLS) * E_CELL, PAD + TAB_H + 12 + (i / E_COLS) * E_CELL, E_CELL, E_CELL);
    }

    private Rectangle recentCell(int i) {
        int x0 = (W - E_COLS * E_CELL) / 2;
        int rows = (Emotes.EMOJI.length + E_COLS - 1) / E_COLS;
        return new Rectangle(x0 + i * E_CELL, PAD + TAB_H + 12 + rows * E_CELL + 36, E_CELL, E_CELL);
    }

    private Rectangle stickerCell(int i) {
        int x0 = (W - S_COLS * S_W) / 2;
        return new Rectangle(x0 + (i % S_COLS) * S_W, PAD + TAB_H + 10 + (i / S_COLS) * S_H, S_W, S_H);
    }

    private int hit(int x, int y) {
        if (stickerTab) {
            for (int i = 0; i < Emotes.STICKERS.length; i++) if (stickerCell(i).contains(x, y)) return i;
        } else {
            for (int i = 0; i < Emotes.EMOJI.length; i++) if (emojiCell(i).contains(x, y)) return i;
            String[] rec = recentList();
            for (int i = 0; i < rec.length; i++) if (recentCell(i).contains(x, y)) return 1000 + i;
        }
        return -1;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.setColor(Theme.BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(0, 0, W - 1, H - 1);

        // tabs
        String[] names = {"Emoji", "Stickers"};
        for (int i = 0; i < 2; i++) {
            Rectangle r = tabRect(i);
            boolean on = (i == 1) == stickerTab;
            RoundRectangle2D rr = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, r.height, r.height);
            if (on) g.setPaint(new GradientPaint(r.x, 0, Theme.SKY, r.x + r.width, 0, Theme.PINK));
            else g.setColor(Theme.PANEL_LIGHT);
            g.fill(rr);
            g.setFont(Theme.font(Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            boolean dim = i == 0 && !emojiEnabled;
            g.setColor(on ? Color.WHITE : dim ? Theme.alpha(Theme.TEXT_MUTED, 120) : Theme.TEXT_MUTED);
            g.drawString(names[i], r.x + (r.width - fm.stringWidth(names[i])) / 2, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }

        if (stickerTab) {
            for (int i = 0; i < Emotes.STICKERS.length; i++) {
                Rectangle r = stickerCell(i);
                if (hover == i) {
                    g.setColor(Theme.ACCENT_SOFT);
                    g.fill(new RoundRectangle2D.Double(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 14, 14));
                }
                EmoteArt.paintSticker(g, Emotes.STICKERS[i][0], r.x + 3, r.y + 3, r.width - 6, r.height - 6, false);
            }
            g.setFont(Theme.font(Font.PLAIN, 11));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("Stickers are sent to everyone right away", PAD + 6, H - 10);
        } else {
            for (int i = 0; i < Emotes.EMOJI.length; i++) {
                Rectangle r = emojiCell(i);
                if (hover == i && emojiEnabled) {
                    g.setColor(Theme.ACCENT_SOFT);
                    g.fill(new RoundRectangle2D.Double(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 12, 12));
                }
                paintMaybeDim(g, Emotes.EMOJI[i], r.x + 4, r.y + 4, E_CELL - 8);
            }
            int rows = (Emotes.EMOJI.length + E_COLS - 1) / E_COLS;
            int ly = PAD + TAB_H + 12 + rows * E_CELL + 22;
            g.setFont(Theme.font(Font.BOLD, 10));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("RECENT", (W - E_COLS * E_CELL) / 2 + 4, ly);
            String[] rec = recentList();
            if (rec.length == 0) {
                g.setFont(Theme.font(Font.ITALIC, 12));
                g.drawString("Emoji you use show up here", (W - E_COLS * E_CELL) / 2 + 4, ly + 34);
            }
            for (int i = 0; i < rec.length; i++) {
                Rectangle r = recentCell(i);
                if (hover == 1000 + i && emojiEnabled) {
                    g.setColor(Theme.ACCENT_SOFT);
                    g.fill(new RoundRectangle2D.Double(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 12, 12));
                }
                paintMaybeDim(g, rec[i], r.x + 4, r.y + 4, E_CELL - 8);
            }
            g.setFont(Theme.font(Font.PLAIN, 11));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(emojiEnabled ? "Tip: type :) or <3 and it becomes an emoji" : "Emoji can be typed by guessers - stickers work for all", PAD + 6, H - 10);
        }
        g.dispose();
    }

    private void paintMaybeDim(Graphics2D g, String id, double x, double y, double size) {
        if (emojiEnabled) {
            EmoteArt.paintEmoji(g, id, x, y, size);
        } else {
            Graphics2D d = (Graphics2D) g.create();
            d.setComposite(AlphaComposite.SrcOver.derive(0.35f));
            EmoteArt.paintEmoji(d, id, x, y, size);
            d.dispose();
        }
    }

    // ------------------------------------------------------------------ the button that opens it

    /** Round smiley button next to the chat box. */
    public static class Button extends JComponent {
        private boolean hot;
        private final Runnable onClick;

        public Button(Runnable onClick) {
            this.onClick = onClick;
            setPreferredSize(new Dimension(44, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Emoji & stickers");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hot = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hot = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (isEnabled()) onClick.run(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            double d = Math.min(getWidth(), getHeight()) - 4;
            double x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
            g.setColor(hot ? Theme.PANEL_RAISED : Theme.PANEL_LIGHT);
            g.fill(new Ellipse2D.Double(x, y, d, d));
            g.setColor(hot ? Theme.ACCENT_HOVER : Theme.BORDER);
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new Ellipse2D.Double(x, y, d, d));
            EmoteArt.paintEmoji(g, hot ? "laugh" : "smile", x + 7, y + 7 - (hot ? 1 : 0), d - 14);
            g.dispose();
        }
    }
}
