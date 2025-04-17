package msg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import msg.MessageTrans;
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
	 * 要注册的服务类型
	 */
	MessageTrans[] messageTrans() default {};

	/**
	 * 描述
	 */
	String des() default "";
}
