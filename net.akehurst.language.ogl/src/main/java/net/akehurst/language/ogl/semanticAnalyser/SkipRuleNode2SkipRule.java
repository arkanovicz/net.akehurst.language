/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.SkipRule;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class SkipRuleNode2SkipRule extends NormalRuleNode2Rule {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "skipRule".equals(left.getName());
	}
	
	@Override
	public SkipRule constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode grammarNode = left.getParent().getParent().getParent().getParent();
			Grammar grammar = transformer.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, grammarNode);
			String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch)left).getChild(1));
			SkipRule right = new SkipRule(grammar, name);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}
	
}
