package com.jetbrains.jetpad.vclang.frontend;

import com.google.common.base.Objects;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AbstractCompareVisitor implements AbstractExpressionVisitor<Abstract.Expression, Boolean> {
  private final Map<Abstract.ReferableSourceNode, Abstract.ReferableSourceNode> mySubstitution = new HashMap<>();

  @Override
  public Boolean visitApp(Abstract.AppExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.AppExpression && expr1.getFunction().accept(this, ((Abstract.AppExpression) expr2).getFunction()) && expr1.getArgument().getExpression().accept(this, ((Abstract.AppExpression) expr2).getArgument().getExpression());
  }

  @Override
  public Boolean visitReference(Abstract.ReferenceExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.ReferenceExpression)) return false;
    Abstract.ReferenceExpression defCallExpr2 = (Abstract.ReferenceExpression) expr2;
    Abstract.ReferableSourceNode ref1 = mySubstitution.get(expr1.getReferent());
    if (ref1 == null) {
      ref1 = expr1.getReferent();
    }
    return ref1.equals(defCallExpr2.getReferent());
  }

  @Override
  public Boolean visitInferenceReference(Abstract.InferenceReferenceExpression expr, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.InferenceReferenceExpression && expr.getVariable() == ((Abstract.InferenceReferenceExpression) expr2).getVariable();
  }

  @Override
  public Boolean visitModuleCall(Abstract.ModuleCallExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.ModuleCallExpression && expr1.getPath().equals(((Abstract.ModuleCallExpression) expr2).getPath());
  }

  private boolean compareArg(Abstract.Parameter arg1, Abstract.Parameter arg2) {
    if (arg1.getExplicit() != arg2.getExplicit()) {
      return false;
    }
    if (arg1 instanceof Abstract.TelescopeParameter && arg2 instanceof Abstract.TelescopeParameter) {
      List<? extends Abstract.ReferableSourceNode> list1 = ((Abstract.TelescopeParameter) arg1).getReferableList();
      List<? extends Abstract.ReferableSourceNode> list2 = ((Abstract.TelescopeParameter) arg2).getReferableList();
      if (list1.size() != list2.size()) {
        return false;
      }
      for (int i = 0; i < list1.size(); i++) {
        mySubstitution.put(list1.get(i), list2.get(i));
      }
      return ((Abstract.TelescopeParameter) arg1).getType().accept(this, ((Abstract.TelescopeParameter) arg2).getType());
    }
    if (arg1 instanceof Abstract.TypeParameter && arg2 instanceof Abstract.TypeParameter) {
      return ((Abstract.TypeParameter) arg1).getType().accept(this, ((Abstract.TypeParameter) arg2).getType());
    }
    if (arg1 instanceof Abstract.NameParameter && arg2 instanceof Abstract.NameParameter) {
      mySubstitution.put((Abstract.NameParameter) arg1, (Abstract.NameParameter) arg2);
      return true;
    }
    return false;
  }

  private boolean compareArgs(List<? extends Abstract.Parameter> args1, List<? extends Abstract.Parameter> args2) {
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
    return expr2 instanceof Abstract.LamExpression && compareArgs(expr1.getParameters(), ((Abstract.LamExpression) expr2).getParameters()) && expr1.getBody().accept(this, ((Abstract.LamExpression) expr2).getBody());
  }

  @Override
  public Boolean visitPi(Abstract.PiExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.PiExpression && compareArgs(expr1.getParameters(), ((Abstract.PiExpression) expr2).getParameters()) && expr1.getCodomain().accept(this, ((Abstract.PiExpression) expr2).getCodomain());
  }

  @Override
  public Boolean visitUniverse(Abstract.UniverseExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.UniverseExpression)) {
      return false;
    }
    Abstract.UniverseExpression uni2 = (Abstract.UniverseExpression) expr2;
    return compareLevel(expr1.getPLevel(), uni2.getPLevel()) && compareLevel(expr1.getHLevel(), uni2.getHLevel());
  }

  public boolean compareLevel(Abstract.LevelExpression level1, Abstract.LevelExpression level2) {
    if (level1 == null) {
      return level2 == null || level2 instanceof Abstract.PLevelExpression || level2 instanceof Abstract.HLevelExpression;
    }
    if (level1 instanceof Abstract.PLevelExpression) {
      return level2 instanceof Abstract.PLevelExpression || level2 == null;
    }
    if (level1 instanceof Abstract.HLevelExpression) {
      return level2 instanceof Abstract.HLevelExpression || level2 == null;
    }
    if (level1 instanceof Abstract.InfLevelExpression) {
      return level2 instanceof Abstract.InfLevelExpression;
    }
    if (level1 instanceof Abstract.NumberLevelExpression) {
      return level2 instanceof Abstract.NumberLevelExpression && ((Abstract.NumberLevelExpression) level1).getNumber() == ((Abstract.NumberLevelExpression) level2).getNumber();
    }
    if (level1 instanceof Abstract.SucLevelExpression) {
      return level2 instanceof Abstract.SucLevelExpression && compareLevel(((Abstract.SucLevelExpression) level1).getExpression(), ((Abstract.SucLevelExpression) level2).getExpression());
    }
    if (level1 instanceof Abstract.MaxLevelExpression) {
      if (!(level2 instanceof Abstract.MaxLevelExpression)) {
        return false;
      }
      Abstract.MaxLevelExpression max1 = (Abstract.MaxLevelExpression) level1;
      Abstract.MaxLevelExpression max2 = (Abstract.MaxLevelExpression) level2;
      return compareLevel(max1.getLeft(), max2.getLeft()) && compareLevel(max1.getRight(), max2.getRight());
    }
    throw new IllegalStateException();
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
    return expr2 instanceof Abstract.SigmaExpression && compareArgs(expr1.getParameters(), ((Abstract.SigmaExpression) expr2).getParameters());
  }

  @Override
  public Boolean visitBinOp(Abstract.BinOpExpression expr1, Abstract.Expression expr2) {
    if (expr2 instanceof Abstract.BinOpSequenceExpression && ((Abstract.BinOpSequenceExpression) expr2).getSequence().isEmpty()) {
      return visitBinOp(expr1, ((Abstract.BinOpSequenceExpression) expr2).getLeft());
    }
    if (!(expr2 instanceof Abstract.BinOpExpression)) return false;
    Abstract.BinOpExpression binOpExpr2 = (Abstract.BinOpExpression) expr2;
    return expr1.getLeft().accept(this, binOpExpr2.getLeft()) && expr1.getRight().accept(this, binOpExpr2.getRight()) && expr1.getReferent().equals(((Abstract.BinOpExpression) expr2).getReferent());
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
      return pattern2 instanceof Abstract.NamePattern && Objects.equal(pattern1, pattern2);
    }
    if (pattern1 instanceof Abstract.ConstructorPattern) {
      return pattern2 instanceof Abstract.ConstructorPattern && ((Abstract.ConstructorPattern) pattern1).getConstructorName().equals(((Abstract.ConstructorPattern) pattern2).getConstructorName());
    }
    return pattern1 instanceof Abstract.EmptyPattern && pattern2 instanceof Abstract.EmptyPattern;
  }

  private boolean compareClause(Abstract.FunctionClause clause1, Abstract.FunctionClause clause2) {
    if (!(clause1.getExpression().accept(this, clause2.getExpression()) && clause1.getPatterns().size() == clause2.getPatterns().size())) return false;
    for (int i = 0; i < clause1.getPatterns().size(); i++) {
      if (!comparePattern(clause1.getPatterns().get(i), clause2.getPatterns().get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean compareElimCase(Abstract.CaseExpression expr1, Abstract.CaseExpression expr2) {
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
  public Boolean visitCase(Abstract.CaseExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.CaseExpression && compareElimCase(expr1, (Abstract.CaseExpression) expr2);
  }

  @Override
  public Boolean visitProj(Abstract.ProjExpression expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.ProjExpression && expr1.getField() == ((Abstract.ProjExpression) expr2).getField() && expr1.getExpression().accept(this, ((Abstract.ProjExpression) expr2).getExpression());
  }

  private boolean compareImplementStatement(Abstract.ClassFieldImpl implStat1, Abstract.ClassFieldImpl implStat2) {
    return implStat1.getImplementation().accept(this, implStat2.getImplementation()) && implStat1.getImplementedFieldName().equals(implStat2.getImplementedFieldName());
  }

  @Override
  public Boolean visitClassExt(Abstract.ClassExtExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.ClassExtExpression)) return false;
    Abstract.ClassExtExpression classExtExpr2 = (Abstract.ClassExtExpression) expr2;
    if (!(expr1.getBaseClassExpression().accept(this, classExtExpr2.getBaseClassExpression()) && expr1.getStatements().size() == classExtExpr2.getStatements().size())) return false;
    for (Iterator<? extends Abstract.ClassFieldImpl> it1 = expr1.getStatements().iterator(), it2 = classExtExpr2.getStatements().iterator(); it1.hasNext(); ) {
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
    return compareArgs(clause1.getParameters(), clause2.getParameters()) && clause1.getTerm().accept(this, clause2.getTerm()) && (clause1.getResultType() == null && clause2.getResultType() == null || clause1.getResultType() != null && clause2.getResultType() != null && clause1.getResultType().accept(this, clause2.getResultType()));
  }

  @Override
  public Boolean visitLet(Abstract.LetExpression expr1, Abstract.Expression expr2) {
    if (!(expr2 instanceof Abstract.LetExpression)) return false;
    Abstract.LetExpression letExpr2 = (Abstract.LetExpression) expr2;
    if (expr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }
    for (int i = 0; i < expr1.getClauses().size(); i++) {
      if (!compareLetClause(expr1.getClauses().get(i), letExpr2.getClauses().get(i))) {
        return false;
      }
      mySubstitution.put(expr1.getClauses().get(i), letExpr2.getClauses().get(i));
    }
    return expr1.getExpression().accept(this, letExpr2.getExpression());
  }

  @Override
  public Boolean visitNumericLiteral(Abstract.NumericLiteral expr1, Abstract.Expression expr2) {
    return expr2 instanceof Abstract.NumericLiteral && expr1.getNumber() == ((Abstract.NumericLiteral) expr2).getNumber();
  }
}
