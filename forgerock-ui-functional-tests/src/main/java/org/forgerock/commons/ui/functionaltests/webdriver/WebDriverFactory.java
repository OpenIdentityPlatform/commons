package org.forgerock.commons.ui.functionaltests.webdriver;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebDriverFactory {
	
	@Inject
	private Constants constants;

	@Bean(name="WebDriver")
	public WebDriver getInstance() {
		WebDriver driver = WebBrowserDriver.valueOf(constants.getWebBrowser()).getDriver();
		driver.manage().timeouts().implicitlyWait(constants.waitTime(), TimeUnit.MILLISECONDS);
		return driver;
	}
	
	@Bean(name="WebDriverWait")
	public WebDriverWait getWebDriverWait(WebDriver driver) {
		return new WebDriverWait(driver, constants.waitTime() * 10, constants.waitTime());
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
