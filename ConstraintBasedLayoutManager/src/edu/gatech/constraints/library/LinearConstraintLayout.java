package edu.gatech.constraints.library;

import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import edu.gatech.constraints.cassowary.CL;
import edu.gatech.constraints.cassowary.ClLinearEquation;
import edu.gatech.constraints.cassowary.ClLinearExpression;
import edu.gatech.constraints.cassowary.ClLinearInequality;
import edu.gatech.constraints.cassowary.ClPoint;
import edu.gatech.constraints.cassowary.ClSimplexSolver;
import edu.gatech.constraints.cassowary.ClStayConstraint;
import edu.gatech.constraints.cassowary.ClStrength;
import edu.gatech.constraints.cassowary.ClVariable;
import edu.gatech.constraints.cassowary.ExCLInternalError;
import edu.gatech.constraints.cassowary.ExCLNonlinearExpression;
import edu.gatech.constraints.cassowary.ExCLRequiredFailure;
import edu.gatech.constraints.demo.Functions;
import edu.gatech.constraints.demo.R;

/**
 * Class that implements the constraint based layout manager in Android
 * Reference: http://developer.android.com/reference/android/view/ViewGroup.html
 * 
 * 
 * @author anandsainath
 */
@SuppressLint("DrawAllocation")
public class LinearConstraintLayout extends LinearLayout {

	private ClSimplexSolver solver;
	SparseArray<ViewElement> elements;
	public static final String DEPENDENT_VAR = "@id/";
	private static final String[] operators = { "+", "-", "/", "*" };
	ClPoint screenDimensions;
	DisplayMetrics displayMetrics;

	public LinearConstraintLayout(Context context) {
		super(context);
		solver = new ClSimplexSolver();
		elements = new SparseArray<ViewElement>();
		displayMetrics = context.getResources().getDisplayMetrics();
		screenDimensions = new ClPoint(displayMetrics.widthPixels, displayMetrics.heightPixels);
	}

	public LinearConstraintLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		solver = new ClSimplexSolver();
		elements = new SparseArray<ViewElement>();
		displayMetrics = context.getResources().getDisplayMetrics();
		screenDimensions = new ClPoint(displayMetrics.widthPixels, displayMetrics.heightPixels);
	}

	public LinearConstraintLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		solver = new ClSimplexSolver();
		elements = new SparseArray<ViewElement>();
		displayMetrics = context.getResources().getDisplayMetrics();
		screenDimensions = new ClPoint(displayMetrics.widthPixels, displayMetrics.heightPixels);
	}

	/**
	 * Allows the children to measure themselves and then compute the measures
	 * of this view based on the children
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	public void resolveConstraints() {
		
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View view = getChildAt(i);
			ViewElement element = new ViewElement(view);
			elements.append(view.getId(), element);
		}

		try {
			/**
			 * Separate loop for adding constraints as view elements might be
			 * dependent on some other view element that occurs lower than it in
			 * the view hierarchy
			 * 
			 * Solver will automatically take the constraints as and when they
			 * are added to the solver
			 * 
			 * Stay constraints need to be added first, followed by normal
			 * constraints
			 */
			for (int pos = 0; pos < elements.size(); pos++) {
				addStayConstraints(elements.get(elements.keyAt(pos)), pos);
			}

			for (int pos = 0; pos < elements.size(); pos++) {
				addConstraints(elements.get(elements.keyAt(pos)), pos);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Functions.d("Error occured" + e.getMessage());
			e.printStackTrace();
		}

