package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

public class CompareVisitor extends BaseExpressionVisitor<Expression, Boolean> implements ElimTreeNodeVisitor<ElimTreeNode,Boolean> {
  private final Equations myEquations;
  private Equations.CMP myCMP;

  private CompareVisitor(Equations equations, Equations.CMP cmp) {
    myEquations = equations;
    myCMP = cmp;
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, Expression expr1, Expression expr2) {
    return new CompareVisitor(equations, cmp).compare(expr1, expr2);
  }

  public static boolean compare(Equations equations, Equations.CMP cmp, ElimTreeNode tree1, ElimTreeNode tree2) {
    return new CompareVisitor(equations, cmp).compare(tree1, tree2);
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

    if (expr1 instanceof LamExpression || expr2 instanceof LamExpression) {
      int number1 = 0, number2 = 0;
      while (expr1 instanceof LamExpression) {
        number1 += DependentLink.Helper.size(((LamExpression) expr1).getParameters());
        expr1 = ((LamExpression) expr1).getBody();
      }
      while (expr2 instanceof LamExpression) {
        number2 += DependentLink.Helper.size(((LamExpression) expr2).getParameters());
        expr2 = ((LamExpression) expr2).getBody();
      }

      if (lamEtaReduce(number1 >= number2 ? expr1 : expr2) == null) {
        return false;
      }

      Equations.CMP oldCMP = myCMP;
      myCMP = Equations.CMP.EQ;
      if (!compare(expr1, expr2)) {
        return false;
      }
      myCMP = oldCMP;
      return true;
    }

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
      return compareReference((ReferenceExpression) expr2, expr1, myCMP.not());
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
      myEquations.add(fun1, expr2, cmp);
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

  private Boolean compareReference(ReferenceExpression expr1, Expression expr2, Equations.CMP cmp) {
    if (expr2 instanceof ReferenceExpression && expr1.getBinding() == ((ReferenceExpression) expr2).getBinding()) {
      return true;
    }
    if (!expr1.getBinding().isInference()) {
      return false;
    }
    myEquations.add(expr1, expr2, cmp);
    return true;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr1, Expression expr2) {
    return compareReference(expr1, expr2, myCMP);
  }

  @Override
  public Boolean visitLam(LamExpression expr1, Expression expr2) {
    throw new IllegalStateException();
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2) {
    if (!(expr2 instanceof PiExpression)) return false;

    List<DependentLink> params1 = new ArrayList<>(), params2 = new ArrayList<>();
    Expression cod1 = expr1.getPiParameters(params1);
    Expression cod2 = expr2.getPiParameters(params2);
    if (params1.size() != params2.size()) {
      return false;
    }

    Equations.CMP oldCMP = myCMP;
    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < params1.size(); i++) {
      if (!compare(params1.get(i).getType(), params2.get(i).getType())) {
        return false;
      }
    }
    if (!compare(cod1, cod2)) {
      return false;
    }
    myCMP = oldCMP;
    return true;
  }

  private boolean compareTypeArguments(DependentLink params1, DependentLink params2) {
    for (; params1.hasNext() && params2.hasNext(); params1 = params1.getNext(), params2 = params2.getNext()) {
      if (!compare(params1.getType(), params2.getType())) {
        return false;
      }
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
    Equations.CMP oldCMP = myCMP;
    myCMP = Equations.CMP.EQ;
    if (!compareTypeArguments(expr1.getParameters(), ((SigmaExpression) expr2).getParameters())) {
      return false;
    }
    myCMP = oldCMP;
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

    Equations.CMP oldCMP = myCMP;
    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      if (!compareTypeArguments(letExpr1.getClauses().get(i).getParameters(), letExpr2.getClauses().get(i).getParameters())) {
        return false;
      }
      if (!compare(letExpr1.getClauses().get(i).getElimTree(), letExpr2.getClauses().get(i).getElimTree())) {
        return false;
      }
    }

    myCMP = oldCMP;
    return compare(letExpr1.getExpression(), letExpr2.getExpression());
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, ElimTreeNode node) {
    if (!(node instanceof BranchElimTreeNode))
      return false;
    BranchElimTreeNode other = (BranchElimTreeNode) node;

    if (other.getReference() != branchNode.getReference())
      return false;
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      ConstructorClause otherClause = other.getClause(clause.getConstructor());
      if (otherClause == null)
        return false;
      if (!clause.getChild().accept(this, otherClause.getChild())) {
        return false;
      }
    }
    for (ConstructorClause clause : other.getConstructorClauses()) {
      if (branchNode.getClause(clause.getConstructor()) == null) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, ElimTreeNode node) {
    if (node instanceof LeafElimTreeNode) {
      return leafNode.getExpression().accept(this, ((LeafElimTreeNode) node).getExpression());
    }
    return false;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, ElimTreeNode node) {
    return node instanceof EmptyElimTreeNode;
  }
}
