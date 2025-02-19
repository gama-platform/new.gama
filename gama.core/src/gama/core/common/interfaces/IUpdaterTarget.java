/*******************************************************************************************************
 *
 * IUpdaterTarget.java, in gama.annotations, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.common.interfaces;

/**
 * The Interface IUpdaterTarget.
 *
 * @param <Message>
 *            the generic type
 */
public interface IUpdaterTarget<Message extends IUpdaterMessage> {

	/**
	 * Checks if is disposed.
	 *
	 * @return true, if is disposed
	 */
	default boolean isDisposed() { return false; }

	/**
	 * Checks if is visible.
	 *
	 * @return true, if is visible
	 */
	default boolean isVisible() { return true; }

	/**
	 * Checks if is busy.
	 *
	 * @return true, if is busy
	 */
	default boolean isBusy() { return false; }

	/**
	 * Update with.
	 *
	 * @param m
	 *            the m
	 */
	default void updateWith(final Message m) {}

	/**
	 * Resume.
	 */
	default void reset() {}

}