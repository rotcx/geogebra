package org.geogebra.common.euclidian.draw;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geogebra.common.awt.GBasicStroke;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GGeneralPath;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.euclidian.EuclidianBoundingBoxHandler;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.kernel.geos.GeoInline;
import org.geogebra.common.kernel.geos.GeoMindMapNode;
import org.geogebra.common.kernel.geos.GeoMindMapNode.NodeAlignment;
import org.geogebra.common.kernel.geos.MoveGeos;
import org.geogebra.common.kernel.matrix.Coords;

public class DrawMindMap extends DrawInlineText {

	private static final int BORDER_RADIUS = 8;
	private static final GBasicStroke connection = AwtFactory.getPrototype().newBasicStroke(2f,
			GBasicStroke.CAP_BUTT, GBasicStroke.JOIN_MITER);

	private static final Comparator<DrawMindMap> verticalComparator
			= Comparator.comparing(mindMap -> mindMap.rectangle.getBottom());

	private static final Comparator<DrawMindMap> horizontalComparator
			= Comparator.comparing(mindMap -> mindMap.rectangle.getRight());

	// default distance from the root node
	private static final int DISTANCE_TO_ROOT = 64;

	// vertical distance between two nodes on the left or the right
	private static final int VERTICAL_DISTANCE_2 = 64;
	// vertical distance between three nodes on the left or the right
	private static final int VERTICAL_DISTANCE_3 = 32;
	// vertical distance between four or more nodes on the left or the right
	private static final int VERTICAL_DISTANCE_4 = 16;

	// horizontal distance between two nodes on the top or bottom
	private static final int HORIZONTAL_DISTANCE_2 = 32;
	// horizontal distance between three or more nodes on th top or bottom
	private static final int HORIZONTAL_DISTANCE_3 = 16;

	private final GeoMindMapNode node;
	private MindMapEdge mindMapEdge;

	private static class MindMapEdge {
		private final double x0;
		private final double x1;
		private final double y0;
		private final double y1;

		public MindMapEdge(DrawMindMap parent, DrawMindMap child, NodeAlignment alignment) {
			x0 = parent.rectangle.getLeft() + alignment.dx0 * parent.rectangle.getWidth();
			y0 = parent.rectangle.getTop() + alignment.dy0 * parent.rectangle.getHeight();
			x1 = child.rectangle.getLeft() + alignment.dx1 * child.rectangle.getWidth();
			y1 = child.rectangle.getTop() + alignment.dy1 * child.rectangle.getHeight();
		}

		private boolean isIntersecting(NodeAlignment alignment) {
			return ((alignment.dx0 - 0.5) * (x0 - x1) > 0)
					|| ((alignment.dy0 - 0.5) * (y0 - y1) > 0);
		}

		private GGeneralPath getConnectionPath(GeoMindMapNode node) {
			GGeneralPath path = AwtFactory.getPrototype().newGeneralPath();
			path.moveTo(x0, y0);
			double w0 = 1.0 / 4;
			if (isIntersecting(node.getAlignment())) {
				w0 = 2;
			}
			double w1 = 1 - w0;
			if (node.getAlignment() == NodeAlignment.TOP
					|| node.getAlignment() == NodeAlignment.BOTTOM) {
				path.curveTo(x0, w0 * y0 + w1 * y1, x1,
						w1 * y0 + w0 * y1, x1, y1);
			} else {
				path.curveTo(w0 * x0 + w1 * x1, y0, w1 * x0 + w0 * x1, y1, x1, y1);
			}
			return path;
		}

		public double getLength() {
			return (x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1);
		}
	}

	public DrawMindMap(EuclidianView view, GeoInline text) {
		super(view, text);
		this.node = (GeoMindMapNode) text;
	}

	@Override
	public void update() {
		super.update();
		GeoMindMapNode parentGeo = node.getParent();
		DrawMindMap parent = (DrawMindMap) view.getDrawableFor(parentGeo);
		if (parent == null) {
			return;
		}
		NodeAlignment alignment = node.getAlignment();
		if (mindMapEdge == null) {
			mindMapEdge = new MindMapEdge(parent, this, alignment);
		}
		if (mindMapEdge.isIntersecting(alignment)
				|| alignment.isOpposite(parent.node.getAlignment())) {
			updateAlignment(parent);
		} else {
			mindMapEdge = new MindMapEdge(parent, this, alignment);
		}
	}

	@Override
	public void draw(GGraphics2D g2) {
		for (GeoMindMapNode childGeo : node.getChildren()) {
			DrawMindMap child = (DrawMindMap) view.getDrawableFor(childGeo);
			if (child == null) {
				continue;
			}
			GGeneralPath path = child.mindMapEdge.getConnectionPath(childGeo);
			g2.setStroke(connection);
			g2.setColor(GColor.MIND_MAP_CONNECTION);
			g2.draw(path);
		}
		draw(g2, BORDER_RADIUS);
	}

