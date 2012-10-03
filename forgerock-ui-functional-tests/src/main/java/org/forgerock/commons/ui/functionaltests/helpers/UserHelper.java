package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.openidmclient.OpenIDMClient;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.springframework.stereotype.Component;

@Component
public class UserHelper {
	
	@Inject
	protected JsonUtils jsonUtils;
	
	@Inject
	private OpenIDMClient openIDMClient;
	
	public void login(String user, String password) {
		String cookie = openIDMClient.loginAndReturnCookie(user, password);
	}
	
	public void logout() {
		openIDMClient.logout("Some cookie");
	}
	
	public void updateProfile(Object json) {
		//TODO
	}
	
	public void assertProfileEquals(Object json) {
		//TODO
	}
	
	public String register(JsonNode user) {
		return openIDMClient.createUser(user);
	}

	public void createDefaultUser() {
		openIDMClient.createUser(jsonUtils.readJsonFromFile("/defaultuser.json"));
	}
	
}
