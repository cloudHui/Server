package model.tablemodel; 
 
 
public class TableModel implements java.io.Serializable {

    //主键
    private int id;

    //类型(1麻将2斗地主)
    private int type;

    //座位数量
    private int seatNum;

    //牌数量
    private int cardNum;

    //额外牌数量
    private int exCardNum;

    //底分
    private int baseScore;

    //番数上限(麻将用)
    private int maxFan;

    //允许吃(麻将用 0关1开)
    private int allowChi;

    //允许点炮(麻将用 0关1开)
    private int allowDianPao;

    //0.0
    private int allowPeng;

    //0.0
    private int allowGang;

    //0.0
    private int allowHu;

    //0.0
    private int allowSevenPairs;

    //0.0
    private int gameSubType;

    //0.0
    private int gangScore;

    //allowGangMing
    private int allowGangMing;

    //allowGangAn
    private int allowGangAn;

    //allowGangBu
    private int allowGangBu;

    //totalRounds
    private int totalRounds;

    //autoNextRound
    private int autoNextRound;

    //超时自动(0关1开)
    private int autoPlay;

    /** 等人超时秒数；0=不启用 */
    private int waitTimeoutSec = 120;

    /** 等人超时动作：0=解散 dissolve，1=补机器人 fillRobot */
    private int waitTimeoutAction = 0;

    /** 全机器人则删桌不开局：0关1开 */
    private int disbandIfAllRobot = 1;
 
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

    public int getDisbandIfAllRobot() {
        return disbandIfAllRobot;
    }

    public void setDisbandIfAllRobot(int disbandIfAllRobot) {
        this.disbandIfAllRobot = disbandIfAllRobot;
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
                "     waitTimeoutSec="+waitTimeoutSec+
                "     waitTimeoutAction="+waitTimeoutAction+
                "     disbandIfAllRobot="+disbandIfAllRobot+
                '}';
    }

 }
