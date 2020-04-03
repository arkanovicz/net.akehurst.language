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

package net.akehurst.language.agl.parser

import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.sppt.SPPTLeafDefault
import net.akehurst.language.agl.sppt.SharedPackedParseTreeDefault
import kotlin.math.max

class ScanOnDemandParser(
        private val runtimeRuleSet: RuntimeRuleSet
) : Parser {

    private var runtimeParser: RuntimeParser? = null

    override fun interrupt(message: String) {
        this.runtimeParser?.interrupt(message)
    }

    override fun build() {
        this.runtimeRuleSet.buildCaches()
    }

    override fun scan(inputText: CharSequence, includeSkipRules: Boolean): List<SPPTLeaf> {
        val undefined = RuntimeRule(-5, "undefined", "", RuntimeRuleKind.TERMINAL, false, true)
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!
        val input = InputFromCharSequence(inputText)
        var terminals = if (includeSkipRules) this.runtimeRuleSet.terminalRules else this.runtimeRuleSet.nonSkipTerminals
        var result = mutableListOf<SPPTLeaf>()

        //eliminate tokens that are empty matches
        terminals = terminals.filter {
            it.value.isNotEmpty()
        }.toTypedArray()

        var position = 0
        var lastLocation = InputLocation(0, 1, 1, 0)
        while (!input.isEnd(position)) {
            val matches: List<SPPTLeaf> = terminals.mapNotNull {
                val match = input.tryMatchText(position, it.value, it.isPattern)
                if (null == match) {
                    null
                } else {
                    val location = input.nextLocation(lastLocation, match.matchedText.length)
                    val leaf = SPPTLeafDefault(it, location, false, match.matchedText, (if (it.isPattern) 0 else 1))
                    leaf.eolPositions = match.eolPositions
                    leaf
                }
            }
            // prefer literals over patterns
            val longest = matches.maxWith(Comparator<SPPTLeaf> { l1, l2 ->
                when {
                    l1.isLiteral && l2.isPattern -> 1
                    l1.isPattern && l2.isLiteral -> -1
                    else -> when {
                        l1.matchedTextLength > l2.matchedTextLength -> 1
                        l2.matchedTextLength > l1.matchedTextLength -> -1
                        else -> 0
                    }
                }
            })
            when {
                (null == longest || longest.matchedTextLength == 0) -> {
                    //TODO: collate unscanned, rather than make a separate token for each char
                    val text = inputText[position].toString()
                    lastLocation = input.nextLocation(lastLocation, text.length)
                    val unscanned = SPPTLeafDefault(undefined, lastLocation, false, text, 0)
                    unscanned.eolPositions = input.eolPositions(text)
                    result.add(unscanned)
                    position++
                }
                else -> {
                    result.add(longest)
                    position = longest.nextInputPosition
                    lastLocation = longest.location
                }
            }
        }
        return result
    }

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val input = InputFromCharSequence(inputText)
        val graph = ParseGraph(goalRule, input)
        val rp = RuntimeParser(this.runtimeRuleSet, graph)
        this.runtimeParser = rp

        rp.start(goalRule)
        var seasons = 1
        var maxNumHeads = graph.growingHead.size
        //       println("[$seasons] ")
        //       graph.growingHead.forEach {
        //           println("  $it")
        //       }

        do {
            rp.grow(false)
//            println("[$seasons] ")
//            graph.growingHead.forEach {
            //               println("  $it")
            //          }
            seasons++
            maxNumHeads = max(maxNumHeads, graph.growingHead.size)
//        } while (rp.canGrow)
        } while (rp.canGrow && graph.goals.isEmpty())
        //TODO: when parsing an ambiguous grammar,
        // how to know we have found all goals? - keep going until cangrow is false
        // but - stop .. some grammars don't stop if we don't do test for a goal!
        // e.g. leftRecursive.test_aa

        val match = graph.longestMatch(seasons, maxNumHeads)
        return if (match != null) {
            SharedPackedParseTreeDefault(match, seasons, maxNumHeads)
        } else {
            val lg = rp.lastGrown.toList()
            val nextExpected = this.findNextExpectedAfterError(rp, graph, input, lg) //this possibly modifies rp and hence may change the longestLastGrown
            throwError(graph, rp, nextExpected, seasons, maxNumHeads)
        }
    }

    private fun throwError(graph: ParseGraph, rp: RuntimeParser, nextExpected: Pair<GrowingNode, List<RuntimeRule>>, seasons: Int, maxNumHeads: Int): SharedPackedParseTreeDefault {
        val llg = rp.longestLastGrown
                ?: throw ParseFailedException("Nothing parsed", null, InputLocation(0, 0, 1, 0), emptySet())

        val gn = nextExpected.first
        val exp = nextExpected.second
        val expected = exp
                .filter { it.number >= 0 && it.isEmptyRule.not() }
                .map { this.runtimeRuleSet.firstTerminals[it.number] }
                .flatMap { it.map { it.value } }
                .toSet()
        val errorPos = gn.lastLocation.position + gn.lastLocation.length
        val lastEolPos = llg.matchedText.lastIndexOf('\n')
        val errorLine = llg.location.line + llg.numberOfLines
        val errorColumn = when {
            llg.lastLocation.position == 0 && llg.lastLocation.length == 0 -> errorPos + 1
            -1 == lastEolPos -> llg.lastLocation.column + llg.lastLocation.length
            else -> llg.matchedTextLength - lastEolPos
        }
        val errorLength = 1
        val location = InputLocation(errorPos, errorColumn, errorLine, errorLength)
        throw ParseFailedException("Could not match goal", SharedPackedParseTreeDefault(llg, seasons, maxNumHeads), location, expected)

    }

    private fun findNextExpectedAfterError(rp: RuntimeParser, graph: ParseGraph, input: InputFromCharSequence, gns: List<GrowingNode>): Pair<GrowingNode, List<RuntimeRule>> {
        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf

        val matches = gns.toMutableList()
        // try grow last leaf with no lookahead
        for (gn in rp.lastGrownLinked) {
            val gnindex = GrowingNodeIndex(gn.currentState, gn.startPosition, gn.nextInputPosition, gn.priority)
            graph.growingHead[gnindex] = gn
        }
        rp.tryGrowWidthOnceThenHightOrGraftUntilFail()
        val lg = rp.lastGrown.maxWith(Comparator<GrowingNode> { a, b -> a.nextInputPosition.compareTo(b.nextInputPosition) })
        return if (null == lg) {
            TODO()
        } else {
            // compute next expected item/RuntimeRule
            TODO("how to get transitions, because lg has no previous!")
            val exp = lg.previous.values.flatMap { prev ->
                lg.currentState.transitions(this.runtimeRuleSet, prev.node.currentState)
            }.flatMap {
                it.to.rulePosition.items
            }
            Pair(lg, exp)
        }
    }

    private fun findNextExpected(rp: RuntimeParser, graph: ParseGraph, input: InputFromCharSequence, gns: List<GrowingNode>): List<RuntimeRule> {
        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf

        val matches = gns.toMutableList()
        // try grow last leaf with no lookahead
        for (gn in rp.lastGrownLinked) {
            val gnindex = GrowingNodeIndex(gn.currentState, gn.startPosition, gn.nextInputPosition, gn.priority)
            graph.growingHead[gnindex] = gn
        }
        do {
            rp.grow(true)
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
        } while (rp.canGrow && graph.goals.isEmpty())

        val expected = matches.filter { it.canGrowWidth }.flatMap { it.nextExpectedItems }
        val nextExpected = mutableListOf<RuntimeRule>()
        for (rr in expected) {
            nextExpected.add(rr)
        }
        return nextExpected
    }

    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuntimeRule> {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val usedText = inputText.subSequence(0, position)
        val input = InputFromCharSequence(usedText)
        val graph = ParseGraph(goalRule, input)
        val rp = RuntimeParser(this.runtimeRuleSet, graph)
        this.runtimeParser = rp

        rp.start(goalRule)
        var seasons = 1

        // final int length = text.length();
        val matches = mutableListOf<GrowingNode>()

        do {
            rp.grow(false)
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
            seasons++
        } while (rp.canGrow)
        val nextExpected = this.findNextExpected(rp, graph, input, matches)
        return nextExpected
        /*
        // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf
        // try grow last leaf with no lookahead
        for (gn in rp.lastGrownLinked) {
            val gnindex = GrowingNodeIndex(gn.currentState, gn.startPosition, gn.nextInputPosition, gn.priority)
            graph.growingHead[gnindex] = gn
        }
        do {
            rp.grow(true)
            for (gn in rp.lastGrown) {
                // may need to change this to finalInputPos!
                if (input.isEnd(gn.nextInputPosition)) {
                    matches.add(gn)
                }
            }
            seasons++
        } while (rp.canGrow)

        val expected = matches.flatMap { it.nextExpectedItems }
        val nextExpected = mutableListOf<RuntimeRule>()
        for (rr in expected) {
            nextExpected.add(rr)
        }
        return nextExpected
         */
    }

    override fun expectedTerminalsAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuntimeRule> {
        return this.expectedAt(goalRuleName, inputText, position).flatMap { it.itemsAt[0].toList() }
    }
}