package wasdev.sample.servlet;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SimpleServlet
 */
@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = "User";
        String environment = "somewhere";

        response.setContentType("text/html");

        Object jndiConstant;

        try {
            jndiConstant = new InitialContext().lookup("username");
            username = (String) jndiConstant;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            jndiConstant = new InitialContext().lookup("env");
            environment = (String) jndiConstant;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //response.getWriter().print("Hello " + username + ", you're viewing the application in " + environment + " environment!");
        response.getWriter().print("Hello A1B2C3, you're viewing the application in " + environment + " environment!");
    }
}
