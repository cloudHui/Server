package msg.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 注册参数
 *
 * @param <T>
 *            处理类型
 * @param <K>
 *            键类型
 * @author liuyunhui
 * @date 2026-01-16
 */
public class RegistryParam<K, T> {
    public Annotation annotation;
    public Class<?> aclass;
    public Map<K, T> handles;
    public Map<Class<?>, T> classProcessMap;

    public RegistryParam(Annotation annotation, Class<?> aclass, Map<K, T> handles, Map<Class<?>, T> classProcessMap) {
        this.annotation = annotation;
        this.aclass = aclass;
        this.handles = handles;
        this.classProcessMap = classProcessMap;
    }
}