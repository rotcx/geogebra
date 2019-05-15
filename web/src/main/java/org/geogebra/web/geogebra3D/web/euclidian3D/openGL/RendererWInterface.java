package org.geogebra.web.geogebra3D.web.euclidian3D.openGL;

/**
 * Renderer interface
 */
public interface RendererWInterface {

	/**
	 * @param pixelRatio
	 *            CSS pixel ratio
	 */
	void setPixelRatio(double pixelRatio);

	/**
	 * @param useBuffer
	 *            whether to use buffer
	 */
	void setBuffering(boolean useBuffer);

}
