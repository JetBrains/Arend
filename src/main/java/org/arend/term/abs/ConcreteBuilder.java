package org.arend.term.abs;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.error.CountingErrorReporter;
import org.arend.error.Error;
import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.term.ClassFieldKind;
import org.arend.term.Fixity;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.ProxyError;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteBuilder implements AbstractDefinitionVisitor<Concrete.Definition>, AbstractExpressionVisitor<Void, Concrete.Expression>, AbstractLevelExpressionVisitor<Void, Concrete.LevelExpression> {
  private final ReferableConverter myReferableConverter;
  private final LocalErrorReporter myErrorReporter;
  private final TCReferable myDefinition;
  private boolean myHasErrors = false;

  private ConcreteBuilder(ReferableConverter referableConverter, ErrorReporter errorReporter, TCReferable definition) {
    myReferableConverter = referableConverter;
    myDefinition = definition;
    myErrorReporter = new LocalErrorReporter() {
      @Override
      public void report(LocalError localError) {
        myHasErrors = true;
        errorReporter.report(new ProxyError(myDefinition, localError));
      }

      @Override
      public void report(GeneralError error) {
        myHasErrors = true;
        errorReporter.report(error);
      }
    };
  }

  public static Concrete.Definition convert(ReferableConverter referableConverter, Abstract.Definition definition, ErrorReporter errorReporter) {
    ConcreteBuilder builder = new ConcreteBuilder(referableConverter, errorReporter, referableConverter.toDataLocatedReferable(definition.getReferable()));
    Concrete.Definition result = definition.accept(builder);
    if (builder.myHasErrors) {
      result.setHasErrors();
    }
    return result;
  }

  public static List<Concrete.Parameter> convertParams(ReferableConverter referableConverter, List<? extends Abstract.Parameter> parameters) {
    CountingErrorReporter cer = new CountingErrorReporter();
    List<Concrete.Parameter> result;
    try {
      ConcreteBuilder cb = new ConcreteBuilder(referableConverter, cer, null);
      result = cb.buildParameters(parameters);
    } catch (AbstractExpressionError.Exception e) {
      return null;
    }
    if (cer.getErrorsNumber() == 0) return result; else return null;
  }

  public static Concrete.Expression convertExpression(ReferableConverter referableConverter, Abstract.Expression expression) {
    CountingErrorReporter cer = new CountingErrorReporter();
    Concrete.Expression result;
    try {
      ConcreteBuilder cb = new ConcreteBuilder(referableConverter, cer, null);
      result = expression.accept(cb, null);
    } catch (AbstractExpressionError.Exception e) {
      return null;
    }
    if (cer.getErrorsNumber() == 0) return result; else return null;
  }

  @Override
  public boolean visitErrors() {
    return true;
  }

  // Definition

  private void setEnclosingClass(Concrete.Definition definition, Abstract.Definition abstractDef) {
    TCReferable enclosingClass = myReferableConverter.toDataLocatedReferable(abstractDef.getEnclosingClass());
    if (enclosingClass instanceof TCClassReferable && !(definition instanceof Concrete.ClassDefinition)) {
      definition.enclosingClass = (TCClassReferable) enclosingClass;
    }
  }

  @Override
  public boolean reportError(Abstract.ErrorData errorData) {
    if (errorData != null) {
      myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, errorData.message, errorData));
      return true;
    } else {
      return false;
    }
  }

  private void throwError(Abstract.ErrorData errorData) {
    if (errorData != null) {
      throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, errorData.message, errorData));
    }
  }

  @Override
  public Concrete.FunctionDefinition visitFunction(Abstract.FunctionDefinition def) {
    reportError(def.getErrorData());

    Concrete.FunctionBody body;
    Abstract.Expression term = def.getTerm();
    try {
      if (def.withTerm()) {
        if (term == null) {
          throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(def));
        }
        Object data = term.getData();
        body = new Concrete.TermFunctionBody(data, term.accept(this, null));
      } else if (def.isCowith()) {
        body = new Concrete.CoelimFunctionBody(myDefinition, buildImplementations(def.getClassFieldImpls()));
      } else {
        body = new Concrete.ElimFunctionBody(myDefinition, buildReferences(def.getEliminatedExpressions()), buildClauses(def.getClauses()));
      }
    } catch (AbstractExpressionError.Exception e) {
      myErrorReporter.report(e.error);
      Object data = term == null ? myReferableConverter.toDataLocatedReferable(def.getReferable()) : term.getData();
      body = new Concrete.TermFunctionBody(data, new Concrete.ErrorHoleExpression(data, e.error));
    }

    List<Concrete.TelescopeParameter> parameters = buildTelescopeParameters(def.getParameters());
    Concrete.Expression type;
    Concrete.Expression typeLevel;
    try {
      Abstract.Expression resultType = def.getResultType();
      Abstract.Expression resultTypeLevel = checkResultTypeLevel(resultType, def.getResultTypeLevel());
      type = resultType == null ? null : resultType.accept(this, null);
      typeLevel = resultTypeLevel == null ? null : resultTypeLevel.accept(this, null);
    } catch (AbstractExpressionError.Exception e) {
      myErrorReporter.report(e.error);
      type = null;
      typeLevel = null;
    }

    Concrete.FunctionDefinition.Kind kind = def.isCoerce()
      ? Concrete.FunctionDefinition.Kind.COERCE
      : def.isLevel()
        ? Concrete.FunctionDefinition.Kind.LEVEL
        : def.isLemma()
          ? Concrete.FunctionDefinition.Kind.LEMMA
          : Concrete.FunctionDefinition.Kind.FUNC;
    Concrete.FunctionDefinition result = Concrete.UseDefinition.make(kind, myDefinition, parameters, type, typeLevel, body, myReferableConverter.toDataLocatedReferable(def.getReferable().getLocatedReferableParent()));
    setEnclosingClass(result, def);
    return result;
  }

  @Override
  public Concrete.DataDefinition visitData(Abstract.DataDefinition def) {
    reportError(def.getErrorData());

    Concrete.Expression universe;
    try {
      Abstract.Expression absUniverse = def.getUniverse();
      universe = absUniverse == null ? null : absUniverse.accept(this, null);
      if (universe != null && !(universe instanceof Concrete.UniverseExpression)) {
        if (!reportError(absUniverse.getErrorData())) {
          myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Expected a universe", universe.getData()));
        }
      }
    } catch (AbstractExpressionError.Exception e) {
      myErrorReporter.report(e.error);
      universe = null;
    }

    List<Concrete.TypeParameter> typeParameters;
    try {
      typeParameters = buildTypeParameters(def.getParameters());
    } catch (AbstractExpressionError.Exception e) {
      myErrorReporter.report(e.error);
      typeParameters = Collections.emptyList();
    }

    Collection<? extends Abstract.ConstructorClause> absClauses = def.getClauses();
    List<Concrete.ConstructorClause> clauses = new ArrayList<>(absClauses.size());
    Collection<? extends Abstract.Reference> elimExpressions = def.getEliminatedExpressions();
    Concrete.DataDefinition data = new Concrete.DataDefinition(myDefinition, typeParameters, elimExpressions == null ? null : buildReferences(elimExpressions), def.isTruncated(), universe instanceof Concrete.UniverseExpression ? (Concrete.UniverseExpression) universe : null, clauses);
    setEnclosingClass(data, def);

    for (Abstract.ConstructorClause clause : absClauses) {
      Collection<? extends Abstract.Constructor> absConstructors = clause.getConstructors();
      if (absConstructors.isEmpty()) {
        if (!reportError(clause.getErrorData())) {
          myErrorReporter.report(AbstractExpressionError.incomplete(clause));
        }
        continue;
      }

      try {
        List<Concrete.Constructor> constructors = new ArrayList<>(absConstructors.size());
        for (Abstract.Constructor constructor : absConstructors) {
          reportError(constructor.getErrorData());
          TCReferable constructorRef = myReferableConverter.toDataLocatedReferable(constructor.getReferable());
          if (constructorRef != null) {
            Concrete.Constructor cons = new Concrete.Constructor(constructorRef, data, buildTypeParameters(constructor.getParameters()), buildReferences(constructor.getEliminatedExpressions()), buildClauses(constructor.getClauses()));
            Abstract.Expression resultType = constructor.getResultType();
            if (resultType != null) {
              cons.setResultType(resultType.accept(this, null));
            }
            constructors.add(cons);
          } else {
            myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Internal error: cannot locate constructor", constructor));
          }
        }

        Collection<? extends Abstract.Pattern> patterns = clause.getPatterns();
        clauses.add(new Concrete.ConstructorClause(clause.getData(), patterns.isEmpty() ? null : buildPatterns(patterns), constructors));
      } catch (AbstractExpressionError.Exception e) {
        myErrorReporter.report(e.error);
      }
    }

    data.setUsedDefinitions(visitUsedDefinitions(def.getUsedDefinitions()));
    return data;
  }

  private void buildClassParameters(Collection<? extends Abstract.FieldParameter> absParameters, Concrete.ClassDefinition classDef, List<Concrete.ClassField> fields) {
    TCFieldReferable coercingField = null;
    boolean isForced = false;

    for (Abstract.FieldParameter absParameter : absParameters) {
      try {
        boolean forced = absParameter.isClassifying();
        Concrete.Parameter parameter = buildParameter(absParameter, false);
        if (parameter instanceof Concrete.TelescopeParameter) {
          for (Referable referable : ((Concrete.TelescopeParameter) parameter).getReferableList()) {
            if (referable instanceof TCFieldReferable) {
              if (forced) {
                if (isForced) {
                  myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Class can have at most one classifying field", parameter));
                } else {
                  coercingField = (TCFieldReferable) referable;
                  isForced = true;
                }
              } else if (coercingField == null && parameter.getExplicit()) {
                coercingField = (TCFieldReferable) referable;
              }
              fields.add(new Concrete.ClassField((TCFieldReferable) referable, classDef, parameter.getExplicit(), ClassFieldKind.FIELD, new ArrayList<>(), ((Concrete.TelescopeParameter) parameter).type, null));
            } else {
              myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Incorrect field parameter", referable));
            }
          }
        } else {
          myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Expected a typed parameter with a name", parameter.getData()));
        }
      } catch (AbstractExpressionError.Exception e) {
        myErrorReporter.report(e.error);
      }
    }

    if (coercingField != null) {
      classDef.setCoercingField(coercingField, isForced);
    }
  }

  @Override
  public Concrete.Definition visitClass(Abstract.ClassDefinition def) {
    reportError(def.getErrorData());

    List<Concrete.ClassFieldImpl> implementations = buildImplementationsSafe(def.getClassFieldImpls());

    Abstract.Reference underlyingClass = def.getUnderlyingClass();
    if (underlyingClass != null && !implementations.isEmpty()) {
      myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Class synonyms cannot have implementations", implementations.get(0)));
    }

    List<? extends Abstract.FieldParameter> classParameters = def.getParameters();
    if (underlyingClass != null && !classParameters.isEmpty()) {
      myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Class synonyms cannot have parameters", def));
    }

    if (underlyingClass == null) {
      List<Concrete.ClassField> classFields = new ArrayList<>();
      Concrete.ClassDefinition classDef = new Concrete.ClassDefinition((TCClassReferable) myDefinition, def.isRecord(), buildReferences(def.getSuperClasses()), classFields, implementations);
      buildClassParameters(classParameters, classDef, classFields);
      setEnclosingClass(classDef, def);

      for (Abstract.ClassField field : def.getClassFields()) {
        Abstract.Expression resultType = field.getResultType();
        LocatedReferable fieldRefOrig = field.getReferable();
        TCReferable fieldRef = myReferableConverter.toDataLocatedReferable(fieldRefOrig);
        if (resultType == null || !(fieldRef instanceof TCFieldReferable)) {
          if (!reportError(field.getErrorData())) {
            myErrorReporter.report(fieldRef != null && !(fieldRef instanceof TCFieldReferable)
              ? new AbstractExpressionError(Error.Level.ERROR, "Incorrect field", fieldRef)
              : AbstractExpressionError.incomplete(fieldRef == null ? field : fieldRef));
          }
        } else {
          try {
            List<? extends Abstract.Parameter> parameters = field.getParameters();
            Concrete.Expression type = resultType.accept(this, null);
            Abstract.Expression resultTypeLevel = field.getResultTypeLevel();
            Concrete.Expression typeLevel = resultTypeLevel == null ? null : resultTypeLevel.accept(this, null);
            classFields.add(new Concrete.ClassField((TCFieldReferable) fieldRef, classDef, true, field.getClassFieldKind(), buildTypeParameters(parameters), type, typeLevel));
          } catch (AbstractExpressionError.Exception e) {
            myErrorReporter.report(e.error);
          }
        }
      }

      classDef.setUsedDefinitions(visitUsedDefinitions(def.getUsedDefinitions()));
      return classDef;
    } else {
      return null;
    }
  }

  private List<TCReferable> visitUsedDefinitions(Collection<? extends LocatedReferable> usedDefinitions) {
    if (!usedDefinitions.isEmpty()) {
      List<TCReferable> tcUsedDefinitions = new ArrayList<>(usedDefinitions.size());
      for (LocatedReferable coercingFunction : usedDefinitions) {
        tcUsedDefinitions.add(myReferableConverter.toDataLocatedReferable(coercingFunction));
      }
      return tcUsedDefinitions;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public Concrete.Instance visitInstance(Abstract.InstanceDefinition def) {
    List<Concrete.TelescopeParameter> parameters = buildTelescopeParameters(def.getParameters());
    List<Concrete.ClassFieldImpl> implementations = buildImplementationsSafe(def.getClassFieldImpls());

    Abstract.Expression resultType = def.getResultType();
    if (resultType == null) {
      throwError(def.getErrorData());
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(def));
    }

    Concrete.Instance instance = new Concrete.Instance(myDefinition, parameters, resultType.accept(this, null), implementations);
    setEnclosingClass(instance, def);
    return instance;
  }

  private Concrete.ReferenceExpression buildReference(Abstract.Reference reference) {
    return new Concrete.ReferenceExpression(reference.getData(), reference.getReferent());
  }

  private List<Concrete.ReferenceExpression> buildReferences(Collection<? extends Abstract.Reference> absElimExpressions) {
    List<Concrete.ReferenceExpression> elimExpressions = new ArrayList<>(absElimExpressions.size());
    for (Abstract.Reference reference : absElimExpressions) {
      elimExpressions.add(buildReference(reference));
    }
    return elimExpressions;
  }

  private List<Concrete.ClassFieldImpl> buildImplementationsSafe(Collection<? extends Abstract.ClassFieldImpl> absImplementations) {
    try {
      return buildImplementations(absImplementations);
    } catch (AbstractExpressionError.Exception e) {
      myErrorReporter.report(e.error);
      return Collections.emptyList();
    }
  }

  private List<Concrete.ClassFieldImpl> buildImplementations(Collection<? extends Abstract.ClassFieldImpl> absImplementations) {
    List<Concrete.ClassFieldImpl> implementations = new ArrayList<>();
    for (Abstract.ClassFieldImpl implementation : absImplementations) {
      Referable implementedField = implementation.getImplementedField();
      if (implementedField == null) {
        continue;
      }

      Abstract.Expression impl = implementation.getImplementation();
      if (impl != null) {
        List<? extends Abstract.Parameter> parameters = implementation.getParameters();
        Concrete.Expression term = impl.accept(this, null);
        if (!parameters.isEmpty()) {
          term = new Concrete.LamExpression(parameters.get(0).getData(), buildParameters(parameters), term);
        }

        implementations.add(new Concrete.ClassFieldImpl(implementation.getData(), implementedField, term, Collections.emptyList()));
      } else {
        Object data = implementation.getData();
        implementations.add(new Concrete.ClassFieldImpl(data, implementedField, null, buildImplementations(implementation.getClassFieldImpls())));
      }
    }
    return implementations;
  }

  private Concrete.Parameter buildParameter(Abstract.Parameter parameter, boolean isNamed) {
    List<? extends Referable> referableList = parameter.getReferableList();
    Abstract.Expression type = parameter.getType();
    if (type == null) {
      if (referableList.size() == 1) {
        return new Concrete.NameParameter(parameter.getData(), parameter.isExplicit(), myReferableConverter.toDataReferable(referableList.get(0)));
      } else {
        throwError(parameter.getErrorData());
        throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, "Expected a single variable", parameter.getData()));
      }
    } else {
      if (!isNamed && (referableList.isEmpty() || referableList.size() == 1 && referableList.get(0) == null)) {
        return new Concrete.TypeParameter(parameter.getData(), parameter.isExplicit(), type.accept(this, null));
      } else {
        List<Referable> dataReferableList = new ArrayList<>(referableList.size());
        for (Referable referable : referableList) {
          dataReferableList.add(referable instanceof LocatedReferable ? myReferableConverter.toDataLocatedReferable((LocatedReferable) referable) : myReferableConverter.toDataReferable(referable));
        }
        return new Concrete.TelescopeParameter(parameter.getData(), parameter.isExplicit(), dataReferableList, type.accept(this, null));
      }
    }
  }

  private List<Concrete.Parameter> buildParameters(Collection<? extends Abstract.Parameter> absParameters) {
    List<Concrete.Parameter> parameters = new ArrayList<>(absParameters.size());
    for (Abstract.Parameter absParameter : absParameters) {
      parameters.add(buildParameter(absParameter, true));
    }
    return parameters;
  }

  private List<Concrete.TypeParameter> buildTypeParameters(Collection<? extends Abstract.Parameter> absParameters) {
    List<Concrete.TypeParameter> parameters = new ArrayList<>(absParameters.size());
    for (Abstract.Parameter absParameter : absParameters) {
      Concrete.Parameter parameter = buildParameter(absParameter, false);
      if (parameter instanceof Concrete.TypeParameter) {
        parameters.add((Concrete.TypeParameter) parameter);
      } else {
        throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, "Expected a typed parameter", parameter.getData()));
      }
    }
    return parameters;
  }

  private List<Concrete.TelescopeParameter> buildTelescopeParameters(Collection<? extends Abstract.Parameter> absParameters) {
    try {
      List<Concrete.TelescopeParameter> parameters = new ArrayList<>(absParameters.size());
      for (Abstract.Parameter absParameter : absParameters) {
        Concrete.Parameter parameter = buildParameter(absParameter, true);
        if (parameter instanceof Concrete.TelescopeParameter) {
          parameters.add((Concrete.TelescopeParameter) parameter);
        } else {
          throw new AbstractExpressionError.Exception(new AbstractExpressionError(Error.Level.ERROR, "Expected a typed parameter", parameter.getData()));
        }
      }
      return parameters;
    } catch (AbstractExpressionError.Exception e) {
      myErrorReporter.report(e.error);
      return Collections.emptyList();
    }
  }

  private Concrete.Pattern buildPattern(Abstract.Pattern pattern) {
    Referable reference = pattern.getHeadReference();
    if (reference == null) {
      Integer number = pattern.getNumber();
      if (number != null) {
        return new Concrete.NumberPattern(pattern.getData(), number);
      }
    }

    if (reference == null && !pattern.isUnnamed()) {
      return new Concrete.TuplePattern(pattern.getData(), pattern.isExplicit(), buildPatterns(pattern.getArguments()));
    } else {
      List<? extends Abstract.Pattern> args = pattern.getArguments();
      if (reference instanceof GlobalReferable || !args.isEmpty()) {
        return new Concrete.ConstructorPattern(pattern.getData(), pattern.isExplicit(), reference, buildPatterns(args));
      } else {
        return new Concrete.NamePattern(pattern.getData(), pattern.isExplicit(), myReferableConverter.toDataReferable(reference));
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
        throwError(clause.getErrorData());
        throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(clause));
      }
      Abstract.Expression expr = clause.getExpression();
      clauses.add(new Concrete.FunctionClause(clause.getData(), buildPatterns(patterns), expr == null ? null : expr.accept(this, null)));
    }
    return clauses;
  }

  // Expression

  @Override
  public Concrete.ReferenceExpression visitReference(@Nullable Object data, @Nonnull Referable referent, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.ReferenceExpression(data, referent, level1 == null ? null : level1.accept(this, null), level2 == null ? null : level2.accept(this, null));
  }

  @Override
  public Concrete.ReferenceExpression visitReference(@Nullable Object data, @Nonnull Referable referent, int lp, int lh, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.ReferenceExpression(data, referent, new Concrete.NumberLevelExpression(data, lp), new Concrete.NumberLevelExpression(data, lh));
  }

  @Override
  public Concrete.ThisExpression visitThis(@Nullable Object data) {
    return new Concrete.ThisExpression(data, null);
  }

  @Override
  public Concrete.Expression visitLam(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression body, @Nullable Abstract.ErrorData errorData, Void params) {
    if (body == null) {
      throwError(errorData);
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }

    if (parameters.isEmpty()) {
      if (!reportError(errorData)) {
        myErrorReporter.report(AbstractExpressionError.incomplete(data));
      }
      return body.accept(this, null);
    }

    reportError(errorData);
    return new Concrete.LamExpression(data, buildParameters(parameters), body.accept(this, null));
  }

  @Override
  public Concrete.Expression visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression codomain, @Nullable Abstract.ErrorData errorData, Void params) {
    if (codomain == null) {
      throwError(errorData);
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }

    if (parameters.isEmpty()) {
      if (!reportError(errorData)) {
        myErrorReporter.report(AbstractExpressionError.incomplete(data));
      }
      return codomain.accept(this, null);
    }

    reportError(errorData);
    return new Concrete.PiExpression(data, buildTypeParameters(parameters), codomain.accept(this, null));
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(@Nullable Object data, @Nullable Integer pLevelNum, @Nullable Integer hLevelNum, @Nullable Abstract.LevelExpression pLevel, @Nullable Abstract.LevelExpression hLevel, @Nullable Abstract.ErrorData errorData, Void params) {
    if (pLevelNum != null && hLevel == null) {
      hLevel = pLevel;
      pLevel = null;
    }
    if (pLevelNum != null && pLevel != null) {
      myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "p-level is already specified", pLevel.getData()));
    }
    if (hLevelNum != null && hLevel != null) {
      myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "h-level is already specified", hLevel.getData()));
    }

    reportError(errorData);
    return new Concrete.UniverseExpression(data,
      pLevelNum != null ? new Concrete.NumberLevelExpression(data, pLevelNum) : pLevel != null ? pLevel.accept(this, null) : null,
      hLevelNum != null ? (hLevelNum == Abstract.INFINITY_LEVEL ? new Concrete.InfLevelExpression(data) : new Concrete.NumberLevelExpression(data, hLevelNum)) : hLevel != null ? hLevel.accept(this, null) : null);
  }

  @Override
  public Concrete.HoleExpression visitInferHole(@Nullable Object data, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.HoleExpression(data);
  }

  @Override
  public Concrete.GoalExpression visitGoal(@Nullable Object data, @Nullable String name, @Nullable Abstract.Expression expression, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.GoalExpression(data, name, expression == null ? null : expression.accept(this, null));
  }

  @Override
  public Concrete.TupleExpression visitTuple(@Nullable Object data, @Nonnull Collection<? extends Abstract.Expression> absFields, @Nullable Abstract.ErrorData errorData, Void params) {
    List<Concrete.Expression> fields = new ArrayList<>(absFields.size());
    for (Abstract.Expression field : absFields) {
      fields.add(field.accept(this, null));
    }

    reportError(errorData);
    return new Concrete.TupleExpression(data, fields);
  }

  @Override
  public Concrete.SigmaExpression visitSigma(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.SigmaExpression(data, buildTypeParameters(parameters));
  }

  private Concrete.Expression makeBinOpSequence(Object data, Concrete.Expression left, Collection<? extends Abstract.BinOpSequenceElem> sequence) {
    if (sequence.isEmpty()) {
      return left;
    }

    List<Concrete.BinOpSequenceElem> elems = new ArrayList<>(sequence.size());
    elems.add(new Concrete.BinOpSequenceElem(left, Fixity.NONFIX, true));
    for (Abstract.BinOpSequenceElem elem : sequence) {
      Abstract.Expression arg = elem.getExpression();
      if (arg == null) {
        continue;
      }

      Concrete.Expression elemExpr = arg.accept(this, null);
      Fixity fixity = elem.getFixity();
      boolean isExplicit = elem.isExplicit();

      if (!isExplicit && fixity != Fixity.NONFIX || (fixity == Fixity.INFIX || fixity == Fixity.POSTFIX) && !(elemExpr instanceof Concrete.ReferenceExpression)) {
        myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "Inconsistent model", elem));
        fixity = isExplicit ? Fixity.UNKNOWN : Fixity.NONFIX;
      }

      elems.add(new Concrete.BinOpSequenceElem(elemExpr, fixity, isExplicit));
    }
    return new Concrete.BinOpSequenceExpression(data, elems);
  }

  @Override
  public Concrete.Expression visitBinOpSequence(@Nullable Object data, @Nonnull Abstract.Expression left, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return makeBinOpSequence(data, left.accept(this, null), sequence);
  }

  @Override
  public Concrete.CaseExpression visitCase(@Nullable Object data, @Nonnull Collection<? extends Abstract.CaseArgument> caseArgs, @Nullable Abstract.Expression resultType, @Nullable Abstract.Expression resultTypeLevel, @Nonnull Collection<? extends Abstract.FunctionClause> clauses, @Nullable Abstract.ErrorData errorData, Void params) {
    if (caseArgs.isEmpty()) {
      throwError(errorData);
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    List<Concrete.CaseArgument> concreteCaseArgs = new ArrayList<>(caseArgs.size());
    for (Abstract.CaseArgument caseArg : caseArgs) {
      Abstract.Expression type = caseArg.getType();
      concreteCaseArgs.add(new Concrete.CaseArgument(caseArg.getExpression().accept(this, null), myReferableConverter.toDataReferable(caseArg.getReferable()), type == null ? null : type.accept(this, null)));
    }

    reportError(errorData);
    resultTypeLevel = checkResultTypeLevel(resultType, resultTypeLevel);
    return new Concrete.CaseExpression(data, concreteCaseArgs, resultType == null ? null : resultType.accept(this, null), resultTypeLevel == null ? null : resultTypeLevel.accept(this, null), buildClauses(clauses));
  }

  private Abstract.Expression checkResultTypeLevel(Abstract.Expression resultType, Abstract.Expression resultTypeLevel) {
    if (resultType == null && resultTypeLevel != null) {
      myErrorReporter.report(new AbstractExpressionError(Error.Level.ERROR, "The level of a type can be specified only if the type is also specified", resultTypeLevel));
      return null;
    } else {
      return resultTypeLevel;
    }
  }

  @Override
  public Concrete.Expression visitFieldAccs(@Nullable Object data, @Nonnull Abstract.Expression expression, @Nonnull Collection<Integer> fieldAccs, @Nullable Abstract.ErrorData errorData, Void params) {
    Concrete.Expression result = expression.accept(this, null);
    for (Integer fieldAcc : fieldAccs) {
      result = new Concrete.ProjExpression(data, result, fieldAcc - 1);
    }

    reportError(errorData);
    return result;
  }

  @Override
  public Concrete.Expression visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, @Nullable Abstract.ErrorData errorData, Void params) {
    if (baseClass == null) {
      throwError(errorData);
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }

    Concrete.Expression result = baseClass.accept(this, null);
    if (implementations != null) {
      result = Concrete.ClassExtExpression.make(data, result, buildImplementations(implementations));
    }
    if (isNew) {
      result = new Concrete.NewExpression(data, result);
    }

    reportError(errorData);
    return makeBinOpSequence(data, result, sequence);
  }

  @Override
  public Concrete.Expression visitLet(@Nullable Object data, @Nonnull Collection<? extends Abstract.LetClause> absClauses, @Nullable Abstract.Expression expression, @Nullable Abstract.ErrorData errorData, Void params) {
    if (expression == null) {
      throwError(errorData);
      throw new AbstractExpressionError.Exception(AbstractExpressionError.incomplete(data));
    }
    if (absClauses.isEmpty()) {
      if (!reportError(errorData)) {
        myErrorReporter.report(AbstractExpressionError.incomplete(data));
      }
      return expression.accept(this, null);
    }

    List<Concrete.LetClause> clauses = new ArrayList<>(absClauses.size());
    for (Abstract.LetClause clause : absClauses) {
      Abstract.Expression term = clause.getTerm();
      if (term == null) {
        myErrorReporter.report(AbstractExpressionError.incomplete(clause.getReferable()));
      } else {
        Abstract.Expression resultType = clause.getResultType();
        clauses.add(new Concrete.LetClause(myReferableConverter.toDataReferable(clause.getReferable()), buildParameters(clause.getParameters()), resultType == null ? null : resultType.accept(this, null), term.accept(this, null)));
      }
    }

    reportError(errorData);
    return new Concrete.LetExpression(data, clauses, expression.accept(this, null));
  }

  @Override
  public Concrete.NumericLiteral visitNumericLiteral(@Nullable Object data, @Nonnull BigInteger number, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.NumericLiteral(data, number);
  }

  @Override
  public Concrete.Expression visitTyped(@Nullable Object data, @Nonnull Abstract.Expression expr, @Nonnull Abstract.Expression type, @Nullable Abstract.ErrorData errorData, Void params) {
    reportError(errorData);
    return new Concrete.TypedExpression(data, expr.accept(this, null), type.accept(this, null));
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
