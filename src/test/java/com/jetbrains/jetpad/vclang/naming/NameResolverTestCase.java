package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.naming.scope.SubScope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class NameResolverTestCase extends VclangTestCase {
  private void _resolveNamesExpr(Scope parentScope, List<String> context, Concrete.Expression expression) {
    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, nameResolver, errorReporter, new ConcreteResolveListener()), null);
  }

  private void _resolveNamesExpr(Concrete.Expression expression) {
    _resolveNamesExpr(globalScope, new ArrayList<String>(), expression);
  }


  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<String> context, String text, int errors) {
    Concrete.Expression result = parseExpr(text);
    _resolveNamesExpr(parentScope, context, result);
    assertThat(errorList, hasSize(errors));
    return result;
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<String>(), text, errors);
  }

  private Concrete.Expression resolveNamesExpr(String text, int errors) {
    return resolveNamesExpr(globalScope, text, errors);
  }

  Concrete.Expression resolveNamesExpr(Scope parentScope, String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(List<Binding> context, String text) {
    List<String> names = new ArrayList<>(context.size());
    for (Binding binding : context) {
      names.add(binding.getName());
    }
    return resolveNamesExpr(globalScope, names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(text, 0);
  }


  private void _resolveNamesDef(Concrete.Definition definition) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(staticNsProvider, dynamicNsProvider,
        new SubScope(globalScope, new SimpleNamespace(definition)), nameResolver, errorReporter, new ConcreteResolveListener());
    definition.accept(visitor, null);
  }

  private void resolveNamesDef(Concrete.Definition definition, int errors) {
    _resolveNamesDef(definition);
    assertThat(errorList, hasSize(errors));
  }

  Concrete.Definition resolveNamesDef(String text, int errors) {
    Concrete.Definition result = parseDef(text);
    resolveNamesDef(result, errors);
    return result;
  }

  protected Concrete.Definition resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  protected void resolveNamesClass(Concrete.ClassDefinition classDefinition, int errors) {
    resolveNamesDef(classDefinition, errors);
  }

  protected Concrete.ClassDefinition resolveNamesClass(String name, String text, int errors) {
    Concrete.ClassDefinition classDefinition = parseClass(name, text);
    resolveNamesClass(classDefinition, errors);
    return classDefinition;
  }

  protected Concrete.ClassDefinition resolveNamesClass(String text, int errors) {
    return resolveNamesClass("test", text, errors);
  }

  protected Concrete.ClassDefinition resolveNamesClass(String name, String text) {
    return resolveNamesClass(name, text, 0);
  }


  public static Abstract.Definition get(Abstract.Definition ref, String path) {
    for (String n : path.split("\\.")) {
      Abstract.Definition oldref = ref;

      ref = SimpleStaticNamespaceProvider.INSTANCE.forDefinition(oldref).resolveName(n);
      if (ref != null) continue;

      if (oldref instanceof Abstract.ClassDefinition) {
        ref = SimpleDynamicNamespaceProvider.INSTANCE.forClass((Abstract.ClassDefinition) oldref).resolveName(n);
      } else if (oldref instanceof ClassDefinition) {
        ref = ((ClassDefinition) oldref).getInstanceNamespace().resolveName(n);
      }
      if (ref == null) return null;
    }
    return ref;
  }

}
