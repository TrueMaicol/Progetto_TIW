package controllers;

import beans.Category;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;

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

@WebServlet("/RenameCategoryasdasda")
public class RenameCategory_OLD extends HttpServlet {
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

        BufferedReader reader = request.getReader();
        RenameRequest renameRequest = gson.fromJson(reader, RenameRequest.class);
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        if(renameRequest.clientTree == null || renameRequest.clientTree.length == 0 || renameRequest.categories == null || renameRequest.mode == null) {
            jsonResponse.addProperty("textError", "Permission denied");

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
            return;
        }

        ArrayList<Category> clientTree = new ArrayList<>(List.of(renameRequest.clientTree));

        try {
            if(!categoryDAO.areTreeEqual(clientTree)) {
                jsonResponse.addProperty("textError", "Something is wrong... refresh the page!");

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            }

        } catch (SQLException | CategoryNotExistsException e) {
            jsonResponse.addProperty("textError", "Internal server error, please try again!");

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
            return;
        }

        if(renameRequest.mode == Mode.ID)
            this.renameCategoryFromId(renameRequest.categories, response);
        else if(renameRequest.mode == Mode.NUM)
            this.renameCategoryFromNum(renameRequest.categories, response);

    }

    @Override
    public void destroy() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class RenameRequest {
        public Category[] clientTree;
        public Category[] categories;
        public Mode mode;
    }
    private enum Mode {NUM, ID}

    private void renameCategoryFromNum(Category[] categories, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonArray jsonArray = new JsonArray();
        System.out.println("/RenameCategoryNum");

        for(Category curr : categories) {
            if(curr.getNum() == null || curr.getNum().isBlank()) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.addProperty("textError", "Could not find the category to be renamed!");

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
                return;
            } else {
                CategoryDAO categoryDAO = new CategoryDAO(conn);
                try {

                    Category toBeRenamed = categoryDAO.getCategoryFromNum(curr.getNum());
                    Category justUpdated = categoryDAO.renameCategoryById(toBeRenamed.getID_Category(), curr.getName());

                    JsonObject jsonResponse = (JsonObject) gson.toJsonTree(justUpdated);
                    jsonArray.add(jsonResponse);

                } catch (SQLException e) {
                    JsonObject jsonResponse = new JsonObject();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                    jsonResponse.addProperty("textError","Internal server error, try again later!");
                    response.getWriter().write(jsonResponse.toString());
                    response.getWriter().flush();
                    return;
                } catch (CategoryNotExistsException e) {
                    JsonObject jsonResponse = new JsonObject();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    jsonResponse.addProperty("textError", "Cannot find the category to rename!");
                    response.getWriter().write(jsonResponse.toString());
                    response.getWriter().flush();
                    return;
                }
                response.getWriter().write(gson.toJson(jsonArray));
                response.getWriter().flush();
            }
        }
    }
    private void renameCategoryFromId(Category[] categories, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();
        System.out.println("/RenameCategoryID");

        for(Category category : categories) {
            if(category.getID_Category() <= 0) {
                jsonResponse.addProperty("textError", "Category ID is invalid");

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();

            } else if(category.getName() == null || category.getName().isBlank()) {
                jsonResponse.addProperty("textError", "The new name is invalid");

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
            } else {
                CategoryDAO categoryDAO = new CategoryDAO(conn);

                try {
                    Category justUpdated = categoryDAO.renameCategoryById(category.getID_Category(), category.getName());
                    response.setStatus(HttpServletResponse.SC_OK);

                    jsonResponse = (JsonObject) gson.toJsonTree(justUpdated);

                } catch (SQLException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    jsonResponse.addProperty("textError","Internal server error, try again later!");
                } catch (CategoryNotExistsException e) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    jsonResponse.addProperty("textError", "Given ID does not relate to any category!");
                } finally {
                    response.getWriter().write(jsonResponse.toString());
                    response.getWriter().flush();
                }
            }
        }

    }
}
