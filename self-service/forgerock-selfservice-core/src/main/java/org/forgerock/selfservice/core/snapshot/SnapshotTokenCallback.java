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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;

/**
 * Callback is invoked when a new snapshot token is created
 * just before requirements are returned to the client.
 *
 * @since 0.1.0
 */
public interface SnapshotTokenCallback {

    /**
     * Preview of the snapshot token just prior to requirements being sent to the client.
     *
     * @param context
     *         the process context
     * @param snapshotToken
     *         the snapshot token
     *
     * @throws ResourceException
     *         should some error occur during the token preview
     */
    void snapshotTokenPreview(ProcessContext context, String snapshotToken) throws ResourceException;

}
