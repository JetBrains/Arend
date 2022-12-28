package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteResolvedClassReferable extends ConcreteLocatedReferable implements ClassReferable {
  protected final List<ClassReferable> superClasses;
  protected final List<ConcreteClassFieldReferable> fields;
  protected List<GlobalReferable> dynamicReferables = Collections.emptyList();

  public ConcreteResolvedClassReferable(Object data, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, LocatedReferable parent, List<ConcreteClassFieldReferable> fields) {
    super(data, name, precedence, aliasName, aliasPrecedence, parent, Kind.CLASS);
    superClasses = new ArrayList<>();
    this.fields = fields;
  }

  @Override
  public Concrete.ClassDefinition getDefinition() {
    return (Concrete.ClassDefinition) super.getDefinition();
  }

  protected boolean setFromConcrete() {
    return true;
  }

  @Override
  public void setDefinition(Concrete.ReferableDefinition definition) {
    super.setDefinition(definition);
    if (!setFromConcrete()) {
      return;
    }
    if (!(definition instanceof Concrete.ClassDefinition)) {
      throw new IllegalArgumentException();
    }

    for (Concrete.ReferenceExpression superClass : ((Concrete.ClassDefinition) definition).getSuperClasses()) {
      Referable ref = superClass.getReferent();
      if (!(ref instanceof ClassReferable)) {
        ref = ref.getUnderlyingReferable();
      }
      if (!(ref instanceof ClassReferable)) {
        throw new IllegalArgumentException();
      }
      superClasses.add((ClassReferable) ref);
    }

    for (Concrete.ClassElement element : ((Concrete.ClassDefinition) definition).getElements()) {
      if (element instanceof Concrete.ClassField) {
        Referable ref = ((Concrete.ClassField) element).getData();
        if (!(ref instanceof ConcreteClassFieldReferable)) {
          throw new IllegalArgumentException();
        }
        fields.add((ConcreteClassFieldReferable) ref);
      }
    }
  }

  @Override
  public boolean isRecord() {
    return getDefinition().isRecord();
  }

  @Override
  public @NotNull List<? extends ClassReferable> getSuperClassReferences() {
    return superClasses;
  }

  @Override
  public @NotNull Collection<? extends FieldReferable> getFieldReferables() {
    return fields;
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getImplementedFields() {
    List<Referable> result = new ArrayList<>();
    for (Concrete.ClassElement element : getDefinition().getElements()) {
      if (element instanceof Concrete.ClassFieldImpl) {
        result.add(((Concrete.ClassFieldImpl) element).getImplementedField());
      }
    }
    return result;
  }

  @Override
  public @NotNull Collection<? extends GlobalReferable> getDynamicReferables() {
    return dynamicReferables;
  }

  public void addDynamicReferable(GlobalReferable ref) {
    if (dynamicReferables.isEmpty()) {
      dynamicReferables = new ArrayList<>();
    }
    dynamicReferables.add(ref);
  }
}
