package org.arend.typechecking.visitor;

import org.arend.naming.reference.*;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.provider.ConcreteProvider;

import java.util.*;

public class CollectDefCallsVisitor extends VoidConcreteVisitor<Void, Void> {
  private final ConcreteProvider myConcreteProvider;
  private final InstanceProvider myInstanceProvider;
  private final Collection<TCReferable> myDependencies;
  private final Deque<TCReferable> myDeque = new ArrayDeque<>();
  private final boolean myWithBodies;
  private Set<TCReferable> myExcluded;

  public CollectDefCallsVisitor(ConcreteProvider concreteProvider, InstanceProvider instanceProvider, Collection<TCReferable> dependencies, boolean withBodies) {
    myConcreteProvider = concreteProvider;
    myInstanceProvider = instanceProvider;
    myDependencies = dependencies;
    myWithBodies = withBodies;
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
            } else if (!parameter.isExplicit()) {
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
  protected Void visitFunctionBody(Concrete.FunctionDefinition def, Void params) {
    if (myWithBodies) {
      super.visitFunctionBody(def, params);
    }
    return null;
  }

  @Override
  protected Void visitDataBody(Concrete.DataDefinition def, Void params) {
    if (myWithBodies) {
      super.visitDataBody(def, params);
    }
    return null;
  }

  @Override
  protected Void visitClassBody(Concrete.ClassDefinition def, Void params) {
    if (myWithBodies) {
      super.visitClassBody(def, params);
    }
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    visitClassHeader(def, null);

    myExcluded = new HashSet<>();
    new ClassFieldImplScope(def.getData(), false).find(ref -> {
      if (ref instanceof TCReferable) {
        myExcluded.add((TCReferable) ref);
      }
      return false;
    });

    visitClassBody(def, null);

    myExcluded = null;
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

  @Override
  public Void visitApp(Concrete.AppExpression expr, Void params) {
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
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr.getReferent() instanceof TCReferable) {
      addDependency((TCReferable) expr.getReferent(), false);
    }
    return null;
  }
}
