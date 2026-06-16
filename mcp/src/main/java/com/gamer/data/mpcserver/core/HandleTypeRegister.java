package com.gamer.data.mpcserver.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HandleTypeRegister 统一注册中心 - 负责消息处理器、类型转换和工厂方法的动态绑定与注册管理
 *
 * @date 2025/04/10 03:27
 */
public class HandleTypeRegister {

    // ==================== 基于 Integer 键的扫描方法 ====================

    /**
     * 初始化处理工厂 - 基于 Integer 键
     * 
     * @param packageSuperClass
     *            包名 和父类
     * @param handles
     *            处理器
     * @param <T>
     *            处理器类型
     */
    public static <T> void initSetFactory(Class<?> packageSuperClass, Map<String, T> handles) {
        try {
            long start = System.currentTimeMillis();
            List<Class<?>> classes = ClazzUtil.getAllAssignedClass(packageSuperClass);
            Map<Class<?>, T> classProcessMap = new HashMap<>();

            for (Class<?> aclass : classes) {
                Process processesType = aclass.getAnnotation(Process.class);
                if (processesType == null) {
                    continue;
                }
                putHandle(processesType.value(), aclass, handles, classProcessMap);
            }
            System.err.printf("initSetFactory %s bind success, size:%d cost:%dms %n", packageSuperClass.getName(),
                handles.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            System.err.printf("initSetFactory %s bind processors error %s", packageSuperClass.getName(), e);
        }
    }

    // ==================== 通用处理器绑定方法 ====================

    /**
     * 处理器绑定核心方法 - 通用版本
     */
    public static <K, T> void putHandle(K key, Class<?> aclass, Map<K, T> handles, Map<Class<?>, T> classProcessMap) {
        T handler = handles.get(key);
        if (handler != null) {
            System.err.printf("putHandle same key:%s old:%s new: %s", key, handler.getClass(), aclass);
        }

        handler = classProcessMap.computeIfAbsent(aclass, k -> newInstance(key, aclass));

        if (handler != null) {
            handles.put(key, handler);
        }
    }

    // ==================== 实例创建方法 ====================

    /**
     * 创建通用实例
     */
    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Object key, Class<?> aclass) {
        try {
            return (T)aclass.getConstructor().newInstance();
        } catch (Exception e) {
            System.err.printf("newInstance %s newInstance fail for key %s %s", aclass.getName(),
                key.getClass().getSimpleName(), e);
        }
        return null;
    }
}