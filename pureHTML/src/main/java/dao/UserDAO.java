package dao;

import beans.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class UserDAO {

    private Connection conn;

    public UserDAO(Connection conn) {
        this.conn = conn;
    }

    public ArrayList<User> getAllUsers() {
        ArrayList<User> users = new ArrayList<>();
        String query = "select ID_User,username from user";
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet result = preparedStatement.executeQuery();
            while(result.next()) {
                User user = new User(result.getInt("ID_User"), result.getString("username"));
                users.add(user);
            }
        } catch(SQLException e) {
            throw new RuntimeException("Error during SQL operation");
        }
        return users;
    }

    public User checkCredentials(String username, String psw) throws SQLException {
        String query = "select ID_User,username from user where username = ? AND psw = ?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setString(1,username);
        preparedStatement.setString(2,psw);
        ResultSet result = preparedStatement.executeQuery();
        if(!result.isBeforeFirst()) { // no user with given credentials found
            return null;
        } else {
            result.next();
            User foundUser = new User(result.getInt("ID_User"),result.getString("username"));
            return foundUser;
        }
    }
}
