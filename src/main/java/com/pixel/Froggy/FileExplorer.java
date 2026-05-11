package com.pixel.Froggy;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class FileExplorer extends JFrame {

    private JTabbedPane tabbedPane;
    private JTextField searchField;
    private JPanel searchTab, topPanel, rightButtons;
    private JProgressBar progressBar;
    private JScrollPane scrollPane;
    private JPanel contentPanel;
    private JButton findBtn, clearBtn, stopBtn;
    private JComboBox<String> sortCombo;
    private JComboBox<DriveItem> driveCombo;

    private JPanel historyTab, historyTopPanel;
    private JButton clearHistoryBtn;
    private JScrollPane historyScroll;
    private JPanel historyContent;
    private SettingsTab settingsTab;

    private final List<HistoryEntry> historyEntries = new ArrayList<>();
    private enum SortMode { NAME, DATE, SIZE }
    private SortMode sortMode = SortMode.NAME;

    private final Map<String, JPanel> groupPanels = new HashMap<>();
    private final Map<String, ArrowButton> groupArrows = new HashMap<>();
    private final Map<String, JPanel> groupHeaders = new HashMap<>();
    private final Map<String, JLabel> groupTitles = new HashMap<>();
    private final Map<String, List<FileItem>> groupItems = new HashMap<>();
    private final List<FileItem> fileItems = new ArrayList<>();
    private final List<File> currentFound = new ArrayList<>();

    private final FileSearcher searcher = new FileSearcher();
    private final FileSorter sorter = new FileSorter();
    private FileSearcher.InternalWorker currentWorker;

    private static class ParsedQuery {
        String text = ""; List<String> extensions = null;
    }

    private ParsedQuery parseQuery(String raw) {
        ParsedQuery pq = new ParsedQuery();
        if (raw.contains("/")) {
            String[] parts = raw.split("/", 2); pq.text = parts[0].trim();
            String extPart = parts[1].trim();
            if (!extPart.isEmpty()) {
                pq.extensions = new ArrayList<>();
                for (String ext : extPart.split(";")) {
                    String e = ext.trim().toLowerCase();
                    if (!e.startsWith(".")) e = "." + e;
                    if (!e.equals(".")) pq.extensions.add(e);
                }
                if (pq.extensions.isEmpty()) pq.extensions = null;
            }
        } else { pq.text = raw.trim(); }
        return pq;
    }

    static class CustomIcon implements Icon {
        private final String type;
        CustomIcon(String type) { this.type = type; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(c.getForeground()); g2.setStroke(new BasicStroke(1.5f));
            switch (type) {
                case "search" -> { g2.drawOval(x+2,y+2,9,9); g2.drawLine(x+10,y+10,x+14,y+14); }
                case "clear"  -> { g2.drawRect(x+3,y+5,10,10); g2.drawLine(x+1,y+4,x+15,y+4); g2.drawLine(x+6,y+2,x+10,y+2); }
                case "trash"  -> { g2.drawRect(x+3,y+4,10,11); g2.drawLine(x+1,y+4,x+15,y+4); g2.drawLine(x+6,y+2,x+10,y+2); g2.drawLine(x+6,y+6,x+6,y+13); g2.drawLine(x+10,y+6,x+10,y+13); }
                case "close"  -> { g2.drawLine(x+4,y+4,x+12,y+12); g2.drawLine(x+12,y+4,x+4,y+12); }
                case "drive"  -> { g2.drawOval(x+2,y+1,12,5); g2.drawLine(x+2,y+3,x+2,y+13); g2.drawLine(x+14,y+3,x+14,y+13); g2.drawOval(x+2,y+9,12,5); g2.drawLine(x+5,y+11,x+9,y+11); }
            }
            g2.dispose();
        }
        @Override public int getIconWidth()  { return 16; }
        @Override public int getIconHeight() { return 16; }
    }

    static class ArrowButton extends JComponent {
        private boolean expanded = true; private Color color;
        ArrowButton(Color color) { this.color = color; setPreferredSize(new Dimension(18,18)); setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
        void setExpanded(boolean v) { expanded = v; repaint(); }
        void setColor(Color c) { color = c; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color);
            int w = getWidth(), h = getHeight();
            if (expanded) g2.fillPolygon(new int[]{w/2-5,w/2+5,w/2}, new int[]{h/2-3,h/2-3,h/2+4}, 3);
            else g2.fillPolygon(new int[]{w/2-3,w/2-3,w/2+4}, new int[]{h/2-5,h/2+5,h/2}, 3);
            g2.dispose();
        }
    }

    public FileExplorer() {
        setTitle("Froggy"); setSize(1100, 800); setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { saveSettings(); dispose(); System.exit(0); } });
        setLocationRelativeTo(null);

        AppSettings.SavedData saved = AppSettings.load();
        AppTheme.setMode(saved.theme);
        historyEntries.addAll(saved.history);

        initUI();
        applyLoadedTheme();
        settingsTab.setSelectedTheme(saved.theme);
        settingsTab.setZoomSize(saved.zoomSize);
        settingsTab.setSearchInContent(saved.searchInContent);
        settingsTab.setUseRegex(saved.useRegex);
    }

    private void applyLoadedTheme() {
        try { UIManager.setLookAndFeel(AppTheme.isDark() ? new FlatDarkLaf() : new FlatLightLaf()); SwingUtilities.updateComponentTreeUI(this); } catch (Exception ignored) {}
        applyThemeToAll();
    }

    private void saveSettings() {
        AppSettings.save(AppTheme.getMode(), historyEntries, settingsTab.getZoomSize(), settingsTab.isSearchInContent(), settingsTab.isUseRegex());
    }

    private void initUI() {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane(); tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabbedPane.addTab("Поиск", buildSearchTab());
        tabbedPane.addTab("История", buildHistoryTab());

        settingsTab = new SettingsTab();
        settingsTab.setOnThemeChanged(() -> {
            AppTheme.setMode(settingsTab.getSelectedTheme());
            try { UIManager.setLookAndFeel(AppTheme.isDark() ? new FlatDarkLaf() : new FlatLightLaf()); SwingUtilities.updateComponentTreeUI(this); } catch (Exception ignored) {}
            saveSettings(); applyThemeToAll();
        });
        settingsTab.setOnZoomChanged(() -> {
            int sz = settingsTab.getZoomSize();
            for (FileItem fi : fileItems) fi.setFontSize(sz);
            contentPanel.revalidate(); contentPanel.repaint(); saveSettings();
        });
        settingsTab.setOnSearchModeChanged(this::saveSettings);

        tabbedPane.addTab("Настройки", settingsTab);
        add(tabbedPane, BorderLayout.CENTER);
        applyThemeToAll();
    }

    private JPanel buildSearchTab() {
        searchTab = new JPanel(new BorderLayout());
        topPanel  = new JPanel(new BorderLayout(8,8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12,15,12,15));

        JPanel searchRow = new JPanel(new BorderLayout(8,0));
        searchRow.setOpaque(false);

        driveCombo = new JComboBox<>(buildDriveItems());
        driveCombo.setFont(new Font("Segoe UI",Font.PLAIN,13));
        driveCombo.setPreferredSize(new Dimension(220,32));

        driveCombo.setRenderer(new javax.swing.ListCellRenderer<DriveItem>() {
            private final Color FIXED_DARK_BLUE = new Color(75, 110, 169);

            private final JPanel panel = new JPanel(new BorderLayout());
            private final JLabel textLabel = new JLabel();

            {
                textLabel.setIcon(new CustomIcon("drive"));
                panel.add(textLabel, BorderLayout.CENTER);
                panel.setOpaque(true); // Панель непрозрачная
                textLabel.setOpaque(false); // Чтобы текст сливался с фоном панели
            }

            @Override
            public Component getListCellRendererComponent(JList<? extends DriveItem> list, DriveItem value, int index, boolean isSelected, boolean cellHasFocus) {
                boolean isDark = AppTheme.isDark();

                if (isSelected) {
                    panel.setBackground(FIXED_DARK_BLUE);
                    textLabel.setForeground(isDark ? Color.WHITE : Color.WHITE);
                } else {
                    panel.setBackground(AppTheme.fieldBackground());
                    textLabel.setForeground(isDark ? Color.WHITE : AppTheme.fieldForeground());
                }

                textLabel.setText(value != null ? value.label : "");
                return panel;
            }
        });

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI",Font.PLAIN,14));
        searchField.addActionListener(e -> startLiveSearch());
        searchRow.add(driveCombo,  BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);

        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        sortPanel.setOpaque(false);
        JLabel sortLabel = new JLabel("Сортировка:");
        sortLabel.setFont(new Font("Segoe UI",Font.PLAIN,12));
        sortPanel.add(sortLabel);
        sortCombo = new JComboBox<>(new String[]{"По имени","По дате","По размеру"});
        sortCombo.setFont(new Font("Segoe UI",Font.PLAIN,12));
        sortCombo.setPreferredSize(new Dimension(120,28));
        sortCombo.addActionListener(e -> {
            sortMode = switch(sortCombo.getSelectedIndex()){
                case 1->SortMode.DATE; case 2->SortMode.SIZE; default->SortMode.NAME;
            };
            resortAllGroups();
        });
        sortPanel.add(sortCombo);

        findBtn  = new JButton("Найти",    new CustomIcon("search"));
        stopBtn  = new JButton("Стоп",     new CustomIcon("close"));
        clearBtn = new JButton("Очистить", new CustomIcon("clear"));

        stopBtn.addActionListener(e -> {
            if (currentWorker != null && !currentWorker.isDone()) {
                currentWorker.cancel(true);
                progressBar.setString("Остановка поиска...");
            }
        });

        rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        rightButtons.setOpaque(false);
        rightButtons.add(sortPanel);
        rightButtons.add(findBtn);
        rightButtons.add(stopBtn);
        rightButtons.add(clearBtn);

        topPanel.add(searchRow,    BorderLayout.CENTER);
        topPanel.add(rightButtons, BorderLayout.EAST);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);

        progressBar = new JProgressBar(0,1);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setPreferredSize(new Dimension(getWidth(),35));
        progressBar.setFont(new Font("Segoe UI",Font.BOLD,14));

        findBtn.addActionListener(e  -> startLiveSearch());
        clearBtn.addActionListener(e -> clearSearch());

        searchTab.add(topPanel,    BorderLayout.NORTH);
        searchTab.add(scrollPane,  BorderLayout.CENTER);
        searchTab.add(progressBar, BorderLayout.SOUTH);
        return searchTab;
    }

    private DriveItem[] buildDriveItems() {
        Set<File> addedRoots = new LinkedHashSet<>(); // Используем Set, чтобы пути не дублировались
        List<DriveItem> items = new ArrayList<>();

        // 1. Домашняя папка
        File home = new File(System.getProperty("user.home"));
        items.add(new DriveItem(home, "Домашняя папка (" + System.getProperty("user.name") + ")"));
        addedRoots.add(home);

        // 2. Стандартные корни (C:\, D:\ на Win или / на Linux)
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File r : roots) {
                if (r.exists() && !addedRoots.contains(r)) {
                    String name = r.getAbsolutePath().equals("/") ? "Корень системы (/)" : r.getAbsolutePath();
                    items.add(new DriveItem(r, name));
                    addedRoots.add(r);
                }
            }
        }

        // 3. Специфично для Linux: парсим /etc/fstab
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            File fstab = new File("/etc/fstab");
            if (fstab.exists() && fstab.canRead()) {
                try (java.util.Scanner scanner = new java.util.Scanner(fstab)) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            File mountPoint = new File(parts[1]);
                            // Добавляем только существующие папки, которые мы еще не добавили
                            // Игнорируем своп, спец-директории и загрузчик
                            if (mountPoint.exists() && mountPoint.isDirectory() && !addedRoots.contains(mountPoint)) {
                                String path = mountPoint.getAbsolutePath();
                                if (!path.startsWith("/proc") && !path.startsWith("/sys") && !path.startsWith("/dev") && !path.startsWith("/tmp") && !path.startsWith("/boot") && !path.startsWith("/swap")) {
                                    items.add(new DriveItem(mountPoint, "Диск: " + mountPoint.getName() + " (" + path + ")"));
                                    addedRoots.add(mountPoint);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            // Дополнительно: проверяем /media и /mnt на наличие смонтированных флешек
            File[] commonMounts = {new File("/media/" + System.getProperty("user.name")), new File("/run/media/" + System.getProperty("user.name")), new File("/mnt")};
            for (File base : commonMounts) {
                if (base.exists() && base.isDirectory()) {
                    File[] subDirs = base.listFiles();
                    if (subDirs != null) {
                        for (File sub : subDirs) {
                            if (sub.isDirectory() && !addedRoots.contains(sub)) {
                                items.add(new DriveItem(sub, "Съемный диск: " + sub.getName()));
                                addedRoots.add(sub);
                            }
                        }
                    }
                }
            }
        }
        return items.toArray(new DriveItem[0]);
    }


    private File getSearchRoot() {
        DriveItem sel = (DriveItem)driveCombo.getSelectedItem();
        return (sel == null || sel.root == null) ? new File(System.getProperty("user.home")) : sel.root;
    }

    private JPanel buildHistoryTab() {
        historyTab = new JPanel(new BorderLayout()); historyTopPanel = new JPanel(new BorderLayout(10,10)); historyTopPanel.setBorder(BorderFactory.createEmptyBorder(12,15,12,15));
        JLabel histLabel = new JLabel("История поисковых запросов"); histLabel.setFont(new Font("Segoe UI",Font.BOLD,14));
        clearHistoryBtn = new JButton("Очистить историю", new CustomIcon("trash")); clearHistoryBtn.addActionListener(e -> clearAllHistory());
        JPanel histRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); histRight.setOpaque(false); histRight.add(clearHistoryBtn);
        historyTopPanel.add(histLabel, BorderLayout.CENTER); historyTopPanel.add(histRight, BorderLayout.EAST);
        historyContent = new JPanel(); historyContent.setLayout(new BoxLayout(historyContent,BoxLayout.Y_AXIS));
        historyScroll = new JScrollPane(historyContent); historyScroll.setBorder(null); historyScroll.getVerticalScrollBar().setUnitIncrement(20);
        historyTab.add(historyTopPanel, BorderLayout.NORTH); historyTab.add(historyScroll, BorderLayout.CENTER);
        return historyTab;
    }

    private void addHistoryEntry(String query, File searchRoot, List<File> found) {
        String driveLabel = searchRoot.getAbsolutePath().equals(System.getProperty("user.home")) ? "~" : searchRoot.getAbsolutePath();
        historyEntries.add(0, new HistoryEntry(query+"  ["+driveLabel+"]", found)); saveSettings(); rebuildHistoryUI();
    }

    private void rebuildHistoryUI() {
        historyContent.removeAll();
        if (historyEntries.isEmpty()) {
            JLabel empty = new JLabel("История пуста", SwingConstants.CENTER); empty.setFont(new Font("Segoe UI",Font.PLAIN,14)); empty.setForeground(AppTheme.dateText()); empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            historyContent.add(Box.createVerticalStrut(40)); historyContent.add(empty);
        } else { for (HistoryEntry e : historyEntries) { historyContent.add(buildHistoryCard(e)); historyContent.add(Box.createRigidArea(new Dimension(0,6))); } }
        historyContent.revalidate(); historyContent.repaint();
    }

    private JPanel buildHistoryCard(HistoryEntry entry) {
        JPanel card = new JPanel(new BorderLayout(8,4)); card.setOpaque(true); card.setBackground(AppTheme.topPanelBackground());
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.buttonBorder(),1), BorderFactory.createEmptyBorder(8,14,8,14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        JPanel cardTop = new JPanel(new BorderLayout()); cardTop.setOpaque(false);
        JLabel queryLabel = new JLabel(" "+entry.query); queryLabel.setIcon(new CustomIcon("search")); queryLabel.setFont(new Font("Segoe UI",Font.BOLD,13)); queryLabel.setForeground(AppTheme.fileText());
        JLabel timeLabel = new JLabel(entry.timestamp); timeLabel.setFont(new Font("Consolas",Font.PLAIN,11)); timeLabel.setForeground(AppTheme.dateText());
        JButton delBtn = new JButton(new CustomIcon("close")); delBtn.setPreferredSize(new Dimension(26,22)); delBtn.setFocusPainted(false); delBtn.setContentAreaFilled(false); delBtn.setBorderPainted(false); delBtn.setForeground(new Color(0xCC3333)); delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.addActionListener(e -> { historyEntries.remove(entry); saveSettings(); rebuildHistoryUI(); });
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); topRight.setOpaque(false); topRight.add(timeLabel); topRight.add(delBtn);
        cardTop.add(queryLabel,BorderLayout.WEST); cardTop.add(topRight,BorderLayout.EAST);
        JPanel filesList = new JPanel(); filesList.setLayout(new BoxLayout(filesList,BoxLayout.Y_AXIS)); filesList.setOpaque(false); filesList.setBorder(BorderFactory.createEmptyBorder(6,0,0,0));
        int shown=Math.min(entry.results.size(),5);
        for(int i=0;i<shown;i++){ File f=entry.results.get(i); JLabel fl=new JLabel("  "+f.getName()+"   —   "+f.getParent()); fl.setFont(new Font("Segoe UI",Font.PLAIN,12)); fl.setForeground(AppTheme.dateText()); filesList.add(fl); }
        if(entry.results.size()>5){ JLabel more=new JLabel("  ... и ещё "+(entry.results.size()-5)+" файлов"); more.setFont(new Font("Segoe UI",Font.ITALIC,12)); more.setForeground(AppTheme.dateText()); filesList.add(more); }
        if(entry.results.isEmpty()){ JLabel none=new JLabel("  Ничего не найдено"); none.setFont(new Font("Segoe UI",Font.ITALIC,12)); none.setForeground(AppTheme.dateText()); filesList.add(none); }
        JButton repeatBtn = new JButton("Повторить поиск"); repeatBtn.setFont(new Font("Segoe UI",Font.PLAIN,12)); repeatBtn.setFocusPainted(false);
        repeatBtn.addActionListener(e -> { String raw=entry.query.contains("  [")?entry.query.substring(0,entry.query.lastIndexOf("  [")):entry.query; searchField.setText(raw); tabbedPane.setSelectedIndex(0); startLiveSearch(); });
        styleButton(repeatBtn);
        JPanel cardBottom=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,4)); cardBottom.setOpaque(false); cardBottom.add(repeatBtn);
        card.add(cardTop,BorderLayout.NORTH); card.add(filesList,BorderLayout.CENTER); card.add(cardBottom,BorderLayout.SOUTH);
        return card;
    }

    private void clearAllHistory() {
        if(JOptionPane.showConfirmDialog(this,"Очистить всю историю поиска?","Подтверждение",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION){ historyEntries.clear(); saveSettings(); rebuildHistoryUI(); }
    }

    private Comparator<FileItem> currentComparator() {
        return switch(sortMode){ case DATE->Comparator.comparingLong(FileItem::getFileModified).reversed(); case SIZE->Comparator.comparingLong(FileItem::getFileSize).reversed(); default->Comparator.comparing(fi->fi.getFileName().toLowerCase()); };
    }
    private void resortGroup(String ext){ JPanel c=groupPanels.get(ext); List<FileItem> items=groupItems.get(ext); if(c==null||items==null)return; items.sort(currentComparator()); c.removeAll(); for(FileItem fi:items)c.add(fi); c.revalidate(); c.repaint(); }
    private void resortAllGroups(){ groupPanels.keySet().forEach(this::resortGroup); }


    private void styleButton(JButton btn){
        btn.setFont(new Font("Segoe UI",Font.PLAIN,13)); btn.setBackground(AppTheme.buttonBackground()); btn.setForeground(AppTheme.buttonForeground());
        btn.setFocusPainted(false); btn.setOpaque(true); btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppTheme.buttonBorder(),1), BorderFactory.createEmptyBorder(4,12,4,12)));
    }

    private void applyThemeToAll(){
        topPanel.setBackground(AppTheme.topPanelBackground());
        searchField.setBackground(AppTheme.fieldBackground()); searchField.setForeground(AppTheme.fieldForeground()); searchField.setCaretColor(AppTheme.fieldForeground());
        driveCombo.setBackground(AppTheme.fieldBackground()); driveCombo.setForeground(AppTheme.fieldForeground());
        styleButton(findBtn); styleButton(stopBtn); styleButton(clearBtn);
        contentPanel.setBackground(AppTheme.background()); scrollPane.getViewport().setBackground(AppTheme.background());

        for(String ext:groupHeaders.keySet()){
            Color groupClr = AppTheme.groupColor(ext);
            JPanel header = groupHeaders.get(ext);
            header.setBackground(groupClr);
            header.setOpaque(true);

            JLabel titleLbl = groupTitles.get(ext);
            titleLbl.setForeground(AppTheme.groupHeaderText());
            titleLbl.setBackground(groupClr);
            titleLbl.setOpaque(true);
            titleLbl.repaint();

            groupArrows.get(ext).setColor(AppTheme.arrowColor());
        }

        for(JPanel gp:groupPanels.values()) gp.setBackground(AppTheme.background());
        for(FileItem fi:fileItems) fi.applyTheme();

        if(AppTheme.isDark()){ UIManager.put("ProgressBar.selectionBackground",Color.WHITE); UIManager.put("ProgressBar.selectionForeground",Color.WHITE); }
        else { UIManager.put("ProgressBar.selectionBackground",Color.BLACK); UIManager.put("ProgressBar.selectionForeground",Color.WHITE); }
        progressBar.setForeground(new Color(34,133,225)); progressBar.updateUI();

        historyTopPanel.setBackground(AppTheme.topPanelBackground()); historyContent.setBackground(AppTheme.background()); historyScroll.getViewport().setBackground(AppTheme.background());
        styleButton(clearHistoryBtn); rebuildHistoryUI();
        tabbedPane.setBackground(AppTheme.topPanelBackground()); tabbedPane.setForeground(AppTheme.fileText());

        settingsTab.applyTheme();
        repaint(); revalidate();
    }

    private void clearSearch(){
        if (currentWorker != null) currentWorker.cancel(true);
        searcher.clearCache(); searchField.setText(""); contentPanel.removeAll();
        groupPanels.clear(); groupArrows.clear(); groupHeaders.clear(); groupTitles.clear();
        groupItems.clear(); fileItems.clear(); currentFound.clear();
        progressBar.setValue(0); progressBar.setString(""); contentPanel.revalidate(); contentPanel.repaint();
    }

    private void startLiveSearch(){
        String rawQuery = searchField.getText().trim(); if(rawQuery.isEmpty()) return;
        ParsedQuery parsed = parseQuery(rawQuery);
        final File searchRoot = getSearchRoot();
        final boolean searchInContent = settingsTab.isSearchInContent();
        final boolean isRegex = settingsTab.isUseRegex();
        final List<String> extensions = parsed.extensions;

        contentPanel.removeAll(); groupPanels.clear(); groupArrows.clear();
        groupHeaders.clear(); groupTitles.clear(); groupItems.clear(); fileItems.clear(); currentFound.clear();
        progressBar.setIndeterminate(false); progressBar.setMinimum(0); progressBar.setMaximum(1); progressBar.setValue(0);
        String dn = searchRoot.getAbsolutePath().equals(System.getProperty("user.home")) ? "~" : searchRoot.getAbsolutePath();
        progressBar.setString("Ищу на "+dn+"...");

        if (currentWorker != null) currentWorker.cancel(true);

        currentWorker = new FileSearcher.InternalWorker(){
            private final List<File> allFound=new ArrayList<>();
            @Override protected Void doInBackground(){ searcher.searchAndPublish(searchRoot, parsed.text, this, searchInContent, isRegex, extensions); return null; }
            @Override protected void process(List<File> chunks){
                if (isCancelled()) return;
                for(File f:chunks){allFound.add(f);currentFound.add(f);addFileToGroup(f);}
                progressBar.setString("Ищу файлы: "+allFound.size()+"..."); contentPanel.revalidate();
            }
            @Override protected void done(){
                if (isCancelled()) { progressBar.setString("Поиск прерван. Найдено: " + allFound.size()); return; }
                resortAllGroups();
                final int total=allFound.size();
                addHistoryEntry(rawQuery,searchRoot,new ArrayList<>(allFound));
                progressBar.setMinimum(0); progressBar.setMaximum(Math.max(total,1)); progressBar.setValue(0);
                Timer anim=new Timer(15,null);
                anim.addActionListener(e->{
                    int next=Math.min(progressBar.getValue()+Math.max(1,total/60),total);
                    progressBar.setValue(next); progressBar.setString("Найдено: "+next+" из "+total);
                    if(next>=total){((Timer)e.getSource()).stop(); progressBar.setString("Готово! Всего найдено: "+total); SwingUtilities.invokeLater(()->scrollPane.getVerticalScrollBar().setValue(0));}
                });
                anim.start();
            }
        };
        currentWorker.execute();
    }

    private void addFileToGroup(File file){
        String ext = sorter.getExtension(file);
        int fontSize = settingsTab.getZoomSize();

        if(!groupPanels.containsKey(ext)){
            JPanel container=new JPanel(); container.setLayout(new BoxLayout(container,BoxLayout.Y_AXIS)); container.setBackground(AppTheme.background());

            JPanel header=new JPanel(new BorderLayout());
            header.setBackground(AppTheme.groupColor(ext));
            header.setOpaque(true);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));

            ArrowButton arrow=new ArrowButton(AppTheme.arrowColor());
            JPanel arrowWrap=new JPanel(new GridBagLayout()); arrowWrap.setOpaque(false); arrowWrap.setBorder(BorderFactory.createEmptyBorder(0,8,0,6)); arrowWrap.add(arrow);

            JLabel title=new JLabel("ГРУППА: "+ext);
            title.setFont(new Font("Segoe UI",Font.BOLD,14));
            title.setOpaque(true);
            title.setBackground(AppTheme.groupColor(ext));
            title.setForeground(AppTheme.groupHeaderText());
            title.setBorder(BorderFactory.createEmptyBorder(5,0,5,15));

            header.add(arrowWrap,BorderLayout.WEST); header.add(title,BorderLayout.CENTER);
            MouseAdapter toggle=new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){container.setVisible(!container.isVisible());arrow.setExpanded(container.isVisible());contentPanel.revalidate();contentPanel.repaint();}
                @Override public void mouseEntered(MouseEvent e){header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
                @Override public void mouseExited(MouseEvent e) {header.setCursor(Cursor.getDefaultCursor());}
            };
            header.addMouseListener(toggle); arrow.addMouseListener(toggle);
            contentPanel.add(header); contentPanel.add(container); contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
            groupPanels.put(ext,container); groupArrows.put(ext,arrow); groupHeaders.put(ext,header); groupTitles.put(ext,title); groupItems.put(ext,new ArrayList<>());
        }
        FileItem item = new FileItem(file);
        item.setFontSize(fontSize);
        fileItems.add(item);
        groupItems.get(ext).add(item);
        groupPanels.get(ext).add(item);
    }

    static class DriveItem {
        final File root;
        final String label;

        DriveItem(File root, String customName) {
            this.root = root;
            this.label = customName + " [" + fmtBytes(root.getFreeSpace()) + " своб. / " + fmtBytes(root.getTotalSpace()) + "]";
        }

        DriveItem(File root) {
            this(root, root.getAbsolutePath());
        }

        private static String fmtBytes(long b) {
            if (b <= 0) return "?";
            if (b < 1024L*1024*1024) return String.format("%.0f МБ", b/(1024.0*1024));
            return String.format("%.1f ГБ", b/(1024.0*1024*1024));
        }

        @Override public String toString() { return label; }
    }
}
