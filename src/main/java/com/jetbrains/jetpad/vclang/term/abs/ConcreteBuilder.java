package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConcreteBuilder implements AbstractDefinitionVisitor<Concrete.Definition>, AbstractExpressionVisitor<Void, Concrete.Expression>, AbstractLevelExpressionVisitor<Void, Concrete.LevelExpression> {
  private final ErrorReporter myErrorReporter;
  private final GlobalReferable myDefinition;

  private ConcreteBuilder(ErrorReporter errorReporter, GlobalReferable definition) {
    myErrorReporter = errorReporter;
    myDefinition = definition;
  }

  public static Concrete.Definition convert(Abstract.Definition definition, ErrorReporter errorReporter) {
    ConcreteBuilder builder = new ConcreteBuilder(errorReporter, definition.getReferable());
    try {
      return definition.accept(builder);
    } catch (AbstractExpressionError.Exception e) {
      errorReporter.report(new ProxyError(builder.myDefinition, e.error));
      return null;
    }
  }

  // Definition

  @Override
  public Concrete.FunctionDefinition visitFunction(Abstract.FunctionDefinition def) {
    Concrete.FunctionBody body;
    Abstract.Expression term = def.getTerm();
    if (term != null) {
      Object data = term.getData();
      body = new Concrete.TermFunctionBody(data, term.accept(this, null));
      if (!def.getEliminatedExpressions().isEmpty()) {
        myErrorReporter.report(new ProxyError(myDefinition, new AbstractExpressionError(Error.Level.WARNING, "Eliminated expressions are ignored", data)));
      }
      if (!def.getClauses().isEmpty()) {
        myErrorReporter.report(new ProxyError(myDefinition, new AbstractExpressionError(Error.Level.WARNING, "Clauses are ignored", data)));
      }
    } else {
      body = new Concrete.ElimFunctionBody(def.getReferable(), buildReferenceExpressionsFromReferences(def.getEliminatedExpressions()), buildClauses(def.getClauses()));
    }

    Abstract.Expression resultType = def.getResultType();
    return new Concrete.FunctionDefinition(myDefinition, buildParameters(def.getParameters()), resultType == null ? null : resultType.accept(this, null), body);
  }

  @Override
  public Concrete.DataDefinition visitData(Abstract.DataDefinition def) {
    Abstract.Expression absUniverse = def.getUniverse();
    Concrete.Expression universe = absUniverse == null ? null : absUniverse.accept(this, null);
    if (universe != null && !(universe instanceof Concrete.UniverseExpression)) {
      myErrorReporter.report(new ProxyError(myDefinition, new AbstractExpressionError(Error.Level.ERROR, "Expected a universe", universe.getData())));
    }

    Collection<? extends Abstract.ConstructorClause> absClauses = def.getClauses();
    List<Concrete.ConstructorClause> clauses = new ArrayList<>(absClauses.size());
    Collection<? extends Abstract.Reference> elimExpressions = def.getEliminatedExpressions();
    Concrete.DataDefinition data = new Concrete.DataDefinition(myDefinition, buildTypeParameters(def.getParameters()), elimExpressions == null ? null : buildReferenceExpressionsFromReferences(elimExpressions), def.isTruncated(), universe instanceof Concrete.UniverseExpression ? (Concrete.UniverseExpression) universe : null, clauses);

    for (Abstract.ConstructorClause clause : absClauses) {
      Collection<? extends Abstract.Constructor> absConstructors = clause.getConstructors();
      if (absConstructors.isEmpty()) {
        myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(clause)));
        continue;
      }

      List<Concrete.Constructor> constructors = new ArrayList<>(absConstructors.size());
      for (Abstract.Constructor constructor : absConstructors) {
        constructors.add(new Concrete.Constructor(constructor.getReferable(), data, buildTypeParameters(constructor.getParameters()), buildReferenceExpressionsFromReferences(constructor.getEliminatedExpressions()), buildClauses(constructor.getClauses())));
      }

      Collection<? extends Abstract.Pattern> patterns = clause.getPatterns();
      clauses.add(new Concrete.ConstructorClause(clause.getData(), patterns.isEmpty() ? null : buildPatterns(patterns), constructors));
    }

    return data;
  }

  private List<Concrete.ReferenceExpression> buildReferenceExpressionsFromReferences(Collection<? extends Abstract.Reference> absElimExpressions) {
    List<Concrete.ReferenceExpression> elimExpressions = new ArrayList<>(absElimExpressions.size());
    for (Abstract.Reference reference : absElimExpressions) {
      elimExpressions.add(new Concrete.ReferenceExpression(reference.getData(), reference.getReferent()));
    }
    return elimExpressions;
  }

  @Override
  public Concrete.ClassDefinition visitClass(Abstract.ClassDefinition def) {
    List<Concrete.ClassField> classFields = new ArrayList<>();
    Concrete.ClassDefinition classDef = new Concrete.ClassDefinition(def.getReferable(), buildTypeParameters(def.getParameters()), buildReferenceExpressionsFromReferences(def.getSuperClasses()), classFields, buildImplementations(def.getClassFieldImpls()));

    for (Abstract.ClassField field : def.getClassFields()) {
      Abstract.Expression resultType = field.getResultType();
      if (resultType == null) {
        myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(field.getReferable())));
      } else {
        classFields.add(new Concrete.ClassField(field.getReferable(), classDef, resultType.accept(this, null)));
      }
    }

    return classDef;
  }

  private List<Concrete.ClassFieldImpl> buildImplementations(Collection<? extends Abstract.ClassFieldImpl> absImplementations) {
    List<Concrete.ClassFieldImpl> implementations = new ArrayList<>();
    for (Abstract.ClassFieldImpl implementation : absImplementations) {
      Abstract.Expression impl = implementation.getImplementation();
      if (impl != null) {
        implementations.add(new Concrete.ClassFieldImpl(implementation.getData(), implementation.getImplementedField(), impl.accept(this, null)));
      } else {
        myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(implementation.getData())));
      }
    }
    return implementations;
  }

  private Concrete.Parameter buildParameter(Abstract.Parameter parameter) {
    List<? extends Referable> referableList = parameter.getReferableList();
    Abstract.Expression type = parameter.getType();
    if (type == null) {
      if (referableList.size() == 1) {
        return new Concrete.NameParameter(parameter.getData(), parameter.isExplicit(), referableList.iterator().next());
      } else {
        throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, "Expected a single variable", parameter.getData()));
      }
    } else {
      return referableList.isEmpty() ? new Concrete.TypeParameter(parameter.getData(), parameter.isExplicit(), type.accept(this, null)) : new Concrete.TelescopeParameter(parameter.getData(), parameter.isExplicit(), referableList, type.accept(this, null));
    }
  }

  private List<Concrete.Parameter> buildParameters(Collection<? extends Abstract.Parameter> absParameters) {
    List<Concrete.Parameter> parameters = new ArrayList<>(absParameters.size());
    for (Abstract.Parameter absParameter : absParameters) {
      parameters.add(buildParameter(absParameter));
    }
    return parameters;
  }

  private List<Concrete.TypeParameter> buildTypeParameters(Collection<? extends Abstract.Parameter> absParameters) {
    List<Concrete.TypeParameter> parameters = new ArrayList<>(absParameters.size());
    for (Abstract.Parameter absParameter : absParameters) {
      Concrete.Parameter parameter = buildParameter(absParameter);
      if (parameter instanceof Concrete.TypeParameter) {
        parameters.add((Concrete.TypeParameter) parameter);
      } else {
        throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, "Expected a typed parameter", parameter.getData()));
      }
    }
    return parameters;
  }

  private Concrete.Pattern buildPattern(Abstract.Pattern pattern) {
    if (pattern.isEmpty()) {
      if (pattern.getHeadReference() == null && pattern.getArguments().isEmpty()) {
        return new Concrete.EmptyPattern(pattern.getData(), pattern.isExplicit());
      } else {
        throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, "Unexpected arguments for an empty pattern", pattern.getData()));
      }
    } else {
      Referable reference = pattern.getHeadReference();
      if (reference instanceof GlobalReferable || reference instanceof UnresolvedReference) {
        return new Concrete.ConstructorPattern(pattern.getData(), pattern.isExplicit(), reference, buildPatterns(pattern.getArguments()));
      } else {
        if (!pattern.getArguments().isEmpty()) {
          myErrorReporter.report(new ProxyError(myDefinition, new AbstractExpressionError(Error.Level.ERROR, "Unexpected argument", pattern.getArguments().iterator().next())));
        }
        return new Concrete.NamePattern(pattern.getData(), pattern.isExplicit(), reference);
      }
    }
  }

  private List<Concrete.Pattern> buildPatterns(Collection<? extends Abstract.Pattern> absPatterns) {
    List<Concrete.Pattern> patterns = new ArrayList<>(absPatterns.size());
    for (Abstract.Pattern pattern : absPatterns) {
      patterns.add(buildPattern(pattern));
    }
    return patterns;
  }

  private List<Concrete.FunctionClause> buildClauses(Collection<? extends Abstract.FunctionClause> absClauses) {
    List<Concrete.FunctionClause> clauses = new ArrayList<>(absClauses.size());
    for (Abstract.FunctionClause clause : absClauses) {
      Collection<? extends Abstract.Pattern> patterns = clause.getPatterns();
      if (patterns.isEmpty()) {
        throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(clause));
      }
      Abstract.Expression expr = clause.getExpression();
      clauses.add(new Concrete.FunctionClause(clause.getData(), buildPatterns(patterns), expr == null ? null : expr.accept(this, null)));
    }
    return clauses;
  }

  // Expression

  @Override
  public Concrete.Expression visitApp(@Nullable Object data, @Nonnull Abstract.Expression expr, @Nonnull Collection<? extends Abstract.Argument> arguments, Void params) {
    Concrete.Expression result = expr.accept(this, null);
    for (Abstract.Argument arg : arguments) {
      result = new Concrete.AppExpression(result.getData(), result, new Concrete.Argument(arg.getExpression().accept(this, null), arg.isExplicit()));
    }
    return result;
  }

  @Override
  public Concrete.ReferenceExpression visitReference(@Nullable Object data, @Nonnull Referable referent, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, Void params) {
    return Concrete.ReferenceExpression.make(data, referent, level1 == null ? null : level1.accept(this, null), level2 == null ? null : level2.accept(this, null));
  }

  @Override
  public Concrete.ReferenceExpression visitReference(@Nullable Object data, @Nonnull Referable referent, int lp, int lh, Void params) {
    return new Concrete.ReferenceExpression(data, referent, new Concrete.NumberLevelExpression(data, lp), new Concrete.NumberLevelExpression(data, lh));
  }

  @Override
  public Concrete.Expression visitLam(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression body, Void params) {
    if (body == null) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    if (parameters.isEmpty()) {
      myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(data)));
      return body.accept(this, null);
    }

    return new Concrete.LamExpression(data, buildParameters(parameters), body.accept(this, null));
  }

  @Override
  public Concrete.Expression visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression codomain, Void params) {
    if (codomain == null) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    if (parameters.isEmpty()) {
      myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(data)));
      return codomain.accept(this, null);
    }

    return new Concrete.PiExpression(data, buildTypeParameters(parameters), codomain.accept(this, null));
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(@Nullable Object data, @Nullable Integer pLevelNum, @Nullable Integer hLevelNum, @Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel, Void params) {
    if (pLevelNum != null && pLevel != null) {
      myErrorReporter.report(new ProxyError(myDefinition, new AbstractExpressionError(Error.Level.ERROR, "p-level is already specified", pLevel.getData())));
    }
    if (hLevelNum != null && hLevel != null) {
      myErrorReporter.report(new ProxyError(myDefinition, new AbstractExpressionError(Error.Level.ERROR, "h-level is already specified", hLevel.getData())));
    }

    return new Concrete.UniverseExpression(data,
      pLevelNum != null ? new Concrete.NumberLevelExpression(data, pLevelNum) : pLevel != null ? pLevel.accept(this, null) : null,
      hLevelNum != null ? (hLevelNum == Abstract.INFINITY_LEVEL ? new Concrete.InfLevelExpression(data) : new Concrete.NumberLevelExpression(data, hLevelNum)) : hLevel != null ? hLevel.accept(this, null) : null);
  }

  @Override
  public Concrete.InferHoleExpression visitInferHole(@Nullable Object data, Void params) {
    return new Concrete.InferHoleExpression(data);
  }

  @Override
  public Concrete.GoalExpression visitGoal(@Nullable Object data, @Nullable String name, @Nullable Abstract.Expression expression, Void params) {
    return new Concrete.GoalExpression(data, name, expression == null ? null : expression.accept(this, null));
  }

  @Override
  public Concrete.TupleExpression visitTuple(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> absFields, Void params) {
    if (absFields.isEmpty()) { // TODO: Implement unit types as empty sigmas
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    List<Concrete.Expression> fields = new ArrayList<>(absFields.size());
    for (Abstract.Expression field : absFields) {
      fields.add(field.accept(this, null));
    }
    return new Concrete.TupleExpression(data, fields);
  }

  @Override
  public Concrete.SigmaExpression visitSigma(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, Void params) {
    if (parameters.isEmpty()) { // TODO: Implement unit types as empty sigmas
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    return new Concrete.SigmaExpression(data, buildTypeParameters(parameters));
  }

  @Override
  public Concrete.BinOpExpression visitBinOp(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Referable binOp, @Nullable Abstract.Expression right, Void params) {
    return new Concrete.BinOpExpression(data, left.accept(this, null), binOp, right == null ? null : right.accept(this, null));
  }

  @Override
  public Concrete.BinOpSequenceExpression visitBinOpSequence(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, Void params) {
    List<Concrete.BinOpSequenceElem> elems = new ArrayList<>(sequence.size());
    for (Abstract.BinOpSequenceElem elem : sequence) {
      Abstract.Expression arg = elem.getArgument();
      Referable referable = elem.getBinOpReference();
      elems.add(new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(referable instanceof UnresolvedReference ? ((UnresolvedReference) referable).getData() : referable, elem.getBinOpReference()), arg == null ? null : arg.accept(this, null)));
    }
    return new Concrete.BinOpSequenceExpression(data, left.accept(this, null), elems);
  }

  @Override
  public Concrete.CaseExpression visitCase(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> absExpressions, @Nonnull Collection<? extends Abstract.FunctionClause> clauses, Void params) {
    if (absExpressions.isEmpty()) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    List<Concrete.Expression> expressions = new ArrayList<>(absExpressions.size());
    for (Abstract.Expression expression : absExpressions) {
      expressions.add(expression.accept(this, null));
    }
    return new Concrete.CaseExpression(data, expressions, buildClauses(clauses));
  }

  @Override
  public Concrete.ProjExpression visitProj(@Nullable Object data, @Nonnull Abstract.Expression expression, int field, Void params) {
    return new Concrete.ProjExpression(data, expression.accept(this, null), field - 1);
  }

  @Override
  public Concrete.Expression visitFieldAccs(@Nullable Object data, @Nonnull Abstract.Expression expression, @Nonnull Collection<Integer> fieldAccs, Void params) {
    Concrete.Expression result = expression.accept(this, null);
    for (Integer fieldAcc : fieldAccs) {
      result = new Concrete.ProjExpression(data, result, fieldAcc - 1);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, Void params) {
    if (baseClass == null) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    Concrete.Expression result = baseClass.accept(this, null);
    if (implementations != null) {
      result = new Concrete.ClassExtExpression(data, result, buildImplementations(implementations));
    }
    if (isNew) {
      result = new Concrete.NewExpression(data, result);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitLet(@Nullable Object data, @Nonnull Collection<? extends Abstract.LetClause> absClauses, @Nullable Abstract.Expression expression, Void params) {
    if (expression == null) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    if (absClauses.isEmpty()) {
      myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(data)));
      return expression.accept(this, null);
    }

    List<Concrete.LetClause> clauses = new ArrayList<>(absClauses.size());
    for (Abstract.LetClause clause : absClauses) {
      Abstract.Expression term = clause.getTerm();
      if (term == null) {
        myErrorReporter.report(new ProxyError(myDefinition, AbstractExpressionError.incomplete(clause.getReferable())));
      } else {
        Abstract.Expression resultType = clause.getResultType();
        clauses.add(new Concrete.LetClause(clause.getReferable(), buildParameters(clause.getParameters()), resultType == null ? null : resultType.accept(this, null), term.accept(this, null)));
      }
    }
    return new Concrete.LetExpression(data, clauses, expression.accept(this, null));
  }

  @Override
  public Concrete.NumericLiteral visitNumericLiteral(@Nullable Object data, @Nonnull BigInteger number, Void params) {
    return new Concrete.NumericLiteral(data, number);
  }

  // LevelExpression

  @Override
  public Concrete.InfLevelExpression visitInf(@Nullable Object data, Void param) {
    return new Concrete.InfLevelExpression(data);
  }

  @Override
  public Concrete.PLevelExpression visitLP(@Nullable Object data, Void param) {
    return new Concrete.PLevelExpression(data);
  }

  @Override
  public Concrete.HLevelExpression visitLH(@Nullable Object data, Void param) {
    return new Concrete.HLevelExpression(data);
  }

  @Override
  public Concrete.NumberLevelExpression visitNumber(@Nullable Object data, int number, Void param) {
    return new Concrete.NumberLevelExpression(data, number);
  }

  @Override
  public Concrete.SucLevelExpression visitSuc(@Nullable Object data, @Nullable Abstract.LevelExpression expr, Void param) {
    if (expr == null) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    return new Concrete.SucLevelExpression(data, expr.accept(this, null));
  }

  @Override
  public Concrete.MaxLevelExpression visitMax(@Nullable Object data, @Nullable Abstract.LevelExpression left, @Nullable Abstract.LevelExpression right, Void param) {
    if (left == null || right == null) {
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    return new Concrete.MaxLevelExpression(data, left.accept(this, null), right.accept(this, null));
  }

  @Override
  public Concrete.InferVarLevelExpression visitVar(@Nullable Object data, @Nonnull InferenceLevelVariable var, Void param) {
    return new Concrete.InferVarLevelExpression(data, var);
  }
}
