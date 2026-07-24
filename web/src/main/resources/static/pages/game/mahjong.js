// ==================== 状态 ====================
var session = GameTable.loadSession();
var sessionId = session.sessionId;
var userId = session.userId;
var nickname = session.nickname;
var tableId = session.tableId;

// 与 const.proto Operation 对齐
var OP = {
    PASS: 0,
    PREPARE: 7,
    DISCARD: 13,
    MJ_PENG: 14,
    MJ_GANG: 15,
    MJ_CHI: 16,
    MJ_HU: 17,
    MJ_PASS: 18
};

var gameState = {
    myTiles: [],
    selectedTile: -1,
    players: [],
    myPosition: -1,
    discardedTiles: [],
    wallLeft: 0,
    lastClaimTile: 0,
    roomId: session.roomId,
    autoNextRound: GameTable.isQuickRobotRoom(session.roomId)
};

// 牌ID到图片名的映射
// 编码: suit*100 + value
// 万(101-109) -> B_character_1~9
// 条(201-209) -> B_bamboo_1~9
// 筒(301-309) -> B_dot_1~9
// 风(401-404东南西北) -> B_wind_1~4
// 箭(501-503中发白) -> B_wind_5~7
function getTileImagePath(tileId) {
    var suit = Math.floor(tileId / 100);
    var value = tileId % 100;
    var name = '';
    if (suit === 1) name = 'B_character_' + value + '.png';
    else if (suit === 2) name = 'B_bamboo_' + value + '.png';
    else if (suit === 3) name = 'B_dot_' + value + '.png';
    else if (suit === 4) name = 'B_wind_' + value + '.png';
    else if (suit === 5) name = 'B_wind_' + (value + 4) + '.png';
    else return '';
    // 必须走 appUrl，外网随机 context-path 下绝对 /img 会 404
    return appUrl('/img/card/' + name);
}

// 牌名显示
function getTileName(tileId) {
    var suit = Math.floor(tileId / 100);
    var value = tileId % 100;
    var suitNames = ['', '万', '条', '筒', '', ''];
    var fengNames = ['', '东', '南', '西', '北'];
    var jianNames = ['', '中', '发', '白'];
    if (suit === 4) return fengNames[value] || '';
    if (suit === 5) return jianNames[value] || '';
    return value + suitNames[suit];
}


GameTable.requireSessionOrRedirect(session);
document.getElementById('myName').textContent = nickname;
document.getElementById('roomInfo').textContent = '桌号: ' + tableId;

var gameWs = GameTable.createGameWs({
    sessionId: sessionId,
    onAuthed: function () { enterTable(); },
    onPush: handleWsPush
});
function sendWsMessage(action, data, callback) { return gameWs.send(action, data, callback); }

function handleWsPush(data) {
    switch (data.action) {
        case 'seatUpdate':
            if (data.data && data.data.players) updatePlayers(data.data.players);
            break;
        case 'notCard':
            handleNotCard(data.data);
            break;
        case 'notOp':
            handleNotOp(data.data);
            break;
        case 'notState':
            handleNotState(data.data);
            break;
        case 'notResult':
            handleNotResult(data.data);
            break;
        case 'notMjState':
            handleNotMjState(data.data);
            break;
        case 'notRoundResult':
            handleNotRoundResult(data.data);
            break;
        case 'notGameResult':
            handleNotGameResult(data.data);
            break;
    }
}

// ==================== 游戏逻辑 ====================
function enterTable() {
    sendWsMessage('enterTable', { tableId: tableId }, function(resp) {
        if (resp.code === 0 && resp.data) {
            updatePlayers(resp.data.players || []);
            if (resp.data.tableInfo) {
                document.getElementById('roomInfo').textContent =
                    '桌号: ' + resp.data.tableInfo.tableId;
            }
            showActionButtons('waiting');
        } else {
            showCenterMsg(resp.msg || '进入桌子失败');
        }
    });
}

function handleNotCard(data) {
    // 发牌通知
    if (!data || !data.nCards) return;
    gameState.myTiles = [];
    for (var i = 0; i < data.nCards.length; i++) {
        var nc = data.nCards[i];
        if (nc.roleId === userId && nc.cards) {
            for (var j = 0; j < nc.cards.length; j++) {
                gameState.myTiles.push(nc.cards[j].value);
            }
        }
    }
    // 同色归组，组内从大到小；万、条、筒、风、箭依次排列。
    gameState.myTiles.sort(function(a, b) {
        var sa = Math.floor(a / 100), sb = Math.floor(b / 100);
        return sa - sb || (b % 100) - (a % 100);
    });
    gameState.selectedTile = -1;
    renderMyTiles();
    updateTileCount();
    showCenterMsg('发牌完成');
}

