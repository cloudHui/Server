// ==================== 状态 ====================
var session = GameTable.loadSession();
var sessionId = session.sessionId;
var userId = session.userId;
var nickname = session.nickname;
var tableId = session.tableId;
var gameState = {
    myCards: [],
    selectedCards: new Set(),
    players: [],
    myPosition: -1,
    lastPlayedCards: [],
    currentOp: null,
    opponentCounts: {},
    landlordId: 0,
    bottomCards: [],
    roomId: session.roomId,
    autoNextRound: false,
    lastChoices: []
};

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
        case 'ackOp':
            if (data.data) handleAckOp(data.data);
            break;
        case 'notState':
            handleNotState(data.data);
            break;
        case 'notResult':
            handleNotResult(data.data);
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
                if (resp.data.tableInfo.roomId) {
                    gameState.roomId = resp.data.tableInfo.roomId;
                    localStorage.setItem('roomId', String(gameState.roomId));
                }
                if (resp.data.tableInfo.landlord) {
                    gameState.landlordId = resp.data.tableInfo.landlord;
                }
            }
            gameState.autoNextRound = GameTable.isQuickRobotRoom(gameState.roomId);
            showActionButtons('waiting');
        } else {
            showCenterMsg(resp.msg || '进入桌子失败');
        }
    });
}

/** 按牌值升序；同牌值按花色：方块→梅花→红桃→黑桃（左到右） */
function sortHandCards(cards) {
    cards.sort(function(a, b) {
        var va = a % 100, vb = b % 100;
        if (va !== vb) return va - vb;
        return Math.floor(a / 100) - Math.floor(b / 100);
    });
}

function handleNotCard(data) {
    // 发牌通知：自己有牌值；roleId=0 为桌面底牌；他人牌值为0但张数有效
    if (!data || !data.nCards) return;
    gameState.myCards = [];
    gameState.opponentCounts = {};
    var bottom = [];
    var maxCount = 0;
    var landlordCandidate = 0;
    for (var i = 0; i < data.nCards.length; i++) {
        var nc = data.nCards[i];
        if (!nc.cards) continue;
        if (nc.roleId === 0) {
            for (var b = 0; b < nc.cards.length; b++) {
                if (nc.cards[b].value) bottom.push(nc.cards[b].value);
            }
            continue;
        }
        if (nc.roleId === userId) {
            for (var j = 0; j < nc.cards.length; j++) {
                gameState.myCards.push(nc.cards[j].value);
            }
        } else {
            gameState.opponentCounts[nc.roleId] = nc.cards.length;
        }
        if (nc.cards.length > maxCount) {
            maxCount = nc.cards.length;
            landlordCandidate = nc.roleId;
        }
    }
    if (bottom.length) {
        gameState.bottomCards = bottom;
        renderDizhuCards(bottom);
    }
    // 20 张视为地主（17+3 底牌）
    if (maxCount >= 20 && landlordCandidate) {
        gameState.landlordId = landlordCandidate;
    }
    sortHandCards(gameState.myCards);
    gameState.selectedCards.clear();
    renderMyCards();
    renderOpponentHands();
    showCenterMsg(bottom.length ? '地主亮牌' : '发牌完成');
}

/** 出牌确认：只保留最后一手；过牌显示「不要」；同步手牌张数 */
function handleAckOp(data) {
    var cards = data.cards || [];
    var choice = data.choice;
    if (choice === 0 || (!cards.length && choice !== 6)) {
        showPassHint(data.opFrom);
        return;
    }
    if (!cards.length) return;
    clearAllPlayedAreas();
    clearPassHints();
    renderPlayedCards(data.opFrom, cards);
    gameState.lastPlayedCards = cards.slice();
    if (data.opFrom === userId) {
        for (var i = 0; i < cards.length; i++) {
            var idx = gameState.myCards.indexOf(cards[i]);
            if (idx >= 0) gameState.myCards.splice(idx, 1);
        }
        gameState.selectedCards.clear();
        renderMyCards();
    } else {
        var prev = gameState.opponentCounts[data.opFrom] || 0;
        gameState.opponentCounts[data.opFrom] = Math.max(0, prev - cards.length);
        renderOpponentHands();
    }
}

function handleNotOp(data) {
    if (!data) return;
    var opSeat = data.opSeat;
    highlightActivePlayer(opSeat);
    var choices = data.choice || [];
    // 仅有出牌、无可过：新一轮首出，清掉桌面上一手牌
    var hasPlay = false, hasPass = false, hasPrepare = false;
    for (var i = 0; i < choices.length; i++) {
        if (choices[i].choice === 6) hasPlay = true;
        if (choices[i].choice === 0) hasPass = true;
        if (choices[i].choice === 7) hasPrepare = true;
    }
    if (hasPlay && !hasPass) {
        clearAllPlayedAreas();
        clearPassHints();
    }
    if (hasPrepare) {
        showActionButtons('prepare');
        return;
    }
    if (gameState.myPosition >= 0 && opSeat === gameState.myPosition) {
        gameState.lastChoices = choices;
        showOperationChoices(choices);
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
    } else if (data.state === 10) {
        document.getElementById('tableState').textContent = '小结算';
    } else {
        document.getElementById('tableState').textContent = '等待中';
        clearAllPlayedAreas();
        clearPassHints();
        gameState.bottomCards = [];
        document.getElementById('dizhuCards').innerHTML = '';
        gameState.landlordId = 0;
    }
}

