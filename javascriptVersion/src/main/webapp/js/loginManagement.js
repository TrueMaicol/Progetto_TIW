(
    function () {
        document.getElementById("loginForm").addEventListener("submit", function(e) {
            e.preventDefault();
            const form = document.getElementById("loginForm");
            if(form.checkValidity()) {
                var jsonData = {};
                const userInput = document.getElementById("userInput");
                const passwordInput = document.getElementById("passwordInput");
                const textError = document.getElementById("textError");
                jsonData.username = userInput.value;
                jsonData.password = passwordInput.value;
                console.log(jsonData);
                resetForm(form);
                makeCall("POST","CheckLogin", JSON.stringify(jsonData), function (req) {
                    const response = JSON.parse(req.responseText);
                    console.log(response);
                    switch(req.status) {
                        case 200: // all good
                            sessionStorage.setItem("user",response.username);
                            window.location.href = "home.html";
                            break;
                        case 400: // bad request
                        case 401: // unauthorized
                            if(response.userError) {
                                userInput.classList.add("displayInputError");
                            }
                            if(response.pswError) {
                                passwordInput.classList.add("displayInputError");
                            }
                            textError.style.display = "block";
                            textError.innerHTML = response.inputErrorText;
                            break;
                        case 404: // resource not found
                            textError.style.display = "block";
                            textError.innerHTML = "Could not reach the server";
                            break;
                        case 500: // server error
                            textError.style.display = "block";
                            textError.innerHTML = "Internal server error, try again later";
                            break;

                    }
                })
            } else {
                form.reportValidity();
            }
        })
    }

)(); // execute the function immediately

function resetForm(form) {
    document.getElementById("userInput").classList.remove("displayInputError");
    document.getElementById("passwordInput").classList.remove("displayInputError");
    document.getElementById("textError").style.display = "none";
    document.getElementById("textError").innerHTML = "";
    form.reset();
}