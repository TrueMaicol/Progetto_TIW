package controllers;

import beans.Category;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.CategoryDAO;
import exceptions.CategoryNotExistsException;
import exceptions.TooManyChildrenException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;


import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
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
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();
        String name, inputErrorText = "";
        Long parent;

        BufferedReader reader = request.getReader();
        NewCategoryRequest newCategoryRequest = gson.fromJson(reader, NewCategoryRequest.class);
        response.setContentType("application/json");
        name = newCategoryRequest.name;
        parent = newCategoryRequest.parent;

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

            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();
        } else {
            CategoryDAO categoryDAO = new CategoryDAO(conn);
            try {
                Category justCreated = categoryDAO.createCategory(name,parent);
                response.setStatus(HttpServletResponse.SC_OK);

                if(justCreated == null) // something went wrong in the DAO (in theory it can't happen)
                    throw new SQLException();

                jsonResponse = (JsonObject) gson.toJsonTree(justCreated);

                //if everything is ok then don't set any attributes
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                inputErrorText = "Internal server error, try again later";
                jsonResponse.addProperty("nameError",false);
                jsonResponse.addProperty("parentError",false);
                jsonResponse.addProperty("inputErrorNewCategory",true);
                jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
            } catch (TooManyChildrenException e) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                inputErrorText = "Selected parent has too many children (max 9)";
                jsonResponse.addProperty("nameError",false);
                jsonResponse.addProperty("parentError",true);
                jsonResponse.addProperty("inputErrorNewCategory",true);
                jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
            } catch (CategoryNotExistsException e) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                inputErrorText = "Selected parent does not exist";
                jsonResponse.addProperty("nameError",false);
                jsonResponse.addProperty("parentError",true);
                jsonResponse.addProperty("inputErrorNewCategory",true);
                jsonResponse.addProperty("inputErrorTextNewCategory",inputErrorText);
            } finally {
                System.out.println(jsonResponse.toString());
                response.getWriter().flush();
                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();
            }
        }
    }

    public void destroy() {

    }

    private class NewCategoryRequest {
        public String name;
        public Long parent;
    }
}
