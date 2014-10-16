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
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;

// Jackson
import com.fasterxml.jackson.databind.ObjectMapper;

// JSON Fluent
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;

// JSON Reference
import org.forgerock.json.ref.JsonReferenceException;
import org.forgerock.json.ref.JsonReferenceTransformer;

/**
 * Resolves JSON reference values using the Jackson JSON processor, converting from JSON
 * representations to standard Java objects. This transformer applies its transformation for
 * {@code http(s)} and {@code file} URL schemes, or associated relative URIs. Any other URIs
 * are ignored.
 *
 * @author Paul C. Bryan
 */
public class JacksonReferenceTransformer extends JsonReferenceTransformer {

    /** Converts between JSON constructs and Java objects. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs a new Jackson JSON reference transformer.
     *
     * @param base the base URI of the referring document.
     * @param root the root value of the referring document.
     * @throws JsonReferenceException if {@code base} is not {@code null} and is not an absolute URI.
     */
    public JacksonReferenceTransformer(URI base, JsonValue root) throws JsonReferenceException {
        super(base, root);
    }

    @Override
    protected boolean isResolvable(URI uri) {
        String scheme = (uri == null ? null : uri.getScheme());
        return (scheme != null && (scheme.equalsIgnoreCase("file") ||
         scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")));
    }

    @Override
    protected JsonValue resolve(URI uri) throws JsonException {
        JsonValue result;
        InputStream in = null;
        try {
            in = uri.toURL().openStream();
            result = new JsonValue(mapper.readValue(in, Object.class));
        } catch (IOException ioe) {
            throw new JsonException(ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // nothing useful we can do about it
                }
            }
        }
        result.getTransformers().add(0, new JacksonReferenceTransformer(uri, result)); // support $refs
        return result;
    }
}
