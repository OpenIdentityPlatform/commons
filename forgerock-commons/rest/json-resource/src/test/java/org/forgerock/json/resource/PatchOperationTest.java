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

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Resources.*;
import static org.forgerock.json.resource.Router.*;
import static org.forgerock.json.resource.TestUtils.*;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;

/**
 * Tests for {@link PatchOperation}.
 */
@SuppressWarnings("javadoc")
public class PatchOperationTest {

    @DataProvider
    private Object[][] operationsNeedsValue() {
        return new String[][] {
            { PatchOperation.OPERATION_ADD },
            { PatchOperation.OPERATION_INCREMENT },
            { PatchOperation.OPERATION_REPLACE },
            { PatchOperation.OPERATION_TRANSFORM }
        };
    }

    @DataProvider
    private Object[][] operationDoesNotAllowFrom() {
        return new String[][] {
            { PatchOperation.OPERATION_ADD },
            { PatchOperation.OPERATION_INCREMENT },
            { PatchOperation.OPERATION_REPLACE },
            { PatchOperation.OPERATION_TRANSFORM },
            { PatchOperation.OPERATION_REMOVE}
        };
    }

    @DataProvider
    private Object[][] operationRequiresFromAndDoesNotAllowValue() {
        return new String[][] {
            { PatchOperation.OPERATION_COPY },
            { PatchOperation.OPERATION_MOVE }
        };
    }

    @Test(dataProvider = "operationsNeedsValue", expectedExceptions = NullPointerException.class)
    public void testPatchWithNullValue(final String operation) {
        PatchOperation.operation(operation, "/users/0", null);
    }

    @Test(dataProvider = "operationDoesNotAllowFrom", expectedExceptions = BadRequestException.class)
    public void testPatchWithFromNotNull(final String operation) throws Exception {
        final JsonValue json = new JsonValue(new LinkedHashMap<>());
        json.put(PatchOperation.FIELD_OPERATION, operation);
        json.put(PatchOperation.FIELD_FIELD, "/users/0");
        json.put(PatchOperation.FIELD_FROM, "/users/1");
        json.put(PatchOperation.FIELD_VALUE, "42");
        PatchOperation.valueOf(json);
    }

    @Test(dataProvider = "operationRequiresFromAndDoesNotAllowValue", expectedExceptions = NullPointerException.class)
    public void testPatchWithFromNull(final String operation) throws Exception {
        PatchOperation.operation(operation, "/users/0", null);
    }

    @Test(dataProvider = "operationRequiresFromAndDoesNotAllowValue", expectedExceptions = BadRequestException.class)
    public void testPatchWithValueNotNull(final String operation) throws Exception {
        final JsonValue json = new JsonValue(new LinkedHashMap<>());
        json.put(PatchOperation.FIELD_OPERATION, operation);
        json.put(PatchOperation.FIELD_FIELD, "/users/0");
        json.put(PatchOperation.FIELD_FROM, "/users/1");
        json.put(PatchOperation.FIELD_VALUE, "42");
        PatchOperation.valueOf(json);
    }

    @Test
    public void testPatchValueOfCopyOperationWithNoValue() throws Exception {
        final JsonValue json = new JsonValue(new LinkedHashMap<>());
        json.put(PatchOperation.FIELD_OPERATION, "copy");
        json.put(PatchOperation.FIELD_FIELD, "/users/0");
        json.put(PatchOperation.FIELD_FROM, "/users/1");
        PatchOperation operation = PatchOperation.valueOf(json);
        assertThat(operation.getOperation()).isEqualTo("copy");
        assertThat(operation.getField()).isEqualTo(new JsonPointer("/users/0"));
        assertThat(operation.getFrom()).isEqualTo(new JsonPointer("/users/1"));
        assertThat(operation.getValue().getObject()).isNull();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testPatchWithNullOperation() throws Exception {
        PatchOperation.operation(null, "users/0", "Dummy");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testPatchWithNullJsonPointer() throws Exception {
        PatchOperation.operation("add", (JsonPointer) null, "Dummy");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPatchIncrementWithInvalidValue() throws Exception {
        PatchOperation.operation(PatchOperation.OPERATION_INCREMENT, "/users/0", "fourty-two");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPatchWithUnknownOperation() throws Exception {
        PatchOperation.operation("Unknown", "users/0", "Dummy");
    }

    /** Test to ensure CREST-320 non regression */
    @Test
    public void testRemoveWithUnknownValue() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        connection.patch(ctx(), Requests.newPatchRequest("users/0", PatchOperation.remove("role", "unknown role")));
        final ResourceResponse resource = connection.read(ctx(), newReadRequest("users/0"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("1");
        assertThat(resource.getContent().getObject()).isEqualTo(userAliceWithIdAndRev(0, 1).getObject());
    }

    /** Test to ensure CREST-320 non regression */
    @Test
    public void testRemoveWithExistingValue() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        connection.patch(ctx(), Requests.newPatchRequest("users/0", PatchOperation.remove("role", "sales")));
        final ResourceResponse resource = connection.read(ctx(), newReadRequest("users/0"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("1");
        assertThat(resource.getContent().getObject()).isEqualTo(
                content(object(field("name", "alice"),
                        field("age", 20),
                        field("_id", "0"),
                        field("_rev", "1"))).getObject());
    }

    private Connection getConnection() {
        final MemoryBackend users = new MemoryBackend();
        final Router router = new Router();
        router.addRoute(uriTemplate("users"), users);
        return newInternalConnection(router);
    }

    private Connection getConnectionWithAliceAndBob() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        return connection;
    }

    private JsonValue userAlice() {
        return content(object(field("name", "alice"), field("age", 20), field("role", "sales")));
    }

    private JsonValue userAliceWithIdAndRev(final int id, final int rev) {
        return content(object(field("name", "alice"), field("age", 20), field("role", "sales"),
                field("_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }
}
