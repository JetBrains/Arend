package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class ConvertingScope implements Scope {
  private final ReferableConverter myConverter;
  private final Scope myScope;

  public ConvertingScope(ReferableConverter converter, Scope scope) {
    myConverter = converter;
    myScope = scope;
  }

  private Referable convertReferable(Referable referable) {
    return referable instanceof ModuleReferable ? referable : referable instanceof LocatedReferable ? myConverter.toDataLocatedReferable((LocatedReferable) referable) : myConverter.toDataReferable(referable);
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    return myScope.find(ref -> pred.test(convertReferable(ref)));
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getElements() {
    Collection<? extends Referable> elements = myScope.getElements();
    List<Referable> result = new ArrayList<>(elements.size());
    for (Referable element : elements) {
      result.add(convertReferable(element));
    }
    return result;
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    return convertReferable(myScope.resolveName(name));
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name) {
    Scope scope = myScope.resolveNamespace(name);
    return scope == null ? null : new ConvertingScope(myConverter, scope);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    Scope subscope = myScope.getGlobalSubscope();
    return subscope == myScope ? this : new ConvertingScope(myConverter, subscope);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    Scope subscope = myScope.getGlobalSubscopeWithoutOpens();
    return subscope == myScope ? this : new ConvertingScope(myConverter, subscope);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myScope.getImportedSubscope();
  }
}
