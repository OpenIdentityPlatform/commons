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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.ref.jackson;

// Java Standard Edition
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

// FEST-Assert
import static org.fest.assertions.Assertions.assertThat;

// TestNG
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;

// JSON Reference
import org.forgerock.json.ref.JsonReference;

/**
 * @author Paul C. Bryan
 */
public class JacksonReferenceTransformerTest {

    @Test
    public void doubleIndirection() throws IOException, URISyntaxException {
        File tmp = File.createTempFile("jrt", ".json");
        tmp.deleteOnExit(); // be kind rewind
        PrintWriter out = new PrintWriter(tmp, "UTF-8");
        out.print("{\n");
        out.print("\"a\": { \"b\": { \"c\": { \"$ref\": \"#/d/e\" } } },\n");
        out.print("\"d\": { \"e\": \"f\" }\n");
        out.print("}\n");
        out.close();
        JsonReference ref = new JsonReference().setURI(new URI(tmp.toURI().toString() + "#/a/b/c"));
        JsonValue root = new JsonValue(new HashMap<String, Object>());
        root.put("g", ref.toJsonValue().getObject());
        root.getTransformers().add(0, new JacksonReferenceTransformer(null, null));
        root.applyTransformers();
        assertThat(root.get("g").getObject().equals("f"));
    }
}
