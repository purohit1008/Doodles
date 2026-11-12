package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** Colour picker in the game's own style (replaces the stock Swing colour chooser). */
public final class ColorPickerPanel extends JComponent {

    private static final int SQ_W = 260, SQ_H = 190, BAR_W = 28, GAP = 16, PAD = 6;

    private float hue, sat, bri;
    private BufferedImage square;
    private float squareHue = -1;

    public ColorPickerPanel(Color initial) {
        float[] hsb = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        setPreferredSize(new Dimension(PAD * 2 + SQ_W + GAP + BAR_W, PAD * 2 + SQ_H + 74));
        MouseAdapter m = new MouseAdapter() {
            private int mode;   // 1 = square, 2 = hue bar

            @Override public void mousePressed(MouseEvent e) {
                mode = e.getX() < PAD + SQ_W + GAP / 2 ? 1 : 2;
                apply(e);
            }

            @Override public void mouseDragged(MouseEvent e) { apply(e); }

            private void apply(MouseEvent e) {
                if (mode == 1) {
                    sat = clamp((e.getX() - PAD) / (float) SQ_W);
                    bri = 1 - clamp((e.getY() - PAD) / (float) SQ_H);
                } else {
                    hue = clamp((e.getY() - PAD) / (float) SQ_H);
                    if (hue >= 1f) hue = 0.999f;
                }
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    public Color getColor() { return Color.getHSBColor(hue, sat, bri); }

    private void rebuildSquare() {
        square = new BufferedImage(SQ_W, SQ_H, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < SQ_W; x++) {
            for (int y = 0; y < SQ_H; y++) {
                square.setRGB(x, y, Color.HSBtoRGB(hue, x / (float) (SQ_W - 1), 1 - y / (float) (SQ_H - 1)));
            }
        }
        squareHue = hue;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        if (square == null || squareHue != hue) rebuildSquare();

        Shape clip = new RoundRectangle2D.Double(PAD, PAD, SQ_W, SQ_H, 20, 20);
        Shape old = g.getClip();
        g.clip(clip);
        g.drawImage(square, PAD, PAD, null);
        g.setClip(old);
        g.setColor(Theme.alpha(Theme.TEXT, 40));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(clip);

        // hue bar
        int bx = PAD + SQ_W + GAP;
        Shape bar = new RoundRectangle2D.Double(bx, PAD, BAR_W, SQ_H, BAR_W, BAR_W);
        g.clip(bar);
        for (int y = 0; y < SQ_H; y++) {
            g.setColor(Color.getHSBColor(y / (float) SQ_H, 1f, 1f));
            g.fillRect(bx, PAD + y, BAR_W, 1);
        }
        g.setClip(old);
        g.setColor(Theme.alpha(Theme.TEXT, 40));
        g.draw(bar);

        // markers
        double mx = PAD + sat * SQ_W, my = PAD + (1 - bri) * SQ_H;
        g.setStroke(new BasicStroke(3f));
        g.setColor(Color.WHITE);
        g.draw(new Ellipse2D.Double(mx - 9, my - 9, 18, 18));
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(Theme.alpha(Color.BLACK, 120));
        g.draw(new Ellipse2D.Double(mx - 10.5, my - 10.5, 21, 21));
        double hy = PAD + hue * SQ_H;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(bx - 4, hy - 5, BAR_W + 8, 10, 10, 10));
        g.setColor(Theme.alpha(Theme.TEXT, 120));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(bx - 4, hy - 5, BAR_W + 8, 10, 10, 10));

        // preview + hex
        Color c = getColor();
        int py = PAD + SQ_H + 16;
        g.setColor(Theme.alpha(Theme.SHADOW, 50));
        g.fill(new Ellipse2D.Double(PAD, py + 3, 48, 48));
        g.setColor(c);
        g.fill(new Ellipse2D.Double(PAD, py, 48, 48));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(PAD + 1.5, py + 1.5, 45, 45));
        g.setFont(Theme.font(Font.BOLD, 11));
        g.setColor(Theme.TEXT_MUTED);
        g.drawString("YOUR COLOUR", PAD + 62, py + 18);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        g.setColor(Theme.TEXT);
        g.drawString(String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()), PAD + 62, py + 42);
        g.dispose();
    }

    /** Shows the picker as a themed dialog. Returns the chosen colour or null if cancelled. */
    public static Color pick(Component parent, Color initial) {
        ColorPickerPanel picker = new ColorPickerPanel(initial);
        JDialog[] dd = new JDialog[1];
        Color[] result = {null};

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 26, 24, 26));
        picker.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(picker);
        body.add(Box.createVerticalStrut(16));

        Widgets.PillButton cancel = Dialogs.button("Cancel", new Color(0x9AA6C6), 120);
        Widgets.PillButton ok = Dialogs.button("Use this colour", Theme.ACCENT, 170);
        cancel.addActionListener(e -> dd[0].dispose());
        ok.addActionListener(e -> { result[0] = picker.getColor(); dd[0].dispose(); });
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        row.add(cancel);
        row.add(ok);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(row);

        dd[0] = Dialogs.framed(parent, "Pick a colour", body, true, Dialogs.Style.WHITE);
        dd[0].getRootPane().setDefaultButton(ok);
        dd[0].setVisible(true);
        return result[0];
    }
}
