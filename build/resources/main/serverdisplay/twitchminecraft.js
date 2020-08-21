function update_header(text) {

    if(text === "") {
        getTwitchResponse()
    } else {
        if(text.response_type === "CLAIMED") {
           document.getElementById("header").innerHTML = "Successfully linked " + text.player_name + " to " + text.channel
        } else if(text.response_type === "ALREADY_CLAIMED") {
            document.getElementById("header").innerHTML = "You've already claimed your Twitch rewards! (" + text.channel + ": Tier " + text.tier + ")" + " Expires: " + text.expires
        } else if(text.response_type === "FAILED") {
            document.getElementById("header").innerHTML = "An error occurred. Usually this happens if the link you used has expired, or already been used. Please try again with a new link"
        } else if(text.response_type === "ACCOUNT_USED") {
            document.getElementById("header").innerHTML = "The Twitch account " + text.channel + " is already in use by the user " + text.player_name + "!"
        } else if(text.response_type === "NOT_SUBBED") {
            document.getElementById("header").innerHTML = "The Twitch account " + text.channel + " is not currently subscribed!"
        }
    }
}

function getTwitchResponse() {
    var xmlHttp = new XMLHttpRequest();

    xmlHttp.onreadystatechange = function() {
        if(this.readyState === 4 && this.status === 200) {
            var json = JSON.parse(xmlHttp.responseText)

            if(json.response_type === "WAITING") {
                console.log("Trying again in 1 second.")
                setTimeout(getTwitchResponse(), 1000);
            } else {
                update_header(json)
            }
        }
    }

    xmlHttp.open("GET", "/twitchresponse?code=" + new URLSearchParams(window.location.search).get("code"), true);
    xmlHttp.send();
}
