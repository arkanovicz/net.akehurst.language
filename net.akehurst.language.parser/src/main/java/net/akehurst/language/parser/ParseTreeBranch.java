package net.akehurst.language.parser;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatination;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.ogl.semanticModel.TangibleItem;

public class ParseTreeBranch extends SubParseTree {

	public ParseTreeBranch(INodeType nodeType, IParseTree branchTree, Rule rule, int inputLength) {
		super(inputLength);
		Branch branch = new Branch(nodeType);
		branch.addChild(branchTree.getRoot());
		this.rule = rule;
		this.root = branch;
		this.nextItemIndex = 1;
		this.length = 1;
		super.canGrow = this.calculateCanGrow();
		super.complete = this.calculateIsComplete();
	}
	
	ParseTreeBranch(int inputLength) {
		super(inputLength);
	}
	
	Rule rule;
	int nextItemIndex;
	int length;
	
	@Override
	public Branch getRoot() {
		return (Branch)super.getRoot();
	}
	
	public ParseTreeBranch extendWith(IParseTree extension) throws ParseTreeException {
		ParseTreeBranch newBranch = this.deepClone();
		newBranch.getRoot().addChild(extension.getRoot());
		if (extension.getRoot().getNodeType() instanceof SkipNodeType) {
			
		} else {
			newBranch.length++;
			newBranch.complete = newBranch.calculateIsComplete();
			newBranch.canGrow = newBranch.calculateCanGrow();
			newBranch.nextItemIndex++;
		}
		return newBranch;
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		RuleItem item = this.rule.getRhs();
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return c.getItem().get(this.nextItemIndex);
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			return m.getItem();
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			if ( (this.length % 2) == 1 ) {
				return sl.getSeparator();
			} else {
				return sl.getConcatination();
			}
		} else {
			throw new RuntimeException("Should never happen");
		}
	}

	boolean calculateIsComplete() {
		RuleItem item = this.rule.getRhs();
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return c.getItem().size() == this.length;
		} else if (item instanceof Choice) {
			return true;
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			int size = this.length;
			return m.getMin() <= size && (size <= m.getMax() || -1 == m.getMax());
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			int size = this.length;
			return (size % 2) == 1;
		} else {
			throw new RuntimeException("Should never happen");
		}
	}
	boolean calculateCanGrow() {
		RuleItem item = this.rule.getRhs();
		boolean reachedEnd = this.getRoot().getLength() >= this.inputLength;
		if (reachedEnd) return false;
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return this.length < c.getItem().size();
		} else if (item instanceof Choice) {
			return false;
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			int size = this.length;
			return size < m.getMax();
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			int size = this.length;
			return true;
		} else {
			throw new RuntimeException("Should never happen");
		}
	}
	
	public ParseTreeBranch deepClone() {
		ParseTreeBranch clone = new ParseTreeBranch(this.inputLength);
		clone.root = this.getRoot().deepClone();
		clone.rule = this.rule;
		clone.nextItemIndex = this.nextItemIndex;
		clone.length = this.length;
		clone.complete = this.complete;
		clone.canGrow = this.canGrow;
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof ParseTreeBranch) {
			ParseTreeBranch other = (ParseTreeBranch)arg;
			return this.getRoot().equals(other.getRoot());
		} else {
			return false;
		}
	}
}
