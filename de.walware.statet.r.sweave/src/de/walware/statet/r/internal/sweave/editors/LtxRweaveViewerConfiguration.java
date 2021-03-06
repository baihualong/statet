/*******************************************************************************
 * Copyright (c) 2007-2012 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.sweave.editors;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import de.walware.ecommons.collections.ConstList;
import de.walware.ecommons.ltk.ui.sourceediting.EcoReconciler2;
import de.walware.ecommons.ltk.ui.sourceediting.EditorInformationProvider;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditorAddon;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditor1;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfiguration;
import de.walware.ecommons.ltk.ui.sourceediting.assist.ContentAssist;
import de.walware.ecommons.ltk.ui.sourceediting.assist.ContentAssistCategory;
import de.walware.ecommons.ltk.ui.sourceediting.assist.ContentAssistProcessor;
import de.walware.ecommons.ltk.ui.sourceediting.assist.IContentAssistComputer;
import de.walware.ecommons.text.ICharPairMatcher;
import de.walware.ecommons.ui.ColorManager;

import de.walware.docmlet.tex.core.ITexCoreAccess;
import de.walware.docmlet.tex.core.TexCore;
import de.walware.docmlet.tex.core.text.ITexDocumentConstants;
import de.walware.docmlet.tex.core.text.LtxHeuristicTokenScanner;
import de.walware.docmlet.tex.ui.sourceediting.LtxViewerConfiguration;
import de.walware.docmlet.tex.ui.text.LtxDoubleClickStrategy;

import de.walware.statet.base.ui.IStatetUIPreferenceConstants;
import de.walware.statet.ext.ui.text.CommentScanner;

import de.walware.statet.r.core.IRCoreAccess;
import de.walware.statet.r.core.RCore;
import de.walware.statet.r.core.rsource.RHeuristicTokenScanner;
import de.walware.statet.r.internal.sweave.SweavePlugin;
import de.walware.statet.r.sweave.ITexRweaveCoreAccess;
import de.walware.statet.r.sweave.TexRweaveCoreAccess;
import de.walware.statet.r.sweave.text.LtxRweaveBracketPairMatcher;
import de.walware.statet.r.sweave.text.LtxRweaveSwitch;
import de.walware.statet.r.sweave.text.RChunkControlCodeScanner;
import de.walware.statet.r.sweave.text.Rweave;
import de.walware.statet.r.sweave.text.RweaveChunkHeuristicScanner;
import de.walware.statet.r.ui.sourceediting.RAutoEditStrategy;
import de.walware.statet.r.ui.sourceediting.RSourceViewerConfiguration;
import de.walware.statet.r.ui.text.r.RDoubleClickStrategy;


/**
 * Default Configuration for SourceViewer of Sweave (LaTeX/R) code.
 */
public class LtxRweaveViewerConfiguration extends SourceEditorViewerConfiguration {
	
	
	private static class RChunkAutoEditStrategy extends RAutoEditStrategy {
		
		public RChunkAutoEditStrategy(final IRCoreAccess coreAccess, final ISourceEditor sourceEditor) {
			super(coreAccess, sourceEditor);
		}
		
		@Override
		protected RHeuristicTokenScanner createScanner() {
			return new RweaveChunkHeuristicScanner();
		}
		
		@Override
		protected IRegion getValidRange(final int offset, final int c) {
			final ITypedRegion cat = Rweave.R_TEX_CAT_UTIL.getCat(getDocument(), offset);
			if (cat.getType() == Rweave.R_CAT) {
				return cat;
			}
			if (cat.getType() == Rweave.CONTROL_CAT) {
				switch (c) {
				case '(':
				case '[':
				case '{':
				case '%':
				case '\"':
				case '\'':
					return cat;
				}
			}
			return null;
		}
		
	}
	
	private static class RChunkViewerConfiguration extends RSourceViewerConfiguration {
		
		public RChunkViewerConfiguration(
				final ISourceEditor sourceEditor,
				final IRCoreAccess coreAccess, final IPreferenceStore preferenceStore, final ColorManager colorManager) {
			super(sourceEditor, coreAccess, preferenceStore, colorManager);
		}
		
		@Override
		protected void setCoreAccess(final IRCoreAccess access) {
			super.setCoreAccess(access);
		}
		
