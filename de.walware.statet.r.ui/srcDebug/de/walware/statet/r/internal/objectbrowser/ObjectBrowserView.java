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

package de.walware.statet.r.internal.objectbrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.text.Collator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.ui.handlers.CollapseAllHandler;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import de.walware.ecommons.FastList;
import de.walware.ecommons.collections.ConstList;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ui.IElementNameProvider;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorCommandIds;
import de.walware.ecommons.ts.ITool;
import de.walware.ecommons.ts.IToolRunnable;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerContributionItem;
import de.walware.ecommons.ui.actions.SearchContributionItem;
import de.walware.ecommons.ui.util.ColumnHoverManager;
import de.walware.ecommons.ui.util.ColumnHoverStickyManager;
import de.walware.ecommons.ui.util.ColumnWidgetTokenOwner;
import de.walware.ecommons.ui.util.DNDUtil;
import de.walware.ecommons.ui.util.DialogUtil;
import de.walware.ecommons.ui.util.InformationDispatchHandler;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.PostSelectionProviderProxy;
import de.walware.ecommons.ui.util.TreeSelectionProxy;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.statet.base.ui.StatetImages;
import de.walware.statet.nico.core.runtime.SubmitType;
import de.walware.statet.nico.core.runtime.ToolController;
import de.walware.statet.nico.core.runtime.ToolProcess;
import de.walware.statet.nico.core.runtime.ToolWorkspace;
import de.walware.statet.nico.core.util.IToolProvider;
import de.walware.statet.nico.core.util.IToolRetargetable;
import de.walware.statet.nico.ui.IToolRegistry;
import de.walware.statet.nico.ui.IToolRegistryListener;
import de.walware.statet.nico.ui.IToolRunnableDecorator;
import de.walware.statet.nico.ui.NicoUI;
import de.walware.statet.nico.ui.NicoUITools;
import de.walware.statet.nico.ui.ToolSessionUIData;
import de.walware.statet.nico.ui.actions.ToolRetargetableHandler;
import de.walware.statet.nico.ui.util.ToolMessageDialog;

import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.REnvironment;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RReference;
import de.walware.rj.data.RStore;

import de.walware.statet.r.console.core.AbstractRDataRunnable;
import de.walware.statet.r.console.core.IRDataAdapter;
import de.walware.statet.r.console.core.RProcess;
import de.walware.statet.r.console.core.RTool;
import de.walware.statet.r.console.core.RWorkspace;
import de.walware.statet.r.console.core.RWorkspace.ICombinedREnvironment;
import de.walware.statet.r.core.data.ICombinedRElement;
import de.walware.statet.r.core.model.RElementComparator;
import de.walware.statet.r.core.model.RElementName;
import de.walware.statet.r.internal.ui.RUIPlugin;
import de.walware.statet.r.internal.ui.rtools.RunPrintInR;
import de.walware.statet.r.ui.RLabelProvider;
import de.walware.statet.r.ui.rtool.RElementInfoHoverCreator;
import de.walware.statet.r.ui.rtool.RElementInfoTask;


public class ObjectBrowserView extends ViewPart implements IToolProvider {
	
	
	private static final String FILTER_USERSPACEONLY_SETTINGS_KEY = "filter.only_userspace.enabled"; //$NON-NLS-1$
	private static final String FILTER_INTERNALINCLUDE_SETTINGS_KEY = "filter.include_internal.enabled"; //$NON-NLS-1$
	private static final String SORT_BYTYPE_SETTINGS_KEY = "sort.by_name.enabled"; //$NON-NLS-1$
	
	private static final RElementComparator ELEMENTNAME_COMPARATOR = new RElementComparator();
	private static final ViewerComparator TYPE_COMPARATOR = new SortByTypeComparator();
	
	private static final String OPEN_COMMAND_ID = "org.eclipse.jdt.ui.edit.text.java.open.editor"; //$NON-NLS-1$
	
	
	private static class ContentInput {
		
		List<? extends ICombinedREnvironment> searchEnvirs;
		boolean processChanged;
		boolean inputChanged;
		final boolean filterUserspace;
		final IModelElement.Filter envFilter;
		final IModelElement.Filter otherFilter;
		ICombinedRElement[] rootElements;
		Map<ICombinedRElement, ICombinedRElement[]> envirElements;
		
		
		public ContentInput(final boolean processChanged, final boolean inputChanged,
				final boolean filterUserspace,
				final IModelElement.Filter envFilter, final IModelElement.Filter otherFilter) {
			this.processChanged = processChanged;
			this.inputChanged = inputChanged;
			this.filterUserspace = filterUserspace;
			this.envFilter = envFilter;
			this.otherFilter = otherFilter;
		}
		
	}
	
	private static class ContentFilter implements IModelElement.Filter {
		
		private final boolean fFilterInternal;
		private final boolean fFilterNoPattern;
		private final SearchPattern fSearchPattern;
		
		
		public ContentFilter(final boolean filterInternal, final SearchPattern pattern) {
			fFilterInternal = filterInternal;
			fFilterNoPattern = (pattern == null);
			fSearchPattern = pattern;
		}
		
		
		@Override
		public boolean include(final IModelElement element) {
			final String name = element.getElementName().getSegmentName();
			if (name != null) {
				if (fFilterInternal && name.length() > 0 && name.charAt(0) == '.') {
					return false;
				}
				return (fFilterNoPattern || fSearchPattern.matches(name));
			}
			else {
				return true;
			}
		}
		
	}
	
	private static class SortByTypeComparator extends ViewerComparator {
		
		private final Collator fClassNameCollator = Collator.getInstance(Locale.ENGLISH);
		
		@Override
		public void sort(final Viewer viewer, final Object[] elements) {
			if (elements == null || elements.length == 0) {
				return;
			}
			if (elements != null && elements.length > 0 && elements[0] instanceof ICombinedRElement) {
				final ICombinedRElement object = (ICombinedRElement) elements[0];
				if (object.getRObjectType() != RObject.TYPE_ENV) {
					Arrays.sort(elements, new Comparator<Object>() {
						@Override
						public int compare(final Object a, final Object b) {
							return SortByTypeComparator.this.compare(
									(ICombinedRElement) a, (ICombinedRElement) b);
						}
					});
				}
			}
		}
		
		public int compare(final ICombinedRElement e1, final ICombinedRElement e2) {
			{	// By type
				final int cat1 = category(e1);
				final int cat2 = category(e2);
				if (cat1 != cat2) {
					return cat1 - cat2;
				}
				if (cat1 == RObject.TYPE_VECTOR || cat1 == RObject.TYPE_ARRAY) {
					final int d1 = getDataOrder(e1.getData().getStoreType());
					final int d2 = getDataOrder(e2.getData().getStoreType());
					if (d1 != d2) {
						return d1 - d2;
					}
				}
			}
			{	// By classname
				final int diff = fClassNameCollator.compare(e1.getRClassName(), e2.getRClassName());
				if (diff != 0) {
					return diff;
				}
			}
			{	// By index
				final RElementName name1 = e1.getElementName();
				final RElementName name2 = e2.getElementName();
				if (name1 instanceof RElementName.IndexElementName
						&& name2 instanceof RElementName.IndexElementName) {
						// index
					return ((RElementName.IndexElementName) name1).getIndex() -
							((RElementName.IndexElementName) name2).getIndex();
				}
			}
			// By name
			return ELEMENTNAME_COMPARATOR.compare(e1, e2);
		}
		
