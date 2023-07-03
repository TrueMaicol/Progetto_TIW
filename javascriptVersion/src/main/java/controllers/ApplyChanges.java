package controllers;

import beans.Category;
import beans.ClientCategory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
import exceptions.InvalidCategoryException;
import exceptions.TooManyChildrenException;
import utils.DataToCheck;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/ApplyChanges")
public class ApplyChanges extends HttpServlet {
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
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        BufferedReader reader = request.getReader();
        AddCategoriesRequest changesRequest;
        response.setContentType("application/json");

        System.out.println("/AddCategories");

        try {
            changesRequest = gson.fromJson(reader, AddCategoriesRequest.class);
        } catch (com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
            jsonResponse.addProperty("textError", "The server could not process the request");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
            return;
        }

        DataToCheck dataToCheck = changesRequest.dataToCheck;

        if (dataToCheck.getClientTree() != null && dataToCheck.getOptions() != null) {
            ArrayList<Category> clientTree = new ArrayList<>(List.of(dataToCheck.getClientTree()));
            ArrayList<Category> options = new ArrayList<>(List.of(dataToCheck.getOptions()));
            try {
                if (!categoryDAO.areTreeEqual(clientTree) || !categoryDAO.areOptionsOk(options)) {
                    throw new CategoryNotExistsException("");
                }
            } catch (SQLException | CategoryNotExistsException e) {
                jsonResponse.addProperty("inputErrorText", "Permission denied");
                jsonResponse.addProperty("inputError", true);

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            }
        } else {
            jsonResponse.addProperty("inputErrorText", "Permission denied");
            jsonResponse.addProperty("inputError", true);

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
            return;
        }

        try {
            conn.setAutoCommit(false);
            addCategories(changesRequest.newCategories);
            renameCategories(changesRequest.renamedCategories);
            //renameCategories(...)
            jsonResponse.addProperty("status", "ok");
            conn.commit();
        } catch (TooManyChildrenException | CategoryNotExistsException | InvalidCategoryException e) {
            try {
                conn.rollback();
                jsonResponse.addProperty("textError", e.getMessage());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } catch (SQLException ex) {
                jsonResponse.addProperty("textError", "Internal server error");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
                jsonResponse.addProperty("textError", e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (SQLException ex) {
                jsonResponse.addProperty("textError", "Internal server error");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                jsonResponse = new JsonObject();
                jsonResponse.addProperty("textError", "Internal server error");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
        }
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
        ClientCategory[] newCategories;
        Category[] renamedCategories;
        DataToCheck dataToCheck;
    }


    private Category buildCategoryFromRequest(ClientCategory req) throws TooManyChildrenException {
        Category x = new Category((long) req.ID_Category, req.name, req.num, (long) req.parent, new ArrayList<Category>());
        for (ClientCategory child : req.childrenList) {
            Category y = buildCategoryFromRequest(child);
            x.addNewChildren(y);
        }
        return x;
    }



    private void addCategories(ClientCategory[] newCategories) throws TooManyChildrenException, SQLException, CategoryNotExistsException, InvalidCategoryException {
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        for(int i=0; i<newCategories.length; i++) {
            Category elem = buildCategoryFromRequest(newCategories[i]);
            if(elem.getName() == null || elem.getName().isBlank())
                throw new InvalidCategoryException("Given a name that was not valid");

            Category parent = categoryDAO.getCategoryFromId(elem.getParent());
            categoryDAO.copySubTree(elem, parent);
            /*
                Even though all the categories we are trying to add via this servlet have fake ids the first parent for each set of category has an id that is true!
                If for some reason it happens that a parent is not found then it is thrown a CategoryNotExistsException.
                We are also ensured that the tree we are adding to the database is valid in terms of number of children, this happens with the buildCategoryFromRequest method.
             */
        }
    }

    private void renameCategories(Category[] renamedCategories) throws SQLException, CategoryNotExistsException, InvalidCategoryException {
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        for(Category curr: renamedCategories) {
                /*
                Even though all the categories we are trying to add via this servlet have fake ids the first parent for each set of category has an id that is true!
                If for some reason it happens that a parent is not found then it is thrown a CategoryNotExistsException.
                We are also ensured that the tree we are adding to the database is valid in terms of number of children, this happens with the buildCategoryFromRequest method.
                 */
            if(curr.getNum() == null || curr.getNum().isBlank() || curr.getName() == null || curr.getName().isBlank())
                throw new InvalidCategoryException("Trying to rename a category with empty name");
            Category toBeRenamed = categoryDAO.getCategoryFromNum(curr.getNum());
            Category justUpdated = categoryDAO.renameCategoryById(toBeRenamed.getID_Category(), curr.getName());

        }
    }
}
