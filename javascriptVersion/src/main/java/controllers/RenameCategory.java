package controllers;

import beans.Category;
import com.google.gson.Gson;
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

@WebServlet("/RenameCategory")
public class RenameCategory extends HttpServlet {
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

        BufferedReader reader = request.getReader();
        RenameRequest renameRequest = gson.fromJson(reader, RenameRequest.class);
        response.setContentType("application/json");

        if(renameRequest.ID_Category <= 0) {
            jsonResponse.addProperty("textError", "Category ID is invalid");

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();

        } else if(renameRequest.newName.isBlank()) {
            jsonResponse.addProperty("textError", "The new name is invalid");

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
        } else {
            CategoryDAO categoryDAO = new CategoryDAO(conn);

            try {
                Category justUpdated = categoryDAO.renameCategoryById(renameRequest.ID_Category, renameRequest.newName);
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

    @Override
    public void destroy() {

    }

    private class RenameRequest {
        public String newName;
        public Long ID_Category;
    }
}
