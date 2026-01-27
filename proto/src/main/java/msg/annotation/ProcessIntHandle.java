package msg.annotation;


import msg.registor.HandleTypeRegister;

/**
 * ProcessInt 的注解处理方法
 * 
 * @author liuyunhui
 * @date 2026-01-16
 */
@ProcessClass(ProcessType.class)
public class ProcessIntHandle implements Register<Integer, Integer> {

    @Override
    public void handle(RegistryParam<Integer, Integer> param) {
            HandleTypeRegister.putHandle(((ProcessType)param.annotation).value(), param.aclass, param.handles, param.classProcessMap);
    }

}
