package dao;

import beans.Category;
import exceptions.CategoryNotExistsException;
import exceptions.TooManyChildrenException;

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
    public Category getCategoryFromId(long ID_requested) throws SQLException, CategoryNotExistsException {
        String query = "SELECT * FROM Category WHERE ID_Category = ?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setLong(1,ID_requested);
        ResultSet result = preparedStatement.executeQuery();
        if(!result.isBeforeFirst()) { // no category has been found
            return null;
        } else {
            result.next();
            long ID_Category = result.getLong("ID_Category");
            String name = result.getString("name");
            String num = result.getString("num");
            long parent = result.getLong("parent");
            ArrayList<Category> children = this.getDirectChildrenOf(ID_Category);

            Category category = new Category(ID_Category, name, num, parent, children);
            return category;
        }
    }
    public Category getCategoryFromNum(String number) throws SQLException, CategoryNotExistsException {
        String query = "SELECT * FROM Category WHERE num = ?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setString(1,number);
        ResultSet result = preparedStatement.executeQuery();
        if(!result.isBeforeFirst()) { // no category has been found
            return null;
        } else {
            result.next();
            long ID_Category = result.getLong("ID_Category");
            String name = result.getString("name");
            String num = result.getString("num");
            long parent = result.getLong("parent");
            ArrayList<Category> children = this.getDirectChildrenOf(ID_Category);

            Category category = new Category(ID_Category, name, num, parent, children);
            return category;
        }
    }

    public ArrayList<Category> getDirectChildrenOf(long ID_requested) throws SQLException, CategoryNotExistsException {
        ArrayList<Category> children = new ArrayList<>();
        String checkQuery = "select * from category where ID_Category = ?";
        String getQuery = "select * from category where parent = ?";
        PreparedStatement checkStatement = conn.prepareStatement(checkQuery);
        checkStatement.setLong(1,ID_requested);
        ResultSet checkResult = checkStatement.executeQuery();

        if(!checkResult.isBeforeFirst()) {
            throw new CategoryNotExistsException("Parent category does not exits in the database");
        }

        PreparedStatement countStatement = conn.prepareStatement(getQuery);
        countStatement.setLong(1,ID_requested);
        ResultSet result = countStatement.executeQuery();
        if(result.isBeforeFirst()) {
            while(result.next()) {
                long ID_Category = result.getLong("ID_Category");
                String name = result.getString("name");
                String num = result.getString("num");
                long parent = result.getLong("parent");
                ArrayList<Category> tempChildren = this.getDirectChildrenOf(ID_Category);

                Category category = new Category(ID_Category, name, num, parent, tempChildren);
                children.add(category);
            }
        } else {
            // there are no categories with this parent -> this parent has no child
        }
        return children;
    }

    /**
     * This method creates a category and puts it inside the tree
     * @param name   is the name of the new category
     * @param parent is the ID_Category of the parent category
     * @return
     * @throws TooManyChildrenException   is thrown if the choosen parent has too many children (max = 9)
     * @throws CategoryNotExistsException is thrown if there is no Category inside the dabatase with the ID_Category of the choosen parent
     * @throws SQLException               is thrown if an error occured when executing the query
     */
    public Category createCategory(String name, long parent) throws TooManyChildrenException, CategoryNotExistsException ,SQLException {

        String num;
        Category parentCategory = getCategoryFromId(parent);
        int currentNumChildren = parentCategory.getChildren().size();

        if(currentNumChildren >= 9)
            throw new TooManyChildrenException("This parent has too many children");

        if(parentCategory.getID_Category() == 1) { // root category
            num = Integer.toString(currentNumChildren + 1);
        } else {
            num = parentCategory.getNum() + Integer.toString(currentNumChildren + 1);
        }
        conn.setAutoCommit(false);
        String query = "INSERT INTO category(name,num,parent) values (?,?,?)";
        PreparedStatement createStatement = conn.prepareStatement(query);
        createStatement.setString(1,name);
        createStatement.setString(2,num);
        createStatement.setLong(3,parent);
        createStatement.executeUpdate();
        try {
            Category temp = this.getCategoryFromNum(num);
            conn.commit();
            return temp;
        } catch(CategoryNotExistsException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

    }


    /**
     * Insert a tree inside another category. If any error occurs any changes made are rolled back
     * @param source
     * @param destination
     * @throws SQLException
     * @throws TooManyChildrenException
     * @throws CategoryNotExistsException
     */
    public void copySubTree(Category source, Category destination) throws SQLException, TooManyChildrenException, CategoryNotExistsException {
        conn.setAutoCommit(false);
        try {
            insertNewSubTree(source, destination);
            conn.commit();
        } catch (SQLException | CategoryNotExistsException | TooManyChildrenException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }


    /**
     * Recursive function to insert each category of a subtree as a children of the parent. First call is sourceTree, destinationParent
     * @param curr
     * @param parent
     * @throws SQLException
     * @throws CategoryNotExistsException
     */
    public void insertNewSubTree(Category curr, Category parent) throws SQLException, CategoryNotExistsException, TooManyChildrenException {
        //update curr
        String num, query;
        query = "INSERT INTO category(name,num,parent) VALUES (?,?,?)";
        PreparedStatement copyStatement = conn.prepareStatement(query);

        // check if the given parent actually exists
        Category newParent = this.getCategoryFromNum(parent.getNum());
        if(newParent.getChildren().size() == 9)
            throw new TooManyChildrenException("");
        if(newParent.getNum().equals("0"))
            num = Integer.toString(newParent.getChildren().size() + 1);
        else
            num = parent.getNum() + Integer.toString(newParent.getChildren().size() + 1);
        curr.setNum(num);
        copyStatement.setString(1,curr.getName());
        copyStatement.setString(2,curr.getNum());
        copyStatement.setLong(3,newParent.getID_Category());
        copyStatement.executeUpdate();

        for(Category child : curr.getChildren()) {
            insertNewSubTree(child,curr);
        }
    }

    public Category renameCategoryById(long ID_Category, String newName) throws SQLException, CategoryNotExistsException {
        String query = "UPDATE category SET name = ? WHERE ID_Category = ?";

        //check if the requested category exists
        Category temp = this.getCategoryFromId(ID_Category);
        conn.setAutoCommit(false);
        PreparedStatement updateQuery = conn.prepareStatement(query);
        updateQuery.setString(1,newName);
        updateQuery.setLong(2, ID_Category);
        updateQuery.executeUpdate();

        // return the update category
        try {
            temp = this.getCategoryFromId(ID_Category);
            conn.commit();
            return temp;
        } catch (CategoryNotExistsException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

    }

    /**
     * Check if two categories are equals beside their children
     * @param x first category to check
     * @param y second category to check
     * @return true if x and y have the same ID, name, num and parent
     */
    private boolean equalsCategory(Category x, Category y) {
        if(x.getID_Category() != y.getID_Category() || !x.getNum().equals(y.getNum()) || !x.getName().equals(y.getName()) ||
           x.getParent() != y.getParent())
            return false;
        return true;
    }
    private int countDirectChildrenOf(long ID_Category) throws SQLException, CategoryNotExistsException {
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
            result.next();
            return result.getInt("num");
        } else {
            // no result from query. don't think can happen because it's a count
            return -1;
        }
    }
    private int countDirectChildrenOf(String num) throws SQLException, CategoryNotExistsException {
        String checkQuery = "select * from category where num = ?";
        String countQuery = "select count(*) as num from category where parent = ?";
        PreparedStatement checkStatement = conn.prepareStatement(checkQuery);
        checkStatement.setString(1,num);
        ResultSet checkResult = checkStatement.executeQuery();

        if(!checkResult.isBeforeFirst()) {
            throw new CategoryNotExistsException("Parent category does not exits in the database");
        }

        PreparedStatement countStatement = conn.prepareStatement(countQuery);
        Category curr = this.getCategoryFromNum(num);
        countStatement.setLong(1,curr.getID_Category());
        ResultSet result = countStatement.executeQuery();
        if(result.isBeforeFirst()) {
            result.next();
            return result.getInt("num");
        } else {
            // no result from query. don't think can happen because it's a count
            return -1;
        }
    }

}
