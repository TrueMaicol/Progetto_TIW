package controllers;

import beans.Category;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@WebServlet("/GetTree")
public class GetTree extends HttpServlet {
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
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();
        try {
            Category tree = categoryDAO.getCategoryFromId(1);
            String jsonTree = gson.toJson(tree);

            response.getWriter().write(jsonTree);
            response.getWriter().flush();
            System.out.println("/GetTree");

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.addProperty("textError", "Internal server error, could not load the tree, try again later!");

            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
        } catch (CategoryNotExistsException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("textError", "Internal server error, could not load the tree, try again later!");

            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

}
