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
/*
console.log("LIST: \n");
console.log(temp);
const x = searchCategory(temp.childrenList,4);
console.log(x);
const y = JSON.parse(JSON.stringify(x));
console.log(y);
x.name = "test";
console.log(x);
console.log(y);
console.log("LIST: \n");
console.log(temp);

temp.childrenList[0].childrenList.push(y);

console.log(searchCategory(temp,1));
console.log(searchCategory(temp,4));
*/

function searchCategory(categoryList, ID_Category) {
    if(categoryList.ID_Category === ID_Category)
        return categoryList;
    if(categoryList.childrenList !== undefined && categoryList.childrenList.length > 0)
        for(let i=0; i<categoryList.childrenList.length; i++) {
            const curr = categoryList.childrenList[i];
            const found = searchCategory(curr,ID_Category);
            if (found)
                return found;
            
        }

    
}

document.getElementById("rootTree").setAttribute("custom","test");


function findCategoryNode(rootList, ID_Category) {

    for(let i=0; i<rootList.lenght; i++) {
        const currElement = rootList.children[i];
        
    }
    

}


document.getElementById("perTest").addEventListener("click", (e) => {
    console.log(1);
});


document.getElementById("perTest").addEventListener("click", (e) => {
    console.log(2);
});



function handleClick(e) {
    var curr = e.target.closest("h1");
    var text = curr.innerText;
    var container = curr.parentElement;

    const newInput = document.createElement("input");
    newInput.type = "text";
    newInput.value = text;
    newInput.addEventListener("blur", handleBlur);
    
    container.removeChild(curr);
    container.appendChild(newInput);
    console.log("CLICKED");
}

function handleBlur(ev) {
    console.log(ev);
    container.removeChild(newInput);
    curr.innerText = newInput.value;
    container.appendChild(curr);
    console.log("blur");
}


function forTest() {
    var curr, container, text;
}


function isAdmissible(node, ids = new Set(), nums = new Set()) {
    if (ids.has(node.ID_Category) || nums.has(parseInt(node.num))) {
        return false;
    }
    ids.add(node.ID_Category);
    nums.add(parseInt(node.num));
    for (let i=0; i<node.childrenList.length; i++) {
        if(node.ID_Category !== node.childrenList[i].parent)
            return false;
        if (!isAdmissible(node.childrenList[i], ids, nums)) {
            return false;
        }
    } 
    return true;  
}



function contains(array, elem) {
    if(array !== undefined && array.length > 0) {
        for(var i=0; i<array.length; i++) {
            const curr = array[i];
            if(categoryEquals(curr,elem))
                return curr;
            }
    }
}

function categoryEquals(x,y) {
    if(x.ID_Category !== y.ID_Category || x.name !== y.name || x.num !== y.num || x.parent !== y.parent)
        return false;
    return true;
}


const x = [{
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
},
{
    "ID_Category": 89,
    "name": "Materiali liquidi",
    "num": "90",
    "parent": 1,
    "childrenList": []
}
];


const y = {
    "ID_Category": 89,
    "name": "Materiali liquidi",
    "num": "90",
    "parent": 1,
    "childrenList": [{
        "ID_Category": 91,
        "name": "Materiali liquidi",
        "num": "90",
        "parent": 1,
        "childrenList": []
    }]
};

const z = [];

x.push(y);
z.push(y);
console.log(x);
console.log(z);
y.childrenList.push({name: "ciaone"});
console.log(x);
console.log(z);
