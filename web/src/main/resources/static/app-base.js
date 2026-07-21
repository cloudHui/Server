/** 兼容随机 context-path：本机根路径为空，外网为 /{随机路径} */
(function (w) {
  var parts = w.location.pathname.split('/').filter(Boolean);
  var base = '';
  if (parts.length && parts[0].indexOf('.') < 0) {
    base = '/' + parts[0];
  }
  w.APP_BASE = base;
  w.appUrl = function (path) {
    if (path == null || path === '') {
      return base || '/';
    }
    if (path.charAt(0) !== '/') {
      path = '/' + path;
    }
    return base + path;
  };
})(window);
