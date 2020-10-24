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

import net.akehurst.language.collections.Stack
import net.akehurst.language.collections.lazyMapNonNull

class ParserStateSet(
        val number: Int,
        val runtimeRuleSet: RuntimeRuleSet,
        val userGoalRule: RuntimeRule,
        val possibleEndOfText: Set<RuntimeRule>,
        val isSkip: Boolean
) {

    private var nextState = 0
    private var nextParentRelation = 0
    var preBuilt = false; private set

    val usedRules: Set<RuntimeRule> by lazy {
        calcUsedRules(this.startState.runtimeRule)
    }
    val usedTerminalRules: Set<RuntimeRule> by lazy {
        this.usedRules.filter { it.kind === RuntimeRuleKind.TERMINAL }.toSet()
    }
    val usedNonTerminalRules: Set<RuntimeRule> by lazy {
        this.usedRules.filter { it.kind !== RuntimeRuleKind.TERMINAL }.toSet()
    }

    /*
     * A RulePosition identifies a Parser state.
     * LR(0) states
     * The parentRelation can be used to determine the LR(1) related lookahead
     */
    val states = lazyMapNonNull<RulePosition, ParserState>() {
        ParserState(StateNumber(this.nextState++), it, this)
    }

    val allBuiltTransitions: Set<Transition> get() = states.values.flatMap { it.allBuiltTransitions }.toSet()

    val startState: ParserState by lazy {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule, this.possibleEndOfText)
        val goalRP = RulePosition(goalRule, 0, 0)
        val startState = this.states[goalRP]
        startState
    }


    private val userGoalParentRelation: ParentRelation by lazy {
        ParentRelation(this, this.nextParentRelation++, this.startState.rulePosition, possibleEndOfText.toSet())
    }

    // runtimeRule -> set of rulePositions where the rule is used
    internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
        //TODO: possibly faster to pre cache this! and goal rules currently not included!
        when {
            (childRR === this.runtimeRuleSet.END_OF_TEXT) -> { //TODO: should this check for contains in possibleEndOfText, and what if something in endOfText is also valid mid text!
                setOf(RulePosition(this.startState.runtimeRule, 0, 1))
            }
            childRR.isSkip -> when {
                //this must be the skipParserStateSet, could test this.isSkip to be sure!
                childRR.number == RuntimeRuleSet.SKIP_RULE_NUMBER -> {
                    setOf(this.startState.rulePosition)
                }
                //childRR.number == RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER -> {
                //    val option = this.runtimeRuleSet.skipRules.indexOf(childRR)
                //    setOf(RulePosition(this.userGoalRule, 0, 0), RulePosition(this.userGoalRule, 0, RulePosition.MULIT_ITEM_POSITION))
                //}
                else -> {
                    val option = this.runtimeRuleSet.skipRules.indexOf(childRR)
                    setOf(RulePosition(this.userGoalRule, option, 0))
                }
            }
            else -> {
                val s = this.runtimeRuleSet.parentPosition[childRR].filter { this.usedNonTerminalRules.contains(it.runtimeRule) }.toSet()
                if (childRR == this.userGoalRule) {
                    s + this.startState.rulePosition
                } else {
                    s
                }
            }
        }
    }

    internal val firstTerminals = lazyMapNonNull<RulePosition, Set<RuntimeRule>> { rp ->
        when (rp.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> setOf(rp.runtimeRule)
            RuntimeRuleKind.EMBEDDED -> {
                TODO()
            }
            RuntimeRuleKind.GOAL -> when {
                rp.isAtStart -> when {
                    this.isSkip -> {
                        this.runtimeRuleSet.skipRules.flatMap {
                            this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                        }.toSet()
                    }
                    else -> this.runtimeRuleSet.firstTerminals[this.userGoalRule.number].filter { this.usedTerminalRules.contains(it) }.toSet()
                }
                rp.isAtEnd -> emptySet()
                else -> emptySet()//this.possibleEndOfText
            }
            RuntimeRuleKind.NON_TERMINAL -> {
                rp.items.flatMap {
                    when {
                        it.number >= 0 -> this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                        else -> when (it.number) {
                            RuntimeRuleSet.GOAL_RULE_NUMBER -> TODO()
                            RuntimeRuleSet.SKIP_RULE_NUMBER -> this.runtimeRuleSet.skipRules.flatMap {
                                this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                            }.toSet()
                            // RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER -> it.rhs.items.flatMap {
                            //     this.runtimeRuleSet.firstTerminals[it.number]
                            // }.toSet()
                            else -> error("should not happen")
                        }
                    }
                }.toSet()
            }
        }
    }

    internal val _lookahead = mutableMapOf<RulePosition, Set<RuntimeRule>>()
    fun fetchOrCreateLookahead(rp: RulePosition): Set<RuntimeRule> {
        var set = this._lookahead[rp]
        if (null == set) {
            set = this.calcLookahead2(rp) //this.calcLookahead1(rp, BooleanArray(this.runtimeRuleSet.runtimeRules.size))
            this._lookahead[rp] = set
        }
        return set
    }

    internal val _next = mutableMapOf<RulePosition, Set<RuntimeRule>>()
    fun fetchOrCreateFirstAt(rp: RulePosition): Set<RuntimeRule> {
        var set = this._next[rp]
        if (null == set) {
            set = this.calcFirstAt(rp, BooleanArray(this.runtimeRuleSet.runtimeRules.size)) //TODO: use usedRules.size!
            this._next[rp] = set
        }
        return set
    }

    internal val _parentRelations = mutableMapOf<RuntimeRule, Set<ParentRelation>>()
    internal fun parentRelation(runtimeRule: RuntimeRule): Set<ParentRelation> {
        var set = _parentRelations[runtimeRule]
        return if (null == set) {
            val t = if (runtimeRule == this.userGoalRule) {
                setOf(this.userGoalParentRelation)
            } else {
                emptySet()
            }
            set = t + this.calcParentRelation(runtimeRule)
            _parentRelations[runtimeRule] = set
            set
        } else {
            set
        }
    }


    fun build(): ParserStateSet {
        val s0 = this.startState
        //val sG = this.states[s0.rulePosition.atEnd()]
        //val trans = s0.transitions(null) //+ sG.transitions(null)
        val done = mutableSetOf<Pair<RuntimeRule, ParserState?>>()//Pair(s0, null))
        val prevStack = Stack<ParserState>()//.push()
        buildAndTraverse(s0, prevStack, done)
        preBuilt = true
        return s0.stateSet
    }

    private fun buildAndTraverse(curState: ParserState, prevStack: Stack<ParserState>, done: MutableSet<Pair<RuntimeRule, ParserState?>>) {
        //TODO: using done here does not work, as the pair (state,prev) does not identify the path to each state, could have alternative 'prev-->prev...'
        // prob need to take the closure of each state first or something, like traditional LR SM build
        // see grammar in test_Java8_Singles.Expressions_Type__int
        //for(prev in prevStack.elements) {
        val prev = prevStack.peekOrNull()
        val dp = Pair(curState.runtimeRule, prev)
        if (done.contains(dp)) {
            //do nothing more
        } else {
            for (rp in curState.runtimeRule.rulePositions) {
                val state = this.fetch(rp)
                done.add(dp)
                val trans = state.transitions(prev)
                for (nt in trans) {
                    val nextState = nt.to
                    when (nt.action) {
                        Transition.ParseAction.WIDTH -> buildAndTraverse(nextState, prevStack.push(state), done)
                        Transition.ParseAction.EMBED -> buildAndTraverse(nextState, prevStack.push(state), done)
                        Transition.ParseAction.HEIGHT -> buildAndTraverse(nextState, prevStack, done)
                        Transition.ParseAction.GRAFT -> buildAndTraverse(nextState, prevStack.pop().stack, done)
                        Transition.ParseAction.GOAL -> null;//buildAndTraverse(nextState, prevStack, done)
                    }
                }
            }
        }
        // }
    }

    internal fun calcParentRelation(childRR: RuntimeRule): Set<ParentRelation> {
        val x = this.parentPosition[childRR].map { rp ->
            if (rp == this.startState.rulePosition) {
                this.userGoalParentRelation
            } else {
                val lh = fetchOrCreateLookahead(rp)
                ParentRelation(this, this.nextParentRelation++, rp, lh)
            }
        }.toSet()
        return x
    }

    /*
    internal fun fetchOrCreateParseState(rulePosition: RulePosition): ParserState { //
        val existing = this.states[rulePosition]
        return if (null == existing) {
            val v = ParserState(StateNumber(this.nextState++), rulePosition, this)
            this.states[rulePosition] = v
            v
        } else {
            existing
        }
    }
*/

    internal fun fetch(rulePosition: RulePosition): ParserState {
        return this.states[rulePosition]
    }

    internal fun fetchOrNull(rulePosition: RulePosition): ParserState? {
        return this.states[rulePosition]
    }

    fun calcLookahead2(rp: RulePosition): Set<RuntimeRule> {
        //TODO: still getting empty terminals in LH set!!
        return when {
            rp.runtimeRule.number < 0 -> { //must be GOAL or SKIP-GOAL
                if (rp.isAtStart) {
                    this.possibleEndOfText.toSet()
                } else {
                    emptySet()
                }
            }
            else -> when {
                rp.isAtEnd -> {
                    val pps = this.parentPosition[rp.runtimeRule]
                    pps.flatMap { parentRp ->
                        val nextRPs = parentRp.next()
                        nextRPs.flatMap { this.fetchOrCreateFirstAt(it) }
                    }.toSet()
                }
                else -> {
                    val nextRPs = rp.next()
                    nextRPs.flatMap {
                        this.fetchOrCreateFirstAt(it)
                    }.toSet()
                }
            }
        }
    }

    fun calcFirstAt(rp: RulePosition, done: BooleanArray): Set<RuntimeRule> {
        //TODO("try and split this so we do different things depending on the 'rule type/position' multi/slist/mid/begining/etc")
        return when {
            rp.runtimeRule.number < 0 -> { //must be GOAL or SKIP-GOAL
                when {
                    rp.isAtStart -> this.runtimeRuleSet.firstTerminals[this.userGoalRule.number].filter { this.usedTerminalRules.contains(it) }.toSet()
                    rp.isAtEnd -> this.possibleEndOfText.toSet()//emptySet()
                    else -> this.possibleEndOfText.toSet()
                }
            }
            rp.runtimeRule.number >= 0 && done[rp.runtimeRule.number] -> {
                when {
                    rp.isAtEnd -> emptySet()
                    else -> {
                        val ns = rp.items.flatMap {
                            when {
                                it.isEmptyRule -> {
                                    rp.next().flatMap { calcFirstAt(it, done) }
                                }
                                else -> when (it.kind) {
                                    RuntimeRuleKind.TERMINAL -> setOf(it)
                                    RuntimeRuleKind.NON_TERMINAL -> {
                                        this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                                    }
                                    RuntimeRuleKind.EMBEDDED -> it.embeddedRuntimeRuleSet!!.firstTerminals[it.embeddedStartRule!!.number] //TODO: filter this by used things
                                    else -> TODO()
                                }
                            }
                        }.toSet()
                        ns
                    }
                }
            }
            rp.isAtEnd -> { //use first of parent next
                val newDone = done //.copyOf() //TODO: do we need to copy?
                if (rp.runtimeRule.number > 0) {
                    newDone[rp.runtimeRule.number] = true
                }
                val pps = this.parentPosition[rp.runtimeRule]
                pps.flatMap { parentRp ->
                    val ns = parentRp.next().flatMap { nprp ->
                        this.calcFirstAt(nprp, newDone)
                    }
                    ns
                }.toSet()
            }
            else -> { // use first of next
                val ns = rp.items.flatMap {
                    when {
                        it.isEmptyRule -> {
                            rp.next().flatMap { calcFirstAt(it, done) }
                        }
                        else -> when (it.kind) {
                            RuntimeRuleKind.TERMINAL -> setOf(it)
                            RuntimeRuleKind.NON_TERMINAL -> {
                                val fsts = it.rulePositionsAt[0]
                                fsts.flatMap {
                                    val newDone = done //.copyOf() //TODO: do we need to copy?
                                    newDone[it.runtimeRule.number] = true
                                    if (it.items.size == 1 && it.items.first().isEmptyRule) {
                                        rp.next().flatMap { calcFirstAt(it, done) }
                                    } else {
                                        calcFirstAt(it, done)
                                    }
                                }
                            }
                            RuntimeRuleKind.EMBEDDED -> it.embeddedRuntimeRuleSet!!.firstTerminals[it.embeddedStartRule!!.number] //TODO: filter this by used things
                            else -> TODO()
                        }
                    }
                }.toSet()
                ns
            }
        }
    }

    private val _growsInto = mutableMapOf<Pair<RulePosition, RulePosition>, Boolean>()
    fun growsInto(ancestor: RulePosition, descendant: RulePosition): Boolean {
        //TODO: can we do this faster somehow? the closure is potentially slow!
        val p = Pair(ancestor, descendant)
        var r = _growsInto[p]
        if (null == r) {
            val thisStart = descendant.runtimeRule.rulePositionsAt[0]
            r = calcClosureLR0(ancestor).any {
                thisStart.contains(it)
            }
            _growsInto[p] = r
        }
        return r
    }

    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private fun calcClosureLR0(rp: RulePosition): Set<RulePosition> {
        var cl = _calcClosureLR0[rp]
        if (null == cl) {
            cl = calcClosureLR0(rp, mutableSetOf())
            _calcClosureLR0[rp] = cl
        }
        return cl
    }

    private fun calcClosureLR0(rp: RulePosition, items: MutableSet<RulePosition>): Set<RulePosition> {
        return when {
            items.contains(rp) -> {
                items
            }
            else -> {
                items.add(rp)
                val itemRps = rp.items.flatMap {
                    it.rulePositionsAt[0]
                }.toSet()
                itemRps.forEach { childRp ->
                    calcClosureLR0(childRp, items)
                }
                items
            }
        }
    }

    private fun calcUsedRules(rule: RuntimeRule, used: MutableSet<RuntimeRule> = mutableSetOf(), done: BooleanArray = BooleanArray(this.runtimeRuleSet.runtimeRules.size)): Set<RuntimeRule> {
        return when {
            0 > rule.number -> {
                used.add(rule)
                for (sr in rule.rhs.items) {
                    calcUsedRules(sr, used, done)
                }
                used
            }
            done[rule.number] -> used
            else -> when {
                rule.kind === RuntimeRuleKind.NON_TERMINAL -> {
                    used.add(rule)
                    done[rule.number] = true
                    for (sr in rule.rhs.items) {
                        calcUsedRules(sr, used, done)
                    }
                    used
                }
                else -> {
                    used.add(rule)
                    done[rule.number] = true
                    used
                }
            }
        }
    }

    override fun hashCode(): Int = this.number
    override fun equals(other: Any?): Boolean = when (other) {
        is ParserStateSet -> this.number == other.number
        else -> false
    }

    override fun toString(): String = "ParserStateSet{$number}"
}