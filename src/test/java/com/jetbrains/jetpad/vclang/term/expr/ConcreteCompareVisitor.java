package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;

import java.util.*;

public class ConcreteCompareVisitor implements ConcreteExpressionVisitor<Concrete.Expression, Boolean> {
  private final Map<Referable, Referable> mySubstitution = new HashMap<>();

  public boolean compare(Concrete.Expression expr1, Concrete.Expression expr2) {
    if (expr1 instanceof Concrete.BinOpSequenceExpression && ((Concrete.BinOpSequenceExpression) expr1).getSequence().size() == 1) {
      expr1 = ((Concrete.BinOpSequenceExpression) expr1).getSequence().get(0).expression;
    }
    if (expr2 instanceof Concrete.BinOpSequenceExpression && ((Concrete.BinOpSequenceExpression) expr2).getSequence().size() == 1) {
      expr2 = ((Concrete.BinOpSequenceExpression) expr2).getSequence().get(0).expression;
    }
    return expr1.accept(this, expr2);
  }

  @Override
  public Boolean visitApp(Concrete.AppExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.AppExpression && compare(expr1.getFunction(), ((Concrete.AppExpression) expr2).getFunction()) && expr1.getArguments().size() == ((Concrete.AppExpression) expr2).getArguments().size())) {
      return false;
    }
    for (int i = 0; i < expr1.getArguments().size(); i++) {
      Concrete.Argument argument2 = ((Concrete.AppExpression) expr2).getArguments().get(i);
      if (!(expr1.getArguments().get(i).isExplicit() == argument2.isExplicit() && compare(expr1.getArguments().get(i).expression, argument2.expression))) {
        return false;
      }
    }
    return true;
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
      return compare(((Concrete.TelescopeParameter) arg1).getType(), ((Concrete.TelescopeParameter) arg2).getType());
    }
    if (arg1 instanceof Concrete.TypeParameter && arg2 instanceof Concrete.TypeParameter) {
      return compare(((Concrete.TypeParameter) arg1).getType(), ((Concrete.TypeParameter) arg2).getType());
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
    return expr2 instanceof Concrete.LamExpression && compareArgs(expr1.getParameters(), ((Concrete.LamExpression) expr2).getParameters()) && compare(expr1.getBody(), ((Concrete.LamExpression) expr2).getBody());
  }

  @Override
  public Boolean visitPi(Concrete.PiExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.PiExpression && compareArgs(expr1.getParameters(), ((Concrete.PiExpression) expr2).getParameters()) && compare(expr1.getCodomain(), ((Concrete.PiExpression) expr2).getCodomain());
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
  public Boolean visitHole(Concrete.HoleExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.HoleExpression && (expr1.getError() == null) == (((Concrete.HoleExpression) expr2).getError() == null);
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
      if (compare(expr1.getFields().get(i), tupleExpr2.getFields().get(i))) {
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
    if (!(expr2 instanceof Concrete.BinOpSequenceExpression)) return false;
    Concrete.BinOpSequenceExpression binOpExpr2 = (Concrete.BinOpSequenceExpression) expr2;
    if (expr1.getSequence().size() != binOpExpr2.getSequence().size()) return false;
    for (int i = 0; i < expr1.getSequence().size(); i++) {
      if (expr1.getSequence().get(i).fixity != binOpExpr2.getSequence().get(i).fixity || expr1.getSequence().get(i).isExplicit != binOpExpr2.getSequence().get(i).isExplicit) return false;
      Concrete.Expression arg1 = expr1.getSequence().get(i).expression;
      Concrete.Expression arg2 = ((Concrete.BinOpSequenceExpression) expr2).getSequence().get(i).expression;
      if (!compare(arg1, arg2)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean comparePattern(Concrete.Pattern pattern1, Concrete.Pattern pattern2) {
    if (pattern1 instanceof Concrete.NamePattern) {
      if (!(pattern2 instanceof Concrete.NamePattern)) {
        return false;
      }
      mySubstitution.put(((Concrete.NamePattern) pattern1).getReferable(), ((Concrete.NamePattern) pattern2).getReferable());
      return true;
    }
    if (pattern1 instanceof Concrete.ConstructorPattern) {
      if (!(pattern2 instanceof Concrete.ConstructorPattern)) {
        return false;
      }

      Concrete.ConstructorPattern conPattern1 = (Concrete.ConstructorPattern) pattern1;
      Concrete.ConstructorPattern conPattern2 = (Concrete.ConstructorPattern) pattern2;
      if (!conPattern1.getConstructor().equals(conPattern2.getConstructor()) || conPattern1.getPatterns().size() != conPattern2.getPatterns().size()) {
        return false;
      }

      for (int i = 0; i < conPattern1.getPatterns().size(); i++) {
        if (!comparePattern(conPattern1.getPatterns().get(i), conPattern2.getPatterns().get(i))) {
          return false;
        }
      }

      return true;
    }
    return pattern1 instanceof Concrete.EmptyPattern && pattern2 instanceof Concrete.EmptyPattern;
  }

  private boolean compareClause(Concrete.FunctionClause clause1, Concrete.FunctionClause clause2) {
    for (int i = 0; i < clause1.getPatterns().size(); i++) {
      if (!comparePattern(clause1.getPatterns().get(i), clause2.getPatterns().get(i))) {
        return false;
      }
    }
    return (clause1.getExpression() == null ? clause2.getExpression() == null : compare(clause1.getExpression(), clause2.getExpression())) && clause1.getPatterns().size() == clause2.getPatterns().size();
  }

  private boolean compareElimCase(Concrete.CaseExpression expr1, Concrete.CaseExpression expr2) {
    if (!(expr1.getExpressions().size() == expr2.getExpressions().size() && expr1.getClauses().size() == expr2.getClauses().size())) return false;
    for (int i = 0; i < expr1.getExpressions().size(); i++) {
      if (!compare(expr1.getExpressions().get(i), expr2.getExpressions().get(i))) {
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
    return expr2 instanceof Concrete.ProjExpression && expr1.getField() == ((Concrete.ProjExpression) expr2).getField() && compare(expr1.getExpression(), ((Concrete.ProjExpression) expr2).getExpression());
  }

  private boolean compareImplementStatement(Concrete.ClassFieldImpl implStat1, Concrete.ClassFieldImpl implStat2) {
    return compare(implStat1.getImplementation(), implStat2.getImplementation()) && Objects.equals(implStat1.getImplementedField(), implStat2.getImplementedField());
  }

  @Override
  public Boolean visitClassExt(Concrete.ClassExtExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.ClassExtExpression)) return false;
    Concrete.ClassExtExpression classExtExpr2 = (Concrete.ClassExtExpression) expr2;
    if (!(compare(expr1.getBaseClassExpression(), classExtExpr2.getBaseClassExpression()) && expr1.getStatements().size() == classExtExpr2.getStatements().size())) return false;
    for (Iterator<? extends Concrete.ClassFieldImpl> it1 = expr1.getStatements().iterator(), it2 = classExtExpr2.getStatements().iterator(); it1.hasNext(); ) {
      if (!compareImplementStatement(it1.next(), it2.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitNew(Concrete.NewExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NewExpression && compare(expr1.getExpression(), ((Concrete.NewExpression) expr2).getExpression());
  }

  private boolean compareLetClause(Concrete.LetClause clause1, Concrete.LetClause clause2) {
    return compareArgs(clause1.getParameters(), clause2.getParameters()) && compare(clause1.getTerm(), clause2.getTerm()) && (clause1.getResultType() == null && clause2.getResultType() == null || clause1.getResultType() != null && clause2.getResultType() != null && compare(clause1.getResultType(), clause2.getResultType()));
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
    return compare(expr1.getExpression(), letExpr2.getExpression());
  }

  @Override
  public Boolean visitNumericLiteral(Concrete.NumericLiteral expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NumericLiteral && expr1.getNumber().equals(((Concrete.NumericLiteral) expr2).getNumber());
  }
}
