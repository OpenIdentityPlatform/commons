package org.forgerock.commons.ui.functionaltests.screenshot;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.forgerock.commons.ui.functionaltests.webdriver.WebDriverFactory;
import org.openqa.selenium.*;

public class ScreenShot {
	
	protected WebDriver driver = WebDriverFactory.getWebDriver();
	
	public void makeScreenShot() {
		String methodName = getMethodName();
		File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(scrFile, new File("target/surefire-reports/" + methodName + ".png"));
		} catch (IOException e) {
			System.err.println("Fail to create screenshot for fail method " + methodName);
		}
	}
	
	private String getMethodName() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (StackTraceElement stackTraceElement : stackTrace) {
			String methodName = stackTraceElement.getMethodName().toLowerCase();
			if (methodName.contains("test") || methodName.startsWith("should")){
				return stackTraceElement.getMethodName();
			}
		}
		return "unknown";
	}

	public void makeScreenShot(String description, AssertionError assertionError) {
		makeScreenShot();
		createDescriptionFile(description, assertionError);
	}

	private void createDescriptionFile(String description, AssertionError assertionError) {
		File descriptionFile = new File("target/surefire-reports/" + getMethodName() + ".txt");
		try {
			FileUtils.writeStringToFile(descriptionFile, description + "\n" + ExceptionUtils.getStackTrace(assertionError));
		} catch (IOException e) {
			System.err.println("Fail to create description file for fail method " + getMethodName());
		}
	}
	
}
