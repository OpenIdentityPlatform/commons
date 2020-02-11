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

package org.forgerock.selfservice.core;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;

/**
 * Responsible for authoring new tokens used to key context snapshots.
 *
 * @since 0.1.0
 */
interface SnapshotAuthor {

    /**
     * Captures a snapshot of the state held within the passed map.
     *
     * @param state
     *         json state
     *
     * @return a snapshot token used as a reference back to the state
     *
     * @throws ResourceException
     *         should some issue occur creating a snapshot
     */
    String captureSnapshotOf(JsonValue state) throws ResourceException;

    /**
     * Retrieves a previous snapshot of some state referenced by the token.
     *
     * @param snapshotToken
     *         the snapshot token
     *
     * @return json state
     *
     * @throws ResourceException
     *         should some issue occur retrieving a snapshot
     */
    JsonValue retrieveSnapshotFrom(String snapshotToken) throws ResourceException;

}
