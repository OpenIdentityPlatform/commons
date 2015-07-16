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

package org.forgerock.selfservice.core.snapshot;

import java.util.Map;

/**
 * Responsible for the validation, generation and parsing of snapshot tokens used for keying a snapshot of some state.
 *
 * @since 0.1.0
 */
public interface SnapshotTokenHandler {

    /**
     * Validates the passed snapshot token string.
     *
     * @param snapshotToken
     *         the token string
     *
     * @return whether the snapshotToken is valid
     */
    boolean validate(String snapshotToken);

    /**
     * Generates a new snapshot token using the state.
     *
     * @param state
     *         the state
     *
     * @return snapshot token
     */
    String generate(Map<String, String> state);

    /**
     * Parses the token, extracting any encapsulated state.
     *
     * @param snapshotToken
     *         the token string
     *
     * @return any encapsulated state
     */
    Map<String, String> parse(String snapshotToken);

}
