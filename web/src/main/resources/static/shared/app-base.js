/** 兼容随机 context-path：本机根路径为空，外网为 /{随机路径} */
(function (w) {
  var parts = w.location.pathname.split('/').filter(Boolean);
  var base = '';
  if (parts.length && parts[0].indexOf('.') < 0) {
    base = '/' + parts[0];
  }
  w.APP_BASE = base;
  // 与服务端 TableState.TABLE_DIS 保持一致：桌子已解散。
  w.TABLE_STATE_DIS = 9;
  w.appUrl = function (path) {
    if (path == null || path === '') {
      return base || '/';
    }
    if (path.charAt(0) !== '/') {
      path = '/' + path;
    }
    return base + path;
  };

  // 全站按钮防连点：首次点击正常执行，3 秒内再次点击只提示，不重复触发业务事件。
  (function () {
    var lastClick = typeof WeakMap !== 'undefined' ? new WeakMap() : null;
    var cooldown = 3000;
    function tip() {
      var el = document.getElementById('global-click-tip');
      if (!el) {
        el = document.createElement('div');
        el.id = 'global-click-tip';
        el.style.cssText = 'position:fixed;left:50%;top:18%;transform:translate(-50%,-50%);z-index:99999;padding:10px 18px;border-radius:20px;background:rgba(25,25,25,.88);color:#fff;font-size:14px;pointer-events:none;opacity:0;transition:opacity .25s';
        document.body.appendChild(el);
      }
      el.textContent = '请稍后';
      el.style.opacity = '1';
      clearTimeout(el._timer);
      el._timer = setTimeout(function () { el.style.opacity = '0'; }, 900);
    }
    document.addEventListener('click', function (event) {
      var button = event.target.closest && event.target.closest('button');
      if (!button || button.disabled) return;
      var now = Date.now(), previous = lastClick && lastClick.get(button) || button._lastClick || 0;
      if (now - previous < cooldown) {
        event.preventDefault(); event.stopImmediatePropagation(); tip(); return;
      }
      if (lastClick) lastClick.set(button, now); else button._lastClick = now;
    }, true);
  })();
})(window);
