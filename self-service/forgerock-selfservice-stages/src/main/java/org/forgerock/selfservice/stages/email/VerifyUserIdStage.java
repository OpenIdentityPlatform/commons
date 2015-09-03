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

package org.forgerock.selfservice.stages.email;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_ID_FIELD;

import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Having retrieved some username attempts to find an associated user and verifies
 * the identity of the user through an email flow using the user's email address.
 *
 * @since 0.1.0
 */
public final class VerifyUserIdStage extends AbstractEmailVerificationStage<VerifyUserIdConfig> {

    /**
     * Constructs a new email stage.
     *
     * @param connectionFactory
     *         the CREST connection factory
     */
    @Inject
    public VerifyUserIdStage(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, VerifyUserIdConfig config) {
        return RequirementsBuilder
                .newInstance("Verify your user account")
                .addRequireProperty("username", "Username")
                .build();
    }

    @Override
    protected String getEmailAddress(ProcessContext context, VerifyUserIdConfig config,
                                     StageResponse.Builder builder) throws ResourceException {
        String username = context
                .getInput()
                .get("username")
                .asString();

        if (isEmpty(username)) {
            throw new BadRequestException("username is missing");
        }

        JsonValue user = findUser(context.getHttpContext(), username, config);

        if (user == null) {
            throw new BadRequestException("Unable to find associated account");
        }

        String userId = user
                .get(config.getIdentityIdField())
                .asString();

        context.putState(USER_ID_FIELD, userId);

        return user
                .get(config.getIdentityEmailField())
                .asString();
    }

    private JsonValue findUser(Context httpContext, String identifier,
                               VerifyUserIdConfig config) throws ResourceException {

        List<QueryFilter<JsonPointer>> filterOptions = new ArrayList<>();
        for (String queryField : config.getQueryFields()) {
            filterOptions.add(QueryFilter.equalTo(new JsonPointer(queryField), identifier));
        }

        QueryRequest request = Requests
                .newQueryRequest(config.getIdentityServiceUrl())
                .setQueryFilter(QueryFilter.or(filterOptions));

        final List<JsonValue> user = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection()) {
            connection.query(httpContext, request, new QueryResourceHandler() {

                @Override
                public boolean handleResource(ResourceResponse resourceResponse) {
                    user.add(resourceResponse.getContent());
                    return true;
                }

            });
        }

        return user.isEmpty() ? null : user.get(0);
    }

}
