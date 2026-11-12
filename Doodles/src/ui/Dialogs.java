package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.regex.Pattern;

/**
 * Every pop-up window of the game. They are frameless, rounded and painted by us, so they look the
 * same on every machine (no stock Swing dialogs, no Windows accent-coloured title bars).
 */
public final class Dialogs {

    private Dialogs() {}

    public enum Kind { INFO, WARN, ERROR, QUESTION, SUCCESS }

    /** WHITE: small message boxes. PASTEL: big screens (lobby, results, settings). */
    public enum Style { WHITE, PASTEL }

    private static final int M = 14;   // transparent margin that carries the drop shadow

    // =========================================================================
    // GENERIC FRAME
    // =========================================================================

    /** Builds (does not show) a themed dialog around the given content. */
    public static JDialog framed(Component parent, String title, JComponent body, boolean modal, Style style) {
        Window owner = parent == null ? null : (parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent));
        JDialog d = new JDialog(owner);
        d.setUndecorated(true);
        d.setModalityType(modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
        boolean translucent = false;
        // Per-pixel transparent windows flicker on macOS whenever something inside them repaints
        // (every hover), so they are only used elsewhere. Force with -Ddoodles.translucent=true.
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        boolean allowTranslucent = !mac || Boolean.getBoolean("doodles.translucent");
        try {
            if (allowTranslucent && GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                d.setBackground(new Color(0, 0, 0, 0));
                translucent = true;
            }
        } catch (Throwable ignored) {
            translucent = false;
        }
        d.setContentPane(new FramePanel(d, title, body, style, translucent));
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack();
        d.setLocationRelativeTo(parent != null && parent.isShowing() ? parent : null);
        return d;
    }

    private static final class FramePanel extends JPanel {
        private final Style style;
        private final boolean translucent;

