package com.pixel.Froggy;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;

public class FileItem extends JPanel {

    private static final FileSystemView fsv = FileSystemView.getFileSystemView();
    private final JLabel nameLabel, dateLabel, sizeLabel;
    private final File file;

    public FileItem(File file) {
        this.file = file;
        setLayout(new BorderLayout(10, 0));
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(16, 16));
        left.add(iconLabel);

        new SwingWorker<Icon, Void>() {
            @Override protected Icon doInBackground() { return fsv.getSystemIcon(file); }
            @Override protected void done() { try { iconLabel.setIcon(get()); } catch (Exception ignored) {} }
        }.execute();

        nameLabel = new JLabel(file.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        left.add(nameLabel);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 12, 0, 0);

        sizeLabel = new JLabel(formatSize(file));
        sizeLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        sizeLabel.setPreferredSize(new Dimension(80, 20));
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        dateLabel = new JLabel(new SimpleDateFormat("dd.MM.yyyy HH:mm").format(file.lastModified()));
        dateLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        dateLabel.setPreferredSize(new Dimension(120, 20));
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0; right.add(sizeLabel, gbc);
        gbc.gridx = 1; right.add(dateLabel, gbc);

        add(left,  BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        applyTheme();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setBackground(AppTheme.fileHover()); repaintChildren(); }
            @Override public void mouseExited(MouseEvent e)  { setBackground(AppTheme.background()); repaintChildren(); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) openFile();
                else if (e.getButton() == MouseEvent.BUTTON3) showContextMenu(e.getX(), e.getY());
            }
        });
    }

    public void applyTheme() {
        setBackground(AppTheme.background());
        nameLabel.setForeground(AppTheme.fileText());
        sizeLabel.setForeground(AppTheme.dateText());
        dateLabel.setForeground(AppTheme.dateText());
    }

    public void setFontSize(int size) {
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, size));
        sizeLabel.setFont(new Font("Consolas", Font.PLAIN, Math.max(10, size - 1)));
        dateLabel.setFont(new Font("Consolas", Font.PLAIN, Math.max(10, size - 1)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, size + 32));
        revalidate();
        repaint();
    }

    private String formatSize(File f) {
        if (f.isDirectory()) return "";
        long b = f.length();
        if (b < 1024) return b + " Б";
        if (b < 1024 * 1024) return (b / 1024) + " КБ";
        if (b < 1024 * 1024 * 1024) return (b / (1024 * 1024)) + " МБ";
        return String.format("%.1f ГБ", b / (1024.0 * 1024 * 1024));
    }

    private void repaintChildren() {
        for (Component c : getComponents()) {
            c.setBackground(getBackground());
            if (c instanceof JPanel p)
                for (Component cc : p.getComponents()) cc.setBackground(getBackground());
        }
    }

    private void showContextMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem   = new JMenuItem("Открыть");
        JMenuItem revealItem = new JMenuItem("Показать в папке");
        JMenuItem copyItem   = new JMenuItem("Копировать путь");

        openItem.addActionListener(e -> openFile());
        revealItem.addActionListener(e -> { try { Desktop.getDesktop().open(file.getParentFile()); } catch (Exception ignored) {} });
        copyItem.addActionListener(e -> { var sel = new StringSelection(file.getAbsolutePath()); Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null); });

        menu.add(openItem); menu.add(revealItem); menu.addSeparator(); menu.add(copyItem);
        menu.show(this, x, y);
    }

    private void openFile() {
        try { Desktop.getDesktop().open(file); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Не удалось открыть файл"); }
    }

    public long getFileModified() { return file.lastModified(); }
    public long getFileSize()     { return file.length(); }
    public String getFileName()   { return file.getName(); }
}