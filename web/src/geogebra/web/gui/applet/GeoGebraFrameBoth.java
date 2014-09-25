package geogebra.web.gui.applet;

import geogebra.html5.WebStatic;
import geogebra.html5.gui.GeoGebraFrame;
import geogebra.html5.gui.laf.GLookAndFeelI;
import geogebra.html5.main.AppW;
import geogebra.html5.main.AppWsimple;
import geogebra.html5.util.ArticleElement;
import geogebra.html5.util.debug.GeoGebraLogger;
import geogebra.web.gui.laf.GLookAndFeel;
import geogebra.web.gui.layout.DockGlassPaneW;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class GeoGebraFrameBoth extends GeoGebraFrame {

	private AppletFactory factory;
	private DockGlassPaneW glass;

	public GeoGebraFrameBoth(AppletFactory factory, GLookAndFeel laf) {
		super(laf);
		this.factory = factory;
	}

	protected AppW createApplication(ArticleElement ae, GeoGebraFrame gf, GLookAndFeelI laf) {
		AppW app = factory.getApplet(ae, gf, laf);
		WebStatic.lastApp = app;
		this.glass = new DockGlassPaneW();
		this.add(glass);
		return app;
	}

	protected AppW createApplicationSimple(ArticleElement ae, GeoGebraFrame gf) {
		AppW app = new AppWsimple(ae, gf);
		WebStatic.lastApp = app;
		return app;
	}

	/**
	 * Main entry points called by geogebra.web.Web.startGeoGebra()
	 * @param geoGebraMobileTags
	 *          list of &lt;article&gt; elements of the web page
	 */
	public static void main(ArrayList<ArticleElement> geoGebraMobileTags, AppletFactory factory, GLookAndFeel laf) {

		for (final ArticleElement articleElement : geoGebraMobileTags) {
			final GeoGebraFrame inst = new GeoGebraFrameBoth(factory, laf);
			inst.ae = articleElement;
			GeoGebraLogger.startLogger(inst.ae);
			inst.createSplash(articleElement);
			if(WebStatic.panelForApplets == null){
				RootPanel.get(articleElement.getId()).add(inst);
			}else{
				WebStatic.panelForApplets.add(inst);
			}
		}
	}
	
	/**
	 * @param el html element to render into
	 */
	public static void renderArticleElement(Element el, AppletFactory factory, GLookAndFeel laf, JavaScriptObject clb) {
		GeoGebraFrame.renderArticleElementWithFrame(el, new GeoGebraFrameBoth(factory, laf), clb);
	}
	
	public Object getGlassPane(){
		return this.glass;
	}
	
	public void attachGlass(){
		if(this.glass!=null){
			this.add(glass);
		}
	}

	@Override
    public void showBrowser(HeaderPanel bg) {
	    // TODO Auto-generated method stub
	    
    }
}
