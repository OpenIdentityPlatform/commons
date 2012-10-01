package org.forgerock.commons.ui.functionaltests;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.helpers.Constants;
import org.forgerock.commons.ui.functionaltests.helpers.FormsHelper;
import org.forgerock.commons.ui.functionaltests.helpers.MessagesHelper;
import org.forgerock.commons.ui.functionaltests.helpers.RouterHelper;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.openqa.selenium.WebDriver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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

	@BeforeClass
	public void onTestStart() {
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}

	@AfterClass
	public void onTestEnd() {
		//driver.close();
	}
	
	@BeforeMethod
	public void cleanup() {
		selenium.removeCookies();
		driver.get(constants.getBasePage());
	}

}
