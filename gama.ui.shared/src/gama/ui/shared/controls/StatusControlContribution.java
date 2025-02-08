/*******************************************************************************************************
 *
 * ExperimentControlContribution.java, in gama.ui.shared.experiment, is part of the source code of the GAMA modeling and
 * simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.shared.controls;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import gama.core.common.StatusMessage;
import gama.core.common.interfaces.IStatusDisplayer;
import gama.core.common.interfaces.IUpdaterMessage;
import gama.core.common.interfaces.IUpdaterMessage.StatusType;
import gama.core.common.interfaces.IUpdaterTarget;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.dev.DEBUG;
import gama.ui.application.workbench.ThemeHelper;
import gama.ui.shared.factories.StatusDisplayer;
import gama.ui.shared.resources.GamaColors;
import gama.ui.shared.resources.GamaColors.GamaUIColor;
import gama.ui.shared.resources.GamaIcon;
import gama.ui.shared.utils.WorkbenchHelper;

/**
 * The Class ExperimentControlContribution.
 */
public class StatusControlContribution extends WorkbenchWindowControlContribution
		implements IUpdaterTarget<StatusMessage> {

	static {
		DEBUG.ON();
	}
	StatusType currentStatus;

	GamaRuntimeException currentException;

	/** The is updating. */
	private volatile boolean isUpdating;

	/** The label. */
	private FlatButton label;

	private ErrorPopUpMenu errorPopup;

	/** The sub task completion. */
	private volatile Double taskCompletion;

	/** The Constant WIDTH. */
	private final static int WIDTH = 400;

	/** The instance. */
	static StatusControlContribution INSTANCE;

	private GamaUIColor inactiveColor;

	private double progress = 1d;

	/**
	 * Gets the single instance of ExperimentControlContribution.
	 *
	 * @return single instance of ExperimentControlContribution
	 */
	public static StatusControlContribution getInstance() { return INSTANCE; }

	/**
	 * Instantiates a new status control contribution.
	 */
	public StatusControlContribution() {
		INSTANCE = this;
		((StatusDisplayer) WorkbenchHelper.getService(IStatusDisplayer.class)).getThreadedUpdater()
				.setStatusTarget(this);
	}

	/**
	 * Instantiates a new status control contribution.
	 *
	 * @param id
	 *            the id
	 */
	public StatusControlContribution(final String id) { // NO_UCD (unused code)
		super(id);
		INSTANCE = this;
	}

	@Override
	protected int computeWidth(final Control control) {
		return WIDTH;
	}

	@Override
	public boolean isBusy() { return isUpdating; }

	@Override
	protected Control createControl(final Composite parent) {
		final Composite compo = new Composite(parent, SWT.DOUBLE_BUFFERED);
		inactiveColor = ThemeHelper.isDark() ? GamaColors.get(GamaColors.get(parent.getBackground()).lighter())
				: GamaColors.get(GamaColors.get(parent.getBackground()).darker());
		GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).applyTo(compo);
		label = FlatButton.label(compo, inactiveColor, "", WIDTH).withMinimalHeight(24);
		label.setEnabled(false);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).hint(WIDTH, 24).applyTo(label);
		errorPopup = new ErrorPopUpMenu(this);
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {

				if (currentStatus == StatusType.ERROR) {
					if (errorPopup.isVisible()) {
						errorPopup.hide();
					} else {
						WorkbenchHelper.asyncRun(() -> errorPopup.display(currentException));
					}
				}

			}

		});
		return compo;
	}

	@Override
	public boolean isDisposed() { return label.isDisposed(); }

	/**
	 * Gets the controlling shell.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @return the controlling shell
	 * @date 26 août 2023
	 */
	public Shell getControllingShell() { return label.getShell(); }

	/**
	 * Gets the absolute origin.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @return the absolute origin
	 * @date 26 août 2023
	 */
	public Point getLocation() { return label.toDisplay(label.getLocation()); }

	/**
	 * Gets the popup width.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @return the popup width
	 * @date 26 août 2023
	 */
	public int getWidth() { return label.getSize().x; }

	/**
	 * Gets the height.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @return the height
	 * @date 26 août 2023
	 */
	public int getHeight() { return label.getSize().y; }

	/**
	 * Method updateWith()
	 *
	 * @see gama.gui.swt.controls.ThreadedUpdater.IUpdaterTarget#updateWith(java.lang.Object)
	 */
	@Override
	public void updateWith(final StatusMessage m) {
		if (isUpdating) return;
		isUpdating = true;
		try {
			if (m == null) {
				taskCompletion = null;
				currentException = null;
				currentStatus = StatusType.NONE;
				label.setColor(inactiveColor);
				label.setTextWithoutRecomputingSize("");
				label.setImageWithoutRecomputingSize(null);
			} else {
				taskCompletion = m.completion();
				currentException = m.exception();
				currentStatus = m.getType();
				String icon = getIcon(m.icon());

				label.setImageWithoutRecomputingSize(icon == null ? null : GamaIcon.named(icon).image());
				// label.setColor(getLabelBackground(m));
				label.setTextWithoutRecomputingSize(getLabelText(m));
			}
		} finally {
			isUpdating = false;
		}

	}

	private String getIcon(final String icon) {
		if (icon == null) return null;
		if (IUpdaterMessage.PROGRESS_ICON.equals(icon)) {
			if (progress > 6) { progress = 1; }
			progress += 0.3;
			return "status/progress" + Math.round(progress);

		}
		return icon;
	}

	private String getLabelText(final StatusMessage m) {
		String taskName = m.message();
		if (taskName == null) { taskName = ""; }
		return taskName + (currentStatus == StatusType.SUBTASK
				? taskCompletion != null ? " [" + (int) (taskCompletion * 100) + "%]" : "" : "");
	}

	public GamaUIColor getLabelBackground(final StatusMessage m) {
		return GamaColors.get(m.color());
	}

	/**
	 * Method resume()
	 *
	 * @see gama.core.common.interfaces.IUpdaterTarget#reset()
	 */
	@Override
	public void reset() {
		WorkbenchHelper.run(() -> updateWith(null));
	}

}
