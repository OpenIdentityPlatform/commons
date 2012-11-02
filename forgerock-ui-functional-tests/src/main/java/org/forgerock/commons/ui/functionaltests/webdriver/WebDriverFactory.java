package org.forgerock.commons.ui.functionaltests.webdriver;

import java.util.concurrent.TimeUnit;

import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebDriverFactory {
	
	private static Constants constants = new Constants();
	
	public static WebDriver driver;
	
	public static WebDriverWait webDriverWait;
	
	public static WebDriver getWebDriver() {
		if (driver == null) {
			WebDriver driverInstance = WebBrowserDriver.valueOf(constants.getWebBrowser()).getDriver();
			driverInstance.manage().timeouts().implicitlyWait(constants.waitTime(), TimeUnit.MILLISECONDS);
			driver = driverInstance;
		}
		return driver;
	}
	
	public static WebDriverWait getWebDriverWait() {
		if (webDriverWait == null) {
			webDriverWait = new WebDriverWait(getWebDriver(), constants.waitTime() / 10);
		}
		return webDriverWait;
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
