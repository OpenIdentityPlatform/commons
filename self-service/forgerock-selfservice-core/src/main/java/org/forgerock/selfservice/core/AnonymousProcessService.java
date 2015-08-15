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
 */

package org.forgerock.selfservice.core;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.exceptions.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.util.Pair;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Anonymous process service progresses a chain of {@link ProgressStage}
 * configurations, handling any required client interactions.
 *
 * @since 0.1.0
 */
public final class AnonymousProcessService extends AbstractRequestHandler {

    private static final String SUBMIT_ACTION = "submitRequirements";

    private static final String TOKEN_FIELD = "token";
    private static final String INPUT_FIELD = "input";
    private static final String TYPE_FIELD = "type";
    private static final String STAGE_FIELD = "stage";
    private static final String STATUS_FIELD = "status";
    private static final String SUCCESS_FIELD = "success";
    private static final String REQUIREMENTS_FIELD = "requirements";
    private static final String END_VALUE = "end";

    private static final int INITIAL_STAGE_INDEX = 0;

    private final ProgressStageFactory progressStageFactory;
    private final List<StageConfig> stageConfigs;
    private final SnapshotTokenHandler snapshotTokenHandler;
    private final SnapshotAuthor snapshotAuthor;

    /**
     * Initialises the anonymous process service with the passed config.
     *
     * @param config
     *         service configuration
     * @param progressStageFactory
     *         workflow stage factory
     * @param tokenHandlerFactory
     *         snapshot token handler factory
     * @param processStore
     *         store for locally persisted state
     */
    @Inject
    public AnonymousProcessService(ProcessInstanceConfig config, ProgressStageFactory progressStageFactory,
                                   SnapshotTokenHandlerFactory tokenHandlerFactory, ProcessStore processStore) {
        Reject.ifNull(config, progressStageFactory, tokenHandlerFactory, processStore);
        this.progressStageFactory = progressStageFactory;

        stageConfigs = config.getStageConfigs();
        snapshotTokenHandler = tokenHandlerFactory.get(config.getTokenType());
        snapshotAuthor = config.getStorageType().newSnapshotAuthor(snapshotTokenHandler, processStore);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            JsonValue clientResponse = initiateProcess();
            return Promises.newResultPromise(Responses.newResourceResponse("1", "1.0", clientResponse));
        } catch (ResourceException rE) {
            return Promises.newExceptionPromise(rE);
        } catch (RuntimeException rE) {
            ResourceException resourceException = ResourceException
                    .getException(ResourceException.INTERNAL_ERROR, "Internal error intercepted", rE)
                    .includeCauseInJsonValue();
            return Promises.newExceptionPromise(resourceException);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        if (SUBMIT_ACTION.equals(request.getAction())) {
            try {
                JsonValue clientResponse = progressProcess(request.getContent());
                return Promises.newResultPromise(Responses.newActionResponse(clientResponse));
            } catch (ResourceException rE) {
                return Promises.newExceptionPromise(rE);
            } catch (RuntimeException rE) {
                ResourceException resourceException = ResourceException
                        .getException(ResourceException.INTERNAL_ERROR, "Internal error intercepted", rE)
                        .includeCauseInJsonValue();
                return Promises.newExceptionPromise(resourceException);
            }
        }

        return Promises.newExceptionPromise(
                ResourceException.getException(ResourceException.NOT_SUPPORTED,
                        "Unknown action " + request.getAction()));
    }

    /*
     * Responsible for retrieving the requirements from the first stage in the flow.
     */
    private JsonValue initiateProcess() throws ResourceException {
        ProcessContext context = ProcessContext
                .newBuilder(INITIAL_STAGE_INDEX)
                .build();

        Pair<? extends ProgressStage<?>, StageConfig> stagePair = retrieveStage(context);
        JsonValue requirements = gatherInitialRequirements(context, stagePair.getFirst(), stagePair.getSecond());

        return renderRequirements(
                context,
                stagePair.getFirst().getStageType(),
                StageResponse
                        .newBuilder()
                        .setRequirements(requirements)
                        .build());
    }

    /*
     * With the process flow already kicked off, progresses to the flow by processing the client input.
     */
    private JsonValue progressProcess(JsonValue clientInput) throws ResourceException {
        JsonValue snapshotTokenValue = clientInput.get(TOKEN_FIELD);
        ProcessContext.Builder contextBuilder;

        if (snapshotTokenValue.isNotNull()) {
            String snapshotToken = snapshotTokenValue.asString();

            if (!snapshotTokenHandler.validate(snapshotToken)) {
                throw new BadRequestException("Invalid token");
            }

            Map<String, String> stageState = snapshotAuthor.retrieveSnapshotFrom(snapshotToken);
            contextBuilder = ProcessContext.newBuilder(stageState);
        } else {
            contextBuilder = ProcessContext.newBuilder(INITIAL_STAGE_INDEX);
        }

        JsonValue input = clientInput.get(INPUT_FIELD);

        if (input.isNull()) {
            throw new BadRequestException("No input provided");
        }

        ProcessContext context = contextBuilder
                .setInput(input)
                .build();

        Pair<? extends ProgressStage<?>, StageConfig> stagePair = retrieveStage(context);
        return enactContext(context, stagePair.getFirst(), stagePair.getSecond());
    }

