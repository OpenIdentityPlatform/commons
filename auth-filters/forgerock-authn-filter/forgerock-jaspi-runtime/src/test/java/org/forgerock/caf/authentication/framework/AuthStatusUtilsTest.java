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

package org.forgerock.caf.authentication.framework;

import static org.assertj.core.api.Assertions.assertThat;

import javax.security.auth.message.AuthStatus;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthStatusUtilsTest {

    @DataProvider(name = "authStatusString")
    public Object[][] getAuthStatusStringData() {
        return new Object[][]{
            {AuthStatus.SUCCESS, "SUCCESS"},
            {AuthStatus.SEND_SUCCESS, "SEND_SUCCESS"},
            {AuthStatus.SEND_CONTINUE, "SEND_CONTINUE"},
            {AuthStatus.SEND_FAILURE, "SEND_FAILURE"},
            {AuthStatus.FAILURE, "FAILURE"},
            {null, "null"},
        };
    }

    @Test(dataProvider = "authStatusString")
    public void asStringShouldConvertAuthStatusToString(AuthStatus authStatus, String expectedString) {

        //When
        String s = AuthStatusUtils.asString(authStatus);

        //Then
        assertThat(s).isEqualTo(expectedString);
    }

    @DataProvider(name = "isAuthStatus")
    public Object[][] getIsAuthStatusData() {
        // formatter:off
        return new Object[][]{
            // AuthStatus             | isSuccess | isSendSuccess | isSendContinue | isSendFailure | isFailure | isNull
            {AuthStatus.SUCCESS,        true,       false,          false,           false,          false,      false},
            {AuthStatus.SEND_SUCCESS,   false,      true,           false,           false,          false,      false},
            {AuthStatus.SEND_CONTINUE,  false,      false,          true,            false,          false,      false},
            {AuthStatus.SEND_FAILURE,   false,      false,          false,           true,           false,      false},
            {AuthStatus.FAILURE,        false,      false,          false,           false,          true,       false},
            {null,                      false,      false,          false,           false,          false,      true},
        };
        // formatter:on
    }

    @Test(dataProvider = "isAuthStatus")
    public void isAuthStatusShouldCompareAuthStatus(AuthStatus authStatus, boolean expectedIsSuccess,
            boolean expectedIsSendSuccess, boolean expectedIsSendContinue, boolean expectedIsSendFailure,
            boolean expectedIsFailure, boolean expectedIsNull) {

        //When
        boolean isSuccess = AuthStatusUtils.isSuccess(authStatus);
        boolean isSendSuccess = AuthStatusUtils.isSendSuccess(authStatus);
        boolean isSendContinue = AuthStatusUtils.isSendContinue(authStatus);
        boolean isSendFailure = AuthStatusUtils.isSendFailure(authStatus);
        boolean isFailure = AuthStatusUtils.isFailure(authStatus);
        boolean isNull = AuthStatusUtils.isNull(authStatus);

        //Then
        assertThat(isSuccess).isEqualTo(expectedIsSuccess);
        assertThat(isSendSuccess).isEqualTo(expectedIsSendSuccess);
        assertThat(isSendContinue).isEqualTo(expectedIsSendContinue);
        assertThat(isSendFailure).isEqualTo(expectedIsSendFailure);
        assertThat(isFailure).isEqualTo(expectedIsFailure);
        assertThat(isNull).isEqualTo(expectedIsNull);
    }
}
