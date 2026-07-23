/**
 * 斗地主/麻将房间页公共逻辑。
 * 页面加载完成后才调用 loadRooms；大厅首页不会引入本脚本，因此不会提前请求房间。
 */
(function () {
    var sessionId = localStorage.getItem('sessionId');
    var gameType = parseInt(document.body.dataset.gameType, 10);
    var gameName = gameType === 2 ? '斗地主' : '麻将';
    if (!sessionId) { window.location.href = appUrl('/'); return; }
    document.getElementById('userDisplay').textContent = localStorage.getItem('nickname') || localStorage.getItem('username') || '玩家';

    function errorText(message) { return '<div class="empty error">' + (message || '加载失败，请稍后重试') + '</div>'; }

    /** 协议暂未携带完整 TableModel，列表先展示固定模板的核心规则，避免重复请求后台配置。 */
    function ruleTip(room) {
        if (room.roomId >= 10000) return '自定义规则 · 以创建房间时的配置为准';
        return gameType === 2
            ? '经典斗地主 · 3人 · 17张手牌 · 3张底牌 · 轮流叫/抢地主'
            : '经典麻将 · 3人 · 13张手牌 · 支持吃/碰/杠 · 4局';
    }

    window.loadRooms = function () {
        var list = document.getElementById('roomList');
        list.innerHTML = '<div class="loading">正在加载' + gameName + '房间…</div>';
        fetch(appUrl('/api/rooms?sessionId=' + encodeURIComponent(sessionId)))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.code === 401) { window.location.href = appUrl('/'); return; }
                if (data.code !== 0) { list.innerHTML = errorText(data.msg); return; }
                renderRooms((data.rooms || []).filter(function (room) { return room.gameType === gameType; }));
            })
            .catch(function () { list.innerHTML = errorText('网络错误，请点击右上角重试'); });
    };

    function renderRooms(rooms) {
        var list = document.getElementById('roomList');
        if (!rooms.length) { list.innerHTML = '<div class="empty">暂无' + gameName + '房间模板</div>'; return; }
        list.innerHTML = '';
        rooms.forEach(function (room) {
            var card = document.createElement('article'); card.className = 'room-card';
            var title = document.createElement('h2'); title.textContent = gameName + ' · 模板 #' + room.roomId; card.appendChild(title);
            var desc = document.createElement('p'); desc.textContent = ruleTip(room); card.appendChild(desc);
            var tableList = document.createElement('div'); tableList.className = 'table-list';
            (room.tables || []).forEach(function (table) {
                var row = document.createElement('div'); row.className = 'table-item';
                var info = document.createElement('span'); info.className = 'table-info';
                var names = (table.players || []).map(function (p) { return p.nickName || ('玩家' + p.roleId); });
                info.textContent = '桌号 ' + table.tableId + ' · ' + (names.join('、') || '空桌') + ' · ' + table.playerCount + '人';
                var state = document.createElement('span'); state.className = 'table-status ' + (table.stat === 0 ? 'waiting' : 'playing');
                state.textContent = table.stat === 0 ? '等待中' : '游戏中'; row.appendChild(info); row.appendChild(state); tableList.appendChild(row);
            });
            card.appendChild(tableList);
            var button = document.createElement('button'); button.className = 'join'; button.textContent = room.myTableId ? '返回我的牌桌' : '进入房间';
            button.onclick = function () { room.myTableId ? goTable(room.myTableId, room.roomId) : joinRoom(room.roomId); };
            card.appendChild(button); list.appendChild(card);
        });
    }

    function joinRoom(roomId) {
        fetch(appUrl('/api/rooms/join'), { method: 'POST', headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({sessionId: sessionId, roomId: roomId}) })
            .then(function (r) { return r.json(); })
            .then(function (data) { if (data.code === 0) goTable(data.tableId, roomId); else alert(data.msg || '进入房间失败'); })
            .catch(function () { alert('网络错误，请重试'); });
    }
    function goTable(tableId, roomId) {
        localStorage.setItem('tableId', tableId); localStorage.setItem('roomId', roomId); localStorage.setItem('gameType', gameType);
        window.location.href = appUrl(gameType === 2 ? '/doudizhu.html' : '/mahjong.html');
    }
    window.logout = function () {
        fetch(appUrl('/api/logout'), {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({sessionId:sessionId})}).catch(function () {});
        localStorage.clear(); window.location.href = appUrl('/');
    };
    // 仅在具体游戏房间页调用一次，避免首页轮询和无意义的 Gate 请求。
    loadRooms();
})();
