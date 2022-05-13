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

import ch.vorburger.fswatch.DirectoryWatcher.ChangeKind;
import ch.vorburger.fswatch.DirectoryWatcher.Listener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Builder which watches one single file for changes.
 *
 * @author Michael Vorburger.ch
 */
public class FileWatcherBuilder extends DirectoryWatcherBuilder {

    @Override public FileWatcherBuilder path(File fileNotDirectory) {
        return (FileWatcherBuilder) super.path(fileNotDirectory.getAbsoluteFile());
    }

    @Override public FileWatcherBuilder path(Path fileNotDirectory) {
        return (FileWatcherBuilder) super.path(fileNotDirectory.toAbsolutePath());
    }

    @Override
    public DirectoryWatcher build() throws IOException {
        check();
        if (!path.toFile().isFile()) {
            throw new IllegalStateException("When using FileWatcherBuilder, set path() to a single file, not a directory (use DirectoryWatcherBuilder to watch a directory, and it's subdirectories)");
        }
        // NOTE We do want to wrap the FileWatcherListener inside the QuietPeriodListener, and not the other way around!
        Listener wrap = getQuietListener(new FileWatcherListener(path, listener));
        DirectoryWatcherImpl watcher = new DirectoryWatcherImpl(false, path.getParent(), wrap, fileFilter, exceptionHandler, eventKinds);
        firstListenerNotification();
        return watcher;
    }

    /**
     * Protected inner {@link Listener} class.
     */
    protected static class FileWatcherListener implements Listener {

        private final Listener delegate;
        private final Path fileToWatch;

        protected FileWatcherListener(Path fileToWatch, Listener listenerToWrap) {
            this.fileToWatch = fileToWatch;
            this.delegate = listenerToWrap;
        }

        @Override
        public void onChange(Path path, ChangeKind changeKind) throws Throwable {
            if (path.equals(fileToWatch)) {
                delegate.onChange(path, changeKind);
            }
        }
    }


}
