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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.jaspi.modules.iwa.wdsso;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KeyTab;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import org.forgerock.http.protocol.Request;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RootContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.testng.annotations.Test;

public class WDSSOTest {

    private static final String SERVICE_PRINCIPAL = "HTTP/service.example.com@EXAMPLE.COM";
    private static final Map<String, String> DEFAULT_OPTIONS =
            Map.of("servicePrincipal", SERVICE_PRINCIPAL, "keytabFileName", "service.keytab");

    // Kerberos V5 GSS header. The test supplies the negotiation result through the GSS context.
    private static final byte[] GSS_HEADER = {
        0x60, 0x0b, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
        (byte) 0x86, (byte) 0xf7, 0x12, 0x01, 0x02, 0x02
    };

    // SPNEGO NegTokenInit offering Kerberos V5 without the optional mechToken.
    private static final byte[] SPNEGO_WITHOUT_MECH_TOKEN = {
        0x60, 0x1b, 0x06, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02,
        (byte) 0xa0, 0x11, 0x30, 0x0f,
        (byte) 0xa0, 0x0d, 0x30, 0x0b, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
        (byte) 0x86, (byte) 0xf7, 0x12, 0x01, 0x02, 0x02
    };

    // SPNEGO NegTokenInit with the Kerberos V5 GSS header inside mechToken.
    private static final byte[] SPNEGO_WITH_KERBEROS_TOKEN = {
        0x60, 0x2c, 0x06, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02,
        (byte) 0xa0, 0x22, 0x30, 0x20,
        (byte) 0xa0, 0x0d, 0x30, 0x0b, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
        (byte) 0x86, (byte) 0xf7, 0x12, 0x01, 0x02, 0x02,
        (byte) 0xa2, 0x0f, 0x04, 0x0d, 0x60, 0x0b, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
        (byte) 0x86, (byte) 0xf7, 0x12, 0x01, 0x02, 0x02
    };

    @Test
    public void loadsAcceptorCredentialsWithoutObtainingATicket() throws Exception {
        Path keytab = Files.createTempFile("wdsso-test", ".keytab");
        try {
            Configuration globalConfig = Configuration.getConfiguration();
            String realm = System.getProperty("java.security.krb5.realm");
            String kdc = System.getProperty("java.security.krb5.kdc");

            // The Kerberos acceptor reads the keys from the keytab when it needs them.
            Subject subject = new WDSSO().serviceLogin(SERVICE_PRINCIPAL, "SRVTAB:" + keytab);
            assertEquals(subject.getPrivateCredentials(KeyTab.class).iterator().next().getPrincipal(),
                    new KerberosPrincipal(SERVICE_PRINCIPAL));
            assertTrue(subject.getPrivateCredentials(KerberosTicket.class).isEmpty());
            assertSame(Configuration.getConfiguration(), globalConfig);
            assertEquals(System.getProperty("java.security.krb5.realm"), realm);
            assertEquals(System.getProperty("java.security.krb5.kdc"), kdc);
        } finally {
            Files.deleteIfExists(keytab);
        }
    }

    @Test
    public void reusesServiceCredentialsForTheSameConfiguration() throws Exception {
        WDSSO wdsso = wdssoWithStubbedServiceLogin();
        doReturn(establishedContext(), establishedContext()).when(wdsso).createGssContext();
        Map<String, String> options = Map.of(
                "servicePrincipal", "HTTP/service.example.com",
                "keytabFileName", "service.keytab",
                "kerberosRealm", "EXAMPLE.COM");

        assertEquals(process(wdsso, options, GSS_HEADER), "refreshed-user");
        assertEquals(process(wdsso, options, GSS_HEADER), "refreshed-user");
        verify(wdsso).serviceLogin(SERVICE_PRINCIPAL, "service.keytab");
    }

    @Test
    public void rejectsAnEarlierIwaFailure() throws Exception {
        AttributesContext context = new AttributesContext(new RootContext());
        context.getAttributes().put("iwa-failed", "true");
        WDSSO wdsso = wdssoWithStubbedServiceLogin();

        expectThrows(IllegalStateException.class, () -> wdsso.process(Map.of(), context, new Request()));
        verify(wdsso, never()).serviceLogin(anyString(), anyString());
    }

    @Test
    public void rejectsMissingTokenBeforeLoadingServiceCredentials() throws Exception {
        WDSSO wdsso = wdssoWithStubbedServiceLogin();
        expectThrows(RuntimeException.class,
                () -> wdsso.process(DEFAULT_OPTIONS, new AttributesContext(new RootContext()), new Request()));
        verify(wdsso, never()).serviceLogin(anyString(), anyString());
    }

    @Test
    public void rejectsSpnegoWithoutMechTokenBeforeLoadingServiceCredentials() throws Exception {
        WDSSO wdsso = wdssoWithStubbedServiceLogin();

        expectThrows(RuntimeException.class, () -> process(wdsso, DEFAULT_OPTIONS, SPNEGO_WITHOUT_MECH_TOKEN));
        verify(wdsso, never()).serviceLogin(anyString(), anyString());
    }

