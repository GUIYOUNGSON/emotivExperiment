console.log("Running server software");
DEBUG = true;
var windowWidth = $(window).width();
var windowHeight = $(window).height();


// Get the id where the stimuli (both pictures) are located
var stimImage1 = $("#stimulus1");
var stimImage2 = $("#stimulus2");

$(window).bind('keypress', function(e) {
     handleKeypress(e.keyCode);
});

var connection;
var DEBUG = true;
var startTrialTime;
var responseTime;
var inInstruct = true;


function handleKeypress(key){
  if(DEBUG) console.log("Got keypress from " + key);
  if(String.fromCharCode(key) == 'q'){
    connection.send("Qq");
    showText("Quit!");
    return;
  }
  if(!inInstruct){
    responseTime = new Date().getTime();
    responseTime = responseTime - startTrialTime;
    connection.send("R," + responseTime);
  }
  // advance to next screen
  else{
    connection.send("C");
  }
}

function showPics(image1, image2, opacity2) {
  // Hide any text first
  $("#instructs").text("");
  stimImage1.attr("src", image1);
  stimImage2.attr("src", image2);
  stimImage2.css("opacity", opacity2);
  $("#image-holder").css('visibility', 'visible');
}

/* Hide image, display instructions */
function showText(text){
  // Hide image
  inInstruct = true;
  $("#image-holder").css('visibility', 'hidden');
  $("#instructs").text(text);
}

/* Hide everything */
function showBlank(){
  $("#image-holder").css('visibility', 'hidden');
  $("#instructs").text("");
}


// Tell backend to start the experiment
function initExperiment(){
  if(!$.connection){
    showText("Not connected to back end server!");
  }
  else{
    $.connection.write("init");
  }
}

/* update the opacity of image 2 */
function updateOpacity(opacity2){
  stimImage2.attr("opacity", opacity2);
}

function startTrial(image1, image2, opacity2){

  startTrialTime = new Date().getTime();
  inInstruct = false;
  console.log("Im 1 is " + image1);
  console.log("Im 2 is " + image2);
  showPics(image1, image2, opacity2);
}




$(function () {
  // if user is running mozilla then use it's built-in WebSocket
  window.WebSocket = window.WebSocket || window.MozWebSocket;

  connection = new WebSocket('ws://127.0.0.1:8885');
  $.conn = connection;
  connection.onopen = function () {
      // connection is opened and ready to use
  };

  connection.onerror = function (error) {
      // an error occurred when sending/receiving data
  };

  /* When a message is recieved from the server, look up the function
  in the instructions table and execute it (if it is a valid
  instruction) */

  connection.onmessage = function (message) {
    if(DEBUG)   console.log("Got command " + message.data);
    var args = message.data.split(",");
    // Show an instruction
    if(args[0] == 'I'){
      showText(args[1]);
    }
    else if(args[0] == 'S'){
      startTrial(args[1], args[2], args[3]);
    }


  };
});
