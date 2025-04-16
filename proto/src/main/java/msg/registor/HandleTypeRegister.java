package msg.registor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * 绑定处理类型与消息类类型
	 */
	public static void bindTransMap(Class<?> classes, Map<Integer, Class<?>> transMap) {
		try {
			Field[] fields = classes.getFields();
			for(Field field: fields){
				ClassType annotation = field.getAnnotation(ClassType.class);
				if(annotation!= null){
					transMap.put((int) field.get(null), annotation.value());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("{} bindTransMap success bind size:{}", classes.getName(), transMap.size());
	}

	/**
	 * 绑定处理类型与处理器
	 */
	public static void bindProcess(String packages, Map<Integer, Handler> processorMap) {
		try {
			List<Class<?>> classes = ClazzUtil.getClasses(packages);
			doBind(processorMap, classes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("{} bindProcess success bind size:{}", packages, processorMap.size());
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
	 * 绑定处理类型与处理器
	 */
	public static void bindProcess(Class<?> packageClass, Map<Integer, Handler> processorMap, String except) {
		try {
			List<Class<?>> classes = ClazzUtil.getAllClassExceptPackageClass(packageClass, except);
			doBind(processorMap, classes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.error("{} bindProcess success bind size:{}", packageClass.getPackage().getName(), processorMap.size());
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

}
