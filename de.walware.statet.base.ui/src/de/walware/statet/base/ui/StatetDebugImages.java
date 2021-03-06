/*******************************************************************************
 * Copyright (c) 2010-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.base.ui;

import org.eclipse.jface.resource.ImageRegistry;

import de.walware.statet.base.internal.ui.StatetUIPlugin;


public class StatetDebugImages {
	
	
	public static final String OVR_BREAKPOINT_INSTALLED = "image/ovr/breakpoint.installed"; //$NON-NLS-1$
	public static final String OVR_BREAKPOINT_INSTALLED_DISABLED = "image/ovr/breakpoint.installed.disabled"; //$NON-NLS-1$
	
	public static final String OVR_BREAKPOINT_CONDITIONAL = "image/ovr/breakpoint.conditional"; //$NON-NLS-1$
	public static final String OVR_BREAKPOINT_CONDITIONAL_DISABLED = "image/ovr/breakpoint.conditional.disabled"; //$NON-NLS-1$
	
	public static final String OVR_METHOD_BREAKPOINT_ENTRY = "image/ovr/breakpoint.method_entry"; //$NON-NLS-1$
	public static final String OVR_METHOD_BREAKPOINT_ENTRY_DISABLED = "image/ovr/breakpoint.method_entry.disabled"; //$NON-NLS-1$
	public static final String OVR_METHOD_BREAKPOINT_EXIT = "image/ovr/breakpoint.method_exit"; //$NON-NLS-1$
	public static final String OVR_METHOD_BREAKPOINT_EXIT_DISABLED = "image/ovr/breakpoint.method_exit.disabled"; //$NON-NLS-1$
	
	
	public static ImageRegistry getImageRegistry() {
		return StatetUIPlugin.getDefault().getImageRegistry();
	}
	
}
