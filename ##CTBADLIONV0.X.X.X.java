/*
 * CTBadlion — Open-source Minecraft Java Edition Launcher
 * Copyright (C) 2025 Team Flames / Samsoft / Flames Co.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  CTBadlion v1.0 — Java Edition Launcher                                  ║
 * ║  Dark GUI · 980×650 · Offline Profiles · Fabric + Mods Manager            ║
 * ║  Badlion-Style Client with Fabric + Fabulously Optimized Support          ║
 * ║                                                                           ║
 * ║  BUILD:  javac CTBadlion.java                                             ║
 * ║  RUN:    java CTBadlion                                                   ║
 * ║  JAR:    jar cfe ctbadlion.jar CTBadlion CTBadlion*.class                 ║
 * ║  Requires: Java 8+ (Java 17/21+ supported via module flags)              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
    static final Color C_ACCENT      = new Color(0, 168, 255);
    static final Color C_ACCENT_HI   = new Color(30, 190, 255);
    static final Color C_ACCENT_DIM  = new Color(0, 120, 190);
    static final Color C_GREEN       = new Color(62, 195, 55);
    static final Color C_WHITE       = new Color(235, 235, 240);
    static final Color C_GREY        = new Color(160, 160, 170);
    static final Color C_DIM         = new Color(100, 100, 115);
    static final Color C_BORDER      = new Color(50, 50, 58);
    static final Color C_CONSOLE     = new Color(14, 14, 18);
    static final Color C_CON_TEXT    = new Color(70, 210, 110);
    static final Color C_CARD_ALT    = new Color(22, 22, 28);
    static final Color C_RED         = new Color(255, 75, 75);

    static final String APP = "CTBadlion";
    static final String VER = "1.0";
    static final String MANIFEST_URL    = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    static final String RESOURCES_URL   = "https://resources.download.minecraft.net/";
    static final String FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions/loader/";

    static Path ROOT, VER_DIR, LIB_DIR, ASS_DIR, NAT_DIR, PROF_FILE, MODS_DIR, LOG_DIR;

    // ═══════════════════════════════════════════════════════════════════
    //  UI STATE
    // ═══════════════════════════════════════════════════════════════════
    JTextField     tfUser, tfRam, tfJavaPath;
    VerDropdown    ddVersion;
    JCheckBox      cbFabric, cbFabOpt, cbSnapshots, cbFullscreen;
    JTextArea      taLog, modDetailArea;
    JButton        btnPlay;
    JProgressBar   progBar;
    JLabel         lblStatus, lblProgress, lblModCount;
    JPanel         contentCards, sidebarPanel;
    CardLayout     contentLay;

    DefaultListModel<ModInfo> modListModel = new DefaultListModel<>();
    JList<ModInfo> modList;

    volatile boolean launching = false;

    final List<String[]> allVersions  = Collections.synchronizedList(new ArrayList<>());
    final List<String[]> dispVersions = Collections.synchronizedList(new ArrayList<>());

    ExecutorService executor = Executors.newFixedThreadPool(4);

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
            app.log("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
            app.log("OS:   " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            app.fetchManifest();
        });
    }

    static void initDirs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path home;
        if (os.contains("win"))      home = Paths.get(System.getenv("APPDATA"), ".minecraft");
        else if (os.contains("mac")) home = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "minecraft");
        else                         home = Paths.get(System.getProperty("user.home"), ".minecraft");
        ROOT      = home;
        VER_DIR   = home.resolve("versions");
        LIB_DIR   = home.resolve("libraries");
        ASS_DIR   = home.resolve("assets");
        NAT_DIR   = home.resolve("natives");
        MODS_DIR  = home.resolve("mods");
        LOG_DIR   = home.resolve("logs");
        PROF_FILE = home.resolve("ctbadlion_profiles.properties");
        for (Path d : new Path[]{VER_DIR, LIB_DIR, ASS_DIR.resolve("indexes"), ASS_DIR.resolve("objects"), NAT_DIR, MODS_DIR, LOG_DIR}) {
            try { Files.createDirectories(d); } catch (IOException e) { System.err.println("Dir fail: " + e); }
        }
    }

    static void applyDarkDefaults() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        Object[][] d = {
            {"Panel.background",C_BG},{"Panel.foreground",C_WHITE},{"Label.foreground",C_WHITE},
            {"ComboBox.background",C_FIELD},{"ComboBox.foreground",C_WHITE},
            {"ComboBox.selectionBackground",C_ACCENT},{"ComboBox.selectionForeground",C_WHITE},
            {"ComboBox.buttonBackground",C_FIELD},{"ComboBox.buttonDarkShadow",C_BORDER},
            {"ComboBox.buttonHighlight",C_HOVER},{"ComboBox.buttonShadow",C_BORDER},
            {"List.background",C_POPUP},{"List.foreground",C_WHITE},
            {"List.selectionBackground",C_ACCENT},{"List.selectionForeground",C_WHITE},
            {"ScrollBar.background",C_BG},{"ScrollBar.thumb",C_HOVER},{"ScrollBar.track",C_BG},{"ScrollBar.width",10},
            {"ScrollPane.background",C_BG},
            {"TextField.background",C_FIELD},{"TextField.foreground",C_WHITE},{"TextField.caretForeground",C_WHITE},
            {"TextArea.background",C_CONSOLE},{"TextArea.foreground",C_CON_TEXT},
            {"ToolTip.background",C_POPUP},{"ToolTip.foreground",C_WHITE},
            {"OptionPane.background",C_BG},{"OptionPane.foreground",C_WHITE},{"OptionPane.messageForeground",C_WHITE},
            {"Button.background",C_FIELD},{"Button.foreground",C_WHITE},
            {"ProgressBar.background",C_FIELD},{"ProgressBar.foreground",C_ACCENT},
            {"PopupMenu.background",C_POPUP},{"PopupMenu.foreground",C_WHITE},
            {"PopupMenu.border",BorderFactory.createLineBorder(C_BORDER)},
            {"MenuItem.background",C_POPUP},{"MenuItem.foreground",C_WHITE},
            {"MenuItem.selectionBackground",C_ACCENT},{"MenuItem.selectionForeground",C_WHITE},
            {"Viewport.background",C_BG},{"Separator.foreground",C_BORDER},{"Separator.background",C_BG},
            {"CheckBox.background",C_BG},{"CheckBox.foreground",C_WHITE},{"CheckBox.focus",C_BG},
        };
        for (Object[] e : d) UIManager.put(e[0], e[1]);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    CTBadlion() {
        setTitle(APP + " v" + VER + " \u2014 Java Edition");
        setSize(980, 650);
        setMinimumSize(new Dimension(860, 560));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(C_BG);
        add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(C_BG);
        main.add(buildContent(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        loadProfile();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SIDEBAR
    // ═══════════════════════════════════════════════════════════════════
    JPanel buildSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(C_SIDEBAR);
        sidebarPanel.setPreferredSize(new Dimension(200, 0));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        sidebarPanel.add(Box.createVerticalStrut(24));
        sidebarPanel.add(mkLabel("CT BADLION", new Font("Segoe UI", Font.BOLD, 22), C_ACCENT));
        sidebarPanel.add(Box.createVerticalStrut(4));
        sidebarPanel.add(mkLabel("v" + VER + " \u2022 GPL-3.0", new Font("Consolas", Font.PLAIN, 10), C_DIM));
        sidebarPanel.add(Box.createVerticalStrut(36));

        addNav("\u25B6  PLAY",     "play",     true);
        addNav("\u29C9  MODS",     "mods",     false);
        addNav("\u2699  SETTINGS", "settings", false);
        addNav("\u2630  CONSOLE",  "console",  false);
        addNav("\u24D8  ABOUT",    "about",    false);

        sidebarPanel.add(Box.createVerticalGlue());
        sidebarPanel.add(mkLabel("GPL-3.0 \u2022 Team Flames", new Font("Segoe UI", Font.PLAIN, 9), C_DIM));
        sidebarPanel.add(Box.createVerticalStrut(16));
        return sidebarPanel;
    }

    JLabel mkLabel(String text, Font font, Color fg) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(font); l.setForeground(fg); l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    void addNav(String title, String card, boolean active) {
        JButton btn = new JButton(title);
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setPreferredSize(new Dimension(200, 42));
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        btn.setBackground(active ? C_SIDEBAR_SEL : C_SIDEBAR);
        btn.setForeground(active ? C_WHITE : C_GREY);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("card", card);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!btn.getBackground().equals(C_SIDEBAR_SEL)) btn.setBackground(C_SIDEBAR_HI); }
            public void mouseExited(MouseEvent e)  { if (!btn.getBackground().equals(C_SIDEBAR_SEL)) btn.setBackground(C_SIDEBAR); }
        });
        btn.addActionListener(e -> switchCard(card, btn));
        sidebarPanel.add(btn);
    }

    void switchCard(String name, JButton active) {
        contentLay.show(contentCards, name);
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton) { c.setBackground(C_SIDEBAR); c.setForeground(C_GREY); }
        }
        active.setBackground(C_SIDEBAR_SEL); active.setForeground(C_WHITE);
        if ("mods".equals(name)) refreshMods();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONTENT CARDS
    // ═══════════════════════════════════════════════════════════════════
    JPanel buildContent() {
        contentLay = new CardLayout();
        contentCards = new JPanel(contentLay);
        contentCards.setBackground(C_BG);
        contentCards.add(buildPlayCard(),     "play");
        contentCards.add(buildModsCard(),     "mods");
        contentCards.add(buildSettingsCard(), "settings");
        contentCards.add(buildConsoleCard(),  "console");
        contentCards.add(buildAboutCard(),    "about");
        return contentCards;
    }

    // ── PLAY CARD ────────────────────────────────────────────────────
    JPanel buildPlayCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(C_BG);
        JPanel splash = new JPanel();
        splash.setLayout(new BoxLayout(splash, BoxLayout.Y_AXIS));
        splash.setBackground(C_BG);
        splash.add(mkLabel("READY TO LAUNCH", new Font("Segoe UI", Font.BOLD, 36), C_WHITE));
        splash.add(Box.createVerticalStrut(8));
        splash.add(mkLabel("Select a version and hit LAUNCH", new Font("Segoe UI", Font.PLAIN, 14), C_DIM));
        center.add(splash);
        p.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 0));
        bottom.setBackground(C_TOPBAR);
        bottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JPanel controls = new JPanel(new GridLayout(2, 1, 4, 6));
        controls.setBackground(C_TOPBAR);

        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        r1.setBackground(C_TOPBAR);
        ddVersion = new VerDropdown(); ddVersion.setPreferredSize(new Dimension(240, 28)); r1.add(ddVersion);
        cbFabric = mkCheck("Fabric", "Auto-install Fabric Loader"); r1.add(cbFabric);
        cbFabOpt = mkCheck("Fab. Optimized", "Install FO modpack (requires Fabric)");
        cbFabOpt.addActionListener(e -> { if (cbFabOpt.isSelected()) cbFabric.setSelected(true); });
        r1.add(cbFabOpt);
        r1.add(Box.createHorizontalStrut(12));
        lblStatus = new JLabel("Idle"); lblStatus.setForeground(C_DIM); lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11)); r1.add(lblStatus);
        lblProgress = new JLabel(""); lblProgress.setForeground(C_GREY); lblProgress.setFont(new Font("Segoe UI", Font.PLAIN, 11)); r1.add(lblProgress);
        controls.add(r1);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        r2.setBackground(C_TOPBAR);
        r2.add(dimLbl("User:")); tfUser = mkField("Player", 110); r2.add(tfUser);
        r2.add(dimLbl("RAM (MB):")); tfRam = mkField("4096", 60); r2.add(tfRam);
        controls.add(r2);
        bottom.add(controls, BorderLayout.CENTER);

        btnPlay = new JButton("LAUNCH");
        btnPlay.setPreferredSize(new Dimension(140, 58));
        btnPlay.setBackground(C_ACCENT); btnPlay.setForeground(C_WHITE);
        btnPlay.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnPlay.setBorder(BorderFactory.createEmptyBorder()); btnPlay.setFocusPainted(false);
        btnPlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPlay.addActionListener(e -> onPlayClick());
        btnPlay.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btnPlay.isEnabled()) btnPlay.setBackground(C_ACCENT_HI); }
            public void mouseExited(MouseEvent e)  { if (btnPlay.isEnabled()) btnPlay.setBackground(C_ACCENT); }
        });
        bottom.add(btnPlay, BorderLayout.EAST);

        progBar = new JProgressBar(0, 100);
        progBar.setPreferredSize(new Dimension(0, 3));
        progBar.setBorderPainted(false); progBar.setBackground(C_TOPBAR); progBar.setForeground(C_ACCENT);
        bottom.add(progBar, BorderLayout.SOUTH);

        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    // ── MODS CARD — reads metadata from inside each .jar ─────────────
    JPanel buildModsCard() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_BG);
        lblModCount = new JLabel("Installed Mods");
        lblModCount.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblModCount.setForeground(C_WHITE);
        header.add(lblModCount, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        hBtns.setBackground(C_BG);
        JButton btnBrowse = mkAccentBtn("\u2B73 Browse Mods"); btnBrowse.addActionListener(e -> openModBrowser()); hBtns.add(btnBrowse);
        JButton btnRef = mkBtn("\u21BB Refresh"); btnRef.addActionListener(e -> refreshMods()); hBtns.add(btnRef);
        JButton btnDir = mkBtn("Open Folder"); btnDir.addActionListener(e -> openDir(MODS_DIR)); hBtns.add(btnDir);
        header.add(hBtns, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setBackground(C_BG); split.setBorder(null); split.setDividerSize(4); split.setResizeWeight(0.6);

        modList = new JList<>(modListModel);
        modList.setBackground(C_CONSOLE); modList.setForeground(C_WHITE);
        modList.setSelectionBackground(C_ACCENT); modList.setSelectionForeground(C_WHITE);
        modList.setFont(new Font("Segoe UI", Font.PLAIN, 13)); modList.setFixedCellHeight(48);
        modList.setCellRenderer(new ModCellRenderer());
        modList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showModDetail(); });
        JScrollPane ls = new JScrollPane(modList);
        ls.setBorder(BorderFactory.createLineBorder(C_BORDER)); ls.getVerticalScrollBar().setUnitIncrement(16);
        split.setTopComponent(ls);

        modDetailArea = new JTextArea("Select a mod to view details...");
        modDetailArea.setEditable(false); modDetailArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        modDetailArea.setBackground(C_CONSOLE); modDetailArea.setForeground(C_CON_TEXT);
        modDetailArea.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        modDetailArea.setLineWrap(true); modDetailArea.setWrapStyleWord(true);
        JScrollPane ds = new JScrollPane(modDetailArea);
        ds.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER), BorderFactory.createLineBorder(C_BORDER)));
        split.setBottomComponent(ds);
        p.add(split, BorderLayout.CENTER);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        acts.setBackground(C_BG);
        JButton bt = mkBtn("Enable / Disable"); bt.addActionListener(e -> toggleMod()); acts.add(bt);
        JButton bd = mkBtn("Delete");            bd.addActionListener(e -> deleteMod()); acts.add(bd);
        acts.add(Box.createHorizontalStrut(12));
        JLabel hint = new JLabel("Reads fabric.mod.json \u2022 quilt.mod.json \u2022 mods.toml \u2022 mcmod.info from inside each .jar");
        hint.setForeground(C_DIM); hint.setFont(new Font("Segoe UI", Font.ITALIC, 11)); acts.add(hint);
        p.add(acts, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::refreshMods);
        return p;
    }

    // ── Mod cell renderer showing name/version/author from jar metadata ──
    class ModCellRenderer extends JPanel implements ListCellRenderer<ModInfo> {
        JLabel ico = new JLabel();
        JLabel nam = new JLabel();
        JLabel sub = new JLabel();

        ModCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            ico.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            ico.setPreferredSize(new Dimension(28, 28));
            add(ico, BorderLayout.WEST);
            JPanel txt = new JPanel(new GridLayout(2, 1));
            txt.setOpaque(false);
            nam.setFont(new Font("Segoe UI", Font.BOLD, 13));
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            txt.add(nam); txt.add(sub);
            add(txt, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ModInfo> list, ModInfo mod, int idx, boolean sel, boolean foc) {
            setBackground(sel ? C_ACCENT : (idx % 2 == 0 ? C_CONSOLE : C_CARD_ALT));
            boolean dis = mod.disabled;
            ico.setText(dis ? "\u25CB" : "\u25CF");
            ico.setForeground(dis ? C_DIM : C_GREEN);
            String display = (mod.modName != null && !mod.modName.isEmpty()) ? mod.modName : mod.fileName;
            if (dis) display += "  [DISABLED]";
            if (mod.subDir != null) display += "  \u2190 " + mod.subDir + "/";
            nam.setText(display); nam.setForeground(dis ? C_DIM : C_WHITE);
            StringBuilder s = new StringBuilder();
            if (mod.modVersion != null) s.append("v").append(mod.modVersion);
            if (mod.modAuthor != null) { if (s.length() > 0) s.append("  \u2022  "); s.append(mod.modAuthor); }
            if (mod.loader != null) { if (s.length() > 0) s.append("  \u2022  "); s.append(mod.loader); }
            if (s.length() == 0) s.append(mod.fileName);
            sub.setText(s.toString()); sub.setForeground(dis ? C_DIM : C_GREY);
            return this;
        }
    }

    void showModDetail() {
        ModInfo mod = modList.getSelectedValue();
        if (mod == null) { modDetailArea.setText("Select a mod to view details..."); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("\u2550\u2550\u2550 MOD DETAILS \u2550\u2550\u2550\n\n");
        sb.append("File:        ").append(mod.fileName).append('\n');
        if (mod.subDir != null) sb.append("Subfolder:   ").append(mod.subDir).append('\n');
        if (mod.filePath != null) sb.append("Full path:   ").append(mod.filePath).append('\n');
        sb.append("Status:      ").append(mod.disabled ? "DISABLED" : "ENABLED").append('\n');
        sb.append("Loader:      ").append(nn(mod.loader)).append('\n');
        sb.append("Mod ID:      ").append(nn(mod.modId)).append('\n');
        sb.append("Name:        ").append(nn(mod.modName)).append('\n');
        sb.append("Version:     ").append(nn(mod.modVersion)).append('\n');
        sb.append("Author(s):   ").append(nn(mod.modAuthor)).append('\n');
        sb.append("Description: ").append(nn(mod.modDescription)).append('\n');
        sb.append("License:     ").append(nn(mod.modLicense)).append('\n');
        sb.append("\nFile size:   ").append(fmtSize(mod.fileSize)).append('\n');
        modDetailArea.setText(sb.toString()); modDetailArea.setCaretPosition(0);
    }

    static String nn(String s) { return s != null ? s : "\u2014"; }
    static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1048576) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1048576.0);
    }

    /** Scan mods/ folder — picks up .jar, .jar.disabled, .litemod, .zip; resilient per-file */
    void refreshMods() {
        modListModel.clear();
        try {
            if (!Files.isDirectory(MODS_DIR)) { Files.createDirectories(MODS_DIR); return; }
            List<ModInfo> mods = new ArrayList<>();
            int errors = 0;

            // Collect ALL candidate files (non-recursive first, then one level deep)
            List<Path> candidates = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(MODS_DIR)) {
                stream.forEach(f -> {
                    if (Files.isRegularFile(f) && isModFile(f)) {
                        candidates.add(f);
                    } else if (Files.isDirectory(f)) {
                        // Some loaders support subdirectory mods (e.g. mods/config-mods/)
                        try (java.util.stream.Stream<Path> sub = Files.list(f)) {
                            sub.filter(sf -> Files.isRegularFile(sf) && isModFile(sf))
                               .forEach(candidates::add);
                        } catch (IOException ignored) {}
                    }
                });
            }

            // Sort alphabetically
            candidates.sort(Comparator.comparing(f -> f.getFileName().toString().toLowerCase()));

            // Read each file individually — one bad jar never kills the rest
            for (Path f : candidates) {
                try {
                    ModInfo info = readModJar(f);
                    if (info != null) mods.add(info);
                } catch (Exception e) {
                    // Absolute last resort — still show the file even if everything fails
                    ModInfo fallback = new ModInfo();
                    fallback.fileName = f.getFileName().toString();
                    fallback.disabled = fallback.fileName.toLowerCase().endsWith(".disabled");
                    try { fallback.fileSize = Files.size(f); } catch (IOException ignored) {}
                    fallback.loader = "Error";
                    fallback.modName = fallback.fileName;
                    mods.add(fallback);
                    errors++;
                    logErr("Mod read error: " + f.getFileName() + " \u2014 " + e.getMessage());
                }
            }

            for (ModInfo m : mods) modListModel.addElement(m);
            String countText = "Installed Mods (" + mods.size() + ")";
            if (errors > 0) countText += "  \u2022  " + errors + " error(s)";
            final String ct = countText;
            SwingUtilities.invokeLater(() -> { if (lblModCount != null) lblModCount.setText(ct); });
            log("Mods: found " + mods.size() + " mod(s) in " + MODS_DIR + (errors > 0 ? " (" + errors + " errors)" : ""));
            if (modDetailArea != null) modDetailArea.setText(mods.isEmpty()
                ? "No mods found in " + MODS_DIR + "\n\nDrop .jar files here or use \u2B73 Browse Mods to install from Modrinth."
                : "Select a mod to view details...");
        } catch (Exception e) { logErr("Mod scan: " + e.getMessage()); }
    }

    /** Check if a file looks like a mod file by extension */
    boolean isModFile(Path f) {
        String n = f.getFileName().toString().toLowerCase();
        return n.endsWith(".jar") || n.endsWith(".jar.disabled")
            || n.endsWith(".litemod") || n.endsWith(".litemod.disabled")
            || n.endsWith(".zip") || n.endsWith(".zip.disabled");
    }

    /** Opens a mod file and reads metadata — tries every known format, always returns a ModInfo (never null) */
    ModInfo readModJar(Path jar) {
        ModInfo info = new ModInfo();
        info.fileName = jar.getFileName().toString();
        info.filePath = jar;  // store full path for subdir mods
        info.disabled = info.fileName.toLowerCase().endsWith(".disabled");
        try { info.fileSize = Files.size(jar); } catch (IOException e) { info.fileSize = 0; }

        // Show relative path for subdir mods
        if (!jar.getParent().equals(MODS_DIR)) {
            info.subDir = MODS_DIR.relativize(jar.getParent()).toString();
        }

        try (ZipFile zf = new ZipFile(jar.toFile())) {

            // 1. Fabric: fabric.mod.json
            ZipEntry e = zf.getEntry("fabric.mod.json");
            if (e != null) {
                String json = readEntry(zf, e);
                info.loader = "Fabric";
                info.modId = jVal(json, "id");
                info.modName = jVal(json, "name");
                info.modVersion = jVal(json, "version");
                info.modDescription = jVal(json, "description");
                info.modLicense = jValOrArr(json, "license");
                info.modAuthor = jAuthors(json);
                return info;
            }

            // 2. Quilt: quilt.mod.json
            e = zf.getEntry("quilt.mod.json");
            if (e != null) {
                String json = readEntry(zf, e);
                info.loader = "Quilt";
                info.modId = jVal(json, "id");
                info.modName = jVal(json, "name");
                info.modVersion = jVal(json, "version");
                info.modDescription = jVal(json, "description");
                info.modLicense = jValOrArr(json, "license");
                info.modAuthor = jAuthors(json);
                return info;
            }

            // 3. Forge / NeoForge: META-INF/mods.toml
            e = zf.getEntry("META-INF/mods.toml");
            if (e != null) {
                String toml = readEntry(zf, e);
                info.loader = "Forge";
                info.modId = tVal(toml, "modId");
                info.modName = tVal(toml, "displayName");
                info.modVersion = tVal(toml, "version");
                info.modDescription = tVal(toml, "description");
                info.modLicense = tVal(toml, "license");
                info.modAuthor = tVal(toml, "authors");
                return info;
            }

            // 4. NeoForge: META-INF/neoforge.mods.toml
            e = zf.getEntry("META-INF/neoforge.mods.toml");
            if (e != null) {
                String toml = readEntry(zf, e);
                info.loader = "NeoForge";
                info.modId = tVal(toml, "modId");
                info.modName = tVal(toml, "displayName");
                info.modVersion = tVal(toml, "version");
                info.modDescription = tVal(toml, "description");
                info.modLicense = tVal(toml, "license");
                info.modAuthor = tVal(toml, "authors");
                return info;
            }

            // 5. Legacy Forge: mcmod.info
            e = zf.getEntry("mcmod.info");
            if (e != null) {
                String json = readEntry(zf, e);
                info.loader = "Forge (legacy)";
                info.modId = jVal(json, "modid");
                info.modName = jVal(json, "name");
                info.modVersion = jVal(json, "version");
                info.modDescription = jVal(json, "description");
                info.modAuthor = jArr(json, "authorList");
                if (info.modAuthor == null) info.modAuthor = jArr(json, "authors");
                if (info.modAuthor == null) info.modAuthor = jVal(json, "authors");
                return info;
            }

            // 6. LiteLoader: litemod.json
            e = zf.getEntry("litemod.json");
            if (e != null) {
                String json = readEntry(zf, e);
                info.loader = "LiteLoader";
                info.modName = jVal(json, "name");
                info.modVersion = jVal(json, "version");
                info.modDescription = jVal(json, "description");
                info.modAuthor = jVal(json, "author");
                return info;
            }

            // 7. Fallback: try MANIFEST.MF for any metadata
            e = zf.getEntry("META-INF/MANIFEST.MF");
            if (e != null) {
                String mf = readEntry(zf, e);
                info.loader = "Unknown";
                info.modName = mfVal(mf, "Implementation-Title");
                if (info.modName == null) info.modName = mfVal(mf, "Specification-Title");
                info.modVersion = mfVal(mf, "Implementation-Version");
                if (info.modVersion == null) info.modVersion = mfVal(mf, "Specification-Version");
                info.modAuthor = mfVal(mf, "Implementation-Vendor");
                if (info.modName != null) return info;
            }

            // 8. Last resort — list some entry names to help identify it
            info.loader = "Unknown";
            Enumeration<? extends ZipEntry> entries = zf.entries();
            StringBuilder contents = new StringBuilder();
            int count = 0;
            while (entries.hasMoreElements() && count < 8) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("META-INF/") && !name.endsWith("/")) {
                    contents.append(name).append(", ");
                    count++;
                }
            }
            if (contents.length() > 0) info.modDescription = "Contents: " + contents.toString();

        } catch (Exception ex) {
            info.loader = "Error: " + ex.getMessage();
        }
        return info;
    }

    /** Extract MANIFEST.MF header value */
    String mfVal(String mf, String key) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(key) + ":\\s*(.+)").matcher(mf);
        return m.find() ? m.group(1).trim() : null;
    }

    String readEntry(ZipFile zf, ZipEntry entry) throws IOException {
        try (InputStream is = zf.getInputStream(entry); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096]; int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }

    /** Extract "key": "value" from JSON */
    String jVal(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*?)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Extract "key": ["a","b"] as comma-joined string */
    String jArr(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*?)\\]").matcher(json);
        if (!m.find()) return null;
        String inner = m.group(1);
        // Extract all quoted strings (handles both simple strings and "name" fields inside objects)
        Matcher sm = Pattern.compile("\"([^\"]+)\"").matcher(inner);
        List<String> items = new ArrayList<>();
        while (sm.find()) {
            String val = sm.group(1);
            // Skip JSON keys like "name", "contact", "email" etc. — only take values after colons
            // But also take bare strings (non-object array elements)
            if (!val.equals("name") && !val.equals("contact") && !val.equals("email")
                && !val.equals("homepage") && !val.equals("sources") && !val.equals("issues"))
                items.add(val);
        }
        return items.isEmpty() ? null : String.join(", ", items);
    }

    /** Fabric/Quilt authors: can be ["str"] or [{"name":"str"}] — handle both */
    String jAuthors(String json) {
        // First try: "authors": [...] with "name" fields inside objects
        Matcher arrM = Pattern.compile("\"authors\"\\s*:\\s*\\[([^\\]]*?)\\]", Pattern.DOTALL).matcher(json);
        if (arrM.find()) {
            String inner = arrM.group(1).trim();
            List<String> names = new ArrayList<>();
            // Try extracting "name": "value" pairs (object-style authors)
            Matcher nameM = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(inner);
            while (nameM.find()) names.add(nameM.group(1));
            if (!names.isEmpty()) return String.join(", ", names);
            // Fallback: plain string array ["Author1", "Author2"]
            Matcher strM = Pattern.compile("\"([^\"]+)\"").matcher(inner);
            while (strM.find()) names.add(strM.group(1));
            if (!names.isEmpty()) return String.join(", ", names);
        }
        // Fallback: "author": "string"
        return jVal(json, "author");
    }

    /** Try "key": "value" first, then "key": ["a","b"] — for fields like license that can be either */
    String jValOrArr(String json, String key) {
        String val = jVal(json, key);
        if (val != null) return val;
        return jArr(json, key);
    }

    /** Extract key = "value" from TOML */
    String tVal(String toml, String key) {
        Matcher m = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*\"([^\"]*?)\"").matcher(toml);
        if (m.find()) return m.group(1);
        m = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*(.+)").matcher(toml);
        return m.find() ? m.group(1).trim() : null;
    }

    void toggleMod() {
        ModInfo mod = modList.getSelectedValue();
        if (mod == null) return;
        Path src = mod.filePath != null ? mod.filePath : MODS_DIR.resolve(mod.fileName);
        try {
            if (mod.disabled) {
                String en = mod.fileName.substring(0, mod.fileName.length() - ".disabled".length());
                Files.move(src, src.getParent().resolve(en)); log("Enabled: " + en);
            } else {
                Files.move(src, src.getParent().resolve(mod.fileName + ".disabled")); log("Disabled: " + mod.fileName);
            }
            refreshMods();
        } catch (Exception e) { logErr("Toggle: " + e.getMessage()); }
    }

    void deleteMod() {
        ModInfo mod = modList.getSelectedValue();
        if (mod == null) return;
        String name = mod.modName != null ? mod.modName : mod.fileName;
        int ok = JOptionPane.showConfirmDialog(this, "Delete mod: " + name + "?\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        Path target = mod.filePath != null ? mod.filePath : MODS_DIR.resolve(mod.fileName);
        try { Files.deleteIfExists(target); log("Deleted: " + mod.fileName); refreshMods(); }
        catch (Exception e) { logErr("Delete: " + e.getMessage()); }
    }

    /** Holds metadata parsed from inside a mod .jar */
    static class ModInfo {
        String fileName;
        Path filePath;    // full path (for subdir mods)
        String subDir;    // null if in mods/ root, else relative subdir name
        boolean disabled;
        long fileSize;
        String loader, modId, modName, modVersion, modAuthor, modDescription, modLicense;
        @Override public String toString() { return modName != null ? modName : fileName; }
    }

    // ── SETTINGS CARD ────────────────────────────────────────────────
    JPanel buildSettingsCard() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22)); title.setForeground(C_WHITE);
        p.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(C_BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 4, 8, 12); gc.anchor = GridBagConstraints.WEST;
        int row = 0;

        gc.gridx = 0; gc.gridy = row; grid.add(dimLbl("Java Path:"), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
        tfJavaPath = mkField(getDefaultJava(), 300); grid.add(tfJavaPath, gc);
        gc.fill = GridBagConstraints.NONE; gc.weightx = 0; row++;

        gc.gridx = 0; gc.gridy = row; grid.add(dimLbl("Show Snapshots:"), gc);
        gc.gridx = 1; cbSnapshots = mkCheck("Include snapshot versions", null);
        cbSnapshots.addActionListener(e -> refreshVersionList()); grid.add(cbSnapshots, gc); row++;

        gc.gridx = 0; gc.gridy = row; grid.add(dimLbl("Fullscreen:"), gc);
        gc.gridx = 1; cbFullscreen = mkCheck("Launch in fullscreen mode", null); grid.add(cbFullscreen, gc); row++;

        gc.gridx = 0; gc.gridy = row; grid.add(dimLbl("Game Dir:"), gc);
        gc.gridx = 1; JLabel dl = new JLabel(ROOT.toString()); dl.setForeground(C_GREY); dl.setFont(new Font("Consolas", Font.PLAIN, 11)); grid.add(dl, gc); row++;

        gc.gridx = 0; gc.gridy = row; grid.add(dimLbl("Mods Dir:"), gc);
        gc.gridx = 1; JLabel ml = new JLabel(MODS_DIR.toString()); ml.setForeground(C_GREY); ml.setFont(new Font("Consolas", Font.PLAIN, 11)); grid.add(ml, gc); row++;

        gc.gridx = 0; gc.gridy = row;
        JButton br = mkBtn("Open Game Dir"); br.addActionListener(e -> openDir(ROOT)); grid.add(br, gc);
        gc.gridx = 1;
        JButton bm = mkBtn("Open Mods Dir"); bm.addActionListener(e -> openDir(MODS_DIR)); grid.add(bm, gc);

        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    // ── CONSOLE CARD ─────────────────────────────────────────────────
    JPanel buildConsoleCard() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel hdr = new JPanel(new BorderLayout()); hdr.setBackground(C_BG);
        JLabel t = new JLabel("Console Output"); t.setFont(new Font("Segoe UI", Font.BOLD, 22)); t.setForeground(C_WHITE);
        hdr.add(t, BorderLayout.WEST);
        JButton bc = mkBtn("Clear"); bc.addActionListener(e -> taLog.setText("")); hdr.add(bc, BorderLayout.EAST);
        p.add(hdr, BorderLayout.NORTH);
        taLog = new JTextArea();
        taLog.setEditable(false); taLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        taLog.setBackground(C_CONSOLE); taLog.setForeground(C_CON_TEXT); taLog.setCaretColor(C_CON_TEXT);
        JScrollPane sp = new JScrollPane(taLog);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER)); sp.getVerticalScrollBar().setUnitIncrement(16);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── ABOUT CARD ───────────────────────────────────────────────────
    JPanel buildAboutCard() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(C_BG);
        JPanel box = new JPanel(); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS)); box.setBackground(C_BG);
        box.add(mkLabel("CTBadlion", new Font("Segoe UI", Font.BOLD, 32), C_ACCENT));
        box.add(Box.createVerticalStrut(6));
        box.add(mkLabel("Version " + VER, new Font("Segoe UI", Font.PLAIN, 14), C_GREY));
        box.add(Box.createVerticalStrut(20));
        box.add(mkLabel("Open-source Minecraft Java Edition launcher", new Font("Segoe UI", Font.PLAIN, 13), C_DIM));
        box.add(mkLabel("with Fabric + Fabulously Optimized support.", new Font("Segoe UI", Font.PLAIN, 13), C_DIM));
        box.add(Box.createVerticalStrut(12));
        box.add(mkLabel("Reads mod metadata directly from .jar files:", new Font("Segoe UI", Font.PLAIN, 12), C_DIM));
        box.add(mkLabel("fabric.mod.json \u2022 quilt.mod.json \u2022 mods.toml \u2022 mcmod.info", new Font("Consolas", Font.PLAIN, 11), C_GREY));
        box.add(Box.createVerticalStrut(24));
        box.add(mkLabel("Made by Team Flames / Samsoft / Flames Co.", new Font("Segoe UI", Font.BOLD, 12), C_GREY));
        box.add(Box.createVerticalStrut(8));
        box.add(mkLabel("Licensed under GNU GPL-3.0 \u2014 Free and Open Source Software", new Font("Segoe UI", Font.PLAIN, 11), C_DIM));
        box.add(Box.createVerticalStrut(30));
        box.add(mkLabel("Badlion-inspired aesthetic \u2022 Not affiliated with Mojang or Badlion", new Font("Segoe UI", Font.ITALIC, 10), C_DIM));
        p.add(box);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UI FACTORIES
    // ═══════════════════════════════════════════════════════════════════
    JLabel dimLbl(String t) { JLabel l = new JLabel(t); l.setForeground(C_DIM); l.setFont(new Font("Segoe UI", Font.PLAIN, 12)); return l; }
    JTextField mkField(String t, int w) {
        JTextField f = new JTextField(t); f.setPreferredSize(new Dimension(w, 26));
        f.setBackground(C_FIELD); f.setForeground(C_WHITE); f.setCaretColor(C_WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        return f;
    }
    JCheckBox mkCheck(String t, String tip) {
        JCheckBox cb = new JCheckBox(t); cb.setBackground(C_TOPBAR); cb.setForeground(C_WHITE); cb.setFocusPainted(false);
        if (tip != null) cb.setToolTipText(tip); return cb;
    }
    JButton mkBtn(String t) {
        JButton b = new JButton(t); b.setBackground(C_FIELD); b.setForeground(C_WHITE); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER), BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(C_HOVER); }
            public void mouseExited(MouseEvent e)  { b.setBackground(C_FIELD); }
        });
        return b;
    }
    JButton mkAccentBtn(String t) {
        JButton b = new JButton(t); b.setBackground(C_ACCENT); b.setForeground(C_WHITE); b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_ACCENT_DIM), BorderFactory.createEmptyBorder(5, 16, 5, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(C_ACCENT_HI); }
            public void mouseExited(MouseEvent e)  { b.setBackground(C_ACCENT); }
        });
        return b;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VERSION MANIFEST
    // ═══════════════════════════════════════════════════════════════════
    void fetchManifest() {
        setStatus("Fetching versions...");
        executor.submit(() -> {
            try {
                String json = httpGet(MANIFEST_URL);
                parseManifest(json);
                SwingUtilities.invokeLater(() -> { refreshVersionList(); setStatus("Ready \u2014 " + allVersions.size() + " versions"); });
            } catch (Exception e) {
                logErr("Manifest: " + e.getMessage()); setStatus("Offline"); scanLocalVersions();
            }
        });
    }

    void parseManifest(String json) {
        allVersions.clear();
        Matcher m = Pattern.compile("\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"type\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        while (m.find()) allVersions.add(new String[]{m.group(1), m.group(2), m.group(3)});
        log("Parsed " + allVersions.size() + " versions");
    }

    void scanLocalVersions() {
        try {
            if (!Files.isDirectory(VER_DIR)) return;
            Files.list(VER_DIR).filter(Files::isDirectory).forEach(dir -> {
                String id = dir.getFileName().toString();
                if (Files.exists(dir.resolve(id + ".json"))) allVersions.add(new String[]{id, "local", ""});
            });
            if (!allVersions.isEmpty()) { log(allVersions.size() + " local version(s)"); SwingUtilities.invokeLater(this::refreshVersionList); }
        } catch (Exception e) { logErr("Local scan: " + e.getMessage()); }
    }

    void refreshVersionList() {
        boolean snaps = cbSnapshots != null && cbSnapshots.isSelected();
        dispVersions.clear();
        synchronized (allVersions) {
            for (String[] v : allVersions) if (snaps || "release".equals(v[1]) || "local".equals(v[1])) dispVersions.add(v);
        }
        ddVersion.updateList();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LAUNCH PIPELINE
    // ═══════════════════════════════════════════════════════════════════
    void onPlayClick() {
        if (launching) return;
        String[] ver = ddVersion.getSelected();
        if (ver == null) { logErr("No version selected"); return; }
        saveProfile(); launching = true;
        btnPlay.setEnabled(false); btnPlay.setBackground(C_DIM); btnPlay.setText("..."); progBar.setValue(0);
        for (Component c : sidebarPanel.getComponents())
            if (c instanceof JButton && "console".equals(((JButton) c).getClientProperty("card")))
                { switchCard("console", (JButton) c); break; }
        executor.submit(() -> {
            try { prepareAndLaunch(ver[0], ver[2]); }
            catch (Exception e) { logErr("Launch: " + e.getMessage()); e.printStackTrace(); }
            finally {
                launching = false;
                SwingUtilities.invokeLater(() -> {
                    btnPlay.setEnabled(true); btnPlay.setBackground(C_ACCENT); btnPlay.setText("LAUNCH");
                    setStatus("Ready"); progBar.setValue(0); lblProgress.setText("");
                });
            }
        });
    }

    void prepareAndLaunch(String verId, String jsonUrl) throws Exception {
        log("\u2550\u2550\u2550 Preparing: " + verId + " \u2550\u2550\u2550");

        // 1 — Version JSON
        setStatus("Version JSON...");
        Path verDir = VER_DIR.resolve(verId); Path jsonPath = verDir.resolve(verId + ".json");
        Files.createDirectories(verDir);
        if (!Files.exists(jsonPath)) {
            if (jsonUrl == null || jsonUrl.isEmpty()) throw new Exception("No URL for version JSON");
            log("Downloading version JSON..."); dl(jsonUrl, jsonPath, "version JSON");
        }
        String verJson = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);

        // 2 — Client JAR
        setStatus("Client JAR...");
        Path clientJar = verDir.resolve(verId + ".jar");
        if (!Files.exists(clientJar)) {
            String u = rx1(verJson, "\"client\"\\s*:\\s*\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");
            if (u != null) { log("Downloading client JAR..."); dl(u, clientJar, "client JAR"); }
            else throw new Exception("No client JAR URL");
        }

        // 3 — Libraries
        setStatus("Libraries...");
        StringBuilder cp = new StringBuilder();
        dlLibs(verJson, cp);
        cp.append(clientJar);

        // 4 — Natives
        setStatus("Natives...");
        Path nDir = NAT_DIR.resolve(verId); Files.createDirectories(nDir);
        extractNatives(verJson, nDir);

        // 5 — Assets
        setStatus("Assets...");
        String assetId = rx1(verJson, "\"assetIndex\"\\s*:\\s*\\{[^}]*?\"id\"\\s*:\\s*\"([^\"]+)\"");
        if (assetId == null) assetId = "legacy";
        dlAssets(verJson, assetId);

        // 6 — Fabric
        if (cbFabric.isSelected()) { setStatus("Fabric..."); installFabric(verId, cp); }

        // 7 — FO hint
        if (cbFabOpt.isSelected()) {
            log("Fabulously Optimized: ensure mods are in " + MODS_DIR);
            log("https://modrinth.com/modpack/fabulously-optimized");
        }

        // 8 — Build command
        setStatus("Launching " + verId + "...");
        List<String> cmd = new ArrayList<>();
        String java = (tfJavaPath != null && !tfJavaPath.getText().trim().isEmpty()) ? tfJavaPath.getText().trim() : getDefaultJava();
        cmd.add(java);
        if (isMac()) { cmd.add("-XstartOnFirstThread"); log("macOS: -XstartOnFirstThread"); }
        if (javaMajor() >= 9) {
            for (String f : new String[]{
                "--add-opens=java.base/java.net=ALL-UNNAMED", "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-exports=java.base/sun.security.action=ALL-UNNAMED"
            }) cmd.add(f);
        }
        cmd.add("-Xmx" + tfRam.getText().trim() + "M"); cmd.add("-Xms256M");
        cmd.add("-Djava.library.path=" + nDir);
        cmd.add("-Dminecraft.launcher.brand=" + APP); cmd.add("-Dminecraft.launcher.version=" + VER);
        cmd.add("-cp"); cmd.add(cp.toString());

        String mainClass = rx1(verJson, "\"mainClass\"\\s*:\\s*\"([^\"]+)\"");
        Path fabricMarker = verDir.resolve("fabric_main.txt");
        if (cbFabric.isSelected() && Files.exists(fabricMarker))
            mainClass = new String(Files.readAllBytes(fabricMarker), StandardCharsets.UTF_8).trim();
        if (mainClass == null) mainClass = "net.minecraft.client.main.Main";
        cmd.add(mainClass);

        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + tfUser.getText().trim()).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        cmd.add("--version"); cmd.add(verId);
        cmd.add("--gameDir"); cmd.add(ROOT.toString());
        cmd.add("--assetsDir"); cmd.add(ASS_DIR.toString());
        cmd.add("--assetIndex"); cmd.add(assetId);
        cmd.add("--uuid"); cmd.add(uuid);
        cmd.add("--accessToken"); cmd.add("0");
        cmd.add("--userType"); cmd.add("legacy");
        cmd.add("--username"); cmd.add(tfUser.getText().trim());
        if (cbFullscreen != null && cbFullscreen.isSelected()) cmd.add("--fullscreen");

        log("Main: " + mainClass); log("Args: " + cmd.size());
        ProcessBuilder pb = new ProcessBuilder(cmd); pb.directory(ROOT.toFile()); pb.redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line; while ((line = br.readLine()) != null) log("[MC] " + line);
        }
        log("Minecraft exited: " + proc.waitFor());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LIBRARY DOWNLOADING
    // ═══════════════════════════════════════════════════════════════════
    void dlLibs(String verJson, StringBuilder cp) {
        log("Resolving libraries...");
        Matcher m = Pattern.compile("\"artifact\"\\s*:\\s*\\{[^}]*?\"path\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL).matcher(verJson);
        List<String[]> libs = new ArrayList<>();
        while (m.find()) libs.add(new String[]{m.group(1), m.group(2)});
        log(libs.size() + " library artifacts");
        for (int i = 0; i < libs.size(); i++) {
            String path = libs.get(i)[0], url = libs.get(i)[1];
            Path target = LIB_DIR.resolve(path.replace("/", File.separator));
            cp.append(target).append(File.pathSeparator);
            if (!Files.exists(target)) {
                try { Files.createDirectories(target.getParent()); dl(url, target, path); }
                catch (Exception e) { logErr("Lib: " + path + " \u2014 " + e.getMessage()); }
            }
            updateProg(i + 1, libs.size(), "Libraries");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NATIVES
    // ═══════════════════════════════════════════════════════════════════
    void extractNatives(String verJson, Path nDir) {
        String key = osNativeKey(); log("Natives: " + key);
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\{[^}]*?\"path\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL).matcher(verJson);
        int c = 0;
        while (m.find()) {
            String path = m.group(1), url = m.group(2);
            Path jar = LIB_DIR.resolve(path.replace("/", File.separator));
            try {
                if (!Files.exists(jar)) { Files.createDirectories(jar.getParent()); dl(url, jar, "native: " + path); }
                try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar))) {
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        String n = e.getName();
                        if (e.isDirectory() || n.startsWith("META-INF")) continue;
                        if (n.endsWith(".so") || n.endsWith(".dll") || n.endsWith(".dylib") || n.endsWith(".jnilib")) {
                            Path out = nDir.resolve(Paths.get(n).getFileName());
                            if (!Files.exists(out)) Files.copy(zis, out);
                        }
                    }
                }
                c++;
            } catch (Exception e) { logErr("Native: " + path); }
        }
        log("Extracted " + c + " native jar(s)");
    }

    String osNativeKey() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "natives-windows" : os.contains("mac") ? "natives-macos" : "natives-linux";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ASSETS
    // ═══════════════════════════════════════════════════════════════════
    void dlAssets(String verJson, String indexId) {
        String indexUrl = rx1(verJson, "\"assetIndex\"\\s*:\\s*\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");
        if (indexUrl == null) { log("No asset index \u2014 skipping"); return; }
        Path indexFile = ASS_DIR.resolve("indexes").resolve(indexId + ".json");
        try {
            if (!Files.exists(indexFile)) { log("Asset index: " + indexId); dl(indexUrl, indexFile, "asset index"); }
            String idx = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
            Matcher hm = Pattern.compile("\"hash\"\\s*:\\s*\"([a-f0-9]{40})\"").matcher(idx);
            List<String> hashes = new ArrayList<>();
            while (hm.find()) hashes.add(hm.group(1));
            log("Assets: " + hashes.size() + " objects");
            int nd = 0;
            for (int i = 0; i < hashes.size(); i++) {
                String h = hashes.get(i), pfx = h.substring(0, 2);
                Path obj = ASS_DIR.resolve("objects").resolve(pfx).resolve(h);
                if (!Files.exists(obj)) {
                    Files.createDirectories(obj.getParent());
                    try { dlQuiet(RESOURCES_URL + pfx + "/" + h, obj); nd++; } catch (Exception ignored) {}
                }
                if (i % 50 == 0) updateProg(i + 1, hashes.size(), "Assets");
            }
            log("Assets: " + nd + " new, " + (hashes.size() - nd) + " cached");
        } catch (Exception e) { logErr("Assets: " + e.getMessage()); }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FABRIC
    // ═══════════════════════════════════════════════════════════════════
    void installFabric(String mcVer, StringBuilder cp) {
        log("Installing Fabric for MC " + mcVer + "...");
        try {
            String meta = httpGet(FABRIC_META_URL + mcVer);
            String lv = rx1(meta, "\"version\"\\s*:\\s*\"([0-9]+\\.[0-9]+\\.[0-9]+)\"");
            if (lv == null) { logErr("Fabric: no loader for " + mcVer); return; }
            log("Fabric Loader: " + lv);
            String profile = httpGet(FABRIC_META_URL + mcVer + "/" + lv + "/profile/json");
            String fm = rx1(profile, "\"mainClass\"\\s*:\\s*\"([^\"]+)\"");
            if (fm != null) log("Fabric main: " + fm);
            Matcher lm = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL).matcher(profile);
            int c = 0;
            while (lm.find()) {
                String coord = lm.group(1), repo = lm.group(2);
                Path lp = mavenPath(coord); if (lp == null) continue;
                Path target = LIB_DIR.resolve(lp);
                cp.insert(0, target + File.pathSeparator);
                if (!Files.exists(target)) {
                    try { Files.createDirectories(target.getParent()); dl(repo + lp.toString().replace(File.separator, "/"), target, "fabric: " + coord); c++; }
                    catch (Exception e) { logErr("Fabric lib: " + coord); }
                }
            }
            log("Fabric: " + c + " new libs");
            if (fm != null) {
                Files.write(VER_DIR.resolve(mcVer).resolve("fabric_main.txt"), fm.getBytes(StandardCharsets.UTF_8));
                log("Fabric complete");
            }
        } catch (Exception e) { logErr("Fabric: " + e.getMessage()); log("Manual: https://fabricmc.net/use/installer/"); }
    }

    Path mavenPath(String coord) {
        String[] p = coord.split(":"); if (p.length < 3) return null;
        return Paths.get(p[0].replace('.', File.separatorChar), p[1], p[2], p[1] + "-" + p[2] + ".jar");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NETWORK
    // ═══════════════════════════════════════════════════════════════════
    String httpGet(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", APP + "/" + VER);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    void dl(String url, Path target, String label) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", APP + "/" + VER); c.setConnectTimeout(10000); c.setReadTimeout(30000);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode() + " " + label);
        long total = c.getContentLengthLong(), cur = 0;
        try (InputStream in = c.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n); cur += n;
                if (total > 0) { final int pct = (int)((cur * 100) / total); SwingUtilities.invokeLater(() -> progBar.setValue(pct)); }
            }
        }
    }

    void dlQuiet(String url, Path target) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", APP + "/" + VER); c.setConnectTimeout(5000); c.setReadTimeout(10000);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        try (InputStream in = c.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════
    String rx1(String s, String regex) { Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(s); return m.find() ? m.group(1) : null; }
    String getDefaultJava() { return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"; }
    boolean isMac() { return System.getProperty("os.name", "").toLowerCase().contains("mac"); }
    int javaMajor() {
        String v = System.getProperty("java.version", "1.8");
        if (v.startsWith("1.")) v = v.substring(2);
        int d = v.indexOf('.'); if (d > 0) v = v.substring(0, d);
        int h = v.indexOf('-'); if (h > 0) v = v.substring(0, h);
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return 8; }
    }
    void openDir(Path dir) { try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir.toFile()); } catch (Exception e) { logErr("Open: " + e.getMessage()); } }

    // ═══════════════════════════════════════════════════════════════════
    //  PROFILE
    // ═══════════════════════════════════════════════════════════════════
    void saveProfile() {
        Properties p = new Properties();
        p.setProperty("user", tfUser.getText()); p.setProperty("ram", tfRam.getText());
        if (tfJavaPath != null) p.setProperty("java", tfJavaPath.getText());
        p.setProperty("fabric", String.valueOf(cbFabric.isSelected()));
        p.setProperty("fabopt", String.valueOf(cbFabOpt.isSelected()));
        if (cbSnapshots != null) p.setProperty("snapshots", String.valueOf(cbSnapshots.isSelected()));
        if (cbFullscreen != null) p.setProperty("fullscreen", String.valueOf(cbFullscreen.isSelected()));
        int idx = ddVersion.getSelectedIndex();
        if (idx >= 0 && idx < dispVersions.size()) p.setProperty("version", dispVersions.get(idx)[0]);
        try (OutputStream out = Files.newOutputStream(PROF_FILE)) { p.store(out, APP + " v" + VER); } catch (Exception ignored) {}
    }

    void loadProfile() {
        if (!Files.exists(PROF_FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PROF_FILE)) {
            p.load(in);
            if (p.containsKey("user")) tfUser.setText(p.getProperty("user"));
            if (p.containsKey("ram")) tfRam.setText(p.getProperty("ram"));
            if (p.containsKey("java") && tfJavaPath != null) tfJavaPath.setText(p.getProperty("java"));
            if (p.containsKey("fabric")) cbFabric.setSelected(Boolean.parseBoolean(p.getProperty("fabric")));
            if (p.containsKey("fabopt")) cbFabOpt.setSelected(Boolean.parseBoolean(p.getProperty("fabopt")));
            if (p.containsKey("snapshots") && cbSnapshots != null) cbSnapshots.setSelected(Boolean.parseBoolean(p.getProperty("snapshots")));
            if (p.containsKey("fullscreen") && cbFullscreen != null) cbFullscreen.setSelected(Boolean.parseBoolean(p.getProperty("fullscreen")));
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LOGGING
    // ═══════════════════════════════════════════════════════════════════
    void log(String s) {
        String ts = String.format("[%tT] %s", System.currentTimeMillis(), s);
        SwingUtilities.invokeLater(() -> { if (taLog != null) { taLog.append(ts + "\n"); taLog.setCaretPosition(taLog.getDocument().getLength()); } });
    }
    void logErr(String s) {
        String ts = String.format("[%tT] [ERR] %s", System.currentTimeMillis(), s);
        SwingUtilities.invokeLater(() -> { if (taLog != null) { taLog.append(ts + "\n"); taLog.setCaretPosition(taLog.getDocument().getLength()); } });
    }
    void setStatus(String s) { SwingUtilities.invokeLater(() -> { if (lblStatus != null) lblStatus.setText(s); }); }
    void updateProg(int done, int total, String label) {
        int pct = total > 0 ? (done * 100) / total : 0;
        SwingUtilities.invokeLater(() -> { progBar.setValue(pct); lblProgress.setText(label + ": " + done + "/" + total); });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOD BROWSER — Modrinth Search & Download
    // ═══════════════════════════════════════════════════════════════════
    void openModBrowser() {
        ModBrowserDialog dlg = new ModBrowserDialog(this);
        dlg.setVisible(true);
        // Refresh installed mods after browser closes
        refreshMods();
    }

    /** Full Modrinth mod browser dialog with search, results, and one-click install */
    class ModBrowserDialog extends JDialog {
        static final String MODRINTH_SEARCH = "https://api.modrinth.com/v2/search";
        static final String MODRINTH_PROJ   = "https://api.modrinth.com/v2/project/";

        JTextField searchField;
        JComboBox<String> loaderFilter, categoryFilter, sortFilter;
        DefaultListModel<ModResult> resultModel = new DefaultListModel<>();
        JList<ModResult> resultList;
        JTextArea detailArea;
        JButton btnInstall, btnSearch;
        JLabel lblInfo;
        int currentOffset = 0;

        ModBrowserDialog(JFrame parent) {
            super(parent, "Browse Mods \u2014 Modrinth", true);
            setSize(780, 580);
            setMinimumSize(new Dimension(640, 460));
            setLocationRelativeTo(parent);
            getContentPane().setBackground(C_BG);
            setLayout(new BorderLayout(0, 0));

            // ── Top: Search bar ──
            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setBackground(C_TOPBAR);
            top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

            searchField = mkField("Search mods...", 200);
            searchField.setPreferredSize(new Dimension(220, 28));
            searchField.addActionListener(e -> doSearch(0));
            top.add(searchField, BorderLayout.CENTER);

            JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            filters.setBackground(C_TOPBAR);

            loaderFilter = mkCombo(new String[]{"All Loaders", "fabric", "forge", "quilt", "neoforge"});
            filters.add(loaderFilter);

            categoryFilter = mkCombo(new String[]{"All Categories", "optimization", "library", "utility", "decoration", "worldgen", "technology", "adventure", "magic", "storage", "food", "equipment", "mobs"});
            filters.add(categoryFilter);

            sortFilter = mkCombo(new String[]{"Relevance", "Downloads", "Newest", "Updated"});
            filters.add(sortFilter);

            btnSearch = mkAccentBtn("\u2315 Search");
            btnSearch.addActionListener(e -> doSearch(0));
            filters.add(btnSearch);

            top.add(filters, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // ── Center: Split — results list + detail ──
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setBackground(C_BG); split.setBorder(null); split.setDividerSize(4); split.setResizeWeight(0.55);

            resultList = new JList<>(resultModel);
            resultList.setBackground(C_CONSOLE); resultList.setForeground(C_WHITE);
            resultList.setSelectionBackground(C_ACCENT); resultList.setSelectionForeground(C_WHITE);
            resultList.setFixedCellHeight(64);
            resultList.setCellRenderer(new ModResultRenderer());
            resultList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showResultDetail(); });
            JScrollPane rl = new JScrollPane(resultList);
            rl.setBorder(BorderFactory.createLineBorder(C_BORDER)); rl.getVerticalScrollBar().setUnitIncrement(16);
            split.setLeftComponent(rl);

            detailArea = new JTextArea("Search for mods to get started...\n\nPowered by the Modrinth API\nhttps://modrinth.com");
            detailArea.setEditable(false); detailArea.setFont(new Font("Consolas", Font.PLAIN, 12));
            detailArea.setBackground(C_CONSOLE); detailArea.setForeground(C_CON_TEXT);
            detailArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            detailArea.setLineWrap(true); detailArea.setWrapStyleWord(true);
            JScrollPane dl = new JScrollPane(detailArea);
            dl.setBorder(BorderFactory.createLineBorder(C_BORDER));
            split.setRightComponent(dl);

            add(split, BorderLayout.CENTER);

            // ── Bottom: Install button + paging ──
            JPanel bot = new JPanel(new BorderLayout());
            bot.setBackground(C_TOPBAR);
            bot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

            JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            leftBtns.setBackground(C_TOPBAR);

            btnInstall = mkAccentBtn("\u2B73 Install Selected Mod");
            btnInstall.setEnabled(false);
            btnInstall.addActionListener(e -> installSelectedMod());
            leftBtns.add(btnInstall);

            JButton btnPrev = mkBtn("\u25C0 Prev");
            btnPrev.addActionListener(e -> { if (currentOffset >= 20) doSearch(currentOffset - 20); });
            leftBtns.add(btnPrev);

            JButton btnNext = mkBtn("Next \u25B6");
            btnNext.addActionListener(e -> doSearch(currentOffset + 20));
            leftBtns.add(btnNext);

            bot.add(leftBtns, BorderLayout.WEST);

            lblInfo = new JLabel("Ready"); lblInfo.setForeground(C_DIM); lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bot.add(lblInfo, BorderLayout.EAST);

            add(bot, BorderLayout.SOUTH);
        }

        JComboBox<String> mkCombo(String[] items) {
            JComboBox<String> cb = new JComboBox<>(items);
            cb.setBackground(C_FIELD); cb.setForeground(C_WHITE);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            cb.setPreferredSize(new Dimension(120, 26));
            return cb;
        }

        void doSearch(int offset) {
            currentOffset = offset;
            String query = searchField.getText().trim();
            btnSearch.setEnabled(false);
            lblInfo.setText("Searching...");
            resultModel.clear();

            executor.submit(() -> {
                try {
                    StringBuilder url = new StringBuilder(MODRINTH_SEARCH);
                    url.append("?limit=20&offset=").append(offset);
                    url.append("&query=").append(URLEncoder.encode(query, "UTF-8"));

                    // Build facets: [[loader],[category],[project_type:mod]]
                    List<String> facets = new ArrayList<>();
                    facets.add("[\"project_type:mod\"]");

                    String loader = (String) loaderFilter.getSelectedItem();
                    if (loader != null && !"All Loaders".equals(loader))
                        facets.add("[\"categories:" + loader + "\"]");

                    String cat = (String) categoryFilter.getSelectedItem();
                    if (cat != null && !"All Categories".equals(cat))
                        facets.add("[\"categories:" + cat + "\"]");

                    url.append("&facets=[").append(String.join(",", facets)).append("]");

                    // Sort
                    String sort = (String) sortFilter.getSelectedItem();
                    if (sort != null) {
                        switch (sort) {
                            case "Downloads": url.append("&index=downloads"); break;
                            case "Newest":    url.append("&index=newest"); break;
                            case "Updated":   url.append("&index=updated"); break;
                            default:          url.append("&index=relevance"); break;
                        }
                    }

                    String json = httpGet(url.toString());
                    List<ModResult> results = parseSearchResults(json);

                    // Extract total_hits
                    String totalStr = rx1(json, "\"total_hits\"\\s*:\\s*(\\d+)");
                    int total = totalStr != null ? Integer.parseInt(totalStr) : results.size();

                    SwingUtilities.invokeLater(() -> {
                        for (ModResult r : results) resultModel.addElement(r);
                        lblInfo.setText(results.size() + " results (page " + (offset / 20 + 1) + ", " + total + " total)");
                        btnSearch.setEnabled(true);
                        if (!results.isEmpty()) resultList.setSelectedIndex(0);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        lblInfo.setText("Search failed: " + e.getMessage());
                        btnSearch.setEnabled(true);
                    });
                    logErr("Modrinth search: " + e.getMessage());
                }
            });
        }

        List<ModResult> parseSearchResults(String json) {
            List<ModResult> list = new ArrayList<>();
            // Split on "hits" array entries — each hit has slug, title, description, author, downloads, etc.
            // We find each hit block by matching project_id patterns
            Pattern hitP = Pattern.compile(
                "\"slug\"\\s*:\\s*\"([^\"]*?)\".*?" +
                "\"title\"\\s*:\\s*\"([^\"]*?)\".*?" +
                "\"description\"\\s*:\\s*\"([^\"]*?)\".*?" +
                "\"project_type\"\\s*:\\s*\"([^\"]*?)\".*?" +
                "\"downloads\"\\s*:\\s*(\\d+).*?" +
                "\"project_id\"\\s*:\\s*\"([^\"]*?)\".*?" +
                "\"author\"\\s*:\\s*\"([^\"]*?)\"",
                Pattern.DOTALL);
            Matcher m = hitP.matcher(json);
            while (m.find()) {
                ModResult r = new ModResult();
                r.slug        = m.group(1);
                r.title       = m.group(2);
                r.description = m.group(3);
                r.projectType = m.group(4);
                r.downloads   = Long.parseLong(m.group(5));
                r.projectId   = m.group(6);
                r.author      = m.group(7);
                list.add(r);
            }
            return list;
        }

        void showResultDetail() {
            ModResult r = resultList.getSelectedValue();
            if (r == null) { detailArea.setText("Select a mod..."); btnInstall.setEnabled(false); return; }
            btnInstall.setEnabled(true);
            StringBuilder sb = new StringBuilder();
            sb.append("\u2550\u2550\u2550 ").append(r.title).append(" \u2550\u2550\u2550\n\n");
            sb.append("Author:      ").append(r.author).append('\n');
            sb.append("Slug:        ").append(r.slug).append('\n');
            sb.append("Project ID:  ").append(r.projectId).append('\n');
            sb.append("Downloads:   ").append(fmtDownloads(r.downloads)).append('\n');
            sb.append("Type:        ").append(r.projectType).append('\n');
            sb.append("\nDescription:\n").append(r.description).append('\n');
            sb.append("\nModrinth:    https://modrinth.com/mod/").append(r.slug).append('\n');
            detailArea.setText(sb.toString());
            detailArea.setCaretPosition(0);
        }

        void installSelectedMod() {
            ModResult r = resultList.getSelectedValue();
            if (r == null) return;
            btnInstall.setEnabled(false);
            lblInfo.setText("Fetching versions for " + r.title + "...");

            executor.submit(() -> {
                try {
                    // Get project versions to find the latest primary file
                    String versionsUrl = MODRINTH_PROJ + r.projectId + "/version";
                    String json = httpGet(versionsUrl);

                    // Find the first version's primary file URL and filename
                    // Pattern: "url": "...", "filename": "..."
                    String fileUrl  = rx1(json, "\"url\"\\s*:\\s*\"(https://cdn\\.modrinth\\.com/[^\"]+)\"");
                    String fileName = rx1(json, "\"filename\"\\s*:\\s*\"([^\"]+\\.jar)\"");

                    if (fileUrl == null || fileName == null) {
                        SwingUtilities.invokeLater(() -> {
                            lblInfo.setText("No downloadable .jar found for " + r.title);
                            btnInstall.setEnabled(true);
                        });
                        return;
                    }

                    Path target = MODS_DIR.resolve(fileName);
                    if (Files.exists(target)) {
                        SwingUtilities.invokeLater(() -> {
                            lblInfo.setText(fileName + " already exists in mods/");
                            btnInstall.setEnabled(true);
                        });
                        return;
                    }

                    SwingUtilities.invokeLater(() -> lblInfo.setText("Downloading " + fileName + "..."));
                    log("Downloading mod: " + fileName + " from " + fileUrl);

                    // Download to mods dir
                    HttpURLConnection c = (HttpURLConnection) new URL(fileUrl).openConnection();
                    c.setRequestProperty("User-Agent", APP + "/" + VER);
                    c.setConnectTimeout(10000); c.setReadTimeout(30000);
                    if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
                    try (InputStream in = c.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
                        byte[] buf = new byte[8192]; int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }

                    log("Installed mod: " + fileName + " (" + fmtSize(Files.size(target)) + ")");
                    SwingUtilities.invokeLater(() -> {
                        lblInfo.setText("\u2713 Installed: " + fileName);
                        btnInstall.setEnabled(true);
                    });

                } catch (Exception e) {
                    logErr("Mod install: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        lblInfo.setText("Install failed: " + e.getMessage());
                        btnInstall.setEnabled(true);
                    });
                }
            });
        }

        String fmtDownloads(long d) {
            if (d >= 1_000_000) return String.format("%.1fM", d / 1_000_000.0);
            if (d >= 1_000)     return String.format("%.1fK", d / 1_000.0);
            return String.valueOf(d);
        }
    }

    /** Modrinth search result entry */
    static class ModResult {
        String slug, title, description, projectType, projectId, author;
        long downloads;
        @Override public String toString() { return title != null ? title : slug; }
    }

    /** Renders search results with title, author, downloads, description preview */
    class ModResultRenderer extends JPanel implements ListCellRenderer<ModResult> {
        JLabel lblTitle = new JLabel();
        JLabel lblMeta  = new JLabel();
        JLabel lblDesc  = new JLabel();

        ModResultRenderer() {
            setLayout(new BorderLayout(0, 2));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(lblTitle, BorderLayout.WEST);
            top.add(lblMeta, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            add(lblDesc, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ModResult> list, ModResult r, int idx, boolean sel, boolean foc) {
            setBackground(sel ? C_ACCENT : (idx % 2 == 0 ? C_CONSOLE : C_CARD_ALT));
            lblTitle.setText(r.title);
            lblTitle.setForeground(sel ? C_WHITE : C_WHITE);
            String dlStr = r.downloads >= 1_000_000 ? String.format("%.1fM", r.downloads / 1e6) :
                           r.downloads >= 1_000 ? String.format("%.1fK", r.downloads / 1e3) :
                           String.valueOf(r.downloads);
            lblMeta.setText(r.author + "  \u2022  \u2B73 " + dlStr);
            lblMeta.setForeground(sel ? C_WHITE : C_DIM);
            String desc = r.description != null ? r.description : "";
            if (desc.length() > 100) desc = desc.substring(0, 97) + "...";
            lblDesc.setText(desc);
            lblDesc.setForeground(sel ? C_WHITE : C_GREY);
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VERSION DROPDOWN
    // ═══════════════════════════════════════════════════════════════════
    class VerDropdown extends JComboBox<String> {
        VerDropdown() {
            setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean sel, boolean foc) {
                    super.getListCellRendererComponent(list, val, idx, sel, foc);
                    setBackground(sel ? C_ACCENT : C_FIELD); setForeground(C_WHITE);
                    setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8)); setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    return this;
                }
            });
            setBackground(C_FIELD); setForeground(C_WHITE); setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }

        void updateList() {
            removeAllItems();
            synchronized (dispVersions) {
                for (String[] v : dispVersions) {
                    String tag = "release".equals(v[1]) ? "" : "local".equals(v[1]) ? " [local]" : " [" + v[1] + "]";
                    addItem(v[0] + tag);
                }
            }
            if (Files.exists(PROF_FILE)) {
                try {
                    Properties p = new Properties(); p.load(Files.newInputStream(PROF_FILE));
                    String saved = p.getProperty("version");
                    if (saved != null) for (int i = 0; i < dispVersions.size(); i++)
                        if (dispVersions.get(i)[0].equals(saved)) { setSelectedIndex(i); return; }
                } catch (Exception ignored) {}
            }
            if (getItemCount() > 0) setSelectedIndex(0);
        }

        String[] getSelected() {
            int idx = getSelectedIndex();
            return (idx >= 0 && idx < dispVersions.size()) ? dispVersions.get(idx) : null;
        }
    }
}
