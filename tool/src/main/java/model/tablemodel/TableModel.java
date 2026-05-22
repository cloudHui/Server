package model.tablemodel;


public class TableModel {

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
                '}';
    }

}
