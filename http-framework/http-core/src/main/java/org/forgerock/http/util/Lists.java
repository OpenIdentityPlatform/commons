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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.util;

import java.util.List;

/**
 * Provides helper methods for {@link List}.
 */
public final class Lists {

    private Lists() { }

    /**
     * Returns the given list content as an array, or {@code null} if the list is empty.
     *
     * @param listOfStrings
     *         list to be converted
     * @return the given list content as an array, or {@code null} if the list is empty.
     */
    public static String[] asArrayOrNull(final List<String> listOfStrings) {
        if (listOfStrings.isEmpty()) {
            return null;
        }
        return listOfStrings.toArray(new String[listOfStrings.size()]);
    }

}
