package beans;

public class Category {

    private long ID_Category;
    private String name;
    private String num;
    private long parent;

    public Category(long ID_Category, String name, String num, long parent) {
        this.ID_Category = ID_Category;
        this.name = name;
        this.num = num;
        this.parent = parent;
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
}
