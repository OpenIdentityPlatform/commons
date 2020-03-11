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

package org.forgerock.json.resource.http.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.MapAssert;
import org.forgerock.json.resource.Request;

/**
 * Abstract assertion methods for {@link Request}s.
 */
@SuppressWarnings("javadoc")
public abstract class RequestAssert<A extends RequestAssert<A, T>, T extends Request> extends AbstractAssert<A, T> {

    protected RequestAssert(final T actual, final Class<A> selfType) {
        super(actual, selfType);
    }

    public A isEqualTo(final T expected) {
        isNotNull();
        assertThat(actual.getResourceVersion()).isEqualTo(expected.getResourceVersion());
        MapAssert<String, String> mapAssert = assertThat(actual.getAdditionalParameters());
        mapAssert.hasSize(actual.getAdditionalParameters().size());
        for (Map.Entry<String, String> entry : actual.getAdditionalParameters().entrySet()) {
            mapAssert.containsEntry(entry.getKey(), entry.getValue());
        }
        assertThat(actual.getRequestType()).isEqualTo(expected.getRequestType());
        assertThat(actual.getResourcePath()).isEqualTo(expected.getResourcePath());
        assertThat(actual.getFields()).containsAll(expected.getFields());
        return myself;
    }
}
