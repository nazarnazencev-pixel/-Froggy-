package com.pixel.Froggy;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryEntry {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public final String      query;
    public final String      timestamp;
    public final List<File>  results;

    public HistoryEntry(String query, List<File> results) {
        this.query     = query;
        this.timestamp = LocalDateTime.now().format(FMT);
        this.results   = new ArrayList<>(results);
    }

    public HistoryEntry(String query, String timestamp, List<File> results) {
        this.query     = query;
        this.timestamp = timestamp;
        this.results   = new ArrayList<>(results);
    }
}