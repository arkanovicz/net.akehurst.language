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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.ogl.runtime.graph.GrowingNode
import net.akehurst.language.ogl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.ogl.runtime.graph.ParseGraph
import net.akehurst.language.ogl.runtime.graph.PreviousInfo
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.parser.sppt.SPPTBranchDefault
import net.akehurst.language.parser.sppt.SPPTLeafDefault
import net.akehurst.language.parser.sppt.SPPTNodeDefault
import net.akehurst.language.parser.sppt.SharedPackedParseTreeDefault


internal class RuntimeParser(
        private val runtimeRuleSet: RuntimeRuleSet,
        private val goalRule: RuntimeRule,
        private val input: InputFromCharSequence
) {

    private val graph = ParseGraph(this.goalRule, this.input)
    // copy of graph growing head for each iteration, cached to that we can find best match in case of error
    private var toGrow: List<GrowingNode> = listOf()

    private val lastGrown: Collection<GrowingNode>
        get() {
            return if (this.graph.growing.isEmpty())
                this.toGrow
            else
                this.graph.growing.values
        }

    private val longestLastGrown: SPPTNode? get() {
        val llg = this.lastGrown.maxWith(Comparator<GrowingNode>{ a,b -> a.nextInputPosition.compareTo(b.nextInputPosition) })
        return if (null==llg) {
            return null
        } else if (llg.isLeaf) {
            this.graph.findOrTryCreateLeaf(llg.runtimeRule, llg.startPosition)
        } else {
            SPPTBranchDefault(llg.runtimeRule, llg.startPosition, llg.nextInputPosition,llg.children,llg.priority)
        }
    }

    val canGrow: Boolean
        get() {
            return this.graph.canGrow
        }

    val longestMatch: SPPTNode
        get() {
            if (!this.graph.goals.isEmpty() && this.graph.goals.size >= 1) {
                var lt = this.graph.goals.iterator().next()
                for (gt in this.graph.goals) {
                    if (gt.matchedTextLength > lt.matchedTextLength) {
                        lt = gt
                    }
                }
                if (!this.input.isEnd(lt.nextInputPosition + 1)) {
                    val llg = longestLastGrown ?: throw ParseException("Internal Error, should not happen")
                    val location = this.input.calcLineAndColumn(llg.nextInputPosition)
                    throw ParseFailedException("Goal does not match full text", SharedPackedParseTreeDefault(llg), location)
                } else {
                    return lt
                }
            } else {
                val llg = longestLastGrown ?: throw ParseException("Nothing parsed")
                val location = this.input.calcLineAndColumn(llg.nextInputPosition)
                throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg), location)
            }
        }



    fun start(goalRule: RuntimeRule) {
        val gnindex = GrowingNodeIndex(goalRule.number, 0, 0, 0)
        val gn = GrowingNode(goalRule, 0, 0, 0, 0, emptyList<SPPTNodeDefault>(), 0)
        this.graph.addGrowingHead(gnindex, gn)
        if (gn.hasCompleteChildren) {
            val cn = this.graph.createBranchNoChildren(goalRule, 0, 0, 0)
            if (gn.isLeaf) {
                // dont try and add children...can't for a leaf
            } else {
                cn.childrenAlternatives.add(gn.children)
            }
        }
    }

    fun grow() {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            this.growNode(gn)
        }
    }

    private fun growNode(gn: GrowingNode) {
        val previous = this.graph.pop(gn)

        val didSkipNode = this.tryGrowWidthWithSkipRules(gn, previous)
        if (didSkipNode) {
            return
        } else {
            if (gn.isSkip) {
                this.tryGraftBackSkipNode(gn, previous)
            } else {
                // TODO: need to find a way to do either height or graft..not both, maybe!
                // problem is deciding which
                // try reduce first
                val grownHeight = this.tryGrowHeight(gn, previous)

                // try reduce rest
                var graftBack = this.tryGraftBack(gn, previous)

                // maybe only shift if not done either of above!
                // tomitas original does that!
                // shift
                val grownWidth = this.tryGrowWidth(gn, previous)
            }
        }
    }

    private fun tryGrowWidthWithSkipRules(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        if (gn.canGrowWidthWithSkip) { // don't grow width if its complete...cant graft back
            val expectedNextTerminal: Set<RuntimeRule> = this.runtimeRuleSet.firstSkipRuleTerminals[gn.runtimeRule.number]
            for (rr in expectedNextTerminal) {
                val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                if (null != l) {
                    modified = this.pushStackNewRoot(l, gn, previous)
                }
            }
        }
        return modified
    }

    private fun tryGraftBackSkipNode(gn: GrowingNode, previous: Set<PreviousInfo>) {

    }

    private fun tryGrowHeight(gn: GrowingNode, previous: Set<PreviousInfo>) : Boolean {
        var result = false
        if (gn.hasCompleteChildren) {
            val childRule = gn.runtimeRule
            if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
                val infos = this.runtimeRuleSet.superRuleInfo(childRule)
                for (info in infos) {
                    val complete = this.graph.getCompleteNode(gn)
                    this.growHeightByType(complete, info, previous)
                    result = result or true // TODO: this should depend on if the growHeight does something
                }
            } else {
                if (previous.isEmpty()) {
                    // do nothing
                } else {
                    val toGrow = HashSet<SuperRuleInfo>()
                    for ((node) in previous) {
                        val prevItemIndex = node.nextItemIndex
                        val prevRule = node.runtimeRule
                        toGrow.addAll(this.runtimeRuleSet.growsInto(childRule, prevRule, prevItemIndex))
                    }
                    for (info in toGrow) {
                        val complete = this.graph.getCompleteNode(gn)
                        this.growHeightByType(complete, info, previous)
                        result = result or true // TODO: this should depend on if the growHeight does something
                    }
                }
            }
        } else {
            // do nothing
        }
        return result
    }

    private fun tryGraftBack(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var graftBack = false
        for (prev in previous) {
            if (gn.canGraftBack(prev)) { // if hascompleteChildren && isStacked && prevInfo is valid
                graftBack = this.tryGraftBack(gn, prev)
            }
        }
        return graftBack
    }

    protected fun tryGraftBack(gn: GrowingNode, info: PreviousInfo): Boolean {
        var result = false
        // TODO: perhaps should return list of those who are not grafted!
        // for (final IGrowingNode.PreviousInfo info : previous) {
        if (info.node.hasNextExpectedItem) {
            result = result or this.tryGraftInto(gn, info)
        } else {
            // can't push back
            result = result or false
        }
        // }
        return result
    }

    private fun tryGraftInto(gn: GrowingNode, info: PreviousInfo): Boolean {
        var result = false
        if (gn.isSkip) {
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.nextItemIndex)
                    ?: throw ParseException("internal error, should never happen")
            this.graph.growNextSkipChild(info.node, complete)
            // info.node.duplicateWithNextSkipChild(gn);
            // this.graftInto(gn, info);
            result = result or true
        } else if (info.node.expectsItemAt(gn.runtimeRule, info.atPosition)) {
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.nextItemIndex)
                    ?: throw ParseException("internal error, should never happen")
            this.graph.growNextChild(info.node, complete, info.atPosition)
            result = result or true
        } else {
            // drop
            result = result or false
        }
        return result
    }

    private fun tryGrowWidth(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        if (gn.canGrowWidth) { // don't grow width if its complete...cant graft back
            val expectedNextTerminal = runtimeRuleSet.findNextExpectedTerminals(gn.runtimeRule, gn.nextItemIndex, gn.numNonSkipChildren)
            for (rr in expectedNextTerminal) {
                val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                if (null != l) {
                    modified = this.pushStackNewRoot(l, gn, previous)
                }
            }
        }
        return modified
    }

    fun pushStackNewRoot(leafNode: SPPTLeafDefault, stack: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        // no existing parent was suitable, use newRoot
        if (this.hasStackedPotential(leafNode, stack)) {
            this.graph.pushToStackOf(leafNode, stack, previous)
            modified = true
        }
        return modified
    }

    private fun hasStackedPotential(completeNode: SPPTNodeDefault, stack: GrowingNode): Boolean {
        if (completeNode.isSkip) {
            return true
        } else {
            // if node is nextexpected item on stack, or could grow into nextexpected item
            if (stack.hasNextExpectedItem) {
                for (expectedRule in stack.nextExpectedItems) {
                    if (completeNode.runtimeRule.number == expectedRule.number) {
                        // node is nextexpected item on stack
                        return true
                    } else {
                        // node is a possible subnode of nextexpected item
                        if (completeNode.runtimeRule.isNonTerminal) {
                            //TODO: would it help to use 'at' here? rather than all subrules
                            val possibles = this.runtimeRuleSet.subNonTerminals[expectedRule.number]
                            val res = possibles.contains(completeNode.runtimeRule)
                            if (res) {
                                return true
                            }
                        } else {
                            val possibles = this.runtimeRuleSet.subTerminals[expectedRule.number]
                            val res = possibles.contains(completeNode.runtimeRule)
                            if (res) {
                                return true
                            }

                        }
                    }
                }
                return false
            } else return if (this.runtimeRuleSet.allSkipTerminals.contains(completeNode.runtimeRule)) {
                true
            } else {
                false
            }
        }
    }

}