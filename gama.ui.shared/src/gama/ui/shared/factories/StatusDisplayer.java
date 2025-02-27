/*******************************************************************************************************
 *
 * StatusDisplayer.java, in gama.ui.shared.experiment, is part of the source code of the GAMA modeling and simulation
 * platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.shared.factories;

import gama.core.common.StatusMessage;
import gama.core.common.interfaces.IStatusDisplayer;
import gama.core.common.interfaces.IUpdaterMessage.StatusType;
import gama.core.kernel.experiment.ITopLevelAgent;
import gama.core.runtime.GAMA;
import gama.core.runtime.IScope;
import gama.core.util.GamaColor;
import gama.ui.shared.utils.ThreadedUpdater;

/**
 * The Class StatusDisplayer.
 */
public class StatusDisplayer implements IStatusDisplayer {

	/** The status. */
	private final ThreadedUpdater<StatusMessage> status = new ThreadedUpdater<>("Status refresh");

	/**
	 * Instantiates a new status displayer.
	 */
	StatusDisplayer() {
		// status.setExperimentTarget(ExperimentControlContribution.getInstance());
		// status.setStatusTarget(StatusControlContribution.getInstance());
		GAMA.registerTopLevelAgentChangeListener(this);
	}

	/**
	 * Sets the listening agent.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param agent
	 *            the new listening agent
	 * @date 14 août 2023
	 */
	@Override
	public void topLevelAgentChanged(final ITopLevelAgent agent) {
		updateExperimentStatus(agent.getScope());
	}

	/**
	 * Wait status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param string
	 *            the string
	 * @date 14 août 2023
	 */
	@Override
	public void waitStatus(final IScope scope, final String string) {
		setStatus(string, StatusType.WAIT);
	}

	/**
	 * Inform status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param string
	 *            the string
	 * @date 14 août 2023
	 */
	@Override
	public void informStatus(final IScope scope, final String string) {
		setStatus(string, StatusType.INFORM);
	}

	/**
	 * Error status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param error
	 *            the error
	 * @date 14 août 2023
	 */
	@Override
	public void errorStatus(final IScope scope, final Exception error) {
		status.updateWith(StatusMessage.ERROR(scope, error));
	}

	/**
	 * Sets the status.
	 *
	 * @param msg
	 *            the msg
	 * @param code
	 *            the code
	 */
	private void setStatus(final String msg, final StatusType code) {
		status.updateWith(StatusMessage.CUSTOM(msg, code, null));
	}

	/**
	 * Resume status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @date 14 août 2023
	 */
	@Override
	public void resetStatus(final IScope scope) {
		status.reset();
	}

	/**
	 * Sets the sub status completion.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param s
	 *            the new sub status completion
	 * @date 14 août 2023
	 */
	@Override
	public void setTaskCompletion(final IScope scope, final double s) {
		status.updateWith(StatusMessage.COMPLETION("", s));
	}

	/**
	 * Inform status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param string
	 *            the string
	 * @param icon
	 *            the icon
	 * @date 14 août 2023
	 */
	@Override
	public void updateExperimentStatus(final IScope scope) {
		status.updateWith(StatusMessage.EXPERIMENT());
	}

	/**
	 * Begin sub status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param name
	 *            the name
	 * @date 14 août 2023
	 */
	@Override
	public void beginTask(final IScope scope, final String name) {
		status.updateWith(StatusMessage.BEGIN(name));
	}

	/**
	 * End sub status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param name
	 *            the name
	 * @date 14 août 2023
	 */
	@Override
	public void endTask(final IScope scope, final String name) {
		status.updateWith(StatusMessage.END(name));
	}

	/**
	 * Sets the status internal.
	 *
	 * @param msg
	 *            the msg
	 * @param color
	 *            the color
	 * @param icon
	 *            the icon
	 */
	private void setUserStatus(final String msg, final GamaColor color, final String icon) {
		status.updateWith(StatusMessage.USER(msg, icon, color));
	}

	/**
	 * Sets the status.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param message
	 *            the message
	 * @param color
	 *            the color
	 * @date 14 août 2023
	 */
	@Override
	public void setStatus(final IScope scope, final String message, final String icon, final GamaColor color) {
		if (message == null) {
			resetStatus(scope);
		} else {
			setUserStatus(message, color, icon);
		}

	}

	public ThreadedUpdater getThreadedUpdater() { return status; }

}