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

package org.metaborg.intellij.jps.builders;

import org.jetbrains.annotations.*;
import org.jetbrains.jps.builders.*;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.module.*;
import org.metaborg.intellij.jps.*;
import org.metaborg.intellij.jps.configuration.*;
import org.metaborg.intellij.jps.projects.*;
import org.metaborg.intellij.logging.*;
import org.metaborg.spoofax.meta.core.project.*;
import org.metaborg.util.log.*;

import java.util.*;

/**
 * The spoofax build target type, which is a module-based build target.
 *
 * @param <T> The associated type of build target.
 */
public abstract class SpoofaxTargetType<T extends SpoofaxTarget> extends ModuleBasedBuildTargetType<T> {

    private final JpsSpoofaxMetaBuilder metaBuilder;
    private final JpsMetaborgModuleType moduleType;
    private final IJpsProjectService projectService;
    private final ISpoofaxLanguageSpecService languageSpecService;
    @InjectLogger
    private ILogger logger;

    protected SpoofaxTargetType(final String typeId, final IJpsProjectService projectService,
                                final JpsMetaborgModuleType moduleType,
                                final JpsSpoofaxMetaBuilder metaBuilder,
                                final ISpoofaxLanguageSpecService languageSpecService) {
        super(typeId);
        this.projectService = projectService;
        this.moduleType = moduleType;
        this.metaBuilder = metaBuilder;
        this.languageSpecService = languageSpecService;
    }

    /**
     * Creates a build target for the specified project.
     *
     * @param project The project.
     * @return The created build target.
     */
    public abstract T createTarget(MetaborgJpsProject project);

    /**
     * Creates a build target for the specified JPS module.
     *
     * @param module The JPS module.
     * @return The created build target.
     */
    public final T createTarget(final JpsModule module) {
        final JpsLanguageSpec languageSpec = this.metaBuilder.getOrCreateLanguageSpec(module);
        return createTarget(languageSpec);
    }

    /**
     * Computes all build targets for the specified JPS model.
     *
     * @param model The JPS model.
     * @return A list of build targets.
     */
    @Override
    public final List<T> computeAllTargets(final JpsModel model) {
        // Default implementation.
        final List<T> targets = new ArrayList<>();
        final Iterable<JpsTypedModule<JpsMetaborgModuleConfig>> modules = model.getProject().getModules(this.moduleType);
        for (final JpsTypedModule<JpsMetaborgModuleConfig> module : modules) {
            targets.add(createTarget(module));
        }
        return targets;
    }

    /**
     * Creates a build target loader, that creates a build target for a JPS module with a specified ID.
     *
     * @param model The JPS model.
     * @return The build target loader.
     */
    @Override
    public final BuildTargetLoader<T> createLoader(final JpsModel model) {
        // Default implementation.
        return new BuildTargetLoader<T>() {
            @Nullable
            @Override
            public T createTarget(final String targetId) {
                for (final T target : computeAllTargets(model)) {
                    if (target.getId().equals(targetId))
                        return target;
                }
                return null;
            }
        };
    }

}