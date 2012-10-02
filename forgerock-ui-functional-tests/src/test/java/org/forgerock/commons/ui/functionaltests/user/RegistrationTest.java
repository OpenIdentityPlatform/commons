package org.forgerock.commons.ui.functionaltests.user;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.testng.annotations.Test;

public class RegistrationTest extends AbstractTest {
	
	@Test
	public void testFieldsValidation() {
//		JsonNode user = jsonUtils.readJsonFromFile("/registration/validuser.json");
//		router.goToRegistration();
//		forms.fillForm("content", user);
//		forms.validateForm("content");
//		forms.assertFormValidationPasses("content");
//		
//		testIfFieldNotValidAfterChange("content", "email", "qwe");
//		testIfFieldNotValidAfterChange("content", "email", "qwe@");
//		testIfFieldNotValidAfterChange("content", "email", "qwe@x.plllll");
//		
//		userHelper.createDefaultUser();
//		testIfFieldNotValidAfterChange("content", "email", "test@test.test");
	}
	
	private void testIfFieldNotValidAfterChange(String element, String fieldName, String valueToSet) {
		String tmpValue = forms.getFieldValue(element, fieldName);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, valueToSet);
		forms.assertValidationError(element, fieldName);
		forms.setField(element, fieldName, tmpValue);
		forms.assertValidationPasses(element, fieldName);
	}
	
}
