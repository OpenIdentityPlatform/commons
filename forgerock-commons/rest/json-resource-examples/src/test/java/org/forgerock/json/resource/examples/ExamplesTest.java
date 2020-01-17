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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.json.resource.examples;

import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public final class ExamplesTest {
    @BeforeClass
    public void setup() {
        DemoUtils.isUnitTest = true;
    }

    @Test
    public void testAsyncReadModifyWrite() throws ResourceException {
        AsyncReadModifyWriteDemo.main(new String[0]);
    }

    @Test
    public void testReadModifyWrite() throws ResourceException {
        ReadModifyWriteDemo.main(new String[0]);
    }

    @Test
    public void testDynamicRealmDemo() throws ResourceException {
        DynamicRealmDemo.main(new String[0]);
    }

    @Test
    public void testSimpleRealmDemo() throws ResourceException {
        SimpleRealmDemo.main(new String[0]);
    }
}
