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

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind

class RuntimeRuleExtender(val rrsb: RuntimeRuleSetBuilder, val rule: RuntimeRule) {

    fun choiceEqual(vararg items: RuntimeRule) {
        rule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL, -1, 0, items)
    }

    fun choicePriority(vararg items: RuntimeRule) {
        rule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_PRIORITY, -1, 0, items)
    }

    fun concatenation(vararg items: RuntimeRule) {
        rule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, items)
    }

    fun multi(min: Int, max: Int, item: RuntimeRule) {
        val items = if (0==min) {
            val e = RuntimeRuleTerminalBuilder(this.rrsb).empty(this.rule)
            arrayOf(item, e)
        } else {
            arrayOf(item)
        }
        rule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.MULTI, min, max, items)
    }

    fun sList(min: Int, max: Int, separator: RuntimeRule, item: RuntimeRule) {
        val items = if (0==min) {
            val e = RuntimeRuleTerminalBuilder(this.rrsb).empty(this.rule)
            arrayOf(item, separator, e)
        } else {
            arrayOf(item,separator)
        }
        rule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.SEPARATED_LIST, min, max, items)
    }
}