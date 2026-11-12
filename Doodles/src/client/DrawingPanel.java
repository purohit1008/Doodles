package client;

import common.PowerUp;
import ui.Icons;
import ui.Theme;
import ui.Widgets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * The shared canvas.
 *
 * - Fixed 900x540 logical canvas, scaled to fit the window, so every player sees the same picture.
 * - Every drawing action (stroke / fill) is an Op in one history list, used identically for the
 *   drawer and for every guesser, so UNDO and replays always match.
 * - Power-up effects (blur, flip, spotlight, freeze) are painted on top of the canvas on the
 *   affected player's screen only.
 *
 * Network format (relayed by the server, drawer -> everyone else):
 *   DRAW x y r g b size brushId     one stroke point
 *   UP                              end of stroke
 *   FILL x y r g b                  flood fill
 *   UNDO                            remove the last stroke / fill
 *   CANVAS_CLEAR                    wipe everything
 */
public class DrawingPanel extends JPanel {

    public static final int CANVAS_W = 900;
    public static final int CANVAS_H = 540;
    private static final int PAD = 14;
    private static final int BAKE_AFTER = 140;

    public enum Tool { BRUSH, ERASER, FILL, LINE, RECT, ELLIPSE }

    // ---- history ----
    private abstract static class Op {}

    private static final class StrokeOp extends Op {
        final Brush brush;
        final Color color;
        final int size;
        final List<Point> pts = new ArrayList<>();
        double dist = 0;

        StrokeOp(Brush brush, Color color, int size) {
            this.brush = brush;
            this.color = color;
            this.size = size;
        }

        /** Adds a point and paints just that new piece. */
        void add(Graphics2D g, Point p) {
            Point prev = pts.isEmpty() ? null : pts.get(pts.size() - 1);
            dist = brush.draw(g, prev, p, color, size, dist);
            pts.add(p);
        }
    }

    private static final class FillOp extends Op {
        final Point at;
        final Color color;

        FillOp(Point at, Color color) {
            this.at = at;
            this.color = color;
        }
    }

