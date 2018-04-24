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
package net.akehurst.language.grammar.parser.converter.rules;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.ChoicePriority;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.language.ogl.semanticStructure.SimpleItem;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class ChoicePrioritySingleConcatenation2RuntimeRuleItem extends AbstractChoice2RuntimeRuleItem<ChoicePriority> {

    @Override
    public boolean isValidForLeft2Right(final ChoicePriority left) {
        return 1 == left.getAlternative().size()
                && (left.getAlternative().get(0).getItem().get(0) instanceof SimpleItem || 1 < left.getAlternative().get(0).getItem().size());
    }

    @Override
    public boolean isAMatch(final ChoicePriority left, final RuntimeRuleItem right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RuntimeRuleItem constructLeft2Right(final ChoicePriority left, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final RuntimeRuleItem right = converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION);
        return right;
    }

    @Override
    public void updateLeft2Right(final ChoicePriority left, final RuntimeRuleItem right, final BinaryTransformer transformer) {

        List<RuntimeRule> rrAlternatives = new ArrayList<>();
        // we know there is only one alternative from isValid
        final Concatenation concat = left.getAlternative().get(0);
        rrAlternatives = (List<RuntimeRule>) transformer.transformAllLeft2Right(
                (Class<? extends BinaryRule<ConcatenationItem, RuntimeRule>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class, concat.getItem());

        final RuntimeRule[] items = rrAlternatives.toArray(new RuntimeRule[rrAlternatives.size()]);
        right.setItems(items);

    }

    @Override
    public void updateRight2Left(final ChoicePriority arg0, final RuntimeRuleItem arg1, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public ChoicePriority constructRight2Left(final RuntimeRuleItem arg0, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRuleItem arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}