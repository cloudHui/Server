package game.manager.table.banner;

/**
 * @author admin
 * @className Banner
 * @description
 * @createDate 2025/10/21 14:28
 */
public class Banner {

	/**
	 * 第一个随机抢地主位置
	 */
	private int firstRandomRobSeat;
	/**
	 * 第一个抢地主位置
	 */
	private int firstRobSeat;

	public Banner() {
	}

	public int getFirstRandomRobSeat() {
		return firstRandomRobSeat;
	}

	public void setFirstRandomRobSeat(int firstRandomRobSeat) {
		this.firstRandomRobSeat = firstRandomRobSeat;
	}

	public int getFirstRobSeat() {
		return firstRobSeat;
	}

	public void setFirstRobSeat(int firstRobSeat) {
		this.firstRobSeat = firstRobSeat;
	}

	/**
	 * 重置
	 */
	public void reset() {
		firstRandomRobSeat = -1;
		firstRobSeat = -1;
	}
}
