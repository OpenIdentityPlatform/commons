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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CloseSilentlyFunctionTest {

    @DataProvider
    public Object[][] data() {
        return new Object[][] {
                // test normal operation, where close is only called once
                { false, false },
                // test silent handling of exceptions, because second-close will cause exception
                { true, false },
                // test that close is called after exception is thrown
                { false, true },
                // test proper handling of exception thrown from delegate and another thrown from CloseSilentlyFunction
                { true, true }
        };
    }

    @Test(dataProvider = "data")
    public void test(final boolean closePrematurely, final boolean throwException) throws Exception {
        // given
        final Function<OnceClosable, Boolean, Exception> closeSilentlyFunction =
                CloseSilentlyFunction.closeSilently(
                    new Function<OnceClosable, Boolean, Exception>() {
                        @Override
                        public Boolean apply(final OnceClosable closeable) throws Exception {
                            if (closePrematurely) {
                                closeable.close();
                            }
                            if (throwException) {
                                throw new DelegateFunctionException("Exception from delegate function");
                            }
                            return true;
                        }
                    });

        final OnceClosable onceClosable = new OnceClosable();
        assertThat(onceClosable.closed).isFalse();

        try {
            // when
            final Boolean output = closeSilentlyFunction.apply(onceClosable);

            // then
            assertThat(output).isTrue();
        } catch (DelegateFunctionException e) {
            assertThat(throwException).isTrue();
        }
        assertThat(onceClosable.closed).isTrue();
    }

    /**
     * Class that can only be closed once.
     */
    private static class OnceClosable implements Closeable {

        boolean closed;

        @Override
        public void close() throws IOException {
            if (closed) {
                throw new IOException("Already closed");
            }
            closed = true;
        }
    }

    /**
     * Exception thrown by a delegate function.
     */
    private static class DelegateFunctionException extends Exception {
        DelegateFunctionException(final String message) {
            super(message);
        }
    }
}
