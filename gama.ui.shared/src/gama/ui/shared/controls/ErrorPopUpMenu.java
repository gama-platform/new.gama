/*******************************************************************************************************
 *
 * SimulationPopupMenu.java, in gama.ui.shared.experiment, is part of the source code of the GAMA modeling and
 * simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.shared.controls;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import gama.core.runtime.GAMA;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.core.util.GamaColor;
import gama.dev.DEBUG;
import gama.ui.shared.resources.GamaColors;
import gama.ui.shared.resources.GamaIcon;
import gama.ui.shared.resources.IGamaColors;
import gama.ui.shared.utils.WorkbenchHelper;

/**
 * The Class CustomMenu. An alternative to Popup & Regular menus
 *
 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
 * @date 22 août 2023
 */
public class ErrorPopUpMenu extends PopupDialog {

	static {
		DEBUG.ON();
	}

	/** The contents. */
	Composite parent, contents;

	Composite labelComposite;
	Label labelButton;
	Label labelText;

	/** The hide. */
	final Listener hide = event -> hide();

	/** The provider. */
	private final StatusControlContribution status;

	/**
	 * Instantiates a new popup 2.
	 *
	 * @param status
	 *            the provider
	 * @param controls
	 *            the controls
	 */
	/*
	 *
	 */
	public ErrorPopUpMenu(final StatusControlContribution status) {
		super(WorkbenchHelper.getShell(), PopupDialog.HOVER_SHELLSTYLE, false, false, false, false, false, null, null);
		this.status = status;
		final Shell shell = status.getControllingShell();
		shell.addListener(SWT.Move, hide);
		shell.addListener(SWT.Resize, hide);
		shell.addListener(SWT.Close, hide);
		shell.addListener(SWT.Deactivate, hide);
		shell.addListener(SWT.Hide, hide);
		shell.addListener(SWT.Dispose, event -> close());
	}

	@Override
	protected Control createContents(final Composite p) {
		if (parent != p || parent.isDisposed()) {
			this.parent = p;
			GridLayoutFactory.swtDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(parent);
		}
		if (contents == null || contents.isDisposed()) {
			this.contents = (Composite) super.createDialogArea(parent);
			GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.CENTER).applyTo(contents);
			GridLayoutFactory.swtDefaults().numColumns(1).margins(5, 5).spacing(0, 5).applyTo(contents);
		}
		if (labelComposite == null || labelComposite.isDisposed()) { createLabel(); }
		return contents;
	}

	/**
	 * Creates the label.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param labels
	 *            the labels
	 * @date 26 août 2023
	 */
	private void createLabel() {
		try {
			labelComposite = new Composite(contents, SWT.NONE);
			GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).spacing(5, 0).applyTo(labelComposite);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(labelComposite);
			labelButton = new Label(labelComposite, SWT.NONE);
			labelButton.setImage(GamaIcon.named("experiment/errors.show").image());
			labelText = new Label(labelComposite, SWT.WRAP);

			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, true).applyTo(labelButton);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(labelText);

			labelText.addMouseTrackListener(new MouseTrackListener() {

				Color background, foreground;

				@Override
				public void mouseHover(final MouseEvent e) {}

				@Override
				public void mouseEnter(final MouseEvent e) {
					background = labelText.getBackground();
					foreground = labelText.getForeground();
					GamaColor c = IGamaColors.ERROR.gamaColor();
					Color b = GamaColors.toSwtColor(c);
					GamaColors.setBackAndForeground(b, GamaColors.getTextColorForBackground(b).color(), labelText);
				}

				@Override
				public void mouseExit(final MouseEvent e) {
					GamaColors.setBackAndForeground(background, foreground, labelText);
				}
			});
			labelText.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseDown(final MouseEvent e) {
					hide();
					EObject editor = (EObject) labelText.getData();
					if (editor != null) { WorkbenchHelper.asyncRun(() -> GAMA.getGui().editModel(editor)); }
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean hasTitleArea() {
		return false;
	}

	@Override
	protected boolean hasInfoArea() {
		return false;
	}

	@Override
	protected void showDialogMenu() {}

	@Override
	protected void setInfoText(final String text) {}

	@Override
	protected void setTitleText(final String text) {}

	@Override
	protected void saveDialogBounds(final Shell shell) {}

	/**
	 * Gets the default location.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @return the default location
	 * @date 26 août 2023
	 */
	@Override
	protected Point getDefaultLocation(final Point initialSize) {
		Point p = status.getLocation();
		return new Point(p.x, p.y + status.getHeight() - 4);
	}

	@Override
	protected Point getDefaultSize() {
		int width = status.getWidth();
		if (width <= 0) { width = SWT.DEFAULT; }
		return getShell().computeSize(width, SWT.DEFAULT, true);
	}

	/**
	 * Checks if is visible.
	 *
	 * @return true, if is visible
	 */
	public boolean isVisible() { return getShell() != null && getShell().isVisible(); }

	/**
	 * Display.
	 */
	public void display(final GamaRuntimeException ex) {
		Shell shell = getShell();
		if (shell == null || shell.isDisposed()) {
			open();
			shell = getShell();
		}
		createContents(parent);
		labelText.setText(ex.getAllText());
		labelText.setData(ex.getEditorContext());
		labelComposite.requestLayout();
		shell.setLocation(getDefaultLocation(null));
		shell.setSize(getDefaultSize());
		shell.setVisible(true);

	}

	/**
	 * Hide.
	 */
	public void hide() {
		final Shell shell = getShell();
		if (shell != null && !shell.isDisposed()) { shell.setVisible(false); }
	}

}
