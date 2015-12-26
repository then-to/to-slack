var https = require('https');
var querystring = require('querystring');
var uriTemplates = require('uri-templates');

var template = uriTemplates("/api/{method}{?token}");

exports.handler = function(event, context) {

  var formData = querystring.stringify(event.params);

  var token = event.token;

  if (context.clientContext !== undefined &&
    context.clientContext.Custom !== undefined &&
    context.clientContext.Custom.token !== undefined) {
    token = context.clientContext.Custom.token;
  }

  var path = template.fill({
    method: event.method,
    token: token
  });

  var options = {
    method: 'POST',
    hostname: 'slack.com',
    port: 443,
    path: path,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(formData)
    }
  };

  var req = https.request(options, function(res) {
    var body = '';
    console.log('Status:', res.statusCode);
    console.log('Headers:', JSON.stringify(res.headers));
    res.setEncoding('utf8');
    res.on('data', function(chunk) {
      body += chunk;
    });
    res.on('end', function() {
      event.result = JSON.parse(body);
      context.succeed(event);
    });
  });
  req.on('error', context.fail);
  req.write(formData);
  req.end();

};
