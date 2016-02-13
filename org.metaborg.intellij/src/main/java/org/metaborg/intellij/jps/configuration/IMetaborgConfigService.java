/*
 * Copyright © 2015-2016
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

package org.metaborg.intellij.jps.configuration;


import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.module.*;

/**
 * Gets or sets the configuration for JPS model objects.
 */
public interface IMetaborgConfigService {

    /**
     * Gets the Metaborg application configuration.
     *
     * @param global The global JPS element.
     * @return The configuration.
     */
    JpsMetaborgApplicationConfig getConfiguration(JpsGlobal global);

    /**
     * Gets the Metaborg project configuration.
     *
     * @param project The project JPS element.
     * @return The configuration.
     */
    JpsMetaborgProjectConfig getConfiguration(JpsProject project);

    /**
     * Gets the Metaborg module configuration.
     *
     * @param module The module JPS element.
     * @return The configuration.
     */
    JpsMetaborgModuleConfig getConfiguration(JpsModule module);


    /**
     * Sets the Metaborg application configuration.
     *
     * @param global The global JPS element.
     * @param config The configuration.
     */
    void setConfiguration(JpsGlobal global, JpsMetaborgApplicationConfig config);

    /**
     * Sets the Metaborg project configuration.
     *
     * @param project The project JPS element.
     * @param config  The configuration.
     */
    void setConfiguration(JpsProject project, JpsMetaborgProjectConfig config);

    /**
     * Sets the Metaborg module configuration.
     *
     * @param module The module JPS element.
     * @param config  The configuration.
     */
    void setConfiguration(JpsModule module, JpsMetaborgModuleConfig config);

}
