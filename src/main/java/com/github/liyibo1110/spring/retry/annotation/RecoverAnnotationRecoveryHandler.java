package com.github.liyibo1110.spring.retry.annotation;

import com.github.liyibo1110.spring.classify.SubclassClassifier;
import com.github.liyibo1110.spring.retry.ExhaustedRetryException;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.interceptor.MethodInvocationRecoverer;
import com.github.liyibo1110.spring.retry.support.RetrySynchronizationManager;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于Recover注解的recoverer实现
 * 职责是在可能多个Recover注解的方法中，选择最正确的那个（方法选择器）
 * @author liyibo
 * @date 2026-01-30 17:56
 */
public class RecoverAnnotationRecoveryHandler<T> implements MethodInvocationRecoverer<T> {
    /** 特定异常对应的特定recover method处理方法 */
    private final SubclassClassifier<Throwable, Method> classifier = new SubclassClassifier<>();
    /** Recovery注解标注的method -> method参数个数 + 对应的Throwable（可能为null） */
    private final Map<Method, SimpleMetadata> methods = new HashMap<>();
    private final Object target;
    private String recoverMethodName;

    public RecoverAnnotationRecoveryHandler(Object target, Method method) {
        this.target = target;
        this.init(target, method);
    }

    /**
     * args就是retryable注解要重试的method的参数，cause是重试最终失败的附带异常
     */
    @Override
    public T recover(Object[] args, Throwable cause) {
        Method method = this.findClosestMatch(args, cause.getClass());
        if(method == null)
            throw new ExhaustedRetryException("Cannot locate recovery method", cause);
        SimpleMetadata meta = this.methods.get(method);
        // 拼出完整的参数值列表
        Object[] argsToUse = meta.getArgs(cause, args);
        ReflectionUtils.makeAccessible(method);
        RetryContext context = RetrySynchronizationManager.getContext();

        Object proxy = null;
        if(context != null) {
            proxy = context.getAttribute("___proxy___");
            if(proxy != null) {
                Method proxyMethod = this.findMethodOnProxy(method, proxy);
                if(proxyMethod == null)
                    proxy = null;
                else
                    method = proxyMethod;
            }
        }
        if(proxy == null)   // 没找到代理，只能用原始的实例了
            proxy = this.target;
        T result = (T)ReflectionUtils.invokeMethod(method, proxy, argsToUse);
        return result;
    }

