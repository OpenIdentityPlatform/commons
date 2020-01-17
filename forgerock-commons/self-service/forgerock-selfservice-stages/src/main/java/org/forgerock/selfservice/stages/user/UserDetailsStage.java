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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;

import java.util.Map;

/**
 * Stage is responsible for request a new user json representation.
 * It expects the "mail" field to be populated in the context
 * which it uses to verify against the email address specified in the passed user object.
 *
 * @since 0.2.0
 */
public final class UserDetailsStage implements ProgressStage<UserDetailsConfig> {

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
            UserDetailsConfig config) throws ResourceException {
        return RequirementsBuilder
                .newInstance("New user details")
                .addRequireProperty("user", "object", "User details")
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, UserDetailsConfig config) throws ResourceException {
        JsonValue user = context.getInput().get("user").required();

        processEmail(context, config, user);

        JsonValue userState = ensureUserInContext(context);
        Map<String, Object> properties = user.asMap();
        updateUserJsonValue(userState, properties);
        context.putState(USER_FIELD, userState);

        return StageResponse.newBuilder().build();
    }

    private void processEmail(ProcessContext context, UserDetailsConfig config, JsonValue user)
            throws BadRequestException {
        if (context.containsState(EMAIL_FIELD)) {
            JsonValue emailFieldContext = context.getState(EMAIL_FIELD);
            JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser == null) {
                user.put(new JsonPointer(config.getIdentityEmailField()), emailFieldContext.asString());
            } else if (!emailFieldUser.asString().equalsIgnoreCase(emailFieldContext.asString())) {
                throw new BadRequestException("Email address mismatch");
            }
        } else {
            JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser != null) {
                context.putState(EMAIL_FIELD, emailFieldUser.asString());
            }
        }
    }

    private JsonValue ensureUserInContext(ProcessContext context) {
        JsonValue user = context.getState(USER_FIELD);
        if (user == null) {
            user = json(object());
            context.putState(USER_FIELD, user);
        }
        return user;
    }

    private void updateUserJsonValue(JsonValue userState, Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            userState.add(key, value);
        }
    }

}
