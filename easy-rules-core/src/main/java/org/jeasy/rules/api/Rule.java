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
package org.jeasy.rules.api;

/**
 * Abstraction for a rule that can be fired by a rules engine.
 * （用于描述规则）规则的描述接口 + 对规则的操作
 *
 * Rules are registered in a namespace of rule of type {@link Rules}
 * in which they must have a <strong>unique</strong> name.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public interface Rule extends Comparable<Rule> {

    /**
     * Default rule name.
     */
    String DEFAULT_NAME = "rule";

    /**
     * Default rule description.
     */
    String DEFAULT_DESCRIPTION = "description";

    /**
     * Default rule priority.
     */
    int DEFAULT_PRIORITY = Integer.MAX_VALUE - 1;

    /**
     * Getter for rule name.
     * @return the rule name
     */
    default String getName() {
        return DEFAULT_NAME;
    }

    /**
     * Getter for rule description.
     * @return rule description
     */
    default String getDescription() {
        return DEFAULT_DESCRIPTION;
    }

    /**
     * Getter for rule priority.
     * @return rule priority
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * 规则引擎判断条件: 如果提供的facts被应用到规则上返回true，否则返回false
     * This method implements the rule's condition(s).
     * <strong>Implementations should handle any runtime exception and return true/false accordingly</strong>
     *
     * @return true if the rule should be applied given the provided facts, false otherwise
     *         如果提供的事实适用于该规则返回true, 否则，返回false
     */
    boolean evaluate(Facts facts);

    /**
     * 规则引擎判断条件返回true后，执行此方法
     * This method implements the rule's action(s).
     * @throws Exception thrown if an exception occurs when performing action(s)
     */
    void execute(Facts facts) throws Exception;

}
