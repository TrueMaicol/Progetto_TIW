package controllers;

import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
import exceptions.TooManyChildrenException;
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

@WebServlet("/CreateNewCategory")
public class CreateNewCategory extends HttpServlet {
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
        String name, parent, inputErrorText = "", path;
        name = request.getParameter("name");
        parent = request.getParameter("parent");
        boolean inputError = false, nameError = false, parentError = false;
        if(name == null || name.isBlank()) {
            nameError = true;
            inputError = true;
            inputErrorText = "Name not set or blank";
        }
        if(parent == null || parent.isBlank()) {
            parentError = true;
            inputError = true;
            inputErrorText = "Parent not set or blank";
        }

        if(inputError) {
            if(nameError && parentError)
                inputErrorText = "Name and Parent not set or blank";

            session.setAttribute("nameError",nameError);
            session.setAttribute("parentError",parentError);
            session.setAttribute("inputErrorNewCategory",inputError);
            session.setAttribute("inputErrorTextNewCategory",inputErrorText);

            path = getServletContext().getContextPath() + "/GoToHome";

            response.sendRedirect(path);
        } else {
            CategoryDAO categoryDAO = new CategoryDAO(conn);
            try {
                categoryDAO.createCategory(name,Long.parseLong(parent));
                //if everything is ok then don't set any attributes
            } catch (SQLException e) {
                inputErrorText = "Internal server error, try again later";
                session.setAttribute("nameError",false);
                session.setAttribute("parentError",false);
                session.setAttribute("inputErrorNewCategory",true);
                session.setAttribute("inputErrorTextNewCategory",inputErrorText);
            } catch (TooManyChildrenException e) {
                inputErrorText = "Selected parent has too many children (max 9)";
                session.setAttribute("nameError",false);
                session.setAttribute("parentError",true);
                session.setAttribute("inputErrorNewCategory",true);
                session.setAttribute("inputErrorTextNewCategory",inputErrorText);
            } catch (CategoryNotExistsException e) {
                inputErrorText = "Selected parent does not exist";
                session.setAttribute("nameError",false);
                session.setAttribute("parentError",true);
                session.setAttribute("inputErrorNewCategory",true);
                session.setAttribute("inputErrorTextNewCategory",inputErrorText);
            } finally {
                path = getServletContext().getContextPath() + "/GoToHome";

                response.sendRedirect(path);
            }
        }
    }

    public void destroy() {

    }
}
