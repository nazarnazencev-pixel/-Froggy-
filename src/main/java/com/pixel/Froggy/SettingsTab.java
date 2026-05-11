package com.pixel.Froggy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SettingsTab extends JPanel {
    private JComboBox<String> zoomCombo;

    private JRadioButton lightRadio;
    private JRadioButton darkRadio;

    private JCheckBox searchContentCheckBox;
    private JCheckBox useRegexCheckBox;

    private JPanel mainPanel;
    private JPanel radioPanel;

    private Runnable onZoomChanged;
    private Runnable onSearchModeChanged;
    private Runnable onThemeChanged;

    private static final String[] ZOOM_LEVELS = {
            "50%", "60%", "70%", "80%", "90%", "100%", "110%", "120%",
            "130%", "140%", "150%", "160%", "170%", "180%", "190%"
    };
    private static final int[] ZOOM_SIZES = {
            7, 8, 9, 10, 12, 13, 14, 16, 18, 19, 20, 22, 23, 24, 25
    };

    public SettingsTab() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        setBackground(AppTheme.background());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(AppTheme.background());

        JPanel themeRow = createRow("Тема оформления:");

        radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        radioPanel.setBackground(AppTheme.background());

        lightRadio = new JRadioButton("Светлая");
        darkRadio = new JRadioButton("Темная");

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightRadio);
        themeGroup.add(darkRadio);

        lightRadio.setSelected(true);

        ActionListener themeListener = e -> { if (onThemeChanged != null) onThemeChanged.run(); };
        lightRadio.addActionListener(themeListener);
        darkRadio.addActionListener(themeListener);

        radioPanel.add(lightRadio);
        radioPanel.add(darkRadio);

        themeRow.add(radioPanel, BorderLayout.CENTER);
        mainPanel.add(themeRow);

        mainPanel.add(Box.createVerticalStrut(15));

        JPanel zoomRow = createRow("Масштаб:");
        zoomCombo = new JComboBox<>(ZOOM_LEVELS);
        zoomCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        zoomCombo.setPreferredSize(new Dimension(100, 28));
        zoomCombo.setSelectedIndex(5); // 100%


        zoomCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                boolean isDark = AppTheme.isDark();

                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(isDark ? Color.WHITE : list.getSelectionForeground());
                } else {
                    setBackground(AppTheme.fieldBackground());
                    setForeground(isDark ? Color.WHITE : AppTheme.fieldForeground());
                }
                return this;
            }
        });

        zoomCombo.addActionListener(e -> { if (onZoomChanged != null) onZoomChanged.run(); });
        zoomRow.add(zoomCombo, BorderLayout.EAST);
        mainPanel.add(zoomRow);

        mainPanel.add(Box.createVerticalStrut(25));

        searchContentCheckBox = new JCheckBox("Осуществлять поиск по содержимому файлов (может быть медленнее)");
        searchContentCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchContentCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchContentCheckBox.addActionListener(e -> { if (onSearchModeChanged != null) onSearchModeChanged.run(); });
        mainPanel.add(searchContentCheckBox);

        mainPanel.add(Box.createVerticalStrut(10));

        useRegexCheckBox = new JCheckBox("Использовать регулярные выражения (Regex)");
        useRegexCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        useRegexCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        useRegexCheckBox.addActionListener(e -> { if (onSearchModeChanged != null) onSearchModeChanged.run(); });
        mainPanel.add(useRegexCheckBox);

        add(mainPanel, BorderLayout.NORTH);
        applyTheme();
    }

    private JPanel createRow(String labelText) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setBackground(AppTheme.background());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        row.add(label, BorderLayout.WEST);

        return row;
    }

    public int getZoomSize() {
        int index = zoomCombo.getSelectedIndex();
        return (index >= 0 && index < ZOOM_SIZES.length) ? ZOOM_SIZES[index] : 13;
    }

    public void setZoomSize(int size) {
        for (int i = 0; i < ZOOM_SIZES.length; i++) {
            if (ZOOM_SIZES[i] == size) { zoomCombo.setSelectedIndex(i); return; }
        }
        zoomCombo.setSelectedIndex(5);
    }

    public boolean isSearchInContent() { return searchContentCheckBox.isSelected(); }
    public void setSearchInContent(boolean enabled) { searchContentCheckBox.setSelected(enabled); }

    public boolean isUseRegex() { return useRegexCheckBox.isSelected(); }
    public void setUseRegex(boolean enabled) { useRegexCheckBox.setSelected(enabled); }

    public AppTheme.Mode getSelectedTheme() {
        return darkRadio.isSelected() ? AppTheme.Mode.DARK : AppTheme.Mode.LIGHT;
    }

    public void setSelectedTheme(AppTheme.Mode mode) {
        if (mode == AppTheme.Mode.DARK) darkRadio.setSelected(true);
        else lightRadio.setSelected(true);
    }

    public void setOnZoomChanged(Runnable r) { this.onZoomChanged = r; }
    public void setOnSearchModeChanged(Runnable r) { this.onSearchModeChanged = r; }
    public void setOnThemeChanged(Runnable r) { this.onThemeChanged = r; }

    public void applyTheme() {
        SwingUtilities.invokeLater(() -> {
            Color bg = AppTheme.background();

            this.setBackground(bg);
            mainPanel.setBackground(bg);
            radioPanel.setBackground(bg);

            for (Component c : mainPanel.getComponents()) {
                if (c instanceof JPanel panel) {
                    panel.setBackground(bg);
                }
            }

            zoomCombo.setBackground(AppTheme.fieldBackground());
            zoomCombo.setForeground(AppTheme.fieldForeground());
        });
    }
}