package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.NameModuleID;
import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    if (errors >= 0) {
      assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    } else {
      assertFalse(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    }
    return result;
  }

  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(context, resolveNamesExpr(text), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    return typeCheckExpr(context, resolveNamesExpr(text), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, resolveNamesExpr(text), expectedType, 0);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }

  public static Definition typeCheckDef(Concrete.Definition definition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(new HashMap<Abstract.Definition, Definition>(), errorReporter);
    visitor.setNamespaceMember(Root.getModule(new NameModuleID("test")).namespace.getMember(definition.getName()));
    Definition result = definition.accept(visitor, null);
    if (errors >= 0) {
      assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    } else {
      assertFalse(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    }
    return result;
  }

  public static Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  public static Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }

  public static NamespaceMember typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    TypecheckingOrdering.typecheck(classDefinition, errorReporter);
    if (errors >= 0) {
      assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    } else {
      assertFalse(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    }
    NamespaceMember nsMember = Root.getModule(new NameModuleID(classDefinition.getName()));
    return nsMember;
  }

  public static NamespaceMember typeCheckClass(String name, String text, int errors) {
    Concrete.ClassDefinition classDefinition = parseClass(name, text);
    resolveNamesClass(classDefinition, 0);
    return typeCheckClass(classDefinition, errors);
  }

  public static NamespaceMember typeCheckClass(String name, String text) {
    return typeCheckClass(name, text, 0);
  }

  public static NamespaceMember typeCheckClass(String text) {
    return typeCheckClass("test", text, 0);
  }

  public static NamespaceMember typeCheckClass(String text, int errors) {
    return typeCheckClass("test", text, errors);
  }
}
