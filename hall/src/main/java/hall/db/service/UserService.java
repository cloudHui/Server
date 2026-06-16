package hall.db.service;

import db.mysql.DBService;
import db.mysql.DBSourceFactory;
import hall.db.dao.UserDao;
import hall.db.entity.UserInfos;

public class UserService extends DBService<UserDao> {

	public UserService() {
		super(DBSourceFactory.INSTANCE.getSqlSessionFactory(), UserDao.class);
	}

	public UserInfos queryUserInfo(long userId) {
		return execute(o -> o.queryUserInfo(userId));
	}

	public UserInfos queryByDeviceId(String deviceId) {
		return execute(o -> o.queryByDeviceId(deviceId));
	}

	public UserInfos queryByLoginKey(String loginKey) {
		return execute(o -> o.queryByLoginKey(loginKey));
	}

	public long insertUser(UserInfos user) {
		execute(o -> o.insertUser(user));
		return user.getUserId();
	}

	public int updateLoginInfo(UserInfos user) {
		return execute(o -> o.updateLoginInfo(user));
	}
}
