package org.psychomechanics.iot.arduino_recv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.sonatype.plexus.components.cipher.Base64;

/**
 * Servlet implementation class ArduinoRecvServlet
 */
@WebServlet("/ArduinoRecvServlet")
/*@ServletSecurity(
        value = @HttpConstraint( rolesAllowed = { "tomcat" }),
        httpMethodConstraints = {	@HttpMethodConstraint(value = "GET", rolesAllowed = "tomcat"),
        							@HttpMethodConstraint(value = "POST", rolesAllowed = { "tomcat" })
        })*/
@ServletSecurity(@HttpConstraint( rolesAllowed = "tomcat" ))

public class ArduinoRecvServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String curPos = "47.117075,10.541476,2.324170";

	private String id;
	private String key;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ArduinoRecvServlet() {
		super();
	}

	/**
	 * Usage: http://<your-servers-ip-address>:<port>/arduin_recv/ArduinoRecvServlet?GPIO0=123
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/*
		 * Retrieve user (ID) and password (KEY) from the "Authorization" header (Basic Auth)
		 */
		final String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
			// Authorization: Basic base64credentials
			String base64Credentials = authorization.substring("Basic".length()).trim();
			byte[] credDecoded = Base64.decodeBase64(base64Credentials.getBytes());
			String credentials = new String(credDecoded, StandardCharsets.UTF_8);
			// credentials = username:password
			final String[] values = credentials.split(":", 2);
			id = values[0];
			key = values[1];
		}

		/*
		 * Check whether a session already exists (the user has successfully logged in before)
		 */
		boolean loggedIn = false;
		HttpSession session = request.getSession(false);
		if(session == null) {
			// TODO: check user and password against database table users
			session = request.getSession(true);
			loggedIn = true;
		}
		// TODO: Remove next line and make sure smartphone gets logged in
		loggedIn = true;

		if(loggedIn) {
			Path FILE_PATH = Paths.get(getServletContext().getRealPath("/"), "temp.txt");
			String lat = request.getParameter("lat");;
			String lng = request.getParameter("lng");
			String alt = request.getParameter("alt");

			if(lat == null && lng == null & alt == null) {
				response.getWriter().append(curPos);

			} else {
				curPos = lat + "," + lng + "," + alt;

				String query = "INSERT INTO coords ("
						+ " pk,"
						+ " id,"
						+ " lat,"
						+ " lng,"
						+ " alt ) VALUES (null, ?, ?, ?, ?)";

				ServletContext ctx = this.getServletContext();
				DBConnectionManager dbManager = (DBConnectionManager) ctx.getAttribute("DBManager");
				Connection connection = dbManager.getConnection();

				try {
					PreparedStatement st = connection.prepareStatement(query);
					st.setString(1, id);
					st.setString(2, lat);
					st.setString(3, lng);
					st.setString(4, alt);		    
					st.executeUpdate();
					st.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//Writing to the file temp.txt
				try (BufferedWriter writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8,
						Files.exists(FILE_PATH) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE)) {
					writer.write(ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
					writer.write(curPos + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				response.getWriter().append(curPos);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
}