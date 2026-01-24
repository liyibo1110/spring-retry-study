package com.github.liyibo1110.spring.retry;

import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author liyibo
 * @date 2026-01-24 14:27
 */
public class AnyThrowTests {

    @Test
    public void testRuntimeException() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> AnyThrow.throwAny(new RuntimeException("planned")));
    }

    @Test
    public void testUncheckedRuntimeException() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> AnyThrow.throwUnchecked(new RuntimeException("planned")));
    }

    private static class AnyThrow {
        private static void throwUnchecked(Throwable e) {
            AnyThrow.throwAny(e);
        }

        /**
         * 将给定异常强制转换成特定异常
         */
        private static <E extends Throwable> void throwAny(Throwable e) throws E {
            throw(E)e;
        }
    }
}
