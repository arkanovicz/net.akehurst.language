package net.akehurst.language.parser.test;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.ogl.semanticModel.TerminalPattern;
import net.akehurst.language.parser.ScannerLessParser;
import net.akehurst.language.parser.forrest.Factory;

import org.junit.Assert;
import org.junit.Test;

abstract
public class AbstractParser_Test {
	
	protected Factory parseTreeFactory;
	
	protected IParseTree process(Grammar grammar, String text, String goalName) throws ParseFailedException {
		try {
			INodeType goal = grammar.findAllRule(goalName).getNodeType();
			IParser parser = new ScannerLessParser(this.parseTreeFactory, grammar);
			IParseTree tree = parser.parse(goal, text);
			return tree;
		} catch (RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

}
