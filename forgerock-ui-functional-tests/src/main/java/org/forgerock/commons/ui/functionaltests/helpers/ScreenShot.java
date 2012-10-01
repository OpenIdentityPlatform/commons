package org.forgerock.commons.ui.functionaltests.helpers;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

@Component
public class ScreenShot {
	
	@Inject
	protected WebDriver driver;
	
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
			if (stackTraceElement.getMethodName().toLowerCase().contains("test")){
				return stackTraceElement.getMethodName();
			}
		}
		return "unknown";
	}

	public void makeScreenShot(String description) {
		makeScreenShot();
		createDescriptionFile(description);
	}

	private void createDescriptionFile(String description) {
		File descriptionFile = new File("target/surefire-reports/" + getMethodName() + ".txt");
		try {
			FileUtils.writeStringToFile(descriptionFile, description);
		} catch (IOException e) {
			System.err.println("Fail to create description file for fail method " + getMethodName());
		}
	}
	
}
