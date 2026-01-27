package msg.annotation;

/**
 * 处理类型注册
 *
 * @param <k>
 *          键类型
 * @param <T>
 *          处理类型
 * @author liuyunhui
 * @date 2026-01-16
 */
public interface Register<k, T> {
  void handle(RegistryParam<k, T> param);
}