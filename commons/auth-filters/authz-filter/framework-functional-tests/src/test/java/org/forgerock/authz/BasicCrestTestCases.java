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

package org.forgerock.authz;

import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import org.testng.annotations.Test;

@Test(testName = "BasicCrestTestCases")
public class BasicCrestTestCases extends AuthzTestCase {

    @Test
    public void alwaysAllowAuthorizationShouldAllowCreateRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(201).
                body("operation", equalTo("create")).
            when().
                post("/basic/crest/simple/users");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowReadRequestsToUsersEndpoint() {

        RestAssured.given().
            expect().
                statusCode(200).
                body("operation", equalTo("read")).
            when().
                get("/basic/crest/simple/users/demo");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowUpdateRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("If-Match", "*").
                body("{}").
            expect().
                statusCode(200).
                body("operation", equalTo("update")).
            when().
                put("/basic/crest/simple/users/demo");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowDeleteRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
            expect().
                statusCode(200).
                body("operation", equalTo("delete")).
            when().
                delete("/basic/crest/simple/users/demo");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowPatchRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(200).
                body("operation", equalTo("patch")).
            when().
                patch("/basic/crest/simple/users/demo");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowActionCollectionRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(200).
                body("operation", equalTo("actionCollection")).
            when().
                post("/basic/crest/simple/users");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowActionRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(200).
                body("operation", equalTo("action")).
            when().
                post("/basic/crest/simple/users/demo");
    }

    @Test
    public void alwaysAllowAuthorizationShouldAllowQueryRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(200).
                body("result", hasItem(hasEntry("operation", "queryCollection"))).
            when().
                get("/basic/crest/simple/users");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyCreateRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/simple/roles");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyReadRequestsToRolesEndpoint() {

        RestAssured.given().
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                get("/basic/crest/simple/roles/admin");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyUpdateRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                put("/basic/crest/simple/roles/admin");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyDeleteRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                delete("/basic/crest/simple/roles/admin");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyPatchRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                patch("/basic/crest/simple/roles/admin");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyActionCollectionRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/simple/roles");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyActionRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/simple/roles/admin");
    }

    @Test
    public void alwaysDenyAuthorizationShouldDenyQueryRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Not authorized for endpoint: roles")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                get("/basic/crest/simple/roles");
    }

    @Test
    public void notActionAuthorizationShouldAllowCreateRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(201).
                body("operation", equalTo("create")).
            when().
                post("/basic/crest/notAction");
    }

    @Test
    public void notActionAuthorizationShouldAllowReadRequestsToUsersEndpoint() {

        RestAssured.given().
            expect().
                statusCode(200).
                body("operation", equalTo("read")).
            when().
                get("/basic/crest/notAction/x");
    }

    @Test
    public void notActionAuthorizationShouldAllowUpdateRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("If-Match", "*").
                body("{}").
            expect().
                statusCode(200).
                body("operation", equalTo("update")).
            when().
                put("/basic/crest/notAction/x");
    }

    @Test
    public void notActionAuthorizationShouldAllowDeleteRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
            expect().
                statusCode(200).
                body("operation", equalTo("delete")).
            when().
                delete("/basic/crest/notAction/x");
    }

    @Test
    public void notActionAuthorizationShouldAllowPatchRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(200).
                body("operation", equalTo("patch")).
            when().
                patch("/basic/crest/notAction/x");
    }

    @Test
    public void  notActionAuthorizationShouldDenyActionCollectionRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Action is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/notAction/x");
    }

    @Test
    public void notActionAuthorizationShouldDenyActionRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Action is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/notAction/x");
    }

    @Test
    public void notActionAuthorizationShouldAllowQueryRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(200).
                body("result", hasItem(hasEntry("operation", "queryCollection"))).
            when().
                get("/basic/crest/notAction");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldDenyCreateRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Create is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/notCreateOrPatch");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldAllowReadRequestsToUsersEndpoint() {

        RestAssured.given().
            expect().
                statusCode(200).
                body("operation", equalTo("read")).
            when().
                get("/basic/crest/notCreateOrPatch/x");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldAllowUpdateRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("If-Match", "*").
                body("{}").
            expect().
                statusCode(200).
                body("operation", equalTo("update")).
            when().
                put("/basic/crest/notCreateOrPatch/x");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldAllowDeleteRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
            expect().
                statusCode(200).
                body("operation", equalTo("delete")).
            when().
                delete("/basic/crest/notCreateOrPatch/x");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldDenyPatchRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Patch is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                patch("/basic/crest/notCreateOrPatch/x");
    }

    @Test
    public void  notCreateOrPatchAuthorizationShouldAllowActionCollectionRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(200).
                body("operation", equalTo("actionCollection")).
            when().
                post("/basic/crest/notCreateOrPatch");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldAllowActionRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(200).
                body("operation", equalTo("action")).
            when().
                post("/basic/crest/notCreateOrPatch/x");
    }

    @Test
    public void notCreateOrPatchAuthorizationShouldAllowQueryRequestsToUsersEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(200).
                body("result", hasItem(hasEntry("operation", "queryCollection"))).
            when().
                get("/basic/crest/notCreateOrPatch");
    }

    @Test
    public void noneAuthorizationShouldDenyCreateRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "create").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Create is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyReadRequestsToRolesEndpoint() {

        RestAssured.given().
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Read is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                get("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyUpdateRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                header("If-Match", "*").
                body("{}").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Update is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                put("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyDeleteRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Delete is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                delete("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyPatchRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("[{\"operation\": \"add\", \"field\": \"FIELD\", \"value\": \"VALUE\"}]").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Patch is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                patch("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyActionCollectionRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Action is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyActionRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                body("{}").
                queryParam("_action", "ACTION").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Action is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                post("/basic/crest/none/x");
    }

    @Test
    public void noneAuthorizationShouldDenyQueryRequestsToRolesEndpoint() {

        RestAssured.given().
                header("Content-Type", "application/json").
                queryParam("_queryId", "QUERY_ID").
            expect().
                statusCode(403).
                body("code", equalTo(403)).
                body("reason", equalTo("Forbidden")).
                body("message", equalTo("Query is not allowed")).
                body("detail", hasEntry("internalCode", 123)).
            when().
                get("/basic/crest/none");
    }
}
