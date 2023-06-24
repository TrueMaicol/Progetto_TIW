package controllers;

import beans.Category;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
import exceptions.TooManyChildrenException;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet("/AddCategories")
public class AddCategories extends HttpServlet {
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
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        BufferedReader reader = request.getReader();
        AddCategoriesRequest[] addRequest = gson.fromJson(reader, AddCategoriesRequest[].class);
        response.setContentType("application/json");
        System.out.println("/AddCategories");
        for(AddCategoriesRequest curr : addRequest) {
            try {
                Category elem = buildCategoryFromRequest(curr);
                Category parent = categoryDAO.getCategoryFromId(elem.getParent());
                categoryDAO.insertCategoryWithChildren(elem, parent);
            } catch (TooManyChildrenException e) {
                jsonResponse.addProperty("textError", "Too many children!");

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            } catch (SQLException e) {
                jsonResponse.addProperty("textError", "Internal server error, try again later!");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            } catch (CategoryNotExistsException e) {
                jsonResponse.addProperty("textError", "Requested an id that does not exist!");

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            }
        }

        jsonResponse.addProperty("status","ok");
        response.getWriter().write(jsonResponse.toString());
        response.getWriter().flush();
    }

    public void destroy() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class AddCategoriesRequest {
        public double ID_Category;
        public String name;
        public String num;
        public double parent;
        public AddCategoriesRequest[] childrenList;
    }

    private Category buildCategoryFromRequest(AddCategoriesRequest req) throws TooManyChildrenException {
        Category x = new Category((long) req.ID_Category, req.name, req.num, (long) req.parent, new ArrayList<Category>());
        for (AddCategoriesRequest child : req.childrenList) {
            Category y = buildCategoryFromRequest(child);
            x.addNewChildren(y);
        }
        return x;
    }
}
