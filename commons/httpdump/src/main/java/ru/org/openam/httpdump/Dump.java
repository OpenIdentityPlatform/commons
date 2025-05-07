/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2020-2024 3A Systems LLC.
 */
package ru.org.openam.httpdump;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.org.openam.geo.Client;

public class Dump {
	final static Logger logger = LoggerFactory.getLogger(Dump.class.getName());
	
	public static String toString(ServletRequest request){
		return toString((HttpServletRequest)request);
	}
	
	public static String toString(HttpServletRequest request) {
		try{
			BufferedRequestWrapper requestWithBody=(BufferedRequestWrapper)request.getAttribute(BufferedRequestWrapper.class.getName());
			return MessageFormat.format("{0}: {1} {2}?{3} {4} Params: {5} {6}", 
					new Object[]{
						Client.get(request),
						request.getMethod(),
						request.getAttribute("javax.servlet.forward.request_uri")==null?request.getRequestURI():(String)request.getAttribute("javax.servlet.forward.request_uri"),
						request.getQueryString(),
						dump_getHeaders(request),
						dump_getParameterMap(request),
						(requestWithBody!=null)?requestWithBody.getRequestBody():"<not inspected>"
			});
		}catch(Throwable e){
			logger.error("toString",e);
		}
		return request.toString();
	}
	
	@SuppressWarnings("unchecked")
	static String dump_getHeaders(HttpServletRequest request){
		String res="";
		Enumeration<String> headers=request.getHeaderNames();
		while(headers.hasMoreElements()){
			String header=headers.nextElement();
			String vres="";
			Enumeration<String> values=request.getHeaders(header);
			while(values.hasMoreElements())
				vres=vres.concat(MessageFormat.format("[{0}];", values.nextElement()));
			res=res.concat(MessageFormat.format("{0}: {1} ", new Object[]{header,vres}));
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	static String dump_getParameterMap(HttpServletRequest request){
		String res="";
		Enumeration<String> params=request.getParameterNames();
		while(params.hasMoreElements()){
			final String param=params.nextElement();
			res=res.concat(MessageFormat.format("{0}={1}; ", new Object[]{param,(request.getParameterValues(param)==null)?null:Arrays.asList(request.getParameterValues(param))}));
		}
		return res;
	}

}