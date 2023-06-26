/**
 * Make a request to the server
 * @param method HTTP method used for the request
 * @param url url of the request
 * @param data data to send to the server along with the request
 * @param callback callback function called when the server answers the request
 */
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
/**
 * Tells if it is possible to copy the subtree inside the destination tree
 * @param subtree source tree
 * @param destination destination tree
 * @returns {boolean} true if the copy is possible, false otherwise
 */
function isCopyPossible(subtree, destination) {
    if(destination.childrenList === undefined || destination.childrenList.length + 1 <= 9)
        return true;
    return false;
}
/**
 * Updates the fields of a category. It initializes the ID_Category as a random double between 0 and 1 (1 excluded)
 * @param elem the object of the category to be updated
 * @param parent the parent object of the element to update
 * @returns {*} the updated element
 */
function updateCategoryProperties(elem, parent) {

    const newNum = parent.num === "0" ? (parseInt(parent.childrenList.indexOf(elem)) + 1).toString() : parent.num.toString() + parseInt(parent.childrenList.indexOf(elem) + 1).toString();
    elem.num = newNum.toString();
    elem.parent = parent.ID_Category;
    elem.ID_Category = Math.random();

    for(var i=0; i<elem.childrenList.length; i++) {
        elem.childrenList[i] = updateCategoryProperties(elem.childrenList[i], elem);
    }
    return elem;
}
/**
 * Tells if the saved tree is valid -> there are no duplicated IDs, numbers and each parent ID is equal to the ID of the parent node
 * @param node the list to check
 * @param parentID the parent of the first node
 * @param ids the set of already used IDs
 * @param nums the set of alreaedy used number
 * @returns {boolean} true if node is a valid tree, false otherwise
 */
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
/**
 * Tells if the tree represented in the html is equal to the one saved locally
 * @param saved the object of the saved tree
 * @param rootList the HTMLElement of the root of the tree
 * @returns {boolean} true if the two trees are identical, false otherwise
 */
function checkPrintedWithSaved(saved, rootList) {
    const topElements = rootList.children;
    for (var i=0; i<topElements.length; i++) {
        const result = areTreeEqual(saved.childrenList[i],topElements[i]);
        if(!result)
            return false;
    }
    return true;
}
/**
 * Tells if the html tree is aligned with a given tree
 * @param node the object of the saved tree
 * @param htmlNode the li element that corresponds to the node
 */
function areTreeEqual(node, htmlNode) {
    const div = htmlNode.querySelector(".treeElementContent");
    const elem = {
        ID_Category: parseFloat(htmlNode.getAttribute("idcategory")),
        name: div.children[1].innerText,
        num: div.children[0].innerText,
        parent: parseFloat(htmlNode.getAttribute("idparent"))
    }

    if(!categoryEquals(node, elem))
        return false;

    const childrenList = htmlNode.querySelector("ul");
    if(childrenList === undefined || childrenList === null)
        return true;
    const childrenElements = childrenList.children;
    if(node.childrenList.length !== childrenElements.length)
        return false;

    for(var i=0; i<node.childrenList.length; i++) {
        if(areTreeEqual(node.childrenList[i],childrenElements[i]))
            return true;
    }
    return false;
}
/**
 * Tells if a given string is undefined, empty of just full of spaces/tabs
 * @param str the string to check
 * @returns {boolean} true if the string is invalid
 */
function isStringBlank(str) {
    return str === undefined || str.trim().length === 0;
}
/**
 * Tells if a given element is contained inside a tree
 * @param array the tree to look into
 * @param elem the param to search for
 * @returns {boolean}
 */
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

function checkForDuplicates(htmlNode, ids = new Set()) {
    const ID_Category = htmlNode.getAttribute("idcategory");
    if(ids.has(ID_Category))
        return true;
    ids.add(ID_Category);
    const childrenList = htmlNode.querySelector("ul");
    if(childrenList === undefined || childrenList === null)
        return true;
    const childrenElements = childrenList.children;
    for(var i=0; i<childrenElements.length; i++) {
        if(checkForDuplicates(childrenElements[i]))
            return true;
    }

    return false;
}

function checkTreeIDs(categoryList, htmlList) {
    const topElements = htmlList.children;
    for (var i=0; i<topElements.length; i++) {
        const result = checkIDs(categoryList.childrenList[i],topElements[i]);
        if(!result)
            return false;
    }
    return true;
}

function checkIDs(node, htmlNode) {
    const ID_Category = parseFloat(htmlNode.getAttribute("idcategory"));
    const parent = parseFloat(htmlNode.getAttribute("idparent"));

    if(node.ID_Category !== ID_Category || node.parent !== parent)
        return false;

    const childrenList = htmlNode.querySelector("ul");
    if(childrenList === undefined || childrenList === null) {
        if(node.childrenList.length !== 0)
            return false;
        else
            return true;
    } else if(childrenList.children.length !== node.childrenList.length)
        return false;

    const childrenElements = childrenList.children;
    for(var i=0; i<node.childrenList.length; i++) {
        if(checkIDs(node.childrenList[i],childrenElements[i]))
            return true;
    }
    return false;
}

function isRoot(ID_Category) {
    if(ID_Category === 1)
        return true;
    return false;
}
