/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: - Mickael Istria (Red Hat Inc.) - extract from QuickAccessElement
 *******************************************************************************/

package gama.ui.shared.access;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.ui.internal.quickaccess.CamelUtil;
import org.eclipse.ui.quickaccess.QuickAccessElement;

import gama.gaml.compilation.GamlIdiomsProvider;
import gama.gaml.interfaces.IGamlDescription;

/**
 * QuickAccessMatch contains the logic to check whether a given {@link QuickAccessElement} matches a input user request.
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class GamlAccessMatcher {

	private final IGamlDescription element;

	public GamlAccessMatcher(final IGamlDescription element) {
		this.element = element;
	}

	private static final int[][] EMPTY_INDICES = {};
	private static final String WS_WILD_START = "^\\s*(\\*|\\?)*"; //$NON-NLS-1$
	private static final String WS_WILD_END = "(\\*|\\?)*\\s*$"; //$NON-NLS-1$
	private static final String ANY_WS = "\\s+"; //$NON-NLS-1$
	private static final String EMPTY_STR = ""; //$NON-NLS-1$
	private static final String PAR_START = "\\("; //$NON-NLS-1$
	private static final String PAR_END = "\\)"; //$NON-NLS-1$
	private static final String ONE_CHAR = ".?"; //$NON-NLS-1$

	// whitespaces filter and patterns
	private String wsFilter;
	private Pattern wsPattern;

	/**
	 * Get the existing {@link Pattern} for the given filter, or create a new one. The generated pattern will replace
	 * whitespace with * to match all.
	 */
	private Pattern getWhitespacesPattern(final String filter) {
		if (wsPattern == null || !filter.equals(wsFilter)) {
			wsFilter = filter;
			String sFilter = filter.replaceFirst(WS_WILD_START, EMPTY_STR).replaceFirst(WS_WILD_END, EMPTY_STR)
					.replaceAll(PAR_START, ONE_CHAR).replaceAll(PAR_END, ONE_CHAR);
			sFilter = String.format(".*(%s).*", sFilter.replaceAll(ANY_WS, ").*(")); //$NON-NLS-1$//$NON-NLS-2$
			wsPattern = safeCompile(sFilter);
		}
		return wsPattern;
	}

	// wildcard filter and patterns
	private String wcFilter;
	private Pattern wcPattern;

	/**
	 * Get the existing {@link Pattern} for the given filter, or create a new one. The generated pattern will handle '*'
	 * and '?' wildcards.
	 */
	private Pattern getWildcardsPattern(String filter) {
		// squash consecutive **** into a single *
		filter = filter.replaceAll("\\*+", "*"); //$NON-NLS-1$ //$NON-NLS-2$
		if (wcPattern == null || !filter.equals(wcFilter)) {
			wcFilter = filter;
			String sFilter = filter.replaceFirst(WS_WILD_START, EMPTY_STR).replaceFirst(WS_WILD_END, EMPTY_STR)
					.replaceAll(PAR_START, ONE_CHAR).replaceAll(PAR_END, ONE_CHAR);
			// replace '*' and '?' with their matchers ").*(" and ").?("
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < sFilter.length(); i++) {
				char c = sFilter.charAt(i);
				if (c == '*') {
					sb.append(").").append(c).append("("); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (c == '?') {
					int n = 1;
					for (; i + 1 < sFilter.length(); i++) {
						if (sFilter.charAt(i + 1) != '?') { break; }
						n++;
					}
					sb.append(").").append(n == 1 ? '?' : String.format("{0,%d}", n)).append("("); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else {
					sb.append(c);
				}
			}
			sFilter = String.format(".*(%s).*", sb.toString()); //$NON-NLS-1$
			// remove empty capturing groups
			sFilter = sFilter.replace("()", EMPTY_STR); //$NON-NLS-1$
			//
			wcPattern = safeCompile(sFilter);
		}
		return wcPattern;
	}

	/**
	 * A safe way to compile some unknown pattern, avoids possible {@link PatternSyntaxException}. If the pattern can't
	 * be compiled, some not matching pattern will be returned.
	 *
	 * @param pattern
	 *            some pattern to compile, not null
	 * @return a {@link Pattern} object compiled from given input or a dummy pattern which do not match anything
	 */
	private static Pattern safeCompile(final String pattern) {
		try {
			return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		} catch (Exception e) {
			// A "bell" special character: should not match anything we can get
			return Pattern.compile("\\a"); //$NON-NLS-1$
		}
	}

	/**
	 * If this element is a match (partial, complete, camel case, etc) to the given filter, returns a
	 * {@link GamlAccessEntry}. Otherwise returns <code>null</code>;
	 *
	 * @param filter
	 *            filter for matching
	 * @param providerForMatching
	 *            the provider that will own the entry
	 * @return a quick access entry or <code>null</code>
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public GamlAccessEntry match(final String filter, final GamlIdiomsProvider providerForMatching) {
		String matchLabel = element.getName();
		// first occurrence of filter
		int index = matchLabel.toLowerCase().indexOf(filter);
		if (index != -1) {
			index = element.getTitle().toLowerCase().indexOf(filter);
			if (index != -1) { // match actual label
				int quality = matchLabel.toLowerCase().equals(filter) ? GamlAccessEntry.MATCH_PERFECT
						: matchLabel.toLowerCase().startsWith(filter) ? GamlAccessEntry.MATCH_EXCELLENT
						: GamlAccessEntry.MATCH_GOOD;
				return new GamlAccessEntry(element, providerForMatching,
						new int[][] { { index, index + filter.length() - 1 } }, EMPTY_INDICES, quality);
			}
			return new GamlAccessEntry(element, providerForMatching, EMPTY_INDICES, EMPTY_INDICES,
					GamlAccessEntry.MATCH_PARTIAL);
		}
		//
		Pattern p;
		if (filter.contains("*") || filter.contains("?")) { //$NON-NLS-1$ //$NON-NLS-2$
			// check for wildcards
			p = getWildcardsPattern(filter);
		} else {
			// check for whitespaces
			p = getWhitespacesPattern(filter);
		}
		Matcher m = p.matcher(matchLabel);
		// if matches, return an entry
		if (m.matches()) {
			// and highlight match on the label only
			String label = element.getTitle();
			if (!matchLabel.equals(label)) {
				m = p.matcher(element.getTitle());
				if (!m.matches()) return new GamlAccessEntry(element, providerForMatching, EMPTY_INDICES, EMPTY_INDICES,
						GamlAccessEntry.MATCH_GOOD);
			}
			int groupCount = m.groupCount();
			int[][] indices = new int[groupCount][];
			for (int i = 0; i < groupCount; i++) {
				int nGrp = i + 1;
				// capturing group
				indices[i] = new int[] { m.start(nGrp), m.end(nGrp) - 1 };
			}
			// return match and list of indices
			int quality = GamlAccessEntry.MATCH_EXCELLENT;
			return new GamlAccessEntry(element, providerForMatching, indices, EMPTY_INDICES, quality);
		}
		//
		String combinedMatchLabel = providerForMatching.getSearchCategory() + " " + element.getName(); //$NON-NLS-1$
		String combinedLabel = providerForMatching.getSearchCategory() + " " + element.getTitle(); //$NON-NLS-1$
		index = combinedMatchLabel.toLowerCase().indexOf(filter);
		if (index != -1) { // match
			index = combinedLabel.toLowerCase().indexOf(filter);
			if (index != -1) { // compute highlight on label
				int lengthOfElementMatch =
						index + filter.length() - providerForMatching.getSearchCategory().length() - 1;
				if (lengthOfElementMatch > 0) return new GamlAccessEntry(element, providerForMatching,
						new int[][] { { 0, lengthOfElementMatch - 1 } },
						new int[][] { { index, index + filter.length() - 1 } }, GamlAccessEntry.MATCH_GOOD);
				return new GamlAccessEntry(element, providerForMatching, EMPTY_INDICES,
						new int[][] { { index, index + filter.length() - 1 } }, GamlAccessEntry.MATCH_GOOD);
			}
			return new GamlAccessEntry(element, providerForMatching, EMPTY_INDICES, EMPTY_INDICES,
					GamlAccessEntry.MATCH_PARTIAL);
		}
		//
		String camelCase = CamelUtil.getCamelCase(element.getName()); // use actual label for camelcase
		index = camelCase.indexOf(filter);
		if (index != -1) {
			int[][] indices = CamelUtil.getCamelCaseIndices(matchLabel, index, filter.length());
			return new GamlAccessEntry(element, providerForMatching, indices, EMPTY_INDICES,
					GamlAccessEntry.MATCH_GOOD);
		}
		String combinedCamelCase = CamelUtil.getCamelCase(combinedLabel);
		index = combinedCamelCase.indexOf(filter);
		if (index != -1) {
			String providerCamelCase = CamelUtil.getCamelCase(providerForMatching.getSearchCategory());
			int lengthOfElementMatch = index + filter.length() - providerCamelCase.length();
			if (lengthOfElementMatch > 0) return new GamlAccessEntry(element, providerForMatching,
					CamelUtil.getCamelCaseIndices(matchLabel, 0, lengthOfElementMatch),
					CamelUtil.getCamelCaseIndices(providerForMatching.getSearchCategory(), index,
							filter.length() - lengthOfElementMatch),
					GamlAccessEntry.MATCH_GOOD);
			return new GamlAccessEntry(element, providerForMatching, EMPTY_INDICES,
					CamelUtil.getCamelCaseIndices(providerForMatching.getSearchCategory(), index, filter.length()),
					GamlAccessEntry.MATCH_GOOD);
		}
		return null;
	}
}
