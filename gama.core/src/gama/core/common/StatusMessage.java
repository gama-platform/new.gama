/*******************************************************************************************************
 *
 * StatusMessage.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.common;

import gama.core.common.interfaces.IUpdaterMessage;
import gama.core.runtime.IScope;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.core.util.GamaColor;

/**
 * The Class StatusMessage.
 */
public record StatusMessage(String message, StatusType type, String icon, GamaColor color, Boolean begin,
		Double completion, GamaRuntimeException exception) implements IUpdaterMessage {

	static GamaColor WAIT_AND_TASK_COLOR = GamaColor.get(207, 119, 56);
	static GamaColor INFORM_COLOR = GamaColor.get(102, 114, 126);
	static GamaColor ERROR_COLOR = GamaColor.get(158, 77, 77);
	static GamaColor NEUTRAL_COLOR = GamaColor.get(102, 114, 126);

	public static StatusMessage USER(final String msg, final String icon, final GamaColor color) {
		return new StatusMessage(msg, StatusType.USER, icon, color, null, null, null);
	}

	public static StatusMessage WAIT(final String msg) {
		return new StatusMessage(msg, StatusType.WAIT, null, WAIT_AND_TASK_COLOR, true, null, null);
	}

	public static StatusMessage INFORM(final String msg) {
		return new StatusMessage(msg, StatusType.INFORM, null, INFORM_COLOR, true, null, null);
	}

	public static StatusMessage ERROR(final IScope scope, final Exception e) {
		return new StatusMessage("Error in previous experiment", StatusType.ERROR, "overlays/small.close", ERROR_COLOR,
				null, null, GamaRuntimeException.create(e, scope));
	}

	public static StatusMessage BEGIN(final String msg) {
		return new StatusMessage(msg, StatusType.SUBTASK, PROGRESS_ICON, WAIT_AND_TASK_COLOR, true, null, null);
	}

	public static StatusMessage END(final String msg) {
		return new StatusMessage(msg, StatusType.SUBTASK, PROGRESS_ICON, WAIT_AND_TASK_COLOR, false, null, null);
	}

	public static StatusMessage COMPLETION(final String msg, final Double completion) {
		return new StatusMessage(msg, StatusType.SUBTASK, PROGRESS_ICON, WAIT_AND_TASK_COLOR, false, completion, null);
	}

	public static StatusMessage CUSTOM(final String msg, final StatusType s, final String icon) {
		return CUSTOM(msg, s, icon, NEUTRAL_COLOR);
	}

	public static StatusMessage CUSTOM(final String msg, final StatusType s, final String icon, final GamaColor color) {
		return new StatusMessage(msg, s, icon, color, null, null, null);
	}

	private static StatusMessage EXPERIMENT =
			new StatusMessage(null, StatusType.EXPERIMENT, null, null, null, null, null);

	public static StatusMessage EXPERIMENT() {
		return EXPERIMENT;
	}

	@Override
	public StatusType getType() { return type(); }

}