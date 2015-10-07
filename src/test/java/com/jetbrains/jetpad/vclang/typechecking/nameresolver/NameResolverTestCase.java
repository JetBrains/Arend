package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;

import java.util.ArrayList;
import java.util.Collection;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static org.junit.Assert.assertEquals;

public class NameResolverTestCase {
  public static Collection<? extends GeneralError> resolveNamesExpr(Concrete.Expression expression, NameResolver nameResolver) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    expression.accept(new ResolveNameVisitor(errorReporter, nameResolver, new ArrayList<String>(0), true), null);
    return errorReporter.getErrorList();
  }

  public static Collection<? extends GeneralError> resolveNamesExpr(Concrete.Expression expression) {
    return resolveNamesExpr(expression, DummyNameResolver.getInstance());
  }

  public static Concrete.Expression resolveNamesExpr(String text, int errors, NameResolver nameResolver) {
    Concrete.Expression result = parseExpr(text);
    Collection<? extends GeneralError> errorList = resolveNamesExpr(result, nameResolver);
    assertEquals(errorList.toString(), errors, errorList.size());
    return result;
  }

  public static Concrete.Expression resolveNamesExpr(String text, int errors) {
    Concrete.Expression result = parseExpr(text);
    Collection<? extends GeneralError> errorList = resolveNamesExpr(result);
    assertEquals(errorList.toString(), errors, errorList.size());
    return result;
  }

  public static Concrete.Expression resolveNamesExpr(String text, NameResolver nameResolver) {
    return resolveNamesExpr(text, 0, nameResolver);
  }

  public static Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(text, 0);
  }

  public static Collection<? extends GeneralError> resolveNamesDef(Concrete.Definition definition) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    definition.accept(new DefinitionResolveNameVisitor(errorReporter, RootModule.ROOT.getChild(new Name("test")), DummyNameResolver.getInstance()), null);
    return errorReporter.getErrorList();
  }

  public static Concrete.Definition resolveNamesDef(String text, int errors) {
    Concrete.Definition result = parseDef(text);
    Collection<? extends GeneralError> errorList = resolveNamesDef(result);
    assertEquals(errorList.toString(), errors, errorList.size());
    return result;
  }

  public static Concrete.Definition resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }

  public static Namespace resolveNamesClass(Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    Namespace localNamespace = new DefinitionResolveNameVisitor(errorReporter, RootModule.ROOT, DummyNameResolver.getInstance()).visitClass(classDefinition, null);
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return localNamespace;
  }

  public static Namespace resolveNamesClass(String name, String text, int errors) {
    return resolveNamesClass(parseClass(name, text), errors);
  }

  public static Namespace resolveNamesClass(String name, String text) {
    return resolveNamesClass(name, text, 0);
  }
}