    private JsonValue enactContext(ProcessContext context, ProgressStage<?> stage,
                                   StageConfig config) throws ResourceException {
        StageResponse response = advanceProgress(context, stage, config);

        if (response.hasRequirements()) {
            // Stage has additional requirements, render response.
            return renderRequirementsWithSnapshot(context, config.getStageType(), response);
        }

        return handleProgression(context, config.getStageType(), response);
    }

    private JsonValue handleProgression(ProcessContext context, StageType<?> stageType,
                                        StageResponse response) throws ResourceException {
        if (context.getStageIndex() + 1 == stageConfigs.size()) {
            // Flow complete, render completion response.
            return renderCompletion(stageType);
        }

        // Stage satisfied, move onto the next stage.
        ProcessContext nextContext = ProcessContext
                .newBuilder(context.getStageIndex() + 1)
                .addState(context.getState())
                .addState(response.getState())
                .build();

        Pair<? extends ProgressStage<?>, StageConfig> stagePair = retrieveStage(nextContext);
        JsonValue requirements = gatherInitialRequirements(nextContext, stagePair.getFirst(), stagePair.getSecond());

        if (requirements.size() > 0) {
            // Stage has some initial requirements, render response.
            return renderRequirementsWithSnapshot(
                    nextContext,
                    stagePair.getFirst().getStageType(),
                    StageResponse
                            .newBuilder()
                            .setRequirements(requirements)
                            .build());
        }

        return enactContext(nextContext, stagePair.getFirst(), stagePair.getSecond());
    }

    private JsonValue renderRequirementsWithSnapshot(ProcessContext context, StageType<?> stageType,
                                                     StageResponse response) throws ResourceException {
        ProcessContext updatedContext = ProcessContext
                .newBuilder(context)
                .setStageTag(response.getStageTag())
                .addState(response.getState())
                .build();

        String snapshotToken = snapshotAuthor.captureSnapshotOf(updatedContext.toFlattenedMap());

        if (response.hasCallback()) {
            response.getCallback().snapshotTokenPreview(updatedContext, snapshotToken);
        }

        return renderRequirements(updatedContext, stageType, response)
                .add(TOKEN_FIELD, snapshotToken);
    }

    private JsonValue renderRequirements(ProcessContext context, StageType<?> stageType, StageResponse response) {
        return json(
                object(
                        field(TYPE_FIELD, stageType.getName()),
                        field(STAGE_FIELD, context.getStageIndex()),
                        field(REQUIREMENTS_FIELD, response.getRequirements().asMap())));
    }

    private JsonValue renderCompletion(StageType<?> stageType) {
        return json(
                object(
                        field(TYPE_FIELD, stageType.getName()),
                        field(STAGE_FIELD, END_VALUE),
                        field(STATUS_FIELD,
                                object(
                                        field(SUCCESS_FIELD, true)))));
    }

    /*
     * Enables a typed stage config to be passed to the stage.
     * Type safety is checked during the stage retrieval.
     */
    private <C extends StageConfig> JsonValue gatherInitialRequirements(
            ProcessContext context, ProgressStage<C> stage, StageConfig config) throws ResourceException {
        return stage.gatherInitialRequirements(context, stage.getStageType().getTypedConfig(config));
    }

    /*
     * Enables a typed stage config to be passed to the stage.
     * Type safety is checked during the stage retrieval.
     */
    private <C extends StageConfig> StageResponse advanceProgress(
            ProcessContext context, ProgressStage<C> stage, StageConfig config) throws ResourceException {
        return stage.advance(context, stage.getStageType().getTypedConfig(config));
    }

    /*
     * Retrieves the stage and its corresponding config based on the context stage index.
     * This method also validates the expected subtype of the config.
     */
    private Pair<? extends ProgressStage<?>, StageConfig> retrieveStage(ProcessContext context) {
        if (context.getStageIndex() >= stageConfigs.size()) {
            throw new StageConfigException("Invalid stage index " + context.getStageIndex());
        }

        StageConfig config = stageConfigs.get(context.getStageIndex());
        ProgressStage<?> stage = progressStageFactory.get(config.getStageType());

        if (stage == null) {
            throw new StageConfigException("Unknown progress stage " + config.getStageType().getName());
        }

        if (!stage.getStageType().equals(config.getStageType())) {
            // Implicit enforcement of the expected subtype of the stage config.
            throw new StageConfigException("Type for progress stage and config should be equivalent");
        }

        return Pair.of(stage, config);
    }

}
