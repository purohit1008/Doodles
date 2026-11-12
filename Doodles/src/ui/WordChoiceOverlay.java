package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Sits on top of the canvas while the drawer picks a word.
 * Drawer: three cards to choose from. Everybody else: a friendly "X is choosing" message.
 */
public class WordChoiceOverlay extends JComponent {

    public static final class Card {
        public final String category, word;
        public final int difficulty;

        public Card(String category, String word, int difficulty) {
            this.category = category;
            this.word = word;
            this.difficulty = difficulty;
        }
    }

    private enum Mode { HIDDEN, CHOOSE, WAIT }

    private Mode mode = Mode.HIDDEN;
    private List<Card> cards = new ArrayList<>();
    private String drawerName = "";
    private long endAt;
    private int totalSeconds = 15;
    private long shownAt;
    private int hover = -1;
    private IntConsumer onPick = i -> { };
    private final Timer anim = new Timer(33, e -> repaint());

    public WordChoiceOverlay() {
        setOpaque(false);
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = mode == Mode.CHOOSE ? cardAt(e.getPoint()) : -1;
                if (i != hover) {
                    hover = i;
                    setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (mode != Mode.CHOOSE) return;
                int i = cardAt(e.getPoint());
                if (i >= 0) pick(i);
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    /** While visible the overlay swallows mouse events so nobody can draw underneath it. */
    @Override public boolean contains(int x, int y) { return mode != Mode.HIDDEN && super.contains(x, y); }

    public void setOnPick(IntConsumer c) { onPick = c; }

    public boolean isShowingChoices() { return mode == Mode.CHOOSE; }

    public void showChoices(List<Card> list, int seconds) {
        cards = new ArrayList<>(list);
        totalSeconds = Math.max(1, seconds);
        endAt = System.currentTimeMillis() + seconds * 1000L;
        shownAt = System.currentTimeMillis();
        mode = Mode.CHOOSE;
        hover = -1;
        anim.start();
        setVisible(true);
        repaint();
    }

    public void showWaiting(String drawer, int seconds) {
        drawerName = drawer;
        totalSeconds = Math.max(1, seconds);
        endAt = System.currentTimeMillis() + seconds * 1000L;
        shownAt = System.currentTimeMillis();
        mode = Mode.WAIT;
        anim.start();
        setVisible(true);
        repaint();
    }

    public void hideOverlay() {
        mode = Mode.HIDDEN;
        anim.stop();
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    public void pick(int i) {
        if (mode != Mode.CHOOSE || i < 0 || i >= cards.size()) return;
        mode = Mode.HIDDEN;
        anim.stop();
        repaint();
        onPick.accept(i);
    }

    // ---- layout ----

    private Rectangle cardRect(int i) {
        int n = Math.max(1, cards.size());
        int cw = Math.min(190, (getWidth() - 80 - (n - 1) * 18) / n);
        int ch = 150;
        int total = n * cw + (n - 1) * 18;
        int x = (getWidth() - total) / 2 + i * (cw + 18);
        int y = getHeight() / 2 - ch / 2 + 14;
        return new Rectangle(x, y, cw, ch);
    }

    private int cardAt(Point p) {
        for (int i = 0; i < cards.size(); i++) if (cardRect(i).contains(p)) return i;
        return -1;
    }

    @Override protected void paintComponent(Graphics g0) {
        if (mode == Mode.HIDDEN) return;
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        long now = System.currentTimeMillis();
        double appear = Math.min(1, (now - shownAt) / 260.0);
        double leftSec = Math.max(0, (endAt - now) / 1000.0);

        g.setComposite(AlphaComposite.SrcOver.derive((float) appear));
        g.setColor(new Color(236, 246, 255, 232));
        g.fillRect(0, 0, w, h);

        if (mode == Mode.WAIT) {
            paintWaiting(g, w, h, now, leftSec);
            g.dispose();
            return;
        }

        g.setFont(Theme.font(Font.BOLD, 24));
        String title = "Pick a word to draw";
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.TEXT);
        g.drawString(title, (w - fm.stringWidth(title)) / 2, h / 2 - 108);
        g.setFont(Theme.font(Font.PLAIN, 13));
        g.setColor(Theme.TEXT_MUTED);
        String sub = "Press 1, 2 or 3  -  or click a card";
        fm = g.getFontMetrics();
        g.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 - 84);

        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            Rectangle r = cardRect(i);
            double pop = Math.min(1, Math.max(0, (now - shownAt - i * 80) / 260.0));
            double ease = 1 - Math.pow(1 - pop, 3);
            double lift = (hover == i ? -6 : 0) + (1 - ease) * 26;
            Color accent = i == 0 ? Theme.SKY : i == 1 ? Theme.PINK : Theme.MINT;
            RoundRectangle2D rr = new RoundRectangle2D.Double(r.x, r.y + lift, r.width, r.height, 26, 26);

            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(appear, ease)));
            g.setColor(Theme.alpha(Theme.SHADOW, hover == i ? 70 : 45));
            g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + lift + 7, r.width - 4, r.height, 26, 26));
            g.setColor(Color.WHITE);
            g.fill(rr);
            g.setColor(hover == i ? accent : Theme.alpha(accent, 150));
            g.setStroke(new BasicStroke(hover == i ? 3f : 2f));
            g.draw(rr);

            // number badge
            g.setColor(accent);
            g.fill(new Ellipse2D.Double(r.x + 14, r.y + lift + 14, 26, 26));
            g.setFont(Theme.font(Font.BOLD, 14));
            g.setColor(Color.WHITE);
            String num = String.valueOf(i + 1);
            fm = g.getFontMetrics();
            g.drawString(num, r.x + 14 + (26 - fm.stringWidth(num)) / 2, (int) (r.y + lift + 14 + (26 + fm.getAscent() - fm.getDescent()) / 2.0));

            // category chip
            g.setFont(Theme.font(Font.BOLD, 10));
            fm = g.getFontMetrics();
            String cat = c.category.toUpperCase();
            int cw = fm.stringWidth(cat) + 16;
            g.setColor(Theme.alpha(accent, 40));
            g.fill(new RoundRectangle2D.Double(r.x + r.width - cw - 12, r.y + lift + 17, cw, 20, 20, 20));
            g.setColor(Theme.ink(accent));
            g.drawString(cat, r.x + r.width - cw - 12 + 8, (int) (r.y + lift + 31));

            // the word, shrunk to fit
            int size = 28;
            Font f = Theme.font(Font.BOLD, size);
            g.setFont(f);
            fm = g.getFontMetrics();
            while (fm.stringWidth(c.word) > r.width - 24 && size > 12) {
                size--;
                g.setFont(Theme.font(Font.BOLD, size));
                fm = g.getFontMetrics();
            }
            g.setColor(Theme.TEXT);
            g.drawString(c.word, r.x + (r.width - fm.stringWidth(c.word)) / 2, (int) (r.y + lift + 88));

            // difficulty dots
            String[] names = {"Easy", "Medium", "Hard"};
            int d = Math.max(1, Math.min(3, c.difficulty));
            double dx = r.x + r.width / 2.0 - 34;
            for (int k = 0; k < 3; k++) {
                g.setColor(k < d ? accent : Theme.BORDER);
                g.fill(new Ellipse2D.Double(dx + k * 14, r.y + lift + 112, 9, 9));
            }
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(names[d - 1], (float) (dx + 46), (float) (r.y + lift + 121));
        }

        g.setComposite(AlphaComposite.SrcOver);
        paintCountdown(g, w, h / 2 + 14 + 90, leftSec, "or we pick one for you");
        g.dispose();
    }

    private void paintCountdown(Graphics2D g, int w, int y, double leftSec, String text) {
        double frac = Math.max(0, Math.min(1, leftSec / totalSeconds));
        double barW = 260, x = (w - barW) / 2.0;
        g.setColor(Theme.alpha(Theme.SHADOW, 28));
        g.fill(new RoundRectangle2D.Double(x, y, barW, 8, 8, 8));
        g.setPaint(new GradientPaint((float) x, 0, Theme.SKY, (float) (x + barW), 0, Theme.PINK));
        g.fill(new RoundRectangle2D.Double(x, y, barW * frac, 8, 8, 8));
        g.setFont(Theme.font(Font.PLAIN, 12));
        g.setColor(Theme.TEXT_MUTED);
        String t = (int) Math.ceil(leftSec) + "s  -  " + text;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, (int) (w / 2.0 - fm.stringWidth(t) / 2.0), y + 26);
    }

    private void paintWaiting(Graphics2D g, int w, int h, long now, double leftSec) {
        Widgets.avatar(g, drawerName, w / 2.0 - 34, h / 2.0 - 110, 68);
        g.setFont(Theme.font(Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String t = drawerName + " is choosing a word";
        g.setColor(Theme.TEXT);
        g.drawString(t, (w - fm.stringWidth(t)) / 2, h / 2 - 8);
        // bouncing dots
        for (int i = 0; i < 3; i++) {
            double bounce = Math.abs(Math.sin((now / 220.0) - i * 0.6)) * 9;
            g.setColor(i == 0 ? Theme.SKY : i == 1 ? Theme.PINK : Theme.MINT);
            g.fill(new Ellipse2D.Double(w / 2.0 - 27 + i * 20, h / 2.0 + 14 - bounce, 14, 14));
        }
        paintCountdown(g, w, h / 2 + 60, leftSec, "get ready to guess");
    }
}
