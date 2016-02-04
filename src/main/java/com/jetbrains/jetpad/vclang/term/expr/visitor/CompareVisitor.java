package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class CompareVisitor extends BaseExpressionVisitor<Expression, Boolean> implements ElimTreeNodeVisitor<ElimTreeNode,Boolean> {
  private final Map<Binding, Binding> mySubstitution;
  private final Equations myEquations;
  private Equations.CMP myCMP;

  private CompareVisitor(Equations equations, Equations.CMP cmp) {
    mySubstitution = new HashMap<>();
    myEquations = equations;
    myCMP = cmp;
  }

  private CompareVisitor(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp) {
    mySubstitution = substitution;
    myEquations = equations;
    myCMP = cmp;
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, Expression expr1, Expression expr2) {
    return new CompareVisitor(equations, cmp).compare(expr1, expr2);
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, ElimTreeNode tree1, ElimTreeNode tree2) {
    return new CompareVisitor(equations, cmp).compare(tree1, tree2);
  }

  public static boolean compare(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp, ElimTreeNode tree1, ElimTreeNode tree2) {
    return new CompareVisitor(substitution, equations, cmp).compare(tree1, tree2);
  }

  public static boolean compare(Map<Binding, Binding> substitution, Equations equations, Equations.CMP cmp, Expression expr1, Expression expr2) {
    return new CompareVisitor(substitution, equations, cmp).compare(expr1, expr2);
  }
  private Expression lamEtaReduce(Expression expr) {
    // TODO
    List<DependentLink> links = new ArrayList<>();
    while (expr instanceof LamExpression) {
      links.add(((LamExpression) expr).getParameters());
      expr = ((LamExpression) expr).getBody();
    }

    DependentLink link = EmptyDependentLink.getInstance();
    for (int i = 0; i < links.size(); link = link.getNext()) {
      if (!(expr instanceof AppExpression)) {
        return null;
      }
      if (!link.hasNext()) {
        link = links.get(i++);
      }

      AppExpression appExpr = (AppExpression) expr;
      if (!(appExpr.getArgument().getExpression() instanceof ReferenceExpression) || ((ReferenceExpression) appExpr.getArgument().getExpression()).getBinding() != link) {
        return null;
      }
      expr = appExpr.getFunction();
      link = link.getNext();
    }
    return expr;
  }

  private Expression pathEtaReduce(AppExpression expr) {
    if (!(expr.getFunction() instanceof ConCallExpression && ((ConCallExpression) expr.getFunction()).getDefinition() != Prelude.PATH_CON)) {
      return null;
    }

    if (!(expr.getArgument().getExpression() instanceof LamExpression)) {
      return null;
    }
    LamExpression lamExpr = (LamExpression) expr.getArgument().getExpression();
    Expression atExpr = lamEtaReduce(lamExpr);
    if (atExpr == null) {
      return null;
    }

    List<Expression> atArgs = new ArrayList<>(4);
    Expression atFun = atExpr.getFunction(atArgs);
    if (!(atArgs.size() == 4 && atFun instanceof FunCallExpression && ((FunCallExpression) atFun).getDefinition() == Prelude.AT && atArgs.get(1).findBinding(lamExpr.getParameters()))) {
      return null;
    }
    return atArgs.get(1);
  }

  private Boolean compare(ElimTreeNode tree1, ElimTreeNode tree2) {
    if (tree1 == tree2) {
      return true;
    }
    return tree1.accept(this, tree2);
  }

  private Boolean compare(Expression expr1, Expression expr2) {
    if (expr1 == expr2 || expr1 instanceof ErrorExpression || expr2 instanceof ErrorExpression) {
      return true;
    }

    // TODO
    // expr1 = lamEtaReduce(expr1);
    // expr2 = lamEtaReduce(expr2);

    if (expr1 instanceof AppExpression) {
      Expression expr = pathEtaReduce((AppExpression) expr1);
      if (expr != null) {
        myCMP = Equations.CMP.EQ;
        return compare(expr, expr2);
      }
    }
    if (expr2 instanceof AppExpression) {
      Expression expr = pathEtaReduce((AppExpression) expr2);
      if (expr != null) {
        myCMP = Equations.CMP.EQ;
        return compare(expr1, expr);
      }
    }

    if (expr2 instanceof ReferenceExpression) {
      return compareReference((ReferenceExpression) expr2, expr1, false);
    }
    return expr1.accept(this, expr2);
  }

  private boolean checkIsInferVar(Expression fun1, List<Expression> args1, Expression expr2, Equations.CMP cmp) {
    if (!(fun1 instanceof ReferenceExpression)) {
      return false;
    }
    if (((ReferenceExpression) fun1).getBinding().isInference()) {
      for (Expression arg : args1) {
        fun1 = Apps(fun1, arg);
      }
      Substitution substitution = new Substitution();
      for (Map.Entry<Binding, Binding> entry : mySubstitution.entrySet()) {
        substitution.addMapping(entry.getKey(), Reference(entry.getValue()));
      }
      myEquations.add(fun1.subst(substitution), expr2, cmp);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitApp(AppExpression expr1, Expression expr2) {
    List<Expression> args1 = new ArrayList<>();
    Expression fun1 = expr1.getFunction(args1);
    if (checkIsInferVar(fun1, args1, expr2, myCMP)) {
      return true;
    }

    List<Expression> args2 = new ArrayList<>(args1.size());
    Expression fun2 = expr2.getFunction(args2);
    if (checkIsInferVar(fun2, args2, expr1, myCMP.not())) {
      return true;
    }

    if (args1.size() != args2.size()) {
      return false;
    }

    myCMP = Equations.CMP.EQ;
    if (!compare(fun1, fun2)) {
      return false;
    }
    for (int i = 0; i < args1.size(); i++) {
      if (!compare(args1.get(i), args2.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr1, Expression expr2) {
    return expr2 instanceof DefCallExpression && expr1.getDefinition() == ((DefCallExpression) expr2).getDefinition();
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ClassCallExpression) || expr1.getDefinition() != ((ClassCallExpression) expr2).getDefinition()) return false;
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats1 = expr1.getImplementStatements();
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats2 = ((ClassCallExpression) expr2).getImplementStatements();
    if (myCMP == Equations.CMP.EQ && implStats1.size() != implStats2.size() ||
        myCMP == Equations.CMP.LE && implStats1.size() <  implStats2.size() ||
        myCMP == Equations.CMP.GE && implStats1.size() >  implStats2.size()) {
      return false;
    }
    Map<ClassField, ClassCallExpression.ImplementStatement> minImplStats = implStats1.size() <= implStats2.size() ? implStats1 : implStats2;
    Map<ClassField, ClassCallExpression.ImplementStatement> maxImplStats = implStats1.size() <= implStats2.size() ? implStats2 : implStats1;

    Equations.CMP oldCMP = myCMP;
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> entry : minImplStats.entrySet()) {
      ClassCallExpression.ImplementStatement maxStat = maxImplStats.get(entry.getKey());
      if (maxStat == null) {
        return false;
      }
      ClassCallExpression.ImplementStatement implStat1 = implStats1.size() <= implStats2.size() ? entry.getValue() : maxStat;
      ClassCallExpression.ImplementStatement implStat2 = implStats1.size() <= implStats2.size() ? maxStat : entry.getValue();

      if (implStat1.term != null && implStat2.term != null) {
        myCMP = Equations.CMP.EQ;
        if (!compare(implStat1.term, implStat2.term)) {
          return false;
        }
      } else
      if (implStat1.term != null || implStat2.term != null) {
        return false;
      }
      myCMP = oldCMP;

      if (implStat1.type == null && implStat2.type == null) {
        continue;
      }
      Expression type1 = implStat1.type;
      Expression type2 = implStat2.type;
      if (type1 == null) {
        if (myCMP == Equations.CMP.GE) {
          continue;
        }
        type1 = entry.getKey().getBaseType();
      }
      if (type2 == null) {
        if (myCMP == Equations.CMP.LE) {
          continue;
        }
        type2 = entry.getKey().getBaseType();
      }
      if (!compare(type1, type2)) {
        return false;
      }
    }
    return true;
  }

  private Boolean compareReference(ReferenceExpression expr1, Expression expr2, boolean first) {
    // TODO
    if (expr2 instanceof ReferenceExpression) {
      Binding binding1 = first ? expr1.getBinding() : ((ReferenceExpression) expr2).getBinding();
      Binding subst1 = mySubstitution.get(binding1);
      if (subst1 != null) {
        binding1 = subst1;
      }
      Binding binding2 = first ? ((ReferenceExpression) expr2).getBinding() : expr1.getBinding();
      if (binding1 == binding2) {
        return true;
      }
      if (!expr1.getBinding().isInference() && !((ReferenceExpression) expr2).getBinding().isInference()) {
        return false;
      }
    } else {
      if (!expr1.getBinding().isInference()) {
        return false;
      }
    }
    myEquations.add(expr1, expr2, first ? myCMP : myCMP.not());
    return true;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr1, Expression expr2) {
    return compareReference(expr1, expr2, true);
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

    for (int i = 0; i < params1.size(); i++) {
      mySubstitution.put(params1.get(i), params2.get(i));
    }
    Equations equations = myEquations.newInstance();
    if (!new CompareVisitor(mySubstitution, equations, Equations.CMP.EQ).compare(body1, body2)) {
      return false;
    }
    for (int i = 0; i < params1.size(); i++) {
      mySubstitution.remove(params1.get(i));
      equations.abstractBinding(params2.get(i));
    }
    myEquations.add(equations);
    return true;
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2) {
    if (!(expr2 instanceof PiExpression)) return false;

    List<DependentLink> params1 = new ArrayList<>(), params2 = new ArrayList<>();
    Expression cod1 = expr1.getPiParameters(params1, false, false);
    Expression cod2 = expr2.getPiParameters(params2, false, false);
    if (params1.size() != params2.size()) {
      return false;
    }

    Equations equations = myEquations.newInstance();
    CompareVisitor visitor = new CompareVisitor(mySubstitution, equations, Equations.CMP.EQ);
    for (int i = 0; i < params1.size(); i++) {
      if (params1.get(i) instanceof UntypedDependentLink && params2.get(i) instanceof UntypedDependentLink) {
        continue;
      }
      if (!visitor.compare(params1.get(i).getType(), params2.get(i).getType())) {
        return false;
      }
      mySubstitution.put(params1.get(i), params2.get(i));
    }

    if (!visitor.compare(cod1, cod2)) {
      return false;
    }
    for (int i = 0; i < params1.size(); i++) {
      mySubstitution.remove(params1.get(i));
      equations.abstractBinding(params2.get(i));
    }
    myEquations.add(equations);
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
    if (!(expr2 instanceof UniverseExpression)) return false;
    Universe.Cmp result = expr1.getUniverse().compare(((UniverseExpression) expr2).getUniverse());
    return result == Universe.Cmp.EQUALS || result == myCMP.toUniverseCmp();
  }

  @Override
  public Boolean visitInferHole(InferHoleExpression expr1, Expression expr2) {
    return false;
  }

  @Override
  public Boolean visitError(ErrorExpression expr1, Expression expr2) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr1, Expression expr2) {
    if (!(expr2 instanceof TupleExpression)) return false;
    TupleExpression tupleExpr2 = (TupleExpression) expr2;
    if (expr1.getFields().size() != tupleExpr2.getFields().size()) {
      return false;
    }

    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < expr1.getFields().size(); i++) {
      if (!compare(expr1.getFields().get(i), tupleExpr2.getFields().get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr1, Expression expr2) {
    if (!(expr2 instanceof SigmaExpression)) return false;
    Equations equations = myEquations.newInstance();
    CompareVisitor visitor = new CompareVisitor(mySubstitution, equations, Equations.CMP.EQ);
    if (!visitor.compareParameters(expr1.getParameters(), ((SigmaExpression) expr2).getParameters())) {
      return false;
    }
    for (DependentLink link = expr1.getParameters(); link.hasNext(); link = link.getNext()) {
      mySubstitution.remove(link);
    }
    for (DependentLink link = ((SigmaExpression) expr2).getParameters(); link.hasNext(); link = link.getNext()) {
      equations.abstractBinding(link);
    }
    myEquations.add(equations);
    return true;
  }

  @Override
  public Boolean visitProj(ProjExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ProjExpression)) return false;
    ProjExpression projExpr2 = (ProjExpression) expr2;
    if (expr1.getField() != projExpr2.getField()) {
      return false;
    }
    myCMP = Equations.CMP.EQ;
    return compare(expr1.getExpression(), projExpr2.getExpression());
  }

  @Override
  public Boolean visitNew(NewExpression expr1, Expression expr2) {
    if (!(expr2 instanceof NewExpression)) return false;
    myCMP = Equations.CMP.EQ;
    return compare(expr1.getExpression(), ((NewExpression) expr2).getExpression());
  }

  @Override
  public Boolean visitLet(LetExpression expr1, Expression expr2) {
    if (!(expr2 instanceof LetExpression)) {
      return false;
    }
    LetExpression letExpr1 = expr1.mergeNestedLets();
    LetExpression letExpr2 = ((LetExpression) expr2).mergeNestedLets();
    if (letExpr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }

    Equations equations = myEquations.newInstance();
    CompareVisitor visitor = new CompareVisitor(mySubstitution, equations, Equations.CMP.EQ);
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
      for (DependentLink link = letExpr2.getClauses().get(i).getParameters(); link.hasNext(); link = link.getNext()) {
        equations.abstractBinding(link);
      }
      mySubstitution.remove(letExpr1.getClauses().get(i));
      equations.abstractBinding(letExpr2.getClauses().get(i));
    }
    myEquations.add(equations);
    return true;
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

      Equations equations = myEquations.newInstance();
      if (!new CompareVisitor(mySubstitution, equations, Equations.CMP.EQ).compare(clause.getChild(), otherClause.getChild())) {
        return false;
      }

      for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext()) {
        mySubstitution.remove(link);
      }
      for (DependentLink link = otherClause.getParameters(); link.hasNext(); link = link.getNext()) {
        equations.abstractBinding(link);
      }
      for (int i = 0; i < clause.getTailBindings().size() && i < otherClause.getTailBindings().size(); i++) {
        mySubstitution.remove(clause.getTailBindings().get(i));
        equations.abstractBinding(otherClause.getTailBindings().get(i));
      }
      myEquations.add(equations);
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
