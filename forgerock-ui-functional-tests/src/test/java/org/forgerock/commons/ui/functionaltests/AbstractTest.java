package org.forgerock.commons.ui.functionaltests;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.forgerock.commons.ui.functionaltests.helpers.*;
import org.forgerock.commons.ui.functionaltests.openidmclient.OpenIDMClient;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.openqa.selenium.WebDriver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.*;

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

}
