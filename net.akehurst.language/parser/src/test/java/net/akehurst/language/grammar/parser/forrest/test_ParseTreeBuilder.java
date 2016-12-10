package net.akehurst.language.grammar.parser.forrest;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class test_ParseTreeBuilder {

	@Test
	public void leaf() {
		try {
			String text = "a";
			RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			GrammarBuilder gb = new GrammarBuilder(new Namespace("test"), "Test");
			gb.rule("a").concatenation(new TerminalLiteral("a"));
			Grammar grammar = gb.get();
			Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			ParseTreeBuilder b = new ParseTreeBuilder(runtimeRules, grammar, "a", text,0);
			ILeaf l = b.leaf("a");

			Assert.assertEquals(0, l.getStartPosition());
//			Assert.assertEquals(1, l.getEnd());
			Assert.assertEquals(1, l.getMatchedTextLength());
			Assert.assertEquals(0, l.getNumberOfLines());
//			Assert.assertEquals(false, l.getIsEmpty());
			Assert.assertEquals(false, l.getIsSkip());
			Assert.assertEquals("a", l.getMatchedText());
			Assert.assertEquals("a", l.getName());
			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

}