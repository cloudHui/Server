package msg.registor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import msg.annotation.ClassField;
import msg.annotation.ClassType;
import msg.annotation.ProcessClass;
import msg.annotation.ProcessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.ClazzUtil;

/**
 * @className HandleTypeRegister
 * @description 统一注册中心 - 负责消息处理器、类型转换和工厂方法的动态绑定与注册管理
 * @createDate 2025/04/10 03:27
 */
public class HandleTypeRegister {

	private static final Logger logger = LoggerFactory.getLogger(HandleTypeRegister.class);

	// 配置常量
	private static final String DEFAULT_HANDLE_PACKAGE = "utils.handle";
	private static final String BIND_SUCCESS_TEMPLATE = "{} bind success, size: {}";

	private static final Map<Integer, Class<?>> TRANS_MAP = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Integer> MSG_TRANS_MAP = new ConcurrentHashMap<>();
	// ==================== 消息类型转换相关方法 ====================

	static {
		try {
			long start = System.currentTimeMillis();
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(HandleTypeRegister.class, "");
			for (Class<?> clazz : classes) {
				ClassType processType = clazz.getAnnotation(ClassType.class);
				if (processType != null) {
					bindTransMap(clazz);
				}
			}
			logger.info("init message id bind total size:{} cost:{}ms", TRANS_MAP.size(), System.currentTimeMillis() - start);
		} catch (Exception e) {
			logger.error("init message id bind class", e);
		}
	}


	/**
	 * 绑定消息类型转换映射
	 *
	 * @param constantClass 消息常量类
	 */
	private static void bindTransMap(Class<?> constantClass) {
		for (Field field : constantClass.getFields()) {
			ClassField annotation = field.getAnnotation(ClassField.class);
			if (annotation == null) {
				continue;
			}
			try {
				Object fieldValue = field.get(null);
				if (fieldValue instanceof Integer) {
					TRANS_MAP.put((Integer) fieldValue, annotation.value());
					MSG_TRANS_MAP.put(annotation.value(), (Integer) fieldValue);
				}
			} catch (Exception e) {
				logger.error("Bind field failed: {}.{}", constantClass.getSimpleName(), field.getName(), e);
			}
		}
	}

	// ==================== 处理器绑定相关方法 ====================


	// ==================== 工厂方法相关方法 ====================

	/**
	 * 初始化处理工厂
	 */
	public static <T> void initFactory(Map<Integer, T> handles) {
		initFactory(DEFAULT_HANDLE_PACKAGE, handles);
	}

	/**
	 * 初始化处理工厂
	 */
	public static <T> void initFactory(Class<?> packageClass, Map<Integer, T> handles) {
		String packageName = packageClass.getPackage().getName();
		initFactory(packageName, handles);
	}

	/**
	 * 通用的初始化处理工厂方法
	 */
	private static <T> void initFactory(String packageName, Map<Integer, T> handles) {
		try {
			long start = System.currentTimeMillis();
			List<Class<?>> classes = ClazzUtil.getClasses(packageName);
			Map<Class<?>, T> classProcessMap = new HashMap<>();

			for (Class<?> aclass : classes) {
				ProcessType processesType = aclass.getAnnotation(ProcessType.class);
				if (processesType == null) {
					continue;
				}
				putHandle(processesType.value(), aclass, handles, classProcessMap);
			}

			logger.info("{} bind success, size:{} cost:{}ms", packageName, handles.size(),
					System.currentTimeMillis() - start);
		} catch (Exception e) {
			logger.error("{} bind processors error", packageName, e);
		}
	}

	/**
	 * 初始化类处理工厂
	 *
	 * @param factoryClass 要扫描的目录中的类
	 * @param handles      处理器存储集合
	 * @param <T>          动态处理器类
	 */
	public static <T> void initClassFactory(Class<?> factoryClass, Map<Class<?>, T> handles) {
		try {
			long start = System.currentTimeMillis();
			List<Class<?>> classes = ClazzUtil.getClasses(factoryClass, "");
			Map<Class<?>, T> classProcessMap = new HashMap<>();

			for (Class<?> aclass : classes) {
				ProcessClass processesType = aclass.getAnnotation(ProcessClass.class);
				if (processesType == null) {
					continue;
				}

				Class<?> value = processesType.value();
				putHandle(value, aclass, handles, classProcessMap);
			}

			logger.info("{} bind success, size:{} cost:{}ms", factoryClass.getPackage().getName(), handles.size(),
					System.currentTimeMillis() - start);
		} catch (Exception e) {
			logger.error("{} bind processors error", factoryClass.getPackage().getName(), e);
		}
	}

