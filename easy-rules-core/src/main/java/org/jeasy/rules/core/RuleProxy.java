/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.jeasy.rules.core;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;

/**
 * Main class to create rule proxies from annotated objects.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class RuleProxy implements InvocationHandler {

    /**
     * 被代理的目标对象
     */
    private final Object target;
    private String name;
    private String description;
    private Integer priority;
    private Method[] methods;
    private Method conditionMethod;
    private Set<ActionMethodOrderBean> actionMethods;
    private Method compareToMethod;
    private Method toStringMethod;
    private org.jeasy.rules.annotation.Rule annotation;

    private static final RuleDefinitionValidator ruleDefinitionValidator = new RuleDefinitionValidator();
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleProxy.class);

    /**
     * Makes the rule object implement the {@link Rule} interface.
     *
     * @param rule the annotated rule object.
     * @return a proxy that implements the {@link Rule} interface.
     */
    public static Rule asRule(final Object rule) {
        Rule result;
        /*
         * 这里可以看出定义的规则都是实现Rule接口去写，不用走代理代码，可以很大的提高运行性能
         */
        if (rule instanceof Rule) {
            // 如果是Rule类型，直接Rule返回
            result = (Rule) rule;
        } else {
            // 不是Rule类型（要校验是否使用了对应的注解）
            ruleDefinitionValidator.validateRuleDefinition(rule);

            // 上面校验通过后，使用Rule类型对其进行代理

            /**
             *<pre class="code">
             *    //新汽车
             *    Car proxyCar = (Car)Proxy.newProxyInstance(
             *        TestDemo.class.getClassLoader(),
             *        car.getClass().getInterfaces(),
             *       new InvocationHandler() {
             *           @Override
             *           public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
             *               //增强代码
             *               if("jump".equals(method.getName())){
             *                   System.out.println("开始增强--飞");
             *                   method.invoke(car,args); //对方法进行增强后,放行原来的方法
             *                   return null;//在此处 return 表示后续的代码将不再运行 直接返回
             *               }
             *               //放行原来放法
             *               method.invoke( car , args);//参数1:实例对象   参数2: 真实的参数
             *               return null;
             *           }
             *        }
             *    );
             *
             *    proxyCar.run();
             *    proxyCar.jump();
             *    proxyCar.stop();
             *</pre>
             */


            /*
             * 学习：InvocationHandler-每个代理实例都有一个关联的调用处理程序。 当在代理实例上调用方法时，方法调用将被编码并分派到其调用处理程序的invoke方法。
             *
             * Rule.class.getClassLoader()：代理类的类加载类
             *  new Class[]{Rule.class, Comparable.class}: 要创建的代理类的类型 + 及其实现的接口列表
             *  new RuleProxy(rule)：调用处理器对象：会默认调用invoke方法，将方法调用分派到的调用处理程序
             */
            result = (Rule) Proxy.newProxyInstance(Rule.class.getClassLoader(), new Class[]{Rule.class, Comparable.class}, new RuleProxy(rule));
        }
        return result;
    }

    private RuleProxy(final Object target) {
        this.target = target;
    }

    /**
     * 覆写了InvocationHandler的invoke方法，会默认调用到这个方法里来
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        String methodName = method.getName();
        // 给代理对象Rule设置规则内容（将被代理对象（没有实现Rule的规则对象才会有这些操作）的规则描述填充到代理对象中）
        switch (methodName) {
            case "getName":
                // 获取规则的名称
                return getRuleName();
            case "getDescription":
                // 获取规则的描述"when（相当于condition） then（相当于action）"
                return getRuleDescription();
            case "getPriority":
                return getRulePriority();
            case "compareTo":
                return compareToMethod(args);
            case "evaluate":

                // 执行evaluate方法，evaluate()封装规则的条件（conditions）- 封装了必须求值为TRUE才能触发规则的条件
                return evaluateMethod(args);
            case "execute":
                // 执行execute方法，execute()执行匹配成功后的操作
                return executeMethod(args);
            case "equals":
                return equalsMethod(args);
            case "hashCode":
                return hashCodeMethod();
            case "toString":
                return toStringMethod();
            default:
                return null;
        }
    }

    private Object evaluateMethod(final Object[] args) throws IllegalAccessException, InvocationTargetException {
        Facts facts = (Facts) args[0];
        Method conditionMethod = getConditionMethod();
        try {

            // 获取评估的数据
            List<Object> actualParameters = getActualParameters(conditionMethod, facts);

            // 执行evaluate方法，condition中的数据进行评估
            return conditionMethod.invoke(target, actualParameters.toArray()); // validated upfront
        } catch (NoSuchFactException e) {
            LOGGER.warn("Rule '{}' has been evaluated to false due to a declared but missing fact '{}' in {}",
                    getTargetClass().getName(), e.getMissingFact(), facts);
            return false;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Types of injected facts in method '{}' in rule '{}' do not match parameters types",
                    conditionMethod.getName(), getTargetClass().getName(), e);
            return false;
        }
    }

    private Object executeMethod(final Object[] args) throws IllegalAccessException, InvocationTargetException {
        Facts facts = (Facts) args[0];
        for (ActionMethodOrderBean actionMethodBean : getActionMethodBeans()) {
            Method actionMethod = actionMethodBean.getMethod();
            List<Object> actualParameters = getActualParameters(actionMethod, facts);
            actionMethod.invoke(target, actualParameters.toArray());
        }
        return null;
    }

    private Object compareToMethod(final Object[] args) throws Exception {
        Method compareToMethod = getCompareToMethod();
        Object otherRule = args[0]; // validated upfront
        if (compareToMethod != null && Proxy.isProxyClass(otherRule.getClass())) {
            if (compareToMethod.getParameters().length != 1) {
                throw new IllegalArgumentException("compareTo method must have a single argument");
            }
            RuleProxy ruleProxy = (RuleProxy) Proxy.getInvocationHandler(otherRule);
            return compareToMethod.invoke(target, ruleProxy.getTarget());
        } else {
            return compareTo((Rule) otherRule);
        }
    }

    /**
     * 获取评估的数据
     */
    private List<Object> getActualParameters(Method method, Facts facts) {
        List<Object> actualParameters = new ArrayList<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (Annotation[] annotations : parameterAnnotations) {
            if (annotations.length == 1) {
                String factName = ((Fact) (annotations[0])).value(); //validated upfront.
                Object fact = facts.get(factName);
                if (fact == null && !facts.asMap().containsKey(factName)) {
                    throw new NoSuchFactException(format("No fact named '%s' found in known facts: %n%s", factName, facts), factName);
                }
                actualParameters.add(fact);
            } else {
                actualParameters.add(facts); //validated upfront, there may be only one parameter not annotated and which is of type Facts.class
            }
        }
        return actualParameters;
    }

    private boolean equalsMethod(final Object[] args) throws Exception {
        if (!(args[0] instanceof Rule)) {
            return false;
        }
        Rule otherRule = (Rule) args[0];
        int otherPriority = otherRule.getPriority();
        int priority = getRulePriority();
        if (priority != otherPriority) {
            return false;
        }
        String otherName = otherRule.getName();
        String name = getRuleName();
        if (!name.equals(otherName)) {
            return false;
        }
        String otherDescription = otherRule.getDescription();
        String description =  getRuleDescription();
        return Objects.equals(description, otherDescription);
    }

    private int hashCodeMethod() throws Exception {
        int result   = getRuleName().hashCode();
        int priority = getRulePriority();
        String description = getRuleDescription();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + priority;
        return result;
    }

    private Method getToStringMethod() {
        if (this.toStringMethod == null) {
            Method[] methods = getMethods();
            for (Method method : methods) {
                if ("toString".equals(method.getName())) {
                    this.toStringMethod = method;
                    return this.toStringMethod;
                }
            }
        }
        return this.toStringMethod;
    }

    private String toStringMethod() throws Exception {
        Method toStringMethod = getToStringMethod();
        if (toStringMethod != null) {
            return (String) toStringMethod.invoke(target);
        } else {
            return getRuleName();
        }
    }

    private int compareTo(final Rule otherRule) throws Exception {
        int otherPriority = otherRule.getPriority();
        int priority = getRulePriority();
        if (priority < otherPriority) {
            return -1;
        } else if (priority > otherPriority) {
            return 1;
        } else {
            String otherName = otherRule.getName();
            String name = getRuleName();
            return name.compareTo(otherName);
        }
    }

    private int getRulePriority() throws Exception {
        if (this.priority == null) {
            int priority = Rule.DEFAULT_PRIORITY;

            org.jeasy.rules.annotation.Rule rule = getRuleAnnotation();
            if (rule.priority() != Rule.DEFAULT_PRIORITY) {
                priority = rule.priority();
            }

            Method[] methods = getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Priority.class)) {
                    priority = (int) method.invoke(target);
                    break;
                }
            }
            this.priority = priority;
        }
        return this.priority;
    }

    private Method getConditionMethod() {
        if (this.conditionMethod == null) {
            Method[] methods = getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Condition.class)) {
                    this.conditionMethod = method;
                    return this.conditionMethod;
                }
            }
        }
        return this.conditionMethod;
    }

    private Set<ActionMethodOrderBean> getActionMethodBeans() {
        if (this.actionMethods == null) {
            this.actionMethods = new TreeSet<>();
            Method[] methods = getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Action.class)) {
                    Action actionAnnotation = method.getAnnotation(Action.class);
                    int order = actionAnnotation.order();
                    this.actionMethods.add(new ActionMethodOrderBean(method, order));
                }
            }
        }
        return this.actionMethods;
    }

    private Method getCompareToMethod() {
        if (this.compareToMethod == null) {
            Method[] methods = getMethods();
            for (Method method : methods) {
                if (method.getName().equals("compareTo")) {
                    this.compareToMethod = method;
                    return this.compareToMethod;
                }
            }
        }
        return this.compareToMethod;
    }

    private Method[] getMethods() {
        if (this.methods == null) {
            this.methods = getTargetClass().getMethods();
        }
        return this.methods;
    }

    private org.jeasy.rules.annotation.Rule getRuleAnnotation() {
        if (this.annotation == null) {
            this.annotation = Utils.findAnnotation(org.jeasy.rules.annotation.Rule.class, getTargetClass());
        }
        return this.annotation;
    }

    private String getRuleName() {
        if (this.name == null) {
            org.jeasy.rules.annotation.Rule rule = getRuleAnnotation();
            this.name = rule.name().equals(Rule.DEFAULT_NAME) ? getTargetClass().getSimpleName() : rule.name();
        }
        return this.name;
    }

    private String getRuleDescription() {
        if (this.description == null) {
            // Default description = "when " + conditionMethodName + " then " + comma separated actionMethodsNames
            StringBuilder description = new StringBuilder();
            appendConditionMethodName(description);
            appendActionMethodsNames(description);
            org.jeasy.rules.annotation.Rule rule = getRuleAnnotation();
            this.description = rule.description().equals(Rule.DEFAULT_DESCRIPTION) ? description.toString() : rule.description();
        }
        return this.description;
    }

    private void appendConditionMethodName(StringBuilder description) {
        Method method = getConditionMethod();
        if (method != null) {
            description.append("when ");
            description.append(method.getName());
            description.append(" then ");
        }
    }

    private void appendActionMethodsNames(StringBuilder description) {
        Iterator<ActionMethodOrderBean> iterator = getActionMethodBeans().iterator();
        while (iterator.hasNext()) {
            description.append(iterator.next().getMethod().getName());
            if (iterator.hasNext()) {
                description.append(",");
            }
        }
    }
    
    public Object getTarget() {
        return target;
    }

    private Class<?> getTargetClass() {
        return target.getClass();
    }

}
