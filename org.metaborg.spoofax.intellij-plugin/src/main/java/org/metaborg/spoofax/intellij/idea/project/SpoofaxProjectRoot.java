/*
 * Copyright © 2015-2015
 *
 * This file is part of Spoofax for IntelliJ.
 *
 * Spoofax for IntelliJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spoofax for IntelliJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Spoofax for IntelliJ.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.metaborg.spoofax.intellij.idea.project;

import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * A detected Spoofax project's root.
 */
public final class SpoofaxProjectRoot extends DetectedProjectRoot {

    /**
     * Initializes a new instance of the {@link SpoofaxProjectRoot} class.
     *
     * @param directory The root directory.
     */
    public SpoofaxProjectRoot(@NotNull final File directory) {
        super(directory);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String getRootTypeName() {
        return "Spoofax module";
    }
}
