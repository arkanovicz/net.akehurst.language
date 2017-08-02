package net.akehurst.language.parse.graph;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IGrowingNode {
	RuntimeRule getRuntimeRule();

	int getRuntimeRuleNumber();

	int getStartPosition();

	int getEndPosition();

	int getNextItemIndex();

	int getPriority();

	int getMatchedTextLength();

	boolean getIsSkip();

	boolean getHasCompleteChildren();

	boolean getCanGrowWidth();

	boolean getCanGraftBack();

	List<RuntimeRule> getNextExpectedTerminals();

	int getNextInputPosition();

	boolean getCanGrowWidthWithSkip();

	boolean hasNextExpectedItem();

	boolean getExpectsItemAt(RuntimeRule runtimeRule, int atPosition);

	boolean getIsStacked();

	public static final class PreviousInfo {
		public PreviousInfo(final IGrowingNode node, final int atPosition) {
			this.node = node;
			this.atPosition = atPosition;
			this.hashCode_cache = Objects.hash(node, atPosition);
		}

		public IGrowingNode node;
		public int atPosition;

		int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(final Object arg) {
			if (!(arg instanceof PreviousInfo)) {
				return false;
			}
			final PreviousInfo other = (PreviousInfo) arg;
			return this.atPosition == other.atPosition && this.node == other.node;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.atPosition)).concat("-").concat(this.node.toString()).concat(")");
		}
	}

	Set<PreviousInfo> getPrevious();

	void addPrevious(IGrowingNode previousNode, int atPosition);

	List<RuntimeRule> getNextExpectedItem();

	boolean getIsLeaf();

	List<ICompleteNode> getGrowingChildren();

}
