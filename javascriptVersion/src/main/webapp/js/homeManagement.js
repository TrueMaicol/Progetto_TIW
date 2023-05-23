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
        var tree = new CategoryTree();
        var newLocalCategory = {};

        this.start = function() {
            // start all the listener to the objects

            tree.printTree();
            const newCategoryForm = document.getElementById("newCategoryForm");

            newCategoryForm.addEventListener("submit", (e) => {
                // handle new category process
                e.preventDefault();
                const name = document.getElementById("newCategoryName").value;
                const parent = document.getElementById("newCategoryParent").value;

                this.newLocalCategory();
                tree.insertNewCategory(newLocalCategory);
            });


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
        var categoryList = {};
        this.printTree = function() {
            // print the tree in the page
            this.getTree();
            // ...
            this.addListeners();
        }
        this.addListeners = function(callback) {
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
        }
    }

}