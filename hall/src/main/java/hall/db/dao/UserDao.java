package hall.db.dao;

import hall.db.entity.UserInfos;
import org.apache.ibatis.annotations.Param;

public interface UserDao {

	UserInfos queryUserInfo(@Param("userId") long userId);

	UserInfos queryByDeviceId(@Param("deviceId") String deviceId);

	UserInfos queryByLoginKey(@Param("loginKey") String loginKey);

	int insertUser(UserInfos user);

	int updateLoginInfo(UserInfos user);
}
