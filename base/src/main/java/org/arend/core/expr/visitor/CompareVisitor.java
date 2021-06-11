package org.arend.core.expr.visitor;

import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.binding.Binding;
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
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Pair;
import org.jetbrains.annotations.TestOnly;

import java.math.BigInteger;
import java.util.*;

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

  public CompareVisitor(Equations equations, CMP cmp, Concrete.SourceNode sourceNode) {
    mySubstitution = new HashMap<>();
    myEquations = equations;
    mySourceNode = sourceNode;
    myCMP = cmp;
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
    if (elimTree1.getSkip() != elimTree2.getSkip()) {
      return false;
    }

    if (elimTree1 instanceof LeafElimTree && elimTree2 instanceof LeafElimTree) {
      return ((LeafElimTree) elimTree1).getClauseIndex() == ((LeafElimTree) elimTree2).getClauseIndex() && Objects.equals(((LeafElimTree) elimTree1).getArgumentIndices(), ((LeafElimTree) elimTree2).getArgumentIndices());
    } else if (elimTree1 instanceof BranchElimTree && elimTree2 instanceof BranchElimTree) {
      BranchElimTree branchElimTree1 = (BranchElimTree) elimTree1;
      BranchElimTree branchElimTree2 = (BranchElimTree) elimTree2;
      if (branchElimTree1.keepConCall() != branchElimTree2.keepConCall() || branchElimTree1.getChildren().size() != branchElimTree2.getChildren().size()) {
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
    } else if (expr1 instanceof TypeCoerceExpression) {
      check = expr2 instanceof TypeCoerceExpression;
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
        return myEquations.addEquation(expr1, substitute(expr2), type, myCMP, (var1 != null ? var1 : var2).getSourceNode(), var1, var2);
      }
    }

    InferenceVariable stuckVar1 = expr1.getInferenceVariable();
    InferenceVariable stuckVar2 = expr2.getInferenceVariable();
    if (stuckVar1 != null || stuckVar2 != null) {
      return myNormalCompare && myEquations.addEquation(expr1, substitute(expr2), type, myCMP, stuckVar1 != null ? stuckVar1.getSourceNode() : stuckVar2.getSourceNode(), stuckVar1, stuckVar2);
    }

    if (expr1 instanceof FieldCallExpression && ((FieldCallExpression) expr1).getDefinition().isProperty() && expr2 instanceof FieldCallExpression && ((FieldCallExpression) expr2).getDefinition().isProperty()) {
      return true;
    }

    boolean onlySolveVars = myOnlySolveVars;
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

        if (!myOnlySolveVars) {
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

          if (!myOnlySolveVars && type1 != null && type2 != null) {
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
        return dataAndApp;
      }
      dataAndApp = checkDefCallAndApp(expr2, expr1, false);
      if (dataAndApp != null) {
        return dataAndApp;
      }
    }

    if (!myOnlySolveVars && myNormalCompare) {
      Boolean result = expr1 instanceof AppExpression && (stuck2 == null || stuck2.getInferenceVariable() == null) ? checkApp((AppExpression) expr1, expr2, true) : null;
      if (result != null) {
        return result;
      }
      result = expr2 instanceof AppExpression && (stuck1 == null || stuck1.getInferenceVariable() == null) ? checkApp((AppExpression) expr2, expr1, false) : null;
      if (result != null) {
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
    if (expr2 instanceof ConCallExpression && ((ConCallExpression) expr2).getDefinition() == Prelude.PATH_CON && !(expr1 instanceof ConCallExpression)) {
      ok = comparePathEta((ConCallExpression) expr2, expr1, false);
    } else if (expr2 instanceof LamExpression) {
      ok = visitLam((LamExpression) expr2, expr1, type, false);
    } else if (expr2 instanceof TupleExpression) {
      ok = visitTuple((TupleExpression) expr2, expr1, false);
    } else if (expr2 instanceof TypeCoerceExpression && !((TypeCoerceExpression) expr2).isFromLeftToRight()) {
      ok = visitTypeCoerce((TypeCoerceExpression) expr2, expr1, false);
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
    if (!(n1 == n2 && e1 instanceof FieldCallExpression && e2 instanceof FieldCallExpression)) return null;
    FieldCallExpression fieldCall1 = (FieldCallExpression) e1;
    FieldCallExpression fieldCall2 = (FieldCallExpression) e2;
    if (fieldCall1.getDefinition() == fieldCall2.getDefinition() && (fieldCall1.getArgument().getInferenceVariable() != null && isInstance(fieldCall2) || fieldCall2.getArgument().getInferenceVariable() != null && isInstance(fieldCall1)))
      return expr1.accept(this, expr2, type);
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
      return false;
    }

    if (stuckVar1 != null && stuckVar2 != null && myAllowEquations) {
      return myEquations.addEquation(expr1, substitute(expr2), type, myCMP, stuckVar1.getSourceNode(), stuckVar1, stuckVar2);
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
      return false;
    }

    if (!compare(fun1, fun2, null, false)) {
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
        return false;
      }
    }

    return true;
  }

  private Boolean comparePathEta(ConCallExpression conCall1, Expression expr2, boolean correctOrder) {
    SingleDependentLink param = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
    ReferenceExpression paramRef = new ReferenceExpression(param);
    List<Expression> args = new ArrayList<>(5);
    for (Expression arg : conCall1.getDataTypeArguments()) {
      args.add(correctOrder ? arg : substitute(arg));
    }
    args.add(expr2);
    args.add(paramRef);
    Sort sort = new Sort(conCall1.getPLevel(), Level.INFINITY);
    expr2 = new LamExpression(sort, param, FunCallExpression.make(Prelude.AT, conCall1.getLevels(), args));
    Expression type = new PiExpression(sort, param, AppExpression.make(conCall1.getDataTypeArguments().get(0), paramRef, true));
    return correctOrder ? compare(conCall1.getDefCallArguments().get(0), expr2, type, true) : compare(expr2, conCall1.getDefCallArguments().get(0), type, true);
  }

  private boolean compareDef(DefCallExpression expr1, DefCallExpression expr2) {
    if (expr2 == null || expr1.getDefinition() != expr2.getDefinition()) {
      return false;
    }
    UniverseKind universeKind = expr1.getUniverseKind();
    if (universeKind == UniverseKind.NO_UNIVERSES) {
      return true;
    }
    CMP cmp = universeKind == UniverseKind.ONLY_COVARIANT ? myCMP : CMP.EQ;
    return LevelPair.compare(expr1.getLevels(), expr2.getLevels(), cmp, myNormalCompare ? myEquations : DummyEquations.getInstance(), mySourceNode);
  }

  private Boolean visitDefCall(DefCallExpression expr1, Expression expr2) {
    DefCallExpression defCall2 = expr2.cast(DefCallExpression.class);
    if (!compareDef(expr1, defCall2)) {
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

  @Override
  public Boolean visitFunCall(FunCallExpression expr1, Expression expr2, Expression type) {
    return visitDefCall(expr1, expr2);
  }

  @Override
  public Boolean visitConCall(ConCallExpression expr1, Expression expr2, Expression type) {
    ConCallExpression conCall2 = expr2.cast(ConCallExpression.class);
    if (expr1.getDefinition() == Prelude.PATH_CON && conCall2 == null) {
      return comparePathEta(expr1, expr2, true);
    }

    Expression it = expr1;
    while (true) {
      expr1 = (ConCallExpression) it;
      if (expr2 instanceof IntegerExpression) {
        return visitInteger((IntegerExpression) expr2, expr1) || myOnlySolveVars;
      }
      conCall2 = expr2.cast(ConCallExpression.class);
      if (!compareDef(expr1, conCall2)) {
        return myOnlySolveVars;
      }

      int recursiveParam = expr1.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        return compareLists(expr1.getDefCallArguments(), conCall2.getDefCallArguments(), expr1.getDefinition().getParameters(), expr1.getDefinition(), null) || myOnlySolveVars;
      }

      for (int i = 0; i < expr1.getDefCallArguments().size(); i++) {
        if (i != recursiveParam && !compare(expr1.getDefCallArguments().get(i), conCall2.getDefCallArguments().get(i), null, true)) {
          return myOnlySolveVars;
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
        return myNormalCompare && myEquations.addEquation(it, infRefExpr2, type, myCMP, infRefExpr2.getVariable().getSourceNode(), it.getStuckInferenceVariable(), infRefExpr2.getVariable()) || myOnlySolveVars;
      }
      InferenceVariable stuckVar2 = expr2.getStuckInferenceVariable();
      if (stuckVar2 != null && (!myNormalCompare || myEquations == DummyEquations.getInstance())) {
        return myOnlySolveVars;
      }
      if (!myNormalCompare || !myNormalize || myOnlySolveVars && stuckVar2 != null) {
        return true;
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
        return myNormalCompare && myEquations.addEquation(it, substitute(expr2), type, myCMP, stuckVar2.getSourceNode(), null, stuckVar2) || myOnlySolveVars;
      }
    }

    return compare(it, expr2, null, true);
  }

  @Override
  public Boolean visitDataCall(DataCallExpression expr1, Expression expr2, Expression type) {
    if (expr1.getDefinition() == Prelude.FIN || expr1.getDefinition() == Prelude.NAT) {
      DataCallExpression dataCall2 = expr2.cast(DataCallExpression.class);
      return dataCall2 != null && (dataCall2.getDefinition() == Prelude.FIN || dataCall2.getDefinition() == Prelude.NAT) && checkFin(myCMP == CMP.GE ? dataCall2 : expr1, myCMP == CMP.GE ? expr1 : dataCall2, myCMP != CMP.GE);
    }
    return visitDefCall(expr1, expr2);
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
    DefCallExpression defCall1 = expr1.cast(DefCallExpression.class);
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
          if (!field.getReferable().isParameterField()) {
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
        dataParams = classCall1.getClassFieldParameters();
        oldDataArgs = classArgs;
      }

      List<? extends Expression> oldList = oldDataArgs.subList(oldDataArgs.size() - args.size(), oldDataArgs.size());
      if (!compareLists(correctOrder ? oldList : args, correctOrder ? args : oldList, dataParams, defCall1.getDefinition(), new ExprSubstitution())) {
        return false;
      }

      Expression lam;
      Sort codSort;
      List<SingleDependentLink> params = new ArrayList<>();
      if (classCall1 == null) {
        int numberOfOldArgs = oldDataArgs.size() - args.size();
        for (int i = 0; i < numberOfOldArgs; i++) {
          dataParams = dataParams.getNext();
        }
        List<Expression> newDataArgs = new ArrayList<>(oldDataArgs.subList(0, numberOfOldArgs));
        lam = defCall1.getDefinition().getDefCall(defCall1.getLevels(), newDataArgs);
        codSort = defCall1 instanceof DataCallExpression ? ((DataCallExpression) defCall1).getDefinition().getSort() : ((UniverseExpression) ((FunCallExpression) defCall1).getDefinition().getResultType()).getSort();

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
      } else {
        Map<ClassField, Expression> implementations = new HashMap<>();
        codSort = classCall1.getSort();
        ClassCallExpression classCall = new ClassCallExpression(classCall1.getDefinition(), classCall1.getLevels(), implementations, codSort, classCall1.getUniverseKind());
        int i = 0;
        for (ClassField field : classCall1.getDefinition().getFields()) {
          if (!classCall1.getDefinition().isImplemented(field)) {
            if (i < oldDataArgs.size() - args.size()) {
              implementations.put(field, classCall1.getImplementationHere(field, new ReferenceExpression(classCall.getThisBinding())));
              i++;
            } else {
              PiExpression piType = classCall1.getDefinition().getFieldType(field, classCall1.getLevels());
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
          impl1 = absImpl1.apply(new ReferenceExpression(binding), classCall1.getLevels());
        }
      }
      if (impl1 == null) {
        return false;
      }
      if (!field.isCovariant()) {
        myCMP = CMP.EQ;
      }
      mySubstitution.put(classCall2.getThisBinding(), binding);
      boolean ok = compare(correctOrder ? impl1 : impl2, correctOrder ? impl2 : impl1, field.getType(classCall2.getLevels()).applyExpression(new ReferenceExpression(binding)), true);
      mySubstitution.remove(classCall2.getThisBinding());
      if (!ok) {
        return false;
      }
      myCMP = origCMP;
    }
    return true;
  }

  private boolean checkClassCallLevels(ClassCallExpression classCall1, ClassCallExpression classCall2, CMP onSuccess, CMP onFailure) {
    ReferenceExpression thisExpr = new ReferenceExpression(classCall1.getThisBinding());
    boolean ok = true;
    for (Map.Entry<ClassField, AbsExpression> entry : classCall1.getDefinition().getImplemented()) {
      if (!entry.getKey().isProperty() && entry.getKey().getUniverseKind() != UniverseKind.NO_UNIVERSES && classCall2.getDefinition().getFields().contains(entry.getKey()) && !classCall2.isImplemented(entry.getKey())) {
        Expression type = entry.getValue().apply(thisExpr, classCall1.getLevels()).normalize(NormalizationMode.WHNF).getType();
        if (type == null) {
          ok = false;
          break;
        }
        CMP origCmp = myCMP;
        myCMP = CMP.LE;
        ok = compare(type, classCall1.getDefinition().getFieldType(entry.getKey(), classCall2.getLevels(), thisExpr), Type.OMEGA, false);
        myCMP = origCmp;
        if (!ok) {
          break;
        }
      }
    }
    if (ok) {
      for (Map.Entry<ClassField, Expression> entry : classCall1.getImplementedHere().entrySet()) {
        if (!entry.getKey().isProperty() && entry.getKey().getUniverseKind() != UniverseKind.NO_UNIVERSES && classCall2.getDefinition().getFields().contains(entry.getKey()) && !classCall2.isImplemented(entry.getKey())) {
          Expression type = entry.getValue().normalize(NormalizationMode.WHNF).getType();
          if (type == null) {
            ok = false;
            break;
          }
          CMP origCmp = myCMP;
          myCMP = CMP.LE;
          ok = compare(type, classCall1.getDefinition().getFieldType(entry.getKey(), classCall2.getLevels(), thisExpr), Type.OMEGA, false);
          myCMP = origCmp;
          if (!ok) {
            break;
          }
        }
      }
    }

    if (ok) {
      return onSuccess == null || myNormalCompare && LevelPair.compare(classCall1.getLevels(), classCall2.getLevels(), onSuccess, myEquations, mySourceNode);
    } else {
      return myNormalCompare && LevelPair.compare(classCall1.getLevels(), classCall2.getLevels(), onFailure, myEquations, mySourceNode);
    }
  }

  public boolean compareClassCallLevels(ClassCallExpression classCall1, ClassCallExpression classCall2) {
    UniverseKind kind1 = classCall1.getUniverseKind();
    UniverseKind kind2 = classCall2.getUniverseKind();
    if (kind1 == UniverseKind.NO_UNIVERSES && kind2 == UniverseKind.NO_UNIVERSES) {
      return true;
    }
    if (myCMP == CMP.EQ || kind1 == kind2) {
      return LevelPair.compare(classCall1.getLevels(), classCall2.getLevels(), kind1 == UniverseKind.ONLY_COVARIANT ? myCMP : CMP.EQ, myNormalCompare ? myEquations : DummyEquations.getInstance(), mySourceNode);
    }
    if (!LevelPair.compare(classCall1.getLevels(), classCall2.getLevels(), myCMP, DummyEquations.getInstance(), mySourceNode)) {
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
      return false;
    }

    if (!compareClassCallLevels(expr1, classCall2)) {
      return false;
    }

    if (myCMP == CMP.LE) {
      if (!expr1.getDefinition().isSubClassOf(classCall2.getDefinition())) {
        return false;
      }
      return checkSubclassImpl(expr1, classCall2, true);
    }

    if (myCMP == CMP.GE) {
      if (!classCall2.getDefinition().isSubClassOf(expr1.getDefinition())) {
        return false;
      }
      return checkSubclassImpl(classCall2, expr1, false);
    }

    return expr1.getDefinition() == classCall2.getDefinition() && expr1.getImplementedHere().size() == classCall2.getImplementedHere().size() && checkSubclassImpl(expr1, classCall2, true);
  }

  private Binding substBinding(Binding binding) {
    Binding subst = mySubstitution.get(binding);
    return subst == null ? binding : subst;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr1, Expression expr2, Expression type) {
    ReferenceExpression ref2 = expr2.cast(ReferenceExpression.class);
    return ref2 != null && substBinding(ref2.getBinding()) == expr1.getBinding();
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr1, Expression expr2, Expression type) {
    if (expr1.getSubstExpression() == null) {
      InferenceReferenceExpression infRefExpr2 = expr2.cast(InferenceReferenceExpression.class);
      return infRefExpr2 != null && infRefExpr2.getVariable() == expr1.getVariable();
    } else {
      return expr1.getSubstExpression().accept(this, expr2, type);
    }
  }

  @Override
  public Boolean visitSubst(SubstExpression expr, Expression expr2, Expression type) {
    if (expr.getExpression() instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr.getExpression()).getVariable() != null) {
      return myEquations.addEquation(expr, substitute(expr2), type, myCMP, mySourceNode, ((InferenceReferenceExpression) expr.getExpression()).getVariable(), expr2.getStuckInferenceVariable());
    } else {
      return expr.getSubstExpression().accept(this, expr2, type);
    }
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
    return result;
  }

  @Override
  public Boolean visitLam(LamExpression expr1, Expression expr2, Expression type) {
    return visitLam(expr1, expr2, type, true);
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2, Expression type) {
    PiExpression piExpr2 = expr2.cast(PiExpression.class);
    if (piExpr2 == null) {
      return false;
    }

    CMP origCMP = myCMP;
    myCMP = CMP.EQ;
    if (!compare(expr1.getParameters().getTypeExpr(), piExpr2.getParameters().getTypeExpr(), Type.OMEGA, false)) {
      return false;
    }

    SingleDependentLink link1 = expr1.getParameters(), link2 = piExpr2.getParameters();
    for (; link1.hasNext() && link2.hasNext(); link1 = link1.getNext(), link2 = link2.getNext()) {
      if (link1.isExplicit() != link2.isExplicit()) {
        return false;
      }
      mySubstitution.put(link2, link1);
    }

    myCMP = origCMP;
    if (!compare(link1.hasNext() ? new PiExpression(expr1.getResultSort(), link1, expr1.getCodomain()) : expr1.getCodomain(), link2.hasNext() ? new PiExpression(piExpr2.getResultSort(), link2, piExpr2.getCodomain()) : piExpr2.getCodomain(), Type.OMEGA, false)) {
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
      if (!compare(list1.get(i).getTypeExpr(), list2.get(i).getTypeExpr(), Type.OMEGA, false)) {
        for (int j = 0; j < i; j++) {
          mySubstitution.remove(list2.get(j));
        }
        myCMP = origCMP;
        return false;
      }
      mySubstitution.put(list2.get(i), list1.get(i));
      myCMP = origCMP;
    }

    return true;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr1, Expression expr2, Expression type) {
    UniverseExpression universe2 = expr2.cast(UniverseExpression.class);
    return universe2 != null && Sort.compare(expr1.getSort(), universe2.getSort(), myCMP, myNormalCompare ? myEquations : DummyEquations.getInstance(), mySourceNode);
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
      return false;
    }

    TupleExpression tuple2 = expr2.cast(TupleExpression.class);
    if (tuple2 != null) {
      return correctOrder ? compareLists(expr1.getFields(), tuple2.getFields(), expr1.getSigmaType().getParameters(), null, new ExprSubstitution()) : compareLists(tuple2.getFields(), expr1.getFields(), tuple2.getSigmaType().getParameters(), null, new ExprSubstitution());
    } else {
      List<Expression> args2 = new ArrayList<>(expr1.getFields().size());
      for (int i = 0; i < expr1.getFields().size(); i++) {
        args2.add(ProjExpression.make(expr2, i));
      }
      return correctOrder ? compareLists(expr1.getFields(), args2, expr1.getSigmaType().getParameters(), null, new ExprSubstitution()) : compareLists(args2, expr1.getFields(), expr1.getSigmaType().getParameters(), null, new ExprSubstitution());
    }
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr1, Expression expr2, Expression type) {
    SigmaExpression sigma2 = expr2.cast(SigmaExpression.class);
    if (sigma2 == null) {
      return false;
    }
    if (!compareParameters(expr1.getParameters(), sigma2.getParameters())) {
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
    return proj2 != null && expr1.getField() == proj2.getField() && compare(expr1.getExpression(), proj2.getExpression(), null, true);
  }

  private boolean compareClassInstances(Expression expr1, ClassCallExpression classCall1, Expression expr2, ClassCallExpression classCall2, Expression type) {
    if (expr1 instanceof ArrayExpression && expr2 instanceof ArrayExpression) return false;
    if (classCall1.getDefinition() == Prelude.ARRAY && classCall2.getDefinition() == Prelude.ARRAY) {
      Expression length1 = classCall1.getImplementationHere(Prelude.ARRAY_LENGTH, expr1);
      Expression length2 = classCall2.getImplementationHere(Prelude.ARRAY_LENGTH, expr1);
      if (length1 != null && length2 != null) {
        length1 = length1.normalize(NormalizationMode.WHNF);
        length2 = length2.normalize(NormalizationMode.WHNF);
        if (length1 instanceof IntegerExpression && ((IntegerExpression) length1).isZero() && length2 instanceof IntegerExpression && ((IntegerExpression) length2).isZero()) {
          Expression elemsType1 = classCall1.getImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE, expr1);
          if (elemsType1 == null) elemsType1 = FieldCallExpression.make(Prelude.ARRAY_ELEMENTS_TYPE, classCall1.getLevels(), expr1);
          Expression elemsType2 = classCall2.getImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE, expr2);
          if (elemsType2 == null) elemsType2 = FieldCallExpression.make(Prelude.ARRAY_ELEMENTS_TYPE, classCall2.getLevels(), expr2);
          return compare(elemsType1, elemsType2, ExpressionFactory.Nat(), false);
        } else {
          Expression at1 = classCall1.getImplementationHere(Prelude.ARRAY_AT, expr1);
          Expression at2 = classCall2.getImplementationHere(Prelude.ARRAY_AT, expr2);
          if (at1 == null && !(expr1 instanceof ArrayExpression) && at2 == null && !(expr2 instanceof ArrayExpression)) {
            return false;
          }
          var pair1 = getSucs(length1);
          var pair2 = getSucs(length2);
          BigInteger m = pair1.proj2.min(pair2.proj2);
          if (!m.equals(BigInteger.ZERO)) {
            Expression elementsType = classCall1.getImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE, expr1);
            if (elementsType == null) elementsType = classCall2.getImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE, expr2);
            for (BigInteger i = BigInteger.ZERO; i.compareTo(m) < 0; i = i.add(BigInteger.ONE)) {
              IntegerExpression index = new BigIntegerExpression(i);
              if (!normalizedCompare(FunCallExpression.make(Prelude.ARRAY_INDEX, classCall1.getLevels(), Arrays.asList(expr1, index)).normalize(NormalizationMode.WHNF), FunCallExpression.make(Prelude.ARRAY_INDEX, classCall2.getLevels(), Arrays.asList(expr2, index)).normalize(NormalizationMode.WHNF), elementsType, true)) {
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
      if (absImpl1 != null && absImpl1 == classCall2.getAbsImplementation(field)) {
        return true;
      }
      Expression impl1 = classCall1.getImplementation(field, expr1);
      Expression impl2 = classCall2.getImplementation(field, expr2);
      if (impl1 == null) {
        impl1 = FieldCallExpression.make(field, classCall1.getLevels(), expr1);
      }
      if (impl2 == null) {
        impl2 = FieldCallExpression.make(field, classCall2.getLevels(), expr2);
      }
      if (!compare(impl1, impl2, field.getType(classCall1.getLevels()).applyExpression(expr1), true)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitNew(NewExpression expr1, Expression expr2, Expression type) {
    return false;
  }

  @Override
  public Boolean visitLet(LetExpression expr1, Expression expr2, Expression type) {
    return false;
  }

  public boolean compareLists(List<? extends Expression> list1, List<? extends Expression> list2, DependentLink link, Definition definition, ExprSubstitution substitution) {
    if (list1.size() != list2.size()) {
      return false;
    }

    CMP origCMP = myCMP;
    for (int i = 0; i < list1.size(); i++) {
      if (definition instanceof DataDefinition) {
        myCMP = ((DataDefinition) definition).isCovariant(i) ? origCMP : CMP.EQ;
      }
      if (!compare(list1.get(i), list2.get(i), substitution != null && link.hasNext() ? link.getTypeExpr().subst(substitution) : null, true)) {
        myCMP = origCMP;
        return false;
      }
      if (substitution != null && link.hasNext()) {
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
      return false;
    }

    if (case1.getArguments().size() != case2.getArguments().size()) {
      return false;
    }

    if (!compareParameters(case1.getParameters(), case2.getParameters())) {
      return false;
    }

    if (!compare(case1.getResultType(), case2.getResultType(), Type.OMEGA, false)) {
      return false;
    }

    for (DependentLink link = case2.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }

    if (!compareLists(case1.getArguments(), case2.getArguments(), case1.getParameters(), null, new ExprSubstitution())) {
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
    if (intExpr2 != null) {
      return expr1.isEqual(intExpr2);
    }

    ConCallExpression conCall2 = expr2.cast(ConCallExpression.class);
    Constructor constructor2 = conCall2 == null ? null : conCall2.getDefinition();
    if (constructor2 == null || !expr1.match(constructor2)) {
      return false;
    }
    if (constructor2 == Prelude.ZERO) {
      return true;
    }
    return compare(expr1.pred(), conCall2.getDefCallArguments().get(0), ExpressionFactory.Nat(), false);
  }

  @Override
  public Boolean visitInteger(IntegerExpression expr, Expression expr2, Expression type) {
    return visitInteger(expr, expr2);
  }

  private Boolean visitTypeCoerce(TypeCoerceExpression expr1, Expression expr2, boolean correctOrder) {
    TypeCoerceExpression typeCoerce2 = expr2.cast(TypeCoerceExpression.class);
    if (typeCoerce2 == null || typeCoerce2.isFromLeftToRight()) {
      Expression arg = TypeCoerceExpression.make(expr1.getDefinition(), expr1.getLevels(), expr1.getClauseIndex(), expr1.getClauseArguments(), expr2, true);
      return compare(correctOrder ? expr1.getArgument() : arg, correctOrder ? arg : expr1.getArgument(), expr1.getArgumentType(), true);
    } else {
      return compare((correctOrder ? expr1 : typeCoerce2).getArgument(), (correctOrder ? typeCoerce2 : expr1).getArgument(), (correctOrder ? expr1 : typeCoerce2).getArgumentType(), true);
    }
  }

  @Override
  public Boolean visitTypeCoerce(TypeCoerceExpression expr, Expression other, Expression type) {
    if (expr.isFromLeftToRight()) {
      TypeCoerceExpression typeCoerce2 = other.cast(TypeCoerceExpression.class);
      if (typeCoerce2 == null) return false;
      if (typeCoerce2.isFromLeftToRight()) {
        return compare(expr.getArgumentType(), typeCoerce2.getArgumentType(), Type.OMEGA, false) && compare(expr.getArgument(), typeCoerce2.getArgument(), expr.getArgumentType(), true);
      } else {
        return compare(TypeCoerceExpression.make(typeCoerce2.getDefinition(), typeCoerce2.getLevels(), typeCoerce2.getClauseIndex(), typeCoerce2.getClauseArguments(), expr, true), typeCoerce2.getArgument(), expr.getArgumentType(), true);
      }
    } else {
      return visitTypeCoerce(expr, other, true);
    }
  }

  @Override
  public Boolean visitArray(ArrayExpression expr, Expression other, Expression type) {
    if (!(other instanceof ArrayExpression)) {
      return false;
    }

    ArrayExpression array2 = (ArrayExpression) other;
    if (!(compare(expr.getElementsType(), array2.getElementsType(), Type.OMEGA, false) && expr.getElements().size() == array2.getElements().size() && (expr.getTail() == null) == (array2.getTail() == null))) {
      return false;
    }

    for (int i = 0; i < expr.getElements().size(); i++) {
      if (!compare(expr.getElements().get(i), array2.getElements().get(i), expr.getElementsType(), true)) {
        return false;
      }
    }
    return expr.getTail() == null || compare(expr.getTail(), array2.getTail(), null, true);
  }

  @Override
  public Boolean visitPEval(PEvalExpression expr, Expression other, Expression type) {
    return other.isInstance(PEvalExpression.class);
  }
}
