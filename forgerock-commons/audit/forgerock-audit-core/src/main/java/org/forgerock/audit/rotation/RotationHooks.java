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
package org.forgerock.audit.rotation;

import java.io.IOException;

/**
 * Callback hooks to allow custom action to be taken before and after file rotation occurs.
 */
public interface RotationHooks {
    /**
     * Method to run an action just after file has been rotated.
     * @param context The rotation context.
     * @throws IOException If the post action fails.
     */
    void postRotationAction(RotationContext context) throws IOException;

    /**
     * Method to run an action just before file will be rotated.
     * @param context The rotation context.
     * @throws IOException If the pre action fails.
     */
    void preRotationAction(RotationContext context) throws IOException;

    /**
     * {@link RotationHooks} that do nothing.
     */
    class NoOpRotatationHooks implements RotationHooks {

        @Override
        public void postRotationAction(RotationContext context) throws IOException {
            // do nothing
        }

        @Override
        public void preRotationAction(RotationContext context) throws IOException {
            // do nothing
        }
    }
}
