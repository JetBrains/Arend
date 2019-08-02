package org.arend.typechecking.visitor;

import org.arend.naming.reference.*;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class CollectDefCallsVisitor extends VoidConcreteVisitor<Boolean, Void> {
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
  public void visitFunctionHeader(Concrete.FunctionDefinition def, Boolean isHeader) {
    if (isHeader) {
      super.visitFunctionHeader(def, true);
    }
  }

  @Override
  public Void visitFunctionBody(Concrete.FunctionDefinition def, Boolean isHeader) {
    if (!isHeader) {
      super.visitFunctionBody(def, false);
    }
    return null;
  }

  @Override
  public void visitDataHeader(Concrete.DataDefinition def, Boolean isHeader) {
    if (isHeader) {
      super.visitDataHeader(def, true);
    }
  }

  @Override
  public Void visitDataBody(Concrete.DataDefinition def, Boolean isHeader) {
    if (!isHeader) {
      super.visitDataBody(def, false);
    }
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Boolean isHeader) {
    visitClassHeader(def, isHeader);

    myExcluded = new HashSet<>();
    new ClassFieldImplScope(def.getData(), false).find(ref -> {
      if (ref instanceof TCReferable) {
        myExcluded.add((TCReferable) ref);
      }
      return false;
    });

    visitClassBody(def, isHeader);

    myExcluded = null;
    return null;
  }

  @Override
  protected void visitPattern(Concrete.Pattern pattern, Boolean params) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Referable constructor = ((Concrete.ConstructorPattern) pattern).getConstructor();
      if (constructor instanceof TCReferable) {
        myDependencies.add((TCReferable) constructor);
      }
    }
    super.visitPattern(pattern, null);
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, Boolean ignore) {
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
  public Void visitReference(Concrete.ReferenceExpression expr, Boolean ignore) {
    if (expr.getReferent() instanceof TCReferable) {
      addDependency((TCReferable) expr.getReferent(), false);
    }
    return null;
  }
}
