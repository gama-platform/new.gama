/*******************************************************************************************************
 *
 * ThreadedOverlayUpdater.java, in gama.ui.shared.experiment, is part of the source code of the GAMA modeling and
 * simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.experiment.views.displays;

import gama.core.common.interfaces.IDisplaySurface;
import gama.core.common.interfaces.IOverlayProvider;
import gama.core.common.interfaces.IUpdaterTarget;
import gama.core.outputs.layers.OverlayStatement.OverlayInfo;
import gama.ui.shared.utils.ThreadedUpdater;

/**
 * The Class ThreadedOverlayUpdater.
 */
public class ThreadedOverlayUpdater extends ThreadedUpdater<OverlayInfo> implements IOverlayProvider<OverlayInfo> {

	/**
	 * Instantiates a new threaded overlay updater.
	 *
	 * @param displayOverlay
	 *            the display overlay
	 */
	public ThreadedOverlayUpdater(final DisplayOverlay displayOverlay) {
		super("Overlay refresh");
		setStatusTarget(displayOverlay);
	}

	@Override
	public void setStatusTarget(final IUpdaterTarget<OverlayInfo> overlay, final IDisplaySurface surface) {
		setStatusTarget(overlay);

	}

}