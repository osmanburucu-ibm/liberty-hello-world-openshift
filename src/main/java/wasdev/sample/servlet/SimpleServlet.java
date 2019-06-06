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
        String key1val = "Default Key 1 Value";
        String key2val = "Default Key 2 Value";
        String username = "User";
        String environment = "Unknown";

        response.setContentType("text/html");

        try {
            Object jndiConstant = new InitialContext().lookup("key1");
            key1val = (String) jndiConstant;

            jndiConstant = new InitialContext().lookup("key2");
            key2val += (String) jndiConstant;

            jndiConstant = new InitialContext().lookup("username");
            username += (String) jndiConstant;

            jndiConstant = new InitialContext().lookup("env");
            environment += (String) jndiConstant;


        } catch (Exception e) {
            e.printStackTrace();
        }

        response.getWriter().print("Hello " + username + " !");
        response.getWriter().print("<table>");
        response.getWriter().print("<tr><td>env</td><td>" + environment + "</td></tr>");
        response.getWriter().print("<tr><td>key1</td><td>" + key1val + "</td></tr>");
        response.getWriter().print("<tr><td>key1</td><td>" + key2val + "</td></tr>");
        response.getWriter().print("</table>");
    }
}
