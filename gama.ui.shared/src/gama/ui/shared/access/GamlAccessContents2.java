/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation Tom Hochstein (Freescale) - Bug 393703 -
 * NotHandledException selecting inactive command under 'Previous Choices' in Quick access Lars Vogel
 * <Lars.Vogel@vogella.com> - Bug 472654, 491272, 491398 Leung Wang Hei <gemaspecial@yahoo.com.hk> - Bug 483343 Patrik
 * Suzzi <psuzzi@gmail.com> - Bug 491291, 491529, 491293, 492434, 492452, 459989, 507322
 *******************************************************************************/
package gama.ui.shared.access;

import static org.eclipse.ui.internal.progress.ProgressManagerUtil.getDefaultParent;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.quickaccess.QuickAccessMessages;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.FrameworkUtil;

import gama.gaml.compilation.GamlIdiomsProvider;
import gama.gaml.interfaces.IGamlDescription;
import gama.ui.shared.utils.WebHelper;

/**
 * Provides the contents for the quick access shell.
 */
public class GamlAccessContents2 extends PopupDialog {
	/**
	 * When opened in a popup we were given the command used to open it. Now that we have a shell, we are just using a
	 * hard coded command id.
	 */
	private static final String QUICK_ACCESS_COMMAND_ID = "org.eclipse.ui.window.quickAccess"; //$NON-NLS-1$

	@SuppressWarnings ("unchecked") private static final List<GamlAccessEntry>[] EMPTY_ENTRIES =
			(List<GamlAccessEntry>[]) Array.newInstance(List.class, 0);

	protected Text filterText;

	private final Map<IGamlDescription, GamlIdiomsProvider> elementsToProviders = new HashMap<>();

	protected Table table;

	private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

	protected String rememberedText;

	GridData browserData;

	/**
	 * A color for dulled out items created by mixing the table foreground. Will be disposed when the
	 * {@link #resourceManager} is disposed.
	 */
	private Color grayColor;
	private TextLayout textLayout;
	private boolean showAllMatches = true;
	protected boolean resized = false;
	private TriggerSequence keySequence;
	private Job computeProposalsJob;
	/** The popup. */
	GamlBrowserPane browser;
	// BrowserInformationControl popup;

