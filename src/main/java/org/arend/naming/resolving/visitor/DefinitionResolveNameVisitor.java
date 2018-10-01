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
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.ConvertingScope;
import org.arend.naming.scope.NamespaceCommandNamespace;
import org.arend.naming.scope.Scope;
import org.arend.term.Fixity;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
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

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, ErrorReporter errorReporter) {
    myResolveTypeClassReferences = false;
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
  }

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, boolean resolveTypeClassReferences, ErrorReporter errorReporter) {
    myResolveTypeClassReferences = resolveTypeClassReferences;
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
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

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, new ArrayList<>(), DummyErrorReporter.INSTANCE);
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

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Scope scope) {
    if (def.getResolved() == Concrete.Resolved.RESOLVED) {
      return null;
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      if (def.getResolved() == Concrete.Resolved.NOT_RESOLVED){
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

    Concrete.FunctionBody body = def.getBody();
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, context, myLocalErrorReporter);
    exprVisitor.visitParameters(def.getParameters(), null);

    Concrete.Expression resultType = def.getResultType();
    if (resultType != null) {
      def.setResultType(resultType.accept(exprVisitor, null));
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

    def.setResolved();
    return null;
  }

  private void visitEliminatedReferences(ExpressionResolveNameVisitor exprVisitor, List<? extends Concrete.ReferenceExpression> eliminatedReferences, GlobalReferable definition) {
    for (int i = 0; i < eliminatedReferences.size(); i++) {
      Concrete.Expression newExpr = exprVisitor.visitReference(eliminatedReferences.get(i), null);
      if (newExpr != eliminatedReferences.get(i)) {
        myLocalErrorReporter.report(new ReferenceError("\\elim can be applied only to a local variable", definition));
        eliminatedReferences.remove(i--);
      }
    }
  }

  private void addNotEliminatedParameters(List<? extends Concrete.Parameter> parameters, List<? extends Concrete.ReferenceExpression> eliminated, List<Referable> context) {
    if (eliminated.isEmpty()) {
      return;
    }

    Set<Referable> referables = eliminated.stream().map(Concrete.ReferenceExpression::getReferent).collect(Collectors.toSet());
    TypeClassReferenceExtractVisitor typeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor(myConcreteProvider);
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        ClassReferable classRef = typeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), ((Concrete.TelescopeParameter) parameter).getType());
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
    if (def.getResolved() == Concrete.Resolved.RESOLVED) {
      return null;
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, context, myLocalErrorReporter);
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
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, parentScope, context, myLocalErrorReporter);
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
          resolveTypeClassReference(Collections.emptyList(), field.getResultType(), scope, true);
        }
      }
      def.setTypeClassReferencesResolved();
      return null;
    }

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, scope, context, myLocalErrorReporter);
    for (int i = 0; i < def.getSuperClasses().size(); i++) {
      Concrete.ReferenceExpression superClass = def.getSuperClasses().get(i);
      if (exprVisitor.visitReference(superClass, null) != superClass) {
        myLocalErrorReporter.report(new NamingError("Expected a class", superClass));
      }
      if (!(superClass.getReferent() instanceof ClassReferable)) {
        def.getSuperClasses().remove(i--);
      }
    }

    for (Concrete.ClassField field : def.getFields()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
        Concrete.Expression resultType = field.getResultType().accept(exprVisitor, null);
        if (resultType != field.getResultType()) {
          field.setResultType(resultType);
        }
      }
    }
    exprVisitor.visitClassFieldImpls(def.getImplementations(), def.getData());

    def.setResolved();
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Scope parentScope) {
    if (def.getResolved() == Concrete.Resolved.RESOLVED) {
      return null;
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      if (def.getResolved() == Concrete.Resolved.NOT_RESOLVED) {
        resolveTypeClassReference(def.getParameters(), def.getResultType(), parentScope, true);
      }
      def.setTypeClassReferencesResolved();
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myConcreteProvider, parentScope, new ArrayList<>(), myLocalErrorReporter);
    exprVisitor.visitParameters(def.getParameters(), null);
    def.setResultType(def.getResultType().accept(exprVisitor, null));
    Referable typeRef = def.getReferenceInType();
    if (typeRef instanceof ClassReferable) {
      exprVisitor.visitClassFieldImpls(def.getClassFieldImpls(), (ClassReferable) typeRef);
    } else {
      def.getClassFieldImpls().clear();
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
    Scope convertedScope = referableConverter == null ? scope : CachingScope.make(new ConvertingScope(referableConverter, scope));
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition) def).accept(this, convertedScope);
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
      LongUnresolvedReference reference = new LongUnresolvedReference(namespaceCommand, namespaceCommand.getPath());
      reference.resolve(convertedScope);
      Scope curScope = reference.resolveNamespace(convertedScope);
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
    }.checkGroup(group, convertedScope);
  }
}
