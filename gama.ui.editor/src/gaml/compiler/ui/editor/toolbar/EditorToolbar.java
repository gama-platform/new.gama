/*******************************************************************************************************
 *
 * EditorToolbar.java, in gama.ui.editor, is part of the source code of the GAMA modeling and simulation platform
 * (v.1.9.3).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.compiler.ui.editor.toolbar;

import static gama.ui.shared.utils.WorkbenchHelper.executeCommand;
import static gama.ui.shared.utils.WorkbenchHelper.getCommand;
import static org.eclipse.ui.IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY;
import static org.eclipse.ui.IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ICommandListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.ToolItem;

import gama.gaml.compilation.kernel.GamaBundleLoader;
import gama.ui.shared.access.GamlAccessContents2;
import gama.ui.shared.bindings.GamaKeyBindings;
import gama.ui.shared.utils.WorkbenchHelper;
import gama.ui.shared.views.toolbar.GamaToolbarSimple;
import gama.ui.shared.views.toolbar.Selector;
import gaml.compiler.ui.editor.GamlEditor;

/**
 * The class EditorNavigationControls.
 *
 * @author drogoul
 * @since 11 nov. 2016
 *
 */
public class EditorToolbar {

	/** The previous. */
	ToolItem next, previous, diagram;

	/** The find. */
	EditorSearchControls find;

	/** The editor. */
	final GamlEditor editor;

	/** The searching. */
	volatile boolean searching;

	/** The global previous. */
	final Selector globalPrevious = e -> executeCommand(NAVIGATE_BACKWARD_HISTORY);

	/** The global next. */
	final Selector globalNext = e -> executeCommand(NAVIGATE_FORWARD_HISTORY);

	/** The search previous. */
	final Selector searchPrevious = e -> find.findPrevious();

	/** The search next. */
	final Selector searchNext = e -> find.findNext();

	/**
	 * Instantiates a new editor toolbar.
	 *
	 * @param editor
	 *            the editor
	 */
	public EditorToolbar(final GamlEditor editor) {
		this.editor = editor;
	}

	/**
	 * Fill.
	 *
	 * @param toolbar
	 *            the toolbar
	 * @return the editor search controls
	 */
	public EditorSearchControls fill(final GamaToolbarSimple toolbar) {

		previous = toolbar.button("editor/command.lastedit", null, "Previous edit location", globalPrevious);

		find = new EditorSearchControls(editor).fill(toolbar);
		next = toolbar.button("editor/command.nextedit", null, "Next edit location", globalNext);
		toolbar.menu("editor/command.outline", null, "Show outline", e -> {
			if (editor == null) return;
			editor.openOutlinePopup();
		});

		if (GamaBundleLoader.isDiagramEditorLoaded()) {
			diagram = toolbar.button("editor/command.graphical", null, "Switch to diagram", e -> {
				if (editor == null) return;
				editor.switchToDiagram();
			});
		}

		// Attaching listeners to the global commands in order to enable/disable the
		// toolbar items
		hookToCommands(previous, next);

		// Attaching a focus listener to the search control to
		hookToSearch(previous, next);

		// ToolItem ref = toolbar.control(reference.createWidget(toolbar), 200);
		// ref.getControl().setVisible(false);
		// toolbar.update();
		ToolItem button = toolbar.button("editor/command.find", null, "Search GAML reference", e -> {
			final GamlAccessContents2 quickAccessDialog = new GamlAccessContents2();
			quickAccessDialog.open();
		});
		// button.getControl().addMouseTrackListener(new MouseTrackListener() {
		//
		// @Override
		// public void mouseHover(final MouseEvent e) {}
		//
		// @Override
		// public void mouseExit(final MouseEvent e) {
		//
		// }
		//
		// @Override
		// public void mouseEnter(final MouseEvent e) {
		// ref.getControl().setVisible(true);
		// }
		// });
		// ref.getControl().addMouseTrackListener(new MouseTrackListener() {
		//
		// @Override
		// public void mouseHover(final MouseEvent e) {}
		//
		// @Override
		// public void mouseExit(final MouseEvent e) {
		// ref.getControl().setVisible(false);
		// }
		//
		// @Override
		// public void mouseEnter(final MouseEvent e) {
		//
		// }
		// });

		return find;
	}

	/**
	 * Hook to search.
	 *
	 * @param lastEdit
	 *            the last edit
	 * @param nextEdit
	 *            the next edit
	 */
	private void hookToSearch(final ToolItem lastEdit, final ToolItem nextEdit) {
		find.getFindControl().addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {
				searching = true;
				previous.removeSelectionListener(globalPrevious);
				previous.setToolTipText("Search previous occurence");
				next.removeSelectionListener(globalNext);
				next.setToolTipText("Search next occurence " + GamaKeyBindings.format(SWT.MOD1, 'G'));
				previous.addSelectionListener(searchPrevious);
				next.addSelectionListener(searchNext);
				previous.setEnabled(true);
				next.setEnabled(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				searching = false;
				previous.removeSelectionListener(searchPrevious);
				previous.setToolTipText("Previous edit location");
				next.removeSelectionListener(searchNext);
				next.setToolTipText("Next edit location");
				previous.addSelectionListener(globalPrevious);
				next.addSelectionListener(globalNext);
				previous.setEnabled(getCommand(NAVIGATE_BACKWARD_HISTORY).isEnabled());
				next.setEnabled(getCommand(NAVIGATE_FORWARD_HISTORY).isEnabled());
			}
		});
	}

	/**
	 * Hook to commands.
	 *
	 * @param lastEdit
	 *            the last edit
	 * @param nextEdit
	 *            the next edit
	 */
	private void hookToCommands(final ToolItem lastEdit, final ToolItem nextEdit) {
		WorkbenchHelper.runInUI("Hooking to commands", 0, m -> {
			final Command nextCommand = getCommand(NAVIGATE_FORWARD_HISTORY);
			if (!nextEdit.isDisposed()) {
				nextEdit.setEnabled(nextCommand.isEnabled());
				final ICommandListener nextListener = e -> nextEdit.setEnabled(searching || nextCommand.isEnabled());
				nextCommand.addCommandListener(nextListener);
				nextEdit.addDisposeListener(e -> nextCommand.removeCommandListener(nextListener));
			}
			final Command lastCommand = getCommand(NAVIGATE_BACKWARD_HISTORY);
			if (!lastEdit.isDisposed()) {
				final ICommandListener lastListener = e -> lastEdit.setEnabled(searching || lastCommand.isEnabled());
				lastEdit.setEnabled(lastCommand.isEnabled());
				lastCommand.addCommandListener(lastListener);
				lastEdit.addDisposeListener(e -> lastCommand.removeCommandListener(lastListener));
			}
			// Attaching dispose listeners to the toolItems so that they remove the
			// command listeners properly

		});

	}

}
