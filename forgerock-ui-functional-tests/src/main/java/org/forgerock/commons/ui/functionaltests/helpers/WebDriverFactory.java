package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebDriverFactory {
	
	@Inject
	private Constants constants;

	@Bean(name="WebDriver")
	public WebDriver getInstance() {
		return WebBrowserDriver.valueOf(constants.getWebBrowser()).getDriver();
	}
	
	enum WebBrowserDriver {
		FIREFOX(FirefoxDriver.class),
		SAFARI(SafariDriver.class),
		IE(InternetExplorerDriver.class),
		CHROME(ChromeDriver.class);
		
		private Class<? extends WebDriver> driver;

		private WebBrowserDriver(Class<? extends WebDriver> driver) {
			this.driver = driver;
		}

		public WebDriver getDriver() {
			try {
				return driver.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	};
	
}
