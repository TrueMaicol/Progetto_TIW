{
    const pageManager = new PageManager();
    window.addEventListener("load",() => {
        if(sessionStorage.getItem("username") == null)
            window.location.href = "index.html";
        else {
            pageManager.start();
        }
    });


    function PageManager() {
        var copyTo = false;
        const tree = new CategoryTree();
        const newCategoryForm = new NewCategoryForm();
        var newLocalCategory = {};

        this.start = function() {
            // start all the listener to the objects

            tree.init();
            newCategoryForm.init();

        };
        this.newLocalCategory = function() {
            // insert a new category inside the newLocalCategory list
        };
        this.updateTree = function() {
            // used to update the tree on the db with the changes in newLocalCategory
            tree.insertNewCategory(newLocalCategory);
            // makeCall...
        };
    }

    function CategoryTree() {
        var categoryList = {}, copyTo = false;
        const rootList = document.getElementById("rootTree");
        const treeTextError = document.getElementById("treeTextError");
        this.init = function() {
            // print the tree in the page
            this.getTree();
            this.printTree();
            this.addListeners();
        }
        this.addListeners = function() {
            const copyLink = document.getElementsByClassName("copyToLink");
            copyLink.forEach((x) => {
                x.addEventListener("click",(e) => {
                    // process the click of the link
                    if(copyTo){}
                    else{}
                })
            })
        }
        this.insertNewCategory = function(newCategories) {
            // print a new category inside the tree
        }
        this.getTree = function() {
            // get the tree from the database
            makeCall("GET", "GetTree",null,function(req) {
                switch(req.status) {
                    case 200:
                        categoryList = JSON.parse(req.responseText);
                        console.log(categoryList);
                        break;
                    case 400:
                    case 401:
                        console.log("Request status "+req.status);
                        treeTextError.innerText = "Something went wrong with the request";
                        break;
                    case 500:
                        console.log("Request status "+req.status);
                        treeTextError.innerText = "Could not load the tree, try again later";
                        break;
                }
            })
        }
        this.printTree = function() {

        }
    }

    function NewCategoryForm() {
        const form = document.getElementById("newCategoryForm");

        this.init = function() {
            form.addEventListener("submit",(e) => {
                // new category process
            });

        }
    }

}