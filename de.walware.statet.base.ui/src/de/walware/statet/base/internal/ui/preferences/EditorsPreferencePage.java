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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.masterdetail.MasterDetailObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.walware.ecommons.IStatusChangeListener;
import de.walware.ecommons.databinding.IntegerValidator;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.Preference.BooleanPref;
import de.walware.ecommons.preferences.Preference.IntPref;
import de.walware.ecommons.preferences.ui.ColorSelectorObservableValue;
import de.walware.ecommons.preferences.ui.ConfigurationBlockPreferencePage;
import de.walware.ecommons.preferences.ui.ManagedConfigurationBlock;
import de.walware.ecommons.preferences.ui.RGBPref;
import de.walware.ecommons.text.ui.settings.AssistPreferences;
import de.walware.ecommons.text.ui.settings.DecorationPreferences;
import de.walware.ecommons.ui.util.LayoutUtil;

import de.walware.statet.base.internal.ui.StatetUIPlugin;
import de.walware.statet.base.ui.IStatetUIPreferenceConstants;


public class EditorsPreferencePage extends ConfigurationBlockPreferencePage<EditorsConfigurationBlock> {
	
	
	public EditorsPreferencePage() {
		setPreferenceStore(StatetUIPlugin.getDefault().getPreferenceStore());
	}
	
	
	@Override
	protected EditorsConfigurationBlock createConfigurationBlock() {
		return new EditorsConfigurationBlock(createStatusChangedListener());
	}
	
}


class EditorsConfigurationBlock extends ManagedConfigurationBlock {
	
	private static class AppearanceColorsItem {
		
		final String name;
		final RGBPref pref;
		
		AppearanceColorsItem(final String label, final RGBPref pref) {
			this.name = label;
			this.pref = pref;
		}
		
	}
	
	
	private ListViewer fColorList;
	private ColorSelector fColorEditor;
	private Button fMatchingBracketsControl;
	private BooleanPref fMatchingBracketsPref;
	private Button fCodeAssistAutoSingleControl;
	private BooleanPref fCodeAssistAutoSinglePref;
	private Button fCodeAssistAutoCommonControl;
	private BooleanPref fCodeAssistAutoCommonPref;
	private Text fCodeAssistDelayControl;
	private IntPref fCodeAssistDelayPref;
	
	
	public EditorsConfigurationBlock(final IStatusChangeListener statusListener) {
		super(null, statusListener);
	}
	
	
	@Override
	protected void createBlockArea(final Composite pageComposite) {
		final Map<Preference<?>, String> prefs = new HashMap<Preference<?>, String>();
		
		final List<AppearanceColorsItem> colors = new ArrayList<AppearanceColorsItem>();
		
		AppearanceColorsItem color;
		
		// Content Assist
		final AssistPreferences assistPreferences = IStatetUIPreferenceConstants.EDITING_ASSIST_PREFERENCES;
		fCodeAssistAutoSinglePref = assistPreferences.getAutoInsertSinglePref();
		prefs.put(fCodeAssistAutoSinglePref, assistPreferences.getGroupId());
		fCodeAssistAutoCommonPref = assistPreferences.getAutoInsertPrefixPref();
		prefs.put(fCodeAssistAutoCommonPref, assistPreferences.getGroupId());
		fCodeAssistDelayPref = assistPreferences.getAutoActivationDelayPref();
		prefs.put(fCodeAssistDelayPref, assistPreferences.getGroupId());
		
		color = new AppearanceColorsItem(Messages.Editors_CodeAssistParametersForegrondColor,
				assistPreferences.getInformationForegroundPref());
		colors.add(color);
		prefs.put(color.pref, assistPreferences.getGroupId());
		color = new AppearanceColorsItem(Messages.Editors_CodeAssistParametersBackgroundColor,
				assistPreferences.getInformationBackgroundPref());
		colors.add(color);
		prefs.put(color.pref, assistPreferences.getGroupId());
//		color = new AppearanceColorsItem(Messages.Editors_CodeAssistReplacementForegroundColor,
//				assistPreferences.getReplacementForegroundPref());
//		colors.add(color);
//		prefs.put(color.pref, assistPreferences.getGroupId());
//		color = new AppearanceColorsItem(Messages.Editors_CodeAssistReplacementBackgroundColor,
//				ContentAssistPreference.REPLACEMENT_BACKGROUND);
//		colors.add(color);
//		prefs.put(color.pref, ContentAssistPreference.GROUP_ID);
		
		// Matching Bracket
		final DecorationPreferences decoPrefs = IStatetUIPreferenceConstants.EDITING_DECO_PREFERENCES;
		fMatchingBracketsPref = decoPrefs.getMatchingBracketsEnabled();
		prefs.put(fMatchingBracketsPref, null);
		color = (new AppearanceColorsItem(Messages.Editors_MatchingBracketsHighlightColor,
				decoPrefs.getMatchingBracketsColor() ));
		colors.add(color);
		prefs.put(color.pref, null);
		
		// Register preferences
		setupPreferenceManager(prefs);
		
		// Controls
		addLinkHeader(pageComposite, Messages.Editors_link);
		Composite group = createAppearanceSection(pageComposite);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		LayoutUtil.addSmallFiller(pageComposite, false);
		group = createCodeAssistSection(pageComposite);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// Binding
		fColorList.setInput(colors.toArray(new AppearanceColorsItem[colors.size()]));
		fColorList.setSelection(new StructuredSelection(colors.get(0)));
		initBindings();
		updateControls();
	}
	
