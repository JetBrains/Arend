package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.error.WrongReferable;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor implements ConcreteDefinitionVisitor<Scope, Void> {
  private final NameResolver myNameResolver;
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveNameVisitor(NameResolver nameResolver, ErrorReporter errorReporter) {
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
  }

  public NameResolver getNameResolver() {
    return myNameResolver;
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

    for (Concrete.TypeParameter param : def.getParameters()) {
      param.getType().accept(exprVisitor, null);
      if (param instanceof Concrete.TelescopeParameter) {
        for (Referable referable : ((Concrete.TelescopeParameter) param).getReferableList()) {
          if (referable != null && referable.textRepresentation().equals("_")) {
            context.add(referable);
          }
        }
      }
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
  public Void visitClassView(Concrete.ClassView def, Scope parentScope) {
    new ExpressionResolveNameVisitor(parentScope, new ArrayList<>(), new ProxyErrorReporter(def.getData(), myErrorReporter)).visitReference(def.getUnderlyingClass(), null);
    if (!(def.getUnderlyingClass().getReferent() instanceof GlobalReferable)) {
      if (!(def.getUnderlyingClass().getReferent() instanceof UnresolvedReference)) {
        myErrorReporter.report(new ProxyError(def.getData(), new WrongReferable("Expected a class", def.getUnderlyingClass().getReferent(), def)));
      }
      return null;
    }

    GlobalReferable underlyingClass = (GlobalReferable) def.getUnderlyingClass().getReferent();
    Referable classifyingField = def.getClassifyingField();
    if (classifyingField instanceof UnresolvedReference) { // TODO[abstract]: Rewrite this using resolve method of UnresolvedReference
      Namespace dynamicNamespace = myNameResolver.nsProviders.dynamics.forReferable(underlyingClass);
      GlobalReferable resolvedClassifyingField = dynamicNamespace.resolveName(classifyingField.textRepresentation());
      if (resolvedClassifyingField == null) {
        // myErrorReporter.report(new ProxyError(def.getData(), new NotInScopeError(classifyingField)));
        return null;
      }
      def.setClassifyingField(resolvedClassifyingField);
    }

    for (Concrete.ClassViewField viewField : def.getFields()) {
      Referable underlyingField = viewField.getUnderlyingField();
      if (underlyingField instanceof UnresolvedReference) { // TODO[abstract]: Rewrite this using resolve method of UnresolvedReference
        GlobalReferable classField = myNameResolver.nsProviders.dynamics.forReferable(underlyingClass).resolveName(underlyingField.textRepresentation());
        if (classField != null) {
          viewField.setUnderlyingField(classField);
        } else {
          myErrorReporter.report(new ProxyError(def.getData(), new NoSuchFieldError(underlyingField.textRepresentation(), def)));
        }
      }
    }
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Scope parentScope) {
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(parentScope, new ArrayList<>(), new ProxyErrorReporter(def.getData(), myErrorReporter));
    exprVisitor.visitParameters(def.getParameters());
    exprVisitor.visitReference(def.getClassView(), null);
    if (def.getClassView().getReferent() instanceof ClassReferable) {
      exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (ClassReferable) def.getClassView().getReferent());
      /* TODO[abstract]
      boolean ok = false;
      for (Concrete.ClassFieldImpl impl : def.getClassFieldImpls()) {
        if (impl.getImplementedField() == ((GlobalReferable) def.getClassView().getReferent()).getClassifyingField()) {
          ok = true;
          Concrete.Expression expr = impl.getImplementation();
          while (expr instanceof Concrete.AppExpression) {
            expr = ((Concrete.AppExpression) expr).getFunction();
          }
          if (expr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr).getReferent() instanceof GlobalReferable) {
            def.setClassifyingDefinition((GlobalReferable) ((Concrete.ReferenceExpression) expr).getReferent());
          } else {
            myErrorReporter.report(new NamingError("Expected a definition applied to arguments", impl.getImplementation()));
          }
        }
      }
      if (!ok) {
        myErrorReporter.report(new NamingError("Classifying field is not implemented", def));
      }
      */
    } else {
      myErrorReporter.report(new ProxyError(def.getData(), new WrongReferable("Expected a class view", def.getClassView().getReferent(), def)));
    }

    return null;
  }
}
