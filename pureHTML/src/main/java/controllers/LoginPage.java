package controllers;

import beans.User;
import dao.UserDAO;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@WebServlet("")
public class LoginPage extends HttpServlet {
    private TemplateEngine templateEngine;
    private Connection conn;
    @Override
    public void init() {
        ServletContext context = getServletContext();
        String driver = context.getInitParameter("dbDriver");
        String url = context.getInitParameter("dbUrl");
        String user = context.getInitParameter("dbUser");
        String password = context.getInitParameter("dbPassword");

        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url,user, password);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext context = getServletContext();
        String path = "WEB-INF/index.html";
        final WebContext ctx = new WebContext(request, response, context, request.getLocale());

        HttpSession session = request.getSession();

        if (!session.isNew()) { // if the session has already been initialized
            Boolean inputError = (Boolean) session.getAttribute("inputError");
            if (inputError != null) { // if it set it is because is true

                Boolean userError = (Boolean) session.getAttribute("userError");
                Boolean pswError = (Boolean) session.getAttribute("pswError");
                String inputErrorText = (String) session.getAttribute("inputErrorText");

                session.removeAttribute("inputError");
                session.removeAttribute("inputErrorText");
                session.removeAttribute("userError");
                session.removeAttribute("inputError");

                ctx.setVariable("userError", userError);
                ctx.setVariable("pswError", pswError);
                ctx.setVariable("inputError", inputError);
                ctx.setVariable("inputErrorText", inputErrorText);
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }
        }

        ctx.setVariable("userError", false);
        ctx.setVariable("pswError", false);
        ctx.setVariable("inputError", false);
        ctx.setVariable("inputErrorText", "");


        templateEngine.process(path, ctx, response.getWriter());
    }

   /* @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean userError = false, pswError = false;
        final WebContext ctx;
        String inputError, path;
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        ServletContext context = getServletContext();
        ctx = new WebContext(request, response, context, response.getLocale());
        if(username == null || username.isBlank())
            userError = true;
        if(password == null || password.isBlank())
            pswError = true;
        // if there is an error with username or password
        if(userError || pswError) {
            path = "WEB-INF/index.html";
            inputError = "Username and/or Password not set or blank";
            ctx.setVariable("userError", userError);
            ctx.setVariable("pswError", pswError);
            ctx.setVariable("inputError", true);
            ctx.setVariable("inputErrorText",inputError);
            templateEngine.process(path, ctx, response.getWriter());
        } else {
            // if the username and password are ok, search for an occurrence
            UserDAO userDAO = new UserDAO(conn);
            path = "WEB-INF/index.html";
            try {
                User result = userDAO.checkCredentials(username,password);
                if(result == null) {
                    ctx.setVariable("userError", true);
                    ctx.setVariable("pswError", true);
                    ctx.setVariable("inputError", true);
                    ctx.setVariable("inputErrorText", "Username and Password combination is incorrect");
                    templateEngine.process(path, ctx, response.getWriter());
                } else {
                    request.getSession().setAttribute("user",result);
                    path = getServletContext().getContextPath() + "/GoToHome";
                    System.out.println(path);
                    response.sendRedirect(path);
                    //response.getWriter().append("Servered ");
                }
            } catch (SQLException e) {
                path = "WEB-INF/index.html";
                ctx.setVariable("userError", false);
                ctx.setVariable("pswError", false);
                ctx.setVariable("inputError", true);
                ctx.setVariable("inputErrorText", "The server could not process your request, try again later");
                templateEngine.process(path, ctx, response.getWriter());
            }
        }


        //this.doGet(request,response);
    }*/

    @Override
    public void destroy() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException sqle) {
        }
    }
}
