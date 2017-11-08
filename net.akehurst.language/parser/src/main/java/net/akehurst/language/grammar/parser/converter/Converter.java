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

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.grammar.parser.runtime.RuleForGroup;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.RuleItem;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.transform.binary.BinaryTransformer;
import net.akehurst.transform.binary.IBinaryRule;

public class Converter extends BinaryTransformer {

	public Converter(final RuntimeRuleSetBuilder builder) {
		this.builder = builder;
		this.virtualRule_cache = new ArrayList<>();

		this.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class);
		this.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class);
		this.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractSimpleItem2RuntimeRule.class);
		this.registerRule(ChoiceSimpleEmpty2RuntimeRuleItem.class);
		this.registerRule(ChoiceSimpleMultiple2RuntimeRuleItem.class);
		this.registerRule(ChoiceSimpleSingleConcatenation2RuntimeRuleItem.class);
		this.registerRule(ChoiceAbstractSingleOneMulti.class);
		this.registerRule(ChoiceAbstractSingleOneSeparatedList.class);
		this.registerRule(ChoicePriorityEmpty2RuntimeRuleItem.class);
		this.registerRule(ChoicePriorityMultiple2RuntimeRuleItem.class);
		this.registerRule(ChoicePrioritySingleConcatenation2RuntimeRuleItem.class);
		this.registerRule(Concatenation2RuntimeRule.class);
		this.registerRule(Concatenation2RuntimeRuleItem.class);
		this.registerRule(Grammar2RuntimeRuleSet.class);
		this.registerRule(Group2RuntimeRule.class);
		this.registerRule(Multi2RuntimeRule.class);
		this.registerRule(Multi2RuntimeRuleItem.class);
		this.registerRule(NonTerminal2RuntimeRule.class);
		this.registerRule(PriorityChoice2RuntimeRuleItem.class);
		this.registerRule(Rule2RuntimeRule.class);
		this.registerRule(SeparatedList2RuntimeRule.class);
		this.registerRule(SeparatedList2RuntimeRuleItem.class);
		this.registerRule(Terminal2RuntimeRule.class);
	}

	private final RuntimeRuleSetBuilder builder;
	private final List<RuntimeRule> virtualRule_cache;

	public RuntimeRuleSetBuilder getFactory() {
		return this.builder;
	}

	public List<RuntimeRule> getVirtualRules() {
		return this.virtualRule_cache;
	}

	private String createIndexString(final RuleItem item) {
		String str = "";
		for (final Integer i : item.getIndex()) {
			str += i + ".";
		}
		str = str.substring(0, str.length() - 1);
		return str;
	}

	RuntimeRule createVirtualRule(final Group group) {
		final Grammar grammar = group.getOwningRule().getGrammar();
		final String name = "$" + group.getOwningRule().getName() + ".group." + this.createIndexString(group);
		final RuleForGroup r = new RuleForGroup(grammar, name, group.getChoice());
		final RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		return rr;
	}

	RuntimeRule createVirtualRule(final Concatenation concatenation) {
		final Grammar grammar = concatenation.getOwningRule().getGrammar();
		final String name = "$" + concatenation.getOwningRule().getName() + ".concatenation." + this.createIndexString(concatenation);
		// final String name = "$group." + concatenation.getOwningRule().getName() + "$";
		final RuleForGroup r = new RuleForGroup(grammar, name, new ChoiceSimple(concatenation));
		final RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		return rr;
	}

	RuntimeRule createVirtualRule(final Multi multi) {
		final Grammar grammar = multi.getOwningRule().getGrammar();
		final String name = "$" + multi.getOwningRule().getName() + ".multi." + this.createIndexString(multi);
		final RuleForGroup r = new RuleForGroup(grammar, name, new ChoiceSimple(new Concatenation(multi)));
		final RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		if (0 == multi.getMin()) {
			final RuntimeRule ruleThatIsEmpty = rr;
			this.createEmptyRuleFor(ruleThatIsEmpty);
		}
		return rr;
	}

	RuntimeRule createVirtualRule(final SeparatedList sepList) {
		final Grammar grammar = sepList.getOwningRule().getGrammar();
		final String name = "$" + sepList.getOwningRule().getName() + ".sepList." + this.createIndexString(sepList);
		final RuleForGroup r = new RuleForGroup(grammar, name, new ChoiceSimple(new Concatenation(sepList)));
		final RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		if (0 == sepList.getMin()) {
			final RuntimeRule ruleThatIsEmpty = rr;
			this.createEmptyRuleFor(ruleThatIsEmpty);
		}
		return rr;
	}

	public RuntimeRule createEmptyRuleFor(final RuntimeRule right) {
		final RuntimeRule rr = this.getFactory().createEmptyRule(right);
		this.virtualRule_cache.add(rr);
		return rr;
	}

}
