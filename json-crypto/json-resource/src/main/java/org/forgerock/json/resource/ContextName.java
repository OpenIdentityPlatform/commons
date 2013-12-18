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
 * Copyright © 2013 ForgeRock AS. All rights reserved.
 */
package org.forgerock.json.resource;

import org.forgerock.util.Reject;

/**
 * A String-wrapper for a friendly name for a {@link Context}.
 */
public class ContextName {

    /** the name */
    private final String name;

    /**
     * Create a ContextName from the given String.
     *
     * @param name
     *            the context name
     * @return
     *            a ContextName
     */
    public static ContextName valueOf(final String name) {
        return new ContextName(name);
    }

    /**
     * Construct a ContextName from a String.
     *
     * @param name the context name
     */
    private ContextName(final String name) {
        Reject.ifNull(name, "Cannot create a null ContextName");
        this.name = name;
    }

    /**
     * Return the String representation of this object.
     *
     * @return the context name as a String.
     */
    public String toString() {
        return name;
    }

    /**
     * Tests whether the provided object is equal to this object.
     *
     * @param o an object to compare
     * @return true if the object is equal to this one, false otherwise.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof ContextName)) {
            return false;
        }

        return o.toString().equals(this.toString());
    }

    /**
     * Return this object's hash code.
     *
     * @return the hashcode of this object.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
