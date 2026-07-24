/**
 * 牌桌页公共能力：会话、WebSocket、导航、中央提示、小结算壳、准备。
 * 玩法页只保留专属逻辑，避免 doudizhu/mahjong/game 三份拷贝。
 */
(function (w) {
  'use strict';

  function loadSession() {
    return {
      sessionId: localStorage.getItem('sessionId'),
      userId: parseInt(localStorage.getItem('userId') || '0', 10),
      nickname: localStorage.getItem('nickname') || '玩家',
      tableId: parseInt(localStorage.getItem('tableId') || '0', 10),
      roomId: parseInt(localStorage.getItem('roomId') || '0', 10)
    };
  }

  function requireSessionOrRedirect(session) {
    if (!session.sessionId || !session.tableId) {
      w.location.href = appUrl('/');
      return false;
    }
    return true;
  }

  function isQuickRobotRoom(roomId) {
    return roomId === 9001 || roomId === 9002 || roomId === 9003;
  }

  function setWsStatus(connected, text) {
    var el = document.getElementById('wsStatus');
    if (!el) return;
    el.textContent = text || (connected ? '已连接' : '已断开');
    el.className = 'ws-status ' + (connected ? 'connected' : 'disconnected');
  }

  /**
   * 创建牌桌 WebSocket 客户端。
   * @param {object} opts
   * @param {string} opts.sessionId
   * @param {function} opts.onAuthed 认证成功回调
   * @param {function} opts.onPush 推送消息回调（非 seq 回调）
   */
  function createGameWs(opts) {
    var ws = null;
    var seqCounter = 0;
    var pending = {};
    var closed = false;

    function send(action, data, callback) {
      var seq = ++seqCounter;
      if (callback) pending[seq] = callback;
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ action: action, seq: seq, data: data || {} }));
      }
      return seq;
    }

    function connect() {
      var protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      ws = new WebSocket(protocol + '//' + location.host + appUrl('/ws/game'));
      ws.onopen = function () {
        setWsStatus(true, '已连接');
        send('auth', { sessionId: opts.sessionId }, function (resp) {
          if (resp.code === 0 && opts.onAuthed) opts.onAuthed(resp);
        });
      };
      ws.onmessage = function (event) {
        var data = JSON.parse(event.data);
        var seq = data.seq;
        if (seq && pending[seq]) {
          pending[seq](data);
          delete pending[seq];
          return;
        }
        if (opts.onPush) opts.onPush(data);
      };
      ws.onclose = function () {
        setWsStatus(false, '已断开');
        if (!closed) setTimeout(connect, 3000);
      };
      ws.onerror = function () {
        console.log('WebSocket错误');
      };
    }

    function stopReconnect() {
      closed = true;
      if (ws) ws.onclose = null;
    }

    return { connect: connect, send: send, stopReconnect: stopReconnect, getWs: function () { return ws; } };
  }

  function showCenterMsg(msg, duration) {
    var el = document.getElementById('centerMsg');
    if (!el) return;
    el.textContent = msg;
    el.className = 'center-message show';
    setTimeout(function () { el.className = 'center-message'; }, duration || 2000);
  }

  function hideActions() {
    var bar = document.getElementById('actionBar');
    if (bar) bar.style.display = 'none';
  }

  function backToLobby() {
    w.location.href = appUrl('/pages/home/room.html');
  }

  function exitRoom(sendFn) {
    sendFn('leave', {}, function (resp) {
      if (resp && resp.code === 0) {
        localStorage.removeItem('tableId');
        w.location.href = appUrl('/pages/home/room.html');
      } else {
        alert((resp && resp.msg) || '退出房间失败');
      }
    });
  }

  function doPrepare(sendFn, onFail) {
    sendFn('op', { choice: 7 }, function (resp) {
      if (resp.code !== 0) {
        showCenterMsg(resp.msg || '准备失败');
        if (onFail) onFail(resp);
      }
    });
    hideActions();
  }

  var settleTimer = null;

  function closeSettle() {
    var overlay = document.getElementById('settleOverlay');
    if (overlay) overlay.className = 'settle-overlay';
    var cd = document.getElementById('settleCountdown');
    if (cd) cd.textContent = '';
    if (settleTimer) {
      clearInterval(settleTimer);
      settleTimer = null;
    }
  }

  /**
   * 展示小结算壳；autoNext 时 15 秒倒计时后自动关闭（开局由服务端超时处理）。
   * @param {object} opt title/meta/rowsHtml/handsHtml/autoNext/onAutoClose
   */
  function showSettle(opt) {
    opt = opt || {};
    document.getElementById('settleTitle').textContent = opt.title || '结算';
    document.getElementById('settleMeta').textContent = opt.meta || '';
    document.getElementById('settleRows').innerHTML = opt.rowsHtml || '';
    var hands = document.getElementById('settleHands');
    if (hands) hands.innerHTML = opt.handsHtml || '';
    document.getElementById('settleOverlay').className = 'settle-overlay show';
    var cd = document.getElementById('settleCountdown');
    if (settleTimer) {
      clearInterval(settleTimer);
      settleTimer = null;
    }
    if (!cd) return;
    if (opt.autoNext) {
      var left = 15;
      cd.textContent = '小结算展示中，' + left + ' 秒后自动准备下一局';
      settleTimer = setInterval(function () {
        left -= 1;
        if (left <= 0) {
          clearInterval(settleTimer);
          settleTimer = null;
          closeSettle();
          if (opt.onAutoClose) opt.onAutoClose();
          return;
        }
        cd.textContent = '小结算展示中，' + left + ' 秒后自动准备下一局';
      }, 1000);
    } else {
      cd.textContent = '请手动准备后开始下一局';
    }
  }

  w.GameTable = {
    loadSession: loadSession,
    requireSessionOrRedirect: requireSessionOrRedirect,
    isQuickRobotRoom: isQuickRobotRoom,
    setWsStatus: setWsStatus,
    createGameWs: createGameWs,
    showCenterMsg: showCenterMsg,
    hideActions: hideActions,
    backToLobby: backToLobby,
    exitRoom: exitRoom,
    doPrepare: doPrepare,
    showSettle: showSettle,
    closeSettle: closeSettle
  };
})(window);
