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
    private Category serverTree;
    public CategoryDAO(Connection conn) {
        this.conn = conn;
        try {
            this.serverTree = this.getCategoryFromId(1);
        } catch (SQLException | CategoryNotExistsException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Category> getTopCategories() throws SQLException, CategoryNotExistsException {

        // retrieve all the top categories
        String query = "select * from category where parent = 1";
        ArrayList<Category> tree = new ArrayList<Category>();
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        //preparedStatement.setLong(1,ID_parent);
        ResultSet result = preparedStatement.executeQuery();
        while(result.next()) {

            long ID_Category = result.getLong("ID_Category");
            String name = result.getString("name");
            String num = result.getString("num");
            long parent = result.getLong("parent");
            ArrayList<Category> children = this.getDirectChildrenOf(ID_Category);

            Category temp = new Category(ID_Category, name, num, parent, children);
            tree.add(temp);
        }
        return tree;
    }
    public ArrayList<Category> getAllCategories() throws SQLException {
        String query = "select * from category order by num";
        ArrayList<Category> tree = new ArrayList<Category>();
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        ResultSet result = preparedStatement.executeQuery();
        while(result.next()) {

            long ID_Category = result.getLong("ID_Category");
            String name = result.getString("name");
            String num = result.getString("num");
            long parent = result.getLong("parent");

            Category temp = new Category(ID_Category, name, num, parent, null);
            tree.add(temp);
        }

        return tree;
    }
    private void printCategory(Category c) {
        System.out.println("ID: "+Long.toString(c.getID_Category()));
        System.out.println("name: "+c.getName());
        System.out.println("num: "+c.getNum());
        System.out.println("parent: "+Long.toString(c.getParent()));
        if(c.getChildren() == null)
            System.out.println("children null");
        else if(c.getChildren().isEmpty())
            System.out.println("children empty");
        else
            for(int i=0; i<c.getChildren().size(); i++) {
                System.out.println("ID: "+Long.toString(c.getChildren().get(i).getID_Category()));
                System.out.println("name: "+c.getChildren().get(i).getName());
                System.out.println("num: "+c.getChildren().get(i).getNum());
                System.out.println("parent: "+Long.toString(c.getChildren().get(i).getParent()));
                if(c.getChildren() == null)
                    System.out.println("children null");
                else if(c.getChildren().isEmpty())
                    System.out.println("children empty");
                else
                    System.out.println("altro problema");

            }
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
            result.next();
            return result.getInt("num");
        } else {
            // no result from query. don't think can happen because it's a count
            return -1;
        }
    }

    public int countDirectChildrenOf(String num) throws SQLException, CategoryNotExistsException {
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
     *
     * @param name   is the name of the new category
     * @param parent is the ID_Category of the parent category
     * @return
     * @throws TooManyChildrenException   is thrown if the choosen parent has too many children (max = 9)
     * @throws CategoryNotExistsException is thrown if there is no Category inside the dabatase with the ID_Category of the choosen parent
     * @throws SQLException               is thrown if an error occured when executing the query
     */
    public Category createCategory(String name, long parent) throws TooManyChildrenException, CategoryNotExistsException ,SQLException {
        /* if the parent category does not exist countDirectChildrenOf(parent) should throw two exceptions:
            1. SQLException: the parent column is references an existing ID_Category (it is a foreign key)
            2. CategoryNotExistsException: the check query found no record for this specific ID_Category
           Since we don't catch the exceptions here, those will be redirected to the caller of createCategory
         */
        String num;
        int currentNumChildren = this.countDirectChildrenOf(parent);
        Category parentCategory = getCategoryFromId(parent);
        if(currentNumChildren >= 9)
            throw new TooManyChildrenException("This parent has too many children");

        if(parentCategory.getID_Category() == 1) { // root category
            num = Integer.toString(currentNumChildren + 1);
        } else {
            num = parentCategory.getNum() + Integer.toString(currentNumChildren + 1);
        }
        String query = "INSERT INTO category(name,num,parent) values (?,?,?)";
        PreparedStatement createStatement = conn.prepareStatement(query);
        createStatement.setString(1,name);
        createStatement.setString(2,num);
        createStatement.setLong(3,parent);
        createStatement.executeUpdate();

        return this.getCategoryFromNum(num);
    }

    public void copySubTree(long ID_source, long ID_destination) throws SQLException, CategoryNotExistsException, TooManyChildrenException {
        String num,query;
        int currentNumDestinationChildren = this.countDirectChildrenOf(ID_destination);
        //int directSourceChildren = this.countDirectChildrenOf(ID_source);
        if(currentNumDestinationChildren + 1 > 9)
            throw new TooManyChildrenException("Impossible to copy the selected sub tree. The resulting tree would have to many children");
        Category source, destination;
        ArrayList<Category> children;
        source = this.getCategoryFromId(ID_source);
        destination = this.getCategoryFromId(ID_destination);
        destination.addNewChildren(source);
        insertCategoryWithChildren(source, destination);
    }

    /**
     * Recursive function to insert each category of a subtree as a children of the parent. First call is sourceTree, destinationParent
     * @param curr
     * @param parent
     * @throws SQLException
     * @throws CategoryNotExistsException
     */
    public void insertCategoryWithChildren(Category curr, Category parent) throws SQLException, CategoryNotExistsException {
        //update curr
        String num, query;
        query = "INSERT INTO category(name,num,parent) VALUES (?,?,?)";
        PreparedStatement copyStatement = conn.prepareStatement(query);

        // check if the given parent actually exists
        Category newParent = this.getCategoryFromNum(parent.getNum());
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
            insertCategoryWithChildren(child,curr);
        }
    }

    public Category renameCategoryById(long ID_Category, String newName) throws SQLException, CategoryNotExistsException {
        String query = "UPDATE category SET name = ? WHERE ID_Category = ?";

        //check if the requested category exists
        Category temp = this.getCategoryFromId(ID_Category);

        PreparedStatement updateQuery = conn.prepareStatement(query);
        updateQuery.setString(1,newName);
        updateQuery.setLong(2, ID_Category);
        updateQuery.executeUpdate();

        // return the update category
        return this.getCategoryFromId(ID_Category);
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

}
