/*******************************************************************************************************
 *
 * IOverlayProvider.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.common.interfaces;

import gama.core.outputs.layers.OverlayStatement.OverlayInfo;

/**
 * Class IOverlay.
 *
 * @author drogoul
 * @since 9 mars 2014
 *
 */
public interface IOverlayProvider<C extends OverlayInfo> {

	/**
	 * Sets the target.
	 *
	 * @param overlay
	 *            the overlay
	 * @param surface
	 *            the surface
	 */
	void setStatusTarget(IUpdaterTarget<C> overlay, IDisplaySurface surface);
}
