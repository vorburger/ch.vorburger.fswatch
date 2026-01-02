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

import ch.vorburger.fswatch.DirectoryWatcher.ChangeKind;
import ch.vorburger.fswatch.DirectoryWatcher.ExceptionHandler;
import ch.vorburger.fswatch.DirectoryWatcher.Listener;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Listener which only notifies by delegating to another wrapped Listener after a certain quiet period.
 *
 * @author Michael Vorburger.ch
 */
public class QuietPeriodListener implements Listener {

    protected final Listener delegate;
    protected final long quietPeriodInMS;
    private final ExceptionHandler exceptionHandler;

    protected @Nullable Thread thread;
    protected volatile boolean sleepAgain;

    /**
     * Constructor.
     * @param quietPeriodInMS the quiet period in milliseconds
     * @param listenerToWrap the listener to wrap
     * @param exceptionHandler the exception handler
     */
    public QuietPeriodListener(long quietPeriodInMS, Listener listenerToWrap, ExceptionHandler exceptionHandler) {
        this.quietPeriodInMS = quietPeriodInMS;
        this.delegate = listenerToWrap;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public synchronized void onChange(Path path, ChangeKind changeKind) {
        if (thread != null && thread.isAlive()) {
            sleepAgain = true;
            //System.out.println("sleepAgain = true");
        } else {
            Runnable r = () -> {
                try {
                    do {
                        sleepAgain = false;
                        //System.out.println("sleepAgain = false");
                        Thread.sleep(quietPeriodInMS);
                    } while (sleepAgain);
                    delegate.onChange(path, changeKind);
                } catch (Throwable e) {
                    exceptionHandler.onException(e);
                }
            };
            thread = new Thread(r, QuietPeriodListener.class.getName());
            thread.setDaemon(true);
            thread.start();
        }
    }
}