        FramePanel(JDialog d, String title, JComponent body, Style style, boolean translucent) {
            this.style = style;
            this.translucent = translucent;
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(M, M, M + 5, M));
            add(new Header(d, title), BorderLayout.NORTH);
            body.setOpaque(false);
            add(body, BorderLayout.CENTER);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            int w = getWidth(), h = getHeight();
            if (translucent) {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, w, h);
                g.setComposite(AlphaComposite.SrcOver);
            } else {
                g.setColor(Theme.BG_TOP);
                g.fillRect(0, 0, w, h);
            }
            RoundRectangle2D card = new RoundRectangle2D.Double(M, M, w - 2 * M, h - 2 * M - 5, 32, 32);
            if (translucent) {
                for (int i = 9; i >= 1; i--) {
                    g.setColor(Theme.alpha(Theme.SHADOW, 6));
                    g.fill(new RoundRectangle2D.Double(M - i, M - i + 6, w - 2 * M + 2 * i, h - 2 * M - 5 + 2 * i, 32 + i * 2, 32 + i * 2));
                }
            }
            Shape oldClip = g.getClip();
            g.clip(card);
            if (style == Style.PASTEL) {
                g.translate(M, M);
                Theme.paintBackdrop(g, w - 2 * M, h - 2 * M - 5);
                g.translate(-M, -M);
            } else {
                g.setColor(Color.WHITE);
                g.fill(card);
            }
            g.setClip(oldClip);
            g.setPaint(new GradientPaint(M, 0, Theme.SKY, w - M, 0, Theme.PINK));
            g.setStroke(new BasicStroke(3f));
            g.draw(new RoundRectangle2D.Double(M + 1.5, M + 1.5, w - 2 * M - 3, h - 2 * M - 8, 30, 30));
            g.dispose();
        }
    }

    /** Title row: little doodle badge, title text and a close button; drag it to move the window. */
    private static final class Header extends JComponent {
        private final JDialog dialog;
        private final String title;
        private boolean closeHot;
        private Point pressScreen, startLocation;

        Header(JDialog dialog, String title) {
            this.dialog = dialog;
            this.title = title == null ? "" : title;
            setPreferredSize(new Dimension(10, 54));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (closeRect().contains(e.getPoint())) { dialog.dispose(); return; }
                    pressScreen = e.getLocationOnScreen();
                    startLocation = dialog.getLocation();
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (pressScreen == null) return;
                    Point now = e.getLocationOnScreen();
                    dialog.setLocation(startLocation.x + now.x - pressScreen.x, startLocation.y + now.y - pressScreen.y);
                }

                @Override public void mouseReleased(MouseEvent e) { pressScreen = null; }

                @Override public void mouseMoved(MouseEvent e) {
                    boolean hot = closeRect().contains(e.getPoint());
                    if (hot != closeHot) {
                        closeHot = hot;
                        setCursor(Cursor.getPredefinedCursor(hot ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override public void mouseExited(MouseEvent e) {
                    closeHot = false;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private Rectangle closeRect() { return new Rectangle(getWidth() - 46, 12, 32, 32); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            g.setPaint(new GradientPaint(16, 12, Theme.SKY, 48, 44, Theme.PINK));
            g.fill(new Ellipse2D.Double(16, 12, 32, 32));
            Icons.paint(g, Icons.PEN, 22, 18, 20, Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, 16));
            g.setColor(Theme.TEXT);
            g.drawString(title, 58, 34);
            Rectangle c = closeRect();
            g.setColor(closeHot ? Theme.alpha(Theme.DANGER, 40) : Theme.PANEL_LIGHT);
            g.fill(new Ellipse2D.Double(c.x, c.y, c.width, c.height));
            g.setColor(closeHot ? Theme.DANGER : Theme.TEXT_MUTED);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cx = c.getCenterX(), cy = c.getCenterY();
            g.drawLine((int) (cx - 5), (int) (cy - 5), (int) (cx + 5), (int) (cy + 5));
            g.drawLine((int) (cx - 5), (int) (cy + 5), (int) (cx + 5), (int) (cy - 5));
            g.dispose();
        }
    }

    // =========================================================================
    // READY-MADE DIALOGS
    // =========================================================================

    public static void message(Component parent, String title, String text, Kind kind) {
        JDialog[] dd = new JDialog[1];
        Widgets.PillButton ok = button("OK", color(kind), 130);
        ok.addActionListener(e -> dd[0].dispose());
        dd[0] = framed(parent, title, messageBody(kind, text, new JComponent[]{ok}), true, Style.WHITE);
        dd[0].getRootPane().setDefaultButton(ok);
        dd[0].setVisible(true);
    }

    public static boolean confirm(Component parent, String title, String text, String yes, String no, Kind kind) {
        JDialog[] dd = new JDialog[1];
        boolean[] result = {false};
        Widgets.PillButton noB = button(no, new Color(0x9AA6C6), 120);
        Widgets.PillButton yesB = button(yes, color(kind), 130);
        noB.addActionListener(e -> dd[0].dispose());
        yesB.addActionListener(e -> { result[0] = true; dd[0].dispose(); });
        dd[0] = framed(parent, title, messageBody(kind, text, new JComponent[]{noB, yesB}), true, Style.WHITE);
        dd[0].getRootPane().setDefaultButton(yesB);
        dd[0].setVisible(true);
        return result[0];
    }

    /** A question with several big answers. Returns the index chosen, or -1 if closed. */
    public static int choose(Component parent, String title, String text, String[] options, Color[] colors) {
        JDialog[] dd = new JDialog[1];
        int[] result = {-1};
        JComponent[] buttons = new JComponent[options.length];
        for (int i = 0; i < options.length; i++) {
            int idx = i;
            Widgets.PillButton b = button(options[i], colors[i % colors.length], 160);
            b.addActionListener(e -> { result[0] = idx; dd[0].dispose(); });
            buttons[i] = b;
        }
        dd[0] = framed(parent, title, messageBody(Kind.QUESTION, text, buttons), true, Style.WHITE);
        dd[0].setVisible(true);
        return result[0];
    }

    /** Asks for a line of text. Returns null if cancelled. */
    public static String input(Component parent, String title, String text, String hint, int minLen, int maxLen,
                               String allowedChars, boolean upper, boolean big) {
        JDialog[] dd = new JDialog[1];
        String[] result = {null};
        RoundField field = new RoundField();
        field.setHint(hint);
        field.setLimits(maxLen, allowedChars, upper);
        field.setBig(big);
        Widgets.PillButton cancel = button("Cancel", new Color(0x9AA6C6), 120);
        Widgets.PillButton ok = button("OK", Theme.ACCENT, 130);
        cancel.addActionListener(e -> dd[0].dispose());
        ok.addActionListener(e -> {
            String t = field.getText().trim();
            if (t.length() >= minLen) { result[0] = t; dd[0].dispose(); }
        });
        field.addActionListener(e -> ok.doClick());
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void sync() { ok.setEnabled(field.getText().trim().length() >= minLen); }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { sync(); }
        });
        ok.setEnabled(false);

        JPanel body = inputBody(text, field, big, new JComponent[]{cancel, ok});

        dd[0] = framed(parent, title, body, true, Style.WHITE);
        dd[0].getRootPane().setDefaultButton(ok);
        dd[0].addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { field.requestFocusInWindow(); }
        });
        dd[0].setVisible(true);
        return result[0];
    }

    // =========================================================================
    // BUILDING BLOCKS
    // =========================================================================

    private static JPanel inputBody(String text, RoundField field, boolean big, JComponent[] buttons) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(6, 30, 26, 30));
        JLabel label = textLabel(text, 340);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(label);
        body.add(Box.createVerticalStrut(16));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(new Dimension(340, big ? 64 : 52));
        field.setPreferredSize(new Dimension(340, big ? 64 : 52));
        body.add(field);
        body.add(Box.createVerticalStrut(20));
        body.add(buttonRow(buttons));
        return body;
    }

    // ---- previews (draw a dialog's look without opening a window; used for screenshots) ----

    public static JComponent framePreview(String title, JComponent body, Style style) {
        return new FramePanel(null, title, body, style, true);
    }

    public static JComponent previewMessage(String title, String text, Kind kind, String... buttons) {
        JComponent[] b = new JComponent[buttons.length];
        for (int i = 0; i < buttons.length; i++) b[i] = button(buttons[i], i == buttons.length - 1 ? color(kind) : new Color(0x9AA6C6), 130);
        return framePreview(title, messageBody(kind, text, b), Style.WHITE);
    }

    public static JComponent previewInput(String title, String text, String hint, String typed, boolean big) {
        RoundField f = new RoundField();
        f.setHint(hint);
        f.setLimits(5, "[A-Za-z0-9]", true);
        f.setBig(big);
        f.setText(typed);
        Widgets.PillButton ok = button("OK", Theme.ACCENT, 130);
        ok.setEnabled(typed.length() >= 5);
        return framePreview(title, inputBody(text, f, big, new JComponent[]{button("Cancel", new Color(0x9AA6C6), 120), ok}), Style.WHITE);
    }

    private static Color color(Kind k) {
        return switch (k) {
            case ERROR -> Theme.DANGER;
            case WARN -> Theme.ORANGE;
            case SUCCESS -> Theme.SUCCESS;
            default -> Theme.ACCENT;
        };
    }

    public static Widgets.PillButton button(String text, Color color, int minWidth) {
        Widgets.PillButton b = new Widgets.PillButton(text, color);
        FontMetrics fm = b.getFontMetrics(b.getFont());
        b.setPreferredSize(new Dimension(Math.max(minWidth, fm.stringWidth(text) + 56), 46));
        return b;
    }

    private static JPanel buttonRow(JComponent[] buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        for (JComponent b : buttons) row.add(b);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    private static JComponent messageBody(Kind kind, String text, JComponent[] buttons) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(8, 30, 26, 30));

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setOpaque(false);
        top.add(new Badge(kind), BorderLayout.WEST);
        top.add(textLabel(text, 300), BorderLayout.CENTER);
        top.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        body.add(top);
        body.add(Box.createVerticalStrut(24));
        body.add(buttonRow(buttons));
        return body;
    }

    private static JLabel textLabel(String text, int width) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        JLabel l = new JLabel("<html><body style='width:" + width + "px; font-family:" + Theme.FAMILY + "'>" + safe + "</body></html>");
        l.setFont(Theme.font(Font.PLAIN, 15));
        l.setForeground(Theme.TEXT);
        return l;
    }

    /** Round badge with a symbol that tells what kind of message this is. */
    private static final class Badge extends JComponent {
        private final Kind kind;

        Badge(Kind kind) {
            this.kind = kind;
            setPreferredSize(new Dimension(62, 62));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            Color c = color(kind);
            double d = 56, x = 3, y = 3;
            g.setColor(Theme.alpha(Theme.SHADOW, 40));
            g.fill(new Ellipse2D.Double(x, y + 3, d, d));
            g.setPaint(new GradientPaint(0, (float) y, Theme.mix(c, Color.WHITE, 0.3), 0, (float) (y + d), c));
            g.fill(new Ellipse2D.Double(x, y, d, d));
            g.setColor(Color.WHITE);
            if (kind == Kind.SUCCESS) {
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(x + 15, y + 29);
                p.lineTo(x + 25, y + 39);
                p.lineTo(x + 42, y + 18);
                g.draw(p);
            } else {
                String s = switch (kind) {
                    case QUESTION -> "?";
                    case INFO -> "i";
                    default -> "!";
                };
                g.setFont(Theme.font(Font.BOLD, 34));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, (float) (x + (d - fm.stringWidth(s)) / 2), (float) (y + (d + fm.getAscent() - fm.getDescent()) / 2));
            }
            g.dispose();
        }
    }

    /** Rounded text box (placeholder, length limit, character filter). */
    public static final class RoundField extends JTextField {
        private String hint = "";
        private boolean big;

        public RoundField() {
            setOpaque(false);
            setForeground(Theme.TEXT);
            setCaretColor(Theme.ACCENT);
            setSelectionColor(Theme.alpha(Theme.ACCENT, 90));
            setSelectedTextColor(Theme.TEXT);
            setFont(Theme.font(Font.BOLD, 18));
            setBorder(new EmptyBorder(10, 20, 10, 20));
        }

        public void setHint(String h) { hint = h == null ? "" : h; }

        public void setBig(boolean b) {
            big = b;
            if (b) {
                setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
                setHorizontalAlignment(JTextField.CENTER);
            }
        }

        public void setLimits(int maxLen, String allowedChars, boolean upper) {
            Pattern allowed = allowedChars == null ? null : Pattern.compile(allowedChars);
            ((PlainDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override public void insertString(FilterBypass fb, int off, String str, AttributeSet a) throws BadLocationException {
                    replace(fb, off, 0, str, a);
                }

                @Override public void replace(FilterBypass fb, int off, int len, String str, AttributeSet a) throws BadLocationException {
                    if (str == null) { super.replace(fb, off, len, str, a); return; }
                    StringBuilder sb = new StringBuilder();
                    for (char ch : str.toCharArray()) {
                        char c = upper ? Character.toUpperCase(ch) : ch;
                        if (Character.isISOControl(c)) continue;
                        if (allowed != null && !allowed.matcher(String.valueOf(c)).matches()) continue;
                        sb.append(c);
                    }
                    int room = maxLen - (fb.getDocument().getLength() - len);
                    if (room <= 0) return;
                    if (sb.length() > room) sb.setLength(room);
                    super.replace(fb, off, len, sb.toString(), a);
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            RoundRectangle2D r = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, getHeight(), getHeight());
            g.setColor(Theme.PANEL_LIGHT);
            g.fill(r);
            g.setColor(isFocusOwner() ? Theme.ACCENT : Theme.BORDER);
            g.setStroke(new BasicStroke(isFocusOwner() ? 2.4f : 1.6f));
            g.draw(r);
            g.dispose();
            super.paintComponent(g0);
            if (getText().isEmpty() && !hint.isEmpty()) {
                Graphics2D h = Theme.smooth(g0);
                h.setFont(getFont());
                h.setColor(Theme.alpha(Theme.TEXT_MUTED, 150));
                FontMetrics fm = h.getFontMetrics();
                int x = big ? (getWidth() - fm.stringWidth(hint)) / 2 : getInsets().left;
                h.drawString(hint, x, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                h.dispose();
            }
        }
    }
}
