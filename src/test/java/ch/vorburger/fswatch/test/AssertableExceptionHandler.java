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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.vorburger.fswatch.DirectoryWatcher.ExceptionHandler;
import ch.vorburger.fswatch.Slf4jLoggingExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExceptionHandler useful in multi-threaded tests.
 *
 * @author Michael Vorburger.ch
 */
public class AssertableExceptionHandler extends Slf4jLoggingExceptionHandler implements ExceptionHandler {
    private final static Logger log = LoggerFactory.getLogger(AssertableExceptionHandler.class);

    // This probably isn't 100% concurrency kosher, but "good enough" to cover the tests..
    private final Object lockObject = new Object();
    private volatile Throwable lastThrowable = null;

    @Override
    public void onException(Throwable t) {
        synchronized (lockObject) {
            super.onException(t);
            if (lastThrowable == null) {
                lastThrowable = t;
            } else {
                log.error("There is already a previous lastThrowable which hasn't been asserted, yet; so ignoring this", t);
            }
        }
    }

    public void assertNoErrorInTheBackgroundThread() throws Throwable {
        Thread.yield();
        Thread.sleep(100); // slow!
        synchronized (lockObject) {
            if (lastThrowable != null) {
                Throwable theThrowable = lastThrowable;
                lastThrowable = null;
                // NOT just throw throwable (this gives us more information via two stack traces)
                throw new AssertionError("Failed to assert that no error occured in the background Thread (see nested cause)", theThrowable);
            }
        }
    }

    public void assertErrorCaughtFromTheBackgroundThread() throws InterruptedException {
        Thread.yield();
        Thread.sleep(100);
        synchronized (lockObject) {
            assertNotNull("Expected an error occuring in the background thread (but there wasn't)", lastThrowable);
            lastThrowable = null;
        }
    }

    public void assertErrorMessageCaughtFromTheBackgroundThreadContains(String message) throws InterruptedException {
        Thread.yield();
        Thread.sleep(100);
        synchronized (lockObject) {
            assertNotNull("Expected an error occuring in the background thread (but there wasn't)", lastThrowable);
            assertTrue(lastThrowable.getMessage(), lastThrowable.getMessage().contains(message));
            lastThrowable = null;
        }
    }

}
