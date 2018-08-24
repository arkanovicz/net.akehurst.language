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

package net.akehurst.language.api.parser


import net.akehurst.language.api.grammar.GrammarRuleNotFoundException
import net.akehurst.language.api.grammar.NodeType
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.sppt.SharedPackedParseTree

interface Parser {

	/**
	 * It is not necessary to call this method, but doing so will speed up future calls to parse as it will build the internal caches for the parser,
	 */
	fun build();

	/**
	 * get a list of the types of node (is this useful!)
	 */
	val nodeTypes: Set<NodeType>

	/**
	 * parse the inputText starting with the given grammar rule and return the shared packed parse Tree.
	 *
	 * @param goalRuleName
	 * @param inputText
	 * @return the result of parsing
	 * @throws ParseFailedException
	 * @throws ParseTreeException
	 * @throws GrammarRuleNotFoundException
	 */
	fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree

	//fun parse(goalRuleName: String, inputText: Reader): SharedPackedParseTree

	/**
	 * @throws ParseFailedException
	 * @throws ParseTreeException
	 * @throws GrammarRuleNotFoundException
	 **/
	fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuleItem>

	//fun expectedAt(goalRuleName: String, inputText: Reader, position: Long): List<RuleItem>
}
