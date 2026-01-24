package com.github.liyibo1110.spring.retry;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * 异常通用测试
 * @author liyibo
 * @date 2026-01-24 12:22
 */
public abstract class AbstractExceptionTests {

    /**
     * 测试各种异常的信息生成方式，是否可以从getMessage中正常获取
     */
    @Test
    public void testExceptionString() throws Exception {
        Exception e = this.getException("foo");
        assertThat(e.getMessage()).isEqualTo("foo");
    }

    /**
     * 测试各种异常的信息生成方式，是否可以从getMessage中正常获取
     */
    @Test
    public void testExceptionStringThrowable() throws Exception {
        Exception e = this.getException("foo", new IllegalStateException());
        assertThat(e.getMessage().substring(0, 3)).isEqualTo("foo");
    }

    public abstract Exception getException(String message);

    public abstract Exception getException(String message, Throwable t);
}
