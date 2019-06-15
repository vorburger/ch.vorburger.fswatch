/*
 * #%L
 * ch.vorburger.fswatch
 * %%
 * Copyright (C) 2018 - 2018 Michael Vorburger.ch
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
package ch.vorburger.fswatch.test;

import java.io.File;
import java.io.IOException;

import ch.vorburger.fswatch.DirectoryWatcher;
import ch.vorburger.fswatch.DirectoryWatcherBuilder;
import ch.vorburger.fswatch.FileWatcherBuilder;

/**
 * Example illustrating fswatch usage.
 *
 * See also src/main/java/ch/vorburger/fswatch/DirectoryWatcherMain.java
 *
 * @author Michael Vorburger.ch
 */
public class ExampleMain {

    private static void watchDirectoryExample() throws IOException {
        File dir = new File(".");
        try (DirectoryWatcher dw = new DirectoryWatcherBuilder().path(dir)
                .listener((path, changeKind) -> System.out.println(changeKind.name() + " " + path.toString())).build()) {

            System.out.println("Press Enter to stop; now watching for changed in directory: " + dir.getAbsolutePath());
            System.in.read();
        }
    }

    private static void watchFileExample() throws IOException {
        File file = new File("pom.xml");
        try (DirectoryWatcher dw = new FileWatcherBuilder().path(file)
                .listener((path, changeKind) -> System.out.println(changeKind.name() + " " + path.toString())).build()) {

            System.out.println("Press Enter to stop; now watching for changed in file: " + file.getAbsolutePath());
            System.in.read();
        }
    }

    public static void main(String[] args) throws IOException {
        watchFileExample();
        watchDirectoryExample();
    }
}
