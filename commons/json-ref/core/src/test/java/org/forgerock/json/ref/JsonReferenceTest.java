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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.ref;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JsonReferenceTest {

    /** Empty JSON object. */
    private JsonValue root;

    @BeforeMethod
    public void beforeMethod() {
        root = new JsonValue(new HashMap<>());
    }

    @Test
    public void x() throws URISyntaxException {
        JsonReference ref = new JsonReference().setURI(new URI("#/foo"));
        root.put("foo", "bar");
        root.put("baz", ref.toJsonValue().getObject());
//        root.getTransformers().add(new JsonReferenceTransformer(null, root));
//        root.applyTransformers();
        new JsonReferenceTransformer(null, root).transform(root);
        assertThat(root.get("baz").getObject().equals("bar"));
    }
}
