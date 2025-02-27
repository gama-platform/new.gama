/*******************************************************************************************************
 *
 * SimulationOutputManager.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * (v.2024-06).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.outputs;

import java.util.HashMap;
import java.util.Map;

import gama.annotations.precompiler.GamlAnnotations.doc;
import gama.annotations.precompiler.GamlAnnotations.example;
import gama.annotations.precompiler.GamlAnnotations.facet;
import gama.annotations.precompiler.GamlAnnotations.facets;
import gama.annotations.precompiler.GamlAnnotations.inside;
import gama.annotations.precompiler.GamlAnnotations.symbol;
import gama.annotations.precompiler.GamlAnnotations.usage;
import gama.annotations.precompiler.IConcept;
import gama.annotations.precompiler.ISymbolKind;
import gama.core.common.interfaces.IKeyword;
import gama.core.kernel.simulation.SimulationAgent;
import gama.core.outputs.SimulationOutputManager.SimulationOutputValidator;
import gama.core.runtime.GAMA;
import gama.core.runtime.IScope;
import gama.gaml.compilation.IDescriptionValidator;
import gama.gaml.compilation.annotations.validator;
import gama.gaml.descriptions.IDescription;
import gama.gaml.factories.DescriptionFactory;
import gama.gaml.interfaces.IGamlIssue;
import gama.gaml.types.IType;

/**
 * The Class OutputManager.
 *
 * @author Alexis Drogoul modified by Romain Lavaud 05.07.2010
 */

/**
 * The Class SimulationOutputManager.
 *
 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
 * @date 16 sept. 2023
 */
@symbol (
		name = IKeyword.OUTPUT,
		kind = ISymbolKind.OUTPUT,
		with_sequence = true,
		concept = { IConcept.DISPLAY, IConcept.OUTPUT })
@facets ({ @facet (
		name = "synchronized",
		type = IType.BOOL,
		optional = true,
		doc = @doc (
				value = "Indicates whether the displays that compose this output should be synchronized with the simulation cycles")),
		@facet (
				name = IKeyword.AUTOSAVE,
				type = { IType.BOOL, IType.STRING },
				optional = true,
				doc = @doc ("Allows to save the whole screen on disk. A value of true/false will save it with the resolution of the physical screen. Passing it a string allows to define the filename "
						+ "Note that setting autosave to true (or to any other value than false) will synchronize all the displays defined in the experiment")) })

@inside (
		kinds = { ISymbolKind.MODEL, ISymbolKind.EXPERIMENT })
@doc (
		value = "`output` blocks define how to visualize a simulation (with one or more display blocks that define separate windows). It will include a set of displays, monitors and files statements. It will be taken into account only if the experiment type is `gui`.",
		usages = { @usage (
				value = "Its basic syntax is: ",
				examples = { @example (
						value = "experiment exp_name type: gui {",
						isExecutable = false),
						@example (
								value = "   // [inputs]",
								isExecutable = false),
						@example (
								value = "   output {",
								isExecutable = false),
						@example (
								value = "      // [display, file, inspect, layout or monitor statements]",
								isExecutable = false),
						@example (
								value = "   }",
								isExecutable = false),
						@example (
								value = "}",
								isExecutable = false) }) },
		see = { IKeyword.DISPLAY, IKeyword.MONITOR, IKeyword.INSPECT, IKeyword.LAYOUT })
@validator (SimulationOutputValidator.class)
public class SimulationOutputManager extends AbstractOutputManager {

	/**
	 * The Class SimulationOutputValidator.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @date 16 sept. 2023
	 */
	public static class SimulationOutputValidator implements IDescriptionValidator<IDescription> {

		@Override
		public void validate(final IDescription description) {
			Iterable<IDescription> displays = description.getChildrenWithKeyword(DISPLAY);
			Map<String, IDescription> map = new HashMap<>();
			for (IDescription display : displays) {
				String s = display.getName();
				if (s == null) { continue; }
				IDescription existing = map.get(s);
				if (existing == null) {
					map.put(s, display);
					continue;
				}

				display.info("'" + s
						+ "' is defined twice in this experiment. Only this definition will be taken into account.",
						IGamlIssue.DUPLICATE_DEFINITION);
				existing.info("'" + s
						+ "' is defined twice in this experiment. This definition will not be taken into account.",
						IGamlIssue.DUPLICATE_DEFINITION);
			}

		}

	}

	/**
	 * Creates the empty.
	 *
	 * @return the simulation output manager
	 */
	public static SimulationOutputManager createEmpty() {
		return new SimulationOutputManager(DescriptionFactory.create(IKeyword.OUTPUT, (String[]) null));
	}

	/**
	 * Instantiates a new simulation output manager.
	 *
	 * @param desc
	 *            the desc
	 */
	public SimulationOutputManager(final IDescription desc) {
		super(desc);

	}

	@Override
	public boolean init(final IScope scope) {
		boolean[] result = { true };
		scope.getGui().getStatus().waitStatus(scope, " Building outputs ", () -> {
			result[0] = super.init(scope);
			updateDisplayOutputsName(scope.getSimulation());
			scope.getGui().getStatus().updateExperimentStatus(scope);
		});
		return result[0];
	}

	/**
	 * Update display outputs name.
	 *
	 * @param agent
	 *            the agent
	 */
	public void updateDisplayOutputsName(final SimulationAgent agent) {
		for (final IOutput out : this) { GAMA.getGui().updateViewTitle(out, agent); }
	}

}
