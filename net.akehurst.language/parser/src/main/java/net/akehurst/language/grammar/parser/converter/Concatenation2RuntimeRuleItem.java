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

import java.util.Arrays;
import java.util.List;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Concatenation2RuntimeRuleItem implements Relation<Concatenation, RuntimeRuleItem> {

	@Override
	public boolean isValidForLeft2Right(Concatenation arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(Concatenation left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		return converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION);
	}
	
	@Override
	public void configureLeft2Right(Concatenation left, RuntimeRuleItem right, Transformer transformer) {
		List<ConcatenationItem> tis = left.getItem();
		
		try {
			List<? extends RuntimeRule> rr = transformer.transformAllLeft2Right((Class<? extends Relation<ConcatenationItem, RuntimeRule>>)(Class<?>)AbstractConcatinationItem2RuntimeRule.class, tis);
			if (rr.isEmpty()) {
				//add an EMPTY_RULE
				RuntimeRule ruleThatIsEmpty = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getOwningRule());
				Converter converter = (Converter)transformer;
				RuntimeRule rhs = converter.createEmptyRuleFor(ruleThatIsEmpty);
				rr = Arrays.asList( rhs );
			}
			RuntimeRule[] items = rr.toArray(new RuntimeRule[rr.size()]);
			
			right.setItems(items);
		
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void configureRight2Left(Concatenation arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Concatenation constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}