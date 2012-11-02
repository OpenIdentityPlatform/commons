package org.forgerock.commons.ui.functionaltests.user;

import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class ProfileTest extends AbstractProfileTest {
	
	private static final String EMPTY_SELECT = "";
	private static final String PLEASE_SELECT_DISPLAY_TEXT = "Please Select";
	private static final String NO_VALUE_SELECT = null;

	@Test
	@AssertNoErrors
	public void updateAddress1() {
		String exampleAddress1 = "Some Example Address 1";
		new AssertValidFieldAfterChange("address1", exampleAddress1).run();
	}
	
	@Test
	@AssertNoErrors
	public void updateAddress2() {
		String exampleAddress2 = "Some Example Address 2";
		new AssertValidFieldAfterChange("address2", exampleAddress2).run();
	}
	
	@Test
	@AssertNoErrors
	public void updatePostalCode() {
		String examplePostalCode = "Some Postal Code";
		new AssertValidFieldAfterChange("postalCode", examplePostalCode).run();
	}
	
	@Test
	@AssertNoErrors
	public void updateGivenNameWithValidValue() {
		String currentGivenName = "TestName";
		String validGivenName = "Jack";
		new AssertValidFieldAfterChange("givenName", validGivenName, validGivenName, currentGivenName).run();
	}
	
	@Test
	@AssertNoErrors
	public void updateGivenNameWithEmptyValue() {
		String emptyGivenName = "";
		new AssertNotValidFieldAfterChange("givenName", emptyGivenName).run();
	}
	
	@Test
	@AssertNoErrors
	public void updateFamilyNameWithValidValue() {
		String currentFamilyName = "TestSurname";
		String validFamilyName = "Bloodrider";
		new AssertValidFieldAfterChange("familyName", validFamilyName, validFamilyName, currentFamilyName).run();
	}
	
	@Test
	@AssertNoErrors
	public void updateFamilyNameWithEmptyValue() {
		String emptyFamilyName = "";
		new AssertNotValidFieldAfterChange("familyName", emptyFamilyName).run();
	}
	
	@Test
	@AssertNoErrors
	public void updatePhoneNumberWithValidValue() {
		String currentPhoneNumber = "0044123456789";
		String validPhoneNumber = "0044987654321";
		new AssertValidFieldAfterChange("phoneNumber", validPhoneNumber, validPhoneNumber, currentPhoneNumber).run();
	}
	
	@Test
	@AssertNoErrors
	public void updatePhoneNumberWithValueWithLetters() {
		String invalidPhoneNumber = "00449A87654321";
		new AssertNotValidFieldAfterChange("phoneNumber", invalidPhoneNumber).run();
	}
	
	@Test
	@AssertNoErrors
	public void updatePhoneNumberCheckRemoveAdditionalValidChars() {
		String currentPhoneNumber = "0044123456789";
		String validNotFormattedPhoneNumber = "+44 500-000-000";
		String validFormattedPhoneNumber = "+44500000000";
		new AssertValidFieldAfterChange("phoneNumber", validNotFormattedPhoneNumber, validFormattedPhoneNumber, currentPhoneNumber).run();
	}
	
	@Test
	@AssertNoErrors
	public void updateCity() {
		String exampleCity = "Some Example City";
		new AssertValidFieldAfterChange("postalCode", exampleCity).run();
	}
	
	@Test
	@AssertNoErrors
	public void testSetCountry() {
		
		new ProfileUpdateTest() {
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
	
	@Test
	@AssertNoErrors
	public void testSetStateProvince() {
		
		new ProfileUpdateTest() {
			@Override
			protected void checkAndChangeData() {
				shouldHaveCountryAndState(EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT, EMPTY_SELECT, NO_VALUE_SELECT);
				forms.setField("content", "country", "poland");
				shouldHaveCountryAndState("poland", "Poland", EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT);
				forms.setField("content", "stateProvince", "lubuskie");
				shouldHaveCountryAndState("poland", "Poland", "lubuskie", "Lubuskie");
			}

			@Override
			protected void checkIfStoredCorrectly() {
				shouldHaveCountryAndState("poland", "Poland", "lubuskie", "Lubuskie");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testMultipleChangesOfCountryAndStateProvince() {		
		userHelper.createDefaultUser();
		userHelper.loginAsDefaultUser();
		router.goToProfile(true);
		router.assertUrl("#profile/");
		
		shouldHaveCountryAndState(EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT, EMPTY_SELECT, NO_VALUE_SELECT);
		forms.setField("content", "country", "poland");
		shouldHaveCountryAndState("poland", "Poland", EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT);
		forms.setField("content", "stateProvince", "lubuskie");
		shouldHaveCountryAndState("poland", "Poland", "lubuskie", "Lubuskie");
		
		forms.setField("content", "stateProvince", EMPTY_SELECT);
		shouldHaveCountryAndState("poland", "Poland", EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT);
		
		forms.setField("content", "stateProvince", "opolskie");
		shouldHaveCountryAndState("poland", "Poland", "opolskie", "Opolskie");
		forms.setField("content", "country", EMPTY_SELECT);
		shouldHaveCountryAndState(EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT, EMPTY_SELECT, NO_VALUE_SELECT);
		
		forms.setField("content", "country", "us");
		shouldHaveCountryAndState("us", "United States", EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT);
		forms.setField("content", "country", EMPTY_SELECT);
		shouldHaveCountryAndState(EMPTY_SELECT, PLEASE_SELECT_DISPLAY_TEXT, EMPTY_SELECT, NO_VALUE_SELECT);

	}
	
	@Test
	@AssertNoErrors
	public void testUpdateEmailNotValidEmailFormat1() {
		String emailToSet = "email";
		new AssertNotValidFieldAfterChange("email", emailToSet).run();
	}
	
	@Test
	@AssertNoErrors
	public void testUpdateEmailNotValidEmailFormat2() {
		String emailToSet = "email@";
		new AssertNotValidFieldAfterChange("email", emailToSet).run();
	}
	
	@Test
	@AssertNoErrors
	public void testUpdateEmailNotValidEmailFormat3() {
		String emailToSet = "email@test.teeeest";
		new AssertNotValidFieldAfterChange("email", emailToSet).run();
	}
	
	@Test
	@AssertNoErrors
	public void testUpdateUserName() {
		String userNameToSet = "second@test.test";
		String fieldName = "userName";
		
		userHelper.createDefaultUser();
		userHelper.loginAsDefaultUser();
		router.goToProfile(true);
		router.assertUrl("#profile/");
		forms.validateForm("content");
		forms.setField("content", fieldName , userNameToSet);
		assertFieldHasValue(fieldName, userNameToSet);
		forms.assertValidationPasses("content", fieldName);
		forms.assertFormValidationPasses("content");
		forms.submit("content", "saveButton");
		
		router.routeTo("#profile/");
		router.assertUrl("#login/");
		
		userHelper.login("second@test.test", "tesT#1#Test");
		router.goToProfile(true);
		router.assertUrl("#profile/");
		forms.validateForm("content");
		
		assertFieldHasValue(fieldName, userNameToSet);
	}
	
	/*@Test
	@AssertNoErrors
	public void testUpdateEmailNotValidEmailAlreadyExists() {
		userHelper.createSecondDefaultUser();
		String usernameToSet = "second@test.test";
		new AssertNotValidFieldAfterChange("userName", usernameToSet).run();
	}*/
	
}
