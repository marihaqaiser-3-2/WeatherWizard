package weatherwizard;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicTextFieldUI;

public class WeatherWizard extends JFrame {

    // ── Theme gradients [top, bottom] ─────────────────────────────────────────
    // 0=default/home  1=sunny  2=partly cloudy  3=overcast  4=rain  5=snow  6=storm
    static final Color[][] T = {
        { new Color(0x1a1a2e), new Color(0x16213e) },   // 0 home (deep navy)
        { new Color(0xFF8C00), new Color(0xFFD700) },   // 1 sunny
        { new Color(0x2196F3), new Color(0x1565C0) },   // 2 partly cloudy
        { new Color(0x607D8B), new Color(0x37474F) },   // 3 overcast/fog
        { new Color(0x1A237E), new Color(0x0D47A1) },   // 4 rain (deep blue)
        { new Color(0xB0BEC5), new Color(0x78909C) },   // 5 snow (icy gray-blue)
        { new Color(0x212121), new Color(0x1A237E) },   // 6 thunderstorm (near black)
    };

    static final Color W  = Color.WHITE;
    static final Color WD = new Color(255, 255, 255, 170);
    static final Color WF = new Color(255, 255, 255,  90);
    static final Color ER = new Color(255, 80, 80);

    // ── Animation state ───────────────────────────────────────────────────────
    private int   themeIdx  = 0;          // current theme (used for art type)
    private int   spinAngle = 0;
    private boolean loading = false;
    private Color animTop   = T[0][0];
    private Color animBot   = T[0][1];
    private Color tgtTop    = T[0][0];
    private Color tgtBot    = T[0][1];

    // Rain / snow particles
    private final float[] pX  = new float[80];
    private final float[] pY  = new float[80];
    private final float[] pSp = new float[80];   // speed
    private final float[] pA  = new float[80];   // alpha 0-1
    private final Random  rng = new Random(42);

    private Timer mainTimer;  // drives spinner, bg lerp, and particles

    // ── UI refs ───────────────────────────────────────────────────────────────
    private BgPanel    bgPanel;
    private JTextField searchField;
    private JButton    goBtn;
    private JLabel     statusLbl;
    private JPanel     mainContent;
    private JPanel     welcomePanel;

