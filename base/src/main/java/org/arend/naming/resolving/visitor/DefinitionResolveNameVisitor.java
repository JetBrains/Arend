package org.arend.naming.resolving.visitor;

import org.arend.core.context.Utils;
import org.arend.error.DummyErrorReporter;
import org.arend.error.ParsingError;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.NameResolverError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.reference.Precedence;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.error.DuplicateOpenedNameError;
import org.arend.naming.error.ExistingOpenedNameError;
import org.arend.naming.error.ReferenceError;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.scope.*;
import org.arend.naming.scope.local.ElimScope;
import org.arend.prelude.Prelude;
import org.arend.term.ClassFieldKind;
import org.arend.term.FunctionKind;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.arend.term.concrete.*;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.order.DFS;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.visitor.SyntacticDesugarVisitor;
import org.arend.util.Pair;
import org.arend.util.SingletonList;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DefinitionResolveNameVisitor implements ConcreteResolvableDefinitionVisitor<Scope, Void> {
  private boolean myResolveTypeClassReferences;
  private final ConcreteProvider myConcreteProvider;
  private final ReferableConverter myReferableConverter;
  private final ErrorReporter myErrorReporter;
  private ErrorReporter myLocalErrorReporter;
  private final ResolverListener myResolverListener;

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter) {
    myResolveTypeClassReferences = false;
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter == null ? IdReferableConverter.INSTANCE : referableConverter;
    myErrorReporter = errorReporter;
    myResolverListener = null;
  }

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, ResolverListener resolverListener) {
    myResolveTypeClassReferences = false;
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter == null ? IdReferableConverter.INSTANCE : referableConverter;
    myErrorReporter = errorReporter;
    myResolverListener = resolverListener;
  }

  public DefinitionResolveNameVisitor(ConcreteProvider concreteProvider, ReferableConverter referableConverter, boolean resolveTypeClassReferences, ErrorReporter errorReporter, ResolverListener resolverListener) {
    myResolveTypeClassReferences = resolveTypeClassReferences;
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter == null ? IdReferableConverter.INSTANCE : referableConverter;
    myErrorReporter = errorReporter;
    myResolverListener = resolverListener;
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

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, scope, new ArrayList<>(), DummyErrorReporter.INSTANCE, myResolverListener);
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

    if (expr instanceof Concrete.BinOpSequenceExpression) {
      Concrete.BinOpSequenceExpression binOpExpr = (Concrete.BinOpSequenceExpression) expr;
      for (Concrete.BinOpSequenceElem elem : binOpExpr.getSequence()) {
        if (elem.expression instanceof Concrete.ReferenceExpression) {
          if (!tryResolve((Concrete.ReferenceExpression) elem.expression, exprVisitor)) {
            return;
          }
        }
      }
    }

    if (expr instanceof Concrete.ReferenceExpression) {
      tryResolve((Concrete.ReferenceExpression) expr, exprVisitor);
    }
  }

  private boolean tryResolve(Concrete.ReferenceExpression expr, ExpressionResolveNameVisitor exprVisitor) {
    Referable ref = expr.getReferent();
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }

    if (ref instanceof UnresolvedReference) {
      List<Referable> resolvedRefs = new ArrayList<>();
      Referable newRef = myReferableConverter.convert(((UnresolvedReference) ref).tryResolve(exprVisitor.getScope(), resolvedRefs));
      if (newRef instanceof MetaReferable) {
        ((UnresolvedReference) ref).reset();
        return false;
      }
      if (newRef == null) {
        return false;
      }
      expr.setReferent(exprVisitor.convertReferable(newRef, expr.getData()));
      if (myResolverListener != null) {
        myResolverListener.referenceResolved(null, ref, expr, resolvedRefs, exprVisitor.getScope());
      }
    }

    return true;
  }

  @Override
  public Void visitMeta(DefinableMetaDefinition def, Scope scope) {
    if (def.getStage().ordinal() >= Concrete.Stage.RESOLVED.ordinal()) {
      return null;
    }

    if (myResolverListener != null) {
      myResolverListener.beforeDefinitionResolved(def);
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      def.setTypeClassReferencesResolved();
      return null;
    }

    checkNameAndPrecedence(def, def.getData());

    List<Referable> context = new ArrayList<>();
    var exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, scope, context, myLocalErrorReporter, myResolverListener, visitLevelParameters(def.getPLevelParameters()), visitLevelParameters(def.getHLevelParameters()));
    exprVisitor.visitParameters(def.getParameters(), null);

    if (def.body != null) {
      def.body = def.body.accept(exprVisitor, null);
    }

    def.setResolved();
    def.accept(new SyntacticDesugarVisitor(myLocalErrorReporter), null);
    if (myResolverListener != null) {
      myResolverListener.definitionResolved(def);
    }

    return null;
  }

  private class ConcreteProxyErrorReporter extends LocalErrorReporter {
    private final Concrete.ResolvableDefinition definition;

    private ConcreteProxyErrorReporter(Concrete.ResolvableDefinition definition) {
      super(definition.getData(), myErrorReporter);
      this.definition = definition;
    }

    @Override
    public void report(GeneralError error) {
      definition.setStatus(error.level);
      myErrorReporter.report(error);
    }
  }

  private void checkNameAndPrecedence(Concrete.ReferableDefinition definition) {
    checkNameAndPrecedence(definition, definition.getData());
  }

  private void checkNameAndPrecedence(ConcreteSourceNode definition, LocatedReferable referable) {
    ExpressionResolveNameVisitor.checkName(referable, myLocalErrorReporter);

    Precedence prec = referable.getPrecedence();
    if (prec.priority < 0 || prec.priority > 10) {
      myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.INVALID_PRIORITY, definition));
    }
  }

  private static Map<String, Referable> visitLevelParameters(List<LevelReferable> params) {
    if (params == null) return Collections.emptyMap();
    Map<String, Referable> result = new HashMap<>();
    for (Referable ref : params) {
      result.put(ref.getRefName(), ref);
    }
    return result;
  }

  private static Map<String, Referable> visitLevelParameters(Concrete.LevelParameters params) {
    return params == null ? Collections.emptyMap() : visitLevelParameters(params.referables);
  }

  private void copyLevelParameters(Concrete.Definition def) {
    if (def.enclosingClass != null && (def.getPLevelParameters() == null || def.getHLevelParameters() == null)) {
      Concrete.GeneralDefinition enclosingDef = myConcreteProvider.getConcrete(def.enclosingClass);
      if (enclosingDef instanceof Concrete.ClassDefinition) {
        Concrete.ClassDefinition classDef = (Concrete.ClassDefinition) enclosingDef;
        if (def.getPLevelParameters() == null && classDef.getPLevelParameters() != null) {
          def.setPLevelParameters(classDef.getPLevelParameters());
          def.pOriginalDef = classDef.getData();
        }
        if (def.getHLevelParameters() == null && classDef.getHLevelParameters() != null) {
          def.setHLevelParameters(classDef.getHLevelParameters());
          def.hOriginalDef = classDef.getData();
        }
      }
    }
  }

  @Override
  public Void visitFunction(Concrete.BaseFunctionDefinition def, Scope scope) {
    if (def.getStage().ordinal() >= Concrete.Stage.RESOLVED.ordinal()) {
      return null;
    }

    if (myResolverListener != null) {
      myResolverListener.beforeDefinitionResolved(def);
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      if (def.getStage() == Concrete.Stage.NOT_RESOLVED) {
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

    copyLevelParameters(def);

    if (def instanceof Concrete.UseDefinition) {
      Concrete.GeneralDefinition enclosingDef = myConcreteProvider.getConcrete(def.getUseParent());
      boolean setPLevels = false;
      boolean setHLevels = false;
      if (def.getPLevelParameters() == null) {
        setPLevels = true;
        if (enclosingDef instanceof Concrete.Definition && ((Concrete.Definition) enclosingDef).getPLevelParameters() != null) {
          def.setPLevelParameters(((Concrete.Definition) enclosingDef).getPLevelParameters());
        }
      } else if (def.getKind().isUse() && enclosingDef instanceof Concrete.Definition) {
        compareUseLevelParameters(def.getPLevelParameters(), ((Concrete.Definition) enclosingDef).getPLevelParameters());
      }
      if (def.getHLevelParameters() == null) {
        setHLevels = true;
        if (enclosingDef instanceof Concrete.Definition && ((Concrete.Definition) enclosingDef).getHLevelParameters() != null) {
          def.setHLevelParameters(((Concrete.Definition) enclosingDef).getHLevelParameters());
        }
      } else if (def.getKind().isUse() && enclosingDef instanceof Concrete.Definition) {
        compareUseLevelParameters(def.getHLevelParameters(), ((Concrete.Definition) enclosingDef).getHLevelParameters());
      }

      if (def instanceof Concrete.CoClauseFunctionDefinition && ((Concrete.CoClauseFunctionDefinition) def).getImplementedField() instanceof UnresolvedReference) {
        Concrete.CoClauseFunctionDefinition function = (Concrete.CoClauseFunctionDefinition) def;
        if (enclosingDef instanceof Concrete.BaseFunctionDefinition) {
          List<Concrete.Parameter> parameters = new SubstConcreteExpressionVisitor(def.getData()).visitParameters(((Concrete.BaseFunctionDefinition) enclosingDef).getParameters());
          for (Concrete.Parameter parameter : parameters) {
            parameter.setExplicit(false);
          }
          def.getParameters().addAll(0, parameters);
          function.setNumberOfExternalParameters(parameters.size());
        }
        if (function.getImplementedField() instanceof UnresolvedReference || function.getData() instanceof LocatedReferableImpl && !((LocatedReferableImpl) function.getData()).isPrecedenceSet()) {
          Referable classRef = null;
          List<? extends Concrete.ClassElement> elements = Collections.emptyList();
          if (enclosingDef instanceof Concrete.BaseFunctionDefinition) {
            Concrete.BaseFunctionDefinition enclosingFunction = (Concrete.BaseFunctionDefinition) enclosingDef;
            if (enclosingFunction.getResultType() != null) {
              if (enclosingFunction.getStage().ordinal() < Concrete.Stage.RESOLVED.ordinal()) {
                resolveTypeClassReference(enclosingFunction.getParameters(), enclosingFunction.getResultType(), scope, true);
              }
              classRef = new TypeClassReferenceExtractVisitor().getTypeClassReference(Collections.emptyList(), enclosingFunction.getResultType());
              elements = enclosingFunction.getBody().getCoClauseElements();
            }
          } else if (enclosingDef instanceof Concrete.ClassDefinition) {
            classRef = enclosingDef.getData();
            elements = ((Concrete.ClassDefinition) enclosingDef).getElements();
          }

          if (classRef != null && !(classRef instanceof ClassReferable)) {
            classRef = classRef.getUnderlyingReferable();
          }
          if (classRef instanceof ClassReferable) {
            Concrete.CoClauseFunctionReference functionRef = null;
            for (Concrete.ClassElement element : elements) {
              if (element instanceof Concrete.CoClauseFunctionReference && ((Concrete.CoClauseFunctionReference) element).getFunctionReference().equals(def.getData())) {
                functionRef = (Concrete.CoClauseFunctionReference) element;
                break;
              }
            }
            if (functionRef != null) {
              function.setImplementedField(new ExpressionResolveNameVisitor(myReferableConverter, scope, null, myLocalErrorReporter, myResolverListener, Collections.emptyMap(), Collections.emptyMap()).visitClassFieldReference(functionRef, function.getImplementedField(), (ClassReferable) classRef));
              Concrete.ReferenceExpression refExpr = functionRef.getReferenceExpression();
              if (setPLevels) {
                if (function.getPLevelParameters() == null) {
                  refExpr.setPLevels(new SingletonList<>(new Concrete.PLevelExpression(refExpr.getData())));
                } else {
                  List<Concrete.LevelExpression> args = new ArrayList<>();
                  for (Referable ref : function.getPLevelParameters().referables) {
                    args.add(new Concrete.IdLevelExpression(refExpr.getData(), ref));
                  }
                  refExpr.setPLevels(args);
                }
              }
              if (setHLevels) {
                if (function.getHLevelParameters() == null) {
                  refExpr.setHLevels(new SingletonList<>(new Concrete.HLevelExpression(refExpr.getData())));
                } else {
                  List<Concrete.LevelExpression> args = new ArrayList<>();
                  for (Referable ref : function.getHLevelParameters().referables) {
                    args.add(new Concrete.IdLevelExpression(refExpr.getData(), ref));
                  }
                  refExpr.setHLevels(args);
                }
              }
            }
          }
          if (function.getData() instanceof LocatedReferableImpl && !((LocatedReferableImpl) function.getData()).isPrecedenceSet() && function.getImplementedField() instanceof GlobalReferable) {
            ((LocatedReferableImpl) function.getData()).setPrecedence(((GlobalReferable) function.getImplementedField()).getPrecedence());
          }
        }
      }
    }

    checkNameAndPrecedence(def);

    Concrete.FunctionBody body = def.getBody();
    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, scope, context, myLocalErrorReporter, myResolverListener, visitLevelParameters(def.getPLevelParameters()), visitLevelParameters(def.getHLevelParameters()));

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
      ClassReferable typeRef = def.getResultType() == null ? null : new TypeClassReferenceExtractVisitor().getTypeClassReference(Collections.emptyList(), def.getResultType());
      if (typeRef != null) {
        if (def.getKind() == FunctionKind.INSTANCE && typeRef.isRecord()) {
          myLocalErrorReporter.report(new NameResolverError("Expected a class, got a record", def));
          body.getCoClauseElements().clear();
        } else {
          for (Concrete.CoClauseElement element : body.getCoClauseElements()) {
            if (element instanceof Concrete.ClassFieldImpl) {
              exprVisitor.visitClassFieldImpl((Concrete.ClassFieldImpl) element, typeRef);
            }
          }
        }
      } else {
        myLocalErrorReporter.report(def.getResultType() != null ? new NameResolverError("Expected a class", def.getResultType()) : new NameResolverError("The type of a function defined by copattern matching must be specified explicitly", def));
        body.getCoClauseElements().clear();
      }
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      if (def.getResultType() == null && !(def instanceof Concrete.CoClauseFunctionDefinition)) {
        myLocalErrorReporter.report(new NameResolverError("The type of a function defined by pattern matching must be specified explicitly", def));
      }
      visitEliminatedReferences(exprVisitor, body.getEliminatedReferences());
      context.clear();
      if (def instanceof Concrete.CoClauseFunctionDefinition && body.getEliminatedReferences().isEmpty() && ((Concrete.CoClauseFunctionDefinition) def).getNumberOfExternalParameters() > 0) {
        List<Boolean> explicitness = new ArrayList<>();
        for (int i = ((Concrete.CoClauseFunctionDefinition) def).getNumberOfExternalParameters(); i < def.getParameters().size(); i++) {
          for (Referable referable : def.getParameters().get(i).getReferableList()) {
            ((Concrete.ElimFunctionBody) body).getEliminatedReferences().add(new Concrete.ReferenceExpression(def.getData(), referable));
            explicitness.add(def.getParameters().get(i).isExplicit());
          }
        }
        for (Concrete.FunctionClause clause : body.getClauses()) {
          int i = 0;
          for (int j = 0; j < clause.getPatterns().size(); j++) {
            Concrete.Pattern pattern = clause.getPatterns().get(j);
            if (i >= explicitness.size()) break;
            if (explicitness.get(i) && !pattern.isExplicit()) {
              myLocalErrorReporter.report(new NameResolverError("Expected an explicit pattern", pattern));
            } else {
              if (!explicitness.get(i) && pattern.isExplicit()) {
                clause.getPatterns().add(j, new Concrete.NamePattern(pattern.getData(), true, null, null));
              }
              if (!pattern.isExplicit()) {
                pattern.setExplicit(true);
              }
              i++;
            }
          }
        }
      }
      addNotEliminatedParameters(def.getParameters(), body.getEliminatedReferences(), context);
      exprVisitor.visitClauses(body.getClauses(), null);
    }

    if (def.getKind().isUse()) {
      TCReferable useParent = def.getUseParent();
      boolean isFunc = useParent.getKind() == GlobalReferable.Kind.FUNCTION || useParent.getKind() == GlobalReferable.Kind.INSTANCE;
      if (isFunc || useParent.getKind() == GlobalReferable.Kind.CLASS || useParent.getKind() == GlobalReferable.Kind.DATA) {
        if (def.getKind() == FunctionKind.COERCE) {
          if (isFunc) {
            myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.MISPLACED_COERCE, def));
          }
          if (def.getParameters().isEmpty() && def.enclosingClass == null) {
            myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.COERCE_WITHOUT_PARAMETERS, def));
          }
        }
      } else {
        myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.MISPLACED_USE, def));
      }
    }

    def.accept(new SyntacticDesugarVisitor(myLocalErrorReporter), null);

    if (def instanceof Concrete.CoClauseFunctionDefinition && def.getKind() == FunctionKind.FUNC_COCLAUSE && ((Concrete.CoClauseFunctionDefinition) def).getNumberOfExternalParameters() > 0) {
      Concrete.CoClauseFunctionDefinition function = (Concrete.CoClauseFunctionDefinition) def;
      BaseConcreteExpressionVisitor<Void> visitor = new BaseConcreteExpressionVisitor<>() {
        @Override
        public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
          if (expr.getReferent() instanceof TCReferable && ((TCReferable) expr.getReferent()).getKind() == GlobalReferable.Kind.COCLAUSE_FUNCTION) {
            Concrete.GeneralDefinition definition = myConcreteProvider.getConcrete((TCReferable) expr.getReferent());
            if (definition instanceof Concrete.CoClauseFunctionDefinition && ((Concrete.CoClauseFunctionDefinition) definition).getUseParent() == function.getUseParent()) {
              List<Concrete.Argument> args = new ArrayList<>();
              int i = 0;
              loop:
              for (Concrete.Parameter parameter : def.getParameters()) {
                for (Referable referable : parameter.getReferableList()) {
                  args.add(new Concrete.Argument(new Concrete.ReferenceExpression(expr.getData(), referable), false));
                  if (++i >= function.getNumberOfExternalParameters()) {
                    break loop;
                  }
                }
              }
              return Concrete.AppExpression.make(expr.getData(), expr, args);
            }
          }
          return expr;
        }

        @Override
        public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
          if (expr.getArguments().get(0).isExplicit() || !(expr.getFunction() instanceof Concrete.ReferenceExpression)) {
            return super.visitApp(expr, params);
          }
          for (Concrete.Argument argument : expr.getArguments()) {
            argument.expression = argument.expression.accept(this, params);
          }
          return expr;
        }
      };
      visitor.visitFunctionHeader(function, null);
    }

    def.setResolved();
    if (myResolverListener != null) {
      myResolverListener.definitionResolved(def);
    }

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
    TypeClassReferenceExtractVisitor typeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor();
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
    if (def.getStage().ordinal() >= Concrete.Stage.RESOLVED.ordinal()) {
      return null;
    }

    if (myResolverListener != null) {
      myResolverListener.beforeDefinitionResolved(def);
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);

    copyLevelParameters(def);
    checkNameAndPrecedence(def);

    Map<String, TCReferable> constructorNames = new HashMap<>();
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        TCReferable ref = constructor.getData();
        TCReferable oldRef = constructorNames.putIfAbsent(ref.textRepresentation(), ref);
        if (oldRef != null) {
          myLocalErrorReporter.report(new DuplicateNameError(GeneralError.Level.ERROR, ref, oldRef));
        }
        if (constructor.isCoerce() && constructor.getParameters().isEmpty()) {
          myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.COERCE_WITHOUT_PARAMETERS, constructor));
        }
      }
    }

    List<Referable> context = new ArrayList<>();
    Map<String, Referable> pLevels = visitLevelParameters(def.getPLevelParameters());
    Map<String, Referable> hLevels = visitLevelParameters(def.getHLevelParameters());
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, scope, context, myLocalErrorReporter, myResolverListener, pLevels, hLevels);
    exprVisitor.visitParameters(def.getParameters(), null);
    if (def.getEliminatedReferences() != null) {
      visitEliminatedReferences(exprVisitor, def.getEliminatedReferences());
    } else {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitConstructor(constructor, scope, context, pLevels, hLevels);
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
            visitConstructor(constructor, scope, context, pLevels, hLevels);
          }
        }
      }
    }

    def.setResolved();
    def.accept(new SyntacticDesugarVisitor(myLocalErrorReporter), null);
    if (myResolverListener != null) {
      myResolverListener.definitionResolved(def);
    }

    return null;
  }

  private void visitConstructor(Concrete.Constructor def, Scope parentScope, List<Referable> context, Map<String, Referable> pLevels, Map<String, Referable> hLevels) {
    checkNameAndPrecedence(def);

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, parentScope, context, myLocalErrorReporter, myResolverListener, pLevels, hLevels);
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

  private void resolveSuperClasses(Concrete.ClassDefinition def, Scope scope, boolean resolveLevels) {
    if (def.getSuperClasses().isEmpty()) {
      return;
    }
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, new ElimScope(scope, Collections.singleton(Prelude.ARRAY.getRef())), null, myErrorReporter, myResolverListener, visitLevelParameters(def.getPLevelParameters()), visitLevelParameters(def.getHLevelParameters()));
    for (int i = 0; i < def.getSuperClasses().size(); i++) {
      Concrete.ReferenceExpression superClass = def.getSuperClasses().get(i);
      Concrete.Expression resolved = exprVisitor.visitReference(superClass, true, resolveLevels);
      Referable ref = RedirectingReferable.getOriginalReferable(superClass.getReferent());
      if (resolved != superClass || !(ref instanceof GlobalReferable && ((GlobalReferable) ref).getKind() == GlobalReferable.Kind.CLASS)) {
        if (!(ref instanceof ErrorReference)) {
          myLocalErrorReporter.report(new NameResolverError("Expected a class", superClass));
        }
        def.getSuperClasses().remove(i--);
      } else {
        superClass.setReferent(ref);
      }
    }
  }

  private boolean compareLevelParameters(Concrete.LevelParameters params1, Concrete.LevelParameters params2) {
    return params1 == null && params2 == null || params1 != null && params2 != null && params1.isIncreasing == params2.isIncreasing && params1.referables.size() == params2.referables.size();
  }

  private void compareUseLevelParameters(Concrete.LevelParameters useParams, Concrete.LevelParameters parentParams) {
    if (!compareLevelParameters(useParams, parentParams)) {
      myErrorReporter.report(new TypecheckingError("The levels parameters of the \\use definition do not match the level parameters of the parent", useParams));
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Scope scope) {
    if (def.getStage().ordinal() >= Concrete.Stage.RESOLVED.ordinal()) {
      return null;
    }

    if (myResolverListener != null) {
      myResolverListener.beforeDefinitionResolved(def);
    }

    myLocalErrorReporter = new ConcreteProxyErrorReporter(def);
    if (myResolveTypeClassReferences) {
      if (def.getStage() == Concrete.Stage.NOT_RESOLVED) {
        for (Concrete.ClassElement element : def.getElements()) {
          if (element instanceof Concrete.ClassField) {
            resolveTypeClassReference(((Concrete.ClassField) element).getParameters(), ((Concrete.ClassField) element).getResultType(), scope, true);
          }
        }
      }
      def.setTypeClassReferencesResolved();
      return null;
    }

    checkNameAndPrecedence(def);

    if (def.isRecord() && def.withoutClassifying()) {
      myErrorReporter.report(new ParsingError(ParsingError.Kind.CLASSIFYING_FIELD_IN_RECORD, def));
    }

    List<Concrete.ClassField> classFields = new ArrayList<>();
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        classFields.add((Concrete.ClassField) element);
      }
    }

    Map<String, TCReferable> fieldNames = new HashMap<>();
    for (Concrete.ClassField field : classFields) {
      TCReferable ref = field.getData();
      TCReferable oldRef = fieldNames.putIfAbsent(ref.textRepresentation(), ref);
      if (oldRef != null) {
        myLocalErrorReporter.report(new DuplicateNameError(GeneralError.Level.ERROR, ref, oldRef));
      }
    }

    if (!def.getSuperClasses().isEmpty() && (def.getPLevelParameters() == null || def.getHLevelParameters() == null)) {
      List<Pair<ClassReferable, Abstract.LevelParameters>> levelParams = new ArrayList<>(2);
      levelParams.add(null);
      levelParams.add(null);
      DFS<ClassReferable, Void> dfs = new DFS<>() {
        @Override
        protected Void forDependencies(ClassReferable ref) {
          if (levelParams.get(0) == null) {
            Abstract.LevelParameters params = ref.getPLevelParameters();
            if (params != null) {
              levelParams.set(0, new Pair<>(ref, params));
            }
          }
          if (levelParams.get(1) == null) {
            Abstract.LevelParameters params = ref.getHLevelParameters();
            if (params != null) {
              levelParams.set(1, new Pair<>(ref, params));
            }
          }
          if (levelParams.get(0) != null && levelParams.get(1) != null) return null;
          int i = 0;
          for (ClassReferable superClass : ref.getSuperClassReferences()) {
            if (!ref.hasLevels(i++)) {
              visit(superClass);
              if (levelParams.get(0) != null && levelParams.get(1) != null) break;
            }
          }
          return null;
        }
      };

      Referable classRef = def.getData();
      if (!(classRef instanceof ClassReferable)) {
        classRef = classRef.getUnderlyingReferable();
      }
      if (classRef instanceof ClassReferable) {
        dfs.visit((ClassReferable) classRef);
        if (def.getPLevelParameters() == null && levelParams.get(0) != null) {
          TCReferable ref = myReferableConverter.toDataLocatedReferable(levelParams.get(0).proj1);
          if (ref instanceof TCDefReferable) {
            def.pOriginalDef = (TCDefReferable) ref;
            def.setPLevelParameters(Concrete.LevelParameters.fromAbstract(levelParams.get(0).proj2));
          }
        }
        if (def.getHLevelParameters() == null && levelParams.get(1) != null) {
          TCReferable ref = myReferableConverter.toDataLocatedReferable(levelParams.get(1).proj1);
          if (ref instanceof TCDefReferable) {
            def.hOriginalDef = (TCDefReferable) ref;
            def.setHLevelParameters(Concrete.LevelParameters.fromAbstract(levelParams.get(1).proj2));
          }
        }
      }
    }

    resolveSuperClasses(def, scope, true);

    List<Referable> context = new ArrayList<>();
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myReferableConverter, scope, context, myLocalErrorReporter, myResolverListener, visitLevelParameters(def.getPLevelParameters()), visitLevelParameters(def.getHLevelParameters()));
    Concrete.Expression previousType = null;
    for (int i = 0; i < classFields.size(); i++) {
      Concrete.ClassField field = classFields.get(i);
      checkNameAndPrecedence(field);

      Concrete.Expression fieldType = field.getResultType();
      if (fieldType == previousType && field.getParameters().isEmpty()) {
        field.setResultType(classFields.get(i - 1).getResultType());
        field.setResultTypeLevel(classFields.get(i - 1).getResultTypeLevel());
      } else {
        if (field.getResultTypeLevel() != null && field.getKind() == ClassFieldKind.FIELD) {
          myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.LEVEL_IGNORED, field));
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

    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassFieldImpl) {
        Referable ref = def.getData();
        if (!(ref instanceof ClassReferable)) {
          ref = ref.getUnderlyingReferable();
        }
        if (ref instanceof ClassReferable) {
          exprVisitor.visitClassFieldImpl((Concrete.ClassFieldImpl) element, (ClassReferable) ref);
        }
      } else if (element instanceof Concrete.OverriddenField) {
        Concrete.OverriddenField field = (Concrete.OverriddenField) element;
        Referable ref = def.getData().getUnderlyingReferable();
        if (!(ref instanceof ClassReferable)) {
          ref = ref.getUnderlyingReferable();
        }
        if (ref instanceof ClassReferable) {
          exprVisitor.visitClassFieldReference(field, field.getOverriddenField(), (ClassReferable) ref);
        }
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
          exprVisitor.visitParameters(field.getParameters(), null);
          field.setResultType(field.getResultType().accept(exprVisitor, null));
          if (field.getResultTypeLevel() != null) {
            field.setResultTypeLevel(field.getResultTypeLevel().accept(exprVisitor, null));
          }
        }
      }
    }

    if ((def.isRecord() || def.withoutClassifying()) && def.isForcedClassifyingField()) {
      myLocalErrorReporter.report(new ParsingError(def.isRecord() ? ParsingError.Kind.CLASSIFYING_FIELD_IN_RECORD : ParsingError.Kind.CLASSIFYING_IGNORED, def));
      def.setClassifyingField(def.getClassifyingField(), false);
    }

    def.setResolved();
    def.accept(new SyntacticDesugarVisitor(myLocalErrorReporter), null);
    if (myResolverListener != null) {
      myResolverListener.definitionResolved(def);
    }

    return null;
  }

  public void resolveGroupWithTypes(Group group, Scope scope) {
    myResolveTypeClassReferences = true;
    resolveGroup(group, scope);
    myResolveTypeClassReferences = false;
    resolveGroup(group, scope);
  }

  private static Scope makeScope(Group group, Scope parentScope, LexicalScope.Extent extent) {
    if (parentScope == null) {
      return null;
    }

    if (group.getNamespaceCommands().isEmpty()) {
      return new MergeScope(LexicalScope.insideOf(group, EmptyScope.INSTANCE, extent), parentScope);
    } else {
      return LexicalScope.insideOf(group, parentScope, extent);
    }
  }

  public void resolveGroup(Group group, Scope scope) {
    LocatedReferable groupRef = group.getReferable();
    Collection<? extends Group> subgroups = group.getSubgroups();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();

    var def = myConcreteProvider.getConcrete(groupRef);
    Scope cachedScope = CachingScope.make(makeScope(group, scope, def instanceof Concrete.ClassDefinition ? LexicalScope.Extent.EXTERNAL_AND_FIELDS : LexicalScope.Extent.EVERYTHING));
    if (def instanceof Concrete.ClassDefinition) {
      resolveSuperClasses((Concrete.ClassDefinition) def, cachedScope, false);
    }
    if (def instanceof Concrete.ResolvableDefinition) {
      ((Concrete.ResolvableDefinition) def).accept(this, cachedScope);
    } else {
      myLocalErrorReporter = new LocalErrorReporter(groupRef, myErrorReporter);
    }

    if (def instanceof Concrete.ClassDefinition && (!subgroups.isEmpty() || !dynamicSubgroups.isEmpty())) {
      cachedScope = CachingScope.make(makeScope(group, scope, LexicalScope.Extent.EVERYTHING));
    }
    for (Group subgroup : subgroups) {
      resolveGroup(subgroup, cachedScope);
    }
    for (Group subgroup : dynamicSubgroups) {
      resolveGroup(subgroup, cachedScope);
    }

    if (myResolveTypeClassReferences) {
      return;
    }

    myLocalErrorReporter = myErrorReporter;

    boolean isTopLevel = !(group instanceof ChildGroup) || ((ChildGroup) group).getParentGroup() == null;
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();
    for (NamespaceCommand namespaceCommand : namespaceCommands) {
      List<String> path = namespaceCommand.getPath();
      NamespaceCommand.Kind kind = namespaceCommand.getKind();
      if (path.isEmpty() || kind == NamespaceCommand.Kind.IMPORT && !isTopLevel) {
        continue;
      }

      LongUnresolvedReference reference = new LongUnresolvedReference(namespaceCommand, path);
      Scope importedScope = kind == NamespaceCommand.Kind.IMPORT ? cachedScope.getImportedSubscope() : cachedScope;
      List<Referable> resolvedRefs = myResolverListener == null ? null : new ArrayList<>();
      reference.resolve(importedScope, resolvedRefs);
      if (myResolverListener != null) {
        myResolverListener.namespaceResolved(namespaceCommand, resolvedRefs);
      }
      Scope curScope = reference.resolveNamespace(importedScope);
      if (curScope == null) {
        myLocalErrorReporter.report(reference.getErrorReference().getError());
      }

      if (curScope != null) {
        for (NameRenaming renaming : namespaceCommand.getOpenedReferences()) {
          Referable oldRef = renaming.getOldReference();
          Referable ref = ExpressionResolveNameVisitor.resolve(oldRef, curScope);
          if (myResolverListener != null) {
            myResolverListener.renamingResolved(renaming, oldRef, ref);
          }
          if (ref instanceof ErrorReference) {
            myLocalErrorReporter.report(((ErrorReference) ref).getError());
          }
        }

        curScope = NamespaceCommandNamespace.makeNamespace(curScope, new NamespaceCommand() {
          @NotNull
          @Override
          public Kind getKind() {
            return namespaceCommand.getKind();
          }

          @NotNull
          @Override
          public List<String> getPath() {
            return namespaceCommand.getPath();
          }

          @Override
          public boolean isUsing() {
            return namespaceCommand.isUsing();
          }

          @NotNull
          @Override
          public Collection<? extends NameRenaming> getOpenedReferences() {
            return namespaceCommand.getOpenedReferences();
          }

          @NotNull
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
        checkField(internalRef.getReferable(), superFields);
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
        myLocalErrorReporter.report(new ParsingError(ParsingError.Kind.MISPLACED_IMPORT, cmd));
      } else {
        checkNamespaceCommand(cmd, referables.keySet());
      }
    }

    List<Pair<NamespaceCommand, Map<String, Referable>>> namespaces = new ArrayList<>(namespaceCommands.size());
    for (NamespaceCommand cmd : namespaceCommands) {
      Collection<? extends Referable> elements = NamespaceCommandNamespace.resolveNamespace(cmd.getKind() == NamespaceCommand.Kind.IMPORT ? cachedScope.getImportedSubscope() : cachedScope, cmd).getElements();
      if (!elements.isEmpty()) {
        Map<String, Referable> map = new LinkedHashMap<>();
        for (Referable element : elements) {
          map.put(element.getRefName(), element);
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
            NamespaceCommand nsCmd = namespaces.get(j).proj1;
            Object cause = nsCmd;
            for (NameRenaming renaming : nsCmd.getOpenedReferences()) {
              String name = renaming.getName();
              if (entry.getKey().equals(name != null ? name : renaming.getOldReference().textRepresentation())) {
                cause = renaming;
                break;
              }
            }
            myLocalErrorReporter.report(new DuplicateOpenedNameError(ref, pair.proj1, cause));
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

  private void checkField(LocatedReferable field, Map<String, Pair<LocatedReferable, ClassReferable>> fields) {
    if (field == null || fields.isEmpty()) {
      return;
    }

    String name = field.textRepresentation();
    if (!name.isEmpty() && !"_".equals(name)) {
      Pair<LocatedReferable, ClassReferable> oldField = fields.get(name);
      if (oldField != null) {
        myLocalErrorReporter.report(new ReferenceError(GeneralError.Level.WARNING, GeneralError.Stage.RESOLVER, "Field '" + field.textRepresentation() + ("' is already defined in super class " + oldField.proj2.textRepresentation()), field));
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
        myLocalErrorReporter.report(new ExistingOpenedNameError(renaming));
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
      myLocalErrorReporter.report(new DuplicateNameError(isInternal ? GeneralError.Level.WARNING : GeneralError.Level.ERROR, newRef, oldRef));
    }
  }
}
