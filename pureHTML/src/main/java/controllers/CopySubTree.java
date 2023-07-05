package controllers;

import beans.Category;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
import exceptions.InvalidCategoryException;
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

@WebServlet("/CopySubTree")
public class CopySubTree extends HttpServlet {
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
        HttpSession session = request.getSession();
        Long from, to;
        String path;
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        from = Long.parseLong(request.getParameter("from"));
        to = Long.parseLong(request.getParameter("to"));

        if(from.equals(to)) {
            session.setAttribute("inputErrorCopySubTree", true);
            session.setAttribute("inputErrorTextCopySubTree","Can't copy a category to itself");
            path = getServletContext().getContextPath() + "/GoToHome";
            response.sendRedirect(path);
        }

        try {

            if(!categoryDAO.isCopyPossible(from, to))
                throw new InvalidCategoryException("Copy request denied");
            categoryDAO.copySubTree(from,to);
        } catch (CategoryNotExistsException | TooManyChildrenException | InvalidCategoryException e) {
            session.setAttribute("inputErrorCopySubTree", true);
            session.setAttribute("inputErrorTextCopySubTree",e.getMessage());
        } catch (SQLException e) {
            session.setAttribute("inputErrorCopySubTree", true);
            session.setAttribute("inputErrorTextCopySubTree","Internal server error, try again later");
        } finally {
            path = getServletContext().getContextPath() + "/GoToHome";

            response.sendRedirect(path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    public void destroy() {

    }
}
