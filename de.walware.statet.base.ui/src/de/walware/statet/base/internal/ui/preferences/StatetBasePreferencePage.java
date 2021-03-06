/*******************************************************************************
 * Copyright (c) 2005-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.base.internal.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.walware.statet.base.internal.ui.StatetUIPlugin;


public class StatetBasePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	
	public StatetBasePreferencePage() {
		
		super(GRID);
		
		setPreferenceStore(StatetUIPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.StatetBase_description);
	}
	
	
	@Override
	public void init(final IWorkbench workbench) {
	}
	
	@Override
	public void createFieldEditors() {
	}
	
}
