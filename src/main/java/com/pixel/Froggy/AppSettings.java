package com.pixel.Froggy;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AppSettings {

    private static final String APP_DIR_NAME = "Froggy";
    private static final String FILE_NAME    = "settings.dat";

    private static Path getSettingsPath() {
        String base;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            base = System.getenv("APPDATA");
            if (base == null) base = System.getProperty("user.home");
        } else {
            base = System.getProperty("user.home") + "/.config";
        }
        return Paths.get(base, APP_DIR_NAME, FILE_NAME);
    }

    public static void save(AppTheme.Mode theme, List<HistoryEntry> history,
                            int zoomSize, boolean searchInContent, boolean useRegex) {
        Path path = getSettingsPath();
        try {
            Files.createDirectories(path.getParent());
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(Files.newOutputStream(path), java.nio.charset.StandardCharsets.UTF_8))) {
                pw.println("theme=" + theme.name());
                pw.println("zoom=" + zoomSize);
                pw.println("searchContent=" + searchInContent);
                pw.println("useRegex=" + useRegex);

                for (HistoryEntry e : history) {
                    pw.println("ENTRY_START");
                    pw.println("query=" + escapeNewlines(e.query));
                    pw.println("time="  + escapeNewlines(e.timestamp));
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < e.results.size(); i++) {
                        if (i > 0) sb.append("|");
                        sb.append(e.results.get(i).getAbsolutePath().replace("|", "\\|"));
                    }
                    pw.println("files=" + sb);
                    pw.println("ENTRY_END");
                }
            }
        } catch (Exception ex) {
            System.err.println("[AppSettings] Ошибка сохранения: " + ex.getMessage());
        }
    }

    public static SavedData load() {
        Path path = getSettingsPath();
        SavedData result = new SavedData();
        if (!Files.exists(path)) return result;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(path), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            String curQuery = null; String curTime = null;
            List<File> curFiles = new ArrayList<>(); boolean inEntry = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("theme=")) {
                    try { result.theme = AppTheme.Mode.valueOf(line.substring(6).trim()); } catch (Exception ignored) {}
                } else if (line.startsWith("zoom=")) {
                    try { result.zoomSize = Integer.parseInt(line.substring(5).trim()); } catch (Exception ignored) {}
                } else if (line.startsWith("searchContent=")) {
                    result.searchInContent = Boolean.parseBoolean(line.substring(14).trim());
                } else if (line.startsWith("useRegex=")) {
                    result.useRegex = Boolean.parseBoolean(line.substring(9).trim());
                } else if (line.equals("ENTRY_START")) {
                    inEntry = true; curQuery = null; curTime = null; curFiles = new ArrayList<>();
                } else if (line.equals("ENTRY_END") && inEntry) {
                    if (curQuery != null) result.history.add(new HistoryEntry(unescapeNewlines(curQuery), unescapeNewlines(curTime != null ? curTime : ""), curFiles));
                    inEntry = false;
                } else if (inEntry) {
                    if (line.startsWith("query=")) curQuery = line.substring(6);
                    else if (line.startsWith("time=")) curTime = line.substring(5);
                    else if (line.startsWith("files=")) {
                        String raw = line.substring(6);
                        if (!raw.isEmpty()) {
                            String[] parts = raw.split("(?<!\\\\)\\|");
                            for (String p : parts) { String fp = p.replace("\\|", "|").trim(); if (!fp.isEmpty()) curFiles.add(new File(fp)); }
                        }
                    }
                }
            }
        } catch (Exception ex) { System.err.println("[AppSettings] Ошибка загрузки: " + ex.getMessage()); }
        return result;
    }

    private static String escapeNewlines(String s) { return s.replace("\n", "\\n").replace("\r", "\\r"); }
    private static String unescapeNewlines(String s) { return s.replace("\\n", "\n").replace("\\r", "\r"); }

    public static class SavedData {
        public AppTheme.Mode theme = AppTheme.Mode.LIGHT; 
        public List<HistoryEntry> history = new ArrayList<>();
        public int zoomSize = 13;
        public boolean searchInContent = false;
        public boolean useRegex = false;
    }
}
