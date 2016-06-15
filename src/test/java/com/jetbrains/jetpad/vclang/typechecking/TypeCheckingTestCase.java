package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.NamespaceUtil;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.*;
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
    TypecheckerState state = new TypecheckerState();
    ListErrorReporter errorReporter = new ListErrorReporter();
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(state, errorReporter);
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

  public static TypecheckerState typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    TypecheckerState state = TypecheckingOrdering.typecheck(classDefinition, errorReporter);
    if (errors >= 0) {
      assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    } else {
      assertFalse(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    }
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

  public static TypeCheckClassResult typeCheckClass(String name, String text, int errors) {
    Concrete.ClassDefinition classDefinition = parseClass(name, text);
    resolveNamesClass(classDefinition, 0);
    TypecheckerState state = typeCheckClass(classDefinition, errors);
    return new TypeCheckClassResult(state, classDefinition);
  }

  public static TypeCheckClassResult typeCheckClass(String name, String text) {
    return typeCheckClass(name, text, 0);
  }

  public static TypeCheckClassResult typeCheckClass(String text) {
    return typeCheckClass("test", text, 0);
  }

  public static TypeCheckClassResult typeCheckClass(String text, int errors) {
    return typeCheckClass("test", text, errors);
  }
}
