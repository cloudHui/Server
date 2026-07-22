/** 休闲小游戏公共：会话校验 + /ws/mini 客户端 */
(function (w) {
  function requireLogin() {
    var sessionId = localStorage.getItem('sessionId');
    if (!sessionId) {
      w.location.href = w.appUrl('/');
      return null;
    }
    return {
      sessionId: sessionId,
      userId: parseInt(localStorage.getItem('userId') || '0', 10),
      nickname: localStorage.getItem('nickname') || localStorage.getItem('username') || '玩家'
    };
  }

  function MiniSocket(onEvent) {
    this.onEvent = onEvent || function () {};
    this.ws = null;
    this.seq = 1;
    this.pending = {};
    this.authed = false;
  }

  MiniSocket.prototype.connect = function (sessionId) {
    var self = this;
    return new Promise(function (resolve, reject) {
      var protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      var url = protocol + '//' + location.host + w.appUrl('/ws/mini');
      self.ws = new WebSocket(url);
      self.ws.onopen = function () {
        self.send('auth', { sessionId: sessionId }).then(function () {
          self.authed = true;
          resolve();
        }).catch(reject);
      };
      self.ws.onerror = function () { reject(new Error('WebSocket 连接失败')); };
      self.ws.onclose = function () {
        self.authed = false;
        self.onEvent({ action: 'closed' });
      };
      self.ws.onmessage = function (ev) {
        var msg;
        try { msg = JSON.parse(ev.data); } catch (e) { return; }
        if (msg.seq && self.pending[msg.seq]) {
          var p = self.pending[msg.seq];
          delete self.pending[msg.seq];
          if (msg.code === 0) p.resolve(msg);
          else p.reject(new Error(msg.msg || '失败'));
        }
        self.onEvent(msg);
      };
    });
  };

  MiniSocket.prototype.send = function (action, data) {
    var self = this;
    var seq = self.seq++;
    return new Promise(function (resolve, reject) {
      if (!self.ws || self.ws.readyState !== 1) {
        reject(new Error('未连接'));
        return;
      }
      self.pending[seq] = { resolve: resolve, reject: reject };
      self.ws.send(JSON.stringify({ action: action, seq: seq, data: data || {} }));
      setTimeout(function () {
        if (self.pending[seq]) {
          delete self.pending[seq];
          reject(new Error('超时'));
        }
      }, 8000);
    });
  };

  MiniSocket.prototype.close = function () {
    if (this.ws) {
      try { this.ws.close(); } catch (e) {}
      this.ws = null;
    }
  };

  w.MiniGames = { requireLogin: requireLogin, MiniSocket: MiniSocket };
})(window);
