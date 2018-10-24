package org.arend.typechecking.visitor;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class CollectDefCallsVisitor implements ConcreteDefinitionVisitor<Boolean, Void>, ConcreteExpressionVisitor<Void, Void> {
  private final ConcreteProvider myConcreteProvider;
  private final InstanceProvider myInstanceProvider;
  private final Collection<TCReferable> myDependencies;
  private final Deque<TCReferable> myDeque = new ArrayDeque<>();
  private Set<TCReferable> myExcluded;

  public CollectDefCallsVisitor(ConcreteProvider concreteProvider, InstanceProvider instanceProvider, Collection<TCReferable> dependencies) {
    myConcreteProvider = concreteProvider;
    myInstanceProvider = instanceProvider;
    myDependencies = dependencies;
  }

  public void addDependency(TCReferable dependency) {
    if (myExcluded != null && myExcluded.contains(dependency)) {
      return;
    }
    if (myInstanceProvider == null) {
      myDependencies.add(dependency);
      return;
    }

    myDeque.push(dependency);
    while (!myDeque.isEmpty()) {
      TCReferable referable = myDeque.pop();
      if (!myDependencies.add(referable)) {
        continue;
      }

      Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete(referable);
      if (definition instanceof Concrete.ClassField) {
        ClassReferable classRef = ((Concrete.ClassField) definition).getRelatedDefinition().getData();
        for (Concrete.Instance instance : myInstanceProvider.getInstances()) {
          Referable ref = instance.getReferenceInType();
          if (ref instanceof ClassReferable && ((ClassReferable) ref).isSubClassOf(classRef)) {
            myDeque.push(instance.getData());
          }
        }
      } else if (definition != null) {
        Collection<? extends Concrete.TypeParameter> parameters = Concrete.getParameters(definition);
        if (parameters != null) {
          for (Concrete.TypeParameter parameter : parameters) {
            TCClassReferable classRef = parameter.getType().getUnderlyingClassReferable(true);
            if (classRef != null) {
              for (Concrete.Instance instance : myInstanceProvider.getInstances()) {
                Referable ref = instance.getReferenceInType();
                if (ref instanceof ClassReferable && ((ClassReferable) ref).isSubClassOf(classRef)) {
                  myDeque.push(instance.getData());
                }
              }
            }
          }
        }
      }
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Boolean isHeader) {
    for (Concrete.TelescopeParameter param : def.getParameters()) {
      param.getType().accept(this, null);
    }

    if (isHeader) {
      Concrete.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(this, null);
      }
    } else {
      Concrete.FunctionBody body = def.getBody();
      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody) body).getTerm().accept(this, null);
      }
      visitClassFieldImpls(body.getClassFieldImpls());
      visitClauses(body.getClauses());
    }

    return null;
  }

  private void visitClauses(List<Concrete.FunctionClause> clauses) {
    for (Concrete.FunctionClause clause : clauses) {
      for (Concrete.Pattern pattern : clause.getPatterns()) {
        visitPattern(pattern);
      }
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, null);
      }
    }
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Boolean isHeader) {
    if (isHeader) {
      for (Concrete.TypeParameter param : def.getParameters()) {
        param.getType().accept(this, null);
      }

      Concrete.Expression universe = def.getUniverse();
      if (universe != null) {
        universe.accept(this, null);
      }
    } else {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        if (clause.getPatterns() != null) {
          for (Concrete.Pattern pattern : clause.getPatterns()) {
            visitPattern(pattern);
          }
        }
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitConstructor(constructor);
        }
      }
    }

    return null;
  }

  private void visitPattern(Concrete.Pattern pattern) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      if (conPattern.getConstructor() instanceof TCReferable) {
        addDependency((TCReferable) conPattern.getConstructor());
      }
      for (Concrete.Pattern patternArg : conPattern.getPatterns()) {
        visitPattern(patternArg);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern patternArg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        visitPattern(patternArg);
      }
    }
  }

  private void visitConstructor(Concrete.Constructor def) {
    for (Concrete.TypeParameter param : def.getParameters()) {
      param.getType().accept(this, null);
    }
    if (def.getResultType() != null) {
      def.getResultType().accept(this, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      visitClauses(def.getClauses());
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Boolean params) {
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      visitReference(superClass, null);
    }

    myExcluded = new HashSet<>();
    new ClassFieldImplScope(def.getData(), false).find(ref -> {
      if (ref instanceof TCReferable) {
        myExcluded.add((TCReferable) ref);
      }
      return false;
    });

    for (Concrete.ClassField field : def.getFields()) {
      field.getResultType().accept(this, null);
    }

    visitClassFieldImpls(def.getImplementations());
    myExcluded = null;
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Boolean params) {
    for (Concrete.Parameter param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(this, null);
      }
    }

    def.getResultType().accept(this, null);
    visitClassFieldImpls(def.getClassFieldImpls());
    return null;
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void ignore) {
    if (expr.getReferent() instanceof TCReferable) {
      TCReferable ref = ((TCReferable) expr.getReferent()).getUnderlyingTypecheckable();
      if (ref != null) {
        addDependency(ref);
      }
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLam(Concrete.LamExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getBody().accept(this, null);
    return null;
  }

  private void visitParameters(List<? extends Concrete.Parameter> params) {
    for (Concrete.Parameter param : params) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitPi(Concrete.PiExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitHole(Concrete.HoleExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression expr, Void ignore) {
    for (Concrete.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void ignore) {
    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      elem.expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitCase(Concrete.CaseExpression expr, Void ignore) {
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      caseArg.expression.accept(this, null);
      if (caseArg.type != null) {
        caseArg.type.accept(this, null);
      }
    }
    if (expr.getResultType() != null) {
      expr.getResultType().accept(this, null);
    }
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    visitClassFieldImpls(expr.getStatements());
    return null;
  }

  private void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls) {
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      if (classFieldImpl.implementation != null) {
        classFieldImpl.implementation.accept(this, null);
      }
      visitClassFieldImpls(classFieldImpl.subClassFieldImpls);
    }
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression letExpression, Void ignore) {
    for (Concrete.LetClause clause : letExpression.getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }
    letExpression.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTyped(Concrete.TypedExpression expr, Void params) {
    expr.expression.accept(this, null);
    expr.type.accept(this, null);
    return null;
  }
}
