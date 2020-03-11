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
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * An HTTP message header.
 */
public abstract class Header {

    /**
     * Returns the name of the header, as it would canonically appear within an
     * HTTP message.
     *
     * @return The name of the header, as it would canonically appear within an
     *         HTTP message.
     */
    public abstract String getName();

    /**
     * Returns the header as a list of strings. Each {@code String} should
     * represent the value component of the key-value pair that makes up the
     * HTTP header - as such, for some {@code Header} implementations each
     * String in this {@code List} may contain multiple token-separated values.
     * <p>
     * The {@code List} returned from this method should not be expected to be
     * mutable. However, some subclasses of {@code Header} may choose to
     * implement it as such.
     *
     * @return The header as a list of string values.
     */
    public abstract List<String> getValues();

    /**
     * Gets the first value of this header instance. As with {@link #getValues},
     * the returned {@code String} may contain multiple token-separated values.
     *
     * @return The first value, or null if none exist.
     */
    public String getFirstValue() {
        List<String> values = getValues();
        return values == null || values.size() == 0 ? null : values.get(0);
    }

    @Override
    public String toString() {
        List<String> strings = new ArrayList<>();
        for (String value : getValues()) {
            strings.add(getName() + ": " + value);
        }
        return strings.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Header)) {
            return false;
        }

        Header that = (Header) o;

        return getName().equals(that.getName()) && getValues().equals(that.getValues());
    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getValues().hashCode();
        return result;
    }

}