		@Override
		public String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
			return Rweave.LTX_R_PARTITIONING;
		}
		
		@Override
		public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
			return Rweave.R_PARTITION_TYPES;
		}
		
		public CommentScanner getCommentScanner() {
			return fCommentScanner;
		}
		
		@Override
		protected RAutoEditStrategy createRAutoEditStrategy() {
			return new RChunkAutoEditStrategy(getRCoreAccess(), getSourceEditor());
		}
		
		@Override
		protected EditorInformationProvider getInformationProvider() {
			return super.getInformationProvider();
		}
		
	}
	
	private static class TexChunkViewerConfiguration extends LtxViewerConfiguration {
		
		public TexChunkViewerConfiguration(final ISourceEditor editor,
				final ITexCoreAccess texCoreAccess, final IPreferenceStore preferenceStore, final ColorManager colorManager) {
			super(editor, texCoreAccess, preferenceStore, colorManager);
		}
		
		@Override
		protected void setCoreAccess(final ITexCoreAccess access) {
			super.setCoreAccess(access);
		}
		
		@Override
		public String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
			return Rweave.LTX_R_PARTITIONING;
		}
		
	}
	
	
	private final TexChunkViewerConfiguration fTexConfig;
	private final RChunkViewerConfiguration fRConfig;
	
	private ITexRweaveCoreAccess fCoreAccess;
	
	private RChunkControlCodeScanner fChunkControlScanner;
	
	private ITextDoubleClickStrategy fTexDoubleClickStrategy;
	private ITextDoubleClickStrategy fRDoubleClickStrategy;
	
	
	public LtxRweaveViewerConfiguration(
			final IPreferenceStore preferenceStore, final ColorManager colorManager) {
		this(null, null, preferenceStore, colorManager);
	}
	
	public LtxRweaveViewerConfiguration(final ISourceEditor sourceEditor,
			final ITexRweaveCoreAccess coreAccess,
			final IPreferenceStore preferenceStore, final ColorManager colorManager) {
		super(sourceEditor);
		fCoreAccess = (coreAccess != null) ? coreAccess : new TexRweaveCoreAccess(
				TexCore.getWorkbenchAccess(), RCore.getWorkbenchAccess() );
		fRConfig = new RChunkViewerConfiguration(sourceEditor, fCoreAccess, preferenceStore, colorManager);
		fRConfig.setHandleDefaultContentType(false);
		fTexConfig = new TexChunkViewerConfiguration(sourceEditor, fCoreAccess, preferenceStore, colorManager);
		
		setup((preferenceStore != null) ? preferenceStore : SweavePlugin.getDefault().getEditorTexRPreferenceStore(),
				colorManager,
				IStatetUIPreferenceConstants.EDITING_DECO_PREFERENCES,
				IStatetUIPreferenceConstants.EDITING_ASSIST_PREFERENCES );
		setScanners(createScanners());
	}
	
	protected ITokenScanner[] createScanners() {
		final IPreferenceStore store = getPreferences();
		final ColorManager colorManager = getColorManager();
		
		fChunkControlScanner = new RChunkControlCodeScanner(colorManager, store);
		
		return new ITokenScanner[] {
				fChunkControlScanner,
		};
	}
	
	protected void setCoreAccess(final ITexRweaveCoreAccess coreAccess) {
		fCoreAccess = (coreAccess != null) ? coreAccess : new TexRweaveCoreAccess(
				TexCore.getWorkbenchAccess(), RCore.getWorkbenchAccess() );
		fRConfig.setCoreAccess(fCoreAccess);
		fTexConfig.setCoreAccess(fCoreAccess);
	}
	
	
	@Override
	public List<ISourceEditorAddon> getAddOns() {
		final List<ISourceEditorAddon> addOns = super.getAddOns();
		addOns.addAll(fTexConfig.getAddOns());
		addOns.addAll(fRConfig.getAddOns());
		return addOns;
	}
	
	@Override
	public void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
		fRConfig.handleSettingsChanged(groupIds, options);
		fTexConfig.handleSettingsChanged(groupIds, options);
		super.handleSettingsChanged(groupIds, options);
	}
	
	
	@Override
	public String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
		return Rweave.LTX_R_PARTITIONING;
	}
	
	@Override
	public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
		return Rweave.ALL_PARTITION_TYPES;
	}
	
	
	@Override
	public ICharPairMatcher createPairMatcher() {
		return new LtxRweaveBracketPairMatcher();
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
		switch (LtxRweaveSwitch.get(contentType)) {
		case LTX:
			if (fTexDoubleClickStrategy == null) {
				fTexDoubleClickStrategy = new LtxDoubleClickStrategy(
						new LtxHeuristicTokenScanner(Rweave.LTX_PARTITIONING_CONFIG) );
			}
			return fTexDoubleClickStrategy;
		case R:
		case CHUNK_CONTROL:
			if (fRDoubleClickStrategy == null) {
				final RweaveChunkHeuristicScanner scanner = new RweaveChunkHeuristicScanner();
				fRDoubleClickStrategy = new RDoubleClickStrategy(scanner,
						LtxRweaveBracketPairMatcher.createRChunkPairMatcher(scanner) );
			}
			return fRDoubleClickStrategy;
		default:
			return null;
		}
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
		final PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(fChunkControlScanner);
		reconciler.setDamager(dr, Rweave.CHUNK_CONTROL_CONTENT_TYPE);
		reconciler.setRepairer(dr, Rweave.CHUNK_CONTROL_CONTENT_TYPE);
		
		dr = new DefaultDamagerRepairer(fRConfig.getCommentScanner());
		reconciler.setDamager(dr, Rweave.CHUNK_COMMENT_CONTENT_TYPE);
		reconciler.setRepairer(dr, Rweave.CHUNK_COMMENT_CONTENT_TYPE);
		
		fRConfig.initDefaultPresentationReconciler(reconciler);
		fTexConfig.initDefaultPresentationReconciler(reconciler);
		
		return reconciler;
	}
	
	@Override
	public int getTabWidth(final ISourceViewer sourceViewer) {
		return fTexConfig.getTabWidth(sourceViewer);
	}
	
	@Override
	public String[] getDefaultPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		if (Rweave.R_PARTITION_CONSTRAINT.matches(contentType)) {
			return fRConfig.getDefaultPrefixes(sourceViewer, contentType);
		}
		return fTexConfig.getDefaultPrefixes(sourceViewer, contentType);
	}
	
	@Override
	public String[] getIndentPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		switch (LtxRweaveSwitch.get(contentType)) {
		case LTX:
			return fTexConfig.getIndentPrefixes(sourceViewer, contentType);
		case R:
			return fRConfig.getIndentPrefixes(sourceViewer, contentType);
		default:
			return new String[0];
		}
	}
	
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(final ISourceViewer sourceViewer, final String contentType) {
		switch (LtxRweaveSwitch.get(contentType)) {
		case LTX:
			return fTexConfig.getAutoEditStrategies(sourceViewer, contentType);
		case R:
			return fRConfig.getAutoEditStrategies(sourceViewer, contentType);
		default:
			return new IAutoEditStrategy[0];
		}
	}
	
	
	protected IReconcilingStrategy getSpellingStrategy(final ISourceViewer sourceViewer) {
		if (!(fRConfig.getRCoreAccess().getPrefs().getPreferenceValue(SweaveEditorOptions.PREF_SPELLCHECKING_ENABLED)
				&& fPreferenceStore.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED)) ) {
			return null;
		}
		final SpellingService spellingService = EditorsUI.getSpellingService();
		if (spellingService.getActiveSpellingEngineDescriptor(fPreferenceStore) == null) {
			return null;
		}
		return new SpellingReconcileStrategy(sourceViewer, spellingService);
	}
	
	
	@Override
	protected ContentAssistant createContentAssistant(final ISourceViewer sourceViewer) {
		if (getSourceEditor() == null) {
			return null;
		}
		final RChunkTemplatesCompletionComputer chunkComputer = new RChunkTemplatesCompletionComputer();
		
		final ContentAssist assistant = (ContentAssist) fTexConfig.getContentAssistant(sourceViewer);
		
		final ContentAssistProcessor texProcessor = (ContentAssistProcessor) assistant.getContentAssistProcessor(ITexDocumentConstants.LTX_DEFAULT_EXPL_CONTENT_TYPE);
		texProcessor.addCategory(new ContentAssistCategory(ITexDocumentConstants.LTX_DEFAULT_EXPL_CONTENT_TYPE,
				new ConstList<IContentAssistComputer>(chunkComputer)));
		texProcessor.setCompletionProposalAutoActivationCharacters(new char[] { '\\', '<' });
		
		final ContentAssistProcessor mathProcessor = (ContentAssistProcessor) assistant.getContentAssistProcessor(ITexDocumentConstants.LTX_MATH_CONTENT_TYPE);
		mathProcessor.addCategory(new ContentAssistCategory(ITexDocumentConstants.LTX_MATH_CONTENT_TYPE,
				new ConstList<IContentAssistComputer>(chunkComputer)));
		mathProcessor.setCompletionProposalAutoActivationCharacters(new char[] { '\\', '<' });
		
		fRConfig.initDefaultContentAssist(assistant);
		
		final ContentAssistProcessor controlProcessor = new ContentAssistProcessor(assistant,
				Rweave.CHUNK_CONTROL_CONTENT_TYPE, SweavePlugin.getDefault().getTexEditorContentAssistRegistry(), getSourceEditor());
		controlProcessor.addCategory(new ContentAssistCategory(Rweave.CHUNK_CONTROL_CONTENT_TYPE,
				new ConstList<IContentAssistComputer>(chunkComputer)));
		assistant.setContentAssistProcessor(controlProcessor, Rweave.CHUNK_CONTROL_CONTENT_TYPE);
		
		return assistant;
	}
	
	@Override
	protected IQuickAssistAssistant createQuickAssistant(final ISourceViewer sourceViewer) {
		if (getSourceEditor() == null) {
			return null;
		}
		final QuickAssistAssistant assistant = new QuickAssistAssistant();
		assistant.setQuickAssistProcessor(new LtxRweaveQuickAssistProcessor(getSourceEditor()));
		assistant.enableColoredLabels(true);
		return assistant;
	}
	
	
	@Override
	public int[] getConfiguredTextHoverStateMasks(final ISourceViewer sourceViewer, final String contentType) {
		switch (LtxRweaveSwitch.get(contentType)) {
		case LTX:
			return fTexConfig.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
		case R:
			return fRConfig.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
		default:
			return null;
		}
	}
	
	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType, final int stateMask) {
		switch (LtxRweaveSwitch.get(contentType)) {
		case LTX:
			return fTexConfig.getTextHover(sourceViewer, contentType, stateMask);
		case R:
			return fRConfig.getTextHover(sourceViewer, contentType, stateMask);
		default:
			return null;
		}
	}
	
	@Override
	protected IInformationProvider getInformationProvider() {
		return new LtxRweaveInformationProvider(fRConfig.getInformationProvider());
	}
	
	
	@Override
	public IReconciler getReconciler(final ISourceViewer sourceViewer) {
		final ISourceEditor editor = getSourceEditor();
		if (!(editor instanceof SourceEditor1)) {
			return null;
		}
		final EcoReconciler2 reconciler = (EcoReconciler2) fTexConfig.getReconciler(sourceViewer);
		if (reconciler != null) {
			final IReconcilingStrategy spellingStrategy = getSpellingStrategy(sourceViewer);
			if (spellingStrategy != null) {
				reconciler.addReconcilingStrategy(spellingStrategy);
			}
		}
		return reconciler;
	}
	
	@Override
	protected Map getHyperlinkDetectorTargets(final ISourceViewer sourceViewer) {
		final Map<String, Object> targets = super.getHyperlinkDetectorTargets(sourceViewer);
		targets.put("de.walware.docmlet.tex.editorHyperlinks.TexEditorTarget", getSourceEditor()); //$NON-NLS-1$
		targets.put("de.walware.statet.r.editorHyperlinks.REditorTarget", getSourceEditor()); //$NON-NLS-1$
		return targets;
	}
	
	@Override
	public boolean isSmartInsertSupported() {
		return true;
	}
	
	@Override
	public boolean isSmartInsertByDefault() {
		return fTexConfig.isSmartInsertByDefault()
				&& fRConfig.isSmartInsertByDefault();
	}
	
}