function handleNotResult(data) {
    if (!data) return;
    if (data.landlord_id) gameState.landlordId = data.landlord_id;
    var title = data.win_team === 0 ? '地主获胜' : '农民获胜';
    if (data.spring) title += ' · 春天';
    if (data.anti_spring) title += ' · 反春';
    var meta = '底分 ' + (data.base_score || 0)
        + ' · 抢地主×' + (data.rob_multiplier || 1)
        + ' · 结算系数 ' + (data.settle_factor || 0);
    var rows = '';
    var players = data.rPlayers || [];
    for (var i = 0; i < players.length; i++) {
        var p = players[i];
        var tag = (p.roleId === data.landlord_id) ? '地主' : '农民';
        var name = findPlayerName(p.roleId);
        rows += '<div class="row"><span>' + name + '（' + tag + '）</span>'
            + '<span>余牌 ' + ((p.cards && p.cards.length) || 0) + '</span></div>';
    }
    showSettle(title, meta, rows, players, data.landlord_id);
    showCenterMsg(title, 2500);
    if (!gameState.autoNextRound) {
        showActionButtons('prepare');
    } else {
        hideActions();
    }
}

function findPlayerName(roleId) {
    if (roleId === userId) return nickname || '我';
    for (var i = 0; i < gameState.players.length; i++) {
        if (gameState.players[i].roleId === roleId) {
            return gameState.players[i].nickName || ('玩家' + roleId);
        }
    }
    return '玩家' + roleId;
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

function buildRemainHandsHtml(remainPlayers, landlordId) {
    if (!remainPlayers || !remainPlayers.length) return '';
    var html = '';
    for (var i = 0; i < remainPlayers.length; i++) {
        var p = remainPlayers[i];
        var cards = p.cards || [];
        if (!cards.length) continue;
        var tag = (p.roleId === landlordId) ? '地主' : '农民';
        html += '<div class="settle-hand-row"><div class="label">'
            + findPlayerName(p.roleId) + '（' + tag + '）剩余手牌</div>'
            + '<div class="settle-hand-cards"></div></div>';
    }
    return html;
}

function fillRemainHandFaces(remainPlayers) {
    var wrap = document.getElementById('settleHands');
    if (!wrap || !remainPlayers) return;
    var rows = wrap.querySelectorAll('.settle-hand-cards');
    var ri = 0;
    for (var i = 0; i < remainPlayers.length; i++) {
        var cards = remainPlayers[i].cards || [];
        if (!cards.length) continue;
        var box = rows[ri++];
        if (!box) continue;
        for (var c = 0; c < cards.length; c++) {
            var face = createCardFace(cards[c]);
            face.classList.add('played-face');
            box.appendChild(face);
        }
    }
}

function showSettle(title, meta, rowsHtml, remainPlayers, landlordId) {
    GameTable.showSettle({
        title: title,
        meta: meta,
        rowsHtml: rowsHtml,
        handsHtml: buildRemainHandsHtml(remainPlayers, landlordId),
        autoNext: gameState.autoNextRound
    });
    fillRemainHandFaces(remainPlayers);
}

function closeSettle() { GameTable.closeSettle(); }

function doOp(choice) {
    var selected = [];
    gameState.selectedCards.forEach(function(cardValue) {
        selected.push({ value: cardValue });
    });
    if (choice === 6 && selected.length === 0) {
        showCenterMsg('请先选择要出的牌');
        return;
    }

    sendWsMessage('op', {
        choice: choice,
        cards: selected.length > 0 ? selected : undefined
    }, function(resp) {
        if (resp.code !== 0) {
            showCenterMsg(resp.msg || '操作失败');
            // 失败时保留选牌，并恢复刚才的操作按钮
            if (gameState.lastChoices && gameState.lastChoices.length) {
                showOperationChoices(gameState.lastChoices);
            }
            return;
        }
        gameState.selectedCards.clear();
        renderMyCards();
    });
    hideActions();
}

function doPrepare() { GameTable.doPrepare(sendWsMessage); }
function backToLobby() { GameTable.backToLobby(); }
function exitRoom() { GameTable.exitRoom(sendWsMessage); }

// ==================== 渲染 ====================
// 花色编码与后端 CardSuit 一致：1方块 2梅花 3红桃 4黑桃
function cardMeta(cardId) {
    if (cardId === 516) return { rank: '小王', suit: '☆', red: false, joker: true };
    if (cardId === 517) return { rank: '大王', suit: '★', red: true, joker: true };
    var suitId = Math.floor(cardId / 100), value = cardId % 100;
    var suits = {1: '♦', 2: '♣', 3: '♥', 4: '♠'};
    var ranks = {11: 'J', 12: 'Q', 13: 'K', 14: 'A', 15: '2'};
    return { rank: ranks[value] || String(value), suit: suits[suitId] || '♦',
        red: suitId === 1 || suitId === 3, joker: false, pipCount: Math.min(value, 10) };
}

function createCardFace(cardId) {
    var meta = cardMeta(cardId), face = document.createElement('div');
    face.className = 'card-face' + (meta.red ? ' red' : '');
    var corner = document.createElement('div'); corner.className = 'card-corner';
    corner.innerHTML = '<span>' + meta.rank + '</span><span class="suit">' + meta.suit + '</span>';
    face.appendChild(corner);
    var art = document.createElement('div'); art.className = 'card-art' + (meta.joker ? ' joker' : '');
    if (meta.joker) art.textContent = meta.rank === '大王' ? '★\nJOKER' : '☆\nJOKER';
    else if (meta.rank === 'J' || meta.rank === 'Q' || meta.rank === 'K' || meta.rank === 'A' || meta.rank === '2') art.textContent = meta.rank + meta.suit;
    else {
        art.className += ' pips';
        for (var i = 0; i < meta.pipCount; i++) { var pip = document.createElement('span'); pip.textContent = meta.suit; art.appendChild(pip); }
    }
    face.appendChild(art);
    return face;
}

function renderMyCards() {
    var container = document.getElementById('myCards');
    container.innerHTML = '';
    for (var i = 0; i < gameState.myCards.length; i++) {
        var cardId = gameState.myCards[i];
        var card = document.createElement('div');
        card.className = 'card' + (gameState.selectedCards.has(cardId) ? ' selected' : '');
        card.dataset.index = i;
        card.style.zIndex = String(i + 1);
        card.appendChild(createCardFace(cardId));
        card.onclick = (function(value) {
            return function(ev) {
                ev.stopPropagation();
                toggleCard(value);
            };
        })(cardId);
        container.appendChild(card);
    }
    renderPlayerLabels();
}

function renderDizhuCards(cardValues) {
    var box = document.getElementById('dizhuCards');
    box.innerHTML = '';
    if (!cardValues || !cardValues.length) return;
    var label = document.createElement('span');
    label.className = 'dizhu-label';
    label.textContent = '底牌';
    box.appendChild(label);
    for (var i = 0; i < cardValues.length; i++) {
        var card = document.createElement('div');
        card.className = 'card';
        card.appendChild(createCardFace(cardValues[i]));
        box.appendChild(card);
    }
}

function clearAllPlayedAreas() {
    ['playedLeft', 'playedRight', 'playedBottom'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.innerHTML = '';
    });
}

