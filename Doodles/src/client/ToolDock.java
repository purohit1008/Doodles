package client;

import ui.ColorPickerPanel;
import ui.Icons;
import ui.Theme;
import ui.Widgets;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Everything the drawer controls: brushes, tools, colours, size and undo/clear.
 * Stays in sync with the DrawingPanel and can be driven by keyboard shortcuts.
 */
public class ToolDock extends JPanel {

    private static final Color[] PALETTE = {
            new Color(20, 20, 24), new Color(110, 114, 125), new Color(125, 80, 50), new Color(235, 60, 70),
            new Color(255, 135, 40), new Color(255, 208, 50), new Color(150, 215, 60), new Color(40, 185, 100),
            new Color(30, 190, 185), new Color(40, 160, 240),
            Color.WHITE, new Color(200, 204, 214), new Color(222, 184, 135), new Color(255, 130, 175),
            new Color(240, 60, 150), new Color(130, 80, 230), new Color(70, 80, 210), new Color(30, 50, 120),
            new Color(120, 235, 190), new Color(255, 190, 160)
    };

    private final DrawingPanel panel;
    private final Map<Brush, Widgets.IconButton> brushButtons = new EnumMap<>(Brush.class);
    private final Map<DrawingPanel.Tool, Widgets.IconButton> toolButtons = new EnumMap<>(DrawingPanel.Tool.class);
    private final Widgets.IconButton undo, clear;
    private final JSlider slider = new JSlider(1, 40, 6);
    private final Preview preview = new Preview();
    private final Swatches swatches = new Swatches();
    private boolean enabledForDrawing = true;

