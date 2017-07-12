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
package net.akehurst.language.ogl.semanticStructure;

import java.util.List;
import java.util.Set;

import net.akehurst.language.core.analyser.IRuleItem;

public abstract class RuleItem implements Visitable, IRuleItem {

	Rule owningRule;

	public Rule getOwningRule() {
		return this.owningRule;
	}

	public abstract void setOwningRule(Rule value, List<Integer> index);

	public abstract List<Integer> getIndex();

	public abstract Set<Terminal> findAllTerminal();

	public abstract Set<NonTerminal> findAllNonTerminal();

}
