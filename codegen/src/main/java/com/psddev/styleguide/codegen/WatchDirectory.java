package com.psddev.styleguide.codegen;

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

/**
 * A directory that is being "watched" for changes by an instance of a
 * {@link ViewClassGenerator}.
 */
class WatchDirectory {

    private final WatchService watcher;
    private final Map<WatchKey, Path> watchKeys;
    private final CliLogger logger;
    private boolean debug;

    private BiFunction<Path, WatchEvent.Kind<Path>, Boolean> processChangeFunction;
    private Consumer<InterruptedException> onInterruptFunction;
    private Consumer<ClosedWatchServiceException> onServiceCloseFunction;

    /**
     * Creates a new watch directory for the collection of directory paths and
     * registers each with the underlying watch service.
     *
     * @param directories the directories to watch.
     * @throws IOException if any of the directories fail to be registered.
     */
    public WatchDirectory(Collection<Path> directories) throws IOException {

        watcher = FileSystems.getDefault().newWatchService();
        watchKeys = new HashMap<>();
        logger = CliLogger.getLogger();

        for (Path directory : directories) {
            registerAll(directory, watcher, watchKeys);
        }
    }

    /**
     * Sets a callback function to be executed whenever a change is detected.
     *
     * @param processChangeFunction the callback function.
     */
    public void setProcessChangeFunction(BiFunction<Path, WatchEvent.Kind<Path>, Boolean> processChangeFunction) {
        this.processChangeFunction = processChangeFunction;
    }

    /**
     * Sets a callback function to be executed if the watch service is interrupted while waiting.
     *
     * @param onInterruptFunction the callback function.
     */
    public void setOnInterruptFunction(Consumer<InterruptedException> onInterruptFunction) {
        this.onInterruptFunction = onInterruptFunction;
    }

    /**
     * Sets a callback function to be executed if the watch service is closed,
     * or it is closed while waiting for the next key.
     *
     * @param onServiceCloseFunction the callback function.
     */
    public void setOnServiceCloseFunction(Consumer<ClosedWatchServiceException> onServiceCloseFunction) {
        this.onServiceCloseFunction = onServiceCloseFunction;
    }

    /**
     * Enables debug mode.
     */
    public void debug() {
        debug = true;
    }

    /**
     * Processes a change that was detected by the watch directory service.
     *
     * @param path the path that changed.
     * @param watchEventKind the kind of change that was detected.
     * @return true if change warrants a regeneration of the view classes.
     */
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
                logger.green().append(">>")
                        .reset().append(" ", pathType, " \"")
                        .green().append(path)
                        .reset().append("\" ", eventType, ".")
                        .log();

                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Calls the onInterrupt function that was set via
     * {@link #setOnInterruptFunction(Consumer)}. Sub-classes may override this
     * method to change behavior on interrupt.
     *
     * @param e the exception that triggered the interrupt.
     */
    protected void onInterrupt(InterruptedException e) {
        if (onInterruptFunction != null) {
            onInterruptFunction.accept(e);
        } else {
            logger.yellow("Watch service interrupted.");
        }
    }

    /**
     * Calls the onServiceClose function that was set via
     * {@link #setOnServiceCloseFunction(Consumer)}. Sub-classes may override
     * this method to change the behavior on service close.
     *
     * @param e the exception that triggered the close of the watch service.
     */
    protected void onServiceClose(ClosedWatchServiceException e) {
        if (onServiceCloseFunction != null) {
            onServiceCloseFunction.accept(e);
        } else {
            logger.yellow("Watch service closed.");
        }
    }

    /**
     * Starts the directory watch service. This method never returns unless the
     * service is interrupted, closed, or if all of the directories that are
     * being watched are removed.
     */
    public void start() {

        boolean changed = true;

        for (;;) {
            if (changed) {
                logger.green("\nWaiting for changes...");
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
                        logger.red().append("Failed to watch new directory: ")
                                .reset().append(path)
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

    /*
     * Registers the given {@code directory} and all of its sub-directories with the
     * given watch service on the given {@code watchKeys}.
     */
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
