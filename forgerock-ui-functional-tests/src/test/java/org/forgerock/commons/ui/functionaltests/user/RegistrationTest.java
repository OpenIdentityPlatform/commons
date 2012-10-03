package org.forgerock.commons.ui.functionaltests.user;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.testng.annotations.Test;

public class RegistrationTest extends AbstractTest {
	
	@Test
	public void testEmailFieldValidation() {
		JsonNode user = jsonUtils.readJsonFromFile("/registration/validuser.json");
		router.goToRegistration();
		forms.fillForm("content", user);
		forms.validateForm("content");
		forms.assertFormValidationPasses("content");
		
		fieldShouldBeNotValidAfterChange("content", "email", "qwe");
		fieldShouldBeNotValidAfterChange("content", "email", "qwe@");
		fieldShouldBeNotValidAfterChange("content", "email", "qwe@x.plllll");
	}
	
	@Test
	public void testEmailAlreadyExists() {
		JsonNode user = jsonUtils.readJsonFromFile("/registration/validuser.json");
		router.goToRegistration();
		forms.fillForm("content", user);
		forms.validateForm("content");
		forms.assertFormValidationPasses("content");
		
		fieldShouldBeValidAfterChange("content", "email", "test@test.test");
		userHelper.createDefaultUser();
		fieldShouldBeNotValidAfterChange("content", "email", "test@test.test");
	}
	
}
