package org.arend.term.concrete;

import org.arend.naming.reference.TCDefReferable;

import java.util.ArrayList;
import java.util.List;

public class ReplaceDataVisitor implements ConcreteExpressionVisitor<Void,Concrete.Expression>, ConcreteLevelExpressionVisitor<Void,Concrete.LevelExpression>, ConcreteResolvableDefinitionVisitor<Void, Concrete.ResolvableDefinition> {
  private final Object myData;
  private final boolean myReplace;

  public ReplaceDataVisitor(Object data) {
    myData = data;
    myReplace = true;
  }

  public ReplaceDataVisitor() {
    myData = null;
    myReplace = false;
  }

  private Object getData(Concrete.SourceNode sourceNode) {
    return myReplace ? myData : sourceNode.getData();
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    List<Concrete.Argument> args = new ArrayList<>(expr.getArguments().size());
    for (Concrete.Argument argument : expr.getArguments()) {
      args.add(new Concrete.Argument(argument.expression.accept(this, null), argument.isExplicit()));
    }
    return Concrete.AppExpression.make(getData(expr), expr.getFunction().accept(this, null), args);
  }

  @Override
  public Concrete.ReferenceExpression visitReference(Concrete.ReferenceExpression expr, Void params) {
    return new Concrete.ReferenceExpression(getData(expr), expr.getReferent(), visitLevels(expr.getPLevels()), visitLevels(expr.getHLevels()));
  }

  @Override
  public Concrete.Expression visitThis(Concrete.ThisExpression expr, Void params) {
    return new Concrete.ThisExpression(getData(expr), expr.getReferent());
  }

