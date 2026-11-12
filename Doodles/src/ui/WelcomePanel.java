package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.util.Random;

/** The start screen: animated logo, live avatar preview, nickname box and the three ways to play. */
public class WelcomePanel extends JPanel {

    private static final int MAX_NAME = 20;
    private static final String[] ADJ = {"Happy", "Sleepy", "Brave", "Sneaky", "Fluffy", "Zesty", "Cosmic", "Jolly", "Lucky",
            "Wobbly", "Speedy", "Mighty", "Cheeky", "Dizzy", "Sparkly", "Giggly", "Silly", "Chill"};
    private static final String[] ANIMAL = {"Panda", "Otter", "Falcon", "Llama", "Koala", "Tiger", "Narwhal", "Gecko",
            "Octopus", "Penguin", "Dragon", "Moose", "Fox", "Owl", "Hippo", "Duck", "Yeti", "Robot"};

    private final NameField nameField = new NameField();
    private final Avatar avatar = new Avatar();
    private final Random rnd = new Random();
    private final long t0 = System.currentTimeMillis();
    private long frozenTime = -1;          // for deterministic screenshots
    private final javax.swing.Timer anim = new javax.swing.Timer(40, e -> repaint());

    public WelcomePanel(Runnable onQuick, Runnable onPublic, Runnable onPrivate, Runnable onJoin) {
        setOpaque(true);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(16, 30, 16, 30));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        Logo logo = new Logo();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(logo);
        column.add(Box.createVerticalStrut(6));

