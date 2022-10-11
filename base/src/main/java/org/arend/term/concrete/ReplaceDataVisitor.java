package org.arend.term.concrete;

import java.util.ArrayList;
import java.util.List;

public class ReplaceDataVisitor implements ConcreteExpressionVisitor<Void,Concrete.Expression>, ConcreteLevelExpressionVisitor<Void,Concrete.LevelExpression> {
  private final Object myData;

  public ReplaceDataVisitor(Object data) {
    myData = data;
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    List<Concrete.Argument> args = new ArrayList<>(expr.getArguments().size());
    for (Concrete.Argument argument : expr.getArguments()) {
      args.add(new Concrete.Argument(argument.expression.accept(this, null), argument.isExplicit()));
    }
    return Concrete.AppExpression.make(myData, expr.getFunction().accept(this, null), args);
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    return new Concrete.ReferenceExpression(myData, expr.getReferent(), visitLevels(expr.getPLevels()), visitLevels(expr.getHLevels()));
  }

  @Override
  public Concrete.Expression visitThis(Concrete.ThisExpression expr, Void params) {
    return new Concrete.ThisExpression(myData, expr.getReferent());
  }

  private List<Concrete.Parameter> visitParameters(List<? extends Concrete.Parameter> parameters) {
    List<Concrete.Parameter> result = new ArrayList<>(parameters.size());
    for (Concrete.Parameter parameter : parameters) {
      Concrete.Parameter param = parameter.copy(myData);
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).type = ((Concrete.TypeParameter) param).type.accept(this, null);
      }
      result.add(param);
    }
    return result;
  }

  private List<Concrete.Pattern> visitPatterns(List<? extends Concrete.Pattern> patterns) {
    List<Concrete.Pattern> result = new ArrayList<>(patterns.size());
    for (Concrete.Pattern pattern : patterns) {
      result.add(pattern == null ? null : visitPattern(pattern));
    }
    return result;
  }

  private Concrete.Pattern visitPattern(Concrete.Pattern pattern) {
    Concrete.Pattern result;
    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      result = new Concrete.NamePattern(myData, pattern.isExplicit(), namePattern.getReferable(), namePattern.type == null ? null : namePattern.type.accept(this, null), namePattern.fixity);
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      List<Concrete.Pattern> args = new ArrayList<>(pattern.getPatterns().size());
      for (Concrete.Pattern subPattern : pattern.getPatterns()) {
        args.add(visitPattern(subPattern));
      }
      result = new Concrete.ConstructorPattern(myData, pattern.isExplicit(), ((Concrete.ConstructorPattern) pattern).getConstructor(), args, null);
    } else if (pattern instanceof Concrete.TuplePattern) {
      List<Concrete.Pattern> args = new ArrayList<>(pattern.getPatterns().size());
      for (Concrete.Pattern subPattern : pattern.getPatterns()) {
        args.add(visitPattern(subPattern));
      }
      result = new Concrete.TuplePattern(myData, pattern.isExplicit(), args, null);
    } else if (pattern instanceof Concrete.NumberPattern) {
      result = new Concrete.NumberPattern(myData, ((Concrete.NumberPattern) pattern).getNumber(), null);
    } else {
      throw new IllegalStateException();
    }

    if (pattern.getAsReferable() != null) {
      result.setAsReferable(new Concrete.TypedReferable(myData, pattern.getAsReferable().referable, pattern.getAsReferable().type == null ? null : pattern.getAsReferable().type.accept(this, null)));
    }
    return result;
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    List<Concrete.Parameter> parameters = visitParameters(expr.getParameters());
    if (expr instanceof Concrete.PatternLamExpression) {
      return Concrete.PatternLamExpression.make(myData, parameters, visitPatterns(((Concrete.PatternLamExpression) expr).getPatterns()), expr.body.accept(this, null));
    } else {
      return new Concrete.LamExpression(myData, parameters, expr.body.accept(this, null));
    }
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    //noinspection unchecked
    return new Concrete.PiExpression(myData, (List<Concrete.TypeParameter>) (List<? extends Concrete.Parameter>) visitParameters(expr.getParameters()), expr.codomain.accept(this, params));
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return new Concrete.UniverseExpression(myData, expr.getPLevel() == null ? null : expr.getPLevel().accept(this, null), expr.getHLevel() == null ? null : expr.getHLevel().accept(this, null));
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void params) {
    return new Concrete.HoleExpression(myData);
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Void params) {
    return new Concrete.ApplyHoleExpression(myData);
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void params) {
    return new Concrete.GoalExpression(myData, expr.getName(), expr.expression == null ? null : expr.expression.accept(this, null), expr.goalSolver, expr.useGoalSolver, expr.errors);
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    List<Concrete.Expression> args = new ArrayList<>(expr.getFields().size());
    for (Concrete.Expression field : expr.getFields()) {
      args.add(field.accept(this, null));
    }
    return new Concrete.TupleExpression(myData, args);
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    //noinspection unchecked
    return new Concrete.SigmaExpression(myData, (List<Concrete.TypeParameter>) (List<? extends Concrete.Parameter>) visitParameters(expr.getParameters()));
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    List<Concrete.CaseArgument> args = new ArrayList<>(expr.getArguments().size());
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      args.add(new Concrete.CaseArgument(caseArg.expression.accept(this, null), caseArg.referable, caseArg.type == null ? null : caseArg.type.accept(this, null), caseArg.isElim));
    }
    List<Concrete.FunctionClause> clauses = new ArrayList<>(expr.getClauses().size());
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      clauses.add(new Concrete.FunctionClause(myData, visitPatterns(clause.getPatterns()), clause.expression == null ? null : clause.expression.accept(this, null)));
    }
    return new Concrete.CaseExpression(myData, expr.isSCase(), args, expr.getResultType() == null ? null : expr.getResultType().accept(this, null), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null), clauses);
  }

  @Override
  public Concrete.Expression visitEval(Concrete.EvalExpression expr, Void params) {
    return new Concrete.EvalExpression(myData, expr.isPEval(), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitBox(Concrete.BoxExpression expr, Void params) {
    return new Concrete.BoxExpression(myData, expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    return new Concrete.ProjExpression(myData, expr.expression.accept(this, null), expr.getField());
  }

  private Concrete.Coclauses visitClassFieldImpls(List<? extends Concrete.ClassFieldImpl> coclauses) {
    List<Concrete.ClassFieldImpl> result = new ArrayList<>(coclauses.size());
    for (Concrete.ClassFieldImpl coclause : coclauses) {
      Concrete.ClassFieldImpl newCoclause = new Concrete.ClassFieldImpl(myData, coclause.getImplementedField(), coclause.implementation == null ? null : coclause.implementation.accept(this, null), coclause.getSubCoclauses() == null ? null : visitClassFieldImpls(coclause.getSubCoclauses().getCoclauseList()), coclause.isDefault());
      newCoclause.classRef = coclause.classRef;
      result.add(newCoclause);
    }
    return new Concrete.Coclauses(myData, result);
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    return Concrete.ClassExtExpression.make(myData, expr.getBaseClassExpression().accept(this, null), visitClassFieldImpls(expr.getCoclauses().getCoclauseList()));
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    return new Concrete.NewExpression(myData, expr.expression.accept(this, null));
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    List<Concrete.LetClause> clauses = new ArrayList<>(expr.getClauses().size());
    for (Concrete.LetClause clause : expr.getClauses()) {
      clauses.add(new Concrete.LetClause(visitParameters(clause.getParameters()), clause.resultType == null ? null : clause.resultType.accept(this, null), clause.term.accept(this, null), clause.getPattern() == null ? null : visitPattern(clause.getPattern())));
    }
    return new Concrete.LetExpression(myData, expr.isHave(), expr.isStrict(), clauses, expr.expression.accept(this, null));
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return new Concrete.NumericLiteral(myData, expr.getNumber());
  }

  @Override
  public Concrete.Expression visitStringLiteral(Concrete.StringLiteral expr, Void params) {
    return new Concrete.StringLiteral(myData, expr.getUnescapedString());
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, Void params) {
    return new Concrete.TypedExpression(myData, expr.expression.accept(this, null), expr.type.accept(this, null));
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
    return new Concrete.InfLevelExpression(myData);
  }

  @Override
  public Concrete.LevelExpression visitLP(Concrete.PLevelExpression expr, Void param) {
    return new Concrete.PLevelExpression(myData);
  }

  @Override
  public Concrete.LevelExpression visitLH(Concrete.HLevelExpression expr, Void param) {
    return new Concrete.HLevelExpression(myData);
  }

  @Override
  public Concrete.LevelExpression visitNumber(Concrete.NumberLevelExpression expr, Void param) {
    return new Concrete.NumberLevelExpression(myData, expr.getNumber());
  }

  @Override
  public Concrete.LevelExpression visitId(Concrete.IdLevelExpression expr, Void param) {
    return new Concrete.IdLevelExpression(myData, expr.getReferent());
  }

  @Override
  public Concrete.LevelExpression visitSuc(Concrete.SucLevelExpression expr, Void param) {
    return new Concrete.SucLevelExpression(myData, expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitMax(Concrete.MaxLevelExpression expr, Void param) {
    return new Concrete.MaxLevelExpression(myData, expr.getLeft().accept(this, null), expr.getRight().accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitVar(Concrete.VarLevelExpression expr, Void param) {
    return new Concrete.VarLevelExpression(myData, expr.getVariable());
  }
}
