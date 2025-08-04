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

package ru.org.openam.geo.servlets;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class Flag extends HttpServlet {
	private static final long serialVersionUID = -4911401913146825227L;

	public Flag() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo()==null||!request.getPathInfo().matches("/[a-z]{2}\\.png"))
			response.sendError(404);
		else{
			URL url = Flag.class.getResource("/flag_icons".concat(request.getPathInfo()));
			if (url==null) 
				response.sendError(404);
			else{
				URLConnection con=url.openConnection();
				response.setHeader("Cache-Control", "public");
				response.setContentType(getServletContext().getMimeType(request.getPathInfo()));
				response.setContentLength(con.getContentLength());
				response.setDateHeader("Last-Modified", con.getLastModified());
				if ("GET".equalsIgnoreCase(request.getMethod()))
					IOUtils.copy(con.getInputStream(), response.getOutputStream());
			}
		}
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
