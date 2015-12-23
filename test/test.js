var index = require('./../index');
var assert = require('chai').assert;
var nock = require('nock');

nock('https://slack.com:443', {"encodedQueryParams":true})
  .post('/api/chat.postMessage', "channel=%23sysops&text=test1")
  .query({"token":"xoxp-1234567890-1234567890-1234567890-1234567890"})
  .reply(200, {"ok":true,"channel":"C00000001","ts":"1450864993.000013"});

nock('https://slack.com:443', {"encodedQueryParams":true})
    .post('/api/chat.postMessage', "channel=%23sysops&text=test2")
    .query({"token":"xoxp-1234567890-1234567890-1234567890-1234567890"})
    .reply(200, {"ok":true,"channel":"C00000001","ts":"1450864993.000013"});

describe("Post chat message", function() {
  it("With token - should return ok", function(done) {
    index.handler({
      method: 'chat.postMessage',
      token: 'xoxp-1234567890-1234567890-1234567890-1234567890',
      data: {
        channel: '#sysops',
        text: 'test1'
      }
    }, {
      succeed: function(data) {
        console.log(data);
        assert.ok(data.ok, 'is ok');
        done();
      },
      fail: function(error) {
        assert.fail();
      }
    });
  });
  it("With clientContext token - should return ok", function(done) {
    index.handler({
      method: 'chat.postMessage',
      data: {
        channel: '#sysops',
        text: 'test2'
      }
    }, {
      clientContext: {
        Custom: {
          token: 'xoxp-1234567890-1234567890-1234567890-1234567890'
        }
      },
      succeed: function(data) {
        console.log(data);
        assert.ok(data.ok, 'is ok');
        done();
      },
      fail: function(error) {
        console.log(error);
        assert.fail();
      }
    });
  });
});
