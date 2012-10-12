package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;

public class AbstractRegistrationTest extends AbstractTest{
	
	protected String formatStringFromForm(String value) {
		return value.substring(1, value.length()-1);
	}
	
	protected abstract class RegistrationValidationTest {
		
		protected abstract void checkRegistrationViewBehavior();
		
		public final void run() {
			JsonNode user = jsonUtils.readJsonFromFile("/registration/validuser.json");
			router.goToRegistration();
			forms.fillForm("content", user);
			forms.validateForm("content");
			forms.assertFormValidationPasses("content");
			checkRegistrationViewBehavior();
		};

	}
	
	protected void assertFieldHasValue(String fieldName, String expectedValue) {
		String fieldValue = forms.getFieldValue("content", fieldName);
		Assert.assertEquals(expectedValue, fieldValue);
	}
	
}
