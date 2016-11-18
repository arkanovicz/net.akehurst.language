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
package net.akehurst.language.parser;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class Parser_Empty_Test extends AbstractParser_Test {
	
	Grammar empty() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice();
		return b.get();
	}
	
	@Test
	public void empty_S_empty() {
		// grammar, goal, input
		try {
			Grammar g = empty();
			String goal = "S";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.emptyLeaf("S")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void empty_S_a() {
		// grammar, goal, input
		try {
			Grammar g = empty();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

			
		} catch (ParseFailedException e) {
			
		}
	}
	
	Grammar multi0m1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0,-1,new TerminalLiteral("a"));
		return b.get();
	}
	
	@Test
	public void multi0_S_empty() {
		// grammar, goal, input
		try {
			Grammar g = multi0m1();
			String goal = "S";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.emptyLeaf("S")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void multi0_S_a() {
		// grammar, goal, input
		try {
			Grammar g = multi0m1();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.leaf("a")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}


	Grammar aeas() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("ae"), new NonTerminal("as"));
		b.rule("as").multi(0,-1,new TerminalLiteral("a"));
		b.rule("ae").multi(0, 1, new TerminalLiteral("a"));
		return b.get();
	}
	
	@Test
	public void aas_S_empty() {
		// grammar, goal, input
		try {
			Grammar g = aeas();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.branch("ae", b.leaf("a")),
						b.branch("as", b.emptyLeaf("as"))
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
