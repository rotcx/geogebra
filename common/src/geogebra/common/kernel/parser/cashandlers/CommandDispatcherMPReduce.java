package geogebra.common.kernel.parser.cashandlers;

import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.StringTemplate;
import geogebra.common.kernel.arithmetic.ExpressionNode;
import geogebra.common.kernel.arithmetic.ExpressionValue;
import geogebra.common.kernel.arithmetic.MyDouble;
import geogebra.common.kernel.arithmetic.MyList;
import geogebra.common.kernel.arithmetic.Variable;
import geogebra.common.plugin.Operation;

/**
 * Handles special MPReduce commands to distinguish them from user defined
 * functions in the Parser.
 * 
 * @author Markus Hohenwarter
 */
public class CommandDispatcherMPReduce {

	/**
	 * Enum for special commands that may be returned by MPReduce.
	 */
	public enum commands {
		/** arbitrary complex number*/
		arbcomplex(Operation.ARBCOMPLEX),
		/** arbitrary constant*/
		arbconst(Operation.ARBCONST),
		/** arbitrary integer (comes from trig equations)*/
		arbint(Operation.ARBINT),
		/** derivative*/
		df(Operation.DERIVATIVE),
		/** logb */
		logb(Operation.LOGB),
		/** sine integral */
		si(Operation.SI),
		/** cosine integral */
		ci(Operation.CI),
		/** cosine integral */
		ei(Operation.EI);
		private Operation op;
		private commands(Operation op){
			this.op = op;
		}
		/**
		 * @return single variable operation
		 */
		public Operation getOperation(){
			return op;
		}
	}

	/**
	 * @return The result of a special MPReduce command for the given argument
	 *         list as needed by the Parser. Returns null when nothing was done.
	 * 
	 * @param cmdName
	 *            name of the MPReduce command to process, see
	 *            CommandDispatcherMPReduce.commands
	 * @param args
	 *            list of command arguments
	 */
	public static ExpressionNode processCommand(String cmdName, MyList args) {

		try {
			ExpressionValue ret = null;
			Kernel kernel = args.getKernel();
			//TODO -- template is not important for arb*, but is this correct for df?
			StringTemplate tpl = StringTemplate.casTemplate;
			switch (commands.valueOf(cmdName)) {
			case logb:
				// e.g. logb[x,3] becomes log(3,x)
				ret = new ExpressionNode(kernel,
						 args.getListElement(1),Operation.LOGB,
								args.getListElement(0));
				break;
			case arbcomplex:
				

			case arbconst:
				

			case arbint:
				
			
				
			case ci:
				
			case si:
				
			case ei:
				// e.g. logb[x,3] becomes log(3,x)
				ret = new ExpressionNode(kernel,
						 args.getListElement(0),commands.valueOf(cmdName).getOperation(),
								null);
				break;

			case df:
				// e.g. df(f(var),var) from MPReduce becomes f'(var)
				// see http://www.geogebra.org/trac/ticket/1420
				String expStr = args.getListElement(0).toString(tpl);
				int nameEnd = expStr.indexOf('(');
				String funLabel = nameEnd > 0 ? expStr.substring(0, nameEnd)
						: expStr;

				// derivative of f gives f'
				ExpressionNode derivative = new ExpressionNode(kernel,
						new Variable(kernel, funLabel), // function label "f"
						Operation.DERIVATIVE, new MyDouble(kernel, 1));
				// function of given variable gives f'(t)
				ret = new ExpressionNode(kernel, derivative,
						Operation.FUNCTION, args.getListElement(1)); // Variable
																		// "t"
				break;
			}

			// no match or ExpressionNode
			if (ret == null || ret instanceof ExpressionNode) {
				return (ExpressionNode) ret;
			}
			// create ExpressionNode
			return new ExpressionNode(kernel, ret);
		} catch (IllegalArgumentException e) {
			// No enum const for cmdName
		} catch (Exception e) {
			System.err
					.println("CommandDispatcherMPReduce: error when processing command: "
							+ cmdName + ", " + args);
		}

		// exception
		return null;
	}

}
