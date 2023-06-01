const temp = {
    "ID_Category": 1,
    "name": "root",
    "num": "0",
    "parent": 0,
    "childrenList": [
        {
            "ID_Category": 2,
            "name": "Materiali solidi",
            "num": "1",
            "parent": 1,
            "childrenList": [
                {
                    "ID_Category": 3,
                    "name": "Materiali inerti",
                    "num": "11",
                    "parent": 2,
                    "childrenList": [
                        {
                            "ID_Category": 6,
                            "name": "Inerti da edilizia",
                            "num": "111",
                            "parent": 3,
                            "childrenList": []
                        }
                    ]
                },
                {
                    "ID_Category": 4,
                    "name": "Materiali ferrosi",
                    "num": "12",
                    "parent": 2,
                    "childrenList": []
                }
            ]
        },
        {
            "ID_Category": 5,
            "name": "Materiali liquidi",
            "num": "2",
            "parent": 1,
            "childrenList": []
        }
    ]
};

console.log(temp, 6);


function searchCategory(categoryList, ID_Category) {
    
    for(let i=0; i<categoryList.length; i++) {
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

document.getElementById("rootTree").setAttribute("custom","test");


function findCategoryNode(rootList, ID_Category) {

    for(let i=0; i<rootList.lenght; i++) {
        const currElement = rootList.children[i];
        
    }
    

}


document.getElementById("perTest").addEventListener("click", (e) => {
    const curr = e.target.closest("h1");
    const text = curr.innerText;
    const container = curr.parentElement;

    const newInput = document.createElement("input");
    newInput.type = "text";
    newInput.value = text;
    newInput.addEventListener("blur", (ev) => {
        container.removeChild(newInput);
        curr.innerText = newInput.value;
        container.appendChild(curr);
        console.log("blur");
    })
    
    container.removeChild(curr);
    container.appendChild(newInput);
    console.log("CLICKED");
});
