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

package de.walware.statet.r.internal.console.ui.launching;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.DateFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.util.DialogUtil;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil.TreeComposite;

import de.walware.rj.server.RjsComConfig;
import de.walware.rj.server.Server;
import de.walware.rj.server.ServerInfo;

import de.walware.statet.r.internal.console.ui.RConsoleMessages;
import de.walware.statet.r.internal.console.ui.RConsoleUIPlugin;


public class RRemoteConsoleSelectionDialog extends SelectionStatusDialog {
	
	
	public static abstract class SpecialAddress {
		
		private final String fPublicHost;
		private final String fPrivateHost;
		private final int fPort;
		
		
		public SpecialAddress(final String publicHost, final String privateHost, final int port) {
			fPublicHost = publicHost;
			fPrivateHost = privateHost;
			fPort = port;
		}
		
		public abstract RMIClientSocketFactory getSocketFactory(IProgressMonitor monitor) throws CoreException;
		
	}
	
	
	private static final String SETTINGS_DIALOG_ID = "RRemoteConsoleSelection"; //$NON-NLS-1$
	private static final String SETTINGS_HOST_HISTORY_KEY = "hosts.history"; //$NON-NLS-1$
	
	private static final Pattern ADDRESS_MULTI_PATTERN = Pattern.compile("\\/?\\s*[\\,\\;]+\\s*"); //$NON-NLS-1$
	private static final Pattern ADDRESS_WITH_PORT_PATTERN = Pattern.compile("(.*):(\\d{1,5})"); //$NON-NLS-1$
	
	
	private static class RemoteR {
		
		final String hostName;
		final String hostIP;
		final String address;
		
		final ServerInfo info;
		
		RemoteR(final String hostName, final String hostIP, final String address, final ServerInfo info) {
			this.hostName = hostName;
			this.hostIP = hostIP;
			this.address = address;
			
			this.info = info;
		}
		
		String createSummary() {
			final StringBuilder sb = new StringBuilder(100);
			sb.append("Address:   ").append(address).append('\n');
			sb.append('\n');
			sb.append("Host-Name: ").append(hostName).append('\n');
			sb.append("Host-IP:   ").append(hostIP).append('\n');
			sb.append("Date:      ").append((info.getTimestamp() != 0) ?
					DateFormat.getDateInstance().format(info.getTimestamp()) : "<unknown>").append('\n');
			sb.append("Directory: ").append(info.getDirectory()).append('\n');
			sb.append("Status:    ");
			switch (this.info.getState()) {
			case Server.S_NOT_STARTED:
				sb.append("New – Ready to connect and start R");
				break;
			case Server.S_CONNECTED:
			case Server.S_CONNECTED_STALE:
				sb.append("Running – Connected (username is ");
				sb.append((info.getUsername(ServerInfo.USER_CONSOLE) != null) ?
						info.getUsername(ServerInfo.USER_CONSOLE)  : "<unknown>").append(')');
				break;
			case Server.S_LOST:
				sb.append("Running – Connection lost / Ready to reconnect");
				break;
			case Server.S_DISCONNECTED:
				sb.append("Running – Disconnected / Ready to reconnect");
				break;
			case Server.S_STOPPED:
				sb.append("Stopped");
				break;
			default:
				sb.append("Unknown");
				break;
			}
			return sb.toString();
		}
		
	}
	
	private static class RemoteRContentProvider implements ITreeContentProvider {
		
		private final HashMap<String, RemoteR[]> fMapping = new HashMap<String, RemoteR[]>();
		
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}
		
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object[] getElements(final Object inputElement) {
			final List<RemoteR> all = (List<RemoteR>) inputElement;
			
			fMapping.clear();
			final Map mapping = fMapping;
			for (final RemoteR r : all) {
				final String username = r.info.getUsername(ServerInfo.USER_OWNER).toLowerCase();
				List<RemoteR> list = (List<RemoteR>) mapping.get(username);
				if (list == null) {
					list = new ArrayList<RemoteR>();
					mapping.put(username, list);
				}
				list.add(r);
			}
			
			final Set<Map.Entry> entrySet = mapping.entrySet();
			for (final Entry cat : entrySet) {
				final List<RemoteR> list = (List<RemoteR>) cat.getValue();
				cat.setValue(list.toArray(new RemoteR[list.size()]));
			}
			return fMapping.keySet().toArray();
		}
		
