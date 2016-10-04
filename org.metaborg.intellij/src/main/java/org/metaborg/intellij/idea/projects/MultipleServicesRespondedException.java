/*
 * Copyright © 2015-2016
 *
 * This file is part of Spoofax for IntelliJ.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.metaborg.intellij.idea.projects;

import javax.annotation.Nullable;

/**
 * Exception thrown when a compound service has multiple services responding.
 */
public final class MultipleServicesRespondedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "More than one service responded.";

    /**
     * Initializes a new instance of the {@link MultipleServicesRespondedException} class.
     *
     * @param message The message; or <code>null</code>.
     */
    public MultipleServicesRespondedException(@Nullable final String message) {
        this(message, null);
    }

    /**
     * Initializes a new instance of the {@link MultipleServicesRespondedException} class.
     *
     * @param message The message; or <code>null</code>.
     * @param cause   The cause; or <code>null</code>.
     */
    public MultipleServicesRespondedException(@Nullable final String message, @Nullable final Throwable cause) {
        super(message != null ? message : DEFAULT_MESSAGE);
    }

    /**
     * Initializes a new instance of the {@link MultipleServicesRespondedException} class.
     *
     * @param cause The cause; or <code>null</code>.
     */
    public MultipleServicesRespondedException(@Nullable final Throwable cause) {
        this(null, cause);
    }

    /**
     * Initializes a new instance of the {@link MultipleServicesRespondedException} class.
     */
    public MultipleServicesRespondedException() {
        this(null, null);
    }
}
