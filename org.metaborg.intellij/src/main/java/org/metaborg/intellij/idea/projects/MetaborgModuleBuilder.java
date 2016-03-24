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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.Icon;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.ProjectException;
import org.metaborg.intellij.UnhandledException;
import org.metaborg.intellij.idea.graphics.IIconManager;
import org.metaborg.intellij.idea.projects.newproject.INewModuleWizardStepFactory;
import org.metaborg.intellij.idea.sdks.MetaborgSdkType;
import org.metaborg.intellij.logging.InjectLogger;
import org.metaborg.intellij.logging.LoggerUtils;
import org.metaborg.intellij.resources.IIntelliJResourceService;
import org.metaborg.spoofax.meta.core.build.CommonPaths;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.generator.GeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.language.ContinuousLanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.language.LanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.util.log.ILogger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.DisposeAwareRunnable;

/**
 * Builds a new Spoofax module.
 */
@Singleton
public final class MetaborgModuleBuilder extends ModuleBuilder implements SourcePathsBuilder {

    private final IIdeaLanguageSpecFactory languageSpecFactory;
    private final IIntelliJResourceService resourceService;
    private final IIdeaProjectService projectService;
    private final IIconManager iconManager;
    private final INewModuleWizardStepFactory wizardStepFactory;
    private final ISpoofaxLanguageSpecConfigBuilder configBuilder;
    private final MetaborgModuleType moduleType;
    @InjectLogger
    private ILogger logger;

    @Nullable
    private List<Pair<String, String>> sourcePaths;

    private String name;
    private String extension;
    private LanguageIdentifier languageId;


    /**
     * Gets the name of the module.
     *
     * @return The name.
     */
    @Override
    public String getName() { return this.name; }

    /**
     * Sets the name of the module.
     *
     * @param name The name.
     */
    @Override
    public void setName(final String name) { this.name = name; }

    /**
     * Gets the extension of the language.
     *
     * @return The language file extension.
     */
    public String getExtension() { return this.extension; }

    /**
     * Sets the extension of the language.
     *
     * @param extension The language file extension.
     */
    public void setExtension(final String extension) { this.extension = extension; }

    /**
     * Gets the language ID of the module.
     *
     * @return The language identifier.
     */
    public LanguageIdentifier getLanguageIdentifier() {
        return this.languageId;
    }

    /**
     * Sets the language ID of the module.
     *
     * @param languageId The language identifier; or <code>null</code>.
     */
    public void setLanguageIdentifier(final LanguageIdentifier languageId) {
        this.languageId = languageId;
    }


    @Inject
    private MetaborgModuleBuilder(
            final IIdeaLanguageSpecFactory languageSpecFactory,
            final IIntelliJResourceService resourceService,
            final IIdeaProjectService projectService,
            final ISpoofaxLanguageSpecConfigBuilder configBuilder,
            final INewModuleWizardStepFactory wizardStepFactory,
            final IIconManager iconManager,
            final MetaborgModuleType moduleType) {
        super();
        this.languageSpecFactory = languageSpecFactory;
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.configBuilder = configBuilder;
        this.wizardStepFactory = wizardStepFactory;
        this.iconManager = iconManager;
        this.moduleType = moduleType;

        setDefaultValues();
    }

    /**
     * Sets the default values.
     */
    private void setDefaultValues() {
        final String uuid = UUID.randomUUID().toString().substring(0, 8).toLowerCase();

        this.name = "Untitled-" + uuid;
        this.extension = "u";
        this.languageId = new LanguageIdentifier(
                "org.example",
                "untitled-" + uuid,
                LanguageVersion.parse("1.0.0-SNAPSHOT")
        );
    }

