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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SPPTNodeIdentity
import net.akehurst.language.parser.scannerless.InputFromCharSequence
import net.akehurst.language.parser.sppt.*

internal class ParseGraph(
        val userGoalRule: RuntimeRule,
        private val input: InputFromCharSequence
) {
    private val leaves: MutableMap<LeafIndex, SPPTLeafDefault> = mutableMapOf()
    private val completeNodes: MutableMap<SPPTNodeIdentity, SPPTNode> = mutableMapOf()
    internal val growing: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()
    private val _goals: MutableList<SPPTNode> = mutableListOf()
    val growingHead: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()

    val canGrow: Boolean
        get() {
            return !this.growingHead.isEmpty()
        }

    val goals: List<SPPTNode>
        get() {
            return this._goals
        }

    fun longestMatch(longestLastGrown: SPPTNode?, seasons: Int, maxNumHeads: Int): SPPTNode {
        if (!this.goals.isEmpty() && this.goals.size >= 1) {
            var lt = this.goals.iterator().next()
            for (gt in this.goals) {
                if (gt.matchedTextLength > lt.matchedTextLength) {
                    lt = gt
                }
            }
            if (!this.input.isEnd(lt.nextInputPosition + 1)) {
                //TODO: this can never happen now I think!
                val llg = longestLastGrown ?: throw ParseException("Internal Error, should not happen")
                val location = this.input.calcLineAndColumn(llg.nextInputPosition)
                throw ParseFailedException("Goal does not match full text", SharedPackedParseTreeDefault(llg, seasons, maxNumHeads), location)
            } else {
                val alternatives = mutableListOf<List<SPPTNode>>()
                val firstSkipNodes = mutableListOf<SPPTNode>()
                val userGoalNodes = mutableListOf<SPPTNode>()
                for (node in lt.asBranch.children) {
                    if (node.isSkip) {
                        firstSkipNodes.add(node)
                    } else if (node.asBranch.runtimeRuleNumber == this.userGoalRule.number) {
                        userGoalNodes.add(node)
                        break;
                    }
                }
                val userGoalNode = userGoalNodes.first()

                val r = SPPTBranchDefault(this.userGoalRule, userGoalNode.startPosition, userGoalNode.nextInputPosition, userGoalNode.priority)
                if (userGoalNode is SPPTBranch) {
                    for (alt in userGoalNode.childrenAlternatives) {
                        r.childrenAlternatives.add(firstSkipNodes + alt)
                    }
                } else {
                    r.childrenAlternatives.add(firstSkipNodes + userGoalNode.asBranch.children)
                }
                return r
            }
        } else {
            val llg = longestLastGrown ?: throw ParseException("Nothing parsed")
            val location = this.input.calcLineAndColumn(llg.nextInputPosition)
            throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg, seasons, maxNumHeads), location)
        }
    }

    private fun tryCreateLeaf(terminalRuntimeRule: RuntimeRule, index: LeafIndex): SPPTLeafDefault? {
        // LeafIndex passed as argument because we already created it to try and find the leaf in the cache
        return if (terminalRuntimeRule.isEmptyRule) {
            val leaf = SPPTLeafDefault(terminalRuntimeRule, index.startPosition, true, "", 0)
            this.leaves[index] = leaf
            this.completeNodes[leaf.identity] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
            leaf
        } else {
            val matchedText =
                    this.input.tryMatchText(index.startPosition, terminalRuntimeRule.patternText, terminalRuntimeRule.isPattern)
                            ?: return null
            val leaf = SPPTLeafDefault(terminalRuntimeRule, index.startPosition, false, matchedText, 0)
            this.leaves[index] = leaf
            this.completeNodes[leaf.identity] = leaf //TODO: maybe search leaves in 'findCompleteNode' so leaf is not cached twice
            return leaf
        }
    }

    fun findOrTryCreateLeaf(terminalRuntimeRule: RuntimeRule, position: Int): SPPTLeafDefault? {
        val index = LeafIndex(terminalRuntimeRule.number, position)
        return this.leaves[index] ?: this.tryCreateLeaf(terminalRuntimeRule, index)
    }

    fun createBranchNoChildren(runtimeRule: RuntimeRule, priority: Int, startPosition: Int, nextInputPosition: Int): SPPTBranchDefault {
        val cn = SPPTBranchDefault(runtimeRule, startPosition, nextInputPosition, priority)
        this.completeNodes.put(cn.identity, cn)
        return cn
    }

    fun findCompleteNode(runtimeRule: RuntimeRule, startPosition: Int, matchedTextLength: Int): SPPTNode? {
        if (runtimeRule.isTerminal) {
            return this.leaves[LeafIndex(runtimeRule.number, startPosition)]
        } else {
            val index = SPPTNodeIdentityDefault(runtimeRule.number, startPosition)//, matchedTextLength)
            return this.completeNodes[index]
        }
    }

    private fun addGrowing(gn: GrowingNode) {
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNodeIndex(gn.currentState, startPosition, nextInputPosition)
        val existing = this.growing[gnindex]
        if (null == existing) {
            this.growing[gnindex] = gn
        } else {
            // merge
            for (info in gn.previous.values) {
                existing.addPrevious(info)
            }
        }
    }

    private fun addGrowing(gn: GrowingNode, previous: Set<PreviousInfo>) {
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNodeIndex(gn.currentState, startPosition, nextInputPosition)
        val existing = this.growing[gnindex]
        if (null == existing) {
            for (info in previous) {
                gn.addPrevious(info)
            }
            this.growing[gnindex] = gn
        } else {
            // merge
            for (info in previous) {
                existing.addPrevious(info)
            }
        }
    }

    private fun removeGrowing(gn: GrowingNode) {
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val gnindex = GrowingNodeIndex(gn.currentState, startPosition, nextInputPosition)
        this.growing.remove(gnindex)
    }

    fun addGrowingHead(gnindex: GrowingNodeIndex, gn: GrowingNode): GrowingNode? {
        val existingGrowing = this.growing[gnindex]
        if (null != existingGrowing) {
            // don't add the head, previous should already have been merged
            return null
        } else {
            val existing = this.growingHead.get(gnindex)
            if (null == existing) {
                this.growingHead.put(gnindex, gn)
                return gn
            } else {
                // merge
                for (info in gn.previous.values) {
                    existing.addPrevious(info)
                }
                return existing
            }
        }
    }

    private fun findOrCreateGrowingLeaf(isSkipGrowth: Boolean, curRp: ParserState, runtimeRule: RuntimeRule, startPosition: Int, nextInputPosition: Int,
                                        stack: GrowingNode, previous: Set<PreviousInfo>, lookahead: Set<RuntimeRule>) {
        this.addGrowing(stack, previous)
        // TODO: remove, this is for test
        for (info in previous) {
            this.addGrowing(info.node)
        }
        val gnindex = GrowingNodeIndex(curRp, startPosition, nextInputPosition)
        val existing = this.growing[gnindex]
        if (null == existing) {
            val nn = GrowingNode(isSkipGrowth, curRp, startPosition, nextInputPosition, 0, emptyList(), 0)
            nn.addPrevious(stack)
            this.addGrowingHead(gnindex, nn)
        } else {
            existing.addPrevious(stack)
            this.addGrowingHead(gnindex, existing)
        }
    }

    private fun findOrCreateGrowingLeafForSkip(isSkipGrowth: Boolean, curRp: ParserState, runtimeRule: RuntimeRule, startPosition: Int, nextInputPosition: Int, previous: Set<PreviousInfo>, skipChildren: List<SPPTNode>) {
        // TODO: remove, this is for test
        for (info in previous) {
            this.addGrowing(info.node)
        }
        val gnindex = GrowingNodeIndex(curRp, startPosition, nextInputPosition) //TODO: not sure we need both tgt and cur for leaves
        val existing = this.growing[gnindex]
        if (null == existing) {
            val nn = GrowingNode(isSkipGrowth, curRp, startPosition, nextInputPosition, 0, emptyList(), 0)
            previous.forEach { nn.addPrevious(it) }
            nn.skipNodes.addAll(skipChildren)
            this.addGrowingHead(gnindex, nn)
        } else {
            previous.forEach { existing.addPrevious(it) }
            existing.skipNodes.addAll(skipChildren)
            this.addGrowingHead(gnindex, existing)
        }
    }

    private fun findOrCreateGrowingNode(isSkipGrowth: Boolean, newRp: ParserState, startPosition: Int, nextInputPosition: Int,
                                        priority: Int, children: List<SPPTNode>, numNonSkipChildren: Int, previous: Set<PreviousInfo>): GrowingNode {
        val gnindex = GrowingNodeIndex(newRp, startPosition, nextInputPosition)
        val existing = this.growing.get(gnindex)
        var result: GrowingNode?
        if (null == existing) {
            val nn = GrowingNode(isSkipGrowth, newRp, startPosition, nextInputPosition, priority, children, numNonSkipChildren)
            for (info in previous) {
                nn.addPrevious(info)
                this.addGrowing(info.node)
            }
            this.addGrowingHead(gnindex, nn)
            if (nn.hasCompleteChildren) {
                this.complete(nn)
            }
            result = nn
        } else {
            for (info in previous) {
                existing.addPrevious(info)
                this.addGrowing(info.node)
            }
            this.addGrowingHead(gnindex, existing)
            result = existing
        }
        return result
    }

    fun recordGoal(completeNode: SPPTNode) {
        this._goals.add(completeNode)
    }

    //TODO: need to detect goal, but indicate that there is additional input, not just reject if additional input
    private fun isGoal(completeNode: SPPTNode): Boolean {
        val isStart = this.input.isStart(completeNode.startPosition)
        val isEnd = this.input.isEnd(completeNode.nextInputPosition)
        val isGoalRule = this.userGoalRule.number == completeNode.asBranch.children[0].runtimeRuleNumber
        return isStart && isEnd && isGoalRule
    }

    fun checkForGoal(completeNode: SPPTNode) {
        if (this.isGoal(completeNode)) {
            // TODO: maybe need to not have duplicates!
            this._goals.add(completeNode)
        }
    }

    private fun complete1(gn: GrowingNode): SPPTNode? {
        if (gn.hasCompleteChildren) {
            val runtimeRule = gn.runtimeRule
            val priority = gn.priority
            val startPosition = gn.startPosition
            val matchedTextLength = gn.matchedTextLength
            var cn: SPPTNode? = this.findCompleteNode(runtimeRule, startPosition, matchedTextLength)
            if (null == cn) {
                cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, gn.nextInputPosition)
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    cn.childrenAlternatives.add(gn.children)
                }
            } else {
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    // final ICompleteNode.ChildrenOption opt = new ICompleteNode.ChildrenOption();
                    // opt.matchedLength = gn.getMatchedTextLength();
                    // opt.nodes = gn.getGrowingChildren();
                    cn = (cn as SPPTBranchDefault)

                    val gnLength = gn.matchedTextLength
                    val existingLength = cn.matchedTextLength
                    when {
                        (gnLength > existingLength) -> {
                            //replace existing with this
                            //cn.childrenAlternatives.clear()
                            cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, gn.nextInputPosition)
                            cn.childrenAlternatives.add(gn.children)
                        }
                        (gnLength < existingLength) -> {
                            //keep existing drop this
                        }
                        (gnLength == existingLength) -> {
                            val existingPriority = cn.priority
                            val newPriority = gn.priority
                            when {
                                (newPriority > existingPriority) -> {
                                    // replace existing with new
                                    //cn.childrenAlternatives.clear()
                                    cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, gn.nextInputPosition)
                                    cn.childrenAlternatives.add(gn.children)
                                }
                                (existingPriority > newPriority) -> {
                                    // then existing is the lower precedence item,
                                    // therefore existing node should be the higher item in the tree
                                    // which it is, so change nothing
                                    // do nothing, drop new one
                                    val i = 0
                                }
                                (existingPriority == newPriority) -> {
                                    if (gn.isEmptyMatch && cn.isEmptyMatch) {
                                        if (cn.childrenAlternatives.isEmpty()) {
                                            cn.childrenAlternatives.add(gn.children)
                                        } else {
                                            if (cn.childrenAlternatives.iterator().next().get(0).isEmptyLeaf) {
                                                //TODO: leave it, no need to add empty alternatives, or is there, if they are empty other things ?
                                            } else {
                                                //TODO: check this!
                                                if (gn.children.get(0).isEmptyLeaf) {
                                                    // use just the empty leaf
                                                    cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, gn.nextInputPosition)
                                                    //cn.childrenAlternatives.clear()
                                                    cn.childrenAlternatives.add(gn.children)
                                                } else {
                                                    // add the alternatives
                                                    cn.childrenAlternatives.add(gn.children)
                                                }
                                            }
                                        }
                                    } else {
                                        //TODO: record ambiguity
                                        cn.childrenAlternatives.add(gn.children)

                                    }
                                }
                            }
                        }
                    }
                }
            }

            //this.checkForGoal(cn)
            return cn
        } else {
            return null
        }
    }

    private fun complete(gn: GrowingNode): SPPTNode? {
        if (gn.hasCompleteChildren) {
            val runtimeRule = gn.runtimeRule
            val priority = gn.priority
            val startPosition = gn.startPosition
            val matchedTextLength = gn.matchedTextLength
            var cn: SPPTNode? = this.findCompleteNode(runtimeRule, startPosition, matchedTextLength)
            if (null == cn) {
                cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, gn.nextInputPosition)
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    cn.childrenAlternatives.add(gn.children)
                }
            } else {
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    // final ICompleteNode.ChildrenOption opt = new ICompleteNode.ChildrenOption();
                    // opt.matchedLength = gn.getMatchedTextLength();
                    // opt.nodes = gn.getGrowingChildren();
                    cn = (cn as SPPTBranchDefault)

                    //TODO: when there is ambiguity, sometimes a complete node is replaced after it has been used in the completiona of another node
                    // this give unexpected (wrong!) results
                    when (runtimeRule.rhs.kind) {
                        RuntimeRuleItemKind.CHOICE_EQUAL -> {
                            val choice = pickLongest(gn, cn) ?: pickHigestPriority(gn,cn)
                            if (null==choice) {
                                //ambiguous, keep existing
                            } else {
                                cn = choice
                            }
                        }
                        RuntimeRuleItemKind.CHOICE_PRIORITY -> {
                            val choice = pickHigestPriority(gn, cn) ?: pickLongest(gn,cn)
                            if (null==choice) {
                                //ambiguous, keep existing
                            } else {
                                cn = choice
                            }
                        }
                        else -> {
                            val choice = pickLongest(gn, cn)
                            if (null==choice) {
                                //ambiguous, keep existing
                            } else {
                                cn = choice
                            }
                        }
                    }
                }
            }

            //this.checkForGoal(cn)
            return cn
        } else {
            return null
        }
    }

    // return null if length is the same
    private fun pickLongest(newNode:GrowingNode, exisingNode:SPPTNode) : SPPTNode? {
        val gnLength = newNode.matchedTextLength
        val existingLength = exisingNode.matchedTextLength
        return when {
            (gnLength > existingLength) -> {
                //replace existing with new node
                val longest = this.createBranchNoChildren(newNode.runtimeRule, newNode.priority, newNode.startPosition, newNode.nextInputPosition)
                longest.childrenAlternatives.add(newNode.children)
                longest
            }
            (gnLength < existingLength) -> {
                //keep existing drop this
                exisingNode
            }
            else -> null
        }
    }

    // return null if priority is the same
    private fun pickHigestPriority(newNode:GrowingNode, exisingNode:SPPTNode) : SPPTNode? {
        val newPriority = newNode.priority
        val existingPriority = exisingNode.priority
        return when {
            (newPriority > existingPriority) -> {
                // replace existing with new
                //cn.childrenAlternatives.clear()
                val highest = this.createBranchNoChildren(newNode.runtimeRule, newNode.priority, newNode.startPosition, newNode.nextInputPosition)
                highest.childrenAlternatives.add(newNode.children)
                highest
            }
            (existingPriority > newPriority) -> {
                // then existing is the higher precedence item,
                // therefore existing node should be the higher item in the tree
                // which it is, so change nothing
                // do nothing, drop new one
                exisingNode
            }
            else -> null
        }
    }

    private fun growNextChildAt(isSkipGrowth: Boolean, nextRp: ParserState, parent: GrowingNode, priority: Int, nextChild: SPPTNode, skipChildren: List<SPPTNode>) {
        val startPosition = parent.startPosition
        val nextInputPosition = if (skipChildren.isEmpty()) {
            nextChild.nextInputPosition
        } else {
            skipChildren.last().nextInputPosition
        }
        val children = parent.children + nextChild + skipChildren
        val previous = parent.previous
        for (pi in previous.values) {
            pi.node.removeNext(parent)
        }
        val numNonSkipChildren = if (nextChild.isSkip) {
            parent.numNonSkipChildren + skipChildren.size
        } else {
            parent.numNonSkipChildren + 1 + skipChildren.size
        }
        this.findOrCreateGrowingNode(isSkipGrowth, nextRp, startPosition, nextInputPosition, priority, children, numNonSkipChildren, previous.values.toSet()) //FIXME: don't convert to set
        if (parent.next.isEmpty()) {
            this.removeGrowing(parent)
        }
    }

    //TODO: addPrevious! goalrule growing node, maybe
    fun start(goalState: ParserState) {
        val goalGN = GrowingNode(false, goalState, 0, 0, 0, emptyList<SPPTNode>(), 0)
        this.addGrowingHead(GrowingNodeIndex(goalState, 0, 0), goalGN)
    }

    fun pop(gn: GrowingNode): Set<PreviousInfo> {
        for (pi in gn.previous.values) {
            pi.node.removeNext(gn)
            this.removeGrowing(pi.node)
        }
        val previous = gn.previous
        gn.newPrevious()
        return previous.values.toSet() //FIXME: don't convert to set
    }

    fun pushToStackOf(isSkipGrowth: Boolean, curRp: ParserState, leafNode: SPPTLeafDefault, stack: GrowingNode, previous: Set<PreviousInfo>, lookahead: Set<RuntimeRule>) {
        this.findOrCreateGrowingLeaf(isSkipGrowth, curRp, leafNode.runtimeRule, leafNode.startPosition, leafNode.nextInputPosition, stack, previous, lookahead)
    }

    fun growNextChild(isSkipGrowth: Boolean, nextRp: ParserState, parent: GrowingNode, nextChild: SPPTNode, position: Int, skipChildren: List<SPPTNode>) {
        if (0 != position && parent.runtimeRule.rhs.kind == RuntimeRuleItemKind.MULTI) {
            val prev = parent.children[position - 1]
            if (prev === nextChild) {
                // dont add same child twice to a multi
                return
            }
        }
        val priority = if (0 == position) {
            when (parent.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.CHOICE_PRIORITY -> parent.runtimeRule.rhs.items.indexOfFirst { it.number== nextChild.runtimeRuleNumber}
                RuntimeRuleItemKind.CHOICE_EQUAL -> parent.runtimeRule.rhs.items.indexOfFirst{ it.number== nextChild.runtimeRuleNumber}
                else -> parent.priority
            }
        } else {
            parent.priority
        }
        this.growNextChildAt(isSkipGrowth, nextRp, parent, priority, nextChild, skipChildren)
    }

    fun growNextSkipChild(parent: GrowingNode, skipNode: SPPTNode) {
        if (parent.runtimeRule.isNonTerminal || parent.runtimeRule.isGoal) {
            this.growNextChildAt(
                    false,
                    parent.currentState,
                    parent,
                    parent.priority,
                    skipNode,
                    emptyList()
            )
        } else {
            val nextRp = parent.currentState
            val nextInputPosition = parent.nextInputPosition + skipNode.matchedTextLength
            val newLeaf = this.findOrCreateGrowingLeafForSkip(
                    false,
                    nextRp,
                    parent.runtimeRule,
                    parent.startPosition,
                    nextInputPosition,
                    parent.previous.values.toSet(),  //FIXME: don't convert to set
                    parent.skipNodes + skipNode
            )
            if (parent.next.isEmpty()) {
                this.removeGrowing(parent)
            }
        }
    }

    fun createWithFirstChild(isSkipGrowth: Boolean, newRp: ParserState, firstChild: SPPTNode, previous: Set<PreviousInfo>, skipChildren: List<SPPTNode>) {
        val startPosition = firstChild.startPosition
        val nextInputPosition = if (skipChildren.isEmpty()) {
            firstChild.nextInputPosition
        } else {
            skipChildren.last().nextInputPosition
        }
        val runtimeRule = newRp.runtimeRule
        val children = listOf(firstChild) + skipChildren
        val numNonSkipChildren = skipChildren.size
        val priority = when (runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.CHOICE_PRIORITY -> runtimeRule.rhs.items.indexOfFirst{ it.number== firstChild.runtimeRuleNumber}
                RuntimeRuleItemKind.CHOICE_EQUAL -> runtimeRule.rhs.items.indexOfFirst{ it.number== firstChild.runtimeRuleNumber}
                else -> 0
            }
        this.findOrCreateGrowingNode(isSkipGrowth, newRp, startPosition, nextInputPosition, priority, children, numNonSkipChildren, previous)
    }
}