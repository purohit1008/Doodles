package ui;

import java.awt.*;

/**
 * Design tokens for the whole Doodles UI: a soft "candy" palette built from light sky blue and
 * light pink on white cards, with deep navy text for contrast.
 */
public final class Theme {

    private Theme() {}

    // ---- Backdrop gradient ----
    public static final Color BG_TOP    = new Color(0xDCEFFF);   // light sky blue
    public static final Color BG_BOTTOM = new Color(0xFFE4F1);   // light pink
    public static final Color BACKGROUND = new Color(0xEEF6FF);

    // ---- Surfaces ----
    public static final Color PANEL        = new Color(0xFFFFFF);
    public static final Color PANEL_LIGHT  = new Color(0xF2F8FF);
    public static final Color PANEL_RAISED = new Color(0xE2EFFF);

    // ---- Text ----
    public static final Color TEXT       = new Color(0x243252);
    public static final Color TEXT_MUTED = new Color(0x7482A4);

    // ---- Accent ----
    public static final Color ACCENT       = new Color(0x3F9BFF);   // sky blue (primary)
    public static final Color ACCENT_HOVER = new Color(0x6DB4FF);
    public static final Color ACCENT_SOFT  = new Color(63, 155, 255, 44);
    public static final Color ACCENT_PINK  = new Color(0xFF6FAE);   // secondary

    // ---- Lines / status ----
    public static final Color BORDER  = new Color(0xD3E4F8);
    public static final Color SUCCESS = new Color(0x2FC48D);
    public static final Color DANGER  = new Color(0xFF5C7A);

    // ---- Palette (power-ups, medals, highlights) ----
    public static final Color YELLOW = new Color(0xFFC93C);
    public static final Color ORANGE = new Color(0xFF9A4D);
    public static final Color PINK   = new Color(0xFF7EB6);
    public static final Color SKY    = new Color(0x4DA3FF);
    public static final Color MINT   = new Color(0x35D0A0);
    public static final Color GOLD   = new Color(0xFFC83D);
    public static final Color SILVER = new Color(0xB9C4DC);
    public static final Color BRONZE = new Color(0xE6A06C);

    /** Soft blue-grey used for drop shadows on light surfaces. */
    public static final Color SHADOW = new Color(50, 80, 140);

    // ---- Fonts ----
    public static final String FAMILY = pickFamily();
    public static final Font TITLE_FONT    = font(Font.BOLD, 42);
    public static final Font SUBTITLE_FONT = font(Font.PLAIN, 16);
    public static final Font NORMAL_FONT   = font(Font.PLAIN, 14);
    public static final Font BUTTON_FONT   = font(Font.BOLD, 15);
    public static final Font SMALL_BOLD    = font(Font.BOLD, 12);
    public static final Font TINY_BOLD     = font(Font.BOLD, 10);

    private static String pickFamily() {
        try {
            java.util.Set<String> have = new java.util.HashSet<>(java.util.Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String f : new String[]{"Segoe UI", "SF Pro Display", "Nunito", "Poppins", "Inter", "Helvetica Neue", "Arial"}) {
                if (have.contains(f)) return f;
            }
        } catch (Throwable ignored) {
        }
        return "SansSerif";
    }

    public static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    // ---- Helpers ----

    /** Same colour with a different alpha (0-255). */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /** Linear blend: t=0 gives a, t=1 gives b. */
    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** A darker, text-safe version of a bright palette colour (for labels on white cards). */
    public static Color ink(Color c) {
        return mix(c, new Color(0x1E2747), 0.5);
    }

    /** Antialiased copy of the given Graphics (caller disposes it). */
    public static Graphics2D smooth(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    /** Paints the shared sky-blue to pink backdrop with a few soft blobs. */
    public static void paintBackdrop(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, BG_TOP, w, h, BG_BOTTOM));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.08, h * 0.55, w * 0.34, w * 0.34));
        g.setColor(new Color(255, 190, 220, 70));
        g.fill(new java.awt.geom.Ellipse2D.Double(w * 0.74, -h * 0.16, w * 0.4, w * 0.4));
        g.setColor(new Color(150, 205, 255, 70));
        g.fill(new java.awt.geom.Ellipse2D.Double(w * 0.38, h * 0.78, w * 0.3, w * 0.3));
    }

    private static final Color[] AVATARS = {
            new Color(255, 160, 180), new Color(255, 196, 120), new Color(255, 230, 120),
            new Color(140, 232, 180), new Color(120, 208, 255), new Color(180, 160, 255),
            new Color(255, 160, 225), new Color(150, 232, 225)
    };

    /** Stable colour for a player name. */
    public static Color avatarColor(String name) {
        int h = name == null ? 0 : Math.abs(name.hashCode());
        return AVATARS[h % AVATARS.length];
    }
}
