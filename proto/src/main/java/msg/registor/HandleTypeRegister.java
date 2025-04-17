package msg.registor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import msg.MessageId;
import msg.MessageTrans;
import msg.annotation.ClassType;
import msg.annotation.ProcessType;
import net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.other.ClazzUtil;

/**
 * @author admin
 * @className MessageRegister
 * @description 处理类型注册绑定
 * @createDate 2025/4/10 3:27
 */
public class HandleTypeRegister {

	private static final Logger logger = LoggerFactory.getLogger(HandleTypeRegister.class);

	/**
	 * 绑定处理类型与消息类类型(专用消息)
	 */
	public static void bindUniqTransMap(Class<?> classes, Map<Integer, Class<?>> transMap) {
		try {
			Field[] fields = classes.getFields();
			for (Field field : fields) {
				ClassType annotation = field.getAnnotation(ClassType.class);
				if (annotation == null) {
					continue;
				}
				transMap.put((int) field.get(null), annotation.value());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("{} bindUniqTransMap success bind size:{}", classes.getName(), transMap.size());

		//Todo
		for (Map.Entry<Integer, Class<?>> entry : transMap.entrySet()) {
			System.out.println(entry.getKey() + "  " + entry.getValue().getName());
		}
	}

	/**
	 * 绑定处理类型与消息类类型(通用消息处理)
	 *
	 * @param type 消息转化类型 MessageId.SERVER MessageId.CLIENT
	 */
	public static void bindCommonTransMap(Class<?> classes, Map<Integer, Class<?>> transMap, int type) {
		try {
			String packageName = classes.getPackage().getName();
			Field[] fields = MessageId.class.getFields();
			for (Field field : fields) {
				ClassType annotation = field.getAnnotation(ClassType.class);
				if (annotation == null) {
					continue;
				}
				MessageTrans[] messageTrans = annotation.messageTrans();
				if (messageTrans.length == 0) {
					continue;
				}
				for (MessageTrans trans : messageTrans) {
					//这个类包名有当前服务名字的
					if (packageName.contains(trans.getServerType().name().toLowerCase())) {
						for (int serverClient : trans.getServerClient()) {
							if (type == serverClient) {
								transMap.put((int) field.get(null), annotation.value());
								break;
							}
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("{} bindCommonTransMap success bind size:{}", classes.getName(), transMap.size());


		//Todo
		for (Map.Entry<Integer, Class<?>> entry : transMap.entrySet()) {
			System.out.println(entry.getKey() + "  " + entry.getValue().getName());
		}
	}

	/**
	 * 绑定处理类型与处理器
	 */
	public static void bindPackageProcess(String packages, Map<Integer, Handler> processorMap) {
		try {
			List<Class<?>> classes = ClazzUtil.getClasses(packages);
			doBind(processorMap, classes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("{} bindPackageProcess success bind size:{}", packages, processorMap.size());


		//Todo
		for (Map.Entry<Integer, Handler> entry : processorMap.entrySet()) {
			System.out.println(entry.getKey() + "  " + entry.getValue().getClass().getName());
		}
	}

	/**
	 * 绑定处理类型与处理器
	 */
	public static void bindClassProcess(Class<?> packageClass, Map<Integer, Handler> processorMap) {
		try {
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(packageClass, "");
			doBind(processorMap, classes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("class:{} bindClassProcess success bind size:{}", packageClass.getPackage().getName(), processorMap.size());


		//Todo
		for (Map.Entry<Integer, Handler> entry : processorMap.entrySet()) {
			System.out.println(entry.getKey() + "  " + entry.getValue().getClass().getName());
		}
	}

	/**
	 * 绑定
	 */
	private static void doBind(Map<Integer, Handler> processorMap, List<Class<?>> classes) {
		Map<Class<?>, Handler> classProcessMap = new HashMap<>();
		for (Class<?> aclass : classes) {
			try {
				if (!Handler.class.isAssignableFrom(aclass)) {
					continue;
				}
				ProcessType processesType = aclass.getAnnotation(ProcessType.class);
				if (processesType != null) {
					putProcess(processesType.value(), aclass, processorMap, classProcessMap);
				} else {
					logger.error("{} no Annotation ProcessType", aclass);
				}
			} catch (Exception e) {
				logger.error("{} {}", aclass, e.getMessage());
			}
		}
	}

	/**
	 * 绑定消息和处理器
	 *
	 * @param processorMap    绑定关系map
	 * @param classProcessMap 实例化存储map 方式多次实例化
	 */
	private static void putProcess(int processId, Class<?> aclass, Map<Integer, Handler> processorMap, Map<Class<?>, Handler> classProcessMap) {
		if (processorMap.containsKey(processId)) {
			try {
				throw new Exception("init " + aclass + " same processId " + processId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Handler iProcessor = classProcessMap.computeIfAbsent(aclass, k -> {
			try {
				return (Handler) aclass.getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		});
		if (iProcessor != null) {
			processorMap.put(processId, iProcessor);
		} else {
			try {
				throw new Exception("init " + aclass + " newInstance fail " + processId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 消息转化
	 */
	public static Message parserMessage(int id, byte[] bytes, Map<Integer, Class<?>> map) {
		Class<?> aClass = map.get(id);
		if (aClass != null) {
			try {
				return (Message) MessageId.getMessageObject((Class<MessageLite>) aClass, bytes);
			} catch (Exception e) {
				logger.error("[parserMessage Exception messageId :{} className:{}]", id, aClass.getSimpleName(), e);
			}
		} else {
			logger.error("[parserMessage error messageId :{} can not find messageType class]", id);
		}
		return null;
	}
}
