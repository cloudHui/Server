package hall.manager;

public class HallUser {

	private UserInfo userInfo;

	private UserData userData;

	public HallUser() {
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}

	public UserData getUserData() {
		return userData;
	}

	public void setUserData(UserData userData) {
		this.userData = userData;
	}

	@Override
	public String toString() {
		return super.toString() + "  " + userInfo.toString() + "  " + userData.toString();
	}
}
