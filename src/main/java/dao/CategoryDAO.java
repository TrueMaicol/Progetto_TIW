package dao;

import beans.Category;
import exceptions.CategoryNotExistsException;
import exceptions.TooManyChildrenException;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class CategoryDAO {

    private Connection conn;
    public CategoryDAO(Connection conn) {
        this.conn = conn;
    }

    public ArrayList<Category> getTreeFromId(long ID_parent) throws SQLException {
        // create a temporary table treeById used to contain all the discendent of the category with ID_Category = ID_parent
        ArrayList<Category> tree = new ArrayList<Category>();
        String query = "WITH RECURSIVE treeById (ID_Category, name, num, parent) as (" +
                "select ID_Category, name, num, parent from category where ID_Category = ? UNION ALL " +
                "select c.ID_Category, c.name, c.num, c.parent from treeById as tree JOIN category as c ON tree.ID_Category = c.parent)" +

                "SELECT t.ID_Category, t.name, t.num, t.parent FROM treeById as t;";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setLong(1,ID_parent);
        ResultSet result = preparedStatement.executeQuery();
        while(result.next()) {
            Category temp = new Category(
                    result.getLong("ID_Category"),
                    result.getString("name"),
                    result.getString("num"),
                    result.getLong("parent")
            );
            tree.add(temp);
        }
        return tree;
    }

    public Category getCategoryFromId(long ID_Category) throws SQLException {
        String query = "SELECT * FROM Category WHERE ID_Category = ?";
        System.out.println("Requested category " + Long.toString(ID_Category));
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setLong(1,ID_Category);
        ResultSet result = preparedStatement.executeQuery();
        if(!result.isBeforeFirst()) { // no category has been found
            return null;
        } else {
            result.next();
            Category category = new Category(
                    result.getLong("ID_Category"),
                    result.getString("name"),
                    result.getString("num"),
                    result.getLong("parent")
                    );
            return category;
        }
    }
    public int countDirectChildrenOf(long ID_Category) throws SQLException, CategoryNotExistsException {
        String checkQuery = "select * from category where ID_Category = ?";
        String countQuery = "select count(*) as num from category where parent = ?";
        PreparedStatement checkStatement = conn.prepareStatement(checkQuery);
        checkStatement.setLong(1,ID_Category);
        ResultSet checkResult = checkStatement.executeQuery();

        if(!checkResult.isBeforeFirst()) {
            throw new CategoryNotExistsException("Parent category does not exits in the database");
        }

        PreparedStatement countStatement = conn.prepareStatement(countQuery);
        countStatement.setLong(1,ID_Category);
        ResultSet result = countStatement.executeQuery();
        if(result.isBeforeFirst()) {
            return result.getInt("num");
        } else {
            // no result from query. don't think can happen because it's a count
            return -1;
        }
    }

    public void createCategory(long ID_Category, String name, long parent) throws TooManyChildrenException, CategoryNotExistsException ,SQLException {
        /* if the parent category does not exist countDirectChildrenOf(parent) should throw two exceptions:
            1. SQLException: the parent column is references an existing ID_Category (it is a foreign key)
            2. CategoryNotExistsException: the check query found no record for this specific ID_Category
           Since we don't catch the exceptions here, those will be redirected to the caller of createCategory
         */
        int currentNumChildren = this.countDirectChildrenOf(parent);
        Category parentCategory = getCategoryFromId(parent);
        if(currentNumChildren >= 9)
            throw new TooManyChildrenException("This parent has too many children");

        String num = parentCategory.getNum() + Integer.toString(currentNumChildren + 1);
        String query = "INSERT INTO category(name,num,parent) values (?,?,?)";
        PreparedStatement createStatement = conn.prepareStatement(query);
        createStatement.setString(1,name);
        createStatement.setString(2,num);
        createStatement.setLong(1,parent);
        createStatement.executeUpdate();
    }

    public void copySubTree(long ID_source, long ID_destination) {

    }

}
