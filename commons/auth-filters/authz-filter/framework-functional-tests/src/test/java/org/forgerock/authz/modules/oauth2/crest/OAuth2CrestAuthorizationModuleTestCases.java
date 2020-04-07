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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2.crest;

import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import org.forgerock.authz.AuthzTestCase;
import org.testng.annotations.Test;

@Test(testName = "OAuth2Crest")
public class OAuth2CrestAuthorizationModuleTestCases extends AuthzTestCase {

    @Test
    public void createNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void readNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                get("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void updateNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                put("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void deleteNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                delete("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void patchNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                patch("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void actionCollectionNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void actionInstanceNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                post("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void queryAllowNotAllowedWhenNoAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                get("/modules/oauth2/crest/resource");
    }

    @Test
    public void createAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(201).
                body("operation", equalTo("create")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void readAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Authorization", "Bearer VALID").
            expect().
                statusCode(200).
                body("operation", equalTo("read")).
            when().
                get("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void updateAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
                header("If-Match", "*").
                body("{}").
            expect().
                statusCode(200).
                body("operation", equalTo("update")).
            when().
                put("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void deleteAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
            expect().
                statusCode(200).
                body("operation", equalTo("delete")).
            when().
                delete("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void patchAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(200).
                body("operation", equalTo("patch")).
            when().
                patch("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void actionCollectionAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(200).
                body("operation", equalTo("actionCollection")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void actionInstanceAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(200).
                body("operation", equalTo("action")).
            when().
                post("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void queryAllowedWhenAccessTokenHeaderSet() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer VALID").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(200).
                body("result", hasItem(hasEntry("operation", "queryCollection"))).
            when().
                get("/modules/oauth2/crest/resource");
    }

    @Test
    public void createNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void readNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Authorization", "NotBearer VALID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                get("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void updateNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
                body("{}").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                put("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void deleteNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                delete("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void patchNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                patch("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void actionCollectionNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void actionInstanceNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                post("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void queryNotAllowedWhenAccessTokenHeaderIsNotBearer() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "NotBearer VALID").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is null.")).
            when().
                get("/modules/oauth2/crest/resource");
    }

    @Test
    public void createNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void readNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Authorization", "Bearer INVALID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                get("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void updateNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
                body("{}").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                put("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void deleteNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                delete("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void patchNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                patch("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void actionCollectionNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                post("/modules/oauth2/crest/resource");
    }

    @Test
    public void actionInstanceNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                post("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void queryNotAllowedWhenAccessTokenIsInvalid() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("Authorization", "Bearer INVALID").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Access Token is invalid.")).
            when().
                get("/modules/oauth2/crest/resource");
    }
}
