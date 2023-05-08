package beans;

import java.util.ArrayList;

public class Category {

    private long ID_Category;
    private String name;
    private String num;
    private long parent;
    private ArrayList<Category> children;
    public Category(long ID_Category, String name, String num, long parent, ArrayList<Category> children) {
        this.ID_Category = ID_Category;
        this.name = name;
        this.num = num;
        this.parent = parent;
        this.children = children;
    }

    public long getID_Category() {
        return ID_Category;
    }

    public String getName() {
        return name;
    }

    public String getNum() {
        return num;
    }

    public long getParent() {
        return parent;
    }

    public ArrayList<Category> getChildren() {
        return children;
    }
}
