/*******************************************************************************************************
 *
 * IGamaRNG.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.util.random;

import java.util.Random;

/**
 * The Class IGamaRNG.
 */
public interface IGamaRNG {

	/**
	 * Gets the usage.
	 *
	 * @return the usage
	 */
	int getUsage();

	/**
	 * Sets the usage.
	 *
	 * @param usage
	 *            the new usage
	 */
	default void setUsage(final int usage) {
		//TODO: If nextInt(nb) with nb a power of 2 we draw less than if not
		// but this doesn't reflect on the rng_usage
		for (long i = 0; i < usage; i++) { nextInt(32); }
	}

//	/**
//	 * Next int.
//	 *
//	 * @return the int
//	 */
//	int nextInt();
	
	
	/**
	 * Draws an int in the interval from 0 (inclusive) to upper_bound (exclusive) with uniform probability
	 * @param i
	 * @return
	 */
	int nextInt(int upper_bound);
	
	/**
	 * Draws an int in the interval lower (inclusive) to upper (exclusive)
	 * @param lower
	 * @param upper
	 * @return
	 */
	int nextInt(int lower, int upper);

	/**
	 * Next double.
	 *
	 * @see java.util.Random#nextDouble()
	 *
	 * @return the double
	 */
	double nextDouble();

	/**
	 * Next gaussian.
	 *
	 * @see java.util.Random#nextGaussian()
	 *
	 * @return the double
	 */
	double nextGaussian();

	/**
	 * Gets the random generator.
	 *
	 * @return the random generator
	 */
	Random getRandomGenerator();

}
