package org.geogebra.web.full.gui.view.probcalculator;

import org.geogebra.common.main.App;
import org.geogebra.web.html5.main.AppW;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.TabLayoutPanel;

public class TabbedProbCalcView extends ProbabilityCalculatorViewW {
	private MyTabLayoutPanel tabbedPane;
	private static final int CONTROL_PANEL_HEIGHT = 150;
	private static final int TABLE_PADDING_AND_SCROLLBAR = 32;

	/**
	 * @param app creates new probabilitycalculatorView
	 */
	public TabbedProbCalcView(AppW app) {
		super(app);
		tabbedPane = new MyTabLayoutPanel(30, Style.Unit.PX);
		tabbedPane.add(probCalcPanel, loc.getMenu("Distribution"));
		tabbedPane.add(statCalculator.getWrappedPanel(),
				loc.getMenu("Statistics"));

		tabbedPane.onResize();
		tabbedPane.selectTab(getApp().getSettings().getProbCalcSettings()
				.getCollection().isActive() ? 1 : 0);
		init();
	}

	private class MyTabLayoutPanel extends TabLayoutPanel implements ClickHandler {

		public MyTabLayoutPanel(int splitterSize, Style.Unit px) {
			super(splitterSize, px);
			this.addDomHandler(this, ClickEvent.getType());
		}

		@Override
		public final void onResize() {
			tabResized();
		}

		public void deferredOnResize() {
			Scheduler.get().scheduleDeferred(this::onResize);
		}

		@Override
		public void onClick(ClickEvent event) {
			getApp().setActiveView(App.VIEW_PROBABILITY_CALCULATOR);
		}
	}

	/**
	 * Tab resize callback
	 */
	public void tabResized() {
		int tableWidth = isDiscreteProbability() ? ((ProbabilityTableW) getTable()).getStatTable()
				.getTable().getOffsetWidth() + TABLE_PADDING_AND_SCROLLBAR : 0;
		int width = mainSplitPane.getOffsetWidth()
				- tableWidth
				- 5;
		int height = probCalcPanel.getOffsetHeight() - 20;
		if (width > 0) {
			resizePlotPanel(width, height);
		}

		if (height > 0 && isDiscreteProbability()) {
			((ProbabilityTableW) getTable()).getWrappedPanel()
					.setPixelSize(tableWidth, height);
		}
	}

	/**
	 * @return wheter distribution tab is open
	 */
	@Override
	public boolean isDistributionTabOpen() {
		return tabbedPane.getSelectedIndex() == 0;
	}

	@Override
	public void setLabels() {
		super.setLabels();
		tabbedPane.setTabText(0, loc.getMenu("Distribution"));
		tabbedPane.setTabText(1, loc.getMenu("Statistics"));
	}

	@Override
	public TabLayoutPanel getWrapperPanel() {
		return tabbedPane;
	}

	protected void tabbedPaneOnResize() {
		tabbedPane.onResize();
	}
}
