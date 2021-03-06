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

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.sppt.SPPTLeafDefault

internal class InputFromCharSequence(val text: CharSequence) {

    companion object {
        val END_OF_TEXT = 3.toChar().toString()
    }

    // private var lastlocationCachePosition = -1
    // private val locationCache = mutableMapOf<Int, InputLocation>()

    data class Match(
            val matchedText: String,
            val eolPositions: List<Int>
    )

    internal fun isStart(position: Int): Boolean {
        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
        return 0 == position
    }

    internal fun isEnd(position: Int): Boolean {
        // TODO what if we want t0 parse part of the text?, e.g. sub grammar
        return position >= this.text.length
    }

    //TODO: write a scanner that counts eols as it goes, rather than scanning the text twice
     fun eolPositions(text: String): List<Int> {
        val regex = Regex("\n", setOf(RegexOption.MULTILINE))
        return regex.findAll(text).toList().map { it.range.first }
    }

    private fun matchLiteral(position: Int, patternText: String): Match? {
        val match = this.text.regionMatches(position, patternText, 0, patternText.length, false)
        val matchedText = if (match) patternText else null
        return if (null == matchedText) {
            null
        } else {
            val eolPositions = this.eolPositions(matchedText)
            Match(matchedText, eolPositions)
        }
    }

    private fun matchRegEx(position: Int, patternText: String): Match? {
        val pattern = Regex(patternText.replace("\\\"", "\""), setOf(RegexOption.MULTILINE))
        val m = pattern.find(this.text, position)
        val lookingAt = (m?.range?.start == position)
        val matchedText = if (lookingAt) m?.value else null
        return if (null == matchedText) {
            null
        } else {
            val eolPositions = this.eolPositions(matchedText)
            Match(matchedText, eolPositions)
        }
    }

    internal fun tryMatchText(position: Int, patternText: String, isPattern: Boolean): Match? {
        val matched = when {
            (position >= this.text.length) -> if (patternText == END_OF_TEXT) Match(END_OF_TEXT, emptyList()) else null// TODO: should we need to do this?
            (!isPattern) -> this.matchLiteral(position, patternText)
            else -> this.matchRegEx(position, patternText)
        }
        return matched
    }

    fun nextLocation(lastLocation: InputLocation, newLength: Int): InputLocation {
        val lastText = this.text.substring(lastLocation.position, lastLocation.position + lastLocation.length)
        var linesInText = 0
        var lastEolInText = -1
        lastText.forEachIndexed { index, ch ->
            if (ch == '\n') {
                linesInText++
                lastEolInText = index
            }
        }
        val position = lastLocation.position + lastLocation.length
        val line = lastLocation.line + linesInText
        val column = when {
            0 == lastLocation.position && 0 == lastLocation.length -> 1
            -1 == lastEolInText -> lastLocation.column + lastLocation.length
            else -> lastLocation.length - lastEolInText
        }
        return InputLocation(position, column, line, newLength)
    }
}
