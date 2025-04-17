package msg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import msg.ServerType;


/**
 * 消息类消息id绑定
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ClassType {

	/**
	 * 绑定的消息类信息
	 */
	Class<?> value();

	/**
	 * 是否是通用 通用需要下面的服务类型判断
	 *
	 * @return 是否通用
	 */
	boolean common() default false;

	/**
	 * 要注册的服务类型
	 */
	ServerType[] serverTypes() default {};
}
