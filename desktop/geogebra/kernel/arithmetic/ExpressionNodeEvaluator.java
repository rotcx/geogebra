package geogebra.kernel.arithmetic;

import geogebra.common.kernel.AbstractKernel;
import geogebra.common.kernel.algos.AlgoDependentNumber;
import geogebra.common.kernel.algos.AlgoListElement;
import geogebra.common.kernel.arithmetic.BooleanValue;
import geogebra.common.kernel.arithmetic.Evaluatable;
import geogebra.common.kernel.arithmetic.ExpressionNode;
import geogebra.common.kernel.arithmetic.ExpressionNodeConstants;
import geogebra.common.kernel.arithmetic.ExpressionNodeEvaluatorInterface;
import geogebra.common.kernel.arithmetic.ExpressionValue;
import geogebra.common.kernel.arithmetic.FunctionNVar;
import geogebra.common.kernel.arithmetic.Functional;
import geogebra.common.kernel.arithmetic.FunctionalNVar;
import geogebra.common.kernel.arithmetic.ListValue;
import geogebra.common.kernel.arithmetic.MyBoolean;
import geogebra.common.kernel.arithmetic.MyDouble;
import geogebra.common.kernel.arithmetic.MyList;
import geogebra.common.kernel.arithmetic.MyStringBuffer;
import geogebra.common.kernel.arithmetic.MyVecNode;
import geogebra.common.kernel.arithmetic.NumberValue;
import geogebra.common.kernel.arithmetic.Operation;
import geogebra.common.kernel.arithmetic.Polynomial;
import geogebra.common.kernel.arithmetic.Term;
import geogebra.common.kernel.arithmetic.TextValue;
import geogebra.common.kernel.arithmetic.VectorValue;
import geogebra.common.kernel.geos.GeoClass;
import geogebra.common.kernel.geos.GeoCurveCartesian;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.GeoFunction;
import geogebra.common.kernel.geos.GeoFunctionable;
import geogebra.common.kernel.geos.GeoList;
import geogebra.common.kernel.geos.GeoNumeric;
import geogebra.common.kernel.geos.GeoVec2D;
import geogebra.common.kernel.geos.ParametricCurve;
import geogebra.common.main.AbstractApplication;
import geogebra.common.main.MyError;
import geogebra.kernel.arithmetic3D.Vector3DValue;
import geogebra.kernel.geos.GeoLine;
import geogebra.kernel.geos.GeoPoint2;


/**
 * @author ggb3D
 * 
 *         Evaluator for ExpressionNode (used in Operation.evaluate())
 */
