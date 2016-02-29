package com.psddev.styleguide;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

class WatchDirectory {

    private final WatchService watcher;
    private final Map<WatchKey, Path> watchKeys;
    private final CliLogger logger;
    private boolean debug;

    private BiFunction<Path, WatchEvent.Kind<Path>, Boolean> processChangeFunction;
    private Consumer<InterruptedException> onInterruptFunction;
    private Consumer<ClosedWatchServiceException> onServiceCloseFunction;

    public WatchDirectory(Collection<Path> directories) throws IOException {

        watcher = FileSystems.getDefault().newWatchService();
        watchKeys = new HashMap<>();
        logger = CliLogger.getLogger();

        for (Path directory : directories) {
            registerAll(directory, watcher, watchKeys);
        }
    }

    public void setProcessChangeFunction(BiFunction<Path, WatchEvent.Kind<Path>, Boolean> processChangeFunction) {
        this.processChangeFunction = processChangeFunction;
    }

    public void setOnInterruptFunction(Consumer<InterruptedException> onInterruptFunction) {
        this.onInterruptFunction = onInterruptFunction;
    }

    public void setOnServiceCloseFunction(Consumer<ClosedWatchServiceException> onServiceCloseFunction) {
        this.onServiceCloseFunction = onServiceCloseFunction;
    }

    public void debug() {
        debug = true;
    }

    protected boolean processChange(Path path, WatchEvent.Kind<Path> watchEventKind) {

        if (processChangeFunction != null) {
            return processChangeFunction.apply(path, watchEventKind);
        } else {
            String pathType = null;

            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                pathType = "File";

            } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                pathType = "Directory";
            }

            String eventType = null;

            if (watchEventKind == StandardWatchEventKinds.ENTRY_CREATE) {
                eventType = "created";

            } else if (watchEventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                eventType = "deleted";

            } else if (watchEventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                eventType = "modified";
            }

            if (pathType != null && eventType != null) {
                logger.green().and(">>")
                        .reset().and(" ", pathType, " \"")
                        .green().and(path)
                        .reset().and("\" ", eventType, ".")
                        .log();

                return true;
            } else {
                return false;
            }
        }
    }

    protected void onInterrupt(InterruptedException e) {
        if (onInterruptFunction != null) {
            onInterruptFunction.accept(e);
        } else {
            logger.yellow("Watch service interrupted.");
        }
    }

    protected void onServiceClose(ClosedWatchServiceException e) {
        if (onServiceCloseFunction != null) {
            onServiceCloseFunction.accept(e);
        } else {
            logger.yellow("Watch service closed.");
        }
    }

    public void start() {

        boolean changed = true;

        for (;;) {
            if (changed) {
                logger.green("Waiting for changes...");
                changed = false;
            }

            // wait for key to be signaled
            WatchKey watchKey;
            try {
                watchKey = watcher.take();

            } catch (ClosedWatchServiceException e) {
                onServiceClose(e);
                break;

            } catch (InterruptedException e) {
                onInterrupt(e);
                break;
            }

            Path directory = watchKeys.get(watchKey);
            if (directory == null) {
                if (debug) {
                    logger.yellow("WatchKey not recognized!!");
                }
                continue;
            }

            for (WatchEvent<?> we : watchKey.pollEvents()) {

                if (StandardWatchEventKinds.OVERFLOW == we.kind()) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> watchEvent = (WatchEvent<Path>) we;

                WatchEvent.Kind<Path> watchEventKind = watchEvent.kind();

                Path path = directory.resolve(watchEvent.context());

                // if directory is created, then register it and its sub-directories
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                        && watchEventKind == StandardWatchEventKinds.ENTRY_CREATE) {

                    try {
                        registerAll(path, watcher, watchKeys);

                    } catch (IOException ex) {
                        logger.red().and("Failed to watch new directory: ")
                                .reset().and(path)
                                .log();
                    }
                }

                // Skipping all symbolic links
                if (!Files.isSymbolicLink(path)) {
                    changed = processChange(path, watchEventKind);
                }
            }

            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.
            boolean valid = watchKey.reset();
            if (!valid) {
                watchKeys.remove(watchKey);

                // all directories are inaccessible
                if (watchKeys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private static void registerAll(Path directory, WatchService watcher, Map<WatchKey, Path> watchKeys) throws IOException {

        // register directory and sub-directories
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path subDirectory, BasicFileAttributes attrs) throws IOException {

                WatchKey watchKey = subDirectory.register(watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                watchKeys.put(watchKey, subDirectory);

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
