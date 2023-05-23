package controllers;

import beans.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.UserDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
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
        boolean userError = false, pswError = false, inputError = false;

        String inputErrorText = "", path;
        BufferedReader reader = request.getReader();
        LoginRequest loginRequest = gson.fromJson(reader, LoginRequest.class);
        String username = loginRequest.username;
        String password = loginRequest.password;

        System.out.println(username);
        System.out.println(password);
        if(username == null || username.isBlank()) {
            userError = true;
            inputError = true;
            inputErrorText = "Username not set or blank";
        }
        if(password == null || password.isBlank()) {
            pswError = true;
            inputError = true;
            inputErrorText = "Password not set or blank";
        }
        // if there is an error with username or password
        if(inputError) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if(userError && pswError)
                inputErrorText = "Username and Password not set or blank";

            jsonResponse.addProperty("userError",userError);
            jsonResponse.addProperty("pswError",pswError);
            jsonResponse.addProperty("inputError",inputError);
            jsonResponse.addProperty("inputErrorText",inputErrorText);

            response.getWriter().write(jsonResponse.toString());
            response.getWriter().flush();

        } else {
            // if the username and password are ok, search for an occurrence
            UserDAO userDAO = new UserDAO(conn);

            try {
                User result = userDAO.checkCredentials(username,password);
                if(result == null) {
                    inputErrorText = "Username, Password combination is incorrect";
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                    jsonResponse.addProperty("userError",true);
                    jsonResponse.addProperty("pswError",true);
                    jsonResponse.addProperty("inputError",true);
                    jsonResponse.addProperty("inputErrorText",inputErrorText);

                    response.getWriter().write(jsonResponse.toString());
                    response.getWriter().flush();

                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    jsonResponse.addProperty("username",result.getUsername());

                    response.getWriter().write(jsonResponse.toString());
                    response.getWriter().flush();
                }
            } catch (SQLException e) {
                inputErrorText = "Internal server error, try again later";
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                jsonResponse.addProperty("userError",false);
                jsonResponse.addProperty("pswError",false);
                jsonResponse.addProperty("inputError",true);
                jsonResponse.addProperty("inputErrorText",inputErrorText);

                response.getWriter().write(jsonResponse.toString());
                response.getWriter().flush();

            }
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

    private class LoginRequest {
        public String username, password;

    }
}