		private int getDataOrder(final int dataType) {
			switch (dataType) {
			case RStore.NUMERIC:
				return 1;
			case RStore.COMPLEX:
				return 2;
			case RStore.LOGICAL:
				return 3;
			case RStore.INTEGER:
				return 4;
			case RStore.FACTOR:
				return 4;
			case RStore.CHARACTER:
				return 5;
			case RStore.RAW:
				return 6;
			default:
				return 7;
			}
		}
		
		public int category(final ICombinedRElement element) {
			byte objectType = element.getRObjectType();
			if (objectType == RObject.TYPE_REFERENCE) {
				final RObject realObject = ((RReference) element).getResolvedRObject();
				if (realObject != null) {
					objectType = realObject.getRObjectType();
				}
			}
			if (objectType == RObject.TYPE_ARRAY) {
				objectType = RObject.TYPE_VECTOR;
			}
			return objectType;
		}
		
	}
	
	private class RefreshWorkspaceR extends AbstractRDataRunnable {
		
		public RefreshWorkspaceR() {
			super("r/objectbrowser/refreshWorkspace.force", "Update Object Browser"); //$NON-NLS-1$
		}
		
		@Override
		public boolean changed(final int event, final ITool tool) {
			if (event == MOVING_FROM) {
				return false;
			}
			return true;
		}
		
		@Override
		protected void run(final IRDataAdapter r,
				final IProgressMonitor monitor) throws CoreException {
			boolean current;
			synchronized (fProcessLock) {
				current = (r.getTool() != fProcess);
			}
			final IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getViewSite().getService(IWorkbenchSiteProgressService.class);
			if (current && progressService != null) {
				progressService.incrementBusy();
			}
			try {
				fInputUpdater.fForceOnWorkspaceChange = true;
				r.refreshWorkspaceData(RWorkspace.REFRESH_COMPLETE, monitor);
			}
			finally {
				if (current && progressService != null) {
					progressService.decrementBusy();
				}
			}
		}
		
	}
	
	private class RefreshHandler extends ToolRetargetableHandler {
		
		private final ElementUpdater fUpdater = new ElementUpdater(IWorkbenchCommandConstants.FILE_REFRESH);
		
		public RefreshHandler() {
			super(ObjectBrowserView.this, ObjectBrowserView.this.getSite());
			init();
		}
		
		@Override
		protected Object doExecute(final ExecutionEvent event) {
			scheduleUpdateAll();
			return null;
		}
		
		@Override
		protected void doRefresh() {
			super.doRefresh();
			fUpdater.schedule();
		}
		
	}
	
	private class FilterUserspaceHandler extends AbstractHandler implements IElementUpdater {
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			fFilterUserspace = !fFilterUserspace;
			fSettings.put(FILTER_USERSPACEONLY_SETTINGS_KEY, fFilterUserspace);
			fInputUpdater.forceUpdate(fProcess);
			fInputUpdater.schedule();
			return null;
		}
		
