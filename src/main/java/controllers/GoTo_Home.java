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
public class GoTo_Home extends HttpServlet {
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

        HttpSession session = request.getSession();
        String path = "/WEB-INF/home.html";
        ServletContext servletContext = getServletContext();
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        try {
            ArrayList<Category> topCategories = categoryDAO.getTopCategories();
            ArrayList<Category> allCategories = categoryDAO.getAllCategories();
            ctx.setVariable("topCategories",topCategories);
            ctx.setVariable("allCategories",allCategories);
            String copyFrom = request.getParameter("from");
            if(copyFrom != null) {
                ctx.setVariable("copyTo", true);
                ctx.setVariable("treeToBeCopied",Long.parseLong(copyFrom));
            } else {
                ctx.setVariable("copyTo", false);
            }

            if(!session.isNew()) { // if the session has already been initialized
                Boolean inputError = (Boolean) session.getAttribute("inputError");
                if(inputError != null) { // there has been a problem
                    Boolean nameError = (Boolean) session.getAttribute("nameError");
                    Boolean parentError = (Boolean) session.getAttribute("parentError");
                    String inputErrorText = (String) session.getAttribute("inputErrorText");

                    ctx.setVariable("nameError",nameError);
                    ctx.setVariable("parentError",parentError);
                    ctx.setVariable("inputError",inputError);
                    ctx.setVariable("inputErrorText",inputErrorText);
                    templateEngine.process(path, ctx, response.getWriter());
                    return;
                }
            }

            ctx.setVariable("nameError",false);
            ctx.setVariable("parentError",false);
            ctx.setVariable("inputError",false);
            ctx.setVariable("inputErrorText","");
            templateEngine.process(path, ctx, response.getWriter());

        } catch (SQLException e) {
            ctx.setVariable("serverError",true);
            ctx.setVariable("serverErrorText","Server error");
            templateEngine.process(path, ctx, response.getWriter());
        } catch (CategoryNotExistsException e) {
            ctx.setVariable("serverError",true);
            ctx.setVariable("serverErrorText","Root category does not exist");
            templateEngine.process(path, ctx, response.getWriter());
        }



    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    public void destroy() {

    }
}
