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
 * Portions Copyright 2016 Nomura Research Institute, Ltd.
 */
package org.forgerock.audit.handlers.jdbc;

import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class JdbcUtilsTest {

    @Test
    public void canInitialisePreparedStatementArrayFieldsWithNullValues() throws Exception {
        // given
        final PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        final List<Parameter> params = Collections.singletonList(new Parameter(Parameter.Type.ARRAY, null));

        // when
        JdbcUtils.initializePreparedStatement(mockPreparedStatement, params);

        // then
        verify(mockPreparedStatement).setString(1, null);
    }

    @Test
    public void canInitialisePreparedStatementIntegerFieldsWithNullValues() throws Exception {
        // given
        final PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        final List<Parameter> params = Collections.singletonList(new Parameter(Parameter.Type.INTEGER, null));

        // when
        JdbcUtils.initializePreparedStatement(mockPreparedStatement, params);

        // then
        verify(mockPreparedStatement).setNull(1, Types.INTEGER);
    }

    @Test
    public void canInitialisePreparedStatementNumberFieldsWithNullValues() throws Exception {
        // given
        final PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        final List<Parameter> params = Collections.singletonList(new Parameter(Parameter.Type.NUMBER, null));

        // when
        JdbcUtils.initializePreparedStatement(mockPreparedStatement, params);

        // then
        verify(mockPreparedStatement).setNull(1, Types.FLOAT);
    }

    @Test
    public void canInitialisePreparedStatementObjectFieldsWithNullValues() throws Exception {
        // given
        final PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        final List<Parameter> params = Collections.singletonList(new Parameter(Parameter.Type.OBJECT, null));

        // when
        JdbcUtils.initializePreparedStatement(mockPreparedStatement, params);

        // then
        verify(mockPreparedStatement).setString(1, null);
    }

    @Test
    public void canInitialisePreparedStatementStringFieldsWithNullValues() throws Exception {
        // given
        final PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        final List<Parameter> params = Collections.singletonList(new Parameter(Parameter.Type.STRING, null));

        // when
        JdbcUtils.initializePreparedStatement(mockPreparedStatement, params);

        // then
        verify(mockPreparedStatement).setString(1, null);
    }

    @Test
    public void canInitialisePreparedStatementBooleanFieldsWithNullValues() throws Exception {
        // given
        final PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        final List<Parameter> params = Collections.singletonList(new Parameter(Parameter.Type.BOOLEAN, null));

        // when
        JdbcUtils.initializePreparedStatement(mockPreparedStatement, params);

        // then
        verify(mockPreparedStatement).setNull(1, Types.BOOLEAN);
    }

}
