package hall.manager;

import hall.manager.model.UserData;
import hall.manager.model.UserInfo;

public class User {

	private UserInfo userInfo;

	private UserData userData;

	public User() {
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
}
