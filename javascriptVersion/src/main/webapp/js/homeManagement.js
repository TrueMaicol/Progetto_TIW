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
        var copyTo = false;

        var newLocalCategory = {};

        this.start = function() {
            // start all the listener to the objects
            categoryTree = new CategoryTree();
            newCategoryForm = new NewCategoryForm();
            newCategoryForm.init();
            categoryTree.init();

        };
        this.newLocalCategory = function() {
            // insert a new category inside the newLocalCategory list
        };
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
        this.categoryList = {};
        let copyTo = false;
        const rootList = document.getElementById("rootTree");
        const treeTextError = document.getElementById("treeTextError");
        let changingName = false;
        /**
         * initialize the CategoryTree component
         */
        this.init = function() {
            this.initializeTree();
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
         * ask the server for the complete tree and print it
         */
        this.initializeTree = function() {
            let self = this;
            // get the tree from the database
            makeCall("GET", "GetTree",null,function(req) {
                const response = JSON.parse(req.responseText);
                switch(req.status) {
                    case 200:
                        self.categoryList = response;
                        self.refreshTree(self.categoryList);
                        newCategoryForm.refreshOptions(self.categoryList);
                        break;
                    case 400:
                    case 401:
                        treeTextError.style.display = "block";
                        treeTextError.innerText = response.textError;
                        break;
                    case 500:
                        treeTextError.style.display = "block";
                        treeTextError.innerText = response.textError;
                        break;
                }
            })
        }

        /**
         * used to print the subtree with curr as its root node
         * @param curr the root node of the tree to print
         * @param parentNode the HTMLElement of the parent to contain the tree
         */
        this.printTreeElement = function(curr, parentNode) {
            // the parent element is the HTMLElement of the parent in which we have to create the new nested list if parent has any children
            const currentElementLi = document.createElement("li");
            const currentElementNum = document.createElement("h3");
            const currentElementName = document.createElement("h3");
            const currentElementDiv = document.createElement("div");
            currentElementDiv.classList.add("treeElementContent");
            currentElementNum.innerText = curr.num + " ";
            currentElementName.innerText = curr.name;
            currentElementLi.classList.add("treeElement");
            currentElementDiv.appendChild(currentElementNum);
            currentElementDiv.appendChild(currentElementName);
            currentElementLi.appendChild(currentElementDiv);
            parentNode.appendChild(currentElementLi);

            currentElementLi.setAttribute("idCategory",curr.ID_Category);
            currentElementName.addEventListener("click",(e) => {
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

                    newInput.addEventListener("blur", (ev) => {
                        const newText = newInput.value;

                        const ID_Category = elementContainer.parentElement.getAttribute("idcategory");
                        let data = {
                            ID_Category: ID_Category,
                            newName: newText
                        };
                        console.log(data);
                        makeCall("POST","RenameCategory",JSON.stringify(data),(req) => {
                            console.log(req.responseText);
                            const response = JSON.parse(req.responseText);
                            switch (req.status) {
                                case 200:
                                    const selectedCategory = searchCategory(categoryTree.categoryList.childrenList, response.ID_Category);
                                    selectedCategory.name = response.name;
                                    this.refreshTree(categoryTree.categoryList);
                                    newCategoryForm.refreshOptions(categoryTree.categoryList);
                                    break;
                                case 400:
                                case 401:
                                case 500:
                                    treeTextError.style.display = "block";
                                    treeTextError.innerText = response.textError;
                                    this.refreshTree(categoryTree.categoryList);
                                    break;
                            }
                            treeTextError.style.display = "none";
                            treeTextError.innerText = "";
                            changingName = false;
                        })
                    })
                } else {
                    treeTextError.style.display = "block";
                    treeTextError.innerText = "Already renaming a category";
                }



            })



            if(curr.childrenList.length > 0) {
                const childrenList = document.createElement("ul");
                childrenList.id = "childrenOf" + curr.parent;
                currentElementLi.appendChild(childrenList);
                curr.childrenList.forEach((x) => {
                    this.printTreeElement(x,childrenList);
                });
            }

        }
        /**
         * used to refresh the content of tree from scratch
         */
        this.refreshTree = function(list) {
            let self = this;
            rootList.innerHTML = ""; // empty the tree
            const topCategories = list.childrenList;
            topCategories.forEach((curr) => {
                self.printTreeElement(curr,rootList);
            });
        }

        this.resetError = function() {
            treeTextError.style.display = "none";
            treeTextError.innerText = "";
        }
    }

    function NewCategoryForm() {
        const form = document.getElementById("newCategoryForm");
        const nameInput = document.getElementsByClassName("newCategoryInput")[0];
        const parentInput = document.getElementsByClassName("newCategoryInput")[1];
        const textError = document.querySelector("#newCategoryTextError");
        this.init = function() {

            form.addEventListener("submit",(e) => {
                // new category process
                e.preventDefault();
                if(form.checkValidity()) {
                    let newCategory = {};
                    newCategory.name = nameInput.value;
                    newCategory.parent = parentInput.value;
                    makeCall("POST","CreateNewCategory",JSON.stringify(newCategory),(req) => {
                        console.log(req.responseText);
                        const response = JSON.parse(req.responseText);
                        switch (req.status) {
                            case 200:
                                categoryTree.insertNewCategory(response);
                                this.refreshOptions(categoryTree.categoryList);
                                categoryTree.refreshTree(categoryTree.categoryList);
                                break;
                            case 400:
                            case 403:
                            case 500:
                                if(response.nameError) {
                                    nameInput.classList.add("displayInputError")
                                }
                                if(response.parentError) {
                                    parentInput.classList.add("displayInputError");
                                }
                                if(response.inputErrorNewCategory) {
                                    textError.style.display = "block";
                                    textError.innerText = response.inputErrorTextNewCategory;
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
            // add all the options elements inside the select input in the new category form
            const currOption = document.createElement("option");
            currOption.value = curr.ID_Category;
            currOption.innerText = curr.num + " " + curr.name;
            parentInput.appendChild(currOption);

            const children = curr.childrenList;
            if(children.length > 0) {
                children.forEach((child) => this.addOptions(child));
            }
        }

        this.refreshOptions = function(categoryList) {
            parentInput.innerHTML = "";
            this.addOptions(categoryList);
        }

        this.resetError = function() {
            textError.style.display = "none";
            textError.innerText = "";
        }
    }

}