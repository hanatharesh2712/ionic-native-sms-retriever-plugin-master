var SmsRetrieverLoader = function(require, exports, module) {
  var exec = require('cordova/exec');

  function SmsRetriever() {}

  SmsRetriever.prototype.startWatching = function(success, failure) {
    exec(success, failure, 'AndroidSmsRetriever', 'start', []);
  };
  
  var smsRetriever = new SmsRetriever();
  module.exports = smsRetriever;
};

SmsRetrieverLoader(require, exports, module);
 
cordova.define("cordova/plugin/SmsRetriever", SmsRetrieverLoader);
