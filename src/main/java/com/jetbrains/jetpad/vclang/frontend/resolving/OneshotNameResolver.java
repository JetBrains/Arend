package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.function.Function;

public class OneshotNameResolver {
  public static void visitModule(Abstract.ClassDefinition module, Scope globalScope, NameResolver nameResolver, Function<Abstract.Definition, Iterable<OpenCommand>> opens, ResolveListener resolveListener, ErrorReporter errorReporter) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(nameResolver, opens, resolveListener, errorReporter);
    visitor.visitClass(module, globalScope);
  }
}
