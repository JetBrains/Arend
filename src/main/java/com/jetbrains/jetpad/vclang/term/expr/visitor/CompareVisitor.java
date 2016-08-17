package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.EtaNormalization;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class CompareVisitor extends BaseExpressionVisitor<Expression, Boolean> implements ElimTreeNodeVisitor<ElimTreeNode,Boolean> {
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

  private CompareVisitor(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp) {
    mySubstitution = substitution;
    myEquations = equations;
    mySourceNode = null;
    myCMP = cmp;
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, Expression expr1, Expression expr2, Abstract.SourceNode sourceNode) {
    return new CompareVisitor(equations, cmp, sourceNode).compare(expr1, expr2);
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, ElimTreeNode tree1, ElimTreeNode tree2) {
    return new CompareVisitor(equations, cmp, null).compare(tree1, tree2);
  }

  public static boolean compare(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp, ElimTreeNode tree1, ElimTreeNode tree2) {
    return new CompareVisitor(substitution, equations, cmp).compare(tree1, tree2);
  }

  public static boolean compare(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp, Expression expr1, Expression expr2) {
    return new CompareVisitor(substitution, equations, cmp).compare(expr1, expr2);
  }

  public Boolean compare(ElimTreeNode tree1, ElimTreeNode tree2) {
    if (tree1 == tree2) {
      return true;
    }
    return tree1.accept(this, tree2);
  }

  public Boolean compare(Expression expr1, Expression expr2) {
    if (expr1 == expr2 || expr1.toError() != null  || expr2.toError() != null) {
      return true;
    }

    expr1 = EtaNormalization.normalize(expr1);
    expr2 = EtaNormalization.normalize(expr2);

    while (expr1.toInferenceReference() != null && expr1.toInferenceReference().getSubstExpression() != null || expr1.toOfType() != null) {
      if (expr1.toOfType() != null) {
        expr1 = expr1.toOfType().getExpression();
      } else {
        expr1 = expr1.toInferenceReference().getSubstExpression();
      }
    }
    while (expr2.toInferenceReference() != null && expr2.toInferenceReference().getSubstExpression() != null || expr2.toOfType() != null) {
      if (expr2.toOfType() != null) {
        expr2 = expr2.toOfType().getExpression();
      } else {
        expr2 = expr2.toInferenceReference().getSubstExpression();
      }
    }

    if (!expr2.getArguments().isEmpty() && checkIsInferVar(expr2.getFunction(), expr1, expr2)) {
      return true;
    }

    InferenceReferenceExpression ref2 = expr2.toInferenceReference();
    if (ref2 != null) {
      return compareInferenceReference(ref2, expr1, false);
    }
    InferenceReferenceExpression ref1 = expr1.toInferenceReference();
    if (ref1 != null) {
      return compareInferenceReference(ref1, expr2, true);
    }

    NewExpression new1 = expr1.toNew();
    NewExpression new2 = expr2.toNew();
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
  public static InferenceVariable checkIsInferVar(Expression expr) {
    InferenceReferenceExpression ref = expr.getFunction().toInferenceReference();
    return ref != null && ref.getSubstExpression() == null ? ref.getVariable() : null;
  }

  private boolean checkIsInferVar(Expression fun, Expression expr1, Expression expr2) {
    InferenceVariable binding = checkIsInferVar(fun);
    return binding != null && myEquations.add(expr1.subst(getSubstition()), expr2, myCMP, binding.getSourceNode(), binding);
  }

  private ExprSubstitution getSubstition() {
    ExprSubstitution substitution = new ExprSubstitution();
    for (Map.Entry<Binding, Binding> entry : mySubstitution.entrySet()) {
      substitution.add(entry.getKey(), Reference(entry.getValue()));
    }
    return substitution;
  }

  @Override
  public Boolean visitApp(AppExpression expr1, Expression expr2) {
    List<? extends Expression> args1 = expr1.getArguments();
    Expression fun1 = expr1.getFunction();
    if (checkIsInferVar(fun1, expr1, expr2)) {
      return true;
    }

    List<? extends Expression> args2 = expr2.getArguments();
    Expression fun2 = expr2.getFunction();
    if (checkIsInferVar(fun2, expr1, expr2)) {
      return true;
    }

    if (args1.size() != args2.size()) {
      return false;
    }

    //Equations.CMP cmp = myCMP;
    //myCMP = Equations.CMP.EQ;
    if (!compare(fun1, fun2)) {
      return false;
    }

    int i = 0;
    if (fun1.toDataCall() != null && fun2.toDataCall() != null) {
      if (args1.isEmpty()) {
      //  myCMP = cmp;
        return true;
      }
      if (fun1.toDataCall().getDefinition().getThisClass() != null) {
        if (!compare(args1.get(i), args2.get(i))) {
          return false;
        }
        if (++i >= args1.size()) {
        //  myCMP = cmp;
          return true;
        }
      }

      /*
      Expression type1 = args1.get(i).getType().normalize(NormalizeVisitor.Mode.NF);
      if (type1.toDataCall() != null && type1.toDataCall().getDefinition() == Preprelude.LVL) {
        Expression type2 = args2.get(i).getType().normalize(NormalizeVisitor.Mode.NF);
        if (type2.toDataCall() != null && type2.toDataCall().getDefinition() == Preprelude.LVL) {
          compare(myEquations, cmp, args1.get(i), args2.get(i), null);
          if (++i >= args1.size()) {
            myCMP = cmp;
            return true;
          }
          type1 = args1.get(i).getType().normalize(NormalizeVisitor.Mode.NF);
          if (type1.toDataCall() != null && type1.toDataCall().getDefinition() == Preprelude.CNAT) {
            type2 = args2.get(i).getType().normalize(NormalizeVisitor.Mode.NF);
            if (type2.toDataCall() != null && type2.toDataCall().getDefinition() == Preprelude.CNAT) {
              compare(myEquations, cmp, args1.get(i), args2.get(i), null);
              i++;
            }
          }
        }
      }/**/
    }

    for (; i < args1.size(); i++) {
      if (!compare(args1.get(i), args2.get(i))) {
        return false;
      }
    }

    //myCMP = cmp;
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr1, Expression expr2) {
    DefCallExpression defCall2 = expr2.toDefCall();
    return defCall2 != null && expr1.getDefinition() == defCall2.getDefinition();
  }

  private boolean checkSubclassImpl(FieldSet fieldSet1, ClassCallExpression classCall2) {
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
    ClassCallExpression classCall2 = expr2.toClassCall();
    if (classCall2 == null) return false;

    boolean subclass2of1Test = myCMP.equals(Equations.CMP.LE) || classCall2.getDefinition().isSubClassOf(expr1.getDefinition());
    boolean subclass1of2Test = myCMP.equals(Equations.CMP.GE) || expr1.getDefinition().isSubClassOf(classCall2.getDefinition());
    if (!(subclass1of2Test && subclass2of1Test)) return false;

    FieldSet fieldSet1 = expr1.getFieldSet();
    FieldSet fieldSet2 = classCall2.getFieldSet();
    boolean implAllOf1Test = myCMP.equals(Equations.CMP.LE) || checkSubclassImpl(fieldSet2, expr1);
    boolean implAllOf2Test = myCMP.equals(Equations.CMP.GE) || checkSubclassImpl(fieldSet1, classCall2);
    return implAllOf1Test && implAllOf2Test;
  }

  private boolean compareInferenceReference(InferenceReferenceExpression expr1, Expression expr2, boolean first) {
    if (expr2.toInferenceReference() != null && expr1.getVariable() == expr2.toInferenceReference().getVariable()) {
      return true;
    }

    return myEquations.add(expr1, expr2.subst(getSubstition()), first ? myCMP : myCMP.not(), expr1.getVariable().getSourceNode(), expr1.getVariable());
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr1, Expression expr2) {
    ReferenceExpression ref2 = expr2.toReference();
    if (ref2 == null) {
      return false;
    }

    Binding binding1 = expr1.getBinding();
    Binding subst1 = mySubstitution.get(binding1);
    if (subst1 != null) {
      binding1 = subst1;
    }
    return binding1 == ref2.getBinding();
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
    if (!new CompareVisitor(substitution, myEquations, Equations.CMP.EQ).compare(body1, body2)) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2) {
    if (expr2.toPi() == null) return false;

    List<DependentLink> params1 = new ArrayList<>(), params2 = new ArrayList<>();
    Expression cod1 = expr1.getPiParameters(params1, false, false);
    Expression cod2 = expr2.getPiParameters(params2, false, false);
    if (params1.size() < params2.size()) {
      cod2 = cod2.fromPiParameters(params2.subList(params1.size(), params2.size()));
      params2 = params2.subList(0, params1.size());
    }
    if (params2.size() < params1.size()) {
      cod1 = cod1.fromPiParameters(params1.subList(params2.size(), params1.size()));
      params1 = params1.subList(0, params2.size());
    }

    CompareVisitor visitor = new CompareVisitor(mySubstitution, myEquations, Equations.CMP.EQ);
    for (int i = 0; i < params1.size(); i++) {
      if (!(params1.get(i) instanceof UntypedDependentLink && params2.get(i) instanceof UntypedDependentLink)) {
        if (!visitor.compare(params1.get(i).getType(), params2.get(i).getType())) {
          return false;
        }
      }
      mySubstitution.put(params1.get(i), params2.get(i));
    }

    visitor.myCMP = myCMP;
    if (!visitor.compare(cod1, cod2)) {
      return false;
    }
    for (DependentLink param : params1) {
      mySubstitution.remove(param);
    }
    return true;
  }

  private boolean compareParameters(DependentLink params1, DependentLink params2) {
    for (; params1.hasNext() && params2.hasNext(); params1 = params1.getNext(), params2 = params2.getNext()) {
      if (!compare(params1.getType(), params2.getType())) {
        return false;
      }
      mySubstitution.put(params1, params2);
    }
    return !params1.hasNext() && !params2.hasNext();
  }


  @Override
  public Boolean visitUniverse(UniverseExpression expr1, Expression expr2) {
    UniverseExpression universe2 = expr2.toUniverse();
    return universe2 != null && universe2.getSort() != null && Sort.compare(expr1.getSort(), universe2.getSort(), myCMP, myEquations, mySourceNode);
  }

  @Override
  public Boolean visitError(ErrorExpression expr1, Expression expr2) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr1, Expression expr2) {
    TupleExpression tuple2 = expr2.toTuple();
    if (tuple2 == null) return false;
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
    SigmaExpression sigma2 = expr2.toSigma();
    if (sigma2 == null) return false;
    CompareVisitor visitor = new CompareVisitor(mySubstitution, myEquations, Equations.CMP.EQ);
    if (!visitor.compareParameters(expr1.getParameters(), sigma2.getParameters())) {
      return false;
    }
    for (DependentLink link = expr1.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }
    return true;
  }

  @Override
  public Boolean visitProj(ProjExpression expr1, Expression expr2) {
    ProjExpression proj2 = expr2.toProj();
    if (proj2 == null) return false;
    if (expr1.getField() != proj2.getField()) {
      return false;
    }

    Equations.CMP cmp = myCMP;
    myCMP = Equations.CMP.EQ;
    boolean result = compare(expr1.getExpression(), proj2.getExpression());
    myCMP = cmp;
    return result;
  }

  private boolean compareNew(NewExpression expr1, Expression expr2) {
    ClassCallExpression classCall = expr1.getExpression().toClassCall();
    if (classCall == null) {
      return false;
    }

    ClassCallExpression classCall2 = ((Expression) expr2.getType()).normalize(NormalizeVisitor.Mode.WHNF).toClassCall();

    for (Map.Entry<ClassField, FieldSet.Implementation> entry : classCall.getFieldSet().getImplemented()) {
      FieldSet.Implementation impl2 = classCall2.getFieldSet().getImplementation(entry.getKey());
      if (!compare(entry.getValue().term, impl2 != null ? impl2.term : FieldCall(entry.getKey()).applyThis(expr2))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitNew(NewExpression expr1, Expression expr2) {
    NewExpression new2 = expr2.toNew();
    if (new2 == null) {
      return compareNew(expr1, expr2);
    }

    Equations.CMP cmp = myCMP;
    myCMP = Equations.CMP.EQ;
    boolean result = compare(expr1.getExpression(), new2.getExpression());
    myCMP = cmp;
    return result;
  }

  @Override
  public Boolean visitLet(LetExpression expr1, Expression expr2) {
    LetExpression letExpr2 = expr2.toLet();
    if (letExpr2 == null) {
      return false;
    }
    LetExpression letExpr1 = expr1.mergeNestedLets();
    letExpr2 = letExpr2.mergeNestedLets();
    if (letExpr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }

    CompareVisitor visitor = new CompareVisitor(mySubstitution, myEquations, Equations.CMP.EQ);
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      if (!visitor.compareParameters(letExpr1.getClauses().get(i).getParameters(), letExpr2.getClauses().get(i).getParameters())) {
        return false;
      }
      if (!visitor.compare(letExpr1.getClauses().get(i).getElimTree(), letExpr2.getClauses().get(i).getElimTree())) {
        return false;
      }
      mySubstitution.put(letExpr1.getClauses().get(i), letExpr2.getClauses().get(i));
    }

    visitor.myCMP = myCMP;
    if (!visitor.compare(letExpr1.getExpression(), letExpr2.getExpression())) {
      return false;
    }
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      for (DependentLink link = letExpr1.getClauses().get(i).getParameters(); link.hasNext(); link = link.getNext()) {
        mySubstitution.remove(link);
      }
      mySubstitution.remove(letExpr1.getClauses().get(i));
    }
    return true;
  }

  @Override
  public Boolean visitOfType(OfTypeExpression expr, Expression params) {
    return expr.getExpression().accept(this, params);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, ElimTreeNode node) {
    if (!(node instanceof BranchElimTreeNode))
      return false;
    BranchElimTreeNode other = (BranchElimTreeNode) node;

    Binding binding1 = mySubstitution.get(branchNode.getReference());
    if (binding1 == null) {
      binding1 = branchNode.getReference();
    }
    if (other.getReference() != binding1) {
      return false;
    }

    if (branchNode.getContextTail().size() != other.getContextTail().size()) {
      return false;
    }
    for (int i = 0; i < branchNode.getContextTail().size(); i++) {
      binding1 = mySubstitution.get(branchNode.getContextTail().get(i));
      if (binding1 == null) {
        binding1 = branchNode.getContextTail().get(i);
      }
      if (binding1 != other.getContextTail().get(i)) {
        return false;
      }
    }

    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      Clause clause1 = other.getClause(clause.getConstructor());
      if (!(clause1 instanceof ConstructorClause))
        return false;
      ConstructorClause otherClause = (ConstructorClause) clause1;

      for (DependentLink link1 = clause.getParameters(), link2 = otherClause.getParameters(); link1.hasNext() && link2.hasNext(); link1 = link1.getNext(), link2 = link2.getNext()) {
        mySubstitution.put(link1, link2);
      }
      for (int i = 0; i < clause.getTailBindings().size() && i < otherClause.getTailBindings().size(); i++) {
        mySubstitution.put(clause.getTailBindings().get(i), otherClause.getTailBindings().get(i));
      }

      if (!new CompareVisitor(mySubstitution, myEquations, Equations.CMP.EQ).compare(clause.getChild(), otherClause.getChild())) {
        return false;
      }

      for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext()) {
        mySubstitution.remove(link);
      }
      for (int i = 0; i < clause.getTailBindings().size() && i < otherClause.getTailBindings().size(); i++) {
        mySubstitution.remove(clause.getTailBindings().get(i));
      }
    }

    for (ConstructorClause clause : other.getConstructorClauses()) {
      if (!(branchNode.getClause(clause.getConstructor()) instanceof ConstructorClause)) {
        return false;
      }
    }
    if ((branchNode.getOtherwiseClause() == null) != (((BranchElimTreeNode) node).getOtherwiseClause() == null)) {
      return false;
    }
    if (branchNode.getOtherwiseClause() != null) {
      if (!compare(branchNode.getOtherwiseClause().getChild(), ((BranchElimTreeNode) node).getOtherwiseClause().getChild())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, ElimTreeNode node) {
    if (node instanceof LeafElimTreeNode) {
      return compare(leafNode.getExpression(), ((LeafElimTreeNode) node).getExpression());
    }
    return false;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, ElimTreeNode node) {
    return node instanceof EmptyElimTreeNode;
  }
}