	///**
	// * 初始化类处理工厂
	// *
	// * @param factoryClass   要扫描的目录中的类
	// * @param handles        处理器存储集合
	// * @param classMethodMap 类方法绑定集合(有 @ProcessClassMethod注解的方法)
	// * @param managerClass   @ProcessClassMethod注解上的父类
	// * @param <T>            动态处理器类
	// */
	//public static <T> void initClassFactory(Class<?> factoryClass, Map<Class<?>, T> handles, Map<Class<?>, Map<Class<?>, Method>> classMethodMap, Class<?> managerClass) {
	//	try {
	//		List<Class<?>> classes = ClazzUtil.getClasses(factoryClass, "");
	//		Map<Class<?>, T> classProcessMap = new HashMap<>();
	//
	//		for (Class<?> aclass : classes) {
	//			ProcessClass processesType = aclass.getAnnotation(ProcessClass.class);
	//			if (processesType == null) {
	//				continue;
	//			}
	//
	//			Class<?> value = processesType.value();
	//			putHandle(value, aclass, handles, classProcessMap);
	//			managerFunctionMap(aclass, managerClass, classMethodMap);
	//		}
	//
	//		logger.info("{} bind success, size:{}", factoryClass.getPackage().getName(), handles.size());
	//	} catch (Exception e) {
	//		logger.error("{} bind processors error", factoryClass.getPackage().getName(), e);
	//	}
	//}

	/**
	 * 处理器绑定核心方法
	 */
	private static <K, T> void putHandle(K key, Class<?> aclass, Map<K, T> handles, Map<Class<?>, T> classProcessMap) {
		T handler = handles.get(key);
		if (handler != null) {
			logger.error("putHandle same key:{} old:{} new:{} ", key, handler.getClass(), aclass);
		}

		handler = classProcessMap.computeIfAbsent(aclass, k -> newInstance(key, aclass));

		if (handler != null) {
			handles.put(key, handler);
		}
	}

	///**
	// * 管理函数方法存储
	// */
	//private static void managerFunctionMap(Class<?> aClass, Class<?> managerClass, Map<Class<?>, Map<Class<?>, Method>> classMethodMap) {
	//	Method[] declaredMethods = aClass.getDeclaredMethods();
	//	for (Method method : declaredMethods) {
	//		ProcessClassMethod annotation = method.getAnnotation(ProcessClassMethod.class);
	//		if (annotation != null) {
	//			if (managerClass.isAssignableFrom(annotation.value())) {
	//				classMethodMap.computeIfAbsent(aClass, k -> new HashMap<>()).put(annotation.value(), method);
	//			}
	//		}
	//	}
	//}

	// ==================== 实例创建方法 ====================

	/**
	 * 创建通用实例
	 */
	private static <T> T newInstance(Object key, Class<?> aclass) {
		try {
			return (T) aclass.getConstructor().newInstance();
		} catch (Exception e) {
			logger.error("init {} newInstance fail for key {} ", aclass, key, e);
		}
		return null;
	}

	// ==================== 消息解析方法 ====================

	/**
	 * 解析消息字节数据为Protocol Buffer消息对象
	 *
	 * @param messageId 消息ID
	 * @param bytes     消息字节数据
	 * @return 解析后的消息对象，解析失败返回null
	 */
	public static Message parseMessage(int messageId, byte[] bytes) {
		Class<?> messageClass = TRANS_MAP.get(messageId);
		if (messageClass == null) {
			logger.error("Unknown message ID: {}", messageId);
			return null;
		}

		try {
			return (Message) parseMessageData((Class<MessageLite>) messageClass, bytes);
		} catch (Exception e) {
			logger.error("Parse message failed, ID: {}, Class: {}",
					messageId, messageClass.getSimpleName(), e);
			return null;
		}
	}

	/**
	 * 通过对象获取消息id
	 */
	public static int parseMessageId(Class<?> aClass) {
		return MSG_TRANS_MAP.getOrDefault(aClass, 0);
	}

	/**
	 * 使用Protocol Buffer解析消息数据
	 */
	private static MessageLite parseMessageData(Class<MessageLite> messageClass, byte[] bytes) throws Exception {
		MessageLite defaultInstance = Internal.getDefaultInstance(messageClass);
		if (bytes == null || bytes.length == 0) {
			return defaultInstance.newBuilderForType().build();
		}
		return defaultInstance.getParserForType().parseFrom(bytes);
	}
}