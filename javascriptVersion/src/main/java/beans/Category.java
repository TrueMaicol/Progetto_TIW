package beans;

import exceptions.TooManyChildrenException;

import java.util.ArrayList;

public class Category {

    private long ID_Category;
    private String name;
    private String num;
    private long parent;
    private ArrayList<Category> childrenList;
    public Category(long ID_Category, String name, String num, long parent, ArrayList<Category> childrenList) {
        this.ID_Category = ID_Category;
        this.name = name;
        this.num = num;
        this.parent = parent;
        this.childrenList = childrenList;
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
    public void setNum(String num) {
        this.num = num;
    }
    public long getParent() {
        return parent;
    }

    public ArrayList<Category> getChildren() {
        return childrenList;
    }

    public void addNewChildren(Category child) throws TooManyChildrenException {
        if(this.childrenList.size() == 9)
            throw new TooManyChildrenException("Destination would have too many children after add");
        childrenList.add(child);
    }
}
