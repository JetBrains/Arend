package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;

public class OneshotNameResolver {
  public static void visitModule(Abstract.ClassDefinition module, Scope globalScope, NameResolver nameResolver, ParserInfoProvider definitionProvider, ResolveListener resolveListener, ErrorReporter errorReporter) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(nameResolver, definitionProvider, resolveListener, errorReporter);
    visitor.visitClass(module, globalScope);
  }
}
