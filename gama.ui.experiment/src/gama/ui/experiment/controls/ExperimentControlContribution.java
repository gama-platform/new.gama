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
package gama.ui.experiment.controls;

import static gama.ui.shared.resources.GamaColors.get;

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
import gama.core.common.interfaces.IUpdaterTarget;
import gama.core.kernel.experiment.IExperimentAgent;
import gama.core.kernel.experiment.IExperimentPlan;
import gama.core.kernel.experiment.ITopLevelAgent;
import gama.core.kernel.root.PlatformAgent;
import gama.core.kernel.simulation.SimulationAgent;
import gama.core.kernel.simulation.SimulationClock;
import gama.core.kernel.simulation.SimulationPopulation;
import gama.core.runtime.GAMA;
import gama.gaml.operators.Strings;
import gama.ui.shared.controls.FlatButton;
import gama.ui.shared.factories.StatusDisplayer;
import gama.ui.shared.resources.GamaIcon;
import gama.ui.shared.resources.IGamaColors;
import gama.ui.shared.resources.IGamaIcons;
import gama.ui.shared.utils.WorkbenchHelper;

/**
 * The Class ExperimentControlContribution.
 */
public class ExperimentControlContribution extends WorkbenchWindowControlContribution
		implements IUpdaterTarget<StatusMessage> {

	static {
		// DEBUG.ON();
	}

	/** The is updating. */
	private volatile boolean isUpdating;

	/** The label. */
	private FlatButton label;

	/** The popup. */
	private SimulationPopupMenu popup;

	/** The Constant WIDTH. */
	private final static int WIDTH = 400;

	/** The text. */
	private final StringBuilder text = new StringBuilder(2000);

	/** The instance. */
	static ExperimentControlContribution INSTANCE;

	/**
	 * Gets the single instance of ExperimentControlContribution.
	 *
	 * @return single instance of ExperimentControlContribution
	 */
	public static ExperimentControlContribution getInstance() { return INSTANCE; }

	/**
	 * Instantiates a new status control contribution.
	 */
	public ExperimentControlContribution() {
		INSTANCE = this;
		((StatusDisplayer) WorkbenchHelper.getService(IStatusDisplayer.class)).getThreadedUpdater()
				.setExperimentTarget(this);
	}

	/**
	 * Instantiates a new status control contribution.
	 *
	 * @param id
	 *            the id
	 */
	public ExperimentControlContribution(final String id) { // NO_UCD (unused code)
		super(id);
		INSTANCE = this;
		((StatusDisplayer) WorkbenchHelper.getService(IStatusDisplayer.class)).getThreadedUpdater()
				.setExperimentTarget(this);
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
		GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).applyTo(compo);
		label = FlatButton.label(compo, IGamaColors.NEUTRAL, "No experiment running", WIDTH)
				.setImage(GamaIcon.named(IGamaIcons.STATUS_CLOCK).image()).withMinimalHeight(24);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).hint(WIDTH, 24).applyTo(label);
		popup = new SimulationPopupMenu(this);
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {

				if (popup.isVisible()) {
					popup.hide();
				} else {
					final ITopLevelAgent agent = GAMA.getCurrentTopLevelAgent();
					if (agent != null && !agent.dead() && !agent.getScope().isClosed() && agent.getExperiment() != null
							&& !(agent instanceof PlatformAgent)) {
						WorkbenchHelper.asyncRun(popup::display);
					}
				}
			}

		});
		return compo;
	}

	@Override
	public boolean isDisposed() { return label.isDisposed(); }

	/**
	 * Sets the selection.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param agent
	 *            the new selection
	 * @date 26 août 2023
	 */

	public void setSelection(final ITopLevelAgent agent) {
		if (agent instanceof IExperimentAgent exp) {
			GAMA.changeCurrentTopLevelAgent(exp, false);
		} else if (agent instanceof SimulationAgent sim) { GAMA.getExperiment().getAgent().setCurrentSimulation(sim); }
	}

	/**
	 * Popup text for.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param exp
	 *            the exp
	 * @return the string
	 * @date 26 août 2023
	 */
	String popupTextFor(final ITopLevelAgent exp) {
		if (exp == null) return "";
		text.setLength(0);
		final SimulationClock clock = exp.getClock();
		clock.getInfo(text).append(Strings.LN);
		text.append("Durations: cycle ").append(clock.getDuration()).append("ms; average ")
				.append((int) clock.getAverageDuration()).append("ms; total ").append(clock.getTotalDuration())
				.append("ms");
		return text.toString();
	}

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
		try {
			isUpdating = true;
			// DEBUG.OUT("Updating with current experiment " + GAMA.getExperiment());
			if (GAMA.getExperiment() == null) {
				label.removeMenuSign();
				popup.wipe();
				if (popup.isVisible()) { popup.hide(); }
			} else {
				label.addMenuSign();
			}
			ITopLevelAgent agent = GAMA.getCurrentTopLevelAgent();
			label.setImageWithoutRecomputingSize(m.icon() == null ? null : GamaIcon.named(m.icon()).image());
			label.setColor(get(agent.getColor()));
			label.setTextWithoutRecomputingSize(getClockMessage(agent));
			if (popup.isVisible()) { popup.display(); }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			isUpdating = false;
		}
	}

	/**
	 * Gets the clock message.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param agent
	 *            the agent
	 * @return the clock message
	 * @date 26 août 2023
	 */
	private String getClockMessage(final ITopLevelAgent agent) {
		if (agent == null) return "";
		if (agent instanceof PlatformAgent) return "No experiment running";
		final IExperimentAgent exp = agent.getExperiment();
		if (exp == null) return "";
		text.setLength(0);
		agent.getClock().getInfo(text);
		final SimulationPopulation pop = exp.getSimulationPopulation();
		final int nbThreads = pop == null ? 1 : pop.getNumberOfActiveThreads();
		if (agent.getScope().isOnUserHold()) {
			text.append(" (waiting)");
		} else if (nbThreads > 1) { text.append(" (" + nbThreads + " threads)"); }
		final IExperimentPlan plan = exp.getSpecies();
		if (plan.shouldBeBenchmarked()) { text.append(" [benchmarking]"); }
		return text.toString();
	}

	/**
	 * Method resume()
	 *
	 * @see gama.core.common.interfaces.IUpdaterTarget#reset()
	 */
	@Override
	public void reset() {}

}
