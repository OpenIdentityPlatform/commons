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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.services.context.SecurityContext;
import org.testng.annotations.Test;

import java.util.HashMap;

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
        assertThat(root.getContextName()).isEqualTo("root");
    }

    @Test
    public void testNewRootContextWithUserID() {
        final Context root = new RootContext("root-id");
        assertThat(root.getParent()).isNull();
        assertThat(root.getId()).isEqualTo("root-id");
        assertThat(root.isRootContext()).isTrue();
        assertThat(root.getContextName()).isEqualTo("root");
    }

    @Test
    public void testContainsContext() throws Exception {
        final Context root = new RootContext("root-id");
        final Context context = ClientContext.newInternalClientContext(root);

        assertThat(context.containsContext(RootContext.class)).isTrue();
        assertThat(context.containsContext(SecurityContext.class)).isFalse();
        assertThat(context.containsContext("root")).isTrue();
        assertThat(context.containsContext("security")).isFalse();
    }

    @Test
    public void testAsContext() throws Exception {
        final Context root = new RootContext("root-id");
        final ClientContext internal = ClientContext.newInternalClientContext(root);
        final UriRouterContext router = new UriRouterContext(internal, "test", "", new HashMap<String, String>(0));
        final ClientContext internal2 = ClientContext.newInternalClientContext(root);

        assertThat(router.asContext(RootContext.class)).isSameAs(root);
        assertThat(router.asContext(UriRouterContext.class)).isSameAs(router);
        assertThat(router.asContext(Context.class)).isSameAs(router);
        assertThat(router.getParent().asContext(Context.class)).isSameAs(internal);
        assertThat(router.asContext(ClientContext.class)).isSameAs(internal);
        assertThat(internal2.asContext(ClientContext.class)).isSameAs(internal2);

        try {
            router.asContext(SecurityContext.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No context of type " + SecurityContext.class.getName() + " found.")
                    .hasNoCause();
        }
    }

    @Test
    public void testGetContext() throws Exception {
        final Context root = new RootContext("root-id");
        final Context context = ClientContext.newInternalClientContext(root);

        assertThat(context.getContext("root")).isSameAs(root);
        try {
            context.getContext("test");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No context of named test found.")
                    .hasNoCause();
        }
    }
}
