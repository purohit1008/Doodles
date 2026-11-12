package ui;

import common.PowerUp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Small reusable, custom-painted building blocks used across the Doodles UI. */
public final class Widgets {

    private Widgets() {}

    // =========================================================================
    // GLOBAL DEFAULTS (tooltips, scroll bars and any stock Swing part that is still used)
    // =========================================================================

    private static boolean installed;

    public static synchronized void installDefaults() {
        if (installed) return;
        installed = true;
        try {
            // the same look on every machine, whatever theme or accent colour the OS uses
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("Panel.background", Theme.PANEL);
        UIManager.put("OptionPane.background", Theme.PANEL);
        UIManager.put("OptionPane.messageForeground", Theme.TEXT);
        UIManager.put("OptionPane.messageFont", Theme.NORMAL_FONT);
        UIManager.put("OptionPane.buttonFont", Theme.BUTTON_FONT);
        UIManager.put("Label.foreground", Theme.TEXT);
        UIManager.put("Button.background", Theme.PANEL_RAISED);
        UIManager.put("Button.foreground", Theme.TEXT);
        UIManager.put("Button.select", Theme.ACCENT);
        UIManager.put("ComboBox.background", Theme.PANEL_LIGHT);
        UIManager.put("ComboBox.foreground", Theme.TEXT);
        UIManager.put("List.background", Theme.PANEL_LIGHT);
        UIManager.put("List.foreground", Theme.TEXT);
        UIManager.put("List.selectionBackground", Theme.ACCENT);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("OptionPane.border", BorderFactory.createEmptyBorder(8, 8, 8, 8));
        UIManager.put("TextArea.background", Theme.PANEL_LIGHT);
        UIManager.put("TextArea.foreground", Theme.TEXT);
        UIManager.put("ToolTip.background", Theme.PANEL_RAISED);
        UIManager.put("ToolTip.foreground", Theme.TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(Theme.BORDER));
        UIManager.put("ToolTip.font", Theme.NORMAL_FONT);
        UIManager.put("ScrollBar.thumb", Theme.PANEL_RAISED);
    }

    // =========================================================================
    // COLOURS
    // =========================================================================

    public static Color powerColor(PowerUp p) {
        return switch (p) {
            case BLUR -> Theme.SKY;
            case MIRROR -> Theme.ORANGE;
            case SPOTLIGHT -> new Color(255, 190, 40);
            case FREEZE -> new Color(70, 200, 245);
            case HINT -> Theme.MINT;
            case DOUBLE -> Theme.PINK;
            case SHIELD -> Theme.ACCENT_HOVER;
        };
    }

    // =========================================================================
    // AVATAR
    // =========================================================================

    public static void avatar(Graphics2D g0, String name, double x, double y, double d) {
        Graphics2D g = Theme.smooth(g0);
        Color c = Theme.avatarColor(name);
        g.setColor(Theme.alpha(Theme.SHADOW, 55));
        g.fill(new Ellipse2D.Double(x, y + 2, d, d));
        g.setPaint(new GradientPaint((float) x, (float) y, c.brighter(), (float) x, (float) (y + d), c));
        g.fill(new Ellipse2D.Double(x, y, d, d));
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 255, 255, 235));
        g.draw(new Ellipse2D.Double(x + 1, y + 1, d - 2, d - 2));
        String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        g.setFont(Theme.font(Font.BOLD, Math.max(10, (int) (d * 0.46))));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(40, 28, 90));
        g.drawString(initial, (float) (x + (d - fm.stringWidth(initial)) / 2.0), (float) (y + (d + fm.getAscent() - fm.getDescent()) / 2.0));
        g.dispose();
    }

    // =========================================================================
    // ROUND PANEL
    // =========================================================================

    public static class RoundPanel extends JPanel {
        private Color fill;
        private Color border;
        private final int arc;

        public RoundPanel(Color fill, int arc) {
            this.fill = fill;
            this.arc = arc;
            setOpaque(false);
        }

        public RoundPanel border(Color c) {
            this.border = c;
            repaint();
            return this;
        }

        public void setFill(Color c) {
            this.fill = c;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, arc, arc);
            g.setColor(fill);
            g.fill(r);
            if (border != null) {
                g.setColor(border);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(r);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // =========================================================================
    // ICON BUTTON
    // =========================================================================

    public static class IconButton extends JButton {
        private final String icon;
        private Color accent = Theme.ACCENT;
        private final int iconSize;

        public IconButton(String icon, String tooltip, int size) {
            this.icon = icon;
            this.iconSize = Math.round(size * 0.58f);
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setToolTipText(tooltip);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public IconButton accent(Color c) {
            this.accent = c;
            return this;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            boolean on = isEnabled();
            boolean sel = isSelected();
            boolean hot = on && getModel().isRollover();
            boolean down = on && getModel().isPressed();
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, down ? 1.5 : 0.5, getWidth() - 1, getHeight() - 2, 14, 14);
            Color bg = sel ? accent : hot ? Theme.PANEL_RAISED : Theme.PANEL_LIGHT;
            if (!on) bg = Theme.alpha(bg, 120);
            g.setColor(Theme.alpha(Theme.SHADOW, on ? 45 : 18));
            g.fill(new RoundRectangle2D.Double(0.5, 2.5, getWidth() - 1, getHeight() - 2, 14, 14));
            g.setColor(bg);
            g.fill(r);
            g.setColor(sel ? Theme.alpha(Color.WHITE, 170) : Theme.alpha(Theme.BORDER, on ? 255 : 120));
            g.setStroke(new BasicStroke(1.3f));
            g.draw(r);
            Color ic = !on ? Theme.alpha(Theme.TEXT_MUTED, 110) : sel ? Color.WHITE : Theme.TEXT;
            double s = iconSize;
            Icons.paint(g, icon, (getWidth() - s) / 2.0, (getHeight() - s) / 2.0 + (down ? 1 : 0), s, ic);
            g.dispose();
        }
    }

    // =========================================================================
    // PILL (PRIMARY) BUTTON
    // =========================================================================

    public static class PillButton extends JButton {
        private final Color base;
        private String iconId;

        public PillButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(Theme.font(Font.BOLD, 14));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(10, 22, 10, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public PillButton icon(String id) {
            this.iconId = id;
            setBorder(new EmptyBorder(10, 22 + 14, 10, 22));
            return this;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            boolean on = isEnabled();
            Color top = getModel().isRollover() && on ? base.brighter() : base;
            Color bot = Theme.mix(top, new Color(0x1E2747), 0.16);
            if (!on) {
                top = Theme.alpha(top, 110);
                bot = Theme.alpha(bot, 110);
            }
            int h = getHeight();
            int press = getModel().isPressed() ? 2 : 0;
            g.setColor(Theme.alpha(Theme.SHADOW, 70));
            g.fill(new RoundRectangle2D.Double(0, 4, getWidth(), h - 4, h, h));
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g.fill(new RoundRectangle2D.Double(0, press, getWidth(), h - 4, h, h));
            g.setColor(Theme.alpha(Color.WHITE, 70));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Double(0.5, 0.5 + press, getWidth() - 1, h - 5, h, h));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int iconW = iconId == null ? 0 : 24;
            int tw = fm.stringWidth(getText());
            int x = (getWidth() - tw - iconW) / 2;
            int ty = (h - 4 - fm.getHeight()) / 2 + fm.getAscent() + press;
            if (iconId != null) Icons.paint(g, iconId, x, (h - 4 - 20) / 2.0 + press, 20, on ? Color.WHITE : Theme.alpha(Color.WHITE, 140));
            g.setColor(Theme.alpha(new Color(0x1E2747), on ? 50 : 20));
            g.drawString(getText(), x + iconW, ty + 1);
            g.setColor(on ? getForeground() : Theme.alpha(getForeground(), 150));
            g.drawString(getText(), x + iconW, ty);
            g.dispose();
        }
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }
}
