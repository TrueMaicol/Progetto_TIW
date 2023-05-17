package controllers;

import beans.User;
import dao.UserDAO;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
    private Connection conn;
    @Override
    public void init() {
        ServletContext context = getServletContext();
        String driver = context.getInitParameter("dbDriver");
        String url = context.getInitParameter("dbUrl");
        String user = context.getInitParameter("dbUser");
        String password = context.getInitParameter("dbPassword");

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url,user, password);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession();
        boolean userError = false, pswError = false, inputError = false;

        String inputErrorText = "", path;
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if(username == null || username.isBlank()) {
            userError = true;
            inputError = true;
            inputErrorText = "Username not set or blank";
        }
        if(password == null || password.isBlank()) {
            pswError = true;
            inputError = true;
            inputErrorText = "Password not set or blank";
        }
        // if there is an error with username or password
        if(inputError) {

            if(userError && pswError)
                inputErrorText = "Username and Password not set or blank";

            session.setAttribute("userError", userError);
            session.setAttribute("userError", pswError);
            session.setAttribute("inputError", inputError);
            session.setAttribute("inputErrorText", inputErrorText);

            path = getServletContext().getContextPath();

            response.sendRedirect(path);

        } else {
            // if the username and password are ok, search for an occurrence
            UserDAO userDAO = new UserDAO(conn);

            try {
                User result = userDAO.checkCredentials(username,password);
                if(result == null) {
                    inputErrorText = "Username, Password combination is incorrect";

                    session.setAttribute("inputError", true);
                    session.setAttribute("userError", true);
                    session.setAttribute("pswError", true);
                    session.setAttribute("inputErrorText", inputErrorText);

                    path = getServletContext().getContextPath();

                    response.sendRedirect(path);

                } else {
                    request.getSession().setAttribute("user",result);
                    path = getServletContext().getContextPath() + "/GoToHome";

                    response.sendRedirect(path);

                }
            } catch (SQLException e) {
                inputErrorText = "Internal server error, try again later";

                session.setAttribute("inputError", false);
                session.setAttribute("userError", false);
                session.setAttribute("pswError", true);
                session.setAttribute("inputErrorText", inputErrorText);

                path = getServletContext().getContextPath();

                response.sendRedirect(path);

            }
        }

    }

    public void destroy() {

    }
}
