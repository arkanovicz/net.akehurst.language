package net.akehurst.language.ogl.semanticAnalyser;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class ChoceNode2Choice extends AbstractRhsNode2RuleItem<Choice> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "choice".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(Choice right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Choice constructLeft2Right(INode left, Transformer transformer) {
		try {
			List<? extends INode> allLeft = ((IBranch) left).getNonSkipChildren();
			List<? extends Concatenation> allRight;

			List<INode> concatenationNodes = new ArrayList<>();
			for (INode n: allLeft) {
				if ("concatenation".equals(n.getName())) {
					concatenationNodes.add(n);
				}
			}
			
			allRight = transformer.transformAllLeft2Right(
					(Class<Relation<INode, Concatenation>>) (Class<?>) Node2Concatenation.class, concatenationNodes);

			Choice right = new Choice(allRight.toArray(new Concatenation[0]));
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public INode constructRight2Left(Choice right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Choice right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, Choice right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
