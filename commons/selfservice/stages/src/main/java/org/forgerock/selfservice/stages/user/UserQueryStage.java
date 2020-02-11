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
package org.forgerock.selfservice.stages.user;

import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USERNAME_FIELD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * Stage is responsible for querying the underlying service for a user based on the supplied query fields.
 * Once identified, it populates "mail" and "userId" fields in the context.
 *
 * @since 0.5.0
 */
public final class UserQueryStage implements ProgressStage<UserQueryConfig> {

    private final ConnectionFactory connectionFactory;

    private final QueryFilterVisitor<Boolean, Set<JsonPointer>, JsonPointer> queryFilterValidator;

    /**
     * Constructs a new user query stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public UserQueryStage(@SelfService ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.queryFilterValidator = new QueryFilterValidator();
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, UserQueryConfig config)
            throws ResourceException {
        Reject.ifTrue(getQueryFieldsAsJsonPointers(config.getValidQueryFields()).isEmpty(),
                "User query stage expects query fields");
        Reject.ifNull(config.getIdentityEmailField(), "User query stage expects identity email field");
        Reject.ifNull(config.getIdentityIdField(), "User query stage expects identity id field");
        Reject.ifNull(config.getIdentityUsernameField(), "User query stage expects identity username field");
        Reject.ifNull(config.getIdentityServiceUrl(), "User query stage expects identity service url");

        return RequirementsBuilder.newInstance("Find your account")
                .addRequireProperty("queryFilter", "filter string to find account")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, UserQueryConfig config) throws ResourceException {
        JsonValue queryFilter = context.getInput().get("queryFilter").required();

        if (!isValidQueryFilter(config, queryFilter)) {
            throw new BadRequestException("Invalid query filter");
        }

        JsonValue user = findUser(context.getRequestContext(), queryFilter.asString(), config);

        putState(USER_ID_FIELD, context, config.getIdentityIdField(), user);
        putState(EMAIL_FIELD, context, config.getIdentityEmailField(), user);
        putState(USERNAME_FIELD, context, config.getIdentityUsernameField(), user);

        return StageResponse.newBuilder().build();
    }

    private Boolean isValidQueryFilter(UserQueryConfig config, JsonValue queryFilter) {
        QueryFilter<JsonPointer> filter = QueryFilters.parse(queryFilter.asString());
        return filter.accept(queryFilterValidator, getQueryFieldsAsJsonPointers(config.getValidQueryFields()));
    }

    private Set<JsonPointer> getQueryFieldsAsJsonPointers(Set<String> queryFields) {
        if (queryFields == null) {
            return Collections.emptySet();
        }

        Set<JsonPointer> result = new HashSet<>();
        for (String queryField : queryFields) {
            result.add(new JsonPointer(queryField));
        }
        return result;
    }

    private JsonValue findUser(Context requestContext, String queryFilter,
                               UserQueryConfig config) throws ResourceException {
        QueryRequest request = Requests
                .newQueryRequest(config.getIdentityServiceUrl())
                .setQueryFilter(QueryFilters.parse(queryFilter));

        final List<JsonValue> user = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection()) {
            connection.query(requestContext, request,
                    new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resourceResponse) {
                            user.add(resourceResponse.getContent());
                            return true;
                        }
                    });
        }

        if (user.isEmpty() || user.size() > 1) {
            throw new BadRequestException("Unable to find account");
        }

        return user.get(0);
    }

    private void putState(String key, ProcessContext context, String userFieldName, JsonValue user) {
        JsonValue value = user.get(new JsonPointer(userFieldName));
        if (value != null) {
            context.putState(key, value.asString());
        }
    }

}

