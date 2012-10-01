package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import junit.framework.Assert;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

@Component
public class RouterHelper {
	
	@Inject
	private WebDriver driver;
	
	@Inject
	private Constants constants;
	
	public void routeTo(String url) {	
		driver.navigate().to(constants.getBasePage() + url);		
	}
	
	public void assertUrl(final String url) {
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return driver.getCurrentUrl().equals(constants.getBasePage() + url);
            }
        });
		
		Assert.assertEquals(constants.getBasePage() + url, driver.getCurrentUrl());
	}
	
}
