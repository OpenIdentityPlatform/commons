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

package org.forgerock.caf.authentication.api;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * <p>Maintains state information and provides to retrieve values in a type safe manner.</p>
 *
 * State values are represented with standard Java objects: {@link String}, {@link Number},
 * {@link Boolean}, {@link Map}, {@link List}, {@link Set} and {@code null}.
 *
 * @since 2.0.0
 */
public class AuthenticationState { //TODO add all appropriate methods and/or replace with JsonValue or generic version

    private final JsonValue state;

    /**
     * Creates a new {@code AuthenticationState} instance.
     */
    public AuthenticationState() {
        this(json(object()));
    }

    private AuthenticationState(JsonValue state) {
        this.state = state;
    }

    /**
     * <p>Adds the specified value.</p>
     *
     * <p>If adding to a list value, the specified key must be parseable as an unsigned base-10
     * integer and be less than or equal to the list size. Adding a value to a list shifts any
     * existing elements at or above the specified index to the right by one.</p>
     *
     * @param key The {@code Map} key or {@code List} index to add.
     * @param object The Java object to add.
     * @return This {@code AuthenticationState}.
     * @throws AuthenticationStateException If not a {@code Map} or the {@code Map} key already
     * exists.
     */
    public AuthenticationState add(String key, Object object) {
        try {
            state.add(key, object);
            return this;
        } catch (JsonValueException e) {
            throw new AuthenticationStateException(e);
        }
    }

    /**
     * Returns {@code true} if this {@code AuthenticationState} contains the specified item.
     *
     * @param key The {@code Map} key or {@code List} index of the item to seek.
     * @return {@code true} if this {@code AuthenticationState} contains the specified member.
     * @throws NullPointerException If {@code key} is {@code null}.
     */
    public boolean isDefined(String key) {
        return state.isDefined(key);
    }

    /**
     * Returns the specified item value. If no such member value exists, then a
     * {@code AuthenticationState} containing {@code null} is returned.
     *
     * @param key The {@code Map} key or {@code List} index identifying the item to return.
     * @return a {@code AuthenticationState} containing the value or {@code null}.
     */
    public AuthenticationState get(String key) {
        return new AuthenticationState(state.get(key));
    }

    /**
     * Returns the {@code AuthenticationState} as a {@link Boolean} object. If the value is
     * {@code null}, this method returns {@code null}.
     *
     * @return The boolean value.
     * @throws AuthenticationStateException If the value is not a boolean type.
     */
    public Boolean asBoolean() {
        try {
            return state.asBoolean();
        } catch (JsonValueException e) {
            throw new AuthenticationStateException(e);
        }
    }

    /**
     * Returns the {@code AuthenticationState} as an {@link Integer} object. This may involve
     * rounding or truncation. If the value is {@code null}, this method returns {@code null}.
     *
     * @return The integer value.
     * @throws AuthenticationStateException If the value is not a number.
     */
    public Integer asInteger() {
        try {
            return state.asInteger();
        } catch (JsonValueException e) {
            throw new AuthenticationStateException(e);
        }
    }
}
