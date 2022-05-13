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
package ch.vorburger.fswatch.test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;

import ch.vorburger.fswatch.DirectoryWatcher.Listener;
import ch.vorburger.fswatch.QuietPeriodListener;
import org.junit.Test;

public class QuietPeriodListenerTest {

    AssertableExceptionHandler assertableExceptionHandler;
    volatile boolean notified;

    @Test
    public void testQuietPeriodListener() throws Throwable {
        assertableExceptionHandler = new AssertableExceptionHandler();
        Listener originalListener = (path, changeKind) -> {
            assertFalse(notified); // We want this to only be called once
            notified = true;
        };

        Listener quietListener = new QuietPeriodListener(100, originalListener, assertableExceptionHandler);

        notified = false;
        quietListener.onChange(null, null);
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        await().atMost(1, SECONDS).until(() -> notified, is(true));
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

        notified = false;
        quietListener.onChange(null, null);
        quietListener.onChange(null, null);
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        await().atMost(1, SECONDS).until(() -> notified, is(true));
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

        notified = false;
        quietListener.onChange(null, null);
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        await().atMost(1, SECONDS).until(() -> notified, is(true));
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();

        Thread.sleep(500);

        notified = false;
        quietListener.onChange(null, null);
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
        await().atMost(1, SECONDS).until(() -> notified, is(true));
        assertableExceptionHandler.assertNoErrorInTheBackgroundThread();
    }
}
