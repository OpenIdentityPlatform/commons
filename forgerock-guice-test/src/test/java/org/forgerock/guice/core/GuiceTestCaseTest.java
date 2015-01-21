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

package org.forgerock.guice.core;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.testng.annotations.Test;

import com.google.inject.Binder;
import com.google.inject.Module;

@GuiceModules({GuiceTestCaseTest.ExtraModule.class})
public class GuiceTestCaseTest extends GuiceTestCase {

    @Override
    public void configure(Binder binder) {
        binder.bind(ArrayList.class).to(BoundOne.class);
    }

    @Test
    public void testTestCase() throws Exception {
        assertThat(InjectorHolder.getInstance(ArrayList.class)).isInstanceOf(BoundOne.class);
        assertThat(InjectorHolder.getInstance(HashMap.class)).isInstanceOf(BoundTwo.class);
    }

    public static class BoundOne extends ArrayList {}

    public static class BoundTwo extends HashMap {}

    public static class ExtraModule implements Module {

        public void configure(Binder binder) {
            binder.bind(HashMap.class).to(BoundTwo.class);
        }

    }

}