    private PrintWriter out;
    private final BufferedImage canvas = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage base = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_ARGB);
    private final Graphics2D cg;
    private final List<Op> history = new ArrayList<>();

    // ---- tool state ----
    private Tool tool = Tool.BRUSH;
    private Brush brush = Brush.PEN;
    private Color color = Color.BLACK;
    private int size = 6;
    private boolean canDraw = true;

    // ---- stroke in progress ----
    private StrokeOp localStroke;
    private StrokeOp remoteStroke;
    private Point shapeStart, shapeEnd;

    // ---- view ----
    private Point hover;          // logical coords of the mouse, or null
    private Point hoverScreen;    // raw component coords

    // ---- power-up effects on THIS player ----
    private final Map<PowerUp, Long> effects = new EnumMap<>(PowerUp.class);
    private final javax.swing.Timer effectTimer = new javax.swing.Timer(60, e -> tickEffects());

    public DrawingPanel(PrintWriter out) {
        this.out = out;
        setOpaque(true);
        setBackground(Theme.PANEL_LIGHT);
        setPreferredSize(new Dimension(CANVAS_W + PAD * 2, CANVAS_H + PAD * 2));
        setMinimumSize(new Dimension(520, 320));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        cg = canvas.createGraphics();
        wipe(canvas, cg);
        Graphics2D bg = base.createGraphics();
        wipe(base, bg);
        bg.dispose();

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                hoverScreen = e.getPoint();
                hover = toLogical(e.getPoint());
                if (!canDraw || !SwingUtilities.isLeftMouseButton(e)) return;
                beginAction(hover);
            }

            @Override public void mouseDragged(MouseEvent e) {
                hoverScreen = e.getPoint();
                hover = toLogical(e.getPoint());
                if (canDraw) continueAction(hover);
                repaint();
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (canDraw) endAction();
            }

            @Override public void mouseMoved(MouseEvent e) {
                hoverScreen = e.getPoint();
                hover = toLogical(e.getPoint());
                repaint();
            }

            @Override public void mouseExited(MouseEvent e) {
                hover = null;
                hoverScreen = null;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /** Called once the connection exists. */
    public void attach(PrintWriter writer) { this.out = writer; }

    public void setTool(Tool t) {
        if (t == null) return;
        if (localStroke != null || shapeStart != null) endAction();
        tool = t;
        updateCursor();
        repaint();
    }

    public Tool getTool() { return tool; }

    public void setBrush(Brush b) {
        if (b == null) return;
        brush = b;
        if (tool == Tool.ERASER || tool == Tool.FILL) tool = Tool.BRUSH;
        updateCursor();
        repaint();
    }

    public Brush getBrush() { return brush; }

    public void setColor(Color c) {
        if (c == null) return;
        color = c;
        if (tool == Tool.ERASER) tool = Tool.BRUSH;
        repaint();
    }

    public Color getCurrentColor() { return color; }

    public void setStrokeSize(int s) {
        size = Math.max(1, Math.min(60, s));
        repaint();
    }

    public int getStrokeSize() { return size; }

    /** A copy of the current picture (for "save as PNG"). */
    public BufferedImage snapshot() {
        BufferedImage copy = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, CANVAS_W, CANVAS_H);
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
        return copy;
    }

    /** Only the drawer may draw. */
    public void setCanDraw(boolean value) {
        canDraw = value;
        if (!value && (localStroke != null || shapeStart != null)) endAction();
        updateCursor();
        repaint();
    }

    public boolean canDraw() { return canDraw; }

    /** Drawer pressed UNDO. */
    public void undo() {
        if (!canDraw) return;
        if (popLast() && out != null) out.println("UNDO");
    }

    /** Drawer pressed CLEAR. */
    public void clearCanvas() {
        if (!canDraw) return;
        wipeAll();
        if (out != null) out.println("CANVAS_CLEAR");
    }

    /** Local-only wipe (new round, server CLEAR). */
    public void clearDrawing() {
        SwingUtilities.invokeLater(this::wipeAll);
    }

    // ---- effects ----

    public void startEffect(PowerUp p, int seconds) {
        if (p == null || !p.isTimedEffect()) return;
        effects.put(p, System.currentTimeMillis() + seconds * 1000L);
        if (!effectTimer.isRunning()) effectTimer.start();
        repaint();
    }

    public void endEffect(PowerUp p) {
        effects.remove(p);
        if (effects.isEmpty()) effectTimer.stop();
        repaint();
    }

    public void clearEffects() {
        effects.clear();
        effectTimer.stop();
        repaint();
    }

    public boolean isEffectActive(PowerUp p) { return effects.containsKey(p); }

    // =========================================================================
    // LOCAL ACTIONS
    // =========================================================================

    private void beginAction(Point p) {
        switch (tool) {
            case FILL -> {
                FillOp op = new FillOp(p, color);
                commit(op);
                if (out != null) out.println("FILL " + p.x + " " + p.y + " " + color.getRed() + " " + color.getGreen() + " " + color.getBlue());
                repaint();
            }
            case BRUSH, ERASER -> {
                localStroke = new StrokeOp(strokeBrush(), strokeColor(), size);
                addLocalPoint(p);
            }
            default -> {
                shapeStart = p;
                shapeEnd = p;
            }
        }
    }

    private void continueAction(Point p) {
        if (tool == Tool.BRUSH || tool == Tool.ERASER) {
            if (localStroke == null) return;
            Point last = localStroke.pts.get(localStroke.pts.size() - 1);
            if (last.distanceSq(p) < 1.0) return;
            addLocalPoint(p);
        } else if (shapeStart != null) {
            shapeEnd = p;
        }
    }

    private void endAction() {
        if (localStroke != null) {
            commitFinished(localStroke);
            localStroke = null;
            if (out != null) out.println("UP");
        } else if (shapeStart != null) {
            List<Point> pts = shapePoints(tool, shapeStart, shapeEnd);
            shapeStart = null;
            shapeEnd = null;
            if (pts.size() >= 2) {
                StrokeOp s = new StrokeOp(brush, color, size);
                for (Point p : pts) {
                    s.add(cg, p);
                    sendPoint(p, s.color, s.size, s.brush);
                }
                commitFinished(s);
                if (out != null) out.println("UP");
            }
        }
        repaint();
    }

    private Brush strokeBrush() { return tool == Tool.ERASER ? Brush.PEN : brush; }
    private Color strokeColor() { return tool == Tool.ERASER ? Color.WHITE : color; }

    private void addLocalPoint(Point p) {
        localStroke.add(cg, p);
        sendPoint(p, localStroke.color, localStroke.size, localStroke.brush);
        repaint();
    }

    private void sendPoint(Point p, Color c, int s, Brush b) {
        if (out == null) return;
        out.println("DRAW " + p.x + " " + p.y + " " + c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " " + s + " " + b.id());
    }

    private static List<Point> shapePoints(Tool t, Point a, Point b) {
        List<Point> pts = new ArrayList<>();
        if (a.distance(b) < 3) return pts;
        switch (t) {
            case LINE -> segment(pts, a, b);
            case RECT -> {
                Point p1 = new Point(a.x, a.y), p2 = new Point(b.x, a.y), p3 = new Point(b.x, b.y), p4 = new Point(a.x, b.y);
                segment(pts, p1, p2);
                segment(pts, p2, p3);
                segment(pts, p3, p4);
                segment(pts, p4, p1);
            }
            case ELLIPSE -> {
                double cx = (a.x + b.x) / 2.0, cy = (a.y + b.y) / 2.0;
                double rx = Math.abs(b.x - a.x) / 2.0, ry = Math.abs(b.y - a.y) / 2.0;
                int n = (int) Math.max(24, Math.min(120, (rx + ry) / 2));
                for (int i = 0; i <= n; i++) {
                    double ang = Math.PI * 2 * i / n;
                    addUnique(pts, new Point((int) Math.round(cx + Math.cos(ang) * rx), (int) Math.round(cy + Math.sin(ang) * ry)));
                }
            }
            default -> { }
        }
        return pts;
    }

    private static void segment(List<Point> pts, Point a, Point b) {
        int n = Math.max(1, (int) Math.ceil(a.distance(b) / 6.0));
        for (int i = 0; i <= n; i++) {
            addUnique(pts, new Point((int) Math.round(a.x + (b.x - a.x) * (i / (double) n)),
                    (int) Math.round(a.y + (b.y - a.y) * (i / (double) n))));
        }
    }

    private static void addUnique(List<Point> pts, Point p) {
        if (pts.isEmpty() || !pts.get(pts.size() - 1).equals(p)) pts.add(p);
    }

    // =========================================================================
    // REMOTE DRAWING
    // =========================================================================

    public void receiveDrawing(String message) {
        if (message == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                handleRemote(message.trim());
            } catch (RuntimeException ex) {
                System.out.println("Invalid drawing command: " + message);
            }
        });
    }

    private void handleRemote(String m) {
        if (m.equals("UP")) {
            if (remoteStroke != null) commitFinished(remoteStroke);
            remoteStroke = null;
            repaint();
            return;
        }
        if (m.equals("UNDO")) {
            popLast();
            return;
        }
        if (m.equals("CANVAS_CLEAR")) {
            wipeAll();
            return;
        }
        String[] t = m.split("\\s+");
        if (m.startsWith("FILL ") && t.length >= 6) {
            if (remoteStroke != null) { commitFinished(remoteStroke); remoteStroke = null; }
            commit(new FillOp(clamp(num(t[1]), num(t[2])), rgb(t[3], t[4], t[5])));
            repaint();
            return;
        }
        if (m.startsWith("DRAW ") && t.length >= 3) {
            Point p = clamp(num(t[1]), num(t[2]));
            Color c = t.length >= 6 ? rgb(t[3], t[4], t[5]) : Color.BLACK;
            int s = t.length >= 7 ? Math.max(1, Math.min(60, num(t[6]))) : 5;
            Brush b = t.length >= 8 ? Brush.byId(num(t[7])) : Brush.PEN;
            if (remoteStroke != null && (remoteStroke.brush != b || remoteStroke.size != s || !remoteStroke.color.equals(c))) {
                commitFinished(remoteStroke);
                remoteStroke = null;
            }
            if (remoteStroke == null) remoteStroke = new StrokeOp(b, c, s);
            remoteStroke.add(cg, p);
            repaint();
        }
    }

    private static int num(String s) { return Integer.parseInt(s); }

    private static Color rgb(String r, String g, String b) {
        return new Color(c255(num(r)), c255(num(g)), c255(num(b)));
    }

    private static int c255(int v) { return Math.max(0, Math.min(255, v)); }

    private static Point clamp(int x, int y) {
        return new Point(Math.max(0, Math.min(CANVAS_W - 1, x)), Math.max(0, Math.min(CANVAS_H - 1, y)));
    }

    // =========================================================================
    // HISTORY
    // =========================================================================

    private void commitFinished(StrokeOp s) {
        if (!s.pts.isEmpty()) addToHistory(s);
    }

    /** Applies a fill (strokes are already painted incrementally) and records it. */
    private void commit(Op op) {
        if (op instanceof FillOp f) floodFill(canvas, f.at.x, f.at.y, f.color);
        addToHistory(op);
    }

    private void addToHistory(Op op) {
        history.add(op);
        while (history.size() > BAKE_AFTER) {
            Op old = history.remove(0);
            Graphics2D bg = base.createGraphics();
            applyOp(base, bg, old);
            bg.dispose();
        }
    }

    private boolean popLast() {
        if (history.isEmpty()) return false;
        history.remove(history.size() - 1);
        replay();
        return true;
    }

    private void replay() {
        cg.setComposite(AlphaComposite.Src);
        cg.drawImage(base, 0, 0, null);
        cg.setComposite(AlphaComposite.SrcOver);
        for (Op op : history) applyOp(canvas, cg, op);
        repaint();
    }

    private static void applyOp(BufferedImage img, Graphics2D g, Op op) {
        if (op instanceof StrokeOp s) {
            Point prev = null;
            double dist = 0;
            for (Point p : s.pts) {
                dist = s.brush.draw(g, prev, p, s.color, s.size, dist);
                prev = p;
            }
        } else if (op instanceof FillOp f) {
            floodFill(img, f.at.x, f.at.y, f.color);
        }
    }

    private void wipeAll() {
        history.clear();
        localStroke = null;
        remoteStroke = null;
        shapeStart = null;
        Graphics2D bg = base.createGraphics();
        wipe(base, bg);
        bg.dispose();
        wipe(canvas, cg);
        repaint();
    }

    private static void wipe(BufferedImage img, Graphics2D g) {
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setComposite(AlphaComposite.SrcOver);
    }

    // =========================================================================
    // FLOOD FILL (tolerance based so anti-aliased edges don't leave big halos)
    // =========================================================================

    private static void floodFill(BufferedImage img, int sx, int sy, Color fill) {
        int w = img.getWidth(), h = img.getHeight();
        if (sx < 0 || sy < 0 || sx >= w || sy >= h) return;
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int target = px[sy * w + sx];
        int fillRgb = 0xFF000000 | fill.getRGB();
        if (close(target, fillRgb, 6)) return;

        boolean[] seen = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(sy * w + sx);
        seen[sy * w + sx] = true;
        final int tol = 70;
        while (!queue.isEmpty()) {
            int idx = queue.poll();
            px[idx] = fillRgb;
            int x = idx % w, y = idx / w;
            if (x > 0) tryPush(px, seen, queue, idx - 1, target, tol);
            if (x < w - 1) tryPush(px, seen, queue, idx + 1, target, tol);
            if (y > 0) tryPush(px, seen, queue, idx - w, target, tol);
            if (y < h - 1) tryPush(px, seen, queue, idx + w, target, tol);
        }
        img.setRGB(0, 0, w, h, px, 0, w);
    }

    private static void tryPush(int[] px, boolean[] seen, ArrayDeque<Integer> q, int i, int target, int tol) {
        if (seen[i]) return;
        seen[i] = true;
        if (close(px[i], target, tol)) q.add(i);
    }

    private static boolean close(int a, int b, int tol) {
        int d = Math.abs(((a >> 16) & 255) - ((b >> 16) & 255))
                + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255))
                + Math.abs((a & 255) - (b & 255));
        return d <= tol;
    }

    // =========================================================================
    // VIEW MAPPING
    // =========================================================================

    private double scale() {
        return Math.max(0.1, Math.min((getWidth() - PAD * 2) / (double) CANVAS_W, (getHeight() - PAD * 2) / (double) CANVAS_H));
    }

    private Rectangle2D viewRect() {
        double s = scale();
        double w = CANVAS_W * s, h = CANVAS_H * s;
        return new Rectangle2D.Double((getWidth() - w) / 2.0, (getHeight() - h) / 2.0, w, h);
    }

    private Point toLogical(Point screen) {
        Rectangle2D v = viewRect();
        double s = scale();
        int x = (int) Math.round((screen.x - v.getX()) / s);
        int y = (int) Math.round((screen.y - v.getY()) / s);
        return clamp(x, y);
    }

    private void updateCursor() {
        if (!canDraw) setCursor(Cursor.getDefaultCursor());
        else if (tool == Tool.FILL) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        else setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    // =========================================================================
    // EFFECT TIMER
    // =========================================================================

    private void tickEffects() {
        long now = System.currentTimeMillis();
        effects.values().removeIf(end -> end <= now);
        if (effects.isEmpty()) effectTimer.stop();
        repaint();
    }

    // =========================================================================
    // PAINT
    // =========================================================================

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // backdrop with a faint dot grid
        g.setColor(Theme.PANEL_LIGHT);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.alpha(Theme.ACCENT, 38));
        for (int x = 10; x < getWidth(); x += 22)
            for (int y = 10; y < getHeight(); y += 22) g.fillRect(x, y, 2, 2);

        Rectangle2D v = viewRect();
        double s = scale();
        RoundRectangle2D card = new RoundRectangle2D.Double(v.getX(), v.getY(), v.getWidth(), v.getHeight(), 16, 16);

        // soft shadow
        for (int i = 6; i >= 1; i--) {
            g.setColor(Theme.alpha(Theme.SHADOW, 13));
            g.fill(new RoundRectangle2D.Double(v.getX() - i, v.getY() - i + 4, v.getWidth() + i * 2, v.getHeight() + i * 2, 18 + i, 18 + i));
        }

        Shape oldClip = g.getClip();
        g.clip(card);
        AffineTransform old = g.getTransform();

        boolean flipped = effects.containsKey(PowerUp.MIRROR);
        if (flipped) g.rotate(Math.PI, v.getCenterX(), v.getCenterY());

        if (effects.containsKey(PowerUp.BLUR)) {
            int bw = Math.max(1, CANVAS_W / 14), bh = Math.max(1, CANVAS_H / 14);
            BufferedImage tiny = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = tiny.createGraphics();
            tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            tg.drawImage(canvas, 0, 0, bw, bh, null);
            tg.dispose();
            g.drawImage(tiny, (int) v.getX(), (int) v.getY(), (int) Math.ceil(v.getWidth()), (int) Math.ceil(v.getHeight()), null);
        } else {
            g.drawImage(canvas, (int) Math.round(v.getX()), (int) Math.round(v.getY()),
                    (int) Math.round(v.getWidth()), (int) Math.round(v.getHeight()), null);
        }

        // shape preview (drawer)
        if (shapeStart != null && shapeEnd != null) {
            g.translate(v.getX(), v.getY());
            g.scale(s, s);
            g.setColor(Theme.alpha(Theme.ACCENT, 230));
            g.setStroke(new BasicStroke((float) (2.0 / s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{(float) (8 / s), (float) (6 / s)}, 0f));
            List<Point> pts = shapePoints(tool, shapeStart, shapeEnd);
            Path2D path = new Path2D.Double();
            for (int i = 0; i < pts.size(); i++) {
                if (i == 0) path.moveTo(pts.get(i).x, pts.get(i).y);
                else path.lineTo(pts.get(i).x, pts.get(i).y);
            }
            g.draw(path);
            g.setTransform(old);
            if (flipped) g.rotate(Math.PI, v.getCenterX(), v.getCenterY());
        }
        g.setTransform(old);

        // brush ring (drawer only)
        if (canDraw && hoverScreen != null && !effects.containsKey(PowerUp.FREEZE)) {
            double d = Math.max(6, (tool == Tool.FILL ? 10 : brush == Brush.MARKER ? size * 1.5 : size) * s);
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(0, 0, 0, 150));
            g.draw(new Ellipse2D.Double(hoverScreen.x - d / 2, hoverScreen.y - d / 2, d, d));
            g.setColor(new Color(255, 255, 255, 190));
            g.draw(new Ellipse2D.Double(hoverScreen.x - d / 2 + 1, hoverScreen.y - d / 2 + 1, d - 2, d - 2));
        }

        if (effects.containsKey(PowerUp.SPOTLIGHT)) paintSpotlight(g, v);
        if (effects.containsKey(PowerUp.FREEZE)) paintFreeze(g, v);

        g.setClip(oldClip);

        // card border
        g.setColor(Theme.BORDER);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(card);

        paintEffectBadges(g, v);
        g.dispose();
    }

    private void paintSpotlight(Graphics2D g, Rectangle2D v) {
        double r = Math.max(46, 80 * scale());
        Point2D c = hoverScreen != null ? hoverScreen : new Point2D.Double(v.getCenterX(), v.getCenterY());
        Area dark = new Area(v);
        dark.subtract(new Area(new Ellipse2D.Double(c.getX() - r, c.getY() - r, r * 2, r * 2)));
        g.setColor(new Color(8, 8, 16, 246));
        g.fill(dark);
        // soft rim
        g.setPaint(new RadialGradientPaint(c, (float) (r * 1.25), new float[]{0.72f, 1f},
                new Color[]{new Color(8, 8, 16, 0), new Color(8, 8, 16, 235)}));
        g.fill(new Ellipse2D.Double(c.getX() - r * 1.25, c.getY() - r * 1.25, r * 2.5, r * 2.5));
        g.setColor(Theme.alpha(Theme.YELLOW, 150));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(c.getX() - r, c.getY() - r, r * 2, r * 2));
    }

    private void paintFreeze(Graphics2D g, Rectangle2D v) {
        g.setColor(new Color(150, 215, 255, 70));
        g.fill(v);
        // frost corners
        float rad = (float) Math.max(v.getWidth(), v.getHeight()) * 0.45f;
        Point2D[] corners = {
                new Point2D.Double(v.getMinX(), v.getMinY()), new Point2D.Double(v.getMaxX(), v.getMinY()),
                new Point2D.Double(v.getMinX(), v.getMaxY()), new Point2D.Double(v.getMaxX(), v.getMaxY())};
        for (Point2D c : corners) {
            g.setPaint(new RadialGradientPaint(c, rad, new float[]{0f, 1f},
                    new Color[]{new Color(225, 245, 255, 190), new Color(225, 245, 255, 0)}));
            g.fill(new Rectangle2D.Double(c.getX() - rad, c.getY() - rad, rad * 2, rad * 2));
        }
        // snowflake + message
        double cx = v.getCenterX(), cy = v.getCenterY();
        Icons.paint(g, Icons.FREEZE, cx - 36, cy - 68, 72, new Color(20, 90, 150, 235));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        String text = "FROZEN - you can't guess!";
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(20, 80, 140, 240));
        g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) (cy + 34));
    }

    private void paintEffectBadges(Graphics2D g, Rectangle2D v) {
        if (effects.isEmpty()) return;
        long now = System.currentTimeMillis();
        double x = v.getX() + 14;
        double y = v.getY() + 14;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        for (Map.Entry<PowerUp, Long> e : effects.entrySet()) {
            double left = Math.max(0, (e.getValue() - now) / 1000.0);
            String label = e.getKey().label().toUpperCase(Locale.ROOT) + "  " + String.format(Locale.ROOT, "%.1fs", left);
            int w = fm.stringWidth(label) + 46;
            Color c = Widgets.powerColor(e.getKey());
            g.setColor(Theme.alpha(Color.WHITE, 238));
            g.fill(new RoundRectangle2D.Double(x, y, w, 30, 30, 30));
            g.setColor(c);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, 29, 30, 30));
            Icons.paint(g, e.getKey().name(), x + 7, y + 5, 20, c);
            g.setColor(Theme.TEXT);
            g.drawString(label, (float) (x + 34), (float) (y + 19.5));
            x += w + 8;
        }
    }
}
