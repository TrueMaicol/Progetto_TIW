
function makeCall(method, url, data, callback) {
    var request = new XMLHttpRequest();
    request.onload = () => callback(request);
    request.open(method,url);
    request.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    if(data == null)
        request.send();
    else 
        request.send(data);

}

/**
 * Return the object with a given ID_Category. Don't give in input the root node of the list, give the list of the top categories instead
 * @param categoryList the list to search in
 * @param ID_Category the ID of the object to be retrieved.
 * @returns {*} if it exists return the object with the given ID_Category
 */
function searchCategoryById(categoryList, ID_Category) {
    if(categoryList.ID_Category === ID_Category)
        return categoryList;
    if(categoryList.childrenList !== undefined && categoryList.childrenList.length > 0)
        for(let i=0; i<categoryList.childrenList.length; i++) {
            const curr = categoryList.childrenList[i];
            const found = searchCategoryById(curr,ID_Category);
            if (found)
                return found;
        }
}

function searchCategoryByNum(categoryList, num) {
    if(categoryList.num === num)
        return categoryList;
    if(categoryList.childrenList !== undefined && categoryList.childrenList.length > 0)
        for(let i=0; i<categoryList.childrenList.length; i++) {
            const curr = categoryList.childrenList[i];
            const found = searchCategoryById(curr,ID_Category);
            if (found)
                return found;
        }
}

/**
 * Returns true if elem is a child of parent. Search for the elem inside the parent subtree.
 * @param parent the parent element
 * @param elem the element to check
 */
function isChildrenOf(parent, elem) {
    if(categoryEquals(parent,elem))
        return true;
    if(parent.childrenList !== undefined && parent.childrenList.length > 0) {
        for(var i=0; i<parent.childrenList.length; i++) {
            if(isChildrenOf(parent.childrenList[i],elem))
                return true;
        }
    }
    return false;
}

/**
 * Returns true if two categories are exactly the same regardless of the childrenList of both
 * @param x
 * @param y
 */
function categoryEquals(x,y) {
    if(x.ID_Category !== y.ID_Category || x.name !== y.name || x.num !== y.num || x.parent !== y.parent)
        return false;
    return true;
}

function isCopyPossible(subtree, destination) {
    if(destination.childrenList === undefined || destination.childrenList.length + 1 <= 9)
        return true;
    return false;
}

function updateCategoryProperties(elem, parent) {

    console.log(parent.childrenList.indexOf(elem) + 1);
    console.log(parent.childrenList);
    const newNum = parent.num === "0" ? (parseInt(parent.childrenList.indexOf(elem)) + 1).toString() : parent.num.toString() + parseInt(parent.childrenList.indexOf(elem) + 1).toString();
    console.log(newNum);

    elem.num = newNum.toString();
    elem.parent = parent.ID_Category;
    elem.ID_Category = Math.random();
    for(var i=0; i<elem.childrenList.length; i++) {
        elem.childrenList[i] = updateCategoryProperties(elem.childrenList[i], elem);
    }
    return elem;
}

function isTreeValid(node,parentID, ids = new Set(), nums = new Set()) {
    if (ids.has(node.ID_Category) || nums.has(parseInt(node.num)) || node.parent !== parentID) {
        return false;
    }
    ids.add(node.ID_Category);
    nums.add(parseInt(node.num));
    for (let i=0; i<node.childrenList.length; i++) {
        if(node.ID_Category !== node.childrenList[i].parent)
            return false;
        if (!isTreeValid(node.childrenList[i],node.ID_Category, ids, nums)) {
            return false;
        }
    }
    return true;
}

function isStringBlank(str) {
    return str === undefined || str.trim().length === 0;
}

function contains(array, elem) {
    if(array !== undefined && array.length > 0) {
        for(var i=0; i<array.length; i++) {
            const curr = array[i];
            if(categoryEquals(curr,elem))
                return true;
        }
        return false;
    }
}

function findCategoryInNew(array, ID_Category) {
    if(array !== undefined && array.length === 0) {
        for(var i=0; i<array.length; i++) {
            const curr = array[i];
            if(curr.ID_Category === ID_Category)
                return curr;
        }
    }
}