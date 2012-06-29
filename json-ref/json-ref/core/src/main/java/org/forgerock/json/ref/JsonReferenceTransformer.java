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

package org.forgerock.json.ref;

// Java SE
import java.net.URI;
import java.net.URISyntaxException;

// JSON Fluent
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonTransformer;
import org.forgerock.json.fluent.JsonValue;

/**
 * Simple JSON reference transformer that can resolve fragment URI references. Classes
 * can extend this class to resolve other types of URIs.
 * <p>
 * Note: The current implementation of this class has no checks for cyclic references. This
 * can lead to scenarios of infinite recursion and throwing of a {@link StackOverflowError}.
 * Plans for mitigating this are in place and will be implemented within this class.
 *
 * @author Paul C. Bryan
 */
public class JsonReferenceTransformer implements JsonTransformer {

    /** The URI of the referring document or directory where it is located. */
    protected URI base;

    /** The root value of the referring document. */
    protected JsonValue root;

    /**
     * Constructs a new JSON reference transformer.
     * <p>
     * If {@code base} is {@code null} then only absolute URI references or fragment-only
     * references can be resolved; an attempt to resolve a relative reference URI will throw
     * a {@link JsonReferenceException}.
     * <p>
     * The purpose of {@code root} is to allow fragment identifier references within the same
     * document without the need to resolve it via an absolute URI. If {@code root} is null,
     * then fragment identifier references are constructed from {@code base}, if specified.
     *
     * @param base the base URI of the referring document.
     * @param root the root value of the referring document.
     * @throws JsonReferenceException if {@code base} is not {@code null} and is not an absolute URI.
     */
    public JsonReferenceTransformer(URI base, JsonValue root) throws JsonReferenceException {
        if (base != null) {
            if (base.isAbsolute()) {
                base = base.normalize(); // do some cleanup
            } else {
                throw new JsonReferenceException("Base URI must be absolute");
            }
        }
        this.base = base;
        this.root = root;
    }

    /**
     * Returns {@code true} if the {@code $ref} URI contains a standalone fragment identifier.
     * For example: {@code "#/foo/bar"}.
     */
    private static boolean isStandaloneFragment(URI uri) {
        return (uri != null && uri.getScheme() == null
         && (uri.getSchemeSpecificPart() == null || uri.getSchemeSpecificPart().length() == 0)
         && uri.getRawFragment() != null);
    }

    /**
     * Resolves any fragment identifier in a URL against the specified root. If no
     * fragment is specified, then the root value is returned.
     */
    private static JsonValue resolveFragment(JsonValue root, URI uri) {
        JsonValue result = root;
        String fragment = uri.getRawFragment();
        if (fragment != null && fragment.length() > 0) {
            result = root.get(new JsonPointer(fragment));
        }
        return result;
    }

    /**
     * Returns {@code true} if the specified URI can be resolved by this transformer.
     * <p>
     * The default implementation returns {@code false}. This method is intended to be
     * overridden by subclasses that can resolve particular types of URIs.
     *
     * @param uri the URI to test to determine if it can be resolved.
     * @return {@code true} if the specified URI can be resolved by this transformer.
     */
    protected boolean isResolvable(URI uri) {
        return false;
    }

    /**
     * Resolves the specified URI into a JSON value. The URI is guaranteed to not contain
     * a fragment identifier.
     * <p>
     * The default implementation throws a {@code JsonException}. This method is intended to
     * be overridden by subclasses that can resolve particular types of URIs.
     *
     * @param uri the URI to resolve.
     * @return the JSON value that the URI resolves to.
     * @throws JsonException if the URI could not be resolved.
     */
    protected JsonValue resolve(URI uri) throws JsonException {
        throw new JsonException("URI cannot be resolved");
    }

    @Override
    public void transform(JsonValue value) throws JsonException {
        if (JsonReference.isJsonReference(value)) {
            URI ref = new JsonReference().fromJsonValue(value).getURI();
            URI rel = null;
            if (base != null) { // normalize reference URI against base (if defined)
                ref = base.resolve(ref); // resolve against base if relative
                rel = base.relativize(ref); // try to make relative if possible
                if (rel.isAbsolute()) {
                    rel = null; // ain't relative
                }
            } else if (!ref.isAbsolute()) {
                  rel = ref; // allows use of standalone fragment without base
            }

            if (root != null && isStandaloneFragment(rel)) { // resolve against already-loaded document
                value.setObject(resolveFragment(root, rel).getObject());
            } else if (isResolvable(ref)) { // supported absolute URI of resource to load
                URI abs = ref;
                if (abs.getFragment() != null) {
                    try {
                        abs = new URI(abs.getScheme(), abs.getSchemeSpecificPart(), null);
                    } catch (URISyntaxException use) {
                        throw new JsonException(use);
                    }
                }
                JsonValue result = resolveFragment(resolve(abs), ref);
                value.setObject(result.getObject());
                value.getTransformers().addAll(0, result.getTransformers()); // resolves relative $refs
            } else if (rel != null) {
                throw new JsonException("No base to resolve relative URI reference");
            }
        }
    }
}
