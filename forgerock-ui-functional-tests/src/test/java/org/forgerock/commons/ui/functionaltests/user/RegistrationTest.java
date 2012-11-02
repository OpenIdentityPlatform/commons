package org.forgerock.commons.ui.functionaltests.user;

import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class RegistrationTest extends AbstractRegistrationTest {
	
	@Test
	@AssertNoErrors
	public void testEmailFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "email", "");
				fieldShouldBeNotValidAfterChange("content", "email", "qwe");
				fieldShouldBeNotValidAfterChange("content", "email", "qwe@");
				fieldShouldBeNotValidAfterChange("content", "email", "qwe@x.plllll");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testFirstNameFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "givenName", "");
				fieldShouldBeNotValidAfterChange("content", "givenName", "ds3fbvd#");
				fieldShouldBeValidAfterChange("content", "givenName", "fre43cdvse4");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testLastNameFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "familyName", "");
				fieldShouldBeNotValidAfterChange("content", "familyName", "ds3fbvd#");
				fieldShouldBeValidAfterChange("content", "familyName", "fre43cdvse4");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testPhoneNumberFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "phoneNumber", "");
				fieldShouldBeNotValidAfterChange("content", "phoneNumber", "0044123456789a");
				fieldShouldBeValidAfterChange("content", "phoneNumber", "0044123456789");
				fieldShouldBeValidAfterChange("content", "phoneNumber", "+44 500-000-000");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testPasswordFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "password", "");

				fieldShouldBeNotValidAfterChange("content", "password", "aa12345!");
				fieldShouldBeNotValidAfterChange("content", "password", "Aaaaaaa!");
				fieldShouldBeNotValidAfterChange("content", "password", "Aa1234!");
				fieldShouldBeNotValidAfterChange("content", "password", "Aa12345");
				
				//fieldShouldBeValidAfterChangeButFormCanBeNotValid("content", "password", "Aa12345!");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testConfirmPasswordFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				forms.setField("content", "password", "");
				forms.assertFormFieldHasValue("content", "password", "");
				
				forms.setField("content", "passwordConfirm", "");
				forms.assertFormFieldHasValue("content", "passwordConfirm", "");
				forms.assertValidationError("content", "passwordConfirm");
				
				forms.setField("content", "password", "XXX");
				forms.assertFormFieldHasValue("content", "password", "XXX");
				
				forms.setField("content", "passwordConfirm", "xxX");
				forms.assertFormFieldHasValue("content", "passwordConfirm", "xxX");
				forms.assertValidationError("content", "passwordConfirm");
				
				forms.setField("content", "passwordConfirm", "XXX");
				forms.assertFormFieldHasValue("content", "passwordConfirm", "XXX");
				forms.assertValidationPasses("content", "passwordConfirm");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testSecurityQuestionFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "securityQuestion", "");
				fieldShouldBeValidAfterChange("content", "securityQuestion", "3");
				fieldShouldBeNotValidAfterChange("content", "securityQuestion", "");
				fieldShouldBeValidAfterChange("content", "securityQuestion", "4");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testSecurityAnswerFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "securityAnswer", "");
				fieldShouldBeValidAfterChange("content", "securityAnswer", "a");
				fieldShouldBeValidAfterChange("content", "securityAnswer", "+XS321$#ASDFcdas-");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testPassPhraseFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "passPhrase", "");
				fieldShouldBeNotValidAfterChange("content", "passPhrase", "aaa");
				fieldShouldBeValidAfterChange("content", "passPhrase", "aaaa");
				fieldShouldBeValidAfterChange("content", "passPhrase", "+XS321$#ASDFcdas-");
			}
		}.run();
	}
	
	/*@Test
	@AssertNoErrors
	public void testTermsFieldValidation() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeNotValidAfterChange("content", "terms", "false");
				fieldShouldBeValidAfterChange("content", "terms", "true");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testEmailAlreadyExists() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				fieldShouldBeValidAfterChange("content", "email", "test@test.test");
				userHelper.createDefaultUser();
				fieldShouldBeNotValidAfterChange("content", "email", "test@test.test");
			}
		}.run();
	}*/
}
