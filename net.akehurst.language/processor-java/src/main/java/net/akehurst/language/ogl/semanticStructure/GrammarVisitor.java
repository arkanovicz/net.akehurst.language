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

import net.akehurst.language.api.grammar.NonTerminal;

public interface GrammarVisitor<T, E extends Throwable> {

	T visit(ChoiceSimpleDefault target, Object... arg) throws E;

	T visit(ChoicePriorityDefault target, Object... arg) throws E;

	T visit(ConcatenationDefault target, Object... arg) throws E;

	T visit(MultiDefault target, Object... arg) throws E;

	T visit(NonTerminal target, Object... arg) throws E;

	T visit(SeparatedListDefault target, Object... arg) throws E;

	T visit(GroupDefault target, Object... arg) throws E;

	T visit(TerminalPatternDefault target, Object... arg) throws E;

	T visit(TerminalLiteralDefault target, Object... arg) throws E;

}