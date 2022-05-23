package noob.toolbox.util;

import noob.toolbox.exception.CustomerException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

public class Assert {

    /**
     * 断言一个布尔表达式，如果表达式的计算结果为 false，则抛出异常
     * @param expression 布尔表达式
     * @param message 断言失败时使用的异常消息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new CustomerException(message);
        }
    }

    /**
     * 断言一个布尔表达式，如果表达式的计算结果为 true，则抛出异常
     * @param expression 布尔表达式
     * @param message 断言失败时使用的异常消息
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new CustomerException(message);
        }
    }

    /**
     * 断言一个对象为空
     * @param object 要检查的对象
     * @param message 断言失败时使用的异常消息
     */
    public static void isNull(@Nullable Object object, String message) {
        if (object != null) {
            throw new CustomerException(message);
        }
    }

    /**
     * 断言一个对象不为空
     * @param object 要检查的对象
     * @param message 断言失败时使用的异常消息
     */
    public static void notNull(@Nullable Object object, String message) {
        if (object == null) {
            throw new CustomerException(message);
        }
    }

    /**
     * 断言集合、字符串为空
     * @param obj
     * @param message
     */
    public static void isEmpty(Object obj, String message) {
        if (!ObjectUtils.isEmpty(obj)) {
            throw new CustomerException(message);
        }
    }

    /**
     * 断言集合、字符串不为空
     * @param obj
     * @param message
     */
    public static void notEmpty(Object obj, String message) {
        if (ObjectUtils.isEmpty(obj)) {
            throw new CustomerException(message);
        }
    }
}
