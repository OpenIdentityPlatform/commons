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

import java.util.Objects;

import org.assertj.core.api.AbstractAssert;
import org.forgerock.json.resource.PatchOperation;

/**
 * Assertion methods for {@link PatchOperation}s.
 *
 * <p>To create a new instance of this class, invoke
 * <code>{@link org.forgerock.json.resource.http.Assertions#assertThat(PatchOperation) assertThat}(actual)</code>.
 */
@SuppressWarnings("javadoc")
public class PatchOperationAssert extends AbstractAssert<PatchOperationAssert, PatchOperation> {

    public PatchOperationAssert(final PatchOperation actual) {
        super(actual, PatchOperationAssert.class);
    }

    public PatchOperationAssert isEqualTo(final PatchOperation expected) {
        isNotNull();
        assertThat(actual.getOperation()).isEqualTo(expected.getOperation());
        assertThat(actual.getField()).isEqualTo(expected.getField());
        assertThat(actual.getFrom()).isEqualTo(expected.getFrom());
        assertThat(Objects.toString(actual.getValue()))
                .isEqualTo(Objects.toString(expected.getValue()));
        return myself;
    }
}