    @Test
    public void passesKerberosMechTokenFromSpnegoToGssContext() throws Exception {
        WDSSO wdsso = wdssoWithStubbedServiceLogin();
        GSSContext context = establishedContext();
        doReturn(context).when(wdsso).createGssContext();

        assertEquals(process(wdsso, DEFAULT_OPTIONS, SPNEGO_WITH_KERBEROS_TOKEN), "refreshed-user");
        verify(context).acceptSecContext(aryEq(GSS_HEADER), eq(0), eq(GSS_HEADER.length));
    }

    @Test
    public void failsConcurrentRequestsWhenTheirSharedCredentialCannotBeRefreshed() throws Exception {
        CyclicBarrier accepting = new CyclicBarrier(2);
        WDSSO wdsso = spy(new WDSSO());
        doReturn(new Subject()).doThrow(new LoginException("Keytab unavailable")).doReturn(new Subject())
                .when(wdsso).serviceLogin(anyString(), anyString());
        doReturn(expiredContext(accepting), expiredContext(accepting), establishedContext())
                .when(wdsso).createGssContext();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> process(wdsso));
            Future<String> second = executor.submit(() -> process(wdsso));
            assertTrue(expectThrows(ExecutionException.class, () -> first.get(10, TimeUnit.SECONDS))
                    .getCause() instanceof LoginException);
            assertTrue(expectThrows(ExecutionException.class, () -> second.get(10, TimeUnit.SECONDS))
                    .getCause() instanceof LoginException);
            verify(wdsso, times(2)).serviceLogin(eq(SERVICE_PRINCIPAL), anyString());
        } finally {
            executor.shutdownNow();
        }

        assertEquals(process(wdsso), "refreshed-user");
    }

    @Test
    public void returnsPrincipalFromRetryAfterCredentialRefresh() throws Exception {
        GSSContext expiredContext = expiredContext(new GSSException(GSSException.CREDENTIALS_EXPIRED));
        GSSContext retryContext = establishedContext();
        WDSSO wdsso = wdssoWithStubbedServiceLogin();
        doReturn(expiredContext, retryContext).when(wdsso).createGssContext();

        assertEquals(process(wdsso), "refreshed-user");
        verify(retryContext).dispose();
        verify(wdsso, times(2)).serviceLogin(eq(SERVICE_PRINCIPAL), anyString());
    }

    @Test
    public void disposesContextAfterIncompleteNegotiation() throws Exception {
        GSSContext context = mock(GSSContext.class);
        when(context.isEstablished()).thenReturn(false);
        WDSSO wdsso = wdssoWithStubbedServiceLogin();
        doReturn(context).when(wdsso).createGssContext();

        expectThrows(RuntimeException.class, () -> process(wdsso));
        verify(context).dispose();
    }

    @Test
    public void retainsCredentialExpiryWhenDisposalAlsoFails() throws Exception {
        GSSException expiry = new GSSException(GSSException.CREDENTIALS_EXPIRED);
        GSSException cleanupFailure = new GSSException(GSSException.FAILURE);
        GSSContext expiredContext = expiredContext(expiry);
        doThrow(cleanupFailure).when(expiredContext).dispose();
        WDSSO wdsso = wdssoWithStubbedServiceLogin();
        doReturn(expiredContext, establishedContext()).when(wdsso).createGssContext();

        assertEquals(process(wdsso), "refreshed-user");
        Throwable[] suppressed = expiry.getSuppressed();
        assertEquals(suppressed.length, 1);
        assertSame(suppressed[0], cleanupFailure);
    }

    private static WDSSO wdssoWithStubbedServiceLogin() throws Exception {
        WDSSO wdsso = spy(new WDSSO());
        doReturn(new Subject(), new Subject()).when(wdsso).serviceLogin(anyString(), anyString());
        return wdsso;
    }

    private static GSSContext expiredContext(GSSException expiry) throws GSSException {
        GSSContext context = mock(GSSContext.class);
        when(context.acceptSecContext(any(byte[].class), eq(0), anyInt())).thenThrow(expiry);
        return context;
    }

    private static GSSContext expiredContext(CyclicBarrier accepting) throws GSSException {
        GSSContext context = mock(GSSContext.class);
        when(context.acceptSecContext(any(byte[].class), eq(0), anyInt())).thenAnswer(invocation -> {
            accepting.await(10, TimeUnit.SECONDS);
            throw new GSSException(GSSException.CREDENTIALS_EXPIRED);
        });
        return context;
    }

    private static GSSContext establishedContext() throws GSSException {
        GSSName name = mock(GSSName.class);
        when(name.toString()).thenReturn("refreshed-user@EXAMPLE.COM");
        GSSContext context = mock(GSSContext.class);
        when(context.isEstablished()).thenReturn(true);
        when(context.getSrcName()).thenReturn(name);
        return context;
    }

    private static String process(WDSSO wdsso) throws Exception {
        return process(wdsso, DEFAULT_OPTIONS, GSS_HEADER);
    }

    private static String process(WDSSO wdsso, Map<String, String> options, byte[] token) throws Exception {
        Request request = new Request();
        request.getHeaders().put("Authorization", "Negotiate " + Base64.getEncoder().encodeToString(token));
        return wdsso.process(options, new AttributesContext(new RootContext()), request);
    }
}
