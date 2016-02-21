/*Initialization*/
const childProcess = require('child_process'), pythonFaceDetection;
pythonFaceDetection = childProcess.exec("python C:/Users/drago/Github/Microshadow/detectTheFace.py", function(code){
    if(code!=null)
        console.log("Error: Failed to launch python script bc of:" + code);
});
var fs = require('fs');
var http = require('http');
var arDrone = require('ar-drone');
var client = arDrone.createClient();
var lastTime = (new Date()).getTime();
var period = 1;
//sets the video stream we get to the bottom facing camera
client.config('video:video_channel', 3);
//makes the drone fly higher by setting its max altitutde to something really big
client.config('control:altitude_max', 100000);
var pngStream = client.getPngStream();
console.log('Connecting png stream ...');
//launch
client.takeoff();
client.after(1000, function(){
    this.land();
});

function savePng () {
    var lastPng;
	pngStream
  	.on('error', console.log)
  	.on('data', function(pngBuffer) {
        var currentTime = (new Date()).getTime();
        if(currentTime - lastTime > period)
        {
            lastTime = currentTime;
            fs.writeFile("frame.png", pngBuffer, function(error)
            {
                if(error)
                    console.log("Couldn't write to PNG because:" + error);
            });
        }
        lastPng = pngBuffer;        
  	});

var server = http.createServer(function(req, res) {
  if (!lastPng) {
    res.writeHead(503);
    res.end('Did not receive any png data yet.');
    return;
  }
  res.writeHead(200, {'Content-Type': 'image/png'});
  res.end(lastPng);
  res.end("Found an image!");
});

server.listen(8080, function() {
    console.log('Serving latest png on port 8080 ...');
});
}

savePng();


/*client
 .after(7500, function() {
 	this.clockwise(0.5);
 })
 .after(3000, function() {
 	this.animate('flipLeft', 15);
 	this.animateLeds('blinkRed',5,2);
 })
  .after(1000, function() {
 	this.stop();
 	this.land();
 });*/
client.on('navdata', console.log);
var pngStream = client.getPngStream();
pngStream.on('data', console.log);
arDrone.createClient(['192.168.1.1', 5, null]);