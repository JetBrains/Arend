package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.error.Error;
import org.arend.error.doc.DocFactory;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.term.concrete.ConcreteLevelExpressionVisitor;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.ImplicitArgsInference;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.result.DefCallResult;
import org.arend.typechecking.result.TResult;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Decision;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

import static org.arend.typechecking.error.local.ArgInferenceError.expression;

public abstract class BaseTypechecker implements ConcreteExpressionVisitor<ExpectedType, TypecheckingResult>, ConcreteLevelExpressionVisitor<LevelVariable, Level> {
  protected final TypecheckerState state;
  protected Map<Referable, Binding> context;
  protected LocalErrorReporter errorReporter;
  private ImplicitArgsInference myArgsInference;

  protected BaseTypechecker(TypecheckerState typecheckerState, Map<Referable, Binding> context) {
    state = typecheckerState;
    this.context = context;
  }

  protected void setImplicitArgsInference(ImplicitArgsInference implicitArgsInference) {
    myArgsInference = implicitArgsInference;
  }

  public abstract boolean isDumb();

  public abstract TypecheckingResult typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, ExpectedType expectedType, Expression implExpr, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr);

  public abstract Equations getEquations();

  protected abstract TypecheckingResult checkResult(ExpectedType expectedType, TypecheckingResult result, Concrete.Expression expr);

  public abstract Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal);

  protected abstract TypecheckingResult checkExpr(Concrete.Expression expr, ExpectedType expectedType);

  public TypecheckingResult finalCheckExpr(Concrete.Expression expr, ExpectedType expectedType, boolean returnExpectedType) {
    return finalize(checkExpr(expr, expectedType), returnExpectedType && expectedType instanceof Expression ? (Expression) expectedType : null, expr);
  }

  public TypecheckingResult finalize(TypecheckingResult result, Expression expectedType, Concrete.SourceNode sourceNode) {
    if (result == null) {
      return expectedType == null ? null : new TypecheckingResult(null, expectedType);
    } else {
      if (expectedType != null && !result.type.isInstance(ClassCallExpression.class)) { // Use the inferred type if it is a class call
        result.type = expectedType;
      }
      return result;
    }
  }

  public Definition getTypechecked(TCReferable referable) {
    return state.getTypechecked(referable);
  }

  public void addBinding(@Nullable Referable referable, Binding binding) {
    if (referable != null) {
      context.put(referable, binding);
    }
  }

  protected TypecheckingResult tResultToResult(ExpectedType expectedType, TResult result, Concrete.Expression expr) {
    if (result != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(this), expr);
  }

  // Classes

  public boolean checkAllImplemented(ClassCallExpression classCall, Set<ClassField> pseudoImplemented, Concrete.SourceNode sourceNode) {
    int notImplemented = classCall.getDefinition().getNumberOfNotImplementedFields() - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else {
      List<GlobalReferable> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field) && !pseudoImplemented.contains(field)) {
          fields.add(field.getReferable());
        }
      }
      if (!fields.isEmpty()) {
        errorReporter.report(new FieldsImplementationError(false, fields, sourceNode));
      }
      return false;
    }
  }

  // Variables

  public Definition referableToDefinition(Referable referable, Concrete.SourceNode sourceNode) {
    if (referable == null || referable instanceof ErrorReference) {
      return null;
    }

    Definition definition = referable instanceof TCReferable ? state.getTypechecked((TCReferable) referable) : null;
    if (definition == null && sourceNode != null) {
      errorReporter.report(new TypecheckingError("Internal error: definition '" + referable.textRepresentation() + "' was not typechecked", sourceNode));
    }
    return definition;
  }

  public <T extends Definition> T referableToDefinition(Referable referable, Class<T> clazz, String errorMsg, Concrete.SourceNode sourceNode) {
    Definition definition = referableToDefinition(referable, sourceNode);
    if (definition == null) {
      return null;
    }
    if (clazz.isInstance(definition)) {
      return clazz.cast(definition);
    }

    if (sourceNode != null) {
      errorReporter.report(new WrongReferable(errorMsg, referable, sourceNode));
    }
    return null;
  }

  public ClassField referableToClassField(Referable referable, Concrete.SourceNode sourceNode) {
    return referableToDefinition(referable, ClassField.class, "Expected a class field", sourceNode);
  }

  private Definition getTypeCheckedDefinition(TCReferable definition, Concrete.Expression expr) {
    Definition typeCheckedDefinition = state.getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      errorReporter.report(new IncorrectReferenceError(definition, expr));
      return null;
    }
    if (!typeCheckedDefinition.status().headerIsOK()) {
      errorReporter.report(new HasErrors(Error.Level.ERROR, definition, expr));
      return null;
    } else {
      if (typeCheckedDefinition.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
        errorReporter.report(new HasErrors(Error.Level.WARNING, definition, expr));
      }
      return typeCheckedDefinition;
    }
  }

  private TResult typeCheckDefCall(TCReferable resolvedDefinition, Concrete.ReferenceExpression expr) {
    Definition definition = getTypeCheckedDefinition(resolvedDefinition, expr);
    if (definition == null) {
      return null;
    }

    Sort sortArgument;
    boolean isMin = definition instanceof DataDefinition && !definition.getParameters().hasNext();
    if (expr.getPLevel() == null && expr.getHLevel() == null) {
      sortArgument = isMin ? Sort.PROP : isDumb() ? Sort.UNKNOWN : Sort.generateInferVars(getEquations(), definition.hasUniverses(), expr);
      Level hLevel = null;
      if (definition instanceof DataDefinition && !sortArgument.isProp()) {
        hLevel = ((DataDefinition) definition).getSort().getHLevel();
      } else if (definition instanceof FunctionDefinition && !sortArgument.isProp()) {
        UniverseExpression universe = ((FunctionDefinition) definition).getResultType().getPiParameters(null, false).checkedCast(UniverseExpression.class);
        if (universe != null) {
          hLevel = universe.getSort().getHLevel();
        }
      }
      if (hLevel != null && hLevel.getMaxAddedConstant() == -1 && hLevel.getVar() == LevelVariable.HVAR) {
        getEquations().bindVariables((InferenceLevelVariable) sortArgument.getPLevel().getVar(), (InferenceLevelVariable) sortArgument.getHLevel().getVar());
      }
    } else {
      Level pLevel = null;
      if (expr.getPLevel() != null) {
        pLevel = expr.getPLevel().accept(this, LevelVariable.PVAR);
      }
      if (pLevel == null) {
        if (isMin) {
          pLevel = new Level(0);
        } else {
          InferenceLevelVariable pl = isDumb() ? InferenceLevelVariable.UNKNOWN_PVAR : new InferenceLevelVariable(LevelVariable.LvlType.PLVL, definition.hasUniverses(), expr);
          getEquations().addVariable(pl);
          pLevel = new Level(pl);
        }
      }

      Level hLevel = null;
      if (expr.getHLevel() != null) {
        hLevel = expr.getHLevel().accept(this, LevelVariable.HVAR);
      }
      if (hLevel == null) {
        if (isMin) {
          hLevel = new Level(-1);
        } else {
          InferenceLevelVariable hl = isDumb() ? InferenceLevelVariable.UNKNOWN_HVAR : new InferenceLevelVariable(LevelVariable.LvlType.HLVL, definition.hasUniverses(), expr);
          getEquations().addVariable(hl);
          hLevel = new Level(hl);
        }
      }

      sortArgument = new Sort(pLevel, hLevel);
    }

    return DefCallResult.makeTResult(expr, definition, sortArgument);
  }

  protected TResult getLocalVar(Referable ref, Concrete.SourceNode sourceNode) {
    if (ref instanceof UnresolvedReference || ref instanceof RedirectingReferable) {
      throw new IllegalStateException();
    }
    if (ref instanceof ErrorReference) {
      return null;
    }

    Binding def = context.get(ref);
    if (def == null) {
      errorReporter.report(new IncorrectReferenceError(ref, sourceNode));
      return null;
    }
    Expression type = def.getTypeExpr();
    if (type == null) {
      errorReporter.report(new ReferenceTypeError(ref));
      return null;
    } else {
      return new TypecheckingResult(def instanceof TypedEvaluatingBinding ? ((TypedEvaluatingBinding) def).getExpression() : new ReferenceExpression(def), type);
    }
  }

  public TResult visitReference(Concrete.ReferenceExpression expr) {
    Referable ref = expr.getReferent();
    if (!(ref instanceof GlobalReferable) && (expr.getPLevel() != null || expr.getHLevel() != null)) {
      errorReporter.report(new TypecheckingError("Level specifications are allowed only after definitions", expr.getPLevel() != null ? expr.getPLevel() : expr.getHLevel()));
    }
    return ref instanceof TCReferable ? typeCheckDefCall((TCReferable) ref, expr) : getLocalVar(expr.getReferent(), expr);
  }

  @Override
  public TypecheckingResult visitReference(Concrete.ReferenceExpression expr, ExpectedType expectedType) {
    TResult result = visitReference(expr);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public TypecheckingResult visitThis(Concrete.ThisExpression expr, ExpectedType expectedType) {
    errorReporter.report(new TypecheckingError("\\this expressions are allowed only in appropriate arguments of definitions and class extensions", expr));
    return null;
  }

  @Override
  public TypecheckingResult visitInferenceReference(Concrete.InferenceReferenceExpression expr, ExpectedType params) {
    return new TypecheckingResult(new InferenceReferenceExpression(expr.getVariable(), getEquations()), expr.getVariable().getType());
  }

  @Override
  public TypecheckingResult visitHole(Concrete.HoleExpression expr, ExpectedType expectedType) {
    if (expr.getError() != null) {
      return null;
    }

    if (expectedType instanceof Expression) {
      return new TypecheckingResult(new InferenceReferenceExpression(myArgsInference.newInferenceVariable((Expression) expectedType, expr), getEquations()), (Expression) expectedType);
    } else {
      errorReporter.report(new ArgInferenceError(expression(), expr, new Expression[0]));
      return null;
    }
  }

  // Level expressions

  @Override
  public Level visitInf(Concrete.InfLevelExpression expr, LevelVariable base) {
    if (base == LevelVariable.PVAR) {
      errorReporter.report(new TypecheckingError("\\inf is not a correct p-level", expr));
      return new Level(base);
    }
    return Level.INFINITY;
  }

  @Override
  public Level visitLP(Concrete.PLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.PVAR) {
      errorReporter.report(new TypecheckingError("Expected " + base, expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitLH(Concrete.HLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.HVAR) {
      errorReporter.report(new TypecheckingError("Expected " + base, expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitNumber(Concrete.NumberLevelExpression expr, LevelVariable base) {
    return new Level(expr.getNumber());
  }

  @Override
  public Level visitSuc(Concrete.SucLevelExpression expr, LevelVariable base) {
    return expr.getExpression().accept(this, base).add(1);
  }

  @Override
  public Level visitMax(Concrete.MaxLevelExpression expr, LevelVariable base) {
    return expr.getLeft().accept(this, base).max(expr.getRight().accept(this, base));
  }

  @Override
  public Level visitVar(Concrete.InferVarLevelExpression expr, LevelVariable base) {
    errorReporter.report(new TypecheckingError("Cannot typecheck an inference variable", expr));
    return new Level(base);
  }

  // Sorts

  public Sort getSortOfType(Expression expr, Concrete.SourceNode sourceNode) {
    Expression type = expr.getType(!isDumb());
    Sort sort = type == null ? null : type.toSort();
    if (sort == null) {
      if (isDumb()) {
        return Sort.UNKNOWN;
      }

      assert type != null;
      if (type.isInstance(ErrorExpression.class)) {
        return Sort.STD;
      }
      Sort result = Sort.generateInferVars(getEquations(), false, sourceNode);
      if (!CompareVisitor.compare(getEquations(), Equations.CMP.LE, type, new UniverseExpression(result), sourceNode)) {
        errorReporter.report(new TypeMismatchError(DocFactory.text("a type"), type, sourceNode));
      }
      return result;
    } else {
      return sort;
    }
  }

  private static Sort generateUniqueUpperBound(List<Sort> sorts) {
    LevelVariable pVar = null;
    LevelVariable hVar = null;
    for (Sort sort : sorts) {
      if (sort.getPLevel().getVar() != null) {
        if (pVar != null && pVar != sort.getPLevel().getVar()) {
          return null;
        }
        if (pVar == null) {
          pVar = sort.getPLevel().getVar();
        }
      }
      if (sort.getHLevel().getVar() != null) {
        if (hVar != null && hVar != sort.getHLevel().getVar()) {
          return null;
        }
        if (hVar == null) {
          hVar = sort.getHLevel().getVar();
        }
      }
    }

    if (sorts.isEmpty()) {
      return Sort.PROP;
    } else {
      Sort resultSort = sorts.get(0);
      for (int i = 1; i < sorts.size(); i++) {
        resultSort = resultSort.max(sorts.get(i));
      }
      return resultSort;
    }
  }

  private Sort generateUpperBound(List<Sort> sorts, Concrete.SourceNode sourceNode) {
    Sort resultSort = generateUniqueUpperBound(sorts);
    if (resultSort != null) {
      return resultSort;
    }

    if (isDumb()) {
      return Sort.UNKNOWN;
    }

    Sort sortResult = Sort.generateInferVars(getEquations(), false, sourceNode);
    for (Sort sort : sorts) {
      getEquations().addEquation(sort.getPLevel(), sortResult.getPLevel(), Equations.CMP.LE, sourceNode);
      getEquations().addEquation(sort.getHLevel(), sortResult.getHLevel(), Equations.CMP.LE, sourceNode);
    }
    return sortResult;
  }

  public ClassCallExpression fixClassExtSort(ClassCallExpression classCall, Concrete.SourceNode sourceNode) {
    Expression thisExpr = new ReferenceExpression(ExpressionFactory.parameter("this", classCall));
    Integer hLevel = classCall.getDefinition().getUseLevel(classCall.getImplementedHere());
    List<Sort> sorts = hLevel != null && hLevel == -1 ? null : new ArrayList<>();
    for (ClassField field : classCall.getDefinition().getFields()) {
      if (classCall.isImplemented(field)) continue;
      PiExpression fieldType = field.getType(classCall.getSortArgument());
      if (fieldType.getCodomain().isInstance(ErrorExpression.class)) continue;
      if (sorts != null) {
        sorts.add(getSortOfType(fieldType.applyExpression(thisExpr).normalize(NormalizeVisitor.Mode.WHNF), sourceNode));
      }
    }

    if (hLevel != null && sorts != null) {
      for (int i = 0; i < sorts.size(); i++) {
        sorts.set(i, new Sort(sorts.get(i).getPLevel(), new Level(hLevel)));
      }
    }

    return new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), classCall.getImplementedHere(), sorts == null ? Sort.PROP : generateUpperBound(sorts, sourceNode).subst(classCall.getSortArgument().toLevelSubstitution()), classCall.hasUniverses());
  }

  // Sigma

  protected DependentLink visitParameters(List<? extends Concrete.TypeParameter> parameters, ExpectedType expectedType, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(context)) {
      for (Concrete.TypeParameter arg : parameters) {
        Type result = checkType(arg.getType(), expectedType == null ? ExpectedType.OMEGA : expectedType, false);
        if (result == null) return null;

        if (arg instanceof Concrete.TelescopeParameter) {
          List<? extends Referable> referableList = arg.getReferableList();
          DependentLink link = ExpressionFactory.parameter(arg.getExplicit(), arg.getNames(), result);
          list.append(link);
          int i = 0;
          for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
            addBinding(referableList.get(i), link1);
          }
        } else {
          list.append(ExpressionFactory.parameter(arg.getExplicit(), (String) null, result));
        }

        Sort resultSort = null;
        if (expectedType instanceof Expression) {
          if (!isDumb()) {
            expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
          }
          UniverseExpression universe = ((Expression) expectedType).checkedCast(UniverseExpression.class);
          if (universe != null && universe.getSort().isProp()) {
            resultSort = Sort.PROP;
          }
        }
        resultSorts.add(resultSort == null ? result.getSortOfType() : resultSort);
      }
    }

    return list.getFirst();
  }

  @Override
  public TypecheckingResult visitSigma(Concrete.SigmaExpression expr, ExpectedType expectedType) {
    if (expr.getParameters().isEmpty()) {
      return checkResult(expectedType, new TypecheckingResult(new SigmaExpression(Sort.PROP, EmptyDependentLink.getInstance()), new UniverseExpression(Sort.PROP)), expr);
    }

    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());
    DependentLink args = visitParameters(expr.getParameters(), expectedType, sorts);
    if (args == null || !args.hasNext()) return null;
    Sort sort = generateUpperBound(sorts, expr);
    return checkResult(expectedType, new TypecheckingResult(new SigmaExpression(sort, args), new UniverseExpression(sort)), expr);
  }

  @Override
  public TypecheckingResult visitTuple(Concrete.TupleExpression expr, ExpectedType expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType instanceof Expression) {
      expectedTypeNorm = (Expression) expectedType;
      if (!isDumb()) {
        expectedTypeNorm = expectedTypeNorm.normalize(NormalizeVisitor.Mode.WHNF);
      }
      if (expectedTypeNorm.isInstance(SigmaExpression.class)) {
        SigmaExpression expectedTypeSigma = expectedTypeNorm.cast(SigmaExpression.class);
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = DependentLink.Helper.size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          errorReporter.report(new TypecheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr));
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        TypecheckingResult tupleResult = new TypecheckingResult(new TupleExpression(fields, expectedTypeSigma), (Expression) expectedType);
        ExprSubstitution substitution = new ExprSubstitution();
        for (Concrete.Expression field : expr.getFields()) {
          Expression expType = sigmaParams.getTypeExpr().subst(substitution);
          TypecheckingResult result = checkExpr(field, expType);
          if (result == null) return null;
          fields.add(result.expression);
          substitution.add(sigmaParams, result.expression);

          sigmaParams = sigmaParams.getNext();
        }
        return tupleResult;
      }

      if (isDumb() && expectedTypeNorm.isWHNF() != Decision.YES) {
        return null;
      }
    }

    List<Sort> sorts = new ArrayList<>(expr.getFields().size());
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    LinkList list = new LinkList();
    for (int i = 0; i < expr.getFields().size(); i++) {
      TypecheckingResult result = checkExpr(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      Sort sort = getSortOfType(result.type, expr);
      sorts.add(sort);
      list.append(ExpressionFactory.parameter(null, result.type instanceof Type ? (Type) result.type : new TypeExpression(result.type, sort)));
    }

    SigmaExpression type = new SigmaExpression(generateUpperBound(sorts, expr), list.getFirst());
    return checkResult(expectedTypeNorm, new TypecheckingResult(new TupleExpression(fields, type), type), expr);
  }

  @Override
  public TypecheckingResult visitProj(Concrete.ProjExpression expr, ExpectedType expectedType) {
    Concrete.Expression expr1 = expr.getExpression();
    TypecheckingResult exprResult = checkExpr(expr1, null);
    if (exprResult == null) return null;

    if (!isDumb()) {
      exprResult.type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    }
    if (!exprResult.type.isInstance(SigmaExpression.class)) {
      Expression stuck = exprResult.type.getCanonicalStuckExpression();
      if ((stuck == null || !stuck.isError()) && (!isDumb() || stuck != null && stuck.getInferenceVariable() == null && exprResult.type.isWHNF() == Decision.YES)) {
        errorReporter.report(new TypeMismatchError(DocFactory.text("A sigma type"), exprResult.type, expr1));
      }
      return null;
    }

    DependentLink sigmaParams = exprResult.type.cast(SigmaExpression.class).getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      errorReporter.report(new TypecheckingError("Index " + (expr.getField() + 1) + " is out of range", expr));
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, ProjExpression.make(exprResult.expression, i));
    }

    exprResult.expression = ProjExpression.make(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getTypeExpr().subst(substitution);
    return checkResult(expectedType, exprResult, expr);
  }

  // Other

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean compareExpressions(boolean isLeft, Expression expected, Expression actual, Concrete.Expression expr) {
    if (!CompareVisitor.compare(getEquations(), Equations.CMP.EQ, actual, expected, expr)) { // TODO
      errorReporter.report(new PathEndpointMismatchError(isLeft, expected, actual, expr));
      return false;
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkPath(TResult result, Concrete.Expression expr) {
    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
      errorReporter.report(new TypecheckingError("Expected an argument for 'path'", expr));
      return false;
    }
    if (result instanceof TypecheckingResult) {
      ConCallExpression conCall = ((TypecheckingResult) result).expression.checkedCast(ConCallExpression.class);
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON) {
        //noinspection RedundantIfStatement
        if (!compareExpressions(true, conCall.getDataTypeArguments().get(1), AppExpression.make(conCall.getDefCallArguments().get(0), ExpressionFactory.Left()), expr) ||
          !compareExpressions(false, conCall.getDataTypeArguments().get(2), AppExpression.make(conCall.getDefCallArguments().get(0), ExpressionFactory.Right()), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public TypecheckingResult visitApp(Concrete.AppExpression expr, ExpectedType expectedType) {
    TResult result = myArgsInference.infer(expr, expectedType);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public TypecheckingResult visitUniverse(Concrete.UniverseExpression expr, ExpectedType expectedType) {
    Level pLevel = expr.getPLevel() != null ? expr.getPLevel().accept(this, LevelVariable.PVAR) : null;
    Level hLevel = expr.getHLevel() != null ? expr.getHLevel().accept(this, LevelVariable.HVAR) : null;

    if (pLevel == null) {
      InferenceLevelVariable pl = isDumb() ? InferenceLevelVariable.UNKNOWN_PVAR : new InferenceLevelVariable(LevelVariable.LvlType.PLVL, true, expr);
      getEquations().addVariable(pl);
      pLevel = new Level(pl);
    }

    if (hLevel == null) {
      InferenceLevelVariable hl = isDumb() ? InferenceLevelVariable.UNKNOWN_HVAR : new InferenceLevelVariable(LevelVariable.LvlType.HLVL, true, expr);
      getEquations().addVariable(hl);
      hLevel = new Level(hl);
    }

    UniverseExpression universe = new UniverseExpression(new Sort(pLevel, hLevel));
    return checkResult(expectedType, new TypecheckingResult(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  @Override
  public TypecheckingResult visitTyped(Concrete.TypedExpression expr, ExpectedType expectedType) {
    Type type = checkType(expr.type, ExpectedType.OMEGA, false);
    if (type == null) {
      return checkExpr(expr.expression, expectedType);
    } else {
      return checkResult(expectedType, checkExpr(expr.expression, type.getExpr()), expr);
    }
  }

  @Override
  public TypecheckingResult visitNumericLiteral(Concrete.NumericLiteral expr, ExpectedType expectedType) {
    Expression resultExpr;
    BigInteger number = expr.getNumber();
    boolean isNegative = number.signum() < 0;
    try {
      int value = number.intValueExact();
      resultExpr = new SmallIntegerExpression(isNegative ? -value : value);
    } catch (ArithmeticException e) {
      resultExpr = new BigIntegerExpression(isNegative ? number.negate() : number);
    }

    TypecheckingResult result;
    if (isNegative) {
      result = new TypecheckingResult(ExpressionFactory.Neg(resultExpr), ExpressionFactory.Int());
    } else {
      result = new TypecheckingResult(resultExpr, ExpressionFactory.Nat());
    }
    return checkResult(expectedType, result, expr);
  }
}
