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

package org.forgerock.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.Parameter;
import org.testng.annotations.Test;

public class PathUtilTest {

    @Test
    public void testBuildPath() {
        assertThat(PathUtil.buildPath("first")).isEqualTo("/first");
        assertThat(PathUtil.buildPath("/first/")).isEqualTo("/first");
        assertThat(PathUtil.buildPath("first", "second")).isEqualTo("/first/second");
        assertThat(PathUtil.buildPath("/first/", "/second/")).isEqualTo("/first/second");
    }

    @Test
    void testBuildPathParameters() {
        assertThat(PathUtil.buildPathParameters("/")).isNull();
        assertThat(PathUtil.buildPathParameters("/{id}")).hasSize(1);
        assertThat(PathUtil.buildPathParameters("/{id}/more")).hasSize(1);
        assertThat(PathUtil.buildPathParameters("/{id}/more/{moreId}")).hasSize(2);
    }

    @Test
    void testMergeParameters() {
        final Parameter parameter1 = Parameter.parameter()
                .name("parameter1")
                .type("string")
                .source(ParameterSource.PATH)
                .required(true)
                .build();

        final Parameter parameter1Override = Parameter.parameter()
                .name("parameter1")
                .type("string")
                .source(ParameterSource.PATH)
                .required(false)
                .build();

        final Parameter parameter2 = Parameter.parameter()
                .name("parameter2")
                .type("string")
                .source(ParameterSource.PATH)
                .required(true)
                .build();

        assertThat(PathUtil.mergeParameters(new ArrayList<Parameter>(), (Parameter[]) null).isEmpty());
        assertThat(PathUtil.mergeParameters(new ArrayList<Parameter>(), parameter1)).hasSize(1);
        assertThat(PathUtil.mergeParameters(new ArrayList<Parameter>(), parameter1, parameter2)).hasSize(2);

        // test overriding parameter with same name
        final List<Parameter> initialParameters = new ArrayList<>(Arrays.asList(parameter1));
        assertThat(initialParameters.get(0).isRequired()).isTrue();

        final List<Parameter> mergedParameters = PathUtil.mergeParameters(
                initialParameters, parameter1Override);
        assertThat(mergedParameters).hasSize(1);
        assertThat(mergedParameters.get(0).isRequired()).isFalse();
    }

}