function handleNotOp(data) {
    // 操作通知 - 轮到谁出牌/暗杠等（NotOperation）
    if (!data) return;
    var opSeat = data.opSeat;
    highlightActivePlayer(opSeat);

    if (gameState.myPosition >= 0 && opSeat === gameState.myPosition) {
        showOperationChoices(data.choice || []);
    } else {
        hideActions();
    }
}

function handleNotState(data) {
    if (data && data.state === TABLE_STATE_DIS) {
        gameWs.stopReconnect();
        window.location.href = appUrl('/pages/home/room.html');
        return;
    }
    if (data.state === 1) {
        document.getElementById('tableState').textContent = '游戏进行中';
    } else {
        document.getElementById('tableState').textContent = '等待中';
    }
}

function handleNotMjState(data) {
    // 麻将状态：弃牌广播 + claim（吃碰杠胡）选项
    if (!data) return;
    if (data.tileId) {
        gameState.lastClaimTile = data.tileId;
    }
    // action=DISCARD(13) 时多为有人刚出牌；同步弃牌区（去重末尾）
    if (data.tileId && data.action === OP.DISCARD) {
        var last = gameState.discardedTiles[gameState.discardedTiles.length - 1];
        if (last !== data.tileId) {
            gameState.discardedTiles.push(data.tileId);
            renderDiscarded();
        }
    }
    if (data.wallLeft != null) {
        gameState.wallLeft = data.wallLeft;
        document.getElementById('wallInfo').textContent = '牌墙剩余: ' + data.wallLeft + '张';
    }
    var choices = data.choice || [];
    if (choices.length > 0 && gameState.myPosition >= 0 && data.opSeat === gameState.myPosition) {
        highlightActivePlayer(data.opSeat);
        showOperationChoices(choices);
    }
}

function handleNotResult(data) {
    var msg = '游戏结束!';
    if (data && data.winner && data.winner > 0) {
        msg = '玩家 ' + data.winner + ' 获胜!';
    } else {
        msg = '流局!';
    }
    showCenterMsg(msg, 3000);
    showActionButtons('prepare');
}

function handleNotRoundResult(data) {
    if (!data) return;
    var title = data.winnerSeat < 0 ? '流局' : ('第 ' + data.round + ' 局 · 胡');
    var meta = (data.winnerSeat >= 0
        ? ('座位 ' + data.winnerSeat + ' 胡 · ' + (data.fan || 0) + ' 番 · ' + (data.winType || ''))
        : ('第 ' + data.round + ' 局'));
    var rows = '';
    var scores = data.seatScores || [];
    for (var i = 0; i < scores.length; i++) {
        var sign = scores[i].score > 0 ? '+' : '';
        rows += '<div class="row"><span>座位 ' + scores[i].seat + '</span><span>'
            + sign + scores[i].score + '</span></div>';
    }
    showSettle(title, meta, rows);
    showCenterMsg(title, 2500);
    showActionButtons('prepare');
}

function handleNotGameResult(data) {
    if (!data) return;
    var title = '总结算';
    var meta = '完成 ' + (data.completedRounds || 0) + ' / ' + (data.totalRounds || 0) + ' 局';
    var rows = '';
    var totals = data.totalScores || [];
    for (var i = 0; i < totals.length; i++) {
        rows += '<div class="row"><span>座位 ' + totals[i].seat + '</span><span>'
            + totals[i].score + ' 分</span></div>';
    }
    showSettle(title, meta, rows);
    showActionButtons('prepare');
}

function showSettle(title, meta, rowsHtml) {
    GameTable.showSettle({
        title: title,
        meta: meta,
        rowsHtml: rowsHtml,
        autoNext: gameState.autoNextRound
    });
}

function closeSettle() { GameTable.closeSettle(); }

function choiceCards(choiceObj) {
    var cards = choiceObj && choiceObj.cards ? choiceObj.cards : [];
    var out = [];
    for (var i = 0; i < cards.length; i++) {
        var v = typeof cards[i] === 'object' ? cards[i].value : cards[i];
        if (v != null) out.push({ value: v });
    }
    return out;
}

function removeTilesFromHand(tileValues) {
    for (var i = 0; i < tileValues.length; i++) {
        var idx = gameState.myTiles.indexOf(tileValues[i]);
        if (idx >= 0) gameState.myTiles.splice(idx, 1);
    }
    gameState.selectedTile = -1;
    renderMyTiles();
    updateTileCount();
}

