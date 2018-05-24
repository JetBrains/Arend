package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.frontend.reference.TypeClassReferenceExtractVisitor;
import com.jetbrains.jetpad.vclang.naming.BinOpParser;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.scope.ClassFieldImplScope;
import com.jetbrains.jetpad.vclang.naming.scope.ListScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class ExpressionResolveNameVisitor implements ConcreteExpressionVisitor<Void, Concrete.Expression> {
  private final TypeClassReferenceExtractVisitor myTypeClassReferenceExtractVisitor;
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final LocalErrorReporter myErrorReporter;

  public ExpressionResolveNameVisitor(ConcreteProvider concreteProvider, Scope parentScope, List<Referable> context, LocalErrorReporter errorReporter) {
    myTypeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor(concreteProvider);
    myParentScope = parentScope;
    myScope = context == null ? parentScope : new MergeScope(new ListScope(context), parentScope);
    myContext = context;
    myErrorReporter = errorReporter;
  }

  Scope getScope() {
    return myScope;
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    Concrete.Expression fun = expr.getFunction().accept(this, null);
    Concrete.Expression arg = expr.getArgument().getExpression().accept(this, null);
    return fun == expr.getFunction() && arg == expr.getArgument().getExpression() ? expr : new Concrete.AppExpression(expr.getData(), fun, new Concrete.Argument(arg, expr.getArgument().isExplicit()));
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable referable = expr.getReferent();
    while (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }

    Concrete.Expression argument = null;
    if (referable instanceof UnresolvedReference) {
      argument = ((UnresolvedReference) referable).resolveArgument(myScope);
      referable = ((UnresolvedReference) referable).resolve(myScope);
      if (referable instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) referable).getError());
      }
      while (referable instanceof RedirectingReferable) {
        referable = ((RedirectingReferable) referable).getOriginalReferable();
      }
    }

    expr.setReferent(referable);
    return argument == null ? expr : new Concrete.AppExpression(expr.getData(), expr, new Concrete.Argument(argument, false));
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return expr;
  }

  void visitParameters(List<Concrete.Parameter> parameters) {
    for (int i = 0; i < parameters.size(); i++) {
      parameters.set(i, visitParameter(parameters.get(i)));
    }
  }

  void visitTypeParameters(List<Concrete.TypeParameter> parameters) {
    for (int i = 0; i < parameters.size(); i++) {
      parameters.set(i, (Concrete.TypeParameter) visitParameter(parameters.get(i)));
    }
  }

  void updateScope(Collection<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        ClassReferable classRef = getTypeClassReference(((Concrete.TelescopeParameter) parameter).getType());
        for (Referable referable : ((Concrete.TelescopeParameter) parameter).getReferableList()) {
          if (referable != null && !referable.textRepresentation().equals("_")) {
            myContext.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
          }
        }
      } else
      if (parameter instanceof Concrete.NameParameter) {
        Referable referable = ((Concrete.NameParameter) parameter).getReferable();
        if (referable != null && !referable.textRepresentation().equals("_")) {
          myContext.add(referable);
        }
      }
    }
  }

  private ClassReferable getTypeClassReference(Concrete.Expression type) {
    return myTypeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), type);
  }

  Concrete.Parameter visitParameter(Concrete.Parameter parameter) {
    if (parameter instanceof Concrete.TypeParameter) {
      Concrete.Expression type = ((Concrete.TypeParameter) parameter).getType();
      Concrete.Expression newType = type.accept(this, null);
      if (type != newType) {
        if (parameter instanceof Concrete.TelescopeParameter) {
          parameter = new Concrete.TelescopeParameter(parameter.getData(), parameter.getExplicit(), ((Concrete.TelescopeParameter) parameter).getReferableList(), newType);
        } else {
          parameter = new Concrete.TypeParameter(parameter.getData(), parameter.getExplicit(), newType);
        }
      }
    }

    if (parameter instanceof Concrete.TelescopeParameter) {
      ClassReferable classRef = getTypeClassReference(((Concrete.TelescopeParameter) parameter).getType());
      List<? extends Referable> referableList = ((Concrete.TelescopeParameter) parameter).getReferableList();
      for (int i = 0; i < referableList.size(); i++) {
        Referable referable = referableList.get(i);
        if (referable != null && !referable.textRepresentation().equals("_")) {
          for (int j = 0; j < i; j++) {
            Referable referable1 = referableList.get(j);
            if (referable1 != null && referable.textRepresentation().equals(referable1.textRepresentation())) {
              myErrorReporter.report(new DuplicateNameError(Error.Level.WARNING, referable1, referable));
            }
          }
          myContext.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
        }
      }
    } else
    if (parameter instanceof Concrete.NameParameter) {
      Referable referable = ((Concrete.NameParameter) parameter).getReferable();
      if (referable != null && !referable.textRepresentation().equals("_")) {
        myContext.add(referable);
      }
    }

    return parameter;
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      Concrete.Expression body = expr.getBody().accept(this, null);
      return body == expr.getBody() ? expr : new Concrete.LamExpression(expr.getData(), expr.getParameters(), body);
    }
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitTypeParameters(expr.getParameters());
      Concrete.Expression codomain = expr.getCodomain().accept(this, null);
      return codomain == expr.getCodomain() ? expr : new Concrete.PiExpression(expr.getData(), expr.getParameters(), codomain);
    }
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitInferHole(Concrete.InferHoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void params) {
    Concrete.Expression expression = expr.getExpression();
    Concrete.Expression newExpression = expression == null ? null : expression.accept(this, null);
    return newExpression == expression ? expr : new Concrete.GoalExpression(expr.getData(), expr.getName(), newExpression);
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    for (int i = 0; i < expr.getFields().size(); i++) {
      expr.getFields().set(i, expr.getFields().get(i).accept(this, null));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitTypeParameters(expr.getParameters());
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    if (expr.getSequence().size() == 1) {
      return expr.getSequence().get(0).expression.accept(this, null);
    }

    BinOpParser parser = new BinOpParser(myErrorReporter);

    for (int i = 0; i < expr.getSequence().size(); i++) {
      Concrete.BinOpSequenceElem elem = expr.getSequence().get(i);
      Concrete.Expression newElem = elem.expression.accept(this, null);
      if (newElem != elem.expression) {
        expr.getSequence().set(i, new Concrete.BinOpSequenceElem(newElem, elem.fixity, elem.isExplicit));
      }
    }

    return parser.parse(expr);
  }

  static void replaceWithConstructor(Concrete.PatternContainer container, int index, Referable constructor) {
    Concrete.Pattern old = container.getPatterns().get(index);
    Concrete.Pattern newPattern = new Concrete.ConstructorPattern(old.getData(), constructor, Collections.emptyList());
    newPattern.setExplicit(old.isExplicit());
    container.getPatterns().set(index, newPattern);
  }

  void visitClauses(List<Concrete.FunctionClause> clauses) {
    for (int i = 0; i < clauses.size(); i++) {
      Concrete.FunctionClause clause = clauses.get(i);
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        Map<String, Concrete.NamePattern> usedNames = new HashMap<>();
        for (int j = 0; j < clause.getPatterns().size(); j++) {
          Referable constructor = visitPattern(clause.getPatterns().get(j), usedNames);
          if (constructor != null) {
            replaceWithConstructor(clause, j, constructor);
          }
          resolvePattern(clause.getPatterns().get(j));
        }

        Concrete.Expression newExpression = clause.getExpression() == null ? null : clause.getExpression().accept(this, null);
        if (newExpression != clause.getExpression()) {
          clauses.set(i, new Concrete.FunctionClause(clause.getData(), clause.getPatterns(), newExpression));
        }
      }
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      Concrete.Expression newExpression = expr.getExpressions().get(i).accept(this, null);
      if (newExpression != expr.getExpressions().get(i)) {
        expr.getExpressions().set(i, newExpression);
      }
    }
    visitClauses(expr.getClauses());
    return expr;
  }

  GlobalReferable visitPattern(Concrete.Pattern pattern, Map<String, Concrete.NamePattern> usedNames) {
    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      Referable referable = namePattern.getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) return null;
      Referable ref = myParentScope.resolveName(name);
      if (ref instanceof GlobalReferable) {
        return (GlobalReferable) ref;
      }
      if (!name.equals("_")) {
        Concrete.NamePattern prev = usedNames.put(name, namePattern);
        if (prev != null) {
          myErrorReporter.report(new DuplicateNameError(Error.Level.WARNING, referable, prev.getReferable()));
        }
        myContext.add(referable);
      }
      return null;
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      List<? extends Concrete.Pattern> patterns = ((Concrete.ConstructorPattern) pattern).getPatterns();
      for (int i = 0; i < patterns.size(); i++) {
        Referable constructor = visitPattern(patterns.get(i), usedNames);
        if (constructor != null) {
          replaceWithConstructor((Concrete.ConstructorPattern) pattern, i, constructor);
        }
      }
      return null;
    } else if (pattern instanceof Concrete.EmptyPattern) {
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  void resolvePattern(Concrete.Pattern pattern) {
    if (!(pattern instanceof Concrete.ConstructorPattern)) {
      return;
    }

    Referable referable = ((Concrete.ConstructorPattern) pattern).getConstructor();
    if (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }
    if (referable instanceof UnresolvedReference) {
      Referable newRef = ((UnresolvedReference) referable).resolve(myParentScope);
      if (newRef instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) newRef).getError());
      } else {
        ((Concrete.ConstructorPattern) pattern).setConstructor(newRef);
      }
    }

    for (Concrete.Pattern patternArg : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
      resolvePattern(patternArg);
    }
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    Concrete.Expression newExpression = expr.getExpression().accept(this, null);
    return newExpression == expr.getExpression() ? expr : new Concrete.ProjExpression(expr.getData(), newExpression, expr.getField());
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    Concrete.Expression baseClass = expr.getBaseClassExpression().accept(this, null);
    GlobalReferable classDef = Concrete.getUnderlyingClassDef(baseClass);
    if (classDef instanceof ClassReferable) {
      visitClassFieldImpls(expr.getStatements(), (ClassReferable) classDef);
    }
    return baseClass == expr.getBaseClassExpression() ? expr : new Concrete.ClassExtExpression(expr.getData(), baseClass, expr.getStatements());
  }

  void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, ClassReferable classDef) {
    for (int i = 0; i < classFieldImpls.size(); i++) {
      Concrete.ClassFieldImpl impl = classFieldImpls.get(i);
      Referable field = impl.getImplementedField();
      if (field instanceof RedirectingReferable) {
        field = ((RedirectingReferable) field).getOriginalReferable();
      }
      if (field instanceof UnresolvedReference) {
        Referable newField = ((UnresolvedReference) field).resolve(new ClassFieldImplScope(classDef, true));
        if (newField instanceof ErrorReference) {
          myErrorReporter.report(((ErrorReference) newField).getError());
        }
        impl.setImplementedField(newField);
      }

      Concrete.Expression implementation = impl.getImplementation().accept(this, null);
      if (implementation != impl.getImplementation()) {
        classFieldImpls.set(i, new Concrete.ClassFieldImpl(impl.getData(), impl.getImplementedField(), implementation));
      }
    }
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    Concrete.Expression newExpression = expr.getExpression().accept(this, null);
    return newExpression == expr.getExpression() ? expr : new Concrete.NewExpression(expr.getData(), newExpression);
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (int i = 0; i < expr.getClauses().size(); i++) {
        Concrete.LetClause clause = expr.getClauses().get(i);
        try (Utils.ContextSaver ignored1 = new Utils.ContextSaver(myContext)) {
          visitParameters(clause.getParameters());

          Concrete.Expression resultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, null);
          Concrete.Expression term = clause.getTerm().accept(this, null);
          if (resultType != clause.getResultType() || term != clause.getTerm()) {
            clause = new Concrete.LetClause(clause.getData(), clause.getParameters(), resultType, term);
            expr.getClauses().set(i, clause);
          }
        }

        ClassReferable classRef = getTypeClassReference(clause.getResultType());
        if (classRef != null) {
          for (Concrete.Parameter parameter : clause.getParameters()) {
            if (parameter.getExplicit()) {
              classRef = null;
              break;
            }
          }
        }
        myContext.add(classRef == null ? clause.getData() : new TypedRedirectingReferable(clause.getData(), classRef));
      }

      Concrete.Expression newExpression = expr.getExpression().accept(this, null);
      return newExpression == expr.getExpression() ? expr : new Concrete.LetExpression(expr.getData(), expr.getClauses(), newExpression);
    }
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return expr;
  }
}
