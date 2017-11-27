package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.BinOpParser;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.scope.ClassFieldImplScope;
import com.jetbrains.jetpad.vclang.naming.scope.ListScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.*;

public class ExpressionResolveNameVisitor implements ConcreteExpressionVisitor<Void, Void> {
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final LocalErrorReporter myErrorReporter;

  public ExpressionResolveNameVisitor(Scope parentScope, List<Referable> context, LocalErrorReporter errorReporter) {
    myParentScope = parentScope;
    myScope = context == null ? parentScope : new MergeScope(new ListScope(context), parentScope);
    myContext = context;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable referable = expr.getReferent();
    if (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }
    if (referable instanceof UnresolvedReference) {
      referable = ((UnresolvedReference) referable).resolve(myScope);
      expr.setReferent(referable);
      if (referable instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) referable).getError());
      }
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  void visitParameters(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) parameter).getType().accept(this, null);
      }
      if (parameter instanceof Concrete.TelescopeParameter) {
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
            myContext.add(referable);
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

  @Override
  public Void visitLam(Concrete.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.getBody().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitPi(Concrete.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.getCodomain().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitInferHole(Concrete.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Void params) {
    Concrete.Expression expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression expr, Void params) {
    for (Concrete.Expression expression : expr.getFields()) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
    }
    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    if (expr.getSequence().isEmpty()) {
      Concrete.Expression left = expr.getLeft();
      left.accept(this, null);
      expr.replace(left);
    } else {
      BinOpParser parser = new BinOpParser(myErrorReporter);

      expr.getLeft().accept(this, null);
      parser.push(expr.getLeft(), true);
      for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
        Precedence precedence = elem.binOp.getReferent() instanceof GlobalReferable ? ((GlobalReferable) elem.binOp.getReferent()).getPrecedence() : null;
        visitReference(elem.binOp, null);
        if (precedence == null) {
          precedence = elem.binOp.getReferent() instanceof GlobalReferable ? ((GlobalReferable) elem.binOp.getReferent()).getPrecedence() : Precedence.DEFAULT;
        }
        parser.push(elem.binOp, precedence, elem.argument == null);

        if (elem.argument != null) {
          elem.argument.accept(this, null);
          parser.push(elem.argument, true);
        }
      }

      expr.replace(parser.rollUp());
    }
    return null;
  }

  static  void replaceWithConstructor(Concrete.PatternContainer container, int index, Referable constructor) {
    Concrete.Pattern old = container.getPatterns().get(index);
    Concrete.Pattern newPattern = new Concrete.ConstructorPattern(old.getData(), constructor, Collections.emptyList());
    newPattern.setExplicit(old.isExplicit());
    container.getPatterns().set(index, newPattern);
  }

  void visitClauses(List<? extends Concrete.FunctionClause> clauses) {
    if (clauses.isEmpty()) {
      return;
    }
    for (Concrete.FunctionClause clause : clauses) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        Map<String, Concrete.NamePattern> usedNames = new HashMap<>();
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          Referable constructor = visitPattern(clause.getPatterns().get(i), usedNames);
          if (constructor != null) {
            replaceWithConstructor(clause, i, constructor);
          }
          resolvePattern(clause.getPatterns().get(i));
        }

        if (clause.getExpression() != null)
          clause.getExpression().accept(this, null);
      }
    }
  }

  @Override
  public Void visitCase(Concrete.CaseExpression expr, Void params) {
    for (Concrete.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    visitClauses(expr.getClauses());
    return null;
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
  public Void visitProj(Concrete.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    GlobalReferable classDef = Concrete.getUnderlyingClassDef(expr);
    if (classDef instanceof ClassReferable) {
      visitClassFieldImpls(expr.getStatements(), (ClassReferable) classDef);
    }
    return null;
  }

  void visitClassFieldImpls(Collection<? extends Concrete.ClassFieldImpl> classFieldImpls, ClassReferable classDef) {
    for (Concrete.ClassFieldImpl impl : classFieldImpls) {
      Referable field = impl.getImplementedField();
      if (field instanceof RedirectingReferable) {
        field = ((RedirectingReferable) field).getOriginalReferable();
      }
      if (field instanceof UnresolvedReference) {
        impl.setImplementedField(((UnresolvedReference) field).resolve(new ClassFieldImplScope(classDef)));
      }
      impl.getImplementation().accept(this, null);
    }
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.LetClause clause : expr.getClauses()) {
        try (Utils.ContextSaver ignored1 = new Utils.ContextSaver(myContext)) {
          visitParameters(clause.getParameters());

          if (clause.getResultType() != null) {
            clause.getResultType().accept(this, null);
          }
          clause.getTerm().accept(this, null);
        }
        myContext.add(clause.getData());
      }

      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return null;
  }
}
