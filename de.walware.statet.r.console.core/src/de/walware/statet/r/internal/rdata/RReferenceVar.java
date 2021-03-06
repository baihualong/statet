/*******************************************************************************
 * Copyright (c) 2009-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.rdata;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import de.walware.rj.data.RJIO;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RObjectFactory;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.ExternalizableRObject;

import de.walware.statet.r.console.core.RWorkspace;
import de.walware.statet.r.core.model.IRLangElement;
import de.walware.statet.r.core.model.RElementName;


public final class RReferenceVar extends CombinedElement
		implements RReference, ExternalizableRObject {
	
	
	private int fType;
	private long fHandle;
	private String fClassName;
	private RWorkspace fResolver;
	
	
	public RReferenceVar(final String className) {
		fClassName = className;
	}
	
	public RReferenceVar(final long handle, final String className, final RElementName name) {
		fHandle = handle;
		fClassName = className;
		fElementName = name;
	}
	
	public RReferenceVar(final RJIO io, final RObjectFactory factory, final CombinedElement parent, final RElementName name) throws IOException {
		fParent = parent;
		fElementName = name;
		readExternal(io, factory);
	}
	
	@Override
	public void readExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		fHandle = io.readLong();
		fType = io.readInt();
		fClassName = io.readString();
	}
	
	@Override
	public void writeExternal(final RJIO io, final RObjectFactory factory) throws IOException {
		io.writeLong(fHandle);
		io.writeInt(fType);
		io.writeString(fClassName);
	}
	
	
	@Override
	public byte getRObjectType() {
		return TYPE_REFERENCE;
	}
	
	@Override
	public int getReferencedRObjectType() {
		return fType;
	}
	
	@Override
	public String getRClassName() {
		return fClassName;
	}
	
	@Override
	public int getLength() {
		return 0;
	}
	
	@Override
	public long getHandle() {
		return fHandle;
	}
	
	public void setResolver(final RWorkspace resolver) {
		fResolver = resolver;
	}
	
	@Override
	public RObject getResolvedRObject() {
		if (fResolver != null) {
			return fResolver.resolve(this, false);
		}
		return null;
	}
	
	@Override
	public RStore getData() {
		return null;
	}
	
	
	@Override
	public int getElementType() {
		return R_GENERAL_VARIABLE;
	}
	
	@Override
	public boolean hasModelChildren(final Filter filter) {
		return false;
	}
	
	@Override
	public List<? extends IRLangElement> getModelChildren(final Filter filter) {
		return Collections.EMPTY_LIST;
	}
	
}
