/*******************************************************************************************************
 *
 * GamaServerStatusDisplayer.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.runtime.server;

import gama.core.common.interfaces.IStatusDisplayer;
import gama.core.kernel.experiment.IExperimentAgent;
import gama.core.runtime.IScope;
import gama.core.util.GamaColor;

/**
 * The Class GamaServerStatusDisplayer.
 *
 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
 * @date 2 nov. 2023
 */
public final class GamaServerStatusDisplayer extends GamaServerMessager implements IStatusDisplayer {

	/**
	 * Can send message.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param exp
	 *            the exp
	 * @return true, if successful
	 * @date 2 nov. 2023
	 */
	@Override
	public boolean canSendMessage(final IExperimentAgent exp) {
		if (exp == null) return false;
		var scope = exp.getScope();
		return scope != null && scope.getServerConfiguration().status();
	}

	@Override
	public void informStatus(final IScope scope, final String string) {
		if (!canSendMessage(scope.getExperiment())) return;
		sendMessage(scope.getExperiment(), "{" + "\"message\": \"" + string + "\"" + "}",
				GamaServerMessage.Type.SimulationStatusInform);
	}

	@Override
	public void errorStatus(final IScope scope, final Exception error) {
		if (!canSendMessage(scope.getExperiment())) return;
		sendMessage(scope.getExperiment(), "{" + "\"message\": \"" + error.getMessage() + "\"" + "}",
				GamaServerMessage.Type.SimulationStatusError);
	}

	@Override
	public void setStatus(final IScope scope, final String msg, final String icon, final GamaColor color) {
		if (!canSendMessage(scope.getExperiment())) return;
		sendMessage(scope.getExperiment(), json.object("message", msg, "color", color, "icon", icon).toString(),
				GamaServerMessage.Type.SimulationStatus);
	}

	@Override
	public void updateExperimentStatus(final IScope scope) {
		if (!canSendMessage(scope.getExperiment())) return;
		sendMessage(scope.getExperiment(), json.object("message", null, "icon", "overlays/status.clock").toString(),
				GamaServerMessage.Type.SimulationStatusInform);
	}

}