package net.akehurst.language.grammar.parser.converter.rules;

import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.ConcatenationDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Concatenation2RuntimeRule implements BinaryRule<ConcatenationDefault, RuntimeRule> {

    @Override
    public boolean isValidForLeft2Right(final ConcatenationDefault left) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRule right) {
        return false;
    }

    @Override
    public boolean isAMatch(final ConcatenationDefault left, final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RuntimeRule constructLeft2Right(final ConcatenationDefault left, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final RuntimeRule right = converter.createVirtualRule(left);
        return right;
    }

    @Override
    public ConcatenationDefault constructRight2Left(final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final ConcatenationDefault left, final RuntimeRule right, final BinaryTransformer transformer) {

        final RuntimeRuleItem ruleItem = transformer.transformLeft2Right(Concatenation2RuntimeRuleItem.class, left);
        right.setRhs(ruleItem);

    }

    @Override
    public void updateRight2Left(final ConcatenationDefault left, final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