	private void updateAlignment(DrawMindMap parent) {
		double length = Double.POSITIVE_INFINITY;
		boolean intersect = true;
		for (NodeAlignment alignment : NodeAlignment.values()) {
			if (alignment.isOpposite(parent.node.getAlignment())) {
				continue;
			}
			MindMapEdge connection = new MindMapEdge(parent, this, alignment);
			double newLength = connection.getLength();
			boolean newIntersect = connection.isIntersecting(alignment);
			if ((!newIntersect && intersect) || ((newIntersect == intersect) && newLength < length)) {
				mindMapEdge = connection;
				node.setAlignment(alignment);
				intersect = newIntersect;
				length = newLength;
			}
		}
	}

	private NodeAlignment toAlignment(EuclidianBoundingBoxHandler addHandler) {
		switch (addHandler) {
		case ADD_TOP:
			return NodeAlignment.TOP;
		case ADD_RIGHT:
			return NodeAlignment.RIGHT;
		case ADD_BOTTOM:
			return NodeAlignment.BOTTOM;
		case ADD_LEFT:
			return NodeAlignment.LEFT;
		default:
			return null;
		}
	}

	public GeoMindMapNode addChildNode(EuclidianBoundingBoxHandler addHandler) {
		NodeAlignment newAlignment = toAlignment(addHandler);

		GPoint2D newLocation = computeNewLocation(newAlignment);
		GeoMindMapNode child = new GeoMindMapNode(node.getConstruction(), newLocation);
		child.setSize(GeoMindMapNode.MIN_WIDTH, GeoMindMapNode.CHILD_HEIGHT);
		child.setParent(node, newAlignment);
		child.setBackgroundColor(child.getKernel().getApplication().isMebis()
				? GColor.MOW_MIND_MAP_CHILD_BG_COLOR : GColor.MIND_MAP_CHILD_BG_COLOR);
		child.setBorderColor(child.getKernel().getApplication().isMebis()
				? GColor.MOW_MIND_MAP_CHILD_BORDER_COLOR : GColor.MIND_MAP_CHILD_BORDER_COLOR);
		return child;
	}

	private GPoint2D computeNewLocation(NodeAlignment newAlignment) {
		Comparator<DrawMindMap> comparator = newAlignment.isVerical()
				? horizontalComparator : verticalComparator;
		Comparator<DrawMindMap> intersectionComparator = newAlignment.isVerical()
				? verticalComparator : horizontalComparator;

		List<GeoMindMapNode> childGeos = node.getChildren().stream()
				.filter(node -> node.getAlignment() == newAlignment)
				.collect(Collectors.toList());

		List<DrawMindMap> children = childGeos.stream()
				.map(node -> (DrawMindMap) view.getDrawableFor(node))
				.sorted(comparator)
				.collect(Collectors.toList());

		List<DrawMindMap> intersectableChildren = node.getChildren().stream()
				.filter(node -> node.getAlignment() != newAlignment)
				.map(node -> (DrawMindMap) view.getDrawableFor(node))
				.sorted(intersectionComparator)
				.collect(Collectors.toList());

		if (newAlignment == NodeAlignment.BOTTOM || newAlignment == NodeAlignment.RIGHT) {
			Collections.reverse(intersectableChildren);
		}

		boolean correctlyAligned = correctlyAligned(newAlignment, children);
		if (correctlyAligned) {
			int spaceGained = decreaseDistanceBetweenChildren(newAlignment, children);

			if (newAlignment.isVerical()) {
				double toMove = marginLeft(newAlignment, children.size())
						+ GeoMindMapNode.MIN_WIDTH - spaceGained;
				MoveGeos.moveObjects(childGeos, new Coords(-view.getInvXscale() * toMove / 2, 0, 0),
						null, null, view);
			} else {
				double toMove = marginTop(newAlignment, children.size())
						+ GeoMindMapNode.CHILD_HEIGHT - spaceGained;
				MoveGeos.moveObjects(childGeos, new Coords(0, view.getInvYscale() * toMove / 2,  0),
						null, null, view);
			}
		}

		double left = 0;
		double top = 0;
		if (children.isEmpty()) {
			left = rectangle.getLeft() + newAlignment.dx0 * rectangle.getWidth();
			top = rectangle.getTop() + newAlignment.dy0 * rectangle.getHeight();

			switch (newAlignment) {
			case BOTTOM:
				left -= GeoMindMapNode.MIN_WIDTH / 2;
				top += 64;
				break;
			case LEFT:
				left -= 64;
				top -= GeoMindMapNode.CHILD_HEIGHT / 2;
				break;
			case TOP:
				left -= GeoMindMapNode.MIN_WIDTH / 2;
				top -= 64;
				break;
			case RIGHT:
				left += 64;
				top -= GeoMindMapNode.CHILD_HEIGHT / 2;
				break;
			}
		} else {
			Stream<DrawMindMap> stream = children.stream();
			DrawMindMap last = children.get(children.size() - 1);

			switch (newAlignment) {
			case BOTTOM:
				left = last.rectangle.getRight();
				top = stream.mapToInt(mindMap -> mindMap.rectangle.getTop()).min().orElse(0);
				break;
			case LEFT:
				left = stream.mapToInt(mindMap -> mindMap.rectangle.getRight()).min().orElse(0);
				top = last.rectangle.getBottom();
				break;
			case TOP:
				left = last.rectangle.getRight();
				top = stream.mapToInt(mindMap -> mindMap.rectangle.getBottom()).max().orElse(0);
				break;
			case RIGHT:
				left = stream.mapToInt(mindMap -> mindMap.rectangle.getLeft()).min().orElse(0);
				top = last.rectangle.getBottom();
				break;
			}

			left += marginLeft(newAlignment, children.size());
			top += marginTop(newAlignment, children.size());
		}

		double extraMovement = 0;
		for (DrawMindMap intersectableChild : intersectableChildren) {
			TransformableRectangle rect = intersectableChild.rectangle;

			if (newAlignment.isVerical()) {
				if (rect.getLeft() < left + GeoMindMapNode.MIN_WIDTH && left < rect.getRight()) {
					if (newAlignment == NodeAlignment.BOTTOM && rect.getBottom() + 16 > top) {
						extraMovement = rect.getBottom() + 16 - top;
						break;
					} else if (newAlignment == NodeAlignment.TOP && rect.getTop() < top + 16) {
						extraMovement = rect.getTop() - 16 - top;
						break;
					}
				}
			} else {
				if (rect.getTop() < top + GeoMindMapNode.CHILD_HEIGHT && top < rect.getBottom()) {
					if (newAlignment == NodeAlignment.RIGHT && rect.getRight() + 16 > left) {
						extraMovement = rect.getRight() + 16 - left;
						break;
					} else if (newAlignment == NodeAlignment.LEFT && rect.getLeft() < left + 16) {
						extraMovement = rect.getLeft() - 16 - left;
						break;
					}
				}
			}
		}

		switch (newAlignment) {
		case TOP:
			top -= GeoMindMapNode.CHILD_HEIGHT;
			break;
		case LEFT:
			left -= GeoMindMapNode.MIN_WIDTH;
			break;
		}

		if (extraMovement != 0 && correctlyAligned) {
			if (newAlignment.isVerical()) {
				MoveGeos.moveObjects(childGeos,
						new Coords(0, -view.getInvYscale() * extraMovement, 0),
						null, null, view);
			} else {
				MoveGeos.moveObjects(childGeos,
						new Coords(view.getInvXscale() * extraMovement, 0, 0),
						null, null, view);
			}
		}

		if (newAlignment.isVerical()) {
			top += extraMovement;
		} else {
			left += extraMovement;
		}

		return new GPoint2D(view.toRealWorldCoordX(left), view.toRealWorldCoordY(top));
	}

