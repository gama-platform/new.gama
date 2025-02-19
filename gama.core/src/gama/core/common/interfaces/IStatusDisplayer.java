/*******************************************************************************************************
 *
 * IStatusDisplayer.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.common.interfaces;

import gama.core.kernel.experiment.ITopLevelAgent;
import gama.core.runtime.IScope;
import gama.core.util.GamaColor;

/**
 * The Interface IStatusDisplayer.
 *
 * Changed AD 11/11/22: puts IScope first like everywhere in the code...
 */
public interface IStatusDisplayer extends ITopLevelAgentChangeListener {

	/**
	 * Sets the listening agent.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param agent
	 *            the new listening agent
	 * @date 14 août 2023
	 */
	@Override
	default void topLevelAgentChanged(final ITopLevelAgent agent) {}

	/**
	 * Resume status.
	 */
	default void resetStatus(final IScope scope) {}

	/**
	 * Wait status.
	 *
	 * @param string
	 *            the string
	 */
	default void waitStatus(final IScope scope, final String string) {}

	default void waitStatus(final IScope scope, final String string, final Runnable run) {
		waitStatus(scope, string);
		run.run();
		resetStatus(scope);
	}

	/**
	 * Inform status.
	 *
	 * @param string
	 *            the string
	 */
	default void informStatus(final IScope scope, final String message) {}

	/**
	 * Error status.
	 *
	 * @param message
	 *            the message
	 */
	default void errorStatus(final IScope scope, final Exception error) {}

	/**
	 * Sets the sub status completion.
	 *
	 * @param status
	 *            the new sub status completion
	 */
	default void setTaskCompletion(final IScope scope, final double status) {}

	/**
	 * Inform status.
	 *
	 * @param message
	 *            the message
	 * @param icon
	 *            the icon
	 */
	default void updateExperimentStatus(final IScope scope) {}

	/**
	 * Sets the status.
	 *
	 * @param msg
	 *            the msg
	 * @param icon
	 *            the icon
	 */
	default void setStatus(final IScope scope, final String message, final String icon, final GamaColor color) {}

	/**
	 * Begin sub status.
	 *
	 * @param name
	 *            the name
	 */
	default void beginTask(final IScope scope, final String name) {}

	/**
	 * End sub status.
	 *
	 * @param name
	 *            the name
	 */
	default void endTask(final IScope scope, final String name) {}

}
