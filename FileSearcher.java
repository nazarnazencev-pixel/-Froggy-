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

    public void searchAndPublish(File root, String query, InternalWorker worker,
                                 boolean searchContent, boolean isRegex, List<String> extensions) {
        String queryLower = query.toLowerCase();
        String cacheKey = queryLower + "|" + (extensions != null ? String.join(",", extensions) : "");

        if (lastQuery != null && cacheKey.startsWith(lastQuery) && lastResults != null) {
            List<File> filtered = new ArrayList<>();
            for (File f : lastResults) {
                if (matchesFile(f, queryLower, extensions) ||
                        (searchContent && isContentMatch(f, queryLower))) {
                    filtered.add(f);
                    worker.doPublish(f);
                }
            }
            if (!worker.isCancelled()) {
                lastQuery   = cacheKey;
                lastResults = filtered;
            }
            return;
        }

        List<File> found = Collections.synchronizedList(new ArrayList<>());
        ForkJoinPool pool = ForkJoinPool.commonPool();

        try {
            pool.submit(() -> scanParallel(root.toPath(), queryLower, extensions, worker, found, pool, searchContent))
                    .get(180, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        if (!worker.isCancelled()) {
            lastQuery   = cacheKey;
            lastResults = new ArrayList<>(found);
        }
    }

    private boolean matchesFile(File file, String query, List<String> extensions) {
        String nameLower = file.getName().toLowerCase();

        // Фильтр по расширению
        if (extensions != null && !extensions.isEmpty()) {
            boolean extMatch = false;
            for (String ext : extensions) {
                if (nameLower.endsWith(ext)) {
                    extMatch = true;
                    break;
                }
            }
            if (!extMatch) return false;
        }

        // Фильтр по имени
        return query.isEmpty() || nameLower.contains(query);
    }

    private boolean isContentMatch(File file, String query) {
        if (query.isEmpty()) return false;
        if (file.length() > 10 * 1024 * 1024) return false;
        try {
            String content = Files.readString(file.toPath()).toLowerCase();
            return content.contains(query);
        } catch (Exception e) {
            return false;
        }
    }

    private void scanParallel(Path root, String query, List<String> extensions,
                              InternalWorker worker, List<File> found,
                              ForkJoinPool pool, boolean searchContent) {
        List<Path> topDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path entry : stream) {
                if (worker.isCancelled()) break;

                boolean nameMatch = matchesFile(entry.toFile(), query, extensions);
                boolean contentMatch = searchContent && !nameMatch
                        && Files.isRegularFile(entry)
                        && isContentMatch(entry.toFile(), query);

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
                    walkDir(dir, query, extensions, worker, found, searchContent);
                }
            };
            tasks.add(task);
            pool.execute(task);
        }
        for (RecursiveAction t : tasks) {
            try { t.get(); } catch (Exception ignored) {}
        }
    }

    private void walkDir(Path dir, String query, List<String> extensions,
                         InternalWorker worker, List<File> found, boolean searchContent) {
        try {
            Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE, new SimpleFileVisitor<>() {

                        @Override
                        public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                            if (worker.isCancelled()) return FileVisitResult.TERMINATE;
                            // Папки показываем только если расширения не заданы
                            if (extensions == null || extensions.isEmpty()) {
                                String name = d.getFileName().toString().toLowerCase();
                                if (query.isEmpty() || name.contains(query)) {
                                    File f = d.toFile();
                                    found.add(f);
                                    worker.doPublish(f);
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                            if (worker.isCancelled()) return FileVisitResult.TERMINATE;

                            boolean nameMatch = matchesFile(file.toFile(), query, extensions);
                            boolean contentMatch = searchContent && !nameMatch
                                    && a.isRegularFile()
                                    && isContentMatch(file.toFile(), query);

                            if (nameMatch || contentMatch) {
                                found.add(file.toFile());
                                worker.doPublish(file.toFile());
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, java.io.IOException exc) {
                            return FileVisitResult.CONTINUE;
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
