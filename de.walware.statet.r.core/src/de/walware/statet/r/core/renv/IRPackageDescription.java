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

package de.walware.statet.r.core.renv;


/**
 * 
 */
public interface IRPackageDescription {
	
	
	String getName();
	
	String getTitle();
	
	String getDescription();
	
	String getPriority();
	
	String getVersion();
	
	String getAuthor();
	
	String getMaintainer();
	
	String getUrl();
	
	
}
