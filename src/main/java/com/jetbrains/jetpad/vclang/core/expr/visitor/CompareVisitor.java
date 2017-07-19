package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.internal.ReadonlyFieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.EtaNormalization;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.FieldCall;

public class CompareVisitor extends BaseExpressionVisitor<Expression, Boolean> {
  private final Map<Binding, Binding> mySubstitution;
  private final Equations myEquations;
  private final Abstract.SourceNode mySourceNode;
  private Equations.CMP myCMP;

  private CompareVisitor(Equations equations, Equations.CMP cmp, Abstract.SourceNode sourceNode) {
    mySubstitution = new HashMap<>();
    myEquations = equations;
    mySourceNode = sourceNode;
    myCMP = cmp;
  }

  private CompareVisitor(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp, Abstract.SourceNode sourceNode) {
    mySubstitution = substitution;
    myEquations = equations;
    mySourceNode = sourceNode;
    myCMP = cmp;
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, Expression expr1, Expression expr2, Abstract.SourceNode sourceNode) {
    return new CompareVisitor(equations, cmp, sourceNode).compare(expr1.normalize(NormalizeVisitor.Mode.NF), expr2.normalize(NormalizeVisitor.Mode.NF));
  }

  public static boolean compare(Equations equations, ElimTree tree1, ElimTree tree2, Abstract.SourceNode sourceNode) {
    return new CompareVisitor(equations, Equations.CMP.EQ, sourceNode).compare(tree1, tree2);
  }

  private Boolean compare(ElimTree elimTree1, ElimTree elimTree2) {
    if (elimTree1 == elimTree2) {
      return true;
    }
    if (!compareParameters(DependentLink.Helper.toList(elimTree1.getParameters()), DependentLink.Helper.toList(elimTree2.getParameters()))) {
      return false;
    }

    boolean ok = false;
    if (elimTree1 instanceof LeafElimTree && elimTree2 instanceof LeafElimTree) {
      ok = compare(((LeafElimTree) elimTree1).getExpression(), ((LeafElimTree) elimTree2).getExpression());
    } else
    if (elimTree1 instanceof BranchElimTree && elimTree2 instanceof BranchElimTree) {
      BranchElimTree branchElimTree1 = (BranchElimTree) elimTree1;
      BranchElimTree branchElimTree2 = (BranchElimTree) elimTree2;
      if (branchElimTree1.getChildren().size() == branchElimTree2.getChildren().size()) {
        ok = true;
        for (Map.Entry<Constructor, ElimTree> entry : branchElimTree1.getChildren()) {
          ElimTree elimTree = branchElimTree2.getChild(entry.getKey());
          if (elimTree == null) {
            ok = false;
            break;
          }
          ok = compare(entry.getValue(), elimTree);
          if (!ok) {
            break;
          }
        }
      }
    }

    for (DependentLink link = elimTree1.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }
    return ok;
  }

