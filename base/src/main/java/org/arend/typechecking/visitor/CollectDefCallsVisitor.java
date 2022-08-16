package org.arend.typechecking.visitor;

import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;

import java.util.*;

public class CollectDefCallsVisitor extends VoidConcreteVisitor<Void> {
  private final Collection<TCReferable> myDependencies;
  private final boolean myWithBodies;
  private Set<TCReferable> myExcluded;

  public CollectDefCallsVisitor(Collection<TCReferable> dependencies, boolean withBodies) {
    myDependencies = dependencies;
    myWithBodies = withBodies;
  }

  public void addDependency(TCReferable dependency) {
    myDependencies.add(dependency);
  }

  @Override
  protected Void visitFunctionBody(Concrete.BaseFunctionDefinition def, Void params) {
    if (myWithBodies) {
      super.visitFunctionBody(def, params);
    }
    return null;
  }

  @Override
  protected void visitDataBody(Concrete.DataDefinition def, Void params) {
    if (myWithBodies) {
      super.visitDataBody(def, params);
    }
  }

  @Override
  protected void visitClassBody(Concrete.ClassDefinition def, Void params) {
    if (myWithBodies) {
      super.visitClassBody(def, params);
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    visitClassHeader(def, null);

    myExcluded = new HashSet<>();
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        myExcluded.add(((Concrete.ClassField) element).getData());
      }
    }

    visitClassBody(def, null);

    myExcluded = null;
    return null;
  }

  @Override
  protected void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, Void params) {
    if (classFieldImpl.implementation != null && !(classFieldImpl instanceof Concrete.CoClauseFunctionReference && classFieldImpl.isDefault())) {
      classFieldImpl.implementation.accept(this, params);
    }
    visitElements(classFieldImpl.getSubCoclauseList(), params);
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
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr.getReferent() instanceof TCReferable) {
      TCReferable ref = (TCReferable) expr.getReferent();
      if (myExcluded == null || !myExcluded.contains(ref)) {
        myDependencies.add(ref);
      }
    }
    return null;
  }
}
