/*
 * #%L
 * ch.vorburger.fswatch
 * %%
 * Copyright (C) 2015 - 2018 Michael Vorburger.ch
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
import java.nio.file.Path;

import ch.vorburger.fswatch.DirectoryWatcher.ChangeKind;
import ch.vorburger.fswatch.DirectoryWatcher.ExceptionHandler;
import ch.vorburger.fswatch.DirectoryWatcher.Listener;

/**
 * Builder for {@link DirectoryWatcher}.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("hiding")
public class DirectoryWatcherBuilder {

    protected Path path;
    protected Listener listener;
    protected ExceptionHandler exceptionHandler = new Slf4jLoggingExceptionHandler();
    protected long quietPeriodInMS = 100;
    protected FileFilter fileFilter;
    protected ChangeKind[] eventKinds = new ChangeKind[] {ChangeKind.CREATED,ChangeKind.MODIFIED};

    public DirectoryWatcherBuilder path(File directory) {
        return path(directory.toPath());
    }

    public DirectoryWatcherBuilder path(Path directory) {
        if (this.path != null) {
            throw new IllegalStateException("path already set");
        }
        this.path = directory;
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
        this.exceptionHandler = exceptionHandler;
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

    public DirectoryWatcher build() throws IOException {
        check();
        if (!path.toFile().isDirectory()) {
            throw new IllegalStateException("When using DirectoryWatcherBuilder, set path() to a directory, not a file (use FileWatcherBuilder to watch a single file)");
        }
        DirectoryWatcherImpl watcher = new DirectoryWatcherImpl(true, path, getQuietListener(listener), fileFilter, exceptionHandler,eventKinds);
        firstListenerNotification();
        return watcher;
    }

    // We intentionally want to first call the listener once for setup, even without any change detected
    protected void firstListenerNotification() {
        try {
            listener.onChange(path, ChangeKind.MODIFIED);
        } catch (Throwable e) {
            exceptionHandler.onException(e);
        }
    }

    protected void check() {
        if (this.path == null) {
            throw new IllegalStateException("path not set");
        }
        if (!this.path.toFile().exists()) {
            throw new IllegalStateException("path does not exist: " + this.path.toString());
        }
        if (this.listener == null) {
            throw new IllegalStateException("listener not set");
        }
        if (this.exceptionHandler == null) {
            throw new IllegalStateException("exceptionHandler not set");
        }
    }

    protected Listener getQuietListener(Listener listenerToWrap) {
        return new QuietPeriodListener(quietPeriodInMS, listenerToWrap, exceptionHandler);
    }

}
