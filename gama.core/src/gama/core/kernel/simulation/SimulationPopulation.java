/*******************************************************************************************************
 *
 * SimulationPopulation.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * (v.2024-06).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.kernel.simulation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gama.core.common.interfaces.IKeyword;
import gama.core.kernel.experiment.ExperimentAgent;
import gama.core.kernel.experiment.ExperimentPlan;
import gama.core.kernel.experiment.ParametersSet;
import gama.core.metamodel.agent.IAgent;
import gama.core.metamodel.agent.ISerialisedAgent;
import gama.core.metamodel.population.GamaPopulation;
import gama.core.metamodel.shape.GamaPoint;
import gama.core.metamodel.topology.continuous.AmorphousTopology;
import gama.core.runtime.GAMA;
import gama.core.runtime.IScope;
import gama.core.runtime.concurrent.GamaExecutorService;
import gama.core.runtime.concurrent.GamaExecutorService.Caller;
import gama.core.runtime.concurrent.ISimulationRunner;
import gama.core.runtime.concurrent.SimulationRunner;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.core.util.GamaListFactory;
import gama.core.util.IList;
import gama.gaml.compilation.IAgentConstructor;
import gama.gaml.species.ISpecies;
import gama.gaml.statements.RemoteSequence;
import gama.gaml.variables.IVariable;

/**
 * The Class SimulationPopulation.
 */
@SuppressWarnings ({ "rawtypes", "unchecked" })
public class SimulationPopulation extends GamaPopulation<SimulationAgent> {

	/** The current simulation. */
	private SimulationAgent currentSimulation;

	/** The runner. */
	private final ISimulationRunner runner;

	/**
	 * Instantiates a new simulation population.
	 *
	 * @param agent
	 *            the agent
	 * @param species
	 *            the species
	 */
	public SimulationPopulation(final ExperimentAgent agent, final ISpecies species) {
		super(agent, species);
		runner = SimulationRunner.of(this);
	}

	/**
	 * Gets the max number of concurrent simulations.
	 *
	 * @return the max number of concurrent simulations
	 */
	public int getMaxNumberOfConcurrentSimulations() {
		return GamaExecutorService.getParallelism(getHost().getScope(), getHost().getSpecies().getConcurrency(),
				Caller.SIMULATION);
	}

	@Override
	public void removeValue(final IScope scope, final Object value) {
		if (value instanceof SimulationAgent sim) {
			int index = indexOf(sim);
			if (index == -1) return;
			setCurrentSimulation(nextSimulationAfter(index));
			super.removeValue(scope, value);
		}
	}

	/**
	 * Next simulation after.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param index
	 *            the index
	 * @return the simulation agent
	 * @date 26 août 2023
	 */
	private SimulationAgent nextSimulationAfter(final int index) {
		if (size() <= 1 || index == -1) return null;
		return get((index + 1) % size());
	}

	/**
	 * Method fireAgentRemoved()
	 *
	 */
	@Override
	protected void fireAgentRemoved(final IScope scope, final IAgent old) {
		super.fireAgentRemoved(scope, old);
		runner.remove((SimulationAgent) old);
	}

	@Override
	public void initializeFor(final IScope scope) {
		computeTopology(scope);
		if (topology != null) { topology.initialize(scope, this); }
		this.currentAgentIndex = 0;
	}

	@Override
	public void dispose() {
		runner.dispose();
		// currentSimulation = null;
		super.dispose();
	}

	@Override
	public Iterable<SimulationAgent> iterable(final IScope scope) {
		return (Iterable<SimulationAgent>) getAgents(scope);
	}

	@Override
	public IList<SimulationAgent> createAgents(final IScope scope, final int number,
			final List<? extends Map<String, Object>> initialValues, final boolean isRestored,
			final boolean toBeScheduled, final RemoteSequence sequence) throws GamaRuntimeException {

		final IList<SimulationAgent> result = GamaListFactory.create(SimulationAgent.class);
		final IAgentConstructor<SimulationAgent> constr = species.getDescription().getAgentConstructor();
		scope.getGui().getStatus().waitStatus(scope, "Initializing simulations", () -> {
			for (int i = 0; i < number; i++) {

				// Model do not only rely on SimulationAgent

				SimulationAgent sim = constr.createOneAgent(this, currentAgentIndex++);
				sim.setScheduled(toBeScheduled);
				sim.setName("Simulation " + sim.getIndex());
				add(sim);
				boolean isBatch = getHost().getSpecies().isBatch();
				// Batch experiments now dont allow their simulations to have outputs

				if (!isBatch) { sim.setOutputs(((ExperimentPlan) host.getSpecies()).getOriginalSimulationOutputs()); }
				if (!scope.interrupted()) {
					// Necessary to set it early -- see Issue #3872
					setCurrentSimulation(sim);
					initSimulation(scope, sim, initialValues, i, isRestored, toBeScheduled, sequence);
					if (toBeScheduled) { runner.add(sim); }
					result.add(sim);
				}
			}
		});

		// Linked to Issue #2430. Should not return this, but the newly created simulations
		// return this;
		return result;
	}