  private List<Concrete.Parameter> visitParameters(List<? extends Concrete.Parameter> parameters) {
    List<Concrete.Parameter> result = new ArrayList<>(parameters.size());
    for (Concrete.Parameter parameter : parameters) {
      Concrete.Parameter param = parameter.copy(getData(parameter));
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).type = ((Concrete.TypeParameter) param).type.accept(this, null);
      }
      result.add(param);
    }
    return result;
  }

  private List<Concrete.Pattern> visitPatterns(List<? extends Concrete.Pattern> patterns) {
    if (patterns == null) return null;
    List<Concrete.Pattern> result = new ArrayList<>(patterns.size());
    for (Concrete.Pattern pattern : patterns) {
      result.add(pattern == null ? null : visitPattern(pattern));
    }
    return result;
  }

  private Concrete.Pattern visitPattern(Concrete.Pattern pattern) {
    Concrete.Pattern result;
    if (pattern instanceof Concrete.NamePattern namePattern) {
      result = new Concrete.NamePattern(getData(pattern), pattern.isExplicit(), namePattern.getReferable(), namePattern.type == null ? null : namePattern.type.accept(this, null), namePattern.fixity);
    } else if (pattern instanceof Concrete.ConstructorPattern conPattern) {
      List<Concrete.Pattern> args = new ArrayList<>(pattern.getPatterns().size());
      for (Concrete.Pattern subPattern : pattern.getPatterns()) {
        args.add(visitPattern(subPattern));
      }
      result = new Concrete.ConstructorPattern(getData(pattern), pattern.isExplicit(), myReplace ? myData : conPattern.getConstructorData(), conPattern.getConstructor(), args, null);
    } else if (pattern instanceof Concrete.TuplePattern) {
      List<Concrete.Pattern> args = new ArrayList<>(pattern.getPatterns().size());
      for (Concrete.Pattern subPattern : pattern.getPatterns()) {
        args.add(visitPattern(subPattern));
      }
      result = new Concrete.TuplePattern(getData(pattern), pattern.isExplicit(), args, null);
    } else if (pattern instanceof Concrete.NumberPattern) {
      result = new Concrete.NumberPattern(getData(pattern), ((Concrete.NumberPattern) pattern).getNumber(), null);
    } else {
      throw new IllegalStateException();
    }

    if (pattern.getAsReferable() != null) {
      result.setAsReferable(new Concrete.TypedReferable(getData(pattern.getAsReferable()), pattern.getAsReferable().referable, pattern.getAsReferable().type == null ? null : pattern.getAsReferable().type.accept(this, null)));
    }
    return result;
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    List<Concrete.Parameter> parameters = visitParameters(expr.getParameters());
    if (expr instanceof Concrete.PatternLamExpression) {
      return Concrete.PatternLamExpression.make(getData(expr), parameters, visitPatterns(((Concrete.PatternLamExpression) expr).getPatterns()), expr.body.accept(this, null));
    } else {
      return new Concrete.LamExpression(getData(expr), parameters, expr.body.accept(this, null));
    }
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    //noinspection unchecked
    return new Concrete.PiExpression(getData(expr), (List<Concrete.TypeParameter>) (List<? extends Concrete.Parameter>) visitParameters(expr.getParameters()), expr.codomain.accept(this, params));
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return new Concrete.UniverseExpression(getData(expr), expr.getPLevel() == null ? null : expr.getPLevel().accept(this, null), expr.getHLevel() == null ? null : expr.getHLevel().accept(this, null));
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void params) {
    return new Concrete.HoleExpression(getData(expr));
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Void params) {
    return new Concrete.ApplyHoleExpression(getData(expr));
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void params) {
    return expr instanceof Concrete.IncompleteExpression ? new Concrete.IncompleteExpression(getData(expr)) : new Concrete.GoalExpression(getData(expr), expr.getName(), expr.expression == null ? null : expr.expression.accept(this, null), expr.goalSolver, expr.useGoalSolver, expr.errors);
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    List<Concrete.Expression> args = new ArrayList<>(expr.getFields().size());
    for (Concrete.Expression field : expr.getFields()) {
      args.add(field.accept(this, null));
    }
    return new Concrete.TupleExpression(getData(expr), args);
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    //noinspection unchecked
    return new Concrete.SigmaExpression(getData(expr), (List<Concrete.TypeParameter>) (List<? extends Concrete.Parameter>) visitParameters(expr.getParameters()));
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    throw new IllegalStateException();
  }

  private List<Concrete.FunctionClause> visitFunctionClauses(List<Concrete.FunctionClause> clauses) {
    List<Concrete.FunctionClause> result = new ArrayList<>(clauses.size());
    for (Concrete.FunctionClause clause : clauses) {
      result.add(new Concrete.FunctionClause(getData(clause), visitPatterns(clause.getPatterns()), clause.expression == null ? null : clause.expression.accept(this, null)));
    }
    return result;
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    List<Concrete.CaseArgument> args = new ArrayList<>(expr.getArguments().size());
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      args.add(new Concrete.CaseArgument(caseArg.expression.accept(this, null), caseArg.referable, caseArg.type == null ? null : caseArg.type.accept(this, null), caseArg.isElim));
    }
    return new Concrete.CaseExpression(getData(expr), expr.isSCase(), args, expr.getResultType() == null ? null : expr.getResultType().accept(this, null), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null), visitFunctionClauses(expr.getClauses()));
  }

  @Override
  public Concrete.Expression visitEval(Concrete.EvalExpression expr, Void params) {
    return new Concrete.EvalExpression(getData(expr), expr.isPEval(), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitBox(Concrete.BoxExpression expr, Void params) {
    return new Concrete.BoxExpression(getData(expr), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    return new Concrete.ProjExpression(getData(expr), expr.expression.accept(this, null), expr.getField());
  }

  private Concrete.Coclauses visitCoclauses(Concrete.Coclauses coclauses) {
    if (coclauses == null) return null;
    List<Concrete.ClassFieldImpl> result = new ArrayList<>(coclauses.getCoclauseList().size());
    for (Concrete.ClassFieldImpl coclause : coclauses.getCoclauseList()) {
      Concrete.ClassFieldImpl newCoclause = new Concrete.ClassFieldImpl(getData(coclause), coclause.getImplementedField(), coclause.implementation == null ? null : coclause.implementation.accept(this, null), coclause.getSubCoclauses() == null ? null : visitCoclauses(coclause.getSubCoclauses()), coclause.isDefault());
      newCoclause.classRef = coclause.classRef;
      result.add(newCoclause);
    }
    return new Concrete.Coclauses(getData(coclauses), result);
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    return Concrete.ClassExtExpression.make(getData(expr), expr.getBaseClassExpression().accept(this, null), visitCoclauses(expr.getCoclauses()));
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    return new Concrete.NewExpression(getData(expr), expr.expression.accept(this, null));
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    List<Concrete.LetClause> clauses = new ArrayList<>(expr.getClauses().size());
    for (Concrete.LetClause clause : expr.getClauses()) {
      clauses.add(new Concrete.LetClause(visitParameters(clause.getParameters()), clause.resultType == null ? null : clause.resultType.accept(this, null), clause.term.accept(this, null), clause.getPattern() == null ? null : visitPattern(clause.getPattern())));
    }
    return new Concrete.LetExpression(getData(expr), expr.isHave(), expr.isStrict(), clauses, expr.expression.accept(this, null));
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return new Concrete.NumericLiteral(getData(expr), expr.getNumber());
  }

  @Override
  public Concrete.Expression visitStringLiteral(Concrete.StringLiteral expr, Void params) {
    return new Concrete.StringLiteral(getData(expr), expr.getUnescapedString());
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, Void params) {
    return new Concrete.TypedExpression(getData(expr), expr.expression.accept(this, null), expr.type.accept(this, null));
  }

  private List<Concrete.LevelExpression> visitLevels(List<? extends Concrete.LevelExpression> levels) {
    if (levels == null) return null;
    List<Concrete.LevelExpression> result = new ArrayList<>(levels.size());
    for (Concrete.LevelExpression level : levels) {
      result.add(level.accept(this, null));
    }
    return result;
  }

  @Override
  public Concrete.LevelExpression visitInf(Concrete.InfLevelExpression expr, Void param) {
    return new Concrete.InfLevelExpression(getData(expr));
  }

  @Override
  public Concrete.LevelExpression visitLP(Concrete.PLevelExpression expr, Void param) {
    return new Concrete.PLevelExpression(getData(expr));
  }

  @Override
  public Concrete.LevelExpression visitLH(Concrete.HLevelExpression expr, Void param) {
    return new Concrete.HLevelExpression(getData(expr));
  }

  @Override
  public Concrete.LevelExpression visitNumber(Concrete.NumberLevelExpression expr, Void param) {
    return new Concrete.NumberLevelExpression(getData(expr), expr.getNumber());
  }

  @Override
  public Concrete.LevelExpression visitId(Concrete.IdLevelExpression expr, Void param) {
    return new Concrete.IdLevelExpression(getData(expr), expr.getReferent());
  }

  @Override
  public Concrete.LevelExpression visitSuc(Concrete.SucLevelExpression expr, Void param) {
    return new Concrete.SucLevelExpression(getData(expr), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitMax(Concrete.MaxLevelExpression expr, Void param) {
    return new Concrete.MaxLevelExpression(getData(expr), expr.getLeft().accept(this, null), expr.getRight().accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitVar(Concrete.VarLevelExpression expr, Void param) {
    return new Concrete.VarLevelExpression(getData(expr), expr.getVariable());
  }

  private List<Concrete.ReferenceExpression> visitReferenceExpressions(List<? extends Concrete.ReferenceExpression> expressions) {
    if (expressions == null) return null;
    List<Concrete.ReferenceExpression> result = new ArrayList<>(expressions.size());
    for (Concrete.ReferenceExpression expression : expressions) {
      result.add(visitReference(expression, null));
    }
    return result;
  }

  private Concrete.CoClauseElement visitCoClauseElement(Concrete.CoClauseElement element) {
    if (element instanceof Concrete.CoClauseFunctionReference oldElement) {
      Concrete.CoClauseFunctionReference newElement = new Concrete.CoClauseFunctionReference(getData(oldElement), oldElement.getImplementedField(), (TCDefReferable) ((Concrete.ReferenceExpression) oldElement.implementation).getReferent(), oldElement.isDefault());
      newElement.classRef = oldElement.classRef;
      return newElement;
    } else if (element instanceof Concrete.ClassFieldImpl classFieldImpl) {
      Concrete.ClassFieldImpl newElement = new Concrete.ClassFieldImpl(getData(classFieldImpl), classFieldImpl.getImplementedField(), classFieldImpl.implementation == null ? null : classFieldImpl.implementation.accept(this, null), visitCoclauses(classFieldImpl.getSubCoclauses()), classFieldImpl.isDefault());
      newElement.classRef = classFieldImpl.classRef;
      return newElement;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Concrete.ResolvableDefinition visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
    Concrete.FunctionBody body = def.getBody();
    Concrete.FunctionBody newBody;
    if (body instanceof Concrete.CoelimFunctionBody) {
      List<Concrete.CoClauseElement> elements = body.getCoClauseElements(), newElements = new ArrayList<>(elements.size());
      for (Concrete.CoClauseElement element : elements) {
        newElements.add(visitCoClauseElement(element));
      }
      newBody = new Concrete.CoelimFunctionBody(getData(body), newElements);
    } else if (body instanceof Concrete.ElimFunctionBody) {
      newBody = new Concrete.ElimFunctionBody(getData(body), visitReferenceExpressions(body.getEliminatedReferences()), visitFunctionClauses(body.getClauses()));
    } else if (body instanceof Concrete.TermFunctionBody) {
      newBody = new Concrete.TermFunctionBody(getData(body), body.getTerm() == null ? null : body.getTerm().accept(this, null));
    } else {
      throw new IllegalStateException();
    }

    Concrete.BaseFunctionDefinition newDef = def.copy(visitParameters(def.getParameters()), newBody);
    if (def.getResultType() != null) {
      newDef.setResultType(def.getResultType().accept(this, null));
    }
    if (def.getResultTypeLevel() != null) {
      newDef.setResultTypeLevel(def.getResultTypeLevel().accept(this, null));
    }
    def.copyData(newDef);
    return newDef;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Concrete.DataDefinition visitData(Concrete.DataDefinition def, Void params) {
    List<Concrete.ConstructorClause> clauses = new ArrayList<>(def.getConstructorClauses().size());
    Concrete.DataDefinition result = new Concrete.DataDefinition(def.getData(), def.getPLevelParameters(), def.getHLevelParameters(), (List<Concrete.TypeParameter>) (List<?>) visitParameters(def.getParameters()), visitReferenceExpressions(def.getEliminatedReferences()), def.isTruncated(), def.getUniverse() == null ? null : visitUniverse(def.getUniverse(), null), clauses);
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      List<Concrete.Constructor> constructors = new ArrayList<>(clause.getConstructors().size());
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        Concrete.Constructor newConstructor = new Concrete.Constructor(constructor.getData(), result, (List<Concrete.TypeParameter>) (List<?>) visitParameters(constructor.getParameters()), visitReferenceExpressions(constructor.getEliminatedReferences()), visitFunctionClauses(constructor.getClauses()), constructor.isCoerce());
        newConstructor.setResultType(constructor.getResultType() == null ? null : constructor.getResultType().accept(this, null));
        constructors.add(newConstructor);
      }
      clauses.add(new Concrete.ConstructorClause(getData(clause), visitPatterns(clause.getPatterns()), constructors));
    }
    def.copyData(result);
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Concrete.ClassDefinition visitClass(Concrete.ClassDefinition def, Void params) {
    List<Concrete.ClassElement> elements = new ArrayList<>(def.getElements().size());
    Concrete.ClassDefinition result = new Concrete.ClassDefinition(def.getData(), def.getPLevelParameters(), def.getHLevelParameters(), def.isRecord(), def.withoutClassifying(), visitReferenceExpressions(def.getSuperClasses()), elements);
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.CoClauseElement coClauseElem) {
        elements.add(visitCoClauseElement(coClauseElem));
      } else if (element instanceof Concrete.ClassField field) {
        elements.add(new Concrete.ClassField(field.getData(), result, field.isExplicit(), field.getKind(), (List<Concrete.TypeParameter>) (List<?>) visitParameters(field.getParameters()), field.getResultType().accept(this, null), field.getResultTypeLevel() == null ? null : field.getResultTypeLevel().accept(this, null), field.isCoerce()));
      } else if (element instanceof Concrete.OverriddenField field) {
        elements.add(new Concrete.OverriddenField(getData(field), field.getOverriddenField(), (List<Concrete.TypeParameter>) (List<?>) visitParameters(field.getParameters()), field.getResultType().accept(this, null), field.getResultTypeLevel() == null ? null : field.getResultTypeLevel().accept(this, null)));
      } else {
        throw new IllegalStateException();
      }
    }
    result.setClassifyingField(def.getClassifyingField(), def.isForcedClassifyingField());
    def.copyData(result);
    return result;
  }

  @Override
  public DefinableMetaDefinition visitMeta(DefinableMetaDefinition def, Void params) {
    @SuppressWarnings("unchecked") DefinableMetaDefinition result = new DefinableMetaDefinition(def.getData(), def.getPLevelParameters(), def.getHLevelParameters(), (List<Concrete.NameParameter>) (List<?>) visitParameters(def.getParameters()), def.body == null ? null : def.body.accept(this, null));
    result.stage = def.stage;
    result.setStatus(def.getStatus());
    return result;
  }
}
