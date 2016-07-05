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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.selfservice.stages.captcha;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.selfservice.core.util.RequirementsBuilder.newEmptyObject;
import static org.forgerock.util.CloseSilentlyFunction.closeSilently;

import org.forgerock.http.Client;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;

/**
 * Stage is responsible for captcha based security.
 *
 * @since 0.2.0
 */
public final class CaptchaStage implements ProgressStage<CaptchaStageConfig> {

    private final Client httpClient;

    /**
     * Constructs a new captcha stage.
     *
     * @param httpClient
     *         the http client
     */
    @Inject
    public CaptchaStage(@SelfService Client httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, CaptchaStageConfig config)
            throws ResourceException {
        Reject.ifNull(config.getRecaptchaSiteKey(), "Captcha stage expects re-CAPTCHA site key");
        Reject.ifNull(config.getRecaptchaSecretKey(), "Captcha stage expects re-CAPTCHA secret key");
        Reject.ifNull(config.getRecaptchaUri(), "Captcha stage expects URI for recaptcha verification");

        return RequirementsBuilder.newInstance("Captcha stage")
                .addRequireProperty("response",
                        newEmptyObject()
                                .addCustomField("recaptchaSiteKey", json(config.getRecaptchaSiteKey()))
                                .addCustomField("description", json("Captcha response"))
                                .addCustomField("type", json("string")))
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, CaptchaStageConfig config) throws ResourceException {
        JsonValue response = context.getInput().get("response").required();

        verifyRecaptchaResponse(config, response);

        return StageResponse.newBuilder().build();
    }

    private void verifyRecaptchaResponse(CaptchaStageConfig config, JsonValue response) throws BadRequestException {
        URI uri = URI.create(config.getRecaptchaUri()
                + "?secret=" + config.getRecaptchaSecretKey()
                + "&response=" + response.asString());

        JsonValue result = sendPostRequest(uri);

        if (!result.get("success").asBoolean()) {
            throw new BadRequestException("Recaptcha verification is unsuccessful. " + result.toString());
        }
    }

    private JsonValue sendPostRequest(URI uri) {
        return httpClient
                .send(new Request()
                        .setMethod("POST")
                        .setUri(uri))
                .then(
                        closeSilently(new Function<Response, JsonValue, NeverThrowsException>() {
                            @Override
                            public JsonValue apply(Response response) {
                                try {
                                    return json(response.getEntity().getJson());
                                } catch (IOException e) {
                                    throw new IllegalStateException("Unable to verify recaptcha", e);
                                }
                            }
                        }),
                        Responses.<JsonValue, NeverThrowsException>noopExceptionFunction())
                .getOrThrowUninterruptibly();
    }

}