	/**
	 * Inits the simulation.
	 *
	 * @param scope
	 *            the scope
	 * @param sim
	 *            the sim
	 * @param initialValues
	 *            the initial values
	 * @param index
	 *            the index
	 * @param isRestored
	 *            the is restored
	 * @param toBeScheduled
	 *            the to be scheduled
	 * @param sequence
	 *            the sequence
	 */
	private void initSimulation(final IScope scope, final SimulationAgent sim,
			final List<? extends Map<String, Object>> initialValues, final int index, final boolean isRestored,
			final boolean toBeScheduled, final RemoteSequence sequence) {
		scope.getGui().getStatus().waitStatus(scope, "Instantiating agents", () -> {
			final Map<String, Object> firstInitValues =
					initialValues.isEmpty() ? ParametersSet.EMPTY : initialValues.get(index);
			final Object firstValue = !firstInitValues.isEmpty() ? firstInitValues.values().toArray()[0] : null;
			if (firstValue instanceof ISerialisedAgent sa) {
				sim.updateWith(scope, sa);
			} else {
				// See issue #130 -- we add the parameters values to make sure they are passed (but not for batch).
				if (!getHost().getSpecies().isBatch()) { sim.setExternalInits(getHost().getParameterValues()); }
				sim.setExternalInits(firstInitValues);
				createVariablesFor(sim.getScope(), Collections.singletonList(sim),
						Collections.singletonList(sim.getExternalInits()));
			}

			if (toBeScheduled) {
				if (isRestored || firstValue instanceof ISerialisedAgent) {
					sim.initOutputs();
				} else {
					sim.schedule(scope);
					if (sequence != null && !sequence.isEmpty()) { scope.execute(sequence, sim, null); }
				}
			}
		});

	}

	@Override
	protected boolean allowVarInitToBeOverridenByExternalInit(final IVariable theVar) {
		return switch (theVar.getName()) {
			case IKeyword.SEED, IKeyword.RNG -> !theVar.hasFacet(IKeyword.INIT);
			default -> true;
		};
	}

	@Override
	public ExperimentAgent getHost() { return (ExperimentAgent) super.getHost(); }

	@Override
	public SimulationAgent getAgent(final IScope scope, final GamaPoint value) {
		return get(null, 0);
	}

	/**
	 * Sets the host.
	 *
	 * @param agent
	 *            the new host
	 */
	public void setHost(final ExperimentAgent agent) { host = agent; }

	@Override
	public void computeTopology(final IScope scope) throws GamaRuntimeException {
		// Temporary topology set before the world gets a shape
		topology = new AmorphousTopology();
	}

	@Override
	protected boolean stepAgents(final IScope scope) {
		runner.step();
		return true;
	}

	/**
	 * This method can be called by the batch experiments to temporarily stop (unschedule) a simulation
	 *
	 * @param sim
	 */
	public void unscheduleSimulation(final SimulationAgent sim) {
		runner.remove(sim);
	}

	/**
	 * Gets the number of active stepables.
	 *
	 * @return the number of active stepables
	 */
	public Set<SimulationAgent> getRunningSimulations() { return runner.getStepable(); }

	/**
	 * Gets the number of active threads.
	 *
	 * @return the number of active threads
	 */
	public int getNumberOfActiveThreads() { return runner.getActiveThreads(); }

	/**
	 * @return
	 */
	public boolean hasScheduledSimulations() {
		return runner.hasSimulations();
	}

	/**
	 * Last simulation created.
	 *
	 * @return the simulation agent
	 */
	public SimulationAgent getCurrentSimulation() { return currentSimulation; }

	/**
	 * Sets the current simulation.
	 *
	 * @param current
	 *            the new current simulation
	 */
	public void setCurrentSimulation(final SimulationAgent current) {
		currentSimulation = current;
		GAMA.changeCurrentTopLevelAgent(current == null ? getHost() : current, false);
	}

}