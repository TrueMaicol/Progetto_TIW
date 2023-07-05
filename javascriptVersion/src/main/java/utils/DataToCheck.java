package utils;

import beans.Category;

import java.util.HashSet;
import java.util.Set;

public class DataToCheck {
    private Category[] clientTree;
    private ClientCategory clientTreeSAVED;
    private Category[] options;

    public Category[] getClientTree() {
        return clientTree;
    }
    public ClientCategory getClientTreeSAVED() {
        return clientTreeSAVED;
    }
    public Category[] getOptions() {
        return options;
    }

    /**
     *
     * @return true if the tree does have a duplicate in ids and/or nums
     */
    public boolean hasSavedDuplicates() {
        Set<Double> ids = new HashSet<>();
        Set<String> nums = new HashSet<>();
        return findDuplicates(clientTreeSAVED, ids, nums);
    }

    private boolean findDuplicates(ClientCategory curr, Set<Double> ids, Set<String> nums) {
        if(ids.contains(curr.ID_Category) || nums.contains(curr.num))
            return true;
        ids.add(curr.ID_Category);
        nums.add(curr.num);
        for(ClientCategory child : curr.childrenList) {
            if(findDuplicates(child, ids, nums))
                return true;
        }
        return false;
    }

}
