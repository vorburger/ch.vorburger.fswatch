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

import ch.vorburger.fswatch.DirectoryWatcher.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExceptionHandler which logs to slfj4j.
 *
 * @author Michael Vorburger.ch
 */
public class Slf4jLoggingExceptionHandler implements ExceptionHandler {
    private final static Logger log = LoggerFactory.getLogger(Slf4jLoggingExceptionHandler.class);

    @Override
    public void onException(Throwable t) {
        log.error("Oopsy daisy", t);
    }
}
