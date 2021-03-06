/*******************************************************************************
 * Copyright (c) 2011 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.ui;

import de.walware.ecommons.ltk.IProblemRequestor;

import de.walware.statet.r.core.model.IRSourceUnit;
import de.walware.statet.r.core.model.RSuModelContainer;


public class RUISuModelContainer extends RSuModelContainer {
	
	
	public RUISuModelContainer(final IRSourceUnit su) {
		super(su);
	}
	
	
	@Override
	protected IProblemRequestor createEditorContextProblemRequestor(final long stamp) {
		return RUIPlugin.getDefault().getRDocumentProvider().createProblemRequestor(
				getSourceUnit(), stamp );
	}
	
}
