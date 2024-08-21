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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import ch.vorburger.fswatch.DirectoryWatcher.ChangeKind;
import ch.vorburger.fswatch.DirectoryWatcher.ExceptionHandler;
import ch.vorburger.fswatch.DirectoryWatcher.Listener;

import static java.util.Objects.requireNonNull;

/**
 * Builder for {@link DirectoryWatcher}.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("hiding")
public class DirectoryWatcherBuilder {

    protected @Nullable Path path;
    protected @Nullable Listener listener;
    protected ExceptionHandler exceptionHandler = new Slf4jLoggingExceptionHandler();
    protected long quietPeriodInMS = 100;
    protected @Nullable FileFilter fileFilter;
    protected ChangeKind[] eventKinds = { ChangeKind.DELETED, ChangeKind.MODIFIED };
    protected boolean existingFiles = false;

    public DirectoryWatcherBuilder path(File directory) {
        return path(directory.toPath());
    }

    public DirectoryWatcherBuilder path(Path directory) {
        if (path != null) {
            throw new IllegalStateException("path already set");
        }
        path = directory;
        return this;
    }

    public DirectoryWatcherBuilder listener(Listener listener) {
        if (this.listener != null) {
            throw new IllegalStateException("listener already set");
        }
        this.listener = listener;
        return this;
    }

    public DirectoryWatcherBuilder eventKinds(ChangeKind... eventKinds) {
        this.eventKinds = eventKinds;
        return this;
    }

    public DirectoryWatcherBuilder exceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = requireNonNull(exceptionHandler);
        return this;
    }

    public DirectoryWatcherBuilder quietPeriodInMS(long quietPeriodInMS) {
        this.quietPeriodInMS = quietPeriodInMS;
        return this;
    }

    /**
     * Filter out directories you don't want to be watched.
     * @param fileFilter match files that don't need to be watched
     * @return this
     */
    public DirectoryWatcherBuilder fileFilter(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
        return this;
    }

    /**
     * Whether the Listener should initially be invoked once for each existing file (but not directory).
     * Defaults to false.
     *
     * @param existing true if yes, false if not
     * @return this
     */
    public DirectoryWatcherBuilder existingFiles(boolean existing) {
        existingFiles = existing;
        return this;
    }

    public DirectoryWatcher build() throws IOException {
        // Copy/paste into child class, for null safety; please keep in sync
        if (path == null)
            throw new IllegalStateException("path not set");
        if (!path.toFile().exists())
            throw new IllegalStateException("path does not exist: " + path.toString());
        if (listener == null)
            throw new IllegalStateException("listener not set");
        if (!path.toFile().isDirectory())
            throw new IllegalStateException(
                    "When using DirectoryWatcherBuilder, set path() to a directory, not a file (use FileWatcherBuilder to watch a single file)");
        DirectoryWatcherImpl watcher = new DirectoryWatcherImpl(true, path, getQuietListener(listener), fileFilter,
                exceptionHandler, eventKinds);
        firstListenerNotification();
        return watcher;
    }

    // We intentionally want to first call the listener once for setup, even without any change detected
    protected void firstListenerNotification() {
        if (path == null)
            throw new IllegalStateException("path not set");
        if (listener == null)
            throw new IllegalStateException("listener not set");
        try {
            listener.onChange(path, ChangeKind.MODIFIED);

            if (existingFiles) {
                try (Stream<Path> stream = Files.walk(path).filter(Files::isRegularFile)) {
                    stream.forEach(file -> {
                        try {
                            listener.onChange(file, ChangeKind.MODIFIED);
                        } catch (Throwable e) {
                            exceptionHandler.onException(e);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            exceptionHandler.onException(e);
        }
    }

    protected Listener getQuietListener(Listener listenerToWrap) {
        return new QuietPeriodListener(quietPeriodInMS, listenerToWrap, exceptionHandler);
    }
}
