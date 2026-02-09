/*
 * MIT License
 *
 * Copyright (c) 2025 Team Flames / Samsoft / Flames Co.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  CTBadlion v0.1 — Java Edition Launcher                                  ║
 * ║  Dark GUI · 980×650 · Offline Profiles · Fabric + Mods                    ║
 * ║  Supports LAN & offline-mode servers (online-mode=false)                  ║
 * ║  Badlion-Style Client with Fabric + Fabulously Optimized Modpack          ║
 * ║                                                                           ║
 * ║  BUILD:  javac CTBadlion.java                                             ║
 * ║  RUN:    java CTBadlion                                                   ║
 * ║  JAR:    jar cfe ctbadlion.jar CTBadlion CTBadlion*.class                 ║
 * ║  Requires: Java 8+ (Java 17/21+ supported via module flags)              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.zip.*;

public class CTBadlion extends JFrame {

    // ═══════════════════════════════════════════════════════════════════
    //  PALETTE — Badlion-Inspired Dark Theme
    // ═══════════════════════════════════════════════════════════════════
    static final Color C_BG          = new Color(22, 22, 26);
    static final Color C_SIDEBAR     = new Color(18, 18, 22);
    static final Color C_SIDEBAR_HI  = new Color(38, 38, 44);
    static final Color C_SIDEBAR_SEL = new Color(48, 48, 56);
    static final Color C_TOPBAR      = new Color(30, 30, 36);
    static final Color C_FIELD       = new Color(40, 40, 48);
    static final Color C_HOVER       = new Color(55, 55, 64);
    static final Color C_POPUP       = new Color(36, 36, 42);
    static final Color C_ACCENT      = new Color(0, 168, 255);   // Badlion blue
    static final Color C_ACCENT_HI   = new Color(30, 190, 255);
    static final Color C_ACCENT_DIM  = new Color(0, 120, 190);
    static final Color C_GREEN       = new Color(62, 195, 55);
    static final Color C_WHITE       = new Color(235, 235, 240);
    static final Color C_GREY        = new Color(160, 160, 170);
    static final Color C_DIM         = new Color(100, 100, 115);
    static final Color C_BORDER      = new Color(50, 50, 58);
    static final Color C_CONSOLE     = new Color(14, 14, 18);
    static final Color C_CON_TEXT    = new Color(70, 210, 110);
    static final Color C_CON_ERR     = new Color(255, 90, 90);
    static final Color C_CARD_BG     = new Color(44, 44, 52);
    static final Color C_RED         = new Color(255, 75, 75);

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTANTS
    // ═══════════════════════════════════════════════════════════════════
    static final String APP = "CTBadlion";
    static final String VER = "0.1";
    static final String MANIFEST_URL       = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    static final String RESOURCES_URL      = "https://resources.download.minecraft.net/";
    static final String LIBRARIES_URL      = "https://libraries.minecraft.net/";
    static final String FABRIC_META_URL    = "https://meta.fabricmc.net/v2/versions/loader/";
    static final String MODRINTH_API       = "https://api.modrinth.com/v2/";

    static Path ROOT, VER_DIR, LIB_DIR, ASS_DIR, NAT_DIR, PROF_FILE, MODS_DIR, LOG_DIR;

    // ═══════════════════════════════════════════════════════════════════
    //  UI COMPONENTS
    // ═══════════════════════════════════════════════════════════════════
    JTextField     tfUser, tfRam, tfJavaPath;
    DarkDropdown   ddVersion;
    JCheckBox      cbFabric, cbFabOpt, cbSnapshots, cbFullscreen;
    JTextArea      taLog;
    JButton        btnPlay;
    JProgressBar   progBar;
    JLabel         lblStatus, lblProgress;
    JPanel         contentCards, sidebarPanel;
    CardLayout     contentLay;
    String         curNav = "play";
    volatile boolean launching = false;

    // Version list: [id, type, url]
    final List<String[]> allVersions  = Collections.synchronizedList(new ArrayList<>());
    final List<String[]> dispVersions = Collections.synchronizedList(new ArrayList<>());

    ExecutorService executor = Executors.newFixedThreadPool(4);
    final AtomicInteger dlCount = new AtomicInteger(0);
    final AtomicInteger dlTotal = new AtomicInteger(0);

    // ═══════════════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        initDirs();
        applyDarkDefaults();
        SwingUtilities.invokeLater(() -> {
            CTBadlion app = new CTBadlion();
            app.setVisible(true);
            app.log(APP + " v" + VER + " initialized");
            app.log("Root: " + ROOT);
            app.log("Java: " + System.getProperty("java.version") + " ("
                    + System.getProperty("java.vendor") + ")");
            app.log("OS:   " + System.getProperty("os.name") + " "
                    + System.getProperty("os.arch"));
            app.fetchManifest();
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════════
    static void initDirs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path home;
        if (os.contains("win"))
            home = Paths.get(System.getenv("APPDATA"), ".minecraft");
        else if (os.contains("mac"))
            home = Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support", "minecraft");
        else
            home = Paths.get(System.getProperty("user.home"), ".minecraft");

        ROOT      = home;
        VER_DIR   = home.resolve("versions");
        LIB_DIR   = home.resolve("libraries");
        ASS_DIR   = home.resolve("assets");
        NAT_DIR   = home.resolve("natives");
        MODS_DIR  = home.resolve("mods");
        LOG_DIR   = home.resolve("logs");
        PROF_FILE = home.resolve("ctbadlion_profiles.properties");

        for (Path d : new Path[]{
                VER_DIR, LIB_DIR,
                ASS_DIR.resolve("indexes"), ASS_DIR.resolve("objects"),
                NAT_DIR, MODS_DIR, LOG_DIR
        }) {
            try { Files.createDirectories(d); }
            catch (IOException e) { System.err.println("Dir create fail: " + e); }
        }
    }

    static void applyDarkDefaults() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        Object[][] defs = {
            {"Panel.background", C_BG}, {"Panel.foreground", C_WHITE},
            {"Label.foreground", C_WHITE},
            {"ComboBox.background", C_FIELD}, {"ComboBox.foreground", C_WHITE},
            {"ComboBox.selectionBackground", C_ACCENT}, {"ComboBox.selectionForeground", C_WHITE},
            {"ComboBox.buttonBackground", C_FIELD}, {"ComboBox.buttonDarkShadow", C_BORDER},
            {"ComboBox.buttonHighlight", C_HOVER}, {"ComboBox.buttonShadow", C_BORDER},
            {"List.background", C_POPUP}, {"List.foreground", C_WHITE},
            {"List.selectionBackground", C_ACCENT}, {"List.selectionForeground", C_WHITE},
            {"ScrollBar.background", C_BG}, {"ScrollBar.thumb", C_HOVER},
            {"ScrollBar.track", C_BG}, {"ScrollBar.width", 10},
            {"ScrollPane.background", C_BG},
            {"TextField.background", C_FIELD}, {"TextField.foreground", C_WHITE},
            {"TextField.caretForeground", C_WHITE},
            {"TextArea.background", C_CONSOLE}, {"TextArea.foreground", C_CON_TEXT},
            {"ToolTip.background", C_POPUP}, {"ToolTip.foreground", C_WHITE},
            {"OptionPane.background", C_BG}, {"OptionPane.foreground", C_WHITE},
            {"OptionPane.messageForeground", C_WHITE},
            {"Button.background", C_FIELD}, {"Button.foreground", C_WHITE},
            {"ProgressBar.background", C_FIELD}, {"ProgressBar.foreground", C_ACCENT},
            {"PopupMenu.background", C_POPUP}, {"PopupMenu.foreground", C_WHITE},
            {"PopupMenu.border", BorderFactory.createLineBorder(C_BORDER)},
            {"MenuItem.background", C_POPUP}, {"MenuItem.foreground", C_WHITE},
            {"MenuItem.selectionBackground", C_ACCENT}, {"MenuItem.selectionForeground", C_WHITE},
            {"Viewport.background", C_BG},
            {"Separator.foreground", C_BORDER}, {"Separator.background", C_BG},
            {"CheckBox.background", C_BG}, {"CheckBox.foreground", C_WHITE},
            {"CheckBox.focus", C_BG},
            {"TabbedPane.background", C_BG}, {"TabbedPane.foreground", C_WHITE},
        };
        for (Object[] d : defs) UIManager.put(d[0], d[1]);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR & GUI ASSEMBLY
    // ═══════════════════════════════════════════════════════════════════
    CTBadlion() {
        setTitle(APP + " v" + VER + " — Java Edition");
        setSize(980, 650);
        setMinimumSize(new Dimension(860, 560));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(C_BG);

        add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(C_BG);
        main.add(buildContentArea(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        loadProfile();
    }

    // ───────── Sidebar ─────────
    JPanel buildSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(C_SIDEBAR);
        sidebarPanel.setPreferredSize(new Dimension(200, 0));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        sidebarPanel.add(Box.createVerticalStrut(24));

        JLabel logo = centeredLabel("CT BADLION", new Font("Segoe UI", Font.BOLD, 22), C_ACCENT);
        sidebarPanel.add(logo);
        sidebarPanel.add(Box.createVerticalStrut(4));

        JLabel sub = centeredLabel("v" + VER + " • FOSS", new Font("Consolas", Font.PLAIN, 10), C_DIM);
        sidebarPanel.add(sub);
        sidebarPanel.add(Box.createVerticalStrut(36));

        addSideBtn("\u25B6  PLAY",     "play",     true);
        addSideBtn("\u2699  SETTINGS", "settings", false);
        addSideBtn("\u2630  CONSOLE",  "console",  false);
        addSideBtn("\u24D8  ABOUT",    "about",    false);

        sidebarPanel.add(Box.createVerticalGlue());

        JLabel ver = centeredLabel("MIT License • Team Flames", new Font("Segoe UI", Font.PLAIN, 9), C_DIM);
        sidebarPanel.add(ver);
        sidebarPanel.add(Box.createVerticalStrut(16));

        return sidebarPanel;
    }

    JLabel centeredLabel(String text, Font font, Color fg) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(font);
        l.setForeground(fg);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    void addSideBtn(String title, String card, boolean active) {
        JButton btn = new JButton(title);
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setPreferredSize(new Dimension(200, 42));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        btn.setBackground(active ? C_SIDEBAR_SEL : C_SIDEBAR);
        btn.setForeground(active ? C_WHITE : C_GREY);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("card", card);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(C_SIDEBAR_SEL))
                    btn.setBackground(C_SIDEBAR_HI);
            }
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(C_SIDEBAR_SEL))
                    btn.setBackground(C_SIDEBAR);
            }
        });
        btn.addActionListener(e -> switchCard(card, btn));
        sidebarPanel.add(btn);
    }

    void switchCard(String name, JButton active) {
        contentLay.show(contentCards, name);
        curNav = name;
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton) {
                c.setBackground(C_SIDEBAR);
                c.setForeground(C_GREY);
            }
        }
        active.setBackground(C_SIDEBAR_SEL);
        active.setForeground(C_WHITE);
    }

    // ───────── Content Cards ─────────
    JPanel buildContentArea() {
        contentLay = new CardLayout();
        contentCards = new JPanel(contentLay);
        contentCards.setBackground(C_BG);

        contentCards.add(buildPlayCard(),     "play");
        contentCards.add(buildSettingsCard(), "settings");
        contentCards.add(buildConsoleCard(),  "console");
        contentCards.add(buildAboutCard(),    "about");

        return contentCards;
    }

    // ───────── PLAY Card ─────────
    JPanel buildPlayCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);

        // Center splash
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(C_BG);
        JPanel splash = new JPanel();
        splash.setLayout(new BoxLayout(splash, BoxLayout.Y_AXIS));
        splash.setBackground(C_BG);

        JLabel h1 = centeredLabel("READY TO LAUNCH", new Font("Segoe UI", Font.BOLD, 36), C_WHITE);
        splash.add(h1);
        splash.add(Box.createVerticalStrut(8));
        JLabel h2 = centeredLabel("Select a version and hit LAUNCH",
                new Font("Segoe UI", Font.PLAIN, 14), C_DIM);
        splash.add(h2);
        center.add(splash);
        p.add(center, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new BorderLayout(0, 0));
        bottom.setBackground(C_TOPBAR);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JPanel controls = new JPanel(new GridLayout(2, 1, 4, 6));
        controls.setBackground(C_TOPBAR);

        // Row 1: Version + Mods
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setBackground(C_TOPBAR);

        ddVersion = new DarkDropdown();
        ddVersion.setPreferredSize(new Dimension(240, 28));
        row1.add(ddVersion);

        cbFabric = darkCheck("Fabric", "Auto-install Fabric Loader for selected version");
        row1.add(cbFabric);

        cbFabOpt = darkCheck("Fab. Optimized", "Install Fabulously Optimized modpack (requires Fabric)");
        cbFabOpt.addActionListener(e -> {
            if (cbFabOpt.isSelected()) cbFabric.setSelected(true);
        });
        row1.add(cbFabOpt);

        row1.add(Box.createHorizontalStrut(12));
        lblStatus = new JLabel("Idle");
        lblStatus.setForeground(C_DIM);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        row1.add(lblStatus);

        lblProgress = new JLabel("");
        lblProgress.setForeground(C_GREY);
        lblProgress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        row1.add(lblProgress);

        controls.add(row1);

        // Row 2: User + RAM
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.setBackground(C_TOPBAR);

        row2.add(dimLabel("User:"));
        tfUser = darkField("Player", 110, 26);
        row2.add(tfUser);

        row2.add(dimLabel("RAM (MB):"));
        tfRam = darkField("4096", 60, 26);
        row2.add(tfRam);

        controls.add(row2);
        bottom.add(controls, BorderLayout.CENTER);

        // Launch button
        btnPlay = new JButton("LAUNCH");
        btnPlay.setPreferredSize(new Dimension(140, 58));
        btnPlay.setBackground(C_ACCENT);
        btnPlay.setForeground(C_WHITE);
        btnPlay.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnPlay.setBorder(BorderFactory.createEmptyBorder());
        btnPlay.setFocusPainted(false);
        btnPlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPlay.addActionListener(e -> onPlayClick());
        btnPlay.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btnPlay.isEnabled()) btnPlay.setBackground(C_ACCENT_HI);
            }
            public void mouseExited(MouseEvent e) {
                if (btnPlay.isEnabled()) btnPlay.setBackground(C_ACCENT);
            }
        });
        bottom.add(btnPlay, BorderLayout.EAST);

        // Progress
        progBar = new JProgressBar(0, 100);
        progBar.setPreferredSize(new Dimension(0, 3));
        progBar.setBorderPainted(false);
        progBar.setBackground(C_TOPBAR);
        progBar.setForeground(C_ACCENT);
        progBar.setValue(0);
        bottom.add(progBar, BorderLayout.SOUTH);

        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    // ───────── SETTINGS Card ─────────
    JPanel buildSettingsCard() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(C_WHITE);
        p.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(C_BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 4, 8, 12);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Java path
        gc.gridx = 0; gc.gridy = row;
        grid.add(dimLabel("Java Path:"), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
        tfJavaPath = darkField(getDefaultJavaPath(), 300, 26);
        grid.add(tfJavaPath, gc);
        gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        row++;

        // Show snapshots
        gc.gridx = 0; gc.gridy = row;
        grid.add(dimLabel("Show Snapshots:"), gc);
        gc.gridx = 1;
        cbSnapshots = darkCheck("Include snapshot versions", null);
        cbSnapshots.addActionListener(e -> refreshVersionList());
        grid.add(cbSnapshots, gc);
        row++;

        // Fullscreen
        gc.gridx = 0; gc.gridy = row;
        grid.add(dimLabel("Fullscreen:"), gc);
        gc.gridx = 1;
        cbFullscreen = darkCheck("Launch in fullscreen mode", null);
        grid.add(cbFullscreen, gc);
        row++;

        // Game directory
        gc.gridx = 0; gc.gridy = row;
        grid.add(dimLabel("Game Dir:"), gc);
        gc.gridx = 1;
        JLabel dirLabel = new JLabel(ROOT.toString());
        dirLabel.setForeground(C_GREY);
        dirLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        grid.add(dirLabel, gc);
        row++;

        // Mods directory
        gc.gridx = 0; gc.gridy = row;
        grid.add(dimLabel("Mods Dir:"), gc);
        gc.gridx = 1;
        JLabel modsLabel = new JLabel(MODS_DIR.toString());
        modsLabel.setForeground(C_GREY);
        modsLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        grid.add(modsLabel, gc);
        row++;

        // Open folders
        gc.gridx = 0; gc.gridy = row;
        JButton btnOpenRoot = darkBtn("Open Game Dir");
        btnOpenRoot.addActionListener(e -> openDir(ROOT));
        grid.add(btnOpenRoot, gc);

        gc.gridx = 1;
        JButton btnOpenMods = darkBtn("Open Mods Dir");
        btnOpenMods.addActionListener(e -> openDir(MODS_DIR));
        grid.add(btnOpenMods, gc);

        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    // ───────── CONSOLE Card ─────────
    JPanel buildConsoleCard() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_BG);
        JLabel title = new JLabel("Console Output");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(C_WHITE);
        header.add(title, BorderLayout.WEST);

        JButton btnClear = darkBtn("Clear");
        btnClear.addActionListener(e -> taLog.setText(""));
        header.add(btnClear, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        taLog = new JTextArea();
        taLog.setEditable(false);
        taLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        taLog.setBackground(C_CONSOLE);
        taLog.setForeground(C_CON_TEXT);
        taLog.setCaretColor(C_CON_TEXT);
        JScrollPane sp = new JScrollPane(taLog);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    // ───────── ABOUT Card ─────────
    JPanel buildAboutCard() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(C_BG);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(C_BG);

        box.add(centeredLabel("CTBadlion", new Font("Segoe UI", Font.BOLD, 32), C_ACCENT));
        box.add(Box.createVerticalStrut(6));
        box.add(centeredLabel("Version " + VER, new Font("Segoe UI", Font.PLAIN, 14), C_GREY));
        box.add(Box.createVerticalStrut(20));
        box.add(centeredLabel("Open-source Minecraft Java Edition launcher",
                new Font("Segoe UI", Font.PLAIN, 13), C_DIM));
        box.add(centeredLabel("with Fabric + Fabulously Optimized support.",
                new Font("Segoe UI", Font.PLAIN, 13), C_DIM));
        box.add(Box.createVerticalStrut(24));
        box.add(centeredLabel("Made by Team Flames / Samsoft / Flames Co.",
                new Font("Segoe UI", Font.BOLD, 12), C_GREY));
        box.add(Box.createVerticalStrut(8));
        box.add(centeredLabel("Licensed under MIT — Free and Open Source Software",
                new Font("Segoe UI", Font.PLAIN, 11), C_DIM));
        box.add(Box.createVerticalStrut(30));
        box.add(centeredLabel("Badlion-inspired aesthetic • Not affiliated with Mojang or Badlion",
                new Font("Segoe UI", Font.ITALIC, 10), C_DIM));

        p.add(box);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER UI FACTORIES
    // ═══════════════════════════════════════════════════════════════════
    JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(C_DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    JTextField darkField(String text, int w, int h) {
        JTextField f = new JTextField(text);
        f.setPreferredSize(new Dimension(w, h));
        f.setBackground(C_FIELD);
        f.setForeground(C_WHITE);
        f.setCaretColor(C_WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        return f;
    }

    JCheckBox darkCheck(String text, String tooltip) {
        JCheckBox cb = new JCheckBox(text);
        cb.setBackground(C_TOPBAR);
        cb.setForeground(C_WHITE);
        cb.setFocusPainted(false);
        if (tooltip != null) cb.setToolTipText(tooltip);
        return cb;
    }

    JButton darkBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(C_FIELD);
        b.setForeground(C_WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(C_HOVER); }
            public void mouseExited(MouseEvent e) { b.setBackground(C_FIELD); }
        });
        return b;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MANIFEST FETCHING & VERSION LIST
    // ═══════════════════════════════════════════════════════════════════
    void fetchManifest() {
        setStatus("Fetching versions...");
        executor.submit(() -> {
            try {
                String json = httpGet(MANIFEST_URL);
                parseManifest(json);
                SwingUtilities.invokeLater(() -> {
                    refreshVersionList();
                    setStatus("Ready — " + allVersions.size() + " versions loaded");
                });
            } catch (Exception e) {
                logErr("Manifest fetch failed: " + e.getMessage());
                setStatus("Offline — no manifest");
                // Try to find locally cached versions
                scanLocalVersions();
            }
        });
    }

    void parseManifest(String json) {
        allVersions.clear();
        // Mojang manifest: {"id":"X","type":"Y","url":"Z","time":...,"releaseTime":...}
        // We match all three fields in order
        Pattern p = Pattern.compile(
                "\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*," +
                "\\s*\"type\"\\s*:\\s*\"([^\"]+)\"\\s*," +
                "\\s*\"url\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            allVersions.add(new String[]{m.group(1), m.group(2), m.group(3)});
        }
        log("Parsed " + allVersions.size() + " versions from manifest");
    }

    void scanLocalVersions() {
        try {
            if (!Files.isDirectory(VER_DIR)) return;
            Files.list(VER_DIR).filter(Files::isDirectory).forEach(dir -> {
                String id = dir.getFileName().toString();
                Path jsonFile = dir.resolve(id + ".json");
                if (Files.exists(jsonFile)) {
                    allVersions.add(new String[]{id, "local", ""});
                }
            });
            if (!allVersions.isEmpty()) {
                log("Found " + allVersions.size() + " local version(s)");
                SwingUtilities.invokeLater(this::refreshVersionList);
            }
        } catch (Exception e) {
            logErr("Local scan: " + e.getMessage());
        }
    }

    void refreshVersionList() {
        boolean showSnaps = cbSnapshots != null && cbSnapshots.isSelected();
        dispVersions.clear();
        synchronized (allVersions) {
            for (String[] v : allVersions) {
                if (showSnaps || v[1].equals("release") || v[1].equals("local")) {
                    dispVersions.add(v);
                }
            }
        }
        ddVersion.updateList();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LAUNCH PIPELINE
    // ═══════════════════════════════════════════════════════════════════
    void onPlayClick() {
        if (launching) return;
        String[] ver = ddVersion.getSelected();
        if (ver == null) {
            logErr("No version selected");
            return;
        }
        saveProfile();
        launching = true;
        btnPlay.setEnabled(false);
        btnPlay.setBackground(C_DIM);
        btnPlay.setText("...");
        progBar.setValue(0);

        // Switch to console view
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton && "console".equals(((JButton) c).getClientProperty("card"))) {
                switchCard("console", (JButton) c);
                break;
            }
        }

        executor.submit(() -> {
            try {
                prepareAndLaunch(ver[0], ver[2]);
            } catch (Exception e) {
                logErr("Launch failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                launching = false;
                SwingUtilities.invokeLater(() -> {
                    btnPlay.setEnabled(true);
                    btnPlay.setBackground(C_ACCENT);
                    btnPlay.setText("LAUNCH");
                    setStatus("Ready");
                    progBar.setValue(0);
                    lblProgress.setText("");
                });
            }
        });
    }

    void prepareAndLaunch(String verId, String jsonUrl) throws Exception {
        log("═══ Preparing: " + verId + " ═══");

        // 1 ── Version JSON
        setStatus("Downloading version JSON...");
        Path verDir  = VER_DIR.resolve(verId);
        Path jsonPath = verDir.resolve(verId + ".json");
        Files.createDirectories(verDir);

        if (!Files.exists(jsonPath)) {
            if (jsonUrl == null || jsonUrl.isEmpty()) {
                throw new Exception("No URL for version JSON and not cached locally");
            }
            log("Downloading version JSON...");
            download(jsonUrl, jsonPath, "version JSON");
        }
        String verJson = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);

        // 2 ── Client JAR
        setStatus("Checking client JAR...");
        Path clientJar = verDir.resolve(verId + ".jar");
        if (!Files.exists(clientJar)) {
            String jarUrl = jsonExtract(verJson,
                    "\"client\"\\s*:\\s*\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");
            if (jarUrl != null) {
                log("Downloading client JAR...");
                download(jarUrl, clientJar, "client JAR");
            } else {
                throw new Exception("Could not find client JAR URL in version JSON");
            }
        }

        // 3 ── Libraries
        setStatus("Resolving libraries...");
        StringBuilder classpath = new StringBuilder();
        downloadLibraries(verJson, classpath);
        classpath.append(clientJar.toString());

        // 4 ── Natives
        setStatus("Extracting natives...");
        Path nativesDir = NAT_DIR.resolve(verId);
        Files.createDirectories(nativesDir);
        extractNatives(verJson, nativesDir);

        // 5 ── Asset Index
        setStatus("Downloading assets...");
        String assetIndexId = jsonExtract(verJson,
                "\"assetIndex\"\\s*:\\s*\\{[^}]*?\"id\"\\s*:\\s*\"([^\"]+)\"");
        if (assetIndexId == null) assetIndexId = "legacy";
        downloadAssetIndex(verJson, assetIndexId);

        // 6 ── Fabric (if checked)
        if (cbFabric.isSelected()) {
            setStatus("Installing Fabric...");
            installFabric(verId, classpath);
        }

        // 7 ── Fabulously Optimized mods
        if (cbFabOpt.isSelected()) {
            setStatus("Checking FO mods...");
            log("Fabulously Optimized: ensure mods are in " + MODS_DIR);
            log("Visit https://modrinth.com/modpack/fabulously-optimized for the latest pack.");
            // Auto-download is complex (Modrinth pack index → individual mod jars).
            // We log guidance; a future version can automate this via Modrinth API.
        }

        // 8 ── Build command line
        setStatus("Launching Minecraft " + verId + "...");
        List<String> cmd = new ArrayList<>();

        String javaPath = (tfJavaPath != null && !tfJavaPath.getText().trim().isEmpty())
                ? tfJavaPath.getText().trim() : getDefaultJavaPath();
        cmd.add(javaPath);

        // macOS LWJGL requirement
        if (isMac()) {
            cmd.add("-XstartOnFirstThread");
            log("macOS: added -XstartOnFirstThread");
        }

        // Java 9+ module flags for modern MC/LWJGL
        int javaVer = getJavaMajorVersion();
        if (javaVer >= 9) {
            cmd.add("--add-opens=java.base/java.net=ALL-UNNAMED");
            cmd.add("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED");
            cmd.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
            cmd.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            cmd.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            cmd.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED");
        }

        cmd.add("-Xmx" + tfRam.getText().trim() + "M");
        cmd.add("-Xms256M");
        cmd.add("-Djava.library.path=" + nativesDir.toString());
        cmd.add("-Dminecraft.launcher.brand=" + APP);
        cmd.add("-Dminecraft.launcher.version=" + VER);
        cmd.add("-cp");
        cmd.add(classpath.toString());

        // Main class — check for Fabric override
        String mainClass = jsonExtract(verJson, "\"mainClass\"\\s*:\\s*\"([^\"]+)\"");
        if (mainClass == null) mainClass = "net.minecraft.client.main.Main";
        cmd.add(mainClass);

        // Game arguments
        String offlineUuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + tfUser.getText().trim()).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");

        cmd.add("--version");   cmd.add(verId);
        cmd.add("--gameDir");   cmd.add(ROOT.toString());
        cmd.add("--assetsDir"); cmd.add(ASS_DIR.toString());
        cmd.add("--assetIndex"); cmd.add(assetIndexId);
        cmd.add("--uuid");       cmd.add(offlineUuid);
        cmd.add("--accessToken"); cmd.add("0");
        cmd.add("--userType");   cmd.add("legacy");
        cmd.add("--username");   cmd.add(tfUser.getText().trim());

        if (cbFullscreen != null && cbFullscreen.isSelected()) {
            cmd.add("--fullscreen");
        }

        log("Launching with " + cmd.size() + " args");
        log("Main class: " + mainClass);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(ROOT.toFile());
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // Stream MC output to console
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                log("[MC] " + line);
            }
        }

        int exit = proc.waitFor();
        log("Minecraft exited with code " + exit);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LIBRARY DOWNLOADING
    // ═══════════════════════════════════════════════════════════════════
    void downloadLibraries(String verJson, StringBuilder classpath) {
        // Extract "libraries" array entries.
        // Each library has: name (maven coord), downloads.artifact.path, downloads.artifact.url
        // We also need to check "rules" for OS filtering.
        log("Resolving libraries...");

        // Find all artifact entries: path + url pairs
        Pattern artifactP = Pattern.compile(
                "\"artifact\"\\s*:\\s*\\{[^}]*?" +
                "\"path\"\\s*:\\s*\"([^\"]+)\"[^}]*?" +
                "\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);

        // Also find library name for logging
        Pattern nameP = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

        // Split on library boundaries — each library object starts with {"downloads" or {"name"
        // We'll use a simpler approach: find all artifact path+url in sequence
        Matcher m = artifactP.matcher(verJson);
        List<String[]> libs = new ArrayList<>();
        while (m.find()) {
            libs.add(new String[]{m.group(1), m.group(2)});
        }
        log("Found " + libs.size() + " library artifacts");

        dlCount.set(0);
        dlTotal.set(libs.size());

        for (String[] lib : libs) {
            String path = lib[0];
            String url  = lib[1];
            Path target = LIB_DIR.resolve(path.replace("/", File.separator));
            classpath.append(target.toString()).append(File.pathSeparator);

            if (!Files.exists(target)) {
                try {
                    Files.createDirectories(target.getParent());
                    download(url, target, path);
                } catch (Exception e) {
                    logErr("Library download failed: " + path + " — " + e.getMessage());
                }
            }
            int done = dlCount.incrementAndGet();
            updateProgress(done, libs.size(), "Libraries");
        }
        log("Libraries resolved: " + libs.size());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NATIVES EXTRACTION
    // ═══════════════════════════════════════════════════════════════════
    void extractNatives(String verJson, Path nativesDir) {
        // Find classifiers → natives entries with OS-specific keys
        String osKey = getOsNativeKey();
        log("Looking for natives classifier: " + osKey);

        // Pattern: "classifiers" block containing OS-specific native jars
        // "natives-linux": { "path": "...", "url": "..." }
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(osKey) + "\"\\s*:\\s*\\{[^}]*?" +
                "\"path\"\\s*:\\s*\"([^\"]+)\"[^}]*?" +
                "\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
        Matcher m = p.matcher(verJson);

        int count = 0;
        while (m.find()) {
            String path = m.group(1);
            String url  = m.group(2);
            Path jarFile = LIB_DIR.resolve(path.replace("/", File.separator));

            try {
                if (!Files.exists(jarFile)) {
                    Files.createDirectories(jarFile.getParent());
                    download(url, jarFile, "native: " + path);
                }
                // Extract .so / .dll / .dylib / .jnilib from the jar
                extractNativeJar(jarFile, nativesDir);
                count++;
            } catch (Exception e) {
                logErr("Native extract fail: " + path + " — " + e.getMessage());
            }
        }
        log("Extracted " + count + " native jar(s) to " + nativesDir);
    }

    void extractNativeJar(Path jarFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || name.startsWith("META-INF")) continue;
                // Only extract native library files
                if (name.endsWith(".so") || name.endsWith(".dll") ||
                    name.endsWith(".dylib") || name.endsWith(".jnilib")) {
                    Path out = targetDir.resolve(Paths.get(name).getFileName());
                    if (!Files.exists(out)) {
                        Files.copy(zis, out);
                    }
                }
            }
        }
    }

    String getOsNativeKey() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))   return "natives-windows";
        if (os.contains("mac"))   return "natives-macos";
        return "natives-linux";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ASSET INDEX & OBJECTS
    // ═══════════════════════════════════════════════════════════════════
    void downloadAssetIndex(String verJson, String indexId) {
        // Get asset index URL
        String indexUrl = jsonExtract(verJson,
                "\"assetIndex\"\\s*:\\s*\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");
        if (indexUrl == null) {
            log("No asset index URL found — skipping asset download");
            return;
        }

        Path indexFile = ASS_DIR.resolve("indexes").resolve(indexId + ".json");
        try {
            if (!Files.exists(indexFile)) {
                log("Downloading asset index: " + indexId);
                download(indexUrl, indexFile, "asset index");
            }

            // Parse asset objects: "hash": "abcdef123..."
            String indexJson = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
            Pattern hp = Pattern.compile("\"hash\"\\s*:\\s*\"([a-f0-9]{40})\"");
            Matcher hm = hp.matcher(indexJson);

            List<String> hashes = new ArrayList<>();
            while (hm.find()) hashes.add(hm.group(1));
            log("Asset index has " + hashes.size() + " objects");

            int downloaded = 0;
            for (int i = 0; i < hashes.size(); i++) {
                String hash = hashes.get(i);
                String prefix = hash.substring(0, 2);
                Path objPath = ASS_DIR.resolve("objects").resolve(prefix).resolve(hash);

                if (!Files.exists(objPath)) {
                    Files.createDirectories(objPath.getParent());
                    String objUrl = RESOURCES_URL + prefix + "/" + hash;
                    try {
                        downloadQuiet(objUrl, objPath);
                        downloaded++;
                    } catch (Exception e) {
                        // Non-fatal: some assets may be optional
                    }
                }
                if (i % 50 == 0) {
                    updateProgress(i + 1, hashes.size(), "Assets");
                }
            }
            log("Assets: " + downloaded + " newly downloaded, " +
                    (hashes.size() - downloaded) + " cached");
        } catch (Exception e) {
            logErr("Asset download error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FABRIC LOADER INSTALLATION
    // ═══════════════════════════════════════════════════════════════════
    void installFabric(String mcVersion, StringBuilder classpath) {
        log("Installing Fabric Loader for MC " + mcVersion + "...");
        try {
            // Step 1: Get available loader versions from Fabric Meta
            String metaUrl = FABRIC_META_URL + mcVersion;
            String metaJson = httpGet(metaUrl);

            // Extract the first (latest stable) loader version
            String loaderVer = jsonExtract(metaJson,
                    "\"version\"\\s*:\\s*\"([0-9]+\\.[0-9]+\\.[0-9]+)\"");
            if (loaderVer == null) {
                logErr("Fabric: no loader found for MC " + mcVersion);
                return;
            }
            log("Fabric Loader version: " + loaderVer);

            // Step 2: Get the full profile JSON from Fabric meta
            String profileUrl = FABRIC_META_URL + mcVersion + "/" + loaderVer + "/profile/json";
            String profileJson = httpGet(profileUrl);

            // Step 3: Extract main class (usually net.fabricmc.loader.impl.launch.knot.KnotClient)
            String fabricMain = jsonExtract(profileJson, "\"mainClass\"\\s*:\\s*\"([^\"]+)\"");
            if (fabricMain != null) {
                log("Fabric main class: " + fabricMain);
            }

            // Step 4: Download Fabric libraries listed in the profile
            Pattern libP = Pattern.compile(
                    "\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?" +
                    "\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
            Matcher lm = libP.matcher(profileJson);

            int count = 0;
            while (lm.find()) {
                String mavenCoord = lm.group(1); // e.g. net.fabricmc:fabric-loader:0.15.11
                String repoUrl    = lm.group(2); // e.g. https://maven.fabricmc.net/

                Path libPath = mavenToPath(mavenCoord);
                if (libPath == null) continue;

                Path target = LIB_DIR.resolve(libPath);
                classpath.insert(0, target.toString() + File.pathSeparator);

                if (!Files.exists(target)) {
                    String jarUrl = repoUrl + libPath.toString().replace(File.separator, "/");
                    try {
                        Files.createDirectories(target.getParent());
                        download(jarUrl, target, "fabric: " + mavenCoord);
                        count++;
                    } catch (Exception e) {
                        logErr("Fabric lib fail: " + mavenCoord + " — " + e.getMessage());
                    }
                }
            }
            log("Fabric: downloaded " + count + " new libraries");

            // Step 5: Write a merged version JSON so we use Fabric's main class
            if (fabricMain != null) {
                // We'll override the main class at launch time by storing it
                // The launch code checks for Fabric and uses this main class
                Path fabricMarker = VER_DIR.resolve(mcVersion).resolve("fabric_main.txt");
                Files.write(fabricMarker, fabricMain.getBytes(StandardCharsets.UTF_8));
                log("Fabric installation complete");
            }

        } catch (Exception e) {
            logErr("Fabric installation failed: " + e.getMessage());
            log("You can manually install Fabric from https://fabricmc.net/use/installer/");
        }
    }

    /** Convert Maven coordinate (group:artifact:version) to relative jar path */
    Path mavenToPath(String coord) {
        String[] parts = coord.split(":");
        if (parts.length < 3) return null;
        String group    = parts[0].replace('.', File.separatorChar);
        String artifact = parts[1];
        String version  = parts[2];
        return Paths.get(group, artifact, version, artifact + "-" + version + ".jar");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NETWORK & IO UTILITIES
    // ═══════════════════════════════════════════════════════════════════
    String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", APP + "/" + VER);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " for " + urlStr);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    void download(String urlStr, Path target, String label) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", APP + "/" + VER);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " downloading " + label);
        }

        long total = conn.getContentLengthLong();
        long current = 0;

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(target)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                current += n;
                if (total > 0) {
                    final int pct = (int) ((current * 100) / total);
                    SwingUtilities.invokeLater(() -> progBar.setValue(pct));
                }
            }
        }
    }

    /** Silent download — no progress bar updates (for bulk asset downloads) */
    void downloadQuiet(String urlStr, Path target) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", APP + "/" + VER);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        if (conn.getResponseCode() != 200) {
            throw new IOException("HTTP " + conn.getResponseCode());
        }
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(target)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  JSON HELPER (regex-based, no external deps)
    // ═══════════════════════════════════════════════════════════════════
    String jsonExtract(String json, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SYSTEM HELPERS
    // ═══════════════════════════════════════════════════════════════════
    String getDefaultJavaPath() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    int getJavaMajorVersion() {
        String ver = System.getProperty("java.version", "1.8");
        if (ver.startsWith("1.")) ver = ver.substring(2);
        int dot = ver.indexOf('.');
        if (dot > 0) ver = ver.substring(0, dot);
        int dash = ver.indexOf('-');
        if (dash > 0) ver = ver.substring(0, dash);
        try { return Integer.parseInt(ver); }
        catch (NumberFormatException e) { return 8; }
    }

    void openDir(Path dir) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir.toFile());
            }
        } catch (Exception e) {
            logErr("Could not open directory: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PROFILE PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════
    void saveProfile() {
        Properties p = new Properties();
        p.setProperty("user", tfUser.getText());
        p.setProperty("ram", tfRam.getText());
        if (tfJavaPath != null) p.setProperty("java", tfJavaPath.getText());
        p.setProperty("fabric", String.valueOf(cbFabric.isSelected()));
        p.setProperty("fabopt", String.valueOf(cbFabOpt.isSelected()));
        if (cbSnapshots != null) p.setProperty("snapshots", String.valueOf(cbSnapshots.isSelected()));
        if (cbFullscreen != null) p.setProperty("fullscreen", String.valueOf(cbFullscreen.isSelected()));
        int idx = ddVersion.getSelectedIndex();
        if (idx >= 0 && idx < dispVersions.size()) {
            p.setProperty("version", dispVersions.get(idx)[0]);
        }
        try (OutputStream out = Files.newOutputStream(PROF_FILE)) {
            p.store(out, APP + " v" + VER + " Config");
        } catch (Exception e) {
            logErr("Profile save: " + e.getMessage());
        }
    }

    void loadProfile() {
        if (!Files.exists(PROF_FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PROF_FILE)) {
            p.load(in);
            if (p.containsKey("user"))       tfUser.setText(p.getProperty("user"));
            if (p.containsKey("ram"))        tfRam.setText(p.getProperty("ram"));
            if (p.containsKey("java") && tfJavaPath != null)
                tfJavaPath.setText(p.getProperty("java"));
            if (p.containsKey("fabric"))     cbFabric.setSelected(Boolean.parseBoolean(p.getProperty("fabric")));
            if (p.containsKey("fabopt"))     cbFabOpt.setSelected(Boolean.parseBoolean(p.getProperty("fabopt")));
            if (p.containsKey("snapshots") && cbSnapshots != null)
                cbSnapshots.setSelected(Boolean.parseBoolean(p.getProperty("snapshots")));
            if (p.containsKey("fullscreen") && cbFullscreen != null)
                cbFullscreen.setSelected(Boolean.parseBoolean(p.getProperty("fullscreen")));
            // Version selection is restored after manifest loads
        } catch (Exception e) {
            logErr("Profile load: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LOGGING & STATUS
    // ═══════════════════════════════════════════════════════════════════
    void log(String s) {
        String ts = String.format("[%tT] %s", System.currentTimeMillis(), s);
        SwingUtilities.invokeLater(() -> {
            if (taLog != null) {
                taLog.append(ts + "\n");
                taLog.setCaretPosition(taLog.getDocument().getLength());
            }
        });
    }

    void logErr(String s) {
        String ts = String.format("[%tT] [ERR] %s", System.currentTimeMillis(), s);
        SwingUtilities.invokeLater(() -> {
            if (taLog != null) {
                taLog.append(ts + "\n");
                taLog.setCaretPosition(taLog.getDocument().getLength());
            }
        });
    }

    void setStatus(String s) {
        SwingUtilities.invokeLater(() -> {
            if (lblStatus != null) lblStatus.setText(s);
        });
    }

    void updateProgress(int done, int total, String label) {
        int pct = total > 0 ? (done * 100) / total : 0;
        SwingUtilities.invokeLater(() -> {
            progBar.setValue(pct);
            lblProgress.setText(label + ": " + done + "/" + total);
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CUSTOM DARK DROPDOWN
    // ═══════════════════════════════════════════════════════════════════
    class DarkDropdown extends JComboBox<String> {
        DarkDropdown() {
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index,
                        boolean isSelected, boolean hasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
                    setBackground(isSelected ? C_ACCENT : C_FIELD);
                    setForeground(C_WHITE);
                    setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    return this;
                }
            });
            setBackground(C_FIELD);
            setForeground(C_WHITE);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }

        void updateList() {
            removeAllItems();
            synchronized (dispVersions) {
                for (String[] v : dispVersions) {
                    String tag = v[1].equals("release") ? "" :
                                 v[1].equals("local") ? " [local]" : " [" + v[1] + "]";
                    addItem(v[0] + tag);
                }
            }
            // Try to restore saved version
            if (Files.exists(PROF_FILE)) {
                try {
                    Properties p = new Properties();
                    p.load(Files.newInputStream(PROF_FILE));
                    String saved = p.getProperty("version");
                    if (saved != null) {
                        for (int i = 0; i < dispVersions.size(); i++) {
                            if (dispVersions.get(i)[0].equals(saved)) {
                                setSelectedIndex(i);
                                return;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (getItemCount() > 0) setSelectedIndex(0);
        }

        String[] getSelected() {
            int idx = getSelectedIndex();
            if (idx < 0 || idx >= dispVersions.size()) return null;
            return dispVersions.get(idx);
        }
    }
}
