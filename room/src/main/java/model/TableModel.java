package model;


public class TableModel {

	//主键
	private int id;

	//类型(1麻将2斗地主)
	private int type;

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


	@Override
	public String toString() {
		return "TableModel{" +
				"     id=" + id +
				"     type=" + type +
				'}';
	}

}
