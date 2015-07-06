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
package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.Transformer;

public class Terminal2RuntimeRule extends AbstractTangibleItem2RuntimeRule<Terminal> {

	@Override
	public boolean isValidForLeft2Right(Terminal arg0) {
		return true;
	}

	@Override
	public RuntimeRule constructLeft2Right(Terminal left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right =  converter.getFactory().createRuntimeRule(left, RuntimeRuleKind.TERMINAL);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Terminal arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(Terminal arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Terminal constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
