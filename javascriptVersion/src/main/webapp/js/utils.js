
function makeCall(method, url, data, callback) {
    var request = new XMLHttpRequest();
    request.onload = () => callback(request);
    request.open(method,url);
    if(data == null)
        request.send();
    else 
        request.send(data);

}