package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConcreteBuilder implements AbstractDefinitionVisitor<Concrete.Definition>, AbstractExpressionVisitor<Void, Concrete.Expression>, AbstractLevelExpressionVisitor<Void, Concrete.LevelExpression> {
  private final ErrorReporter myErrorReporter;

  private ConcreteBuilder(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public static Concrete.Definition convert(Abstract.Definition definition, ErrorReporter errorReporter) {
    try {
      return definition.accept(new ConcreteBuilder(errorReporter));
    } catch (AbstractConversionError.Exception e) {
      errorReporter.report(e.error);
      return null;
    }
  }

  // Definition

  @Override
  public Concrete.FunctionDefinition visitFunction(Abstract.FunctionDefinition def) {
    Concrete.FunctionBody body;
    Abstract.FunctionBody absBody = def.getBody();
    Abstract.Expression term = absBody.getTerm();
    if (term != null) {
      Object data = absBody.getData();
      body = new Concrete.TermFunctionBody(data, term.accept(this, null));
      if (!absBody.getEliminatedExpressions().isEmpty()) {
        myErrorReporter.report(new AbstractConversionError(Error.Level.WARNING, "Eliminated expressions are ignored", data));
      }
      if (!absBody.getClauses().isEmpty()) {
        myErrorReporter.report(new AbstractConversionError(Error.Level.WARNING, "Clauses are ignored", data));
      }
    } else {
      body = new Concrete.ElimFunctionBody(absBody.getData(), buildReferenceExpressions(absBody.getEliminatedExpressions()), buildClauses(absBody.getClauses()));
    }

    Abstract.Expression resultType = def.getResultType();
    return new Concrete.FunctionDefinition(def.getReferable(), buildParameters(def.getParameters()), resultType == null ? null : resultType.accept(this, null), body);
  }

  @Override
  public Concrete.DataDefinition visitData(Abstract.DataDefinition def) {
    Abstract.Expression absUniverse = def.getUniverse();
    Concrete.Expression universe = absUniverse == null ? null : absUniverse.accept(this, null);
    if (universe != null && !(universe instanceof Concrete.UniverseExpression)) {
      myErrorReporter.report(new AbstractConversionError(Error.Level.ERROR, "Expected a universe", universe.getData()));
    }

    Collection<? extends Abstract.ConstructorClause> absClauses = def.getClauses();
    List<Concrete.ConstructorClause> clauses = new ArrayList<>(absClauses.size());
    Concrete.DataDefinition data = new Concrete.DataDefinition(def.getReferable(), buildTypeParameters(def.getParameters()), buildReferenceExpressions(def.getEliminatedReferences()), def.isTruncated(), universe instanceof Concrete.UniverseExpression ? (Concrete.UniverseExpression) universe : null, clauses);

    for (Abstract.ConstructorClause clause : absClauses) {
      Collection<? extends Abstract.Constructor> absConstructors = clause.getConstructors();
      List<Concrete.Constructor> constructors = new ArrayList<>(absConstructors.size());
      for (Abstract.Constructor constructor : absConstructors) {
        constructors.add(new Concrete.Constructor(constructor.getReferable(), data, buildTypeParameters(constructor.getParameters()), buildReferenceExpressions(constructor.getEliminatedReferences()), buildClauses(constructor.getClauses())));
      }
      clauses.add(new Concrete.ConstructorClause(clause.getData(), buildPatterns(clause.getPatterns()), constructors));
    }

    return data;
  }

  private List<Concrete.ReferenceExpression> buildReferenceExpressions(Collection<? extends Abstract.Expression> absElimExpressions) {
    List<Concrete.ReferenceExpression> elimExpressions = new ArrayList<>(absElimExpressions.size());
    for (Abstract.Expression absElimExpression : absElimExpressions) {
      Concrete.Expression elimExpression = absElimExpression.accept(this, null);
      if (elimExpression instanceof Concrete.ReferenceExpression) {
        elimExpressions.add((Concrete.ReferenceExpression) elimExpression);
      } else {
        throw new AbstractConversionError.Exception(new AbstractConversionError(Error.Level.ERROR, "Expected a reference", absElimExpression.getData()));
      }
    }
    return elimExpressions;
  }

  @Override
  public Concrete.ClassDefinition visitClass(Abstract.ClassDefinition def) {
    List<Concrete.ClassField> classFields = new ArrayList<>();
    Concrete.ClassDefinition classDef = new Concrete.ClassDefinition(def.getReferable(), buildTypeParameters(def.getParameters()), buildReferenceExpressions(def.getSuperClasses()), classFields, buildImplementations(def.getClassFieldImpls()));

    for (Abstract.ClassField field : def.getClassFields()) {
      classFields.add(new Concrete.ClassField(field.getReferable(), classDef, field.getResultType().accept(this, null)));
    }

    return classDef;
  }

  private List<Concrete.ClassFieldImpl> buildImplementations(Collection<? extends Abstract.ClassFieldImpl> absImplementations) {
    List<Concrete.ClassFieldImpl> implementations = new ArrayList<>();
    for (Abstract.ClassFieldImpl implementation : absImplementations) {
      implementations.add(new Concrete.ClassFieldImpl(implementation.getData(), implementation.getImplementedField(), implementation.getImplementation().accept(this, null)));
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
        throw new AbstractConversionError.Exception(new AbstractConversionError(Error.Level.ERROR, "Expected a single variable", parameter.getData()));
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
        throw new AbstractConversionError.Exception(new AbstractConversionError(Error.Level.ERROR, "Expected a typed parameter", parameter.getData()));
      }
    }
    return parameters;
  }

  private Concrete.Pattern buildPattern(Abstract.Pattern pattern) {
    Referable reference = pattern.getReference();
    if (reference == null) {
      if (pattern.getArguments().isEmpty()) {
        return new Concrete.EmptyPattern(pattern.getData(), pattern.isExplicit());
      } else {
        throw new AbstractConversionError.Exception(new AbstractConversionError(Error.Level.ERROR, "Missing a reference", pattern.getData()));
      }
    } else {
      return reference instanceof GlobalReferable || reference instanceof UnresolvedReference ? new Concrete.ConstructorPattern(pattern.getData(), pattern.isExplicit(), reference, buildPatterns(pattern.getArguments())) : new Concrete.NamePattern(pattern.getData(), pattern.isExplicit(), reference);
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
      clauses.add(new Concrete.FunctionClause(clause.getData(), buildPatterns(clause.getPatterns()), clause.getExpression().accept(this, null)));
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
  public Concrete.ReferenceExpression visitReference(@Nullable Object data, @Nullable Abstract.Expression expr, @Nonnull Referable referent, Void params) {
    return new Concrete.ReferenceExpression(data, expr == null ? null : expr.accept(this, null), referent);
  }

  @Override
  public Concrete.ModuleCallExpression visitModuleCall(@Nullable Object data, @Nonnull ModulePath modulePath, Void params) {
    return new Concrete.ModuleCallExpression(data, modulePath);
  }

  @Override
  public Concrete.LamExpression visitLam(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nonnull Abstract.Expression body, Void params) {
    return new Concrete.LamExpression(data, buildParameters(parameters), body.accept(this, null));
  }

  @Override
  public Concrete.PiExpression visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nonnull Abstract.Expression codomain, Void params) {
    return new Concrete.PiExpression(data, buildTypeParameters(parameters), codomain.accept(this, null));
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(@Nullable Object data, @Nonnull Abstract.LevelExpression pLevel, @Nonnull Abstract.LevelExpression hLevel, Void params) {
    return new Concrete.UniverseExpression(data, pLevel.accept(this, null), hLevel.accept(this, null));
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
    List<Concrete.Expression> fields = new ArrayList<>(absFields.size());
    for (Abstract.Expression field : absFields) {
      fields.add(field.accept(this, null));
    }
    return new Concrete.TupleExpression(data, fields);
  }

  @Override
  public Concrete.SigmaExpression visitSigma(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, Void params) {
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
      Concrete.Expression binOp = elem.getBinOpCall().accept(this, null);
      if (!(binOp instanceof Concrete.ReferenceExpression)) {
        throw new AbstractConversionError.Exception(new AbstractConversionError(Error.Level.ERROR, "Expected an infix operator", binOp.getData()));
      }
      elems.add(new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(binOp.getData(), ((Concrete.ReferenceExpression) binOp).getExpression(), ((Concrete.ReferenceExpression) binOp).getReferent()), arg == null ? null : arg.accept(this, null)));
    }
    return new Concrete.BinOpSequenceExpression(data, left.accept(this, null), elems);
  }

  @Override
  public Concrete.CaseExpression visitCase(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> absExpressions, @Nonnull Collection<? extends Abstract.FunctionClause> clauses, Void params) {
    List<Concrete.Expression> expressions = new ArrayList<>(absExpressions.size());
    for (Abstract.Expression expression : absExpressions) {
      expressions.add(expression.accept(this, null));
    }
    return new Concrete.CaseExpression(data, expressions, buildClauses(clauses));
  }

  @Override
  public Concrete.ProjExpression visitProj(@Nullable Object data, @Nonnull Abstract.Expression expression, int field, Void params) {
    return new Concrete.ProjExpression(data, expression.accept(this, null), field);
  }

  @Override
  public Concrete.ClassExtExpression visitClassExt(@Nullable Object data, @Nonnull Abstract.Expression baseClass, @Nonnull Collection<? extends Abstract.ClassFieldImpl> implementations, Void params) {
    return new Concrete.ClassExtExpression(data, baseClass.accept(this, null), buildImplementations(implementations));
  }

  @Override
  public Concrete.NewExpression visitNew(@Nullable Object data, @Nonnull Abstract.Expression expression, Void params) {
    return new Concrete.NewExpression(data, expression.accept(this, null));
  }

  @Override
  public Concrete.LetExpression visitLet(@Nullable Object data, @Nonnull Collection<? extends Abstract.LetClause> absClauses, @Nonnull Abstract.Expression expression, Void params) {
    List<Concrete.LetClause> clauses = new ArrayList<>(absClauses.size());
    for (Abstract.LetClause clause : absClauses) {
      Abstract.Expression resultType = clause.getResultType();
      clauses.add(new Concrete.LetClause(clause.getReferable(), buildParameters(clause.getParameters()), resultType == null ? null : resultType.accept(this, null), clause.getTerm().accept(this, null)));
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
  public Concrete.SucLevelExpression visitSuc(@Nullable Object data, Abstract.LevelExpression expr, Void param) {
    return new Concrete.SucLevelExpression(data, expr.accept(this, null));
  }

  @Override
  public Concrete.MaxLevelExpression visitMax(@Nullable Object data, Abstract.LevelExpression left, Abstract.LevelExpression right, Void param) {
    return new Concrete.MaxLevelExpression(data, left.accept(this, null), right.accept(this, null));
  }

  @Override
  public Concrete.InferVarLevelExpression visitVar(@Nullable Object data, InferenceLevelVariable var, Void param) {
    return new Concrete.InferVarLevelExpression(data, var);
  }
}