	private Composite createAppearanceSection(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Editors_Appearance);
		group.setLayout(LayoutUtil.applyGroupDefaults(new GridLayout(), 2));
		Label label;
		GridData gd;
		
		fMatchingBracketsControl = new Button(group, SWT.CHECK | SWT.LEFT);
		fMatchingBracketsControl.setText(Messages.Editors_HighlightMatchingBrackets);
		fMatchingBracketsControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		LayoutUtil.addSmallFiller(group, false);
		final Composite colorComposite = new Composite(group, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		colorComposite.setLayoutData(gd);
		colorComposite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
		label = new Label(colorComposite, SWT.LEFT);
		label.setText(Messages.Editors_AppearanceColors);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		fColorList = new ListViewer(colorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		fColorList.setContentProvider(new ArrayContentProvider());
		fColorList.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				final AppearanceColorsItem item = (AppearanceColorsItem) element;
				return item.name;
			}
		});
		fColorList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		final Composite colorOptions = new Composite(colorComposite, SWT.NONE);
		colorOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		colorOptions.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
		label = new Label(colorOptions, SWT.LEFT);
		label.setText(Messages.Editors_Color);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		fColorEditor = new ColorSelector(colorOptions);
		fColorEditor.getButton().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		return group;
	}
	
	private Composite createCodeAssistSection(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Editors_CodeAssist);
		group.setLayout(LayoutUtil.applyGroupDefaults(new GridLayout(), 2));
		Label label;
		GridData gd;
		
		fCodeAssistAutoSingleControl = new Button(group, SWT.CHECK);
		fCodeAssistAutoSingleControl.setText(Messages.Editors_CodeAssist_AutoInsertSingle);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
		fCodeAssistAutoSingleControl.setLayoutData(gd);
		
		fCodeAssistAutoCommonControl = new Button(group, SWT.CHECK);
		fCodeAssistAutoCommonControl.setText(Messages.Editors_CodeAssist_AutoInsertCommon);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
		fCodeAssistAutoCommonControl.setLayoutData(gd);
		
		label = new Label(group, SWT.LEFT);
		label.setText(Messages.Editors_CodeAssist_AutoTriggerDelay_label);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		fCodeAssistDelayControl = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gd.widthHint = LayoutUtil.hintWidth(fCodeAssistDelayControl, 4);
		fCodeAssistDelayControl.setLayoutData(gd);
		
		return group;
	}
	
	@Override
	protected void addBindings(final DataBindingContext dbc, final Realm realm) {
		dbc.bindValue(SWTObservables.observeSelection(fMatchingBracketsControl),
				createObservable(fMatchingBracketsPref),
				null, null);
		final IObservableValue colorItem = ViewersObservables.observeSingleSelection(fColorList);
		dbc.bindValue(new ColorSelectorObservableValue(fColorEditor),
				MasterDetailObservables.detailValue(colorItem, new IObservableFactory() {
					@Override
					public IObservable createObservable(final Object target) {
						final AppearanceColorsItem item = (AppearanceColorsItem) target;
						return EditorsConfigurationBlock.this.createObservable(item.pref);
					}
				}, RGB.class),
				null, null);
		dbc.bindValue(SWTObservables.observeSelection(fCodeAssistAutoSingleControl),
				createObservable(fCodeAssistAutoSinglePref),
				null, null);
		dbc.bindValue(SWTObservables.observeSelection(fCodeAssistAutoCommonControl),
				createObservable(fCodeAssistAutoCommonPref),
				null, null);
		dbc.bindValue(SWTObservables.observeText(fCodeAssistDelayControl, SWT.Modify),
				createObservable(fCodeAssistDelayPref),
				new UpdateValueStrategy().setAfterGetValidator(new IntegerValidator(10, 2000, Messages.Editors_CodeAssist_AutoTriggerDelay_error_message)), null);
	}
	
}
