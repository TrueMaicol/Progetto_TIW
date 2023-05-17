package controllers;

import beans.Category;
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
        String from, to, path;
        Category fromCategory, toCategory;
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        from = request.getParameter("from");
        to = request.getParameter("to");

        try {
            categoryDAO.copySubTree(Long.parseLong(from),Long.parseLong(to));
        } catch (NumberFormatException e) {
            session.setAttribute("inputErrorCopySubTree", true);
            session.setAttribute("inputErrorTextCopySubTree","Could not identify which sub tree to copy");
        } catch (CategoryNotExistsException | TooManyChildrenException e) {
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
