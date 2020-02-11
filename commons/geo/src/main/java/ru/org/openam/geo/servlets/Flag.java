package ru.org.openam.geo.servlets;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
