/*******************************************************************************************************
 *
 * ProgressCounter.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.util.file;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;

import org.geotools.util.SimpleInternationalString;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

import gama.core.common.interfaces.IStatusDisplayer;
import gama.core.runtime.GAMA;
import gama.core.runtime.IScope;
import gama.core.runtime.exceptions.GamaRuntimeException;

/**
 * The Class ProgressCounter.
 */
public class ProgressCounter implements ProgressListener, IIOReadProgressListener {

	/** The scope. */
	final IScope scope;

	/** The name. */
	final String name;

	/** The progress. */
	float progress;

	/**
	 * Instantiates a new progress counter.
	 *
	 * @param scope
	 *            the scope
	 * @param name
	 *            the name
	 */
	public ProgressCounter(final IScope scope, final String name) {
		this.scope = scope;
		this.name = name;
	}

	/**
	 * Gets the displayer.
	 *
	 * @return the displayer
	 */
	IStatusDisplayer getDisplayer() { return GAMA.getGui().getStatus(); }

	@Override
	public void complete() {
		getDisplayer().setTaskCompletion(scope, 1d);
	}

	@Override
	public void dispose() {
		getDisplayer().endTask(scope, name);
	}

	@Override
	public void exceptionOccurred(final Throwable arg0) {
		GAMA.reportAndThrowIfNeeded(scope, GamaRuntimeException.create(arg0, scope), true);
	}

	@Override
	public float getProgress() { return progress; }

	@Override
	public InternationalString getTask() { return new SimpleInternationalString(name); }

	@Override
	public boolean isCanceled() { return scope.interrupted(); }

	@Override
	public void progress(final float p) {
		progress = p;
		getDisplayer().setTaskCompletion(scope, progress);
	}

	@Override
	public void setCanceled(final boolean cancel) {
		getDisplayer().endTask(scope, name);
	}

	@Override
	public void setTask(final InternationalString n) {}

	@Override
	public void started() {
		getDisplayer().beginTask(scope, name);
	}

	@Override
	public void warningOccurred(final String source, final String location, final String warning) {
		GAMA.reportAndThrowIfNeeded(scope, GamaRuntimeException.warning(warning, scope), false);
	}

	@Override
	public void sequenceStarted(final ImageReader source, final int minIndex) {}

	@Override
	public void sequenceComplete(final ImageReader source) {}

	@Override
	public void imageStarted(final ImageReader source, final int imageIndex) {
		getDisplayer().beginTask(scope, name);
	}

	@Override
	public void imageProgress(final ImageReader source, final float percentageDone) {
		progress(percentageDone);
	}

	@Override
	public void imageComplete(final ImageReader source) {
		getDisplayer().setTaskCompletion(scope, 1d);
		getDisplayer().endTask(scope, name);
	}

	@Override
	public void thumbnailStarted(final ImageReader source, final int imageIndex, final int thumbnailIndex) {}

	@Override
	public void thumbnailProgress(final ImageReader source, final float percentageDone) {}

	@Override
	public void thumbnailComplete(final ImageReader source) {}

	@Override
	public void readAborted(final ImageReader source) {
		getDisplayer().endTask(scope, name);
	}

}