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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.api.jackson;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;

import javax.validation.ValidationException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CrestStringSchemaTest {

    @DataProvider
    public Object[][] dateFormats() {
        return new Object[][] {
            // format, value, valid
            {"date-time", "2016-03-15T10:15:30Z", true},
            {"date-time", "2016-03-15T10:15:30.123+01:00", true},
            {"date-time", "2016-03-15T10:15:30", true},
            {"date-time", "2016-03-15T24:00:00Z", true},
            {"date-time", "-0001-03-15T10:15:30Z", true},
            // date-time trims XML white space, date does not
            {"date-time", " 2016-03-15T10:15:30Z\n", true},
            {"date-time", "\t2016-03-15T10:15:30Z\r\n", true},
            {"date-time", "\u000b2016-03-15T10:15:30Z", false},
            {"date-time", " 2016-03-15T10:15:30Z", false},
            {"date-time", "2016-13-15T10:15:30Z", false},
            {"date-time", "2016-02-30T10:15:30Z", false},
            {"date-time", "2016-03-15 10:15:30Z", false},
            {"date-time", "2016-03-15T10:15:30Zjunk", false},
            {"date-time", "", false},
            {"date-time", "garbage", false},
            {"date", "2016-03-15", true},
            {"date", "2016-03-15Z", true},
            {"date", "2016-03-15+05:00", true},
            {"date", " 2016-03-15", false},
            {"date", "2016-03-15 ", false},
            {"date", "2016-3-15", false},
            {"date", "garbage", false},
            {"full-date", "2016-03-15", true},
            {"full-date", "\n2016-03-15", false},
        };
    }

    @Test(dataProvider = "dateFormats")
    public void testValidateDateFormat(final String format, final String value, final boolean valid) {
        final CrestStringSchema schema = new CrestStringSchema();
        schema.setPropertyFormat(format);

        if (valid) {
            schema.validate(json(value));
        } else {
            assertThatThrownBy(() -> schema.validate(json(value))).isInstanceOf(ValidationException.class);
        }
    }
}
