package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * SimpleRetryPolicy的特殊扩展policy，主要区别在canRetry中，最终判断将由一个SpEL表达式来决定
 * @author liyibo
 * @date 2026-01-30 15:41
 */
public class ExpressionRetryPolicy extends SimpleRetryPolicy implements BeanFactoryAware {
    private static final Log logger = LogFactory.getLog(ExpressionRetryPolicy.class);

    private static final TemplateParserContext PARSER_CONTEXT = new TemplateParserContext();

    private final Expression expression;

    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    public ExpressionRetryPolicy(Expression expression) {
        Assert.notNull(expression, "expression cannot be null");
        this.expression = expression;
    }

    public ExpressionRetryPolicy(String expressionString) {
        Assert.notNull(expressionString, "'expressionString' cannot be null");
        this.expression = getExpression(expressionString);
    }

    public ExpressionRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
                                 boolean traverseCauses, Expression expression) {
        super(maxAttempts, retryableExceptions, traverseCauses);
        Assert.notNull(expression, "expression cannot be null");
        this.expression = expression;
    }

    public ExpressionRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
                                 boolean traverseCauses, String expressionString, boolean defaultValue) {
        super(maxAttempts, retryableExceptions, traverseCauses, defaultValue);
        Assert.notNull(expressionString, "expressionString cannot be null");
        this.expression = getExpression(expressionString);
    }

    /**
     * 让SpEL表达式可以用Spring里面的Bean
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
    }

    public ExpressionRetryPolicy withBeanFactory(BeanFactory beanFactory) {
        setBeanFactory(beanFactory);
        return this;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable lastThrowable = context.getLastThrowable();
        if(lastThrowable == null)
            return super.canRetry(context);
        else {  // 在原始canRetry结果上，多了个附加条件，根据特定异常的表达式计算结果也为true才可以retry（表达式只关心异常，不关心失败次数）
            return super.canRetry(context) && Boolean.TRUE.equals(
                    this.expression.getValue(this.evaluationContext, lastThrowable, Boolean.class));
        }
    }

    private static Expression getExpression(String expression) {
        if(isTemplate(expression)) {
            logger.warn("#{...} syntax is not required for this run-time expression "
                    + "and is deprecated in favor of a simple expression string");
            return new SpelExpressionParser().parseExpression(expression, PARSER_CONTEXT);
        }
        return new SpelExpressionParser().parseExpression(expression);
    }

    public static boolean isTemplate(String expression) {
        return expression.contains(PARSER_CONTEXT.getExpressionPrefix())
                && expression.contains(PARSER_CONTEXT.getExpressionSuffix());
    }
}
