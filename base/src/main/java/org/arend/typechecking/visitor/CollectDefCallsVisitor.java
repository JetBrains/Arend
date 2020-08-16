package org.arend.typechecking.visitor;

import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.DefinableMetaDefinition;

import java.util.*;

public class CollectDefCallsVisitor extends VoidConcreteVisitor<Void, Void> {
  private final Collection<TCReferable> myDependencies;
  private final Collection<MetaReferable> myMetas;
  private final boolean myWithBodies;
  private Set<TCReferable> myExcluded;

  public CollectDefCallsVisitor(Collection<TCReferable> dependencies, Collection<MetaReferable> metas, boolean withBodies) {
    myDependencies = dependencies;
    myMetas = metas;
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
    if (expr.getReferent() instanceof MetaReferable) {
      var ref = (MetaReferable) expr.getReferent();
      myMetas.add(ref);
      if (ref.getDefinition() instanceof DefinableMetaDefinition) {
        ((DefinableMetaDefinition) ref.getDefinition()).accept(this, null);
      }
    }
    return null;
  }
}