	public GamlAccessContents2() {
		super(getDefaultParent(), SWT.RESIZE, true, true, false, false, false, "GAML Reference", null);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		boolean isWin32 = Util.isWindows();
		GridLayoutFactory.fillDefaults().extendedMargins(isWin32 ? 0 : 3, 3, 2, 2).numColumns(2).applyTo(composite);
		hintText = createHintText(composite, SWT.DEFAULT);
		GridData gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1);
		gridData.horizontalIndent = IDialogConstants.HORIZONTAL_MARGIN;
		hintText.setLayoutData(gridData);
		createTable(composite);
		createBrowser(composite);
		return composite;
	}

	@Override
	public int open() {

		int result = super.open();
		updateProposals("");
		return result;
	}

	/**
	 * Returns the number of items the table can fit in its current layout
	 */
	private int computeNumberOfItems() {
		Rectangle rect = table.getClientArea();
		int itemHeight = table.getItemHeight();
		int headerHeight = table.getHeaderHeight();
		return (rect.height - headerHeight + itemHeight - 1) / (itemHeight + table.getGridLineWidth());
	}

	/**
	 * Refreshes the contents of the quick access shell
	 *
	 * @param filter
	 *            The filter text to apply to results
	 */
	public void updateProposals(final String filter) {
		if (computeProposalsJob != null) {
			computeProposalsJob.cancel();
			computeProposalsJob = null;
		}
		if (table == null || table.isDisposed()) return;
		final Display display = table.getDisplay();

		// perfect match, to be selected in the table if not null
		// IGamlDescription perfectMatch = getPerfectMatch(filter);

		// String computingMessage = NLS.bind(QuickAccessMessages.QuickaAcessContents_computeMatchingEntries, filter);
		int maxNumberOfItemsInTable = computeNumberOfItems();
		AtomicReference<List<GamlAccessEntry>[]> entries = new AtomicReference<>();
		final Job currentComputeEntriesJob = Job.create("Computing entries", theMonitor -> {
			entries.set(computeMatchingEntries(filter, null, maxNumberOfItemsInTable, theMonitor));
			return theMonitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		});
		currentComputeEntriesJob.setPriority(Job.INTERACTIVE);
		// feedback is delayed in a job as we don't want to show it on every keystroke
		// but only when user seems to be waiting
		UIJob computingFeedbackJob = new UIJob(table.getDisplay(), "") {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				if (currentComputeEntriesJob.getResult() == null && !monitor.isCanceled() && !table.isDisposed()) {
					showHintText("Computing entries", grayColor);
					return Status.OK_STATUS;
				}
				return Status.CANCEL_STATUS;
			}
		};
		currentComputeEntriesJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				computingFeedbackJob.cancel();
				if (computeProposalsJob == currentComputeEntriesJob && event.getResult().isOK()
						&& !table.isDisposed()) {
					display.asyncExec(() -> {
						computingFeedbackJob.cancel();
						refreshTable(entries.get(), filter);
					});
				}
			}
		});
		this.computeProposalsJob = currentComputeEntriesJob;
		currentComputeEntriesJob.schedule();
		computingFeedbackJob.schedule(200); // delay a bit so if proposals compute fast enough, we don't show feedback
	}

	/**
	 * Sets whether to display all matches to the current filter or limit the results. Will refresh the table contents
	 * and update the info label.
	 *
	 * @param showAll
	 *            whether to display all matches
	 */
	public void setShowAllMatches(final boolean showAll) {
		if (showAllMatches != showAll) {
			showAllMatches = showAll;
			updateProposals(filterText.getText().toLowerCase());
		}
	}

	/**
	 * Returns the trigger sequence that can be used to open the quick access dialog as well as toggle the show all
	 * results feature. Can return <code>null</code> if no trigger sequence is known.
	 *
	 * @return the trigger sequence used to open the quick access or <code>null</code>
	 */
	public TriggerSequence getTriggerSequence() {
		if (keySequence == null) {
			IBindingService bindingService = Adapters.adapt(PlatformUI.getWorkbench(), IBindingService.class);
			keySequence = bindingService.getBestActiveBindingFor(QUICK_ACCESS_COMMAND_ID);
		}
		return keySequence;
	}

	/**
	 * Return whether the shell is currently set to display all matches or limit the results.
	 *
	 * @return whether all matches will be displayed
	 */
	public boolean getShowAllMatches() { return showAllMatches; }

	private void refreshTable(final List<GamlAccessEntry>[] entries, final String filter) {
		if (table.isDisposed()) return;
		if (table.getItemCount() > entries.length && table.getItemCount() - entries.length > 20) { table.removeAll(); }
		TableItem[] items = table.getItems();
		int selectionIndex = -1;
		int index = 0;
		for (List<GamlAccessEntry> entriesForCurrentCategory : entries) {
			if (entriesForCurrentCategory != null) {
				boolean firstEntry = true;
				for (Iterator<GamlAccessEntry> it = entriesForCurrentCategory.iterator(); it.hasNext();) {
					GamlAccessEntry entry = it.next();
					entry.firstInCategory = firstEntry;
					firstEntry = false;
					if (!it.hasNext()) { entry.lastInCategory = true; }
					TableItem item;
					if (index < items.length) {
						item = items[index];
						table.clear(index);
					} else {
						item = new TableItem(table, SWT.NONE);
					}
					item.setData(entry);
					item.setText(0, entry.provider.getSearchCategory());
					item.setText(1, entry.element.getTitle());
					index++;
				}
			}
		}
		if (index < items.length) { table.remove(index, items.length - 1); }
		if (selectionIndex == -1) { selectionIndex = 0; }

		if (table.getItemCount() > 0) {
			table.setSelection(selectionIndex);
			hideHintText();
		} else if (filter.isEmpty()) {
			showHintText("Start typing text to find GAML elements", grayColor);
		} else {
			showHintText("No matching GAML elements", grayColor);
		}
	}

	/**
	 * Returns a list per provider containing matching {@link GamlAccessEntry} that should be displayed in the table
	 * given a text filter and a perfect match entry that should be given priority. The number of items returned is
	 * affected by {@link #getShowAllMatches()} and the size of the table's composite.
	 *
	 * @param filter
	 *            the string text filter to apply, possibly empty
	 * @param perfectMatch
	 *            a quick access element that should be given priority or <code>null</code>
	 *
	 * @return the array of lists (one per provider) contains the quick access entries that should be added to the
	 *         table, possibly empty
	 */
	private List<GamlAccessEntry>[] computeMatchingEntries(String filter, final IGamlDescription perfectMatch,
			final int maxNumberOfItemsInTable, IProgressMonitor aMonitor) {
		if (filter == null || filter.isBlank()) return EMPTY_ENTRIES;
		if (aMonitor == null) { aMonitor = new NullProgressMonitor(); }
		// check for a category filter, like "Views: "
		Matcher categoryMatcher = getCategoryPattern().matcher(filter);
		String category = null;
		if (categoryMatcher.matches()) {
			category = categoryMatcher.group(1);
			filter = category + " " + categoryMatcher.group(2); //$NON-NLS-1$
		}
		final String finalFilter = filter;

		// collect matching elements
		LinkedHashMap<GamlIdiomsProvider, List<IGamlDescription>> elementsForProviders =
				new LinkedHashMap<>(GamlIdiomsProvider.PROVIDERS.size());
		for (GamlIdiomsProvider provider : GamlIdiomsProvider.PROVIDERS) {
			if (aMonitor.isCanceled()) { break; }
			// boolean isPreviousPickProvider = provider instanceof PreviousPicksProvider;
			// skip if filter contains a category, and current provider isn't this category
			if (category != null && !category.equalsIgnoreCase(provider.getSearchCategory())) { continue; }
			if (!filter.isEmpty() || showAllMatches) {
				AtomicReference<List<IGamlDescription>> sortedElementRef = new AtomicReference<>();
				sortedElementRef.set(Arrays.asList(provider.getSortedElements()));
				List<IGamlDescription> sortedElements = sortedElementRef.get();
				if (sortedElements == null) { sortedElements = Collections.emptyList(); }
				for (IGamlDescription element : sortedElements) { elementsToProviders.put(element, provider); }
				if (!filter.isEmpty() && !sortedElements.isEmpty()) {
					sortedElements = putPrefixMatchFirst(sortedElements, filter);
				}
				elementsForProviders.put(provider, new ArrayList<>(sortedElements));
			}
		}

		for (Entry<GamlIdiomsProvider, List<IGamlDescription>> entry : elementsForProviders.entrySet()) {
			List<IGamlDescription> filteredElements = new ArrayList<>(entry.getValue());
			entry.setValue(filteredElements);
		}

		LinkedHashMap<GamlIdiomsProvider, List<GamlAccessEntry>> entriesPerProvider =
				new LinkedHashMap<>(elementsForProviders.size());
		if (showAllMatches) {
			// Map elements to entries
			for (Entry<GamlIdiomsProvider, List<IGamlDescription>> elementsPerProvider : elementsForProviders
					.entrySet()) {
				GamlIdiomsProvider provider = elementsPerProvider.getKey();
				List<GamlAccessEntry> entries = elementsPerProvider.getValue().stream() //
						.map(GamlAccessMatcher::new) //
						.map(matcher -> matcher.match(finalFilter, provider)) //
						.filter(Objects::nonNull) //
						.collect(Collectors.toList());
				if (!entries.isEmpty()) { entriesPerProvider.put(provider, entries); }
			}
		} else {
			int numberOfSlotsLeft = perfectMatch != null ? maxNumberOfItemsInTable - 1 : maxNumberOfItemsInTable;
			while (!elementsForProviders.isEmpty() && numberOfSlotsLeft > 0) {
				int nbEntriesPerProvider = numberOfSlotsLeft / elementsForProviders.size();
				if (nbEntriesPerProvider > 0) {
					for (Entry<GamlIdiomsProvider, List<IGamlDescription>> elementsPerProvider : elementsForProviders
							.entrySet()) {
						GamlIdiomsProvider provider = elementsPerProvider.getKey();
						List<IGamlDescription> elements = elementsPerProvider.getValue();
						int toPickEntries = nbEntriesPerProvider;
						while (toPickEntries > 0 && !elements.isEmpty()) {
							IGamlDescription element = elements.remove(0);
							GamlAccessEntry entry = new GamlAccessMatcher(element).match(filter, provider);
							if (entry != null) {
								numberOfSlotsLeft--;
								toPickEntries--;
								if (!entriesPerProvider.containsKey(provider)) {
									entriesPerProvider.put(provider, new LinkedList<>());
								}
								entriesPerProvider.get(provider).add(entry);
							}
						}
					}
				} else {
					for (Entry<GamlIdiomsProvider, List<IGamlDescription>> elementsForProvider : elementsForProviders
							.entrySet()) {
						if (numberOfSlotsLeft > 0) {
							GamlIdiomsProvider provider = elementsForProvider.getKey();
							List<IGamlDescription> elements = elementsForProvider.getValue();
							boolean entryPicked = false;
							while (!entryPicked && !elements.isEmpty()) {
								IGamlDescription element = elements.remove(0);
								GamlAccessEntry entry = new GamlAccessMatcher(element).match(filter, provider);
								if (entry != null) {
									numberOfSlotsLeft--;
									entryPicked = true;
									if (!entriesPerProvider.containsKey(provider)) {
										entriesPerProvider.put(provider, new LinkedList<>());
									}
									entriesPerProvider.get(provider).add(entry);
								}
							}
						}
					}
				}
				Set<GamlIdiomsProvider> exhaustedProviders = new HashSet<>();
				elementsForProviders.forEach(
						(provider, elements) -> { if (elements.isEmpty()) { exhaustedProviders.add(provider); } });
				exhaustedProviders.forEach(elementsForProviders::remove);
			}
		}
		//
		List<List<GamlAccessEntry>> res = new ArrayList<>();
		res.addAll(entriesPerProvider.values());
		return (List<GamlAccessEntry>[]) res.toArray(new List<?>[res.size()]);
	}

	/*
	 * Consider whether we could directly check the "matchQuality" here, but it seems to be a more expensive operation
	 */
	private static List<IGamlDescription> putPrefixMatchFirst(final List<IGamlDescription> elements,
			final String prefix) {
		List<IGamlDescription> res = new ArrayList<>(elements);
		List<Integer> matchingIndexes = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i).getTitle().toLowerCase().startsWith(prefix.toLowerCase())) { matchingIndexes.add(i); }
		}
		int currentMatchIndex = 0;
		int currentNonMatchIndex = matchingIndexes.size();
		for (int i = 0; i < res.size(); i++) {
			boolean isMatch = !matchingIndexes.isEmpty() && matchingIndexes.iterator().next().intValue() == i;
			if (isMatch) {
				matchingIndexes.remove(0);
				res.set(currentMatchIndex, elements.get(i));
				currentMatchIndex++;
			} else {
				res.set(currentNonMatchIndex, elements.get(i));
				currentNonMatchIndex++;
			}
		}
		return res;
	}

	Pattern categoryPattern;

	/**
	 * Return a pattern like {@code "^(:?Views|Perspective):\\s?(.*)"}, with all the provider names separated by
	 * semicolon.
	 *
	 * @return Returns the patternProvider.
	 */
	protected Pattern getCategoryPattern() {
		if (categoryPattern == null) {
			// build regex like "^(:?Views|Perspective):\\s?(.*)"
			StringBuilder sb = new StringBuilder();
			sb.append("^(:?"); //$NON-NLS-1$
			for (int i = 0; i < GamlIdiomsProvider.PROVIDERS.size(); i++) {
				if (i != 0) {
					sb.append("|"); //$NON-NLS-1$
				}
				sb.append(GamlIdiomsProvider.PROVIDERS.get(i).getSearchCategory());
			}
			sb.append("):\\s?(.*)"); //$NON-NLS-1$
			String regex = sb.toString();
			categoryPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		}
		return categoryPattern;
	}

	private void doDispose() {
		if (textLayout != null && !textLayout.isDisposed()) { textLayout.dispose(); }
		if (resourceManager != null) {
			// Disposing the resource manager will dispose the color
			resourceManager.dispose();
			resourceManager = null;
		}
	}

	protected String getId() {
		return "org.eclipse.ui.internal.QuickAccess"; //$NON-NLS-1$
	}

	protected void handleElementSelected(final String text, final IGamlDescription element) {
		if (element == null) return;
		final String search = "https://gama-platform.org/search?q=" + element.getName();
		WebHelper.openPage(search);
	}

	private void handleSelection() {
		IGamlDescription selectedElement = null;
		String text = filterText.getText().toLowerCase();
		if (table.getSelectionCount() == 1) {
			GamlAccessEntry entry = (GamlAccessEntry) table.getSelection()[0].getData();
			selectedElement = entry == null ? null : entry.element;
		}
		if (selectedElement != null) {
			doClose();
			handleElementSelected(text, selectedElement);
		}
	}

	/**
	 * Informs the owner of the parent composite that the quick access dialog should be closed
	 */
	protected void doClose() {
		this.close();
	}

	/**
	 * Allows the dialog contents to interact correctly with the text box used to open it
	 *
	 * @param filterText
	 *            text box to hook up
	 */
	public void hookFilterText(final Text filterText) {
		this.filterText = filterText;
		filterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				switch (e.keyCode) {
					case SWT.CR:
					case SWT.KEYPAD_CR:
						handleSelection();
						break;
					case SWT.ARROW_DOWN:
						int index = table.getSelectionIndex();
						if (index != -1 && table.getItemCount() > index + 1) { table.setSelection(index + 1); }
						break;
					case SWT.ARROW_UP:
						index = table.getSelectionIndex();
						if (index != -1 && index >= 1) { table.setSelection(index - 1); }
						break;
					case SWT.ESC:
						doClose();
						break;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				// do nothing
			}
		});
		filterText.addModifyListener(e -> {
			String text = ((Text) e.widget).getText();
			updateProposals(text);
		});
	}

	Label hintText;
	private boolean displayHintText;

	/** Create HintText as child of the given parent composite */
	Label createHintText(final Composite composite, final int defaultOrientation) {
		hintText = new Label(composite, SWT.FILL);
		hintText.setOrientation(defaultOrientation);
		displayHintText = true;
		return hintText;
	}

	/** Hide the hint text */
	private void hideHintText() {
		if (displayHintText) { setHintTextToDisplay(false); }
	}

	/** Show the hint text with the given color */
	private void showHintText(final String text, final Color color) {
		if (hintText == null || hintText.isDisposed()) // toolbar hidden
			return;
		hintText.setText(text);
		if (color != null) { hintText.setForeground(color); }
		if (!displayHintText) { setHintTextToDisplay(true); }
	}

	/**
	 * Sets hint text to be displayed and requests the layout
	 */
	private void setHintTextToDisplay(final boolean toDisplay) {
		GridData data = (GridData) hintText.getLayoutData();
		data.exclude = !toDisplay;
		hintText.setVisible(toDisplay);
		hintText.requestLayout();
		this.displayHintText = toDisplay;
	}

	/**
	 * Creates the table providing the contents for the quick access dialog
	 *
	 * @param composite
	 *            parent composite with {@link GridLayout}
	 * @param defaultOrientation
	 *            the window orientation to use for the table {@link SWT#RIGHT_TO_LEFT} or {@link SWT#LEFT_TO_RIGHT}
	 * @return the created table
	 */
	public Table createTable(final Composite composite) {
		composite.addDisposeListener(e -> doDispose());
		Composite tableComposite = new Composite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);

		table = new Table(tableComposite, SWT.SINGLE | SWT.FULL_SELECTION);

		textLayout = new TextLayout(table.getDisplay());
		Font boldFont = resourceManager.create(FontDescriptor.createFrom(table.getFont()).setStyle(SWT.BOLD));
		// textLayout.setFont(table.getFont());
		textLayout.setFont(boldFont);
		textLayout.setText("Available categories");
		int maxProviderWidth = textLayout.getBounds().width;

		for (GamlIdiomsProvider provider : GamlIdiomsProvider.PROVIDERS) {
			textLayout.setText(provider.getSearchCategory());
			int width = textLayout.getBounds().width;
			if (width > maxProviderWidth) { maxProviderWidth = width; }
		}
		tableColumnLayout.setColumnData(new TableColumn(table, SWT.NONE), new ColumnWeightData(0, maxProviderWidth));
		tableColumnLayout.setColumnData(new TableColumn(table, SWT.NONE), new ColumnWeightData(100, 100));
		table.getShell().addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				if (!showAllMatches && !resized) {
					resized = true;
					e.display.timerExec(100, () -> {
						if (table != null && !table.isDisposed() && filterText != null && !filterText.isDisposed()) {
							updateProposals(filterText.getText().toLowerCase());
						}
						resized = false;
					});
				}
			}
		});

		table.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP && table.getSelectionIndex() == 0) {
					filterText.setFocus();
				} else if (e.character == SWT.ESC) { doClose(); }
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				// do nothing
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(final MouseEvent e) {

				if (table.getSelectionCount() < 1 || e.button != 1) return;

				if (table.equals(e.getSource())) {
					Object o = table.getItem(new Point(e.x, e.y));
					TableItem selection = table.getSelection()[0];
					if (selection.equals(o)) { handleSelection(); }
				}
			}
		});

		table.addMouseMoveListener(new MouseMoveListener() {
			TableItem lastItem = null;

			@Override
			public void mouseMove(final MouseEvent e) {
				if (table.equals(e.getSource())) {
					TableItem tableItem = table.getItem(new Point(e.x, e.y));
					if (tableItem != null) {
						if (!tableItem.equals(lastItem)) {
							lastItem = tableItem;
							table.setSelection(new TableItem[] { lastItem });
							final GamlAccessEntry entry = (GamlAccessEntry) lastItem.getData();
							if (entry != null) {

								final IGamlDescription element = entry.element;
								browser.setInput(entry.provider.document(element));
								showBrowser();
							}
						}
					} else {
						lastItem = null;
					}
				}
				if (lastItem == null) {
					hideBrowser();
				} else {
					showBrowser();
				}
			}
		});

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				handleSelection();
			}
		});

		filterText.addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseEnter(final MouseEvent e) {
				hideBrowser();
			}
		});

		Listener listener = event -> {
			GamlAccessEntry entry = (GamlAccessEntry) event.item.getData();
			if (entry != null) {
				switch (event.type) {
					case SWT.MeasureItem:
						entry.measure(event, textLayout);
						break;
					case SWT.PaintItem:
						entry.paint(event, textLayout);
						break;
					case SWT.EraseItem:
						entry.erase(event);
						break;
				}
			}
		};
		table.addListener(SWT.MeasureItem, listener);
		table.addListener(SWT.EraseItem, listener);
		table.addListener(SWT.PaintItem, listener);

		return table;
	}

	public Text getFilterText() { return filterText; }

	public Table getTable() { return table; }

	@Override
	protected Control createTitleControl(final Composite parent) {
		parent.getShell().setText(QuickAccessMessages.QuickAccessContents_QuickAccess);
		filterText = new Text(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(filterText);
		hookFilterText(filterText);
		return filterText;
	}

	private void createBrowser(final Composite composite) {
		browser = new GamlBrowserPane();
		browserData = GridDataFactory.fillDefaults().grab(true, true).create();
		browser.createContent(composite);
		browser.getControl().setLayoutData(browserData);
		hideBrowser();
	}

	private void showBrowser() {
		browserData.exclude = false;
		browser.getControl().setVisible(true);
		browser.getControl().getParent().requestLayout();

	}

	private void hideBrowser() {
		browserData.exclude = true;
		browser.getControl().setVisible(false);
		browser.getControl().getParent().requestLayout();
	}

	@Override
	protected Control getFocusControl() { return filterText; }

	@Override
	protected Point getDefaultSize() {
		GC gc = new GC(getContents());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		int x = Dialog.convertHorizontalDLUsToPixels(fontMetrics, 300);
		if (x < 350) { x = 350; }
		int y = Dialog.convertVerticalDLUsToPixels(fontMetrics, 270);
		if (y < 420) { y = 420; }
		return new Point(x, y);
	}

	@Override
	protected Point getDefaultLocation(final Point size) {
		Rectangle parentBounds = getParentShell().getBounds();
		int x = parentBounds.x + parentBounds.width / 2 - size.x / 2;
		int y = parentBounds.y + parentBounds.height / 2 - size.y / 2;
		return new Point(x, y);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		final IDialogSettings workbenchDialogSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(GamlAccessContents2.class)).getDialogSettings();
		IDialogSettings result = workbenchDialogSettings.getSection(getId());
		if (result == null) { result = workbenchDialogSettings.addNewSection(getId()); }
		return result;
	}

}