var SmsRetrieverLoader = function(require, exports, module) {
  var exec = require('cordova/exec');

  //var intervalId;
  
  function SmsRetriever() {}

  SmsRetriever.prototype.start = function(success, failure, timeOffset) {
    exec(success, failure, 'AndroidSmsRetriever', 'start', []);
    setTimeout(function() {
      exec(success, failure, 'AndroidSmsRetriever', 'getCurrent', []);
    }, timeOffset || 500);
  };

  SmsRetriever.prototype.stop = function(success, failure) {
    // if (intervalId) {
    //   clearInterval(intervalId);
    //   intervalId = null;
    // }
    exec(success, failure, 'AndroidSmsRetriever', 'stop', []);
  };
  
  var smsRetriever = new SmsRetriever();
  module.exports = smsRetriever;
};

SmsRetrieverLoader(require, exports, module);

cordova.define("cordova/plugin/SmsRetriever", SmsRetrieverLoader);
