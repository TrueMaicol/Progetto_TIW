package dao;

import beans.Category;
import exceptions.CategoryNotExistsException;
import exceptions.TooManyChildrenException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class CategoryDAO {

    private Connection conn;
    public CategoryDAO(Connection conn) {
        this.conn = conn;
    }

    public ArrayList<Category> getTopCategories() throws SQLException, CategoryNotExistsException {
        // create a temporary table treeById used to contain all the discendent of the category with ID_Category = ID_parent

        /*String query = "WITH RECURSIVE treeById (ID_Category, name, num, parent) as (" +
                "select ID_Category, name, num, parent from category where ID_Category = ? UNION ALL " +
                "select c.ID_Category, c.name, c.num, c.parent from treeById as tree JOIN category as c ON tree.ID_Category = c.parent)" +

                "SELECT t.ID_Category, t.name, t.num, t.parent FROM treeById as t;"; */

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
        // remove the root category, it is not to be seen
        //tree.remove(0);
        for(int i=0; i<tree.size(); i++) {
            printCategory(tree.get(i));
        }

        //printTree(tree);
        return tree;
    }
    public ArrayList<Category> getAllCategories() throws SQLException {
        String query = "select * from category where ID_Category <> 1";
        ArrayList<Category> tree = new ArrayList<Category>();
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        //preparedStatement.setLong(1,ID_parent);
        ResultSet result = preparedStatement.executeQuery();
        while(result.next()) {

            long ID_Category = result.getLong("ID_Category");
            String name = result.getString("name");
            String num = result.getString("num");
            long parent = result.getLong("parent");

            Category temp = new Category(ID_Category, name, num, parent, null);
            tree.add(temp);
        }
        // remove the root category, it is not to be seen
        //tree.remove(0);
        System.out.println("\n ORDERED CATEGORIES: ");
        for(int i=0; i<tree.size(); i++) {
            System.out.println("ID: "+Long.toString(tree.get(i).getID_Category()));
            System.out.println("name: "+tree.get(i).getName());
            System.out.println("num: "+tree.get(i).getNum());
            System.out.println("parent: "+Long.toString(tree.get(i).getParent()));
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
        System.out.println("Requested category " + Long.toString(ID_requested));
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
     *
     * @param ID_Category
     * @param name
     * @param parent
     * @throws TooManyChildrenException
     * @throws CategoryNotExistsException
     * @throws SQLException
     */
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