    /**
     * Gets the wizard step shown under the SDK selection.
     *
     * @param context          The wizard context.
     * @param parentDisposable The parent disposable.
     * @return The wizard step.
     */
    @Override
    @Nullable
    public final ModuleWizardStep getCustomOptionsStep(
            final WizardContext context,
            final Disposable parentDisposable) {

        return this.wizardStepFactory.create(this, context);
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(
            final WizardContext wizardContext, final ModulesProvider modulesProvider) {
        return ModuleWizardStep.EMPTY_ARRAY;
    }

    @Override
    @Nullable
    public final ModuleWizardStep modifySettingsStep(final SettingsStep settingsStep) {
        return getModuleType().modifySettingsStep(settingsStep, this);
    }

    @Override
    public final ModuleWizardStep modifyProjectTypeStep(final SettingsStep settingsStep) {
        @Nullable final ModuleWizardStep wizardStep = StdModuleTypes.JAVA.modifyProjectTypeStep(settingsStep, this);
        assert wizardStep != null;
        return wizardStep;
    }

    /**
     * Setups the root model.
     *
     * @param rootModel The root model.
     * @throws ConfigurationException
     */
    @Override
    public void setupRootModel(final ModifiableRootModel rootModel) throws ConfigurationException {

        final FileObject location = MetaborgModuleBuilder.
                this.resourceService.resolve(getContentEntryPath());

        final ISpoofaxLanguageSpecConfig config = this.configBuilder
                .reset()
                .withIdentifier(getLanguageIdentifier())
                .withName(getName())
                .build(location);

        @Nullable final IdeaLanguageSpec languageSpec = this.languageSpecFactory.create(
                rootModel.getModule(), location, config);

        assert languageSpec != null;

        setContentRoots(rootModel);
        setCompilerOutputPath(rootModel, new CommonPaths(languageSpec.location()));
        setSdk(rootModel);

        // Set the module.
        final Module module = rootModel.getModule();
        final Project project = module.getProject();

        // Generate the project files.
        ApplicationManager.getApplication().runWriteAction(() -> {
            MetaborgModuleBuilder.this.logger.debug("Generating project files.");

            // Generate the module structure (files and directories).
            MetaborgModuleBuilder.this.projectService.open(languageSpec);
            WriteCommandAction.runWriteCommandAction(
                    project, "Create new Spoofax module", null, () -> generateModuleStructure(languageSpec));
            MetaborgModuleBuilder.this.logger.info("Generated project files.");
        });

    }

    private void setContentRoots(final ModifiableRootModel rootModel)
            throws ConfigurationException {
        // Set the content roots.
        this.logger.debug("Adding content and source roots.");
        @Nullable final ContentEntry contentEntry = doAddContentEntryAndSourceRoots(rootModel);
        if (contentEntry != null) {
            // TODO: Get this from the paths interface.
            contentEntry.addExcludeFolder(contentEntry.getUrl() + File.separator + ".idea");
            contentEntry.addExcludeFolder(contentEntry.getUrl() + File.separator + ".cache");
            contentEntry.addExcludeFolder(contentEntry.getUrl() + File.separator + "lib");
            contentEntry.addExcludeFolder(contentEntry.getUrl() + File.separator + "src-gen");
            contentEntry.addExcludeFolder(contentEntry.getUrl() + File.separator + "include");
        }
        this.logger.info("Added content and source roots.");
    }

    private void setCompilerOutputPath(final ModifiableRootModel rootModel, final CommonPaths paths) {
        // Set the compiler output path.
        this.logger.debug("Setting compiler output path.");

        final String outputFolder;
        try {
            outputFolder = paths.targetClassesDir().getURL().toString();
        } catch (final FileSystemException e) {
            throw new UnhandledException(e);
        }

        final String testOutputFolder;
        try {
            testOutputFolder = paths.targetTestClassesDir().getURL().toString();
        } catch (final FileSystemException e) {
            throw new UnhandledException(e);
        }

        final CompilerModuleExtension compilerModuleExtension =
                rootModel.getModuleExtension(CompilerModuleExtension.class);
        compilerModuleExtension.setCompilerOutputPath(outputFolder);
        compilerModuleExtension.setCompilerOutputPathForTests(testOutputFolder);
        compilerModuleExtension.inheritCompilerOutputPath(false);
        this.logger.info("Set compiler output path.");
    }

    /**
     * Runs a runnable once the specified project has been initialized.
     *
     * @param project  The project.
     * @param runnable The runnable.
     */
    private static void runWhenInitialized(final Project project, final Runnable runnable) {
        if (project.isDisposed())
            // Project is disposed. Nothing to do.
            return;

        final Application application = ApplicationManager.getApplication();
        if (application.isHeadlessEnvironment() || application.isUnitTestMode()) {
            // Runnable cannot be run in background. Just run it.
            runnable.run();
        } else if (!project.isInitialized()) {
            // Run runnable once project has initialized.
            StartupManager.getInstance(project).registerPostStartupActivity(DisposeAwareRunnable.create(
                    runnable,
                    project
            ));
        } else if (DumbService.isDumbAware(runnable)) {
            // The runnable is dumb aware. Just run it.
            runnable.run();
        } else {
            // The runnable is not dumb aware. Run it when applicable.
            DumbService.getInstance(project).runWhenSmart(DisposeAwareRunnable.create(runnable, project));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuitableSdkType(final SdkTypeId sdkType) {
        return sdkType instanceof MetaborgSdkType;
    }

    /**
     * Sets the SDK.
     *
     * @param rootModel The root model.
     */
    private void setSdk(final ModifiableRootModel rootModel) {

        // Set the SDK.
        this.logger.debug("Setting SDK.");
        if (this.myJdk != null) {
            // An SDK was selected in the wizard.
            rootModel.setSdk(this.myJdk);
        } else {
            // No SDK was selected in the wizard.
            rootModel.inheritSdk();
        }

        this.logger.info("Set SDK.");
    }

    /**
     * Generates the module directory structure and files.
     *
     * @param languageSpec The language specification.
     */
    private void generateModuleStructure(
            final ISpoofaxLanguageSpec languageSpec) {

        try {
            final GeneratorSettings settings = new GeneratorSettings(languageSpec.location(), languageSpec.config());
//            // TODO: Get from SDK.
//            generatorSettings.setMetaborgVersion("1.5.0-SNAPSHOT");
            // FIXME: Factory?
            final LanguageSpecGenerator newGenerator = new LanguageSpecGenerator(
                    settings,
                    new String[]{getExtension()}
            );
            newGenerator.generateAll();
            // FIXME: Factory?
            final ContinuousLanguageSpecGenerator generator = new ContinuousLanguageSpecGenerator(settings);
            generator.generateAll();

//            // TODO: Get the source folders and exclude folders from the generator, and add them to the `contentEntry`.
//            final VirtualFile f = resourceService.unresolve(project.location().resolveFile("editor/java/"));
//            contentEntry.addSourceFolder(f, false, "");


        } catch (ProjectException | IOException e) {
            throw LoggerUtils.exception(this.logger, UnhandledException.class, "Unexpected unhandled exception.", e);
        }
    }

    /**
     * Gets the module type.
     *
     * @return The module type.
     */
    @Override
    public final ModuleType getModuleType() {
        return this.moduleType;
    }

    /**
     * Gets the module's big icon.
     *
     * @return The big icon.
     */
    // TODO: Use project's ILanguage facet defined icon.
    @Override
    public final Icon getBigIcon() {
        return this.iconManager.getDefaultIcon();
    }

    /**
     * Gets the module's normal icon.
     *
     * @return The normal icon.
     */
    // TODO: Use project's ILanguage facet defined icon.
    @Override
    public final Icon getNodeIcon() {
        return this.iconManager.getDefaultIcon();
    }

    /**
     * Gets the module builder's description.
     *
     * @return The module builder's description.
     */
    @Override
    public final String getDescription() {
        return "Creates a new <b>Spoofax Language</b> module, used for developing domain-specific languages " +
                "using the <b>Spoofax Language Workbench</b>.";
    }

    /**
     * Gets the module builder's presentable name.
     * <p>
     * This name is shown in the <em>New Project</em> and <em>New Module</em> wizards.
     *
     * @return The module builder's presentable name.
     */
    @Override
    public final String getPresentableName() {
        return "Spoofax Language";
    }

    /**
     * Gets the module builder's group name.
     * <p>
     * I suspect module builders with the same group name are grouped
     * in the <em>New Project</em> and <em>New Module</em> wizards.
     *
     * @return The group name.
     */
    @Override
    public final String getGroupName() {
        return "Spoofax";
    }

    /**
     * Gets a list of source paths.
     *
     * @return A list of (path, packagePrefix) pairs.
     * @throws ConfigurationException
     */
    @Override
    @Nullable
    public List<Pair<String, String>> getSourcePaths() throws ConfigurationException {
        if (this.sourcePaths == null) {
            final List<Pair<String, String>> paths = new ArrayList<>();
            final String path = getContentEntryPath() + File.separator + "editor" + File.separator + "java";
            final boolean foldersCreated = new File(path).mkdirs();
            if (!foldersCreated) {
                this.logger.error("Failed to create some folders in path: {}", path);
            }
            paths.add(Pair.create(path, ""));
            return paths;
        }
        return this.sourcePaths;
    }

    /**
     * Sets the list of source paths.
     *
     * @param sourcePaths A list of (path, packagePrefix) pairs.
     */
    @Override
    public void setSourcePaths(@Nullable final List<Pair<String, String>> sourcePaths) {
        this.sourcePaths = sourcePaths != null ? new ArrayList<>(sourcePaths) : null;
    }

    /**
     * Adds a source path.
     *
     * @param sourcePathInfo A (path, packagePrefix) pair.
     */
    @Override
    public void addSourcePath(final Pair<String, String> sourcePathInfo) {
        if (this.sourcePaths == null) {
            this.sourcePaths = new ArrayList<>();
        }
        this.sourcePaths.add(sourcePathInfo);
    }

    @Nullable
    protected ContentEntry doAddContentEntryAndSourceRoots(final ModifiableRootModel rootModel) throws
            ConfigurationException {
        // Add the content entry path as a content root.
        @Nullable final ContentEntry contentEntry = doAddContentEntry(rootModel);
        if (contentEntry == null)
            return null;

        @Nullable final List<Pair<String, String>> sourcePaths = getSourcePaths();

        if (sourcePaths == null)
            return null;

        for (final Pair<String, String> sourcePath : sourcePaths) {
            final String first = sourcePath.first;
            try {
                VfsUtil.createDirectories(first);
            } catch (final IOException e) {
                throw new UnhandledException(e);
            }
            @Nullable final VirtualFile sourceRoot = LocalFileSystem.getInstance()
                    .refreshAndFindFileByPath(FileUtil.toSystemIndependentName(first));
            if (sourceRoot != null) {
                contentEntry.addSourceFolder(sourceRoot, false, sourcePath.second);
            }
        }

        return contentEntry;
    }

    @Nullable
    @Override
    public List<Module> commit(@NotNull final Project project,
                               final ModifiableModuleModel model,
                               final ModulesProvider modulesProvider) {
        final LanguageLevelProjectExtension extension = LanguageLevelProjectExtension.getInstance(ProjectManager.getInstance().getDefaultProject());
        @Nullable final Boolean aDefault = extension.getDefault();
        final LanguageLevelProjectExtension instance = LanguageLevelProjectExtension.getInstance(project);
        if(aDefault != null && !aDefault) {
            instance.setLanguageLevel(extension.getLanguageLevel());
            instance.setDefault(false);
        } else {
            instance.setDefault(true);
        }

        return super.commit(project, model, modulesProvider);
    }
}