package org.forgerock.commons.ui.functionaltests.openidmclient;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.*;
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
	
	HttpHeaders getAllUsersHeaders = new HttpHeaders();
	HttpHeaders deleteUserHeaders = new HttpHeaders();
	HttpHeaders createUserHeaders = new HttpHeaders();
	
	public OpenIDMClient() {
		initializeHeaders();
	}
	
	public String createUser(JsonNode user) {
		HttpEntity<JsonNode> requestEntity = new HttpEntity<JsonNode>(user, createUserHeaders);
		
		ResponseEntity<EntityBaseInfo> response = getRestTemplate().exchange(constants.getOpenIDMServer() + "openidm/managed/user/?_action=create",
			      HttpMethod.POST, requestEntity, EntityBaseInfo.class);
		
		EntityBaseInfo result = response.getBody();
		if (result != null && result.getError() != null) {
			throw new IllegalStateException("An error occured: " + result.getError() + "\nDetails: " + result.getReason());
		}
		
		return result.get_id();
	}
	
	/**
	 * @param cookie like JSESSIONID=ixnekr105coj11ji67xcluux8
	 */
	public String loginAndReturnCookie(String userName, String password) {
		try {
			ResponseEntity<EntityBaseInfoArray> response = getRestTemplate().exchange(constants.getOpenIDMServer() + "openidm/info/login",
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
		ResponseEntity<BaseResult> response = getRestTemplate().exchange(constants.getOpenIDMServer() + "openidm/managed/user/" + entityId,
			      HttpMethod.DELETE, new HttpEntity<byte[]>(deleteUserHeaders), BaseResult.class);
		BaseResult result = response.getBody();
		if (result != null && result.getError() != null) {
			throw new IllegalStateException("An error occured: " + result.getError() + "\nDetails: " + result.getReason());
		}
	}
	
	public void updateUserField(String userName, String fieldName, String fieldValue) {
		ArrayNode arrayNode  = JsonNodeFactory.instance.arrayNode();
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("replace", "/" + fieldName);
		node.put("value", fieldValue);
		arrayNode.add(node);
		
		HttpEntity<JsonNode> requestEntity = new HttpEntity<JsonNode>(arrayNode, getAllUsersHeaders);
		
		ResponseEntity<BaseResult> response = getRestTemplate().exchange(constants.getOpenIDMServer() + "openidm/managed/user?_action=patch&_query-id=for-userName&uid=" + userName,
			      HttpMethod.POST, requestEntity, BaseResult.class);
		BaseResult result = response.getBody();
		if (result != null && result.getError() != null) {
			throw new IllegalStateException("An error occured: " + result.getError() + "\nDetails: " + result.getReason());
		}
	}

	public EntityBaseInfo[] getAllUsers() {
		ResponseEntity<EntityBaseInfoArray> response = getRestTemplate().exchange(constants.getOpenIDMServer() + "openidm/managed/user/?_query-id=query-all-ids",
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
	
	private void initializeHeaders() {
		
		getAllUsersHeaders.clear();
		getAllUsersHeaders.add("X-OpenIDM-Username", "openidm-admin");
		getAllUsersHeaders.add("X-OpenIDM-Password", "openidm-admin");
		getAllUsersHeaders.add("X-OpenIDM-NoSession", "true");
		
		deleteUserHeaders.clear();
		deleteUserHeaders.add("X-OpenIDM-Username", "openidm-admin");
		deleteUserHeaders.add("X-OpenIDM-Password", "openidm-admin");
		deleteUserHeaders.add("If-Match", "*");
		
		createUserHeaders.clear();
		createUserHeaders.add("X-OpenIDM-Username", "anonymous");
		createUserHeaders.add("X-OpenIDM-Password", "anonymous");
		createUserHeaders.add("X-OpenIDM-NoSession", "true");
	}

	public RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		List<HttpMessageConverter<?>> mc = restTemplate.getMessageConverters();
		MappingJacksonHttpMessageConverter json = new MappingJacksonHttpMessageConverter();
		List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();
		supportedMediaTypes.add(new MediaType("text", "javascript"));
		json.setSupportedMediaTypes(supportedMediaTypes);
		mc.add(json);
		restTemplate.setMessageConverters(mc);
		return restTemplate;
	}

}
