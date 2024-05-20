package model; 
 
public class TableModel {

    //主键
    private int id;

    //类型(1麻将2斗地主)
    private int type;

    //可观战人数
    private int watch;

    //规则
    private int[] rule;

    //规则值
    private int[] ruleValue;
 
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

    public int getWatch() {
        return watch;
    }

    public void setWatch(int watch) {
        this.watch = watch;
    }

    public int[] getRule() {
        return rule;
    }

    public void setRule(int[] rule) {
        this.rule = rule;
    }

    public int[] getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(int[] ruleValue) {
        this.ruleValue = ruleValue;
    }

 }
