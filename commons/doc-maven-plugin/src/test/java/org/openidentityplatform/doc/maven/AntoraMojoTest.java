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
 * Copyright 2024 3A Systems LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class AntoraMojoTest extends AbstractMojoTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testExecute() throws Exception {
        File pom = getTestFile("src/test/resources/antora/pom.xml");
        assertThat(pom).isNotNull();

        AntoraMojo antoraMojo = (AntoraMojo) lookupMojo("antora", pom);
        assertThat(antoraMojo).isNotNull();
        this.configureMojo(antoraMojo, "doc-maven-plugin", pom);
        this.getVariablesAndValuesFromObject(antoraMojo);
    }
}