function doOp(choice, cards) {
    cards = cards || [];
    if (choice === OP.DISCARD && cards.length === 0 && gameState.selectedTile >= 0) {
        cards = [{ value: gameState.myTiles[gameState.selectedTile] }];
    }

    sendWsMessage('op', {
        choice: choice,
        cards: cards.length > 0 ? cards : undefined
    }, function(resp) {
        if (resp.code !== 0) {
            showCenterMsg(resp.msg || '操作失败');
            return;
        }
        if (choice === OP.DISCARD && cards.length > 0) {
            removeTilesFromHand([cards[0].value]);
            gameState.discardedTiles.push(cards[0].value);
            renderDiscarded();
        } else if (choice === OP.MJ_CHI || choice === OP.MJ_PENG || choice === OP.MJ_GANG) {
            var vals = [];
            for (var i = 0; i < cards.length; i++) vals.push(cards[i].value);
            // 碰/明杠手牌只扣自己那几张；吃扣 combo 里非弃牌
            if (choice === OP.MJ_PENG) {
                removeTilesFromHand([gameState.lastClaimTile, gameState.lastClaimTile]);
            } else if (choice === OP.MJ_GANG && vals.length === 1) {
                // 暗杠/补杠：目标牌；明杠 claim 时手牌 3 张
                var g = vals[0];
                var cnt = 0;
                for (var j = 0; j < gameState.myTiles.length; j++) {
                    if (gameState.myTiles[j] === g) cnt++;
                }
                var need = [];
                for (var k = 0; k < Math.min(cnt, 4); k++) need.push(g);
                removeTilesFromHand(need);
            } else if (choice === OP.MJ_CHI) {
                var vals = [];
                for (var c = 0; c < cards.length; c++) vals.push(cards[c].value);
                removeTilesFromHand(vals);
            }
        }
    });
    hideActions();
}

function doPrepare() { GameTable.doPrepare(sendWsMessage); }
function backToLobby() { GameTable.backToLobby(); }
function exitRoom() { GameTable.exitRoom(sendWsMessage); }

// ==================== 渲染 ====================
function renderMyTiles() {
    var container = document.getElementById('myTiles');
    container.innerHTML = '';
    for (var i = 0; i < gameState.myTiles.length; i++) {
        var tileId = gameState.myTiles[i];
        var tile = document.createElement('div');
        tile.className = 'tile' + (gameState.selectedTile === i ? ' selected' : '');
        tile.dataset.index = i;
        tile.style.zIndex = String(i + 1);

        var face = document.createElement('div');
        face.className = 'tile-face';
        face.textContent = getTileName(tileId);
        var img = document.createElement('img');
        img.src = getTileImagePath(tileId);
        img.alt = getTileName(tileId);
        img.onload = function() {
            face.style.display = 'none';
            img.style.display = 'block';
        };
        img.onerror = function() {
            // 图片失败时保留中文牌名（旧逻辑 parseInt(alt) 会把「3万」变成纯数字）
            img.style.display = 'none';
            face.style.display = 'flex';
        };
        img.style.display = 'none';
        tile.appendChild(face);
        tile.appendChild(img);

        tile.onclick = (function(idx) {
            return function() {
                toggleTile(idx);
            };
        })(i);

        container.appendChild(tile);
    }
}

function toggleTile(index) {
    if (gameState.selectedTile === index) {
        // 再次点击已选中的牌 = 出牌
        doOp(13); // DISCARD
    } else {
        gameState.selectedTile = index;
        renderMyTiles();
    }
}

function renderCardBacks(containerId, count, isVertical) {
    var container = document.getElementById(containerId);
    container.innerHTML = '';
    for (var i = 0; i < count; i++) {
        var back = document.createElement('div');
        back.className = 'tile-back';
        container.appendChild(back);
    }
}

function renderDiscarded() {
    var container = document.getElementById('discardedArea');
    container.innerHTML = '';
    for (var i = 0; i < gameState.discardedTiles.length; i++) {
        var tileId = gameState.discardedTiles[i];
        var tile = document.createElement('div');
        tile.className = 'tile';
        var face = document.createElement('div');
        face.className = 'tile-face small';
        face.textContent = getTileName(tileId);
        var img = document.createElement('img');
        img.src = getTileImagePath(tileId);
        img.alt = getTileName(tileId);
        img.onload = function() {
            this.previousSibling.style.display = 'none';
            this.style.display = 'block';
        };
        img.onerror = function() {
            this.style.display = 'none';
            this.previousSibling.style.display = 'flex';
        };
        img.style.display = 'none';
        tile.appendChild(face);
        tile.appendChild(img);
        container.appendChild(tile);
    }
}

