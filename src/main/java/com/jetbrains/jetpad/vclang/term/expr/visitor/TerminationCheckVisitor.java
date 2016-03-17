package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class TerminationCheckVisitor extends BaseExpressionVisitor<Void, Boolean> implements ElimTreeNodeVisitor<Void, Boolean> {
  private final Definition myDef;
  private final List<Expression> myPatterns;

  public TerminationCheckVisitor(Definition def, DependentLink... allParameters) {
    myDef = def;

    myPatterns = new ArrayList<>();
    for (DependentLink parameter : allParameters) {
      for (; parameter.hasNext(); parameter = parameter.getNext()) {
        myPatterns.add(Reference(parameter));
      }
    }
  }

  private TerminationCheckVisitor(Definition def, List<Expression> patterns) {
    myDef = def;
    myPatterns = patterns;
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, Void params) {
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      if (!clause.getChild().accept(new TerminationCheckVisitor(myDef, clause.getSubst().substExprs(myPatterns)), null)) {
        return false;
      }
    }

    return branchNode.getOtherwiseClause() == null || branchNode.getOtherwiseClause().getChild().accept(this, null);
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return leafNode.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return true;
  }

  private enum Ord { LESS, EQUALS, NOT_LESS }

  private Ord isLess(Expression expr1, Expression expr2) {
    List<? extends Expression> args1 = expr1.getArguments();
    Expression fun1 = expr1.getFunction();
    List<? extends Expression> args2 = expr2.getArguments();
    Expression fun2 = expr2.getFunction();
    if (fun1.equals(fun2)) {
      Ord ord = isLess(args1, args2);
      if (ord != Ord.NOT_LESS) return ord;
    }
    for (Expression arg : args2) {
      if (isLess(expr1, arg) != Ord.NOT_LESS) return Ord.LESS;
    }
    return Ord.NOT_LESS;
  }

  private Ord isLess(List<? extends Expression> exprs1, List<? extends Expression> exprs2) {
    for (int i = 0; i < Math.min(exprs1.size(), exprs2.size()); i++) {
      Ord ord = isLess(exprs1.get(i), exprs2.get(i));
      if (ord != Ord.EQUALS) return ord;
    }
    return exprs1.size() >= exprs2.size() ? Ord.EQUALS : Ord.NOT_LESS;
  }

  @Override
  public Boolean visitApp(AppExpression expr, Void params) {
    List<? extends Expression> args = expr.getArguments();
    Expression fun = expr.getFunction();
    if (fun instanceof ConCallExpression) {
      List<Expression> dataTypeArguments = ((ConCallExpression) fun).getDataTypeArguments();
      if (!dataTypeArguments.isEmpty()) {
        List<Expression> newArgs = new ArrayList<>(args.size() + dataTypeArguments.size());
        newArgs.addAll(dataTypeArguments);
        newArgs.addAll(args);
        args = newArgs;
      }
    }
    if (fun instanceof DefCallExpression) {
      if (((DefCallExpression) fun).getDefinition() == myDef && isLess(args, myPatterns) != Ord.LESS) {
        return false;
      }
      if (fun instanceof ConCallExpression && ((ConCallExpression) fun).getDefinition() != myDef && !visitConCall((ConCallExpression) fun, null)) {
        return false;
      }
      if (fun instanceof ClassCallExpression && !visitClassCall((ClassCallExpression) fun, null)) {
        return false;
      }
    } else {
      if (!fun.accept(this, null)) {
        return false;
      }
    }

    for (Expression arg : args) {
      if (!arg.accept(this, null)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr, Void params) {
    return expr.getDefinition() != myDef;
  }

  @Override
  public Boolean visitConCall(ConCallExpression expr, Void params) {
    for (Expression parameter : expr.getDataTypeArguments()) {
      if (!parameter.accept(this, null)) {
        return false;
      }
    }
    return expr.getDefinition() != myDef;
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      if (elem.getValue().type != null && !elem.getValue().type.accept(this, null) || elem.getValue().term != null && !elem.getValue().term.accept(this, null)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitReference(ReferenceExpression expr, Void params) {
    return true;
  }

  @Override
  public Boolean visitLam(LamExpression expr, Void params) {
    return visitArguments(expr.getParameters()) && expr.getBody().accept(this, null);
  }

  @Override
  public Boolean visitPi(PiExpression expr, Void params) {
    return visitArguments(expr.getParameters()) && expr.getCodomain().accept(this, null);
  }

  private boolean visitArguments(DependentLink parameters) {
    for (; parameters.hasNext(); parameters = parameters.getNext()) {
      if (!parameters.getType().accept(this, null)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr, Void params) {
    return true;
  }

  @Override
  public Boolean visitError(ErrorExpression expr, Void params) {
    return true;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr, Void params) {
    for (Expression field : expr.getFields()) {
      if (!field.accept(this, null)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr, Void params) {
    return visitArguments(expr.getParameters());
  }

  @Override
  public Boolean visitProj(ProjExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitNew(NewExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Boolean visitLet(LetExpression letExpression, Void params) {
    for (LetClause clause : letExpression.getClauses()) {
      if (!visitLetClause(clause)) {
        return false;
      }
    }
    return letExpression.getExpression().accept(this, null);
  }

  private boolean visitLetClause(LetClause clause) {
    if (!visitArguments(clause.getParameters())) {
      return false;
    }
    return clause.getElimTree().accept(this, null);
  }
}
