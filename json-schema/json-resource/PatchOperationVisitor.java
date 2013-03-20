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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * A visitor of {@code PatchOperation}s, in the style of the visitor design
 * pattern.
 * <p>
 * Classes implementing this interface can patch operations in a type-safe
 * manner. When a visitor is passed to a filter's accept method, the
 * corresponding visit method most applicable to that patch operation is
 * invoked.
 *
 * @param <R>
 *            The return type of this visitor's methods. Use
 *            {@link java.lang.Void} for visitors that do not need to return
 *            results.
 * @param <P>
 *            The type of the additional parameter to this visitor's methods.
 *            Use {@link java.lang.Void} for visitors that do not need an
 *            additional parameter.
 */
public interface PatchOperationVisitor<R, P> {

    /**
     * Visits an {@code add} patch operation.
     *
     * @param p
     *            A visitor specified parameter.
     * @return Returns a visitor specified result.
     */
    R visitAddOperation(P p, JsonPointer field, JsonValue value);

    /**
     * Visits a {@code remove} patch operation.
     *
     * @param p
     *            A visitor specified parameter.
     * @return Returns a visitor specified result.
     */
    R visitRemoveOperation(P p, JsonPointer field);

    /**
     * Visits a {@code replace} patch operation.
     *
     * @param p
     *            A visitor specified parameter.
     * @return Returns a visitor specified result.
     */
    R visitReplaceOperation(P p, JsonPointer field, JsonValue value);

    /**
     * Visits an {@code unrecognized} patch operation. An unrecognized filter is
     * one which does not have the type "add", "remove", or "replace".
     *
     * @param p
     *            A visitor specified parameter.
     * @return Returns a visitor specified result.
     */
    R visitUnrecognizedOperation(P p, String type, JsonPointer field, JsonValue value);

}
