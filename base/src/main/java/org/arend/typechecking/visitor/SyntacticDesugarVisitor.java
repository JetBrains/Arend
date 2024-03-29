package org.arend.typechecking.visitor;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.naming.binOp.ExpressionBinOpEngine;
import org.arend.naming.reference.*;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.DefinableMetaDefinition;

import java.util.*;
import java.util.stream.Collectors;

public class SyntacticDesugarVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final ErrorReporter myErrorReporter;
  private final boolean myParametersRefsSupported;

  private SyntacticDesugarVisitor(ErrorReporter errorReporter, boolean parametersRefsSupported) {
    myErrorReporter = errorReporter;
    myParametersRefsSupported = parametersRefsSupported;
  }

  public static void desugar(Concrete.ResolvableDefinition definition, ErrorReporter errorReporter) {
    definition.accept(new SyntacticDesugarVisitor(errorReporter, !(definition instanceof DefinableMetaDefinition)), null);
  }

  public static Concrete.Expression desugar(Concrete.Expression expression, ErrorReporter errorReporter) {
    return expression.accept(new SyntacticDesugarVisitor(errorReporter, false), null);
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    if (ref instanceof ParameterReferable && !myParametersRefsSupported) {
      myErrorReporter.report(new TypecheckingError("External variables are not allowed here", expr));
    }
    expr.setReferent(ref);
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertBinOpAppHoles(expr, parameters);
    return !parameters.isEmpty()
            ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
            : ExpressionBinOpEngine.parse(expr, myErrorReporter).accept(this, null);
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
        : super.visitApp(expr, params);
  }

  @Override
  protected void visitPattern(Concrete.Pattern pattern, Void params) {
    super.visitPattern(pattern, params);
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertCaseAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
        : super.visitCase(expr, params);
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertProjAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.expression.getData(), parameters, expr).accept(this, null)
        : super.visitProj(expr, params);
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    List<Concrete.Expression> fields = expr.getFields().stream()
        .map(element -> element instanceof Concrete.ApplyHoleExpression ? createAppHoleRef(parameters, element.getData()) : element)
        .collect(Collectors.toList());
    if (!parameters.isEmpty()) {
      Object data = expr.getData();
      Concrete.TupleExpression tuple = new Concrete.TupleExpression(data, fields);
      return new Concrete.LamExpression(data, parameters, tuple)
          .accept(this, null);
    } else return super.visitTuple(expr, params);
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertNewAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
        : super.visitNew(expr, params);
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertClassExtAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
        : super.visitClassExt(expr, params);
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertPiAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
        : super.visitPi(expr, params);
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    convertSigmaAppHoles(expr, parameters);
    return !parameters.isEmpty()
        ? new Concrete.LamExpression(expr.getData(), parameters, expr).accept(this, null)
        : super.visitSigma(expr, params);
  }

  private static Concrete.ReferenceExpression createAppHoleRef(List<Concrete.Parameter> parameters, Object data) {
    LocalReferable ref = new LocalReferable("p" + parameters.size());
    parameters.add(new Concrete.NameParameter(data, true, ref));
    return new Concrete.ReferenceExpression(data, ref);
  }

  private void convertBinOpAppHoles(Concrete.BinOpSequenceExpression expr, List<Concrete.Parameter> parameters) {
    boolean isLastElemInfix = true;
    for (Concrete.BinOpSequenceElem<Concrete.Expression> elem : expr.getSequence()) {
      if (elem.getComponent() instanceof Concrete.ApplyHoleExpression)
        elem.setComponent(createAppHoleRef(parameters, elem.getComponent().getData()));
      else if (isLastElemInfix) convertRecursively(elem.getComponent(), parameters);
      else if (elem.getComponent() instanceof Concrete.BinOpSequenceExpression)
        elem.setComponent(elem.getComponent().accept(this, null));
      else if (elem.getComponent() instanceof Concrete.SigmaExpression
          || elem.getComponent() instanceof Concrete.PiExpression
          || elem.getComponent() instanceof Concrete.CaseExpression
      ) convertRecursively(elem.getComponent(), parameters);
      isLastElemInfix = elem.isInfixReference();
    }
  }

  private void convertRecursively(Concrete.Expression expression, List<Concrete.Parameter> parameters) {
    if (expression instanceof Concrete.AppExpression)
      convertAppHoles((Concrete.AppExpression) expression, parameters);
    else if (expression instanceof Concrete.ProjExpression)
      convertProjAppHoles((Concrete.ProjExpression) expression, parameters);
    else if (expression instanceof Concrete.BinOpSequenceExpression)
      convertBinOpAppHoles((Concrete.BinOpSequenceExpression) expression, parameters);
    else if (expression instanceof Concrete.ClassExtExpression)
      convertClassExtAppHoles((Concrete.ClassExtExpression) expression, parameters);
    else if (expression instanceof Concrete.NewExpression)
      convertNewAppHoles((Concrete.NewExpression) expression, parameters);
/*
    else if (expression instanceof Concrete.CaseExpression)
      convertCaseAppHoles((Concrete.CaseExpression) expression, parameters);
    else if (expression instanceof Concrete.PiExpression)
      convertPiAppHoles((Concrete.PiExpression) expression, parameters);
    else if (expression instanceof Concrete.SigmaExpression)
      convertSigmaAppHoles((Concrete.SigmaExpression) expression, parameters);
*/
  }

  private void convertAppHoles(
      Concrete.AppExpression expr,
      List<Concrete.Parameter> parameters) {
    Concrete.Expression originalFunc = expr.getFunction();
    if (originalFunc instanceof Concrete.ApplyHoleExpression)
      expr.setFunction(createAppHoleRef(parameters, originalFunc.getData()));
    else if (originalFunc instanceof Concrete.AppExpression)
      convertRecursively(originalFunc, parameters);
    for (Concrete.Argument argument : expr.getArguments())
      if (argument.expression instanceof Concrete.ApplyHoleExpression)
        argument.expression = createAppHoleRef(parameters, argument.expression.getData());
      else if (argument.expression instanceof Concrete.AppExpression)
        convertRecursively(argument.expression, parameters);
  }

  private void convertProjAppHoles(Concrete.ProjExpression proj, List<Concrete.Parameter> parameters) {
    if (proj.expression instanceof Concrete.ApplyHoleExpression)
      proj.expression = createAppHoleRef(parameters, proj.expression.getData());
    else convertRecursively(proj.expression, parameters);
  }

  private void convertNewAppHoles(Concrete.NewExpression expr, List<Concrete.Parameter> parameters) {
    if (expr.expression instanceof Concrete.ApplyHoleExpression)
      expr.expression = createAppHoleRef(parameters, expr.expression.getData());
    else convertRecursively(expr.expression, parameters);
  }

  private void convertClassExtAppHoles(Concrete.ClassExtExpression expr, List<Concrete.Parameter> parameters) {
    Concrete.Expression baseClassExpression = expr.getBaseClassExpression();
    if (baseClassExpression instanceof Concrete.ApplyHoleExpression)
      expr.setBaseClassExpression(createAppHoleRef(parameters, baseClassExpression.getData()));
    else convertRecursively(baseClassExpression, parameters);
    for (Concrete.ClassFieldImpl statement : expr.getStatements())
      if (statement.implementation instanceof Concrete.ApplyHoleExpression)
        statement.implementation = createAppHoleRef(parameters, statement.getData());
  }

  private void convertCaseAppHoles(Concrete.CaseExpression expr, List<Concrete.Parameter> parameters) {
    for (Concrete.CaseArgument argument : expr.getArguments())
      if (argument.expression instanceof Concrete.ApplyHoleExpression)
        argument.expression = createAppHoleRef(parameters, argument.expression.getData());
      else convertRecursively(argument.expression, parameters);
  }

  private void convertPiAppHoles(Concrete.PiExpression expr, List<Concrete.Parameter> parameters) {
    for (Concrete.TypeParameter parameter : expr.getParameters())
      convertParameterAppHoles(parameter, parameters);
    if (expr.codomain instanceof Concrete.ApplyHoleExpression)
      expr.codomain = createAppHoleRef(parameters, expr.codomain.getData());
    else convertRecursively(expr.codomain, parameters);
  }

  private void convertSigmaAppHoles(Concrete.SigmaExpression expr, List<Concrete.Parameter> parameters) {
    for (Concrete.TypeParameter parameter : expr.getParameters())
      convertParameterAppHoles(parameter, parameters);
  }

  private void convertParameterAppHoles(Concrete.TypeParameter parameter, List<Concrete.Parameter> parameters) {
    if (parameter.type instanceof Concrete.ApplyHoleExpression)
      parameter.type = createAppHoleRef(parameters, parameter.type.getData());
    else convertRecursively(parameter.type, parameters);
  }
}