    /**
     * 确认Method是否在proxy实例上确实有，没有则返回null
     * 例如：
     * 1、method是个private。
     * 2、不是接口里的方法。
     * 3、proxy来自JDK动态代理。
     */
    private Method findMethodOnProxy(Method method, Object proxy) {
        try {
            return proxy.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    /**
     * 根据给定的参数值和Throwable，寻找最合适的recover method
     */
    private Method findClosestMatch(Object[] args, Class<? extends Throwable> cause) {
        Method result = null;
        if(StringUtils.hasText(this.recoverMethodName)) {   // 说明从@Retryable里面直接获取到了recovery method name
            for(var entry : this.methods.entrySet()) {  // 直接遍历，从methods里面找匹配
                Method method = entry.getKey();
                if(method.getName().equals(this.recoverMethodName)) {
                    SimpleMetadata meta = entry.getValue();
                    if((meta.type == null || meta.type.isAssignableFrom(cause))
                        && this.compareParameters(args, meta.getArgCount(), method.getParameterTypes(), true)) {
                        result = method;
                        break;
                    }
                }
            }
        }else { // @Retryable里面没有指定recovery method name
            int min = Integer.MAX_VALUE;
            for(var entry : this.methods.entrySet()) {  // 直接遍历，从methods里面找匹配
                Method method = entry.getKey();
                SimpleMetadata meta = entry.getValue();
                Class<? extends Throwable> type = meta.getType();
                if(type == null)
                    type = Throwable.class;
                if(type.isAssignableFrom(cause)) {  // 说明异常对上了
                    int distance = this.calculateDistance(cause, type); // 寻找最符合实际异常的recovery异常版本
                    if(distance < min) {
                        min = distance;
                        result = method;
                    }else if(distance == min) {
                        boolean parametersMatch = compareParameters(args, meta.getArgCount(),
                                method.getParameterTypes(), false);
                        if(parametersMatch)
                            result = method;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 计算2个异常类之间的继承相关层数
     */
    private int calculateDistance(Class<? extends Throwable> cause, Class<? extends Throwable> type) {
        int result = 0;
        Class<?> current = cause;
        while(current != type && current != Throwable.class) {
            result++;
            current = current.getSuperclass();
        }
        return result;
    }

    private boolean compareParameters(Object[] args, int argCount, Class<?>[] parameterTypes, boolean withRecoverMethodName) {
        if((withRecoverMethodName && argCount == args.length) || argCount == (args.length + 1)) {
            int startingIndex = 0;
            if(parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0]))
                startingIndex = 1;
            for(int i = startingIndex; i < parameterTypes.length; i++) {
                final Object argument = i - startingIndex < args.length ? args[i - startingIndex] : null;
                if(argument == null)
                    continue;
                Class<?> parameterType = parameterTypes[i];
                parameterType = ClassUtils.resolvePrimitiveIfNecessary(parameterType);
                if(!parameterType.isAssignableFrom(argument.getClass()))
                    return false;
            }
            // 通过了for的逐项检查，说明参数完全一致
            return true;
        }
        return false;
    }

    private void init(final Object target, Method method) {
        final Map<Class<? extends Throwable>, Method> types = new HashMap<>();
        final Method failingMethod = method;
        // 优先找Retryable注解上的recover字段值
        Retryable retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);
        if(retryable != null)
            this.recoverMethodName = retryable.recover();

        ReflectionUtils.doWithMethods(target.getClass(), candidate -> {
            // 寻找带有Recover注解的Method
            Recover recover = AnnotatedElementUtils.findMergedAnnotation(candidate, Recover.class);
            if(recover == null) // 找不到就在代理目标类再找
                recover = this.findAnnotationOnTarget(target, candidate);
            if(recover != null && failingMethod.getGenericReturnType() instanceof ParameterizedType
                && candidate.getGenericReturnType() instanceof ParameterizedType) {
                // 要判断retryable方法和recover方法的返回值要一致（要符合recover的语义）
                if(isParameterizedTypeAssignable((ParameterizedType)candidate.getGenericReturnType(),
                    (ParameterizedType)failingMethod.getGenericReturnType())) {
                    this.putToMethodsMap(candidate, types);
                }
            }else if(recover != null && candidate.getReturnType().isAssignableFrom(failingMethod.getReturnType())) {
                this.putToMethodsMap(candidate, types);
            }
        });
        this.classifier.setTypeMap(types);
        this.optionallyFilterMethodsBy(failingMethod.getReturnType());
    }


    /**
     * 判断2个泛型类型是否一致，判断时会考虑嵌套泛型（注意传进来的都是泛型类型）
     * @param methodReturnType @Recover注解标记的方法
     * @param failingMethodReturnType @Retryable注解标记的方法
     */
    private static boolean isParameterizedTypeAssignable(ParameterizedType methodReturnType,
                                                         ParameterizedType failingMethodReturnType) {
        Type[] methodActualArgs = methodReturnType.getActualTypeArguments();
        Type[] failingMethodActualArgs = failingMethodReturnType.getActualTypeArguments();
        if(methodActualArgs.length != failingMethodActualArgs.length)
            return false;

        int startingIndex = 0;
        for(int i = startingIndex; i < methodActualArgs.length; i++) {
            Type methodArgType = methodActualArgs[i];
            Type failingMethodArgType = failingMethodActualArgs[i];
            // 泛型有可能是嵌套的，例如返回值是Response<List<String>>这样子，递归判断即可
            if(methodArgType instanceof ParameterizedType && failingMethodArgType instanceof ParameterizedType) {
                if(!isParameterizedTypeAssignable((ParameterizedType)methodArgType, (ParameterizedType)failingMethodArgType))
                    return false;
            }else if(methodArgType instanceof Class && failingMethodArgType instanceof Class) {
                if(!failingMethodArgType.equals(methodArgType))
                    return false;
            }else if(!methodArgType.equals(failingMethodArgType))
                return false;
        }
        return true;
    }

    /**
     * Throwable -> Recover注解标记的method
     * 根据Recover规定，recover方法第一个参数可以是Throwable，后面的参数必须要和retryable保持前缀一致（但也可以没有参数）
     */
    private void putToMethodsMap(Method method, Map<Class<? extends Throwable>, Method> types) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 第1个参数是Throwable，则放置
        if(parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0])) {
            Class<? extends Throwable> type = (Class<? extends Throwable>)parameterTypes[0];
            types.put(type, method);
            RecoverAnnotationRecoveryHandler.this.methods.put(method, new SimpleMetadata(parameterTypes.length, type));
        }else { // 不是Throwable或者压根没参数
            RecoverAnnotationRecoveryHandler.this.classifier.setDefaultValue(method);   // 匹配不到异常，就作为保底recover方法
            RecoverAnnotationRecoveryHandler.this.methods.put(method, new SimpleMetadata(parameterTypes.length, null));
        }
    }

    private Recover findAnnotationOnTarget(Object target, Method method) {
        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            return AnnotatedElementUtils.findMergedAnnotation(targetMethod, Recover.class);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * 防止歧义的保底处理。
     * 如果至少存在1个：返回值类型和failingMethod返回值完全一致的@Recovery方法，
     * 则丢弃所有返回值可赋值，但又不完全一致的@Recovery方法。
     * 个人认为应该后续追加的bug fix方案，因为只有遇到了完全一致的返回值类型才会触发里面的替换逻辑。
     */
    private void optionallyFilterMethodsBy(Class<?> returnClass) {
        Map<Method, SimpleMetadata> filteredMethods = new HashMap<>();
        for(Method method : this.methods.keySet()) {
            if(method.getReturnType() == returnClass)
                filteredMethods.put(method, this.methods.get(method));
        }

        if(filteredMethods.size() > 0) {
            this.methods.clear();
            this.methods.putAll(filteredMethods);
        }
    }

    /**
     * Recover注解标注的method的参数情况
     */
    private static class SimpleMetadata {
        /** 方法参数总个数 */
        private final int argCount;
        /** 对应的异常类型，可以为null */
        private final Class<? extends Throwable> type;

        public SimpleMetadata(int argCount, Class<? extends Throwable> type) {
            super();
            this.argCount = argCount;
            this.type = type;
        }

        public int getArgCount() {
            return this.argCount;
        }

        public Class<? extends Throwable> getType() {
            return this.type;
        }

        public Object[] getArgs(Throwable t, Object[] args) {
            Object[] result = new Object[getArgCount()];
            int startArgs = 0;
            if(this.type != null) {
                result[0] = t;
                startArgs = 1;
            }
            int length = Math.min(result.length - startArgs, args.length);
            if(length == 0)
                return result;
            System.arraycopy(args, 0, result, startArgs, length);
            return result;
        }
    }
}
