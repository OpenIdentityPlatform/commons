package org.forgerock.commons.ui.functionaltests;

import javax.inject.Inject;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.forgerock.commons.ui.functionaltests.helpers.*;
import org.forgerock.commons.ui.functionaltests.openidmclient.OpenIDMClient;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrorsAspect;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.openqa.selenium.WebDriver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;

@ContextConfiguration(locations = { "classpath:testApplicationContext.xml" })
public class AbstractTest extends AbstractTestNGSpringContextTests {

	@Inject
	protected WebDriver driver;

	@Inject
	protected FormsHelper forms;
	
	@Inject
	protected MessagesHelper messages;

	@Inject
	protected Constants constants;
	
	@Inject
	protected RouterHelper router;
	
	@Inject
	protected SeleniumHelper selenium;
	
	@Inject
	protected JsonUtils jsonUtils;
	
	@Inject
	protected OpenIDMClient openidmClient;
	
	@Inject
	protected UserHelper userHelper;
	
	@Inject
	protected DialogsHelper dialogsHelper;
	
	@Inject
	protected AssertNoErrorsAspect assertNoErrorsAspect;
	
	@BeforeMethod
	public void cleanup() {
		selenium.removeCookies();
		driver.get(constants.getBasePage());
		openidmClient.removeAllUsers();
	}
	
	@AfterClass
	public void cleanupOnEnd() {
		openidmClient.removeAllUsers();
	}
	
	protected void fieldShouldBeValidAfterChange(String element, String fieldName, String valueToSet) {
		String tmpValue = forms.getFieldValue(element, fieldName);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, valueToSet);
		forms.assertFormFieldHasValue(element, fieldName, valueToSet);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
		forms.setField(element, fieldName, tmpValue);
		forms.assertFormFieldHasValue(element, fieldName, tmpValue);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
	}
	
	protected void fieldShouldBeValidAfterChangeButFormCanBeNotValid(String element, String fieldName, String valueToSet) {
		String tmpValue = forms.getFieldValue(element, fieldName);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, valueToSet);
		forms.assertFormFieldHasValue(element, fieldName, valueToSet);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, tmpValue);
		forms.assertFormFieldHasValue(element, fieldName, tmpValue);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
	}

	protected void fieldShouldBeNotValidAfterChange(String element, String fieldName, String valueToSet) {
		String tmpValue = forms.getFieldValue(element, fieldName);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, valueToSet);
		forms.assertFormFieldHasValue(element, fieldName, valueToSet);
		forms.assertValidationError(element, fieldName);
		forms.assertFormValidationError(element);
		forms.setField(element, fieldName, tmpValue);
		forms.assertFormFieldHasValue(element, fieldName, tmpValue);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
	}
	
	protected void assertFieldHasValue(String element, String fieldName, String expectedValue) {
		String fieldValue = forms.getFieldValue(element, fieldName);
		Assert.assertEquals(expectedValue, fieldValue);
	}
	
}