//		Functions.d("After solving");
//		Functions.d(elements.size() + " are the number of elements!");
		for (int i = 0; i < elements.size(); i++) {
			ViewElement element = elements.get(elements.keyAt(i));
			element.setDimension();
		}
	}

	/**
	 * Function that will solve the constraints and will layout the children
	 * once the constraints are met.
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		Log.d("Output", "" + changed);

		resolveConstraints();
		// ViewElement firstElement = elements.get(elements.keyAt(0));
		// ViewElement lastElement = elements.get(elements.keyAt(elements.size()
		// - 1));
		// float measuredHeight = (lastElement.view.getHeight() +
		// lastElement.view.getY()) - firstElement.view.getY();
		// Functions.d("Height is: " + measuredHeight);
		// Functions.d("Parent Height is: " + getHeight());
	}

	private void addStayConstraints(ViewElement element, int position) throws ExCLRequiredFailure, ExCLInternalError {
		ClStayConstraint stayConstraint;
		LinearConstraintLayout.LayoutParams params = (LinearConstraintLayout.LayoutParams) element.view
				.getLayoutParams();

		if (params.fixWidth) {
			stayConstraint = new ClStayConstraint(element.dimension.Width(), ClStrength.strong);
			solver.addConstraint(stayConstraint);
		}
		if (params.fixHeight) {
			stayConstraint = new ClStayConstraint(element.dimension.Height(), ClStrength.strong);
			solver.addConstraint(stayConstraint);
		}

		if (params.fixX) {
			stayConstraint = new ClStayConstraint(element.topLeft.X(), ClStrength.strong);
			solver.addConstraint(stayConstraint);
		}

		if (params.fixY) {
			stayConstraint = new ClStayConstraint(element.topLeft.Y(), ClStrength.strong);
			solver.addConstraint(stayConstraint);
		}
	}

	public void addConstraints(ViewElement element, int position) throws Exception {
		LinearConstraintLayout.LayoutParams params = (LinearConstraintLayout.LayoutParams) element.view
				.getLayoutParams();

		if (params.right_padding != null) {
			params.constraint_expr += " ; self.x = screen.width - self.w - " + params.right_padding + " - "
					+ params.rightMargin;
		}
		if (params.left_padding != null) {
			params.constraint_expr += " ; self.x = " + params.left_padding + " + " + params.leftMargin;
		}
		if (params.top_padding != null) {
			params.constraint_expr += " ; self.y = " + params.top_padding + " + " + params.topMargin;
		}
		if (params.bottom_padding != null) {
			params.constraint_expr += " ; self.y = screen.height - self.h - " + params.bottom_padding + " - "
					+ params.bottomMargin;
		}

		if (params != null) {
			if (params.constraint_expr == null) {
				// Functions.d("constraint_expr is null");
				/**
				 * Add constraint so that the linear layout's properties are
				 * followed in the new layout as well
				 */
				switch (position) {
				case 0:
					/**
					 * Element is the first one in its hierarchy and would be
					 * placed properly.
					 */
					break;
				default:
					/**
					 * Any other case, check if any of its parents have a
					 * constraint specified
					 */
					ViewElement aboveSibbling = elements.get(elements.keyAt(position - 1));
					LinearConstraintLayout.LayoutParams aboveSibblingParams = (LinearConstraintLayout.LayoutParams) aboveSibbling.view
							.getLayoutParams();
					if (aboveSibblingParams.constraint_expr == null) {
						/**
						 * nothing to be done here, as the parent linear layout
						 * manager will take care of its alignment
						 */
					} else {
						/**
						 * add a constraint based on whether the linear layout
						 * is oriented horizontally or vertically
						 */
						// Functions.d("Position: " + position);
						String[] resName = getResources().getResourceName(aboveSibbling.view.getId()).split(":");
						String resId = "@" + resName[1].trim();
						// Functions.d("Going to create constraints based on : "
						// + resId);
						switch (getOrientation()) {
						case HORIZONTAL:
							params.constraint_expr = "self.x = " + resId + ".x + " + resId + ".w + "
									+ params.leftMargin;
							params.constraint_expr_strength = ClStrength.strong;
							break;
						case VERTICAL:
							params.constraint_expr = "self.y = " + resId + ".y + " + resId + ".h + " + params.topMargin;
							params.constraint_expr_strength = ClStrength.strong;
							break;
						}
					}
				}
			}

			if (params.constraint_expr != null && !params.constraint_expr.equals("no_constraint")) {
				String[] constraints = params.constraint_expr.split(";");
				for (String constraint : constraints) {
					if (constraint.contains("LEQ")) {
						solver.addConstraint(getInequalityConstraint(CL.Op.LEQ, element, constraint));
						// Functions.d("A LEQ constraint must have been added!");
					} else if (constraint.contains("GEQ")) {
						solver.addConstraint(getInequalityConstraint(CL.Op.GEQ, element, constraint));
						// Functions.d("A GEQ constraint must have been added!");
					} else if (constraint.contains("=")) {
						// equality constraint.
						solver.addConstraint(addEqualityConstraint(params, element, constraint));
						// Functions.d("A constraint must have been added!");
					}
				}
			}

		}// end params != null
	}

	private ClLinearInequality getInequalityConstraint(CL.Op operator, ViewElement source, String constraint)
			throws ExCLInternalError, ExCLNonlinearExpression {
		ClLinearExpression cle = null;
		String parts[] = null;
		switch (operator) {
		case GEQ:
			parts = constraint.split("GEQ", 2);
			break;
		case LEQ:
			parts = constraint.split("LEQ", 2);
			break;
		}
		cle = (ClLinearExpression) evaluatePostFixExpression(new InfixToPostfix().convertInfixToPostfix(parts[1]),
				source);
		if (parts[0].contains(".w")) {
			source.setWidthConstraint();
		} else if (parts[0].contains(".h")) {
			source.setHeightConstraint();
		}
		ClVariable lhs = getVariable(parts[0], source);
		return new ClLinearInequality(lhs, operator, cle);
	}

	private ClLinearEquation addEqualityConstraint(LinearConstraintLayout.LayoutParams params, ViewElement source,
			String constraint) throws Exception {
		String parts[] = constraint.split("=", 2);
		ClLinearExpression cle = null;
		cle = (ClLinearExpression) evaluatePostFixExpression(new InfixToPostfix().convertInfixToPostfix(parts[1]),
				source);
		// Functions.d("The equation is " + cle.toString());
		// Functions.d("Going to call getVariable for the LHS in addEqualityConstraint");
		if (parts[0].contains(".w")) {
			source.setWidthConstraint();
		} else if (parts[0].contains(".h")) {
			source.setHeightConstraint();
		}
		ClVariable lhs = getVariable(parts[0], source);
		return new ClLinearEquation(lhs, cle, params.constraint_expr_strength);
	}

	private CL evaluatePostFixExpression(List<String> postFixExpr, ViewElement source) throws ExCLNonlinearExpression {
		Iterator<String> iterator = postFixExpr.iterator();
		MyStackList<ClLinearExpression> stack = new MyStackList<ClLinearExpression>();

		while (iterator.hasNext()) {
			String str = iterator.next();
			if (contains(operators, str)) {
				// operator
				ClLinearExpression a = stack.pop();
				ClLinearExpression b = stack.pop();
				if (str.equals("+")) {
					b = b.plus(a);
				} else if (str.equals("-")) {
					b = b.minus(a);
				} else if (str.equals("/")) {
					b = b.divide(a);
				} else if (str.equals("*")) {
					b = b.times(a);
				}
				stack.push(b);
			} else {
				try {
					float constant = pixelValue(str);
					stack.add(new ClLinearExpression(constant));
				} catch (NumberFormatException nfe) {
					if (str.contains("screen")) {
						if (str.contains("screen.height")) {
							stack.add(new ClLinearExpression(this.screenDimensions.Yvalue()));
						} else if (str.contains("screen.width")) {
							stack.add(new ClLinearExpression(this.screenDimensions.Xvalue()));
						}
					} else {
						// Functions.d("Added new variable");
						stack.add(new ClLinearExpression(getVariable(str, source)));
					}
				}
			}
		}
		return stack.pop();
	}

	private ClVariable getVariable(String notation, ViewElement source) {
		ViewElement temp = null;
		ClVariable variable = null;
		// Functions.d("GetVariable called, Notation:" + notation +
		// ", source:");
		if (notation.contains(DEPENDENT_VAR)) {
			String dependent_name = notation.replace(DEPENDENT_VAR, "");
			String names[] = dependent_name.split("[.]", 2);
			int resId = getResources().getIdentifier(names[0], "id", getContext().getPackageName());
			temp = elements.get(resId);
		} else if (notation.contains("self")) {
			temp = source;
		} else if (notation.length() > 0) {
			// Functions.d(notation + " is being parsed as an integer!");
			return new ClVariable(Integer.parseInt(notation));
		}

		if (notation.contains(".w")) {
			// Functions.d("Width procesed: " + temp.dimension.widthValue() +
			// " notation: " + notation);
			variable = temp.dimension.Width();
		} else if (notation.contains(".h")) {
			variable = temp.dimension.Height();
		} else if (notation.contains(".x")) {
			variable = temp.topLeft.X();
		} else if (notation.contains(".y")) {
			variable = temp.topLeft.Y();
		}
		return variable;
	}

	private boolean contains(String[] haystack, String needle) {
		boolean contains = false;
		for (String item : haystack) {
			if (needle.equalsIgnoreCase(item)) {
				contains = true;
				break; // No need to look further.
			}
		}
		return contains;
	}

	private float pixelValue(String pixelString) {
		if (pixelString.matches("(\\d+)dp")) {
			float dip = Float.parseFloat(pixelString.replaceAll("(\\d+)dp", "$1"));
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, displayMetrics);
			// Functions.d("Dip after conversion is " + constant);
		} else {
			return Float.parseFloat(pixelString);
		}
	}

	/**
	 * Any layout manager that doesn't scroll will want this.
	 */
	@Override
	public boolean shouldDelayChildPressedState() {
		return false;
	}

	/***
	 * Function that are required when there are attributes specified using the
	 * XML file
	 ***/
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LinearConstraintLayout.LayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}

	@Override
	protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	/*** Custom Layout parameters class ***/
	public static class LayoutParams extends LinearLayout.LayoutParams {

		public String constraint_expr;
		public ClStrength constraint_expr_strength = ClStrength.weak;
		public Boolean fixWidth, fixHeight, fixX, fixY;
		public String left_padding, right_padding, top_padding, bottom_padding;

		public static final int INVALID_ID = -1;

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);

			/**
			 * Fetch the values of the various attributes from the XML file
			 */
			TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CLP);
			constraint_expr = a.getString(R.styleable.CLP_constraint_expr);
			fixHeight = a.getBoolean(R.styleable.CLP_fixHeight, true);
			fixWidth = a.getBoolean(R.styleable.CLP_fixWidth, true);
			fixX = a.getBoolean(R.styleable.CLP_fixX, false);
			fixY = a.getBoolean(R.styleable.CLP_fixY, false);
			left_padding = a.getString(R.styleable.CLP_left_padding);
			right_padding = a.getString(R.styleable.CLP_right_padding);
			top_padding = a.getString(R.styleable.CLP_top_padding);
			bottom_padding = a.getString(R.styleable.CLP_bottom_padding);

			switch (a.getInt(R.styleable.CLP_constraint_expr_strength, 4)) {
			case 1:
				constraint_expr_strength = ClStrength.required;
				break;
			case 2:
				constraint_expr_strength = ClStrength.strong;
				break;
			case 3:
				constraint_expr_strength = ClStrength.medium;
				break;
			case 4:
				constraint_expr_strength = ClStrength.weak;
				break;
			}
			a.recycle();
		}

		public LayoutParams(int width, int height) {
			super(width, height);
			if (fixWidth == null) {
				fixWidth = true;
			}
			if (fixHeight == null) {
				fixHeight = true;
			}
			if (fixX == null) {
				fixX = false;
			}
			if (fixY == null) {
				fixY = false;
			}

		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

	}
}
