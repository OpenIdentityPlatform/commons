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

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.forgerock.http.context.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig.StorageType;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.config.StageType;
import org.forgerock.selfservice.core.exceptions.IllegalInputException;
import org.forgerock.selfservice.core.exceptions.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotAuthor;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Anonymous process service progresses a chain of {@link ProgressStage} configurations, handling any required client interactions.
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
    private static final String REASON_FIELD = "reason";
    private static final String END_VALUE = "end";

    private static final int INITIAL_STAGE_INDEX = 0;

    private final ProgressStageFactory progressStageFactory;
    private final List<StageConfig> stageConfigs;
    private final ProcessStore processStore;
    private final SnapshotTokenHandler snapshotTokenHandler;
    private final InternalSnapshotAuthor snapshotAuthor;

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
        this.processStore = processStore;

        stageConfigs = config.getStageConfigs();
        snapshotAuthor = newSnapshotAuthor(config.getStorageType());
        snapshotTokenHandler = tokenHandlerFactory.get(config.getTokenType());
    }

    @Override
    public Promise<Resource, ResourceException> handleRead(ServerContext context, ReadRequest request) {
        return initiateProcess();
    }

    @Override
    public Promise<JsonValue, ResourceException> handleAction(ServerContext context, ActionRequest request) {
        if (SUBMIT_ACTION.equals(request.getAction())) {
            return progressProcess(request.getContent());
        }

        return Promises.newExceptionPromise(
                ResourceException.getException(ResourceException.NOT_SUPPORTED, "Unknown action " + request.getAction()));
    }

    /**
     * Responsible for kicking off the process flow.
     *
     * @return promise encapsulating the response
     */
    private Promise<Resource, ResourceException> initiateProcess() {
        try {
            ProcessContext context = ProcessContext
                    .newBuilder(INITIAL_STAGE_INDEX)
                    .build();

            JsonValue feedback = enactContext(context);
            Resource resource = new Resource("1", "1.0", feedback);
            return Promises.newResultPromise(resource);
        } catch (ResourceException rE) {
            return Promises.newExceptionPromise(rE);
        } catch (RuntimeException rE) {
            ResourceException resourceException = ResourceException
                    .getException(ResourceException.INTERNAL_ERROR, "Internal error intercepted", rE)
                    .includeCauseInJsonValue();
            return Promises.newExceptionPromise(resourceException);
        }

    }

    /**
     * With the process flow already kicked off, progresses to the next stage.
     *
     * @param interaction
     *         json value representing the response from the client
     *
     * @return promise encapsulating the response
     */
    private Promise<JsonValue, ResourceException> progressProcess(JsonValue interaction) {
        try {
            String snapshotToken = interaction.get(TOKEN_FIELD).asString();

            if (!snapshotTokenHandler.validate(snapshotToken)) {
                return Promises.newExceptionPromise(ResourceException
                        .getException(ResourceException.BAD_REQUEST, "Invalid token"));
            }

            JsonValue input = interaction.get(INPUT_FIELD);

            if (input.isNull()) {
                return Promises.newExceptionPromise(ResourceException
                        .getException(ResourceException.BAD_REQUEST, "No input provided"));
            }

            Map<String, String> stageState = snapshotAuthor.retrieveSnapshotState(snapshotToken);
            ProcessContext context = ProcessContext
                    .newBuilder(stageState)
                    .setInput(input)
                    .build();

            JsonValue feedback = enactContext(context);
            return Promises.newResultPromise(feedback);
        } catch (ResourceException rE) {
            return Promises.newExceptionPromise(rE);
        } catch (RuntimeException rE) {
            ResourceException resourceException = ResourceException
                    .getException(ResourceException.INTERNAL_ERROR, "Internal error intercepted", rE)
                    .includeCauseInJsonValue();
            return Promises.newExceptionPromise(resourceException);
        }
    }

    private JsonValue enactContext(ProcessContext context) throws IllegalInputException {
        StageConfig config = stageConfigs.get(context.getStageIndex());
        ProgressStage<?> stage = progressStageFactory.get(config.getStageType());

        if (stage == null) {
            throw new StageConfigException("Unknown progress stage " + config.getStageType().getName());
        }

        StageResponse response = advanceProgress(stage, context, config);

        if (response.hasRequirements()) {
            return renderRequirements(context, config.getStageType(), response);
        }

        return handleProgression(context, config.getStageType(), response);
    }

    private <C extends StageConfig> StageResponse advanceProgress(
            ProgressStage<C> stage, ProcessContext context, StageConfig config) throws IllegalInputException {

        if (!stage.getStageType().equals(config.getStageType())) {
            throw new StageConfigException("Type for progress stage and config should be equivalent");
        }

        return stage.advance(context, snapshotAuthor, stage.getStageType().getTypedConfig(config));
    }

    public JsonValue handleProgression(ProcessContext context, StageType<?> stageType,
                                       StageResponse response) throws IllegalInputException {
        if (context.getStageIndex() + 1 < stageConfigs.size()) {
            // Move onto the next progress stage
            return enactContext(ProcessContext
                    .newBuilder(context.getStageIndex() + 1)
                    .addState(context.getState())
                    .addState(response.getState())
                    .build());
        }

        // Process is complete
        return json(
                object(
                        field(TYPE_FIELD, stageType.getName()),
                        field(STAGE_FIELD, END_VALUE),
                        field(STATUS_FIELD,
                                object(
                                        field(SUCCESS_FIELD, true)
                                )
                        )
                ));
    }

    private JsonValue renderRequirements(ProcessContext context, StageType<?> stageType, StageResponse response) {
        String snapshotToken = snapshotAuthor.captureSnapshotOf(ProcessContext
                .newBuilder(context)
                .setStageTag(response.getStageTag())
                .addState(response.getState())
                .build());

        return json(
                object(
                        field(TOKEN_FIELD, snapshotToken),
                        field(TYPE_FIELD, stageType.getName()),
                        field(STAGE_FIELD, context.getStageIndex()),
                        field(REQUIREMENTS_FIELD, response.getRequirements().asMap())
                ));
    }

    private InternalSnapshotAuthor newSnapshotAuthor(StorageType type) {
        switch (type) {
            case LOCAL:
                return new LocalSnapshotAuthor();
            case STATELESS:
                return new StatelessSnapshotAuthor();
            default:
                throw new IllegalArgumentException("Unknown storage type " + type);
        }
    }

    private final class LocalSnapshotAuthor implements InternalSnapshotAuthor {

        @Override
        public String captureSnapshotOf(ProcessContext context) {
            String snapshotToken = snapshotTokenHandler.generate(Collections.<String, String>emptyMap());
            processStore.push(snapshotToken, context.asFlattenedMap());
            return snapshotToken;
        }

        @Override
        public Map<String, String> retrieveSnapshotState(String snapshotToken) {
            return processStore.pop(snapshotToken);
        }
    }

    private final class StatelessSnapshotAuthor implements InternalSnapshotAuthor {

        @Override
        public String captureSnapshotOf(ProcessContext context) {
            return snapshotTokenHandler.generate(context.asFlattenedMap());
        }

        @Override
        public Map<String, String> retrieveSnapshotState(String snapshotToken) {
            return snapshotTokenHandler.parse(snapshotToken);
        }

    }

    private interface InternalSnapshotAuthor extends SnapshotAuthor {

        Map<String, String> retrieveSnapshotState(String snapshotToken);

    }

}
