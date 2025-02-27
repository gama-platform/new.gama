/*******************************************************************************************************
 *
 * StatusDisplayerFactory.java, in gama.ui.shared.experiment, is part of the source code of the GAMA modeling and
 * simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.shared.factories;

import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

import gama.core.common.interfaces.IStatusDisplayer;

/**
 * A factory for creating StatusDisplayer objects.
 */
public class StatusDisplayerFactory extends AbstractServiceFactory {

	/** The displayer. */
	IStatusDisplayer displayer = new StatusDisplayer();

	@Override
	public Object create(final Class serviceInterface, final IServiceLocator parentLocator,
			final IServiceLocator locator) {
		return displayer;
	}

}
