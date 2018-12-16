package org.arend.naming.scope;

import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;

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
    Referable origRef = referable instanceof RedirectingReferable ? ((RedirectingReferable) referable).getOriginalReferable() : referable;
    while (origRef instanceof RedirectingReferable) {
      origRef = ((RedirectingReferable) origRef).getOriginalReferable();
    }

    if (origRef instanceof ModuleReferable) {
      return origRef;
    }

    origRef = origRef instanceof LocatedReferable ? myConverter.toDataLocatedReferable((LocatedReferable) origRef) : myConverter.toDataReferable(origRef);
    if (referable instanceof RedirectingReferable) {
      return new RedirectingReferableImpl(origRef, ((RedirectingReferable) referable).getPrecedence(), referable.textRepresentation());
    } else {
      return origRef;
    }
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    return myScope.find(ref -> {
      Referable convertedRef = convertReferable(ref);
      return convertedRef != null && pred.test(convertedRef);
    });
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getElements() {
    Collection<? extends Referable> elements = myScope.getElements();
    List<Referable> result = new ArrayList<>(elements.size());
    for (Referable element : elements) {
      Referable ref = convertReferable(element);
      if (ref != null) {
        result.add(ref);
      }
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
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    Scope scope = myScope.resolveNamespace(name, onlyInternal);
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
