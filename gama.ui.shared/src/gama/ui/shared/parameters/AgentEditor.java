/*******************************************************************************************************
 *
 * AgentEditor.java, in gama.ui.shared.shared, is part of the source code of the GAMA modeling and simulation platform .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.shared.parameters;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import gama.core.common.interfaces.IKeyword;
import gama.core.kernel.experiment.IParameter;
import gama.core.metamodel.agent.IAgent;
import gama.gaml.types.IType;
import gama.ui.shared.interfaces.EditorListener;
import gama.ui.shared.interfaces.IAgentMenuFactory;
import gama.ui.shared.menus.MenuAction;
import gama.ui.shared.resources.GamaIcon;
import gama.ui.shared.resources.IGamaIcons;
import gama.ui.shared.utils.WorkbenchHelper;

/**
 * The Class AgentEditor.
 */
@SuppressWarnings ({ "unchecked", "rawtypes" })
public class AgentEditor extends ExpressionBasedEditor {

	/** The species. */
	String species;

	/**
	 * Instantiates a new agent editor.
	 *
	 * @param scope
	 *            the scope
	 * @param agent
	 *            the agent
	 * @param param
	 *            the param
	 * @param l
	 *            the l
	 */
	AgentEditor(final IAgent agent, final IParameter param, final EditorListener l) {
		super(agent, param, l);
	}

	@Override
	public void applyChange() {
		Shell shell = editorToolbar.getItem(CHANGE).getParent().getShell();
		final Menu old = shell.getMenu();
		shell.setMenu(null);
		if (old != null) { old.dispose(); }
		// FIXME Not adapted to multiple scales !

		final MenuAction action = new MenuAction(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final MenuItem mi = (MenuItem) e.widget;
				final IAgent a = (IAgent) mi.getData("agent");
				if (a != null && !a.dead()) { modifyAndDisplayValue(a); }
			}

		}, GamaIcon.named(IGamaIcons.MENU_AGENT).image(), "Choose");

		final Menu dropMenu = new Menu(shell);
		final IAgent a = (IAgent) (currentValue instanceof IAgent ? currentValue : getAgent());
		if (a != null) {
			final IAgentMenuFactory factory = WorkbenchHelper.getService(IAgentMenuFactory.class);
			if (factory != null) {
				factory.fillPopulationSubMenu(dropMenu, a.getSimulation().getMicroPopulation(getSpecies()), null,
						action);
			}
		}
		final Rectangle rect = editorToolbar.getItem(CHANGE).getBounds();
		final Point pt = editorToolbar.getItem(CHANGE).getParent().toDisplay(new Point(rect.x, rect.y));
		dropMenu.setLocation(pt.x, pt.y + rect.height);
		dropMenu.setVisible(true);

	}

	@Override
	public IType getExpectedType() {
		String s = getSpecies();
		return IKeyword.MODEL.equals(s) ? getScope().getSimulation().getSpecies().getGamlType() : getScope().getType(s);
	}

	/**
	 * Method getToolItems()
	 *
	 * @see gama.ui.shared.parameters.AbstractEditor#getToolItems()
	 */
	@Override
	protected int[] getToolItems() { return new int[] { INSPECT, CHANGE, REVERT }; }

	@Override
	protected void applyInspect() {
		if (currentValue instanceof IAgent a && !a.dead()) { getScope().getGui().setSelectedAgent(a); }

	}

	String getSpecies() {
		if (species == null) { species = param.getType().getSpeciesName(); }
		return species;
	}

}
