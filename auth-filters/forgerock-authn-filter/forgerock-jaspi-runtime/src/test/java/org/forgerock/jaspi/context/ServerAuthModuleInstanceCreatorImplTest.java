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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.context;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class ServerAuthModuleInstanceCreatorImplTest {

    private ServerAuthModuleInstanceCreator moduleInstanceCreator;

    @BeforeMethod
    public void setUp() {
        moduleInstanceCreator = ServerAuthModuleInstanceCreatorImpl.INSTANCE;
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionIfClassNameNull() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        //When
        moduleInstanceCreator.construct(null, new HashMap<String, Object>(),
                messagePolicy, handler);

        //Then
        fail();
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionIfClassNameEmpty() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        //When
        moduleInstanceCreator.construct("", new HashMap<String, Object>(),
                messagePolicy, handler);

        //Then
        fail();
    }

    @Test
    public void shouldConstructServerAuthModule() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        //When
        ServerAuthModule serverAuthModule = moduleInstanceCreator.construct(TestServerAuthModuleOne.class.getName(),
                new HashMap<String, Object>(), messagePolicy, handler);

        //Then
        assertNotNull(serverAuthModule);
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionIfClassNotFound() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        //When
        moduleInstanceCreator.construct("INVALID_CLASS_NAME",
                new HashMap<String, Object>(), messagePolicy, handler);

        //Then
        fail();
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionIfIllegalAccessExceptionThrown() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        //When
        moduleInstanceCreator.construct(TestServerAuthModuleTwo.class.getName(),
                new HashMap<String, Object>(), messagePolicy, handler);

        //Then
        fail();
    }

    @Test (expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionIfInstantiationExceptionThrown() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);


        //When
        moduleInstanceCreator.construct(TestServerAuthModuleThree.class.getName(),
                new HashMap<String, Object>(), messagePolicy, handler);

        //Then
        fail();
    }

    static class TestServerAuthModuleOne implements ServerAuthModule {

        @SuppressWarnings("rawtypes")
        @Override
        public void initialize(MessagePolicy messagePolicy, MessagePolicy messagePolicy2, CallbackHandler handler,
                Map map) throws AuthException {
        }

        @Override
        public Class[] getSupportedMessageTypes() {
            return null;
        }

        @Override
        public AuthStatus validateRequest(MessageInfo messageInfo, Subject subject, Subject subject2)
                throws AuthException {
            return null;
        }

        @Override
        public AuthStatus secureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
            return null;
        }

        @Override
        public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        }
    }

    static class TestServerAuthModuleTwo extends TestServerAuthModuleOne {
        TestServerAuthModuleTwo() throws IllegalAccessException {
            throw new IllegalAccessException();
        }
    }

    static class TestServerAuthModuleThree extends TestServerAuthModuleOne {
        TestServerAuthModuleThree() throws InstantiationException {
            throw new InstantiationException();
        }
    }
}
