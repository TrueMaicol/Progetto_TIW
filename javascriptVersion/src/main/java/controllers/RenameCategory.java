package controllers;

import beans.Category;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
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
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        BufferedReader reader = request.getReader();
        RenameRequest renameRequest;
        response.setContentType("application/json");
        System.out.println("/RenameCategory");
        try {
            renameRequest = gson.fromJson(reader, RenameRequest.class);
        } catch (com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
            jsonResponse.addProperty("textError", "The server could not process the request");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
            return;
        }

        DataToCheck dataToCheck = renameRequest.dataToCheck;

        if(dataToCheck.getClientTree() != null && dataToCheck.getOptions() != null) {
            ArrayList<Category> clientTree = new ArrayList<>(List.of(dataToCheck.getClientTree()));
            ArrayList<Category> options = new ArrayList<>(List.of(dataToCheck.getOptions()));
            try {
                if(!categoryDAO.areTreeEqual(clientTree) || !categoryDAO.areOptionsOk(options)) {
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

        RenamedCategory renamedCategory = renameRequest.renamed;

        if(renamedCategory.ID_Category == 1) {
            jsonResponse.addProperty("textError", "Can't rename the root");

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else if(renamedCategory.name == null || renamedCategory.name.isBlank()) {
            jsonResponse.addProperty("textError", "The new name is invalid");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            try {
                conn.setAutoCommit(false);
                Category justUpdated = categoryDAO.renameCategoryById(renamedCategory.ID_Category, renamedCategory.name);
                response.setStatus(HttpServletResponse.SC_OK);

                jsonResponse = (JsonObject) gson.toJsonTree(justUpdated);
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                }
                catch (SQLException ex) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    jsonResponse.addProperty("textError", "Internal server error");
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonResponse.addProperty("textError","Internal server error");
            } catch (CategoryNotExistsException e) {
                try {
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    jsonResponse.addProperty("textError", "Given ID does not relate to any category!");
                }
                catch (SQLException ex) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    jsonResponse.addProperty("textError", "Internal server error");
                }
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    jsonResponse = new JsonObject();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    jsonResponse.addProperty("textError","Internal server error");
                }
            }
        }
        response.getWriter().write(jsonResponse.toString());
        response.getWriter().flush();
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
        public RenamedCategory renamed;
        public DataToCheck dataToCheck;
    }

    private class RenamedCategory {
        public String name;
        public Long ID_Category;
    }
}
