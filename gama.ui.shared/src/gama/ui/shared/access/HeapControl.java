package gama.ui.shared.access;

import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

import gama.ui.shared.controls.StatusControlContribution;
import gama.ui.shared.utils.WorkbenchHelper;
import gama.ui.shared.views.toolbar.GamaToolbarSimple;

public class HeapControl {

	ToolItem item;

	Control displayOn(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).extendedMargins(0, 5, 5, 5).numColumns(2)
				.equalWidth(false).applyTo(composite);
		GamaToolbarSimple bar = new GamaToolbarSimple(composite, SWT.NONE);
		bar.button("spacer", "", "", null);
		item = bar.button("generic/garbage.collect", "", "", e -> { System.gc(); });
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).indent(30, 0).applyTo(bar);
		bar.addListener(SWT.MouseEnter, e -> updateToolTip());

		new StatusControlContribution().fill(bar, 0);
		return composite;
	}

	protected void updateToolTip() {
		Runtime runtime = Runtime.getRuntime();
		long totalMem = convertToMeg(runtime.totalMemory());
		item.setToolTipText(
				"Memory used: " + (totalMem - convertToMeg(runtime.freeMemory())) + "M on " + totalMem + "M");
	}

	private long convertToMeg(final long numBytes) {
		return (numBytes + 512 * 1024) / (1024 * 1024);
	}

	public static void install() {
		WorkbenchHelper.runInUI("Install GAMA Status and Heap Controls", 0, m -> {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window instanceof WorkbenchWindow) {
				final MTrimBar topTrim = ((WorkbenchWindow) window).getTopTrim();
				for (final MTrimElement element : topTrim.getChildren()) {
					if ("SearchField".equals(element.getElementId())) {
						final Composite parent = ((Control) element.getWidget()).getParent();
						final Control old = (Control) element.getWidget();
						WorkbenchHelper.runInUI("Disposing old search control", 500, m2 -> old.dispose());
						element.setWidget(new HeapControl().displayOn(parent));
						parent.layout(true, true);
						parent.update();
						break;
					}
				}
			}
		});
	}

}
