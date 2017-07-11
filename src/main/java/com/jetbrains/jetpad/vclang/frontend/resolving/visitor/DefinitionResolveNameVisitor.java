package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongDefinition;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Scope, Void>, AbstractStatementVisitor<Scope, Scope> {
  private List<Abstract.ReferableSourceNode> myContext;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;

  public DefinitionResolveNameVisitor(NameResolver nameResolver, ResolveListener resolveListener) {
    this(new ArrayList<>(), nameResolver, resolveListener);
  }

  private DefinitionResolveNameVisitor(List<Abstract.ReferableSourceNode> context,
                                       NameResolver nameResolver, ResolveListener resolveListener) {
    myContext = context;
    myNameResolver = nameResolver;
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
    Scope scope = new FunctionScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def)));

    for (Abstract.Statement statement : def.getGlobalStatements()) {
      if (statement instanceof Abstract.NamespaceCommandStatement) {
        scope = statement.accept(this, scope);
      }
    }
    for (Abstract.Statement statement : def.getGlobalStatements()) {
      if (!(statement instanceof Abstract.NamespaceCommandStatement)) {
        scope = statement.accept(this, scope);
      }
    }

    Abstract.FunctionBody body = def.getBody();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext, myNameResolver, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitArguments(def.getArguments());

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
        addNotEliminatedArguments(def.getArguments(), references);
        exprVisitor.visitClauses(((Abstract.ElimFunctionBody) body).getClauses());
      }
    }

    return null;
  }

  private void addNotEliminatedArguments(List<? extends Abstract.Argument> arguments, List<? extends Abstract.ReferenceExpression> eliminated) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Abstract.ReferableSourceNode> referables = eliminated.stream().map(Abstract.ReferenceExpression::getReferent).collect(Collectors.toSet());
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TelescopeArgument) {
        for (Abstract.ReferableSourceNode referable : ((Abstract.TelescopeArgument) argument).getReferableList()) {
          if (referable != null && referable.getName() != null && !referable.getName().equals("_") && !referables.contains(referable)) {
            myContext.add(referable);
          }
        }
      } else if (argument instanceof Abstract.NameArgument) {
        Abstract.ReferableSourceNode referable = ((Abstract.NameArgument) argument).getReferable();
        if (referable != null && referable.getName() != null && !referable.getName().equals("_") && !referables.contains(referable)) {
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
        resultType.accept(new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener), null);
      }
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Scope parentScope) {
    Scope scope = new DataScope(parentScope, new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def)));
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext, myNameResolver, myResolveListener);
    try (Utils.CompleteContextSaver<Abstract.ReferableSourceNode> saver = new Utils.CompleteContextSaver<>(myContext)) {
      exprVisitor.visitArguments(def.getParameters());
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
        addNotEliminatedArguments(def.getParameters(), def.getEliminatedReferences());
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
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitArguments(def.getArguments());
      if (def.getEliminatedReferences() != null) {
        for (Abstract.ReferenceExpression ref : def.getEliminatedReferences()) {
          exprVisitor.visitReference(ref, null);
        }
      }
    }

    if (def.getEliminatedReferences() != null) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        addNotEliminatedArguments(def.getArguments(), def.getEliminatedReferences());
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

  private void mergeNames(Scope parent, Scope child) {
    Set<String> parentNames = parent.getNames();
    if (!parentNames.isEmpty()) {
      Set<String> childNames = child.getNames();
      if (!childNames.isEmpty()) {
        for (String name : parentNames) {
          if (childNames.contains(name)) {
            myResolveListener.report(new DuplicateDefinitionError(Error.Level.WARNING, parent.resolveName(name), child.resolveName(name)));
          }
        }
      }
    }
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Scope parentScope) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener);
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(exprVisitor, null);
    }

    try {
      Scope staticNamespace = new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(def));
      Scope staticScope = new StaticClassScope(parentScope, staticNamespace);
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        if (statement instanceof Abstract.NamespaceCommandStatement) {
          staticScope = statement.accept(this, staticScope);
        }
      }
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        if (!(statement instanceof Abstract.NamespaceCommandStatement)) {
          staticScope = statement.accept(this, staticScope);
        }
      }

      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        for (Abstract.TypeArgument polyParam : def.getPolyParameters()) {
          polyParam.getType().accept(exprVisitor, null);
          if (polyParam instanceof Abstract.TelescopeArgument) {
            for (Abstract.ReferableSourceNode referable : ((Abstract.TelescopeArgument) polyParam).getReferableList()) {
              if (referable != null && referable.getName() != null && referable.getName().equals("_")) {
                myContext.add(referable);
              }
            }
          }
        }

        Scope child = new NamespaceScope(myNameResolver.nsProviders.dynamics.forClass(def));
        mergeNames(staticNamespace, child);
        Scope dynamicScope = new DynamicClassScope(staticScope, child);

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
      myResolveListener.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, Scope parentScope) {
    Abstract.ClassField referable = myNameResolver.resolveClassField(def.getParentDefinition(), def.getName());
    if (referable != null) {
      myResolveListener.implementResolved(def, referable);
    } else {
      myResolveListener.report(new NoSuchFieldError(def, def.getName()));
    }

    def.getImplementation().accept(new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener), null);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Scope parentScope) {
    def.getUnderlyingClassReference().accept(new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener), null);
    Abstract.ReferableSourceNode resolvedUnderlyingClass = def.getUnderlyingClassReference().getReferent();
    if (!(resolvedUnderlyingClass instanceof Abstract.ClassDefinition)) {
      if (resolvedUnderlyingClass != null) {
        myResolveListener.report(new WrongDefinition("Expected a class", resolvedUnderlyingClass, def));
      }
      return null;
    }

    Namespace dynamicNamespace = myNameResolver.nsProviders.dynamics.forClass((Abstract.ClassDefinition) resolvedUnderlyingClass);
    Abstract.Definition resolvedClassifyingField = dynamicNamespace.resolveName(def.getClassifyingFieldName());
    if (!(resolvedClassifyingField instanceof Abstract.ClassField)) {
      myResolveListener.report(resolvedClassifyingField != null ? new WrongDefinition("Expected a class field", resolvedClassifyingField, def) : new NotInScopeError(def, def.getClassifyingFieldName()));
      return null;
    }

    myResolveListener.classViewResolved(def, (Abstract.ClassField) resolvedClassifyingField);

    for (Abstract.ClassViewField viewField : def.getFields()) {
      Abstract.ClassField classField = myNameResolver.resolveClassField((Abstract.ClassDefinition) resolvedUnderlyingClass, viewField.getUnderlyingFieldName());
      if (classField != null) {
        myResolveListener.classViewFieldResolved(viewField, classField);
      } else {
        myResolveListener.report(new NoSuchFieldError(def, def.getName()));
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
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, myContext, myNameResolver, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      exprVisitor.visitArguments(def.getArguments());
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
              myResolveListener.report(new GeneralError("Expected a definition applied to arguments", impl.getImplementation()));
            }
          }
        }
        if (!ok) {
          myResolveListener.report(new GeneralError("Classifying field is not implemented", def));
        }
      } else {
        myResolveListener.report(new WrongDefinition("Expected a class view", def.getClassView().getReferent(), def));
      }
    }

    return null;
  }

  @Override
  public Scope visitDefine(Abstract.DefineStatement stat, Scope parentScope) {
    stat.getDefinition().accept(this, parentScope);
    return parentScope;
  }

  @Override
  public Scope visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Scope parentScope) {
    if (stat.getResolvedClass() == null) {
      final Abstract.Definition referredClass;
      if (stat.getModulePath() == null) {
        if (stat.getPath().isEmpty()) {
          myResolveListener.report(new GeneralError("Structure error: empty namespace command", stat));
          return parentScope;
        }
        referredClass = myNameResolver.resolveDefinition(parentScope, stat.getPath());
      } else {
        ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(stat.getModulePath());
        Abstract.ClassDefinition moduleClass = moduleNamespace != null ? moduleNamespace.getRegisteredClass() : null;
        if (moduleClass == null) {
          myResolveListener.report(new GeneralError("Module not found: " + stat.getModulePath(), stat));
          return parentScope;
        }
        if (stat.getPath().isEmpty()) {
          referredClass = moduleNamespace.getRegisteredClass();
        } else {
          referredClass = myNameResolver.resolveDefinition(new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(moduleClass)), stat.getPath());
        }
      }

      if (referredClass == null) {
        myResolveListener.report(new GeneralError("Class not found", stat));
        return parentScope;
      }
      myResolveListener.nsCmdResolved(stat, referredClass);
    }

    if (stat.getKind().equals(Abstract.NamespaceCommandStatement.Kind.OPEN)) {
      Scope scope = new NamespaceScope(myNameResolver.nsProviders.statics.forDefinition(stat.getResolvedClass()));
      if (stat.getNames() != null) {
        scope = new FilteredScope(scope, new HashSet<>(stat.getNames()), !stat.isHiding());
      }
      mergeNames(scope, parentScope);
      parentScope = new OverridingScope(scope, parentScope);
    }

    return parentScope;
  }
}
