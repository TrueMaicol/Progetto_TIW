package dao;

import beans.Category;
import beans.ClientCategory;
import exceptions.CategoryNotExistsException;
import exceptions.InvalidCategoryException;
import exceptions.TooManyChildrenException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
            throw new CategoryNotExistsException("Requested category has not been found");
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
        String query = "INSERT INTO category(name,num,parent) values (?,?,?)";
        PreparedStatement createStatement = conn.prepareStatement(query);
        createStatement.setString(1,name);
        createStatement.setString(2,num);
        createStatement.setLong(3,parent);
        createStatement.executeUpdate();
        Category temp = this.getCategoryFromNum(num);
        return temp;

    }


    /**
     * Insert a tree inside another category. If any error occurs any changes made are rolled back
     * @param source
     * @param destination
     * @throws SQLException
     * @throws TooManyChildrenException
     * @throws CategoryNotExistsException
     */
    public void copySubTree(Category source, Category destination) throws SQLException, TooManyChildrenException, CategoryNotExistsException, InvalidCategoryException {
        if(source.getID_Category() == destination.getID_Category() || source.getNum().equals(destination.getNum()))
            throw new InvalidCategoryException("Source and destination are the same");
        insertNewSubTree(source, destination);
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
        PreparedStatement updateQuery = conn.prepareStatement(query);
        updateQuery.setString(1,newName);
        updateQuery.setLong(2, ID_Category);
        updateQuery.executeUpdate();
        temp = this.getCategoryFromId(ID_Category);
        return temp;


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

    /**
     * Checks if the tree are equal. It does not check if the root exists
     * @param userTree
     * @return
     * @throws SQLException
     * @throws CategoryNotExistsException
     */
    public boolean areTreeEqual(ArrayList<Category> userTree) throws SQLException, CategoryNotExistsException {
        ArrayList<Category> serverTree = this.getCategoryFromId(1).getChildren();
        if(serverTree.size() != userTree.size())
            return false;
        for(int i=0; i<serverTree.size(); i++) {
            if(!categoryEquals(serverTree.get(i),userTree.get(i)))
                return false;
        }
        return true;
    }

    private boolean categoryEquals(Category x, Category y) {
        if(x.getID_Category() == y.getID_Category() && x.getName().equals(y.getName()) && x.getNum().equals(y.getNum()) && x.getParent() == y.getParent()) {
            if(x.getChildren().size() == y.getChildren().size()) {
                ArrayList<Category> xChildren = x.getChildren();
                ArrayList<Category> yChildren = y.getChildren();
                for(int i=0; i< xChildren.size(); i++) {
                    if(!this.categoryEquals(xChildren.get(i), yChildren.get(i)))
                        return false;
                }
                return true;
            }
        }
        return false;
    }


    public boolean areOptionsOk(ArrayList<Category> options) throws SQLException, CategoryNotExistsException {
        Category serverTree = this.getCategoryFromId(1);
        ArrayList<Long> serverIDs = new ArrayList<>(), clientIDs = new ArrayList<>();
        ArrayList<String> serverNUMs = new ArrayList<>(), clientNUMs = new ArrayList<>();
        Set<Long> x, y;
        Set<String> a,b;
        ArrayList<Category> temp = new ArrayList<>();
        temp.add(serverTree);

        createListId(temp, serverIDs, serverNUMs);
        createListIdFromOptions(options, clientIDs, clientNUMs);
        x = new HashSet<>(serverIDs);
        y = new HashSet<>(clientIDs);
        if(y.size() != clientIDs.size() || x.size() != serverIDs.size()) {
            return false;
        } else if(x.containsAll(y) && x.size() == y.size()) {
            return true;
        }

        a = new HashSet<>(serverNUMs);
        b = new HashSet<>(clientNUMs);
        if(b.size() != clientNUMs.size() || a.size() != serverNUMs.size()) {
            return false;
        } else if(a.containsAll(b) && a.size() == b.size()) {
            return true;
        }
        return false;
    }

    private void createListId(ArrayList<Category> categories, ArrayList<Long> ids, ArrayList<String> nums) throws SQLException, CategoryNotExistsException {
        for(Category curr : categories) {
            ids.add(curr.getID_Category());
            nums.add(curr.getNum());
            if(curr.getChildren() != null && curr.getChildren().size() != 0) {
                createListId(curr.getChildren(), ids, nums);
            }
        }
    }

    private void createListIdFromOptions(ArrayList<Category> options, ArrayList<Long> ids, ArrayList<String> nums) {
        for(Category curr : options) {
            ids.add(curr.getID_Category());
            nums.add(curr.getNum());
        }
    }


    private Set<String> getParentNums(ClientCategory root) throws SQLException, CategoryNotExistsException {
        Category curr = this.getCategoryFromId((long) root.parent);
        Set<String> parentNums = new HashSet<>();

        do {
            parentNums.add(curr.getNum());
            curr = this.getCategoryFromId(curr.getParent());
        } while(curr.getID_Category() != 1);

        return parentNums;
    }

    /**
     *
     * @param newCategory the category tree to be checked
     * @return
     */
    public boolean isCopyPossible(ClientCategory newCategory) throws SQLException, CategoryNotExistsException {
        Set<String> parentNums = this.getParentNums(newCategory);

        return checkNums(newCategory, parentNums);

    }

    private boolean checkNums(ClientCategory curr, Set<String> nums) {
        if(nums.contains(curr.sourceNum)) {
            return false;
        } else {
            nums.add(curr.num);
            for(ClientCategory x : curr.childrenList) {
                if(!checkNums(x,nums))
                    return false;
            }
            return true;
        }
    }

}
