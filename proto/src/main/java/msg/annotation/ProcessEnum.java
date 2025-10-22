package msg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import msg.registor.enums.TableState;


/**
 * 多个(单个)处理类型 对应一个处理 注解 兼容单个
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProcessEnum {

	/**
	 * 处理类型
	 * 使用value做属性可以 直接在加注解的时候存值不需要使用  属性名=
	 *
	 * @return 类型值
	 */
	TableState[] value();
}
