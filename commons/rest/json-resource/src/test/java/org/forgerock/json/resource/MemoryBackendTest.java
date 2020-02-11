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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.PatchOperation.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.json.resource.TestUtils.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.testng.Assert.fail;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.query.QueryFilter;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link MemoryBackend}.
 */
@SuppressWarnings("javadoc")
public final class MemoryBackendTest {

    @Test
    public void testActionCollectionClear() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        connection.action(ctx(), newActionRequest("users", "clear"));
        try {
            connection.read(ctx(), newReadRequest("users/0"));
            fail("Read succeeded unexpectedly");
        } catch (final Exception e) {
            assertThat(e).isInstanceOf(NotFoundException.class);
        }
        try {
            connection.read(ctx(), newReadRequest("users/1"));
            fail("Read succeeded unexpectedly");
        } catch (final Exception e) {
            assertThat(e).isInstanceOf(NotFoundException.class);
        }
    }

    @Test(expectedExceptions = NotSupportedException.class)
    public void testActionCollectionUnknown() throws Exception {
        final Connection connection = getConnection();
        connection.action(ctx(), newActionRequest("users", "unknown"));
    }

    @Test(expectedExceptions = NotSupportedException.class)
    public void testActionInstanceUnknown() throws Exception {
        final Connection connection = getConnection();
        connection.action(ctx(), newActionRequest("users/0", "unknown"));
    }

    @Test
    public void testCreateCollection() throws Exception {
        final Connection connection = getConnection();
        final ResourceResponse resource1 =
                connection.create(ctx(), newCreateRequest("users", userAlice()));
        final ResourceResponse resource2 = connection.read(ctx(), newReadRequest("users/0"));
        assertThat(resource1).isEqualTo(resource2);
        assertThat(resource1.getId()).isEqualTo("0");
        assertThat(resource1.getRevision()).isEqualTo("0");
        assertThat(resource1.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(0, 0).getObject());
    }

    @Test
    public void testCreateInstance() throws Exception {
        final Connection connection = getConnection();
        final ResourceResponse resource1 =
                connection.create(ctx(), newCreateRequest("users", "123", userAlice()));
        final ResourceResponse resource2 = connection.read(ctx(), newReadRequest("users/123"));
        assertThat(resource1).isEqualTo(resource2);
        assertThat(resource1.getId()).isEqualTo("123");
        assertThat(resource1.getRevision()).isEqualTo("0");
        assertThat(resource1.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(123, 0).getObject());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testDeleteCollection() throws Exception {
        final Connection connection = getConnection();
        connection.delete(ctx(), newDeleteRequest("users"));
    }

    @Test
    public void testDeleteInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final ResourceResponse resource = connection.delete(ctx(), newDeleteRequest("users/0"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(0, 0).getObject());
        try {
            connection.read(ctx(), newReadRequest("users/0"));
            fail("Read succeeded unexpectedly");
        } catch (final NotFoundException e) {
            // Expected.
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testPatchCollection() throws Exception {
        final Connection connection = getConnection();
        connection.patch(ctx(), newPatchRequest("users", add("/test", "value")));
    }

    @Test
    public void testPatchInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final ResourceResponse resource =
                connection.patch(ctx(), newPatchRequest("users/0", replace("/name", "bob"),
                        increment("/age", 10), remove("/role"), add("/role", "it")));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("1");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userBobWithIdAndRev(0, 1).getObject());
    }

    @Test
    public void testQueryCollection() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        final Collection<ResourceResponse> results = new ArrayList<>();
        connection.query(ctx(), newQueryRequest("users"), results);
        assertThat(results).containsOnly(asResource(userAliceWithIdAndRev(0, 0)),
                asResource(userBobWithIdAndRev(1, 0)));
    }

    @Test
    public void testQueryCollectionWithSort() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo", 30, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo3", 33, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo1", 31, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo4", 34, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo5", 35, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo2", 32, "it")));

        final QueryRequest request = newQueryRequest("users")
                .addSortKey("+/age");

        final List<ResourceResponse> results = new ArrayList<ResourceResponse>();
        final QueryResponse result = connection.query(ctx(), request, results);

        assertThat(results.get(0).getContent().get("name").asString()).isEqualTo("foo");
        assertThat(results.get(3).getContent().get("name").asString()).isEqualTo("foo3");
        assertThat(results.get(5).getContent().get("name").asString()).isEqualTo("foo5");
    }

    @Test
    public void testQueryCollectionWithOffset() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo", 30, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo1", 31, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo2", 32, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo3", 33, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo4", 34, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo5", 35, "it")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo6", 36, "it")));

        final QueryRequest request = newQueryRequest("users")
                .addSortKey("+/age") // have to sort since backend is a hash
                .setPageSize(2)
                .setPagedResultsOffset(2);

        final List<ResourceResponse> results = new ArrayList<ResourceResponse>();
        final QueryResponse result = connection.query(ctx(), request, results);

        assertThat(results.get(0).getContent().get("name").asString()).isEqualTo("foo2");
        assertThat(results.get(1).getContent().get("name").asString()).isEqualTo("foo3");

        assertThat(results.size()).isEqualTo(2);
        assertThat(result.getPagedResultsCookie()).isNotNull();
    }

    @Test
    public void testQueryCollectionWithCookie() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo", 30, "eng")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo1", 31, "eng")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo2", 32, "eng")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo3", 33, "eng")));
        connection.create(ctx(), newCreateRequest("users", jsonUser("foo4", 34, "eng")));

        // first page

        QueryRequest request = newQueryRequest("users").addSortKey("+/name").setPageSize(2);
        List<ResourceResponse> results = new ArrayList<ResourceResponse>();
        QueryResponse result = connection.query(ctx(), request, results);

        assertThat(results.size()).isEqualTo(2);
        assertThat(result.getPagedResultsCookie()).isNotNull();
        assertThat(results.get(0).getContent().get("name").asString()).isEqualTo("foo");
        assertThat(results.get(1).getContent().get("name").asString()).isEqualTo("foo1");

        // second page

        results = new ArrayList<ResourceResponse>();
        request.setPagedResultsCookie(result.getPagedResultsCookie());
        result = connection.query(ctx(), request, results);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).getContent().get("name").asString()).isEqualTo("foo2");
        assertThat(results.get(1).getContent().get("name").asString()).isEqualTo("foo3");
        assertThat(result.getPagedResultsCookie()).isNotNull();

        // third (final) page

        results = new ArrayList<ResourceResponse>();
        request.setPagedResultsCookie(result.getPagedResultsCookie());
        result = connection.query(ctx(), request, results);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getContent().get("name").asString()).isEqualTo("foo4");
        assertThat(result.getPagedResultsCookie()).isNull();
    }

    @Test
    public void testQueryCollectionFailsWithOffsetAndCookie() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        connection.create(ctx(), newCreateRequest("users", userBob()));

        QueryRequest request = newQueryRequest("users").setPageSize(1).addSortKey("+/name");
        final QueryResponse result = connection.query(ctx(), request, new ArrayList<ResourceResponse>());
        final String nextCookie = result.getPagedResultsCookie();
        request.setPagedResultsOffset(1).setPagedResultsCookie(nextCookie);

        try {
            connection.query(ctx(), request, new ArrayList<ResourceResponse>());
            fail("Query with offset and cookie unexpectedly succeeded");
        } catch (final Exception e) {
            assertThat(e).isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    public void testQueryCollectionWithFilters() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        final Collection<ResourceResponse> results = new ArrayList<>();
        connection.query(ctx(), newQueryRequest("users").setQueryFilter(
                QueryFilter.equalTo(new JsonPointer("name"), "alice")).addField("_id"), results);
        assertThat(results).hasSize(1);
        final ResourceResponse resource = results.iterator().next();
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent().getObject()).isEqualTo(object(field("_id", "0")));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testQueryInstance() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        final Collection<ResourceResponse> results = new ArrayList<>();
        connection.query(ctx(), newQueryRequest("users/0"), results);
    }

    @Test
    public void testQueryCollectionEstimateCountWithoutPaging() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        final Collection<ResourceResponse> results = new ArrayList<ResourceResponse>();

        final QueryRequest request = newQueryRequest("users").setTotalPagedResultsPolicy(CountPolicy.ESTIMATE);
        final QueryResponse result = connection.query(ctx(), request, results);

        assertThat(result.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.NONE);
        assertThat(result.getTotalPagedResults()).isEqualTo(QueryResponse.NO_COUNT);
    }

    @Test
    public void testQueryCollectionExactCountWithoutPaging() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();
        final Collection<ResourceResponse> results = new ArrayList<ResourceResponse>();

        final QueryRequest request = newQueryRequest("users").setTotalPagedResultsPolicy(CountPolicy.EXACT);
        final QueryResponse result = connection.query(ctx(), request, results);

        assertThat(result.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.NONE);
        assertThat(result.getTotalPagedResults()).isEqualTo(QueryResponse.NO_COUNT);
    }

    @Test
    public void testQueryCollectionDefaultsToNoneCount() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();

        final Collection<ResourceResponse> results = new ArrayList<ResourceResponse>();
        QueryResponse result = connection.query(ctx(), newQueryRequest("users"), results);

        assertThat(result.getTotalPagedResults()).isEqualTo(QueryResponse.NO_COUNT);
        assertThat(result.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.NONE);
    }

    @Test
    public void testQueryCollectionWithNoneCount() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();

        final Collection<ResourceResponse> results = new ArrayList<ResourceResponse>();
        QueryRequest request = newQueryRequest("users");

        request.setTotalPagedResultsPolicy(CountPolicy.NONE);
        QueryResponse result = connection.query(ctx(), request, results);

        assertThat(result.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.NONE);
        assertThat(result.getTotalPagedResults()).isEqualTo(QueryResponse.NO_COUNT);
    }

    @Test
    public void testQueryCollectionWithExactCount() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();

        QueryRequest request = newQueryRequest("users");
        request.setTotalPagedResultsPolicy(CountPolicy.EXACT);
        request.setPageSize(5);

        final Collection<ResourceResponse> results = new ArrayList<ResourceResponse>();
        final QueryResponse result = connection.query(ctx(), request, results);

        assertThat(result.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.EXACT);
        assertThat(result.getTotalPagedResults()).isEqualTo(2);
    }

    @Test
    public void testQueryCollectionWithEstimatedCountIsExact() throws Exception {
        final Connection connection = getConnectionWithAliceAndBob();

        QueryRequest request = newQueryRequest("users")
                .setTotalPagedResultsPolicy(CountPolicy.ESTIMATE)
                .setPageSize(5);

        final Collection<ResourceResponse> results = new ArrayList<ResourceResponse>();
        QueryResponse result = connection.query(ctx(), request, results);

        // It should fall back to an exact count
        assertThat(result.getTotalPagedResultsPolicy()).isEqualTo(CountPolicy.EXACT);
        assertThat(result.getTotalPagedResults()).isEqualTo(2);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testReadCollection() throws Exception {
        final Connection connection = getConnection();
        connection.read(ctx(), newReadRequest("users"));
    }

    @Test
    public void testReadInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final ResourceResponse resource = connection.read(ctx(), newReadRequest("users/0"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userAliceWithIdAndRev(0, 0).getObject());
    }

    @Test
    public void testReadInstanceWithFieldFilter() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final ResourceResponse resource =
                connection.read(ctx(), newReadRequest("users/0").addField("_id"));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("0");
        assertThat(resource.getContent()).stringAt("_id").isEqualTo("0");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testUpdateCollection() throws Exception {
        final Connection connection = getConnection();
        connection.update(ctx(), newUpdateRequest("users", userAlice()));
    }

    @Test
    public void testUpdateInstance() throws Exception {
        final Connection connection = getConnection();
        connection.create(ctx(), newCreateRequest("users", userAlice()));
        final ResourceResponse resource = connection.update(ctx(), newUpdateRequest("users/0", userBob()));
        assertThat(resource.getId()).isEqualTo("0");
        assertThat(resource.getRevision()).isEqualTo("1");
        assertThat(resource.getContent().getObject()).isEqualTo(
                userBobWithIdAndRev(0, 1).getObject());
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
        connection.create(ctx(), newCreateRequest("users", userBob()));

        return connection;
    }

    private JsonValue jsonUser(String name, int age, String role) {
        return content(object(field("name", name), field("age", age), field("role", role)));
    }

    private JsonValue userAlice() {
        return content(object(field("name", "alice"), field("age", 20), field("role", "sales")));
    }

    private JsonValue userAliceWithIdAndRev(final int id, final int rev) {
        return content(object(field("name", "alice"), field("age", 20), field("role", "sales"),
                field("_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }

    private JsonValue userBob() {
        return content(object(field("name", "bob"), field("age", 30), field("role", "it")));
    }

    private JsonValue userBobWithIdAndRev(final int id, final int rev) {
        return content(object(field("name", "bob"), field("age", 30), field("role", "it"), field(
                "_id", String.valueOf(id)), field("_rev", String.valueOf(rev))));
    }

}
