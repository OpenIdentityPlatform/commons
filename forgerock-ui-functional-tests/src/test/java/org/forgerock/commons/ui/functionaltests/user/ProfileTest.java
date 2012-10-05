package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

public class ProfileTest extends AbstractTest {
	
	private static final String EMPTY_SELECT = "";
	private static final String PLEASE_SELECT_DISPLAY_TEXT = "Please Select";
	private static final String NO_VALUE_SELECT = null;

	@Test
	@AssertNoErrors
	public void testSetCountry() {
		
		new ProfileUpdateTestTemplate() {
			@Override
			protected void checkAndChangeData() {
				shouldHaveCountryAndState(EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT, EMPTY_SELECT, NO_VALUE_SELECT);
				forms.setField("content", "country", "poland");
				shouldHaveCountryAndState("poland", "Poland", EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT);
			}

			@Override
			protected void checkIfStoredCorrectly() {
				shouldHaveCountryAndState("poland", "Poland", EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT);
			}
		}.run();
		
	}
	
	public void shouldHaveCountryAndState(String countryValue, String countryDisplayValue, String stateValue, String stateDisplayValue) {
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
	
	private String formatStringFromForm(String value) {
		return value.substring(1, value.length()-1);
	}
	
	private abstract class ProfileUpdateTestTemplate {
		
		protected abstract void checkAndChangeData();
		
		protected abstract void checkIfStoredCorrectly();
		
		public final void run() {
			userHelper.createDefaultUser();
			loginAndGoToProfile();
			checkAndChangeData();
			forms.submit("content", "saveButton");
			userHelper.logout();
			loginAndGoToProfile();
			checkIfStoredCorrectly();
		};

		private void loginAndGoToProfile() {
			userHelper.loginAsDefaultUser();
			router.goToProfile(true);
			router.assertUrl("#profile/");
			forms.validateForm("content");
		}
	}
	
}
