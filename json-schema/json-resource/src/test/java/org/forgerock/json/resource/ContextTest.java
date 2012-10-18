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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

/**
 * Tests {@code Context}.
 */
@SuppressWarnings("javadoc")
public final class ContextTest {

    @Test
    public void testNewRootContext() {
        final RootContext root = new RootContext();
        assertThat(root.getParent()).isNull();
        assertThat(root.getId()).isNotEmpty();
        assertThat(root.isRootContext()).isTrue();
    }

    @Test
    public void testNewRootContextWithUserID() {
        final Context root = new RootContext("root-id");
        assertThat(root.getParent()).isNull();
        assertThat(root.getId()).isEqualTo("root-id");
        assertThat(root.isRootContext()).isTrue();
    }

    @Test
    public void testNewServerContext() throws Exception {
        final Connection connection = mock(Connection.class);
        final ConnectionProvider provider = mock(ConnectionProvider.class);
        when(provider.getConnectionId(any(Connection.class))).thenReturn("my-connection-id");
        when(provider.getConnection(anyString())).thenReturn(connection);
        final PersistenceConfig config = PersistenceConfig.builder().connectionProvider(provider)
                .build();

        final Context root = new RootContext("root-id");
        final ServerContext context = new ServerContext(root, connection);

        assertThat(context.getParent()).isSameAs(root);
        assertThat(context.getId()).isNotEmpty();
        assertThat(context.isRootContext()).isFalse();

        final JsonValue json = ServerContext.saveToJson(context, config);
        assertThat(json.isMap()).isTrue();
        assertThat(json.size()).isEqualTo(4);
        assertThat(json.get("class").asString()).isEqualTo(
                "org.forgerock.json.resource.ServerContext");
        assertThat(json.get("id").asUUID()).isNotNull();
        assertThat(json.get("parent").isMap()).isTrue();
        assertThat(json.get("parent").size()).isEqualTo(2);
        assertThat(json.get("parent").get("class").asString()).isEqualTo(
                "org.forgerock.json.resource.RootContext");
        assertThat(json.get("parent").get("id").asString()).isEqualTo("root-id");
        assertThat(json.get("parent").get("parent").asMap()).isNull();
        assertThat(json.get("connection-id").asString()).isEqualTo("my-connection-id");
    }

    @Test
    public void testServerContextSaveLoad() throws Exception {
        final Connection connection = mock(Connection.class);
        final ConnectionProvider provider = mock(ConnectionProvider.class);
        when(provider.getConnectionId(any(Connection.class))).thenReturn("my-connection-id");
        when(provider.getConnection(anyString())).thenReturn(connection);
        final PersistenceConfig config = PersistenceConfig.builder().connectionProvider(provider)
                .build();

        final JsonValue inRoot = new JsonValue(new LinkedHashMap<String, Object>());
        inRoot.add("class", "org.forgerock.json.resource.RootContext");
        inRoot.add("id", "root-id");
        inRoot.add("parent", null);

        final JsonValue in = new JsonValue(new LinkedHashMap<String, Object>());
        in.add("class", "org.forgerock.json.resource.ServerContext");
        in.add("id", "child-id");
        in.add("parent", inRoot.asMap());
        in.add("connection-id", "my-connection-id");

        final ServerContext context = ServerContext.loadFromJson(in, config);
        assertThat(context.getId()).isEqualTo("child-id");
        assertThat(context.isRootContext()).isFalse();
        assertThat(context.getParent().getId()).isEqualTo("root-id");
        assertThat(context.getParent().getParent()).isNull();
        assertThat(context.getParent().isRootContext()).isTrue();
        assertThat(context.getConnection()).isSameAs(connection);
    }

}
