
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
function searchCategory(categoryList, ID_Category) {
    for(var i=0; i<categoryList.length; i++) {
        const curr = categoryList[i];
        if(curr.ID_Category === ID_Category)
            return curr;

        if(curr.childrenList !== undefined && curr.childrenList.length > 0) { // if the list is undefined then
            const found = searchCategory(curr.childrenList,ID_Category);
            if (found)
                return found;
        }
    }
}

/**
 * Return the HTMLElement of the requested category
 * @param rootList the HTMLElement of the root node of the list
 * @param ID_Category the ID_Category you want to get the relative node
 * @returns {*} if it exists return the HTMLElement relative to the category with the ID_Category requested
 */
function findCategoryNode(rootList, ID_Category) {

    const currElement = rootList.childNodes;
    console.log(currElement)


}