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

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.forgerock.authz.AuthzTestCase;
import org.testng.annotations.Test;

@Test(testName = "OAuth2Crest")
public class OAuth2CrestAuthorizationModuleTestCases extends AuthzTestCase {

    @Test
    public void createNotAllowedWhenNoAccessTokenHeaderSet() {

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
                header("Authorization", "Bearer VALID").
            expect().
                statusCode(200).
                body("operation", equalTo("read")).
            when().
                get("/modules/oauth2/crest/resource/a");
    }

    @Test
    public void updateAllowedWhenAccessTokenHeaderSet() {

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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

        given().
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