function updatePlayers(players) {
    gameState.players = players;

    var elTop = document.getElementById('playerTop');
    var elLeft = document.getElementById('playerLeft');
    var elRight = document.getElementById('playerRight');

    elTop.innerHTML = '<span class="name">等待加入</span>';
    elLeft.innerHTML = '<span class="name">等待加入</span>';
    elRight.innerHTML = '<span class="name">等待加入</span>';
    document.getElementById('tilesTop').innerHTML = '';
    document.getElementById('tilesLeft').innerHTML = '';
    document.getElementById('tilesRight').innerHTML = '';

    for (var i = 0; i < players.length; i++) {
        if (players[i].roleId === userId) {
            gameState.myPosition = players[i].position;
            break;
        }
    }

    // 麻将4人: 底部=自己, 对面=+2, 左边=+3, 右边=+1
    for (var i = 0; i < players.length; i++) {
        var p = players[i];
        if (p.roleId === userId) continue;

        var tileCount = p.cardCount || 0;
        var nameHtml = '<span class="name">' + (p.nickName || '玩家') + '</span>' +
            '<span class="tile-count">' + tileCount + '张</span>';

        var relPos = (p.position - gameState.myPosition + 4) % 4;
        if (relPos === 2) {
            elTop.innerHTML = nameHtml;
            renderCardBacks('tilesTop', tileCount, false);
        } else if (relPos === 1) {
            elRight.innerHTML = nameHtml;
            renderCardBacks('tilesRight', tileCount, true);
        } else {
            elLeft.innerHTML = nameHtml;
            renderCardBacks('tilesLeft', tileCount, true);
        }
    }
}

function updateTileCount() {
    document.getElementById('myTileCount').textContent = gameState.myTiles.length + '张';
}

function highlightActivePlayer(position) {
    document.getElementById('playerTop').className = 'player-info';
    document.getElementById('playerLeft').className = 'player-info';
    document.getElementById('playerRight').className = 'player-info';
    document.getElementById('playerBottom').className = 'player-info';

    if (position === gameState.myPosition) {
        document.getElementById('playerBottom').className = 'player-info active';
    } else {
        var relPos = (position - gameState.myPosition + 4) % 4;
        if (relPos === 2) {
            document.getElementById('playerTop').className = 'player-info active';
        } else if (relPos === 1) {
            document.getElementById('playerRight').className = 'player-info active';
        } else {
            document.getElementById('playerLeft').className = 'player-info active';
        }
    }
}

function showOperationChoices(choices) {
    var bar = document.getElementById('actionBar');
    bar.innerHTML = '';
    bar.style.display = 'flex';

    for (var i = 0; i < choices.length; i++) {
        (function(choiceObj) {
            var code = choiceObj.choice;
            var cards = choiceCards(choiceObj);
            var btn = document.createElement('button');
            btn.className = 'action-btn';

            if (code === OP.DISCARD || code === 6) {
                btn.className += ' btn-discard';
                btn.textContent = '出牌';
                btn.onclick = function() {
                    if (gameState.selectedTile >= 0) {
                        doOp(OP.DISCARD);
                    } else {
                        showCenterMsg('请先选择一张牌');
                    }
                };
            } else if (code === OP.PASS || code === OP.MJ_PASS) {
                btn.className += ' btn-pass';
                btn.textContent = '过';
                btn.onclick = function() { doOp(OP.MJ_PASS); };
            } else if (code === OP.MJ_CHI) {
                btn.className += ' btn-chi';
                var chiLabel = '吃';
                if (cards.length > 0) {
                    var names = [];
                    for (var n = 0; n < cards.length; n++) names.push(getTileName(cards[n].value));
                    chiLabel = '吃(' + names.join('') + ')';
                }
                btn.textContent = chiLabel;
                btn.onclick = function() { doOp(OP.MJ_CHI, cards); };
            } else if (code === OP.MJ_PENG) {
                btn.className += ' btn-peng';
                btn.textContent = '碰';
                btn.onclick = function() { doOp(OP.MJ_PENG, cards); };
            } else if (code === OP.MJ_GANG) {
                btn.className += ' btn-gang';
                btn.textContent = cards.length === 1
                    ? ('杠(' + getTileName(cards[0].value) + ')')
                    : '杠';
                btn.onclick = function() { doOp(OP.MJ_GANG, cards); };
            } else if (code === OP.MJ_HU) {
                btn.className += ' btn-hu';
                btn.textContent = '胡';
                btn.onclick = function() { doOp(OP.MJ_HU, cards); };
            } else {
                return;
            }
            bar.appendChild(btn);
        })(choices[i]);
    }
}

function showActionButtons(type) {
    var bar = document.getElementById('actionBar');
    bar.innerHTML = '';
    bar.style.display = 'flex';
    if (type === 'prepare') {
        var btn = document.createElement('button');
        btn.className = 'action-btn btn-prepare';
        btn.textContent = '准备';
        btn.onclick = doPrepare;
        bar.appendChild(btn);
    } else if (type === 'waiting') {
        var tip = document.createElement('span');
        tip.style.cssText = 'color:#fff;font-size:14px;padding:8px 12px;';
        tip.textContent = '等待玩家坐满后自动开局';
        bar.appendChild(tip);
    }
}

function hideActions() { GameTable.hideActions(); }
function showCenterMsg(msg, duration) { GameTable.showCenterMsg(msg, duration); }

// ==================== 启动 ====================
gameWs.connect();
