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

package org.metaborg.intellij.idea.transformations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.core.transform.ITransformService;
import org.metaborg.core.transform.TransformException;
import org.metaborg.core.transform.TransformResult;
import org.metaborg.core.transform.TransformResults;
import org.metaborg.intellij.logging.InjectLogger;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;

import com.google.inject.Inject;

/**
 * Executes a transformation action on resources.
 */
public final class ResourceTransformer<P, A, T> implements IResourceTransformer {

    private final IContextService contextService;
    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final ITransformService<P, A, T> transformService;
    @InjectLogger
    private ILogger logger;

    /**
     * Initializes a new instance of the {@link ResourceTransformer} class.
     */
    @Inject
    public ResourceTransformer(
            final IContextService contextService,
            final IParseResultRequester<P> parseResultRequester,
            final IAnalysisResultRequester<P, A> analysisResultRequester,
            final ITransformService<P, A, T> transformService
    ) {
        this.contextService = contextService;
        this.parseResultRequester = parseResultRequester;
        this.analysisResultRequester = analysisResultRequester;
        this.transformService = transformService;
    }

    /**
     * Executes the specified action.
     *
     * @param language  The language implementation.
     * @param resources The active resources.
     * @param goal      The transformation goal.
     */
    @Override
    public List<FileObject> execute(
            final Iterable<TransformResource> resources, final ILanguageImpl language,
            final ITransformGoal goal) throws MetaborgException {

        final List<FileObject> outputFiles = new ArrayList<>();
        for (final TransformResource transformResource : resources) {
            final FileObject resource = transformResource.resource();
            try {
                this.logger.info("Transforming {}", resource);
                final TransformResults<?, T> results = transform(
                        resource,
                        transformResource.project(),
                        language,
                        transformResource.text(),
                        goal
                );
                for (final TransformResult<?, T> r : results.results) {
                    outputFiles.add(r.output);
                }
            } catch (ContextException | TransformException e) {
                this.logger.error("Transformation failed for {}", e, resource);
            }
        }
        return outputFiles;
    }

    /**
     * Transforms a resource.
     *
     * @param resource The resource.
     * @param project The project that contains the resource.
     * @param language The language of the resource.
     * @param text The contents of the resource.
     * @param goal The transformation goal.
     * @return The transformation results.
     * @throws ContextException
     * @throws TransformException
     */
    private TransformResults<?, T> transform(
            final FileObject resource,
            final IProject project,
            final ILanguageImpl language,
            final String text,
            final ITransformGoal goal)
            throws ContextException, TransformException {
        final IContext context = this.contextService.get(resource, project, language);
        final TransformResults<?, T> results;
        if (this.transformService.requiresAnalysis(context, goal)) {
            results = transformAnalysis(resource, text, goal, context);
        } else {
            results = transformParse(resource, language, text, goal, context);
        }
        return results;
    }

    /**
     * Transform a resource from its parse result.
     *
     * @param resource The resource.
     * @param language The language of the resource.
     * @param text The contents of the resource.
     * @param goal The transformation goal.
     * @param context The context.
     * @return The transformation results.
     * @throws TransformException
     */
    private TransformResults<P, T> transformParse(
            final FileObject resource,
            final ILanguageImpl language,
            final String text,
            final ITransformGoal goal, final IContext context) throws TransformException {
        final ParseResult<P> parseResult =
                this.parseResultRequester.request(resource, language, text).toBlocking().single();
        return this.transformService.transform(parseResult, context, goal);
    }

    /**
     * Transform a resource from its analysis result.
     *
     * @param resource The resource.
     * @param text The contents of the resource.
     * @param goal The transformation goal.
     * @param context The context.
     * @return The transformation results.
     * @throws TransformException
     */
    private TransformResults<A, T> transformAnalysis(
            final FileObject resource,
            final String text,
            final ITransformGoal goal,
            final IContext context) throws TransformException {
        final AnalysisFileResult<P, A> analysisResult =
                this.analysisResultRequester.request(resource, context, text).toBlocking().single();
        //noinspection unused
        try (IClosableLock lock = context.read()) {
            return this.transformService.transform(analysisResult, context, goal);
        }
    }
}