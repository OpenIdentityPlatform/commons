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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class ResourceExceptionTest {

    @Test
    public void shouldReturnFalseForIsServerSideErrorWithCodeLessThan500() {

        //Given
        ResourceException resourceException = newResourceException(499);

        //When
        boolean isServerSideError = resourceException.isServerError();

        //Then
        assertFalse(isServerSideError);
    }

    @Test
    public void shouldReturnTrueForIsServerSideErrorWith500Code() {

        //Given
        ResourceException resourceException = newResourceException(500);

        //When
        boolean isServerSideError = resourceException.isServerError();

        //Then
        assertTrue(isServerSideError);
    }

    @Test
    public void shouldReturnTrueForIsServerSideErrorWith599Code() {

        //Given
        ResourceException resourceException = newResourceException(599);

        //When
        boolean isServerSideError = resourceException.isServerError();

        //Then
        assertTrue(isServerSideError);
    }

    @Test
    public void shouldReturnFalseForIsServerSideErrorWithCodeMoreThan599() {

        //Given
        ResourceException resourceException = newResourceException(600);

        //When
        boolean isServerSideError = resourceException.isServerError();

        //Then
        assertFalse(isServerSideError);
    }

    @Test
    public void asPromiseCanSpecifyTypeOfPromise() {
        //Given
        ResourceException e = new InternalServerErrorException();

        //When
        Promise<ResourceResponse, ResourceException> responsePromise = e.asPromise();
        Promise<Integer, ResourceException> integerPromise = e.asPromise();

        //Then
        assertThat(responsePromise).failedWithException().isInstanceOf(InternalServerErrorException.class);
        assertThat(integerPromise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }
}
