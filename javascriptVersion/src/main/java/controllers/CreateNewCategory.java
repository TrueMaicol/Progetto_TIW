package controllers;

import beans.Category;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
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
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();
        String name, inputErrorText = "";
        Long parent;
        CategoryDAO categoryDAO = new CategoryDAO(conn);
        NewCategoryRequest newCategoryRequest;
        response.setContentType("application/json");
        System.out.println("/CreateNewCategory");
        BufferedReader reader = request.getReader();

        try {
            newCategoryRequest = gson.fromJson(reader, NewCategoryRequest.class);
        } catch (com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
            jsonResponse.addProperty("textError", "The server could not process the request");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
            return;
        }


        DataToCheck dataToCheck = newCategoryRequest.dataToCheck;

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

        name = newCategoryRequest.newCategory.name;
        parent = newCategoryRequest.newCategory.parent;
        boolean inputError = false, nameError = false, parentError = false;

        if(name == null || name.isBlank()) {
            nameError = true;
            inputError = true;
            inputErrorText = "Name not set or blank";
        }
        if(parent == null || parent <= 0) {
            parentError = true;
            inputError = true;
            inputErrorText = "Parent not set or does not exist";
        }

        if(inputError) {
            if(nameError && parentError)
                inputErrorText = "Name and Parent are not valid";
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.addProperty("nameError",nameError);
            jsonResponse.addProperty("parentError",parentError);
            jsonResponse.addProperty("inputErrorNewCategory",inputError);
            jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
        } else {
            try {
                conn.setAutoCommit(false);
                Category justCreated = categoryDAO.createCategory(name,parent);
                response.setStatus(HttpServletResponse.SC_OK);

                if(justCreated == null) // something went wrong in the DAO (in theory it can't happen)
                    throw new SQLException();

                response.setStatus(HttpServletResponse.SC_OK);
                jsonResponse = (JsonObject) gson.toJsonTree(justCreated);
                conn.commit();
                //if everything is ok then don't set any attributes
            } catch (SQLException e) {
                try {
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    inputErrorText = "Internal server error, try again later";
                    jsonResponse.addProperty("nameError",false);
                    jsonResponse.addProperty("parentError",false);
                    jsonResponse.addProperty("inputErrorNewCategory",true);
                    jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
                } catch (SQLException ex) {
                    inputErrorText = "Internal server error, try again later";
                    jsonResponse.addProperty("nameError",false);
                    jsonResponse.addProperty("parentError",false);
                    jsonResponse.addProperty("inputErrorNewCategory",true);
                    jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
                }

            } catch (TooManyChildrenException e) {
                try {
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    inputErrorText = "Selected parent has too many children (max 9)";
                    jsonResponse.addProperty("nameError",false);
                    jsonResponse.addProperty("parentError",true);
                    jsonResponse.addProperty("inputErrorNewCategory",true);
                    jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
                } catch (SQLException ex) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    inputErrorText = "Internal server error";
                    jsonResponse.addProperty("nameError",false);
                    jsonResponse.addProperty("parentError",false);
                    jsonResponse.addProperty("inputErrorNewCategory",true);
                    jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
                }
            } catch (CategoryNotExistsException e) {
                try {
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    inputErrorText = "Selected parent does not exist";
                    jsonResponse.addProperty("nameError",false);
                    jsonResponse.addProperty("parentError",true);
                    jsonResponse.addProperty("inputErrorNewCategory",true);
                    jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
                } catch (SQLException ex) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    inputErrorText = "Internal server error";
                    jsonResponse.addProperty("nameError",false);
                    jsonResponse.addProperty("parentError",false);
                    jsonResponse.addProperty("inputErrorNewCategory",true);
                    jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
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

    public void destroy() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class NewCategoryRequest {
        DataToCheck dataToCheck;
        NewCategory newCategory;
    }

    private class NewCategory {
        public String name;
        public Long parent;
        public Options[] options;
    }

    private class Options {
        public long ID_Category;
        public String name;
        public String num;
    }
}
