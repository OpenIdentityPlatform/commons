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
package org.forgerock.selfservice.stages.captcha;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link CaptchaStage}.
 *
 * @since 0.2.0
 */
public final class CaptchaStageTest {

    private CaptchaStage captchaStage;

    private CaptchaStageConfig config;
    @Mock
    private ProcessContext context;
    @Mock
    private Handler handler;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        config = new CaptchaStageConfig();
        captchaStage = new CaptchaStage(new Client(handler));
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Captcha stage expects re-CAPTCHA site key")
    public void testGatherInitialRequirementsNoSiteKey() throws Exception {
        // When
        captchaStage.gatherInitialRequirements(context, config);
    }


    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Captcha stage expects re-CAPTCHA secret key")
    public void testGatherInitialRequirementsNoSecretKey() throws Exception {
        // Given
        config.setRecaptchaSiteKey("siteKey1");

        // When
        captchaStage.gatherInitialRequirements(context, config);
    }

    @Test (expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Captcha stage expects URI for recaptcha verification")
    public void testGatherInitialRequirementsNoRecaptchaUri() throws Exception {
        // Given
        config.setRecaptchaSiteKey("siteKey1");
        config.setRecaptchaSecretKey("secretKey1");

        // When
        captchaStage.gatherInitialRequirements(context, config);
    }

    @Test
    public void testGatherInitialRequirements() throws Exception {
        // Given
        config.setRecaptchaSiteKey("siteKey1");
        config.setRecaptchaSecretKey("secretKey1");
        config.setRecaptchaUri("https://www.google.com/recaptcha/api/siteverify");

        // When
        JsonValue jsonValue = captchaStage.gatherInitialRequirements(context, config);

        // Then
        assertThat(jsonValue).stringAt("description").isEqualTo("Captcha stage");
        assertThat(jsonValue).stringAt("properties/response/description").isEqualTo("Captcha response");
        assertThat(jsonValue).stringAt("properties/response/recaptchaSiteKey").isEqualTo("siteKey1");
    }

    @Test (expectedExceptions = JsonValueException.class,
            expectedExceptionsMessageRegExp = "/response: Expecting a value")
    public void testAdvanceNoInput() throws Exception {
        // Given
        config.setRecaptchaSecretKey("6Le4og4TAAAAAFPprcsXlHE9bYYPAMX794A6R3Mv");
        config.setRecaptchaUri("https://www.google.com/recaptcha/api/siteverify");
        given(context.getInput()).willReturn(newEmptyJsonValue());
        given(handler.handle(any(Context.class), any(Request.class))).willReturn(newPromiseFailure());
        // When
        captchaStage.advance(context, config);
    }

    @Test (expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Recaptcha verification is unsuccessful\\. "
                    + "\\{ \"success\": false, \"error-codes\": \\[ \"missing-input-response\", "
                    + "\"missing-input-secret\" \\] \\}")
    public void testAdvanceVerificationFailed() throws Exception {
        // Given
        config.setRecaptchaSecretKey("6Le4og4TAAAAAFPprcsXlHE9bYYPAMX794A6R3Mv");
        config.setRecaptchaUri("https://www.google.com/recaptcha/api/siteverify");
        given(context.getInput()).willReturn(getInputCaptchaResponse());
        given(handler.handle(any(Context.class), any(Request.class))).willReturn(newPromiseFailure());
        // When
        captchaStage.advance(context, config);
    }

    @Test
    public void testAdvance() throws Exception {
        // Given
        config.setRecaptchaSecretKey("6Le4og4TAAAAAFPprcsXlHE9bYYPAMX794A6R3Mv");
        config.setRecaptchaUri("https://www.google.com/recaptcha/api/siteverify");
        given(context.getInput()).willReturn(getInputCaptchaResponse());
        given(handler.handle(any(Context.class), any(Request.class))).willReturn(newPromiseSuccess());
        // When
        captchaStage.advance(context, config);
    }

    private JsonValue getInputCaptchaResponse() {
        return json(object(field("response", "03AHJ_Vusv7AmXhUspWlOLLvaCb5tSgtiPCBE0ci4vgmkXA")));
    }

    private Promise<Response, NeverThrowsException> newPromiseSuccess() {
        Response response = new Response();
        response.getEntity().setJson(json(object(
                field("success", true))));
        return Promises.newResultPromise(response);
    }

    private Promise<Response, NeverThrowsException> newPromiseFailure() {
        Response response = new Response();
        response.getEntity().setJson(json(object(
                field("success", false),
                field("error-codes",
                        array("missing-input-response", "missing-input-secret")))));
        return Promises.newResultPromise(response);
    }

    private JsonValue newEmptyJsonValue() {
        return json(object());
    }

}
