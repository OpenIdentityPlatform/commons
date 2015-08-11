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

import static org.forgerock.selfservice.core.ServiceUtils.EMPTY_TAG;

import org.forgerock.http.context.RootContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.StageType;
import org.forgerock.selfservice.core.exceptions.IllegalInputException;
import org.forgerock.selfservice.core.exceptions.IllegalStageTagException;
import org.forgerock.selfservice.core.exceptions.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotAuthor;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.query.QueryFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Email stage.
 *
 * @since 0.1.0
 */
public class EmailStage implements ProgressStage<EmailStageConfig> {

    private static final String VALIDATE_LINK_TAG = "validateLinkTag";

    private final ConnectionFactory connectionFactory;

    public EmailStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, EmailStageConfig config) {
        return RequirementsBuilder
                .newInstance("Reset your password")
                .addRequireProperty("mail", "Email address for account")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, EmailStageConfig config,
                                 SnapshotAuthor snapshotAuthor) throws IllegalInputException {
        switch (context.getStageTag()) {
        case EMPTY_TAG:
            return sendEmail(context, snapshotAuthor);
        case VALIDATE_LINK_TAG:
            return validateLink(context);
        }

        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse sendEmail(ProcessContext context,
                                    EmailStageConfig config,
                                    SnapshotAuthor snapshotAuthor) throws IllegalInputException {
        String emailAddress = context
                .getInput()
                .get("mail")
                .asString();

        if (emailAddress == null || emailAddress.isEmpty()) {
            throw new IllegalInputException("mail is missing");
        }

        JsonValue user = findUser(emailAddress, config);

        System.out.println(user);

        context = ProcessContext
                .newBuilder(context)
                .addState("mail", emailAddress)
                .setStageTag(VALIDATE_LINK_TAG)
                .build();

        String snapshotToken = snapshotAuthor.captureSnapshotOf(context);

        JsonValue requirements = RequirementsBuilder
                .newInstance("Verify email address")
                .addRequireProperty("code", "Enter code emailed to address provided")
                .addProperty("jwt", "Encrypted value emailed to address provided (when using different browser)")
                .build();

        System.out.println("Email sent with token: " + snapshotToken);

        return StageResponse
                .newBuilder()
                .setRequirements(requirements)
                .setStageTag(VALIDATE_LINK_TAG)
                .build();
    }

    private StageResponse validateLink(ProcessContext context) throws IllegalInputException {
        String emailAddress = context.getState("mail");

        if (emailAddress == null || emailAddress.isEmpty()) {
            throw new IllegalInputException("Missing email address");
        }

        System.out.println("Token valid, found email address " + emailAddress);

        return StageResponse
                .newBuilder()
                .build();
    }

    private JsonValue findUser(String identifier, EmailStageConfig config) {
        try {
            Connection connection = connectionFactory.getConnection();

            QueryRequest request = Requests.newQueryRequest("/user");
            request.setQueryFilter(
                    QueryFilter.or(
                            QueryFilter.equalTo(new JsonPointer("userId"), "fred"),
                            QueryFilter.equalTo(new JsonPointer("mail"), "a@b.com")));

            final List<JsonValue> user = new ArrayList<>();
            connection.query(new RootContext(), request, new QueryResourceHandler() {

                @Override
                public boolean handleResource(ResourceResponse resourceResponse) {
                    user.add(resourceResponse.getContent());
                    return true;
                }

            });

            if (user.size() > 1) {
                throw new StageConfigException("More than one user identified");
            }

            return user.isEmpty() ? null : user.get(0);
        } catch (ResourceException rE) {
            throw new StageConfigException(rE.getMessage());
        }
    }

    @Override
    public StageType<EmailStageConfig> getStageType() {
        return EmailStageConfig.TYPE;
    }

}