        Card card = new Card();
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 34, 24, 34));
        card.setPreferredSize(new Dimension(560, 500));
        card.setMaximumSize(new Dimension(560, 500));

        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(avatar);
        card.add(Box.createVerticalStrut(14));

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        labelRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        labelRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelRow.add(Widgets.label("YOUR NICKNAME", Theme.TINY_BOLD, Theme.TEXT_MUTED), BorderLayout.WEST);
        card.add(labelRow);
        card.add(Box.createVerticalStrut(6));

        JPanel nameRow = new JPanel(new BorderLayout(10, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        nameRow.add(nameField, BorderLayout.CENTER);
        DiceButton dice = new DiceButton(() -> setNickname(randomName()));
        nameRow.add(dice, BorderLayout.EAST);
        card.add(nameRow);
        card.add(Box.createVerticalStrut(22));

        Widgets.PillButton quick = new Widgets.PillButton("QUICK PLAY", Theme.SUCCESS).icon(Icons.PLAY);
        quick.setFont(Theme.font(Font.BOLD, 20));
        quick.setAlignmentX(Component.CENTER_ALIGNMENT);
        quick.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        quick.addActionListener(e -> onQuick.run());
        card.add(quick);
        card.add(Box.createVerticalStrut(12));

        JPanel two = new JPanel(new GridLayout(1, 2, 12, 0));
        two.setOpaque(false);
        two.setAlignmentX(Component.CENTER_ALIGNMENT);
        two.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        Widgets.PillButton pub = new Widgets.PillButton("CREATE PUBLIC", Theme.ACCENT).icon(Icons.USERS);
        pub.addActionListener(e -> onPublic.run());
        Widgets.PillButton priv = new Widgets.PillButton("CREATE PRIVATE", Theme.ACCENT_PINK).icon(Icons.SHIELD);
        priv.addActionListener(e -> onPrivate.run());
        two.add(pub);
        two.add(priv);
        card.add(two);
        card.add(Box.createVerticalStrut(12));

        Widgets.PillButton join = new Widgets.PillButton("JOIN A ROOM", new Color(0x8E9BC4)).icon(Icons.TARGET);
        join.setAlignmentX(Component.CENTER_ALIGNMENT);
        join.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        join.addActionListener(e -> onJoin.run());
        card.add(join);
        card.add(Box.createVerticalGlue());

        JLabel tip = Widgets.label("The host picks rounds & drawing time in the lobby", Theme.font(Font.PLAIN, 12), Theme.TEXT_MUTED);
        tip.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(tip);

        column.add(card);
        add(column);

        nameField.addActionListener(e -> onQuick.run());
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { avatar.setPlayer(nameField.getText()); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { avatar.setPlayer(nameField.getText()); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
        setNickname(randomName());
    }

    @Override public void addNotify() {
        super.addNotify();
        anim.start();
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
    }

    @Override public void removeNotify() {
        anim.stop();
        super.removeNotify();
    }

    public String getNickname() { return nameField.getText().trim(); }

    public void focusName() { nameField.requestFocusInWindow(); }

    /** Fixes the animation clock (used for screenshots). */
    public void freezeAnimation(long millis) { frozenTime = millis; }

    public void setNickname(String s) {
        nameField.setText(s);
        nameField.setCaretPosition(s.length());
    }

    private String randomName() {
        String n = ADJ[rnd.nextInt(ADJ.length)] + ANIMAL[rnd.nextInt(ANIMAL.length)];
        return n.length() <= MAX_NAME ? n : n.substring(0, MAX_NAME);
    }

    private double time() {
        return (frozenTime >= 0 ? frozenTime : System.currentTimeMillis() - t0) / 1000.0;
    }

    // =========================================================================
    // BACKGROUND
    // =========================================================================

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.smooth(g0);
        int w = getWidth(), h = getHeight();
        Theme.paintBackdrop(g, w, h);

        double t = time();
        Random r = new Random(11);
        Color[] pal = {Theme.SKY, Theme.PINK, Theme.YELLOW, Theme.MINT, new Color(0xB59CFF), Theme.ORANGE};
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 26; i++) {
            double bx = r.nextDouble() * w, by = r.nextDouble() * h;
            double x = bx + Math.sin(t * 0.7 + i) * 8, y = by + Math.cos(t * 0.6 + i * 1.3) * 8;
            if (x > w * 0.14 && x < w * 0.86 && y < 218) continue;
            Color c = Theme.alpha(pal[i % pal.length], 95);
            g.setColor(c);
            double s = 12 + r.nextInt(16);
            switch (i % 5) {
                case 0 -> g.draw(new Ellipse2D.Double(x, y, s, s));
                case 1 -> {
                    Path2D sq = new Path2D.Double();
                    sq.moveTo(x, y);
                    sq.curveTo(x + s * 0.4, y - s * 0.8, x + s * 0.8, y + s * 0.8, x + s * 1.4, y);
                    sq.curveTo(x + s * 1.8, y - s * 0.8, x + s * 2.2, y + s * 0.8, x + s * 2.8, y);
                    g.draw(sq);
                }
                case 2 -> {
                    g.draw(new Line2D.Double(x - s / 2, y, x + s / 2, y));
                    g.draw(new Line2D.Double(x, y - s / 2, x, y + s / 2));
                }
                case 3 -> {
                    Path2D st = new Path2D.Double();
                    for (int k = 0; k < 10; k++) {
                        double rad = k % 2 == 0 ? s * 0.8 : s * 0.35, a = -Math.PI / 2 + k * Math.PI / 5;
                        double px = x + Math.cos(a) * rad, py = y + Math.sin(a) * rad;
                        if (k == 0) st.moveTo(px, py); else st.lineTo(px, py);
                    }
                    st.closePath();
                    g.draw(st);
                }
                default -> g.fill(new Ellipse2D.Double(x, y, s * 0.45, s * 0.45));
            }
        }
        g.dispose();
    }

    // =========================================================================
    // COMPONENTS
    // =========================================================================

    /** Big bouncing logo. */
    private final class Logo extends JComponent {
        Logo() { setPreferredSize(new Dimension(620, 168)); setMaximumSize(new Dimension(620, 168)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            String word = "DOODLES";
            Color[] cols = {Theme.SKY, Theme.PINK, Theme.YELLOW, Theme.MINT, Theme.ORANGE, new Color(0xB59CFF), Theme.SKY};
            Font f = Theme.font(Font.BOLD, 84);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int spacing = 4;
            int total = 0;
            for (char c : word.toCharArray()) total += fm.charWidth(c) + spacing;
            total -= spacing;
            double x = (getWidth() - total) / 2.0;
            double base = 98;
            double t = time();
            for (int i = 0; i < word.length(); i++) {
                String ch = String.valueOf(word.charAt(i));
                double bob = Math.sin(t * 2.2 + i * 0.8) * 4;
                double rot = Math.toRadians(((i % 2 == 0) ? -4 : 4) + Math.sin(t * 1.5 + i) * 1.5);
                TextLayout tl = new TextLayout(ch, f, g.getFontRenderContext());
                Shape outline = tl.getOutline(null);
                double cw = fm.charWidth(word.charAt(i));
                Graphics2D lg = (Graphics2D) g.create();
                lg.translate(x + cw / 2, base + bob);
                lg.rotate(rot);
                lg.translate(-cw / 2, 0);
                lg.setColor(Theme.alpha(Theme.SHADOW, 60));
                AffineTransform sh = AffineTransform.getTranslateInstance(0, 7);
                lg.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                lg.draw(sh.createTransformedShape(outline));
                lg.fill(sh.createTransformedShape(outline));
                lg.setColor(Color.WHITE);
                lg.draw(outline);
                lg.setPaint(new GradientPaint(0, -62, Theme.mix(cols[i], Color.WHITE, 0.25), 0, 0, cols[i]));
                lg.fill(outline);
                lg.dispose();
                x += cw + spacing;
            }
            // squiggle underline + pencil
            g.setColor(Theme.alpha(Theme.PINK, 200));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D sq = new Path2D.Double();
            double sx = (getWidth() - total) / 2.0 + 20, sy = 128;
            sq.moveTo(sx, sy);
            for (int k = 0; k < 14; k++) sq.quadTo(sx + k * (total - 40) / 14.0 + (total - 40) / 28.0, sy + (k % 2 == 0 ? -9 : 9), sx + (k + 1) * (total - 40) / 14.0, sy);
            g.draw(sq);
            g.setFont(Theme.font(Font.BOLD, 15));
            fm = g.getFontMetrics();
            String tag = "Draw it. Guess it. Win it.";
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(tag, (getWidth() - fm.stringWidth(tag)) / 2, 158);
            g.dispose();
        }
    }

    /** White card with a soft shadow. */
    private static final class Card extends JPanel {
        Card() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            int w = getWidth(), h = getHeight();
            for (int i = 8; i >= 1; i--) {
                g.setColor(Theme.alpha(Theme.SHADOW, 7));
                g.fill(new RoundRectangle2D.Double(4 - i, 8 - i + 2, w - 8 + i * 2, h - 12 + i * 2, 40 + i, 40 + i));
            }
            RoundRectangle2D r = new RoundRectangle2D.Double(2, 2, w - 4, h - 12, 38, 38);
            g.setColor(Color.WHITE);
            g.fill(r);
            g.setPaint(new GradientPaint(0, 0, Theme.SKY, w, 0, Theme.PINK));
            g.setStroke(new BasicStroke(3f));
            g.draw(r);
            g.dispose();
        }
    }

    /** The live avatar: exactly what other players will see next to the nickname. */
    private final class Avatar extends JComponent {
        private String name = "";

        Avatar() {
            setPreferredSize(new Dimension(140, 120));
            setMaximumSize(new Dimension(140, 120));
        }

        void setPlayer(String n) {
            name = n == null ? "" : n.trim();
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            double t = time();
            double d = 92;
            double x = (getWidth() - d) / 2.0, y = 12 + Math.sin(t * 2) * 2;
            g.setColor(Theme.alpha(Color.WHITE, 220));
            g.fill(new Ellipse2D.Double(x - 8, y - 8, d + 16, d + 16));
            Color c = Theme.avatarColor(name.isEmpty() ? "?" : name);
            g.setColor(Theme.alpha(c, 70));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(x - 8, y - 8, d + 16, d + 16));
            Widgets.avatar(g, name.isEmpty() ? "?" : name, x, y, d);
            g.setFont(Theme.font(Font.BOLD, 10));
            String s = "THAT'S YOU!";
            FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(s) + 18;
            g.setColor(Theme.ACCENT_PINK);
            g.fill(new RoundRectangle2D.Double((getWidth() - pw) / 2.0, getHeight() - 20, pw, 18, 18, 18));
            g.setColor(Color.WHITE);
            g.drawString(s, (getWidth() - pw) / 2 + 9, getHeight() - 7);
            g.dispose();
        }
    }

    /** Rounded nickname input with a character counter. */
    private static final class NameField extends JTextField {
        NameField() {
            setOpaque(false);
            setFont(Theme.font(Font.BOLD, 20));
            setForeground(Theme.TEXT);
            setCaretColor(Theme.ACCENT);
            setBorder(new EmptyBorder(12, 20, 12, 62));
            setPreferredSize(new Dimension(300, 54));
            ((PlainDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override public void insertString(FilterBypass fb, int off, String str, AttributeSet a) throws BadLocationException {
                    replace(fb, off, 0, str, a);
                }

                @Override public void replace(FilterBypass fb, int off, int len, String str, AttributeSet a) throws BadLocationException {
                    if (str == null) { super.replace(fb, off, len, str, a); return; }
                    String clean = str.replaceAll("[|:\\p{Cntrl}]", "");
                    int room = MAX_NAME - (fb.getDocument().getLength() - len);
                    if (room <= 0) return;
                    if (clean.length() > room) clean = clean.substring(0, room);
                    super.replace(fb, off, len, clean, a);
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
            Graphics2D h = Theme.smooth(g0);
            h.setFont(Theme.font(Font.BOLD, 11));
            String cnt = getText().length() + "/" + MAX_NAME;
            h.setColor(Theme.TEXT_MUTED);
            h.drawString(cnt, getWidth() - 20 - h.getFontMetrics().stringWidth(cnt), getHeight() / 2 + 4);
            if (getText().isEmpty()) {
                h.setFont(getFont());
                h.setColor(Theme.alpha(Theme.TEXT_MUTED, 200));
                h.drawString("Enter a nickname...", getInsets().left, (getHeight() + h.getFontMetrics().getAscent() - h.getFontMetrics().getDescent()) / 2);
            }
            h.dispose();
        }
    }

    /** Dice button: rolls a random funny nickname. */
    private static final class DiceButton extends JComponent {
        private boolean hot;

        DiceButton(Runnable action) {
            setPreferredSize(new Dimension(54, 54));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Random nickname");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hot = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hot = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { action.run(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.smooth(g0);
            double d = Math.min(getWidth(), getHeight()) - 4;
            double x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
            g.setColor(Theme.alpha(Theme.SHADOW, 40));
            g.fill(new Ellipse2D.Double(x, y + 3, d, d));
            g.setPaint(new GradientPaint(0, (float) y, hot ? Theme.PINK : Theme.mix(Theme.PINK, Color.WHITE, 0.3), 0, (float) (y + d), Theme.ACCENT_PINK));
            g.fill(new Ellipse2D.Double(x, y, d, d));
            // die face
            double s = d * 0.5, dx = x + (d - s) / 2, dy = y + (d - s) / 2;
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(dx, dy, s, s, 9, 9));
            g.setColor(Theme.ACCENT_PINK);
            double p = s * 0.2;
            for (double[] pip : new double[][]{{0.25, 0.25}, {0.75, 0.25}, {0.5, 0.5}, {0.25, 0.75}, {0.75, 0.75}})
                g.fill(new Ellipse2D.Double(dx + s * pip[0] - p / 2, dy + s * pip[1] - p / 2, p, p));
            g.dispose();
        }
    }
}