	private boolean correctlyAligned(NodeAlignment newAlignment,
			List<DrawMindMap> children) {
		if (newAlignment == NodeAlignment.TOP || newAlignment == NodeAlignment.BOTTOM) {
			for (int i = 1; i < children.size(); i++) {
				int rightOfLeft = children.get(i - 1).rectangle.getRight();
				int leftOfRight = children.get(i).rectangle.getLeft();

				int distance = leftOfRight - rightOfLeft;
				if (Math.abs(distance - marginLeft(newAlignment, children.size() - 1)) > 3) {
					return false;
				}
			}
		} else {
			for (int i = 1; i < children.size(); i++) {
				int bottomOfTop = children.get(i - 1).rectangle.getBottom();
				int topOfBottom = children.get(i).rectangle.getTop();

				int distance = topOfBottom - bottomOfTop;
				if (Math.abs(distance - marginTop(newAlignment, children.size() - 1)) > 3) {
					return false;
				}
			}
		}

		return true;
	}

	private int decreaseDistanceBetweenChildren(NodeAlignment newAlignment,
			List<DrawMindMap> children) {
		if (children.size() == 1) {
			return 0;
		}

		if (newAlignment.isVerical()) {
			if (children.size() == 2) {
				double toMove = -view.getInvXscale() * 16;
				MoveGeos.moveObjects(Collections.singletonList(children.get(1).node),
						new Coords(toMove, 0, 0), null, null, view);
				return 16;
			}
		} else {
			if (children.size() == 2) {
				double toMove = view.getInvYscale() * 32;
				MoveGeos.moveObjects(Collections.singletonList(children.get(1).node),
						new Coords(0, toMove, 0), null, null, view);
				return 32;
			} else if (children.size() == 3) {
				double toMove1 = view.getInvYscale() * 16;
				double toMove2 = view.getInvYscale() * 32;

				MoveGeos.moveObjects(Collections.singletonList(children.get(1).node),
						new Coords(0, toMove1, 0), null, null, view);
				MoveGeos.moveObjects(Collections.singletonList(children.get(2).node),
						new Coords(0, toMove2, 0), null, null, view);
				return 32;
			}
		}

		return 0;
	}

	private int marginLeft(NodeAlignment newAlignment, int size) {
		if (newAlignment.isVerical()) {
			if (size == 1) {
				return 32;
			} else {
				return 16;
			}
		}

		return 0;
	}

	private double marginTop(NodeAlignment newAlignment, int size) {
		if (!newAlignment.isVerical()) {
			if (size == 1) {
				return 64;
			} else if (size == 2) {
				return 32;
			} else {
				return 16;
			}
		}

		return 0;
	}
}