    // Weather labels
    private JLabel iconLbl, tempLbl, condLbl, cityLbl;
    private JLabel humVal, windVal, pressVal, visVal;
    private JPanel hourlyRow, dailyList;
    private JPanel currentSection, metricsSection, hourlySection, dailySection;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(12)).build();

    // ═════════════════════════════════════════════════════════════════════════
    public WeatherWizard() {
        setTitle("WeatherWizard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 800);
        setMinimumSize(new Dimension(380, 600));
        setLocationRelativeTo(null);

        bgPanel = new BgPanel();
        bgPanel.setLayout(new BorderLayout());
        setContentPane(bgPanel);

        initParticles();

        // One master timer: spinner + bg lerp + particles
        mainTimer = new Timer(30, e -> {
            spinAngle = (spinAngle + 6) % 360;
            float step = 0.05f;
            animTop = lerp(animTop, tgtTop, step);
            animBot = lerp(animBot, tgtBot, step);
            tickParticles();
            bgPanel.repaint();
        });
        mainTimer.start();

        buildLayout();
    }

    // ── Particle init ─────────────────────────────────────────────────────────
    void initParticles() {
        for (int i = 0; i < pX.length; i++) resetParticle(i, true);
    }

    void resetParticle(int i, boolean randomY) {
        pX[i] = rng.nextFloat() * 420;
        pY[i] = randomY ? rng.nextFloat() * 800 : -10;
        pSp[i] = 2f + rng.nextFloat() * 4f;
        pA[i]  = 0.3f + rng.nextFloat() * 0.6f;
    }

    void tickParticles() {
        int theme = themeIdx;
        if (theme != 4 && theme != 5 && theme != 6) return;   // only for rain/snow/storm
        for (int i = 0; i < pX.length; i++) {
            pY[i] += pSp[i];
            if (theme == 5) pX[i] += (rng.nextFloat() - 0.5f) * 1.5f;  // snow drift
            if (pY[i] > 820) resetParticle(i, false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Background panel — draws gradient + weather art + particles + spinner
    // ══════════════════════════════════════════════════════════════════════════
    class BgPanel extends JPanel {
        BgPanel() { setOpaque(true); }

        @Override protected void paintComponent(Graphics g) {
            int W = getWidth(), H = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);

            // Gradient background
            g2.setPaint(new GradientPaint(0, 0, animTop, 0, H, animBot));
            g2.fillRect(0, 0, W, H);

            // Theme art — clipped to sky strip (top 28%) so nothing overlaps content
            Shape origClip = g2.getClip();
            int skyH = H * 28 / 100;
            g2.clipRect(0, 0, W, skyH);
            switch (themeIdx) {
                case 0 -> drawHome(g2, W, H);
                case 1 -> drawSunny(g2, W, H);
                case 2 -> drawPartlyCloudy(g2, W, H);
                case 3 -> drawOvercast(g2, W, H);
                case 4 -> drawRainy(g2, W, H);
                case 5 -> drawSnowy(g2, W, H);
                case 6 -> drawStormy(g2, W, H);
            }
            g2.setClip(origClip);
            // Particles (rain/snow) go full height intentionally
            if (themeIdx == 4 || themeIdx == 5 || themeIdx == 6) drawParticles(g2);

            // Loading overlay + spinner
            if (loading) {
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, W, H);
                int cx = W / 2, cy = H / 2, r = 32;
                g2.setColor(new Color(255, 255, 255, 35));
                g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(new Color(255, 255, 255, 230));
                g2.drawArc(cx - r, cy - r, r * 2, r * 2, -spinAngle, 100);
                g2.setFont(new Font("Dialog", Font.BOLD, 14));
                g2.setColor(new Color(255, 255, 255, 200));
                String msg = "Fetching weather...";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + r + 28);
            }
            g2.dispose();
        }
    }

    // ── Home screen art — stars + moon ────────────────────────────────────────
    void drawHome(Graphics2D g, int W, int H) {
        // Stars
        rng.setSeed(7);
        for (int i = 0; i < 60; i++) {
            float x = rng.nextFloat() * W;
            float y = rng.nextFloat() * H * 0.7f;
            float s = 0.5f + rng.nextFloat() * 2.5f;
            float a = 0.3f + rng.nextFloat() * 0.7f;
            g.setColor(new Color(1f, 1f, 1f, a));
            g.fill(new Ellipse2D.Float(x - s/2, y - s/2, s, s));
        }

        // Moon
        int mx = W / 2 + 40, my = H / 4;
        g.setColor(new Color(255, 245, 200, 220));
        g.fillOval(mx - 55, my - 55, 110, 110);
        // Shadow bite
        g.setColor(T[0][0]);
        g.fillOval(mx - 35, my - 60, 105, 105);

        // Moon craters (subtle)
        g.setColor(new Color(220, 210, 170, 60));
        g.fillOval(mx + 10, my - 10, 18, 18);
        g.fillOval(mx - 20, my + 20, 12, 12);

        // Hint text
        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.setColor(new Color(255, 255, 255, 180));
        String h1 = "Search any city";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(h1, (W - fm.stringWidth(h1)) / 2, H * 2 / 3);
        g.setFont(new Font("Dialog", Font.PLAIN, 13));
        g.setColor(new Color(255, 255, 255, 110));
        String h2 = "to see live weather";
        g.drawString(h2, (W - g.getFontMetrics().stringWidth(h2)) / 2, H * 2 / 3 + 24);
    }

    // ── Sunny art — big radiant sun ───────────────────────────────────────────
    void drawSunny(Graphics2D g, int W, int H) {
        int cx = W / 2, cy = H / 5;
        // Outer glow
        RadialGradientPaint rp = new RadialGradientPaint(cx, cy, 140,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(255,255,200,120), new Color(255,200,0,40), new Color(255,160,0,0)});
        g.setPaint(rp);
        g.fillOval(cx - 140, cy - 140, 280, 280);

        // Sun rays
        g.setColor(new Color(255, 235, 100, 100));
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {
            double ang = Math.toRadians(i * 30);
            int x1 = (int)(cx + 68 * Math.cos(ang)),  y1 = (int)(cy + 68 * Math.sin(ang));
            int x2 = (int)(cx + 105 * Math.cos(ang)), y2 = (int)(cy + 105 * Math.sin(ang));
            g.drawLine(x1, y1, x2, y2);
        }
        // Sun disc
        RadialGradientPaint sp = new RadialGradientPaint(cx - 10, cy - 10, 58,
                new float[]{0f, 1f},
                new Color[]{new Color(255,250,200), new Color(255,180,0)});
        g.setPaint(sp);
        g.fillOval(cx - 58, cy - 58, 116, 116);

        // Horizon shimmer
        g.setPaint(new GradientPaint(0, H - 120, new Color(255,200,50,60), 0, H, new Color(255,140,0,0)));
        g.fillRect(0, H - 120, W, 120);
    }

    // ── Partly cloudy art ─────────────────────────────────────────────────────
    void drawPartlyCloudy(Graphics2D g, int W, int H) {
        // Small sun top-right
        int sx = W * 3 / 4, sy = H / 6;
        g.setColor(new Color(255, 230, 100, 160));
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45);
            int x1 = (int)(sx + 38*Math.cos(ang)), y1 = (int)(sy + 38*Math.sin(ang));
            int x2 = (int)(sx + 56*Math.cos(ang)), y2 = (int)(sy + 56*Math.sin(ang));
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x1, y1, x2, y2);
        }
        g.setColor(new Color(255, 220, 80, 200));
        g.fillOval(sx - 32, sy - 32, 64, 64);

        // Clouds
        drawCloud(g, W / 2 - 30, H / 4, 1.2f, 200);
        drawCloud(g, W / 4,       H / 3, 0.8f, 150);
    }

    // ── Overcast ──────────────────────────────────────────────────────────────
    void drawOvercast(Graphics2D g, int W, int H) {
        drawCloud(g, W / 2 - 20, H / 5,      1.4f, 180);
        drawCloud(g, W / 4 - 20, H / 4 + 30, 1.0f, 140);
        drawCloud(g, W * 3/4,    H / 4,      1.1f, 160);
        drawCloud(g, W / 2 + 10, H / 3 + 20, 0.9f, 120);
    }

    // ── Rainy art (clouds only — particles drawn after clip restore) ──────────
    void drawRainy(Graphics2D g, int W, int H) {
        drawCloud(g, W / 2 - 20, H / 8,  1.3f, 180);
        drawCloud(g, W / 4,      H / 6,  0.9f, 140);
    }

    // ── Snowy art (cloud only) ────────────────────────────────────────────────
    void drawSnowy(Graphics2D g, int W, int H) {
        drawCloud(g, W / 2 - 10, H / 8, 1.2f, 200);
    }

    // ── Stormy art (clouds only) ──────────────────────────────────────────────
    void drawStormy(Graphics2D g, int W, int H) {
        drawCloud(g, W / 2 - 30, H / 9,  1.5f, 160);
        drawCloud(g, W / 3 - 20, H / 6,  1.1f, 130);
    }

    // ── Particles drawn full-height AFTER clip is restored ────────────────────
    void drawParticles(Graphics2D g) {
        switch (themeIdx) {
            case 4 -> {   // rain
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < pX.length; i++) {
                    g.setColor(new Color(160, 200, 255, (int)(pA[i] * 180)));
                    g.drawLine((int)pX[i], (int)pY[i], (int)(pX[i]-3), (int)(pY[i]+12));
                }
            }
            case 5 -> {   // snow
                for (int i = 0; i < pX.length; i++) {
                    float sz = 3 + pSp[i];
                    g.setColor(new Color(255, 255, 255, (int)(pA[i] * 200)));
                    drawSnowflake(g, pX[i], pY[i], sz);
                }
            }
            case 6 -> {   // storm — heavy rain + lightning
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < pX.length; i++) {
                    g.setColor(new Color(130, 160, 220, (int)(pA[i] * 160)));
                    g.drawLine((int)pX[i], (int)pY[i], (int)(pX[i]-5), (int)(pY[i]+16));
                }
                // Lightning bolt below the sky strip (always visible)
                drawLightning(g, bgPanel.getWidth() / 2 + 30, bgPanel.getHeight() / 4);
            }
        }
    }

    void drawSnowflake(Graphics2D g, float x, float y, float r) {
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(i * 60);
            g.drawLine((int)x, (int)y,
                    (int)(x + r * Math.cos(ang)), (int)(y + r * Math.sin(ang)));
        }
    }

    void drawLightning(Graphics2D g, int x, int y) {
        int[] px = { x, x-14, x+4, x-12, x+18, x+2, x-8 };
        int[] py = { y, y+28, y+28, y+56, y+56, y+82, y+82 };
        g.setColor(new Color(255, 255, 100, 60));
        g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(px, py, px.length);
        g.setColor(new Color(255, 255, 180, 210));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(px, py, px.length);
    }

    // ── Cloud shape helper — Area union gives clean merged silhouette ─────────
    void drawCloud(Graphics2D g, int cx, int cy, float sc, int alpha) {
        float s = 80 * sc;
        // Five overlapping ellipses — Area.add merges them into one outline
        Area cloud = new Area();
        // Centre-left big bump
        cloud.add(new Area(new Ellipse2D.Float(cx,           cy,           s*0.65f, s*0.65f)));
        // Top-centre (tallest bump)
        cloud.add(new Area(new Ellipse2D.Float(cx + s*0.32f, cy - s*0.22f, s*0.58f, s*0.58f)));
        // Right bump
        cloud.add(new Area(new Ellipse2D.Float(cx + s*0.72f, cy + s*0.04f, s*0.52f, s*0.52f)));
        // Far-left small bump
        cloud.add(new Area(new Ellipse2D.Float(cx - s*0.18f, cy + s*0.12f, s*0.40f, s*0.40f)));
        // Far-right small bump
        cloud.add(new Area(new Ellipse2D.Float(cx + s*1.04f, cy + s*0.15f, s*0.34f, s*0.34f)));
        // Flat base — fills the notches between bottom of circles
        cloud.add(new Area(new Rectangle2D.Float(cx - s*0.18f, cy + s*0.36f, s*1.56f, s*0.30f)));

        g.setColor(new Color(255, 255, 255, alpha));
        g.fill(cloud);

        // Soft inner highlight along top edge
        g.setColor(new Color(255, 255, 255, Math.min(255, alpha + 40)));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(cloud);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UI Layout
    // ═════════════════════════════════════════════════════════════════════════
    void buildLayout() {
        // ── Top: title + search ──────────────────────────────────────────────
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(24, 20, 8, 20));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        titleRow.setOpaque(false);
        titleRow.add(lbl("🌤", 22, Font.PLAIN, W));
        titleRow.add(lbl("WeatherWizard", 22, Font.BOLD,  W));
        top.add(titleRow);
        top.add(Box.createVerticalStrut(16));

        // Pill search bar
        JPanel pill = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255,255,255,42));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),50,50);
                g2.setColor(new Color(255,255,255,80));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,50,50);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setPreferredSize(new Dimension(360, 48));
        pill.setMaximumSize(new Dimension(9999, 48));

        JLabel ico = lbl(" 🔍", 15, Font.PLAIN, WD);
        searchField = new JTextField("Search city...");
        searchField.setUI(new BasicTextFieldUI());
        searchField.setOpaque(false);
        searchField.setBackground(new Color(0,0,0,0));
        searchField.setBorder(new EmptyBorder(0,6,0,6));
        searchField.setFont(new Font("Dialog", Font.PLAIN, 14));
        searchField.setForeground(WD);
        searchField.setCaretColor(W);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search city...")) { searchField.setText(""); searchField.setForeground(W); }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) { searchField.setForeground(WD); searchField.setText("Search city..."); }
            }
        });

        goBtn = new JButton("GO") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(255,255,255,70)
                          : getModel().isRollover()? new Color(255,255,255,55)
                          : new Color(255,255,255,35));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),48,48);
                g2.setFont(new Font("Dialog",Font.BOLD,13));
                g2.setColor(W);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        goBtn.setOpaque(false); goBtn.setContentAreaFilled(false);
        goBtn.setBorderPainted(false); goBtn.setFocusPainted(false);
        goBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        goBtn.setPreferredSize(new Dimension(54, 48));

        pill.add(ico, BorderLayout.WEST);
        pill.add(searchField, BorderLayout.CENTER);
        pill.add(goBtn, BorderLayout.EAST);

        JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pillRow.setOpaque(false); pillRow.add(pill);
        pillRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(pillRow);

        statusLbl = lbl("", 12, Font.PLAIN, ER);
        statusLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(Box.createVerti
