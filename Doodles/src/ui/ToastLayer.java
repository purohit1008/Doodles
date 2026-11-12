package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Transparent overlay (use as the window's glass pane): slide-in notifications, floating sticker
 * reactions, the end-of-round recap card and confetti. It never steals mouse events.
 */
public class ToastLayer extends JComponent {

    // ---------------------------------------------------------------- data

    private static final class Toast {
        final String title, sub, icon;
        final Color color;
        final long born = System.currentTimeMillis();
        final int life;

        Toast(String title, String sub, String icon, Color color, int life) {
            this.title = title;
            this.sub = sub;
            this.icon = icon;
            this.color = color;
            this.life = life;
        }
    }

    private static final class Pop {
        final String id, who;
        final double cx, cy, drift;
        final long born = System.currentTimeMillis();
        final int life = 2800;

        Pop(String id, String who, double cx, double cy, double drift) {
            this.id = id;
            this.who = who;
            this.cx = cx;
            this.cy = cy;
            this.drift = drift;
        }
    }

    private static final class Bit {
        double x, y, vx, vy, rot, vr;
        Color color;
        int w, h;
        boolean round;
    }

    public static final class RecapRow {
        public final String name;
        public final int gained;
        public final boolean drawer;

        public RecapRow(String name, int gained, boolean drawer) {
            this.name = name;
            this.gained = gained;
            this.drawer = drawer;
        }
    }

    private final List<Toast> toasts = new ArrayList<>();
    private final List<Pop> pops = new ArrayList<>();
    private final List<Bit> bits = new ArrayList<>();
    private final Random rnd = new Random();
    private final Timer timer = new Timer(30, e -> tick());
    private long lastTick = System.currentTimeMillis();
    private int topOffset = 110;

    private String recapWord;
    private List<RecapRow> recapRows = new ArrayList<>();
    private Rectangle recapArea = new Rectangle(0, 0, 0, 0);
    private long recapBorn;
    private int recapLife;

    public ToastLayer() {
        setOpaque(false);
    }

    /** Never steals mouse events from the UI underneath. */
    @Override public boolean contains(int x, int y) { return false; }

    public void setTopOffset(int y) { topOffset = y; }

    private void wake() {
        if (!timer.isRunning()) {
            lastTick = System.currentTimeMillis();
            timer.start();
        }
        repaint();
    }

    // ---------------------------------------------------------------- public API

    public void show(String title, String sub, String icon, Color color) {
        show(title, sub, icon, color, 2600);
    }

    public void show(String title, String sub, String icon, Color color, int millis) {
        SwingUtilities.invokeLater(() -> {
            toasts.add(new Toast(title, sub, icon, color, millis));
            while (toasts.size() > 3) toasts.remove(0);
            wake();
        });
    }

    /** A sticker pops up over the given area (usually the canvas) and floats away. */
    public void pop(String stickerId, String who, Rectangle area) {
        SwingUtilities.invokeLater(() -> {
            double cx = area.x + area.width * (0.22 + rnd.nextDouble() * 0.56);
            double cy = area.y + area.height * (0.62 + rnd.nextDouble() * 0.2);
            pops.add(new Pop(stickerId, who, cx, cy, (rnd.nextDouble() - 0.5) * 50));
            while (pops.size() > 6) pops.remove(0);
            wake();
        });
    }

    public void showRecap(String word, List<RecapRow> rows, Rectangle area, int millis) {
        SwingUtilities.invokeLater(() -> {
            recapWord = word;
            recapRows = new ArrayList<>(rows);
            recapArea = new Rectangle(area);
            recapBorn = System.currentTimeMillis();
            recapLife = millis;
            wake();
        });
    }

    public void hideRecap() {
        SwingUtilities.invokeLater(() -> {
            recapWord = null;
            repaint();
        });
    }

    public void confetti(Rectangle area, int count) {
        SwingUtilities.invokeLater(() -> {
            Color[] pal = {Theme.SKY, Theme.PINK, Theme.MINT, Theme.YELLOW, Theme.ORANGE, new Color(0xB59CFF)};
            for (int i = 0; i < count; i++) {
                Bit b = new Bit();
                b.x = area.x + area.width * (0.2 + rnd.nextDouble() * 0.6);
                b.y = area.y + area.height * 0.25;
                double ang = -Math.PI / 2 + (rnd.nextDouble() - 0.5) * 2.2;
                double sp = 280 + rnd.nextDouble() * 420;
                b.vx = Math.cos(ang) * sp;
                b.vy = Math.sin(ang) * sp;
                b.rot = rnd.nextDouble() * 6;
                b.vr = (rnd.nextDouble() - 0.5) * 14;
                b.color = pal[rnd.nextInt(pal.length)];
                b.w = 6 + rnd.nextInt(7);
                b.h = 4 + rnd.nextInt(6);
                b.round = rnd.nextInt(4) == 0;
                bits.add(b);
            }
            wake();
        });
    }

    // ---------------------------------------------------------------- animation

    private void tick() {
        long now = System.currentTimeMillis();
        double dt = Math.min(0.1, (now - lastTick) / 1000.0);
        lastTick = now;
        for (Iterator<Toast> it = toasts.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            if (now - t.born > t.life) it.remove();
        }
        pops.removeIf(p -> now - p.born > p.life);
        for (Iterator<Bit> it = bits.iterator(); it.hasNext(); ) {
            Bit b = it.next();
            b.vy += 900 * dt;
            b.vx *= Math.pow(0.35, dt);
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.rot += b.vr * dt;
            if (b.y > getHeight() + 30) it.remove();
        }
        if (recapWord != null && now - recapBorn > recapLife) recapWord = null;
        if (toasts.isEmpty() && pops.isEmpty() && bits.isEmpty() && recapWord == null) timer.stop();
        repaint();
    }

    // ---------------------------------------------------------------- painting

    @Override protected void paintComponent(Graphics g0) {
        if (toasts.isEmpty() && pops.isEmpty() && bits.isEmpty() && recapWord == null) return;
        Graphics2D g = Theme.smooth(g0);
        long now = System.currentTimeMillis();
        if (recapWord != null) paintRecap(g, now);
        paintPops(g, now);
        paintBits(g);
        paintToasts(g, now);
        g.dispose();
    }

    private void paintToasts(Graphics2D g, long now) {
        int y = topOffset;
        for (Toast t : toasts) {
            long age = now - t.born;
            double in = Math.min(1, age / 260.0);
            double out = Math.min(1, Math.max(0, (t.life - age) / 300.0));
            double a = Math.min(in, out);
            double ease = 1 - Math.pow(1 - in, 3);

            g.setFont(Theme.font(Font.BOLD, 16));
            FontMetrics tf = g.getFontMetrics();
            g.setFont(Theme.font(Font.PLAIN, 12));
            FontMetrics sf = g.getFontMetrics();
            int textW = Math.max(tf.stringWidth(t.title), t.sub == null ? 0 : sf.stringWidth(t.sub));
            int w = textW + 86;
            int h = t.sub == null ? 52 : 66;
            double x = (getWidth() - w) / 2.0;
            double yy = y - (1 - ease) * 18;

            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, a))));
            g.setColor(Theme.alpha(Theme.SHADOW, 60));
            g.fill(new RoundRectangle2D.Double(x, yy + 5, w, h, h, h));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(x, yy, w, h, h, h));
            g.setColor(t.color);
            g.setStroke(new BasicStroke(2.2f));
            g.draw(new RoundRectangle2D.Double(x + 1, yy + 1, w - 2, h - 2, h, h));
            Icons.paint(g, t.icon, x + 18, yy + (h - 30) / 2.0, 30, t.color);
            g.setFont(Theme.font(Font.BOLD, 16));
            g.setColor(Theme.TEXT);
            g.drawString(t.title, (float) (x + 60), (float) (yy + (t.sub == null ? h / 2.0 + 6 : 28)));
            if (t.sub != null) {
                g.setFont(Theme.font(Font.PLAIN, 12));
                g.setColor(Theme.TEXT_MUTED);
                g.drawString(t.sub, (float) (x + 60), (float) (yy + 47));
            }
            g.setComposite(old);
            y += h + 10;
        }
    }

    private void paintPops(Graphics2D g, long now) {
        for (Pop p : pops) {
            double age = (now - p.born) / (double) p.life;
            double in = Math.min(1, (now - p.born) / 280.0);
            double overshoot = in < 1 ? 1 + Math.sin(in * Math.PI) * 0.18 : 1;
            double scale = (0.4 + 0.6 * in) * overshoot;
            double fade = age > 0.78 ? Math.max(0, (1 - age) / 0.22) : 1;
            double rise = age * 120;
            double sw = 150, sh = 58;
            double x = p.cx - sw * scale / 2 + p.drift * age;
            double y = p.cy - rise - sh * scale / 2;

            Graphics2D d = (Graphics2D) g.create();
            d.setComposite(AlphaComposite.SrcOver.derive((float) fade));
            d.translate(x, y);
            d.scale(scale, scale);
            EmoteArt.paintSticker(d, p.id, 0, 0, sw, sh, true);
            d.setFont(Theme.font(Font.BOLD, 11));
            FontMetrics fm = d.getFontMetrics();
            int cw = fm.stringWidth(p.who) + 16;
            d.setColor(Theme.alpha(Color.WHITE, 235));
            d.fill(new RoundRectangle2D.Double((sw - cw) / 2, sh + 2, cw, 18, 18, 18));
            d.setColor(Theme.TEXT);
            d.drawString(p.who, (float) ((sw - cw) / 2 + 8), (float) (sh + 15));
            d.dispose();
        }
    }

    private void paintBits(Graphics2D g) {
        AffineTransform old = g.getTransform();
        for (Bit b : bits) {
            g.setTransform(old);
            g.translate(b.x, b.y);
            g.rotate(b.rot);
            g.setColor(b.color);
            if (b.round) g.fill(new Ellipse2D.Double(-b.w / 2.0, -b.w / 2.0, b.w, b.w));
            else g.fill(new Rectangle2D.Double(-b.w / 2.0, -b.h / 2.0, b.w, b.h));
        }
        g.setTransform(old);
    }

    private void paintRecap(Graphics2D g, long now) {
        long age = now - recapBorn;
        double in = Math.min(1, age / 300.0);
        double out = Math.min(1, Math.max(0, (recapLife - age) / 350.0));
        double a = Math.min(in, out);
        double ease = 1 - Math.pow(1 - in, 3);
        int rows = Math.min(6, recapRows.size());
        int w = 380, h = 112 + rows * 40 + 16;
        double x = recapArea.getCenterX() - w / 2.0;
        double y = recapArea.getCenterY() - h / 2.0 + (1 - ease) * 20;

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, a))));
        g.setColor(Theme.alpha(Theme.SHADOW, 60));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 10, w - 6, h, 30, 30));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 30, 30));
        g.setPaint(new GradientPaint((float) x, 0, Theme.SKY, (float) (x + w), 0, Theme.PINK));
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Double(x + 1.5, y + 1.5, w - 3, h - 3, 30, 30));

        g.setFont(Theme.font(Font.BOLD, 11));
        g.setColor(Theme.TEXT_MUTED);
        String small = "THE WORD WAS";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(small, (float) (x + (w - fm.stringWidth(small)) / 2.0), (float) (y + 28));

        int size = 34;
        g.setFont(Theme.font(Font.BOLD, size));
        fm = g.getFontMetrics();
        while (fm.stringWidth(recapWord) > w - 40 && size > 14) {
            size--;
            g.setFont(Theme.font(Font.BOLD, size));
            fm = g.getFontMetrics();
        }
        g.setColor(Theme.TEXT);
        g.drawString(recapWord, (float) (x + (w - fm.stringWidth(recapWord)) / 2.0), (float) (y + 68));
        g.setPaint(new GradientPaint((float) (x + 40), 0, Theme.SKY, (float) (x + w - 40), 0, Theme.PINK));
        g.fill(new RoundRectangle2D.Double(x + w / 2.0 - 40, y + 78, 80, 4, 4, 4));

        NumberFormat fmt = NumberFormat.getIntegerInstance();
        for (int i = 0; i < rows; i++) {
            RecapRow r = recapRows.get(i);
            double ry = y + 98 + i * 40;
            g.setColor(Theme.PANEL_LIGHT);
            g.fill(new RoundRectangle2D.Double(x + 16, ry, w - 32, 34, 17, 17));
            Widgets.avatar(g, r.name, x + 20, ry + 2, 30);
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(Theme.TEXT);
            String nm = r.name + (r.drawer ? "  (drawer)" : "");
            g.drawString(nm, (float) (x + 60), (float) (ry + 22));
            String pts = r.gained > 0 ? "+" + fmt.format(r.gained) : "0";
            g.setFont(Theme.font(Font.BOLD, 13));
            fm = g.getFontMetrics();
            int pw = fm.stringWidth(pts) + 20;
            Color pc = r.gained > 0 ? Theme.MINT : Theme.TEXT_MUTED;
            g.setColor(Theme.alpha(pc, 45));
            g.fill(new RoundRectangle2D.Double(x + w - 28 - pw, ry + 6, pw, 22, 22, 22));
            g.setColor(r.gained > 0 ? Theme.ink(Theme.MINT) : Theme.TEXT_MUTED);
            g.drawString(pts, (float) (x + w - 28 - pw + 10), (float) (ry + 22));
        }
        g.setComposite(old);
    }
}
