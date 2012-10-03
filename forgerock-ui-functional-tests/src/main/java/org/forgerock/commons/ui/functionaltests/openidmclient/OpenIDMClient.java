package org.forgerock.commons.ui.functionaltests.openidmclient;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.forgerock.commons.ui.functionaltests.openidmclient.transfer.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenIDMClient {
	
	@Inject
	private Constants constants;
	
	private RestTemplate restTemplate;
	
	HttpHeaders getAllUsersHeaders = new HttpHeaders();
	HttpHeaders deleteUserHeaders = new HttpHeaders();
	HttpHeaders createUserHeaders = new HttpHeaders();
	
	public OpenIDMClient() {
		initializeRestTemplate();
		initializeHeaders();
	}
	
	public String createUser(JsonNode user) {
		String entityId = user.get("email").toString().replace('[', ' ').replace(']', ' ').replace('"', ' ').trim();
		HttpEntity<JsonNode> requestEntity = new HttpEntity<JsonNode>(user, createUserHeaders);
		
		ResponseEntity<EntityBaseInfo> response = restTemplate.exchange(constants.getOpenIDMServer() + "openidm/managed/user/" + entityId ,
			      HttpMethod.PUT, requestEntity, EntityBaseInfo.class);
		
		EntityBaseInfo result = response.getBody();
		if (result != null && result.getError() != null) {
			throw new IllegalStateException("An error occured: " + result.getError() + "\nDetails: " + result.getReason());
		}
		
		return result.get_id();
	}
	
	
//	public void logout(String cookie) {
//		restTemplate.exchange(constants.getOpenIDMServer() + "openidm/",
//				HttpMethod.GET, new HttpEntity<byte[]>(getHeadersForLogout(cookie)), EntityBaseInfoArray.class);
//	}
	
	/**
	 * @param cookie like JSESSIONID=ixnekr105coj11ji67xcluux8
	 */
	public String loginAndReturnCookie(String userName, String password) {
		try {
			ResponseEntity<EntityBaseInfoArray> response = restTemplate.exchange(constants.getOpenIDMServer() + "openidm/managed/user/?_query-id=for-credentials",
				      HttpMethod.GET, new HttpEntity<byte[]>(getHeadersForUser(userName, password)), EntityBaseInfoArray.class);
			return response.getHeaders().get("Set-Cookie").get(0);
		} catch (RestClientException e) {
			throw new IllegalArgumentException("Invalid credentials");
		}
	}

	public void removeAllUsers() {
		EntityBaseInfo[] users = getAllUsers();
		for (EntityBaseInfo entityBaseInfo : users) {
			deleteUser(entityBaseInfo.get_id());
		}
	}
	
	public void deleteUser(String entityId) {
		ResponseEntity<BaseResult> response = restTemplate.exchange(constants.getOpenIDMServer() + "openidm/managed/user/" + entityId,
			      HttpMethod.DELETE, new HttpEntity<byte[]>(deleteUserHeaders), BaseResult.class);
		BaseResult result = response.getBody();
		if (result != null && result.getError() != null) {
			throw new IllegalStateException("An error occured: " + result.getError() + "\nDetails: " + result.getReason());
		}
	}

	public EntityBaseInfo[] getAllUsers() {
		ResponseEntity<EntityBaseInfoArray> response = restTemplate.exchange(constants.getOpenIDMServer() + "openidm/managed/user/?_query-id=query-all-ids",
			      HttpMethod.GET, new HttpEntity<byte[]>(getAllUsersHeaders), EntityBaseInfoArray.class);
		EntityBaseInfoArray result = response.getBody();
		if (result.getError() != null) {
			throw new IllegalStateException("An error occured: " + result.getError() + "\nDetails: " + result.getReason());
		} else {
			return result.getResult();
		}
	}
	
	private HttpHeaders getHeadersForUser(String userName, String password) {
		HttpHeaders userHeaders = new HttpHeaders();
		userHeaders.add("X-OpenIDM-Username", userName);
		userHeaders.add("X-OpenIDM-Password", password);
		return userHeaders;
	}
	
//	private HttpHeaders getHeadersForLogout(String cookie) {
//		HttpHeaders headersForLogout = new HttpHeaders();
//		headersForLogout.add("X-OpenIDM-Logout", "true");
//		headersForLogout.add("Cookie", cookie);
//		return headersForLogout;
//	}

	private void initializeHeaders() {
		getAllUsersHeaders.add("X-OpenIDM-Username", "openidm-admin");
		getAllUsersHeaders.add("X-OpenIDM-Password", "openidm-admin");
		getAllUsersHeaders.add("X-OpenIDM-NoSession", "true");
		
		deleteUserHeaders.add("X-OpenIDM-Username", "openidm-admin");
		deleteUserHeaders.add("X-OpenIDM-Password", "openidm-admin");
		deleteUserHeaders.add("If-Match", "*");
		
		createUserHeaders.add("X-OpenIDM-Username", "anonymous");
		createUserHeaders.add("X-OpenIDM-Password", "anonymous");
		createUserHeaders.add("X-OpenIDM-NoSession", "true");
	}

	private void initializeRestTemplate() {
		restTemplate= new RestTemplate();
		List<HttpMessageConverter<?>> mc = restTemplate.getMessageConverters();
		MappingJacksonHttpMessageConverter json = new MappingJacksonHttpMessageConverter();
		List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();
		supportedMediaTypes.add(new MediaType("text", "javascript"));
		json.setSupportedMediaTypes(supportedMediaTypes);
		mc.add(json);
		restTemplate.setMessageConverters(mc);
	}

}
