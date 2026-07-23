/**
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
 * 斗地主/麻将房间页公共逻辑。
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
 * 页面加载完成后才调用 loadRooms；大厅首页不会引入本脚本，因此不会提前请求房间。
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
 */
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
(function () {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    var sessionId = localStorage.getItem('sessionId');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    var gameType = parseInt(document.body.dataset.gameType, 10);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    var gameName = gameType === 2 ? '斗地主' : '麻将';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    if (!sessionId) { window.location.href = appUrl('/'); return; }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    document.getElementById('userDisplay').textContent = localStorage.getItem('nickname') || localStorage.getItem('username') || '玩家';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g

ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    function errorText(message) { return '<div class="empty error">' + (message || '加载失败，请稍后重试') + '</div>'; }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g

ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    /** 协议暂未携带完整 TableModel，列表先展示固定模板的核心规则，避免重复请求后台配置。 */
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    function ruleTip(room) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        if (room.roomId === 9003) return '电脑快速房间 · 叫地主后逆时针抢/再抢 · 抢一次倍数翻倍';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        if (room.roomId >= 10000) return '自定义规则 · 以创建房间时的配置为准';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        return gameType === 2
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            ? '经典斗地主 · 3人 · 17张手牌 · 3张底牌 · 轮流叫/抢地主'
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            : '经典麻将 · 3人 · 13张手牌 · 支持吃/碰/杠 · 4局';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g

ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    window.loadRooms = function () {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        var list = document.getElementById('roomList');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        list.innerHTML = '<div class="loading">正在加载' + gameName + '房间…</div>';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        fetch(appUrl('/api/rooms?sessionId=' + encodeURIComponent(sessionId)))
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            .then(function (r) { return r.json(); })
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            .then(function (data) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                if (data.code === 401) { window.location.href = appUrl('/'); return; }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                if (data.code !== 0) { list.innerHTML = errorText(data.msg); return; }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                renderRooms((data.rooms || []).filter(function (room) { return room.gameType === gameType; }));
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            })
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            .catch(function () { list.innerHTML = errorText('网络错误，请点击右上角重试'); });
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    };
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g

ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    function renderRooms(rooms) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        var list = document.getElementById('roomList');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        if (!rooms.length) { list.innerHTML = '<div class="empty">暂无' + gameName + '房间模板</div>'; return; }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        list.innerHTML = '';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        rooms.forEach(function (room) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            var card = document.createElement('article'); card.className = 'room-card';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            var title = document.createElement('h2');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            title.textContent = gameName + ' · 模板 #' + room.roomId;
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            if (room.roomId === 9001 || room.roomId === 9002 || room.roomId === 9003) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                var fast = document.createElement('span'); fast.className = 'quick-room-badge'; fast.textContent = '快速房间';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                title.appendChild(fast);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            card.appendChild(title);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            var desc = document.createElement('p'); desc.textContent = ruleTip(room); card.appendChild(desc);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            var tableList = document.createElement('div'); tableList.className = 'table-list';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            (room.tables || []).forEach(function (table) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                var row = document.createElement('div'); row.className = 'table-item';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                var info = document.createElement('span'); info.className = 'table-info';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                var names = (table.players || []).map(function (p) { return p.nickName || ('玩家' + p.roleId); });
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                info.textContent = '桌号 ' + table.tableId + ' · ' + (names.join('、') || '空桌') + ' · ' + table.playerCount + '人';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                var state = document.createElement('span'); state.className = 'table-status ' + (table.stat === 0 ? 'waiting' : 'playing');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
                state.textContent = table.stat === 0 ? '等待中' : '游戏中'; row.appendChild(info); row.appendChild(state); tableList.appendChild(row);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            });
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            card.appendChild(tableList);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            var button = document.createElement('button'); button.className = 'join'; button.textContent = room.myTableId ? '返回我的牌桌' : '进入房间';
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            button.onclick = function () { room.myTableId ? goTable(room.myTableId, room.roomId) : joinRoom(room.roomId); };
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            card.appendChild(button); list.appendChild(card);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        });
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g

ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    function joinRoom(roomId) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        fetch(appUrl('/api/rooms/join'), { method: 'POST', headers: {'Content-Type': 'application/json'},
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            body: JSON.stringify({sessionId: sessionId, roomId: roomId}) })
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            .then(function (r) { return r.json(); })
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            .then(function (data) { if (data.code === 0) goTable(data.tableId, roomId); else alert(data.msg || '进入房间失败'); })
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
            .catch(function () { alert('网络错误，请重试'); });
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    function goTable(tableId, roomId) {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        localStorage.setItem('tableId', tableId); localStorage.setItem('roomId', roomId); localStorage.setItem('gameType', gameType);
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        window.location.href = appUrl(gameType === 2 ? '/doudizhu.html' : '/mahjong.html');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    }
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    window.logout = function () {
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        fetch(appUrl('/api/logout'), {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({sessionId:sessionId})}).catch(function () {});
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
        localStorage.clear(); window.location.href = appUrl('/');
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    };
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    // 仅在具体游戏房间页调用一次，避免首页轮询和无意义的 Gate 请求。
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
    loadRooms();
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
})();
ppUrl('/admin.html')#appUrl('/pages/admin/admin.html')#g
ppUrl('/replays.html')#appUrl('/pages/admin/replays.html')#g