		@Override
		public Object getParent(final Object element) {
			if (element instanceof RemoteR) {
				return ((RemoteR) element).info.getUsername(ServerInfo.USER_OWNER);
			}
			return null;
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			return (element instanceof String);
		}
		
		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof String) {
				return fMapping.get(parentElement);
			}
			return null;
		}
		
		@Override
		public void dispose() {
		}
		
	}
	
	private static abstract class RemoteRLabelProvider extends CellLabelProvider {
		
		@Override
		public void update(final ViewerCell cell) {
			final Object element = cell.getElement();
			String text = null;
			if (element instanceof RemoteR) {
				text = getText((RemoteR) element);
			}
			cell.setText(text);
		}
		
		public abstract String getText(RemoteR r);
		
		@Override
		public Font getToolTipFont(final Object element) {
			if (element instanceof RemoteR) {
				return JFaceResources.getTextFont();
			}
			return null;
		}
		
		@Override
		public String getToolTipText(final Object element) {
			if (element instanceof RemoteR) {
				return ((RemoteR) element).createSummary();
			}
			return null;
		}
		
	}
	
	
	private Combo fHostAddressControl;
	
	private TreeViewer fRServerViewer;
	
	private List<RemoteR> fRServerList;
	
	private final boolean fFilterOnlyRunning;
	
	private String fUsername;
	
	private final List<String> fHistoryAddress = new ArrayList<String>(DialogUtil.HISTORY_MAX + 10);
	private final List<String> fAdditionalAddress = new ArrayList<String>(8);
	private final Map<String, SpecialAddress> fSpecialAddress = new HashMap<String, SpecialAddress>(8);
	private String fInitialAddress;
	
	
	public RRemoteConsoleSelectionDialog(final Shell parentShell, final boolean onlyRunning) {
		super(parentShell);
		setTitle(RConsoleMessages.RRemoteConsoleSelectionDialog_title);
		setMessage(RConsoleMessages.RRemoteConsoleSelectionDialog_message);
		
		setStatusLineAboveButtons(true);
		setDialogBoundsSettings(getDialogSettings(), Dialog.DIALOG_PERSISTSIZE);
		
		fUsername = System.getProperty("user.name"); //$NON-NLS-1$
		fFilterOnlyRunning = onlyRunning;
	}
	
	
	public void setUser(final String username) {
		if (username != null && username.length() > 0) {
			fUsername = username;
		}
	}
	
	public void setInitialAddress(final String address) {
		fAdditionalAddress.remove(address);
		fAdditionalAddress.add(0, address);
		fInitialAddress = address;
	}
	
	public void addAdditionalAddress(final String label, final SpecialAddress factory) {
		fAdditionalAddress.add(label);
		if (factory != null) {
			fSpecialAddress.put(label, factory);
		}
	}
	
	public void clearAdditionaAddress(final boolean specialOnly) {
		if (specialOnly) {
			for (final Entry<String, ?> entry : fSpecialAddress.entrySet()) {
				fAdditionalAddress.remove(entry.getKey());
			}
		}
		else {
			fAdditionalAddress.clear();
		}
		fSpecialAddress.clear();
	}
	
	
	protected IDialogSettings getDialogSettings() {
		return DialogUtil.getDialogSettings(RConsoleUIPlugin.getDefault(), SETTINGS_DIALOG_ID);
	}
	
	@Override
	protected Control createContents(final Composite parent) {
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "de.walware.statet.r.ui.remote_engine_selection_dialog"); //$NON-NLS-1$
		
		return super.createContents(parent);
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		// page group
		final Composite area = (Composite) super.createDialogArea(parent);
		
		createMessageArea(area);
		final IDialogSettings dialogSettings = getDialogSettings();
		
		{	final Composite composite = new Composite(area, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			composite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 3));
			
			final Label label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText(RConsoleMessages.RRemoteConsoleSelectionDialog_Hostname_label);
			
			fHostAddressControl = new Combo(composite, SWT.DROP_DOWN);
			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint = LayoutUtil.hintWidth(fHostAddressControl, 50);
			fHostAddressControl.setLayoutData(gd);
			final String[] history = dialogSettings.getArray(SETTINGS_HOST_HISTORY_KEY);
			if (history != null) {
				fHistoryAddress.addAll(Arrays.asList(history));
			}
			fHostAddressControl.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					update();
				}
			});
			
			final Button goButton = new Button(composite, SWT.PUSH);
			goButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			goButton.setText(RConsoleMessages.RRemoteConsoleSelectionDialog_Update_label);
			goButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					update();
				}
			});
		}
		
		{	final TreeComposite composite = new TreeComposite(area, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = LayoutUtil.hintHeight(composite.tree, 10);
			composite.setLayoutData(gd);
			fRServerViewer = composite.viewer;
			composite.tree.setHeaderVisible(true);
			ColumnViewerToolTipSupport.enableFor(composite.viewer);
			
			{	final TreeViewerColumn column = new TreeViewerColumn(fRServerViewer, SWT.NONE);
				column.getColumn().setText(RConsoleMessages.RRemoteConsoleSelectionDialog_Table_UserOrEngine_label);
				composite.layout.setColumnData(column.getColumn(), new ColumnWeightData(1));
				column.setLabelProvider(new RemoteRLabelProvider() {
					@Override
					public void update(final ViewerCell cell) {
						final Object element = cell.getElement();
						String text = null;
						Image image = null;
						if (element instanceof String) {
							text = (String) element;
							image = SharedUIResources.getImages().get(SharedUIResources.OBJ_USER_IMAGE_ID);
						}
						else if (element instanceof RemoteR) {
							text = getText((RemoteR) element);
						}
						cell.setText(text);
						cell.setImage(image);
					}
					
					@Override
					public String getText(final RemoteR r) {
						return r.info.getName();
					}
				});
			}
			{	final TreeViewerColumn column = new TreeViewerColumn(fRServerViewer, SWT.NONE);
				column.getColumn().setText(RConsoleMessages.RRemoteConsoleSelectionDialog_Table_Host_label);
				composite.layout.setColumnData(column.getColumn(), new ColumnWeightData(1));
				column.setLabelProvider(new RemoteRLabelProvider() {
					@Override
					public String getText(final RemoteR r) {
						return r.hostName;
					}
				});
			}
			
			fRServerViewer.setContentProvider(new RemoteRContentProvider());
			
			fRServerViewer.getTree().addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateState();
				}
				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					updateState();
					if (getOkButton().isEnabled()) {
						buttonPressed(IDialogConstants.OK_ID);
					}
				}
			});
		}
		
		Dialog.applyDialogFont(area);
		
		updateInput();
		if (fRServerList != null) {
			updateStatus(new Status(IStatus.OK, RConsoleUIPlugin.PLUGIN_ID,
					RConsoleMessages.RRemoteConsoleSelectionDialog_info_ListRestored_message ));
		}
		return area;
	}
	
	private void update() {
		final String input = fHostAddressControl.getText();
		fRServerList = null;
		final AtomicReference<IStatus> status = new AtomicReference<IStatus>();
		if (input != null && input.length() > 0) {
			try {
				new ProgressMonitorDialog(getShell()).run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor) throws InvocationTargetException {
						status.set(updateRServerList(input, monitor));
					}
				});
			}
			catch (final InvocationTargetException e) {
				// not used
			}
			catch (final InterruptedException e) {
				fRServerViewer = null;
				status.compareAndSet(null, Status.CANCEL_STATUS);
			}
		}
		if (status.get() != null) {
			updateStatus(status.get());
		}
		getOkButton().setEnabled(false);
		if (fRServerList != null && fRServerList.size() > 0) {
			if (!fSpecialAddress.containsKey(input)) {
				fHistoryAddress.remove(input);
				fHistoryAddress.add(0, input);
			}
			if (fFilterOnlyRunning) {
				for (final Iterator<RemoteR> iter = fRServerList.iterator(); iter.hasNext();) {
					switch (iter.next().info.getState()) {
					case Server.S_NOT_STARTED:
					case Server.S_STOPPED:
						iter.remove();
					}
				}
			}
			updateInput();
			return;
		}
		else {
			fRServerViewer.setInput(null);
		}
	}
	
	private void updateInput() {
		final String selectedAddress = fHostAddressControl.getText();
		final List<String> list = new ArrayList<String>(fHistoryAddress.size() + fAdditionalAddress.size());
		list.addAll(fHistoryAddress);
		for (final String address : fAdditionalAddress) {
			if (!list.contains(address)) {
				list.add(address);
			}
		}
		fHostAddressControl.setItems(list.toArray(new String[list.size()]));
		
		fRServerViewer.setInput(fRServerList);
		
		if (fUsername != null && fUsername.length() > 0) {
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (fInitialAddress != null) {
						fHostAddressControl.setText(fInitialAddress);
						fInitialAddress = null;
					}
					else if (selectedAddress != null && selectedAddress.length() > 0) {
						fHostAddressControl.setText(selectedAddress);
					}
					else if (fHostAddressControl.getItemCount() > 0) {
						fHostAddressControl.select(0);
					}
					fRServerViewer.expandToLevel(fUsername.toLowerCase(), 1);
					updateState();
				}
			});
		}
	}
	
	private void updateState() {
		final IStructuredSelection selection = (IStructuredSelection) fRServerViewer.getSelection();
		getOkButton().setEnabled(selection.getFirstElement() instanceof RemoteR);
	}
	
	@Override
	protected void computeResult() {
		final IStructuredSelection selection = (IStructuredSelection) fRServerViewer.getSelection();
		final Object element = selection.getFirstElement();
		if (element instanceof RemoteR) {
			setSelectionResult(new Object[] { ((RemoteR) element).address });
		}
	}
	
	@Override
	public boolean close() {
		final IDialogSettings dialogSettings = getDialogSettings();
		dialogSettings.put(SETTINGS_HOST_HISTORY_KEY, fHistoryAddress.toArray(new String[fHistoryAddress.size()]));
		
		return super.close();
	}
	
	private IStatus updateRServerList(final String combined, final IProgressMonitor monitor) {
		final List<RemoteR> infos = new ArrayList<RemoteR>();
		
		final String[] addresses = ADDRESS_MULTI_PATTERN.split(combined, -1);
		if (addresses.length == 0) {
			return null;
		}
		final SubMonitor progress = SubMonitor.convert(monitor, RConsoleMessages.RRemoteConsoleSelectionDialog_task_Gathering_message, addresses.length*2 +2);
		
		String failedHosts = null;
		final List<IStatus> failedStatus = new ArrayList<IStatus>();
		progress.worked(1);
		
		// Collect R engines for each address
		for (int i = 0; i < addresses.length; i++) {
			progress.setWorkRemaining((addresses.length-i)*2 +1);
			
			String address = addresses[i];
			final SpecialAddress special = fSpecialAddress.get(address);
			if (special == null) {
				if (address.startsWith("rmi:")) { //$NON-NLS-1$
					address = address.substring(4);
				}
				if (address.startsWith("//")) { //$NON-NLS-1$
					address = address.substring(2);
				}
			}
			if (address.isEmpty()) {
				return null;
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			IStatus status;
			if (special == null) {
				final Matcher matcher = ADDRESS_WITH_PORT_PATTERN.matcher(address);
				if (matcher.matches()) {
					status = collectServerInfos(matcher.group(1), Integer.parseInt(matcher.group(2)), null,
							infos, progress );
				}
				else {
					status = collectServerInfos(address, Registry.REGISTRY_PORT, null,
							infos, progress );
				}
			}
			else {
				address = special.fPublicHost;
				status = collectServerInfos(null, 0, special, infos, progress);
			}
			switch (status.getSeverity()) {
			case IStatus.CANCEL:
				return status;
			case IStatus.ERROR:
				StatusManager.getManager().handle(status, StatusManager.LOG);
				return status;
			case IStatus.WARNING:
				failedStatus.add(status);
				failedHosts = (failedHosts == null) ? address : (failedHosts + ", " + address); //$NON-NLS-1$
				continue;
			default:
				continue;
			}
		}
		
		if (!failedStatus.isEmpty()) {
			StatusManager.getManager().handle(new MultiStatus(RConsoleUIPlugin.PLUGIN_ID, 0, 
					failedStatus.toArray(new IStatus[failedStatus.size()]),
					"Info about connection failures when browsing R engines:", null), //$NON-NLS-1$
					StatusManager.LOG );
		}
		if (!infos.isEmpty() || failedStatus.isEmpty() ) {
			fRServerList = infos;
		}
		
		if (failedHosts != null) {
			return new Status(IStatus.WARNING, RConsoleUIPlugin.PLUGIN_ID,
					RConsoleMessages.RRemoteConsoleSelectionDialog_error_ConnectionFailed_message+failedHosts );
		}
		return Status.OK_STATUS;
	}
	
	private static IStatus collectServerInfos(String address, int port,
			final SpecialAddress special,
			final List<RemoteR> infos, final SubMonitor progress) {
		try {
			if (special != null) {
				address = special.fPublicHost;
				port = special.fPort;
			}
			progress.subTask(NLS.bind(RConsoleMessages.RRemoteConsoleSelectionDialog_task_Resolving_message, address));
			final InetAddress inetAddress = InetAddress.getByName(address);
			final String hostname = inetAddress.getHostName();
			final String hostip = inetAddress.getHostAddress();
			progress.worked(1);
			if (progress.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			progress.subTask(NLS.bind(RConsoleMessages.RRemoteConsoleSelectionDialog_task_Connecting_message, hostname));
			final Registry registry;
			if (special != null) {
				final RMIClientSocketFactory socketFactory = special.getSocketFactory(progress.newChild(5));
				RjsComConfig.setRMIClientSocketFactory(socketFactory);
				registry = LocateRegistry.getRegistry(special.fPrivateHost, port, socketFactory );
			}
			else {
				RjsComConfig.setRMIClientSocketFactory(null);
				registry = LocateRegistry.getRegistry(address, port);
			}
			final String rmiBase = (port == Registry.REGISTRY_PORT) ?
					"//" + address + '/' : //$NON-NLS-1$
					"//" + address + ':' + port + '/'; //$NON-NLS-1$
			final String[] names = registry.list();
			for (final String name : names) {
				try {
					final Remote remote = registry.lookup(name);
					if (remote instanceof Server) {
						final Server server = (Server) remote;
						final ServerInfo info = server.getInfo();
						final String rmiAddress = rmiBase+name;
						final RemoteR r = new RemoteR(hostname, hostip, rmiAddress, info);
						infos.add(r);
					}
				}
				catch (final Exception e) {
				}
			}
			return Status.OK_STATUS;
		}
		catch (final RemoteException e) {
			return new Status(IStatus.WARNING, RConsoleUIPlugin.PLUGIN_ID, address);
		}
		catch (final UnknownHostException e) {
			return new Status(IStatus.ERROR, RConsoleUIPlugin.PLUGIN_ID, "Unknown host: " + e.getLocalizedMessage()); //$NON-NLS-1$
		}
		catch (final CoreException e) {
			return e.getStatus();
		}
		finally {
			RjsComConfig.clearRMIClientSocketFactory();
		}
	}
	
}
