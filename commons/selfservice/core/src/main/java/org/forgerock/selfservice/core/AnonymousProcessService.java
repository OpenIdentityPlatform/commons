/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions copyright 2024 3A Systems LLC.
 */

package org.forgerock.selfservice.core;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.config.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.List;

/**
 * Anonymous process service progresses a chain of {@link ProgressStage}
 * configurations, handling any required client interactions.
 *
 * @since 0.1.0
 */
public final class AnonymousProcessService extends AbstractRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AnonymousProcessService.class);

    private static final String SUBMIT_ACTION = "submitRequirements";

    private static final String TOKEN_FIELD = "token";
    private static final String INPUT_FIELD = "input";
    private static final String TYPE_FIELD = "type";
    private static final String TAG_FIELD = "tag";
    private static final String STATUS_FIELD = "status";
    private static final String SUCCESS_FIELD = "success";
    private static final String REQUIREMENTS_FIELD = "requirements";
    private static final String ADDITIONS_FIELD = "additions";
    private static final String END_VALUE = "end";

    private final ProgressStageBinder progressStageBinder;
    private final List<StageConfig> stageConfigs;
    private final SnapshotAuthor snapshotAuthor;
    private final int configVersion;

    /**
     * Initialises the anonymous process service with the passed config.
     *
     * @param config
     *         service configuration
     * @param progressStageProvider
     *         progress stage provider
     * @param tokenHandlerFactory
     *         snapshot token handler factory
     * @param processStore
     *         store for locally persisted state
     * @param classLoader
     *         class loader used for dynamic stage class instantiation
     */
    @Inject
    public AnonymousProcessService(ProcessInstanceConfig config, ProgressStageProvider progressStageProvider,
            SnapshotTokenHandlerFactory tokenHandlerFactory, ProcessStore processStore, ClassLoader classLoader) {
        Reject.ifNull(config, progressStageProvider, tokenHandlerFactory, processStore);
        Reject.ifNull(config.getStageConfigs(), config.getSnapshotTokenConfig(), config.getStorageType());
        Reject.ifTrue(config.getStageConfigs().isEmpty());

        progressStageBinder = new ProgressStageBinder(progressStageProvider, classLoader);
        stageConfigs = config.getStageConfigs();
        configVersion = config.hashCode();

        SnapshotTokenHandler snapshotTokenHandler = tokenHandlerFactory.get(config.getSnapshotTokenConfig());
        snapshotAuthor = config.getStorageType().newSnapshotAuthor(snapshotTokenHandler, processStore);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            JsonValue clientResponse = initiateProcess(new SelfServiceContext(context), request);
            String revision = String.valueOf(clientResponse.getObject().hashCode());
            return newResourceResponse("1", revision, clientResponse).asPromise();
        } catch (ResourceException | RuntimeException e) {
            return logAndAdaptException(e).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        if (SUBMIT_ACTION.equals(request.getAction())) {
            try {
                JsonValue clientResponse = progressProcess(new SelfServiceContext(context), request);
                return newActionResponse(clientResponse).asPromise();
            } catch (ResourceException | RuntimeException e) {
                return logAndAdaptException(e).asPromise();
            }
        }

        return new NotSupportedException("Unknown action " + request.getAction()).asPromise();
    }

    private ResourceException logAndAdaptException(Exception exception) {
        try {
            throw exception;
        } catch (InternalServerErrorException iseE) {
            logger.error("Internal error intercepted", iseE);
            return iseE;
        } catch (ResourceException rE) {
            logger.warn("Resource exception intercepted", rE);
            return rE;
        } catch (Exception ex) {
            logger.error("Exception intercepted", ex);
            return new InternalServerErrorException("Exception intercepted", ex);
        }
    }

    /*
     * Responsible for retrieving the requirements from the first stage in the flow.
     */
    private JsonValue initiateProcess(Context requestContext, ReadRequest request) throws ResourceException {
        ProcessContextImpl context = ProcessContextImpl
                .newBuilder(requestContext, request)
                .setConfigVersion(configVersion)
                .build();

        ProgressStageBinding<?> stage = retrieveStage(context);
        JsonValue requirements = stage.gatherInitialRequirements(context);

        if (logger.isDebugEnabled()) {
            logger.debug("Initial requirements retrieved for stage " + stage.getName());
        }

        return renderRequirements(
                stage,
                StageResponse
                        .newBuilder()
                        .setRequirements(requirements)
                        .build());
    }

    /*
     * With the process flow already kicked off, progresses to the flow by processing the client input.
     */
    private JsonValue progressProcess(Context requestContext, ActionRequest request) throws ResourceException {
        JsonValue clientInput = request.getContent();
        JsonValue snapshotTokenValue = clientInput.get(TOKEN_FIELD);
        ProcessContextImpl.Builder contextBuilder;

        if (snapshotTokenValue.isNotNull()) {
            JsonValue jsonContext = snapshotAuthor.retrieveSnapshotFrom(snapshotTokenValue.asString());
            contextBuilder = ProcessContextImpl.newBuilder(requestContext, request, jsonContext);
        } else {
            contextBuilder = ProcessContextImpl.newBuilder(requestContext, request)
                    .setConfigVersion(configVersion);
        }

        JsonValue input = clientInput.get(INPUT_FIELD);

        if (input.isNull()) {
            throw new BadRequestException("No input provided");
        }

        ProcessContextImpl context = contextBuilder
                .setInput(input)
                .build();

        if (configVersion != context.getConfigVersion()) {
            throw new BadRequestException("Invalid token");
        }

        ProgressStageBinding<?> stage = retrieveStage(context);

        if (logger.isDebugEnabled()) {
            logger.debug("Advancing stage " + stage.getName());
        }

        return enactContext(context, stage);
    }

    private JsonValue enactContext(ProcessContextImpl context, ProgressStageBinding<?> stage) throws ResourceException {
        StageResponse response = stage.advance(context);

        if (response.hasRequirements()) {
            // Stage has additional requirements, render response.
            return renderRequirementsWithSnapshot(context, stage, response);
        }

        return handleProgression(context, stage);
    }

    private JsonValue handleProgression(ProcessContextImpl context,
            ProgressStageBinding<?> stage) throws ResourceException {
        if (context.getStageIndex() + 1 == stageConfigs.size()) {
            // Flow complete, render completion response.
            return renderCompletion(context, stage);
        }

        // Stage satisfied, move onto the next stage.
        int nextIndex = context.getStageIndex() + 1;

        ProcessContextImpl nextContext = ProcessContextImpl
                .newBuilder(context.getRequestContext(), context.getRequest())
                .setStageIndex(nextIndex)
                .setConfigVersion(context.getConfigVersion())
                .setState(context.getState())
                .build();

        ProgressStageBinding<?> nextStage = retrieveStage(nextContext);
        JsonValue requirements = nextStage.gatherInitialRequirements(nextContext);

        if (logger.isDebugEnabled()) {
            logger.debug("Initial requirements retrieved for stage " + nextStage.getName());
        }

        if (requirements.size() > 0) {
            // Stage has some initial requirements, render response.
            return renderRequirementsWithSnapshot(
                    nextContext,
                    nextStage,
                    StageResponse
                            .newBuilder()
                            .setRequirements(requirements)
                            .build());
        }

        return enactContext(nextContext, nextStage);
    }

    private JsonValue renderRequirementsWithSnapshot(ProcessContextImpl context, ProgressStageBinding<?> stage,
            StageResponse response) throws ResourceException {
        ProcessContextImpl updatedContext = ProcessContextImpl
                .newBuilder(context)
                .setStageTag(response.getStageTag())
                .build();

        String snapshotToken = snapshotAuthor.captureSnapshotOf(updatedContext.toJson());

        if (response.hasCallback()) {
            response.getCallback().snapshotTokenPreview(updatedContext, snapshotToken);
        }

        return renderRequirements(stage, response)
                .add(TOKEN_FIELD, snapshotToken);
    }

    private JsonValue renderRequirements(ProgressStageBinding<?> stage, StageResponse response) {
        return json(
                object(
                        field(TYPE_FIELD, stage.getName()),
                        field(TAG_FIELD, response.getStageTag()),
                        field(REQUIREMENTS_FIELD, response.getRequirements().asMap())));
    }

    private JsonValue renderCompletion(ProcessContextImpl context, ProgressStageBinding<?> stage) {
        JsonValue response = json(
                object(
                        field(TYPE_FIELD, stage.getName()),
                        field(TAG_FIELD, END_VALUE),
                        field(STATUS_FIELD,
                                object(
                                        field(SUCCESS_FIELD, true)))));

        JsonValue successAdditions = context.hasSuccessAdditions()
                ? context.getSuccessAdditions()
                : emptyJson();

        response.add(ADDITIONS_FIELD, successAdditions.asMap());

        return response;
    }

    private ProgressStageBinding<?> retrieveStage(ProcessContextImpl context) {
        if (context.getStageIndex() >= stageConfigs.size()) {
            throw new StageConfigException("Invalid stage index " + context.getStageIndex());
        }

        StageConfig config = stageConfigs.get(context.getStageIndex());
        return progressStageBinder.getBinding(config);
    }

}
