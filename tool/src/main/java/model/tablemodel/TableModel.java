package model.tablemodel; 
 
 
public class TableModel implements java.io.Serializable {

    /** Excel列: id; 主键 */
    private int id;

    /** Excel列: type; 类型(1麻将2斗地主) */
    private int type;

    /** Excel列: seatNum; 座位数量 */
    private int seatNum;

    /** Excel列: cardNum; 牌数量 */
    private int cardNum;

    /** Excel列: exCardNum; 额外牌数量 */
    private int exCardNum;

    /** Excel列: baseScore; 底分 */
    private int baseScore;

    /** Excel列: maxFan; 番数上限(麻将用) */
    private int maxFan;

    /** Excel列: allowChi; 允许吃(麻将用 0关1开) */
    private int allowChi;

    /** Excel列: allowDianPao; 允许点炮(麻将用 0关1开) */
    private int allowDianPao;

    /** Excel列: allowPeng; 0.0 */
    private int allowPeng;

    /** Excel列: allowGang; 0.0 */
    private int allowGang;

    /** Excel列: allowHu; 0.0 */
    private int allowHu;

    /** Excel列: allowSevenPairs; 0.0 */
    private int allowSevenPairs;

    /** Excel列: gameSubType; 0.0 */
    private int gameSubType;

    /** Excel列: gangScore; 0.0 */
    private int gangScore;

    /** Excel列: allowGangMing; allowGangMing */
    private int allowGangMing;

    /** Excel列: allowGangAn; allowGangAn */
    private int allowGangAn;

    /** Excel列: allowGangBu; allowGangBu */
    private int allowGangBu;

    /** Excel列: totalRounds; totalRounds */
    private int totalRounds;

    /** Excel列: autoNextRound; autoNextRound */
    private int autoNextRound;

    /** Excel列: autoPlay; 超时自动(0关1开) */
    private int autoPlay;

    /** 等待超时配置：秒数及超时动作（0=解散，1=填充机器人）。 */
    private int waitTimeoutSec;
    private int waitTimeoutAction;
 
    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSeatNum() {
        return seatNum;
    }

    public void setSeatNum(int seatNum) {
        this.seatNum = seatNum;
    }

    public int getCardNum() {
        return cardNum;
    }

    public void setCardNum(int cardNum) {
        this.cardNum = cardNum;
    }

    public int getExCardNum() {
        return exCardNum;
    }

    public void setExCardNum(int exCardNum) {
        this.exCardNum = exCardNum;
    }

    public int getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(int baseScore) {
        this.baseScore = baseScore;
    }

    public int getMaxFan() {
        return maxFan;
    }

    public void setMaxFan(int maxFan) {
        this.maxFan = maxFan;
    }

    public int getAllowChi() {
        return allowChi;
    }

    public void setAllowChi(int allowChi) {
        this.allowChi = allowChi;
    }

    public int getAllowDianPao() {
        return allowDianPao;
    }

    public void setAllowDianPao(int allowDianPao) {
        this.allowDianPao = allowDianPao;
    }

    public int getAllowPeng() {
        return allowPeng;
    }

    public void setAllowPeng(int allowPeng) {
        this.allowPeng = allowPeng;
    }

    public int getAllowGang() {
        return allowGang;
    }

    public void setAllowGang(int allowGang) {
        this.allowGang = allowGang;
    }

    public int getAllowHu() {
        return allowHu;
    }

    public void setAllowHu(int allowHu) {
        this.allowHu = allowHu;
    }

    public int getAllowSevenPairs() {
        return allowSevenPairs;
    }

    public void setAllowSevenPairs(int allowSevenPairs) {
        this.allowSevenPairs = allowSevenPairs;
    }

    public int getGameSubType() {
        return gameSubType;
    }

    public void setGameSubType(int gameSubType) {
        this.gameSubType = gameSubType;
    }

    public int getGangScore() {
        return gangScore;
    }

    public void setGangScore(int gangScore) {
        this.gangScore = gangScore;
    }

    public int getAllowGangMing() {
        return allowGangMing;
    }

    public void setAllowGangMing(int allowGangMing) {
        this.allowGangMing = allowGangMing;
    }

    public int getAllowGangAn() {
        return allowGangAn;
    }

    public void setAllowGangAn(int allowGangAn) {
        this.allowGangAn = allowGangAn;
    }

    public int getAllowGangBu() {
        return allowGangBu;
    }

    public void setAllowGangBu(int allowGangBu) {
        this.allowGangBu = allowGangBu;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    public int getAutoNextRound() {
        return autoNextRound;
    }

    public void setAutoNextRound(int autoNextRound) {
        this.autoNextRound = autoNextRound;
    }

    public int getAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(int autoPlay) {
        this.autoPlay = autoPlay;
    }

    public int getWaitTimeoutSec() {
        return waitTimeoutSec;
    }

    public void setWaitTimeoutSec(int waitTimeoutSec) {
        this.waitTimeoutSec = waitTimeoutSec;
    }

    public int getWaitTimeoutAction() {
        return waitTimeoutAction;
    }

    public void setWaitTimeoutAction(int waitTimeoutAction) {
        this.waitTimeoutAction = waitTimeoutAction;
    }

 
    @Override
    public String toString() {
        return "TableModel{"+
                "     id="+id+ 
                "     type="+type+ 
                "     seatNum="+seatNum+ 
                "     cardNum="+cardNum+ 
                "     exCardNum="+exCardNum+ 
                "     baseScore="+baseScore+ 
                "     maxFan="+maxFan+ 
                "     allowChi="+allowChi+ 
                "     allowDianPao="+allowDianPao+ 
                "     allowPeng="+allowPeng+ 
                "     allowGang="+allowGang+ 
                "     allowHu="+allowHu+ 
                "     allowSevenPairs="+allowSevenPairs+ 
                "     gameSubType="+gameSubType+ 
                "     gangScore="+gangScore+ 
                "     allowGangMing="+allowGangMing+ 
                "     allowGangAn="+allowGangAn+ 
                "     allowGangBu="+allowGangBu+ 
                "     totalRounds="+totalRounds+ 
                "     autoNextRound="+autoNextRound+ 
                "     autoPlay="+autoPlay+ 
                '}';
    }

 }