  private Boolean compare(Expression expr1, Expression expr2) {
    if (expr1 == expr2 || expr1.isInstance(ErrorExpression.class) || expr2.isInstance(ErrorExpression.class)) {
      return true;
    }

    expr1 = EtaNormalization.normalize(expr1);
    expr2 = EtaNormalization.normalize(expr2);

    while (expr1.isInstance(InferenceReferenceExpression.class) && expr1.cast(InferenceReferenceExpression.class).getSubstExpression() != null || expr1.isInstance(OfTypeExpression.class)) {
      if (expr1.isInstance(OfTypeExpression.class)) {
        expr1 = expr1.cast(OfTypeExpression.class).getExpression();
      } else {
        expr1 = expr1.cast(InferenceReferenceExpression.class).getSubstExpression();
      }
    }
    while (expr2.isInstance(InferenceReferenceExpression.class) && expr2.cast(InferenceReferenceExpression.class).getSubstExpression() != null || expr2.isInstance(OfTypeExpression.class)) {
      if (expr2.isInstance(OfTypeExpression.class)) {
        expr2 = expr2.cast(OfTypeExpression.class).getExpression();
      } else {
        expr2 = expr2.cast(InferenceReferenceExpression.class).getSubstExpression();
      }
    }

    if (expr2.isInstance(InferenceReferenceExpression.class)) {
      return compareInferenceReference(expr2.cast(InferenceReferenceExpression.class), expr1, false);
    }
    if (expr1.isInstance(InferenceReferenceExpression.class)) {
      return compareInferenceReference(expr1.cast(InferenceReferenceExpression.class), expr2, true);
    }

    FieldCallExpression fieldCall1 = expr1.checkedCast(FieldCallExpression.class);
    if (fieldCall1 != null) {
      InferenceReferenceExpression ref1 = fieldCall1.getExpression().checkedCast(InferenceReferenceExpression.class);
      if (ref1 != null && ref1.getSubstExpression() == null) {
        InferenceVariable variable = ref1.getVariable();
        return myEquations.add(expr1, expr2, myCMP, variable.getSourceNode(), variable);
      }
    }

    FieldCallExpression fieldCall2 = expr2.checkedCast(FieldCallExpression.class);
    if (fieldCall2 != null) {
      InferenceReferenceExpression ref2 = fieldCall2.getExpression().checkedCast(InferenceReferenceExpression.class);
      if (ref2 != null && ref2.getSubstExpression() == null) {
        InferenceVariable variable = ref2.getVariable();
        return myEquations.add(expr1, expr2, myCMP, variable.getSourceNode(), variable);
      }
    }

    NewExpression new1 = expr1.checkedCast(NewExpression.class);
    NewExpression new2 = expr2.checkedCast(NewExpression.class);
    if (new1 != null && new2 == null) {
      return new1.accept(this, expr2);
    }
    if (new2 != null && new1 == null) {
      myCMP = myCMP.not();
      return new2.accept(this, expr1);
    }

    return expr1.accept(this, expr2);
  }

  // TODO: should we check other stuck terms?
  private static InferenceVariable checkIsInferVar(Expression expr) {
    while (expr.isInstance(AppExpression.class)) {
      expr = expr.cast(AppExpression.class).getFunction();
    }
    InferenceReferenceExpression ref = expr.checkedCast(InferenceReferenceExpression.class);
    return ref != null && ref.getSubstExpression() == null ? ref.getVariable() : null;
  }

  private boolean checkIsInferVar(Expression fun, Expression expr1, Expression expr2) {
    InferenceVariable binding = checkIsInferVar(fun);
    return binding != null && myEquations.add(expr1.subst(getSubstitution()), expr2, myCMP, binding.getSourceNode(), binding);
  }

  private ExprSubstitution getSubstitution() {
    ExprSubstitution substitution = new ExprSubstitution();
    for (Map.Entry<Binding, Binding> entry : mySubstitution.entrySet()) {
      substitution.add(entry.getKey(), new ReferenceExpression(entry.getValue()));
    }
    return substitution;
  }

