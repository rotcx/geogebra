package org.geogebra.web.full.gui.view.algebra.contextmenu;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.web.full.gui.view.algebra.AlgebraViewW;
import org.geogebra.web.full.gui.view.algebra.MenuAction;
import org.geogebra.web.full.gui.view.algebra.RadioTreeItem;
import org.geogebra.web.full.main.AppWFull;

/**
 * Duplicates geo in AV and puts focus to the new input
 */
public class DuplicateAction extends MenuAction<GeoElement> {
	private AlgebraViewW algebraView;

	/**
	 * @param av
	 *            algebra view
	 */
	public DuplicateAction(AlgebraViewW av) {
		super("Duplicate");
		this.algebraView = av;
	}

	@Override
	public void execute(GeoElement geo, AppWFull app) {
		RadioTreeItem input = algebraView.getInputTreeItem();
		String dup = "";
		if ("".equals(geo.getDefinition(StringTemplate.defaultTemplate))) {
			dup = geo.getValueForInputBar();
		} else {
			dup = geo.getDefinitionNoLabel(StringTemplate.editorTemplate);
		}
		RadioTreeItem currentNode = algebraView.getNode(geo);
		if (currentNode != null) {
			currentNode.selectItem(false);
		}
		input.setText(dup);
		input.setFocus(true, true);
	}

	@Override
	public boolean isAvailable(GeoElement geo) {
		return geo.isAlgebraDuplicateable();
	}
}

