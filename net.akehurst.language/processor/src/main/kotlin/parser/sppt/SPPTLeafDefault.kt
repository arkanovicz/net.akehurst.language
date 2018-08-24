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

package net.akehurst.language.parser.sppt

import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor
import net.akehurst.language.parser.runtime.RuntimeRule

class SPPTLeafDefault(terminalRule: RuntimeRule, startPosition:Int, override val isEmptyLeaf: Boolean, override val matchedText: String) : SPPTNodeDefault(terminalRule, startPosition, matchedText.length), SPPTLeaf {

    // --- SPPTLeaf ---

    override val isPattern: Boolean = terminalRule.isPattern


    // --- SPPTNode ---

    override val nonSkipMatchedText: String = if (isSkip) "" else this.matchedText

    override fun contains(other: SPPTNode): Boolean {
        return this.identity == other.identity
    }

    override val isLeaf: Boolean = true
    override val isBranch: Boolean = false

    override fun <T, A> accept(visitor: SharedPackedParseTreeVisitor<T, A>, arg: A): T {
        return visitor.visit(this,  arg)
    }
}