  @Override
  public Boolean visitApp(AppExpression expr1, Expression expr2) {
    List<Expression> args1 = new ArrayList<>();
    Expression fun1 = expr1;
    while (fun1.isInstance(AppExpression.class)) {
      args1.add(fun1.cast(AppExpression.class).getArgument());
      fun1 = fun1.cast(AppExpression.class).getFunction();
    }
    if (checkIsInferVar(fun1, expr1, expr2)) {
      return true;
    }

    List<Expression> args2 = new ArrayList<>();
    Expression fun2 = expr2;
    while (fun2.isInstance(AppExpression.class)) {
      args2.add(fun2.cast(AppExpression.class).getArgument());
      fun2 = fun2.cast(AppExpression.class).getFunction();
    }
    if (checkIsInferVar(fun2, expr1, expr2)) {
      return true;
    }

    if (args1.size() != args2.size()) {
      return false;
    }

    Equations.CMP cmp = myCMP;
    myCMP = Equations.CMP.EQ;
    if (!compare(fun1, fun2)) {
      return false;
    }
    Collections.reverse(args1);
    Collections.reverse(args2);

    int i = 0;
    if (fun1.isInstance(DataCallExpression.class) && fun2.isInstance(DataCallExpression.class)) {
      if (args1.isEmpty()) {
        myCMP = cmp;
        return true;
      }
      if (fun1.cast(DataCallExpression.class).getDefinition().getThisClass() != null) {
        if (!compare(args1.get(i), args2.get(i))) {
          return false;
        }
        if (++i >= args1.size()) {
          myCMP = cmp;
          return true;
        }
      }
    }

    for (; i < args1.size(); i++) {
      if (!compare(args1.get(i), args2.get(i))) {
        return false;
      }
    }

    myCMP = cmp;
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr1, Expression expr2) {
    DefCallExpression defCall2 = expr2.checkedCast(DefCallExpression.class);
    if (defCall2 == null || expr1.getDefinition() != defCall2.getDefinition() || expr1.getDefCallArguments().size() != defCall2.getDefCallArguments().size()) {
      return false;
    }
    for (int i = 0; i < expr1.getDefCallArguments().size(); i++) {
      if (!compare(expr1.getDefCallArguments().get(i), defCall2.getDefCallArguments().get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean checkSubclassImpl(ReadonlyFieldSet fieldSet1, ClassCallExpression classCall2) {
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : classCall2.getImplementedHere()) {
      FieldSet.Implementation impl1 = fieldSet1.getImplementation(entry.getKey());
      if (impl1 == null) return false;
      Equations.CMP oldCMP = myCMP;
      myCMP = Equations.CMP.EQ;
      if (!compare(impl1.term, entry.getValue().term)) return false;
      myCMP = oldCMP;
    }
    return true;
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr1, Expression expr2) {
    if (!expr2.isInstance(ClassCallExpression.class)) {
      return false;
    }
    ClassCallExpression classCall2 = expr2.cast(ClassCallExpression.class);

    boolean subclass2of1Test = myCMP.equals(Equations.CMP.LE) || classCall2.getDefinition().isSubClassOf(expr1.getDefinition());
    boolean subclass1of2Test = myCMP.equals(Equations.CMP.GE) || expr1.getDefinition().isSubClassOf(classCall2.getDefinition());
    if (!(subclass1of2Test && subclass2of1Test)) {
      return false;
    }

    ReadonlyFieldSet fieldSet1 = expr1.getFieldSet();
    ReadonlyFieldSet fieldSet2 = classCall2.getFieldSet();
    boolean implAllOf1Test = myCMP.equals(Equations.CMP.LE) || checkSubclassImpl(fieldSet2, expr1);
    boolean implAllOf2Test = myCMP.equals(Equations.CMP.GE) || checkSubclassImpl(fieldSet1, classCall2);
    return implAllOf1Test && implAllOf2Test;
  }

  private boolean compareInferenceReference(InferenceReferenceExpression expr1, Expression expr2, boolean first) {
    //noinspection SimplifiableIfStatement
    if (expr2.isInstance(InferenceReferenceExpression.class) && expr1.getVariable() == expr2.cast(InferenceReferenceExpression.class).getVariable()) {
      return true;
    }

    return myEquations.add(expr1, expr2.subst(getSubstitution()), first ? myCMP : myCMP.not(), expr1.getVariable().getSourceNode(), expr1.getVariable());
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr1, Expression expr2) {
    if (!expr2.isInstance(ReferenceExpression.class)) {
      return false;
    }

    Binding binding1 = expr1.getBinding();
    Binding subst1 = mySubstitution.get(binding1);
    if (subst1 != null) {
      binding1 = subst1;
    }
    return binding1 == expr2.cast(ReferenceExpression.class).getBinding();
  }

  @Override
  public Boolean visitInferenceReference(InferenceReferenceExpression expr1, Expression expr2) {
    if (expr1.getSubstExpression() != null) {
      return compare(expr1.getSubstExpression(), expr2);
    } else {
      return compareInferenceReference(expr1, expr2, true);
    }
  }

  @Override
  public Boolean visitLam(LamExpression expr1, Expression expr2) {
    List<DependentLink> params1 = new ArrayList<>();
    List<DependentLink> params2 = new ArrayList<>();
    Expression body1 = expr1.getLamParameters(params1);
    Expression body2 = expr2.getLamParameters(params2);
    if (params1.size() != params2.size()) {
      return false;
    }

    Map<Binding, Binding> substitution = new HashMap<>(mySubstitution);
    for (int i = 0; i < params1.size(); i++) {
      substitution.put(params1.get(i), params2.get(i));
    }
    return new CompareVisitor(substitution, myEquations, Equations.CMP.EQ, mySourceNode).compare(body1, body2);
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2) {
    if (!expr2.isInstance(PiExpression.class)) {
      return false;
    }
    PiExpression piExpr2 = expr2.cast(PiExpression.class);

    Equations.CMP oldCMP = myCMP;
    myCMP = Equations.CMP.EQ;
    if (!compare(expr1.getParameters().getTypeExpr(), piExpr2.getParameters().getTypeExpr())) {
      return false;
    }
    myCMP = oldCMP;

    SingleDependentLink link1 = expr1.getParameters(), link2 = piExpr2.getParameters();
    for (; link1.hasNext() && link2.hasNext(); link1 = link1.getNext(), link2 = link2.getNext()) {
      mySubstitution.put(link1, link2);
    }
    if (!compare(link1.hasNext() ? new PiExpression(expr1.getResultSort(), link1, expr1.getCodomain()) : expr1.getCodomain(), link2.hasNext() ? new PiExpression(piExpr2.getResultSort(), link2, piExpr2.getCodomain()) : piExpr2.getCodomain())) {
      return false;
    }

    for (DependentLink link = expr1.getParameters(); link != link1; link = link.getNext()) {
      mySubstitution.remove(link);
    }
    mySubstitution.remove(link1);
    return true;
  }

  private boolean compareParameters(List<DependentLink> params1, List<DependentLink> params2) {
    if (params1.size() != params2.size()) {
      return false;
    }

    for (int i = 0; i < params1.size() && i < params2.size(); ++i) {
      if (!compare(params1.get(i).getTypeExpr(), params2.get(i).getTypeExpr())) {
        return false;
      }
      mySubstitution.put(params1.get(i), params2.get(i));
    }

    return true;
  }


  @Override
  public Boolean visitUniverse(UniverseExpression expr1, Expression expr2) {
    return expr2.isInstance(UniverseExpression.class) && Sort.compare(expr1.getSort(), expr2.cast(UniverseExpression.class).getSort(), myCMP, myEquations, mySourceNode);
  }

  @Override
  public Boolean visitError(ErrorExpression expr1, Expression expr2) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr1, Expression expr2) {
    if (!expr2.isInstance(TupleExpression.class)) {
      return false;
    }
    TupleExpression tuple2 = expr2.cast(TupleExpression.class);
    if (expr1.getFields().size() != tuple2.getFields().size()) {
      return false;
    }

    Equations.CMP cmp = myCMP;
    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < expr1.getFields().size(); i++) {
      if (!compare(expr1.getFields().get(i), tuple2.getFields().get(i))) {
        return false;
      }
    }
    myCMP = cmp;
    return true;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr1, Expression expr2) {
    if (!expr2.isInstance(SigmaExpression.class)) {
      return false;
    }
    CompareVisitor visitor = new CompareVisitor(mySubstitution, myEquations, Equations.CMP.EQ, mySourceNode);
    if (!visitor.compareParameters(DependentLink.Helper.toList(expr1.getParameters()), DependentLink.Helper.toList(expr2.cast(SigmaExpression.class).getParameters()))) {
      return false;
    }
    for (DependentLink link = expr1.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }
    return true;
  }

  @Override
  public Boolean visitProj(ProjExpression expr1, Expression expr2) {
    if (!expr2.isInstance(ProjExpression.class)) {
      return false;
    }
    ProjExpression proj2 = expr2.cast(ProjExpression.class);
    if (expr1.getField() != proj2.getField()) {
      return false;
    }

    Equations.CMP cmp = myCMP;
    myCMP = Equations.CMP.EQ;
    boolean result = compare(expr1.getExpression(), proj2.getExpression());
    myCMP = cmp;
    return result;
  }

  // TODO: WTF is this? Eta equivalence?
  private boolean compareNew(NewExpression expr1, Expression expr2) {
    ClassCallExpression classCall = expr1.getExpression().checkedCast(ClassCallExpression.class);
    if (classCall == null) {
      return false;
    }

    ClassCallExpression classCall2 = expr2.getType().normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class);
    if (classCall2 == null) {
      return false;
    }

    for (Map.Entry<ClassField, FieldSet.Implementation> entry : classCall.getFieldSet().getImplemented()) {
      FieldSet.Implementation impl2 = classCall2.getFieldSet().getImplementation(entry.getKey());
      if (!compare(entry.getValue().term, impl2 != null ? impl2.term : FieldCall(entry.getKey(), expr2))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitNew(NewExpression expr1, Expression expr2) {
    if (!expr2.isInstance(NewExpression.class)) {
      return compareNew(expr1, expr2);
    }

    Equations.CMP cmp = myCMP;
    myCMP = Equations.CMP.EQ;
    boolean result = compare(expr1.getExpression(), expr2.cast(NewExpression.class).getExpression());
    myCMP = cmp;
    return result;
  }

  @Override
  public Boolean visitLet(LetExpression letExpr1, Expression expr2) {
    // TODO[cmpNorm]: throw new IllegalStateException();
    if (!expr2.isInstance(LetExpression.class)) {
      return false;
    }
    letExpr1 = letExpr1.mergeNestedLets();
    LetExpression letExpr2 = expr2.cast(LetExpression.class).mergeNestedLets();
    if (letExpr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }

    CompareVisitor visitor = new CompareVisitor(mySubstitution, myEquations, Equations.CMP.EQ, mySourceNode);
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      if (!visitor.compare(letExpr1.getClauses().get(i).getExpression(), letExpr2.getClauses().get(i).getExpression())) {
        return false;
      }
      mySubstitution.put(letExpr1.getClauses().get(i), letExpr2.getClauses().get(i));
    }

    visitor.myCMP = myCMP;
    if (!visitor.compare(letExpr1.getExpression(), letExpr2.getExpression())) {
      return false;
    }
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      mySubstitution.remove(letExpr1.getClauses().get(i));
    }
    return true;
  }

  @Override
  public Boolean visitCase(CaseExpression case1, Expression expr2) {
    if (!expr2.isInstance(CaseExpression.class)) {
      return false;
    }
    CaseExpression case2 = expr2.cast(CaseExpression.class);

    if (case1.getArguments().size() != case2.getArguments().size()) {
      return false;
    }

    Equations.CMP oldCMP = myCMP;
    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < case1.getArguments().size(); i++) {
      if (!compare(case1.getArguments().get(i), case2.getArguments().get(i))) {
        myCMP = oldCMP;
        return false;
      }
    }

    boolean ok = compare(case1.getElimTree(), case2.getElimTree());
    myCMP = oldCMP;
    return ok;
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expr, Expression params) {
    return expr.getExpression().accept(this, params);
  }
}
