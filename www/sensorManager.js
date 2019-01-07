var DeviceSensorLoader = function(require, exports, module) {
  var exec = require('cordova/exec');

  var intervalId;
  
  function DeviceSensor() {}

  DeviceSensor.prototype.start = function(success, failure, timeOffset) {
    exec(success, failure, 'AndroidSensorManager', 'start', []);
    intervalId = setInterval(function() {
      exec(success, failure, 'AndroidSensorManager', 'getCurrent', []);
    }, timeOffset || 500);
  };

  DeviceSensor.prototype.stop = function(success, failure) {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
    exec(success, failure, 'AndroidSensorManager', 'stop', []);
  };
  
  var deviceSensor = new DeviceSensor();
  module.exports = deviceSensor;
};

DeviceSensorLoader(require, exports, module);

cordova.define("cordova/plugin/DeviceSensor", DeviceSensorLoader);