    public ToolDock(DrawingPanel panel, Runnable onClear) {
        this.panel = panel;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));

        // ---- row 1: brushes + size ----
        JPanel row1 = row();
        for (Brush b : Brush.values()) {
            Widgets.IconButton btn = new Widgets.IconButton(b.iconId(), b.label() + " - " + b.tip(), 40);
            btn.addActionListener(e -> selectBrush(b));
            brushButtons.put(b, btn);
            row1.add(btn);
        }
        row1.add(Box.createHorizontalStrut(10));
        JLabel sz = Widgets.label("SIZE", Theme.TINY_BOLD, Theme.TEXT_MUTED);
        row1.add(sz);
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setPreferredSize(new Dimension(110, 28));
        slider.addChangeListener(e -> {
            panel.setStrokeSize(slider.getValue());
            preview.repaint();
        });
        row1.add(slider);
        row1.add(preview);

        // ---- row 2: tools + colours ----
        JPanel row2 = row();
        addTool(row2, DrawingPanel.Tool.ERASER, Icons.ERASER, "Eraser (E)");
        addTool(row2, DrawingPanel.Tool.FILL, Icons.FILL, "Fill bucket (F)");
        addTool(row2, DrawingPanel.Tool.LINE, Icons.LINE, "Straight line (L)");
        addTool(row2, DrawingPanel.Tool.RECT, Icons.RECT, "Rectangle (R)");
        addTool(row2, DrawingPanel.Tool.ELLIPSE, Icons.ELLIPSE, "Ellipse (O)");
        row2.add(Box.createHorizontalStrut(6));
        undo = new Widgets.IconButton(Icons.UNDO, "Undo (Ctrl+Z)", 40);
        undo.addActionListener(e -> panel.undo());
        clear = new Widgets.IconButton(Icons.CLEAR, "Clear canvas", 40);
        clear.addActionListener(e -> onClear.run());
        row2.add(undo);
        row2.add(clear);
        row2.add(Box.createHorizontalStrut(12));
        row2.add(swatches);

        add(row1);
        add(Box.createVerticalStrut(6));
        add(row2);
        syncSelection();
    }

    private static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        p.setOpaque(false);
        return p;
    }

    private void addTool(JPanel row, DrawingPanel.Tool t, String icon, String tip) {
        Widgets.IconButton b = new Widgets.IconButton(icon, tip, 40);
        b.addActionListener(e -> selectTool(t));
        toolButtons.put(t, b);
        row.add(b);
    }

    // =========================================================================
    // SELECTION (also used by keyboard shortcuts)
    // =========================================================================

    public void selectBrush(Brush b) {
        panel.setBrush(b);
        panel.setTool(DrawingPanel.Tool.BRUSH);
        syncSelection();
    }

    public void selectTool(DrawingPanel.Tool t) {
        panel.setTool(t);
        syncSelection();
    }

    public void adjustSize(int delta) {
        slider.setValue(Math.max(1, Math.min(40, slider.getValue() + delta)));
    }

    public void undoNow() { panel.undo(); }

    private void syncSelection() {
        DrawingPanel.Tool t = panel.getTool();
        boolean brushLike = t == DrawingPanel.Tool.BRUSH;
        for (Map.Entry<Brush, Widgets.IconButton> e : brushButtons.entrySet())
            e.getValue().setSelected(brushLike && e.getKey() == panel.getBrush());
        for (Map.Entry<DrawingPanel.Tool, Widgets.IconButton> e : toolButtons.entrySet())
            e.getValue().setSelected(e.getKey() == t);
        swatches.repaint();
        preview.repaint();
        repaint();
    }

    /** Only the drawer can use the dock. */
    public void setDrawerMode(boolean on) {
        enabledForDrawing = on;
        for (Component c : brushButtons.values()) c.setEnabled(on);
        for (Component c : toolButtons.values()) c.setEnabled(on);
        undo.setEnabled(on);
        clear.setEnabled(on);
        slider.setEnabled(on);
        swatches.setEnabled(on);
        preview.repaint();
        swatches.repaint();
    }

    // =========================================================================
    // COLOUR SWATCHES
    // =========================================================================

    private final class Swatches extends JComponent {
        private static final int D = 20, G = 4;

        Swatches() {
            setPreferredSize(new Dimension(10 * (D + G) + 8 + 38, 2 * (D + G) + 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (!enabledForDrawing) return;
                    int col = e.getX() / (D + G), r = e.getY() / (D + G);
                    if (col < 10 && r < 2) {
                        panel.setColor(PALETTE[r * 10 + col]);
                        syncSelection();
                    } else if (col >= 10) {
                        Color c = ColorPickerPanel.pick(ToolDock.this, panel.getCurrentColor());
                        if (c != null) {
                            panel.setColor(c);
                            syncSelection();
                        }
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            Color cur = panel.getCurrentColor();
            for (int i = 0; i < PALETTE.length; i++) {
                int col = i % 10, r = i / 10;
                double x = col * (D + G) + 2, y = r * (D + G) + 1;
                Color c = enabledForDrawing ? PALETTE[i] : Theme.alpha(PALETTE[i], 90);
                g.setColor(Theme.alpha(Theme.SHADOW, 55));
                g.fill(new Ellipse2D.Double(x, y + 1.5, D, D));
                g.setColor(c);
                g.fill(new Ellipse2D.Double(x, y, D, D));
                g.setColor(Theme.alpha(Theme.TEXT, 55));
                g.setStroke(new BasicStroke(1f));
                g.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, D - 1, D - 1));
                if (PALETTE[i].equals(cur) && panel.getTool() != DrawingPanel.Tool.ERASER) {
                    g.setColor(Theme.ACCENT_HOVER);
                    g.setStroke(new BasicStroke(2.4f));
                    g.draw(new Ellipse2D.Double(x - 2.5, y - 2.5, D + 5, D + 5));
                }
            }
            // custom colour button: rainbow disc
            double x = 10 * (D + G) + 8, y = (getHeight() - 32) / 2.0;
            for (int a = 0; a < 360; a += 6) {
                g.setColor(Color.getHSBColor(a / 360f, 0.8f, enabledForDrawing ? 1f : 0.5f));
                g.fill(new java.awt.geom.Arc2D.Double(x, y, 32, 32, a, 7, java.awt.geom.Arc2D.PIE));
            }
            g.setColor(Theme.PANEL);
            g.fill(new Ellipse2D.Double(x + 8, y + 8, 16, 16));
            g.setColor(cur);
            g.fill(new Ellipse2D.Double(x + 10, y + 10, 12, 12));
            g.dispose();
        }
    }

    // =========================================================================
    // LIVE BRUSH PREVIEW
    // =========================================================================

    private final class Preview extends JComponent {
        Preview() {
            setPreferredSize(new Dimension(130, 40));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.alpha(Color.WHITE, 245));
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = img.createGraphics();
            Brush b = panel.getBrush();
            Color c = panel.getTool() == DrawingPanel.Tool.ERASER ? new Color(190, 200, 220) : panel.getCurrentColor();
            int s = Math.min(slider.getValue(), 22);
            Point prev = null;
            double dist = 0;
            for (int x = 14; x <= w - 14; x += 3) {
                double t = (x - 14) / (double) (w - 28);
                Point p = new Point(x, (int) Math.round(h / 2.0 + Math.sin(t * Math.PI * 2) * (h * 0.22)));
                dist = b.draw(ig, prev, p, c, s, dist);
                prev = p;
            }
            ig.dispose();
            g.setClip(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
            g.drawImage(img, 0, 0, null);
            g.setClip(null);
            g.setColor(enabledForDrawing ? Theme.alpha(Theme.BORDER, 255) : Theme.alpha(Theme.BORDER, 120));
            g.setStroke(new BasicStroke(1.3f));
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 14, 14));
            if (!enabledForDrawing) {
                g.setColor(new Color(240, 246, 255, 170));
                g.fill(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
            }
            g.dispose();
        }
    }
}
