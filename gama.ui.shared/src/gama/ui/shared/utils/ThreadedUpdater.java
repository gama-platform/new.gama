/*******************************************************************************************************
 *
 * ThreadedUpdater.java, in gama.ui.shared.shared, is part of the source code of the GAMA modeling and simulation
 * platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.shared.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

import gama.core.common.interfaces.IUpdaterMessage;
import gama.core.common.interfaces.IUpdaterMessage.StatusType;
import gama.core.common.interfaces.IUpdaterTarget;

/**
 * Class ThreadedUpdater.
 *
 * @author drogoul
 * @since 10 mars 2014
 *
 */
public class ThreadedUpdater<Message extends IUpdaterMessage> extends UIJob implements IUpdaterTarget<Message> {

	/** The message. */
	Message message = null;

	/** The experimentControl. */
	private IUpdaterTarget<Message> experimentControl = new IUpdaterTarget<>() {};
	private IUpdaterTarget<Message> statusControl = new IUpdaterTarget<>() {};

	/**
	 * Instantiates a new threaded updater.
	 *
	 * @param name
	 *            the name
	 */
	public ThreadedUpdater(final String name) {
		super(WorkbenchHelper.getDisplay(), name);
		setPriority(DECORATE);
		setSystem(true);
	}

	@Override
	public boolean isDisposed() { return experimentControl.isDisposed() || statusControl.isDisposed(); }

	@Override
	public boolean isVisible() { return experimentControl.isVisible() && statusControl.isVisible(); }

	@Override
	public void updateWith(final Message m) {
		if (isDisposed() || !isVisible() || isBusy() || m == null) return;
		message = m;
		schedule();
	}

	/**
	 * Sets the target.
	 *
	 * @param l
	 *            the l
	 * @param s
	 *            the s
	 */
	public void setExperimentTarget(final IUpdaterTarget<Message> l) { experimentControl = l; }

	public void setStatusTarget(final IUpdaterTarget<Message> l) { statusControl = l; }

	@Override
	public boolean isBusy() {
		return false;

		// message != null
		// && (message.getType() == StatusType.EXPERIMENT && experimentControl.isBusy() || statusControl.isBusy());
	}

	@Override
	public IStatus runInUIThread(final IProgressMonitor monitor) {
		if (isDisposed()) return Status.CANCEL_STATUS;
		try {
			if (message != null && message.getType() == StatusType.EXPERIMENT) {
				if (experimentControl.isBusy() || !experimentControl.isVisible()) return Status.OK_STATUS;
				experimentControl.updateWith(message);
			} else {
				if (statusControl.isBusy() || !statusControl.isVisible()) return Status.OK_STATUS;
				statusControl.updateWith(message);
			}
		} finally {
			message = null;
		}
		return Status.OK_STATUS;
	}

	@Override
	public void reset() {
		statusControl.reset();
		experimentControl.reset();
	}

}
