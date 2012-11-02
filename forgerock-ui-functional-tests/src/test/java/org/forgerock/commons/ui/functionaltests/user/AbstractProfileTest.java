package org.forgerock.commons.ui.functionaltests.user;

import javax.inject.Inject;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrorsAspect;

public class AbstractProfileTest extends AbstractTest {
	
	@Inject
	private AssertNoErrorsAspect assertNoErrorsAspect;
	
	protected String formatStringFromForm(String value) {
		return value.substring(1, value.length()-1);
	}
	
	protected abstract class ProfileUpdateTest {
		
		protected abstract void checkAndChangeData();
		
		protected abstract void checkIfStoredCorrectly();
		
		protected boolean shouldUpdateAndRunPostUpdateAssert() {
			return true;
		}
		
		public final void run() {
			userHelper.createDefaultUser();
			loginAndGoToProfile();
			checkAndChangeData();
			if (shouldUpdateAndRunPostUpdateAssert()) {
				forms.validateForm("content");
				forms.assertFormValidationPasses("content");
				forms.submit("content", "saveButton");
				messages.assertInfoMessage("Profile has been updated");
				userHelper.logout();
				loginAndGoToProfile();
				checkIfStoredCorrectly();
			}
		};

		private void loginAndGoToProfile() {
			userHelper.loginAsDefaultUser();
			assertNoErrorsAspect.assertNoErrors();
			router.goToProfile(true);
			router.assertUrl("#profile/");
			forms.validateForm("content");
		}
	}
	
	protected class AssertValidFieldAfterChange extends ProfileUpdateTest {

		private String fieldName;
		private String fieldValue;
		private String formattedFieldValue;
		private String currentFieldValue = "";

		public AssertValidFieldAfterChange(String fieldName, String fieldValue) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
			this.formattedFieldValue = fieldValue;
		}
		
		public AssertValidFieldAfterChange(String fieldName, String fieldValue, String formattedFieldValue) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
			this.formattedFieldValue = formattedFieldValue;
		}
		
		public AssertValidFieldAfterChange(String fieldName, String fieldValue, String formattedFieldValue, String currentFieldValue) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
			this.formattedFieldValue = formattedFieldValue;
			this.currentFieldValue  = currentFieldValue;
		}
		
		@Override
		protected void checkAndChangeData() {
			assertFieldHasValue(fieldName, currentFieldValue);
			forms.setField("content", fieldName, fieldValue);
			assertFieldHasValue(fieldName, fieldValue);
		}

		@Override
		protected void checkIfStoredCorrectly() {
			assertFieldHasValue(fieldName, formattedFieldValue);
		}
		
	}
	
	protected class AssertNotValidFieldAfterChange extends ProfileUpdateTest {

		private String fieldName;
		private String fieldValue;

		public AssertNotValidFieldAfterChange(String fieldName, String fieldValue) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
		}
		
		@Override
		protected boolean shouldUpdateAndRunPostUpdateAssert() {
			return false;
		}
		
		@Override
		protected void checkAndChangeData() {
			forms.setField("content", fieldName, fieldValue);
			assertFieldHasValue(fieldName, fieldValue);
			forms.assertValidationError("content", fieldName);
			forms.assertFormValidationError("content");
		}

		@Override
		protected void checkIfStoredCorrectly() {
		}
		
	}
	
	protected void assertFieldHasValue(String fieldName, String expectedValue) {
		String fieldValue = forms.getFieldValue("content", fieldName);
		Assert.assertEquals(expectedValue, fieldValue);
	}
	
	protected void shouldHaveCountryAndState(String countryValue, String countryDisplayValue, String stateValue, String stateDisplayValue) {
		JsonNode profileForm = forms.readForm("content");
		
		String country = formatStringFromForm(profileForm.get("country").toString());
		Assert.assertEquals(countryValue, country);
		
		String state = formatStringFromForm(profileForm.get("stateProvince").toString());
		Assert.assertEquals(stateValue, state);
		
		String countryDisplay = forms.getSelectDisplayValue("content", "country");
		Assert.assertEquals(countryDisplayValue, countryDisplay);
		
		String stateDisplay = forms.getSelectDisplayValue("content", "stateProvince");
		Assert.assertEquals(stateDisplayValue, stateDisplay);
	}
	
}
