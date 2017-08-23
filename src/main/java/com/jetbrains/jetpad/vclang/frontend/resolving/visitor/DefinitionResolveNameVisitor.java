package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.HasOpens;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.*;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.DataScope;
import com.jetbrains.jetpad.vclang.naming.scope.DynamicClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.FunctionScope;
import com.jetbrains.jetpad.vclang.naming.scope.StaticClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefinitionResolveNameVisitor<T> implements ConcreteDefinitionVisitor<T, Scope, Void> {
  private final List<Referable> myContext;
  private final NameResolver myNameResolver;
  private final ParserInfoProvider myInfoProvider;
  private final ErrorReporter<T> myErrorReporter;

  public DefinitionResolveNameVisitor(NameResolver nameResolver, ParserInfoProvider definitionProvider, ErrorReporter<T> errorReporter) {
    this(new ArrayList<>(), nameResolver, definitionProvider, errorReporter);
  }

  private DefinitionResolveNameVisitor(List<Referable> context, NameResolver nameResolver, ParserInfoProvider infoProvider, ErrorReporter<T> errorReporter) {
    myContext = context;
    myNameResolver = nameResolver;
    myInfoProvider = infoProvider;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, Scope parentScope) {
    Iterable<Scope> extraScopes = getExtraScopes(def, new OverridingScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(def))));
    FunctionScope scope = new FunctionScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(def)), extraScopes);
    scope.findIntroducedDuplicateNames(this::warnDuplicate);

    for (Concrete.Definition<T> definition : def.getGlobalDefinitions()) {
      definition.accept(this, scope);
    }

    Concrete.FunctionBody<T> body = def.getBody();
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(scope, myContext, myNameResolver, myInfoProvider, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitParameters(def.getParameters());

      Concrete.Expression<T> resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(exprVisitor, null);
      }

      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody<T>) body).getTerm().accept(exprVisitor, null);
      }
      if (body instanceof Concrete.ElimFunctionBody) {
        for (Concrete.ReferenceExpression<T> expression : ((Concrete.ElimFunctionBody<T>) body).getEliminatedReferences()) {
          exprVisitor.visitReference(expression, null);
        }
      }
    }

    if (body instanceof Concrete.ElimFunctionBody) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        addNotEliminatedParameters(def.getParameters(), ((Concrete.ElimFunctionBody<T>) body).getEliminatedReferences());
        exprVisitor.visitClauses(((Concrete.ElimFunctionBody<T>) body).getClauses());
      }
    }

    return null;
  }

  private void addNotEliminatedParameters(List<? extends Concrete.Parameter<T>> parameters, List<? extends Concrete.ReferenceExpression> eliminated) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Referable> referables = eliminated.stream().map(Concrete.ReferenceExpression::getReferent).collect(Collectors.toSet());
    for (Concrete.Parameter<T> parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        for (Referable referable : ((Concrete.TelescopeParameter<T>) parameter).getReferableList()) {
          if (referable != null && referable.getName() != null && !referable.getName().equals("_") && !referables.contains(referable)) {
            myContext.add(referable);
          }
        }
      } else if (parameter instanceof Concrete.NameParameter) {
        Referable referable = ((Concrete.NameParameter) parameter).getReferable();
        if (referable != null && referable.getName() != null && !referable.getName().equals("_") && !referables.contains(referable)) {
          myContext.add(referable);
        }
      }
    }
  }

  @Override
  public Void visitData(Concrete.DataDefinition<T> def, Scope parentScope) {
    Scope scope = new DataScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(def)));
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(scope, myContext, myNameResolver, myInfoProvider, myErrorReporter);
    try (Utils.CompleteContextSaver<Referable> ignored = new Utils.CompleteContextSaver<>(myContext)) {
      exprVisitor.visitParameters(def.getParameters());
      if (def.getUniverse() != null) {
        def.getUniverse().accept(exprVisitor, null);
      }
      if (def.getEliminatedReferences() != null) {
        for (Concrete.ReferenceExpression<T> ref : def.getEliminatedReferences()) {
          exprVisitor.visitReference(ref, null);
        }
      } else {
        for (Concrete.ConstructorClause<T> clause : def.getConstructorClauses()) {
          for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
            visitConstructor(constructor, scope);
          }
        }
      }
    }

    if (def.getEliminatedReferences() != null) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences());
        for (Concrete.ConstructorClause<T> clause : def.getConstructorClauses()) {
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            visitConstructorClause(clause, exprVisitor);
            for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
              visitConstructor(constructor, scope);
            }
          }
        }
      }
    }

    return null;
  }

  private void visitConstructor(Concrete.Constructor<T> def, Scope parentScope) {
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(parentScope, myContext, myNameResolver, myInfoProvider, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitParameters(def.getParameters());
      for (Concrete.ReferenceExpression<T> ref : def.getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
      }
    }

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences());
      exprVisitor.visitClauses(def.getClauses());
    }
  }

  private void visitConstructorClause(Concrete.ConstructorClause<T> clause, ExpressionResolveNameVisitor<T> exprVisitor) {
    List<? extends Concrete.Pattern<T>> patterns = clause.getPatterns();
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
  public Void visitClass(Concrete.ClassDefinition<T> def, Scope parentScope) {
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(parentScope, myContext, myNameResolver, myInfoProvider, myErrorReporter);
    for (Concrete.ReferenceExpression<T> superClass : def.getSuperClasses()) {
      exprVisitor.visitReference(superClass, null);
    }

    try {
      Iterable<Scope> extraScopes = getExtraScopes(def, new OverridingScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(def))));
      StaticClassScope staticScope = new StaticClassScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(def)), extraScopes);
      staticScope.findIntroducedDuplicateNames(this::warnDuplicate);

      for (Concrete.Definition<T> definition : def.getGlobalDefinitions()) {
        definition.accept(this, staticScope);
      }

      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        for (Concrete.TypeParameter<T> polyParam : def.getPolyParameters()) {
          polyParam.getType().accept(exprVisitor, null);
          if (polyParam instanceof Concrete.TelescopeParameter) {
            for (Referable referable : ((Concrete.TelescopeParameter<T>) polyParam).getReferableList()) {
              if (referable != null && referable.getName() != null && referable.getName().equals("_")) {
                myContext.add(referable);
              }
            }
          }
        }

        DynamicClassScope dynamicScope = new DynamicClassScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(def)), new NamespaceScope(myNameResolver.nsProviders.dynamics.forReferable(def)), extraScopes);
        dynamicScope.findIntroducedDuplicateNames(this::warnDuplicate);

        if (!def.getFields().isEmpty()) {
          ExpressionResolveNameVisitor<T> visitor = new ExpressionResolveNameVisitor<>(dynamicScope, myContext, myNameResolver, myInfoProvider, myErrorReporter);
          for (Concrete.ClassField<T> field : def.getFields()) {
            try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
              field.getResultType().accept(visitor, null);
            }
          }
        }
        if (!def.getImplementations().isEmpty()) {
          new ExpressionResolveNameVisitor<>(dynamicScope, myContext, myNameResolver, myInfoProvider, myErrorReporter).visitClassFieldImpls(def.getImplementations(), def);
        }
        for (Concrete.Definition<T> definition : def.getInstanceDefinitions()) {
          definition.accept(this, dynamicScope);
        }
      }
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView<T> def, Scope parentScope) {
    new ExpressionResolveNameVisitor<>(parentScope, myContext, myNameResolver, myInfoProvider, myErrorReporter).visitReference(def.getUnderlyingClass(), null);
    if (def.getUnderlyingClass().getExpression() != null || !(def.getUnderlyingClass().getReferent() instanceof GlobalReferable)) {
      if (!(def.getUnderlyingClass().getReferent() instanceof UnresolvedReference)) {
        myErrorReporter.report(new WrongReferable<>("Expected a class", def.getUnderlyingClass().getReferent(), def));
      }
      return null;
    }

    GlobalReferable underlyingClass = (GlobalReferable) def.getUnderlyingClass().getReferent();
    Referable classifyingField = def.getClassifyingField();
    if (classifyingField instanceof UnresolvedReference) {
      Namespace dynamicNamespace = myNameResolver.nsProviders.dynamics.forReferable(underlyingClass);
      GlobalReferable resolvedClassifyingField = dynamicNamespace.resolveName(classifyingField.getName());
      if (resolvedClassifyingField == null) {
        myErrorReporter.report(new NotInScopeError<>(classifyingField.getName(), def));
        return null;
      }
      def.setClassifyingField(resolvedClassifyingField);
    }

    for (Concrete.ClassViewField<T> viewField : def.getFields()) {
      Referable underlyingField = viewField.getUnderlyingField();
      if (underlyingField instanceof UnresolvedReference) {
        GlobalReferable classField = myNameResolver.nsProviders.dynamics.forReferable(underlyingClass).resolveName(underlyingField.getName());
        if (classField != null) {
          viewField.setUnderlyingField(classField);
        } else {
          myErrorReporter.report(new NoSuchFieldError<>(def.getName(), def));
        }
      }
    }
    return null;
  }

  @Override
  public Void visitClassViewField(Concrete.ClassViewField def, Scope parentScope) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitClassViewInstance(Concrete.ClassViewInstance<T> def, Scope parentScope) {
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(parentScope, myContext, myNameResolver, myInfoProvider, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitParameters(def.getParameters());
      exprVisitor.visitReference(def.getClassView(), null);
      if (def.getClassView().getReferent() instanceof Concrete.ClassView) {
        exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (Concrete.ClassView) def.getClassView().getReferent());
        boolean ok = false;
        for (Concrete.ClassFieldImpl<T> impl : def.getClassFieldImpls()) {
          if (impl.getImplementedField() == ((Concrete.ClassView) def.getClassView().getReferent()).getClassifyingField()) {
            ok = true;
            Concrete.Expression expr = impl.getImplementation();
            while (expr instanceof Concrete.AppExpression) {
              expr = ((Concrete.AppExpression) expr).getFunction();
            }
            if (expr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr).getReferent() instanceof GlobalReferable) {
              def.setClassifyingDefinition((GlobalReferable) ((Concrete.ReferenceExpression) expr).getReferent());
            } else {
              myErrorReporter.report(new NamingError<>("Expected a definition applied to arguments", impl.getImplementation()));
            }
          }
        }
        if (!ok) {
          myErrorReporter.report(new NamingError<>("Classifying field is not implemented", def));
        }
      } else {
        myErrorReporter.report(new WrongReferable<>("Expected a class view", def.getClassView().getReferent(), def));
      }
    }

    return null;
  }

  private Iterable<Scope> getExtraScopes(Concrete.Definition def, Scope currentScope) {
    if (def instanceof HasOpens) {
      return StreamSupport.stream(((HasOpens) def).getOpens().spliterator(), false)
          .flatMap(cmd -> processOpenCommand(cmd, currentScope))
          .collect(Collectors.toList());
    } else {
      return Collections.emptySet();
    }
  }

  private Stream<Scope> processOpenCommand(OpenCommand cmd, Scope currentScope) { // TODO[abstract]
    if (cmd.getResolvedClass() == null) {
      final GlobalReferable referredClass;
      if (cmd.getModulePath() == null) {
        if (cmd.getPath().isEmpty()) {
          myErrorReporter.report(new NamingError<>("Structure error: empty namespace command", (Concrete.SourceNode<T>) cmd));
          return Stream.empty();
        }
        referredClass = myNameResolver.resolveDefinition(currentScope, cmd.getPath());
      } else {
        ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(cmd.getModulePath());
        GlobalReferable moduleClass = moduleNamespace != null ? moduleNamespace.getRegisteredClass() : null;
        if (moduleClass == null) {
          myErrorReporter.report(new NamingError<>("Module not found: " + cmd.getModulePath(), (Concrete.SourceNode<T>) cmd));
          return Stream.empty();
        }
        if (cmd.getPath().isEmpty()) {
          referredClass = moduleNamespace.getRegisteredClass();
        } else {
          referredClass = myNameResolver.resolveDefinition(new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(moduleClass)), cmd.getPath());
        }
      }

      if (referredClass == null) {
        myErrorReporter.report(new NamingError<>("Class not found", (Concrete.SourceNode<T>) cmd));
        return Stream.empty();
      }
      ((Concrete.NamespaceCommandStatement) cmd).setResolvedClass(referredClass);
    }

    Scope scope = new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(cmd.getResolvedClass()));
    if (cmd.getNames() != null) {
      scope = new FilteredScope(scope, new HashSet<>(cmd.getNames()), !cmd.isHiding());
    }

    return Stream.of(scope);
  }

  private void warnDuplicate(Referable ref1, Referable ref2) {
    myErrorReporter.report(new DuplicateNameError<>(Error.Level.WARNING, ref1, ref2, (Concrete.SourceNode<T>) ref1)); // TODO[abstract]
  }
}
