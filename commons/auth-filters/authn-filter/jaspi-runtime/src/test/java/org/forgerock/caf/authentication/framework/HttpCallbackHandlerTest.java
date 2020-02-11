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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.CertStoreCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.callback.PrivateKeyCallback;
import javax.security.auth.message.callback.SecretKeyCallback;
import javax.security.auth.message.callback.TrustStoreCallback;
import java.security.Principal;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpCallbackHandlerTest {

    private HttpCallbackHandler callbackHandler;

    @BeforeMethod
    public void setUp() {
        callbackHandler = new HttpCallbackHandler();
    }

    @Test
    public void shouldHandleCallerPrincipalCallbackWithCallbackName() throws UnsupportedCallbackException {

        //Given
        CallerPrincipalCallback callbackOne = mock(CallerPrincipalCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};
        Subject subject = new Subject();
        Principal principal = null;

        given(callbackOne.getSubject()).willReturn(subject);
        given(callbackOne.getName()).willReturn("PRN_NAME");
        given(callbackOne.getPrincipal()).willReturn(principal);

        //When
        callbackHandler.handle(callbacks);

        //Then
        assertEquals(subject.getPrincipals().size(), 1);
        assertEquals(subject.getPrincipals().toArray(new Principal[0])[0].getName(), "PRN_NAME");
        assertNotEquals(subject.getPrincipals().toArray(new Principal[0])[0], principal);
    }

    @Test
    public void shouldHandleCallerPrincipalCallbackWithCallbackNameButNonNullPrincipal()
            throws UnsupportedCallbackException {

        //Given
        CallerPrincipalCallback callbackOne = mock(CallerPrincipalCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};
        Subject subject = new Subject();
        Principal principal = mock(Principal.class);

        given(callbackOne.getSubject()).willReturn(subject);
        given(callbackOne.getName()).willReturn("PRN_NAME");
        given(callbackOne.getPrincipal()).willReturn(principal);

        //When
        callbackHandler.handle(callbacks);

        //Then
        assertEquals(subject.getPrincipals().size(), 1);
        assertNull(subject.getPrincipals().toArray(new Principal[0])[0].getName());
        assertEquals(subject.getPrincipals().toArray(new Principal[0])[0], principal);
    }

    @Test
    public void shouldHandleCallerPrincipalCallbackWithCallbackPrincipal() throws UnsupportedCallbackException {

        //Given
        CallerPrincipalCallback callbackOne = mock(CallerPrincipalCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};
        Subject subject = new Subject();
        Principal principal = mock(Principal.class);

        given(callbackOne.getSubject()).willReturn(subject);
        given(callbackOne.getName()).willReturn(null);
        given(callbackOne.getPrincipal()).willReturn(principal);

        //When
        callbackHandler.handle(callbacks);

        //Then
        assertEquals(subject.getPrincipals().size(), 1);
        assertEquals(subject.getPrincipals().toArray(new Principal[0])[0], principal);
    }

    @Test
    public void shouldHandleCallerPrincipalCallbackWithNoCallbackNameOrPrincipal() throws UnsupportedCallbackException {

        //Given
        CallerPrincipalCallback callbackOne = mock(CallerPrincipalCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};
        Subject subject = new Subject();

        given(callbackOne.getSubject()).willReturn(subject);
        given(callbackOne.getName()).willReturn(null);
        given(callbackOne.getPrincipal()).willReturn(null);

        //When
        callbackHandler.handle(callbacks);

        //Then
        assertEquals(subject.getPrincipals().size(), 0);
    }

    @Test
    public void shouldHandleGroupPrincipalCallbackWithNoGroups() throws UnsupportedCallbackException {

        //Given
        GroupPrincipalCallback callbackOne = mock(GroupPrincipalCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};
        Subject subject = new Subject();

        given(callbackOne.getSubject()).willReturn(subject);
        given(callbackOne.getGroups()).willReturn(null);

        //When
        callbackHandler.handle(callbacks);

        //Then
        assertEquals(subject.getPrincipals().size(), 0);
    }

    @Test
    public void shouldHandleGroupPrincipalCallback() throws UnsupportedCallbackException {

        //Given
        GroupPrincipalCallback callbackOne = mock(GroupPrincipalCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};
        Subject subject = new Subject();
        String groupOne = "GROUP_ONE";
        String groupTwo = "GROUP_TWO";
        String[] groups = new String[]{groupOne, groupTwo};

        given(callbackOne.getSubject()).willReturn(subject);
        given(callbackOne.getGroups()).willReturn(groups);

        //When
        callbackHandler.handle(callbacks);

        //Then
        assertEquals(subject.getPrincipals().size(), 2);
        assertEquals(subject.getPrincipals().toArray(new Principal[0])[0].getName(), "GROUP_ONE");
        assertEquals(subject.getPrincipals().toArray(new Principal[0])[1].getName(), "GROUP_TWO");
    }

    @Test (expectedExceptions = UnsupportedCallbackException.class)
    public void shouldHandlePasswordValidationCallback() throws UnsupportedCallbackException {

        //Given
        PasswordValidationCallback callbackOne = mock(PasswordValidationCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};

        //When
        callbackHandler.handle(callbacks);

        //Then
        fail();
    }

    @Test (expectedExceptions = UnsupportedCallbackException.class)
    public void shouldHandleCertStoreCallback() throws UnsupportedCallbackException {

        //Given
        CertStoreCallback callbackOne = mock(CertStoreCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};

        //When
        callbackHandler.handle(callbacks);

        //Then
        fail();
    }

    @Test (expectedExceptions = UnsupportedCallbackException.class)
    public void shouldHandlePrivateKeyCallback() throws UnsupportedCallbackException {

        //Given
        PrivateKeyCallback callbackOne = mock(PrivateKeyCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};

        //When
        callbackHandler.handle(callbacks);

        //Then
        fail();
    }

    @Test (expectedExceptions = UnsupportedCallbackException.class)
    public void shouldHandleSecretKeyCallback() throws UnsupportedCallbackException {

        //Given
        SecretKeyCallback callbackOne = mock(SecretKeyCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};

        //When
        callbackHandler.handle(callbacks);

        //Then
        fail();
    }

    @Test (expectedExceptions = UnsupportedCallbackException.class)
    public void shouldHandleTrustStoreCallback() throws UnsupportedCallbackException {

        //Given
        TrustStoreCallback callbackOne = mock(TrustStoreCallback.class);
        Callback[] callbacks = new Callback[]{callbackOne};

        //When
        callbackHandler.handle(callbacks);

        //Then
        fail();
    }
}
