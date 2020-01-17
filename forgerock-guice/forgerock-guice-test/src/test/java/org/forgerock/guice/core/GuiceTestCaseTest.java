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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import org.testng.annotations.Test;

@GuiceModules({ GuiceTestCaseTest.ExtraModule.class })
public class GuiceTestCaseTest extends GuiceTestCase {

    @Override
    public void configure(Binder binder) {
        binder.bind(Key.get(new TypeLiteral<ArrayList<Object>>() { })).to(BoundOne.class);
        binder.bind(BoundThree.class).to(BoundThreeA.class);
    }

    @Override
    protected void configureOverrideBindings(Binder binder) {
        binder.bind(BoundThree.class).to(BoundThreeB.class);
    }

    @Test
    public void testTestCase() throws Exception {
        assertThat(InjectorHolder.getInstance(Key.get(new TypeLiteral<ArrayList<Object>>() { })))
                .isInstanceOf(BoundOne.class);
        assertThat(InjectorHolder.getInstance(Key.get(new TypeLiteral<HashMap<String, Object>>() { })))
                .isInstanceOf(BoundTwo.class);
        assertThat(InjectorHolder.getInstance(BoundThree.class)).isInstanceOf(BoundThreeB.class);
    }

    public static class BoundOne extends ArrayList<Object> {
        private static final long serialVersionUID = 0L;
    }

    public static class BoundTwo extends HashMap<String, Object> {
        private static final long serialVersionUID = 0L;
    }

    public interface BoundThree {
    }

    public static class BoundThreeA implements BoundThree {
    }

    public static class BoundThreeB implements BoundThree {
    }

    public static class ExtraModule implements Module {
        public void configure(Binder binder) {
            binder.bind(Key.get(new TypeLiteral<HashMap<String, Object>>() { })).to(BoundTwo.class);
        }
    }
}
