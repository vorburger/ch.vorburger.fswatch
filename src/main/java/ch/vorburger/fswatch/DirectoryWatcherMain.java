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
import java.io.IOException;

/**
 * main() for DirectoryWatcher.
 *
 * See also src/test/java/ch/vorburger/fswatch/test/ExampleMain.java.
 *
 * @author Michael Vorburger.ch
 */
public class DirectoryWatcherMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("USAGE: <root-directory-to-watch-for-changes>");
            return;
        }

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");

        File dir = new File(args[0]);
        DirectoryWatcherImpl dw = (DirectoryWatcherImpl) new DirectoryWatcherBuilder().path(dir)
                // Using explicit anonymous inner classes instead of Lambdas for clarity to readers
                .listener((path, changeKind) -> System.out.println(changeKind.toString() + " " + path.toString())).exceptionHandler(t -> t.printStackTrace()).build();

        // This is just because it's a main(), you normally would NOT do this:
        dw.thread.join();

        // You must close() a DirectoryWatcher when you don't need it anymore
        // (In this main() scenario this will unlikely ever actually get reached; this is just an illustration.)
        dw.close();
    }

}
