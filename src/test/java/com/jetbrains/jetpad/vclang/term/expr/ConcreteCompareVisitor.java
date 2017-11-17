package com.jetbrains.jetpad.vclang.term.expr;

import com.google.common.base.Objects;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConcreteCompareVisitor implements ConcreteExpressionVisitor<Concrete.Expression, Boolean> {
  private final Map<Referable, Referable> mySubstitution = new HashMap<>();

  @Override
  public Boolean visitApp(Concrete.AppExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.AppExpression && expr1.getFunction().accept(this, ((Concrete.AppExpression) expr2).getFunction()) && expr1.getArgument().getExpression().accept(this, ((Concrete.AppExpression) expr2).getArgument().getExpression());
  }

  @Override
  public Boolean visitReference(Concrete.ReferenceExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.ReferenceExpression)) return false;
    Concrete.ReferenceExpression defCallExpr2 = (Concrete.ReferenceExpression) expr2;
    Referable ref1 = mySubstitution.get(expr1.getReferent());
    if (ref1 == null) {
      ref1 = expr1.getReferent();
    }
    return ref1.equals(defCallExpr2.getReferent());
  }

  @Override
  public Boolean visitInferenceReference(Concrete.InferenceReferenceExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.InferenceReferenceExpression && expr.getVariable() == ((Concrete.InferenceReferenceExpression) expr2).getVariable();
  }

  private boolean compareArg(Concrete.Parameter arg1, Concrete.Parameter arg2) {
    if (arg1.getExplicit() != arg2.getExplicit()) {
      return false;
    }
    if (arg1 instanceof Concrete.TelescopeParameter && arg2 instanceof Concrete.TelescopeParameter) {
      List<? extends Referable> list1 = ((Concrete.TelescopeParameter) arg1).getReferableList();
      List<? extends Referable> list2 = ((Concrete.TelescopeParameter) arg2).getReferableList();
      if (list1.size() != list2.size()) {
        return false;
      }
      for (int i = 0; i < list1.size(); i++) {
        mySubstitution.put(list1.get(i), list2.get(i));
      }
      return ((Concrete.TelescopeParameter) arg1).getType().accept(this, ((Concrete.TelescopeParameter) arg2).getType());
    }
    if (arg1 instanceof Concrete.TypeParameter && arg2 instanceof Concrete.TypeParameter) {
      return ((Concrete.TypeParameter) arg1).getType().accept(this, ((Concrete.TypeParameter) arg2).getType());
    }
    if (arg1 instanceof Concrete.NameParameter && arg2 instanceof Concrete.NameParameter) {
      mySubstitution.put(((Concrete.NameParameter) arg1).getReferable(), ((Concrete.NameParameter) arg2).getReferable());
      return true;
    }
    return false;
  }

  private boolean compareArgs(List<? extends Concrete.Parameter> args1, List<? extends Concrete.Parameter> args2) {
    if (args1.size() != args2.size()) return false;
    for (int i = 0; i < args1.size(); i++) {
      if (!compareArg(args1.get(i), args2.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitLam(Concrete.LamExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.LamExpression && compareArgs(expr1.getParameters(), ((Concrete.LamExpression) expr2).getParameters()) && expr1.getBody().accept(this, ((Concrete.LamExpression) expr2).getBody());
  }

  @Override
  public Boolean visitPi(Concrete.PiExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.PiExpression && compareArgs(expr1.getParameters(), ((Concrete.PiExpression) expr2).getParameters()) && expr1.getCodomain().accept(this, ((Concrete.PiExpression) expr2).getCodomain());
  }

  @Override
  public Boolean visitUniverse(Concrete.UniverseExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.UniverseExpression)) {
      return false;
    }
    Concrete.UniverseExpression uni2 = (Concrete.UniverseExpression) expr2;
    return compareLevel(expr1.getPLevel(), uni2.getPLevel()) && compareLevel(expr1.getHLevel(), uni2.getHLevel());
  }

  public boolean compareLevel(Concrete.LevelExpression level1, Concrete.LevelExpression level2) {
    if (level1 == null) {
      return level2 == null || level2 instanceof Concrete.PLevelExpression || level2 instanceof Concrete.HLevelExpression;
    }
    if (level1 instanceof Concrete.PLevelExpression) {
      return level2 instanceof Concrete.PLevelExpression || level2 == null;
    }
    if (level1 instanceof Concrete.HLevelExpression) {
      return level2 instanceof Concrete.HLevelExpression || level2 == null;
    }
    if (level1 instanceof Concrete.InfLevelExpression) {
      return level2 instanceof Concrete.InfLevelExpression;
    }
    if (level1 instanceof Concrete.NumberLevelExpression) {
      return level2 instanceof Concrete.NumberLevelExpression && ((Concrete.NumberLevelExpression) level1).getNumber() == ((Concrete.NumberLevelExpression) level2).getNumber();
    }
    if (level1 instanceof Concrete.SucLevelExpression) {
      return level2 instanceof Concrete.SucLevelExpression && compareLevel(((Concrete.SucLevelExpression) level1).getExpression(), ((Concrete.SucLevelExpression) level2).getExpression());
    }
    if (level1 instanceof Concrete.MaxLevelExpression) {
      if (!(level2 instanceof Concrete.MaxLevelExpression)) {
        return false;
      }
      Concrete.MaxLevelExpression max1 = (Concrete.MaxLevelExpression) level1;
      Concrete.MaxLevelExpression max2 = (Concrete.MaxLevelExpression) level2;
      return compareLevel(max1.getLeft(), max2.getLeft()) && compareLevel(max1.getRight(), max2.getRight());
    }
    throw new IllegalStateException();
  }

  @Override
  public Boolean visitInferHole(Concrete.InferHoleExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.InferHoleExpression;
  }

  @Override
  public Boolean visitGoal(Concrete.GoalExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.GoalExpression;
  }

  @Override
  public Boolean visitTuple(Concrete.TupleExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.TupleExpression)) return false;
    Concrete.TupleExpression tupleExpr2 = (Concrete.TupleExpression) expr2;
    if (expr1.getFields().size() != tupleExpr2.getFields().size()) return false;
    for (int i = 0; i < expr1.getFields().size(); i++) {
      if (expr1.getFields().get(i).accept(this, tupleExpr2.getFields().get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitSigma(Concrete.SigmaExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.SigmaExpression && compareArgs(expr1.getParameters(), ((Concrete.SigmaExpression) expr2).getParameters());
  }

  @Override
  public Boolean visitBinOpSequence(Concrete.BinOpSequenceExpression expr1, Concrete.Expression expr2) {
    if (expr1.getSequence().isEmpty()) {
      return expr1.getLeft().accept(this, expr2);
    }
    if (!(expr2 instanceof Concrete.BinOpSequenceExpression)) return false;
    Concrete.BinOpSequenceExpression binOpExpr2 = (Concrete.BinOpSequenceExpression) expr2;
    if (!expr1.getLeft().accept(this, binOpExpr2.getLeft())) return false;
    if (expr1.getSequence().size() != binOpExpr2.getSequence().size()) return false;
    for (int i = 0; i < expr1.getSequence().size(); i++) {
      Concrete.Expression arg1 = expr1.getSequence().get(i).argument;
      Concrete.Expression arg2 = ((Concrete.BinOpSequenceExpression) expr2).getSequence().get(i).argument;
      if (!(expr1.getSequence().get(i).binOp == binOpExpr2.getSequence().get(i).binOp && (arg1 == null && arg2 == null || arg1 != null && arg2 != null && arg1.accept(this, arg2)))) {
        return false;
      }
    }
    return true;
  }

  private boolean comparePattern(Concrete.Pattern pattern1, Concrete.Pattern pattern2) {
    if (pattern1 instanceof Concrete.NamePattern) {
      return pattern2 instanceof Concrete.NamePattern && Objects.equal(pattern1, pattern2);
    }
    if (pattern1 instanceof Concrete.ConstructorPattern) {
      return pattern2 instanceof Concrete.ConstructorPattern && ((Concrete.ConstructorPattern) pattern1).getConstructor().equals(((Concrete.ConstructorPattern) pattern2).getConstructor());
    }
    return pattern1 instanceof Concrete.EmptyPattern && pattern2 instanceof Concrete.EmptyPattern;
  }

  private boolean compareClause(Concrete.FunctionClause clause1, Concrete.FunctionClause clause2) {
    if (!((clause1.getExpression() == null ? clause2.getExpression() == null : clause1.getExpression().accept(this, clause2.getExpression())) && clause1.getPatterns().size() == clause2.getPatterns().size())) return false;
    for (int i = 0; i < clause1.getPatterns().size(); i++) {
      if (!comparePattern(clause1.getPatterns().get(i), clause2.getPatterns().get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean compareElimCase(Concrete.CaseExpression expr1, Concrete.CaseExpression expr2) {
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
  public Boolean visitCase(Concrete.CaseExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.CaseExpression && compareElimCase(expr1, (Concrete.CaseExpression) expr2);
  }

  @Override
  public Boolean visitProj(Concrete.ProjExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ProjExpression && expr1.getField() == ((Concrete.ProjExpression) expr2).getField() && expr1.getExpression().accept(this, ((Concrete.ProjExpression) expr2).getExpression());
  }

  private boolean compareImplementStatement(Concrete.ClassFieldImpl implStat1, Concrete.ClassFieldImpl implStat2) {
    return implStat1.getImplementation().accept(this, implStat2.getImplementation()) && implStat1.getImplementedField().equals(implStat2.getImplementedField());
  }

  @Override
  public Boolean visitClassExt(Concrete.ClassExtExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.ClassExtExpression)) return false;
    Concrete.ClassExtExpression classExtExpr2 = (Concrete.ClassExtExpression) expr2;
    if (!(expr1.getBaseClassExpression().accept(this, classExtExpr2.getBaseClassExpression()) && expr1.getStatements().size() == classExtExpr2.getStatements().size())) return false;
    for (Iterator<? extends Concrete.ClassFieldImpl> it1 = expr1.getStatements().iterator(), it2 = classExtExpr2.getStatements().iterator(); it1.hasNext(); ) {
      if (!compareImplementStatement(it1.next(), it2.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitNew(Concrete.NewExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NewExpression && expr1.getExpression().accept(this, ((Concrete.NewExpression) expr2).getExpression());
  }

  private boolean compareLetClause(Concrete.LetClause clause1, Concrete.LetClause clause2) {
    return compareArgs(clause1.getParameters(), clause2.getParameters()) && clause1.getTerm().accept(this, clause2.getTerm()) && (clause1.getResultType() == null && clause2.getResultType() == null || clause1.getResultType() != null && clause2.getResultType() != null && clause1.getResultType().accept(this, clause2.getResultType()));
  }

  @Override
  public Boolean visitLet(Concrete.LetExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.LetExpression)) return false;
    Concrete.LetExpression letExpr2 = (Concrete.LetExpression) expr2;
    if (expr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }
    for (int i = 0; i < expr1.getClauses().size(); i++) {
      if (!compareLetClause(expr1.getClauses().get(i), letExpr2.getClauses().get(i))) {
        return false;
      }
      mySubstitution.put(expr1.getClauses().get(i).getData(), letExpr2.getClauses().get(i).getData());
    }
    return expr1.getExpression().accept(this, letExpr2.getExpression());
  }

  @Override
  public Boolean visitNumericLiteral(Concrete.NumericLiteral expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NumericLiteral && expr1.getNumber().equals(((Concrete.NumericLiteral) expr2).getNumber());
  }
}
