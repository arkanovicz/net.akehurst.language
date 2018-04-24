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
package net.akehurst.language.ogl.semanticAnalyser.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.ogl.semanticAnalyser.SemanicAnalyser;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class GrammarDefinitionBranch2Grammar implements BinaryRule<ISPNode, Grammar> {

    @Override
    public boolean isValidForLeft2Right(final ISPNode left) {
        return left.getName().equals("grammarDefinition");
    }

    @Override
    public boolean isValidForRight2Left(final Grammar right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final ISPNode left, final Grammar right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Grammar constructLeft2Right(final ISPNode left, final BinaryTransformer transformer) {

        final ISPBranch namespaceBranch = (ISPBranch) ((ISPBranch) left).getChild(0);
        final ISPBranch grammarBranch = (ISPBranch) ((ISPBranch) left).getChild(1);
        final ISPBranch grammarNameBranch = (ISPBranch) grammarBranch.getChild(1);

        final Namespace namespace = transformer.transformLeft2Right(Node2Namespace.class, namespaceBranch);
        final String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, grammarNameBranch);

        final Grammar right = new Grammar(namespace, name);

        return right;

    }

    @Override
    public ISPBranch constructRight2Left(final Grammar arg0, final BinaryTransformer arg1) {
        // TODO Auto-generated method stub, handle extends !!
        return null;
    }

    @Override
    public void updateLeft2Right(final ISPNode left, final Grammar right, final BinaryTransformer transformer) {

        final ISPBranch grammarBranch = (ISPBranch) ((ISPBranch) left).getChild(1);
        final ISPBranch extendsBranch = (ISPBranch) grammarBranch.getChild(2);
        final ISPBranch rulesBranch = (ISPBranch) grammarBranch.getChild(4);
        final List<ISPBranch> ruleBranches = rulesBranch.getBranchNonSkipChildren();

        if (0 == extendsBranch.getMatchedTextLength()) {
            // no extended grammar
        } else {
            final ISPBranch extendsListBranch = (ISPBranch) ((ISPBranch) extendsBranch.getChild(0)).getChild(1);
            final List<String> extendsList = new ArrayList<>();
            for (final ISPNode n : extendsListBranch.getNonSkipChildren()) {
                if ("qualifiedName".equals(n.getName())) {
                    final ISPBranch b = (ISPBranch) n;
                    String qualifiedName = n.getMatchedText().trim();
                    if (1 == b.getNonSkipChildren().size()) {
                        qualifiedName = right.getNamespace().getQualifiedName() + "::" + qualifiedName;
                    }
                    extendsList.add(qualifiedName);
                }
            }

            final List<IGrammar> extendedGrammars = ((SemanicAnalyser) transformer).getGrammarLoader().resolve(extendsList.toArray(new String[0]));
            final List<Grammar> grammars = extendedGrammars.stream().map((e) -> (Grammar) e).collect(Collectors.toList());
            right.setExtends(grammars);
        }

        final List<? extends Rule> rules = transformer.transformAllLeft2Right(AnyRuleNode2Rule.class, ruleBranches);
        right.setRule((List<Rule>) rules);

    }

    @Override
    public void updateRight2Left(final ISPNode arg0, final Grammar arg1, final BinaryTransformer arg2) {
        // TODO Auto-generated method stub

    }

}