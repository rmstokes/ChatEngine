package HttpReqHandler;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet
public class HttpReqHandlerClass extends HttpServlet {
	final String pass = "RRzmC6C2bQEZqwZXNL3h9s9zw8Q7FHaBEXrrWS4FvbrCRygZcekgeUwJtNCNH33f";
	
	
	public HttpReqHandlerClass() throws UnsupportedEncodingException {
		
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, java.io.IOException{
		
		String typeParam = req.getParameter("type");
		String passParam = req.getParameter("pass");
		
		System.out.println("received GET request for log");
		System.out.println(typeParam);
		System.out.println(passParam);
		
		
		if (pass.equals(passParam)) {
			System.out.println("Good Pass");
		} else {
			System.out.println("Bad Pass");
		}
		
		
	}
	
	

}
