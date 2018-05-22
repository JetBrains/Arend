package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.reference.TypeClassReferenceExtractVisitor;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.error.ReferenceError;
import com.jetbrains.jetpad.vclang.naming.error.WrongReferable;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
import com.jetbrains.jetpad.vclang.naming.resolving.NameClashesChecker;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor implements ConcreteDefinitionVisitor<Scope, Void> {
  private boolean myResolveTypeClassReferences;
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter) {
    myResolveTypeClassReferences = false;
    myErrorReporter = errorReporter;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  private static void resolveTypeClassReference(List<Concrete.Parameter> parameters, Concrete.Expression type, Scope scope) {
    Referable ref = TypeClassReferenceExtractVisitor.getTypeReference(parameters, type);
    if (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    if (ref instanceof UnresolvedReference) {
      if (!parameters.isEmpty() || type instanceof Concrete.PiExpression) {
        ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, new ArrayList<>(), DummyErrorReporter.INSTANCE);
        exprVisitor.visitParameters(parameters);
        while (type instanceof Concrete.PiExpression) {
          exprVisitor.visitTypeParameters(((Concrete.PiExpression) type).getParameters());
          type = ((Concrete.PiExpression) type).getCodomain();
        }
        scope = exprVisitor.getScope();
      }
      ((UnresolvedReference) ref).resolve(scope);
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Scope scope) {
    if (myResolveTypeClassReferences) {
      resolveTypeClassReference(def.getParameters(), def.getResultType(), scope);
      return null;
    }

    Concrete.FunctionBody body = def.getBody();
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitParameters(def.getParameters());

    Concrete.Expression resultType = def.getResultType();
    if (resultType != null) {
      def.setResultType(resultType.accept(exprVisitor, null));
    }

    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).setTerm(((Concrete.TermFunctionBody) body).getTerm().accept(exprVisitor, null));
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      visitEliminatedReferences(exprVisitor, ((Concrete.ElimFunctionBody) body).getEliminatedReferences(), def.getData());
    }

    if (body instanceof Concrete.ElimFunctionBody) {
      context.clear();
      addNotEliminatedParameters(def.getParameters(), ((Concrete.ElimFunctionBody) body).getEliminatedReferences(), context);
      exprVisitor.visitClauses(((Concrete.ElimFunctionBody) body).getClauses());
    }

    return null;
  }

  private void visitEliminatedReferences(ExpressionResolveNameVisitor exprVisitor, List<? extends Concrete.ReferenceExpression> eliminatedReferences, GlobalReferable definition) {
    for (int i = 0; i < eliminatedReferences.size(); i++) {
      Concrete.Expression newExpr = exprVisitor.visitReference(eliminatedReferences.get(i), null);
      if (newExpr != eliminatedReferences.get(i)) {
        myErrorReporter.report(new ProxyError(definition, new ReferenceError("\\elim can be applied only to a local variable", definition)));
        eliminatedReferences.remove(i--);
      }
    }
  }

  private void addNotEliminatedParameters(List<? extends Concrete.Parameter> parameters, List<? extends Concrete.ReferenceExpression> eliminated, List<Referable> context) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Referable> referables = eliminated.stream().map(Concrete.ReferenceExpression::getReferent).collect(Collectors.toSet());
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        ClassReferable classRef = ExpressionResolveNameVisitor.getTypeClassReference(((Concrete.TelescopeParameter) parameter).getType());
        for (Referable referable : ((Concrete.TelescopeParameter) parameter).getReferableList()) {
          if (referable != null && !referable.textRepresentation().equals("_") && !referables.contains(referable)) {
            context.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
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
    if (myResolveTypeClassReferences) {
      return null;
    }

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitTypeParameters(def.getParameters());
    if (def.getEliminatedReferences() != null) {
      visitEliminatedReferences(exprVisitor, def.getEliminatedReferences(), def.getData());
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
      exprVisitor.visitTypeParameters(def.getParameters());
      visitEliminatedReferences(exprVisitor, def.getEliminatedReferences(), def.getData());
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
    if (myResolveTypeClassReferences) {
      for (Concrete.ClassField field : def.getFields()) {
        resolveTypeClassReference(Collections.emptyList(), field.getResultType(), scope);
      }
      return null;
    }

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, context, new ProxyErrorReporter(def.getData(), myErrorReporter));
    visitSuperClasses(exprVisitor, def.getSuperClasses(), def.getData());

    for (Concrete.ClassField field : def.getFields()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
        Concrete.Expression resultType = field.getResultType().accept(exprVisitor, null);
        if (resultType != field.getResultType()) {
          field.setResultType(resultType);
        }
      }
    }
    exprVisitor.visitClassFieldImpls(def.getImplementations(), def.getData());

    return null;
  }

  private boolean visitClassReference(ExpressionResolveNameVisitor exprVisitor, Concrete.ReferenceExpression classRef, GlobalReferable definition) {
    Concrete.Expression newClassRef = exprVisitor.visitReference(classRef, null);
    if (newClassRef != classRef || !(classRef.getReferent() instanceof ClassReferable)) {
      myErrorReporter.report(new ProxyError(definition, new WrongReferable("Expected a reference to a class", classRef.getReferent(), classRef)));
      return false;
    } else {
      return true;
    }
  }

  private void visitSuperClasses(ExpressionResolveNameVisitor exprVisitor, List<Concrete.ReferenceExpression> superClasses, GlobalReferable definition) {
    for (int i = 0; i < superClasses.size(); i++) {
      if (!visitClassReference(exprVisitor, superClasses.get(i), definition)) {
        superClasses.remove(i--);
      }
    }
  }

  @Override
  public Void visitClassSynonym(Concrete.ClassSynonym def, Scope parentScope) {
    if (myResolveTypeClassReferences) {
      return null;
    }

    LocalErrorReporter localErrorReporter = new ProxyErrorReporter(def.getData(), myErrorReporter);
    ExpressionResolveNameVisitor visitor = new ExpressionResolveNameVisitor(parentScope, Collections.emptyList(), localErrorReporter);
    visitSuperClasses(visitor, def.getSuperClasses(), def.getData());

    if (visitClassReference(visitor, def.getUnderlyingClass(), def.getData())) {
      if (!def.getFields().isEmpty()) {
        visitor = new ExpressionResolveNameVisitor(new ClassFieldImplScope((ClassReferable) def.getUnderlyingClass().getReferent(), false), Collections.emptyList(), localErrorReporter);
        for (Concrete.ClassFieldSynonym fieldSyn : def.getFields()) {
          visitor.visitReference(fieldSyn.getUnderlyingField(), null);
        }
      }
    } else {
      myErrorReporter.report(new ProxyError(def.getData(), new WrongReferable("Expected a class", def.getUnderlyingClass().getReferent(), def)));
      def.getFields().clear();
    }

    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Scope parentScope) {
    if (myResolveTypeClassReferences) {
      resolveTypeClassReference(def.getParameters(), def.getClassReference(), parentScope);
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, new ArrayList<>(), new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitParameters(def.getParameters());

    if (visitClassReference(exprVisitor, def.getClassReference(), def.getData())) {
      exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (ClassReferable) def.getClassReference().getReferent());
    } else {
      myErrorReporter.report(new ProxyError(def.getData(), new WrongReferable("Expected a reference to a class", def.getClassReference().getReferent(), def.getClassReference())));
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

  public void resolveGroupWithTypes(Group group, ReferableConverter referableConverter, Scope scope, ConcreteProvider concreteProvider) {
    myResolveTypeClassReferences = true;
    resolveGroup(group, referableConverter, scope, concreteProvider);
    myResolveTypeClassReferences = false;
    resolveGroup(group, referableConverter, scope, concreteProvider);
  }

  public void resolveGroup(Group group, ReferableConverter referableConverter, Scope scope, ConcreteProvider concreteProvider) {
    Concrete.ReferableDefinition def = concreteProvider.getConcrete(group.getReferable());
    Scope convertedScope = referableConverter == null ? scope : CachingScope.make(new ConvertingScope(referableConverter, scope));
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition) def).accept(this, convertedScope);
    }

    for (Group subgroup : group.getSubgroups()) {
      resolveGroup(subgroup, referableConverter, makeScope(subgroup, scope), concreteProvider);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resolveGroup(subgroup, referableConverter, makeScope(subgroup, scope), concreteProvider);
    }

    if (myResolveTypeClassReferences) {
      return;
    }

    for (NamespaceCommand namespaceCommand : group.getNamespaceCommands()) {
      LongUnresolvedReference reference = new LongUnresolvedReference(namespaceCommand, namespaceCommand.getPath());
      Scope curScope = reference.resolveNamespace(convertedScope);
      if (curScope == null) {
        myErrorReporter.report(new ProxyError(group.getReferable(), reference.getErrorReference().getError()));
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

        curScope = NamespaceCommandNamespace.makeNamespace(curScope, new NamespaceCommand() {
          @Nonnull
          @Override
          public Kind getKind() {
                                return namespaceCommand.getKind();
                                                                  }

          @Nonnull
          @Override
          public List<String> getPath() {
                                        return namespaceCommand.getPath();
                                                                          }

          @Override
          public boolean isUsing() {
                                   return namespaceCommand.isUsing();
                                                                     }

          @Nonnull
          @Override
          public Collection<? extends NameRenaming> getOpenedReferences() {
            return namespaceCommand.getOpenedReferences();
          }

          @Nonnull
          @Override
          public Collection<? extends Referable> getHiddenReferences() {
                                                                       return Collections.emptyList();
                                                                                                      }
        });

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

    new NameClashesChecker() {
      @Override
      public void definitionNamesClash(LocatedReferable ref1, LocatedReferable ref2, Error.Level level) {
        myErrorReporter.report(new ProxyError(group.getReferable(), new DuplicateNameError(level, ref2, ref1)));
      }

      @Override
      public void namespacesClash(NamespaceCommand cmd1, NamespaceCommand cmd2, String name, Error.Level level) {
        myErrorReporter.report(new ProxyError(group.getReferable(), new LocalError(level, "Definition '" + name + "' is imported from modules " + new LongName(cmd1.getPath()) + " and " + new LongName(cmd2.getPath()))));
      }

      @Override
      public void namespaceDefinitionNameClash(NameRenaming renaming, LocatedReferable ref, Error.Level level) {
        myErrorReporter.report(new ProxyError(group.getReferable(), new LocalError(level, "Definition '" + ref.textRepresentation() + "' is not imported since it is defined in this module")));
      }
    }.checkGroup(group, convertedScope);
  }
}
