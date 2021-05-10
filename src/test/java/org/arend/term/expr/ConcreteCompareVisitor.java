package org.arend.term.expr;

import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.ConcreteExpressionVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConcreteCompareVisitor implements ConcreteExpressionVisitor<Concrete.Expression, Boolean>, ConcreteDefinitionVisitor<Concrete.Definition, Boolean> {
  private final Map<Referable, Referable> mySubstitution = new HashMap<>();

  public boolean compare(Concrete.Expression expr1, Concrete.Expression expr2) {
    if (expr1 == expr2) {
      return true;
    }
    if (expr1 == null || expr2 == null) {
      return false;
    }
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
  public Boolean visitThis(Concrete.ThisExpression expr, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ThisExpression && expr.getReferent() == ((Concrete.ThisExpression) expr2).getReferent();
  }

  private boolean compareParameter(Concrete.Parameter arg1, Concrete.Parameter arg2) {
    if (arg1.isExplicit() != arg2.isExplicit()) {
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
        mySubstitution.put(list1.get(i), list2.get(i));
      }
    }
    return compare(arg1.getType(), arg2.getType());
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

  @Override
  public Boolean visitLam(Concrete.LamExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.LamExpression && compareParameters(expr1.getParameters(), ((Concrete.LamExpression) expr2).getParameters()) && compare(expr1.getBody(), ((Concrete.LamExpression) expr2).getBody());
  }

  @Override
  public Boolean visitPi(Concrete.PiExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.PiExpression && compareParameters(expr1.getParameters(), ((Concrete.PiExpression) expr2).getParameters()) && compare(expr1.getCodomain(), ((Concrete.PiExpression) expr2).getCodomain());
  }

  @Override
  public Boolean visitUniverse(Concrete.UniverseExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.UniverseExpression)) {
      return false;
    }
    Concrete.UniverseExpression uni2 = (Concrete.UniverseExpression) expr2;
    return compareLevel(expr1.getPLevel(), uni2.getPLevel()) && compareLevel(expr1.getHLevel(), uni2.getHLevel());
  }

  private boolean compareLevel(Concrete.LevelExpression level1, Concrete.LevelExpression level2) {
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
    return expr2 instanceof Concrete.HoleExpression && expr1.isErrorHole() == ((Concrete.HoleExpression) expr2).isErrorHole();
  }

  @Override
  public Boolean visitGoal(Concrete.GoalExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.GoalExpression;
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
    return expr2 instanceof Concrete.SigmaExpression && compareParameters(expr1.getParameters(), ((Concrete.SigmaExpression) expr2).getParameters());
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
    if (pattern1.isExplicit() != pattern2.isExplicit()) {
      return false;
    }

    if ((pattern1.getAsReferable() == null) != (pattern2.getAsReferable() == null)) {
      return false;
    }
    if (pattern1.getAsReferable() != null) {
      if (!compare(pattern1.getAsReferable().type, pattern2.getAsReferable().type)) {
        return false;
      }
      if ((pattern1.getAsReferable().referable == null) != (pattern2.getAsReferable().referable == null)) {
        return false;
      }
      if (pattern1.getAsReferable().referable != null) {
        mySubstitution.put(pattern1.getAsReferable().referable, pattern2.getAsReferable().referable);
      }
    }

    if (pattern1 instanceof Concrete.NamePattern) {
      if (!(pattern2 instanceof Concrete.NamePattern && compare(((Concrete.NamePattern) pattern1).type, ((Concrete.NamePattern) pattern2).type))) {
        return false;
      }
      mySubstitution.put(((Concrete.NamePattern) pattern1).getReferable(), ((Concrete.NamePattern) pattern2).getReferable());
      return true;
    }

    if (pattern1 instanceof Concrete.NumberPattern) {
      if (!(pattern2 instanceof Concrete.NumberPattern)) {
        return false;
      }
      return ((Concrete.NumberPattern) pattern1).getNumber() == ((Concrete.NumberPattern) pattern2).getNumber();
    }

    if (pattern1 instanceof Concrete.ConstructorPattern) {
      if (!(pattern2 instanceof Concrete.ConstructorPattern)) {
        return false;
      }

      Concrete.ConstructorPattern conPattern1 = (Concrete.ConstructorPattern) pattern1;
      Concrete.ConstructorPattern conPattern2 = (Concrete.ConstructorPattern) pattern2;
      return conPattern1.getConstructor().equals(conPattern2.getConstructor()) && comparePatterns(conPattern1.getPatterns(), conPattern2.getPatterns());
    }

    if (pattern1 instanceof Concrete.TuplePattern) {
      return pattern2 instanceof Concrete.TuplePattern && comparePatterns(((Concrete.TuplePattern) pattern1).getPatterns(), ((Concrete.TuplePattern) pattern2).getPatterns());
    }

    throw new IllegalStateException();
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

  private boolean compareClause(Concrete.Clause clause1, Concrete.Clause clause2) {
    if (clause1.getPatterns() == clause2.getPatterns()) {
      return true;
    }
    if (clause1.getPatterns() == null || clause2.getPatterns() == null) {
      return false;
    }
    if (clause1.getPatterns().size() != clause2.getPatterns().size()) {
      return false;
    }
    for (int i = 0; i < clause1.getPatterns().size(); i++) {
      if (!comparePattern(clause1.getPatterns().get(i), clause2.getPatterns().get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean compareFunctionClauses(List<Concrete.FunctionClause> clauses1, List<Concrete.FunctionClause> clauses2) {
    if (clauses1.size() != clauses2.size()) {
      return false;
    }
    for (int i = 0; i < clauses1.size(); i++) {
      if (!(compareClause(clauses1.get(i), clauses2.get(i)) && (clauses1.get(i).getExpression() == null ? clauses2.get(i).getExpression() == null : clauses2.get(i).getExpression() != null && compare(clauses1.get(i).getExpression(), clauses2.get(i).getExpression())))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitCase(Concrete.CaseExpression expr1, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.CaseExpression)) {
      return false;
    }
    Concrete.CaseExpression case2 = (Concrete.CaseExpression) expr2;
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
    return compare(expr1.getResultType(), case2.getResultType()) && compare(expr1.getResultTypeLevel(), case2.getResultTypeLevel()) && compareFunctionClauses(expr1.getClauses(), case2.getClauses());
  }

  @Override
  public Boolean visitEval(Concrete.EvalExpression expr, Concrete.Expression expr2) {
    if (!(expr2 instanceof Concrete.EvalExpression)) {
      return false;
    }
    Concrete.EvalExpression eval2 = (Concrete.EvalExpression) expr2;
    return expr.isPEval() == eval2.isPEval() && compare(expr.getExpression(), eval2.getExpression());
  }

  @Override
  public Boolean visitProj(Concrete.ProjExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.ProjExpression && expr1.getField() == ((Concrete.ProjExpression) expr2).getField() && compare(expr1.getExpression(), ((Concrete.ProjExpression) expr2).getExpression());
  }

  private boolean compareImplementStatement(Concrete.ClassFieldImpl implStat1, Concrete.ClassFieldImpl implStat2) {
    return !compareImplementStatements(implStat1.getSubCoclauseList(), implStat2.getSubCoclauseList()) && (implStat1.implementation == implStat2.implementation || implStat1.implementation != null && implStat2.implementation != null && compare(implStat1.implementation, implStat2.implementation)) && Objects.equals(implStat1.getImplementedField(), implStat2.getImplementedField());
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
    if (!(expr2 instanceof Concrete.ClassExtExpression)) return false;
    Concrete.ClassExtExpression classExtExpr2 = (Concrete.ClassExtExpression) expr2;
    return compare(expr1.getBaseClassExpression(), classExtExpr2.getBaseClassExpression()) && compareImplementStatements(expr1.getStatements(), classExtExpr2.getStatements());
  }

  @Override
  public Boolean visitNew(Concrete.NewExpression expr1, Concrete.Expression expr2) {
    return expr2 instanceof Concrete.NewExpression && compare(expr1.getExpression(), ((Concrete.NewExpression) expr2).getExpression());
  }

  private boolean compareLetClause(Concrete.LetClause clause1, Concrete.LetClause clause2) {
    return compareParameters(clause1.getParameters(), clause2.getParameters()) && compare(clause1.getTerm(), clause2.getTerm()) && (clause1.getResultType() == null && clause2.getResultType() == null || clause1.getResultType() != null && clause2.getResultType() != null && compare(clause1.getResultType(), clause2.getResultType())) && comparePattern(clause1.getPattern(), clause2.getPattern());
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
    }
    return compare(expr1.getExpression(), letExpr2.getExpression());
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
      visitor.mySubstitution.put(def1.getData(), def2.getData());
      return def2 instanceof Concrete.Definition && Objects.equals(((Concrete.Definition) def1).enclosingClass, ((Concrete.Definition) def2).enclosingClass) && ((Concrete.Definition) def1).accept(visitor, (Concrete.Definition) def2);
    }
    if (def1 instanceof Concrete.Constructor) {
      return def2 instanceof Concrete.Constructor && visitor.compareConstructor((Concrete.Constructor) def1, (Concrete.Constructor) def2);
    }
    if (def1 instanceof Concrete.ClassField) {
      return def2 instanceof Concrete.ClassField && visitor.compareField((Concrete.ClassField) def1, (Concrete.ClassField) def2);
    }
    return false;
  }

  @Override
  public Boolean visitFunction(Concrete.BaseFunctionDefinition def, Concrete.Definition def2) {
    if (!(def2 instanceof Concrete.BaseFunctionDefinition)) {
      return false;
    }
    Concrete.BaseFunctionDefinition fun2 = (Concrete.BaseFunctionDefinition) def2;

    if (def.getKind() != fun2.getKind()) {
      return false;
    }
    if (!compareParameters(def.getParameters(), fun2.getParameters())) {
      return false;
    }
    if ((def.getResultType() != null || fun2.getResultType() != null) && (def.getResultType() == null || fun2.getResultType() == null || !compare(def.getResultType(), fun2.getResultType()))) {
      return false;
    }
    if ((def.getResultTypeLevel() != null || fun2.getResultTypeLevel() != null) && (def.getResultTypeLevel() == null || fun2.getResultTypeLevel() == null || !compare(def.getResultTypeLevel(), fun2.getResultTypeLevel()))) {
      return false;
    }
    if (def.getBody() instanceof Concrete.TermFunctionBody) {
      return fun2.getBody() instanceof Concrete.TermFunctionBody && compare(((Concrete.TermFunctionBody) def.getBody()).getTerm(), ((Concrete.TermFunctionBody) fun2.getBody()).getTerm());
    }
    if (def.getBody() instanceof Concrete.CoelimFunctionBody) {
      return fun2.getBody() instanceof Concrete.CoelimFunctionBody && compareCoClauseElements(def.getBody().getCoClauseElements(), fun2.getBody().getCoClauseElements());
    }
    if (def.getBody() instanceof Concrete.ElimFunctionBody) {
      if (!(fun2.getBody() instanceof Concrete.ElimFunctionBody)) {
        return false;
      }
      Concrete.ElimFunctionBody elim1 = (Concrete.ElimFunctionBody) def.getBody();
      Concrete.ElimFunctionBody elim2 = (Concrete.ElimFunctionBody) fun2.getBody();
      return compareExpressionList(elim1.getEliminatedReferences(), elim2.getEliminatedReferences()) && compareFunctionClauses(elim1.getClauses(), elim2.getClauses());
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitData(Concrete.DataDefinition def, Concrete.Definition def2) {
    if (!(def2 instanceof Concrete.DataDefinition)) {
      return false;
    }
    Concrete.DataDefinition data2 = (Concrete.DataDefinition) def2;

    if (!compareParameters(def.getParameters(), data2.getParameters())) {
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
      if (!compareClause(def.getConstructorClauses().get(i), data2.getConstructorClauses().get(i))) {
        return false;
      }
      if (def.getConstructorClauses().get(i).getConstructors().size() != data2.getConstructorClauses().get(i).getConstructors().size()) {
        return false;
      }
      for (int j = 0; j < def.getConstructorClauses().get(i).getConstructors().size(); j++) {
        if (!compareConstructor(def.getConstructorClauses().get(i).getConstructors().get(j), data2.getConstructorClauses().get(i).getConstructors().get(j))) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean compareConstructor(Concrete.Constructor con1, Concrete.Constructor con2) {
    mySubstitution.put(con1.getData(), con2.getData());
    return compareParameters(con1.getParameters(), con2.getParameters()) && compare(con1.getResultType(), con2.getResultType()) && compareExpressionList(con1.getEliminatedReferences(), con2.getEliminatedReferences()) && compareFunctionClauses(con1.getClauses(), con2.getClauses());
  }

  @Override
  public Boolean visitClass(Concrete.ClassDefinition def, Concrete.Definition def2) {
    if (!(def2 instanceof Concrete.ClassDefinition)) {
      return false;
    }
    Concrete.ClassDefinition class2 = (Concrete.ClassDefinition) def2;

    if (!compareExpressionList(def.getSuperClasses(), class2.getSuperClasses())) {
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
    return Objects.equals(def.getClassifyingField(), class2.getClassifyingField());
  }

  private boolean compareOverriddenField(Concrete.OverriddenField field1, Concrete.OverriddenField field2) {
    return Objects.equals(field1.getOverriddenField(), field2.getOverriddenField()) && compareParameters(field1.getParameters(), field2.getParameters()) && compare(field1.getResultType(), field2.getResultType()) && compare(field1.getResultTypeLevel(), field2.getResultTypeLevel());
  }

  private boolean compareField(Concrete.ClassField field1, Concrete.ClassField field2) {
    mySubstitution.put(field1.getData(), field2.getData());
    return field1.isExplicit() == field2.isExplicit() && compareParameters(field1.getParameters(), field2.getParameters()) && compare(field1.getResultType(), field2.getResultType()) && compare(field1.getResultTypeLevel(), field2.getResultTypeLevel());
  }
}
