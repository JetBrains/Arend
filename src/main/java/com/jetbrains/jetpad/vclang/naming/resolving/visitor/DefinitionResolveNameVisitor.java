package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongReferable;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor implements ConcreteDefinitionVisitor<Scope, Void> {
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Scope scope) {
    Concrete.FunctionBody body = def.getBody();
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitParameters(def.getParameters());

    Concrete.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(exprVisitor, null);
    }

    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).getTerm().accept(exprVisitor, null);
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      for (Concrete.ReferenceExpression expression : ((Concrete.ElimFunctionBody) body).getEliminatedReferences()) {
        exprVisitor.visitReference(expression, null);
      }
    }

    if (body instanceof Concrete.ElimFunctionBody) {
      context.clear();
      addNotEliminatedParameters(def.getParameters(), ((Concrete.ElimFunctionBody) body).getEliminatedReferences(), context);
      exprVisitor.visitClauses(((Concrete.ElimFunctionBody) body).getClauses());
    }

    return null;
  }

  private void addNotEliminatedParameters(List<? extends Concrete.Parameter> parameters, List<? extends Concrete.ReferenceExpression> eliminated, List<Referable> context) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Referable> referables = eliminated.stream().map(Concrete.ReferenceExpression::getReferent).collect(Collectors.toSet());
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        for (Referable referable : ((Concrete.TelescopeParameter) parameter).getReferableList()) {
          if (referable != null && !referable.textRepresentation().equals("_") && !referables.contains(referable)) {
            context.add(referable);
          }
        }
      } else if (parameter instanceof Concrete.NameParameter) {
        Referable referable = ((Concrete.NameParameter) parameter).getReferable();
        if (referable != null && !referable.textRepresentation().equals("_") && !referables.contains(referable)) {
          context.add(referable);
        }
      }
    }
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Scope scope) {
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitParameters(def.getParameters());
    if (def.getUniverse() != null) {
      def.getUniverse().accept(exprVisitor, null);
    }
    if (def.getEliminatedReferences() != null) {
      for (Concrete.ReferenceExpression ref : def.getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
      }
    } else {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitConstructor(constructor, scope, context);
        }
      }
    }

    if (def.getEliminatedReferences() != null) {
      context.clear();
      addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences(), context);
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
          visitConstructorClause(clause, exprVisitor);
          for (Concrete.Constructor constructor : clause.getConstructors()) {
            visitConstructor(constructor, scope, context);
          }
        }
      }
    }

    return null;
  }

  private void visitConstructor(Concrete.Constructor def, Scope parentScope, List<Referable> context) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      exprVisitor.visitParameters(def.getParameters());
      for (Concrete.ReferenceExpression ref : def.getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
      }
    }

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences(), context);
      exprVisitor.visitClauses(def.getClauses());
    }
  }

  private void visitConstructorClause(Concrete.ConstructorClause clause, ExpressionResolveNameVisitor exprVisitor) {
    List<? extends Concrete.Pattern> patterns = clause.getPatterns();
    if (patterns != null) {
      for (int i = 0; i < patterns.size(); i++) {
        Referable constructor = exprVisitor.visitPattern(patterns.get(i), new HashMap<>());
        if (constructor != null) {
          ExpressionResolveNameVisitor.replaceWithConstructor(clause, i, constructor);
        }
        exprVisitor.resolvePattern(patterns.get(i));
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Scope scope) {
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      exprVisitor.visitReference(superClass, null);
    }

    for (Concrete.ClassField field : def.getFields()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
        field.getResultType().accept(exprVisitor, null);
      }
    }
    exprVisitor.visitClassFieldImpls(def.getImplementations(), def.getData());

    return null;
  }

  @Override
  public Void visitClassSynonym(Concrete.ClassSynonym def, Scope parentScope) {
    LocalErrorReporter localErrorReporter = new ProxyErrorReporter(def.getData(), myErrorReporter);
    ExpressionResolveNameVisitor visitor = new ExpressionResolveNameVisitor(parentScope, Collections.emptyList(), localErrorReporter);
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      visitor.visitReference(superClass, null);
    }
    visitor.visitReference(def.getUnderlyingClass(), null);

    if (def.getUnderlyingClass().getReferent() instanceof ClassReferable) {
      if (!def.getFields().isEmpty()) {
        visitor = new ExpressionResolveNameVisitor(new ClassFieldImplScope((ClassReferable) def.getUnderlyingClass().getReferent()), Collections.emptyList(), localErrorReporter);
        for (Concrete.ClassFieldSynonym fieldSyn : def.getFields()) {
          visitor.visitReference(fieldSyn.getUnderlyingField(), null);
        }
      }
    } else {
      myErrorReporter.report(new ProxyError(def.getData(), new WrongReferable("Expected a class view", def.getUnderlyingClass().getReferent(), def)));
      def.getFields().clear();
    }

    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Scope parentScope) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, new ArrayList<>(), new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitParameters(def.getParameters());
    exprVisitor.visitReference(def.getClassReference(), null);
    if (def.getClassReference().getReferent() instanceof ClassReferable) {
      exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (ClassReferable) def.getClassReference().getReferent());
    } else {
      myErrorReporter.report(new ProxyError(def.getData(), new WrongReferable("Expected a class view", def.getClassReference().getReferent(), def)));
      def.getClassFieldImpls().clear();
    }

    return null;
  }

  private static Scope makeScope(Group group, Scope parentScope) {
    if (group.getNamespaceCommands().isEmpty()) {
      return new MergeScope(LexicalScope.insideOf(group, EmptyScope.INSTANCE), parentScope);
    } else {
      return LexicalScope.insideOf(group, parentScope);
    }
  }

  public void resolveGroup(Group group, ReferableConverter referableConverter, Scope scope, ConcreteProvider concreteProvider) {
    Concrete.ReferableDefinition def = concreteProvider.getConcrete(group.getReferable());

    if (def instanceof Concrete.Definition || !group.getNamespaceCommands().isEmpty()) {
      Scope convertedScope = referableConverter == null ? scope : CachingScope.make(new ConvertingScope(referableConverter, scope));
      if (def instanceof Concrete.Definition) {
        ((Concrete.Definition) def).accept(this, convertedScope);
      }

      for (NamespaceCommand namespaceCommand : group.getNamespaceCommands()) {
        List<String> path = namespaceCommand.getPath();
        Scope curScope = convertedScope;
        for (int i = 0; i < path.size(); i++) {
          curScope = curScope.resolveNamespace(path.get(i), true);
          if (curScope == null) {
            myErrorReporter.report(new ProxyError(group.getReferable(), new NotInScopeError(namespaceCommand, i == 0 ? null : new LongUnresolvedReference(namespaceCommand, path.subList(0, i)), path.get(i))));
            break;
          }
        }

        if (curScope != null) {
          for (NameRenaming renaming : namespaceCommand.getOpenedReferences()) {
            Referable ref = renaming.getOldReference();
            if (ref instanceof UnresolvedReference) {
              ref = ((UnresolvedReference) ref).resolve(curScope);
            }
            if (ref instanceof ErrorReference) {
              myErrorReporter.report(new ProxyError(group.getReferable(), ((ErrorReference) ref).getError()));
            }
          }
          for (Referable ref : namespaceCommand.getHiddenReferences()) {
            if (ref instanceof UnresolvedReference) {
              ref = ((UnresolvedReference) ref).resolve(curScope);
            }
            if (ref instanceof ErrorReference) {
              myErrorReporter.report(new ProxyError(group.getReferable(), ((ErrorReference) ref).getError()));
            }
          }
        }
      }
    }

    for (Group subgroup : group.getSubgroups()) {
      resolveGroup(subgroup, referableConverter, makeScope(subgroup, scope), concreteProvider);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resolveGroup(subgroup, referableConverter, makeScope(subgroup, scope), concreteProvider);
    }
  }
}
