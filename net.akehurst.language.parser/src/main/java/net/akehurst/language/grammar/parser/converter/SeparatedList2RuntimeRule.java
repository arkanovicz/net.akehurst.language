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
package net.akehurst.language.grammar.parser.converter;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class SeparatedList2RuntimeRule extends AbstractConcatinationItem2RuntimeRule<SeparatedList> {

	@Override
	public boolean isValidForLeft2Right(SeparatedList arg0) {
		return true;
	}
	
	@Override
	public RuntimeRule constructLeft2Right(SeparatedList left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}
	
	@Override
	public void configureLeft2Right(SeparatedList left, RuntimeRule right, Transformer transformer) {
		try {
			
			RuntimeRuleItem ruleItem = transformer.transformLeft2Right(SeparatedList2RuntimeRuleItem.class, left);
			right.setRhs(ruleItem);

		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(SeparatedList arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SeparatedList constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}