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
        let newLocalCategory = {};
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
        this.addNewLocalCategory = function(newLocals) {
            // update the newLocalCategory list with the
            newLocalCategory = newLocals;
        };
        this.resetLocalCategories = function() {
            newLocalCategory = {};
        }
        this.updateTree = function() {
            // used to update the tree on the db with the changes in newLocalCategory

            // makeCall...
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
            var self = this;
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
        let changingName = false;
        let draggedElement;

        this.categoryList = {};
        /**
         * initialize the CategoryTree component
         */
        this.init = function() {
            let self = this;
            // get the tree from the database
            makeCall("GET", "GetTree",null,function(req) {
                console.log(req.responseText);
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
            currentElementDiv.addEventListener("dragover", dragEnter);
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

            currentElementLi.setAttribute("idCategory",curr.ID_Category);

            currentElementName.addEventListener("click",clickCallback);

            if(curr.childrenList.length > 0) {
                const childrenList = document.createElement("ul");
                childrenList.setAttribute("childrenof", curr.parent);
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
            var self = this;
            if(!changingName) {
                const clickedElement = e.target.closest("h3");
                const currText = clickedElement.innerText;
                const elementContainer = clickedElement.parentElement;
                const newInput = document.createElement("input");
                newInput.type = "text";
                newInput.value = currText;
                changingName = true;

                elementContainer.removeChild(clickedElement);
                elementContainer.appendChild(newInput);

                newInput.focus();

                newInput.addEventListener("blur", function(ev) {
                    const newText = newInput.value;
                    const ID_Category = elementContainer.parentElement.getAttribute("idcategory");
                    if(parseInt(ID_Category) === 1) {
                        self.setError("Can't rename the root category!");
                        return;
                    }
                    let data = {
                        categories: [
                            {
                                ID_Category: ID_Category,
                                name: newText
                            }],
                        mode: "ID"
                    };
                    makeCall("POST","RenameCategory",JSON.stringify(data),function(req) {
                        console.log(req.responseText);
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
                        changingName = false;
                    })
                })
            } else {
                self.setError("Already renaming a category");
            }
        }
        var renameCategoryLocal = (e) => {
            var self = this;
            if(!changingName) {
                const clickedElement = e.target.closest("h3");
                const currText = clickedElement.innerText;
                const elementContainer = clickedElement.parentElement;
                const newInput = document.createElement("input");
                newInput.type = "text";
                newInput.value = currText;
                changingName = true;

                elementContainer.removeChild(clickedElement);
                elementContainer.appendChild(newInput);

                newInput.focus();

                newInput.addEventListener("blur", function(ev) {
                    const newText = newInput.value;
                    const ID_Category = elementContainer.parentElement.getAttribute("idcategory");
                    if(parseInt(ID_Category) === 1) {
                        self.setError("Can't rename the root category!");
                        return;
                    }
                    if(newText === undefined || isStringBlank(newText)) {
                        self.setError("The new name is blank!");
                        return;
                    }
                    const category = searchCategoryById(self.categoryList,parseFloat(ID_Category));
                    if(category.name !== newText) {
                        category.name = newText;
                        if(!contains(newCategories,category))
                            if(!contains(renamedCategories,category))
                                renamedCategories.push(category);
                    }
                    pageManager.refresh(self.categoryList);
                    console.log("RENAMED:");
                    console.log(renamedCategories);
                    changingName = false;
                })
            } else {
                self.setError("Already renaming a category");
            }

        }

        this.showDragDropModal = function(source,destination) {
            var self = this;
            // show the drag and drop modal
            dragDropModal.classList.remove("hide");
            dragDropModal.classList.add("show");

            const confirmButton = dragDropModal.querySelector(".confirm");
            const cancelButton = dragDropModal.querySelector(".cancel");

            confirmButton.addEventListener("click",function confirmCallback(e) {

                copySubTree(source,destination);
                pageManager.setModeToLocal(self.categoryList);
                self.hideDragDropModal();
                self.showSaveButton();
                confirmButton.removeEventListener("click",confirmCallback);
            });
            cancelButton.addEventListener("click", function cancelCallback(e) {
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
        var dragEnter = (e) => {
            var self = this;
            e.preventDefault()
            const draggedOn = e.target.closest(".treeElementContent");

            const source = searchCategoryById(self.categoryList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategoryById(self.categoryList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

            if(!isChildrenOf(source, destination))
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

            if(!isTreeValid(self.categoryList,0)) {
                self.setError("Current tree is not valid, refresh the page!");
            } else if(source.ID_Category === 1) {
                self.setError("Root category is protected");
            } else if(isChildrenOf(source,destination)) {
                // 1
                self.setError("Can't copy a subtree inside itself!");
            } else if(!isCopyPossible(source,destination)) {
                // 2
                self.setError("The destination parent would have too many children!");
            } else {
                // 3
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
            console.log("CATEGORY LIST:");
            console.log(self.categoryList);
            console.log("NEW CATEGORIES:");
            console.log(newCategories);
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
            div.addEventListener("dragover", dragEnter);
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
            var self = this;
            saveDragButton.classList.remove("hide");
            saveDragButton.classList.add("show");
        }

        this.hideSaveButton = () => {
            saveDragButton.classList.remove("show");
            saveDragButton.classList.add("hide");
        }

        var handleSaveButtonClick = (e) => {
            var self = this;
            console.log("newCategories");
            console.log(JSON.stringify(newCategories));
            makeCall("POST","AddCategories",JSON.stringify(newCategories),function(req) {
                console.log("SUBTREE response");
                console.log(req.responseText);
                const response = JSON.parse(req.responseText);
                switch (req.status) {
                    case 200:
                        if(renamedCategories.length > 0) {
                            /*self.init();
                            for(var i=0; i<renamedCategories.length; i++) {
                                renamedCategories[i].ID_Category = searchCategoryByNum(renamedCategories[i].num).ID_Category;
                                renamedCategories[i].parent = searchCategoryByNum(renamedCategories[i].num).parent;
                            }

                            let data = {
                                categories: renamedCategories,
                                mode: "ID"
                            }*/

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
            if(!isTreeValid(categoryTree.categoryList,0)) {
                self.setError("The tree is invalid, refresh the page");
                return;
            }

            if(form.checkValidity()) {
                let newCategory = {};
                newCategory.name = nameInput.value;
                newCategory.parent = parentInput.value;
                newCategory.childrenList = [];
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
                    const text = parentInput.options[parentInput.selectedIndex].innerText.split(" ");
                    const num = text[0];
                    const name = text[1];
                    const parent = {
                        ID_Category: newCategory.parent,
                        name: name,
                        num: num,
                    }

                    if(selectedParent.ID_Category !== parseFloat(parent.ID_Category) || selectedParent.name !== parent.name ||
                        selectedParent.num !== parent.num) {
                        self.setError("Something is wrong. The parent selected is not the one it appears to be");
                    }
                }
                if(selectedParent.childrenList.length + 1 > 9) {
                    self.setError("Selected parent already have 9 children (9 max)")
                    parentInput.classList.add("displayInputError");
                    return;
                }

                makeCall("POST","CreateNewCategory",JSON.stringify(newCategory),function(req) {
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
            if(form.checkValidity()) {
                let newCategory = {};
                newCategory.name = nameInput.value;
                newCategory.parent = parseFloat(parentInput.value);
                newCategory.childrenList = [];
                const selectedParent = searchCategoryById(categoryTree.categoryList, parseFloat(newCategory.parent));
                if(isStringBlank(newCategory.name)) {
                    self.setError("Given name is not valid!");
                } else if(selectedParent === undefined) {
                    self.setError("Cannot find the parent!");
                } else {
                    const text = parentInput.options[parentInput.selectedIndex].innerText.split(" ");
                    const num = text[0];
                    const name = text[1];
                    const parent = {
                        ID_Category: newCategory.parent,
                        name: name,
                        num: num,
                    }

                    if(selectedParent.ID_Category !== parseFloat(parent.ID_Category) || selectedParent.name !== parent.name ||
                        selectedParent.num !== parent.num) {
                        self.setError("Something is wrong. The parent selected is not the one it appears to be");
                    }

                    if(selectedParent.childrenList.length + 1 > 9) {
                        self.setError("Selected parent already have 9 children (9 max)");
                    } else {
                        categoryTree.insertNewCategory(newCategory);
                        updateCategoryProperties(newCategory, selectedParent);
                        categoryTree.addCategoryToNew(newCategory);
                        pageManager.refresh(categoryTree.categoryList);
                    }
                }

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