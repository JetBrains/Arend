package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Iterator;
import java.util.List;

public class AbstractCompareVisitor implements AbstractExpressionVisitor<Abstract.Expression, Boolean> {
  @Override
  public Boolean visitApp(Abstract.AppExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.AppExpression && expr1.getFunction().accept(this, ((Abstract.AppExpression) expr2).getFunction()) && expr1.getArgument().getExpression().accept(this, ((Abstract.AppExpression) expr2).getArgument().getExpression());
  }

  @Override
  public Boolean visitDefCall(Abstract.DefCallExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.DefCallExpression)) return false;
    Abstract.DefCallExpression defCallExpr2 = (Abstract.DefCallExpression) expr2;
    return expr1.getName().equals(defCallExpr2.getName());
  }

  @Override
  public Boolean visitModuleCall(Abstract.ModuleCallExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.ModuleCallExpression && expr1.getPath().equals(((Abstract.ModuleCallExpression) expr2).getPath());
  }

  private boolean compareArg(Abstract.Argument arg1, Abstract.Argument arg2) {
    if (arg1.getExplicit() != arg2.getExplicit()) {
      return false;
    }
    if (arg1 instanceof Abstract.TelescopeArgument) {
      return arg2 instanceof Abstract.TelescopeArgument && ((Abstract.TelescopeArgument) arg1).getNames().equals(((Abstract.TelescopeArgument) arg2).getNames()) && ((Abstract.TelescopeArgument) arg1).getType().accept(this, ((Abstract.TelescopeArgument) arg2).getType());
    }
    if (arg1 instanceof Abstract.TypeArgument) {
      return arg2 instanceof Abstract.TypeArgument && ((Abstract.TypeArgument) arg1).getType().accept(this, ((Abstract.TypeArgument) arg2).getType());
    }
    return arg1 instanceof Abstract.NameArgument && arg2 instanceof Abstract.NameArgument && ((Abstract.NameArgument) arg1).getName().equals(((Abstract.NameArgument) arg2).getName());
  }

  private boolean compareArgs(List<? extends Abstract.Argument> args1, List<? extends Abstract.Argument> args2) {
    if (args1.size() != args2.size()) return false;
    for (int i = 0; i < args1.size(); i++) {
      if (!compareArg(args1.get(i), args2.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitLam(Abstract.LamExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.LamExpression && compareArgs(expr1.getArguments(), ((Abstract.LamExpression) expr2).getArguments()) && expr1.getBody().accept(this, ((Abstract.LamExpression) expr2).getBody());
  }

  @Override
  public Boolean visitPi(Abstract.PiExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.PiExpression && compareArgs(expr1.getArguments(), ((Abstract.PiExpression) expr2).getArguments()) && expr1.getCodomain().accept(this, ((Abstract.PiExpression) expr2).getCodomain());
  }

  @Override
  public Boolean visitUniverse(Abstract.UniverseExpression expr1, Abstract.Expression expr2) {
    //return expr2 instanceof Abstract.UniverseExpression && expr1.getUniverse().compare(((Abstract.UniverseExpression) expr2).getUniverse()) == UniverseOld.Cmp.EQUALS;
    return expr2 instanceof Abstract.UniverseExpression && expr1.getUniverse().equals(((Abstract.UniverseExpression) expr2).getUniverse());
  }

  @Override
  public Boolean visitPolyUniverse(Abstract.PolyUniverseExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.PolyUniverseExpression && expr1.getPLevel().accept(this, ((Abstract.PolyUniverseExpression) expr2).getPLevel()) &&
            expr1.getHLevel().accept(this, ((Abstract.PolyUniverseExpression) expr2).getHLevel());
  }

  @Override
  public Boolean visitInferHole(Abstract.InferHoleExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.InferHoleExpression;
  }

  @Override
  public Boolean visitError(Abstract.ErrorExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.ErrorExpression;
  }

  @Override
  public Boolean visitTuple(Abstract.TupleExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.TupleExpression)) return false;
    Abstract.TupleExpression tupleExpr2 = (Abstract.TupleExpression) expr2;
    if (expr1.getFields().size() != tupleExpr2.getFields().size()) return false;
    for (int i = 0; i < expr1.getFields().size(); i++) {
      if (expr1.getFields().get(i).accept(this, tupleExpr2.getFields().get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitSigma(Abstract.SigmaExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.SigmaExpression && compareArgs(expr1.getArguments(), ((Abstract.SigmaExpression) expr2).getArguments());
  }

  @Override
  public Boolean visitBinOp(Abstract.BinOpExpression expr1, Abstract.Expression expr2) {
    if (expr2 instanceof Abstract.BinOpSequenceExpression && ((Abstract.BinOpSequenceExpression) expr2).getSequence().isEmpty()) {
      return visitBinOp(expr1, ((Abstract.BinOpSequenceExpression) expr2).getLeft());
    }
    if (!(expr2 instanceof Abstract.BinOpExpression)) return false;
    Abstract.BinOpExpression binOpExpr2 = (Abstract.BinOpExpression) expr2;
    return expr1.getLeft().accept(this, binOpExpr2.getLeft()) && expr1.getRight().accept(this, binOpExpr2.getRight()) && expr1.getResolvedBinOp().equals(((Abstract.BinOpExpression) expr2).getResolvedBinOp());
  }

  @Override
  public Boolean visitBinOpSequence(Abstract.BinOpSequenceExpression expr1, Abstract.Expression expr2) {
    if (expr1.getSequence().isEmpty()) {
      return expr1.getLeft().accept(this, expr2);
    }
    if (!(expr2 instanceof Abstract.BinOpSequenceExpression)) return false;
    Abstract.BinOpSequenceExpression binOpExpr2 = (Abstract.BinOpSequenceExpression) expr2;
    if (!expr1.getLeft().accept(this, binOpExpr2.getLeft())) return false;
    if (expr1.getSequence().size() != binOpExpr2.getSequence().size()) return false;
    for (int i = 0; i < expr1.getSequence().size(); i++) {
      if (!(expr1.getSequence().get(i).binOp == binOpExpr2.getSequence().get(i).binOp && expr1.getSequence().get(i).argument.accept(this, ((Abstract.BinOpSequenceExpression) expr2).getSequence().get(i).argument))) {
        return false;
      }
    }
    return true;
  }

  private boolean comparePattern(Abstract.Pattern pattern1, Abstract.Pattern pattern2) {
    if (pattern1 instanceof Abstract.NamePattern) {
      return pattern2 instanceof Abstract.NamePattern && ((Abstract.NamePattern) pattern1).getName().equals(((Abstract.NamePattern) pattern2).getName());
    }
    if (pattern1 instanceof Abstract.ConstructorPattern) {
      return pattern2 instanceof Abstract.ConstructorPattern && ((Abstract.ConstructorPattern) pattern1).getConstructorName().equals(((Abstract.ConstructorPattern) pattern2).getConstructorName());
    }
    return pattern1 instanceof Abstract.AnyConstructorPattern && pattern2 instanceof Abstract.AnyConstructorPattern;
  }

  private boolean compareClause(Abstract.Clause clause1, Abstract.Clause clause2) {
    if (!(clause1.getArrow() == clause2.getArrow() && clause1.getExpression().accept(this, clause2.getExpression()) && clause1.getPatterns().size() == clause2.getPatterns().size())) return false;
    for (int i = 0; i < clause1.getPatterns().size(); i++) {
      if (!comparePattern(clause1.getPatterns().get(i), clause2.getPatterns().get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean compareElimCase(Abstract.ElimCaseExpression expr1, Abstract.ElimCaseExpression expr2) {
    if (!(expr1.getExpressions().size() == expr2.getExpressions().size() && expr1.getClauses().size() == expr2.getClauses().size())) return false;
    for (int i = 0; i < expr1.getExpressions().size(); i++) {
      if (!expr1.getExpressions().get(i).accept(this, expr2.getExpressions().get(i))) {
        return false;
      }
    }
    for (int i = 0; i < expr1.getClauses().size(); i++) {
      if (!compareClause(expr1.getClauses().get(i), expr2.getClauses().get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitElim(Abstract.ElimExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.ElimExpression && compareElimCase(expr1, (Abstract.ElimExpression) expr2);
  }

  @Override
  public Boolean visitCase(Abstract.CaseExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.CaseExpression && compareElimCase(expr1, (Abstract.CaseExpression) expr2);
  }

  @Override
  public Boolean visitProj(Abstract.ProjExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.ProjExpression && expr1.getField() == ((Abstract.ProjExpression) expr2).getField() && expr1.getExpression().accept(this, ((Abstract.ProjExpression) expr2).getExpression());
  }

  private boolean compareImplementStatement(Abstract.ImplementStatement implStat1, Abstract.ImplementStatement implStat2) {
    return implStat1.getExpression().accept(this, implStat2.getExpression()) && implStat1.getName().equals(implStat2.getName());
  }

  @Override
  public Boolean visitClassExt(Abstract.ClassExtExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.ClassExtExpression)) return false;
    Abstract.ClassExtExpression classExtExpr2 = (Abstract.ClassExtExpression) expr2;
    if (!(expr1.getBaseClassExpression().accept(this, classExtExpr2.getBaseClassExpression()) && expr1.getStatements().size() == classExtExpr2.getStatements().size())) return false;
    for (Iterator<? extends Abstract.ImplementStatement> it1 = expr1.getStatements().iterator(), it2 = classExtExpr2.getStatements().iterator(); it1.hasNext(); ) {
      if (!compareImplementStatement(it1.next(), it2.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitNew(Abstract.NewExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.NewExpression && expr1.getExpression().accept(this, ((Abstract.NewExpression) expr2).getExpression());
  }

  private boolean compareLetClause(Abstract.LetClause clause1, Abstract.LetClause clause2) {
    return clause1.getArrow() == clause2.getArrow() && compareArgs(clause1.getArguments(), clause2.getArguments()) && clause1.getTerm().accept(this, clause2.getTerm()) && (clause1.getResultType() == null && clause2.getResultType() == null || clause1.getResultType() != null && clause2.getResultType() != null && clause1.getResultType().accept(this, clause2.getResultType()));
  }

  @Override
  public Boolean visitLet(Abstract.LetExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.LetExpression)) return false;
    Abstract.LetExpression letExpr2 = (Abstract.LetExpression) expr2;
    if (!(expr1.getExpression().accept(this, letExpr2.getExpression()) && expr1.getClauses().size() == letExpr2.getClauses().size())) return false;
    for (int i = 0; i < expr1.getClauses().size(); i++) {
      if (!compareLetClause(expr1.getClauses().get(i), letExpr2.getClauses().get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitNumericLiteral(Abstract.NumericLiteral expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.NumericLiteral && expr1.getNumber() == ((Abstract.NumericLiteral) expr2).getNumber();
  }
}