public class ExpressionNodeEvaluator implements ExpressionNodeConstants,
	ExpressionNodeEvaluatorInterface{

	/**
	 * Evaluates the ExpressionNode described by the parameters
	 * 
	 * @param expressionNode
	 *            ExpressionNode to evaluate
	 * @return corresponding ExpressionValue
	 */
	public ExpressionValue evaluate(ExpressionValue expressionValue) {
		ExpressionNode expressionNode = (ExpressionNode) expressionValue;
		boolean leaf = expressionNode.leaf;
		ExpressionValue left = expressionNode.left;

		if (leaf)
			return left.evaluate(); // for wrapping ExpressionValues as
		// ValidExpression

		AbstractKernel kernel = expressionNode.kernel;
		ExpressionValue right = expressionNode.right;
		Operation operation = expressionNode.operation;
		AbstractApplication app = expressionNode.app;
		boolean holdsLaTeXtext = expressionNode.holdsLaTeXtext;

		// Application.debug(operation+"");

		ExpressionValue lt, rt;
		MyDouble num;
		MyBoolean bool;
		GeoVec2D vec, vec2;
		MyStringBuffer msb;
		Polynomial poly;

		lt = left.evaluate(); // left tree
		if (operation.equals(Operation.NO_OPERATION))
			return lt;
		rt = right.evaluate(); // right tree

		// handle list operations first

		if (lt.isListValue()) {
			if (operation == Operation.MULTIPLY && rt.isVectorValue()) {
				MyList myList = ((ListValue) lt).getMyList();
				boolean isMatrix = myList.isMatrix();
				int rows = myList.getMatrixRows();
				int cols = myList.getMatrixCols();
				if (isMatrix && rows == 2 && cols == 2) {
					GeoVec2D myVec = (GeoVec2D)((VectorValue) rt).getVector();
					// 2x2 matrix
					myVec.multiplyMatrix(myList);

					return myVec;
				} else if (isMatrix && rows == 3 && cols == 3) {
					GeoVec2D myVec = (GeoVec2D)((VectorValue) rt).getVector();
					// 3x3 matrix, assume it's affine
					myVec.multiplyMatrixAffine(myList, rt);
					return myVec;
				}

			} else if (operation == Operation.VECTORPRODUCT && rt.isListValue()) {

				MyList listL = ((ListValue) lt.evaluate()).getMyList();
				MyList listR = ((ListValue) rt.evaluate()).getMyList();
				if ((listL.size() == 3 && listR.size() == 3) || (listL.size() == 2 && listR.size() == 2)) {
					listL.vectorProduct(listR);
					return listL;
				}

			} else if (operation != Operation.EQUAL_BOOLEAN // added EQUAL_BOOLEAN Michael
													// Borcherds 2008-04-12
					&& operation != Operation.NOT_EQUAL // ditto
					&& operation != Operation.IS_SUBSET_OF // ditto
					&& operation != Operation.IS_SUBSET_OF_STRICT // ditto
					&& operation != Operation.SET_DIFFERENCE // ditto
					&& operation != Operation.ELEMENT_OF // list1(1) to get first element
					&& !rt.isVectorValue() // eg {1,2} + (1,2)
					&& !rt.isTextValue()) // bugfix "" + {1,2} Michael Borcherds
											// 2008-06-05
			{
				MyList myList = ((ListValue) lt).getMyList();
				// list lt operation rt
				myList.applyRight(operation, rt);
				return myList;
			}
		} else if (rt.isListValue()
				&& !operation.equals(Operation.EQUAL_BOOLEAN) // added
				// EQUAL_BOOLEAN
				// Michael
				// Borcherds
				// 2008-04-12
				&& !operation.equals(Operation.NOT_EQUAL) // ditto
				&& !operation.equals(Operation.FUNCTION_NVAR) // ditto
				&& !operation.equals(Operation.FREEHAND) // ditto
				&& !lt.isVectorValue() // eg {1,2} + (1,2)
				&& !lt.isTextValue() // bugfix "" + {1,2} Michael Borcherds
				// 2008-06-05
				&& !operation.equals(Operation.IS_ELEMENT_OF)) {
			MyList myList = ((ListValue) rt).getMyList();
			// lt operation list rt
			myList.applyLeft(operation, lt);
			return myList;
		}

		else if (lt instanceof FunctionalNVar && rt instanceof FunctionalNVar
				&& !operation.equals(Operation.EQUAL_BOOLEAN)
				&& !operation.equals(Operation.NOT_EQUAL)) {
			return GeoFunction.operationSymb(operation, (FunctionalNVar) lt,
					(FunctionalNVar) rt);
		}
		// we want to use function arithmetic in cases like f*2 or f+x^2, but
		// not for f(2), f'(2) etc.
		else if (lt instanceof FunctionalNVar && rt.isNumberValue()
				&& operation.ordinal() < Operation.FUNCTION.ordinal()) {
			return GeoFunction.applyNumberSymb(operation, (FunctionalNVar) lt,
					right, true);
		} else if (rt instanceof FunctionalNVar && lt.isNumberValue()) {
			return GeoFunction.applyNumberSymb(operation, (FunctionalNVar) rt,
					left, false);
		}

		// NON-List operations (apart from EQUAL_BOOLEAN and list + text)
		switch (operation) {
		/*
		 * case NO_OPERATION: if (lt.isNumber()) return
		 * ((NumberValue)lt).getNumber(); else if (lt.isVector()) return
		 * ((VectorValue)lt).getVector(); else if (lt.isText()) return
		 * ((TextValue)lt).getText(); else { throw new MyError(app,
		 * "Unhandeled ExpressionNode entry: " + lt); }
		 */

		// spreadsheet reference: $A1, A$1, $A$1
		case $VAR_ROW:
		case $VAR_COL:
		case $VAR_ROW_COL:
			return lt;

			/*
			 * BOOLEAN operations
			 */
		case NOT:
			// NOT boolean
			if (lt.isBooleanValue()) {
				bool = ((BooleanValue) lt).getMyBoolean();
				bool.setValue(!bool.getBoolean());
				return bool;
			}

			else {
				String[] str = { "IllegalBoolean", strNOT, lt.toString() };
				throw new MyError(app, str);
			}

		case OR:
			// boolean OR boolean
			if (lt.isBooleanValue() && rt.isBooleanValue()) {
				bool = ((BooleanValue) lt).getMyBoolean();
				bool.setValue(bool.getBoolean()
						|| ((BooleanValue) rt).getBoolean());
				return bool;
			}

			else {
				String[] str = { "IllegalBoolean", lt.toString(), strOR,
						rt.toString() };
				throw new MyError(app, str);
			}

		case AND:
			// boolean AND boolean
			if (lt.isBooleanValue() && rt.isBooleanValue()) {
				bool = ((BooleanValue) lt).getMyBoolean();
				bool.setValue(bool.getBoolean()
						&& ((BooleanValue) rt).getBoolean());
				return bool;
			}

			else {
				String[] str = { "IllegalBoolean", lt.toString(), strAND,
						rt.toString() };
				throw new MyError(app, str);
			}

			/*
			 * COMPARING operations
			 */

		case EQUAL_BOOLEAN: {
			MyBoolean b = evalEquals(kernel, lt, rt);
			if (b == null) {
				String[] str = { "IllegalComparison", lt.toString(),
						strEQUAL_BOOLEAN, rt.toString() };
				throw new MyError(app, str);
			} else {
				return b;
			}
		}

		case NOT_EQUAL: {
			MyBoolean b = evalEquals(kernel, lt, rt);
			if (b == null) {
				String[] str = { "IllegalComparison", lt.toString(),
						strNOT_EQUAL, rt.toString() };
				throw new MyError(app, str);
			} else {
				// NOT equal
				b.setValue(!b.getBoolean());
				return b;
			}
		}

		case IS_ELEMENT_OF: {
			if (rt.isListValue()) {
				return new MyBoolean(kernel, MyList.isElementOf(lt,
						((ListValue) rt).getMyList()));
			} else {
				String[] str = { "IllegalListOperation", lt.toString(),
						strIS_ELEMENT_OF, rt.toString() };
				throw new MyError(app, str);
			}
		}

		case IS_SUBSET_OF: {
			if (lt.isListValue() && rt.isListValue()) {
				return new MyBoolean(kernel, MyList.listContains(
						((ListValue) rt).getMyList(),
						((ListValue) lt).getMyList()));
			} else {
				String[] str = { "IllegalListOperation", lt.toString(),
						strIS_SUBSET_OF, rt.toString() };
				throw new MyError(app, str);
			}
		}

		case IS_SUBSET_OF_STRICT: {
			if (lt.isListValue() && rt.isListValue()) {
				return new MyBoolean(kernel, MyList.listContainsStrict(
						((ListValue) rt).getMyList(),
						((ListValue) lt).getMyList()));
			} else {
				String[] str = { "IllegalListOperation", lt.toString(),
						strIS_SUBSET_OF_STRICT, rt.toString() };
				throw new MyError(app, str);
			}
		}

		case SET_DIFFERENCE: {
			if (lt.isListValue() && rt.isListValue()) {
				return MyList.setDifference(kernel,
						((ListValue) lt).getMyList(),
						((ListValue) rt).getMyList());
			} else {
				String[] str = { "IllegalListOperation", lt.toString(),
						strSET_DIFFERENCE, rt.toString() };
				throw new MyError(app, str);
			}
		}

		case LESS:
			// number < number
			if (lt.isNumberValue() && rt.isNumberValue())
				return new MyBoolean(kernel, AbstractKernel.isGreater(
						((NumberValue) rt).getDouble(),
						((NumberValue) lt).getDouble()));
			else {
				String[] str = { "IllegalComparison", lt.toString(), "<",
						rt.toString() };
				throw new MyError(app, str);
			}

		case GREATER:

			// number > number
			if (lt.isNumberValue() && rt.isNumberValue())
				return new MyBoolean(kernel, AbstractKernel.isGreater(
						((NumberValue) lt).getDouble(),
						((NumberValue) rt).getDouble()));
			else {
				String[] str = { "IllegalComparison", lt.getClass().getName(),
						lt.toString(), ">", rt.toString(),
						rt.getClass().getName() };
				throw new MyError(app, str);
			}

		case LESS_EQUAL:
			// number <= number
			if (lt.isNumberValue() && rt.isNumberValue())
				return new MyBoolean(kernel, AbstractKernel.isGreaterEqual(
						((NumberValue) rt).getDouble(),
						((NumberValue) lt).getDouble()));
			else {
				String[] str = { "IllegalComparison", lt.toString(),
						strLESS_EQUAL, rt.toString() };
				throw new MyError(app, str);
			}

		case GREATER_EQUAL:
			// number >= number
			if (lt.isNumberValue() && rt.isNumberValue())
				return new MyBoolean(kernel, AbstractKernel.isGreaterEqual(
						((NumberValue) lt).getDouble(),
						((NumberValue) rt).getDouble()));
			else {
				String[] str = { "IllegalComparison", lt.toString(),
						strGREATER_EQUAL, rt.toString() };
				throw new MyError(app, str);
			}

		case PARALLEL:
			// line parallel to line
			if (lt instanceof GeoLine && rt instanceof GeoLine) {
				return new MyBoolean(kernel,
						((GeoLine) lt).isParallel((GeoLine) rt));
			} else {
				String[] str = { "IllegalComparison", lt.toString(),
						strPARALLEL, rt.toString() };
				throw new MyError(app, str);
			}

		case PERPENDICULAR:
			// line perpendicular to line
			if (lt instanceof GeoLine && rt instanceof GeoLine) {
				return new MyBoolean(kernel,
						((GeoLine) lt).isPerpendicular((GeoLine) rt));
			} else {
				String[] str = { "IllegalComparison", lt.toString(),
						strPERPENDICULAR, rt.toString() };
				throw new MyError(app, str);
			}

			/*
			 * ARITHMETIC operations
			 */
		case PLUS:
			// number + number
			if (lt.isNumberValue() && rt.isNumberValue()) {
				num = ((NumberValue) lt).getNumber();
				MyDouble.add(num, ((NumberValue) rt).getNumber(), num);
				return num;
			}
			// vector + vector
			else if (lt.isVectorValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				GeoVec2D.add(vec, (GeoVec2D) ((VectorValue) rt).getVector(),
						vec);
				return vec;
			}
			// vector + number (for complex addition)
			else if (lt.isVectorValue() && rt.isNumberValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				GeoVec2D.add(vec, ((NumberValue) rt), vec);
				return vec;
			}
			// number + vector (for complex addition)
			else if (lt.isNumberValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) rt).getVector();
				GeoVec2D.add(vec, ((NumberValue) lt), vec);
				return vec;
			}
			// list + vector
			else if (lt.isListValue() && rt.isVectorValue()) {
				MyList list = ((ListValue) lt).getMyList();
				if (list.size() > 0) {
					ExpressionValue ev = list.getListElement(0);
					if (ev.isNumberValue()) { // eg {1,2} + (1,2) treat as point
						// + point
						vec = (GeoVec2D) ((VectorValue) rt).getVector();
						GeoVec2D.add(vec, ((ListValue) lt), vec);
						return vec;
					}
				}
				// not a list with numbers, do list operation
				MyList myList = ((ListValue) lt).getMyList();
				// list lt operation rt
				myList.applyRight(operation, rt);
				return myList;

			}
			// vector + list
			else if (rt.isListValue() && lt.isVectorValue()) {
				MyList list = ((ListValue) rt).getMyList();
				if (list.size() > 0) {
					ExpressionValue ev = list.getListElement(0);
					if (ev.isNumberValue()) { // eg {1,2} + (1,2) treat as point
						// + point
						vec = (GeoVec2D) ((VectorValue) lt).getVector();
						GeoVec2D.add(vec, ((ListValue) rt), vec);
						return vec;
					}
				}
				// not a list with numbers, do list operation
				MyList myList = ((ListValue) rt).getMyList();
				// lt operation list rt
				myList.applyLeft(operation, lt);
				return myList;
			}
			// text concatenation (left)
			else if (lt.isTextValue()) {
				msb = ((TextValue) lt).getText();
				if (holdsLaTeXtext) {
					msb.append(rt.toLaTeXString(false));
				} else {
					if (rt.isGeoElement()) {
						GeoElement geo = (GeoElement) rt;
						msb.append(geo.toDefinedValueString());
					} else {
						msb.append(rt.toValueString());
					}
				}
				return msb;
			} // text concatenation (right)
			else if (rt.isTextValue()) {
				msb = ((TextValue) rt).getText();
				if (holdsLaTeXtext) {
					msb.insert(0, lt.toLaTeXString(false));
				} else {
					if (lt.isGeoElement()) {
						GeoElement geo = (GeoElement) lt;
						msb.insert(0, geo.toDefinedValueString());
					} else {
						msb.insert(0, lt.toValueString());
					}
				}
				return msb;
			}
			// polynomial + polynomial
			else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()) {
				poly = new Polynomial(kernel, (Polynomial) lt);
				poly.add((Polynomial) rt);
				return poly;
			} else {
				String[] str = { "IllegalAddition", lt.toString(), "+",
						rt.toString() };
				throw new MyError(app, str);
			}

		case MINUS:
			// number - number
			if (lt.isNumberValue() && rt.isNumberValue()) {
				num = ((NumberValue) lt).getNumber();
				MyDouble.sub(num, ((NumberValue) rt).getNumber(), num);
				return num;
			}
			// vector - vector
			else if (lt.isVectorValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				GeoVec2D.sub(vec, (GeoVec2D) ((VectorValue) rt).getVector(),
						vec);
				return vec;
			}
			// 3D vector - 3D vector
			/*
			 * else if (lt.isVector3DValue() && rt.isVector3DValue()) { Geo3DVec
			 * vec3D = ((Vector3DValue)lt).get3DVec(); Geo3DVec.sub(vec3D,
			 * ((Vector3DValue)rt).get3DVec(), vec3D); return vec3D; }
			 */
			// vector - number (for complex subtraction)
			else if (lt.isVectorValue() && rt.isNumberValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				GeoVec2D.sub(vec, ((NumberValue) rt), vec);
				return vec;
			}
			// number - vector (for complex subtraction)
			else if (lt.isNumberValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) rt).getVector();
				GeoVec2D.sub(((NumberValue) lt), vec, vec);
				return vec;
			}
			// list - vector
			else if (lt.isListValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) rt).getVector();
				GeoVec2D.sub(vec, ((ListValue) lt), vec, false);
				return vec;
			}
			// vector - list
			else if (rt.isListValue() && lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				GeoVec2D.sub(vec, ((ListValue) rt), vec, true);
				return vec;
			}
			// polynomial - polynomial
			else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()) {
				poly = new Polynomial(kernel, (Polynomial) lt);
				poly.sub((Polynomial) rt);
				return poly;
			} else {
				String[] str = { "IllegalSubtraction", lt.toString(), "-",
						rt.toString() };
				throw new MyError(app, str);
			}

		case MULTIPLY:
			// text concatenation (left)
			if (lt.isTextValue()) {
				msb = ((TextValue) lt).getText();
				if (holdsLaTeXtext) {
					msb.append(rt.toLaTeXString(false));
				} else {
					if (rt.isGeoElement()) {
						GeoElement geo = (GeoElement) rt;
						msb.append(geo.toDefinedValueString());
					} else {
						msb.append(rt.toValueString());
					}
				}
				return msb;
			} // text concatenation (right)
			else if (rt.isTextValue()) {
				msb = ((TextValue) rt).getText();
				if (holdsLaTeXtext) {
					msb.insert(0, lt.toLaTeXString(false));
				} else {
					if (lt.isGeoElement()) {
						GeoElement geo = (GeoElement) lt;
						msb.insert(0, geo.toDefinedValueString());
					} else {
						msb.insert(0, lt.toValueString());
					}
				}
				return msb;
			} else
			// number * ...
			if (lt.isNumberValue()) {
				// number * number
				if (rt.isNumberValue()) {
					num = ((NumberValue) lt).getNumber();
					MyDouble.mult(num, ((NumberValue) rt).getNumber(), num);
					return num;
				}
				// number * vector
				else if (rt.isVectorValue()) {
					vec = (GeoVec2D) ((VectorValue) rt).getVector();
					GeoVec2D.mult(vec, ((NumberValue) lt).getDouble(), vec);
					return vec;
				}
				// number * boolean
				else if (rt.isBooleanValue()) {
					num = ((NumberValue) lt).getNumber();
					MyDouble.mult(num, ((BooleanValue) rt).getDouble(), num);
					return num;
				}

				// number * 3D vector
				/*
				 * else if (rt.isVector3DValue()) { Geo3DVec vec3D =
				 * ((Vector3DValue)rt).get3DVec(); Geo3DVec.mult(vec3D,
				 * ((NumberValue)lt).getDouble(), vec3D); return vec3D; }
				 */
				else {
					String[] str = { "IllegalMultiplication", lt.toString(),
							"*", rt.toString() };
					throw new MyError(app, str);
				}
			}
			// boolean * number
			else if (lt.isBooleanValue() && rt.isNumberValue()) {
				num = ((NumberValue) rt).getNumber();
				MyDouble.mult(num, ((BooleanValue) lt).getDouble(), num);
				return num;
			}
			/*
			 * // 3D vector * number else if (lt.isVector3DValue() &&
			 * rt.isNumberValue()) { Geo3DVec vec3D =
			 * ((Vector3DValue)lt).get3DVec(); Geo3DVec.mult(vec3D,
			 * ((NumberValue)rt).getDouble(), vec3D); return vec3D; } // 3D
			 * vector * 3D Vector (inner/dot product) else if
			 * (lt.isVector3DValue() && rt.isVector3DValue()) { Geo3DVec vec3D =
			 * ((Vector3DValue)lt).get3DVec(); num = new MyDouble(kernel);
			 * Geo3DVec.inner(vec3D, ((Vector3DValue)rt).get3DVec(), num);
			 * return num; }
			 */
			// vector * ...
			else if (lt.isVectorValue()) {
				// vector * number
				if (rt.isNumberValue()) {
					vec = (GeoVec2D) ((VectorValue) lt).getVector();
					GeoVec2D.mult(vec, ((NumberValue) rt).getDouble(), vec);
					return vec;
				}
				// vector * vector (inner/dot product)
				else if (rt.isVectorValue()) {
					vec = (GeoVec2D) ((VectorValue) lt).getVector();
					if (vec.getMode() == AbstractKernel.COORD_COMPLEX) {

						// complex multiply

						GeoVec2D.complexMultiply(vec,
								(GeoVec2D) ((VectorValue) rt).getVector(), vec);
						return vec;
					} else {
						num = new MyDouble(kernel);
						GeoVec2D.inner(vec,
								(GeoVec2D) ((VectorValue) rt).getVector(), num);
						return num;
					}
				}

				else {
					String[] str = { "IllegalMultiplication", lt.toString(),
							"*", rt.toString() };
					throw new MyError(app, str);
				}
			}
			// polynomial * polynomial
			else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()) {
				poly = new Polynomial(kernel, (Polynomial) lt);
				poly.multiply((Polynomial) rt);
				return poly;
			} else if (lt.isTextValue()) {
				msb = ((TextValue) lt).getText();
				if (holdsLaTeXtext) {
					msb.append(rt.toLaTeXString(false));
				} else {
					if (rt.isGeoElement()) {
						GeoElement geo = (GeoElement) rt;
						msb.append(geo.toDefinedValueString());
					} else {
						msb.append(rt.toValueString());
					}
				}
				return msb;
			} // text concatenation (right)
			else if (rt.isTextValue()) {
				msb = ((TextValue) rt).getText();
				if (holdsLaTeXtext) {
					msb.insert(0, lt.toLaTeXString(false));
				} else {
					if (lt.isGeoElement()) {
						GeoElement geo = (GeoElement) lt;
						msb.insert(0, geo.toDefinedValueString());
					} else {
						msb.insert(0, lt.toValueString());
					}
				}
				return msb;
			} else {
				String[] str = { "IllegalMultiplication", lt.toString(), "*",
						rt.toString() };
				throw new MyError(app, str);
			}

		case DIVIDE:
			if (rt.isNumberValue()) {
				// number / number
				if (lt.isNumberValue()) {
					num = ((NumberValue) lt).getNumber();
					MyDouble.div(num, ((NumberValue) rt).getNumber(), num);
					return num;
				}
				// vector / number
				else if (lt.isVectorValue()) {
					vec = (GeoVec2D) ((VectorValue) lt).getVector();
					GeoVec2D.div(vec, ((NumberValue) rt).getDouble(), vec);
					return vec;
				} else if (lt instanceof GeoFunction) {
					return GeoFunction.applyNumberSymb(Operation.DIVIDE,
							(GeoFunction) lt, right, true);
				}
				/*
				 * // number * 3D vector else if (lt.isVector3DValue()) {
				 * Geo3DVec vec3D = ((Vector3DValue)lt).get3DVec();
				 * Geo3DVec.div(vec3D, ((NumberValue)rt).getDouble(), vec3D);
				 * return vec3D; }
				 */
				else {
					String[] str = { "IllegalDivision", lt.toString(), "/",
							rt.toString() };
					throw new MyError(app, str);
				}
			}
			// polynomial / polynomial
			else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()) {
				// the divisor must be a polynom of degree 0
				if (((Polynomial) rt).degree() != 0) {
					String[] str = { "DivisorMustBeConstant", lt.toString(),
							"/", rt.toString() };
					throw new MyError(app, str);
				}

				poly = new Polynomial(kernel, (Polynomial) lt);
				poly.divide((Polynomial) rt);
				return poly;
			}
			// vector / vector (complex division Michael Borcherds 2007-12-09)
			else if (lt.isVectorValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				GeoVec2D.complexDivide(vec,
						(GeoVec2D) ((VectorValue) rt).getVector(), vec);
				return vec;

			}
			// number / vector (complex division Michael Borcherds 2007-12-09)
			else if (lt.isNumberValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) rt).getVector(); // just to
																	// initialise
																	// vec
				GeoVec2D.complexDivide((NumberValue) lt,
						(GeoVec2D) ((VectorValue) rt).getVector(), vec);
				return vec;

			}

			else if (rt instanceof GeoFunction && lt.isNumberValue()) {
				return GeoFunction.applyNumberSymb(Operation.DIVIDE,
						(GeoFunction) rt, left, false);
			} else {
				String[] str = { "IllegalDivision", lt.toString(), "/",
						rt.toString() };
				throw new MyError(app, str);
			}

		case VECTORPRODUCT:
			if (lt.isVectorValue() && rt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				vec2 = (GeoVec2D) ((VectorValue) rt).getVector();
				num = new MyDouble(kernel);
				GeoVec2D.vectorProduct(vec, vec2, num);
				return num;
			} else {
				String[] str2 = { "IllegalMultiplication", lt.toString(),
						strVECTORPRODUCT, rt.toString() };
				throw new MyError(app, str2);
			}

		case POWER:
			// number ^ number
			if (lt.isNumberValue() && rt.isNumberValue()) {
				num = ((NumberValue) lt).getNumber();
				double base = num.getDouble();
				MyDouble exponent = ((NumberValue) rt).getNumber();

				// special case: e^exponent (Euler number)
				if (base == Math.E) {
					return exponent.exp();
				}

				// special case: left side is negative and
				// right side is a fraction a/b with a and b integers
				// x^(a/b) := (x^a)^(1/b)
				if (base < 0 && right.isExpressionNode()) {
					ExpressionNode node = (ExpressionNode) right;
					if (node.operation.equals(Operation.DIVIDE)) {
						// check if we have a/b with a and b integers
						double a = ((NumberValue) node.left.evaluate())
								.getDouble();
						long al = Math.round(a);
						if (AbstractKernel.isEqual(a, al)) { // a is integer
							double b = ((NumberValue) node.right.evaluate())
									.getDouble();
							long bl = Math.round(b);
							if (b == 0)
								// (x^a)^(1/0)
								num.set(Double.NaN);
							else if (AbstractKernel.isEqual(b, bl)) { // b is integer
								// divide through greatest common divisor of a
								// and b
								long gcd = AbstractKernel.gcd(al, bl);
								al = al / gcd;
								bl = bl / gcd;

								// we will now evaluate (x^a)^(1/b) instead of
								// x^(a/b)
								// set base = x^a
								if (al != 1)
									base = Math.pow(base, al);
								if (base > 0) {
									// base > 0 => base^(1/b) is no problem
									num.set(Math.pow(base, 1d / bl));
								} else { // base < 0
									boolean oddB = Math.abs(bl) % 2 == 1;
									if (oddB) {
										// base < 0 and b odd: (base)^(1/b) =
										// -(-base^(1/b))
										num.set(-Math.pow(-base, 1d / bl));
									} else {
										// base < 0 and a & b even: (base)^(1/b)
										// = undefined
										num.set(Double.NaN);
									}
								}
								return num;
							}
						}
					}
				}

				// standard case
				MyDouble.pow(num, exponent, num);
				return num;
			}
			/*
			 * // vector ^ 2 (inner product) (3D) else if (lt.isVector3DValue()
			 * && rt.isNumberValue()) { num = ((NumberValue)rt).getNumber();
			 * Geo3DVec vec3D = ((Vector3DValue)lt).get3DVec(); if
			 * (num.getDouble() == 2.0) { Geo3DVec.inner(vec3D, vec3D, num); }
			 * else { num.set(Double.NaN); } return num; }
			 */
			// vector ^ 2 (inner product)
			else if (lt.isVectorValue() && rt.isNumberValue()) {
				// if (!rt.isConstant()) {
				// String [] str = { "ExponentMustBeConstant", lt.toString(),
				// "^", rt.toString() };
				// throw new MyError(app, str);
				// }
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				num = ((NumberValue) rt).getNumber();

				if (vec.getMode() == AbstractKernel.COORD_COMPLEX) {

					// complex power

					GeoVec2D.complexPower(vec, num, vec);
					return vec;

				} else {
					// inner/scalar/dot product
					if (num.getDouble() == 2.0) {
						GeoVec2D.inner(vec, vec, num);
						return num;
					} else {
						num.set(Double.NaN);
						return num;
						// String [] str = { "IllegalExponent", lt.toString(),
						// "^", rt.toString() };
						// throw new MyError(app, str);
					}
				}
				// complex number ^ complex number
			} else if (lt.isVectorValue() && rt.isVectorValue()) {
				// if (!rt.isConstant()) {
				// String [] str = { "ExponentMustBeConstant", lt.toString(),
				// "^", rt.toString() };
				// throw new MyError(app, str);
				// }
				vec = (GeoVec2D) ((VectorValue) lt).getVector();
				vec2 = (GeoVec2D) ((VectorValue) rt).getVector();

				// complex power

				GeoVec2D.complexPower(vec, vec2, vec);
				return vec;

			} else if (lt.isNumberValue() && rt.isVectorValue()) {
				// if (!rt.isConstant()) {
				// String [] str = { "ExponentMustBeConstant", lt.toString(),
				// "^", rt.toString() };
				// throw new MyError(app, str);
				// }
				num = ((NumberValue) lt).getNumber();
				vec = (GeoVec2D) ((VectorValue) rt).getVector();

				// real ^ complex

				GeoVec2D.complexPower(num, vec, vec);
				return vec;

			}
			// polynomial ^ number
			else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()) {
				// the exponent must be a number
				if (((Polynomial) rt).degree() != 0) {
					String[] str = { "ExponentMustBeInteger", lt.toString(),
							"^", rt.toString() };
					throw new MyError(app, str);
				}

				// is the base also a number? In this case pull base^exponent
				// together into lt polynomial
				boolean baseIsNumber = ((Polynomial) lt).degree() == 0;
				if (baseIsNumber) {
					Term base = ((Polynomial) lt).getTerm(0);
					Term exponent = ((Polynomial) rt).getTerm(0);
					Term newBase = new Term(kernel, new ExpressionNode(kernel,
							base.getCoefficient(), Operation.POWER,
							exponent.getCoefficient()), "");

					return new Polynomial(kernel, newBase);
				}

				// number is not a base
				if (!rt.isConstant()) {
					String[] str = { "ExponentMustBeConstant", lt.toString(),
							"^", rt.toString() };
					throw new MyError(app, str);
				}

				// get constant coefficent of given polynomial
				double exponent = ((Polynomial) rt).getConstantCoeffValue();
				if ((AbstractKernel.isInteger(exponent) && (int) exponent >= 0)) {
					poly = new Polynomial(kernel, (Polynomial) lt);
					poly.power((int) exponent);
					return poly;
				} else {
					String[] str = { "ExponentMustBeInteger", lt.toString(),
							"^", rt.toString() };
					throw new MyError(app, str);
				}
			} else {
				AbstractApplication.debug("power: lt :" + lt.getClass() + ", rt: "
						+ rt.getClass());
				String[] str = { "IllegalExponent", lt.toString(), "^",
						rt.toString() };
				throw new MyError(app, str);
			}

		case COS:
			// cos(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().cos();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.COS, null), ""));
			} else {
				String[] str = { "IllegalArgument", "cos", lt.toString() };
				throw new MyError(app, str);
			}

		case SIN:
			// sin(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().sin();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.SIN, null), ""));
			} else {
				String[] str = { "IllegalArgument", "sin", lt.toString() };
				throw new MyError(app, str);
			}

		case FREEHAND:
			// freehand function
			if (lt.isNumberValue() && rt.isListValue()) {
				double x = ((NumberValue) lt).getDouble();
				double ret = Double.NaN;
				if (rt.isGeoElement()) {
					GeoList list = (GeoList) rt;
					int n = list.size() - 3;
					if (n >= 1
							&& list.getElementType().equals(GeoClass.NUMERIC)) {
						double min = ((GeoNumeric) (list.get(0))).getDouble();
						double max = ((GeoNumeric) (list.get(1))).getDouble();

						if (min > max || x > max || x < min)
							return new MyDouble(kernel, Double.NaN);

						double step = (max - min) / n;

						int index = (int) Math.floor((x - min) / step);

						if (index > n - 1) {
							ret = ((GeoNumeric) (list.get(n + 2))).getDouble();
						} else {

							double y1 = ((GeoNumeric) (list.get(index + 2)))
									.getDouble();
							double y2 = ((GeoNumeric) (list.get(index + 3)))
									.getDouble();
							double x1 = min + index * step;

							// linear interpolation between (x1,y1) and
							// (x2,y2+step) to give (x,ret)
							ret = y1 + (x - x1) * (y2 - y1) / step;
						}
					}
				}

				// Application.debug(lt.getClass()+" "+rt.getClass());

				return new MyDouble(kernel, ret);

			} else {
				String[] str = { "IllegalArgument", "freehand", lt.toString() };
				AbstractApplication.debug(lt.getClass() + " " + rt.getClass());
				throw new MyError(app, str);
			}

		case TAN:
			// tan(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().tan();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.TAN, null), ""));
			} else {
				String[] str = { "IllegalArgument", "tan", lt.toString() };
				throw new MyError(app, str);
			}

		case ARCCOS:
			// arccos(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().acos();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ARCCOS, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "arccos", lt.toString() };
				throw new MyError(app, str);
			}

		case ARCSIN:
			// arcsin(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().asin();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ARCSIN, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "arcsin", lt.toString() };
				throw new MyError(app, str);
			}

		case ARCTAN:
			// arctan(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().atan();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ARCTAN, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "arctan", lt.toString() };
				throw new MyError(app, str);
			}

		case ARCTAN2:
			// arctan2(number, number)
			if (lt.isNumberValue() && rt.isNumberValue()) {
				return ((NumberValue) lt).getNumber().atan2((NumberValue) rt)
						.getNumber();
			} else {
				String[] str = { "IllegalArgument", "arctan2", lt.toString(),
						rt.toString() };
				throw new MyError(app, str);
			}

		case COSH:
			// cosh(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().cosh();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.COSH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "cosh", lt.toString() };
				throw new MyError(app, str);
			}

		case SINH:
			// sinh(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().sinh();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.SINH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "sinh", lt.toString() };
				throw new MyError(app, str);
			}

		case TANH:
			// tanh(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().tanh();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.TANH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "tanh", lt.toString() };
				throw new MyError(app, str);
			}

		case ACOSH:
			// acosh(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().acosh();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ACOSH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "acosh", lt.toString() };
				throw new MyError(app, str);
			}

		case ASINH:
			// asinh(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().asinh();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ASINH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "asinh", lt.toString() };
				throw new MyError(app, str);
			}

		case ATANH:
			// tanh(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().atanh();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ATANH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "atanh", lt.toString() };
				throw new MyError(app, str);
			}

		case CSC:
			// csc(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().csc();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.CSC, null), ""));
			} else {
				String[] str = { "IllegalArgument", "csc", lt.toString() };
				throw new MyError(app, str);
			}

		case SEC:
			// sec(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().sec();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.SEC, null), ""));
			} else {
				String[] str = { "IllegalArgument", "sec", lt.toString() };
				throw new MyError(app, str);
			}

		case COT:
			// cot(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().cot();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.COT, null), ""));
			} else {
				String[] str = { "IllegalArgument", "cot", lt.toString() };
				throw new MyError(app, str);
			}

		case CSCH:
			// csch(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().csch();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.CSCH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "csch", lt.toString() };
				throw new MyError(app, str);
			}

		case SECH:
			// sech(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().sech();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.SECH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "sech", lt.toString() };
				throw new MyError(app, str);
			}

		case COTH:
			// coth(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().coth();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.COTH, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "coth", lt.toString() };
				throw new MyError(app, str);
			}

		case EXP:
			// exp(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().exp();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.EXP, null), ""));
			} else if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				// complex e^z

				GeoVec2D.complexExp(vec, vec);
				return vec;

			} else {
				String[] str = { "IllegalArgument", "exp", lt.toString() };
				throw new MyError(app, str);
			}

		case LOG:
			// log(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().log();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.LOG, null), ""));
			} else if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				// complex natural log(z)

				GeoVec2D.complexLog(vec, vec);
				return vec;

			} else {
				String[] str = { "IllegalArgument", "log", lt.toString() };
				throw new MyError(app, str);
			}

		case LOGB:
			// log(base, number)
			if (lt.isNumberValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber().log((NumberValue) lt);
			} else {
				String[] str = { "IllegalArgument", "log", lt.toString(),
						rt.toString() };
				throw new MyError(app, str);
			}

		case GAMMA_INCOMPLETE:
			// log(base, number)
			if (lt.isNumberValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber().gammaIncomplete(
						(NumberValue) lt);
			} else {
				String[] str = { "IllegalArgument", "gammaIncomplete",
						lt.toString(), rt.toString() };
				throw new MyError(app, str);
			}

		case GAMMA_INCOMPLETE_REGULARIZED:
			// log(base, number)
			if (lt.isNumberValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber()
						.gammaIncompleteRegularized((NumberValue) lt);
			} else {
				String[] str = { "IllegalArgument",
						"gammaIncompleteRegularized", lt.toString(),
						rt.toString() };
				throw new MyError(app, str);
			}

		case BETA:
			// log(base, number)
			if (lt.isNumberValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber().beta((NumberValue) lt);
			} else {
				String[] str = { "IllegalArgument", "beta", lt.toString(),
						rt.toString() };
				throw new MyError(app, str);
			}

		case BETA_INCOMPLETE:
			// log(base, number)
			if (lt.isVectorValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber().betaIncomplete(
						(VectorValue) lt);
			} else {
				String[] str = { "IllegalArgument", "betaIncomplete",
						lt.toString(), rt.toString() };
				throw new MyError(app, str);
			}

		case BETA_INCOMPLETE_REGULARIZED:
			// log(base, number)
			if (lt.isVectorValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber()
						.betaIncompleteRegularized((VectorValue) lt);
			} else {
				String[] str = { "IllegalArgument",
						"betaIncompleteRegularized", lt.toString(),
						rt.toString() };
				throw new MyError(app, str);
			}

		case ERF:
			// log(base, number)
			if (lt.isNumberValue()) {
				return ((NumberValue) lt).getNumber().erf();
			} else {
				String[] str = { "IllegalArgument", "erf", lt.toString() };
				throw new MyError(app, str);
			}

		case PSI:
			// log(base, number)
			if (lt.isNumberValue()) {
				return ((NumberValue) lt).getNumber().psi();
			} else {
				String[] str = { "IllegalArgument", "erf", lt.toString() };
				throw new MyError(app, str);
			}

		case POLYGAMMA:
			// polygamma(order, number)
			if (lt.isNumberValue() && rt.isNumberValue()) {
				return ((NumberValue) rt).getNumber().polygamma(
						(NumberValue) lt);
			} else {
				String[] str = { "IllegalArgument", "polygamma", lt.toString(),
						rt.toString() };
				throw new MyError(app, str);
			}

		case LOG10:
			// log(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().log10();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.LOG10, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "lg", lt.toString() };
				throw new MyError(app, str);
			}

		case LOG2:
			// log(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().log2();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.LOG2, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "ld", lt.toString() };
				throw new MyError(app, str);
			}

		case SQRT:
			// sqrt(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().sqrt();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.SQRT, null),
						""));
			} else if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				// complex sqrt
				GeoVec2D.complexSqrt(vec, vec);
				return vec;

			} else {
				String[] str = { "IllegalArgument", "sqrt", lt.toString() };
				throw new MyError(app, str);
			}

		case CBRT:
			// cbrt(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().cbrt();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.CBRT, null),
						""));
			} else if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				// complex cbrt
				GeoVec2D.complexCbrt(vec, vec);
				return vec;

			} else {
				String[] str = { "IllegalArgument", "cbrt", lt.toString() };
				throw new MyError(app, str);
			}

		case CONJUGATE:
			// cbrt(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber();
			else if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				// complex cbrt
				GeoVec2D.complexConjugate(vec, vec);
				return vec;

			} else {
				String[] str = { "IllegalArgument", "cbrt", lt.toString() };
				throw new MyError(app, str);
			}

		case ARG:
			if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				MyDouble ret = new MyDouble(kernel, GeoVec2D.arg(vec));
				ret.setAngle();
				return ret;
			} else if (lt.isNumberValue()) {
				return new MyDouble(kernel,
						((NumberValue) lt).getDouble() < 0 ? Math.PI : 0);
			} else {
				String[] str = { "IllegalArgument", "arg", lt.toString() };
				throw new MyError(app, str);
			}

		case ABS:
			// abs(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().abs();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.ABS, null), ""));
			} else if (lt.isVectorValue()) {
				vec = (GeoVec2D) ((VectorValue) lt).getVector();

				// complex Abs(z)
				// or magnitude of point

				return new MyDouble(kernel, GeoVec2D.complexAbs(vec));

			} else {
				String[] str = { "IllegalArgument", "abs", lt.toString() };
				throw new MyError(app, str);
			}

		case SGN:
			// sgn(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().sgn();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.SGN, null), ""));
			} else {
				String[] str = { "IllegalArgument", "sgn", lt.toString() };
				throw new MyError(app, str);
			}

		case FLOOR:
			// floor(number)
			if (lt.isNumberValue()) {
				return ((NumberValue) lt).getNumber().floor();
			} else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.FLOOR, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "floor", lt.toString() };
				throw new MyError(app, str);
			}

		case CEIL:
			// ceil(number)
			if (lt.isNumberValue()) {
				return ((NumberValue) lt).getNumber().ceil();
			} else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.CEIL, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "ceil", lt.toString() };
				throw new MyError(app, str);
			}

		case ROUND:
			// ceil(number)
			if (lt.isNumberValue()) {
				return ((NumberValue) lt).getNumber().round();
			} else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ROUND, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "round", lt.toString() };
				throw new MyError(app, str);
			}

		case FACTORIAL:
			// factorial(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().factorial();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.FACTORIAL,
								null), ""));
			} else {
				String[] str = { "IllegalArgument", lt.toString(), " !" };
				throw new MyError(app, str);
			}

		case GAMMA:
			// ceil(number)
			if (lt.isNumberValue())
				return ((NumberValue) lt).getNumber().gamma();
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.GAMMA, null),
						""));
			} else {
				String[] str = { "IllegalArgument", "gamma", lt.toString() };
				throw new MyError(app, str);
			}

		case RANDOM:
			// random()
			// note: left tree holds MyDouble object to set random number
			// in randomize()
			return ((NumberValue) lt).getNumber();

		case XCOORD:
			// x(vector)
			if (lt.isVectorValue())
				return new MyDouble(kernel, ((VectorValue) lt).getVector()
						.getX());
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.XCOORD, null),
						""));
			} else if (lt.isVector3DValue()) {
				return new MyDouble(kernel,
						((Vector3DValue) lt).getPointAsDouble()[0]);
			} else if (lt instanceof GeoLine) {
				return new MyDouble(kernel, ((GeoLine) lt).x);
			} else {
				String[] str = { "IllegalArgument", "x(", lt.toString(), ")" };
				throw new MyError(app, str);
			}

		case YCOORD:
			// y(vector)
			if (lt.isVectorValue())
				return new MyDouble(kernel, ((VectorValue) lt).getVector()
						.getY());
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.YCOORD, null),
						""));
			} else if (lt.isVector3DValue()) {
				return new MyDouble(kernel,
						((Vector3DValue) lt).getPointAsDouble()[1]);
			} else if (lt instanceof GeoLine) {
				return new MyDouble(kernel, ((GeoLine) lt).y);
			} else {
				String[] str = { "IllegalArgument", "y(", lt.toString(), ")" };
				throw new MyError(app, str);
			}

		case ZCOORD:
			// z(vector)
			if (lt.isVectorValue())
				return new MyDouble(kernel, 0);
			else if (lt.isPolynomialInstance()
					&& ((Polynomial) lt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.ZCOORD, null),
						""));
			} else if (lt.isVector3DValue()) {
				return new MyDouble(kernel,
						((Vector3DValue) lt).getPointAsDouble()[2]);
			} else if (lt instanceof GeoLine) {
				return new MyDouble(kernel, ((GeoLine) lt).z);
			} else {
				String[] str3 = { "IllegalArgument", "z(", lt.toString(), ")" };
				throw new MyError(app, str3);
			}

			/*
			 * list1={1,2,3} list1(3) to get 3rd element
			 */
		case ELEMENT_OF:
			// Application.debug(rt.getClass()+" "+rt.getClass());
			if (rt.isListValue() && lt instanceof GeoList) {

				GeoElement subList = ((GeoList) lt);
				ListValue lv = (ListValue) rt;

				NumberValue[] nvs = new NumberValue[lv.size()];
				// convert list1(1,2) into Element[Element[list1,1],2]
				for (int i = 0; i < lv.size(); i++) {
					ExpressionNode ith = (ExpressionNode) lv.getMyList()
							.getListElement(i);
					if (ith.isConstant())
						nvs[i] = (NumberValue) ith.evaluate();
					else {
						AlgoDependentNumber adn = new AlgoDependentNumber(
								kernel.getConstruction(), ith, false);
						nvs[i] = adn.getNumber();
					}
				}
				AlgoListElement algo = new AlgoListElement(
						kernel.getConstruction(), (GeoList) subList, nvs, true);
				return algo.getElement();
			} else

			// fallthrough
			{
				// Application.debug("FUNCTION lt: " + lt + ", " + lt.getClass()
				// + " rt: " + rt + ", " + rt.getClass());
				String[] str = { "IllegalArgument", rt.toString() };
				throw new MyError(app, str);
			}

		case FUNCTION:
			// function(number)
			if (rt.isNumberValue()) {
				if (lt instanceof Evaluatable) {
					NumberValue arg = (NumberValue) rt;
					if (lt instanceof GeoFunction
							&& ((GeoFunction) lt).isBooleanFunction())
						return new MyBoolean(kernel,
								((GeoFunction) lt).evaluateBoolean(arg
										.getDouble()));
					return arg.getNumber().apply((Evaluatable) lt);
				}
			} else if (rt instanceof GeoPoint2) {
				if (lt instanceof Evaluatable) {
					GeoPoint2 pt = (GeoPoint2) rt;
					if (lt instanceof GeoFunction) {
						FunctionNVar fun = ((GeoFunction) lt).getFunction();
						if (lt instanceof GeoFunction
								&& fun.isBooleanFunction())
							return new MyBoolean(kernel,
									fun.evaluateBoolean(pt));
						return new MyDouble(kernel, fun.evaluate(pt));
					} else if (lt instanceof GeoFunctionable) {
						// eg GeoLine
						return new MyDouble(kernel, ((GeoFunctionable) lt)
								.getGeoFunction().getFunction().evaluate(pt));
					} else
						AbstractApplication
								.debug("missing case in ExpressionNodeEvaluator");
				}
			} else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()
					&& ((Polynomial) rt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				rt = ((Polynomial) rt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.FUNCTION, rt),
						""));
			} else {
				// Application.debug("FUNCTION lt: " + lt + ", " + lt.getClass()
				// + " rt: " + rt + ", " + rt.getClass());
				String[] str = { "IllegalArgument", rt.toString() };
				throw new MyError(app, str);
			}

		case FUNCTION_NVAR:
			// function(list of numbers)
			if (rt.isListValue() && lt instanceof FunctionalNVar) {
				FunctionNVar funN = (FunctionNVar)((FunctionalNVar) lt).getFunction();
				ListValue list = (ListValue) rt;
				if (funN.getVarNumber() == list.size()) {
					double[] args = list.toDouble();
					if (args != null) {
						if (funN.isBooleanFunction())
							return new MyBoolean(kernel,
									funN.evaluateBoolean(args));
						return new MyDouble(kernel, funN.evaluate(args));
					}
					// let's assume that we called this as f(x,y) and we
					// actually want the function
					return lt;
				} else if (list.size() == 1) {
					ExpressionValue ev = list.getMyList().getListElement(0)
							.evaluate();
					AbstractApplication.debug(ev.getClass());
					if (funN.getVarNumber() == 2 && ev instanceof GeoPoint2) {
						GeoPoint2 pt = (GeoPoint2) ev;
						if (funN.isBooleanFunction())
							return new MyBoolean(kernel,
									funN.evaluateBoolean(pt));
						return new MyDouble(kernel, funN.evaluate(pt));
					} else if (funN.getVarNumber() == 2
							&& ev instanceof MyVecNode) {
						MyVecNode pt = (MyVecNode) ev;
						double[] vals = new double[] {
								((NumberValue) pt.getX().evaluate())
										.getDouble(),
								((NumberValue) pt.getY().evaluate())
										.getDouble() };
						if (funN.isBooleanFunction())
							return new MyBoolean(kernel,
									funN.evaluateBoolean(vals));
						return new MyDouble(kernel, funN.evaluate(vals));
					} else if (ev instanceof ListValue
							&& ((ListValue) ev).getMyList().getListElement(0)
									.evaluate().isNumberValue()) {
						double[] vals = ((ListValue) ev).toDouble();
						if (vals != null) {
							if (funN.isBooleanFunction())
								return new MyBoolean(kernel,
										funN.evaluateBoolean(vals));
							return new MyDouble(kernel, funN.evaluate(vals));
						}
					} else if (ev instanceof ListValue) { // f(x,y) called with
						// list of points
						MyList l = ((ListValue) ev).getMyList();
						MyList ret = new MyList(kernel);
						for (int i = 0; i < l.size(); i++) {
							MyList lArg = new MyList(kernel); // need to wrap
							// arguments to
							// f(x,y) in
							// MyList
							lArg.addListElement(l.getListElement(i));
							ret.addListElement(new ExpressionNode(kernel, funN,
									Operation.FUNCTION_NVAR, lArg));
						}
						return ret;
					}

					// let's assume that we called this as f(x,y) and we
					// actually want the function
					return lt;
				}
			}
			// Application.debug("FUNCTION lt: " + lt + ", " + lt.getClass() +
			// " rt: " + rt + ", " + rt.getClass());
			String[] str3 = { "IllegalArgument", rt.toString() };
			throw new MyError(app, str3);

		case VEC_FUNCTION:
			// vecfunction(number)
			if (rt.isNumberValue()) {
				NumberValue arg = (NumberValue) rt;
				return ((ParametricCurve) lt).evaluateCurve(arg.getDouble());
			} else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()
					&& ((Polynomial) rt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				rt = ((Polynomial) rt).getConstantCoefficient();
				return new Polynomial(kernel, new Term(kernel,
						new ExpressionNode(kernel, lt, Operation.VEC_FUNCTION,
								rt), ""));
			} else {
				// Application.debug("lt: " + lt.getClass() + " rt: " +
				// rt.getClass());
				String[] str = { "IllegalArgument", rt.toString() };
				throw new MyError(app, str);
			}

		case DERIVATIVE:
			// Application.debug("DERIVATIVE called");
			// derivative(function, order)
			if (rt.isNumberValue()) {
				if (lt instanceof Functional) { // derivative of GeoFunction
					return ((Functional) lt).getGeoDerivative((int) Math
							.round(((NumberValue) rt).getDouble()));
				} else if (lt instanceof GeoCurveCartesian) { // derivative of
																// GeoCurveCartesian
					return ((GeoCurveCartesian) lt).getGeoDerivative((int) Math
							.round(((NumberValue) rt).getDouble()));
				}
			} else if (lt.isPolynomialInstance() && rt.isPolynomialInstance()
					&& ((Polynomial) rt).degree() == 0) {
				lt = ((Polynomial) lt).getConstantCoefficient();
				rt = ((Polynomial) rt).getConstantCoefficient();
				return new Polynomial(kernel,
						new Term(kernel, new ExpressionNode(kernel, lt,
								Operation.DERIVATIVE, rt), ""));
			}
			// error if we get here
			String[] str = { "IllegalArgument", rt.toString() };
			throw new MyError(app, str);

		default:
			throw new MyError(app, "ExpressionNode: Unhandled operation."
					+ (operation.toString()));

		}
	}

	/**
	 * 
	 * @param lt
	 * @param rt
	 * @return false if not defined
	 */
	private MyBoolean evalEquals(AbstractKernel kernel, ExpressionValue lt,
			ExpressionValue rt) {
		// booleans
		if (lt.isBooleanValue() && rt.isBooleanValue())
			return new MyBoolean(kernel,
					((BooleanValue) lt).getBoolean() == ((BooleanValue) rt)
							.getBoolean());

		// nummber == number
		else if (lt.isNumberValue() && rt.isNumberValue())
			return new MyBoolean(kernel, AbstractKernel.isEqual(
					((NumberValue) lt).getDouble(),
					((NumberValue) rt).getDouble()));

		// needed for eg If[""=="a",0,1]
		// when lt and rt are MyStringBuffers
		else if (lt.isTextValue() && rt.isTextValue()) {

			String strL = ((TextValue) lt).toValueString();
			String strR = ((TextValue) rt).toValueString();

			// needed for eg Sequence[If[Element[list1,i]=="b",0,1],i,i,i]
			if (strL == null || strR == null)
				return new MyBoolean(kernel, false);

			return new MyBoolean(kernel, strL.equals(strR));
		} else if (lt.isListValue() && rt.isListValue()) {

			MyList list1 = ((ListValue) lt).getMyList();
			MyList list2 = ((ListValue) rt).getMyList();

			int size = list1.size();

			if (size != list2.size())
				return new MyBoolean(kernel, false);

			for (int i = 0; i < size; i++) {
				if (!evalEquals(kernel, list1.getListElement(i).evaluate(),
						list2.getListElement(i).evaluate()).getBoolean())
					return new MyBoolean(kernel, false);
			}

			return new MyBoolean(kernel, true);

		} else if (lt.isGeoElement() && rt.isGeoElement()) {
			GeoElement geo1 = (GeoElement) lt;
			GeoElement geo2 = (GeoElement) rt;

			return new MyBoolean(kernel, geo1.isEqual(geo2));
		} else if (lt.isVectorValue() && rt.isVectorValue()) {
			VectorValue vec1 = (VectorValue) lt;
			VectorValue vec2 = (VectorValue) rt;
			return new MyBoolean(kernel,
					((GeoVec2D) vec1.getVector()).isEqual((GeoVec2D) vec2
							.getVector()));
		} else if (lt.isVector3DValue() && rt.isVector3DValue()) {
			Vector3DValue vec1 = (Vector3DValue) lt;
			Vector3DValue vec2 = (Vector3DValue) rt;
			return new MyBoolean(kernel, vec1.get3DVec().isEqual(
					vec2.get3DVec()));
		}

		return new MyBoolean(kernel, false);
	}

}
