/* Copyright 2025 RTX BBN Technologies */

export const getData = (key : string) => {
    var data = localStorage.getItem(key);

    if (data != null) {
        return JSON.parse(data);
    }
    return null;
}

export const saveData = (key : string, value : any) => {
    var json = JSON.stringify(value);
    localStorage.setItem(key, json)
}

export const getUrl = (path : string, isWebsockets : boolean) => {
    const api_url = getData("api_url");
    var protocol = "";
    if (window.location.protocol === "https:") {
        if (isWebsockets) {
            protocol = "wss://";
        } else {
            protocol = "https://";
        }
    } else {
        if (isWebsockets) {
            protocol = "ws://"
        } else {
            protocol = "http://"
        }
    }
    var url = `${protocol}${api_url}/${path}`;
    // console.log("URL: " + url);
    return url;
}

export const getTime = (time : Date) => {
    if (time === null || time === undefined) {
        return "";
    } else {
        return time.toLocaleDateString() + " " + 
            time.toLocaleTimeString(navigator.language, 
                {hour: 'numeric', minute: '2-digit'});
    }
}

// For debugging and testing only.
export const generateSimpleHash = () => {
    const str = "abcdef1234567890";
    var result = "";
    for (var i = 0; i < 20; ++i) {
        result += str.charAt(Math.floor(Math.random() * (str.length-1)));
    }
    return result;
}

export const stringToBoolean = (str : string) => {
    return str.toLocaleLowerCase() === "true";
}