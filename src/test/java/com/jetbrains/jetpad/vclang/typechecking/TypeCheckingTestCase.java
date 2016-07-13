package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NamespaceUtil;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DefinitionCheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.*;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseClass;
import static com.jetbrains.jetpad.vclang.util.TestUtil.assertErrorListSize;

public class TypeCheckingTestCase {
  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, ErrorReporter errorReporter) {
    return new CheckTypeVisitor.Builder(context, errorReporter).build().checkType(expression, expectedType);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = typeCheckExpr(context, expression, expectedType, errorReporter);
    assertErrorListSize(errorReporter.getErrorList(), errors);
    return result;
  }

  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, 0);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }

  public static Definition typeCheckDef(Concrete.Definition definition, int errors) {
    TypecheckerState state = new TypecheckerState();
    ListErrorReporter errorReporter = new ListErrorReporter();
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(state, errorReporter);
    Definition result = definition.accept(visitor, null);
    assertErrorListSize(errorReporter.getErrorList(), errors);
    return result;
  }

  public static Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  public static Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }

  public static TypecheckerState typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    TypecheckerState state = TypecheckingOrdering.typecheck(classDefinition, errorReporter);
    assertErrorListSize(errorReporter.getErrorList(), errors);
    return state;
  }


  public static class TypeCheckClassResult {
    public final TypecheckerState typecheckerState;
    public final Concrete.ClassDefinition classDefinition;

    public TypeCheckClassResult(TypecheckerState typecheckerState, Concrete.ClassDefinition classDefinition) {
      this.typecheckerState = typecheckerState;
      this.classDefinition = classDefinition;
    }

    public Definition getDefinition(String path) {
      Referable ref = NamespaceUtil.get(classDefinition, path);
      return ref != null ? typecheckerState.getTypechecked(ref) : null;
    }
  }

  public static TypeCheckClassResult typeCheckClass(String text, int nameErrors, int tcErrors) {
    Concrete.ClassDefinition classDefinition = parseClass("text", text);
    resolveNamesClass(classDefinition, nameErrors);
    TypecheckerState state = typeCheckClass(classDefinition, tcErrors);
    return new TypeCheckClassResult(state, classDefinition);
  }

  public static TypeCheckClassResult typeCheckClass(String text, int tcErrors) {
    return typeCheckClass(text, 0, tcErrors);
  }

  public static TypeCheckClassResult typeCheckClass(String text) {
    return typeCheckClass(text, 0, 0);
  }
}
