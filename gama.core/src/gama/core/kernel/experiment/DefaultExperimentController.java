/*******************************************************************************************************
 *
 * DefaultExperimentController.java, in gama.core, is part of the source code of the GAMA modeling and simulation
 * platform (v.2024-06).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.kernel.experiment;

import java.util.concurrent.ArrayBlockingQueue;

import gama.core.runtime.GAMA;
import gama.core.runtime.IExperimentStateListener;
import gama.core.runtime.IScope;
import gama.core.runtime.concurrent.GamaExecutorService;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.dev.DEBUG;

/**
 * The Class ExperimentController.
 */
public class DefaultExperimentController extends AbstractExperimentController {

	/** The execution thread. */

	/** The agent. */
	private IExperimentAgent agent;

	/** The r. */

	/** The command thread. */
	private final Thread executionThread;

	/**
	 * Instantiates a new experiment controller.
	 *
	 * @param experiment
	 *            the experiment
	 */
	public DefaultExperimentController(final IExperimentPlan experiment) {
		commands = new ArrayBlockingQueue<>(10);
		this.experiment = experiment;
		executionThread = new Thread(() -> { while (experimentAlive) { step(); } }, "Front end scheduler");
		executionThread.setUncaughtExceptionHandler(GamaExecutorService.EXCEPTION_HANDLER);
		commandThread.setUncaughtExceptionHandler(GamaExecutorService.EXCEPTION_HANDLER);
		lock.acquire();
		commandThread.start();
		executionThread.start();
	}

	/**
	 * Process user command.
	 *
	 * @param command
	 *            the command
	 */
	@Override
	protected boolean processUserCommand(final ExperimentCommand command) {
		final IScope scope = getScope();
		switch (command) {
			case _CLOSE:
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.NONE);
				return true;
			case _OPEN:
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.NOTREADY);
				try {
					experiment.open();
					return true;
					// Following a comment made here: #3925 and in
					// https://github.com/gama-platform/gama/commit/8068457d11d25289bf001bb6f29553e4037f1cda#r130876638,
					// removes the thread
					// new Thread(() -> experiment.open()).start();
				} catch (final GamaRuntimeException e) {
					DEBUG.ERR("Error in previous experiment: " + e.getMessage());
					closeExperiment(e);
					return false;
				} catch (final Exception e) {
					DEBUG.ERR("Error when opening the experiment: " + e.getMessage());
					closeExperiment(e);
					return false;
				}
			case _START:
				try {
					paused = false;
					lock.release();
					return true;
				} catch (final GamaRuntimeException e) {
					closeExperiment(e);
					return false;
				} finally {
					GAMA.updateExperimentState(experiment, IExperimentStateListener.State.RUNNING);
				}
			case _PAUSE:
				paused = true;
				if (!disposing) { GAMA.updateExperimentState(experiment, IExperimentStateListener.State.PAUSED); }
				return true;
			case _STEP:
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.PAUSED);
				paused = true;
				lock.release();
				return true;
			case _BACK:
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.PAUSED);
				paused = true;
				experiment.getAgent().backward(getScope());// ?? scopes[0]);
				return true;
			case _RELOAD:
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.NOTREADY);
				try {
					final boolean wasRunning = !isPaused() && !experiment.isAutorun();
					paused = true;
					scope.getGui().getStatus().waitStatus(scope, "Reloading...", () -> experiment.reload());
					if (wasRunning) return processUserCommand(ExperimentCommand._START);
					scope.getGui().getStatus().informStatus(scope, "Experiment reloaded");
					return true;
				} catch (final GamaRuntimeException e) {
					closeExperiment(e);
					return false;
				} catch (final Throwable e) {
					closeExperiment(GamaRuntimeException.create(e, scope));
					return false;
				} finally {
					GAMA.updateExperimentState(experiment);
				}
		}
		return false;
	}

	@Override
	public void dispose() {
		scope = null;
		agent = null;
		if (experiment != null) {
			try {
				paused = true;
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.NOTREADY);
				getScope().getGui().closeDialogs(getScope());
				// Dec 2015 This method is normally now called from
				// ExperimentPlan.dispose()
				GAMA.updateExperimentState(experiment, IExperimentStateListener.State.NONE);
			} finally {
				acceptingCommands = false;
				experimentAlive = false;
				lock.release();
				if (commandThread != null && commandThread.isAlive()) {
					// Disposing, so no need to pay attention to the result of offer()
					commands.offer(ExperimentCommand._CLOSE);
				}
			}
		}
	}

	@Override
	public void close() {
		closeExperiment(null);
	}

	/**
	 * Close experiment.
	 *
	 * @param e
	 *            the e
	 */
	public void closeExperiment(final Exception e) {
		disposing = true;
		if (e != null) { getScope().getGui().getStatus().errorStatus(scope, e); }
		experiment.dispose(); // will call own dispose() later
	}

	/**
	 * Checks if is paused.
	 *
	 * @return true, if is paused
	 */
	@Override
	public boolean isPaused() { return paused; }

	/**
	 * Schedule.
	 *
	 * @param scope
	 *            the scope
	 * @param agent
	 *            the agent
	 */
	@Override
	public void schedule(final ExperimentAgent agent) {
		this.agent = agent;
		scope = agent.getScope();
		serverConfiguration = GAMA.getPlatformAgent().getServer() != null
				? GAMA.getPlatformAgent().getServer().obtainGuiServerConfiguration() : null;
		scope.setServerConfiguration(serverConfiguration);
		try {
			if (!scope.init(agent).passed()) {
				scope.setDisposeStatus();
			} else if (agent instanceof TestAgent || agent.getSpecies().isAutorun()) { asynchronousStart(); }
		} catch (final Throwable e) {
			if (scope != null && scope.interrupted()) {} else if (!(e instanceof GamaRuntimeException)) {
				GAMA.reportError(scope, GamaRuntimeException.create(e, scope), true);
			}
		}
	}

	/**
	 * Step.
	 */
	protected void step() {
		if (paused) {
			lock.acquire();
			// experimentAlive = false;
		}
		try {
			if (scope == null) return;
			IScope savedScope = scope;
			if (!savedScope.step(agent).passed()) {
				savedScope.setDisposeStatus();
				paused = true;
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

}
