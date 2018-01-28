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

import java.io.Closeable;
import java.nio.file.Path;

/**
 * Watch a directory and be notified on your Listener for changes in it.
 *
 * @author Michael Vorburger.ch
 */
public interface DirectoryWatcher extends Closeable {

    public enum ChangeKind {
        MODIFIED, DELETED
    }

    /**
     * Listener for change notifications.
     */
    interface Listener {
        /**
         * Called back method.
         * @param path Path to what caused the change. Note that when watching directory trees, we get a notification of one file (or new/deleted directory) causing it, not the registered root directory.
         * @param changeKind whether the change was a modification or a deletion
         * @throws Throwable if anything went wrong
         */
        void onChange(Path path, ChangeKind changeKind) throws Throwable;
    }

    /**
     * Handles exceptions which occur during {@link Listener#onChange(Path, ChangeKind)}.
     */
    interface ExceptionHandler {
        void onException(Throwable t);
    }

    @Override String toString();

    @Override void close(); // do NOT throws (IO)Exception
}
