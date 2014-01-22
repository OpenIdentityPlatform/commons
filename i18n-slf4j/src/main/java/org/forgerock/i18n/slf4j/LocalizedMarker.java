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
 *      Copyright 2014 ForgeRock AS
 */
package org.forgerock.i18n.slf4j;

import java.util.Collections;
import java.util.Iterator;

import org.forgerock.i18n.LocalizableMessage;
import org.slf4j.Marker;

/**
 * An implementation of SLF4J marker that contains a {@code LocalizableMessage}
 * and does not allow to manage references to other markers.
 */
public class LocalizedMarker implements Marker {

    private static final long serialVersionUID = 1L;

    private final LocalizableMessage message;

    /**
     * Create a marker with provided localizable message.
     * <p>
     * Name of the marker is the resource name provided by the message.
     *
     * @param message
     *            Message embedded into this marker.
     */
    public LocalizedMarker(LocalizableMessage message) {
        this.message = message;
    }

    /**
     * Returns the message embedded into this marker.
     *
     * @return the localizable message.
     */
    public LocalizableMessage getMessage() {
        return message;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return message.resourceName();
    }

    /** {@inheritDoc} */
    @Override
    public void add(Marker reference) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Marker reference) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChildren() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasReferences() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<?> iterator() {
        return Collections.emptySet().iterator();
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(Marker other) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(String name) {
        return false;
    }

}
