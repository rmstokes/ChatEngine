package websocket.chat;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet
public class HttpReqHandlerOrig extends HttpServlet {
	final String pass = "RRzmC6C2bQEZqwZXNL3h9s9zw8Q7FHaBEXrrWS4FvbrCRygZcekgeUwJtNCNH33f";
	
	
	public HttpReqHandlerOrig() throws UnsupportedEncodingException {
		
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException{
		System.out.println("received GET request for log");
		
		/*System.out.println(req.getParameter(pass));
		if (req.getParameter(pass) == pass) {
			System.out.println("Good Pass");
		} else {
			System.out.println("Bad Pass");
		}*/
		
		
	}
	
	

}
