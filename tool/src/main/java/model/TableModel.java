package model; 
 
 
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

 
    @Override
    public String toString() {
        return "TableModel{"+
                "     id="+id+ 
                "     type="+type+ 
                "     seatNum="+seatNum+ 
                "     cardNum="+cardNum+ 
                "     exCardNum="+exCardNum+ 
                '}';
    }

 }
