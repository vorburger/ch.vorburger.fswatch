/*
 * #%L
 * ch.vorburger.fswatch
 * %%
 * Copyright (C) 2015 - 2022 Michael Vorburger.ch
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ch.vorburger.fswatch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DirectoryWatcher based on java.nio.file.WatchService.
 *
 * @author Michael Vorburger.ch
 */
// intentionally package local, for now
public class DirectoryWatcherImpl implements DirectoryWatcher {
    private final static Logger log = LoggerFactory.getLogger(DirectoryWatcherImpl.class);

    protected final WatchService watcher = FileSystems.getDefault().newWatchService(); // better final, as it will be accessed by both threads (normally OK either way, but still)
    protected final Thread thread;
    protected final List<ChangeKind> changeKindsList = new ArrayList<>();

    protected DirectoryWatcherImpl(boolean watchSubDirectories, final Path watchBasePath, final Listener listener,
            FileFilter fileFilter, ExceptionHandler exceptionHandler) throws IOException {
        this(watchSubDirectories, watchBasePath, listener, fileFilter, exceptionHandler,
                new ChangeKind[] { ChangeKind.MODIFIED, ChangeKind.DELETED });
    }

    // protected because typical code should use the DirectoryWatcherBuilder instead of this directly
    protected DirectoryWatcherImpl(boolean watchSubDirectories, final Path watchBasePath, final Listener listener,
            @Nullable FileFilter fileFilter, ExceptionHandler exceptionHandler, ChangeKind[] eventKinds)
            throws IOException {
        if (!watchBasePath.toFile().isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + watchBasePath);
        }
        changeKindsList.addAll(Arrays.asList(eventKinds));

        register(watchSubDirectories, watchBasePath, fileFilter);
        Runnable r = () -> {
            for (;;) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (ClosedWatchServiceException e) {
                    log.debug(
                            "WatchService take() interrupted by ClosedWatchServiceException, terminating Thread (as planned).");
                    return;
                } catch (InterruptedException e) {
                    log.debug("Thread InterruptedException, terminating (as planned, if caused by close()).");
                    return;
                }
                Path watchKeyWatchablePath = (Path) key.watchable();
                // We have a polled event, now we traverse it and receive all the states from it
                for (WatchEvent<?> event : key.pollEvents()) {

                    Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        // TODO Not sure how to correctly "handle" an Overflow.. ?
                        log.error("Received {} (TODO how to handle?)", kind.name());
                        continue;
                    }

                    Path relativePath = (Path) event.context();
                    if (relativePath == null) {
                        log.error("Received {} but event.context() == null: {}", kind.name(), event);
                        continue;
                    }
                    Path absolutePath = watchKeyWatchablePath.resolve(relativePath);
                    log.trace("Received {} for: {}", kind.name(), absolutePath);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(absolutePath)) { // don't NOFOLLOW_LINKS
                        try {
                            register(watchSubDirectories, watchBasePath, fileFilter);
                        } catch (IOException e) {
                            exceptionHandler.onException(e);
                        }
                    }

                    try {
                        ChangeKind ourKind = null;
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            ourKind = ChangeKind.CREATED;
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            ourKind = ChangeKind.MODIFIED;
                        } else {
                            ourKind = ChangeKind.DELETED;
                        }
                        if (changeKindsList.contains(ourKind)) { // Only send the evnts that the client is interested in
                            listener.onChange(absolutePath, ourKind);
                        }
                    } catch (Throwable e) {
                        exceptionHandler.onException(e);
                    }

                }
                key.reset();
            }

        };
        String threadName = DirectoryWatcherImpl.class.getSimpleName() + ": " + watchBasePath;
        thread = new Thread(r, threadName);
        thread.setDaemon(true);
        // Because we're catch-ing expected exceptions above, this normally
        // should never be needed, but still be better safe than sorry.. ;-)
        thread.setUncaughtExceptionHandler((t, e) -> exceptionHandler.onException(e));
        thread.start();
    }

    private void register(boolean watchSubDirectories, final Path path, @Nullable FileFilter fileFilter)
            throws IOException {
        if (watchSubDirectories) {
            registerAll(path, fileFilter);
        } else {
            registerOne(path);
        }
    }

    private void registerOne(final Path path) throws IOException {
        path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (log.isTraceEnabled()) {
            log.trace("Registered: {}", path);
        }
    }

    // Implementation inspired by https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java, from https://docs.oracle.com/javase/tutorial/essential/io/notification.html

    private void registerAll(final Path basePath, @Nullable FileFilter fileFilter) throws IOException {
        // register basePath directory and sub-directories
        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (fileFilter == null || !fileFilter.accept(dir.toFile())) {
                    registerOne(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void close() {
        // The order here is important - first we stop the Thread, then close the Watcher.
        thread.interrupt();
        try {
            watcher.close();
        } catch (IOException e) {
            log.error("WatchService close() failed", e);
        }
    }

    @Override
    public String toString() {
        return thread.getName();
    }
}
