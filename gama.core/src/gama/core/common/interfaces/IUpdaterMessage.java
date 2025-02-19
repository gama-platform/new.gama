/*******************************************************************************************************
 *
 * IUpdaterMessage.java, in gama.annotations, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.common.interfaces;

/**
 * The Interface IUpdaterMessage.
 */
public interface IUpdaterMessage {

	String PROGRESS_ICON = "progress";

	public enum StatusType {
		ERROR, INFORM, USER, SUBTASK, EXPERIMENT, WAIT, NONE;
	}

	StatusType getType();

}