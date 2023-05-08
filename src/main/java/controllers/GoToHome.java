package controllers;

import beans.Category;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
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

@WebServlet("/GoToHome")
public class GoToHome extends HttpServlet {
    private Connection conn;
    private TemplateEngine templateEngine;
    @Override
    public void init() {
        ServletContext context = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");

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
        /*
            Login checks done using Filters
         */


        String path = "/WEB-INF/home.html";
        ServletContext servletContext = getServletContext();
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        try {
            ArrayList<Category> topCategories = categoryDAO.getTopCategories();
            ArrayList<Category> allCategories = categoryDAO.getAllCategories();
            ctx.setVariable("topCategories",topCategories);
            ctx.setVariable("allCategories",allCategories);
        } catch (SQLException e) {
            // send to the page error codes
        } catch (CategoryNotExistsException e) {
            // send to the page error codes
        }


        templateEngine.process(path, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    public void destroy() {

    }
}
