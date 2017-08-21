package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;

public class OneshotNameResolver {
  public static void visitModule(Concrete.ClassDefinition<Position> module, Scope globalScope, NameResolver nameResolver, ParserInfoProvider definitionProvider, ErrorReporter<Position> errorReporter) {
    DefinitionResolveNameVisitor<Position> visitor = new DefinitionResolveNameVisitor<>(nameResolver, definitionProvider, errorReporter);
    visitor.visitClass(module, globalScope);
  }
}
