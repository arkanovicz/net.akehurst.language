/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.regex

fun regexMatcher(pattern:String) = RegexParser(pattern).parse()

class RegexMatcher(
        val start:State,
        val nfa: List<State>
) {

    data class MatchResult(
            val matchedText: String,
            val countOfNewLines: Int
    )

    companion object {
        val MATCH_STATE = State(-1, false)
    }

    fun match(text: CharSequence, startPosition: Int = 0): MatchResult? {
        var pos = startPosition
        var currentStates = mutableListOf<State>()
        this.addNextStates(currentStates, this.start)
        var nextStates = mutableListOf<State>()
        var matched = false
        var count = 0
        while (!matched && currentStates.isNotEmpty() && pos < text.length) {
            var c = text[pos++]
            if (c=='\n') count++
            for (state in currentStates) {
                state.outgoing.forEach { trans ->
                    if (trans.match(c)) {
                        if (trans.to == MATCH_STATE)
                            matched = true
                        addNextStates(nextStates, trans.to)
                    }
                }
            }
            val t = currentStates
            currentStates = nextStates
            nextStates = t
            nextStates.clear()
        }
        return if (currentStates.contains(MATCH_STATE)) {
            val matchedText = text.substring(startPosition, pos)
            MatchResult(matchedText, count)
        } else {
            null
        }
    }

    private fun addNextStates(nextStates:MutableList<State>, next:State?) {
        if (null==next) {
        } else {
            if (next.isSplit) {
                next.outgoing.forEach { addNextStates(nextStates, it.to) }
            } else {
                nextStates.add(next)
            }
        }
    }

}