package org.geogebra.common.main;

public enum Feature {
	ALL_LANGUAGES,

	LOCALSTORAGE_FILES,

	TUBE_BETA,

	EXERCISES,

	TOOL_EDITOR,

	POLYGON_TRIANGULATION,

	DATA_COLLECTION,

	IMPLICIT_SURFACES,

	CONTOUR_PLOT_COMMAND,

	LOG_AXES,

	HIT_PARAMETRIC_SURFACE,

	PARAMETRIC_SURFACE_IS_REGION,

	EXAM_TABLET,

	ACRA,

	ANALYTICS,

	SAVE_SETTINGS_TO_FILE,

	HANDWRITING,

	AV_DEFINITION_AND_VALUE,

	CONVEX_HULL_3D,

	/** GGB-334, TRAC-3401 */
	ADJUST_WIDGETS,

	/** GGB-944 */
	EXPORT_ANIMATED_PDF,

	/** GGB-776 */
	ABSOLUTE_TEXTS,

	/** MOB-788 */
	MOBILE_USE_FBO_FOR_3D_IMAGE_EXPORT,

	/** GGB-1263 */
	AUTOSCROLLING_SPREADSHEET,

	/** GGB-1252 */
	KEYBOARD_BEHAVIOUR,

	/** GGB-2336 */
	KEYBOARD_ATTACHED_TO_TABLET,

	/** MOW */
	WHITEBOARD_APP,

	/**
	 * GGB-1398 + GGB-1529
	 */
	SHOW_ONE_KEYBOARD_BUTTON_IN_FRAME,

	/** MOW-97 */
	ERASER, ROUNDED_POLYGON,

	/** MOW-175 */
	MOW_CONTEXT_MENU,

	/** MOV-169 */
	DYNAMIC_STYLEBAR,

	/** MOW-29 */
	MOW_TOOLBAR,

	MOW_PEN_IS_LOCUS,

	MOW_PEN_EVENTS,

	/** MOW-105 */
	MOW_PEN_SMOOTHING,

	/** GGB-1617 */
	AUTOMATIC_DERIVATIVES,

	/** SolveQuartic in CAS GGB-1635 */
	SOLVE_QUARTIC,

	/** MOW-166 */
	MOW_AXES_STYLE_SUBMENU,

	/** MOW-55 */
	MOW_BOUNDING_BOXES,

	/** MOW-320 */
	MOW_PIN_IMAGE,
	
	/** MOW-239 */
	MOW_IMPROVE_CONTEXT_MENU,

	/** MOW-251, MOW-197 */
	MOW_CLEAR_VIEW_STYLEBAR,

	/** MOW-197 */
	MOW_COLORPOPUP_IMPROVEMENTS,

	/** MOW-88 */
	MOW_DIRECT_FORMULA_CONVERSION,

	/** MOW-368 */
	MOW_IMAGE_DIALOG_UNBUNDLED,

	EXPORT_SCAD_IN_MENU,

	EXPORT_COLLADA_IN_MENU,

	EXPORT_OBJ_IN_MENU,

	/** GGB-1708 */
	INPUT_BAR_ADD_SLIDER,

	/** GGB-1916 */
	DEFAULT_OBJECT_STYLES,
	
	/** GGB-2008 */
	OBJECT_DEFAULTS_AND_COLOR,

	SHOW_STEPS,

	/** GGB-1907 */
	DYNAMIC_STYLEBAR_SELECTION_TOOL,

	/** GGB-1910 */
	LABEL_SETTING_ON_STYLEBAR,

	CENTER_STANDARD_VIEW,

	SURFACE_2D,

	/** GGB-1985*/
	FLOATING_SETTINGS,

	/** GGB-2005 */
	TOOLTIP_DESIGN,

	/** GGB-1986 */
	DIALOG_DESIGN,

	INITIAL_PORTRAIT,

	/** MOW-261 */
	MOW_COLOR_FILLING_LINE,

	/** MOW-269 */
	MOW_MULTI_PAGE,

	/** MOB-1293 */
	SELECT_TOOL_NEW_BEHAVIOUR,

	/** GGB-2183 change sin(15) -> sin(15deg) */
	AUTO_ADD_DEGREE,

	/** GGB-2222 change asin(0.5) -> asind(0.5) */
	CHANGE_INVERSE_TRIG_TO_DEGREES,

	/** MOB-1310 */
	SHOW_HIDE_LABEL_OBJECT_DELETE_MULTIPLE,

	/** MOB-1319 */
	MOB_NOTIFICATION_BAR_TRIGGERS_EXAM_ALERT_IOS_11,

	/** GGB-2347 */
	READ_DROPDOWNS,

	/** GGB-2215 */
	ARIA_CONTEXT_MENU,

	/** MOW-390 GGB */
	WHOLE_PAGE_DRAG,

	/** GGB-650 */
	GGB_WEB_ASSEMBLY,

	/** GGB-2394 */
	SPLITTER_LOADING,

	/** MOW-285 */
	MOW_BOUNDING_BOX_FOR_PEN_TOOL,

	/** GGB-2258 */
	VOICEOVER_CURSOR,

	/** GGB-2346 */
	CURRENCY_UNIT,
	
	/** GBB-2374 */
	MAT_DESIGN_HEADER,

	/** MOW-360, MOW-381, MOW-382 */
	MOW_CROP_IMAGE,
	
	/** MOW-379, MOW-380 */
	MOW_IMAGE_BOUNDING_BOX,

	/** MOW-336 */
	MOW_DRAG_AND_DROP_PAGES,

	/** MOW-336 */
	MOW_DRAG_AND_DROP_ANIMATION,

	/** MOW-345 */
	MOW_MOVING_CANVAS,

	/** MOW-349 */
	MOW_AUDIO_TOOL,

	/** MOW-299 */
	MOW_VIDEO_TOOL,

	/** MOW-278 */
	MOW_HIGHLIGHTER_TOOL,

	/** MOW-459 */
	MOW_DOUBLE_CANVAS,

	/** MOW-348 */
	MOW_PDF_TOOL,

	MOW_GEOGEBRA_TOOL,

	/** MOW-479 */
	MOW_OPEN_FILE_VIEW,

	COMMAND_HOLES, WEB_CLASSIC_FLOATING_MENU,

	/** MOB-1537 */
	MOB_PREVIEW_WHEN_EDITING,

	/** GGB-2366 */
	TIKZ_AXES,

	/** AND-1071 and IGR-819 */
	MOB_EXAM_MODE_SCREENSHOT_SAVED_NOTIFICATION,

	/** AND-887 and IGR-732 */
	MOB_PROPERTY_SORT_BY,

	/** IGR-898 */
	MOB_REACT_SEARCH_VIEW,

	/** GGB-2375 */
	INPUT_BOX_LINE_UP_BETTER,

	/** MOB-1517 */
	MOB_PACK_PLANES,

	/** MOB-1518 */
	MOB_PACK_QUADRICS,

	/** MOB-1519 */
	MOB_PACK_SURFACES_GRAPHS,

	/** GGB-2416 */
	SHARE_DIALOG_MAT_DESIGN,

	/** MOB-1520 */
	MOB_PACK_ALL_SURFACES,

	/** MOB-1521 */
	MOB_PACK_QUADRIC_PART,

	/** MOB-1522 */
	MOB_PACK_LISTS,

	/** MOB-1722 */
	MOB_EXPORT_STL,

	EMBED_EXTENSION,

	/** beta */
	PRERELEASE,

	/** alpha */
	CANARY
}

