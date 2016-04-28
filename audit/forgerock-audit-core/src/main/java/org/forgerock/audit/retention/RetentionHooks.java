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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.retention;

import java.io.IOException;

/**
 * Hook into the retention checking operations for a file.
 */
public interface RetentionHooks {

    /**
     * Method to run an action just before the file is run through the {@link RetentionPolicy}'s.
     * @throws IOException If the pre action fails.
     */
    void postRetentionAction() throws IOException;

    /**
     * {@link RetentionHooks} that do nothing.
     */
    class NullRetentionHooks implements RetentionHooks {

        @Override
        public void postRetentionAction() throws IOException {
            // do nothing
        }
    }
}