function playedTargetForRole(roleId) {
    if (roleId === userId) return document.getElementById('playedBottom');
    for (var i = 0; i < gameState.players.length; i++) {
        if (gameState.players[i].roleId === roleId) {
            var rel = (gameState.players[i].position - gameState.myPosition + 3) % 3;
            return document.getElementById(rel === 1 ? 'playedLeft' : 'playedRight');
        }
    }
    return null;
}

/** 将最后一手牌放到对应方位（朝向出牌者）。 */
function renderPlayedCards(roleId, cardValues) {
    var target = playedTargetForRole(roleId);
    if (!target) return;
    target.innerHTML = '';
    (cardValues || []).forEach(function (value) {
        var face = createCardFace(value);
        face.classList.add('played-face');
        target.appendChild(face);
    });
}

function showPassHint(roleId) {
    var target = playedTargetForRole(roleId);
    if (!target) return;
    target.innerHTML = '<div class="pass-hint">不要</div>';
}

function clearPassHints() {
    ['playedLeft', 'playedRight', 'playedBottom'].forEach(function(id) {
        var el = document.getElementById(id);
        if (!el) return;
        if (el.querySelector('.pass-hint')) el.innerHTML = '';
    });
}

function toggleCard(cardValue) {
    if (gameState.selectedCards.has(cardValue)) {
        gameState.selectedCards.delete(cardValue);
    } else {
        gameState.selectedCards.add(cardValue);
    }
    renderMyCards();
}

function roleBadgeHtml(roleId) {
    if (!gameState.landlordId) return '';
    if (roleId === gameState.landlordId) {
        return '<span class="avatar-mark landlord">地</span><span class="role-badge landlord">地主</span>';
    }
    return '<span class="avatar-mark farmer">农</span><span class="role-badge farmer">农民</span>';
}

