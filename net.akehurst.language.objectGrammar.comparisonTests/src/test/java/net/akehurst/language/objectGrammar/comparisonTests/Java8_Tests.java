package net.akehurst.language.objectGrammar.comparisonTests;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import antlr4.Java8Parser;
import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

public class Java8_Tests {

	static OGLanguageProcessor processor;

	static {
		getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == processor) {
			processor = new OGLanguageProcessor();
			processor.getParser().build(processor.getDefaultGoal());
		}
		return processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == javaProcessor) {
			try {
				String grammarText = new String(Files.readAllBytes(Paths.get("src/test/grammar/Java8.og")));
				Grammar javaGrammar = getOGLProcessor().process(grammarText, Grammar.class);
				javaProcessor = new LanguageProcessor(javaGrammar, "compilationUnit", null);
				javaProcessor.getParser().build(javaProcessor.getDefaultGoal());
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (ParseFailedException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (UnableToAnalyseExeception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}

		}
		return javaProcessor;
	}

	String toString(Path file) {
		try {
			byte[] bytes = Files.readAllBytes(file);
			String str = new String(bytes);
			return str;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static IParseTree parse(String input) {
		try {

			IParseTree tree = getJavaProcessor().getParser().parse(getJavaProcessor().getDefaultGoal(), input);
			return tree;
		} catch (ParseFailedException e) {
			return e.getLongestMatch();
		} catch (ParseTreeException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void emptyCompilationUnit() {

		String input = "";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

	}

	@Test
	public void ifReturn() {

		String input = "class Test {";
		input += "void test() {";
		for (int i = 0; i < 100; ++i) {
			input += "  if("+i+") return "+i+";";
		}
		input += "}";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

	}

}