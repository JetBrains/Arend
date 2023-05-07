package org.arend.term.concrete;

import org.arend.naming.reference.Referable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class ConcreteCompareVisitor implements ConcreteExpressionVisitor<Concrete.Expression, Boolean>, ConcreteResolvableDefinitionVisitor<Concrete.ResolvableDefinition, Boolean> {
  private final Map<Referable, Referable> mySubstitution = new HashMap<>();

  public boolean compare(Concrete.Expression expr1, Concrete.Expression expr2) {
    if (expr1 == expr2) {
      return true;
    }
    if (expr1 == null || expr2 == null) {
      return false;
    }
    if (expr1 instanceof Concrete.BinOpSequenceExpression && ((Concrete.BinOpSequenceExpression) expr1).getSequence().size() == 1) {
      expr1 = ((Concrete.BinOpSequenceExpression) expr1).getSequence().get(0).getComponent();
    }
    if (expr2 instanceof Concrete.BinOpSequenceExpression && ((Concrete.BinOpSequenceExpression) expr2).getSequence().size() == 1) {
      expr2 = ((Concrete.BinOpSequenceExpression) expr2).getSequence().get(0).getComponent();
    }
    return expr1.accept(this, expr2);
  }

  public Map<Referable, Referable> getSubstitution() {
    return mySubstitution;
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
    if (!(expr2 instanceof Concrete.ReferenceExpression refExpr2)) return false;
    Referable ref1 = mySubstitution.get(expr1.getReferent());
    if (ref1 == null) {
      ref1 = expr1.getReferent();
    }
    return ref1.equals(refExpr2.getReferent()) && compareLevels(expr1.getPLevels(), refExpr2.getPLevels()) && compareLevels(expr1.getHLevels(), refExpr2.getHLevels());
  }

  private boolean compareLevels(List<Concrete.LevelExpression> levels1, List<Concrete.LevelExpression> levels2) {
    if (levels1 == null) return levels2 == null;
    if (levels1.size() != levels2.size()) return false;
    for (int i = 0; i < levels1.size(); i++) {
      if (!compareLevel(levels1.get(i), levels2.get(i))) return false;
    }
    return true;
  }

  @Override
  public Boolean visitThis(Concrete.ThisExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ThisExpression && expr.getReferent() == ((Concrete.ThisExpression) expr2).getReferent();
  }

  private boolean compareParameter(Concrete.Parameter arg1, Concrete.Parameter arg2) {
    if (arg1.isExplicit() != arg2.isExplicit() || arg1.isProperty() != arg2.isProperty() || arg1.isStrict() != arg2.isStrict()) {
      return false;
    }

    List<? extends Referable> list1 = arg1.getReferableList();
    List<? extends Referable> list2 = arg2.getReferableList();
    if (list1.size() != list2.size()) {
      return false;
    }
    for (int i = 0; i < list1.size(); i++) {
      if (list1.get(i) == null && list2.get(i) != null || list1.get(i) != null && list2.get(i) == null) {
        return false;
      }
      if (list1.get(i) != null) {
        if (!list1.get(i).getRefName().equals(list2.get(i).getRefName())) {
          return false;
        }
        mySubstitution.put(list1.get(i), list2.get(i));
      }
    }
    return compare(arg1.getType(), arg2.getType());
  }

  private void freeParameter(Concrete.Parameter param) {
    for (Referable referable : param.getReferableList()) {
      mySubstitution.remove(referable);
    }
  }

  private boolean compareParameters(List<? extends Concrete.Parameter> args1, List<? extends Concrete.Parameter> args2) {
    if (args1.size() != args2.size()) return false;
    for (int i = 0; i < args1.size(); i++) {
      if (!compareParameter(args1.get(i), args2.get(i))) {
        return false;
      }
    }
    return true;
  }

  private void freeParameters(List<? extends Concrete.Parameter> params) {
    for (Concrete.Parameter param : params) {
      freeParameter(param);
    }
  }

  @Override
  public Boolean visitLam(Concrete.LamExpression expr1, Concrete.Expression expr2) {
    if (expr1 instanceof Concrete.PatternLamExpression lamExpr1) {
      if (!(expr2 instanceof Concrete.PatternLamExpression lamExpr2 && lamExpr1.getPatterns().size() == lamExpr2.getPatterns().size() && lamExpr1.getParameters().size() == lamExpr2.getParameters().size())) return false;
      int j = 0;
      for (int i = 0; i < lamExpr1.getPatterns().size(); i++) {
        Concrete.Pattern pattern1 = lamExpr1.getPatterns().get(i);
        Concrete.Pattern pattern2 = lamExpr2.getPatterns().get(i);
        if ((pattern1 == null) != (pattern2 == null)) return false;
        if (pattern1 == null) {
          Concrete.Parameter parameter1 = lamExpr1.getParameters().get(j);
          Concrete.Parameter parameter2 = lamExpr2.getParameters().get(j++);
          if (!compareParameter(parameter1, parameter2)) return false;
        } else {
          if (!comparePattern(pattern1, pattern2)) return false;
        }
      }
      boolean result = compare(expr1.getBody(), lamExpr2.getBody());
      freePatterns(lamExpr1.getPatterns());
      freeParameters(lamExpr1.getParameters());
      return result;
    }

    if (!(expr2 instanceof Concrete.LamExpression lamExpr2 && compareParameters(expr1.getParameters(), lamExpr2.getParameters()))) return false;
    boolean result = compare(expr1.getBody(), lamExpr2.getBody());
    freeParameters(expr1.getParameters());
    return result;
  }

  @Override
  public Boolean visitPi(Concrete.PiExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.PiExpression && compareParameters(expr1.getParameters(), ((Concrete.PiExpression) expr2).getParameters()))) return false;
    boolean result = compare(expr1.getCodomain(), ((Concrete.PiExpression) expr2).getCodomain());
    freeParameters(expr1.getParameters());
    return result;
  }

  @Override
  public Boolean visitUniverse(Concrete.UniverseExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.UniverseExpression uni2 && compareLevel(expr1.getPLevel(), uni2.getPLevel()) && compareLevel(expr1.getHLevel(), uni2.getHLevel());
  }

  private boolean compareLevel(Concrete.LevelExpression level1, Concrete.LevelExpression level2) {
    if (level1 == null) {
      return level2 == null;
    }
    if (level1 instanceof Concrete.PLevelExpression) {
      return level2 instanceof Concrete.PLevelExpression;
    }
    if (level1 instanceof Concrete.HLevelExpression) {
      return level2 instanceof Concrete.HLevelExpression;
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
    if (level1 instanceof Concrete.MaxLevelExpression max1) {
      return level2 instanceof Concrete.MaxLevelExpression max2 && compareLevel(max1.getLeft(), max2.getLeft()) && compareLevel(max1.getRight(), max2.getRight());
    }
    if (level1 instanceof Concrete.VarLevelExpression var1) {
      Referable ref = mySubstitution.get(var1.getReferent());
      if (ref == null) ref = var1.getReferent();
      return level2 instanceof Concrete.VarLevelExpression var2 && ref.equals(var2.getReferent()) && var1.isInference() == var2.isInference();
    }
    throw new IllegalStateException();
  }

  @Override
  public Boolean visitHole(Concrete.HoleExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.HoleExpression && expr1.isErrorHole() == ((Concrete.HoleExpression) expr2).isErrorHole();
  }

  @Override
  public Boolean visitGoal(Concrete.GoalExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.GoalExpression goalExpr2)) return false;
    if ((expr1.getName() == null) != (goalExpr2.getName() == null) || expr1.useGoalSolver != goalExpr2.useGoalSolver || (expr1.originalExpression == null) != (goalExpr2.originalExpression == null) || (expr1.expression == null) != (goalExpr2.expression == null)) return false;
    if (expr1.getName() != null && !expr1.getName().equals(goalExpr2.getName())) return false;
    if (expr1.expression != null && !expr1.expression.accept(this, goalExpr2.expression)) return false;
    return expr1.originalExpression == null || expr1.originalExpression.accept(this, goalExpr2.originalExpression);
  }

  @Override
  public Boolean visitApplyHole(Concrete.ApplyHoleExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ApplyHoleExpression;
  }

  @Override
  public Boolean visitTuple(Concrete.TupleExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.TupleExpression)) return false;
    return compareExpressionList(expr1.getFields(), ((Concrete.TupleExpression) expr2).getFields());
  }

  @Override
  public Boolean visitSigma(Concrete.SigmaExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.SigmaExpression)) return false;
    boolean result = compareParameters(expr1.getParameters(), ((Concrete.SigmaExpression) expr2).getParameters());
    freeParameters(expr1.getParameters());
    return result;
  }

  @Override
  public Boolean visitBinOpSequence(Concrete.BinOpSequenceExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.BinOpSequenceExpression binOpExpr2)) return false;
    if (expr1.getSequence().size() != binOpExpr2.getSequence().size()) return false;
    for (int i = 0; i < expr1.getSequence().size(); i++) {
      if (expr1.getSequence().get(i).fixity != binOpExpr2.getSequence().get(i).fixity || expr1.getSequence().get(i).isExplicit != binOpExpr2.getSequence().get(i).isExplicit) return false;
      Concrete.Expression arg1 = expr1.getSequence().get(i).getComponent();
      Concrete.Expression arg2 = ((Concrete.BinOpSequenceExpression) expr2).getSequence().get(i).getComponent();
      if (!compare(arg1, arg2)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean comparePattern(Concrete.Pattern pattern1, Concrete.Pattern pattern2) {
    if (pattern1.isExplicit() != pattern2.isExplicit()) {
      return false;
    }

    if ((pattern1.getAsReferable() == null) != (pattern2.getAsReferable() == null)) {
      return false;
    }
    if (pattern1.getAsReferable() != null) {
      if (pattern2.getAsReferable() == null || !compare(pattern1.getAsReferable().type, pattern2.getAsReferable().type)) {
        return false;
      }
      if ((pattern1.getAsReferable().referable == null) != (pattern2.getAsReferable().referable == null)) {
        return false;
      }
      if (pattern1.getAsReferable().referable != null && !pattern1.getAsReferable().referable.getRefName().equals(pattern2.getAsReferable().referable.getRefName())) {
        return false;
      }
      if (pattern1.getAsReferable().referable != null) {
        mySubstitution.put(pattern1.getAsReferable().referable, pattern2.getAsReferable().referable);
      }
    }

    if (pattern1 instanceof Concrete.NamePattern namePattern1) {
      if (!(pattern2 instanceof Concrete.NamePattern namePattern2 && compare(namePattern1.type, namePattern2.type) && (namePattern1.getReferable() == null) == (namePattern2.getReferable() == null))) {
        return false;
      }
      if (namePattern1.getReferable() != null) {
        if (namePattern2.getReferable() != null && !namePattern1.getReferable().getRefName().equals(namePattern2.getReferable().getRefName())) {
          return false;
        }
        mySubstitution.put(namePattern1.getReferable(), namePattern2.getReferable());
      }
      return true;
    }

    if (pattern1 instanceof Concrete.NumberPattern) {
      if (!(pattern2 instanceof Concrete.NumberPattern)) {
        return false;
      }
      return ((Concrete.NumberPattern) pattern1).getNumber() == ((Concrete.NumberPattern) pattern2).getNumber();
    }

    if (pattern1 instanceof Concrete.ConstructorPattern conPattern1) {
      if (!(pattern2 instanceof Concrete.ConstructorPattern conPattern2)) {
        return false;
      }

      return Objects.equals(conPattern1.getConstructor(), conPattern2.getConstructor()) && comparePatterns(conPattern1.getPatterns(), conPattern2.getPatterns());
    }

    if (pattern1 instanceof Concrete.TuplePattern) {
      return pattern2 instanceof Concrete.TuplePattern && comparePatterns(((Concrete.TuplePattern) pattern1).getPatterns(), ((Concrete.TuplePattern) pattern2).getPatterns());
    }

    throw new IllegalStateException();
  }

  private void freePattern(Concrete.Pattern pattern) {
    if (pattern == null) return;
    if (pattern.getAsReferable() != null && pattern.getAsReferable().referable != null) {
      mySubstitution.remove(pattern.getAsReferable().referable);
    }
    if (pattern instanceof Concrete.NamePattern) {
      mySubstitution.remove(((Concrete.NamePattern) pattern).getReferable());
    }
    freePatterns(pattern.getPatterns());
  }

  private void freePatterns(List<? extends Concrete.Pattern> patterns) {
    for (Concrete.Pattern pattern : patterns) {
      freePattern(pattern);
    }
  }

  private boolean comparePatterns(List<Concrete.Pattern> patterns1, List<Concrete.Pattern> patterns2) {
    if (patterns1.size() != patterns2.size()) {
      return false;
    }

    for (int i = 0; i < patterns1.size(); i++) {
      if (!comparePattern(patterns1.get(i), patterns2.get(i))) {
        return false;
      }
    }

    return true;
  }

  private boolean compareClause(Concrete.Clause clause1, Concrete.Clause clause2, Supplier<Boolean> bodyHandler) {
    if (clause1.getPatterns() == clause2.getPatterns()) {
      return bodyHandler == null || bodyHandler.get();
    }
    if (clause1.getPatterns() == null || clause2.getPatterns() == null) {
      return false;
    }
    boolean result = comparePatterns(clause1.getPatterns(), clause2.getPatterns());
    if (clause1 instanceof Concrete.FunctionClause) {
      if (!(clause2 instanceof Concrete.FunctionClause)) return false;
      Concrete.Expression body1 = ((Concrete.FunctionClause) clause1).expression;
      Concrete.Expression body2 = ((Concrete.FunctionClause) clause2).expression;
      if ((body1 == null) != (body2 == null) || body1 != null && !compare(body1, body2)) return false;
    }
    if (result && bodyHandler != null && !bodyHandler.get()) return false;
    freePatterns(clause1.getPatterns());
    return result;
  }

  private boolean compareClauses(List<? extends Concrete.Clause> clauses1, List<? extends Concrete.Clause> clauses2) {
    if (clauses1.size() != clauses2.size()) {
      return false;
    }
    for (int i = 0; i < clauses1.size(); i++) {
      if (!(compareClause(clauses1.get(i), clauses2.get(i), null))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitCase(Concrete.CaseExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.CaseExpression case2)) {
      return false;
    }
    if (expr1.getArguments().size() != case2.getArguments().size()) {
      return false;
    }
    for (int i = 0; i < expr1.getArguments().size(); i++) {
      Concrete.CaseArgument caseArg1 = expr1.getArguments().get(i);
      Concrete.CaseArgument caseArg2 = case2.getArguments().get(i);
      if (caseArg1.isElim != caseArg2.isElim || !(compare(caseArg1.expression, caseArg2.expression) && compare(caseArg1.type, caseArg2.type) && (caseArg1.referable == null) == (caseArg2.referable == null))) {
        return false;
      }
      if (caseArg1.referable != null) {
        mySubstitution.put(caseArg1.referable, caseArg2.referable);
      }
    }
    if (!(compare(expr1.getResultType(), case2.getResultType()) && compare(expr1.getResultTypeLevel(), case2.getResultTypeLevel()))) return false;
    for (Concrete.CaseArgument argument : expr1.getArguments()) {
      if (argument.referable != null) {
        mySubstitution.remove(argument.referable);
      }
    }
    return compareClauses(expr1.getClauses(), case2.getClauses());
  }

  @Override
  public Boolean visitEval(Concrete.EvalExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.EvalExpression eval2 && expr.isPEval() == eval2.isPEval() && compare(expr.getExpression(), eval2.getExpression());
  }

  @Override
  public Boolean visitBox(Concrete.BoxExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.BoxExpression && compare(expr.getExpression(), ((Concrete.BoxExpression) expr2).getExpression());
  }

  @Override
  public Boolean visitProj(Concrete.ProjExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ProjExpression && expr1.getField() == ((Concrete.ProjExpression) expr2).getField() && compare(expr1.getExpression(), ((Concrete.ProjExpression) expr2).getExpression());
  }

  private boolean compareImplementStatement(Concrete.ClassFieldImpl implStat1, Concrete.ClassFieldImpl implStat2) {
    return compareImplementStatements(implStat1.getSubCoclauseList(), implStat2.getSubCoclauseList()) && (implStat1.implementation == implStat2.implementation || implStat1.implementation != null && implStat2.implementation != null && compare(implStat1.implementation, implStat2.implementation)) && Objects.equals(implStat1.getImplementedField(), implStat2.getImplementedField());
  }

  private boolean compareImplementStatements(List<Concrete.ClassFieldImpl> implStats1, List<Concrete.ClassFieldImpl> implStats2) {
    if (implStats1.size() != implStats2.size()) {
      return false;
    }
    for (int i = 0; i < implStats1.size(); i++) {
      if (!compareImplementStatement(implStats1.get(i), implStats2.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean compareCoClauseElement(Concrete.CoClauseElement element1, Concrete.CoClauseElement element2) {
    if (element1 instanceof Concrete.ClassFieldImpl && element2 instanceof Concrete.ClassFieldImpl) {
      return compareImplementStatement((Concrete.ClassFieldImpl) element1, (Concrete.ClassFieldImpl) element2);
    }
    return false;
  }

  private boolean compareCoClauseElements(List<Concrete.CoClauseElement> elements1, List<Concrete.CoClauseElement> elements2) {
    if (elements1.size() != elements2.size()) {
      return false;
    }
    for (int i = 0; i < elements1.size(); i++) {
      if (!compareCoClauseElement(elements1.get(i), elements2.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitClassExt(Concrete.ClassExtExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ClassExtExpression classExtExpr2 && compare(expr1.getBaseClassExpression(), classExtExpr2.getBaseClassExpression()) && compareImplementStatements(expr1.getStatements(), classExtExpr2.getStatements());
  }

  @Override
  public Boolean visitNew(Concrete.NewExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NewExpression && compare(expr1.getExpression(), ((Concrete.NewExpression) expr2).getExpression());
  }

  private boolean compareLetClause(Concrete.LetClause clause1, Concrete.LetClause clause2) {
    boolean result = compareParameters(clause1.getParameters(), clause2.getParameters()) && compare(clause1.getTerm(), clause2.getTerm()) && (clause1.getResultType() == null && clause2.getResultType() == null || clause1.getResultType() != null && clause2.getResultType() != null && compare(clause1.getResultType(), clause2.getResultType()));
    freeParameters(clause1.getParameters());
    return result;
  }

  @Override
  public Boolean visitLet(Concrete.LetExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.LetExpression letExpr2)) return false;
    if (expr1.isHave() != letExpr2.isHave() || expr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }
    for (int i = 0; i < expr1.getClauses().size(); i++) {
      if (!comparePattern(expr1.getClauses().get(i).getPattern(), letExpr2.getClauses().get(i).getPattern())) return false;
      if (!compareLetClause(expr1.getClauses().get(i), letExpr2.getClauses().get(i))) {
        return false;
      }
    }
    boolean result = compare(expr1.getExpression(), letExpr2.getExpression());
    for (Concrete.LetClause clause : expr1.getClauses()) {
      freePattern(clause.getPattern());
    }
    return result;
  }

  @Override
  public Boolean visitNumericLiteral(Concrete.NumericLiteral expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NumericLiteral && expr1.getNumber().equals(((Concrete.NumericLiteral) expr2).getNumber());
  }

  @Override
  public Boolean visitStringLiteral(Concrete.StringLiteral expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.StringLiteral && expr1.getUnescapedString().equals(((Concrete.StringLiteral) expr2).getUnescapedString());
  }

  @Override
  public Boolean visitQNameLiteral(Concrete.QNameLiteral expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.QNameLiteral && expr1.getReference().equals(((Concrete.QNameLiteral) expr2).getReference());
  }

  @Override
  public Boolean visitTyped(Concrete.TypedExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.TypedExpression && compare(expr.expression, ((Concrete.TypedExpression) expr2).expression) && compare(expr.type, ((Concrete.TypedExpression) expr2).type);
  }

  private boolean compareExpressionList(List<? extends Concrete.Expression> list1, List<? extends Concrete.Expression> list2) {
    if (list1.size() != list2.size()) {
      return false;
    }
    for (int i = 0; i < list1.size(); i++) {
      if (!compare(list1.get(i), list2.get(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean compare(Concrete.ReferableDefinition def1, Concrete.ReferableDefinition def2) {
    ConcreteCompareVisitor visitor = new ConcreteCompareVisitor();
    if (def1 instanceof Concrete.Definition) {
      return def2 instanceof Concrete.Definition && Objects.equals(((Concrete.Definition) def1).enclosingClass, ((Concrete.Definition) def2).enclosingClass) && ((Concrete.Definition) def1).accept(visitor, (Concrete.Definition) def2);
    }
    if (def1 instanceof Concrete.Constructor) {
      boolean result = def2 instanceof Concrete.Constructor && visitor.compareConstructor((Concrete.Constructor) def1, (Concrete.Constructor) def2);
      visitor.mySubstitution.remove(def1.getData());
      return result;
    }
    if (def1 instanceof Concrete.ClassField) {
      boolean result = def2 instanceof Concrete.ClassField && visitor.compareField((Concrete.ClassField) def1, (Concrete.ClassField) def2);
      visitor.mySubstitution.remove(def1.getData());
      return result;
    }
    return false;
  }

  private boolean compareLevelParameters(Concrete.LevelParameters params1, Concrete.LevelParameters params2) {
    if ((params1 == null) != (params2 == null)) return false;
    if (params1 == null) return true;
    if (params1.isIncreasing != params2.isIncreasing || params1.referables.size() != params2.referables.size()) return false;
    for (int i = 0; i < params1.referables.size(); i++) {
      mySubstitution.put(params1.referables.get(i), params2.referables.get(i));
    }
    return true;
  }

  private boolean compareLevelParameters(Concrete.ResolvableDefinition def1, Concrete.ResolvableDefinition def2) {
    return compareLevelParameters(def1.pLevelParameters, def2.pLevelParameters) && compareLevelParameters(def1.hLevelParameters, def2.hLevelParameters);
  }

  @Override
  public Boolean visitFunction(Concrete.BaseFunctionDefinition def, Concrete.ResolvableDefinition def2) {
    if (!(def2 instanceof Concrete.BaseFunctionDefinition fun2)) {
      return false;
    }

    mySubstitution.put(def.getData(), fun2.getData());

    if (def.getKind() != fun2.getKind()) {
      return false;
    }
    if (!compareLevelParameters(def, def2) || !compareParameters(def.getParameters(), fun2.getParameters())) {
      return false;
    }
    if ((def.getResultType() != null || fun2.getResultType() != null) && (def.getResultType() == null || fun2.getResultType() == null || !compare(def.getResultType(), fun2.getResultType()))) {
      return false;
    }
    if ((def.getResultTypeLevel() != null || fun2.getResultTypeLevel() != null) && (def.getResultTypeLevel() == null || fun2.getResultTypeLevel() == null || !compare(def.getResultTypeLevel(), fun2.getResultTypeLevel()))) {
      return false;
    }
    boolean result;
    if (def.getBody() instanceof Concrete.TermFunctionBody) {
      result = fun2.getBody() instanceof Concrete.TermFunctionBody && compare(((Concrete.TermFunctionBody) def.getBody()).getTerm(), ((Concrete.TermFunctionBody) fun2.getBody()).getTerm());
    } else if (def.getBody() instanceof Concrete.CoelimFunctionBody) {
      result = fun2.getBody() instanceof Concrete.CoelimFunctionBody && compareCoClauseElements(def.getBody().getCoClauseElements(), fun2.getBody().getCoClauseElements());
    } else if (def.getBody() instanceof Concrete.ElimFunctionBody elim1) {
      result = fun2.getBody() instanceof Concrete.ElimFunctionBody elim2 && compareExpressionList(elim1.getEliminatedReferences(), elim2.getEliminatedReferences()) && compareClauses(elim1.getClauses(), elim2.getClauses());
    } else {
      return false;
    }
    freeParameters(def.getParameters());
    mySubstitution.remove(def.getData());
    return result;
  }

  @Override
  public Boolean visitData(Concrete.DataDefinition def, Concrete.ResolvableDefinition def2) {
    if (!(def2 instanceof Concrete.DataDefinition data2)) {
      return false;
    }

    mySubstitution.put(def.getData(), data2.getData());

    if (!(compareLevelParameters(def, def2) && compareParameters(def.getParameters(), data2.getParameters()) && (def.getUniverse() == data2.getUniverse() || visitUniverse(def.getUniverse(), data2.getUniverse())))) {
      return false;
    }
    List<Concrete.ReferenceExpression> elimRefs1 = def.getEliminatedReferences();
    List<Concrete.ReferenceExpression> elimRefs2 = data2.getEliminatedReferences();
    if (elimRefs1 == null && elimRefs2 != null || elimRefs1 != null && elimRefs2 == null) {
      return false;
    }
    if (elimRefs1 != null) {
      if (elimRefs1.size() != elimRefs2.size()) {
        return false;
      }
      for (int i = 0; i < elimRefs1.size(); i++) {
        if (!compare(elimRefs1.get(i), elimRefs2.get(i))) {
          return false;
        }
      }
    }
    if (def.isTruncated() != data2.isTruncated()) {
      return false;
    }
    if (def.getConstructorClauses().size() != data2.getConstructorClauses().size()) {
      return false;
    }
    for (int i = 0; i < def.getConstructorClauses().size(); i++) {
      Concrete.ConstructorClause clause1 = def.getConstructorClauses().get(i);
      Concrete.ConstructorClause clause2 = data2.getConstructorClauses().get(i);
      if (!compareClause(def.getConstructorClauses().get(i), data2.getConstructorClauses().get(i), () -> {
        if (clause1.getConstructors().size() != clause2.getConstructors().size()) {
          return false;
        }
        for (int j = 0; j < clause1.getConstructors().size(); j++) {
          if (!compareConstructor(clause1.getConstructors().get(j), clause2.getConstructors().get(j))) {
            return false;
          }
        }
        return true;
      })) {
        return false;
      }
    }
    freeParameters(def.getParameters());
    for (Concrete.ConstructorClause constructorClause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : constructorClause.getConstructors()) {
        mySubstitution.remove(constructor.getData());
      }
    }
    mySubstitution.remove(def.getData());
    return true;
  }

  private boolean compareConstructor(Concrete.Constructor con1, Concrete.Constructor con2) {
    mySubstitution.put(con1.getData(), con2.getData());
    boolean result = compareParameters(con1.getParameters(), con2.getParameters()) && compare(con1.getResultType(), con2.getResultType()) && compareExpressionList(con1.getEliminatedReferences(), con2.getEliminatedReferences()) && compareClauses(con1.getClauses(), con2.getClauses());
    freeParameters(con1.getParameters());
    return result;
  }

  @Override
  public Boolean visitClass(Concrete.ClassDefinition def, Concrete.ResolvableDefinition def2) {
    if (!(def2 instanceof Concrete.ClassDefinition class2)) {
      return false;
    }

    mySubstitution.put(def.getData(), class2.getData());

    if (!compareLevelParameters(def, def2) || !compareExpressionList(def.getSuperClasses(), class2.getSuperClasses())) {
      return false;
    }
    if (def.getElements().size() != class2.getElements().size()) {
      return false;
    }
    for (int i = 0; i < def.getElements().size(); i++) {
      Concrete.ClassElement element1 = def.getElements().get(i);
      Concrete.ClassElement element2 = class2.getElements().get(i);
      if (element1 instanceof Concrete.ClassField && element2 instanceof Concrete.ClassField) {
        if (!compareField((Concrete.ClassField) element1, (Concrete.ClassField) element2)) {
          return false;
        }
      } else if (element1 instanceof Concrete.ClassFieldImpl && element2 instanceof Concrete.ClassFieldImpl) {
        if (!compareImplementStatement((Concrete.ClassFieldImpl) element1, (Concrete.ClassFieldImpl) element2)) {
          return false;
        }
      } else if (element1 instanceof Concrete.OverriddenField && element2 instanceof Concrete.OverriddenField) {
        if (!compareOverriddenField((Concrete.OverriddenField) element1, (Concrete.OverriddenField) element2)) {
          return false;
        }
      } else {
        return false;
      }
    }
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        mySubstitution.remove(element.getData());
      }
    }
    mySubstitution.remove(def.getData());
    return Objects.equals(def.getClassifyingField(), class2.getClassifyingField());
  }

  private boolean compareOverriddenField(Concrete.OverriddenField field1, Concrete.OverriddenField field2) {
    boolean result = Objects.equals(field1.getOverriddenField(), field2.getOverriddenField()) && compareParameters(field1.getParameters(), field2.getParameters()) && compare(field1.getResultType(), field2.getResultType()) && compare(field1.getResultTypeLevel(), field2.getResultTypeLevel());
    freeParameters(field1.getParameters());
    return result;
  }

  private boolean compareField(Concrete.ClassField field1, Concrete.ClassField field2) {
    mySubstitution.put(field1.getData(), field2.getData());
    boolean result = field1.isExplicit() == field2.isExplicit() && compareParameters(field1.getParameters(), field2.getParameters()) && compare(field1.getResultType(), field2.getResultType()) && compare(field1.getResultTypeLevel(), field2.getResultTypeLevel());
    freeParameters(field1.getParameters());
    return result;
  }

  @Override
  public Boolean visitMeta(DefinableMetaDefinition def, Concrete.ResolvableDefinition def2) {
    return def2 instanceof DefinableMetaDefinition meta2 && compareParameters(def.getParameters(), meta2.getParameters()) && (def.body == null) == (meta2.body == null) && (def.body == null || def.body.accept(this, meta2.body));
  }
}
