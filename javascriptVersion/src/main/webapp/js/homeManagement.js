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



    function PageManager() {
        var currentMode = Modes.ONLINE;
        this.start = function() {
            var self = this;
            // start all the listener to the objects
            categoryTree = new CategoryTree();
            newCategoryForm = new NewCategoryForm();
            newCategoryForm.init();
            categoryTree.init();

            window.addEventListener("click",function handleResetError(e) {
                e.preventDefault();
                self.resetErrors();
            })
        };

        this.setModeToLocal = (categoryList) => {
            var self = this;
            currentMode = Modes.LOCAL;
            self.refresh(categoryList);
        }

        this.setModeToOnline = (categoryList) => {
            var self = this;
            currentMode = Modes.ONLINE;
            self.refresh(categoryList);
        }

        this.refresh = (categoryList) => {
            if(currentMode === Modes.ONLINE) {
                categoryTree.refreshOnline(categoryList);
                newCategoryForm.refreshOnline(categoryList);
            } else if(currentMode === Modes.LOCAL) {
                categoryTree.refreshLocal(categoryList);
                newCategoryForm.refreshLocal(categoryList);

            }
        }

        this.resetErrors = function() {
            categoryTree.resetError();
            newCategoryForm.resetError();
        }
    }

    function CategoryTree() {

        const rootList = document.getElementById("rootTree");
        const treeTextError = document.getElementById("treeTextError");
        const dragDropModal = document.getElementById("dragDropModal");
        const saveDragButton = document.getElementById("saveDragOperation");
        var newCategories = [], renamedCategories = [];
        let draggedElement;

        this.categoryList = {};
        /**
         * initialize the CategoryTree component
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
         * Insert a new category in the local tree
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
            const currentElementNum = document.createElement("h3");
            const currentElementName = document.createElement("h3");
            const currentElementDiv = document.createElement("div");
            currentElementDiv.classList.add("treeElementContent");

            currentElementDiv.draggable = true;
            currentElementDiv.addEventListener("dragstart",dragStart);
            currentElementDiv.addEventListener("dragover", dragOver);
            currentElementDiv.addEventListener("dragleave",dragLeave);
            currentElementDiv.addEventListener("drop",drop);
            currentElementDiv.addEventListener("dragend",deleteRootNode);
            currentElementNum.innerText = curr.num + " ";
            currentElementName.innerText = curr.name;
            currentElementLi.classList.add("treeElement");
            currentElementDiv.appendChild(currentElementNum);
            currentElementDiv.appendChild(currentElementName);
            currentElementLi.appendChild(currentElementDiv);
            parentNode.appendChild(currentElementLi);

            currentElementLi.setAttribute("idcategory",curr.ID_Category);
            currentElementLi.setAttribute("idparent", curr.parent);
            currentElementName.addEventListener("click",clickCallback);

            if(curr.childrenList.length > 0) {
                const childrenList = document.createElement("ul");

                currentElementLi.appendChild(childrenList);
                curr.childrenList.forEach(function(x) {
                    self.printTreeElement(x,childrenList,clickCallback);
                });
            }

        }

        this.refreshTree = function(list, clickCallback) {
            var self = this;
            rootList.innerHTML = ""; // empty the tree
            const topCategories = list.childrenList;
            topCategories.forEach(function(curr) {
                self.printTreeElement(curr,rootList, clickCallback);
            });
        }
        this.resetError = function() {
            treeTextError.classList.remove("show");
            treeTextError.classList.add("hide");
            treeTextError.innerText = "";
        }
        this.setError = function(textError) {
            treeTextError.classList.remove("hide");
            treeTextError.classList.add("show");
            treeTextError.innerText = textError;
        }
        var renameCategoryOnline = (e) => {
            e.stopPropagation();
            var self = this;

            if(!checkTreeIDs(self.categoryList,rootList)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
                return;
            }
                const clickedElement = e.target.closest("h3");
                const currText = clickedElement.innerText;
                const elementContainer = clickedElement.parentElement;
                const newInput = document.createElement("input");
                newInput.type = "text";
                newInput.value = currText;

                elementContainer.removeChild(clickedElement);
                elementContainer.appendChild(newInput);

                newInput.focus();
                elementContainer.draggable = false;


                newInput.addEventListener("blur", function(ev) {
                    ev.stopPropagation();
                    const newText = newInput.value;
                    const ID_Category = elementContainer.parentElement.getAttribute("idcategory");

                    if(!checkTreeIDs(self.categoryList,rootList)) {
                        self.setError("The tree was corrupted, try again");
                        pageManager.refresh(self.categoryList);
                    } else if(isRoot(parseFloat(ID_Category))) {
                        self.setError("Can't rename the root category!");
                        pageManager.refresh(self.categoryList);
                    } else if(isStringBlank(newText)) {
                        self.setError("The new name is blank!");
                        pageManager.refresh(self.categoryList);
                    } else {
                        let data = {
                            categories: [{
                                    ID_Category: ID_Category,
                                    name: newText
                                }],
                            mode: "ID"
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
                                case 500:
                                    self.setError(response.textError);
                                    pageManager.refresh(self.categoryList);
                                    break;
                            }
                        })
                    }
                })
        }
        var renameCategoryLocal = (e) => {
            e.stopPropagation();
            var self = this;

            if(!checkTreeIDs(self.categoryList,rootList)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
                return;
            }

                const clickedElement = e.target.closest("h3");
                const currText = clickedElement.innerText;
                const elementContainer = clickedElement.parentElement;
                const newInput = document.createElement("input");
                newInput.type = "text";
                newInput.value = currText;

                elementContainer.removeChild(clickedElement);
                elementContainer.appendChild(newInput);

                newInput.focus();
                elementContainer.draggable = false;
                newInput.addEventListener("blur", function(ev) {
                    ev.stopPropagation();
                    const newText = newInput.value;
                    const ID_Category = parseFloat(elementContainer.parentElement.getAttribute("idcategory"));

                    if(!checkTreeIDs(self.categoryList,rootList)) {
                        self.setError("The tree was corrupted, try again");
                        pageManager.refresh(self.categoryList);
                    } else if(isRoot(ID_Category)) {
                        self.setError("Can't rename the root category!");
                        pageManager.refresh(self.categoryList);
                    } else if(isStringBlank(newText)) {
                        self.setError("The new name is blank!");
                        pageManager.refresh(self.categoryList);
                    } else {
                        const category = searchCategoryById(self.categoryList, ID_Category);
                        if(category.name !== newText) {
                            category.name = newText;
                            if(!contains(newCategories,category))
                                if(!contains(renamedCategories,category))
                                    renamedCategories.push(category);
                        }
                        pageManager.refresh(self.categoryList);
                    }
                })


        }

        this.showDragDropModal = function(source,destination) {
            var self = this;
            // show the drag and drop modal
            dragDropModal.classList.remove("hide");
            dragDropModal.classList.add("show");

            const confirmButton = dragDropModal.querySelector(".confirm");
            const cancelButton = dragDropModal.querySelector(".cancel");

            confirmButton.addEventListener("click",function confirmCallback() {

                copySubTree(source,destination);
                pageManager.setModeToLocal(self.categoryList);
                self.hideDragDropModal();
                self.showSaveButton();
                confirmButton.removeEventListener("click",confirmCallback);
            });
            cancelButton.addEventListener("click", function cancelCallback() {
                pageManager.refresh(self.categoryList);
                self.hideDragDropModal();
                cancelButton.removeEventListener("click",cancelCallback);
            })
        }

        this.hideDragDropModal = function() {
            dragDropModal.classList.remove("show");
            dragDropModal.classList.add("hide");
        }
        function dragStart(e) {
            draggedElement = e.target.closest(".treeElementContent");
            createRootNode();
        }
        var dragOver = (e) => {
            var self = this;
            e.preventDefault()
            const draggedOn = e.target.closest(".treeElementContent");

            const source = searchCategoryById(self.categoryList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategoryById(self.categoryList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

            if(!isChildrenOf(source, destination) && isCopyPossible(source,destination))
                draggedOn.classList.add("textGreen");
        }
        function dragLeave(e) {
            const draggedOn = e.target.closest(".treeElementContent");
            draggedOn.classList.remove("textGreen");
        }

        var drop = (e) => {
            var self = this;
            const draggedOn = e.target.closest(".treeElementContent");
            const source = searchCategoryById(self.categoryList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategoryById(self.categoryList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

            draggedOn.classList.remove("textGreen");

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
                self.showDragDropModal(source,destination);
            }

        }

        var copySubTree = (source,destination) => {
            var self = this;
            if(categoryEquals(source,destination)) {
                self.setError("Can't copy itself");
            } else if(isCopyPossible(destination, source)) {
                const sourceCopy = JSON.parse(JSON.stringify(source));
                destination.childrenList.push(sourceCopy);
                updateCategoryProperties(sourceCopy,destination);
                self.addCategoryToNew(sourceCopy);
            }
        }

        this.addCategoryToNew = (elem) => {
            var self = this;

            const parent = searchCategoryById(self.categoryList, elem.parent);

            if(!contains(newCategories,parent)) {
                newCategories.push(elem);
            }

        }

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

        var deleteRootNode = () => {
            const rootNode = document.querySelector(".copyInRoot");
            rootNode.remove();
        }

        this.refreshOnline = (categoryList) => {
            var self = this;
            self.refreshTree(categoryList,renameCategoryOnline);
        }

        this.refreshLocal = (categoryList) => {
            var self = this;
            self.refreshTree(categoryList,renameCategoryLocal);
        }

        this.showSaveButton = () => {
            saveDragButton.classList.remove("hide");
            saveDragButton.classList.add("show");
        }

        this.hideSaveButton = () => {
            saveDragButton.classList.remove("show");
            saveDragButton.classList.add("hide");
        }

        var handleSaveButtonClick = () => {
            var self = this;



            if(!checkTreeIDs(self.categoryList,rootList) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("The tree was corrupted, try again");
                pageManager.refresh(self.categoryList);
            }

            makeCall("POST","AddCategories",JSON.stringify(newCategories),function(req) {

                const response = JSON.parse(req.responseText);
                switch (req.status) {
                    case 200:
                        if(renamedCategories.length > 0) {

                            let data = {
                                categories: renamedCategories,
                                mode: "NUM"
                            }

                            makeCall("POST","RenameCategory",JSON.stringify(data),function(req) {
                                const response = JSON.parse(req.responseText);
                                switch (req.status) {
                                    case 200:
                                        pageManager.setModeToOnline(self.categoryList);
                                        newCategories = [];
                                        renamedCategories = [];
                                        break;
                                    case 400:
                                    case 403:
                                    case 500:
                                        self.init();
                                        self.setError(response.textError);
                                        break;
                                }
                            });
                        } else {
                            self.init();
                            pageManager.setModeToOnline(self.categoryList);
                            newCategories = [];
                            renamedCategories = [];
                        }
                        break;
                    case 400:
                    case 403:
                    case 500:
                        self.setError("There has been an error during the save of the changes!");
                        self.init();
                        pageManager.setModeToOnline(self.categoryList);
                        newCategories = [];
                        renamedCategories = [];
                        break;
                }
            })
            self.hideSaveButton();
        }
    }

    function NewCategoryForm() {
        const form = document.getElementById("newCategoryForm");
        const nameInput = document.getElementsByClassName("newCategoryInput")[0];
        const parentInput = document.getElementsByClassName("newCategoryInput")[1];
        const textError = document.querySelector("#newCategoryTextError");
        this.init = function() {
            var self = this;
            form.addEventListener("submit",self.insertNewCategoryOnline);
            document.querySelector(".submitNewCategoryButton").addEventListener("click",(e) => {
                e.stopPropagation();
            })
        }

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

        this.refreshOptions = function(categoryList) {
            parentInput.innerHTML = "";
            this.addOptions(categoryList);
        }

        this.resetError = function() {
            textError.classList.remove("show");
            textError.classList.add("hide");
            nameInput.classList.remove("displayInputError");
            parentInput.classList.remove("displayInputError");
            textError.innerText = "";
        }

        this.setError = function(text) {
            textError.classList.remove("hide");
            textError.classList.add("show");
            textError.innerText = text;
        }

        this.insertNewCategoryOnline = (e) => {
            var self = this;
            e.preventDefault();
            /*if(!isTreeValid(categoryTree.categoryList,0) || !checkPrintedWithSaved(categoryTree.categoryList,document.querySelector(".rootTreeNode"))) {
                self.setError("The tree is invalid, refresh the page");
                return;
            }*/

            if(!checkTreeIDs(categoryTree.categoryList,document.querySelector("#rootTree")) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("Current tree is invalid, please refresh the page");
                pageManager.refresh(self.categoryList);
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

                makeCall("POST","CreateNewCategory",JSON.stringify(newCategory),function(req) {
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
                            if(response.inputErrorNewCategory) {
                                self.setError(response.inputErrorTextNewCategory);
                            }
                            break;
                    }
                })
            } else {
                form.reportValidity();
            }
        }


        this.insertNewCategoryLocally = (e) => {
            var self = this;
            e.preventDefault();

            /*if(!isTreeValid(categoryTree.categoryList,0) || !checkPrintedWithSaved(categoryTree.categoryList,document.querySelector(".rootTreeNode"))) {
                self.setError("The tree is invalid, refresh the page");
                return;
            }*/

            if(!checkTreeIDs(categoryTree.categoryList,document.querySelector("#rootTree")) || !isTreeValid(categoryTree.categoryList,0)) {
                self.setError("Current tree is invalid, please refresh the page");
                pageManager.refresh(self.categoryList);
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
                categoryTree.addCategoryToNew(newCategory);
                pageManager.refresh(categoryTree.categoryList);

            } else {
                form.reportValidity();
            }

        }

        this.refreshLocal = (categoryList) => {
            // change callback to be on local
            var self = this;
            self.refreshOptions(categoryList);
            form.removeEventListener("submit",self.insertNewCategoryOnline);
            form.addEventListener("submit",self.insertNewCategoryLocally);
        }

        this.refreshOnline = (categoryList) => {
            // change callback to be on online
            var self = this;
            self.refreshOptions(categoryList);
            form.removeEventListener("submit",self.insertNewCategoryLocally);
            form.addEventListener("submit",self.insertNewCategoryOnline);

        }
    }

}