package com.pixel.Froggy;

import javax.swing.SwingWorker;
import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public class FileSearcher {

    private volatile String lastQuery   = null;
    private volatile List<File> lastResults = null;

    public void searchAndPublish(File root, String query, InternalWorker worker, boolean searchContent, boolean isRegex, List<String> extensions) {
        String queryLower = query.toLowerCase();

        if (lastQuery != null && queryLower.startsWith(lastQuery) && lastResults != null) {
            List<File> filtered = new ArrayList<>();
            for (File f : lastResults) {
                if (f.getName().toLowerCase().contains(queryLower) || (searchContent && isContentMatch(f, queryLower))) {
                    filtered.add(f);
                    worker.doPublish(f);
                }
            }
            if (!worker.isCancelled()) {
                lastQuery   = queryLower;
                lastResults = filtered;
            }
            return;
        }

        List<File> found = Collections.synchronizedList(new ArrayList<>());
        ForkJoinPool pool = ForkJoinPool.commonPool();

        try {
            pool.submit(() -> scanParallel(root.toPath(), queryLower, worker, found, pool, searchContent)).get(180, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        if (!worker.isCancelled()) {
            lastQuery   = queryLower;
            lastResults = new ArrayList<>(found);
        }
    }

    private boolean isContentMatch(File file, String query) {
        if (file.length() > 10 * 1024 * 1024) return false;
        try {
            String content = Files.readString(file.toPath()).toLowerCase();
            return content.contains(query);
        } catch (Exception e) {
            return false;
        }
    }

    private void scanParallel(Path root, String query, InternalWorker worker, List<File> found, ForkJoinPool pool, boolean searchContent) {
        List<Path> topDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path entry : stream) {
                if (worker.isCancelled()) break;
                String name = entry.getFileName().toString();
                boolean nameMatch = name.toLowerCase().contains(query);
                boolean contentMatch = searchContent && !nameMatch && Files.isRegularFile(entry) && isContentMatch(entry.toFile(), query);

                if (nameMatch || contentMatch) {
                    File f = entry.toFile();
                    found.add(f);
                    worker.doPublish(f);
                }
                if (Files.isDirectory(entry)) {
                    topDirs.add(entry);
                }
            }
        } catch (Exception ignored) {}

        List<RecursiveAction> tasks = new ArrayList<>();
        for (Path dir : topDirs) {
            RecursiveAction task = new RecursiveAction() {
                @Override protected void compute() {
                    if (worker.isCancelled()) return;
                    walkDir(dir, query, worker, found, searchContent);
                }
            };
            tasks.add(task);
            pool.execute(task);
        }
        for (RecursiveAction t : tasks) {
            try { t.get(); } catch (Exception ignored) {}
        }
    }

    private void walkDir(Path dir, String query, InternalWorker worker, List<File> found, boolean searchContent) {
        try {
            Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                    if (worker.isCancelled()) return FileVisitResult.TERMINATE;
                    String name = d.getFileName().toString();
                    if (name.toLowerCase().contains(query)) {
                        File f = d.toFile();
                        found.add(f);
                        worker.doPublish(f);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                    if (worker.isCancelled()) return FileVisitResult.TERMINATE;
                    String name = file.getFileName().toString();
                    boolean nameMatch = name.toLowerCase().contains(query);
                    boolean contentMatch = searchContent && !nameMatch && a.isRegularFile() && isContentMatch(file.toFile(), query);

                    if (nameMatch || contentMatch) {
                        found.add(file.toFile());
                        worker.doPublish(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, java.io.IOException exc) {
                    return FileVisitResult.CONTINUE; // пропускаем недоступные файлы, но не останавливаемся
                }
            });
        } catch (Exception ignored) {}
    }

    public void clearCache() {
        lastQuery = null;
        lastResults = null;
    }

    public static abstract class InternalWorker extends SwingWorker<Void, File> {
        public void doPublish(File file) {
            publish(file);
        }
    }
}