		@Override
		public void updateElement(final UIElement element, final Map parameters) {
			element.setChecked(fFilterUserspace);
		}
		
	}
	
	private class FilterInternalHandler extends AbstractHandler implements IElementUpdater {
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			fFilterIncludeInternal = !fFilterIncludeInternal;
			fSettings.put(FILTER_INTERNALINCLUDE_SETTINGS_KEY, fFilterIncludeInternal);
			updateFilter();
			return null;
		}
		
		@Override
		public void updateElement(final UIElement element, final Map parameters) {
			element.setChecked(fFilterIncludeInternal);
		}
		
	}
	
	private class SortByTypeHandler extends AbstractHandler implements IElementUpdater {
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			fSortByType = !fSortByType;
			fSettings.put(SORT_BYTYPE_SETTINGS_KEY, fSortByType);
			updateSorter();
			return null;
		}
		
		@Override
		public void updateElement(final UIElement element, final Map parameters) {
			element.setChecked(fSortByType);
		}
		
	}
	
	private class ToggleAutoRefreshHandler extends ToolRetargetableHandler implements IElementUpdater {
		
		private boolean fCurrentState;
		
		public ToggleAutoRefreshHandler() {
			super(ObjectBrowserView.this, ObjectBrowserView.this.getSite());
			init();
		}
		
		@Override
		protected void setBaseEnabled(final boolean state) {
			super.setBaseEnabled(state);
		}
		@Override
		public void updateElement(final UIElement element, final Map parameters) {
			fCurrentState = false;
			if (fProcess != null) {
				final RWorkspace workspace = fProcess.getWorkspaceData();
				if (workspace != null) {
					fCurrentState = workspace.isAutoRefreshEnabled();
				}
			}
			element.setChecked(fCurrentState);
		}
		
		@Override
		protected Object doExecute(final ExecutionEvent event) {
			final RWorkspace workspace = (RWorkspace) getCheckedTool().getWorkspaceData();
			if (workspace != null) {
				fCurrentState = !fCurrentState;
				workspace.setAutoRefresh(fCurrentState);
			}
			return null;
		}
		
	}
	
	private class CopyElementNameHandler extends AbstractHandler {
		
		@Override
		public void setEnabled(final Object evaluationContext) {
			setBaseEnabled(!fTreeViewer.getSelection().isEmpty());
		}
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			if (!UIAccess.isOkToUse(fTreeViewer)) {
				return null;
			}
			final ITreeSelection selection = (ITreeSelection) fTreeViewer.getSelection();
			if (selection == null) {
				return null;
			}
			final TreePath[] treePaths = selection.getPaths();
			if (treePaths.length == 0) {
				return null;
			}
			final List<String> list = new ArrayList<String>();
			int length = 0;
			int failed = 0;
			for (int i = 0; i < treePaths.length; i++) {
				final IElementName elementName = getElementName(treePaths[i]);
				final String name = (elementName != null) ? elementName.getDisplayName() : null;
				if (name != null) {
					length += name.length();
					list.add(name);
				}
				else {
					failed++;
				}
			}
			
			String text;
			if (list.size() > 0) {
				final StringBuilder sb = new StringBuilder(length + list.size()*2);
				for (final String name : list) {
					sb.append(name);
					sb.append(", "); //$NON-NLS-1$
				}
				text = sb.substring(0, sb.length()-2);
			}
			else {
				text = ""; //$NON-NLS-1$
			}
			if (failed > 0) {
				fStatusLine.setMessage(IStatus.WARNING, "Could not copy element name for " + failed + " of " + treePaths.length + " objects.");
			}
			DNDUtil.setContent(getClipboard(), 
					new String[] { text }, 
					new Transfer[] { TextTransfer.getInstance() } );
			
			return null;
		}
		
	}
	
	private class PrintHandler extends AbstractHandler {
		
		
		public PrintHandler() {
		}
		
		
		@Override
		public void setEnabled(final Object evaluationContext) {
			final ToolProcess process = fProcess;
			setBaseEnabled(process != null && !process.isTerminated()
					&& ((ITreeSelection) fTreeViewer.getSelection()).size() == 1);
		}
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			if (!UIAccess.isOkToUse(fTreeViewer)) {
				return null;
			}
			final ITreeSelection selection = (ITreeSelection) fTreeViewer.getSelection();
			if (selection == null) {
				return null;
			}
			final TreePath[] treePaths = selection.getPaths();
			if (treePaths.length != 1) {
				return null;
			}
			final RElementName elementName = getElementName(treePaths[0]);
			if (elementName != null) {
				final String name = RElementName.createDisplayName(elementName, RElementName.DISPLAY_NS_PREFIX | RElementName.DISPLAY_EXACT);
				
				final ToolProcess process = fProcess;
				try {
					final ToolController controller = NicoUITools.accessController(RTool.TYPE, process);
					controller.submit(name, SubmitType.TOOLS);
				}
				catch (final CoreException e) {
				}
			}
			
			return null;
		}
		
	}
	
	private class DeleteHandler extends AbstractHandler {
		
		
		private class DeleteRunnable extends AbstractRDataRunnable implements IToolRunnableDecorator {
			
			private final List<String> fNames;
			private final List<String> fCommands;
			private final Set<IElementName> fTopEnvirs;
			
			public DeleteRunnable(final List<String> names, final List<String> commands,
					final Set<IElementName> topEnvirs) {
				super("r/objectbrowser/delete", "Delete Elements"); //$NON-NLS-1$
				fNames = names;
				fCommands = commands;
				fTopEnvirs = topEnvirs;
			}
			
			@Override
			public Image getImage() {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			}
			
			@Override
			public boolean changed(final int event, final ITool tool) {
				if (event == MOVING_FROM) {
					return false;
				}
				return true;
			}
			
			@Override
			protected void run(final IRDataAdapter r,
					final IProgressMonitor monitor) throws CoreException {
				try {
					for (int i = 0; i < fNames.size(); i++) {
						r.evalVoid(fCommands.get(i), monitor);
					}
				}
				finally {
					r.briefAboutChange(fTopEnvirs, 0);
				}
			}
			
		}
		
		
		public DeleteHandler() {
		}
		
		
		@Override
		public void setEnabled(final Object evaluationContext) {
			final ToolProcess process = fProcess;
			setBaseEnabled(process != null && !process.isTerminated()
					&& ((ITreeSelection) fTreeViewer.getSelection()).size() >= 1);
		}
		
		@Override
		public Object execute(final ExecutionEvent event) throws ExecutionException {
			if (!UIAccess.isOkToUse(fTreeViewer)) {
				return null;
			}
			final ITreeSelection selection = (ITreeSelection) fTreeViewer.getSelection();
			if (selection == null) {
				return null;
			}
			final TreePath[] treePaths = selection.getPaths();
			Arrays.sort(treePaths, new Comparator<TreePath>() {
				@Override
				public int compare(final TreePath o1, final TreePath o2) {
					return o1.getSegmentCount()-o2.getSegmentCount();
				}
			});
			final IElementComparer comparer = new IElementComparer() {
				@Override
				public int hashCode(final Object e) {
					return e.hashCode();
				}
				@Override
				public boolean equals(final Object e1, final Object e2) {
					return (e1 == e2);
				}
			};
			final List<String> commands = new ArrayList<String>(treePaths.length);
			final List<String> names = new ArrayList<String>(treePaths.length);
			final Set<IElementName> topEnvirs = new HashSet<IElementName>(treePaths.length);
			ITER_ELEMENTS: for (int i = 0; i < treePaths.length; i++) {
				for (int j = 0; j < i; j++) {
					if (treePaths[j] != null && treePaths[i].startsWith(treePaths[j], comparer)) {
						treePaths[i] = null;
						continue ITER_ELEMENTS;
					}
				}
				
				final TreePath treePath = treePaths[i];
				final ICombinedRElement element = (ICombinedRElement) treePath.getLastSegment();
				final ICombinedRElement parent = element.getModelParent();
				
				final RElementName elementName = getElementName(treePath);
				if (parent != null && elementName != null) {
					final RElementName topName;
					switch (parent.getRObjectType()) {
					case RObject.TYPE_ENV: {
						final RElementName envirName = (treePath.getSegmentCount() > 1) ? getElementName(treePath.getParentPath()) : parent.getElementName();
						final IElementName itemName = element.getElementName();
						topName = elementName.getNamespace();
						if (envirName != null) { // elementName ok => segmentName ok
							commands.add("rm(`"+itemName.getSegmentName()+"`,"+ //$NON-NLS-1$ 
									"pos="+RElementName.createDisplayName(envirName, RElementName.DISPLAY_NS_PREFIX | RElementName.DISPLAY_EXACT)+ //$NON-NLS-1$
									")"); //$NON-NLS-1$
							names.add(elementName.getDisplayName());
							topEnvirs.add(topName);
							continue ITER_ELEMENTS;
						}
						break; }
					case RObject.TYPE_LIST:
					case RObject.TYPE_DATAFRAME:
					case RObject.TYPE_S4OBJECT:
						topName = elementName.getNamespace();
						final String name = RElementName.createDisplayName(elementName, RElementName.DISPLAY_EXACT);
						commands.add("with("+RElementName.createDisplayName(topName, RElementName.DISPLAY_NS_PREFIX)+","+ //$NON-NLS-1$ 
								name+"<-NULL"+ //$NON-NLS-1$
								")"); //$NON-NLS-1$
						names.add(elementName.getDisplayName());
						topEnvirs.add(topName);
						continue ITER_ELEMENTS;
					}
				}
				
				final StringBuilder message = new StringBuilder("Selection contains unsupported object");
				if (elementName != null) {
					message.append("\n\t"); //$NON-NLS-1$
					message.append(elementName.getDisplayName());
				}
				else {
					message.append("."); //$NON-NLS-1$
				}
				MessageDialog.openError(getSite().getShell(), "Delete", message.toString());
				return null;
			}
			
			final StringBuilder message = new StringBuilder(names.size() == 1 ?
					"Are you sure you want to deleting the object" :
					NLS.bind("Are you sure you want to delete these {0} objects", names.size()));
			final int show = (names.size() > 5) ? 3 : names.size();
			for (int i = 0; i < show; i++) {
				message.append("\n\t"); //$NON-NLS-1$
				message.append(names.get(i));
			}
			if (show < names.size()) {
				message.append("\n\t..."); //$NON-NLS-1$
			}
			
			final ToolProcess process = fProcess;
			if (ToolMessageDialog.openConfirm(process, getSite().getShell(), "Delete", message.toString())) {
				process.getQueue().add(new DeleteRunnable(names, commands, topEnvirs));
			}
			
			return null;
		}
		
	}
	
	private class ContentJob extends Job implements ToolWorkspace.Listener {
		
		/** true if RefreshR is running */
		private boolean fForceOnWorkspaceChange;
		/** the process to update */
		private RProcess fUpdateProcess;
		/** the process of last update */
		private RProcess fLastProcess;
		/** update all environment */
		private boolean fForce;
		/** environments to update, if force is false*/
		private final Set<ICombinedREnvironment> fUpdateSet = new HashSet<ICombinedREnvironment>();
		
		private List<? extends ICombinedREnvironment> fInput;
		private ICombinedRElement[] fUserspaceInput;
		
		private volatile boolean fIsScheduled;
		
		
		public ContentJob() {
			super("R Object Browser Update");
			setSystem(true);
			setUser(false);
		}
		
		
		@Override
		public void propertyChanged(final ToolWorkspace workspace, final Map<String, Object> properties) {
			final RWorkspace rWorkspace = (RWorkspace) workspace;
			if (properties.containsKey("REnvironments")) {
				if (fForceOnWorkspaceChange) {
					fForceOnWorkspaceChange = false;
					final RProcess process = rWorkspace.getProcess();
					forceUpdate(process);
					schedule();
				}
				else {
					final List<RWorkspace.ICombinedREnvironment> envirs = (List<RWorkspace.ICombinedREnvironment>) properties.get("REnvironments");
					schedule(rWorkspace.getProcess(), envirs);
				}
			}
			
			final Object autorefresh = properties.get("AutoRefresh.enabled");
			if (autorefresh instanceof Boolean) {
				UIAccess.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (fProcess != rWorkspace.getProcess()) {
							return;
						}
						updateAutoRefresh(((Boolean) autorefresh).booleanValue());
					}
				});
			}
			else { // autorefresh already updates dirty
				final Object dirty = properties.get("RObjectDB.dirty");
				if (dirty instanceof Boolean) {
					UIAccess.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (fProcess != rWorkspace.getProcess()) {
								return;
							}
							updateDirty(((Boolean) dirty).booleanValue());
						}
					});
				}
			}
		}
		
		public void forceUpdate(final RProcess process) {
			synchronized (fProcessLock) {
				if ((process != null) ? (fProcess != process) : (fProcess != null)) {
					return;
				}
				fUpdateProcess = process;
				fForce = true;
				fUpdateSet.clear();
			}
		}
		
		public void schedule(final RProcess process, final List<RWorkspace.ICombinedREnvironment> envirs) {
			if (envirs != null && process != null) {
				synchronized (fProcessLock) {
					if (fProcess == null || fProcess != process) {
						return;
					}
					fUpdateProcess = process;
					if (!fForce) {
						fUpdateSet.removeAll(envirs);
						fUpdateSet.addAll(envirs);
					}
				}
			}
			schedule();
		}
		
		@Override
		public boolean shouldSchedule() {
			fIsScheduled = true;
			return true;
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			if (!fIsScheduled) {
				return Status.CANCEL_STATUS;
			}
			fIsScheduled = false;
			
			final IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getViewSite().getService(IWorkbenchSiteProgressService.class);
			if (progressService != null) {
				progressService.incrementBusy();
			}
			
			try {
				final List<ICombinedREnvironment> updateList;
				final boolean force;
				final boolean updateInput;
				final RProcess process;
				synchronized (fProcessLock) {
					force = fForce;
					updateInput = (force || fUpdateProcess != null);
					process = (updateInput) ? fUpdateProcess : fProcess;
					if ((process != null) ? (fProcess != process) : (fProcess != null)
							|| fForceOnWorkspaceChange) {
						return Status.OK_STATUS;
					}
					updateList = new ArrayList<ICombinedREnvironment>(fUpdateSet.size());
					updateList.addAll(fUpdateSet);
					fUpdateSet.clear();
					fUpdateProcess = null;
					fForce = false;
				}
				
				final ContentInput input = createHandler(process, updateInput);
				
				// Update input and refresh
				final List<ICombinedREnvironment> toUpdate;
				if (updateInput) {
					toUpdate = updateInput((!force) ? updateList : null, process, input);
				}
				else {
					toUpdate = null;
				}
				
				if (fInput != null) {
					prescan(input);
				}
				else if (process != null) {
					input.processChanged = false;
				}
				
				synchronized (fProcessLock) {
					if ((process != null) ? (fProcess != process) : (fProcess != null)) {
						return Status.CANCEL_STATUS;
					}
					if ((!input.processChanged && fIsScheduled) || monitor.isCanceled()) {
						fUpdateSet.addAll(updateList);
						fForce |= force;
						if (updateInput && fUpdateProcess == null) {
							fUpdateProcess = process;
						}
						return Status.CANCEL_STATUS;
					}
				}
				UIAccess.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						if ((process != null) ? (fProcess != process) : (fProcess != null)) {
							return;
						}
						fActiveInput = input;
						updateViewer(toUpdate);
					}
				});
				
				if (fInput != null) {
					fLastProcess = process;
				}
				else {
					fLastProcess = null;
				}
				return Status.OK_STATUS;
			}
			finally {
				if (progressService != null) {
					progressService.decrementBusy();
				}
			}
		}
		
		private ContentInput createHandler(final RProcess process, final boolean updateInput) {
			final boolean processChanged = ((process != null) ? process != fLastProcess : fLastProcess != null);
			final boolean filterInternal = !fFilterIncludeInternal;
			final String filterText = fFilterText;
			IModelElement.Filter envFilter;
			IModelElement.Filter otherFilter;
			if (filterText != null && filterText.length() > 0) {
				final SearchPattern filterPattern = new SearchPattern(SearchPattern.RULE_EXACT_MATCH
						| SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_CAMELCASE_MATCH
						| SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_BLANK_MATCH);
				filterPattern.setPattern(filterText);
				envFilter = new ContentFilter(filterInternal, filterPattern);
				otherFilter = (filterInternal) ? new ContentFilter(filterInternal, null) : null;
			}
			else if (filterInternal) {
				envFilter = new ContentFilter(filterInternal, null);
				otherFilter = new ContentFilter(filterInternal, null);
			}
			else {
				envFilter = null;
				otherFilter = null;
			}
			return new ContentInput(processChanged, updateInput, fFilterUserspace, envFilter, otherFilter);
		}
		
		private List<ICombinedREnvironment> updateInput(final List<ICombinedREnvironment> updateList, 
				final RProcess process, final ContentInput input) {
			if (process != null) {
				final List<? extends ICombinedREnvironment> oldInput = fInput;
				final RWorkspace workspaceData = process.getWorkspaceData();
				fInput = workspaceData.getRSearchEnvironments();
				if (fInput == null || fInput.size() == 0) {
					fInput = null;
					fUserspaceInput = null;
					return null;
				}
				input.searchEnvirs = fInput;
				// If search path (environments) is not changed and not in force mode, refresh only the updated entries
				List<ICombinedREnvironment> updateEntries = null;
				TRY_PARTIAL : if (!input.filterUserspace
						&& fInput != null && oldInput != null && fInput.size() == oldInput.size()
						&& updateList != null && updateList.size() < fInput.size() ) {
					updateEntries = new ArrayList<ICombinedREnvironment>(updateList.size());
					for (int i = 0; i < fInput.size(); i++) {
						final ICombinedREnvironment envir = fInput.get(i);
						if (envir.equals(oldInput.get(i))) {
							if (updateList.remove(envir)) {
								updateEntries.add(envir);
							}
						}
						else { // search path is changed
							updateEntries = null;
							break TRY_PARTIAL;
						}
					}
					if (!updateList.isEmpty()) {
						updateEntries = null;
						break TRY_PARTIAL;
					}
				}
				
				// Prepare Userspace filter
				if (input.filterUserspace) {
					int length = 0;
					final List<ICombinedREnvironment> userEntries = new ArrayList<ICombinedREnvironment>(fInput.size());
					for (final ICombinedREnvironment env : fInput) {
						if (env.getSpecialType() > 0 && env.getSpecialType() <= REnvironment.ENVTYPE_PACKAGE) {
							continue;
						}
						userEntries.add(env);
						length += env.getLength();
					}
					final List<IModelElement> elements = new ArrayList<IModelElement>(length);
					for (final ICombinedREnvironment entry : userEntries) {
						elements.addAll(entry.getModelChildren((IModelElement.Filter) null));
					}
					
					final ICombinedRElement[] array = elements.toArray(new ICombinedRElement[elements.size()]);
					Arrays.sort(array, ELEMENTNAME_COMPARATOR);
					fUserspaceInput = array;
				}
				else {
					fUserspaceInput = null;
				}
				return updateEntries;
			}
			else {
				fInput = null;
				fUserspaceInput = null;
				return null;
			}
		}
		
		private void prescan(final ContentInput input) {
			// Prescan filter
			if (!input.filterUserspace) {
				input.rootElements = fInput.toArray(new ICombinedRElement[fInput.size()]);
				if (input.envFilter != null) {
					input.envirElements = new HashMap<ICombinedRElement, ICombinedRElement[]>(fInput.size());
					for (int i = 0; i < fInput.size(); i++) {
						final ICombinedREnvironment envir = fInput.get(i);
						final List<? extends IModelElement> children = envir.getModelChildren(input.envFilter);
						input.envirElements.put(envir, children.toArray(new ICombinedRElement[children.size()]));
					}
				}
			}
			else {
				if (input.envFilter != null) {
					final List<ICombinedRElement> list = new ArrayList<ICombinedRElement>(fUserspaceInput.length);
					for (int i = 0; i < fUserspaceInput.length; i++) {
						if (input.envFilter.include(fUserspaceInput[i])) {
							list.add(fUserspaceInput[i]);
						}
					}
					input.rootElements = list.toArray(new ICombinedRElement[list.size()]);
				}
				else {
					input.rootElements = fUserspaceInput;
				}
			}
		}
		
	}
	
	private class ContentProvider implements ITreeContentProvider {
		
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}
		
		@Override
		public Object[] getElements(final Object inputElement) {
			if (fActiveInput == null || fActiveInput.rootElements == null) {
				return new Object[0];
			}
			return fActiveInput.rootElements;
		}
		
		@Override
		public Object getParent(final Object element) {
			if (element instanceof ICombinedRElement) {
				return ((ICombinedRElement) element).getModelParent();
			}
			return null;
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			if (element instanceof ICombinedRElement) {
				final ICombinedRElement object = (ICombinedRElement) element;
				switch (object.getRObjectType()) {
				case RObject.TYPE_DATAFRAME:
					final RDataFrame dataframe = (RDataFrame) object;
					return (dataframe.getColumnCount() > 0);
				case RObject.TYPE_ENV:
					if (fActiveInput != null && fActiveInput.envirElements != null) {
						final ICombinedRElement[] children = fActiveInput.envirElements.get(object);
						if (children != null) {
							return (children.length > 0);
						}
					}
					return object.hasModelChildren(fActiveInput.envFilter);
				case RObject.TYPE_REFERENCE: {
					final RObject realObject = ((RReference) object).getResolvedRObject();
					if (realObject != null) {
						fUsedReferences.add((RReference) object);
						return hasChildren(realObject);
					}
					return false; }
				default:
					return object.hasModelChildren(fActiveInput.otherFilter);
				}
			}
			return false;
		}
		
		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof ICombinedRElement) {
				final ICombinedRElement object = (ICombinedRElement) parentElement;
				switch (object.getRObjectType()) {
				case RObject.TYPE_ENV: {
					if (fActiveInput != null && fActiveInput.envirElements != null) {
						final ICombinedRElement[] children = fActiveInput.envirElements.get(object);
						if (children != null) {
							return children;
						}
					}
					return object.getModelChildren(fActiveInput.envFilter).toArray(); }
				case RObject.TYPE_REFERENCE: {
					final RObject realObject = ((RReference) object).getResolvedRObject();
					if (realObject != null) {
						return getChildren(realObject);
					}
					return new Object[0]; }
				default:
					return object.getModelChildren(fActiveInput.otherFilter).toArray();
				}
			}
			return null;
		}
		
		@Override
		public void dispose() {
		}
		
	}
	
	
	private class TreeElementSelection extends TreeSelectionProxy implements IElementNameProvider {
		
		public TreeElementSelection(ITreeSelection selection) {
			super(selection);
		}
		
		@Override
		public IElementName getElementName(Object selectionElement) {
			if (selectionElement instanceof TreePath) { 
				return ObjectBrowserView.this.getElementName((TreePath) selectionElement);
			}
			return null;
		}
		
	}
	
	private class TreeSelectionProvider extends PostSelectionProviderProxy {
		
		public TreeSelectionProvider() {
			super(fTreeViewer);
		}
		
		@Override
		protected ISelection getSelection(ISelection originalSelection) {
			return new TreeElementSelection((ITreeSelection) originalSelection);
		}
		
	}
	
	private class HoverManager extends ColumnHoverManager {
		
		HoverManager() {
			super(fTreeViewer, fTokenOwner, new RElementInfoHoverCreator());
			
			final ColumnHoverStickyManager stickyManager = new ColumnHoverStickyManager(fTokenOwner, this);
			getInternalAccessor().setInformationControlReplacer(stickyManager);
		}
		
		@Override
		protected Object prepareHoverInformation(final ViewerCell cell) {
			final TreePath treePath = cell.getViewerRow().getTreePath();
			final RElementName elementName = getElementName(treePath);
			if (elementName != null && elementName.getNamespace() != null) {
				return elementName;
			}
			return null;
		}
		
		@Override
		protected Object getHoverInformation(final Object element) {
			if (element instanceof RElementName && fProcess != null) {
				final RElementInfoTask updater = new RElementInfoTask((RElementName) element);
				return updater.load(fProcess, getSubjectControl());
			}
			return null;
		}
		
	}
	
	private class StatusLine {
		
		private boolean fMessageSetted;
		
		public void setMessage(final int status, final String message) {
			boolean error = false;
			final IStatusLineManager manager = getViewSite().getActionBars().getStatusLineManager();
			Image image;
			switch (status) {
			case IStatus.INFO:
				image = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO);
				break;
			case IStatus.WARNING:
				image = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
				break;
			case IStatus.ERROR:
				image = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
				error = true;
				break;
			default:
				image = null;
			}
			if (manager != null) {
				fMessageSetted = true;
				if (error) {
					manager.setErrorMessage(image, message);
				}
				else {
					manager.setMessage(image, message);
				}
			}
		}
		
		void cleanStatusLine() {
			if (fMessageSetted) {
				final IStatusLineManager manager = getViewSite().getActionBars().getStatusLineManager();
				if (manager != null) {
					manager.setMessage(null);
					manager.setErrorMessage(null);
					fMessageSetted = false;
				}
				
			}
		}
		
	}
	
	
	private TreeViewer fTreeViewer;
	private TreeSelectionProvider fTreeSelectionProvider;
	private ColumnWidgetTokenOwner fTokenOwner;
	private Clipboard fClipboard;
	private final StatusLine fStatusLine = new StatusLine();
	
	private IDialogSettings fSettings;
	
	private RProcess fProcess; // note: we write only in ui thread
	private final Object fProcessLock = new Object();
	private IToolRegistryListener fToolRegistryListener;
	private final FastList<IToolRetargetable> fToolListenerList =
			new FastList<IToolRetargetable>(IToolRetargetable.class);
	
	private final RefreshWorkspaceR fManualRefreshRunnable = new RefreshWorkspaceR();
	
	private final ContentJob fInputUpdater = new ContentJob();
	
	private ContentInput fActiveInput;
	/** References used by the viewer. Use only in UI thread */
	private Set<RReference> fUsedReferences = new HashSet<RReference>();
	
	private boolean fFilterUserspace;
	private boolean fFilterIncludeInternal;
	private String fFilterText;
	private boolean fSortByType;
	
	private SearchContributionItem fSearchTextItem;
	
	private boolean fFilterUserspaceActivated;
	
	private CopyElementNameHandler fCopyElementNameHandler;
	private DeleteHandler fDeleteElementHandler;
	private PrintHandler fPrintElementHandler;
	private IHandler2 fOpenInEditorHandler;
	
	private Object fCurrentInfoObject;
	
	private ColumnHoverManager fHoveringController;
	
	private AbstractHandler fSearchStartHandler;
	
	private HandlerContributionItem fRefreshToolbarItem;
	private HandlerContributionItem fRefreshMenuItem;
	private boolean fRefreshDirtyIndicator;
	
	
	public ObjectBrowserView() {
	}
	
	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);
		
		fSettings = DialogUtil.getDialogSettings(RUIPlugin.getDefault(), "ObjectBrowser");
		
		fFilterUserspaceActivated = fFilterUserspace = fSettings.getBoolean(FILTER_USERSPACEONLY_SETTINGS_KEY);
		fFilterIncludeInternal = fSettings.getBoolean(FILTER_INTERNALINCLUDE_SETTINGS_KEY);
		fSortByType = fSettings.getBoolean(SORT_BYTYPE_SETTINGS_KEY);
	}
	
	@Override
	public void saveState(final IMemento memento) {
		super.saveState(memento);
		
		fSettings = DialogUtil.getDialogSettings(RUIPlugin.getDefault(), "ObjectBrowser");
	}
	
	
	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(LayoutUtil.applySashDefaults(new GridLayout(), 1));
		
		fTreeViewer = new TreeViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		fTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		fTreeViewer.setUseHashlookup(true);
		
		fTreeSelectionProvider = new TreeSelectionProvider();
		
		fTreeViewer.setLabelProvider(new RLabelProvider(RLabelProvider.COUNT) {
			@Override
			protected String getEnvCountInfo(final ICombinedREnvironment envir) {
				final StringBuilder textBuilder = getTextBuilder();
				textBuilder.append(" ("); //$NON-NLS-1$
				if (fActiveInput != null && fActiveInput.envirElements != null) {
					final ICombinedRElement[] elements = fActiveInput.envirElements.get(envir);
					if (elements != null) {
						textBuilder.append(elements.length);
						textBuilder.append('/');
					}
				}
				textBuilder.append(envir.getLength());
				textBuilder.append(')');
				return textBuilder.toString();
			}
		});
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.size() != 1) {
					return;
				}
				final Object element = selection.getFirstElement();
				if (element instanceof RObject) {
					final RObject object = (RObject) element;
					if (object != null) {
						switch (object.getRObjectType()) {
						case RObject.TYPE_ENV:
						case RObject.TYPE_LIST:
						case RObject.TYPE_DATAFRAME:
						case RObject.TYPE_S4OBJECT:
						case RObject.TYPE_REFERENCE:
							fTreeViewer.setExpandedState(element, !fTreeViewer.getExpandedState(element));
						}
					}
				}
			}
		});
		fTreeSelectionProvider.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				updateSelectionInfo((ITreeSelection) event.getSelection());
			}
		});
		fTreeViewer.setContentProvider(new ContentProvider());
		updateSorter();
		fTreeViewer.setInput(this);
		
		fTokenOwner = new ColumnWidgetTokenOwner(fTreeViewer);
		fHoveringController = new HoverManager();
		fHoveringController.setSizeConstraints(100, 12, false, true);
		fHoveringController.install(fTreeViewer.getTree());
		
		createActions();
		hookContextMenu();
		getSite().setSelectionProvider(fTreeSelectionProvider);
		
		// listen on console changes
		final IToolRegistry toolRegistry = NicoUI.getToolRegistry();
		fToolRegistryListener = new IToolRegistryListener() {
			@Override
			public void toolSessionActivated(final ToolSessionUIData sessionData) {
				final ToolProcess process = sessionData.getProcess();
				UIAccess.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						connect(process);
					}
				});
			}
			@Override
			public void toolTerminated(final ToolSessionUIData sessionData) {
				final ToolProcess process = sessionData.getProcess();
				UIAccess.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						if (fProcess != null && fProcess == process) {
							connect(null);
						}
					}
				});
			}
		};
		toolRegistry.addListener(fToolRegistryListener, getViewSite().getPage());
		connect(toolRegistry.getActiveToolSession(getViewSite().getPage()).getProcess());
	}
	
	private void createActions() {
		final IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
		final IContextService contexts = (IContextService) getSite().getService(IContextService.class);
		
		contexts.activateContext("de.walware.ecommons.base.contexts.StructuredElementViewer"); //$NON-NLS-1$
		
		final RefreshHandler refreshHandler = new RefreshHandler();
		handlerService.activateHandler(IWorkbenchCommandConstants.FILE_REFRESH, refreshHandler);
		final CollapseAllHandler collapseAllHandler = new CollapseAllHandler(fTreeViewer);
		handlerService.activateHandler(CollapseAllHandler.COMMAND_ID, collapseAllHandler);
		fCopyElementNameHandler = new CopyElementNameHandler();
		handlerService.activateHandler(ISourceEditorCommandIds.COPY_ELEMENT_NAME, fCopyElementNameHandler);
		handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, fCopyElementNameHandler);
		fDeleteElementHandler = new DeleteHandler();
		handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_DELETE, fDeleteElementHandler);
		fPrintElementHandler = new PrintHandler();
		handlerService.activateHandler(RunPrintInR.COMMAND_ID, fPrintElementHandler);
		final InformationDispatchHandler infoHandler = new InformationDispatchHandler(fTokenOwner);
		handlerService.activateHandler(InformationDispatchHandler.COMMAND_ID, infoHandler);
		fOpenInEditorHandler = new OpenInEditorHandler();
		handlerService.activateHandler(OPEN_COMMAND_ID, fOpenInEditorHandler);
		
		fSearchTextItem = new SearchContributionItem("search.text", true) { //$NON-NLS-1$
			@Override
			protected void search() {
				fFilterText = getText();
				updateFilter();
			}
		};
		fSearchTextItem.setToolTip("Filter Elements");
		fSearchTextItem.setSizeControl(fTreeViewer.getControl().getParent());
		fSearchTextItem.setResultControl(fTreeViewer.getTree());
		
		fSearchStartHandler = new AbstractHandler() {
			@Override
			public Object execute(final ExecutionEvent arg0) {
				fSearchTextItem.show();
				return null;
			}
		};
		handlerService.activateHandler(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, fSearchStartHandler);
		// add next/previous handler
		
		final ToggleAutoRefreshHandler autoRefreshHandler = new ToggleAutoRefreshHandler();
		
		final IActionBars actionBars = getViewSite().getActionBars();
		final IMenuManager viewMenu = actionBars.getMenuManager();
		final IToolBarManager viewToolbar = actionBars.getToolBarManager();
		
		viewMenu.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Filter.OnlyUserspace", HandlerContributionItem.NO_COMMAND_ID, null, //$NON-NLS-1$
				null, null, null,
				"Show non-&package Variables only", null, null,
				HandlerContributionItem.STYLE_CHECK, null, false),
				new FilterUserspaceHandler() ));
		viewMenu.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Filter.IncludeInternal", HandlerContributionItem.NO_COMMAND_ID, null, //$NON-NLS-1$
				null, null, null,
				"Show &Internal Variables ('.*')", null, null,
				HandlerContributionItem.STYLE_CHECK, null, false),
				new FilterInternalHandler() ));
		viewMenu.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Sort.ByType", HandlerContributionItem.NO_COMMAND_ID, null, //$NON-NLS-1$
				null, null, null,
				"Sort by &Type", null, null,
				HandlerContributionItem.STYLE_CHECK, null, false),
				new SortByTypeHandler() ));
		
		viewMenu.add(new Separator());
		final HandlerContributionItem autoRefreshItem = new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				null, HandlerContributionItem.NO_COMMAND_ID, null, 
				null, null, null,
				"Refresh &automatically", null, null,
				HandlerContributionItem.STYLE_CHECK, null, false),
				autoRefreshHandler );
		viewMenu.add(autoRefreshItem);
		fRefreshMenuItem = new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				null, IWorkbenchCommandConstants.FILE_REFRESH, null, 
				StatetImages.getDescriptor(StatetImages.TOOL_REFRESH), StatetImages.getDescriptor(StatetImages.TOOLD_REFRESH), null,
				"&Refresh", null, null,
				HandlerContributionItem.STYLE_PUSH, null, false),
				refreshHandler );
		viewMenu.add(fRefreshMenuItem);
		
		viewMenu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				autoRefreshItem.update();
			}
		});
		
		viewToolbar.add(fSearchTextItem);
		viewToolbar.add(new Separator());
		fRefreshToolbarItem = new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Refresh", IWorkbenchCommandConstants.FILE_REFRESH, null, //$NON-NLS-1$
				StatetImages.getDescriptor(StatetImages.TOOL_REFRESH), StatetImages.getDescriptor(StatetImages.TOOLD_REFRESH), null,
				null, null, null,
				HandlerContributionItem.STYLE_PUSH, null, false),
				refreshHandler);
		fRefreshToolbarItem.setVisible(false);
		viewToolbar.add(fRefreshToolbarItem);
		viewToolbar.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Collapse.All", CollapseAllHandler.COMMAND_ID, null, //$NON-NLS-1$
				null, null, null,
				null, null, null,
				HandlerContributionItem.STYLE_PUSH, null, false),
				collapseAllHandler));
	}
	
	private void updateAutoRefresh(final boolean enabled) {
		if (fProcess == null || fProcess.isTerminated()) {
			return;
		}
		
		if (enabled) {
			updateDirty(false);
		}
		else {
			updateDirty(fProcess != null && !fProcess.isTerminated()
					&& fProcess.getWorkspaceData().isROBjectDBDirty());
		}
		
		if (fRefreshToolbarItem.isVisible() != enabled) {
			return;
		}
		fRefreshToolbarItem.setVisible(!enabled);
		final IContributionManager manager = fRefreshToolbarItem.getParent();
		manager.update(true);
		fSearchTextItem.resize();
	}
	
	private void updateDirty(final boolean enabled) {
		if (fRefreshDirtyIndicator == enabled) {
			return;
		}
		final ImageDescriptor icon = (enabled) ?
				RUIPlugin.getDefault().getImageRegistry().getDescriptor(RUIPlugin.IMG_LOCTOOL_REFRESH_RECOMMENDED) :
				StatetImages.getDescriptor(StatetImages.TOOL_REFRESH);
		fRefreshToolbarItem.setIcon(icon);
		fRefreshMenuItem.setIcon(icon);
		fRefreshDirtyIndicator = enabled;
	}
	
	
	private void hookContextMenu() {
		final MenuManager menuManager = new MenuManager("ContextMenu",
				"de.walware.statet.r.menus.RObjectBrowserContextMenu" ); //$NON-NLS-1$
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager m) {
				contextMenuAboutToShow(m);
			}
		});
		final Menu contextMenu = menuManager.createContextMenu(fTreeViewer.getTree());
		fTreeViewer.getTree().setMenu(contextMenu);
		getSite().registerContextMenu(menuManager, fTreeViewer);
	}
	
	private void contextMenuAboutToShow(final IMenuManager m) {
		m.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				null, OPEN_COMMAND_ID, null,
				null, null, null,
				"Open in Data &Viewer", null, null,
				HandlerContributionItem.STYLE_PUSH, null, true), fOpenInEditorHandler));
		
		m.add(new Separator("edit"));
		m.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Copy.ElementName", ISourceEditorCommandIds.COPY_ELEMENT_NAME, null, //$NON-NLS-1$
				null, null, null,
				null, null, null,
				HandlerContributionItem.STYLE_PUSH, null, false),
				fCopyElementNameHandler));
		m.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				"Delete", IWorkbenchCommandConstants.EDIT_DELETE, null, //$NON-NLS-1$
				null, null, null,
				null, null, null,
				HandlerContributionItem.STYLE_PUSH, null, false),
				fDeleteElementHandler));
		m.add(new Separator());
		m.add(new HandlerContributionItem(new CommandContributionItemParameter(getSite(),
				null, RunPrintInR.COMMAND_ID, null, 
				null, null, null,
				null, null, null,
				HandlerContributionItem.STYLE_PUSH, null, false),
				fPrintElementHandler));
		
		m.add(new Separator(SharedUIResources.ADDITIONS_MENU_ID));
	}
	
	/** May only be called in UI thread */
	public void connect(final ToolProcess process) {
		if (fProcess == process) {
			return;
		}
		final RProcess oldProcess = fProcess;
		if (oldProcess != null) {
			oldProcess.getWorkspaceData().removePropertyListener(fInputUpdater);
		}
		synchronized (fProcessLock) {
			fProcess = (RProcess) 
					((process != null && process.isProvidingFeatureSet(RTool.R_DATA_FEATURESET_ID)) ?
							process : null);
		}
		if (fHoveringController != null) {
			fHoveringController.stop();
		}
		if (oldProcess != null) {
			clearInfo();
			oldProcess.getQueue().remove(new IToolRunnable[] { fManualRefreshRunnable });
		}
		if (!UIAccess.isOkToUse(fTreeViewer)) {
			return;
		}
		for (final IToolRetargetable listener : fToolListenerList.toArray()) {
			listener.setTool(fProcess);
		}
		fInputUpdater.forceUpdate(fProcess);
		if (fProcess != null) {
			fProcess.getWorkspaceData().addPropertyListener(fInputUpdater);
			updateAutoRefresh(fProcess.getWorkspaceData().isAutoRefreshEnabled());
		}
		fInputUpdater.schedule();
	}
	
	@Override
	public ToolProcess getTool() {
		return fProcess;
	}
	
	public ITreeSelection getSelection() {
		return (ITreeSelection) fTreeViewer.getSelection();
	}
	
	@Override
	public void addToolRetargetable(final IToolRetargetable action) {
		fToolListenerList.add(action);
	}
	
	@Override
	public void removeToolRetargetable(final IToolRetargetable action) {
		fToolListenerList.remove(action);
	}
	
	@Override
	public Object getAdapter(final Class required) {
		if (required.equals(ITool.class)) {
			return fProcess;
		}
		return super.getAdapter(required);
	}
	
	private void scheduleUpdateAll() {
		if (fProcess != null) {
			fProcess.getQueue().add(fManualRefreshRunnable);
		}
	}
	
	/** May only be called in UI thread (called by update job) */
	private void updateViewer(final List<ICombinedREnvironment> updateEnvirs) {
		if (!UIAccess.isOkToUse(fTreeViewer)) {
			return;
		}
		fHoveringController.stop();
		
		final boolean changed = fActiveInput.processChanged;
		ISelection selection = null;
		if (changed) {
			fActiveInput.processChanged = false;
		}
		/*else*/ if (fActiveInput.filterUserspace != fFilterUserspaceActivated) {
			// If filter is changed, we have to retain the selection manually
			selection = fTreeViewer.getSelection();
		}
		fFilterUserspaceActivated = fActiveInput.filterUserspace;
		
		final Set<RReference> usedReferences;
		if (fUsedReferences.isEmpty()) {
			usedReferences = null;
		}
		else if (changed || updateEnvirs == null) {
			usedReferences = null;
			fUsedReferences.clear();
		}
		else {
			usedReferences = fUsedReferences;
			fUsedReferences = new HashSet<RReference>();
		}
		if (updateEnvirs != null) {
			for (final ICombinedREnvironment entry : updateEnvirs) {
				fTreeViewer.refresh(entry, true);
			}
			if (usedReferences != null) {
				ITER_REFS: for (final RReference reference : usedReferences) {
					if (!fUsedReferences.contains(reference)) {
						// Update the envir copy in the viewer, if it refers to an updated envir
						for (final ICombinedREnvironment entry : updateEnvirs) {
							if (entry.getHandle() == reference.getHandle()) {
								fTreeViewer.refresh(reference, true);
								// reference is readded automatically to new set, if necessary
								continue ITER_REFS;
							}
						}
						// Keep/readd the reference, if it refers to an envir in the search path
						for (final ICombinedREnvironment entry : fActiveInput.searchEnvirs) {
							if (entry.getHandle() == reference.getHandle()) {
								fUsedReferences.add(reference);
								continue ITER_REFS;
							}
						}
					}
				}
			}
		}
		else {
			fTreeViewer.refresh(true);
		}
		
		// Adapt and set selection
		if (selection != null && !selection.isEmpty()) {
			final ITreeSelection s = (ITreeSelection) selection;
			final TreePath[] paths = s.getPaths();
			for (int i = 0; i < paths.length; i++) {
				final TreePath oldPath = paths[i];
				int count = oldPath.getSegmentCount();
				count = count + ((fActiveInput.filterUserspace) ? -1 : +1);
				final Object[] newPath = new Object[count];
				ICombinedRElement entry;
				{	final Object segment = oldPath.getLastSegment();
					if (segment instanceof ICombinedRElement) {
						entry = (ICombinedRElement) segment;
					}
					else if (oldPath.getSegmentCount() >= 2) {
						entry = (ICombinedRElement) oldPath.getSegment(oldPath.getSegmentCount()-2);
					}
					else {
						break;
					}
				}
				for (int j = count-1; j >= 0; j--) {
					newPath[j] = entry;
					entry = entry.getModelParent();
				}
				paths[i] = new TreePath(newPath);
			}
			fTreeViewer.setSelection(new StructuredSelection(paths), true);
		}
		
		// Expand Global_Env, if it is a new process and has valid input
		EXPAND_GLOBALENV : if (changed && !fActiveInput.filterUserspace 
				&& fActiveInput.rootElements != null && fActiveInput.rootElements.length > 0) {
			for (int i = 0; i < fActiveInput.rootElements.length; i++) {
				if (fTreeViewer.getExpandedState(fActiveInput.rootElements[i])) {
					break EXPAND_GLOBALENV;
				}
			}
			fTreeViewer.expandToLevel(fActiveInput.rootElements[0], 1);
		}
	}
	
	private void updateFilter() {
		fInputUpdater.schedule();
	}
	
	private void updateSorter() {
		fTreeViewer.setComparator(fSortByType ? TYPE_COMPARATOR : null);
	}
	
	private void updateSelectionInfo(final ITreeSelection selection) {
		final Object previousInfoObject = fCurrentInfoObject;
		fCurrentInfoObject = null;
		
		final RProcess process = fProcess;
		if (process == null) {
			return;
		}
		if (selection.size() == 1) {
			fCurrentInfoObject = selection.getFirstElement();
			final TreePath treePath = selection.getPaths()[0];
			final IElementName elementName = getElementName(treePath);
			final String name = (elementName != null) ? elementName.getDisplayName() : null;
			if (name != null) {
				if (fCurrentInfoObject.equals(previousInfoObject)) {
					clearInfo();
				}
				final String msg = name + "  \u2012  " + process.getLabel(ITool.DEFAULT_LABEL); //$NON-NLS-1$
				fStatusLine.setMessage(0, msg);
				return;
			}
		}
		clearInfo();
		if (selection.size() > 1) {
			final String msg = NLS.bind("{0} items selected", selection.size());
			fStatusLine.setMessage(0, msg);
			return;
		}
		fStatusLine.cleanStatusLine();
	}
	
	private void clearInfo() {
	}
	
	/**
	 * Returns a shared clipboard resource, which can be used by actions of this view.
	 * 
	 * @return a clipboard object.
	 */
	public Clipboard getClipboard() {
		if (fClipboard == null) {
			fClipboard = new Clipboard(Display.getCurrent());
		}
		return fClipboard;
	}
	
	@Override
	public void setFocus() {
		fTreeViewer.getControl().setFocus();
	}
	
	@Override
	public void dispose() {
		if (fToolRegistryListener != null) {
			NicoUI.getToolRegistry().removeListener(fToolRegistryListener);
			fToolRegistryListener = null;
		}
		if (fProcess != null) {
			fProcess.getWorkspaceData().removePropertyListener(fInputUpdater);
			fProcess.getQueue().remove(new IToolRunnable[] { fManualRefreshRunnable });
		}
		fInputUpdater.cancel();
		if (fHoveringController != null) {
			fHoveringController.dispose();
			fHoveringController = null;
		}
		if (fSearchStartHandler != null) {
			fSearchStartHandler.dispose();
			fSearchStartHandler = null;
		}
		if (fCopyElementNameHandler != null) {
			fCopyElementNameHandler.dispose();
			fCopyElementNameHandler = null;
		}
		if (fPrintElementHandler != null) {
			fPrintElementHandler.dispose();
			fPrintElementHandler = null;
		}
		
		for (final IToolRetargetable listener : fToolListenerList.toArray()) {
			listener.setTool(null);
		}
		
		super.dispose();
		
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
	}
	
	
	public RElementName getElementName(final TreePath treePath) {
		if (treePath.getSegmentCount() == 0) {
			return null;
		}
		int segmentIdx = 0;
		if (!fFilterUserspaceActivated) {
			if (treePath.getSegmentCount() == 1) { // search path item
				return ((ICombinedRElement) treePath.getFirstSegment()).getElementName();
			}
			else { // main name at 1
				segmentIdx = 1;
			}
		}
		final RElementName[] names = new RElementName[treePath.getSegmentCount()-segmentIdx+1];
		final ICombinedRElement first = (ICombinedRElement) treePath.getSegment(segmentIdx++);
		names[0] = first.getModelParent().getElementName();
		names[1] = first.getElementName();
		for (int namesIdx = 2; namesIdx < names.length; namesIdx++) {
			final Object segment = treePath.getSegment(segmentIdx++);
			if (segment instanceof ICombinedRElement) {
				names[namesIdx] = ((ICombinedRElement) segment).getElementName();
			}
			if (names[namesIdx] == null) {
				return null;
			}
		}
		return RElementName.concat(new ConstList<RElementName>(names));
	}
	
}
