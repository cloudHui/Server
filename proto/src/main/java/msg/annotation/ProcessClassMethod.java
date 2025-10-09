package msg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 处理类型 对应一个处理 注解 兼容单个
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProcessClassMethod {

	/**
	 * 处理类型
	 */
	Class<?> value();
}