function renderPlayerLabels() {
    var bottom = document.getElementById('playerBottom');
    bottom.innerHTML = roleBadgeHtml(userId)
        + '<span class="name" id="myName">' + (nickname || '我') + '</span>';
}

function renderCardBacks(containerId, count) {
    var container = document.getElementById(containerId);
    container.innerHTML = '';
    for (var i = 0; i < count; i++) {
        var back = document.createElement('div');
        back.className = 'card-back';
        container.appendChild(back);
    }
}

/** 根据相对座位刷新左右对手牌背、张数与地主/农民标识 */
function renderOpponentHands() {
    var leftEl = document.getElementById('playerLeft');
    var rightEl = document.getElementById('playerRight');
    var players = gameState.players || [];
    renderPlayerLabels();
    for (var i = 0; i < players.length; i++) {
        var p = players[i];
        if (p.roleId === userId) continue;
        var cardCount = gameState.opponentCounts[p.roleId];
        if (cardCount == null) cardCount = p.cardCount || 0;
        var displayName = p.nickName || (p.robot || p.roleId < 0 ? '机器人' : '玩家');
        var nameHtml = roleBadgeHtml(p.roleId)
            + '<span class="name">' + displayName + '</span>'
            + '<span class="card-count">' + cardCount + '张</span>';
        var relPos = (p.position - gameState.myPosition + 3) % 3;
        if (relPos === 1) {
            leftEl.innerHTML = nameHtml;
            renderCardBacks('cardsLeft', cardCount);
        } else {
            rightEl.innerHTML = nameHtml;
            renderCardBacks('cardsRight', cardCount);
        }
    }
}

function updatePlayers(players) {
    gameState.players = players;
    var leftEl = document.getElementById('playerLeft');
    var rightEl = document.getElementById('playerRight');
    var leftCards = document.getElementById('cardsLeft');
    var rightCards = document.getElementById('cardsRight');

    leftEl.innerHTML = '<span class="name">等待加入</span>';
    rightEl.innerHTML = '<span class="name">等待加入</span>';
    leftCards.innerHTML = '';
    rightCards.innerHTML = '';

    // 找到自己的位置
    for (var i = 0; i < players.length; i++) {
        if (players[i].roleId === userId) {
            gameState.myPosition = players[i].position;
            break;
        }
    }

    renderOpponentHands();
}

function highlightActivePlayer(position) {
    document.getElementById('playerLeft').className = 'player-info';
    document.getElementById('playerRight').className = 'player-info';
    document.getElementById('playerBottom').className = 'player-info';

    if (position === gameState.myPosition) {
        document.getElementById('playerBottom').className = 'player-info active';
    } else {
        var relPos = (position - gameState.myPosition + 3) % 3;
        if (relPos === 1) {
            document.getElementById('playerLeft').className = 'player-info active';
        } else {
            document.getElementById('playerRight').className = 'player-info active';
        }
    }
}

function showOperationChoices(choices) {
    var bar = document.getElementById('actionBar');
    bar.innerHTML = '';
    bar.style.display = 'flex';

    for (var i = 0; i < choices.length; i++) {
        var choice = choices[i];
        var btn = document.createElement('button');
        btn.className = 'action-btn';

        switch (choice.choice) {
            case 6: // PLAY
                btn.className += ' btn-play';
                btn.textContent = '出牌';
                btn.onclick = function() { doOp(6); };
                break;
            case 0: // PASS
                btn.className += ' btn-pass';
                btn.textContent = '不出';
                btn.onclick = function() { doOp(0); };
                break;
            case 1: // CALL
                btn.className += ' btn-call';
                btn.textContent = '叫地主';
                btn.onclick = function() { doOp(1); };
                break;
            case 2: // ROB
                btn.className += ' btn-rob';
                btn.textContent = '抢地主';
                btn.onclick = function() { doOp(2); };
                break;
            case 3: // NOT_CALL
                btn.className += ' btn-pass';
                btn.textContent = '不叫';
                btn.onclick = function() { doOp(3); };
                break;
            case 4: // NOT_ROB
                btn.className += ' btn-pass';
                btn.textContent = '不抢';
                btn.onclick = function() { doOp(4); };
                break;
            case 9: // CALL_SCORE_1
                btn.className += ' btn-call';
                btn.textContent = '1分';
                btn.onclick = function() { doOp(9); };
                break;
            case 10: // CALL_SCORE_2
                btn.className += ' btn-call';
                btn.textContent = '2分';
                btn.onclick = function() { doOp(10); };
                break;
            case 11: // CALL_SCORE_3
                btn.className += ' btn-call';
                btn.textContent = '3分';
                btn.onclick = function() { doOp(11); };
                break;
            default:
                continue;
        }
        bar.appendChild(btn);
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
