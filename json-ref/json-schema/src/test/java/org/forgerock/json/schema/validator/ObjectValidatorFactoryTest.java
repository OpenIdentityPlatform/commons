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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.schema.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.schema.validator.validators.*;
import org.testng.annotations.Test;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("javadoc")
public class ObjectValidatorFactoryTest {
    @SuppressWarnings("deprecation")
    @Test
    public void testGetTypeValidatorBySchema() throws Exception {
        Map<String, Object> schema = new HashMap<>();
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(AnyTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_STRING);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(StringTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_NUMBER);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(NumberTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_INTEGER);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(IntegerTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_BOOLEAN);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(BooleanTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_OBJECT);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(ObjectTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_ARRAY);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(ArrayTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_NULL);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(NullTypeValidator.class);
        schema.put(Constants.TYPE,Constants.TYPE_ANY);
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(AnyTypeValidator.class);
        schema.put(Constants.TYPE, Arrays.asList(Constants.TYPE_ANY,Constants.TYPE_NULL));
        assertThat(ObjectValidatorFactory.getTypeValidator(schema)).isInstanceOf(UnionTypeValidator.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetUnsupportedTypeValidator() throws Exception {
        Map<String, Object> schema = new HashMap<>();
        schema.put(Constants.TYPE,"FAKE");
        ObjectValidatorFactory.getTypeValidator(schema);

    }

     @Test(expectedExceptions = RuntimeException.class)
    public void testInvalidSchema() throws Exception {
        Map<String, Object> schema = new HashMap<>();
        schema.put(Constants.TYPE,1);
        ObjectValidatorFactory.getTypeValidator(schema);
    }

    @Test
    public void enginesTest() {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factoryList = manager.getEngineFactories();
        for (ScriptEngineFactory factory : factoryList) {
          System.out.println(factory.getEngineName());
          System.out.println(factory.getLanguageName());
        }
    }
}
