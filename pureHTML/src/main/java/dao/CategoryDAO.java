package dao;

import beans.Category;
import exceptions.CategoryNotExistsException;
import exceptions.InvalidCategoryException;
import exceptions.TooManyChildrenException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CategoryDAO {

    private Connection conn;
    public CategoryDAO(Connection conn) {
        this.conn = conn;
    }
    public ArrayList<Category> getTopCategories() throws SQLException, CategoryNotExistsException {

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
    public Category getCategoryFromId(long ID_requested) throws SQLException, CategoryNotExistsException {
        String query = "SELECT * FROM Category WHERE ID_Category = ?";
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setLong(1,ID_requested);
        ResultSet result = preparedStatement.executeQuery();
        if(!result.isBeforeFirst()) {
            return null;
            // could throw CategoryNotExists though
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
    private ArrayList<Category> getDirectChildrenOf(long ID_requested) throws SQLException, CategoryNotExistsException {
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
     * @param name is the name of the new category
     * @param parent is the ID_Category of the parent category
     * @throws TooManyChildrenException is thrown if the choosen parent has too many children (max = 9)
     * @throws CategoryNotExistsException is thrown if there is no Category inside the dabatase with the ID_Category of the choosen parent
     * @throws SQLException is thrown if an error occured when executing the query
     */
    public void createCategory(String name, long parent) throws TooManyChildrenException, CategoryNotExistsException ,SQLException {
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
        } catch(CategoryNotExistsException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    public void copySubTree(long ID_source, long ID_destination) throws SQLException, CategoryNotExistsException, TooManyChildrenException {
        Category source, destination;
        source = this.getCategoryFromId(ID_source);
        destination = this.getCategoryFromId(ID_destination);

        int currentNumDestinationChildren = destination.getChildren().size();
        if(currentNumDestinationChildren >= 9)
            throw new TooManyChildrenException("Impossible to copy the selected sub tree. The resulting tree would have to many children");

        destination.addNewChildren(source);
        try {
            conn.setAutoCommit(false);
            insertNewSubTree(source, destination);
            conn.commit();
        } catch(SQLException | CategoryNotExistsException | TooManyChildrenException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);;
        }
    }
    private void insertNewSubTree(Category curr, Category parent) throws SQLException, CategoryNotExistsException, TooManyChildrenException {
        //update curr
        String num, query;
        query = "INSERT INTO category(name,num,parent) VALUES (?,?,?)";
        PreparedStatement copyStatement = conn.prepareStatement(query);
        Category newParent = this.getCategoryFromNum(parent.getNum());
        int currentParentNumChildren = newParent.getChildren().size();

        if(currentParentNumChildren >= 9)
            throw new TooManyChildrenException("Can't add any more children, it would exceed the limit");

        if(parent.getNum().equals("0"))
            num = Integer.toString(currentParentNumChildren + 1);
        else
            num = parent.getNum() + Integer.toString(currentParentNumChildren + 1);

        curr.setNum(num);
        copyStatement.setString(1,curr.getName());
        copyStatement.setString(2,curr.getNum());
        copyStatement.setLong(3,newParent.getID_Category());
        copyStatement.executeUpdate();

        for(Category child : curr.getChildren()) {
            insertNewSubTree(child,curr);
        }
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
            return -1;
        }
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

    public boolean isCopyPossible(Long from, Long to) throws SQLException, CategoryNotExistsException {
        Category source = this.getCategoryFromId(from), destination = this.getCategoryFromId(to);
        if(to == 1 && destination.getChildren().size() < 9)
            return true;
        Set<Long> parentIDs = this.getParentIDs(destination);

        // if the set of direct parents of the destination contains the source the copy request is invalid
        if(parentIDs.contains(source.getID_Category()))
            return false;
        else
            return true;

    }

    private boolean checkSubtree(Category curr, Set<Long> ids) {
        if(curr.getNum().equals("0"))
            return false;
        if(ids.contains(curr.getID_Category())) {
            return false;
        } else {
            ids.add(curr.getID_Category());
            if(curr.getChildren().size() > 9)
                return false;
            for(Category x : curr.getChildren()) {
                if(!checkSubtree(x,ids))
                    return false;
            }
            return true;
        }
    }

    private Set<Long> getParentIDs(Category root) throws SQLException, CategoryNotExistsException {
        Category curr = this.getCategoryFromId(root.getParent());
        Set<Long> parentIDs = new HashSet<>();

        do {
            parentIDs.add(curr.getID_Category());
            curr = this.getCategoryFromId(curr.getParent());
        } while(curr.getID_Category() != 1);

        return parentIDs;
    }

}
