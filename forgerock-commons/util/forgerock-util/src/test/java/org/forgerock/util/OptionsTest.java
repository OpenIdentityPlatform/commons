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
package org.forgerock.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Tests the {@link Option} and {@link Options} classes.
 */
@SuppressWarnings("javadoc")
public class OptionsTest {

    @Test
    public void getDefaultValue() {
        Option<Boolean> testOption1 = Option.withDefault(true);
        Option<Boolean> testOption2 = Option.withDefault(false);
        Options options = Options.defaultOptions();
        assertThat(options.get(testOption1)).isTrue();
        assertThat(options.get(testOption2)).isFalse();
    }

    @Test
    public void getConfiguredValue() {
        Option<Boolean> testOption = Option.withDefault(false);
        Options options = Options.defaultOptions();
        assertThat(options.get(testOption)).isFalse();
        options.set(testOption, true);
        assertThat(options.get(testOption)).isTrue();
    }

    @Test
    public void resetBackToDefaultValue() {
        Option<Boolean> testOption = Option.withDefault(false);
        Options options = Options.defaultOptions();
        options.set(testOption, true);
        assertThat(options.get(testOption)).isTrue();
        options.reset(testOption);
        assertThat(options.get(testOption)).isFalse();
    }

    @Test
    public void copyOf() {
        Option<Boolean> testOption = Option.withDefault(false);
        Options options1 = Options.defaultOptions();
        options1.set(testOption, true);
        Options options2 = Options.copyOf(options1);

        assertThat(options1).isNotSameAs(options2);
        assertThat(options1.get(testOption)).isTrue();
        assertThat(options2.get(testOption)).isTrue();
        options1.set(testOption, false);
        assertThat(options1.get(testOption)).isFalse();
        assertThat(options2.get(testOption)).isTrue(); // was copied
        options2.set(testOption, false);
        assertThat(options2.get(testOption)).isFalse();
    }

    @Test
    public void unmodifiableGetters() {
        Option<Boolean> testOption = Option.withDefault(false);
        Options options1 = Options.defaultOptions();
        options1.set(testOption, true);
        Options options2 = Options.unmodifiableCopyOf(options1);

        assertThat(options1).isNotSameAs(options2);
        assertThat(options1.get(testOption)).isTrue();
        assertThat(options2.get(testOption)).isTrue();
        options1.set(testOption, false);
        assertThat(options1.get(testOption)).isFalse();
        assertThat(options2.get(testOption)).isTrue(); // was copied
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void unmodifiableSettersThrowUnsupportedOperationException() {
        Option<Boolean> testOption = Option.withDefault(false);
        Options options1 = Options.defaultOptions();
        options1.set(testOption, true);
        Options options2 = Options.unmodifiableCopyOf(options1);
        options2.set(testOption, false);
    }
}
