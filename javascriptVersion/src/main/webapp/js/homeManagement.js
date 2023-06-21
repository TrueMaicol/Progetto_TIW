{
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

        this.start = function() {
            var self = this;
            // start all the listener to the objects
            categoryTree = new CategoryTree();
            newCategoryForm = new NewCategoryForm();
            newCategoryForm.init();
            categoryTree.init();

            window.addEventListener("click",(e) => {
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
                        self.refreshTree(self.categoryList, self.renameCategoryOnline);
                        self.initModal();
                        newCategoryForm.refreshOptions(self.categoryList);
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
            // print a new category inside the tree
            /*const parentId = "#childrenOf" + newCategory.parent;
            const parentNode = document.querySelector(parentId);*/
            if(newCategory.parent === 1) {
                self.categoryList.childrenList.push(newCategory);
            } else {
                const parent = searchCategory(self.categoryList.childrenList, newCategory.parent);
                parent.childrenList.push(newCategory);
            }
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

            currentElementNum.innerText = curr.num + " ";
            currentElementName.innerText = curr.name;
            currentElementLi.classList.add("treeElement");
            currentElementDiv.appendChild(currentElementNum);
            currentElementDiv.appendChild(currentElementName);
            currentElementLi.appendChild(currentElementDiv);
            parentNode.appendChild(currentElementLi);

            currentElementLi.setAttribute("idCategory",curr.ID_Category);

            currentElementName.addEventListener("click",function(e) {
                clickCallback(e);
            });

            if(curr.childrenList.length > 0) {
                const childrenList = document.createElement("ul");
                childrenList.setAttribute("childrenof", curr.parent);
                currentElementLi.appendChild(childrenList);
                curr.childrenList.forEach(function(x) {
                    self.printTreeElement(x,childrenList,clickCallback);
                });
            }

        }
        /**
         * used to refresh the content of tree from scratch
         */
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

        // arrow functions maintain the scope, so this points towards the last this => this = CategoryTree
        this.renameCategoryOnline = (e) => {
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
                    let data = {
                        ID_Category: ID_Category,
                        newName: newText
                    };
                    makeCall("POST","RenameCategory",JSON.stringify(data),function(req) {
                        console.log(req.responseText);
                        const response = JSON.parse(req.responseText);
                        switch (req.status) {
                            case 200:
                                const selectedCategory = searchCategory(categoryTree.categoryList.childrenList, response.ID_Category);
                                selectedCategory.name = response.name;
                                self.refreshTree(categoryTree.categoryList, self.renameCategoryOnline);
                                newCategoryForm.refreshOptions(categoryTree.categoryList);
                                break;
                            case 400:
                            case 401:
                            case 500:
                                self.setError(response.textError);
                                self.refreshTree(categoryTree.categoryList);
                                break;
                        }
                        self.resetError();
                        changingName = false;
                    })
                })
            } else {
                self.setError("Already renaming a category");
            }
        }

        function renameCategoryLocally() {
            // used to rename a cateogry locally
        }

        this.showDragDropModal = function(source,destination) {
            var self = this;
            // show the drag and drop modal
            dragDropModal.classList.remove("hide");
            dragDropModal.classList.add("show");

            //add listeners to buttons
            const confirmButton = dragDropModal.querySelector(".confirm");
            const cancelButton = dragDropModal.querySelector(".cancel");

            confirmButton.addEventListener("click",function confirmCallback(e) {

                copySubTree(source,destination);
                confirmButton.removeEventListener("click",confirmCallback);
                self.hideDragDropModal();
            });
            cancelButton.addEventListener("click", function cancelCallback(e) {
                // close the modal
                self.hideDragDropModal();
                cancelButton.removeEventListener("click",cancelCallback);
            })
        }

        this.hideDragDropModal = function() {
            // hide the drag and drop modal
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

            const source = searchCategory(self.categoryList.childrenList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategory(self.categoryList.childrenList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

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
            const source = searchCategory(self.categoryList.childrenList, parseFloat(draggedElement.parentElement.getAttribute("idcategory")));
            const destination = searchCategory(self.categoryList.childrenList, parseFloat(draggedOn.parentElement.getAttribute("idcategory")));

            deleteRootNode();
            draggedOn.classList.remove("textGreen");

            if(isChildrenOf(source,destination)) {
                // 1
                self.setError("Can't copy a subtree inside itself!");
            } else if(!isCopyPossible(source,destination)) {
                // 2
                self.setError("The destination parent would have too many children!");
            } else {
                // 3
                self.showDragDropModal(source,destination);

            }
            /*
            1. check if dropping inside a children of dragged element. If yes block the copy and display error message
            2. check if the element in which we want to copy the subtree to has enough free slots.
               If yes block the copy and display error message
            3. display the confirmation/cancellation modal
            4. if the update is confirmed
               -> remove the modal
               -> add a copy of the object to the list of current categories
               -> disable new category form
               -> display save button
               -> refresh the tree including the updates with no callback on click
               if the update is cancelled
               -> remove the modal
               -> refresh the tree with rename callback
             */
        }

        var copySubTree = (source,destination) => {
            var self = this;
            if(destination.ID_Category === 1) { //trying to copy to the root
                // copy the source in the root
                if(!isCopyPossible(self.categoryList,source))
                    return;

                const sourceCopy = JSON.parse(JSON.stringify(source));
                destination.childrenList.push(sourceCopy);
                updateCategoryProperties(sourceCopy,destination);
                self.refreshTree(self.categoryList);

            } else if(categoryEquals(source,destination)) {
                self.setError("Can't copy itself");
            } else {
                if(isCopyPossible(destination,source)) {
                    const sourceCopy = JSON.parse(JSON.stringify(source));
                    destination.childrenList.push(sourceCopy);
                    updateCategoryProperties(sourceCopy,destination);
                    self.refreshTree(self.categoryList);
                }
            }
        }

        var createRootNode = () => {
            const treeContainer = document.getElementsByClassName("treeContainer")[0];
            const div = document.createElement("div");
            const h3 = document.createElement("h3");
            div.classList.add("copyInRoot");
            h3.innerText = "...";
            div.appendChild(h3);
            div.setAttribute("idcategory","1");
            treeContainer.insertBefore(div, treeTextError);

        };

        var deleteRootNode = () => {
            const rootNode = document.querySelector(".copyInRoot");
            const treeContainer = document.getElementsByClassName("treeContainer")[0];

            treeContainer.removeChild(rootNode);
        }

    }

    function NewCategoryForm() {
        const form = document.getElementById("newCategoryForm");
        const nameInput = document.getElementsByClassName("newCategoryInput")[0];
        const parentInput = document.getElementsByClassName("newCategoryInput")[1];
        const textError = document.querySelector("#newCategoryTextError");
        this.init = function() {
            var self = this;
            form.addEventListener("submit",function(e) {
                e.preventDefault();
                if(form.checkValidity()) {
                    let newCategory = {};
                    newCategory.name = nameInput.value;
                    newCategory.parent = parentInput.value;
                    makeCall("POST","CreateNewCategory",JSON.stringify(newCategory),function(req) {
                        console.log(req.responseText);
                        const response = JSON.parse(req.responseText);
                        switch (req.status) {
                            case 200:
                                categoryTree.insertNewCategory(response);
                                self.refreshOptions(categoryTree.categoryList);
                                categoryTree.refreshTree(categoryTree.categoryList);
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
            });
        }

        this.addOptions = function(curr) {
            var self = this;
            // add all the options elements inside the select input in the new category form
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
            textError.innerText = "";
        }

        this.setError = function(text) {
            textError.classList.remove("hide");
            textError.classList.add("show");
            textError.innerText = text;
        }
    }

}