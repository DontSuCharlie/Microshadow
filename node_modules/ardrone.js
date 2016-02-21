var arDrone = require('ar-drone');
var client = arDrone.createClient();
/*client.config('video:video_channel', 3);
var http = require('http');
var pngStream = client.getPngStream();
console.log('Connecting png stream ...');
client.config('control:altitude_max', 100000);
client.takeoff();

function pictureSave () {
	var lastPng;
	pngStream
  	.on('error', console.log)
  	.on('data', function(pngBuffer) {
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

pictureSave();

*/
client
 .after(7500, function() {
 	this.clockwise(0.5);
 })
 //.after(3000, function() {
 	//this.animate('flipLeft', 15);
 	//this.animateLeds('blinkRed',5,2);
 //})

  .after(1000, function() {
 	this.stop();
 	this.land();
 });
 client.on('navdata', console.log);

 var pngStream = client.getPngStream();
 pngStream.on('data', console.log);
 arDrone.createClient(['192.168.1.1', 5, null]);



