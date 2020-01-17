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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

public class SizeBasedRotationPolicyTest {

    public static final long MAX_FILE_SIZE_IN_BYTES = 50L;
    public static final long MAX_FILE_SIZE_DISABLED = 0L;

    @Test
    public void testRotateFileThatIsTooLarge() {
        // given
        final SizeBasedRotationPolicy rotationPolicy = new SizeBasedRotationPolicy(MAX_FILE_SIZE_IN_BYTES);
        final RotatableObject rotatableObject = mock(RotatableObject.class);
        when(rotatableObject.getBytesWritten()).thenReturn(MAX_FILE_SIZE_IN_BYTES + 1L);

        // when
        final boolean rotate = rotationPolicy.shouldRotateFile(rotatableObject);

        // then
        assertThat(rotate).isTrue();
    }

    @Test
    public void testRotateFileThatIsLessThanMaxSize() {
        // given
        final SizeBasedRotationPolicy rotationPolicy = new SizeBasedRotationPolicy(MAX_FILE_SIZE_IN_BYTES);
        final RotatableObject rotatableObject = mock(RotatableObject.class);
        when(rotatableObject.getBytesWritten()).thenReturn(MAX_FILE_SIZE_IN_BYTES - 1L);

        // when
        final boolean rotate = rotationPolicy.shouldRotateFile(rotatableObject);

        // then
        assertThat(rotate).isFalse();
    }

    @Test
    public void testRotateFileWhenMaxSizeIsDisabled() {
        // given
        final SizeBasedRotationPolicy rotationPolicy = new SizeBasedRotationPolicy(MAX_FILE_SIZE_DISABLED);
        final RotatableObject rotatableObject = mock(RotatableObject.class);
        when(rotatableObject.getBytesWritten()).thenReturn(MAX_FILE_SIZE_IN_BYTES + 1L);

        // when
        final boolean rotate = rotationPolicy.shouldRotateFile(rotatableObject);

        // then
        assertThat(rotate).isFalse();
    }
}
