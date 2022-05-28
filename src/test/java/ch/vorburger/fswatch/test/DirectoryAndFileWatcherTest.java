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
package ch.vorburger.fswatch.test;

import static com.google.common.base.Charsets.US_ASCII;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ch.vorburger.fswatch.DirectoryWatcher;
import ch.vorburger.fswatch.DirectoryWatcher.ChangeKind;
import ch.vorburger.fswatch.DirectoryWatcherBuilder;
import ch.vorburger.fswatch.FileWatcherBuilder;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import java.io.File;
import java.nio.file.FileSystems;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link DirectoryWatcherBuilder} and @link FileWatcherBuilder}.
 *
 * @author Michael Vorburger.ch
 */
public class DirectoryAndFileWatcherTest {

    AssertableExceptionHandler assertableExceptionHandler;
    volatile boolean changed;

    @BeforeClass
    static public void configureSlf4jSimpleShowAllLogs() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    @Test
    public void testFileWatcher() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        final File dir = new File("target/tests/FileWatcherTest/");
        final File subDir = new File(dir.getParentFile(), "subDir");
        dir.mkdirs();
        subDir.mkdirs();
        File file = new File(dir, "yo.txt");
        Files.asCharSink(file, US_ASCII).write("yo");
        Files.asCharSink(new File(subDir, "bo.txt"), US_ASCII).write("bo");

        changed = false;
        try (DirectoryWatcher dw = new FileWatcherBuilder().path(file).listener((p, c) -> {
            assertFalse(changed); // We want this to only be called once
            changed = true;
        }).exceptionHandler(assertableExceptionHandler).build()) {

            // We want it to call the listener once for setup, even without any change
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(file, US_ASCII).write("ho");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(file, US_ASCII).write("do");
            await().atMost(20, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            file.delete();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(file, US_ASCII).write("yo");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            File anotherFile = new File(dir, "another.txt");
            Files.asCharSink(anotherFile, US_ASCII).write("another");
            await().atMost(5, SECONDS).until(() -> changed, is(false));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        }
    }

    @Test
    public void testDirectoryWatcher() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        final File dir = new File("target/tests/DirectoryWatcherTest/some/sub/directory");
        dir.mkdirs();
        final File subDir = new File(dir.getParentFile(), "another");
        File newFile = new File(subDir, "yo.txt");
        newFile.delete();
        subDir.delete();

        changed = false;
        try (DirectoryWatcher dw = new DirectoryWatcherBuilder().path(dir.getParentFile().getParentFile()).listener((p, c) -> {
            assertFalse(changed); // We want this to only be called once
            changed = true;
        }).exceptionHandler(assertableExceptionHandler).build()) {

            // We want it to call the listener once for setup, even without any change
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            // Note we're creating another new sub-directory (because we want to
            // test that not only existing but also new directories are scanned)
            changed = false;
            assertTrue(subDir.mkdirs());
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(newFile, US_ASCII).write("yo");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await(). // conditionEvaluationListener(new ConditionEvaluationLogger()).
                    atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(newFile, US_ASCII).write("do");
            await().atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            newFile.delete();
            await().atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        }
    }

    @Test
    public void testExistingFilesDirectoryWatcher() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        File file = new File("target/tests/DirectoryWatcherTest/existing");
        MoreFiles.deleteRecursively(file.getParentFile().toPath());
        file.getParentFile().mkdirs();
        Files.asCharSink(file, US_ASCII).write("yo");
        changed = false;
        try (DirectoryWatcher dw = new DirectoryWatcherBuilder().existingFiles(true).path(file.getParentFile()).listener((p, c) -> {
            if (!p.toFile().isDirectory()) {
                assertFalse(changed); // We want this to only be called once
                changed = true;
            }
        }).exceptionHandler(assertableExceptionHandler).build()) {
            // We want it to call the listener once for setup for the file (not the directory), even without any change
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(file, US_ASCII).write("ho");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        }
    }

    @Test
    public void testFilteredDirectoryWatcher() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        final File dir = new File("target/tests/DirectoryWatcherTest/some/sub/directory");
        dir.mkdirs();
        final File subDir = new File(dir.getParentFile(), "another");
        File newFile = new File(subDir, "yo.txt");
        File newFile2 = new File(dir, "yo.txt");
        newFile.delete();
        newFile2.delete();
        subDir.delete();

        try (DirectoryWatcher dw = new DirectoryWatcherBuilder().path(dir.getParentFile().getParentFile())
                .fileFilter(file -> "another".equals(file.getName())).listener((p, c) -> {
                    assertFalse(changed); // We want this to only be called once
                    changed = true;
                }).exceptionHandler(assertableExceptionHandler).build()) {

            assertTrue(subDir.mkdirs());
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(newFile, US_ASCII).write("yo");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            Files.asCharSink(newFile2, US_ASCII).write("do");
            await().atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        }
    }

    @Test(expected = AssertionError.class)
    public void testDirectoryWatcherListenerExceptionPropagation() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        final File testFile = new File("target/tests/DirectoryWatcherTest/someFile");
        testFile.delete();
        final File dir = testFile.getParentFile();
        dir.mkdirs();
        try (DirectoryWatcher dw = new DirectoryWatcherBuilder().path(dir).quietPeriodInMS(0).listener((p, c) -> {
            fail("duh!");
        }).exceptionHandler(assertableExceptionHandler).build()) {

            Files.asCharSink(testFile, US_ASCII).write("yo");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        }
    }

    @Test
    public void testFileWatcherWithSelectedChangeKinds() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        final File dir = new File("target/tests/FileWatcherTest/");
        final File subDir = new File(dir.getParentFile(), "subDir");
        dir.mkdirs();
        subDir.mkdirs();
        File file = new File(dir, "yo.txt");
        Files.asCharSink(file, US_ASCII).write("yo");
        Files.asCharSink(new File(subDir, "bo.txt"), US_ASCII).write("bo");

        changed = false;
        try (DirectoryWatcher dw = new FileWatcherBuilder().path(file).listener((p, c) -> {
            assertFalse(changed); // We want this to only be called once
            changed = true;
        }).eventKinds(ChangeKind.MODIFIED, ChangeKind.DELETED).exceptionHandler(assertableExceptionHandler).build()) {

            // We want it to call the listener once for setup, even without any change
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(file, US_ASCII).write("ho");
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
            await().atMost(30, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            Files.asCharSink(file, US_ASCII).write("do");
            await().atMost(20, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

            changed = false;
            file.delete();
            await().atMost(5, SECONDS).until(() -> changed, is(true));
            assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        }
    }

    @Test
    public void testFileWatcherRelativeFileNPE() throws Throwable {
        new FileWatcherBuilder().path(new File("LICENSE")).listener((path, changeKind) -> {}).build().close();
    }

    @Test
    public void testDirectoryWatcherRelativeFileNPE() throws Throwable {
        new DirectoryWatcherBuilder().path(new File(".")).listener((path, changeKind) -> {}).build().close();
    }

    @Test
    public void testFileWatcherRelativePathNPE() throws Throwable {
        new FileWatcherBuilder().path(FileSystems.getDefault().getPath("LICENSE")).listener((path, changeKind) -> {}).build().close();
    }

    @Test
    public void testDirectoryWatcherRelativePathNPE() throws Throwable {
        new DirectoryWatcherBuilder().path(FileSystems.getDefault().getPath(".")).listener((path, changeKind) -> {}).build().close();
    }
}
