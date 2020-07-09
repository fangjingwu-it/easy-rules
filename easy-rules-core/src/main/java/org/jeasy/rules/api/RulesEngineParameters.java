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

import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.core.InferenceRulesEngine;

/**
 * 设置规则引擎执行时的一些机制（可以理解成设置 要创建的规则引擎的属性）
 * Parameters of a rules engine.
 *
 * <ul>
 *     <li>When parameters are used with a {@link DefaultRulesEngine}, they are applied on <strong>all registered rules</strong>.</li>
 *     <li>When parameters are used with a {@link InferenceRulesEngine}, they are applied on <strong>candidate rules in each iteration</strong>.</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class RulesEngineParameters {

    /**
     * Default rule priority threshold.
     */
    public static final int DEFAULT_RULE_PRIORITY_THRESHOLD = Integer.MAX_VALUE;
    
    /**
     * Parameter to skip next applicable rules when a rule is applied.
     * 在执行RuleClass中的规则时，只要有一个规则被触发，则当前被传进来的Fact就不再判断是否满足规则链上的其他规则的条件（自然也不会执行其对应的操作）
     */
    private boolean skipOnFirstAppliedRule;

    /**
     * Parameter to skip next applicable rules when a rule is non triggered
     * 客户端中每一个被 put 到 Facts 中的 number 都会对 .yml 文件中定义的所有规则按照优先级从高到低的顺序进行判断，如果满足当前的规则，
     * 则执行相应的操作，直到遇到不满足条件的规则为止，并且也不会对其他规则进行判断了
     *
     */
    private boolean skipOnFirstNonTriggeredRule;

    /**
     * Parameter to skip next applicable rules when a rule has failed.
     * 告诉引擎在规则失败时跳过后面的规则
     */
    private boolean skipOnFirstFailedRule;

    /**
     * Parameter to skip next rules if priority exceeds a user defined threshold.
     * 告诉引擎如果优先级超过定义的阈值，则大于这个阈值的优先级的规则都不会执行。版本3.3已经不支持更改，默认MaxInt
     */
    private int priorityThreshold;

    /**
     * Create a new {@link RulesEngineParameters} with default values.
     */
    public RulesEngineParameters() {
        this.priorityThreshold = RulesEngineParameters.DEFAULT_RULE_PRIORITY_THRESHOLD;
    }

    /**
     * Create a new {@link RulesEngineParameters}.
     *
     * @param skipOnFirstAppliedRule parameter to skip next applicable rules on first applied rule.
     * @param skipOnFirstFailedRule parameter to skip next applicable rules on first failed rule.
     * @param skipOnFirstNonTriggeredRule parameter to skip next applicable rules on first non triggered rule.
     * @param priorityThreshold threshold after which rules should be skipped.
     */
    public RulesEngineParameters(final boolean skipOnFirstAppliedRule, final boolean skipOnFirstFailedRule, final boolean skipOnFirstNonTriggeredRule, final int priorityThreshold) {
        this.skipOnFirstAppliedRule = skipOnFirstAppliedRule;
        this.skipOnFirstFailedRule = skipOnFirstFailedRule;
        this.skipOnFirstNonTriggeredRule = skipOnFirstNonTriggeredRule;
        this.priorityThreshold = priorityThreshold;
    }

    public int getPriorityThreshold() {
        return priorityThreshold;
    }

    public void setPriorityThreshold(final int priorityThreshold) {
        this.priorityThreshold = priorityThreshold;
    }

    public RulesEngineParameters priorityThreshold(final int priorityThreshold) {
        setPriorityThreshold(priorityThreshold);
        return this;
    }

    public boolean isSkipOnFirstAppliedRule() {
        return skipOnFirstAppliedRule;
    }

    public void setSkipOnFirstAppliedRule(final boolean skipOnFirstAppliedRule) {
        this.skipOnFirstAppliedRule = skipOnFirstAppliedRule;
    }

    public RulesEngineParameters skipOnFirstAppliedRule(final boolean skipOnFirstAppliedRule) {
        setSkipOnFirstAppliedRule(skipOnFirstAppliedRule);
        return this;
    }

    public boolean isSkipOnFirstNonTriggeredRule() {
        return skipOnFirstNonTriggeredRule;
    }

    public void setSkipOnFirstNonTriggeredRule(final boolean skipOnFirstNonTriggeredRule) {
        this.skipOnFirstNonTriggeredRule = skipOnFirstNonTriggeredRule;
    }

    public RulesEngineParameters skipOnFirstNonTriggeredRule(final boolean skipOnFirstNonTriggeredRule) {
        setSkipOnFirstNonTriggeredRule(skipOnFirstNonTriggeredRule);
        return this;
    }

    public boolean isSkipOnFirstFailedRule() {
        return skipOnFirstFailedRule;
    }

    public void setSkipOnFirstFailedRule(final boolean skipOnFirstFailedRule) {
        this.skipOnFirstFailedRule = skipOnFirstFailedRule;
    }

    public RulesEngineParameters skipOnFirstFailedRule(final boolean skipOnFirstFailedRule) {
        setSkipOnFirstFailedRule(skipOnFirstFailedRule);
        return this;
    }

    @Override
    public String toString() {
        return "Engine parameters { " +
                "skipOnFirstAppliedRule = " + skipOnFirstAppliedRule +
                ", skipOnFirstNonTriggeredRule = " + skipOnFirstNonTriggeredRule +
                ", skipOnFirstFailedRule = " + skipOnFirstFailedRule +
                ", priorityThreshold = " + priorityThreshold +
                " }";
    }
}
