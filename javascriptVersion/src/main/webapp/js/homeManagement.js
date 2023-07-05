{
    const Modes = {
        LOCAL: "local",
        ONLINE: "online"
    }

    const pageManager = new PageManager();
    let categoryTree, newCategoryForm;
    window.addEventListener("load",() => {
        if(sessionStorage.getItem("user") === null)
            window.location.href = "index.html";
        else {
            pageManager.start();
        }
    });


    /**
     * Page manager
     */
    function PageManager() {
        this.currentMode = Modes.ONLINE;
        /**
         * Start the page
         */
        this.start = function() {
            var self = this;
            categoryTree = new CategoryTree();
            newCategoryForm = new NewCategoryForm();
            newCategoryForm.init();
            categoryTree.init();

            window.addEventListener("click",function handleResetError(e) {
                e.preventDefault();
                self.resetErrors();
            })
        };
        /**
         * Set page mode to LOCAL and refresh the page
         * @param categoryList the list to display
         */
        this.setModeToLocal = (categoryList) => {
            var self = this;
            self.currentMode = Modes.LOCAL;
            self.refresh(categoryList);
        }
        /**
         * Set page mode to ONLINE and refresh the page
         * @param categoryList the list to display
         */
        this.setModeToOnline = (categoryList) => {
            var self = this;
            self.currentMode = Modes.ONLINE;
            self.refresh(categoryList);
        }
        /**
         * Refresh the page keeping the same mode
         * @param categoryList
         */
        this.refresh = (categoryList) => {
            var self = this;
            if(self.currentMode === Modes.ONLINE) {
                categoryTree.refreshOnline(categoryList);
                newCategoryForm.refreshOnline(categoryList);
            } else if(self.currentMode === Modes.LOCAL) {
                categoryTree.refreshLocal(categoryList);
                newCategoryForm.refreshLocal(categoryList);

            }
        }
        /**
         * Reset all the errors of the page
         */
        this.resetErrors = function() {
            categoryTree.resetError();
            newCategoryForm.resetError();
        }
    }

    /**
     * CategoryTree component
     */
    function CategoryTree() {

        const rootList = document.getElementById("rootTree");
        const treeTextError = document.getElementById("treeTextError");
        const dragDropModal = document.getElementById("dragDropModal");
        const saveDragButton = document.getElementById("saveDragOperation");
        var newCategories = [], renamedCategories = [], currentTree = {}, options = [];
        let draggedElement;
        this.categoryList = {};
        /**
         * gets the category tree from the server, saves it locally and displays the content on the page
         */
        this.init = function() {
            let self = this;
            // get the tree from the database
            makeCall("GET", "GetTree",null,function(req) {
                const response = JSON.parse(req.responseText);
                switch(req.status) {
                    case 200:
                        self.categoryList = response;
                        pageManager.setModeToOnline(self.categoryList);
                        saveDragButton.addEventListener("click",handleSaveButtonClick);
                        break;
                    case 400:
                    case 401:
                        self.setError(response.textError);
                        break;
                    case 500:
                        self.setError(response.textError);
                        break;
                }
            })
        }

        /**
         * Inserts a new category in the local tree
         * @param newCategory the new category to be put in the local tree
         */
        this.insertNewCategory = function(newCategory) {
            let self = this;
            const parent = searchCategoryById(self.categoryList, newCategory.parent);
            parent.childrenList.push(newCategory);
        }

        /**
         * used to print the subtree with curr as its root node
         * @param curr the root node of the tree to print
         * @param parentNode the HTMLElement of the parent to contain the tree
         * @param clickCallback callback to be called when a tree node is clicked
         */
        this.printTreeElement = function(curr, parentNode, clickCallback) {
            var self = this;
            // the parent element is the HTMLElement of the parent in which we have to create the new nested list if parent has any children
            const currentElementLi = document.createElement("li");
            const currentElementText = document.createElement("h3");
            /*const currentElementNum = document.createElement("h3");
            const currentElementName = document.createElement("h3");*/
            const currentElementDiv = document.createElement("div");
            currentElementDiv.classList.add("treeElementContent");

            currentElementDiv.draggable = true;
            currentElementDiv.addEventListener("dragstart",dragStart);
            currentElementDiv.addEventListener("dragover", dragOver);
            currentElementDiv.addEventListener("dragleave",dragLeave);
            currentElementDiv.addEventListener("drop",drop);
            currentElementDiv.addEventListener("dragend",deleteRootNode);

            currentElementText.addEventListener("click",clickCallback);
            currentElementText.innerText = curr.num + " " + curr.name;
            /*currentElementNum.innerText = curr.num + " ";
            currentElementName.innerText = curr.name;
            currentElementName.addEventListener("click",clickCallback);
            currentElementDiv.appendChild(currentElementNum);
            currentElementDiv.appendChild(currentElementName);
            */
            currentElementLi.classList.add("treeElement");
            currentElementDiv.appendChild(currentElementText);
            currentElementLi.appendChild(currentElementDiv);
            parentNode.appendChild(currentElementLi);

            currentElementLi.setAttribute("idcategory",curr.ID_Category);
            currentElementLi.setAttribute("idparent", curr.parent);

            if(curr.childrenList.length > 0) {
                const childrenList = document.createElement("ul");

                currentElementLi.appendChild(childrenList);
                curr.childrenList.forEach(function(x) {
                    self.printTreeElement(x,childrenList,clickCallback);
                });
            }
        }

        /**
         * Used to refresh the tree with the saved tree
         * @param list the tree to display
         * @param clickCallback the callback to call when an element of the tree is clicked
         */
        this.refreshTree = function(list, clickCallback) {
            var self = this;
            rootList.innerHTML = ""; // empty the tree
            const topCategories = list.childrenList;
            topCategories.forEach(function(curr) {
                self.printTreeElement(curr,rootList, clickCallback);
            });
        }
        /**
         * Reset to default all the errors of the component
         */
        this.resetError = function() {
            treeTextError.classList.remove("show");
            treeTextError.classList.add("hide");
            treeTextError.innerText = "";
        }
        /**
         * Set a new error so that the user can understand what is happening
         * @param textError the text to display to the user as an error
         */
        this.setError = function(textError) {
            treeTextError.classList.remove("hide");
            treeTextError.classList.add("show");
            treeTextError.innerText = textError;
        }
        /**
         * Callback to handle the click event of an element of the tree to rename a category right away
         * @param e the click event that was triggered
         */
        var renameCategoryOnline = (e) => {
            e.stopPropagation();
            var self = this;

            currentTree = buildTreeFromHTML(rootList);

            if(!checkTreeIDs(self.categoryList,rootList)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
                return;
            }
            const clickedElement = e.target.closest("h3");
            const [num, ...name] = clickedElement.innerText.split(" ");
            const currText = name.join(" ");
            const elementContainer = clickedElement.parentElement;
            const currNum = document.createElement("h3");
            currNum.innerText = num;
            elementContainer.classList.add("renaming");
            const newNameInput = document.createElement("input");
            newNameInput.type = "text";
            newNameInput.value = currText;

            elementContainer.removeChild(clickedElement);
            elementContainer.appendChild(currNum);
            elementContainer.appendChild(newNameInput);

            newNameInput.focus();
            elementContainer.draggable = false;

            newNameInput.addEventListener("blur", function sendServer(ev) {
                ev.stopPropagation();
                const newName = newNameInput.value;
                const ID_Category = elementContainer.parentElement.getAttribute("idcategory");
                const curr = parseFloat(searchCategoryById(self.categoryList, ID_Category));
                if(curr.name === newName)
                    pageManager.refresh(self.categoryList);
                if(!checkTreeIDs(self.categoryList,rootList)) {
                    self.setError("The tree was corrupted, try again");
                    pageManager.refresh(self.categoryList);
                } else if(isRoot(parseFloat(ID_Category))) {
                    self.setError("Can't rename the root category!");
                    pageManager.refresh(self.categoryList);
                } else if(isStringBlank(newName)) {
                    self.setError("The new name is blank!");
                    pageManager.refresh(self.categoryList);
                } else {
                    let data = {
                        dataToCheck: {
                            clientTree: currentTree,
                            options: buildListFromOptions(document.getElementsByClassName("newCategoryInput")[1])
                        },
                        renamed: {
                            ID_Category: ID_Category,
                            name: newName
                        },
                    };
                    makeCall("POST","RenameCategory",JSON.stringify(data),function(req) {
                        const response = JSON.parse(req.responseText);
                        switch (req.status) {
                            case 200:
                                const selectedCategory = searchCategoryById(categoryTree.categoryList, response.ID_Category);
                                selectedCategory.name = response.name;
                                pageManager.refresh(self.categoryList);
                                break;
                            case 400:
                            case 401:
                            case 403:
                            case 500:
                                self.setError(response.textError);
                                pageManager.refresh(self.categoryList);
                                break;
                        }
                    })
                }
            })
        }
        /**
         * Callback to handle the click event of an element of the tree to rename a category in local mode
         * @param e the click event that was triggered
         */
        var renameCategoryLocal = (e) => {
            e.stopPropagation();
            var self = this;

            if(!checkTreeIDs(self.categoryList,rootList)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
                return;
            }

            const clickedElement = e.target.closest("h3");
            const [num, ...name] = clickedElement.innerText.split(" ");
            const currText = name.join(" ");
            const elementContainer = clickedElement.parentElement;
            const currNum = document.createElement("h3");
            currNum.innerText = num;
            elementContainer.classList.add("renaming");
            const newNameInput = document.createElement("input");
            newNameInput.type = "text";
            newNameInput.value = currText;

            elementContainer.removeChild(clickedElement);
            elementContainer.appendChild(currNum);
            elementContainer.appendChild(newNameInput);

            newNameInput.focus();
            elementContainer.draggable = false;
            newNameInput.addEventListener("blur", function addToRenamed(ev) {
                ev.stopPropagation();
                const newName = newNameInput.value;
                const ID_Category = parseFloat(elementContainer.parentElement.getAttribute("idcategory"));

                if(!checkTreeIDs(self.categoryList,rootList)) {
                    self.setError("The tree was corrupted, try again");
                    pageManager.refresh(self.categoryList);
                } else if(isRoot(ID_Category)) {
                    self.setError("Can't rename the root category!");
                    pageManager.refresh(self.categoryList);
                } else if(isStringBlank(newName)) {
                    self.setError("The new name is blank!");
                    pageManager.refresh(self.categoryList);
                } else {
                    const category = searchCategoryById(self.categoryList, ID_Category);
                    if(category.name !== newName) {
                        category.name = newName;
                        if(!contains(newCategories,category))
                            if(!contains(renamedCategories,category))
                                renamedCategories.push(category);
                    }
                    pageManager.refresh(self.categoryList);
                }
            })
        }
        /**
         * Function to display and handle the modal to confirm or cancel the drag and drop that was just made
         * @param source the source category of drag and drop
         * @param destination the destination category of the drag and drop
         */
        var showDragDropModal = (source,destination) => {
            var self = this;
            // show the drag and drop modal
            dragDropModal.classList.remove("hide");
            dragDropModal.classList.add("show");

            const confirmButton = dragDropModal.querySelector(".confirm");
            const cancelButton = dragDropModal.querySelector(".cancel");

            confirmButton.addEventListener("click",function confirmCallback() {
                //currentTree = buildTreeFromHTML(rootList);
                options = buildListFromOptions(document.getElementsByClassName("newCategoryInput")[1]);
                copySubTree(source,destination);
                pageManager.setModeToLocal(self.categoryList);
                hideDragDropModal();
                showSaveButton();
                confirmButton.removeEventListener("click",confirmCallback);
            });
            cancelButton.addEventListener("click", function cancelCallback() {
                pageManager.refresh(self.categoryList);
                hideDragDropModal();
                cancelButton.removeEventListener("click",cancelCallback);
            })
        }
        /**
         * Hide from the user the drag and drop modal
         */
        var hideDragDropModal = function() {
            dragDropModal.classList.remove("show");
            dragDropModal.classList.add("hide");
        }

        /**
         * Function to handle the start of a drag and drop process
         * @param e the dragstart event that was triggered
         */
        var dragStart = function(e) {
            draggedElement = e.target.closest(".treeElementContent");
            createRootNode();
        }

        /**
         * Function to handle the dragover event of the drag and drop process
         * @param e the dragover event that was triggered
         */
        var dragOver = (e) => {
            var self = this;
            e.preventDefault()
            const draggedOn = e.target.closest(".treeElementContent");

            const source = searchCategoryById(self.categoryList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategoryById(self.categoryList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

            if(!isChildrenOf(source, destination) && isCopyPossible(source,destination))
                draggedOn.classList.add("textGreen");
        }

        /**
         * Function to handle the dragleave event of the drag and drop process
         * @param e the dragleave event that was triggered
         */
        var dragLeave = function(e) {
            const draggedOn = e.target.closest(".treeElementContent");
            draggedOn.classList.remove("textGreen");
        }

        /**
         * Function to handle the drop event of the drag and drop process
         * @param e the drop event that was triggered
         */
        var drop = (e) => {
            var self = this;
            const draggedOn = e.target.closest(".treeElementContent");
            const source = searchCategoryById(self.categoryList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategoryById(self.categoryList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

            draggedOn.classList.remove("textGreen");

            if(pageManager.currentMode === Modes.ONLINE)
                currentTree = buildTreeFromHTML(rootList);

            if(!checkTreeIDs(self.categoryList,rootList) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
            } else if(isRoot(source.ID_Category)) {
                self.setError("Root category is protected");
            } else if(isChildrenOf(source,destination)) {
                self.setError("Can't copy a subtree inside itself!");
            } else if(!isCopyPossible(source,destination)) {
                self.setError("The destination parent would have too many children!");
            } else {
                showDragDropModal(source,destination);
            }

        }
        /**
         * Copy a source category to a destination category
         * @param source object of the source category
         * @param destination object of the destination category
         */
        var copySubTree = (source,destination) => {
            var self = this;
            if(categoryEquals(source,destination)) {
                self.setError("Can't copy itself");
            } else if(isCopyPossible(destination, source)) {
                const sourceCopy = JSON.parse(JSON.stringify(source));
                destination.childrenList.push(sourceCopy);
                updateCategoryProperties(sourceCopy,destination);
                insertSourceNum(source, sourceCopy);
                self.addCategoryToNew(sourceCopy);
            }
        }
        /**
         * Add a category to the local list of new categories
         * @param elem the object of the category to be added
         */
        this.addCategoryToNew = (elem) => {
            var self = this;

            const parent = searchCategoryById(self.categoryList, elem.parent);

            if(!contains(newCategories,parent)) {
                newCategories.push(elem);
            }

        }
        /**
         * Function used to create the html to represent the root element during a drag and drop
         */
        var createRootNode = () => {
            const treeSection = document.getElementsByClassName("treeSection")[0];
            const treeContainer = document.getElementsByClassName("treeContainer")[0];
            const div = document.createElement("div");
            const h3 = document.createElement("h3");
            div.classList.add("copyInRoot");
            h3.innerText = "...";
            h3.classList.add("treeElementContent");
            div.appendChild(h3);
            div.setAttribute("idcategory","1");
            div.draggable = true;
            div.addEventListener("dragover", dragOver);
            div.addEventListener("dragleave",dragLeave);
            div.addEventListener("drop",drop);
            treeSection.insertBefore(div, treeContainer);

        };
        /**
         * Remove the root node from the DOM
         */
        var deleteRootNode = () => {
            const rootNode = document.querySelector(".copyInRoot");
            rootNode.remove();
        }
        /**
         * Refresh the component in ONLINE mode
         * @param categoryList the list to display
         */
        this.refreshOnline = (categoryList) => {
            var self = this;
            self.refreshTree(categoryList,renameCategoryOnline);
        }
        /**
         * Refresh the component in LOCAL mode
         * @param categoryList the list to display
         */
        this.refreshLocal = (categoryList) => {
            var self = this;
            self.refreshTree(categoryList,renameCategoryLocal);
        }
        /**
         * Show the save button to save the changes made in LOCAL mode
         */
        var showSaveButton = () => {
            saveDragButton.classList.remove("hide");
            saveDragButton.classList.add("show");
        }
        /**
         * Hide the save button
         */
        var hideSaveButton = () => {
            saveDragButton.classList.remove("show");
            saveDragButton.classList.add("hide");
        }
        /**
         * Function to handle the click of the save button. Saves the local changes on the server
         */
        var handleSaveButtonClick = () => {
            var self = this;

            if(!checkTreeIDs(self.categoryList,rootList) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
            }

            //currentTree = buildTreeFromHTML(rootList);

            let data = {
                dataToCheck: {
                    clientTree: currentTree,
                    clientTreeSAVED: self.categoryList,
                    options: options
                },
                newCategories: newCategories,
                renamedCategories: renamedCategories
            }


            makeCall("POST","ApplyChanges",JSON.stringify(data),function(req) {
                console.log(req.responseText);
                const response = JSON.parse(req.responseText);
                switch (req.status) {
                    case 200:
                        self.init();
                        pageManager.setModeToOnline(self.categoryList);
                        newCategories = [];
                        renamedCategories = [];

                        break;
                    case 400:
                    case 403:
                    case 500:
                        self.setError(response.textError);
                        self.init();
                        pageManager.setModeToOnline(self.categoryList);
                        newCategories = [];
                        renamedCategories = [];
                        break;
                }
            })
            hideSaveButton();
        }
    }

    function NewCategoryForm() {
        const form = document.getElementById("newCategoryForm");
        const nameInput = document.getElementsByClassName("newCategoryInput")[0];
        const parentInput = document.getElementsByClassName("newCategoryInput")[1];
        const textError = document.querySelector("#newCategoryTextError");
        /**
         * Initializes the NewCategoryForm component. Does not handle the options of the select element in the form
         */
        this.init = function() {
            var self = this;
            form.addEventListener("submit",self.insertNewCategoryOnline);
            document.querySelector(".submitNewCategoryButton").addEventListener("click",(e) => {
                e.stopPropagation();
            })
        }
        /**
         * Adds to the select element of the form an option relative to a category
         * @param curr the object of the category to be displayed as option
         */
        this.addOptions = function(curr) {
            var self = this;
            const currOption = document.createElement("option");
            currOption.value = curr.ID_Category;
            currOption.innerText = curr.num + " " + curr.name;
            parentInput.appendChild(currOption);

            const children = curr.childrenList;
            if(children.length > 0) {
                children.forEach(function(child){ self.addOptions(child) });
            }
        }
        /**
         * Refresh the options list given a tree of categories
         * @param categoryList the tree that contains the categories to display
         */
        this.refreshOptions = function(categoryList) {
            parentInput.innerHTML = "";
            this.addOptions(categoryList);
        }
        /**
         * Reset all the errors of the component
         */
        this.resetError = function() {
            textError.classList.remove("show");
            textError.classList.add("hide");
            nameInput.classList.remove("displayInputError");
            parentInput.classList.remove("displayInputError");
            textError.innerText = "";
        }
        /**
         * Display a new error
         * @param text the text to display the user
         */
        this.setError = function(text) {
            textError.classList.remove("hide");
            textError.classList.add("show");
            textError.innerText = text;
        }
        /**
         * Function to handle the submitting of the form in ONLINE mode
         * @param e the submit event that was triggered on the form
         */
        this.insertNewCategoryOnline = (e) => {
            var self = this;
            e.preventDefault();

            if(!checkTreeIDs(categoryTree.categoryList,document.querySelector("#rootTree")) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("Current tree is invalid, please refresh the page");
                pageManager.refresh(categoryTree.categoryList);
                return;
            }

            if(form.checkValidity()) {
                let newCategory = {
                    name: nameInput.value,
                    parent: parseFloat(parentInput.value),
                    childrenList: []
                };

                if(isStringBlank(newCategory.name)) {
                    self.setError("Given name is not valid!");
                    return;
                }

                const selectedParent = searchCategoryById(categoryTree.categoryList, parseFloat(newCategory.parent));
                if(selectedParent === undefined) {
                    self.setError("Could not find the selected parent");
                    parentInput.classList.add("displayInputError");
                    return;
                } else {

                    const [num, ...name] = parentInput.options[parentInput.selectedIndex].innerText.split(" ");
                    const parent = {
                        ID_Category: newCategory.parent,
                        name: name.join(" "),
                        num: num
                    }

                    if(selectedParent.ID_Category !== parseFloat(parent.ID_Category) || selectedParent.name !== parent.name ||
                        selectedParent.num !== parent.num) {
                        self.setError("Something is wrong. The parent selected is not the one it appears to be");
                        pageManager.refresh(categoryTree.categoryList);
                        return;
                    }
                }
                if(selectedParent.childrenList.length + 1 > 9) {
                    self.setError("Selected parent already have 9 children (9 max)")
                    parentInput.classList.add("displayInputError");
                    return;
                }



                let data = {
                    dataToCheck: {
                        clientTree: buildTreeFromHTML(document.querySelector("#rootTree")),
                        options: buildListFromOptions(parentInput)
                    },
                    newCategory: newCategory
                }

                console.log(data);

                makeCall("POST","CreateNewCategory",JSON.stringify(data),function(req) {
                    console.log(req.responseText);
                    const response = JSON.parse(req.responseText);
                    switch (req.status) {
                        case 200:
                            categoryTree.insertNewCategory(response);
                            pageManager.refresh(categoryTree.categoryList);
                            break;
                        case 400:
                        case 403:
                        case 500:
                            if(response.nameError) {
                                nameInput.classList.add("displayInputError");
                            }
                            if(response.parentError) {
                                parentInput.classList.add("displayInputError");
                            }
                            if(response.inputError) {
                                self.setError(response.inputErrorText);
                            }
                            break;
                    }
                })
            } else {
                form.reportValidity();
            }
        }

        /**
         * Function to handle the submitting of the form in LOCAL mode
         * @param e the submit event that was triggered on the form
         */
        this.insertNewCategoryLocally = (e) => {
            var self = this;
            e.preventDefault();

            if(!checkTreeIDs(categoryTree.categoryList,document.querySelector("#rootTree")) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("Current tree is invalid, please refresh the page");
                pageManager.refresh(categoryTree.categoryList);
                return;
            }

            if(form.checkValidity()) {
                let newCategory = {
                    name: nameInput.value,
                    parent: parseFloat(parentInput.value),
                    childrenList: []
                };

                if(isStringBlank(newCategory.name)) {
                    self.setError("Given name is not valid!");
                    return;
                }

                const selectedParent = searchCategoryById(categoryTree.categoryList, newCategory.parent);
                if(selectedParent === undefined) {
                    self.setError("Could not find the selected parent");
                    parentInput.classList.add("displayInputError");
                    return;
                } else {
                    const [num, ...name] = parentInput.options[parentInput.selectedIndex].innerText.split(" ");
                    const parent = {
                        ID_Category: newCategory.parent,
                        name: name.join(" "),
                        num: num
                    }

                    if(selectedParent.ID_Category !== parent.ID_Category || selectedParent.name !== parent.name ||
                        selectedParent.num !== parent.num) {
                        self.setError("Something is wrong. The parent selected is not the one it appears to be");
                        pageManager.refresh(categoryTree.categoryList);
                        return;
                    }
                }
                if(selectedParent.childrenList.length + 1 > 9) {
                    self.setError("Selected parent already have 9 children (9 max)")
                    parentInput.classList.add("displayInputError");
                    return;
                }

                categoryTree.insertNewCategory(newCategory);
                updateCategoryProperties(newCategory, selectedParent);
                newCategory.sourceNum = newCategory.ID_Category.toString();
                categoryTree.addCategoryToNew(newCategory);
                pageManager.refresh(categoryTree.categoryList);

            } else {
                form.reportValidity();
            }

        }
        /**
         * Refresh the component in LOCAL mode
         * @param categoryList the tree to be used to display the options
         */
        this.refreshLocal = (categoryList) => {
            // change callback to be on local
            var self = this;
            self.refreshOptions(categoryList);
            form.removeEventListener("submit",self.insertNewCategoryOnline);
            form.addEventListener("submit",self.insertNewCategoryLocally);
        }
        /**
         * Refresh the component in ONLINE mode
         * @param categoryList the tree to be used to display the options
         */
        this.refreshOnline = (categoryList) => {
            // change callback to be on online
            var self = this;
            self.refreshOptions(categoryList);
            form.removeEventListener("submit",self.insertNewCategoryLocally);
            form.addEventListener("submit",self.insertNewCategoryOnline);

        }
    }

}