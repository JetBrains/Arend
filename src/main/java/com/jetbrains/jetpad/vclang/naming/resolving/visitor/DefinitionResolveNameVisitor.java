package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongReferable;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor<T> implements ConcreteDefinitionVisitor<T, Scope, Void> {
  private final NameResolver myNameResolver;
  private final ParserInfoProvider myInfoProvider;
  private final ErrorReporter<T> myErrorReporter;

  public DefinitionResolveNameVisitor(NameResolver nameResolver, ParserInfoProvider infoProvider, ErrorReporter<T> errorReporter) {
    myNameResolver = nameResolver;
    myInfoProvider = infoProvider;
    myErrorReporter = errorReporter;
  }

  public NameResolver getNameResolver() {
    return myNameResolver;
  }

  public ErrorReporter<T> getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, Scope scope) {
    Concrete.FunctionBody<T> body = def.getBody();
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(scope, context, myNameResolver, myInfoProvider, myErrorReporter);
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

    if (body instanceof Concrete.ElimFunctionBody) {
      context.clear();
      addNotEliminatedParameters(def.getParameters(), ((Concrete.ElimFunctionBody<T>) body).getEliminatedReferences(), context);
      exprVisitor.visitClauses(((Concrete.ElimFunctionBody<T>) body).getClauses());
    }

    return null;
  }

  private void addNotEliminatedParameters(List<? extends Concrete.Parameter<T>> parameters, List<? extends Concrete.ReferenceExpression> eliminated, List<Referable> context) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Referable> referables = eliminated.stream().map(Concrete.ReferenceExpression::getReferent).collect(Collectors.toSet());
    for (Concrete.Parameter<T> parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        for (Referable referable : ((Concrete.TelescopeParameter<T>) parameter).getReferableList()) {
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
  public Void visitData(Concrete.DataDefinition<T> def, Scope scope) {
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(scope, context, myNameResolver, myInfoProvider, myErrorReporter);
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
          visitConstructor(constructor, scope, context);
        }
      }
    }

    if (def.getEliminatedReferences() != null) {
      context.clear();
      addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences(), context);
      for (Concrete.ConstructorClause<T> clause : def.getConstructorClauses()) {
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
          visitConstructorClause(clause, exprVisitor);
          for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
            visitConstructor(constructor, scope, context);
          }
        }
      }
    }

    return null;
  }

  private void visitConstructor(Concrete.Constructor<T> def, Scope parentScope, List<Referable> context) {
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(parentScope, context, myNameResolver, myInfoProvider, myErrorReporter);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      exprVisitor.visitParameters(def.getParameters());
      for (Concrete.ReferenceExpression<T> ref : def.getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
      }
    }

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences(), context);
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
  public Void visitClass(Concrete.ClassDefinition<T> def, Scope scope) {
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(scope, context, myNameResolver, myInfoProvider, myErrorReporter);
    for (Concrete.ReferenceExpression<T> superClass : def.getSuperClasses()) {
      exprVisitor.visitReference(superClass, null);
    }

    for (Concrete.TypeParameter<T> param : def.getParameters()) {
      param.getType().accept(exprVisitor, null);
      if (param instanceof Concrete.TelescopeParameter) {
        for (Referable referable : ((Concrete.TelescopeParameter<T>) param).getReferableList()) {
          if (referable != null && referable.textRepresentation().equals("_")) {
            context.add(referable);
          }
        }
      }
    }

    for (Concrete.ClassField<T> field : def.getFields()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
        field.getResultType().accept(exprVisitor, null);
      }
    }
    exprVisitor.visitClassFieldImpls(def.getImplementations(), def.getReferable());

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView<T> def, Scope parentScope) {
    new ExpressionResolveNameVisitor<>(parentScope, new ArrayList<>(), myNameResolver, myInfoProvider, myErrorReporter).visitReference(def.getUnderlyingClass(), null);
    if (def.getUnderlyingClass().getExpression() != null || !(def.getUnderlyingClass().getReferent() instanceof GlobalReferable)) {
      if (!(def.getUnderlyingClass().getReferent() instanceof UnresolvedReference)) {
        myErrorReporter.report(new WrongReferable<>("Expected a class", def.getUnderlyingClass().getReferent(), def));
      }
      return null;
    }

    GlobalReferable underlyingClass = (GlobalReferable) def.getUnderlyingClass().getReferent();
    Referable classifyingField = def.getClassifyingField();
    if (classifyingField instanceof UnresolvedReference) { // TODO[abstract]: Rewrite this using resolve method of UnresolvedReference
      Namespace dynamicNamespace = myNameResolver.nsProviders.dynamics.forReferable(underlyingClass);
      GlobalReferable resolvedClassifyingField = dynamicNamespace.resolveName(classifyingField.textRepresentation());
      if (resolvedClassifyingField == null) {
        myErrorReporter.report(new NotInScopeError<>(classifyingField.textRepresentation(), def));
        return null;
      }
      def.setClassifyingField(resolvedClassifyingField);
    }

    for (Concrete.ClassViewField<T> viewField : def.getFields()) {
      Referable underlyingField = viewField.getUnderlyingField();
      if (underlyingField instanceof UnresolvedReference) { // TODO[abstract]: Rewrite this using resolve method of UnresolvedReference
        GlobalReferable classField = myNameResolver.nsProviders.dynamics.forReferable(underlyingClass).resolveName(underlyingField.textRepresentation());
        if (classField != null) {
          viewField.setUnderlyingField(classField);
        } else {
          myErrorReporter.report(new NoSuchFieldError<>(underlyingField.textRepresentation(), def));
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
  public Void visitInstance(Concrete.Instance<T> def, Scope parentScope) {
    ExpressionResolveNameVisitor<T> exprVisitor = new ExpressionResolveNameVisitor<>(parentScope, new ArrayList<>(), myNameResolver, myInfoProvider, myErrorReporter);
    exprVisitor.visitParameters(def.getParameters());
    exprVisitor.visitReference(def.getClassView(), null);
    if (def.getClassView().getReferent() instanceof GlobalReferable) {
      exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (GlobalReferable) def.getClassView().getReferent());
      /* TODO[abstract]
      boolean ok = false;
      for (Concrete.ClassFieldImpl<T> impl : def.getClassFieldImpls()) {
        if (impl.getImplementedField() == ((GlobalReferable) def.getClassView().getReferent()).getClassifyingField()) {
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
      */
    } else {
      myErrorReporter.report(new WrongReferable<>("Expected a class view", def.getClassView().getReferent(), def));
    }

    return null;
  }
}
