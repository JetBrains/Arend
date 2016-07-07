package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.StringPrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.*;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingResult;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.StdImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final TypecheckerState myState;
  private final Abstract.Definition myParentDefinition;
  private final List<Binding> myContext;
  private final ErrorReporter myErrorReporter;
  private TypeCheckingDefCall myTypeCheckingDefCall;
  private TypeCheckingElim myTypeCheckingElim;
  private ImplicitArgsInference myArgsInference;

  public static class Result extends TypeCheckingResult {
    public Expression expression;
    public Expression type;

    public Result(Expression expression, Expression type) {
      this.expression = expression;
      this.type = type;
    }

    @Override
    public void subst(Substitution substitution) {
      expression = expression.subst(substitution.ExprSubst);
      type = type.subst(substitution.ExprSubst);
      expression = expression.subst(substitution.LevelSubst);
      type = type.subst(substitution.LevelSubst);
    }
  }

  public static class LetClauseResult extends TypeCheckingResult {
    LetClause letClause;

    public LetClauseResult(LetClause letClause) {
      this.letClause = letClause;
    }

    @Override
    public void subst(Substitution substitution) {
      letClause = letClause.subst(substitution.ExprSubst);
      letClause = letClause.subst(substitution.LevelSubst);
    }
  }

  private CheckTypeVisitor(TypecheckerState state, Abstract.Definition definition, List<Binding> localContext, ErrorReporter errorReporter, TypeCheckingDefCall typeCheckingDefCall, ImplicitArgsInference argsInference) {
    myState = state;
    myParentDefinition = definition;
    myContext = localContext;
    myErrorReporter = errorReporter;
    myTypeCheckingDefCall = typeCheckingDefCall;
    myArgsInference = argsInference;
  }

  public static class Builder {
    private final TypecheckerState myTypecheckerState;
    private final List<Binding> myLocalContext;
    private final ErrorReporter myErrorReporter;
    private TypeCheckingDefCall myTypeCheckingDefCall;
    private ImplicitArgsInference myArgsInference;
    private ClassDefinition myThisClass;
    private Expression myThisExpr;

    public Builder(TypecheckerState typecheckerState, List<Binding> localContext, ErrorReporter errorReporter) {
      this.myTypecheckerState = typecheckerState;
      myLocalContext = localContext;
      myErrorReporter = errorReporter;
    }

    public Builder(List<Binding> localContext, ErrorReporter errorReporter) {
      this(new TypecheckerState(), localContext, errorReporter);
    }

    public Builder typeCheckingDefCall(TypeCheckingDefCall typeCheckingDefCall) {
      myTypeCheckingDefCall = typeCheckingDefCall;
      return this;
    }

    public Builder argsInference(ImplicitArgsInference argsInference) {
      myArgsInference = argsInference;
      return this;
    }

    public Builder thisClass(ClassDefinition thisClass, Expression thisExpr) {
      myThisClass = thisClass;
      myThisExpr = thisExpr;
      return this;
    }

    public CheckTypeVisitor build(Abstract.Definition definition) {
      CheckTypeVisitor visitor = new CheckTypeVisitor(myTypecheckerState, definition, myLocalContext, myErrorReporter, myTypeCheckingDefCall, myArgsInference);
      if (myTypeCheckingDefCall == null) {
        visitor.myTypeCheckingDefCall = new TypeCheckingDefCall(myTypecheckerState, definition, visitor);
        visitor.myTypeCheckingDefCall.setThisClass(myThisClass, myThisExpr);
      }
      visitor.myTypeCheckingElim = new TypeCheckingElim(definition, visitor);
      if (myArgsInference == null) {
        visitor.myArgsInference = new StdImplicitArgsInference(definition, visitor);
      }
      return visitor;
    }

    @Deprecated
    public CheckTypeVisitor build() {
      return build(null);
    }
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public ImplicitArgsInference getImplicitArgsInference() {
    return myArgsInference;
  }

  public TypeCheckingElim getTypeCheckingElim() {
    return myTypeCheckingElim;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myTypeCheckingDefCall.setThisClass(thisClass, thisExpr);
  }

  public ClassDefinition getThisClass() {
    return myTypeCheckingDefCall.getThisClass();
  }

  public Expression getThisExpression() {
    return myTypeCheckingDefCall.getThisExpression();
  }

  public List<Binding> getContext() {
    return myContext;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  public Result checkResult(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }

    if (compare(result, expectedType, Equations.CMP.GE, expression)) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    } else {
      return null;
    }
  }

  public boolean compare(Result result, Expression expectedType, Equations.CMP cmp, Abstract.Expression expr) {
    if (result.getEquations() instanceof DummyEquations) {
      result.setEquations(myArgsInference.newEquations());
    }

    Expression expectedType1 = expectedType.normalize(NormalizeVisitor.Mode.NF);
    Expression actualType = result.type.normalize(NormalizeVisitor.Mode.NF);

    if (expectedType1.isAnyUniverse()) {
      if (actualType.toUniverse() != null) {
        return true;
      }

      /*InferenceBinding lvl = new LevelInferenceBinding("lvl", Level(), expr);
      result.addUnsolvedVariable(lvl);
      expectedType1 = Universe(Reference(lvl)); /**/
      InferenceBinding lp = new LevelInferenceBinding("lp", Lvl(), expr);
      InferenceBinding lh = new LevelInferenceBinding("lh", CNat(), expr);
      result.addUnsolvedVariable(lp);
      result.addUnsolvedVariable(lh);
      expectedType1 = Universe(new LevelExpression(lp, 0), new LevelExpression(lh, 0));
    }

    if (CompareVisitor.compare(result.getEquations(), cmp, expectedType1, actualType, expr)) {
      result.expression = new OfTypeExpression(result.expression, expectedType1);
      if (expectedType1.toUniverse() != null && actualType.toUniverse() == null) {
        result.type = expectedType1;
      }
      return true;
    } else {
      TypeCheckingError error = new TypeMismatchError(myParentDefinition, expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
  }

  public Result checkResultImplicit(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      result.update(true);
      return result;
    }
    return myArgsInference.inferTail(result, expectedType, expression);
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Incomplete expression", null);
      myErrorReporter.report(error);
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    result.update(false);
    result.reportErrors(myErrorReporter);
    if (result.hasUnsolvedVariables()) {
      return null;
    }

    result.expression = result.expression.strip();
    result.type = result.type.strip();
    return result;
  }

  private boolean compareExpressions(Result result, Expression expected, Expression actual, Abstract.Expression expr) {
    if (result.getEquations() instanceof DummyEquations) {
      result.setEquations(myArgsInference.newEquations());
    }
    if (!CompareVisitor.compare(result.getEquations(), Equations.CMP.EQ, expected.normalize(NormalizeVisitor.Mode.NF), actual.normalize(NormalizeVisitor.Mode.NF), expr)) {
      TypeCheckingError error = new SolveEquationsError<>(myParentDefinition, expected.normalize(NormalizeVisitor.Mode.HUMAN_NF), actual.normalize(NormalizeVisitor.Mode.HUMAN_NF), null, expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
    return true;
  }

  private boolean checkPath(Result result, Abstract.Expression expr) {
    if (result != null) {
      ConCallExpression conExpr = result.expression.getFunction().toConCall();
      if (conExpr != null && Prelude.isPathCon(conExpr.getDefinition())) {
        result.expression = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
        if (result.expression.getArguments().isEmpty()) {
          // FIXME[errorformat]
          TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Expected an argument for 'path'", expr);
          expr.setWellTyped(myContext, Error(result.expression, error));
          myErrorReporter.report(error);
          return false;
        }

        List<? extends Expression> args = result.expression.getFunction().toConCall().getDataTypeArguments();
        if (!compareExpressions(result, args.get(1), Apps(result.expression.getArguments().get(0), Left()), expr) ||
            !compareExpressions(result, args.get(2), Apps(result.expression.getArguments().get(0), Right()), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    if (!checkPath(result, expr)) {
      return null;
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitAppLevel(Abstract.ApplyLevelExpression expr, Expression expectedType) {
    Result result = typeCheck(expr.getFunction(), expectedType);
    if (result == null) {
      return null;
    }

    DefCallExpression defCall = result.expression.getFunction().toDefCall();
    if (defCall == null) {
      TypeCheckingError error = new TypeCheckingError("Level can only be assigned to a definition", expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return null;
    }

    LevelExpression level = typeCheckLevel(expr.getLevel(), null);
    if (level == null) {
      return null;
    }

    List<Binding> polyParams = defCall.getDefinition().getPolyParams();

    for (Binding param : polyParams) {
      LevelExpression value = defCall.getPolyParamsSubst().get(param);
      if (value == null) {
        assert false;
        return null;
      }
      if (value.isBinding() && value.getUnitBinding() instanceof LevelInferenceBinding) {
        result.getEquations().add(new LevelExpression(value.getUnitBinding()), level, Equations.CMP.EQ, expr);
        return result;
      }
    }

    return result;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = myTypeCheckingDefCall.typeCheckDefCall(expr);
    if (!checkPath(result, expr)) {
      return null;
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitModuleCall(Abstract.ModuleCallExpression expr, Expression params) {
    if (expr.getModule() == null) {
      TypeCheckingError error = new UnresolvedReferenceError(myParentDefinition, expr, new ModulePath(expr.getPath()).toString());
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    Definition typechecked = myState.getTypechecked(expr.getModule());
    if (typechecked == null) {
      assert false;
      // FIXME[errorformat]
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Internal error: module '" + new ModulePath(expr.getPath()) + "' is not available yet", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    return new Result(ClassCall((ClassDefinition) typechecked), new UniverseExpression(typechecked.getUniverse()));
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    List<DependentLink> piParams = new ArrayList<>();
    Expression expectedCodomain = expectedType == null ? null : expectedType.getPiParameters(piParams, true, false);
    LinkList list = new LinkList();
    DependentLink actualPiLink = null;
    Result result = new Result(null, null);
    ExprSubstitution piLamSubst = new ExprSubstitution();
    int piParamsIndex = 0;
    int argIndex = 1;

    Result bodyResult;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      Map<Binding, InferenceBinding> bindingTypes = new HashMap<>();

      for (Abstract.Argument argument : expr.getArguments()) {
        List<String> names;
        Result argResult = null;
        Abstract.Expression argType = null;
        boolean isExplicit = argument.getExplicit();

        if (argument instanceof Abstract.NameArgument) {
          names = Collections.singletonList(((Abstract.NameArgument) argument).getName());
        } else if (argument instanceof Abstract.TypeArgument) {
          names = argument instanceof Abstract.TelescopeArgument ? ((Abstract.TelescopeArgument) argument).getNames() : Collections.<String>singletonList(null);
          argType = ((Abstract.TypeArgument) argument).getType();
          argResult = typeCheck(argType, Universe());
          if (argResult == null) return null;
          result.add(argResult);
        } else {
          throw new IllegalStateException();
        }

        DependentLink link = param(isExplicit, names, argResult == null ? null : argResult.expression);
        list.append(link);

        for (String name : names) {
          if (piParamsIndex < piParams.size()) {
            DependentLink piLink = piParams.get(piParamsIndex++);
            if (piLink.isExplicit() != isExplicit) {
              // FIXME[errorformat]
              myErrorReporter.report(new TypeCheckingError(myParentDefinition, ordinal(argIndex) + " argument of the lambda should be " + (piLink.isExplicit() ? "explicit" : "implicit"), expr));
              link.setExplicit(piLink.isExplicit());
            }

            Expression piLinkType = piLink.getType().subst(piLamSubst);
            if (argResult != null) {
              if (result.getEquations() instanceof DummyEquations) {
                result.setEquations(myArgsInference.newEquations());
              }
              if (!CompareVisitor.compare(result.getEquations(), Equations.CMP.EQ, piLinkType.normalize(NormalizeVisitor.Mode.NF), argResult.expression.normalize(NormalizeVisitor.Mode.NF), argType)) {
                TypeCheckingError error = new TypeMismatchError(myParentDefinition, piLinkType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argResult.expression.normalize(NormalizeVisitor.Mode.HUMAN_NF), argType);
                myErrorReporter.report(error);
                return null;
              }
            } else {
              link.setType(piLinkType);
            }

            piLamSubst.add(piLink, Reference(link));
          } else {
            if (argResult == null) {
              InferenceBinding pLvlInferenceBinding = new LevelInferenceBinding("plvl-of-" + name, DataCall(Preprelude.LVL), expr); // new LambdaInferenceBinding("plvl-of-" + name, DataCall(Preprelude.LVL), argIndex, expr, true);
              InferenceBinding hLvlInferenceBinding = new LevelInferenceBinding("hlvl-of-" + name, DataCall(Preprelude.CNAT), expr); // new LambdaInferenceBinding("hlvl-of-" + name, DataCall(Preprelude.CNAT), argIndex, expr, true);
              InferenceBinding inferenceBinding = new LambdaInferenceBinding("type-of-" + name, Universe(new LevelExpression(pLvlInferenceBinding), new LevelExpression(hLvlInferenceBinding)), argIndex, expr, false);
              link.setType(Reference(inferenceBinding));
              bindingTypes.put(link, inferenceBinding);
            }
            if (actualPiLink == null) {
              actualPiLink = link;
            } /**/
          }

          argIndex++;
          myContext.add(link);
          link = link.getNext();
        }
      }

      Expression expectedBodyType = null;
      if (actualPiLink == null && expectedCodomain != null) {
        expectedBodyType = expectedCodomain.fromPiParameters(piParams.subList(piParamsIndex, piParams.size())).subst(piLamSubst);
      }

      Abstract.Expression body = expr.getBody();
      bodyResult = typeCheck(body, expectedBodyType);
      if (bodyResult == null) return null;
      result.add(bodyResult);
      if (actualPiLink != null && expectedCodomain != null) {
        result.expression = bodyResult.expression;
        result.type = Pi(actualPiLink, bodyResult.type);
        if (!compare(result, expectedCodomain, Equations.CMP.EQ, body)) {
          return null;
        }
      }

      for (int i = myContext.size() - 1; i >= saver.getOriginalSize(); i--) {
        result.getEquations().abstractBinding(myContext.get(i));
        InferenceBinding bindingType = bindingTypes.get(myContext.get(i));
        if (bindingType != null) {
          result.addUnsolvedVariable((InferenceBinding) bindingType.getType().toUniverse().getUniverse().getPLevel().getUnitBinding());
          result.addUnsolvedVariable((InferenceBinding) bindingType.getType().toUniverse().getUniverse().getHLevel().getUnitBinding());
          result.addUnsolvedVariable(bindingType);
          Substitution substitution = result.getSubstitution(false);
          if (!substitution.getDomain().isEmpty()) {
            bodyResult.expression = bodyResult.expression.subst(substitution.ExprSubst);
            bodyResult.expression = bodyResult.expression.subst(substitution.LevelSubst);
            bodyResult.type = bodyResult.type.subst(substitution.ExprSubst);
            bodyResult.type = bodyResult.type.subst(substitution.LevelSubst);
            ((DependentLink) myContext.get(i)).setType(myContext.get(i).getType().subst(substitution.ExprSubst));
            ((DependentLink) myContext.get(i)).setType(myContext.get(i).getType().subst(substitution.LevelSubst));
          }
        }
      }
    }

    result.expression = Lam(list.getFirst(), bodyResult.expression);
    result.type = Pi(list.getFirst(), bodyResult.type);
    return result;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), expr.getCodomain(), expr), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    int hlevel = expr.getUniverse().myHLevel == Abstract.UniverseExpression.Universe.NOT_TRUNCATED ? TypeUniverse.NOT_TRUNCATED : expr.getUniverse().myHLevel;
    UniverseExpression universe = Universe(expr.getUniverse().myPLevel, hlevel);
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getUniverse().succ())), expr);
  }

  private LevelExpression typeCheckLevelAtom(Abstract.Expression expr, Expression expectedType) {
    int num_sucs = 0;
    TypeCheckingError error = null;

    while (expr instanceof Abstract.AppExpression) {
      Abstract.AppExpression app = (Abstract.AppExpression) expr;
      Abstract.Expression suc = app.getFunction();
      if (!(suc instanceof Abstract.AppExpression) && (!(suc instanceof Abstract.DefCallExpression) || !((Abstract.DefCallExpression) suc).getName().equals("suc"))) {
        error = new TypeCheckingError("Expression " + suc + " is invalid, 'suc' expected", expr);
        break;
      }
      expr = app.getArgument().getExpression();
      ++num_sucs;
    }

    if (error == null) {
      if (expr instanceof Abstract.NumericLiteral) {
        int val = ((Abstract.NumericLiteral) expr).getNumber();
        return new LevelExpression(val + num_sucs);
      }
      Result refResult = typeCheck(expr, expectedType);
      if (refResult != null) {
        ReferenceExpression ref = refResult.expression.toReference();
        if (ref != null) {
          return new LevelExpression(ref.getBinding(), num_sucs);
        }
      }
    }

    return null;
  }

  private LevelExpression typeCheckLevel(Abstract.Expression expr, Expression expectedType) {
    LevelExpression result = new LevelExpression(0);
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    Abstract.Expression max = Abstract.getFunction(expr, args) ;

    if (max instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) max).getName().equals("max")) {
      for (int i = 0; i < args.size(); ++i) {
        result = result.max(typeCheckLevelAtom(args.get(i).getExpression(), expectedType));
      }
      return result;
    }
    return typeCheckLevelAtom(expr, expectedType);
  }

  @Override
  public Result visitPolyUniverse(Abstract.PolyUniverseExpression expr, Expression expectedType) {
    LevelExpression levelP = typeCheckLevel(expr.getPLevel(), Lvl());
    LevelExpression levelH = typeCheckLevel(expr.getHLevel(), CNat());
    if (levelP == null || levelH == null) return null;
    UniverseExpression universe = Universe(new TypeUniverse(levelP, levelH));
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getUniverse().succ())), expr);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myParentDefinition, myContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    expr.setWellTyped(myContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    if (expectedType != null) {
      InferenceBinding binding = new ExpressionInferenceBinding(expectedType, expr);
      Result result = new Result(Reference(binding), expectedType);
      result.addUnsolvedVariable(binding);
      return result;
    } else {
      TypeCheckingError error = new ArgInferenceError(expression(), expr, new Expression[0], new LevelExpression[0]);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      SigmaExpression expectedTypeSigma = expectedTypeNorm.toSigma();
      if (expectedTypeSigma != null) {
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          // FIXME[errorformat]
          TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr);
          expr.setWellTyped(myContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Result tupleResult = new Result(Tuple(fields, expectedTypeSigma), expectedType);
        ExprSubstitution substitution = new ExprSubstitution();
        for (Abstract.Expression field : expr.getFields()) {
          Expression expType = sigmaParams.getType().subst(substitution);
          Result result = typeCheck(field, expType);
          if (result == null) return null;
          fields.add(result.expression);
          substitution.add(sigmaParams, result.expression);
          tupleResult.add(result);

          sigmaParams = sigmaParams.getNext();
        }
        return tupleResult;
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    LinkList list = new LinkList();
    Result tupleResult = new Result(null, null);
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      list.append(param(result.type));
      tupleResult.add(result);
    }

    SigmaExpression type = Sigma(list.getFirst());
    tupleResult.expression = Tuple(fields, type);
    tupleResult.type = type;
    tupleResult = checkResult(expectedTypeNorm, tupleResult, expr);
    tupleResult.update(true);
    return tupleResult;
  }

  public Result visitArguments(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression codomain, Abstract.Expression expr) {
    Expression[] domainTypes = new Expression[arguments.size()];
    Result argsResult = new Result(null, null);
    LinkList list = new LinkList();
    Result codomainResult = null;

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (int i = 0; i < domainTypes.length; i++) {
        Result result = typeCheck(arguments.get(i).getType(), Universe());
        if (result == null) return null;
        domainTypes[i] = result.type;
        argsResult.add(result);

        if (arguments.get(i) instanceof Abstract.TelescopeArgument) {
          DependentLink link = param(arguments.get(i).getExplicit(), ((Abstract.TelescopeArgument) arguments.get(i)).getNames(), result.expression);
          list.append(link);
          myContext.addAll(DependentLink.Helper.toContext(link));
        } else {
          DependentLink link = param(arguments.get(i).getExplicit(), (String) null, result.expression);
          list.append(link);
          myContext.add(link);
        }
      }

      if (codomain != null) {
        codomainResult = typeCheck(codomain, Universe());
        if (codomainResult == null) return null;
        argsResult.add(codomainResult);
      }

      if (!argsResult.getEquations().isEmpty()) {
        for (int i = saver.getOriginalSize(); i < myContext.size(); i++) {
          argsResult.getEquations().abstractBinding(myContext.get(i));
        }
      }
    }

    TypeUniverse maxDomainUni = null;
    for (Expression domainType : domainTypes) {
      TypeUniverse argUniverse = domainType.normalize(NormalizeVisitor.Mode.NF).toUniverse().getUniverse();
      if (maxDomainUni == null) {
        maxDomainUni = argUniverse;
        continue;
      }
      maxDomainUni = maxDomainUni.max(argUniverse);
    }
    TypeUniverse codomainUniverse = null;
    if (codomainResult != null) {
      codomainUniverse = codomainResult.type.normalize(NormalizeVisitor.Mode.NF).toUniverse().getUniverse();
    }

    TypeUniverse finalUniverse = null;

    if (codomainUniverse != null) {
      finalUniverse = new TypeUniverse(maxDomainUni != null ? maxDomainUni.getPLevel().max(codomainUniverse.getPLevel()) : codomainUniverse.getPLevel(),
                          codomainUniverse.getHLevel());
    } else if (maxDomainUni != null){
      finalUniverse = new TypeUniverse(maxDomainUni.getPLevel(),
                         maxDomainUni.getHLevel());
    }

    argsResult.expression = codomainResult == null ? Sigma(list.getFirst()) : Pi(list.getFirst(), codomainResult.expression);
    argsResult.type = new UniverseExpression(finalUniverse);
    argsResult.update(false);
    return argsResult;
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), null, expr), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    return checkResultImplicit(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression expectedType) {
    assert expr.getSequence().isEmpty();
    return typeCheck(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = new TypeCheckingError(myParentDefinition, "\\elim is allowed only at the root of a definition", expr);
    myErrorReporter.report(error);
    expr.setWellTyped(myContext, Error(null, error));
    return null;
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Cannot infer type of the result", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Result caseResult = new Result(null, expectedType);
    LetClause letBinding = let(Abstract.CaseExpression.FUNCTION_NAME, EmptyDependentLink.getInstance(), expectedType, (ElimTreeNode) null);
    List<? extends Abstract.Expression> expressions = expr.getExpressions();

    LinkList list = new LinkList();
    List<Expression> letArguments = new ArrayList<>(expressions.size());
    for (int i = 0; i < expressions.size(); i++) {
      Result exprResult = typeCheck(expressions.get(i), null);
      if (exprResult == null) return null;
      if (!exprResult.getEquations().isEmpty()) {
        for (DependentLink link = list.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
          exprResult.getEquations().abstractBinding(link);
        }
      }
      caseResult.add(exprResult);
      list.append(param(true, vars(Abstract.CaseExpression.ARGUMENT_NAME + i), exprResult.type));
      letArguments.add(exprResult.expression);
    }
    letBinding.setParameters(list.getFirst());

    TypeCheckingElim.Result elimResult = myTypeCheckingElim.typeCheckElim(expr, list.getFirst(), expectedType, true);
    if (elimResult == null) return null;
    if (!elimResult.getEquations().isEmpty()) {
      for (DependentLink link = list.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
        elimResult.getEquations().abstractBinding(link);
      }
    }
    caseResult.add(elimResult);
    letBinding.setElimTree(elimResult.elimTree);

    caseResult.expression = Let(lets(letBinding), Apps(Reference(letBinding), letArguments));
    caseResult.update(false);
    expr.setWellTyped(myContext, caseResult.expression);
    return caseResult;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Abstract.Expression expr1 = expr.getExpression();
    Result exprResult = typeCheck(expr1, null);
    if (exprResult == null) return null;
    Expression type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    SigmaExpression sigmaType = type.toSigma();
    if (sigmaType == null) {
      TypeCheckingError error = new TypeMismatchError(myParentDefinition, new StringPrettyPrintable("A sigma type"), type, expr1);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    DependentLink sigmaParams = sigmaType.getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      // FIXME[errorformat]
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Index " + (expr.getField() + 1) + " out of range", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, Proj(exprResult.expression, i));
    }

    exprResult.expression = Proj(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getType().subst(substitution);
    return checkResult(expectedType, exprResult, expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result result = typeCheck(baseClassExpr, null);
    if (result == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.toClassCall();
    if (classCallExpr == null) {
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Expected a class", baseClassExpr);
      expr.setWellTyped(myContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = classCallExpr.getDefinition();
    if (baseClass.hasErrors()) {
      TypeCheckingError error = new HasErrors(myParentDefinition, baseClass.getName(), expr);
      expr.setWellTyped(myContext, Error(classCallExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    Collection<? extends Abstract.ImplementStatement> statements = expr.getStatements();
    if (statements.isEmpty()) {
      result.expression = classCallExpr;
      result.type = baseClass.getType();
      return checkResult(expectedType, result, expr);
    }

    class ImplementStatement {
      ClassField classField;
      Abstract.Expression term;

      public ImplementStatement(ClassField classField, Abstract.Expression term) {
        this.classField = classField;
        this.term = term;
      }
    }

    List<ImplementStatement> fields = new ArrayList<>(statements.size());
    for (Abstract.ImplementStatement statement : statements) {
      String name = statement.getName();
      Map.Entry<ClassField, ClassDefinition.FieldImplementation> fieldEntry = baseClass.getFieldEntry(name);
      if (fieldEntry == null) {
        myErrorReporter.report(new TypeCheckingError(myParentDefinition, "Class '" + baseClass.getName() + "' does not have field '" + name + "'", statement));
      } else
      if (fieldEntry.getValue().isImplemented()) {
        myErrorReporter.report(new TypeCheckingError(myParentDefinition, "Field '" + fieldEntry.getValue().name + "' is already implemented", statement));
      } else {
        boolean ok = true;
        for (ImplementStatement implementStatement : fields) {
          if (implementStatement.classField.getName().equals(name)) {
            myErrorReporter.report(new TypeCheckingError(myParentDefinition, "Field '" + name + "' is already implemented", statement));
            ok = false;
          }
        }
        if (ok) {
          fields.add(new ImplementStatement(fieldEntry.getKey(), statement.getExpression()));
        }
      }
    }

    Result classExtResult = new Result(null, null);
    Map<ClassField, ClassCallExpression.ImplementStatement> typeCheckedStatements = Collections.emptyMap();
    if (!classCallExpr.getImplementStatements().isEmpty()) {
      typeCheckedStatements = new HashMap<>(classCallExpr.getImplementStatements());
    }

    for (int i = 0; i < fields.size(); i++) {
      ImplementStatement field = fields.get(i);
      Expression thisExpr = New(ClassCall(baseClass, typeCheckedStatements));
      Result result1 = typeCheck(field.term, field.classField.getBaseType().subst(field.classField.getThisParameter(), thisExpr));
      if (result1 == null) {
        for (i++; i < fields.size(); i++) {
          typeCheck(fields.get(i).term, fields.get(i).classField.getBaseType().subst(fields.get(i).classField.getThisParameter(), thisExpr));
        }
        return null;
      }

      typeCheckedStatements = new HashMap<>(typeCheckedStatements);
      typeCheckedStatements.put(field.classField, new ClassCallExpression.ImplementStatement(result1.type, result1.expression));
      classExtResult.add(result1);
    }

    ClassCallExpression resultExpr = ClassCall(baseClass, typeCheckedStatements);
    classExtResult.expression = resultExpr;
    classExtResult.type = new UniverseExpression(resultExpr.getUniverse());
    classExtResult.update(true);
    return checkResult(expectedType, classExtResult, expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.toClassCall();
    if (classCallExpr == null) {
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Expected a class", expr.getExpression());
      expr.setWellTyped(myContext, Error(normExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    int fieldsNumber = 0;
    for (Map.Entry<ClassField, ClassDefinition.FieldImplementation> entry : classCallExpr.getDefinition().getFieldsMap()) {
      if (!entry.getValue().isImplemented()) {
        fieldsNumber++;
      }
    }

    if (classCallExpr.getImplementStatements().size() == fieldsNumber) {
      exprResult.expression = New(normExpr);
      exprResult.type = normExpr;
      return checkResult(expectedType, exprResult, expr);
    } else {
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Class '" + classCallExpr.getDefinition().getName() + "' has " + classCallExpr.getDefinition().getNumberOfVisibleFields() + " fields", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private LetClauseResult typeCheckLetClause(Abstract.LetClause clause) {
    LinkList links = new LinkList();
    Expression resultType;
    ElimTreeNode elimTree;
    LetClauseResult letResult = new LetClauseResult(null);

    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument arg : clause.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          Result result = typeCheck(teleArg.getType(), Universe());
          if (result == null) return null;
          if (!result.getEquations().isEmpty()) {
            for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
              result.getEquations().abstractBinding(link);
            }
          }
          letResult.add(result);
          links.append(param(teleArg.getExplicit(), teleArg.getNames(), result.expression));
          for (DependentLink link = links.getLast(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            myContext.add(link);
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (result == null) return null;
        if (!result.getEquations().isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            result.getEquations().abstractBinding(link);
          }
        }
        letResult.add(result);
        expectedType = result.expression;
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        myContext.subList(myContext.size() - size(links.getFirst()), myContext.size()).clear();
        TypeCheckingElim.Result elimResult = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? links.getFirst() : null, expectedType, false);
        if (elimResult == null)
          return null;
        if (!elimResult.getEquations().isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            elimResult.getEquations().abstractBinding(link);
          }
        }
        letResult.add(elimResult);
        elimTree = elimResult.elimTree;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        if (!termResult.getEquations().isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            termResult.getEquations().abstractBinding(link);
          }
        }
        letResult.add(termResult);
        elimTree = top(links.getFirst(), leaf(clause.getArrow(), termResult.expression));
        resultType = termResult.type;
      }

      TypeCheckingError error = TypeCheckingElim.checkCoverage(clause, links.getFirst(), elimTree, expectedType);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
      error = TypeCheckingElim.checkConditions(clause, links.getFirst(), elimTree);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
    }

    letResult.letClause = new LetClause(clause.getName(), links.getFirst(), resultType, elimTree);
    letResult.update(false);
    myContext.add(letResult.letClause);
    return letResult;
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      List<LetClause> clauses = new ArrayList<>();
      Result letResult = new Result(null, null);
      for (int i = 0; i < expr.getClauses().size(); i++) {
        LetClauseResult clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (clauseResult == null) return null;
        for (Binding binding : clauses) {
          clauseResult.getEquations().abstractBinding(binding);
        }
        letResult.add(clauseResult);
        clauses.add(clauseResult.letClause);
      }
      Result result = typeCheck(expr.getExpression(), expectedType == null ? null : expectedType);
      if (result == null) return null;
      for (Binding binding : clauses) {
        result.getEquations().abstractBinding(binding);
      }
      letResult.add(result);

      letResult.expression = Let(clauses, result.expression);
      letResult.type = Let(clauses, result.type);
      letResult.update(false);
      return letResult;
    }
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression expectedType) {
    int number = expr.getNumber();
    Expression expression = Zero();
    for (int i = 0; i < number; ++i) {
      expression = Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, Nat()), expr);
  }
}
