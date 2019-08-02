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
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.scope.*;
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
import org.arend.util.Pair;

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
        if (parameter.isExplicit()) {
          return;
        }
      }

      Concrete.Expression expr1 = expr;
      while (expr1 instanceof Concrete.PiExpression) {
        for (Concrete.TypeParameter parameter : ((Concrete.PiExpression) expr1).getParameters()) {
          if (parameter.isExplicit()) {
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
      Referable typeRef = def.getResultType() == null ? null : def.getResultType().getUnderlyingReferable();
      if (typeRef instanceof ClassReferable) {
        if (def.getKind() == Concrete.FunctionDefinition.Kind.INSTANCE && ((ClassReferable) typeRef).isRecord()) {
          myLocalErrorReporter.report(new NamingError("Expected a class, got a record", def.getData()));
          body.getClassFieldImpls().clear();
        } else {
          exprVisitor.visitClassFieldImpls(body.getClassFieldImpls(), (ClassReferable) typeRef);
        }
      } else {
        if (!(typeRef instanceof ErrorReference)) {
          myLocalErrorReporter.report(def.getResultType() != null ? new NamingError("Expected a class", def.getResultType().getData()) : new NamingError("The type of a function defined by copattern matching must be specified explicitly", def.getData()));
        }
        body.getClassFieldImpls().clear();
      }
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      if (def.getResultType() == null) {
        myLocalErrorReporter.report(new NamingError("The type of a function defined by pattern matching must be specified explicitly", def.getData()));
      }
      visitEliminatedReferences(exprVisitor, body.getEliminatedReferences());
      context.clear();
      addNotEliminatedParameters(def.getParameters(), body.getEliminatedReferences(), context);
      exprVisitor.visitClauses(body.getClauses(), null);
    }

    if (def.getKind().isUse()) {
      TCReferable useParent = def.getUseParent();
      boolean isFunc = myConcreteProvider.isFunction(useParent);
      if (isFunc || useParent instanceof ClassReferable || myConcreteProvider.isData(useParent)) {
        if (def.getKind() == Concrete.FunctionDefinition.Kind.COERCE) {
          if (isFunc) {
            myLocalErrorReporter.report(new NamingError(NamingError.Kind.MISPLACED_COERCE, def.getData()));
          }
          if (def.getParameters().isEmpty() && def.enclosingClass == null) {
            myLocalErrorReporter.report(new NamingError(NamingError.Kind.COERCE_WITHOUT_PARAMETERS, def.getData()));
          }
        }
      } else {
        myLocalErrorReporter.report(new NamingError(NamingError.Kind.MISPLACED_USE, def.getData()));
      }
    }

    def.setResolved();
    return null;
  }

  private void visitEliminatedReferences(ExpressionResolveNameVisitor exprVisitor, List<? extends Concrete.ReferenceExpression> eliminatedReferences) {
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
      visitEliminatedReferences(exprVisitor, def.getEliminatedReferences());
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
      visitEliminatedReferences(exprVisitor, def.getEliminatedReferences());
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
      if (exprVisitor.visitReference(superClass, null) != superClass || !(superClass.getReferent() instanceof ClassReferable)) {
        if (!(superClass.getReferent() instanceof ErrorReference)) {
          myLocalErrorReporter.report(new NamingError("Expected a class", superClass));
        }
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

  private static Scope makeScope(Group group, Scope parentScope) {
    if (parentScope == null) {
      return null;
    }

    if (group.getNamespaceCommands().isEmpty()) {
      return new MergeScope(LexicalScope.insideOf(group, EmptyScope.INSTANCE), parentScope);
    } else {
      return LexicalScope.insideOf(group, parentScope);
    }
  }

  public void resolveGroup(Group group, ReferableConverter referableConverter, Scope scope) {
    LocatedReferable groupRef = group.getReferable();
    Collection<? extends Group> subgroups = group.getSubgroups();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();

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

    for (Group subgroup : subgroups) {
      resolveGroup(subgroup, referableConverter, makeScope(subgroup, scope));
    }
    for (Group subgroup : dynamicSubgroups) {
      resolveGroup(subgroup, referableConverter, makeScope(subgroup, scope));
    }

    if (myResolveTypeClassReferences) {
      return;
    }

    boolean isTopLevel = !(group instanceof ChildGroup) || ((ChildGroup) group).getParentGroup() == null;
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();
    for (NamespaceCommand namespaceCommand : namespaceCommands) {
      List<String> path = namespaceCommand.getPath();
      NamespaceCommand.Kind kind = namespaceCommand.getKind();
      if (path.isEmpty() || kind == NamespaceCommand.Kind.IMPORT && !isTopLevel) {
        continue;
      }

      LongUnresolvedReference reference = new LongUnresolvedReference(namespaceCommand, path);
      Scope importedScope = kind == NamespaceCommand.Kind.IMPORT ? convertedScope.getImportedSubscope() : convertedScope;
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

    // Some checks

    Collection<? extends Group.InternalReferable> fields = group.getFields();
    if (!fields.isEmpty()) {
      Map<String, Pair<LocatedReferable, ClassReferable>> superFields = collectClassFields(groupRef);
      for (Group.InternalReferable internalRef : fields) {
        checkField(internalRef.getReferable(), superFields, groupRef);
      }
    }

    Map<String, LocatedReferable> referables = new HashMap<>();
    for (Group.InternalReferable internalRef : group.getInternalReferables()) {
      LocatedReferable ref = internalRef.getReferable();
      String name = ref.textRepresentation();
      if (!name.isEmpty() && !"_".equals(name)) {
        referables.putIfAbsent(name, ref);
      }
    }

    for (Group subgroup : subgroups) {
      checkReference(subgroup.getReferable(), referables, false);
    }

    for (Group subgroup : dynamicSubgroups) {
      checkReference(subgroup.getReferable(), referables, false);
    }

    checkSubgroups(dynamicSubgroups, referables);

    checkSubgroups(subgroups, referables);

    if (namespaceCommands.isEmpty()) {
      return;
    }

    for (NamespaceCommand cmd : namespaceCommands) {
      if (!isTopLevel && cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        myLocalErrorReporter.report(new NamingError("\\import is allowed only on the top level", cmd));
      } else {
        checkNamespaceCommand(cmd, referables.keySet());
      }
    }

    if (convertedScope == null) {
      return;
    }

    List<Pair<NamespaceCommand, Map<String, Referable>>> namespaces = new ArrayList<>(namespaceCommands.size());
    for (NamespaceCommand cmd : namespaceCommands) {
      Collection<? extends Referable> elements = NamespaceCommandNamespace.resolveNamespace(cmd.getKind() == NamespaceCommand.Kind.IMPORT ? convertedScope.getImportedSubscope() : convertedScope, cmd).getElements();
      if (!elements.isEmpty()) {
        Map<String, Referable> map = new LinkedHashMap<>();
        for (Referable element : elements) {
          map.put(element.textRepresentation(), element.getUnderlyingReferable());
        }
        namespaces.add(new Pair<>(cmd, map));
      }
    }

    for (int i = 0; i < namespaces.size(); i++) {
      Pair<NamespaceCommand, Map<String, Referable>> pair = namespaces.get(i);
      for (Map.Entry<String, Referable> entry : pair.proj2.entrySet()) {
        if (referables.containsKey(entry.getKey())) {
          continue;
        }

        for (int j = i + 1; j < namespaces.size(); j++) {
          Referable ref = namespaces.get(j).proj2.get(entry.getKey());
          if (ref != null && !ref.equals(entry.getValue())) {
            myLocalErrorReporter.report(new NamingError(Error.Level.WARNING, "Definition '" + ref.textRepresentation() + "' is imported from modules " + new LongName(pair.proj1.getPath()) + " and " + new LongName(namespaces.get(j).proj1.getPath()), namespaces.get(j).proj1));
          }
        }
      }
    }
  }

  private static Map<String, Pair<LocatedReferable, ClassReferable>> collectClassFields(LocatedReferable referable) {
    Collection<? extends ClassReferable> superClasses = referable instanceof ClassReferable ? ((ClassReferable) referable).getSuperClassReferences() : Collections.emptyList();
    if (superClasses.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Pair<LocatedReferable, ClassReferable>> fields = new HashMap<>();
    Set<ClassReferable> visited = new HashSet<>();
    visited.add((ClassReferable) referable);
    Deque<ClassReferable> toVisit = new ArrayDeque<>(superClasses);
    while (!toVisit.isEmpty()) {
      ClassReferable superClass = toVisit.pop();
      if (!visited.add(superClass)) {
        continue;
      }

      for (LocatedReferable fieldRef : superClass.getFieldReferables()) {
        String name = fieldRef.textRepresentation();
        if (!name.isEmpty() && !"_".equals(name)) {
          fields.putIfAbsent(name, new Pair<>(fieldRef, superClass));
        }
      }

      toVisit.addAll(superClass.getSuperClassReferences());
    }

    return fields;
  }

  private void checkField(LocatedReferable field, Map<String, Pair<LocatedReferable, ClassReferable>> fields, LocatedReferable classRef) {
    if (field == null || fields.isEmpty()) {
      return;
    }

    String name = field.textRepresentation();
    if (!name.isEmpty() && !"_".equals(name)) {
      Pair<LocatedReferable, ClassReferable> oldField = fields.get(name);
      if (oldField != null) {
        myLocalErrorReporter.report(new ReferenceError(Error.Level.WARNING, "Field '" + field.textRepresentation() + ("' is already defined in super class " + oldField.proj2.textRepresentation()), field));
      }
    }
  }

  private void checkNamespaceCommand(NamespaceCommand cmd, Set<String> defined) {
    if (defined == null) {
      return;
    }

    for (NameRenaming renaming : cmd.getOpenedReferences()) {
      String name = renaming.getName();
      if (name == null) {
        name = renaming.getOldReference().textRepresentation();
      }
      if (defined.contains(name)) {
        myLocalErrorReporter.report(new NamingError(Error.Level.WARNING, "Definition '" + name + "' is not imported since it is defined in this module", renaming));
      }
    }
  }

  private void checkSubgroups(Collection<? extends Group> subgroups, Map<String, LocatedReferable> referables) {
    for (Group subgroup : subgroups) {
      for (Group.InternalReferable internalReferable : subgroup.getInternalReferables()) {
        if (internalReferable.isVisible()) {
          checkReference(internalReferable.getReferable(), referables, true);
        }
      }
      for (Group.InternalReferable internalReferable : subgroup.getInternalReferables()) {
        if (internalReferable.isVisible()) {
          LocatedReferable newRef = internalReferable.getReferable();
          String name = newRef.textRepresentation();
          if (!name.isEmpty() && !"_".equals(name)) {
            referables.putIfAbsent(name, newRef);
          }
        }
      }
    }
  }

  private void checkReference(LocatedReferable newRef, Map<String, LocatedReferable> referables, boolean isInternal) {
    String name = newRef.textRepresentation();
    if (name.isEmpty() || "_".equals(name)) {
      return;
    }

    LocatedReferable oldRef = isInternal ? referables.get(name) : referables.putIfAbsent(name, newRef);
    if (oldRef != null) {
      myLocalErrorReporter.report(new DuplicateNameError(isInternal ? Error.Level.WARNING : Error.Level.ERROR, newRef, oldRef));
    }
  }
}
