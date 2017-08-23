package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MergeScope implements MergingScope {
  private final Iterable<Scope> myScopes;

  public MergeScope(Iterable<Scope> scopes) {
    myScopes = scopes;
  }

  @Override
  public Set<String> getNames() {
    return StreamSupport.stream(myScopes.spliterator(), false).flatMap(s -> s.getNames().stream()).collect(Collectors.toSet());
  }

  private InvalidScopeException createException(final Referable ref1, final Referable ref2) {
    return new InvalidScopeException() {
      @Override
      public GeneralError toError() {
        return new DuplicateNameError(Error.Level.ERROR, ref1, ref2, (Concrete.SourceNode) ref1); // TODO[abstract]
      }
    };

  }

  @Override
  public Referable resolveName(String name) {
    Referable resolved = null;
    for (Scope scope : myScopes) {
      Referable ref = scope.resolveName(name);
      if (ref != null) {
        if (resolved == null) {
          resolved = ref;
        } else {
          throw createException(ref, resolved);
        }
      }
    }
    return resolved;
  }

  @Override
  public void findIntroducedDuplicateNames(BiConsumer<Referable, Referable> reporter) {
    Multimap<String, Referable> known = HashMultimap.create();
    for (Scope scope : myScopes) {
      for (String name : scope.getNames()) {
        try {
          Referable ref = scope.resolveName(name);
          for (Referable prev : known.get(name)) {
            if (prev != ref) {
              reporter.accept(ref, prev);
            }
          }
          known.put(name, ref);
        } catch (InvalidScopeException ignored) {
        }
      }
    }
  }

  @Override
  public void findIntroducedDuplicateInstances(BiConsumer<Concrete.Instance, Concrete.Instance> reporter) {
    Multimap<Pair<Referable, Referable>, Concrete.Instance> known = HashMultimap.create();
    for (Scope scope : myScopes) {
      for (Concrete.Instance instance : scope.getInstances()) {
        Pair<Referable, Referable> pair = new Pair<>(instance.getClassView().getReferent(), instance.getClassifyingDefinition());
        for (Concrete.Instance prev : known.get(pair)) {
          if (prev != instance) {
            reporter.accept(prev, instance);
          }
        }
        known.put(pair, instance);
      }
    }
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    return StreamSupport.stream(myScopes.spliterator(), false).flatMap(s -> s.getInstances().stream()).collect(Collectors.toSet());
  }
}
