package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.StdImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final List<Binding> myContext;
  private final ErrorReporter myErrorReporter;
  private TypeCheckingDefCall myTypeCheckingDefCall;
  private TypeCheckingElim myTypeCheckingElim;
  private ImplicitArgsInference myArgsInference;

  public static class Result {
    public Expression expression;
    public Expression type;
    public Equations equations;

    public Result(Expression expression, Expression type, Equations equations) {
      assert equations != null;
      this.expression = expression;
      this.type = type;
      this.equations = equations;
    }
  }

  public static class LetClauseResult {
    LetClause letClause;
    Equations equations;

    public LetClauseResult(LetClause letClause, Equations equations) {
      this.letClause = letClause;
      this.equations = equations;
    }
  }

  private CheckTypeVisitor(List<Binding> localContext, ErrorReporter errorReporter, TypeCheckingDefCall typeCheckingDefCall, ImplicitArgsInference argsInference) {
    myContext = localContext;
    myErrorReporter = errorReporter;
    myTypeCheckingDefCall = typeCheckingDefCall;
    myArgsInference = argsInference;
  }

  public static class Builder {
    private final List<Binding> myLocalContext;
    private final ErrorReporter myErrorReporter;
    private TypeCheckingDefCall myTypeCheckingDefCall;
    private ImplicitArgsInference myArgsInference;
    private ClassDefinition myThisClass;
    private Expression myThisExpr;

    public Builder(List<Binding> localContext, ErrorReporter errorReporter) {
      myLocalContext = localContext;
      myErrorReporter = errorReporter;
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

    public CheckTypeVisitor build() {
      CheckTypeVisitor visitor = new CheckTypeVisitor(myLocalContext, myErrorReporter, myTypeCheckingDefCall, myArgsInference);
      if (myTypeCheckingDefCall == null) {
        visitor.myTypeCheckingDefCall = new TypeCheckingDefCall(visitor);
        visitor.myTypeCheckingDefCall.setThisClass(myThisClass, myThisExpr);
      }
      visitor.myTypeCheckingElim = new TypeCheckingElim(visitor);
      if (myArgsInference == null) {
        visitor.myArgsInference = new StdImplicitArgsInference(visitor);
      }
      return visitor;
    }
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public TypeCheckingElim getTypeCheckingElim() {
    return myTypeCheckingElim;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myTypeCheckingDefCall.setThisClass(thisClass, thisExpr);
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
    if (CompareVisitor.compare(result.equations, cmp, expectedType.normalize(NormalizeVisitor.Mode.NF), result.type.normalize(NormalizeVisitor.Mode.NF))) {
      return true;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
  }

  public Result checkResultImplicit(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }
    result = myArgsInference.inferTail(result, expectedType, expression);
    updateAppResult(result, expression);
    return result;
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    result.equations.reportErrors(myErrorReporter);
    return result;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    if (expectedType == null) {
      updateAppResult(result, expr);
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = myTypeCheckingDefCall.typeCheckDefCall(expr);
    if (result == null) {
      return null;
    }
    result.equations = myArgsInference.newEquations();
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    List<DependentLink> piParams = new ArrayList<>();
    Expression expectedCodomain = expectedType == null ? null : expectedType.getPiParameters(piParams, true, false);
    LinkList list = new LinkList();
    DependentLink actualPiLink = null;
    Equations equations = myArgsInference.newEquations();
    int piParamsIndex = 0;
    int argIndex = 1;

    Result bodyResult;
    List<InferenceBinding> inferenceBindings = new ArrayList<>();
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
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
          equations.add(argResult.equations);
        } else {
          throw new IllegalStateException();
        }

        DependentLink link = param(isExplicit, names, argResult == null ? null : argResult.expression);
        list.append(link);

        for (String name : names) {
          if (piParamsIndex < piParams.size()) {
            DependentLink piLink = piParams.get(piParamsIndex++);
            if (piLink.isExplicit() != isExplicit) {
              myErrorReporter.report(new TypeCheckingError(ordinal(argIndex) + " argument of the lambda should be " + (piLink.isExplicit() ? "explicit" : "implicit"), expr));
              link.setExplicit(piLink.isExplicit());
            }
            if (argResult != null) {
              if (!CompareVisitor.compare(equations, Equations.CMP.EQ, piLink.getType().normalize(NormalizeVisitor.Mode.NF), argResult.expression.normalize(NormalizeVisitor.Mode.NF))) {
                TypeCheckingError error = new TypeMismatchError(piLink.getType().normalize(NormalizeVisitor.Mode.HUMAN_NF), argResult.expression.normalize(NormalizeVisitor.Mode.HUMAN_NF), argType);
                myErrorReporter.report(error);
                return null;
              }
            } else {
              link.setType(piLink.getType());
            }
          } else {
            if (argResult == null) {
              InferenceBinding inferenceBinding = new InferenceBinding("type_of_" + name, Universe());
              link.setType(Reference(inferenceBinding));
              inferenceBindings.add(inferenceBinding);
            }
            if (actualPiLink == null) {
              actualPiLink = link;
            }
          }

          argIndex++;
          myContext.add(link);
          link = link.getNext();
        }
      }

      Expression expectedBodyType = null;
      if (actualPiLink == null && expectedCodomain != null) {
        expectedBodyType = expectedCodomain.fromPiParameters(piParams.subList(piParamsIndex, piParams.size()));
      }

      Abstract.Expression body = expr.getBody();
      bodyResult = typeCheck(body, expectedBodyType);
      if (bodyResult == null) return null;
      equations.add(bodyResult.equations);
      if (actualPiLink != null && expectedCodomain != null && !compare(new Result(bodyResult.expression, Pi(actualPiLink, bodyResult.type), equations), expectedCodomain, Equations.CMP.EQ, body)) {
        return null;
      }

      for (int i = saver.getOriginalSize(); i < myContext.size(); i++) {
        equations.abstractBinding(myContext.get(i));
      }
    }

    Result result = new Result(Lam(list.getFirst(), bodyResult.expression), Pi(list.getFirst(), bodyResult.type), equations);
    updateResult(result, inferenceBindings, expr, true);
    return result;
  }

  private void updateAppResult(CheckTypeVisitor.Result result, Abstract.SourceNode fun) {
    if (result == null || result.equations.isEmpty()) {
      return;
    }
    while (fun instanceof Abstract.AppExpression) {
      fun = ((Abstract.AppExpression) fun).getFunction();
    }

    List<InferenceBinding> bindings = new ArrayList<>();
    for (Expression expr = result.expression; expr instanceof AppExpression; expr = ((AppExpression) expr).getFunction()) {
      Expression argument = ((AppExpression) expr).getArgument().getExpression();
      if (argument instanceof ReferenceExpression && ((ReferenceExpression) argument).getBinding() instanceof InferenceBinding) {
        bindings.add((InferenceBinding) ((ReferenceExpression) argument).getBinding());
      }
    }
    if (bindings.isEmpty()) {
      return;
    }
    Collections.reverse(bindings);
    updateResult(result, bindings, fun, false);
  }

  private void updateResult(CheckTypeVisitor.Result result, List<InferenceBinding> bindings, Abstract.SourceNode fun, boolean lambda) {
    Substitution substitution = result.equations.getInferenceVariables(bindings);
    result.expression = result.expression.subst(substitution);
    result.type = result.type.subst(substitution);

    for (int i = 0; i < bindings.size(); i++) {
      if (substitution.get(bindings.get(i)) == null) {
        TypeCheckingError error = new ArgInferenceError(lambda ? ArgInferenceError.lambdaArg(i + 1) : ArgInferenceError.functionArg(i + 1), fun, null);
        myErrorReporter.report(error);
      }
    }
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), expr.getCodomain(), expr), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(new UniverseExpression(expr.getUniverse()), new UniverseExpression(expr.getUniverse().succ()), myArgsInference.newEquations()), expr);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.NF), expr);
    expr.setWellTyped(myContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    TypeCheckingError error = new ArgInferenceError(expression(), expr, null);
    expr.setWellTyped(myContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      if (!(expectedTypeNorm instanceof SigmaExpression || expectedType instanceof InferHoleExpression)) {
        // TODO
        // Expression fExpectedTypeNorm = expectedTypeNorm.getFunction(new ArrayList<Expression>());

        TypeCheckingError error = new TypeMismatchError(expectedTypeNorm, Sigma(params(param(Error(null, null)), param(Error(null, null)))), expr);
        expr.setWellTyped(myContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      if (expectedTypeNorm instanceof SigmaExpression) {
        DependentLink sigmaParams = ((SigmaExpression) expectedTypeNorm).getParameters();
        int sigmaParamsSize = DependentLink.Helper.size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          TypeCheckingError error = new TypeCheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr);
          expr.setWellTyped(myContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Expression expression = Tuple(fields, (SigmaExpression) expectedTypeNorm);
        Equations equations = myArgsInference.newEquations();
        for (int i = 0; i < sigmaParamsSize; i++) {
          Substitution substitution = new Substitution();
          for (int j = fields.size() - 1; j >= 0; --j) {
            substitution.add(sigmaParams, fields.get(j));
          }

          Expression expType = sigmaParams.getType().subst(substitution);
          Result result = typeCheck(expr.getFields().get(i), expType);
          if (result == null) return null;
          fields.add(result.expression);
          if (result.equations != null) {
            equations.add(result.equations);
          }

          sigmaParams = sigmaParams.getNext();
        }
        return new Result(expression, expectedType, equations);
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    LinkList list = new LinkList();
    Equations equations = myArgsInference.newEquations();
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      list.append(param(result.type));
      if (result.equations != null) {
        equations.add(result.equations);
      }
    }

    SigmaExpression type = Sigma(list.getFirst());
    /* TODO
    if (expectedTypeNorm instanceof InferHoleExpression) {
      equations.add(new CompareVisitor.Equation((InferHoleExpression) expectedTypeNorm, type));
    }
    */
    return new Result(Tuple(fields, type), type, equations);
  }

  public Result visitArguments(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression codomain, Abstract.Expression expr) {
    Expression[] domainTypes = new Expression[arguments.size()];
    Equations equations = myArgsInference.newEquations();
    LinkList list = new LinkList();
    Result codomainResult = null;

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (int i = 0; i < domainTypes.length; i++) {
        Result result = typeCheck(arguments.get(i).getType(), Universe());
        if (result == null) return null;
        domainTypes[i] = result.type;
        equations.add(result.equations);

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
        if (!codomainResult.equations.isEmpty()) {
          for (int j = saver.getOriginalSize(); j < myContext.size(); j++) {
            codomainResult.equations.abstractBinding(myContext.get(j));
          }
        }
        equations.add(codomainResult.equations);
      }

      if (!equations.isEmpty()) {
        for (int i = saver.getOriginalSize(); i < myContext.size(); i++) {
          equations.abstractBinding(myContext.get(i));
        }
      }
    }

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainTypes.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainTypes[i].normalize(NormalizeVisitor.Mode.NF)).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + ordinal(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr);
        expr.setWellTyped(myContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = maxUniverse;
    }
    if (codomainResult != null) {
      Universe codomainUniverse = ((UniverseExpression) codomainResult.type.normalize(NormalizeVisitor.Mode.NF)).getUniverse();
      Universe maxUniverse = universe.max(codomainUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr);
        expr.setWellTyped(myContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = maxUniverse;
    }

    return new Result(codomainResult == null ? Sigma(list.getFirst()) : Pi(list.getFirst(), codomainResult.expression), new UniverseExpression(universe), equations);
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), null, expr), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    if (expectedType == null) {
      updateAppResult(result, expr);
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression expectedType) {
    assert expr.getSequence().isEmpty();
    return typeCheck(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    myErrorReporter.report(error);
    expr.setWellTyped(myContext, Error(null, error));
    return null;
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError("Cannot infer type of the type", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Equations equations = myArgsInference.newEquations();
    LetClause letBinding = let(Abstract.CaseExpression.FUNCTION_NAME, EmptyDependentLink.getInstance(), expectedType, (ElimTreeNode) null);
    Expression letTerm = Reference(letBinding);
    List<? extends Abstract.Expression> expressions = expr.getExpressions();

    LinkList list = new LinkList();
    for (int i = 0; i < expressions.size(); i++) {
      Result exprResult = typeCheck(expressions.get(i), null);
      if (exprResult == null) return null;
      if (!exprResult.equations.isEmpty()) {
        for (DependentLink link = list.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
          exprResult.equations.abstractBinding(link);
        }
        equations.add(exprResult.equations);
      }
      list.append(param(true, vars(Abstract.CaseExpression.ARGUMENT_NAME + i), exprResult.type));
      letTerm = Apps(letTerm, exprResult.expression);
    }
    letBinding.setParameters(list.getFirst());

    TypeCheckingElim.Result elimResult = myTypeCheckingElim.typeCheckElim(expr, list.getFirst(), expectedType, true);
    if (elimResult == null) return null;
    if (!elimResult.equations.isEmpty()) {
      for (DependentLink link = list.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
        elimResult.equations.abstractBinding(link);
      }
      equations.add(elimResult.equations);
    }
    letBinding.setElimTree(elimResult.elimTree);

    LetExpression letExpression = Let(lets(letBinding), letTerm);
    expr.setWellTyped(myContext, letExpression);
    return new Result(letExpression, expectedType, equations);
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (!(type instanceof SigmaExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected an type of a sigma type", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    DependentLink sigmaParams = ((SigmaExpression) type).getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      TypeCheckingError error = new TypeCheckingError("Index " + (expr.getField() + 1) + " out of range", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Substitution substitution = new Substitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, Proj(exprResult.expression, i));
    }
    return checkResult(expectedType, new Result(Proj(exprResult.expression, expr.getField()), fieldLink.getType().subst(substitution), exprResult.equations), expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result result = typeCheck(baseClassExpr, null);
    if (result == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
    if (!(normalizedBaseClassExpr instanceof ClassCallExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", baseClassExpr);
      expr.setWellTyped(myContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = ((ClassCallExpression) normalizedBaseClassExpr).getDefinition();
    if (baseClass.hasErrors()) {
      TypeCheckingError error = new HasErrors(baseClass.getName(), baseClassExpr);
      expr.setWellTyped(myContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    Collection<? extends Abstract.ImplementStatement> statements = expr.getStatements();
    if (statements.isEmpty()) {
      return checkResult(expectedType, new Result(normalizedBaseClassExpr, baseClass.getType(), myArgsInference.newEquations()), expr);
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
      ClassField field = baseClass.removeField(name);
      if (field == null) {
        TypeCheckingError error = new TypeCheckingError("Class '" + baseClass.getName() + "' does not have field '" + name + "'", statement);
        myErrorReporter.report(error);
      } else {
        fields.add(new ImplementStatement(field, statement.getExpression()));
      }
    }

    Equations equations = myArgsInference.newEquations();
    Map<ClassField, ClassCallExpression.ImplementStatement> typeCheckedStatements = new HashMap<>();
    for (int i = 0; i < fields.size(); i++) {
      ImplementStatement field = fields.get(i);
      Expression thisExpr = New(ClassCall(baseClass, typeCheckedStatements));
      Result result1 = typeCheck(field.term, field.classField.getBaseType().subst(field.classField.getThisParameter(), thisExpr));
      baseClass.addField(field.classField);
      if (result1 == null) {
        for (i++; i < fields.size(); i++) {
          typeCheck(fields.get(i).term, fields.get(i).classField.getBaseType().subst(field.classField.getThisParameter(), thisExpr));
          baseClass.addField(fields.get(i).classField);
        }
        return null;
      }

      typeCheckedStatements.put(field.classField, new ClassCallExpression.ImplementStatement(result1.type, result1.expression));
      equations.add(result1.equations);
    }

    ClassCallExpression resultExpr = ClassCall(baseClass, typeCheckedStatements);
    return checkResult(expectedType, new Result(resultExpr, new UniverseExpression(resultExpr.getUniverse()), equations), expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    if (!(normExpr instanceof ClassCallExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", expr.getExpression());
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassCallExpression classCall = (ClassCallExpression) normExpr;
    if (classCall.getImplementStatements().size() == classCall.getDefinition().getFields().size()) {
      return checkResult(expectedType, new Result(New(normExpr), normExpr, exprResult.equations), expr);
    } else {
      TypeCheckingError error = new TypeCheckingError("Class '" + classCall.getDefinition().getName() + "' has " + classCall.getDefinition().getNumberOfVisibleFields() + " fields", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private LetClauseResult typeCheckLetClause(Abstract.LetClause clause) {
    LinkList links = new LinkList();
    Expression resultType;
    ElimTreeNode elimTree;
    Equations equations = myArgsInference.newEquations();

    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument arg : clause.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          Result result = typeCheck(teleArg.getType(), Universe());
          if (result == null) return null;
          if (!result.equations.isEmpty()) {
            for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
              result.equations.abstractBinding(link);
            }
            equations.add(result.equations);
          }
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
        if (!result.equations.isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            result.equations.abstractBinding(link);
          }
          equations.add(result.equations);
        }
        expectedType = result.expression;
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        TypeCheckingElim.Result elimResult = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? links.getFirst() : null, expectedType, false);
        if (elimResult == null)
          return null;
        if (!elimResult.equations.isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            elimResult.equations.abstractBinding(link);
          }
          equations.add(elimResult.equations);
        }
        elimTree = elimResult.elimTree;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        if (!termResult.equations.isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            termResult.equations.abstractBinding(link);
          }
          equations.add(termResult.equations);
        }
        elimTree = new LeafElimTreeNode(clause.getArrow(), termResult.expression);
        resultType = termResult.type;
      }

      TypeCheckingError error = TypeCheckingElim.checkCoverage(clause, links.getFirst(), elimTree);
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

    LetClause result = new LetClause(clause.getName(), links.getFirst(), resultType, elimTree);
    myContext.add(result);
    return new LetClauseResult(result, equations);
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      List<LetClause> clauses = new ArrayList<>();
      Equations equations = myArgsInference.newEquations();
      for (int i = 0; i < expr.getClauses().size(); i++) {
        LetClauseResult clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (clauseResult == null) return null;
        for (Binding binding : clauses) {
          clauseResult.equations.abstractBinding(binding);
        }
        equations.add(clauseResult.equations);
        clauses.add(clauseResult.letClause);
      }
      Result result = typeCheck(expr.getExpression(), expectedType == null ? null : expectedType);
      if (result == null) return null;
      for (Binding binding : clauses) {
        result.equations.abstractBinding(binding);
      }
      equations.add(result.equations);

      return new Result(Let(clauses, result.expression), Let(clauses, result.type).normalize(NormalizeVisitor.Mode.NF), equations);
    }
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression expectedType) {
    int number = expr.getNumber();
    Expression expression = Zero();
    for (int i = 0; i < number; ++i) {
      expression = Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, Nat(), myArgsInference.newEquations()), expr);
  }

}
