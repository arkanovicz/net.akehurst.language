package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public class GraphNodeBranch extends AbstractGraphNode implements IGraphNode {

	public GraphNodeBranch(IParseGraph graph, RuntimeRule rr, int priority, int startPosition) {
		this.graph = graph;
		this.runtimeRule = rr;
		this.priority = priority;
		this.children = new ArrayList<>();
//		this.children.add(firstChild);
		this.nextItemIndex = 0;//nextItemIndex;
//		this.identifier = new NodeIdentifier(rr.getRuleNumber(), firstChild.getStartPosition());// , firstChild.getEndPosition(), nextItemIndex);
		this.startPosition = startPosition;
//		this.nextInputPosition = firstChild.getNextInputPosition();
//		this.currentLength = firstChild.getMatchedTextLength();
//		firstChild.addParent(this, 0);

	}

	IParseGraph graph;
	RuntimeRule runtimeRule;
	int priority;
	int startPosition;
	int currentLength;
	List<IGraphNode> children;
	int nextItemIndex;
	int nextInputPosition;

	@Override
	public IGraphNode duplicate() {
		GraphNodeBranch duplicate = (GraphNodeBranch)this.graph.createBranch(this.runtimeRule, this.priority, this.startPosition);
		duplicate.currentLength = this.currentLength;
		duplicate.nextInputPosition = this.nextInputPosition;
		duplicate.nextItemIndex = this.nextItemIndex;
		duplicate.parents = new HashMap<>(this.parents);
		duplicate.children = new ArrayList<>(this.children);
		duplicate.getPrevious().addAll(this.getPrevious());
		return duplicate;
	}

	@Override
	public int getNextItemIndex() {
		return this.nextItemIndex;
	}
	
	@Override
	public boolean getIsLeaf() {
		return false;
	}

	@Override
	public boolean getIsEmpty() {
		//FIXME: temp, will mean empty nodes may have issues
		return false;
//		boolean empty = true;
//		for (IGraphNode c : this.children) {
//			empty &= c.getIsEmpty();
//		}
//		return empty;
	}

	@Override
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

//	@Override
	public int getEndPosition() {
		return this.getNextInputPosition();
//		throw new RuntimeException("Internal Error: Should never happen");
//		return this.children.get(this.children.size() - 1).getEndPosition();
	}

	@Override
	public int getNextInputPosition() {
		return this.nextInputPosition;
	}
	
	@Override
	public int getMatchedTextLength() {
		return getEndPosition() - getStartPosition();
	}

	@Override
	public boolean getCanGrow() {
		if (this.getIsStacked()) {
			return true;
		} else {
			return this.getCanGrowWidth();
		}
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean getIsComplete() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE:
				return true;
			case PRIORITY_CHOICE:
				return true;
			case CONCATENATION: {
				return this.getRuntimeRule().getRhs().getItems().length <= this.nextItemIndex || this.nextItemIndex == -1; // the -1 is used when creating dummy
																															// // test here!
			}
			case MULTI: {
				int size = this.nextItemIndex;
				return size >= this.getRuntimeRule().getRhs().getMultiMin() || this.nextItemIndex == -1; // the -1 is used when creating dummy branch...should
																											// really need the test here!
			}
			case SEPARATED_LIST: {
				int size = this.nextItemIndex;
				return (size % 2) == 1 || this.nextItemIndex == -1; // the -1 is used when creating dummy branch...should really need the test here!
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");

	}

	@Override
	public boolean getCanGraftBack() {
		if (getPrevious().isEmpty()) {
			return this.getIsComplete();
		}
		PreviousInfo info = this.getPrevious().get(0);
		return this.getIsComplete() && this.getIsStacked() && info.node.getExpectedItemAt(info.atPosition).getRuleNumber() == this.getRuntimeRule().getRuleNumber();
	}

	@Override
	public boolean getCanGrowWidth() {
		// boolean reachedEnd = this.getMatchedTextLength() >= inputLength;
		// if (reachedEnd)
		// return false;
		if (this.getIsComplete() && this.getIsEmpty()) {
			return false;
		}
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY: {
				return false;
			}
			case CHOICE: {
				return false;
			}
			case PRIORITY_CHOICE: {
				return false;
			}
			case CONCATENATION: {
				if (this.nextItemIndex < this.getRuntimeRule().getRhs().getItems().length) {
					return true;
				} else {
					return false; // !reachedEnd;
				}
			}
			case MULTI: {
				int size = this.nextItemIndex;
				int max = this.getRuntimeRule().getRhs().getMultiMax();
				return -1 == max || size < max;
			}
			case SEPARATED_LIST: {
				return true;
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");

	}

	@Override
	public boolean getIsStacked() {
		return !this.getPrevious().isEmpty();
	}

	@Override
	public boolean hasNextExpectedItem() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
				return false;
			case CHOICE:
				return false;
			case PRIORITY_CHOICE:
				return false;
			case CONCATENATION: {
				if (this.nextItemIndex >= this.getRuntimeRule().getRhs().getItems().length) {
					return false;
				} else {
					return true;
				}
			}
			case MULTI: {
				return true;
			}
			case SEPARATED_LIST:
				return true;
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public RuntimeRule getNextExpectedItem() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE: {
				throw new RuntimeException("Internal Error: item is choice");
			}
			case PRIORITY_CHOICE: {
				throw new RuntimeException("Internal Error: item is priority choice");
			}
			case CONCATENATION: {
				if (this.nextItemIndex >= this.getRuntimeRule().getRhs().getItems().length) {
					throw new RuntimeException("Internal Error: No NextExpectedItem");
				} else {
					return this.getRuntimeRule().getRhsItem(this.nextItemIndex);
				}
			}
			case MULTI: {
				return this.getRuntimeRule().getRhsItem(0);
			}
			case SEPARATED_LIST: {
				if ((this.nextItemIndex % 2) == 1) {
					return this.getRuntimeRule().getSeparator();
				} else {
					return this.getRuntimeRule().getRhsItem(0);
				}
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public RuntimeRule getExpectedItemAt(int atPosition) {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE: {
				throw new RuntimeException("Internal Error: item is choice");
			}
			case PRIORITY_CHOICE: {
				throw new RuntimeException("Internal Error: item is priority choice");
			}
			case CONCATENATION: {
				if (atPosition >= this.getRuntimeRule().getRhs().getItems().length) {
					throw new RuntimeException("Internal Error: No Item at position "+atPosition);
				} else {
					return this.getRuntimeRule().getRhsItem(atPosition);
				}
			}
			case MULTI: {
				return this.getRuntimeRule().getRhsItem(0);
			}
			case SEPARATED_LIST: {
				if ((this.nextItemIndex % 2) == 1) {
					return this.getRuntimeRule().getSeparator();
				} else {
					return this.getRuntimeRule().getRhsItem(0);
				}
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}
	
	@Override
	public List<IGraphNode> getChildren() {
//		return this.graph.getChildren(this);
		return this.children;
	}

	@Override
	public IGraphNode addNextChild(IGraphNode gn) {
		//try not adding children, rather adding parents!
		
//		gn.addParent(this,this.nextItemIndex);
		this.nextInputPosition = gn.getNextInputPosition();
//		this.children.add(this.nextItemIndex, gn);
		this.children.add(gn);
		this.currentLength+=gn.getMatchedTextLength();
		this.nextItemIndex++;
		if (this.getCanGrow()) {
			this.graph.getGrowable().add(this);
		}
		if (this.getIsComplete()) {
			this.graph.registerCompleteNode(this);
		}
		return this;
	}

	@Override
	public IGraphNode addSkipChild(IGraphNode gn) {
//		gn.addParent(this,this.nextItemIndex);
		this.nextInputPosition = gn.getNextInputPosition();
		this.children.add(gn);
		this.currentLength+=gn.getMatchedTextLength();
		if (this.getCanGrow()) {
			this.graph.getGrowable().add(this);
		}
		return this;
	}

	@Override
	public IGraphNode replace(IGraphNode newNode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return this.getRuntimeRule().getNodeTypeName()
				+ "(" +this.getRuntimeRule().getRuleNumber()+","+this.startPosition+","+this.currentLength+")"
				+ (this.getPrevious().isEmpty() ? "" : " -> " + this.getPrevious().get(0));
	}
}
