package org.arend.core.expr.visitor;

import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.binding.inference.MetaInferenceVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.*;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.TestOnly;

import java.math.BigInteger;
import java.util.*;

import static org.arend.core.expr.ExpressionFactory.Fin;
import static org.arend.core.expr.ExpressionFactory.Suc;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class CompareVisitor implements ExpressionVisitor2<Expression, Expression, Boolean> {
  private final Map<Binding, Binding> mySubstitution;
  private final Equations myEquations;
  private final Concrete.SourceNode mySourceNode;
  private CMP myCMP;
  private boolean myNormalCompare = true;
  private boolean myOnlySolveVars = false;
  private boolean myAllowEquations = true;
  private boolean myNormalize = true;
  private Result myResult;

  public CompareVisitor(Equations equations, CMP cmp, Concrete.SourceNode sourceNode) {
    mySubstitution = new HashMap<>();
    myEquations = equations;
    mySourceNode = sourceNode;
    myCMP = cmp;
  }

  public static class Result {
    public Expression wholeExpr1;
    public Expression wholeExpr2;
    public Expression subExpr1;
    public Expression subExpr2;
    public Levels levels1;
    public Levels levels2;

    private int index = -1;

    public Result(Expression wholeExpr1, Expression wholeExpr2, Expression subExpr1, Expression subExpr2, Levels levels1, Levels levels2) {
      this.wholeExpr1 = wholeExpr1;
      this.wholeExpr2 = wholeExpr2;
      this.subExpr1 = subExpr1;
      this.subExpr2 = subExpr2;
      this.levels1 = levels1;
      this.levels2 = levels2;
    }

    public Result(Expression wholeExpr1, Expression wholeExpr2, Expression subExpr1, Expression subExpr2) {
      this(wholeExpr1, wholeExpr2, subExpr1, subExpr2, null, null);
    }
  }

  public Result getResult() {
    return myResult;
  }

  public void setCMP(CMP cmp) {
    myCMP = cmp;
  }

  public void doNotAllowEquations() {
    myAllowEquations = false;
  }

  public void doNotNormalize() {
    myNormalize = false;
  }

  public static boolean compare(Equations equations, CMP cmp, Expression expr1, Expression expr2, Expression type, Concrete.SourceNode sourceNode) {
    return new CompareVisitor(equations, cmp, sourceNode).compare(expr1, expr2, type, true);
  }

  public boolean compare(ElimTree elimTree1, ElimTree elimTree2) {
    if (myCMP == CMP.GE) {
      myCMP = CMP.LE;
      boolean result = compare(elimTree2, elimTree1);
      myCMP = CMP.GE;
      return result;
    }
    if (elimTree1.getSkip() != elimTree2.getSkip()) {
      return false;
    }

    if (elimTree1 instanceof LeafElimTree && elimTree2 instanceof LeafElimTree) {
      return ((LeafElimTree) elimTree1).getClauseIndex() == ((LeafElimTree) elimTree2).getClauseIndex() && Objects.equals(((LeafElimTree) elimTree1).getArgumentIndices(), ((LeafElimTree) elimTree2).getArgumentIndices());
    } else if (elimTree1 instanceof BranchElimTree branchElimTree1 && elimTree2 instanceof BranchElimTree branchElimTree2) {
      if (branchElimTree1.keepConCall() != branchElimTree2.keepConCall() || branchElimTree1.getChildren().size() > branchElimTree2.getChildren().size() || myCMP == CMP.EQ && branchElimTree1.getChildren().size() != branchElimTree2.getChildren().size() ) {
        return false;
      }
      SingleConstructor single1 = branchElimTree1.getSingleConstructorKey();
      if (single1 != null) {
        SingleConstructor single2 = branchElimTree2.getSingleConstructorKey();
        return single2 != null && single1.compare(single2, myEquations, mySourceNode) && compare(branchElimTree1.getSingleConstructorChild(), branchElimTree2.getSingleConstructorChild());
      } else {
        for (Map.Entry<BranchKey, ElimTree> entry : branchElimTree1.getChildren()) {
          ElimTree subTree = branchElimTree2.getChild(entry.getKey());
          if (subTree == null || !compare(entry.getValue(), subTree)) {
            return false;
          }
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @TestOnly
  public static boolean compare(Equations equations, ElimBody elimBody1, ElimBody elimBody2, Concrete.SourceNode sourceNode) {
    return new CompareVisitor(equations, CMP.EQ, sourceNode).compare(elimBody1, elimBody2, null);
  }

  private Boolean compare(ElimBody elimBody1, ElimBody elimBody2, Expression type) {
    if (elimBody1 == elimBody2) {
      return true;
    }
    if (elimBody1.getClauses().size() != elimBody2.getClauses().size()) {
      return false;
    }
    for (int i = 0; i < elimBody1.getClauses().size(); i++) {
      ElimClause<Pattern> clause1 = elimBody1.getClauses().get(i);
      ElimClause<Pattern> clause2 = elimBody2.getClauses().get(i);
      if (!compareParameters(clause1.getParameters(), clause2.getParameters())) {
        return false;
      }
      boolean ok = clause1.getExpression() == null && clause2.getExpression() == null || clause1.getExpression() != null && clause2.getExpression() != null && compare(clause1.getExpression(), clause2.getExpression(), type, true);
      for (DependentLink link = clause2.getParameters(); link.hasNext(); link = link.getNext()) {
        mySubstitution.remove(link);
      }
      if (!ok) {
        return false;
      }
    }

    return true;
  }

  public boolean nonNormalizingCompare(Expression expr1, Expression expr2, Expression type) {
    expr1 = expr1.getUnderlyingExpression();
    expr2 = expr2.getUnderlyingExpression();

    // Optimization for let clause calls
    if (expr1 instanceof ReferenceExpression && expr2 instanceof ReferenceExpression && ((ReferenceExpression) expr1).getBinding() == ((ReferenceExpression) expr2).getBinding()) {
      return true;
    }

    // Another optimization
    boolean check = !myNormalCompare || !myNormalize;
    if (expr1 instanceof FunCallExpression) {
      check = expr2 instanceof FunCallExpression && ((FunCallExpression) expr1).getDefinition() == ((FunCallExpression) expr2).getDefinition() && (check || !((FunCallExpression) expr1).getDefinition().isSFunc());
    } else if (expr1 instanceof AppExpression) {
      check = expr2 instanceof AppExpression;
    } else if (expr1 instanceof FieldCallExpression) {
      check = expr2 instanceof FieldCallExpression && ((FieldCallExpression) expr1).getDefinition() == ((FieldCallExpression) expr2).getDefinition() && (check || !((FieldCallExpression) expr1).getDefinition().isProperty());
    } else if (expr1 instanceof ProjExpression) {
      check = expr2 instanceof ProjExpression && ((ProjExpression) expr1).getField() == ((ProjExpression) expr2).getField();
    } else if (expr1 instanceof TypeDestructorExpression) {
      check = expr2 instanceof TypeDestructorExpression;
    }

    if (check) {
      CMP origCMP = myCMP;
      myCMP = CMP.EQ;
      boolean normalCompare = myNormalCompare;
      myNormalCompare = false;

      boolean ok = expr1.accept(this, expr2, type);

      myNormalCompare = normalCompare;
      myCMP = origCMP;
      return ok;
    }

    return false;
  }

  private boolean initResult(Expression expr1, Expression expr2) {
    if (myNormalCompare && myResult == null) {
      myResult = new Result(expr1, expr2, expr1, expr2);
    }
    return myResult != null;
  }

  private boolean initResult(Expression expr1, Expression expr2, boolean correctOrder) {
    return initResult(correctOrder ? expr1 : expr2, correctOrder ? expr2 : expr1);
  }

  private void initResult(Expression expr1, Expression expr2, Levels levels1, Levels levels2) {
    if (myNormalCompare && myResult == null) {
      myResult = new Result(expr1, expr2, null, null, levels1, levels2);
    }
  }

  public boolean normalizedCompare(Expression expr1, Expression expr2, Expression type, boolean useType) {
    Expression stuck1 = expr1.getStuckExpression();
    Expression stuck2 = expr2.getStuckExpression();
    if (stuck1 != null) stuck1 = stuck1.getUnderlyingExpression();
    if (stuck2 != null) stuck2 = stuck2.getUnderlyingExpression();
    if (stuck1 instanceof ErrorExpression && !(stuck2 instanceof InferenceReferenceExpression) ||
        stuck2 instanceof ErrorExpression && !(stuck1 instanceof InferenceReferenceExpression)) {
      return true;
    }

    if ((stuck1 instanceof InferenceReferenceExpression || stuck2 instanceof InferenceReferenceExpression) && myAllowEquations) {
      InferenceVariable var1 = stuck1 instanceof InferenceReferenceExpression ? ((InferenceReferenceExpression) stuck1).getVariable() : null;
      InferenceVariable var2 = stuck2 instanceof InferenceReferenceExpression ? ((InferenceReferenceExpression) stuck2).getVariable() : null;
      if (var1 instanceof MetaInferenceVariable || var2 instanceof MetaInferenceVariable || var1 != null && var2 != null && !(expr1 instanceof DefCallExpression && !(expr1 instanceof FieldCallExpression) && expr2 instanceof DefCallExpression && ((DefCallExpression) expr1).getDefinition() == ((DefCallExpression) expr2).getDefinition())) {
        if (!myEquations.addEquation(expr1, substitute(expr2), type, myCMP, (var1 != null ? var1 : var2).getSourceNode(), var1, var2)) {
          initResult(expr1, expr2);
          return false;
        }
        return true;
      }
    }

    InferenceVariable stuckVar1 = expr1.getInferenceVariable();
    InferenceVariable stuckVar2 = expr2.getInferenceVariable();
    if (stuckVar1 != null || stuckVar2 != null) {
      if (!(myNormalCompare && myEquations.addEquation(expr1, substitute(expr2), type, myCMP, stuckVar1 != null ? stuckVar1.getSourceNode() : stuckVar2.getSourceNode(), stuckVar1, stuckVar2))) {
        initResult(expr1, expr2);
        return false;
      }
      return true;
    }

    if (expr1 instanceof FieldCallExpression && ((FieldCallExpression) expr1).getDefinition().isProperty() && expr2 instanceof FieldCallExpression && ((FieldCallExpression) expr2).getDefinition().isProperty()) {
      return true;
    }

    boolean onlySolveVars = myOnlySolveVars;
    if (myNormalCompare && !myOnlySolveVars && expr1.isBoxed() && expr2.isBoxed()) {
      myOnlySolveVars = true;
    }
    if (useType && myNormalCompare && !myOnlySolveVars) {
      Expression normType = type == null ? null : type.getUnderlyingExpression();
      boolean allowProp = normType instanceof DataCallExpression && ((DataCallExpression) normType).getDefinition().getConstructors().isEmpty() || !expr1.canBeConstructor() && !expr2.canBeConstructor();
      if (normType instanceof SigmaExpression && !((SigmaExpression) normType).getParameters().hasNext() ||
          normType instanceof ClassCallExpression && (((ClassCallExpression) normType).getNumberOfNotImplementedFields() == 0 || Boolean.TRUE.equals(ConstructorExpressionPattern.isArrayEmpty(normType)) && ((ClassCallExpression) normType).isImplemented(Prelude.ARRAY_ELEMENTS_TYPE)) ||
          allowProp && normType != null && Sort.PROP.equals(normType.getSortOfType())) {
        myOnlySolveVars = true;
      }

      if (!myOnlySolveVars && (normType == null || normType.getStuckInferenceVariable() != null || normType instanceof ClassCallExpression)) {
        Expression type1 = expr1.getType();
        if (type1 != null && type1.getStuckInferenceVariable() != null) {
          type1 = null;
        }
        if (type1 != null) {
          type1 = myNormalize ? type1.normalize(NormalizationMode.WHNF) : type1;
          if (allowProp) {
            Sort sort1 = type1.getSortOfType();
            if (sort1 != null && sort1.isProp() && !type1.isInstance(ClassCallExpression.class)) {
              myOnlySolveVars = true;
            }
          }
        }

        if (!myOnlySolveVars && type1 != null) {
          Expression type2 = expr2.getType();
          if (type2 != null && type2.getStuckInferenceVariable() != null) {
            type2 = null;
          }
          if (type2 != null) {
            type2 = myNormalize ? type2.normalize(NormalizationMode.WHNF) : type2;
            if (allowProp) {
              Sort sort2 = type2.getSortOfType();
              if (sort2 != null && sort2.isProp() && !type2.isInstance(ClassCallExpression.class)) {
                myOnlySolveVars = true;
              }
            }
          }

          if (!myOnlySolveVars && type2 != null) {
            ClassCallExpression classCall1 = type1.cast(ClassCallExpression.class);
            ClassCallExpression classCall2 = type2.cast(ClassCallExpression.class);
            if (classCall1 != null && classCall2 != null && compareClassInstances(expr1, classCall1, expr2, classCall2, normType)) {
              return true;
            }
          }
        }
      }
    }

    CMP origCMP = myCMP;
    if (!myOnlySolveVars && myAllowEquations) {
      Boolean dataAndApp = checkDefCallAndApp(expr1, expr2, true);
      if (dataAndApp != null) {
        if (!dataAndApp) initResult(expr1, expr2);
        return dataAndApp;
      }
      dataAndApp = checkDefCallAndApp(expr2, expr1, false);
      if (dataAndApp != null) {
        if (!dataAndApp) initResult(expr1, expr2);
        return dataAndApp;
      }
    }

    if (!myOnlySolveVars && myNormalCompare) {
      Boolean result = expr1 instanceof AppExpression && (stuck2 == null || stuck2.getInferenceVariable() == null) ? checkApp((AppExpression) expr1, expr2, true) : null;
      if (result != null) {
        if (!result) initResult(expr1, expr2);
        return result;
      }
      result = expr2 instanceof AppExpression && (stuck1 == null || stuck1.getInferenceVariable() == null) ? checkApp((AppExpression) expr2, expr1, false) : null;
      if (result != null) {
        if (!result) initResult(expr1, expr2);
        return result;
      }
    }

    if (expr1 instanceof ErrorExpression) {
      return true;
    }
    if (!(expr1 instanceof UniverseExpression || expr1 instanceof PiExpression || expr1 instanceof ClassCallExpression || expr1 instanceof DataCallExpression || expr1 instanceof AppExpression || expr1 instanceof SigmaExpression || expr1 instanceof LamExpression)) {
      myCMP = CMP.EQ;
    }

    boolean ok;
    if (expr2 instanceof ErrorExpression) {
      return true;
    }
    if (expr2 instanceof PathExpression && !(expr1 instanceof PathExpression)) {
      ok = comparePathEta((PathExpression) expr2, expr1, type, false);
    } else if (expr2 instanceof LamExpression) {
      ok = visitLam((LamExpression) expr2, expr1, type, false);
    } else if (expr2 instanceof TupleExpression) {
      ok = visitTuple((TupleExpression) expr2, expr1, false);
    } else if (expr2 instanceof TypeConstructorExpression) {
      ok = visitTypeConstructor((TypeConstructorExpression) expr2, expr1, false);
    } else if (expr2 instanceof FunCallExpression && ((FunCallExpression) expr2).getDefinition() == Prelude.ARRAY_INDEX && !(expr1 instanceof FunCallExpression && ((FunCallExpression) expr1).getDefinition() == Prelude.ARRAY_INDEX)) {
      ok = compareConstArray((FunCallExpression) expr2, expr1, type, false);
    } else {
      ok = expr1.accept(this, expr2, type);
    }

    if (!ok && !myOnlySolveVars && myAllowEquations) {
      InferenceVariable variable1 = stuck1 == null ? null : stuck1.getInferenceVariable();
      InferenceVariable variable2 = stuck2 == null ? null : stuck2.getInferenceVariable();
      ok = (variable1 != null || variable2 != null) && myNormalCompare && myEquations.addEquation(expr1, substitute(expr2), type, origCMP, variable1 != null ? variable1.getSourceNode() : variable2.getSourceNode(), variable1, variable2);
    }
    if (myOnlySolveVars) {
      ok = true;
    }
    myOnlySolveVars = onlySolveVars;
    return ok;
  }

  private static boolean isInstance(FieldCallExpression fieldCall) {
    FunCallExpression funCall = fieldCall.getArgument().cast(FunCallExpression.class);
    return funCall != null && funCall.getDefinition().getBody() == null && funCall.getDefinition().getResultType() instanceof ClassCallExpression && ((ClassCallExpression) funCall.getDefinition().getResultType()).isImplemented(fieldCall.getDefinition());
  }

  private Boolean compareImmediately(Expression expr1, Expression expr2, Expression type) {
    int n1 = 0;
    Expression e1 = expr1;
    while (e1 instanceof AppExpression) {
      e1 = e1.getFunction().getUnderlyingExpression();
      n1++;
    }
    int n2 = 0;
    Expression e2 = expr2;
    while (e2 instanceof AppExpression) {
      e2 = e2.getFunction().getUnderlyingExpression();
      n2++;
    }
    if (!(n1 == n2 && e1 instanceof FieldCallExpression fieldCall1 && e2 instanceof FieldCallExpression fieldCall2)) return null;
    if (fieldCall1.getDefinition() == fieldCall2.getDefinition() && (fieldCall1.getArgument().getInferenceVariable() != null && isInstance(fieldCall2) || fieldCall2.getArgument().getInferenceVariable() != null && isInstance(fieldCall1))) {
      if (!e1.accept(this, e2, type)) {
        return false;
      }
      e1 = expr1;
      e2 = expr2;
      while (e1 instanceof AppExpression && e2 instanceof AppExpression) {
        if (!compare(((AppExpression) e1).getArgument(), ((AppExpression) e2).getArgument(), null, true)) {
          return false;
        }
        e1 = e1.getFunction().getUnderlyingExpression();
        e2 = e2.getFunction().getUnderlyingExpression();
      }
      return true;
    }
    else return null;
  }

  public Boolean compare(Expression expr1, Expression expr2, Expression type, boolean useType) {
    expr1 = expr1.getUnderlyingExpression();
    expr2 = expr2.getUnderlyingExpression();
    if (expr1 == expr2) {
      return true;
    }

    InferenceReferenceExpression infRefExpr1 = expr1.cast(InferenceReferenceExpression.class);
    InferenceReferenceExpression infRefExpr2 = expr2.cast(InferenceReferenceExpression.class);
    if (infRefExpr1 != null && infRefExpr2 != null && infRefExpr1.getVariable() == infRefExpr2.getVariable()) {
      return true;
    }
    if (infRefExpr1 != null && infRefExpr1.getVariable() != null) {
      return myNormalCompare && myEquations.addEquation(infRefExpr1, substitute(expr2), type, myCMP, infRefExpr1.getVariable().getSourceNode(), infRefExpr1.getVariable(), expr2.getStuckInferenceVariable());
    }
    if (infRefExpr2 != null && infRefExpr2.getVariable() != null) {
      return myNormalCompare && myEquations.addEquation(expr1, infRefExpr2, type, myCMP, infRefExpr2.getVariable().getSourceNode(), expr1.getStuckInferenceVariable(), infRefExpr2.getVariable());
    }

    InferenceVariable stuckVar1 = expr1.getStuckInferenceVariable();
    InferenceVariable stuckVar2 = expr2.getStuckInferenceVariable();
    if (stuckVar1 != stuckVar2 && (!myNormalCompare || myEquations == DummyEquations.getInstance())) {
      return myOnlySolveVars;
    }
    Boolean result = compareImmediately(expr1, expr2, type);
    if (result != null) {
      return result;
    }
    if (stuckVar1 == stuckVar2 && nonNormalizingCompare(expr1, expr2, type)) {
      return true;
    }

    if (myOnlySolveVars && (stuckVar1 != null || stuckVar2 != null)) {
      return true;
    }

    if (!myNormalCompare || !myNormalize) {
      initResult(expr1, expr2);
      return false;
    }

    if (stuckVar1 != null && stuckVar2 != null && myAllowEquations) {
      if (!myEquations.addEquation(expr1, substitute(expr2), type, myCMP, stuckVar1.getSourceNode(), stuckVar1, stuckVar2)) {
        initResult(expr1, expr2);
        return false;
      }
      return true;
    }

    return normalizedCompare(expr1.normalize(NormalizationMode.WHNF), expr2.normalize(NormalizationMode.WHNF), type == null ? null : type.normalize(NormalizationMode.WHNF), useType);
  }

  private Expression substitute(Expression expr) {
    return expr.accept(new SubstVisitor(getSubstitution(true), LevelSubstitution.EMPTY, false), null);
  }

  private ExprSubstitution getSubstitution(boolean correctOrder) {
    ExprSubstitution substitution = new ExprSubstitution();
    for (Map.Entry<Binding, Binding> entry : mySubstitution.entrySet()) {
      substitution.add(correctOrder ? entry.getKey() : entry.getValue(), new ReferenceExpression(correctOrder ? entry.getValue() : entry.getKey()));
    }
    return substitution;
  }

  private List<Pair<Expression,Boolean>> getArguments(Expression expr) {
    List<Pair<Expression,Boolean>> result = new ArrayList<>();
    AppExpression app = expr.cast(AppExpression.class);
    Expression fun;
    for (; app != null; app = fun.cast(AppExpression.class)) {
      result.add(new Pair<>(app.getArgument(), app.isExplicit()));
      fun = app.getFunction();
    }
    Collections.reverse(result);
    return result;
  }

  @Override
  public Boolean visitApp(AppExpression expr1, Expression expr2, Expression type) {
    List<Expression> args1 = new ArrayList<>();
    List<Expression> args2 = new ArrayList<>();
    Expression fun1 = expr1.getArguments(args1);
    Expression fun2 = expr2.getArguments(args2);

    InferenceVariable var1 = fun1.getInferenceVariable();
    InferenceVariable var2 = fun2.getInferenceVariable();
    if (var1 != null || var2 != null) {
      if (myNormalCompare && !myOnlySolveVars && myEquations.addEquation(expr1, substitute(expr2), type, myCMP, var1 != null ? var1.getSourceNode() : var2.getSourceNode(), var1, var2)) {
        return true;
      }
    }

    if (args1.size() != args2.size()) {
      initResult(expr1, expr2);
      return false;
    }

    if (!compare(fun1, fun2, null, false)) {
      if (initResult(fun1, fun2)) {
        List<Pair<Expression,Boolean>> argsExp1 = getArguments(expr1);
        for (Pair<Expression, Boolean> arg : argsExp1) {
          myResult.wholeExpr1 = AppExpression.make(myResult.wholeExpr1, arg.proj1, arg.proj2);
        }
        List<Pair<Expression,Boolean>> argsExp2 = getArguments(expr2);
        for (Pair<Expression, Boolean> arg : argsExp2) {
          myResult.wholeExpr2 = AppExpression.make(myResult.wholeExpr2, arg.proj1, arg.proj2);
        }
      }
      return false;
    }
    if (args1.isEmpty()) {
      return true;
    }

    myCMP = CMP.EQ;
    Expression type1 = fun1.getType();
    List<SingleDependentLink> params = Collections.emptyList();
    if (type1 != null) {
      params = new ArrayList<>();
      type1.getPiParameters(params, false);
    }

    for (int i = 0; i < args1.size(); i++) {
      if (!compare(args1.get(i), args2.get(i), i < params.size() ? params.get(i).getTypeExpr() : null, true)) {
        if (initResult(args1.get(i), args2.get(i))) {
          List<Pair<Expression, Boolean>> argsExp1 = getArguments(expr1);
          for (int j = 0; j < argsExp1.size(); j++) {
            fun1 = i == j ? myResult.wholeExpr1 : AppExpression.make(fun1, argsExp1.get(j).proj1, argsExp1.get(j).proj2);
          }
          myResult.wholeExpr1 = fun1;
          List<Pair<Expression, Boolean>> argsExp2 = getArguments(expr2);
          for (int j = 0; j < argsExp2.size(); j++) {
            fun2 = i == j ? myResult.wholeExpr2 : AppExpression.make(fun2, argsExp2.get(j).proj1, argsExp2.get(j).proj2);
          }
          myResult.wholeExpr2 = fun2;
        }
        return false;
      }
    }

    return true;
  }

  private Boolean comparePathEta(PathExpression pathExpr1, Expression expr2, Expression type, boolean correctOrder) {
    SingleDependentLink param = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
    ReferenceExpression paramRef = new ReferenceExpression(param);
    Expression argumentType = pathExpr1.getArgumentType();
    Sort sort = new Sort(pathExpr1.getLevels().toLevelPair().get(LevelVariable.PVAR), Level.INFINITY);
    LamExpression lamExpr = new LamExpression(sort, param, AtExpression.make(expr2, paramRef, false));
    Expression argType = new PiExpression(sort, param, AppExpression.make(argumentType, paramRef, true));
    if (!(correctOrder ? compare(pathExpr1.getArgument(), lamExpr, argType, true) : compare(lamExpr, pathExpr1.getArgument(), argType, true))) {
      initResult(pathExpr1, expr2, correctOrder);
      return false;
    }
    return true;
  }

  private boolean compareDef(LeveledDefCallExpression expr1, LeveledDefCallExpression expr2, Expression origExpr2) {
    if (expr2 == null || expr1.getDefinition() != expr2.getDefinition()) {
      initResult(expr1, origExpr2);
      return false;
    }
    UniverseKind universeKind = expr1.getUniverseKind();
    if (universeKind == UniverseKind.NO_UNIVERSES) {
      return true;
    }
    CMP cmp = universeKind == UniverseKind.ONLY_COVARIANT ? myCMP : CMP.EQ;
    if (!expr1.getLevels().compare(expr2.getLevels(), cmp, myNormalCompare ? myEquations : DummyEquations.getInstance(), mySourceNode)) {
      initResult(expr1, expr2, expr1.getLevels(), expr2.getLevels());
      return false;
    }
    return true;
  }

  private Boolean visitDefCall(LeveledDefCallExpression expr1, Expression expr2) {
    LeveledDefCallExpression defCall2 = expr2.cast(LeveledDefCallExpression.class);
    if (!compareDef(expr1, defCall2, expr2)) {
      return false;
    }

    ExprSubstitution substitution = new ExprSubstitution();

    /* Stricter version of iso
    if (expr1.getDefinition() == Prelude.ISO) {
      return correctOrder ? compareIsoArgs(expr1.getDefCallArguments(), defCall2.getDefCallArguments(), substitution) : compareIsoArgs(defCall2.getDefCallArguments(), expr1.getDefCallArguments(), substitution);
    }
    */
    return compareLists(expr1.getDefCallArguments(), defCall2.getDefCallArguments(), expr1.getDefinition().getParameters(), expr1.getDefinition(), substitution);
  }

  @Override
  public Boolean visitAt(AtExpression expr1, Expression expr2, Expression type) {
    AtExpression atExpr2 = expr2.cast(AtExpression.class);
    if (atExpr2 == null) {
      initResult(expr1, expr2);
      return false;
    }
    if (!compare(expr1.getPathArgument(), atExpr2.getPathArgument(), null, true)) {
      if (myResult == null) {
        initResult(expr1, expr2);
      } else {
        myResult.wholeExpr1 = AtExpression.make(myResult.wholeExpr1, expr1.getIntervalArgument(), false);
        myResult.wholeExpr2 = AtExpression.make(myResult.wholeExpr2, atExpr2.getIntervalArgument(), false);
      }
      return false;
    }
    if (!compare(expr1.getIntervalArgument(), atExpr2.getIntervalArgument(), null, true)) {
      if (myResult == null) {
        initResult(expr1, expr2);
      } else {
        myResult.wholeExpr1 = AtExpression.make(expr1.getPathArgument(), myResult.wholeExpr1, false);
        myResult.wholeExpr2 = AtExpression.make(atExpr2.getPathArgument(), myResult.wholeExpr2, false);
      }
      return false;
    }
    return true;
  }

  /*
  private boolean compareIsoArgs(List<? extends Expression> list1, List<? extends Expression> list2, ExprSubstitution substitution) {
    DependentLink link = Prelude.ISO.getParameters();
    if (!compare(list1.get(0), list2.get(0), link.getTypeExpr())) {
      return false;
    }
    substitution.add(link, (myCMP == CMP.LE ? list2 : list1).get(0));
    link = link.getNext();

    if (!compare(list1.get(1), list2.get(1), link.getTypeExpr())) {
      return false;
    }
    substitution.add(link, (myCMP == CMP.LE ? list2 : list1).get(1));
    link = link.getNext();

    if (!compare(list1.get(2), list2.get(2), link.getTypeExpr().subst(substitution))) {
      return false;
    }
    return compare(list1.get(6), list2.get(6), link.getNext().getNext().getNext().getNext().getTypeExpr());
  }
  */

  private boolean compareConstArray(FunCallExpression atExpr, Expression otherExpr, Expression type, boolean correctOrder) {
    boolean ok = true;
    if (otherExpr.getStuckInferenceVariable() != null) {
      ok = false;
    } else {
      Expression arg = atExpr.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
      if (!(arg instanceof ArrayExpression arrayExpr)) {
        ok = correctOrder ? visitDefCall(atExpr, otherExpr) : otherExpr instanceof FunCallExpression && ((FunCallExpression) otherExpr).getDefinition() == Prelude.ARRAY_INDEX ? visitDefCall((FunCallExpression) otherExpr, atExpr) : otherExpr.accept(this, atExpr, type);
      } else {
        if (arrayExpr.getTail() != null) {
          ok = false;
        } else {
          for (Expression element : arrayExpr.getElements()) {
            if (!compare(element, otherExpr, type, false)) {
              ok = false;
              break;
            }
          }
        }
      }
    }
    if (!ok) {
      initResult(atExpr, otherExpr, correctOrder);
    }
    return ok;
  }

  @Override
  public Boolean visitFunCall(FunCallExpression expr1, Expression expr2, Expression type) {
    if (expr1.getDefinition() == Prelude.ARRAY_INDEX) {
      boolean ok;
      if (expr2 instanceof FunCallExpression && ((FunCallExpression) expr2).getDefinition() == Prelude.ARRAY_INDEX) {
        ok = visitDefCall(expr1, expr2) || compareConstArray(expr1, expr2, type, true) || compareConstArray((FunCallExpression) expr2, expr1, type, false);
      } else {
        ok = compareConstArray(expr1, expr2, type, true);
      }
      if (!ok) {
        initResult(expr1, expr2);
      }
      return ok;
    } else {
      if (!visitDefCall(expr1, expr2)) {
        if (myResult == null) {
          initResult(expr1, expr2);
        } else {
          if (myResult.index >= 0 && myResult.index < expr1.getDefCallArguments().size()) {
            List<Expression> args = new ArrayList<>(expr1.getDefCallArguments());
            args.set(myResult.index, myResult.wholeExpr1);
            myResult.wholeExpr1 = FunCallExpression.make(expr1.getDefinition(), expr1.getLevels(), args);
          } else {
            myResult.wholeExpr1 = expr1;
          }
          FunCallExpression funCall2 = expr2.cast(FunCallExpression.class);
          if (funCall2 != null && myResult.index >= 0 && myResult.index < funCall2.getDefCallArguments().size()) {
            List<Expression> args = new ArrayList<>(funCall2.getDefCallArguments());
            args.set(myResult.index, myResult.wholeExpr2);
            myResult.wholeExpr2 = FunCallExpression.make(funCall2.getDefinition(), funCall2.getLevels(), args);
          } else {
            myResult.wholeExpr2 = expr2;
          }
          myResult.index = -1;
        }
        return false;
      }
      return true;
    }
  }

  private void restoreConCalls(List<Pair<ConCallExpression, ConCallExpression>> stack) {
    if (stack == null) return;
    for (int i = stack.size() - 1; i >= 0; i--) {
      ConCallExpression conCall1 = stack.get(i).proj1;
      ConCallExpression conCall2 = stack.get(i).proj2;
      List<Expression> args1 = new ArrayList<>(conCall1.getDefCallArguments());
      args1.set(conCall1.getDefinition().getRecursiveParameter(), myResult.wholeExpr1);
      myResult.wholeExpr1 = ConCallExpression.make(conCall1.getDefinition(), conCall1.getLevels(), conCall1.getDataTypeArguments(), args1);
      List<Expression> args2 = new ArrayList<>(conCall2.getDefCallArguments());
      args2.set(conCall2.getDefinition().getRecursiveParameter(), myResult.wholeExpr2);
      myResult.wholeExpr2 = ConCallExpression.make(conCall2.getDefinition(), conCall2.getLevels(), conCall2.getDataTypeArguments(), args2);
    }
  }

  @Override
  public Boolean visitConCall(ConCallExpression expr1, Expression expr2, Expression type) {
    ConCallExpression conCall2;
    Expression it = expr1;
    List<Pair<ConCallExpression, ConCallExpression>> stack = null;
    while (true) {
      expr1 = (ConCallExpression) it;
      if (expr2 instanceof IntegerExpression) {
        return visitInteger((IntegerExpression) expr2, expr1) || myOnlySolveVars;
      }
      conCall2 = expr2.cast(ConCallExpression.class);
      if (!compareDef(expr1, conCall2, expr2)) {
        return myOnlySolveVars;
      }

      int recursiveParam = expr1.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        if (!(compareLists(expr1.getDefCallArguments(), conCall2.getDefCallArguments(), expr1.getDefinition().getParameters(), expr1.getDefinition(), null) || myOnlySolveVars)) {
          if (myResult == null) {
            initResult(expr1, conCall2);
          } else {
            if (myResult.index >= 0 && myResult.index < expr1.getDefCallArguments().size()) {
              List<Expression> args = new ArrayList<>(expr1.getDefCallArguments());
              args.set(myResult.index, myResult.wholeExpr1);
              myResult.wholeExpr1 = ConCallExpression.make(expr1.getDefinition(), expr1.getLevels(), args, expr1.getDefCallArguments());
            } else {
              myResult.wholeExpr1 = expr1;
            }
            if (myResult.index >= 0 && myResult.index < conCall2.getDefCallArguments().size()) {
              List<Expression> args = new ArrayList<>(conCall2.getDefCallArguments());
              args.set(myResult.index, myResult.wholeExpr2);
              myResult.wholeExpr2 = ConCallExpression.make(conCall2.getDefinition(), conCall2.getLevels(), args, conCall2.getDefCallArguments());
            } else {
              myResult.wholeExpr2 = conCall2;
            }
            myResult.index = -1;
            restoreConCalls(stack);
          }
          return false;
        }
        return true;
      }

      if (stack == null) {
        stack = new ArrayList<>();
      }
      stack.add(new Pair<>(expr1, conCall2));

      for (int i = 0; i < expr1.getDefCallArguments().size(); i++) {
        if (i != recursiveParam && !compare(expr1.getDefCallArguments().get(i), conCall2.getDefCallArguments().get(i), null, true)) {
          if (myOnlySolveVars) {
            return true;
          }
          if (myResult == null) {
            initResult(expr1, conCall2);
          } else {
            if (myResult.index >= 0 && myResult.index < expr1.getDefCallArguments().size()) {
              List<Expression> args = new ArrayList<>(expr1.getDefCallArguments());
              args.set(i, myResult.wholeExpr1);
              myResult.wholeExpr1 = ConCallExpression.make(expr1.getDefinition(), expr1.getLevels(), expr1.getDataTypeArguments(), args);
            } else {
              myResult.wholeExpr1 = expr1;
            }
            if (myResult.index >= 0 && myResult.index < conCall2.getDefCallArguments().size()) {
              List<Expression> args = new ArrayList<>(conCall2.getDefCallArguments());
              args.set(i, myResult.wholeExpr2);
              myResult.wholeExpr2 = ConCallExpression.make(conCall2.getDefinition(), conCall2.getLevels(), conCall2.getDataTypeArguments(), args);
            } else {
              myResult.wholeExpr2 = conCall2;
            }
          }
          if (myResult != null) {
            restoreConCalls(stack);
          }
          return false;
        }
      }

      it = expr1.getDefCallArguments().get(recursiveParam).getUnderlyingExpression();
      expr2 = conCall2.getDefCallArguments().get(recursiveParam).getUnderlyingExpression();
      if (it == expr2) {
        return true;
      }
      it = it.normalize(NormalizationMode.WHNF);
      expr2 = expr2.normalize(NormalizationMode.WHNF);
      if (!(it instanceof ConCallExpression)) {
        break;
      }

      // compare(it, expr2, null)
      InferenceReferenceExpression infRefExpr2 = expr2.cast(InferenceReferenceExpression.class);
      if (infRefExpr2 != null && infRefExpr2.getVariable() != null) {
        if (!(myNormalCompare && myEquations.addEquation(it, infRefExpr2, type, myCMP, infRefExpr2.getVariable().getSourceNode(), it.getStuckInferenceVariable(), infRefExpr2.getVariable()) || myOnlySolveVars)) {
          if (initResult(expr1, expr2)) {
            restoreConCalls(stack);
          }
          return false;
        }
        return true;
      }
      InferenceVariable stuckVar2 = expr2.getStuckInferenceVariable();
      if (stuckVar2 != null && (!myNormalCompare || myEquations == DummyEquations.getInstance())) {
        if (!myOnlySolveVars) {
          if (initResult(expr1, expr2)) {
            restoreConCalls(stack);
          }
          return false;
        }
        return true;
      }

      if (myOnlySolveVars && stuckVar2 != null) {
        return true;
      }

      if (!myNormalCompare || !myNormalize) {
        if (initResult(expr1, expr2)) {
          restoreConCalls(stack);
        }
        return false;
      }

      // normalizedCompare(it, expr2, null)
      if (expr2 instanceof ErrorExpression) {
        return true;
      }
      Expression stuck2 = expr2.getStuckExpression();
      if (stuck2 != null && stuck2.isError()) {
        return true;
      }
      stuckVar2 = expr2.getInferenceVariable();
      if (stuckVar2 != null) {
        if (!(myNormalCompare && myEquations.addEquation(it, substitute(expr2), type, myCMP, stuckVar2.getSourceNode(), null, stuckVar2) || myOnlySolveVars)) {
          if (initResult(expr1, expr2)) {
            restoreConCalls(stack);
          }
          return false;
        }
        return true;
      }
    }

    if (!compare(it, expr2, null, true)) {
      if (initResult(it, expr2)) {
        restoreConCalls(stack);
      }
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitDataCall(DataCallExpression expr1, Expression expr2, Expression type) {
    if (expr1.getDefinition() == Prelude.FIN || expr1.getDefinition() == Prelude.NAT) {
      DataCallExpression dataCall2 = expr2.cast(DataCallExpression.class);
      if (!(dataCall2 != null && (dataCall2.getDefinition() == Prelude.FIN || dataCall2.getDefinition() == Prelude.NAT) && checkFin(myCMP == CMP.GE ? dataCall2 : expr1, myCMP == CMP.GE ? expr1 : dataCall2, myCMP != CMP.GE))) {
        if (myNormalCompare) {
          if (dataCall2 != null && dataCall2.getDefinition() == Prelude.FIN && expr1.getDefinition() == Prelude.FIN) {
            int sucs = 0;
            Expression arg1 = expr1.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
            Expression arg2 = dataCall2.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
            while (arg1 instanceof ConCallExpression conCall1 && conCall1.getDefinition() == Prelude.SUC && arg2 instanceof ConCallExpression conCall2 && conCall2.getDefinition() == Prelude.SUC) {
              sucs++;
              arg1 = conCall1.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
              arg2 = conCall2.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
            }
            Expression wholeExpr1 = arg1;
            Expression wholeExpr2 = arg2;
            for (int i = 0; i < sucs; i++) {
              wholeExpr1 = Suc(wholeExpr1);
              wholeExpr2 = Suc(wholeExpr2);
            }
            wholeExpr1 = Fin(wholeExpr1);
            wholeExpr2 = Fin(wholeExpr2);
            if (initResult(wholeExpr1, wholeExpr2)) {
              myResult.wholeExpr1 = wholeExpr1;
              myResult.wholeExpr2 = wholeExpr2;
              myResult.subExpr1 = arg1;
              myResult.subExpr2 = arg2;
            }
          } else {
            initResult(expr1, expr2);
          }
        }
        return false;
      }
      return true;
    }
    if (!visitDefCall(expr1, expr2)) {
      if (myResult == null) {
        initResult(expr1, expr2);
      } else {
        if (myResult.index >= 0 && myResult.index < expr1.getDefCallArguments().size()) {
          List<Expression> args = new ArrayList<>(expr1.getDefCallArguments());
          args.set(myResult.index, myResult.wholeExpr1);
          myResult.wholeExpr1 = DataCallExpression.make(expr1.getDefinition(), expr1.getLevels(), args);
        } else {
          myResult.wholeExpr1 = expr1;
        }
        DataCallExpression dataCall2 = expr2.cast(DataCallExpression.class);
        if (dataCall2 != null && myResult.index >= 0 && myResult.index < dataCall2.getDefCallArguments().size()) {
          List<Expression> args = new ArrayList<>(dataCall2.getDefCallArguments());
          args.set(myResult.index, myResult.wholeExpr2);
          myResult.wholeExpr2 = DataCallExpression.make(dataCall2.getDefinition(), dataCall2.getLevels(), args);
        } else {
          myResult.wholeExpr2 = expr2;
        }
        myResult.index = -1;
      }
      return false;
    }
    return true;
  }

  private Pair<Expression, BigInteger> getSucs(Expression expr) {
    int sucs = 0;
    while (true) {
      if (!(expr instanceof ConCallExpression && ((ConCallExpression) expr).getDefinition() == Prelude.SUC)) {
        return expr instanceof IntegerExpression ? new Pair<>(new SmallIntegerExpression(0), ((IntegerExpression) expr).plus(sucs).getBigInteger()) : new Pair<>(expr, BigInteger.valueOf(sucs));
      }
      expr = ((ConCallExpression) expr).getDefCallArguments().get(0);
      expr = myNormalize ? expr.normalize(NormalizationMode.WHNF) : expr.getUnderlyingExpression();
      sucs++;
    }
  }

  private boolean checkFin(DataCallExpression expr1, DataCallExpression expr2, boolean correctOrder) {
    if (expr1.getDefinition() == Prelude.NAT) {
      return expr2.getDefinition() == Prelude.NAT;
    }
    if (expr2.getDefinition() == Prelude.NAT) {
      return myCMP != CMP.EQ;
    }

    Expression arg1 = expr1.getDefCallArguments().get(0);
    Expression arg2 = expr2.getDefCallArguments().get(0);
    if (myCMP == CMP.EQ) {
      return compare(correctOrder ? arg1 : arg2, correctOrder ? arg2 : arg1, ExpressionFactory.Nat(), false);
    }

    if (myNormalize) {
      arg1 = arg1.normalize(NormalizationMode.WHNF);
      arg2 = arg2.normalize(NormalizationMode.WHNF);
    }
    if (arg1 instanceof IntegerExpression && arg2 instanceof IntegerExpression) {
      return ((IntegerExpression) arg1).compare((IntegerExpression) arg2) <= 0;
    }

    var pair1 = getSucs(arg1);
    var pair2 = getSucs(arg2);
    InferenceVariable stuckVar1 = pair1.proj1.getStuckInferenceVariable();
    InferenceVariable stuckVar2 = pair2.proj1.getStuckInferenceVariable();
    if (stuckVar2 == null && pair1.proj2.compareTo(pair2.proj2) > 0) {
      return false;
    }
    if (stuckVar2 == null && pair1.proj1 instanceof IntegerExpression || stuckVar2 != null && pair1.proj1 instanceof IntegerExpression && pair1.proj2.compareTo(pair2.proj2) <= 0) {
      return true;
    }
    if (stuckVar1 != null || stuckVar2 != null) {
      if (pair1.proj1.getInferenceVariable() == null && pair2.proj1.getInferenceVariable() == null) {
        boolean allowEquations = myAllowEquations;
        CMP cmp = myCMP;
        myAllowEquations = false;
        boolean ok = normalizedCompare((correctOrder ? pair1 : pair2).proj1, (correctOrder ? pair2 : pair1).proj1, ExpressionFactory.Nat(), false);
        myAllowEquations = allowEquations;
        myCMP = cmp;
        if (ok) {
          return true;
        }
      }

      if (pair1.proj1 instanceof InferenceReferenceExpression && pair2.proj1 instanceof InferenceReferenceExpression && stuckVar1 == stuckVar2) {
        return pair1.proj2.compareTo(pair2.proj2) <= 0;
      }
      if (!myNormalCompare || myEquations == DummyEquations.getInstance()) {
        return false;
      }
      if (pair1.proj1 instanceof InferenceReferenceExpression && stuckVar2 == null) {
        return myEquations.addEquation(pair1.proj1, pair2.proj1 instanceof IntegerExpression ? new BigIntegerExpression(pair2.proj2.subtract(pair1.proj2)) : ExpressionFactory.add(pair2.proj1, pair2.proj2.intValueExact() - pair1.proj2.intValueExact()), ExpressionFactory.Nat(), CMP.EQ, stuckVar1.getSourceNode(), stuckVar1, null);
      }
      if (pair2.proj1 instanceof InferenceReferenceExpression && stuckVar1 == null) {
        return myEquations.addEquation(
          pair1.proj1 instanceof IntegerExpression
            ? new BigIntegerExpression(pair1.proj2.subtract(pair2.proj2))
            : pair1.proj2.compareTo(pair2.proj2) <= 0 ? pair1.proj1 : ExpressionFactory.add(pair1.proj1, pair1.proj2.intValueExact() - pair2.proj2.intValueExact()),
          pair2.proj1, ExpressionFactory.Nat(), CMP.EQ, stuckVar2.getSourceNode(), null, stuckVar2);
      }
      if (!myAllowEquations) {
        return false;
      }
      if (myNormalize) {
        expr1 = ExpressionFactory.Fin(pair1.proj1 instanceof IntegerExpression ? new BigIntegerExpression(pair1.proj2) : ExpressionFactory.add(pair1.proj1, pair1.proj2.intValueExact()));
        expr2 = ExpressionFactory.Fin(pair2.proj1 instanceof IntegerExpression ? new BigIntegerExpression(pair2.proj2) : ExpressionFactory.add(pair2.proj1, pair2.proj2.intValueExact()));
      }
      return myEquations.addEquation((correctOrder ? expr1 : expr2), substitute(correctOrder ? expr2 : expr1), Type.OMEGA, myCMP, (stuckVar1 != null ? stuckVar1 : stuckVar2).getSourceNode(), stuckVar1, stuckVar2);
    } else {
      return normalizedCompare((correctOrder ? pair1 : pair2).proj1, (correctOrder ? pair2 : pair1).proj1, ExpressionFactory.Nat(), false);
    }
  }

  public static Expression checkedSubst(Expression expr, ExprSubstitution substitution, Set<Binding> allowedBindings, Set<Object> foundVars) {
    boolean[] found = new boolean[] { false };
    expr.accept(new FindMissingBindingVisitor(allowedBindings) {
      @Override
      public Boolean visitReference(ReferenceExpression expr, Void params) {
        Binding binding = expr.getBinding();
        if (!allowedBindings.contains(binding) && binding.getTypeExpr().findBinding(substitution.getKeys()) != null) {
          found[0] = true;
          if (foundVars == null) return true;
          foundVars.add(binding);
        }
        return false;
      }
    }, null);
    return found[0] ? null : expr.accept(new SubstVisitor(substitution, LevelSubstitution.EMPTY, false), null);
  }

  private Boolean checkApp(AppExpression expr1, Expression expr2, boolean correctOrder) {
    InferenceReferenceExpression infExpr;
    List<Pair<Binding,Boolean>> bindings = new ArrayList<>();
    while (true) {
      ReferenceExpression arg = expr1.getArgument().cast(ReferenceExpression.class);
      if (arg == null) return null;
      bindings.add(new Pair<>(arg.getBinding(), expr1.isExplicit()));
      Expression fun = myNormalCompare ? expr1.getFunction().normalize(NormalizationMode.WHNF) : expr1.getFunction().getUnderlyingExpression();
      if (fun instanceof InferenceReferenceExpression) {
        infExpr = (InferenceReferenceExpression) fun;
        break;
      }
      if (!(fun instanceof AppExpression)) return null;
      expr1 = (AppExpression) fun;
    }
    if (infExpr.getVariable() == null) return null;

    ExprSubstitution substitution = getSubstitution(correctOrder);
    SubstVisitor substVisitor = new SubstVisitor(substitution, LevelSubstitution.EMPTY, false);
    if (!substVisitor.isEmpty()) {
      expr2 = expr2.accept(substVisitor, null);
    }

    List<TypedSingleDependentLink> params = new ArrayList<>();
    ExprSubstitution paramSubst = new ExprSubstitution();
    Set<Binding> allowedBindings = new HashSet<>();
    for (int i = bindings.size() - 1; i >= 0; i--) {
      Pair<Binding, Boolean> pair = bindings.get(i);
      Type type = pair.proj1.getType();
      if (type == null) return null;
      if (!substVisitor.isEmpty()) {
        type = type.subst(substVisitor);
      }
      if (!paramSubst.isEmpty()) {
        Expression typeExpr = checkedSubst(type.getExpr(), paramSubst, allowedBindings, null);
        if (typeExpr == null) return null;
        type = typeExpr instanceof Type ? (Type) typeExpr : new TypeExpression(typeExpr, type.getSortOfType());
      }
      TypedSingleDependentLink param = new TypedSingleDependentLink(pair.proj2, pair.proj1.getName(), type);
      params.add(param);
      paramSubst.add(pair.proj1, new ReferenceExpression(param));
      allowedBindings.add(pair.proj1);
    }

    Expression result = checkedSubst(expr2, paramSubst, allowedBindings, null);
    Expression bodyType = result == null ? null : result.getType();
    Sort bodySort = bodyType == null ? null : bodyType.getSortOfType();
    if (bodySort == null) return null;
    if (!correctOrder) {
      infExpr.getVariable().getBounds().removeAll(mySubstitution.keySet());
    }
    for (int i = params.size() - 1; i >= 0; i--) {
      result = new LamExpression(PiExpression.generateUpperBound(params.get(i).getType().getSortOfType(), bodySort, myEquations, mySourceNode), params.get(i), result);
    }
    return myEquations.addEquation(correctOrder ? infExpr : result, correctOrder ? result : infExpr, null, myCMP, infExpr.getVariable().getSourceNode(), correctOrder ? infExpr.getVariable() : null, correctOrder ? null : infExpr.getVariable());
  }

  private Boolean checkDefCallAndApp(Expression expr1, Expression expr2, boolean correctOrder) {
    LeveledDefCallExpression defCall1 = expr1.cast(LeveledDefCallExpression.class);
    if (!(defCall1 instanceof DataCallExpression || defCall1 instanceof ClassCallExpression || defCall1 instanceof FunCallExpression && ((FunCallExpression) defCall1).getDefinition().getKind() == CoreFunctionDefinition.Kind.TYPE)) return null;
    AppExpression app2 = expr2.cast(AppExpression.class);
    if (app2 == null) {
      return null;
    }
    ClassCallExpression classCall1 = defCall1 instanceof ClassCallExpression ? (ClassCallExpression) defCall1 : null;

    List<Expression> args = new ArrayList<>();
    while (true) {
      args.add(app2.getArgument());
      Expression fun = app2.getFunction();
      Expression funNorm = myNormalCompare ? fun.normalize(NormalizationMode.WHNF) : fun;
      app2 = funNorm.cast(AppExpression.class);
      if (app2 != null) {
        continue;
      }

      TypeClassInferenceVariable variable;
      FieldCallExpression fieldCall = funNorm.cast(FieldCallExpression.class);
      if (fieldCall != null) {
        InferenceVariable infVar = fieldCall.getArgument().getInferenceVariable();
        variable = infVar instanceof TypeClassInferenceVariable ? (TypeClassInferenceVariable) infVar : null;
      } else {
        variable = null;
      }
      if (variable == null || classCall1 == null && args.size() > defCall1.getDefCallArguments().size() || classCall1 != null && args.size() > classCall1.getDefinition().getNumberOfNotImplementedFields()) {
        return null;
      }
      Collections.reverse(args);

      DependentLink dataParams;
      List<? extends Expression> oldDataArgs;
      if (classCall1 == null) {
        dataParams = defCall1.getDefinition().getParameters();
        oldDataArgs = defCall1.getDefCallArguments();
      } else {
        List<Expression> classArgs = new ArrayList<>();
        for (ClassField field : classCall1.getDefinition().getFields()) {
          if (field.isProperty() || !field.getReferable().isParameterField()) {
            break;
          }
          Expression implementation = classCall1.getAbsImplementationHere(field);
          if (implementation != null) {
            classArgs.add(implementation);
          } else {
            if (!classCall1.getDefinition().isImplemented(field)) {
              break;
            }
          }
        }
        if (args.size() > classArgs.size() || classCall1.getImplementedHere().size() > classArgs.size() && !(correctOrder && myCMP == CMP.LE || !correctOrder && myCMP == CMP.GE)) {
          return null;
        }
        dataParams = new ClassCallExpression(classCall1.getDefinition(), classCall1.getLevels(), new LinkedHashMap<>(), classCall1.getDefinition().getSort(), classCall1.getDefinition().getUniverseKind()).getClassFieldParameters();
        oldDataArgs = classArgs;
      }

      int numberOfOldArgs = oldDataArgs.size() - args.size();
      ExprSubstitution substitution = new ExprSubstitution();
      for (int i = 0; i < numberOfOldArgs; i++) {
        substitution.add(dataParams, oldDataArgs.get(i));
        dataParams = dataParams.getNext();
      }
      List<? extends Expression> oldList = oldDataArgs.subList(numberOfOldArgs, oldDataArgs.size());
      if (!compareLists(correctOrder ? oldList : args, correctOrder ? args : oldList, null, defCall1.getDefinition(), substitution)) {
        return false;
      }

      Expression lam;
      Sort codSort;
      List<SingleDependentLink> params = new ArrayList<>();
      if (classCall1 == null) {
        List<Expression> newDataArgs = new ArrayList<>(oldDataArgs.subList(0, numberOfOldArgs));
        SingleDependentLink firstParam = null;
        SingleDependentLink lastParam = null;
        for (; dataParams.hasNext(); dataParams = dataParams.getNext()) {
          SingleDependentLink link;
          if (dataParams instanceof TypedDependentLink) {
            link = new TypedSingleDependentLink(dataParams.isExplicit(), dataParams.getName(), dataParams.getType());
          } else {
            link = new UntypedSingleDependentLink(dataParams.getName());
          }
          newDataArgs.add(new ReferenceExpression(link));
          if (firstParam == null) {
            firstParam = link;
          }
          if (lastParam == null) {
            lastParam = link;
          } else {
            lastParam.setNext(link);
          }
          if (link instanceof TypedSingleDependentLink) {
            params.add(firstParam);
            firstParam = null;
            lastParam = null;
          }
        }
        lam = defCall1.getDefinition().getDefCall(defCall1.getLevels(), newDataArgs);
        codSort = defCall1 instanceof DataCallExpression ? ((DataCallExpression) defCall1).getDefinition().getSort() : ((UniverseExpression) ((FunCallExpression) defCall1).getDefinition().getResultType()).getSort();
      } else {
        Map<ClassField, Expression> implementations = new LinkedHashMap<>();
        codSort = classCall1.getSortOfType();
        ClassCallExpression classCall = new ClassCallExpression(classCall1.getDefinition(), classCall1.getLevels(), implementations, classCall1.getSort(), classCall1.getUniverseKind());
        int i = 0;
        for (ClassField field : classCall1.getDefinition().getFields()) {
          if (!classCall1.getDefinition().isImplemented(field)) {
            if (i < oldDataArgs.size() - args.size()) {
              implementations.put(field, classCall1.getImplementationHere(field, new ReferenceExpression(classCall.getThisBinding())));
              i++;
            } else {
              PiExpression piType = classCall1.getDefinition().getFieldType(field, classCall1.getLevels(field.getParentClass()));
              Expression type = piType.getCodomain();
              TypedSingleDependentLink link = new TypedSingleDependentLink(field.getReferable().isExplicitField(), field.getName(), type instanceof Type ? (Type) type : new TypeExpression(type, piType.getResultSort()));
              params.add(link);
              implementations.put(field, new ReferenceExpression(link));
            }
            if (implementations.size() == oldDataArgs.size()) {
              break;
            }
          }
        }
        classCall.updateHasUniverses();
        lam = classCall;
      }

      for (int i = params.size() - 1; i >= 0; i--) {
        if (!myNormalCompare || !myEquations.supportsLevels()) {
          return false;
        }
        codSort = PiExpression.generateUpperBound(params.get(i).getType().getSortOfType(), codSort, myEquations, mySourceNode);
        lam = new LamExpression(codSort, params.get(i), lam);
      }

      Expression finalExpr1 = correctOrder ? lam : substitute(fun);
      Expression finalExpr2 = correctOrder ? fun : substitute(lam);
      if (variable.isSolved()) {
        CompareVisitor visitor = new CompareVisitor(myEquations, myCMP, variable.getSourceNode());
        visitor.myNormalCompare = myNormalCompare;
        return visitor.compare(finalExpr1, finalExpr2, null, true);
      } else {
        return myNormalCompare && myEquations.addEquation(finalExpr1, finalExpr2, null, myCMP, variable.getSourceNode(), correctOrder ? null : variable, correctOrder ? variable : null) ? true : null;
      }
    }
  }

  @Override
  public Boolean visitFieldCall(FieldCallExpression fieldCall1, Expression expr2, Expression type) {
    FieldCallExpression fieldCall2 = expr2.cast(FieldCallExpression.class);
    if (fieldCall2 == null || fieldCall1.getDefinition() != fieldCall2.getDefinition()) {
      initResult(fieldCall1, expr2);
      return false;
    }

    InferenceVariable var = fieldCall1.getArgument().getInferenceVariable();
    if (var instanceof TypeClassInferenceVariable) {
      Boolean result = myEquations.solveInstance((TypeClassInferenceVariable) var, fieldCall1, fieldCall2);
      if (result != null) {
        return result || compare(fieldCall1, fieldCall2, type, true);
      }
    }
    var = fieldCall2.getArgument().getInferenceVariable();
    if (var instanceof TypeClassInferenceVariable) {
      Boolean result = myEquations.solveInstance((TypeClassInferenceVariable) var, fieldCall2, fieldCall1);
      if (result != null) {
        return result || compare(fieldCall1, fieldCall2, type, true);
      }
    }

    return compare(fieldCall1.getArgument(), fieldCall2.getArgument(), null, false);
  }

  private boolean checkSubclassImpl(ClassCallExpression classCall1, ClassCallExpression classCall2, boolean correctOrder) {
    CMP origCMP = myCMP;
    for (ClassField field : classCall2.getDefinition().getFields()) {
      if (field.isProperty()) {
        continue;
      }

      Expression impl2 = classCall2.getAbsImplementationHere(field);
      if (impl2 == null) {
        continue;
      }

      Expression impl1 = classCall1.getAbsImplementationHere(field);
      Binding binding = classCall1.getThisBinding();
      if (impl1 == null) {
        AbsExpression absImpl1 = classCall1.getDefinition().getImplementation(field);
        if (absImpl1 != null) {
          impl1 = absImpl1.apply(new ReferenceExpression(binding), classCall1.getLevelSubstitution());
        }
      }
      if (impl1 == null) {
        return false;
      }
      if (field == Prelude.ARRAY_ELEMENTS_TYPE && !classCall2.isImplemented(Prelude.ARRAY_LENGTH) && classCall1.isImplemented(Prelude.ARRAY_LENGTH)) {
        impl2 = impl2.subst(classCall2.getThisBinding(), new NewExpression(null, classCall1));
      }
      if (!classCall2.getDefinition().isCovariantField(field)) {
        myCMP = CMP.EQ;
      }
      mySubstitution.put(classCall2.getThisBinding(), binding);
      boolean ok = compare(correctOrder ? impl1 : impl2, correctOrder ? impl2 : impl1, field.getType(classCall2.getLevels(field.getParentClass())).applyExpression(new ReferenceExpression(binding)), true);
      mySubstitution.remove(classCall2.getThisBinding());
      if (!ok) {
        return false;
      }
      myCMP = origCMP;
    }
    return true;
  }

  private boolean compareClassCallLevelsLE(ClassCallExpression classCall1, ClassCallExpression classCall2, CMP cmp, Equations equations) {
    return classCall1.getLevels(classCall2.getDefinition()).compare(classCall2.getLevels(), cmp, equations, mySourceNode);
  }

  private boolean doesImplementationFit(Expression implementation, ClassField field, ClassCallExpression classCall1, ClassCallExpression classCall2) {
    Expression type = implementation.normalize(NormalizationMode.WHNF).getType(true);
    if (type == null) {
      return false;
    }
    CMP origCmp = myCMP;
    myCMP = CMP.LE;
    boolean ok = compare(type, classCall1.getDefinition().getFieldType(field, classCall2.getLevels(field.getParentClass()), new ReferenceExpression(classCall1.getThisBinding())), Type.OMEGA, false);
    myCMP = origCmp;
    return ok;
  }

  private boolean checkClassCallLevels(ClassCallExpression classCall1, ClassCallExpression classCall2, CMP onSuccess, CMP onFailure) {
    boolean ok = true;
    for (Map.Entry<ClassField, AbsExpression> entry : classCall1.getDefinition().getImplemented()) {
      if (!entry.getKey().isProperty() && entry.getKey().getUniverseKind() != UniverseKind.NO_UNIVERSES && classCall2.getDefinition().getFields().contains(entry.getKey()) && !classCall2.isImplemented(entry.getKey())) {
        if (!doesImplementationFit(entry.getValue().apply(new ReferenceExpression(classCall1.getThisBinding()), classCall1.getLevelSubstitution()), entry.getKey(), classCall1, classCall2)) {
          ok = false;
          break;
        }
      }
    }
    if (ok) {
      for (Map.Entry<ClassField, Expression> entry : classCall1.getImplementedHere().entrySet()) {
        if (!entry.getKey().isProperty() && entry.getKey().getUniverseKind() != UniverseKind.NO_UNIVERSES && classCall2.getDefinition().getFields().contains(entry.getKey()) && !classCall2.isImplemented(entry.getKey())) {
          if (!doesImplementationFit(entry.getValue(), entry.getKey(), classCall1, classCall2)) {
            ok = false;
            break;
          }
        }
      }
    }

    if (ok) {
      return onSuccess == null || myNormalCompare && compareClassCallLevelsLE(classCall1, classCall2, onSuccess, myEquations);
    } else {
      return myNormalCompare && compareClassCallLevelsLE(classCall1, classCall2, onFailure, myEquations);
    }
  }

  private boolean compareClassCallLevels(ClassCallExpression classCall1, ClassCallExpression classCall2, CMP cmp, Equations equations) {
    return cmp == CMP.GE ? compareClassCallLevelsLE(classCall2, classCall1, cmp.not(), equations) : compareClassCallLevelsLE(classCall1, classCall2, cmp, equations);
  }

  public boolean compareClassCallLevels(ClassCallExpression classCall1, ClassCallExpression classCall2) {
    UniverseKind kind1 = classCall1.getUniverseKind();
    UniverseKind kind2 = classCall2.getUniverseKind();
    if (kind1 == UniverseKind.NO_UNIVERSES && kind2 == UniverseKind.NO_UNIVERSES) {
      return true;
    }
    if (myCMP == CMP.EQ || kind1 == kind2) {
      return compareClassCallLevels(classCall1, classCall2, kind1 == UniverseKind.ONLY_COVARIANT ? myCMP : CMP.EQ, myNormalCompare ? myEquations : DummyEquations.getInstance());
    }
    if (!compareClassCallLevels(classCall1, classCall2, myCMP, DummyEquations.getInstance())) {
      CMP onSuccess = kind1 == UniverseKind.NO_UNIVERSES || kind2 == UniverseKind.NO_UNIVERSES ? null : CMP.LE;
      CMP onFailure = kind1 == UniverseKind.WITH_UNIVERSES || kind2 == UniverseKind.WITH_UNIVERSES ? CMP.EQ : CMP.LE;
      return myCMP == CMP.LE ? checkClassCallLevels(classCall1, classCall2, onSuccess, onFailure) : checkClassCallLevels(classCall2, classCall1, onSuccess, onFailure);
    }
    return true;
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr1, Expression expr2, Expression type) {
    ClassCallExpression classCall2 = expr2.cast(ClassCallExpression.class);
    if (classCall2 == null) {
      initResult(expr1, expr2);
      return false;
    }

    if (myCMP == CMP.LE && !expr1.getDefinition().isSubClassOf(classCall2.getDefinition())) {
      initResult(expr1, expr2);
      return false;
    }
    if (myCMP == CMP.GE && !classCall2.getDefinition().isSubClassOf(expr1.getDefinition())) {
      initResult(expr1, expr2);
      return false;
    }
    if (myCMP == CMP.EQ && expr1.getDefinition() != classCall2.getDefinition()) {
      initResult(expr1, expr2);
      return false;
    }

    if (!compareClassCallLevels(expr1, classCall2)) {
      initResult(expr1, expr2);
      return false;
    }

    if (myCMP == CMP.LE) {
      if (!checkSubclassImpl(expr1, classCall2, true)) {
        initResult(expr1, expr2);
        return false;
      }
      return true;
    }

    if (myCMP == CMP.GE) {
      if (!checkSubclassImpl(classCall2, expr1, false)) {
        initResult(expr1, expr2);
        return false;
      }
      return true;
    }

    for (ClassField field : classCall2.getImplementedHere().keySet()) {
      if (!field.isProperty() && !expr1.isImplemented(field)) {
        initResult(expr1, expr2);
        return false;
      }
    }

    if (!checkSubclassImpl(expr1, classCall2, true)) {
      initResult(expr1, expr2);
      return false;
    }
    return true;
  }

  private Binding substBinding(Binding binding) {
    Binding subst = mySubstitution.get(binding);
    return subst == null ? binding : subst;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr1, Expression expr2, Expression type) {
    ReferenceExpression ref2 = expr2.cast(ReferenceExpression.class);
    if (!(ref2 != null && substBinding(ref2.getBinding()) == expr1.getBinding())) {
      initResult(expr1, expr2);
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr1, Expression expr2, Expression type) {
    if (expr1.getSubstExpression() == null) {
      InferenceReferenceExpression infRefExpr2 = expr2.cast(InferenceReferenceExpression.class);
      if (!(infRefExpr2 != null && infRefExpr2.getVariable() == expr1.getVariable())) {
        initResult(expr1, expr2);
        return false;
      }
      return true;
    } else {
      return expr1.getSubstExpression().accept(this, expr2, type);
    }
  }

  @Override
  public Boolean visitSubst(SubstExpression expr, Expression expr2, Expression type) {
    if (expr.getExpression() instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr.getExpression()).getVariable() != null) {
      if (!myEquations.addEquation(expr, substitute(expr2), type, myCMP, mySourceNode, ((InferenceReferenceExpression) expr.getExpression()).getVariable(), expr2.getStuckInferenceVariable())) {
        initResult(expr, expr2);
        return false;
      }
      return true;
    } else {
      return expr.getSubstExpression().accept(this, expr2, type);
    }
  }

  private DependentLink replaceParameter(List<DependentLink> params, Expression type, ExprSubstitution subst) {
    LinkList list = new LinkList();
    for (DependentLink param : params) {
      DependentLink newParam = param.subst(new SubstVisitor(subst, LevelSubstitution.EMPTY), 1, false);
      list.append(newParam);
      subst.add(param, new ReferenceExpression(newParam));
    }
    DependentLink.Helper.get(list.getFirst(), myResult.index).getNextTyped(null).setType(makeType(type));
    return list.getFirst();
  }

  private Expression addLamParameters(LamExpression lamExpr1, List<DependentLink> params1, Expression body1, List<DependentLink> params2) {
    if (params2.size() > params1.size()) {
      params1 = new ArrayList<>(params1);
      params1.addAll(params2);
    }
    ExprSubstitution subst = new ExprSubstitution();
    List<SingleDependentLink> newParams = new ArrayList<>();
    for (int i = 0; i < params1.size(); i++) {
      DependentLink param = params1.get(i);
      List<DependentLink> names = new ArrayList<>();
      DependentLink typedParam = param;
      while (!(typedParam instanceof TypedDependentLink)) {
        names.add(typedParam);
        typedParam = typedParam.getNext();
      }
      i += names.size();
      SingleDependentLink newParam = new TypedSingleDependentLink(typedParam.isExplicit(), typedParam.getName(), typedParam.getType().subst(new SubstVisitor(subst, LevelSubstitution.EMPTY)));
      subst.add(typedParam, new ReferenceExpression(newParam));
      for (int j = names.size() - 1; j >= 0; j--) {
        newParam = new UntypedSingleDependentLink(names.get(j).getName(), newParam);
      }
      newParams.add(newParam);
    }
    Expression result = body1.subst(subst);
    for (int i = newParams.size() - 1; i >= 0; i--) {
      result = new LamExpression(lamExpr1.getResultSort(), newParams.get(i), result);
    }
    return result;
  }

  private Boolean visitLam(LamExpression expr1, Expression expr2, Expression type, boolean correctOrder) {
    List<DependentLink> params1 = new ArrayList<>();
    List<DependentLink> params2 = new ArrayList<>();
    Expression body1 = expr1.getLamParameters(params1);
    Expression body2 = expr2.getLamParameters(params2);

    for (int i = 0; i < params1.size() && i < params2.size(); i++) {
      mySubstitution.put(correctOrder ? params2.get(i) : params1.get(i), correctOrder ? params1.get(i) : params2.get(i));
    }

    if (params1.size() < params2.size()) {
      for (int i = params1.size(); i < params2.size(); i++) {
        body1 = AppExpression.make(body1, new ReferenceExpression(params2.get(i)), params2.get(i).isExplicit());
      }
    }
    if (params2.size() < params1.size()) {
      for (int i = params2.size(); i < params1.size(); i++) {
        body2 = AppExpression.make(body2, new ReferenceExpression(params1.get(i)), params1.get(i).isExplicit());
      }
    }

    type = type == null ? null : type.dropPiParameter(Math.max(params1.size(), params2.size()));
    Boolean result = compare(correctOrder ? body1 : body2, correctOrder ? body2 : body1, type, true);
    for (int i = 0; i < params1.size() && i < params2.size(); i++) {
      mySubstitution.remove(correctOrder ? params2.get(i) : params1.get(i));
    }
    if (!result) {
      LamExpression lamExpr2 = expr2.cast(LamExpression.class);
      if (myResult == null || lamExpr2 == null) {
        initResult(expr1, expr2, correctOrder);
      } else {
        myResult.wholeExpr1 = addLamParameters(correctOrder ? expr1 : lamExpr2, correctOrder ? params1 : params2, myResult.wholeExpr1, correctOrder ? params2 : params1);
        myResult.wholeExpr2 = addLamParameters(correctOrder ? lamExpr2 : expr1, correctOrder ? params2 : params1, myResult.wholeExpr2, correctOrder ? params1 : params2);
      }
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitLam(LamExpression expr1, Expression expr2, Expression type) {
    return visitLam(expr1, expr2, type, true);
  }

  private Type makeType(Expression expr) {
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, Sort.TypeOfLevel(Integer.MAX_VALUE));
  }

  private Expression replaceDomain(PiExpression oldPi, Expression newType) {
    ExprSubstitution subst = new ExprSubstitution();
    SingleDependentLink newParams = DependentLink.Helper.subst(oldPi.getParameters(), subst);
    newParams.getNextTyped(null).setType(makeType(newType));
    return new PiExpression(oldPi.getResultSort(), newParams, oldPi.getCodomain().subst(subst));
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2, Expression type) {
    PiExpression piExpr2 = expr2.cast(PiExpression.class);
    if (piExpr2 == null) {
      initResult(expr1, expr2);
      return false;
    }

    CMP origCMP = myCMP;
    myCMP = CMP.EQ;
    if (!compare(expr1.getParameters().getTypeExpr(), piExpr2.getParameters().getTypeExpr(), Type.OMEGA, false)) {
      if (myResult == null) {
        initResult(expr1, expr2);
      } else {
        myResult.wholeExpr1 = replaceDomain(expr1, myResult.wholeExpr1);
        myResult.wholeExpr2 = replaceDomain(piExpr2, myResult.wholeExpr2);
      }
      return false;
    }

    SingleDependentLink link1 = expr1.getParameters(), link2 = piExpr2.getParameters();
    for (; link1.hasNext() && link2.hasNext(); link1 = link1.getNext(), link2 = link2.getNext()) {
      if (link1.isExplicit() != link2.isExplicit()) {
        initResult(expr1, expr2);
        return false;
      }
      mySubstitution.put(link2, link1);
    }

    myCMP = origCMP;
    if (!compare(link1.hasNext() ? new PiExpression(expr1.getResultSort(), link1, expr1.getCodomain()) : expr1.getCodomain(), link2.hasNext() ? new PiExpression(piExpr2.getResultSort(), link2, piExpr2.getCodomain()) : piExpr2.getCodomain(), Type.OMEGA, false)) {
      if (link1.hasNext() || link2.hasNext() || myResult == null) {
        initResult(expr1, expr2);
      } else {
        myResult.wholeExpr1 = new PiExpression(expr1.getResultSort(), expr1.getParameters(), myResult.wholeExpr1);
        myResult.wholeExpr2 = new PiExpression(piExpr2.getResultSort(), piExpr2.getParameters(), myResult.wholeExpr2);
      }
      return false;
    }

    for (DependentLink link = piExpr2.getParameters(); link != link2; link = link.getNext()) {
      mySubstitution.remove(link);
    }
    mySubstitution.remove(link2);
    return true;
  }

  public boolean compareParameters(DependentLink params1, DependentLink params2) {
    List<DependentLink> list1 = DependentLink.Helper.toList(params1);
    List<DependentLink> list2 = DependentLink.Helper.toList(params2);

    if (list1.size() != list2.size()) {
      return false;
    }

    CMP origCMP = myCMP;
    for (int i = 0; i < list1.size() && i < list2.size(); ++i) {
      DependentLink param1 = list1.get(i);
      DependentLink param2 = list2.get(i);
      if (param1.isProperty() != param2.isProperty() || !compare(param1.getTypeExpr(), param2.getTypeExpr(), Type.OMEGA, false)) {
        for (int j = 0; j < i; j++) {
          mySubstitution.remove(list2.get(j));
        }
        myCMP = origCMP;
        if (param1.isProperty() == param2.isProperty()) {
          if (initResult(param1.getTypeExpr(), param2.getTypeExpr())) {
            myResult.index = i;
          }
        }
        return false;
      }
      mySubstitution.put(param2, param1);
      myCMP = origCMP;
    }

    return true;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr1, Expression expr2, Expression type) {
    UniverseExpression universe2 = expr2.cast(UniverseExpression.class);
    if (universe2 == null) {
      initResult(expr1, expr2);
      return false;
    }
    if (!Sort.compare(expr1.getSort(), universe2.getSort(), myCMP, myNormalCompare ? myEquations : DummyEquations.getInstance(), mySourceNode)) {
      initResult(expr1, expr2);
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitError(ErrorExpression expr1, Expression expr2, Expression type) {
    return true;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr1, Expression expr2, Expression type) {
    return visitTuple(expr1, expr2, true);
  }

  private Boolean visitTuple(TupleExpression expr1, Expression expr2, boolean correctOrder) {
    Expression type2 = expr2.getType();
    if (type2 == null || !compare(correctOrder ? expr1.getSigmaType() : type2, correctOrder ? type2 : expr1.getSigmaType(), Type.OMEGA, false)) {
      initResult(expr1, expr2, correctOrder);
      return false;
    }

    TupleExpression tuple2 = expr2.cast(TupleExpression.class);
    if (tuple2 != null) {
      if (!(correctOrder ? compareLists(expr1.getFields(), tuple2.getFields(), expr1.getSigmaType().getParameters(), null, new ExprSubstitution(), true) : compareLists(tuple2.getFields(), expr1.getFields(), tuple2.getSigmaType().getParameters(), null, new ExprSubstitution(), true))) {
        if (myResult == null) {
          initResult(expr1, expr2);
        } else {
          TupleExpression expr1_ = correctOrder ? expr1 : tuple2;
          TupleExpression tuple2_ = correctOrder ? tuple2 : expr1;
          if (myResult.index >= 0 && myResult.index < expr1_.getFields().size()) {
            List<Expression> newFields = new ArrayList<>(expr1_.getFields());
            newFields.set(myResult.index, myResult.wholeExpr1);
            myResult.wholeExpr1 = new TupleExpression(newFields, expr1_.getSigmaType());
          } else {
            myResult.wholeExpr1 = expr1_;
          }
          if (myResult.index >= 0 && myResult.index < tuple2_.getFields().size()) {
            List<Expression> newFields = new ArrayList<>(tuple2_.getFields());
            newFields.set(myResult.index, myResult.wholeExpr2);
            myResult.wholeExpr2 = new TupleExpression(newFields, tuple2_.getSigmaType());
          } else {
            myResult.wholeExpr2 = tuple2_;
          }
          myResult.index = -1;
        }
        return false;
      }
    } else {
      List<Expression> args2 = new ArrayList<>(expr1.getFields().size());
      for (int i = 0; i < expr1.getFields().size(); i++) {
        args2.add(ProjExpression.make(expr2, i, expr1.getFields().get(i).isBoxed()));
      }
      if (!(correctOrder ? compareLists(expr1.getFields(), args2, expr1.getSigmaType().getParameters(), null, new ExprSubstitution(), true) : compareLists(args2, expr1.getFields(), expr1.getSigmaType().getParameters(), null, new ExprSubstitution(), true))) {
        initResult(expr1, expr2, correctOrder);
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr1, Expression expr2, Expression type) {
    SigmaExpression sigma2 = expr2.cast(SigmaExpression.class);
    if (sigma2 == null) {
      initResult(expr1, expr2);
      return false;
    }
    if (!compareParameters(expr1.getParameters(), sigma2.getParameters())) {
      if (myResult == null || myResult.index < 0) {
        initResult(expr1, expr2);
      } else {
        List<DependentLink> list1 = DependentLink.Helper.toList(expr1.getParameters());
        List<DependentLink> list2 = DependentLink.Helper.toList(sigma2.getParameters());
        if (myResult.index < list1.size() && myResult.index < list2.size()) {
          myResult.wholeExpr1 = new SigmaExpression(expr1.getSort(), replaceParameter(list1, myResult.wholeExpr1, new ExprSubstitution()));
          myResult.wholeExpr2 = new SigmaExpression(sigma2.getSort(), replaceParameter(list2, myResult.wholeExpr2, new ExprSubstitution()));
        }
        myResult.index = -1;
      }
      return false;
    }
    for (DependentLink link = sigma2.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }
    return true;
  }

  @Override
  public Boolean visitProj(ProjExpression expr1, Expression expr2, Expression type) {
    ProjExpression proj2 = expr2.cast(ProjExpression.class);
    if (proj2 == null || expr1.getField() != proj2.getField()) {
      initResult(expr1, expr2);
      return false;
    }
    if (!compare(expr1.getExpression(), proj2.getExpression(), null, true)) {
      if (myResult == null) {
        initResult(expr1, expr2);
      } else {
        myResult.wholeExpr1 = ProjExpression.make(myResult.wholeExpr1, expr1.getField(), expr1.isBoxed());
        myResult.wholeExpr2 = ProjExpression.make(myResult.wholeExpr2, proj2.getField(), proj2.isBoxed());
      }
      return false;
    }
    return true;
  }

  private boolean compareClassInstances(Expression expr1, ClassCallExpression classCall1, Expression expr2, ClassCallExpression classCall2, Expression type) {
    if (expr1 instanceof ArrayExpression && expr2 instanceof ArrayExpression) return false;
    if (classCall1.getDefinition() == Prelude.DEP_ARRAY && classCall2.getDefinition() == Prelude.DEP_ARRAY) {
      Expression length1 = classCall1.getImplementationHere(Prelude.ARRAY_LENGTH, expr1);
      Expression length2 = classCall2.getImplementationHere(Prelude.ARRAY_LENGTH, expr1);
      if (length1 != null && length2 != null) {
        length1 = length1.normalize(NormalizationMode.WHNF);
        length2 = length2.normalize(NormalizationMode.WHNF);
        if (length1 instanceof IntegerExpression && ((IntegerExpression) length1).isZero() && length2 instanceof IntegerExpression && ((IntegerExpression) length2).isZero()) {
          Expression elemsType1 = classCall1.getImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE, expr1);
          if (elemsType1 == null) elemsType1 = FieldCallExpression.make(Prelude.ARRAY_ELEMENTS_TYPE, expr1);
          Expression elemsType2 = classCall2.getImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE, expr2);
          if (elemsType2 == null) elemsType2 = FieldCallExpression.make(Prelude.ARRAY_ELEMENTS_TYPE, expr2);
          return compare(elemsType1, elemsType2, null, false);
        } else {
          if (!classCall1.isImplemented(Prelude.ARRAY_AT) && !(expr1 instanceof ArrayExpression) && !classCall2.isImplemented(Prelude.ARRAY_AT) && !(expr2 instanceof ArrayExpression)) {
            return false;
          }
          var pair1 = getSucs(length1);
          var pair2 = getSucs(length2);
          BigInteger m = pair1.proj2.min(pair2.proj2);
          if (!m.equals(BigInteger.ZERO)) {
            for (BigInteger i = BigInteger.ZERO; i.compareTo(m) < 0; i = i.add(BigInteger.ONE)) {
              IntegerExpression index = new BigIntegerExpression(i);
              if (!normalizedCompare(FunCallExpression.make(Prelude.ARRAY_INDEX, classCall1.getLevels(), Arrays.asList(expr1, index)).normalize(NormalizationMode.WHNF),
                                     FunCallExpression.make(Prelude.ARRAY_INDEX, classCall2.getLevels(), Arrays.asList(expr2, index)).normalize(NormalizationMode.WHNF), null, true)) {
                return false;
              }
            }
            return true;
          }
        }
      }
    }

    Set<? extends ClassField> fields = null;
    if (type != null) {
      ClassCallExpression classCall = type.cast(ClassCallExpression.class);
      if (classCall != null) {
        Set<ClassField> fieldsCopy = new HashSet<>();
        for (ClassField field : classCall.getDefinition().getFields()) {
          if (!classCall.isImplemented(field)) {
            fieldsCopy.add(field);
          }
        }
        fields = fieldsCopy;
      }
    }
    if (fields == null) {
      fields = classCall1.getDefinition().getFields();
      if (classCall1.getDefinition() != classCall2.getDefinition()) {
        fields = new HashSet<>(fields);
        //noinspection SuspiciousMethodCalls
        fields.retainAll(classCall2.getDefinition().getFields());
      }
    }

    for (ClassField field : fields) {
      if (!field.isProperty() && !classCall1.isImplemented(field) && !classCall2.isImplemented(field)) {
        return false;
      }
    }

    for (ClassField field : fields) {
      if (field.isProperty()) {
        continue;
      }

      AbsExpression absImpl1 = classCall1.getAbsImplementation(field);
      AbsExpression absImpl2 = classCall2.getAbsImplementation(field);
      if (absImpl1 == null || absImpl2 == null) {
        Expression impl1 = absImpl1 == null ? FieldCallExpression.make(field, expr1) : classCall1.getImplementation(field, expr1);
        Expression impl2 = absImpl2 == null ? FieldCallExpression.make(field, expr2) : classCall2.getImplementation(field, expr2);
        if (!compare(impl1, impl2, field.getType(classCall1.getLevels(field.getParentClass())).applyExpression(expr1), true)) {
          return false;
        }
      } else {
        mySubstitution.put(classCall2.getThisBinding(), classCall1.getThisBinding());
        Expression impl1 = classCall1.getAbsImplementationHere(field);
        Expression impl2 = classCall2.getAbsImplementationHere(field);
        if (impl1 == null) {
          impl1 = Objects.requireNonNull(classCall1.getDefinition().getImplementation(field)).apply(expr1, LevelSubstitution.EMPTY);
        }
        if (impl2 == null) {
          impl2 = Objects.requireNonNull(classCall2.getDefinition().getImplementation(field)).apply(expr2, LevelSubstitution.EMPTY);
        }
        boolean ok = compare(impl1, impl2, field.getType(classCall1.getLevels(field.getParentClass())).applyExpression(expr1), true);
        mySubstitution.remove(classCall2.getThisBinding());
        if (!ok) return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitNew(NewExpression expr1, Expression expr2, Expression type) {
    initResult(expr1, expr2);
    return false;
  }

  @Override
  public Boolean visitLet(LetExpression expr1, Expression expr2, Expression type) {
    initResult(expr1, expr2);
    return false;
  }

  public boolean compareLists(List<? extends Expression> list1, List<? extends Expression> list2, DependentLink link, Definition definition, ExprSubstitution substitution) {
    return compareLists(list1, list2, link, definition, substitution, false);
  }

  private boolean compareLists(List<? extends Expression> list1, List<? extends Expression> list2, DependentLink link, Definition definition, ExprSubstitution substitution, boolean skipBoxed) {
    if (list1.size() != list2.size()) {
      return false;
    }

    CMP origCMP = myCMP;
    for (int i = 0; i < list1.size(); i++) {
      if (definition instanceof DataDefinition) {
        myCMP = ((DataDefinition) definition).isCovariant(i) ? origCMP : CMP.EQ;
      }
      
      boolean oldVarsValue = myOnlySolveVars;
      try {
        myOnlySolveVars |= skipBoxed && link != null && link.getNextTyped(null).isProperty();
        boolean ok;
        if (link == null) {
          Expression type1 = list1.get(i).getType().normalize(NormalizationMode.WHNF);
          Expression type2 = list2.get(i).getType().normalize(NormalizationMode.WHNF);
          boolean isGE;
          if (type1 instanceof ClassCallExpression classCall1 && type2 instanceof ClassCallExpression classCall2) {
            isGE = classCall1.getDefinition() == classCall2.getDefinition() ? classCall2.getImplementedHere().size() > classCall1.getImplementedHere().size() : ((ClassCallExpression) type2).getDefinition().isSubClassOf(((ClassCallExpression) type1).getDefinition());
          } else {
            isGE = type2 instanceof DataCallExpression && ((DataCallExpression) type2).getDefinition() == Prelude.FIN || type1 instanceof DataCallExpression && ((DataCallExpression) type1).getDefinition() == Prelude.NAT;
          }
          myCMP = isGE ? CMP.GE : CMP.LE;
          TypecheckerState state = new TypecheckerState(null, 0, 0, null, null, null, true);
          myEquations.saveState(state);
          ok = normalizedCompare(type1, type2, Type.OMEGA, false);
          myCMP = origCMP;
          if (ok) {
            ok = compare(list1.get(i), list2.get(i), isGE ? type1 : type2, true);
          }
          if (!ok) {
            state.numberOfLevelVariables = Integer.MAX_VALUE;
            myEquations.loadState(state);
          }
        } else {
          ok = compare(list1.get(i), list2.get(i), substitution != null && link.hasNext() ? link.getTypeExpr().subst(substitution) : null, true);
        }
        if (!ok) {
          myCMP = origCMP;
          if (initResult(list1.get(i), list2.get(i))) {
            myResult.index = i;
          }
          return false;
        }
      } finally {
        myOnlySolveVars = oldVarsValue;
      }
      if (substitution != null && link != null && link.hasNext()) {
        substitution.add(link, (myCMP == CMP.LE ? list2 : list1).get(i));
        link = link.getNext();
      }
    }

    myCMP = origCMP;
    return true;
  }

  @Override
  public Boolean visitCase(CaseExpression case1, Expression expr2, Expression type) {
    CaseExpression case2 = expr2.cast(CaseExpression.class);
    if (case2 == null) {
      initResult(case1, expr2);
      return false;
    }

    if (case1.getArguments().size() != case2.getArguments().size()) {
      initResult(case1, expr2);
      return false;
    }

    if (!compareParameters(case1.getParameters(), case2.getParameters())) {
      if (myResult == null || myResult.index < 0) {
        initResult(case1, case2);
      } else {
        List<DependentLink> list1 = DependentLink.Helper.toList(case1.getParameters());
        List<DependentLink> list2 = DependentLink.Helper.toList(case2.getParameters());
        if (myResult.index < list1.size() && myResult.index < list2.size()) {
          ExprSubstitution subst1 = new ExprSubstitution();
          ExprSubstitution subst2 = new ExprSubstitution();
          myResult.wholeExpr1 = new CaseExpression(case1.isSCase(), replaceParameter(list1, myResult.wholeExpr1, subst1), case1.getResultType().subst(subst1), case1.getResultTypeLevel() == null ? null : case1.getResultTypeLevel().subst(subst1), case1.getElimBody(), case1.getArguments());
          myResult.wholeExpr2 = new CaseExpression(case2.isSCase(), replaceParameter(list2, myResult.wholeExpr2, subst2), case2.getResultType().subst(subst2), case2.getResultTypeLevel() == null ? null : case2.getResultTypeLevel().subst(subst2), case2.getElimBody(), case2.getArguments());
        }
        myResult.index = -1;
      }
      return false;
    }

    if (!compare(case1.getResultType(), case2.getResultType(), Type.OMEGA, false)) {
      if (myResult == null) {
        initResult(case1, case2);
      } else {
        myResult.wholeExpr1 = new CaseExpression(case1.isSCase(), case1.getParameters(), myResult.wholeExpr1, case1.getResultTypeLevel(), case1.getElimBody(), case1.getArguments());
        myResult.wholeExpr2 = new CaseExpression(case2.isSCase(), case2.getParameters(), myResult.wholeExpr2, case2.getResultTypeLevel(), case2.getElimBody(), case2.getArguments());
      }
      return false;
    }

    for (DependentLink link = case2.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }

    if (!compareLists(case1.getArguments(), case2.getArguments(), case1.getParameters(), null, new ExprSubstitution())) {
      if (myResult == null) {
        initResult(case1, case2);
      } else {
        if (myResult.index >= 0 && myResult.index < case1.getArguments().size()) {
          List<Expression> newArgs = new ArrayList<>(case1.getArguments());
          newArgs.set(myResult.index, myResult.wholeExpr1);
          myResult.wholeExpr1 = new CaseExpression(case1.isSCase(), case1.getParameters(), case1.getResultType(), case1.getResultTypeLevel(), case1.getElimBody(), newArgs);
        } else {
          myResult.wholeExpr1 = case1;
        }
        if (myResult.index >= 0 && myResult.index < case2.getArguments().size()) {
          List<Expression> newArgs = new ArrayList<>(case2.getArguments());
          newArgs.set(myResult.index, myResult.wholeExpr2);
          myResult.wholeExpr2 = new CaseExpression(case2.isSCase(), case2.getParameters(), case2.getResultType(), case2.getResultTypeLevel(), case2.getElimBody(), newArgs);
        } else {
          myResult.wholeExpr2 = case2;
        }
        myResult.index = -1;
      }
      return false;
    }

    return compare(case1.getElimBody(), case2.getElimBody(), type);
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expr, Expression expr2, Expression type) {
    return expr.getExpression().accept(this, expr2, type);
  }

  private boolean visitInteger(IntegerExpression expr1, Expression expr2) {
    IntegerExpression intExpr2 = expr2.cast(IntegerExpression.class);
    boolean ok;
    if (intExpr2 != null) {
      ok = expr1.isEqual(intExpr2);
    } else {
      ConCallExpression conCall2 = expr2.cast(ConCallExpression.class);
      Constructor constructor2 = conCall2 == null ? null : conCall2.getDefinition();
      if (constructor2 == null || !expr1.match(constructor2)) {
        ok = false;
      } else if (constructor2 == Prelude.ZERO) {
        return true;
      } else {
        ok = compare(expr1.pred(), conCall2.getDefCallArguments().get(0), ExpressionFactory.Nat(), false);
      }
    }
    if (!ok) {
      initResult(expr1, expr2);
    }
    return ok;
  }

  @Override
  public Boolean visitInteger(IntegerExpression expr, Expression expr2, Expression type) {
    return visitInteger(expr, expr2);
  }

  private Boolean visitTypeConstructor(TypeConstructorExpression expr1, Expression expr2, boolean correctOrder) {
    TypeConstructorExpression typeCoerce2 = expr2.cast(TypeConstructorExpression.class);
    if (typeCoerce2 == null) {
      Expression arg = TypeDestructorExpression.make(expr1.getDefinition(), expr2);
      if (!compare(correctOrder ? expr1.getArgument() : arg, correctOrder ? arg : expr1.getArgument(), expr1.getArgumentType(), true)) {
        initResult(expr1, expr2, correctOrder);
        return false;
      }
    } else {
      if (!compare((correctOrder ? expr1 : typeCoerce2).getArgument(), (correctOrder ? typeCoerce2 : expr1).getArgument(), (correctOrder ? expr1 : typeCoerce2).getArgumentType(), true)) {
        if (myResult == null) {
          initResult(expr1, expr2);
        } else {
          TypeConstructorExpression expr1_ = correctOrder ? expr1 : typeCoerce2;
          TypeConstructorExpression typeCoerce2_ = correctOrder ? typeCoerce2 : expr1;
          myResult.wholeExpr1 = TypeConstructorExpression.make(expr1_.getDefinition(), expr1_.getLevels(), expr1_.getClauseIndex(), expr1_.getClauseArguments(), myResult.wholeExpr1);
          myResult.wholeExpr2 = TypeConstructorExpression.make(typeCoerce2_.getDefinition(), typeCoerce2_.getLevels(), typeCoerce2_.getClauseIndex(), typeCoerce2_.getClauseArguments(), myResult.wholeExpr2);
        }
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitTypeConstructor(TypeConstructorExpression expr, Expression other, Expression type) {
    return visitTypeConstructor(expr, other, true);
  }

  @Override
  public Boolean visitTypeDestructor(TypeDestructorExpression expr, Expression other, Expression type) {
    TypeDestructorExpression typeCoerce2 = other.cast(TypeDestructorExpression.class);
    if (typeCoerce2 == null) {
      initResult(expr, other);
      return false;
    }
    if (!compare(expr.getArgument(), typeCoerce2.getArgument(), null, false)) {
      if (myResult == null) {
        initResult(expr, other);
      } else {
        myResult.wholeExpr1 = new TypeDestructorExpression(expr.getDefinition(), myResult.wholeExpr1);
        myResult.wholeExpr2 = new TypeDestructorExpression(typeCoerce2.getDefinition(), myResult.wholeExpr2);
      }
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitArray(ArrayExpression expr, Expression other, Expression type) {
    if (!(other instanceof ArrayExpression array2 && expr.getElements().size() == array2.getElements().size() && (expr.getTail() == null) == (array2.getTail() == null))) {
      initResult(expr, other);
      return false;
    }

    if (!compare(expr.getElementsType(), array2.getElementsType(), null, false)) {
      if (myResult == null) {
        initResult(expr, other);
      } else {
        myResult.wholeExpr1 = ArrayExpression.make(expr.getLevels(), myResult.wholeExpr1, expr.getElements(), expr.getTail());
        myResult.wholeExpr2 = ArrayExpression.make(array2.getLevels(), myResult.wholeExpr2, array2.getElements(), array2.getTail());
      }
      return false;
    }

    for (int i = 0; i < expr.getElements().size(); i++) {
      if (!compare(expr.getElements().get(i), array2.getElements().get(i), AppExpression.make(expr.getElementsType(), new SmallIntegerExpression(i), true), true)) {
        if (myResult == null) {
          initResult(expr, other);
        } else {
          List<Expression> args1 = new ArrayList<>(expr.getElements());
          List<Expression> args2 = new ArrayList<>(array2.getElements());
          args1.set(i, myResult.wholeExpr1);
          args2.set(i, myResult.wholeExpr2);
          myResult.wholeExpr1 = ArrayExpression.make(expr.getLevels(), expr.getElementsType(), args1, expr.getTail());
          myResult.wholeExpr2 = ArrayExpression.make(array2.getLevels(), array2.getElementsType(), args2, array2.getTail());
        }
        return false;
      }
    }

    if (expr.getTail() == null) {
      return true;
    }

    if (!compare(expr.getTail(), array2.getTail(), null, true)) {
      if (myResult == null) {
        initResult(expr, other);
      } else {
        myResult.wholeExpr1 = ArrayExpression.make(expr.getLevels(), expr.getElementsType(), expr.getElements(), myResult.wholeExpr1);
        myResult.wholeExpr2 = ArrayExpression.make(array2.getLevels(), array2.getElementsType(), array2.getElements(), myResult.wholeExpr2);
      }
      return false;
    }

    return true;
  }

  @Override
  public Boolean visitPath(PathExpression expr, Expression expr2, Expression type) {
    PathExpression pathExpr2 = expr2.cast(PathExpression.class);
    if (pathExpr2 == null) {
      return comparePathEta(expr, expr2, type, true);
    }
    if (!compare(expr.getArgument(), pathExpr2.getArgument(), null, false)) {
      if (myResult == null) {
        initResult(expr, expr2);
      } else {
        myResult.wholeExpr1 = new PathExpression(expr.getLevels(), expr.getArgumentType(), myResult.wholeExpr1);
        myResult.wholeExpr2 = new PathExpression(pathExpr2.getLevels(), pathExpr2.getArgumentType(), myResult.wholeExpr2);
      }
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitPEval(PEvalExpression expr, Expression other, Expression type) {
    if (!other.isInstance(PEvalExpression.class)) {
      initResult(expr, other);
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitBox(BoxExpression expr, Expression other, Expression type) {
    if (!other.isBoxed()) {
      initResult(expr, other);
      return false;
    }

    if (!(other instanceof BoxExpression)) return true;
    boolean onlySolveVars = myOnlySolveVars;
    myOnlySolveVars = true;
    compare(expr.getExpression(), ((BoxExpression) other).getExpression(), type, true);
    myOnlySolveVars = onlySolveVars;
    return true;
  }
}
