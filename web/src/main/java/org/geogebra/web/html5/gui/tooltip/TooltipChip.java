package org.geogebra.web.html5.gui.tooltip;

import org.geogebra.common.awt.GRectangle2D;
import org.geogebra.common.euclidian.DrawableND;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.web.html5.main.AppW;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;

public class TooltipChip {
	private Label tooltipMsg;

	public final static TooltipChip INSTANCE = new TooltipChip();
	private Timer hideTimer;

	/**
	 * @param tooltip tooltip content
	 * @param label label of a geo to be used as position anchor
	 * @param color CSS color code (e.g. "#00FF00")
	 * @param app app
	 */
	public void showMessage(String tooltip, String label, String color, AppW app) {
		GeoElement geo = app.getKernel().lookupLabel(label);
		DrawableND drawable = app.getActiveEuclidianView().getDrawableFor(geo);
		if (drawable != null) {
			GRectangle2D bounds = drawable.getBoundsForStylebarPosition();
			double x = bounds.getMaxX();
			double y = bounds.getMinY();
			if (tooltipMsg == null) {
				tooltipMsg = new Label(tooltip);
				tooltipMsg.addStyleName("tooltipChip");
			} else {
				tooltipMsg.setText(tooltip);
			}
			Style style = tooltipMsg.getElement().getStyle();
			style.setVisibility(Style.Visibility.VISIBLE);
			double left = app.getActiveEuclidianView().getAbsoluteLeft() - app.getAbsLeft();
			double top = app.getActiveEuclidianView().getAbsoluteTop() - app.getAbsTop();
			style.setBackgroundColor(color);
			style.setLeft(left + x, Style.Unit.PX);
			style.setTop(top + y, Style.Unit.PX);

			app.getAppletFrame().add(tooltipMsg);
			if	(hideTimer == null) {
				hideTimer = new Timer() {

					@Override
					public void run() {
						style.setVisibility(Style.Visibility.HIDDEN);
					}
				};
			}
			hideTimer.cancel();
			hideTimer.schedule(4000);
		}
	}
}
