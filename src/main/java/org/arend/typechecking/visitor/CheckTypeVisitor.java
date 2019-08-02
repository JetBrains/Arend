package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.binding.inference.LambdaInferenceVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Clause;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.ReplaceBindingVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.DummyErrorReporter;
import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.error.IncorrectExpressionException;
import org.arend.error.doc.DocFactory;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.term.concrete.ConcreteLevelExpressionVisitor;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.ListLocalErrorReporter;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.LocalErrorReporterCounter;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.ImplicitArgsInference;
import org.arend.typechecking.implicitargs.StdImplicitArgsInference;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.implicitargs.equations.TwoStageEquations;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.patternmatching.ConditionsChecking;
import org.arend.typechecking.patternmatching.ElimTypechecking;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.typechecking.result.DefCallResult;
import org.arend.typechecking.result.TResult;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Pair;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

import static org.arend.typechecking.error.local.ArgInferenceError.expression;
import static org.arend.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements ConcreteExpressionVisitor<ExpectedType, TypecheckingResult>, ConcreteLevelExpressionVisitor<LevelVariable, Level> {
  private Set<Binding> myFreeBindings;
  private Definition.TypeCheckingStatus myStatus = Definition.TypeCheckingStatus.NO_ERRORS;
  private final Equations myEquations;
  private GlobalInstancePool myInstancePool;
  private final ImplicitArgsInference myArgsInference;
  protected final TypecheckerState state;
  protected Map<Referable, Binding> context;
  protected LocalErrorReporter errorReporter;

  private class MyErrorReporter implements LocalErrorReporter {
    private final LocalErrorReporter myErrorReporter;

    private MyErrorReporter(LocalErrorReporter errorReporter) {
      myErrorReporter = errorReporter;
    }

    private void setStatus(Error error) {
      myStatus = myStatus.max(error.level == Error.Level.ERROR ? Definition.TypeCheckingStatus.HAS_ERRORS : Definition.TypeCheckingStatus.HAS_WARNINGS);
    }

    @Override
    public void report(LocalError localError) {
      setStatus(localError);
      myErrorReporter.report(localError);
    }

    @Override
    public void report(GeneralError error) {
      setStatus(error);
      myErrorReporter.report(error);
    }
  }

  public CheckTypeVisitor(TypecheckerState state, Map<Referable, Binding> localContext, LocalErrorReporter errorReporter, GlobalInstancePool pool) {
    myFreeBindings = new HashSet<>();
    this.errorReporter = new MyErrorReporter(errorReporter);
    myEquations = new TwoStageEquations(this);
    myInstancePool = pool;
    myArgsInference = new StdImplicitArgsInference(this);
    this.state = state;
    context = localContext;
  }

  public Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal) {
    return isFinal ? finalCheckType(expr, expectedType) : checkType(expr, expectedType);
  }

  public void addBinding(@Nullable Referable referable, Binding binding) {
    if (referable == null) {
      myFreeBindings.add(binding);
    } else {
      context.put(referable, binding);
    }
  }

  public void setHasErrors() {
    myStatus = myStatus.max(Definition.TypeCheckingStatus.HAS_ERRORS);
  }

  public TypecheckerState getTypecheckingState() {
    return state;
  }

  public Definition getTypechecked(TCReferable referable) {
    return state.getTypechecked(referable);
  }

  public GlobalInstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(GlobalInstancePool pool) {
    myInstancePool = pool;
  }

  public Map<Referable, Binding> getContext() {
    return context;
  }

  public void setContext(Map<Referable, Binding> context) {
    this.context = context;
  }

  public Set<? extends Binding> getFreeBindings() {
    return myFreeBindings;
  }

  public void setFreeBindings(Set<Binding> freeBindings) {
    myFreeBindings = freeBindings;
  }

  public Set<Binding> getAllBindings() {
    Set<Binding> allBindings = new HashSet<>(context.values());
    allBindings.addAll(myFreeBindings);
    return allBindings;
  }

  public LocalErrorReporter getErrorReporter() {
    return errorReporter;
  }

  public Equations getEquations() {
    return myEquations;
  }

  public Definition.TypeCheckingStatus getStatus() {
    return myStatus;
  }

  private TypecheckingResult checkResult(ExpectedType expectedType, TypecheckingResult result, Concrete.Expression expr) {
    if (result == null || expectedType == null || expectedType == ExpectedType.OMEGA && result.type instanceof UniverseExpression || result.type == expectedType) {
      return result;
    }

    CompareVisitor cmpVisitor = new CompareVisitor(DummyEquations.getInstance(), Equations.CMP.LE, expr);
    if (expectedType instanceof Expression && cmpVisitor.nonNormalizingCompare(result.type, (Expression) expectedType)) {
      return result;
    }

    result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
    TypecheckingResult coercedResult = CoerceData.coerce(result, expectedType, expr, this);
    if (coercedResult != null) {
      return coercedResult;
    }

    return expectedType instanceof Expression ? checkResultExpr((Expression) expectedType, result, expr) : result;
  }

  private TypecheckingResult checkResultExpr(Expression expectedType, TypecheckingResult result, Concrete.Expression expr) {
    if (new CompareVisitor(myEquations, Equations.CMP.LE, expr).normalizedCompare(result.type, expectedType)) {
      result.expression = OfTypeExpression.make(result.expression, result.type, expectedType);
      return result;
    }

    if (!result.type.isError()) {
      errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }
    return null;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean checkNormalizedResult(ExpectedType expectedType, TypecheckingResult result, Concrete.Expression expr, boolean strict) {
    if (expectedType instanceof Expression && new CompareVisitor(strict ? DummyEquations.getInstance() : myEquations, Equations.CMP.LE, expr).normalizedCompare(result.type, (Expression) expectedType) || expectedType == ExpectedType.OMEGA && result.type.isInstance(UniverseExpression.class)) {
      if (!strict && expectedType instanceof Expression) {
        result.expression = OfTypeExpression.make(result.expression, result.type, (Expression) expectedType);
      }
      return true;
    }

    if (!strict && !result.type.isError()) {
      errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }

    return false;
  }

  private TypecheckingResult tResultToResult(ExpectedType expectedType, TResult result, Concrete.Expression expr) {
    if (result != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(this), expr);
  }

  public TypecheckingResult checkExpr(Concrete.Expression expr, ExpectedType expectedType) {
    if (expr == null) {
      assert false;
      errorReporter.report(new LocalError(Error.Level.ERROR, "Incomplete expression"));
      return null;
    }

    try {
      return expr.accept(this, expectedType);
    } catch (IncorrectExpressionException e) {
      errorReporter.report(new TypecheckingError(e.getMessage(), expr));
      return null;
    }
  }

  public TypecheckingResult finalCheckExpr(Concrete.Expression expr, ExpectedType expectedType, boolean returnExpectedType) {
    return finalize(checkExpr(expr, expectedType), returnExpectedType && expectedType instanceof Expression ? (Expression) expectedType : null, expr);
  }

  public TypecheckingResult finalize(TypecheckingResult result, Expression expectedType, Concrete.SourceNode sourceNode) {
    if (result == null) {
      if (expectedType == null) {
        return null;
      }
      result = new TypecheckingResult(null, expectedType);
    } else {
      if (expectedType != null && !result.type.isInstance(ClassCallExpression.class)) { // Use the inferred type if it is a class call
        result.type = expectedType;
      }
    }

    LevelSubstitution substitution = myEquations.solve(sourceNode);
    if (!substitution.isEmpty()) {
      if (result.expression != null) {
        result.expression = result.expression.subst(substitution);
      }
      result.type = result.type.subst(new ExprSubstitution(), substitution);
    }

    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(Error.Level.ERROR, errorReporter);
    if (result.expression != null) {
      result.expression = result.expression.strip(counter);
    }
    result.type = result.type.strip(counter.getErrorsNumber() == 0 ? errorReporter : DummyErrorReporter.INSTANCE);
    return result;
  }

  public Type checkType(Concrete.Expression expr, ExpectedType expectedType) {
    if (expr == null) {
      assert false;
      errorReporter.report(new LocalError(Error.Level.ERROR, "Incomplete expression"));
      return null;
    }

    TypecheckingResult result;
    try {
      ExpectedType expectedType1 = expectedType;
      if (expectedType instanceof Expression) {
        expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
        if (((Expression) expectedType).getStuckInferenceVariable() != null) {
          expectedType1 = ExpectedType.OMEGA;
        }
      }

      result = expr.accept(this, expectedType1);
      if (result != null && expectedType1 != expectedType) {
        result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
        result = checkResultExpr((Expression) expectedType, result, expr);
      }
    } catch (IncorrectExpressionException e) {
      errorReporter.report(new TypecheckingError(e.getMessage(), expr));
      return null;
    }
    if (result == null) {
      return null;
    }
    if (result.expression instanceof Type) {
      return (Type) result.expression;
    }

    Expression type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    UniverseExpression universe = type.checkedCast(UniverseExpression.class);
    if (universe == null) {
      Expression stuck = type.getCanonicalStuckExpression();
      if (stuck == null || !stuck.isInstance(InferenceReferenceExpression.class) && !stuck.isError()) {
        if (stuck == null || !stuck.isError()) {
          errorReporter.report(new TypeMismatchError(DocFactory.text("a universe"), type, expr));
        }
        return null;
      }

      universe = new UniverseExpression(Sort.generateInferVars(myEquations, false, expr));
      InferenceVariable infVar = stuck.getInferenceVariable();
      if (infVar != null) {
        myEquations.addEquation(type, universe, Equations.CMP.LE, expr, infVar, null);
      }
    }

    return new TypeExpression(result.expression, universe.getSort());
  }

  private Type finalCheckType(Concrete.Expression expr, ExpectedType expectedType) {
    Type result = checkType(expr, expectedType);
    if (result == null) return null;
    return result.subst(new SubstVisitor(new ExprSubstitution(), myEquations.solve(expr))).strip(errorReporter);
  }

  public TypecheckingResult checkArgument(Concrete.Expression expr, ExpectedType expectedType, TResult result) {
    return expr instanceof Concrete.ThisExpression && result instanceof DefCallResult && ((DefCallResult) result).getDefinition().isGoodParameter(((DefCallResult) result).getArguments().size())
      ? tResultToResult(expectedType, getLocalVar(((Concrete.ThisExpression) expr).getReferent(), expr), expr)
      : checkExpr(expr, expectedType);
  }

  // Classes

  @Override
  public TypecheckingResult visitClassExt(Concrete.ClassExtExpression expr, ExpectedType expectedType) {
    Concrete.Expression baseClassExpr = expr.getBaseClassExpression();
    TypecheckingResult typeCheckedBaseClass = checkExpr(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }

    ClassCallExpression classCall = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class);
    if (classCall == null) {
      errorReporter.report(new TypecheckingError("Expected a class", baseClassExpr));
      return null;
    }

    return typecheckClassExt(expr.getStatements(), expectedType, null, classCall, null, expr);
  }

  public TypecheckingResult typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, ExpectedType expectedType, Expression implExpr, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr) {
    ClassDefinition baseClass = classCallExpr.getDefinition();
    Map<ClassField, Expression> fieldSet = new HashMap<>(classCallExpr.getImplementedHere());
    ClassCallExpression resultClassCall = new ClassCallExpression(baseClass, classCallExpr.getSortArgument(), fieldSet, Sort.PROP, baseClass.hasUniverses());

    Set<ClassField> defined = implExpr == null ? null : new HashSet<>();
    List<Pair<Definition,Concrete.ClassFieldImpl>> implementations = new ArrayList<>(classFieldImpls.size());
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      Definition definition = referableToDefinition(classFieldImpl.getImplementedField(), classFieldImpl);
      if (definition != null) {
        implementations.add(new Pair<>(definition,classFieldImpl));
        if (defined != null) {
          if (definition instanceof ClassField) {
            defined.add((ClassField) definition);
          } else if (definition instanceof ClassDefinition) {
            defined.addAll(((ClassDefinition) definition).getFields());
          }
        }
      }
    }

    if (defined != null) {
      for (ClassField field : baseClass.getFields()) {
        if (!defined.contains(field) && !resultClassCall.isImplemented(field)) {
          Definition found = FindDefCallVisitor.findDefinition(field.getType(Sort.STD).getCodomain(), defined);
          if (found != null) {
            Concrete.SourceNode sourceNode = null;
            for (Pair<Definition, Concrete.ClassFieldImpl> implementation : implementations) {
              if (implementation.proj1 == found) {
                sourceNode = implementation.proj2;
              }
            }
            if (sourceNode == null) {
              sourceNode = expr;
            }
            errorReporter.report(new TypecheckingError("Field '" + field.getName() + "' depends on '" + found.getName() + "', but is not implemented", sourceNode));
            return null;
          }
          fieldSet.put(field, FieldCallExpression.make(field, classCallExpr.getSortArgument(), implExpr));
        }
      }
    }

    for (Pair<Definition,Concrete.ClassFieldImpl> pair : implementations) {
      if (pair.proj1 instanceof ClassField) {
        ClassField field = (ClassField) pair.proj1;
        Expression impl = typecheckImplementation(field, pair.proj2.implementation, resultClassCall);
        if (impl != null) {
          Expression oldImpl = null;
          if (!field.isProperty()) {
            oldImpl = resultClassCall.getImplementationHere(field);
            if (oldImpl == null) {
              LamExpression lamImpl = resultClassCall.getDefinition().getImplementation(field);
              oldImpl = lamImpl == null ? null : lamImpl.getBody();
            }
          }
          if (oldImpl != null) {
            if (!classCallExpr.isImplemented(field) || !CompareVisitor.compare(myEquations, Equations.CMP.EQ, impl, oldImpl, pair.proj2.implementation)) {
              errorReporter.report(new FieldsImplementationError(true, baseClass.getReferable(), Collections.singletonList(field.getReferable()), pair.proj2));
            }
          } else if (!resultClassCall.isImplemented(field)) {
            fieldSet.put(field, impl);
          }
        } else if (pseudoImplemented != null) {
          pseudoImplemented.add(field);
        } else if (!resultClassCall.isImplemented(field)) {
          fieldSet.put(field, new ErrorExpression(null, null));
        }
      } else if (pair.proj1 instanceof ClassDefinition) {
        TypecheckingResult result = checkExpr(pair.proj2.implementation, null);
        if (result != null) {
          Expression type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
          ClassCallExpression classCall = type.checkedCast(ClassCallExpression.class);
          if (classCall == null) {
            if (!type.isInstance(ErrorExpression.class)) {
              InferenceVariable var = type instanceof InferenceReferenceExpression ? ((InferenceReferenceExpression) type).getVariable() : null;
              errorReporter.report(var == null ? new TypeMismatchError(DocFactory.text("a class"), type, pair.proj2.implementation) : var.getErrorInfer());
            }
          } else {
            if (!classCall.getDefinition().isSubClassOf((ClassDefinition) pair.proj1)) {
              errorReporter.report(new TypeMismatchError(new ClassCallExpression((ClassDefinition) pair.proj1, Sort.PROP), type, pair.proj2.implementation));
            } else {
              for (ClassField field : ((ClassDefinition) pair.proj1).getFields()) {
                Expression impl = FieldCallExpression.make(field, classCall.getSortArgument(), result.expression);
                Expression oldImpl = field.isProperty() ? null : resultClassCall.getImplementation(field, result.expression);
                if (oldImpl != null) {
                  if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, impl, oldImpl, pair.proj2.implementation)) {
                    errorReporter.report(new FieldsImplementationError(true, baseClass.getReferable(), Collections.singletonList(field.getReferable()), pair.proj2));
                  }
                } else if (!resultClassCall.isImplemented(field)) {
                  fieldSet.put(field, impl);
                }
              }
            }
          }
        }
      } else {
        errorReporter.report(new WrongReferable("Expected either a field or a class", pair.proj2.getImplementedField(), pair.proj2));
      }
    }

    resultClassCall = fixClassExtSort(resultClassCall, expr);
    resultClassCall.updateHasUniverses();
    return checkResult(expectedType, new TypecheckingResult(resultClassCall, new UniverseExpression(resultClassCall.getSort())), expr);
  }

  private Expression typecheckImplementation(ClassField field, Concrete.Expression implBody, ClassCallExpression fieldSetClass) {
    PiExpression piType = field.getType(fieldSetClass.getSortArgument());
    ReplaceBindingVisitor visitor = new ReplaceBindingVisitor(piType.getParameters(), fieldSetClass);
    Expression type = piType.getCodomain().accept(visitor, null);
    if (!visitor.isOK()) {
      errorReporter.report(new TypecheckingError("The type of '" + field.getName() + "' depends non-trivially on \\this parameter", implBody));
      return null;
    }

    if (implBody instanceof Concrete.HoleExpression && field.getReferable().isParameterField() && !field.getReferable().isExplicitField() && field.isTypeClass() && type instanceof ClassCallExpression && !((ClassCallExpression) type).getDefinition().isRecord()) {
      ClassDefinition classDef = ((ClassCallExpression) type).getDefinition();
      if (classDef.getClassifyingField() == null) {
        Expression instance = myInstancePool.getInstance(null, classDef.getReferable(), myEquations, implBody);
        if (instance == null) {
          ArgInferenceError error = new InstanceInferenceError(classDef.getReferable(), implBody, new Expression[0]);
          errorReporter.report(error);
          return new ErrorExpression(null, error);
        } else {
          return instance;
        }
      } else {
        return new InferenceReferenceExpression(new TypeClassInferenceVariable(field.getName(), type, classDef.getReferable(), false, implBody, getAllBindings()), myEquations);
      }
    }

    TypecheckingResult result = implBody instanceof Concrete.ThisExpression && fieldSetClass.getDefinition().isGoodField(field)
      ? tResultToResult(type, getLocalVar(((Concrete.ThisExpression) implBody).getReferent(), implBody), implBody)
      : checkExpr(implBody, type);
    return result == null ? null : result.expression;
  }

  @Override
  public TypecheckingResult visitNew(Concrete.NewExpression expr, ExpectedType expectedType) {
    TypecheckingResult exprResult = null;
    Set<ClassField> pseudoImplemented = Collections.emptySet();
    if (expr.getExpression() instanceof Concrete.ClassExtExpression || expr.getExpression() instanceof Concrete.ReferenceExpression) {
      if (expectedType != null) {
        expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      }
      Concrete.Expression baseExpr = expr.getExpression() instanceof Concrete.ClassExtExpression ? ((Concrete.ClassExtExpression) expr.getExpression()).getBaseClassExpression() : expr.getExpression();
      if (baseExpr instanceof Concrete.HoleExpression || baseExpr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) baseExpr).getReferent() instanceof ClassReferable && expectedType instanceof ClassCallExpression) {
        ClassCallExpression actualClassCall = null;
        if (baseExpr instanceof Concrete.HoleExpression && !(expectedType instanceof ClassCallExpression)) {
          errorReporter.report(new TypecheckingError("Cannot infer an expression", baseExpr));
          return null;
        }

        ClassCallExpression expectedClassCall = (ClassCallExpression) expectedType;
        if (baseExpr instanceof Concrete.ReferenceExpression) {
          Concrete.ReferenceExpression baseRefExpr = (Concrete.ReferenceExpression) baseExpr;
          Referable ref = baseRefExpr.getReferent();
          boolean ok = ref instanceof TCReferable;
          if (ok) {
            Definition actualDef = state.getTypechecked((TCReferable) ref);
            if (actualDef instanceof ClassDefinition) {
              ok = ((ClassDefinition) actualDef).isSubClassOf(expectedClassCall.getDefinition());
              if (ok && (actualDef != expectedClassCall.getDefinition() || baseRefExpr.getPLevel() != null || baseRefExpr.getHLevel() != null)) {
                boolean fieldsOK = true;
                for (ClassField implField : expectedClassCall.getImplementedHere().keySet()) {
                  if (((ClassDefinition) actualDef).isImplemented(implField)) {
                    fieldsOK = false;
                    break;
                  }
                }
                Level pLevel = baseRefExpr.getPLevel() == null ? null : baseRefExpr.getPLevel().accept(this, LevelVariable.PVAR);
                Level hLevel = baseRefExpr.getHLevel() == null ? null : baseRefExpr.getHLevel().accept(this, LevelVariable.HVAR);
                Sort expectedSort = expectedClassCall.getSortArgument();
                actualClassCall = new ClassCallExpression((ClassDefinition) actualDef, pLevel == null && hLevel == null ? expectedSort : new Sort(pLevel == null ? expectedSort.getPLevel() : pLevel, hLevel == null ? expectedSort.getHLevel() : hLevel), fieldsOK ? expectedClassCall.getImplementedHere() : Collections.emptyMap(), expectedClassCall.getSort(), actualDef.hasUniverses());
              }
            } else {
              ok = false;
            }
          }
          if (!ok) {
            errorReporter.report(new TypeMismatchError(expectedType, baseExpr, baseExpr));
            return null;
          }
        }

        if (actualClassCall != null) {
          expectedClassCall = actualClassCall;
          expectedClassCall.updateHasUniverses();
        }
        pseudoImplemented = new HashSet<>();
        exprResult = typecheckClassExt(expr.getExpression() instanceof Concrete.ClassExtExpression ? ((Concrete.ClassExtExpression) expr.getExpression()).getStatements() : Collections.emptyList(), null, null, expectedClassCall, pseudoImplemented, expr.getExpression());
        if (exprResult == null) {
          return null;
        }
      }
    }

    if (exprResult == null) {
      Concrete.Expression baseClassExpr;
      List<Concrete.ClassFieldImpl> classFieldImpls;
      if (expr.getExpression() instanceof Concrete.ClassExtExpression) {
        baseClassExpr = ((Concrete.ClassExtExpression) expr.getExpression()).getBaseClassExpression();
        classFieldImpls = ((Concrete.ClassExtExpression) expr.getExpression()).getStatements();
      } else {
        baseClassExpr = expr.getExpression();
        classFieldImpls = Collections.emptyList();
      }

      TypecheckingResult typeCheckedBaseClass = checkExpr(baseClassExpr, null);
      if (typeCheckedBaseClass == null) {
        return null;
      }

      typeCheckedBaseClass.expression = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF);
      Expression implExpr = null;
      ClassCallExpression classCall = typeCheckedBaseClass.expression.checkedCast(ClassCallExpression.class);
      if (classCall == null) {
        classCall = typeCheckedBaseClass.type.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class);
        if (classCall == null) {
          errorReporter.report(new TypecheckingError("Expected a class or a class instance", baseClassExpr));
          return null;
        }
        implExpr = typeCheckedBaseClass.expression;
      }

      exprResult = typecheckClassExt(classFieldImpls, null, implExpr, classCall, null, baseClassExpr);
      if (exprResult == null) {
        return null;
      }
    }

    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.checkedCast(ClassCallExpression.class);
    if (classCallExpr == null) {
      TypecheckingError error = new TypecheckingError("Expected a class", expr.getExpression());
      errorReporter.report(error);
      return new TypecheckingResult(new ErrorExpression(null, error), normExpr);
    }

    if (checkAllImplemented(classCallExpr, pseudoImplemented, expr)) {
      return checkResult(expectedType, new TypecheckingResult(new NewExpression(classCallExpr), normExpr), expr);
    } else {
      return null;
    }
  }

  public boolean checkAllImplemented(ClassCallExpression classCall, Set<ClassField> pseudoImplemented, Concrete.SourceNode sourceNode) {
    int notImplemented = classCall.getDefinition().getNumberOfNotImplementedFields() - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else {
      List<FieldReferable> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field) && !pseudoImplemented.contains(field)) {
          fields.add(field.getReferable());
        }
      }
      if (!fields.isEmpty()) {
        errorReporter.report(new FieldsImplementationError(false, classCall.getDefinition().getReferable(), fields, sourceNode));
      }
      return false;
    }
  }

  // Variables

  private Definition referableToDefinition(Referable referable, Concrete.SourceNode sourceNode) {
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
      sortArgument = isMin ? Sort.PROP : Sort.generateInferVars(getEquations(), definition.hasUniverses(), expr);
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
          InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, definition.hasUniverses(), expr);
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
          InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, definition.hasUniverses(), expr);
          getEquations().addVariable(hl);
          hLevel = new Level(hl);
        }
      }

      sortArgument = new Sort(pLevel, hLevel);
    }

    return DefCallResult.makeTResult(expr, definition, sortArgument);
  }

  private TResult getLocalVar(Referable ref, Concrete.SourceNode sourceNode) {
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
    Expression type = expr.getType(true);
    Sort sort = type == null ? null : type.toSort();
    if (sort == null) {
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

  // Parameters

  private TypedSingleDependentLink visitNameParameter(Concrete.NameParameter param, int argIndex, Concrete.SourceNode sourceNode) {
    Referable referable = param.getReferable();
    String name = referable == null ? null : referable.textRepresentation();
    Sort sort = Sort.generateInferVars(myEquations, false, sourceNode);
    InferenceVariable inferenceVariable = new LambdaInferenceVariable(name == null ? "_" : "type-of-" + name, new UniverseExpression(sort), argIndex, sourceNode, false, getAllBindings());
    Expression argType = new InferenceReferenceExpression(inferenceVariable, myEquations);

    TypedSingleDependentLink link = new TypedSingleDependentLink(param.isExplicit(), name, new TypeExpression(argType, sort));
    if (referable != null) {
      context.put(referable, link);
    } else {
      myFreeBindings.add(link);
    }
    return link;
  }

  private SingleDependentLink visitTypeParameter(Concrete.TypeParameter param, List<Sort> sorts, Type expectedType) {
    Type argResult = checkType(param.getType(), ExpectedType.OMEGA);
    if (argResult == null) return null;
    if (expectedType != null) {
      Expression expected = expectedType.getExpr().normalize(NormalizeVisitor.Mode.WHNF);
      if (expected.isInstance(ClassCallExpression.class) ||
        expected.isInstance(PiExpression.class) ||
        expected.isInstance(SigmaExpression.class) ||
        expected.isInstance(UniverseExpression.class)) {
        if (expected.isLessOrEquals(argResult.getExpr(), myEquations, param)) {
          argResult = expectedType;
        }
      }
    }
    if (sorts != null) {
      sorts.add(argResult.getSortOfType());
    }

    if (param instanceof Concrete.TelescopeParameter) {
      List<? extends Referable> referableList = param.getReferableList();
      SingleDependentLink link = ExpressionFactory.singleParams(param.isExplicit(), param.getNames(), argResult);
      int i = 0;
      for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
        if (referableList.get(i) != null) {
          context.put(referableList.get(i), link1);
        } else {
          myFreeBindings.add(link1);
        }
      }
      return link;
    } else {
      return new TypedSingleDependentLink(param.isExplicit(), null, argResult);
    }
  }

  protected DependentLink visitParameters(List<? extends Concrete.TypeParameter> parameters, ExpectedType expectedType, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(context)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        for (Concrete.TypeParameter arg : parameters) {
          Type result = checkType(arg.getType(), expectedType == null ? ExpectedType.OMEGA : expectedType, false);
          if (result == null) return null;

          if (arg instanceof Concrete.TelescopeParameter) {
            List<? extends Referable> referableList = arg.getReferableList();
            DependentLink link = ExpressionFactory.parameter(arg.isExplicit(), arg.getNames(), result);
            list.append(link);
            int i = 0;
            for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
              addBinding(referableList.get(i), link1);
            }
          } else {
            list.append(ExpressionFactory.parameter(arg.isExplicit(), (String) null, result));
          }

          Sort resultSort = null;
          if (expectedType instanceof Expression) {
            expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
            UniverseExpression universe = ((Expression) expectedType).checkedCast(UniverseExpression.class);
            if (universe != null && universe.getSort().isProp()) {
              resultSort = Sort.PROP;
            }
          }
          resultSorts.add(resultSort == null ? result.getSortOfType() : resultSort);
        }
      }
    }

    return list.getFirst();
  }

  // Pi

  private TypecheckingResult bodyToLam(SingleDependentLink params, TypecheckingResult bodyResult, Concrete.SourceNode sourceNode) {
    if (bodyResult == null) {
      return null;
    }
    Sort sort = PiExpression.generateUpperBound(params.getType().getSortOfType(), getSortOfType(bodyResult.type, sourceNode), myEquations, sourceNode);
    return new TypecheckingResult(new LamExpression(sort, params, bodyResult.expression), new PiExpression(sort, params, bodyResult.type));
  }

  private TypecheckingResult visitLam(List<? extends Concrete.Parameter> parameters, Concrete.LamExpression expr, Expression expectedType, int argIndex) {
    if (parameters.isEmpty()) {
      return checkExpr(expr.getBody(), expectedType);
    }

    Concrete.Parameter param = parameters.get(0);
    if (expectedType != null) {
      expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      if (param.isExplicit() && expectedType.isInstance(PiExpression.class) && !expectedType.cast(PiExpression.class).getParameters().isExplicit()) {
        PiExpression piExpectedType = expectedType.cast(PiExpression.class);
        SingleDependentLink piParams = piExpectedType.getParameters();
        for (SingleDependentLink link = piParams; link.hasNext(); link = link.getNext()) {
          myFreeBindings.add(link);
        }
        return bodyToLam(piParams, visitLam(parameters, expr, piExpectedType.getCodomain(), argIndex + DependentLink.Helper.size(piParams)), expr);
      }
    }

    if (param instanceof Concrete.NameParameter) {
      if (expectedType == null || !expectedType.isInstance(PiExpression.class)) {
        TypedSingleDependentLink link = visitNameParameter((Concrete.NameParameter) param, argIndex, expr);
        TypecheckingResult bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, null, argIndex + 1);
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOfType(bodyResult.type, expr), myEquations, expr);
        TypecheckingResult result = new TypecheckingResult(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
        if (expectedType != null && checkResult(expectedType, result, expr) == null) {
          return null;
        }
        return result;
      } else {
        PiExpression piExpectedType = expectedType.cast(PiExpression.class);
        Referable referable = ((Concrete.NameParameter) param).getReferable();
        SingleDependentLink piParams = piExpectedType.getParameters();
        if (piParams.isExplicit() && !param.isExplicit()) {
          errorReporter.report(new TypecheckingError(ordinal(argIndex) + " argument of the lambda is implicit, but the first parameter of the expected type is not", expr));
        }

        Type paramType = piParams.getType();
        DefCallExpression defCallParamType = paramType.getExpr().checkedCast(DefCallExpression.class);
        if (defCallParamType != null && !defCallParamType.hasUniverses()) { // fixes test pLevelTest
          if (defCallParamType.getDefinition() instanceof DataDefinition) {
            paramType = new DataCallExpression((DataDefinition) defCallParamType.getDefinition(), Sort.generateInferVars(myEquations, false, param), new ArrayList<>(defCallParamType.getDefCallArguments()));
          } else if (defCallParamType.getDefinition() instanceof FunctionDefinition) {
            paramType = new TypeExpression(new FunCallExpression((FunctionDefinition) defCallParamType.getDefinition(), Sort.generateInferVars(myEquations, false, param), new ArrayList<>(defCallParamType.getDefCallArguments())), paramType.getSortOfType());
          }
        }

        SingleDependentLink link = new TypedSingleDependentLink(piParams.isExplicit(), referable == null ? null : referable.textRepresentation(), paramType);
        if (referable != null) {
          context.put(referable, link);
        } else {
          myFreeBindings.add(link);
        }
        Expression codomain = piExpectedType.getCodomain().subst(piParams, new ReferenceExpression(link));
        return bodyToLam(link, visitLam(parameters.subList(1, parameters.size()), expr, piParams.getNext().hasNext() ? new PiExpression(piExpectedType.getResultSort(), piParams.getNext(), codomain) : codomain, argIndex + 1), expr);
      }
    } else if (param instanceof Concrete.TypeParameter) {
      PiExpression piExpectedType = expectedType == null ? null : expectedType.checkedCast(PiExpression.class);
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param, null, piExpectedType == null || piExpectedType.getParameters().isExplicit() != param.isExplicit() ? null : piExpectedType.getParameters().getType());
      if (link == null) {
        return null;
      }

      SingleDependentLink actualLink = null;
      Expression expectedBodyType = null;
      int namesCount = param.getNumberOfParameters();
      if (expectedType != null) {
        Concrete.Expression paramType = param.getType();
        Expression argType = link.getTypeExpr();

        SingleDependentLink lamLink = link;
        ExprSubstitution substitution = new ExprSubstitution();
        Expression argExpr = null;
        int checked = 0;
        while (true) {
          if (!expectedType.isInstance(PiExpression.class)) {
            actualLink = link;
            for (int i = 0; i < checked; i++) {
              actualLink = actualLink.getNext();
            }
            expectedType = expectedType.subst(substitution);
            break;
          }
          if (argExpr == null) {
            argExpr = argType;
          }

          piExpectedType = expectedType.cast(PiExpression.class);
          Expression argExpectedType = piExpectedType.getParameters().getTypeExpr().subst(substitution);
          if (piExpectedType.getParameters().isExplicit() && !param.isExplicit()) {
            errorReporter.report(new TypecheckingError(ordinal(argIndex) + " argument of the lambda is implicit, but the first parameter of the expected type is not", expr));
          }
          if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, argExpr, argExpectedType, paramType)) {
            if (!argType.isError()) {
              errorReporter.report(new TypeMismatchError("Type mismatch in an argument of the lambda", argExpectedType, argType, paramType));
            }
            return null;
          }

          int parametersCount = 0;
          for (DependentLink link1 = piExpectedType.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
            parametersCount++;
            if (lamLink.hasNext()) {
              substitution.add(link1, new ReferenceExpression(lamLink));
              lamLink = lamLink.getNext();
            }
          }

          checked += parametersCount;
          if (checked >= namesCount) {
            if (checked == namesCount) {
              expectedBodyType = piExpectedType.getCodomain().subst(substitution);
            } else {
              int skip = parametersCount - (checked - namesCount);
              SingleDependentLink link1 = piExpectedType.getParameters();
              for (int i = 0; i < skip; i++) {
                link1 = link1.getNext();
              }
              expectedBodyType = new PiExpression(piExpectedType.getResultSort(), link1, piExpectedType.getCodomain()).subst(substitution);
            }
            break;
          }
          expectedType = piExpectedType.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }
      }

      TypecheckingResult bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, expectedBodyType, argIndex + namesCount);
      if (bodyResult == null) return null;
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOfType(bodyResult.type, expr), myEquations, expr);
      if (actualLink != null) {
        if (checkResult(expectedType, new TypecheckingResult(null, new PiExpression(sort, actualLink, bodyResult.type)), expr) == null) {
          return null;
        }
      }

      return new TypecheckingResult(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public TypecheckingResult visitLam(Concrete.LamExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(context)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        TypecheckingResult result = visitLam(expr.getParameters(), expr, expectedType instanceof Expression ? (Expression) expectedType : null, 1);
        if (result != null && expectedType != null && !(expectedType instanceof Expression)) {
          if (!result.type.isError()) {
            errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
          }
          return null;
        }
        return result;
      }
    }
  }

  @Override
  public TypecheckingResult visitPi(Concrete.PiExpression expr, ExpectedType expectedType) {
    List<SingleDependentLink> list = new ArrayList<>();
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(context)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        for (Concrete.TypeParameter arg : expr.getParameters()) {
          SingleDependentLink link = visitTypeParameter(arg, sorts, null);
          if (link == null) {
            return null;
          }
          list.add(link);
        }

        Type result = checkType(expr.getCodomain(), ExpectedType.OMEGA);
        if (result == null) return null;
        Sort codSort = result.getSortOfType();

        Expression piExpr = result.getExpr();
        for (int i = list.size() - 1; i >= 0; i--) {
          codSort = PiExpression.generateUpperBound(sorts.get(i), codSort, myEquations, expr);
          piExpr = new PiExpression(codSort, list.get(i), piExpr);
        }

        return checkResult(expectedType, new TypecheckingResult(piExpr, new UniverseExpression(codSort)), expr);
      }
    }
  }

  // Sigma

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
      expectedTypeNorm = ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF);
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

    exprResult.type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (!exprResult.type.isInstance(SigmaExpression.class)) {
      Expression stuck = exprResult.type.getCanonicalStuckExpression();
      if (stuck == null || !stuck.isError()) {
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

  // Let

  private TypecheckingResult typecheckLetClause(List<? extends Concrete.Parameter> parameters, Concrete.LetClause letClause, int argIndex) {
    if (parameters.isEmpty()) {
      Concrete.Expression letResult = letClause.getResultType();
      if (letResult != null) {
        Type type = checkType(letResult, ExpectedType.OMEGA);
        if (type == null) {
          return null;
        }

        TypecheckingResult result = checkExpr(letClause.getTerm(), type.getExpr());
        if (result == null) {
          return new TypecheckingResult(new ErrorExpression(type.getExpr(), null), type.getExpr());
        }
        if (result.expression.isInstance(ErrorExpression.class)) {
          result.expression = new ErrorExpression(type.getExpr(), result.expression.cast(ErrorExpression.class).getError());
        }
        return new TypecheckingResult(result.expression, type.getExpr());
      } else {
        return checkExpr(letClause.getTerm(), null);
      }
    }

    Concrete.Parameter param = parameters.get(0);
    if (param instanceof Concrete.NameParameter) {
      return bodyToLam(visitNameParameter((Concrete.NameParameter) param, argIndex, letClause), typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + 1), letClause);
    } else if (param instanceof Concrete.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param, null, null);
      return link == null ? null : bodyToLam(link, typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + param.getNumberOfParameters()), letClause);
    } else {
      throw new IllegalStateException();
    }
  }

  private void getLetClauseName(Concrete.LetClausePattern pattern, StringBuilder builder) {
    if (pattern.getReferable() != null) {
      builder.append(pattern.getReferable().textRepresentation());
    } else {
      boolean first = true;
      for (Concrete.LetClausePattern subPattern : pattern.getPatterns()) {
        if (first) {
          first = false;
        } else {
          builder.append('_');
        }
        getLetClauseName(subPattern, builder);
      }
    }
  }

  private Pair<LetClause,Expression> typecheckLetClause(Concrete.LetClause clause) {
    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(context)) {
      try (Utils.SetContextSaver ignore1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        TypecheckingResult result = typecheckLetClause(clause.getParameters(), clause, 1);
        if (result == null) {
          return null;
        }
        StringBuilder builder = new StringBuilder();
        getLetClauseName(clause.getPattern(), builder);
        return new Pair<>(new LetClause(builder.toString(), null, result.expression), result.type);
      }
    }
  }

  private LetClausePattern typecheckLetClausePattern(Concrete.LetClausePattern pattern, Expression expression, Expression type) {
    if (pattern.getReferable() != null) {
      if (pattern.type != null) {
        Type typeResult = checkType(pattern.type, ExpectedType.OMEGA);
        if (typeResult != null && !type.isLessOrEquals(typeResult.getExpr(), myEquations, pattern.type)) {
          errorReporter.report(new TypeMismatchError(typeResult.getExpr(), type, pattern.type));
        }
      }

      String name = pattern.getReferable().textRepresentation();
      context.put(pattern.getReferable(), new TypedEvaluatingBinding(name, expression, type));
      return new NameLetClausePattern(name);
    }

    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    SigmaExpression sigma = type.checkedCast(SigmaExpression.class);
    ClassCallExpression classCall = type.checkedCast(ClassCallExpression.class);
    List<ClassField> notImplementedFields = classCall == null ? null : classCall.getNotImplementedFields();
    int numberOfPatterns = pattern.getPatterns().size();
    if (sigma == null && classCall == null || sigma != null && DependentLink.Helper.size(sigma.getParameters()) != numberOfPatterns || notImplementedFields != null && notImplementedFields.size() != numberOfPatterns) {
      errorReporter.report(new TypeMismatchError("Cannot match an expression with the pattern", DocFactory.text(sigma == null && classCall == null ? "A sigma type or a record" : sigma != null ? "A sigma type with " + numberOfPatterns + " fields" : "A records with " + numberOfPatterns + " not implemented fields"), type, pattern));
      return null;
    }

    List<LetClausePattern> patterns = new ArrayList<>();
    DependentLink link = sigma == null ? null : sigma.getParameters();
    for (int i = 0; i < numberOfPatterns; i++) {
      assert link != null || notImplementedFields != null;
      Concrete.LetClausePattern subPattern = pattern.getPatterns().get(i);
      Expression newType;
      if (link != null) {
        ExprSubstitution substitution = new ExprSubstitution();
        int j = 0;
        for (DependentLink link1 = sigma.getParameters(); link1 != link; link1 = link1.getNext(), j++) {
          substitution.add(link1, ProjExpression.make(expression, j));
        }
        newType = link.getTypeExpr().subst(substitution);
      } else {
        newType = notImplementedFields.get(i).getType(classCall.getSortArgument()).applyExpression(expression);
      }
      LetClausePattern letClausePattern = typecheckLetClausePattern(subPattern, link != null ? ProjExpression.make(expression, i) : FieldCallExpression.make(notImplementedFields.get(i), classCall.getSortArgument(), expression), newType);
      if (letClausePattern == null) {
        return null;
      }
      patterns.add(letClausePattern);
      if (link != null) {
        link = link.getNext();
      }
    }

    return sigma == null ? new RecordLetClausePattern(notImplementedFields, patterns) : new TupleLetClausePattern(patterns);
  }

  @Override
  public TypecheckingResult visitLet(Concrete.LetExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(context)) {
      try (Utils.SetContextSaver ignore1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        List<? extends Concrete.LetClause> abstractClauses = expr.getClauses();
        List<LetClause> clauses = new ArrayList<>(abstractClauses.size());
        for (Concrete.LetClause clause : abstractClauses) {
          Pair<LetClause, Expression> pair = typecheckLetClause(clause);
          if (pair == null) {
            return null;
          }
          if (clause.getPattern().getReferable() != null) {
            pair.proj1.setPattern(new NameLetClausePattern(clause.getPattern().getReferable().textRepresentation()));
            context.put(clause.getPattern().getReferable(), pair.proj1);
          } else {
            myFreeBindings.add(pair.proj1);
            LetClausePattern pattern = typecheckLetClausePattern(clause.getPattern(), new ReferenceExpression(pair.proj1), pair.proj2);
            if (pattern == null) {
              return null;
            }
            pair.proj1.setPattern(pattern);
          }
          clauses.add(pair.proj1);
        }

        TypecheckingResult result = checkExpr(expr.getExpression(), expectedType);
        if (result == null) {
          return null;
        }

        ExprSubstitution substitution = new ExprSubstitution();
        for (LetClause clause : clauses) {
          substitution.add(clause, clause.getExpression().subst(substitution));
        }
        return new TypecheckingResult(new LetExpression(expr.isStrict(), clauses, result.expression), result.type.subst(substitution));
      }
    }
  }

  // Other

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean compareExpressions(boolean isLeft, Expression expected, Expression actual, Concrete.Expression expr) {
    if (!CompareVisitor.compare(getEquations(), Equations.CMP.EQ, actual, expected, expr)) {
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
      InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, true, expr);
      getEquations().addVariable(pl);
      pLevel = new Level(pl);
    }

    if (hLevel == null) {
      InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, true, expr);
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

  @Override
  public TypecheckingResult visitGoal(Concrete.GoalExpression expr, ExpectedType expectedType) {
    List<Error> errors = Collections.emptyList();
    TypecheckingResult exprResult = null;
    if (expr.getExpression() != null) {
      LocalErrorReporter errorReporter = this.errorReporter;
      Definition.TypeCheckingStatus status = myStatus;
      errors = new ArrayList<>();
      this.errorReporter = new ListLocalErrorReporter(errors);
      exprResult = checkExpr(expr.getExpression(), expectedType);
      this.errorReporter = errorReporter;
      myStatus = status;
    }

    TypecheckingError error = new GoalError(expr.getName(), context, expectedType, exprResult == null ? null : exprResult.type, errors, expr);
    errorReporter.report(error);
    Expression result = new ErrorExpression(exprResult == null ? null : exprResult.expression, error);
    return new TypecheckingResult(result, expectedType instanceof Expression ? (Expression) expectedType : result);
  }

  @Override
  public TypecheckingResult visitBinOpSequence(Concrete.BinOpSequenceExpression expr, ExpectedType expectedType) {
    throw new IllegalStateException();
  }

  public Integer getExpressionLevel(DependentLink link, Expression type, Expression expr, Equations equations, Concrete.SourceNode sourceNode) {
    boolean ok = expr != null;

    int level = -2;
    if (ok) {
      List<DependentLink> parameters = new ArrayList<>();
      for (; link.hasNext(); link = link.getNext()) {
        parameters.add(link);
      }

      Expression resultType = type == null ? null : type.getPiParameters(parameters, false);
      for (int i = 0; i < parameters.size(); i++) {
        link = parameters.get(i);
        if (link instanceof TypedDependentLink) {
          if (!CompareVisitor.compare(equations, Equations.CMP.EQ, link.getTypeExpr(), expr, sourceNode)) {
            ok = false;
            break;
          }
        }

        List<Expression> pathArgs = new ArrayList<>();
        pathArgs.add(expr);
        pathArgs.add(new ReferenceExpression(link));
        i++;
        if (i >= parameters.size()) {
          ok = false;
          break;
        }
        link = parameters.get(i);
        if (!CompareVisitor.compare(equations, Equations.CMP.EQ, link.getTypeExpr(), expr, sourceNode)) {
          ok = false;
          break;
        }

        pathArgs.add(new ReferenceExpression(link));
        expr = new FunCallExpression(Prelude.PATH_INFIX, Sort.STD, pathArgs);
        level++;
      }

      if (ok && resultType != null && !CompareVisitor.compare(equations, Equations.CMP.EQ, resultType, expr, sourceNode)) {
        ok = false;
      }
    }

    if (!ok) {
      errorReporter.report(new TypecheckingError("\\level has wrong format", sourceNode));
      return null;
    } else {
      return level;
    }
  }

  @Override
  public TypecheckingResult visitCase(Concrete.CaseExpression expr, ExpectedType expectedType) {
    if (expectedType == null && expr.getResultType() == null) {
      errorReporter.report(new TypecheckingError("Cannot infer the result type", expr));
      return null;
    }

    List<? extends Concrete.CaseArgument> caseArgs = expr.getArguments();
    LinkList list = new LinkList();
    List<Expression> expressions = new ArrayList<>(caseArgs.size());

    ExprSubstitution substitution = new ExprSubstitution();
    Type resultType = null;
    Expression resultExpr;
    Integer level = null;
    Expression resultTypeLevel = null;
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(context)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        for (Concrete.CaseArgument caseArg : caseArgs) {
          Type argType = null;
          if (caseArg.type != null) {
            argType = checkType(caseArg.type, ExpectedType.OMEGA);
          }

          TypecheckingResult exprResult = checkExpr(caseArg.expression, argType == null ? null : argType.getExpr().subst(substitution));
          if (exprResult == null) return null;
          DependentLink link = ExpressionFactory.parameter(caseArg.referable == null ? null : caseArg.referable.textRepresentation(), argType != null ? argType : exprResult.type instanceof Type ? (Type) exprResult.type : new TypeExpression(exprResult.type, getSortOfType(exprResult.type, expr)));
          list.append(link);
          if (caseArg.referable != null) {
            context.put(caseArg.referable, link);
          }
          myFreeBindings.add(link);
          expressions.add(exprResult.expression);
          substitution.add(link, exprResult.expression);
        }

        if (expr.getResultType() != null) {
          resultType = checkType(expr.getResultType(), ExpectedType.OMEGA);
        }
        if (resultType == null && expectedType == null) {
          return null;
        }
        resultExpr = resultType != null ? resultType.getExpr() : expectedType instanceof Expression ? (Expression) expectedType : new UniverseExpression(Sort.generateInferVars(myEquations, false, expr));

        if (expr.getResultTypeLevel() != null) {
          TypecheckingResult levelResult = checkExpr(expr.getResultTypeLevel(), null);
          if (levelResult != null) {
            resultTypeLevel = levelResult.expression;
            level = getExpressionLevel(EmptyDependentLink.getInstance(), levelResult.type, resultExpr, myEquations, expr.getResultTypeLevel());
          }
        }
      }
    }

    // Check if the level of the result type is specified explicitly
    if (expr.getResultTypeLevel() == null && expr.getResultType() instanceof Concrete.TypedExpression) {
      Concrete.Expression typeType = ((Concrete.TypedExpression) expr.getResultType()).type;
      if (typeType instanceof Concrete.UniverseExpression) {
        Concrete.UniverseExpression universeType = (Concrete.UniverseExpression) typeType;
        if (universeType.getHLevel() instanceof Concrete.NumberLevelExpression) {
          level = ((Concrete.NumberLevelExpression) universeType.getHLevel()).getNumber();
        }
      }
    }

    // Try to infer level either directly or from a path type.
    if (level == null && expr.getResultTypeLevel() == null) {
      resultExpr = resultExpr.normalize(NormalizeVisitor.Mode.WHNF);
      Expression resultExprWithoutPi = resultExpr.getPiParameters(null, false);
      DefCallExpression defCall = resultExprWithoutPi.checkedCast(DefCallExpression.class);
      level = defCall == null ? null : defCall.getUseLevel();

      if (level == null) {
        Sort sort = resultType == null ? null : resultType.getSortOfType();
        if (sort == null) {
          Expression type = resultExprWithoutPi.getType();
          if (type != null) {
            sort = type.toSort();
          }
        }
        if (sort != null && sort.getHLevel().isClosed()) {
          if (sort.getHLevel() != Level.INFINITY) {
            level = sort.getHLevel().getConstant();
          }
        } else if (sort == null || sort.getHLevel().getVar() instanceof InferenceLevelVariable) {
          DataCallExpression dataCall = resultExprWithoutPi.checkedCast(DataCallExpression.class);
          if (dataCall != null && dataCall.getDefinition() == Prelude.PATH) {
            LamExpression lamExpr = dataCall.getDefCallArguments().get(0).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(LamExpression.class);
            Expression bodyType = lamExpr == null ? null : lamExpr.getBody().getType();
            UniverseExpression universeBodyType = bodyType == null ? null : bodyType.checkedCast(UniverseExpression.class);
            if (universeBodyType != null && universeBodyType.getSort().getHLevel().isClosed() && universeBodyType.getSort().getHLevel() != Level.INFINITY) {
              level = universeBodyType.getSort().getHLevel().getConstant() - 1;
              if (level < -1) {
                level = -1;
              }
            }
          }
        }
      }
    }

    List<Clause> resultClauses = new ArrayList<>();
    ElimTree elimTree = new ElimTypechecking(this, resultExpr, EnumSet.of(PatternTypechecking.Flag.ALLOW_CONDITIONS, PatternTypechecking.Flag.CHECK_COVERAGE), level).typecheckElim(expr.getClauses(), expr, list.getFirst(), resultClauses);
    if (elimTree == null) {
      return null;
    }

    ConditionsChecking.check(resultClauses, elimTree, errorReporter);
    TypecheckingResult result = new TypecheckingResult(new CaseExpression(list.getFirst(), resultExpr, resultTypeLevel, elimTree, expressions), resultType != null ? resultExpr.subst(substitution) : resultExpr);
    return resultType == null ? result : checkResult(expectedType, result, expr);
  }
}
