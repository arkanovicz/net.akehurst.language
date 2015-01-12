package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.ogl.semanticModel.Rule;

public class Branch extends AbstractNode implements IBranch {

	public Branch(INodeType nodeType) {
		super(nodeType);
		this.childeren = new ArrayList<>();
		this.length = 0;
	}
	Rule rule;
	
	List<INode> childeren;
	@Override
	public List<INode> getChildren() {
		return this.childeren;
	}
	public IBranch addChild(INode child) {
		this.childeren.add(child);
		//child.setParent(this);
		this.length += child.getLength();
		return this;
	}

	@Override
	public Branch deepClone() {
		Branch clone = new Branch(this.getNodeType());
		clone.name = this.name;
		for(INode n: this.getChildren()) {
			AbstractNode an = (AbstractNode)n;
			clone.addChild(an.deepClone());
		}
		clone.length = this.length;
		return clone;
	}
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Leaf) {
			Leaf other = (Leaf)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
