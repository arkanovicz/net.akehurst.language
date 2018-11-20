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

package net.akehurst.language.processor

import net.akehurst.language.api.sppt2ast.Sppt2AstTransformer
import net.akehurst.language.api.sppt2ast.UnableToTransformSppt2AstExeception
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.Parser
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.ogl.grammar.runtime.Converter
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.parser.scannerless.ScannerlessParser
import kotlin.js.JsName

internal class LanguageProcessorDefault(val grammar: Grammar, val semanticAnalyser: Sppt2AstTransformer?) : LanguageProcessor {

    private val converter: Converter = Converter(this.grammar)
    private val parser: Parser = ScannerlessParser(this.converter.transform())
    private val completionProvider: CompletionProvider = CompletionProvider()

    override fun build() {
        this.parser.build()
    }

    override fun scan(inputText: CharSequence): List<SPPTLeaf> {
        return this.parser.scan(inputText);
    }

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val sppt: SharedPackedParseTree = this.parser.parse(goalRuleName, inputText)
        return sppt
    }

    override fun <T> process(goalRuleName: String, inputText: CharSequence): T {
        val sppt: SharedPackedParseTree = this.parse(goalRuleName, inputText)
        if (null == this.semanticAnalyser) {
            throw UnableToTransformSppt2AstExeception("No Sppt2AstTransformer supplied", null);
        }
        val t: T = this.semanticAnalyser.transform(sppt);

        return t;
    }

    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int, desiredDepth: Int): List<CompletionItem> {
        val parserExpected: List<RuntimeRule> = this.parser.expectedAt(goalRuleName, inputText, position);
        val grammarExpected : List<RuleItem> = parserExpected.map {  this.converter.originalRuleItemFor(it) }
        val expected = grammarExpected.flatMap { this.completionProvider.provideFor(it, desiredDepth) }
        return expected
    }

}