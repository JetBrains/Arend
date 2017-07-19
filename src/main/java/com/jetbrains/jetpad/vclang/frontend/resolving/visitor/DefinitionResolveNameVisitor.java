package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.*;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.DataScope;
import com.jetbrains.jetpad.vclang.naming.scope.DynamicClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.FunctionScope;
import com.jetbrains.jetpad.vclang.naming.scope.StaticClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Scope, Void> {
  private final List<Abstract.ReferableSourceNode> myContext;
  private final NameResolver myNameResolver;
  private final Function<Abstract.Definition, Iterable<OpenCommand>> myOpens;
  private final ResolveListener myResolveListener;
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveNameVisitor(NameResolver nameResolver, Function<Abstract.Definition, Iterable<OpenCommand>> opens, ResolveListener resolveListener, ErrorReporter errorReporter) {
    this(new ArrayList<>(), nameResolver, opens, resolveListener, errorReporter);
  }

  private DefinitionResolveNameVisitor(List<Abstract.ReferableSourceNode> context, NameResolver nameResolver,
                                       Function<Abstract.Definition, Iterable<OpenCommand>> opens, ResolveListener resolveListener, ErrorReporter errorReporter) {
    myContext = context;
    myNameResolver = nameResolver;
    myOpens = opens;
    myResolveListener = resolveListener;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
    Iterable<Scope> extraScopes = getExtraScopes(def, new OverridingScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def))));
    FunctionScope scope = new FunctionScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def)), extraScopes);
    scope.findIntroducedDuplicateNames(this::warnDuplicate);

    for (Abstract.Definition definition : def.getGlobalDefinitions()) {
      definition.accept(this, scope);
    }

    Abstract.FunctionBody body = def.getBody();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext, myNameResolver, myResolveListener, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitParameters(def.getParameters());

      Abstract.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(exprVisitor, null);
      }

      if (body instanceof Abstract.TermFunctionBody) {
        ((Abstract.TermFunctionBody) body).getTerm().accept(exprVisitor, null);
      }
      if (body instanceof Abstract.ElimFunctionBody) {
        for (Abstract.ReferenceExpression expression : ((Abstract.ElimFunctionBody) body).getEliminatedReferences()) {
          exprVisitor.visitReference(expression, null);
        }
      }
    }

    if (body instanceof Abstract.ElimFunctionBody) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        List<? extends Abstract.ReferenceExpression> references = ((Abstract.ElimFunctionBody) body).getEliminatedReferences();
        addNotEliminatedParameters(def.getParameters(), references);
        exprVisitor.visitClauses(((Abstract.ElimFunctionBody) body).getClauses());
      }
    }

    return null;
  }

  private void addNotEliminatedParameters(List<? extends Abstract.Parameter> parameters, List<? extends Abstract.ReferenceExpression> eliminated) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Abstract.ReferableSourceNode> referables = eliminated.stream().map(Abstract.ReferenceExpression::getReferent).collect(Collectors.toSet());
    for (Abstract.Parameter parameter : parameters) {
      if (parameter instanceof Abstract.TelescopeParameter) {
        for (Abstract.ReferableSourceNode referable : ((Abstract.TelescopeParameter) parameter).getReferableList()) {
          if (referable != null && referable.getName() != null && !referable.getName().equals("_") && !referables.contains(referable)) {
            myContext.add(referable);
          }
        }
      } else if (parameter instanceof Abstract.NameParameter) {
        Abstract.ReferableSourceNode referable = (Abstract.NameParameter) parameter;
        if (referable.getName() != null && !referable.getName().equals("_") && !referables.contains(referable)) {
          myContext.add(referable);
        }
      }
    }
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Scope parentScope) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      Abstract.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener, myErrorReporter), null);
      }
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Scope parentScope) {
    Scope scope = new DataScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def)));
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext, myNameResolver, myResolveListener, myErrorReporter);
    try (Utils.CompleteContextSaver<Abstract.ReferableSourceNode> ignored = new Utils.CompleteContextSaver<>(myContext)) {
      exprVisitor.visitParameters(def.getParameters());
      if (def.getUniverse() != null) {
        def.getUniverse().accept(exprVisitor, null);
      }
      if (def.getEliminatedReferences() != null) {
        for (Abstract.ReferenceExpression ref : def.getEliminatedReferences()) {
          exprVisitor.visitReference(ref, null);
        }
      } else {
        for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
          for (Abstract.Constructor constructor : clause.getConstructors()) {
            visitConstructor(constructor, scope);
          }
        }
      }
    }

    if (def.getEliminatedReferences() != null) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences());
        for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            if (clause.getPatterns() != null) {
              visitPatterns(clause.getPatterns(), exprVisitor);
            }
            for (Abstract.Constructor constructor : clause.getConstructors()) {
              visitConstructor(constructor, scope);
            }
          }
        }
      }
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Scope parentScope) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitParameters(def.getParameters());
      if (def.getEliminatedReferences() != null) {
        for (Abstract.ReferenceExpression ref : def.getEliminatedReferences()) {
          exprVisitor.visitReference(ref, null);
        }
      }
    }

    if (def.getEliminatedReferences() != null) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences());
        exprVisitor.visitClauses(def.getClauses());
      }
    }
    return null;
  }

  private void visitPatterns(List<? extends Abstract.Pattern> patterns, ExpressionResolveNameVisitor exprVisitor) {
    for (int i = 0; i < patterns.size(); i++) {
      Abstract.Constructor constructor = exprVisitor.visitPattern(patterns.get(i), new HashSet<>());
      if (constructor != null) {
        myResolveListener.replaceWithConstructor(patterns, i, constructor);
      }
      exprVisitor.resolvePattern(patterns.get(i));
    }
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Scope parentScope) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener, myErrorReporter);
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(exprVisitor, null);
    }

    try {
      Iterable<Scope> extraScopes = getExtraScopes(def, new OverridingScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def))));
      StaticClassScope staticScope = new StaticClassScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def)), extraScopes);
      staticScope.findIntroducedDuplicateNames(this::warnDuplicate);

      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        definition.accept(this, staticScope);
      }

      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        for (Abstract.TypeParameter polyParam : def.getPolyParameters()) {
          polyParam.getType().accept(exprVisitor, null);
          if (polyParam instanceof Abstract.TelescopeParameter) {
            for (Abstract.ReferableSourceNode referable : ((Abstract.TelescopeParameter) polyParam).getReferableList()) {
              if (referable != null && referable.getName() != null && referable.getName().equals("_")) {
                myContext.add(referable);
              }
            }
          }
        }

        DynamicClassScope dynamicScope = new DynamicClassScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def)), new NamespaceScope(myNameResolver.nsProviders.dynamics.forClass(def)), extraScopes);
        dynamicScope.findIntroducedDuplicateNames(this::warnDuplicate);

        for (Abstract.ClassField field : def.getFields()) {
          field.accept(this, dynamicScope);
        }
        for (Abstract.Implementation implementation : def.getImplementations()) {
          implementation.accept(this, dynamicScope);
        }
        for (Abstract.Definition definition : def.getInstanceDefinitions()) {
          definition.accept(this, dynamicScope);
        }
      }
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, Scope parentScope) {
    Abstract.ClassField referable = myNameResolver.resolveClassField(def.getParentDefinition(), def.getName());
    if (referable != null) {
      myResolveListener.implementResolved(def, referable);
    } else {
      myErrorReporter.report(new NoSuchFieldError(def, def.getName()));
    }

    def.getImplementation().accept(new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener, myErrorReporter), null);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Scope parentScope) {
    def.getUnderlyingClassReference().accept(new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener, myErrorReporter), null);
    Abstract.ReferableSourceNode resolvedUnderlyingClass = def.getUnderlyingClassReference().getReferent();
    if (!(resolvedUnderlyingClass instanceof Abstract.ClassDefinition)) {
      if (resolvedUnderlyingClass != null) {
        myErrorReporter.report(new WrongDefinition("Expected a class", resolvedUnderlyingClass, def));
      }
      return null;
    }

    Namespace dynamicNamespace = myNameResolver.nsProviders.dynamics.forClass((Abstract.ClassDefinition) resolvedUnderlyingClass);
    Abstract.Definition resolvedClassifyingField = dynamicNamespace.resolveName(def.getClassifyingFieldName());
    if (!(resolvedClassifyingField instanceof Abstract.ClassField)) {
      myErrorReporter.report(resolvedClassifyingField != null ? new WrongDefinition("Expected a class field", resolvedClassifyingField, def) : new NotInScopeError(def, def.getClassifyingFieldName()));
      return null;
    }

    myResolveListener.classViewResolved(def, (Abstract.ClassField) resolvedClassifyingField);

    for (Abstract.ClassViewField viewField : def.getFields()) {
      Abstract.ClassField classField = myNameResolver.resolveClassField((Abstract.ClassDefinition) resolvedUnderlyingClass, viewField.getUnderlyingFieldName());
      if (classField != null) {
        myResolveListener.classViewFieldResolved(viewField, classField);
      } else {
        myErrorReporter.report(new NoSuchFieldError(def, def.getName()));
      }
    }
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Scope parentScope) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Scope parentScope) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitParameters(def.getParameters());
      exprVisitor.visitReference(def.getClassView(), null);
      if (def.getClassView().getReferent() instanceof Abstract.ClassView) {
        exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (Abstract.ClassView) def.getClassView().getReferent(), null);
        boolean ok = false;
        for (Abstract.ClassFieldImpl impl : def.getClassFieldImpls()) {
          if (impl.getImplementedField() == ((Abstract.ClassView) def.getClassView().getReferent()).getClassifyingField()) {
            ok = true;
            Abstract.Expression expr = impl.getImplementation();
            while (expr instanceof Abstract.AppExpression) {
              expr = ((Abstract.AppExpression) expr).getFunction();
            }
            if (expr instanceof Abstract.ReferenceExpression && ((Abstract.ReferenceExpression) expr).getReferent() instanceof Abstract.Definition) {
              myResolveListener.classViewInstanceResolved(def, (Abstract.Definition) ((Abstract.ReferenceExpression) expr).getReferent());
            } else {
              myErrorReporter.report(new GeneralError("Expected a definition applied to arguments", impl.getImplementation()));
            }
          }
        }
        if (!ok) {
          myErrorReporter.report(new GeneralError("Classifying field is not implemented", def));
        }
      } else {
        myErrorReporter.report(new WrongDefinition("Expected a class view", def.getClassView().getReferent(), def));
      }
    }

    return null;
  }

  private Iterable<Scope> getExtraScopes(Abstract.Definition def, Scope currentScope) {
    return StreamSupport.stream(myOpens.apply(def).spliterator(), false)
        .flatMap(cmd -> processOpenCommand(cmd, currentScope))
        .collect(Collectors.toList());
  }

  private Stream<Scope> processOpenCommand(OpenCommand cmd, Scope currentScope) {
    if (cmd.getResolvedClass() == null) {
      final Abstract.Definition referredClass;
      if (cmd.getModulePath() == null) {
        if (cmd.getPath().isEmpty()) {
          myErrorReporter.report(new GeneralError("Structure error: empty namespace command", cmd));
          return Stream.empty();
        }
        referredClass = myNameResolver.resolveDefinition(currentScope, cmd.getPath());
      } else {
        ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(cmd.getModulePath());
        Abstract.ClassDefinition moduleClass = moduleNamespace != null ? moduleNamespace.getRegisteredClass() : null;
        if (moduleClass == null) {
          myErrorReporter.report(new GeneralError("Module not found: " + cmd.getModulePath(), cmd));
          return Stream.empty();
        }
        if (cmd.getPath().isEmpty()) {
          referredClass = moduleNamespace.getRegisteredClass();
        } else {
          referredClass = myNameResolver.resolveDefinition(new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(moduleClass)), cmd.getPath());
        }
      }

      if (referredClass == null) {
        myErrorReporter.report(new GeneralError("Class not found", cmd));
        return Stream.empty();
      }
      myResolveListener.openCmdResolved(cmd, referredClass);
    }

    Scope scope = new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(cmd.getResolvedClass()));
    if (cmd.getNames() != null) {
      scope = new FilteredScope(scope, new HashSet<>(cmd.getNames()), !cmd.isHiding());
    }

    return Stream.of(scope);
  }

  private void warnDuplicate(Abstract.Definition ref1, Abstract.Definition ref2) {
    myErrorReporter.report(new DuplicateDefinitionError(Error.Level.WARNING, ref1, ref2));
  }
}
