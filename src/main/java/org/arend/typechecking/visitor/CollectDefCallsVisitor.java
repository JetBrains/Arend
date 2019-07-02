package org.arend.typechecking.visitor;

import org.arend.naming.reference.*;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class CollectDefCallsVisitor extends VoidConcreteVisitor<Void, Boolean> {
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
    addDependency(dependency, false);
  }

  private void addDependency(TCReferable dependency, boolean ignoreFirstParameter) {
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

      if (referable instanceof FieldReferable) {
        if (!ignoreFirstParameter) {
          LocatedReferable fieldParent = referable.getLocatedReferableParent();
          if (fieldParent instanceof ClassReferable) {
            addClassInstances((ClassReferable) fieldParent);
          }
        }
      } else {
        Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete(referable);
        if (definition != null && !definition.isDesugarized()) {
          TCClassReferable enclosingClass = definition.getRelatedDefinition().enclosingClass;
          if (enclosingClass != null) {
            if (ignoreFirstParameter) {
              ignoreFirstParameter = false;
            } else {
              addClassInstances(enclosingClass);
            }
          }
        }

        Collection<? extends Concrete.TypeParameter> parameters = Concrete.getParameters(definition, false);
        if (parameters != null) {
          for (Concrete.TypeParameter parameter : parameters) {
            if (ignoreFirstParameter) {
              ignoreFirstParameter = false;
            } else if (!parameter.getExplicit()) {
              TCClassReferable classRef = parameter.getType().getUnderlyingTypeClass();
              if (classRef != null) {
                addClassInstances(classRef);
              }
            }
          }
        }
      }
    }
  }

  private void addClassInstances(ClassReferable classRef) {
    myInstanceProvider.findInstance(classRef, instance -> {
      myDeque.push(instance.getData());
      return false;
    });
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Boolean isHeader) {
    visitParameters(def.getParameters(), null);

    if (isHeader) {
      if (def.getResultType() != null) {
        def.getResultType().accept(this, null);
      }
      if (def.getResultTypeLevel() != null) {
        def.getResultTypeLevel().accept(this, null);
      }
    } else {
      Concrete.FunctionBody body = def.getBody();
      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody) body).getTerm().accept(this, null);
      }
      visitClassFieldImpls(body.getClassFieldImpls(), null);
      visitClauses(body.getClauses(), null);
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Boolean isHeader) {
    if (isHeader) {
      visitParameters(def.getParameters(), null);
      Concrete.Expression universe = def.getUniverse();
      if (universe != null) {
        universe.accept(this, null);
      }
    } else {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        if (clause.getPatterns() != null) {
          for (Concrete.Pattern pattern : clause.getPatterns()) {
            visitPattern(pattern, null);
          }
        }
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitConstructor(constructor);
        }
      }
    }

    return null;
  }

  @Override
  protected void visitPattern(Concrete.Pattern pattern, Void params) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Referable constructor = ((Concrete.ConstructorPattern) pattern).getConstructor();
      if (constructor instanceof TCReferable) {
        myDependencies.add((TCReferable) constructor);
      }
    }
    super.visitPattern(pattern, null);
  }

  private void visitConstructor(Concrete.Constructor def) {
    visitParameters(def.getParameters(), null);
    if (def.getResultType() != null) {
      def.getResultType().accept(this, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      visitClauses(def.getClauses(), null);
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
      visitParameters(field.getParameters(), null);
      field.getResultType().accept(this, null);
      if (field.getResultTypeLevel() != null) {
        field.getResultTypeLevel().accept(this, null);
      }
    }

    visitClassFieldImpls(def.getImplementations(), null);
    myExcluded = null;
    return null;
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, Void ignore) {
    if (expr.getFunction() instanceof Concrete.ReferenceExpression) {
      Referable ref = ((Concrete.ReferenceExpression) expr.getFunction()).getReferent();
      if (ref instanceof TCReferable) {
        addDependency((TCReferable) ref, !expr.getArguments().get(0).isExplicit());
      }
    } else {
      expr.getFunction().accept(this, null);
    }
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void ignore) {
    if (expr.getReferent() instanceof TCReferable) {
      addDependency((TCReferable) expr.getReferent(), false);
    }
    return null;
  }
}
