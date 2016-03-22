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

package org.forgerock.util.time;

/**
 * Provides time related methods for computing durations, for providing the now
 * instant and other use cases.
 * <p>
 * Why using a service interface instead of JVM provided time methods?
 * <p>
 * Simply because you gain better control over the time understood by the
 * application. For example, if you would have to code an expiration time logic,
 * you would check periodically if the computed expiration timestamp is greater
 * than the now timestamp. So far, so good.
 * <p>
 * When it comes to testing, things gets worst: you typically have to use
 * {@link Thread#sleep(long)} to wait for a given amount of time so that your
 * expiration date is reached. That makes tests much longer to execute and
 * somehow brittle when you're testing short timeouts.
 * <p>
 * Using a {@link TimeService} helps you to keep your code readable and provides
 * a way to better control how the time is flowing for your application
 * (especially useful in the tests).
 * <p>
 * For example, {@link #now()} is used in place of
 * {@link System#currentTimeMillis()}. in your code and you can easily mock it
 * and make it return controlled values. Here is an example with <a
 * href="https://code.google.com/p/mockito/">Mockito</a>:
 *
 * <pre>
 * &#064;Mock
 * private TimeService time;
 *
 * &#064;BeforeMethod
 * public void setUp() throws Exception {
 *     MockitoAnnotations.initMocks(this);
 * }
 *
 * &#064;Test
 * public shouldAdvanceInTime() throws Exception {
 *     // Mimics steps in the future at each call
 *     when(time.now()).thenReturn(0L, 1000L, 10000L);
 *
 *     assertThat(time.now()).isEqualTo(0L);
 *     assertThat(time.now()).isEqualTo(1000L);
 *     assertThat(time.now()).isEqualTo(10000L);
 * }
 * </pre>
 *
 * TimeService provides a {@linkplain #SYSTEM default service implementation}
 * using the System provided time methods for ease of use.
 *
 * @see System#currentTimeMillis()
 * @since 1.3.4
 */
public interface TimeService {

    /**
     * {@link TimeService} implementation based on {@link System}.
     *
     * @see System#currentTimeMillis()
     * @since 1.3.4
     */
    TimeService SYSTEM = new TimeService() {
        @Override
        public long now() {
            return System.currentTimeMillis();
        }

        @Override
        public long since(final long past) {
            return now() - past;
        }
    };

    /**
     * Returns a value that represents "now" since the epoch.
     *
     * @return a value that represents "now" since the epoch.
     * @since 1.3.4
     */
    long now();

    /**
     * Computes the elapsed time between {@linkplain #now() now} and {@code past}.
     *
     * @param past
     *         time value to compare to now.
     * @return the elapsed time
     * @since 1.3.4
     */
    long since(long past);

}
