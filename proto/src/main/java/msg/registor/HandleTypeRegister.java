package msg.registor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import msg.annotation.ProcessClassMethod;
import msg.annotation.ProcessType;
import net.handler.Handler;
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
	private static final String BIND_ERROR_TEMPLATE = "{} bind error";

	// 全局处理器实例缓存 (避免重复实例化)
	private static final Map<Class<?>, Object> GLOBAL_HANDLER_CACHE = new ConcurrentHashMap<>();

	private static final Map<Integer, Class<?>> TRANS_MAP = new ConcurrentHashMap<>();
	// ==================== 消息类型转换相关方法 ====================

	static {
		try {
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(HandleTypeRegister.class, "");
			for (Class<?> clazz : classes) {
				ClassType processType = clazz.getAnnotation(ClassType.class);
				if (processType != null) {
					bindTransMap(clazz);
				}
			}
			logger.info("init message id bind total size:{}", TRANS_MAP.size());
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
		int bindCount = 0;

		for (Field field : constantClass.getFields()) {
			ClassField annotation = field.getAnnotation(ClassField.class);
			if (annotation == null) {
				continue;
			}
			try {
				Object fieldValue = field.get(null);
				if (fieldValue instanceof Integer) {
					TRANS_MAP.put((Integer) fieldValue, annotation.value());
					bindCount++;
				}
			} catch (Exception e) {
				logger.error("Bind field failed: {}.{}", constantClass.getSimpleName(), field.getName(), e);
			}
		}

		logger.info(BIND_SUCCESS_TEMPLATE, constantClass.getSimpleName(), bindCount);
	}

	// ==================== 处理器绑定相关方法 ====================

	/**
	 * 绑定默认包下的处理器
	 */
	public static void bindDefaultPackageProcess(Map<Integer, Handler> processorMap) {
		bindPackageProcess(DEFAULT_HANDLE_PACKAGE, processorMap);
	}

	/**
	 * 绑定指定包下的处理器
	 *
	 * @param packageName  扫描的包路径
	 * @param processorMap 处理器映射表
	 */
	public static void bindPackageProcess(String packageName, Map<Integer, Handler> processorMap) {
		try {
			List<Class<?>> classes = ClazzUtil.getClasses(packageName);
			doBindProcessors(classes, processorMap);
			logger.info(BIND_SUCCESS_TEMPLATE, packageName, processorMap.size());
		} catch (Exception e) {
			logger.error(BIND_ERROR_TEMPLATE, packageName, e);
		}
	}

	/**
	 * 绑定指定类所在包下的处理器
	 */
	public static void bindClassPackageProcess(Class<?> packageClass, Map<Integer, Handler> processorMap) {
		String packageName = packageClass.getPackage().getName();
		try {
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(packageClass, "");
			doBindProcessors(classes, processorMap);
			logger.info("Package:{} bind success, size:{}", packageName, processorMap.size());
		} catch (Exception e) {
			logger.error(BIND_ERROR_TEMPLATE, packageName, e);
		}
	}

	/**
	 * 通用绑定方法 - 扫描包内所有处理器
	 */
	public static void bindAllProcessorInPackage(Class<?> packageClass, Map<Integer, Class<?>> processorMap) {
		bindProcessors(packageClass, processorMap, null);
	}

	/**
	 * 通用绑定方法 - 带包过滤条件
	 */
	public static void bindAllProcessorWithExceptPackage(Class<?> packageClass, Map<Integer, Class<?>> processorMap, String except) {
		bindProcessors(packageClass, processorMap, except);
	}

	// ==================== 工厂方法相关方法 ====================

	/**
	 * 初始化处理工厂
	 */
	public static <T> void initFactory(Class<?> factoryClass, Map<Integer, T> handles) {
		bindProcessors(factoryClass, handles, null);
	}

	/**
	 * 初始化类处理工厂
	 */
	public static <T> void initClassFactory(Class<?> factoryClass, Map<Class<?>, T> handles,
											Map<Class<?>, Map<Class<?>, Method>> classMethodMap,
											Class<?> managerClass) {
		bindClassProcessors(factoryClass, handles, classMethodMap, managerClass);
	}

	// ==================== 核心绑定逻辑 ====================

	/**
	 * 执行处理器绑定逻辑
	 */
	private static void doBindProcessors(List<Class<?>> classes, Map<Integer, Handler> processorMap) {
		for (Class<?> clazz : classes) {
			if (!Handler.class.isAssignableFrom(clazz)) {
				continue;
			}

			ProcessType processType = clazz.getAnnotation(ProcessType.class);
			if (processType != null) {
				registerProcessor(processType, clazz, processorMap);
			}
		}
	}

	/**
	 * 通用绑定方法 - 处理ProcessType注解
	 */
	private static <T> void bindProcessors(Class<?> packageClass, Map<Integer, T> processorMap, String filter) {
		try {
			List<Class<?>> classes = ClazzUtil.getClasses(packageClass, filter != null ? filter : "");
			Map<Class<?>, T> classProcessMap = new HashMap<>();

			for (Class<?> aclass : classes) {
				ProcessType processesType = aclass.getAnnotation(ProcessType.class);
				if (processesType == null) {
					continue;
				}
				putHandle(processesType.value(), aclass, processorMap, classProcessMap);
			}

			logger.info("{} bind success, size:{}", packageClass.getPackage().getName(), processorMap.size());
		} catch (Exception e) {
			logger.error("{} bind processors error", packageClass.getPackage().getName(), e);
		}
	}

	/**
	 * 通用绑定方法 - 处理ProcessClass注解
	 */
	private static <T> void bindClassProcessors(Class<?> packageClass, Map<Class<?>, T> processorMap,
												Map<Class<?>, Map<Class<?>, Method>> classMethodMap,
												Class<?> managerClass) {
		try {
			List<Class<?>> classes = ClazzUtil.getClasses(packageClass, "");
			Map<Class<?>, T> classProcessMap = new HashMap<>();

			for (Class<?> aclass : classes) {
				ProcessClass processesType = aclass.getAnnotation(ProcessClass.class);
				if (processesType == null) {
					continue;
				}

				Class<?> value = processesType.value();
				putHandle(value, aclass, processorMap, classProcessMap);
				managerFunctionMap(aclass, managerClass, classMethodMap);
			}

			logger.info("{} bind success, size:{}", packageClass.getPackage().getName(), processorMap.size());
		} catch (Exception e) {
			logger.error("{} bind processors error", packageClass.getPackage().getName(), e);
		}
	}

	// ==================== 注册器核心方法 ====================

	/**
	 * 注册处理器实例
	 */
	private static void registerProcessor(ProcessType processType, Class<?> handlerClass, Map<Integer, Handler> processorMap) {
		int processId = processType.value();
		// 重复ID检查
		if (processorMap.containsKey(processId)) {
			logger.error("Duplicate process ID: {} for handler: {}", processId, handlerClass.getName());
			throw new IllegalStateException(String.format("Duplicate process ID %d for handler %s", processId, handlerClass.getName()));
		}

		// 获取或创建处理器实例
		Handler processor = (Handler) GLOBAL_HANDLER_CACHE.computeIfAbsent(handlerClass, key -> createHandlerInstance(processId, handlerClass));

		if (processor != null) {
			processorMap.put(processId, processor);
			logger.debug("Registered processor: {} -> {} ", processId, handlerClass.getSimpleName());
		} else {
			logger.error("Failed to create handler instance: {}", handlerClass.getName());
		}
	}

	/**
	 * 处理器绑定核心方法
	 */
	private static <K, T> void putHandle(K key, Class<?> aclass, Map<K, T> handles,
										 Map<Class<?>, T> classProcessMap) {
		T handler = handles.get(key);
		if (handler != null) {
			logger.error("putHandle same key:{} old:{} new:{} ", key, handler.getClass(), aclass);
		}

		handler = classProcessMap.computeIfAbsent(aclass, k -> newInstance(key, aclass));

		if (handler != null) {
			handles.put(key, handler);
		}
	}

	/**
	 * 管理函数方法存储
	 */
	private static void managerFunctionMap(Class<?> aClass, Class<?> managerClass,
										   Map<Class<?>, Map<Class<?>, Method>> classMethodMap) {
		Method[] declaredMethods = aClass.getDeclaredMethods();
		for (Method method : declaredMethods) {
			ProcessClassMethod annotation = method.getAnnotation(ProcessClassMethod.class);
			if (annotation != null) {
				if (managerClass.isAssignableFrom(annotation.value())) {
					classMethodMap.computeIfAbsent(aClass, k -> new HashMap<>()).put(annotation.value(), method);
				}
			}
		}
	}

	// ==================== 实例创建方法 ====================

	/**
	 * 创建处理器实例
	 */
	private static Handler createHandlerInstance(int processId, Class<?> handlerClass) {
		try {
			return (Handler) handlerClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			logger.error("Create handler instance failed: {} for process ID: {}",
					handlerClass.getName(), processId, e);
			return null;
		}
	}

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