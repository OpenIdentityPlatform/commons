package org.forgerock.commons.ui.functionaltests;

import javax.inject.Inject;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.forgerock.commons.ui.functionaltests.helpers.*;
import org.forgerock.commons.ui.functionaltests.openidmclient.OpenIDMClient;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrorsAspect;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.forgerock.commons.ui.functionaltests.webdriver.WebDriverFactory;
import org.openqa.selenium.WebDriver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.*;
@ContextConfiguration(locations = { "classpath:testApplicationContext.xml" })
public class AbstractTest extends AbstractTestNGSpringContextTests {

	protected WebDriver driver = WebDriverFactory.getWebDriver();

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
		try{
			openidmClient.removeAllUsers();
		} catch (Exception e) {
			System.out.println("Error removing users. Retrying...");
			try {
				Thread.sleep(1000l);
				openidmClient.removeAllUsers();
			} catch (Exception e1) {
				System.err.println("Failed to remove users");
				Assert.fail("Failed to remove users from openidm");
			}
		}
	}
	
	@AfterClass
	public void cleanupOnEnd() {
		openidmClient.removeAllUsers();
	}
	
	@AfterSuite
	public void closeBrowser() {
		driver.close();
	}
	
	protected void fieldShouldBeValidAfterChange(String element, String fieldName, String valueToSet) {
		String tmpValue = forms.getFieldValue(element, fieldName);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, valueToSet);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
		forms.setField(element, fieldName, tmpValue);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
	}

	protected void fieldShouldBeNotValidAfterChange(String element, String fieldName, String valueToSet) {
		String tmpValue = forms.getFieldValue(element, fieldName);
		forms.assertValidationPasses(element, fieldName);
		forms.setField(element, fieldName, valueToSet);
		forms.assertValidationError(element, fieldName);
		forms.assertFormValidationError(element);
		forms.setField(element, fieldName, tmpValue);
		forms.assertValidationPasses(element, fieldName);
		forms.assertFormValidationPasses(element);
	}
	
	protected void assertFieldHasValue(String element, String fieldName, String expectedValue) {
		String fieldValue = forms.getFieldValue(element, fieldName);
		Assert.assertEquals(expectedValue, fieldValue);
	}
	
}
