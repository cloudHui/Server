package hall.db.service;

import db.mysql.DBService;
import db.mysql.DBSourceFactory;
import hall.db.dao.UserDao;
import hall.db.entity.UserInfos;
import org.apache.ibatis.session.SqlSessionFactory;

public class UserService extends DBService<UserDao> {

	public UserService() {
		super(DBSourceFactory.INSTANCE.getSqlSessionFactory(), UserDao.class);
	}

	private UserService(SqlSessionFactory sqlSessionFactory, Class<UserDao> clazz) {
		super(sqlSessionFactory, clazz);
	}

	public UserInfos queryUserInfo(int userId) {
		return execute(o -> o.queryUserInfo(userId));
	}

	public UserInfos insertUserInfo(String plant) {
		return execute(o -> o.insertUserInfo(plant));
	}
}
