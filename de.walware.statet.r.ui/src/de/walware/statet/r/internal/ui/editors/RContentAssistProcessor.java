/*******************************************************************************
 * Copyright (c) 2008-2009 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.ui.editors;

import de.walware.ecommons.ui.text.sourceediting.AssistInvocationContext;
import de.walware.ecommons.ui.text.sourceediting.ContentAssist;
import de.walware.ecommons.ui.text.sourceediting.ContentAssistComputerRegistry;
import de.walware.ecommons.ui.text.sourceediting.ContentAssistProcessor;
import de.walware.ecommons.ui.text.sourceediting.ISourceEditor;


public class RContentAssistProcessor extends ContentAssistProcessor {
	
	
	public RContentAssistProcessor(final ContentAssist assistant, final String partition, 
			final ContentAssistComputerRegistry registry, final ISourceEditor editor) {
		super(assistant, partition, registry, editor);
	}
	
	
	@Override
	protected AssistInvocationContext createCompletionProposalContext(final int offset) {
		return new RAssistInvocationContext(getEditor(), offset, true);
	}
	
	@Override
	protected AssistInvocationContext createContextInformationContext(final int offset) {
		return new RAssistInvocationContext(getEditor(), offset, false);
	}
	
}
