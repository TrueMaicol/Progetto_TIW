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
import java.util.ArrayList;

@WebServlet("/ListOfUsers")
public class ListOfUsers extends HttpServlet {
    private Connection conn;
    private TemplateEngine templateEngine;

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
        ArrayList<User> users;
        UserDAO userDAO = new UserDAO(conn);
        users = userDAO.getAllUsers();
        System.out.println(users.get(0).getUsername());
        String path = "/WEB-INF/home.html";
        ServletContext servletContext = getServletContext();
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        ctx.setVariable("users", users);
        templateEngine.process(path, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    public void destroy() {
        try {
            if(conn != null)
                conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
