package org.arend.naming.resolving.visitor;

import org.arend.core.context.Utils;
import org.arend.error.DummyErrorReporter;
import org.arend.error.Error;
import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;
import org.arend.frontend.reference.TypeClassReferenceExtractVisitor;
import org.arend.naming.BinOpParser;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.error.NamingError;
import org.arend.naming.error.ReferenceError;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.resolving.NameResolvingChecker;
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.ConvertingScope;
import org.arend.naming.scope.NamespaceCommandNamespace;
import org.arend.naming.scope.Scope;
import org.arend.term.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.ProxyError;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.ProxyErrorReporter;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;
import org.arend.util.LongName;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor implements ConcreteDefinitionVisitor<Scope, Void> {
  private boolean myResolveTypeClassReferences;
  private final ConcreteProvider myConcreteProvider;
  private final ErrorReporter myErrorReporter;
  private LocalErrorReporter myLocalErrorReporter;
  private final ResolverListener myResolverListener;

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, ErrorReporter errorReporter) {
    myResolveTypeClassReferences = false;
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
    myResolverListener = null;
  }

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, ErrorReporter errorReporter, ResolverListener resolverListener) {
    myResolveTypeClassReferences = false;
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
    myResolverListener = resolverListener;
  }

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, boolean resolveTypeClassReferences, ErrorReporter errorReporter) {
    myResolveTypeClassReferences = resolveTypeClassReferences;
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
    myResolverListener = null;
  }

  private void resolveTypeClassReference(List<? extends Concrete.Parameter> parameters, Concrete.Expression expr, Scope scope, boolean isType) {
    if (isType) {
      for (Concrete.Parameter parameter : parameters) {
        if (parameter.getExplicit()) {
          return;
        }
      }

      Concrete.Expression expr1 = expr;
      while (expr1 instanceof Concrete.PiExpression) {
        for (Concrete.TypeParameter parameter : ((Concrete.PiExpression) expr1).getParameters()) {
          if (parameter.getExplicit()) {
            return;
          }
        }
        expr1 = ((Concrete.PiExpression) expr1).getCodomain();
      }
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, new ArrayList<>(), DummyErrorReporter.INSTANCE, myResolverListener);
    exprVisitor.updateScope(parameters);
    if (isType) {
      while (expr instanceof Concrete.PiExpression) {
        exprVisitor.updateScope(((Concrete.PiExpression) expr).getParameters());
        expr = ((Concrete.PiExpression) expr).getCodomain();
      }
    } else {
      while (expr instanceof Concrete.LamExpression) {
        exprVisitor.updateScope(((Concrete.LamExpression) expr).getParameters());
        expr = ((Concrete.LamExpression) expr).getBody();
      }
    }

    while (true) {
      if (expr instanceof Concrete.AppExpression) {
        expr = ((Concrete.AppExpression) expr).getFunction();
      } else if (expr instanceof Concrete.ClassExtExpression) {
        expr = ((Concrete.ClassExtExpression) expr).getBaseClassExpression();
      } else {
        break;
      }
    }

    scope = exprVisitor.getScope();

    if (expr instanceof Concrete.BinOpSequenceExpression) {
      Concrete.BinOpSequenceExpression binOpExpr = (Concrete.BinOpSequenceExpression) expr;
      for (Concrete.BinOpSequenceElem elem : binOpExpr.getSequence()) {
        if (elem.expression instanceof Concrete.ReferenceExpression) {
          Concrete.ReferenceExpression referenceExpression = (Concrete.ReferenceExpression) elem.expression;
          Referable ref = referenceExpression.getReferent();
          if (ref instanceof UnresolvedReference) {
            ref = ((UnresolvedReference) ref).tryResolve(scope);
            if (ref == null) {
              return;
            }
            referenceExpression.setReferent(ref);
          }
        }
      }

      BinOpParser binOpParser = new BinOpParser(myLocalErrorReporter);
      expr = binOpParser.parse(binOpExpr);
      binOpExpr.getSequence().clear();
      binOpExpr.getSequence().add(new Concrete.BinOpSequenceElem(expr, Fixity.NONFIX, true));

      if (expr instanceof Concrete.AppExpression) {
        expr = ((Concrete.AppExpression) expr).getFunction();
      }
    }

    if (!(expr instanceof Concrete.ReferenceExpression)) {
      return;
    }
    Referable ref = ((Concrete.ReferenceExpression) expr).getReferent();
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    if (ref instanceof UnresolvedReference) {
      ref = ((UnresolvedReference) ref).tryResolve(scope);
      if (ref != null) {
        ((Concrete.ReferenceExpression) expr).setReferent(ref);
      }
    }
  }

  private class ConcreteProxyErrorReporter implements LocalErrorReporter {
    private final Concrete.Definition definition;

    private ConcreteProxyErrorReporter(Concrete.Definition definition) {
      this.definition = definition;
    }

    @Override
    public void report(GeneralError error) {
      definition.setHasErrors();
      myErrorReporter.report(error);
    }

    @Override
    public void report(LocalError localError) {
      definition.setHasErrors();
      myErrorReporter.report(new ProxyError(definition.getData(), localError));
    }
  }

  private void checkPrecedence(Concrete.ReferableDefinition definition) {
    Precedence prec = definition.getData().getPrecedence();
    if (prec.priority < 0 || prec.priority > 10) {
      myLocalErrorReporter.report(new NamingError(NamingError.Kind.INVALID_PRIORITY, definition.getData()));
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Scope scope) {
    if (def.getResolved() == Concrete.Resolved.RESOLVED) {
      return null;
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      if (def.getResolved() == Concrete.Resolved.NOT_RESOLVED) {
        if (def.getBody() instanceof Concrete.TermFunctionBody) {
          resolveTypeClassReference(def.getParameters(), ((Concrete.TermFunctionBody) def.getBody()).getTerm(), scope, false);
        }
        if (def.getResultType() != null) {
          resolveTypeClassReference(def.getParameters(), def.getResultType(), scope, true);
        }
      }
      def.setTypeClassReferencesResolved();
      return null;
    }

    checkPrecedence(def);

    Concrete.FunctionBody body = def.getBody();
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, context, myLocalErrorReporter, myResolverListener);
    exprVisitor.visitParameters(def.getParameters(), null);

    if (def.getResultType() != null) {
      def.setResultType(def.getResultType().accept(exprVisitor, null));
    }
    if (def.getResultTypeLevel() != null) {
      def.setResultTypeLevel(def.getResultTypeLevel().accept(exprVisitor, null));
    }

    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).setTerm(((Concrete.TermFunctionBody) body).getTerm().accept(exprVisitor, null));
    }
    if (body instanceof Concrete.CoelimFunctionBody) {
      Concrete.ReferenceExpression typeRefExpr = Concrete.getReferenceExpressionInType(def.getResultType());
      Referable typeRef = typeRefExpr == null ? null : typeRefExpr.getReferent();
      if (typeRef instanceof ClassReferable) {
        exprVisitor.visitClassFieldImpls(body.getClassFieldImpls(), (ClassReferable) typeRef);
      } else {
        body.getClassFieldImpls().clear();
      }
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      visitEliminatedReferences(exprVisitor, body.getEliminatedReferences(), def.getData());
      context.clear();
      addNotEliminatedParameters(def.getParameters(), body.getEliminatedReferences(), context);
      exprVisitor.visitClauses(body.getClauses(), null);
    }

    if (def.enclosingClass != null && def.getKind().isUse()) {
      myLocalErrorReporter.report(new NamingError(NamingError.Kind.USE_IN_CLASS, def.getData()));
      def.enclosingClass = null;
    }

    def.setResolved();
    return null;
  }

  private void visitEliminatedReferences(ExpressionResolveNameVisitor exprVisitor, List<? extends Concrete.ReferenceExpression> eliminatedReferences, GlobalReferable definition) {
    for (Concrete.ReferenceExpression eliminatedReference : eliminatedReferences) {
      exprVisitor.resolveLocal(eliminatedReference);
    }
  }

  private void addNotEliminatedParameters(List<? extends Concrete.Parameter> parameters, List<? extends Concrete.ReferenceExpression> eliminated, List<Referable> context) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Referable> referables = eliminated.stream().map(Concrete.ReferenceExpression::getReferent).collect(Collectors.toSet());
    TypeClassReferenceExtractVisitor typeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor(myConcreteProvider);
    for (Concrete.Parameter parameter : parameters) {
      ClassReferable classRef = typeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), parameter.getType());
      for (Referable referable : parameter.getReferableList()) {
        if (referable != null && !referable.textRepresentation().equals("_") && !referables.contains(referable)) {
          context.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
        }
      }
    }
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Scope scope) {
    if (myResolveTypeClassReferences) {
      return null;
    }
    if (def.getResolved() == Concrete.Resolved.RESOLVED) {
      return null;
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);

    checkPrecedence(def);

    Map<String, TCReferable> constructorNames = new HashMap<>();
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        TCReferable ref = constructor.getData();
        TCReferable oldRef = constructorNames.putIfAbsent(ref.textRepresentation(), ref);
        if (oldRef != null) {
          myLocalErrorReporter.report(new DuplicateNameError(Error.Level.ERROR, ref, oldRef));
        }
      }
    }

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, context, myLocalErrorReporter, myResolverListener);
    exprVisitor.visitParameters(def.getParameters(), null);
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

    def.setResolved();
    return null;
  }

  private void visitConstructor(Concrete.Constructor def, Scope parentScope, List<Referable> context) {
    checkPrecedence(def);

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, parentScope, context, myLocalErrorReporter, myResolverListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      exprVisitor.visitParameters(def.getParameters(), null);
      if (def.getResultType() != null) {
        def.setResultType(def.getResultType().accept(exprVisitor, null));
      }
      visitEliminatedReferences(exprVisitor, def.getEliminatedReferences(), def.getData());
    }

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      addNotEliminatedParameters(def.getParameters(), def.getEliminatedReferences(), context);
      exprVisitor.visitClauses(def.getClauses(), null);
    }
  }

  private void visitConstructorClause(Concrete.ConstructorClause clause, ExpressionResolveNameVisitor exprVisitor) {
    List<Concrete.Pattern> patterns = clause.getPatterns();
    if (patterns != null) {
      exprVisitor.visitPatterns(patterns, new HashMap<>(), true);
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Scope scope) {
    if (def.getResolved() == Concrete.Resolved.RESOLVED) {
      return null;
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      if (def.getResolved() == Concrete.Resolved.NOT_RESOLVED) {
        for (Concrete.ClassField field : def.getFields()) {
          resolveTypeClassReference(field.getParameters(), field.getResultType(), scope, true);
        }
      }
      def.setTypeClassReferencesResolved();
      return null;
    }

    checkPrecedence(def);

    Map<String, TCReferable> fieldNames = new HashMap<>();
    for (Concrete.ClassField field : def.getFields()) {
      TCReferable ref = field.getData();
      TCReferable oldRef = fieldNames.putIfAbsent(ref.textRepresentation(), ref);
      if (oldRef != null) {
        myLocalErrorReporter.report(new DuplicateNameError(Error.Level.ERROR, ref, oldRef));
      }
    }

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, context, myLocalErrorReporter, myResolverListener);
    for (int i = 0; i < def.getSuperClasses().size(); i++) {
      Concrete.ReferenceExpression superClass = def.getSuperClasses().get(i);
      if (exprVisitor.visitReference(superClass, null) != superClass) {
        myLocalErrorReporter.report(new NamingError("Expected a class", superClass));
      }
      if (!(superClass.getReferent() instanceof ClassReferable)) {
        def.getSuperClasses().remove(i--);
      }
    }

    Concrete.Expression previousType = null;
    for (int i = 0; i < def.getFields().size(); i++) {
      Concrete.ClassField field = def.getFields().get(i);
      checkPrecedence(field);

      Concrete.Expression fieldType = field.getResultType();
      if (fieldType == previousType && field.getParameters().isEmpty()) {
        field.setResultType(def.getFields().get(i - 1).getResultType());
        field.setResultTypeLevel(def.getFields().get(i - 1).getResultTypeLevel());
      } else {
        if (field.getResultTypeLevel() != null && field.getKind() == ClassFieldKind.FIELD) {
          myLocalErrorReporter.report(new NamingError(NamingError.Kind.LEVEL_IN_FIELD, field.getResultTypeLevel()));
          field.setResultTypeLevel(null);
        }

        try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
          previousType = field.getParameters().isEmpty() ? fieldType : null;
          exprVisitor.visitParameters(field.getParameters(), null);
          field.setResultType(fieldType.accept(exprVisitor, null));
          if (field.getResultTypeLevel() != null) {
            field.setResultTypeLevel(field.getResultTypeLevel().accept(exprVisitor, null));
          }
        }
      }
    }
    exprVisitor.visitClassFieldImpls(def.getImplementations(), def.getData());

    if (def.isRecord() && def.isForcedCoercingField()) {
      myLocalErrorReporter.report(new NamingError(NamingError.Kind.CLASSIFYING_FIELD_IN_RECORD, def.getCoercingField()));
    }

    def.setResolved();
    return null;
  }

  public void resolveGroupWithTypes(Group group, ReferableConverter referableConverter, Scope scope) {
    myResolveTypeClassReferences = true;
    resolveGroup(group, referableConverter, scope);
    myResolveTypeClassReferences = false;
    resolveGroup(group, referableConverter, scope);
  }

  public void resolveGroup(Group group, ReferableConverter referableConverter, Scope scope) {
    LocatedReferable groupRef = group.getReferable();
    Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(groupRef);
    Scope convertedScope = CachingScope.make(referableConverter == null ? scope : new ConvertingScope(referableConverter, scope));
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition) def).accept(this, convertedScope);
      if (myResolverListener != null) {
        myResolverListener.definitionResolved((Concrete.Definition) def);
      }
    } else {
      myLocalErrorReporter = new ProxyErrorReporter(groupRef, myErrorReporter);
    }

    for (Group subgroup : group.getSubgroups()) {
      resolveGroup(subgroup, referableConverter, NameResolvingChecker.makeScope(subgroup, scope));
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resolveGroup(subgroup, referableConverter, NameResolvingChecker.makeScope(subgroup, scope));
    }

    if (myResolveTypeClassReferences) {
      return;
    }

    for (NamespaceCommand namespaceCommand : group.getNamespaceCommands()) {
      List<String> path = namespaceCommand.getPath();
      if (path.isEmpty()) {
        continue;
      }

      LongUnresolvedReference reference = new LongUnresolvedReference(namespaceCommand, path);
      Scope importedScope = namespaceCommand.getKind() == NamespaceCommand.Kind.IMPORT ? convertedScope.getImportedSubscope() : convertedScope;
      reference.resolve(importedScope);
      Scope curScope = reference.resolveNamespace(importedScope);
      if (curScope == null) {
        myLocalErrorReporter.report(reference.getErrorReference().getError());
      }

      if (curScope != null) {
        for (NameRenaming renaming : namespaceCommand.getOpenedReferences()) {
          Referable ref = ExpressionResolveNameVisitor.resolve(renaming.getOldReference(), curScope);
          if (ref instanceof ErrorReference) {
            myLocalErrorReporter.report(((ErrorReference) ref).getError());
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
          ref = ExpressionResolveNameVisitor.resolve(ref, curScope);
          if (ref instanceof ErrorReference) {
            myLocalErrorReporter.report(((ErrorReference) ref).getError());
          }
        }
      }
    }

    new NameResolvingChecker(false, group instanceof ChildGroup && ((ChildGroup) group).getParentGroup() == null, myConcreteProvider) {
      @Override
      public void onDefinitionNamesClash(LocatedReferable ref1, LocatedReferable ref2, Error.Level level) {
        myLocalErrorReporter.report(new DuplicateNameError(level, ref2, ref1));
      }

      @Override
      public void onFieldNamesClash(LocatedReferable ref1, ClassReferable superClass1, LocatedReferable ref2, ClassReferable superClass2, ClassReferable currentClass, Error.Level level) {
        myLocalErrorReporter.report(new ReferenceError(level, "Field '" + ref2.textRepresentation() +
          (superClass2 == currentClass ? "' is already defined in super class " + superClass1.textRepresentation() : "' is defined in super classes " + superClass1.textRepresentation() + " and " + superClass2.textRepresentation()), superClass2 == currentClass ? ref2 : currentClass));
      }

      @Override
      public void onNamespacesClash(NamespaceCommand cmd1, NamespaceCommand cmd2, String name, Error.Level level) {
        myLocalErrorReporter.report(new NamingError(level, "Definition '" + name + "' is imported from modules " + new LongName(cmd1.getPath()) + " and " + new LongName(cmd2.getPath()), cmd2));
      }

      @Override
      protected void onError(LocalError error) {
        myLocalErrorReporter.report(error);
      }
    }.checkGroup(group, referableConverter == null ? convertedScope : CachingScope.make(scope));
  }
}
