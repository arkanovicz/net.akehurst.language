/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.api.parser.ParseException

fun runtimeRuleSet(init: RuntimeRuleSetBuilder2.() -> Unit): RuntimeRuleSet {
    val b = RuntimeRuleSetBuilder2()
    b.init()
    return b.ruleSet()
}

class RuntimeRuleSetBuilder2() {

    private var runtimeRuleSet: RuntimeRuleSet? = null
    private var nextGroupNumber: Int = 0
    private var nextChoiceNumber: Int = 0
    private var nextMultiNumber: Int = 0
    private var nextListNumber: Int = 0

    val rules: MutableList<RuntimeRule> = mutableListOf()

    fun createGroupRuleName(parentRuleName: String): String {
        return "§${parentRuleName}§group" + this.nextGroupNumber++ //TODO: include original rule name fo easier debug
    }

    fun createChoiceRuleName(parentRuleName: String): String { //TODO: split into priority or simple choice type
        return "§${parentRuleName}§choice" + this.nextChoiceNumber++ //TODO: include original rule name fo easier debug
    }

    fun createMultiRuleName(parentRuleName: String): String {
        return "§${parentRuleName}§multi" + this.nextMultiNumber++ //TODO: include original rule name fo easier debug
    }

    fun createListRuleName(parentRuleName: String): String {
        return "§${parentRuleName}§sList" + this.nextListNumber++ //TODO: include original rule name fo easier debug
    }

    fun findRuleByName(ruleName: String, terminal: Boolean): RuntimeRule? {
        return this.rules.firstOrNull {
            if (terminal) {
                it.isTerminal && it.name == ruleName
            } else {
                it.isNonTerminal && it.name == ruleName
            }
        }
    }

    fun ruleSet(): RuntimeRuleSet {
        if (null == this.runtimeRuleSet) {
            this.runtimeRuleSet = RuntimeRuleSet(this.rules)
        }
        return this.runtimeRuleSet ?: throw ParseException("Should never happen")
    }

    fun skip(name: String, init: RuntimeRuleBuilder2.() -> Unit): RuntimeRule {
        val b = RuntimeRuleBuilder2(this, name, RuntimeRuleItemKind.CONCATENATION, isSkip = true)
        b.init()
        return b.build()
    }

    fun literal(name: String, value: String): RuntimeRule {
        val b = RuntimeRuleBuilder2(this, name, RuntimeRuleItemKind.CONCATENATION)
        b.literal(value)
        return b.build()
    }

    fun pattern(name: String, regEx: String): RuntimeRule {
        val b = RuntimeRuleBuilder2(this, name, RuntimeRuleItemKind.CONCATENATION)
        b.pattern(regEx)
        return b.build()
    }

    private fun _rule(name: String, kind: RuntimeRuleItemKind, choiceKind: RuntimeRuleChoiceKind, init: RuntimeRuleBuilder2.() -> Unit): RuntimeRule {
        val b = RuntimeRuleBuilder2(this, name, kind, choiceKind)
        b.init()
        return b.build()
    }

    fun concatenation(name: String, init: RuntimeRuleBuilder2.() -> Unit): RuntimeRule = _rule(name, RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, init)
    fun choice(name: String, choiceKind: RuntimeRuleChoiceKind, init: RuntimeRuleBuilder2.() -> Unit): RuntimeRule = _rule(name, RuntimeRuleItemKind.CHOICE, choiceKind, init)
    fun multi(name: String, init: RuntimeRuleBuilder2.() -> Unit): RuntimeRule = _rule(name, RuntimeRuleItemKind.MULTI, RuntimeRuleChoiceKind.NONE, init)

}

class RuntimeRuleBuilder2(
        val rrsb: RuntimeRuleSetBuilder2,
        val ruleName: String,
        val kind: RuntimeRuleItemKind,
        val choiceKind: RuntimeRuleChoiceKind = RuntimeRuleChoiceKind.NONE,
        val min: Int = -1,
        val max: Int = 0,
        val isSkip: Boolean = false
) {

    private val items = mutableListOf<RuntimeRule>()

    fun literal(value: String): RuntimeRule {
        val existing = this.rrsb.findRuleByName(value, true)
        return if (null == existing) {
            val rr = RuntimeRule(this.rrsb.rules.size, value, RuntimeRuleKind.TERMINAL, false, false)
            this.rrsb.rules.add(rr)
            items.add(rr)
            rr
        } else {
            items.add(existing)
            existing
        }
    }

    fun pattern(pattern: String): RuntimeRule {
        val existing = this.rrsb.findRuleByName(pattern, true)
        return if (null == existing) {
            val rr = RuntimeRule(this.rrsb.rules.size, pattern, RuntimeRuleKind.TERMINAL, true, false)
            this.rrsb.rules.add(rr)
            items.add(rr)
            rr
        } else {
            items.add(existing)
            existing
        }
    }

    fun ref(name: String): RuntimeRule {
        val existing = this.rrsb.rules.firstOrNull { it.name == name }
        return if (null == existing) {
            //add placeholder rule
            val rr = RuntimeRule(this.rrsb.rules.size, name, RuntimeRuleKind.NON_TERMINAL, false, false)
            this.rrsb.rules.add(rr)
            items.add(rr)
            rr
        } else {
            items.add(existing)
            existing
        }
    }

    fun build(): RuntimeRule {
        val rhs = RuntimeRuleItem(this.kind, this.choiceKind, this.min, this.max, this.items.toTypedArray())
        val existing = this.rrsb.rules.firstOrNull { it.name == ruleName }
        return if (null == existing) {
            val rr = RuntimeRule(this.rrsb.rules.size, ruleName, RuntimeRuleKind.NON_TERMINAL, false, isSkip)
            rr.rhsOpt = rhs
            this.rrsb.rules.add(rr)
            rr
        } else {
            existing.rhsOpt = rhs
            existing
        }
    }
}