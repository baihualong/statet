/*******************************************************************************
 * Copyright (c) 2007-2009 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.SourceContent;
import de.walware.ecommons.ltk.SourceDocumentRunnable;
import de.walware.ecommons.ltk.WorkingBuffer;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.statet.base.internal.ui.StatetUIPlugin;


/**
 * WorkingBuffer using {@link ITextFileBuffer}
 * usually for editors and the editor context
 */
public class FileBufferWorkingBuffer extends WorkingBuffer {
	
	
	public static void syncExec(final SourceDocumentRunnable runnable)
			throws InvocationTargetException {
		final AtomicReference<InvocationTargetException> error = new AtomicReference<InvocationTargetException>();
		UIAccess.getDisplay().syncExec(new Runnable() {
			public void run() {
				Object docLock = null;
				final AbstractDocument document = runnable.getDocument();
				if (document instanceof ISynchronizable) {
					docLock = ((ISynchronizable) document).getLockObject();
				}
				if (docLock == null) {
					docLock = new Object();
				}
				
				DocumentRewriteSession rewriteSession = null;
				try {
					if (runnable.getRewriteSessionType() != null) {
						rewriteSession = document.startRewriteSession(runnable.getRewriteSessionType());
					}
					synchronized (docLock) {
						if (runnable.getStampAssertion() > 0 && document.getModificationStamp() != runnable.getStampAssertion()) {
							throw new CoreException(new Status(Status.ERROR, StatetUIPlugin.PLUGIN_ID, "Document out of sync (usuallly caused by concurrent document modifications)."));
						}
						runnable.run();
					}
				}
				catch (final InvocationTargetException e) {
					error.set(e);
				}
				catch (final Exception e) {
					error.set(new InvocationTargetException(e));
				}
				finally {
					if (rewriteSession != null) {
						document.stopRewriteSession(rewriteSession);
					}
				}
			}
		});
		if (error.get() != null) {
			throw error.get();
		}
	}
	
	
	/** Mode for IFile (in workspace) */
	private static final int IFILE = 1;
	/** Mode for IFileStore (URI) */
	private static final int FILESTORE = 2;
	
	
	private ITextFileBuffer fBuffer;
	/**
	 * Mode of this working buffer:<ul>
	 *   <li>= 0 - uninitialized</li>
	 *   <li>< 0 - invalid/no source found</li>
	 *   <li>> 0 - mode constant {@link #IFILE}, {@link #FILESTORE}</li>
	 * </ul>
	 */
	private int fMode;
	
	
	public FileBufferWorkingBuffer(final ISourceUnit unit) {
		super(unit);
	}
	
	
	/**
	 * Checks the mode of this working buffer
	 * 
	 * @return <code>true</code> if valid mode, otherwise <code>false</code>
	 */
	private boolean detectMode() {
		if (fMode == 0) {
			if (fUnit.getResource() != null) {
				if (fUnit.getResource() instanceof IFile) {
					fMode = IFILE;
				}
			}
			else {
				final IFileStore store = (IFileStore) fUnit.getAdapter(IFileStore.class);
				if (store != null && !store.fetchInfo().isDirectory()) {
					fMode = FILESTORE;
				}
			}
			if (fMode == 0) {
				fMode = -1;
			}
		}
		return (fMode > 0);
	}
	
	@Override
	protected AbstractDocument createDocument(final SubMonitor progress) {
		if (detectMode()) {
			if (fMode == IFILE) {
				final IPath path = fUnit.getPath();
				try {
					FileBuffers.getTextFileBufferManager().connect(path, LocationKind.IFILE, progress);
					fBuffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(path, LocationKind.IFILE);
				}
				catch (final CoreException e) {
					StatetUIPlugin.log(e.getStatus());
				}
			}
			else if (fMode == FILESTORE) {
				final IFileStore store = (IFileStore) fUnit.getAdapter(IFileStore.class);
				try {
					FileBuffers.getTextFileBufferManager().connectFileStore(store, progress);
					fBuffer = FileBuffers.getTextFileBufferManager().getFileStoreTextFileBuffer(store);
				}
				catch (final CoreException e) {
					StatetUIPlugin.log(e.getStatus());
				}
			}
			if (fBuffer != null) {
				final IDocument fileDoc = fBuffer.getDocument();
				if (!(fileDoc instanceof AbstractDocument)) {
					return null;
				}
				return (AbstractDocument) fileDoc;
			}
			return null;
		}
		return super.createDocument(progress);
	}
	
	@Override
	protected SourceContent createContent(final SubMonitor progress) {
		if (detectMode()) {
			ITextFileBuffer buffer = fBuffer;
			if (buffer == null) {
				if (fMode == IFILE) {
					final IPath path = fUnit.getPath();
					buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(path, LocationKind.IFILE);
				}
				else if (fMode == FILESTORE) {
					final IFileStore store = (IFileStore) fUnit.getAdapter(IFileStore.class);
					buffer = FileBuffers.getTextFileBufferManager().getFileStoreTextFileBuffer(store);
				}
			}
			if (buffer != null) {
				return createContentFromDocument(buffer.getDocument());
			}
		}
		return super.createContent(progress);
	}
	
	@Override
	public void releaseDocument(final IProgressMonitor monitor) {
		if (fBuffer != null) {
			try {
				final SubMonitor progress = SubMonitor.convert(monitor);
				if (fMode == IFILE) {
					final IPath path = fUnit.getPath();
					FileBuffers.getTextFileBufferManager().disconnect(path, LocationKind.IFILE, progress);
				}
				else if (fMode == FILESTORE) {
					final IFileStore store = (IFileStore) fUnit.getAdapter(IFileStore.class);
					FileBuffers.getTextFileBufferManager().disconnectFileStore(store, progress);
				}
			}
			catch (final CoreException e) {
				StatetUIPlugin.log(e.getStatus());
			}
			finally {
				fBuffer = null;
				super.releaseDocument(monitor);
			}
			return;
		}
		else {
			super.releaseDocument(monitor);
		}
	}
	
}
