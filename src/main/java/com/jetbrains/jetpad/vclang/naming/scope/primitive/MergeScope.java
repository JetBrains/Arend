package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.term.Abstract;
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

  private InvalidScopeException createException(final Abstract.ReferableSourceNode ref1, final Abstract.ReferableSourceNode ref2) {
    return new InvalidScopeException() {
      @Override
      public GeneralError toError() {
        return new DuplicateNameError(Error.Level.ERROR, ref1, ref2, (Concrete.SourceNode) ref1);
      }
    };

  }

  @Override
  public Abstract.ReferableSourceNode resolveName(String name) {
    Abstract.ReferableSourceNode resolved = null;
    for (Scope scope : myScopes) {
      Abstract.ReferableSourceNode ref = scope.resolveName(name);
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
  public void findIntroducedDuplicateNames(BiConsumer<Abstract.ReferableSourceNode, Abstract.ReferableSourceNode> reporter) {
    Multimap<String, Abstract.ReferableSourceNode> known = HashMultimap.create();
    for (Scope scope : myScopes) {
      for (String name : scope.getNames()) {
        try {
          Abstract.ReferableSourceNode ref = scope.resolveName(name);
          for (Abstract.ReferableSourceNode prev : known.get(name)) {
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
  public void findIntroducedDuplicateInstances(BiConsumer<Abstract.ClassViewInstance, Abstract.ClassViewInstance> reporter) {
    Multimap<Pair<Abstract.ReferableSourceNode, Abstract.ReferableSourceNode>, Abstract.ClassViewInstance> known = HashMultimap.create();
    for (Scope scope : myScopes) {
      for (Abstract.ClassViewInstance instance : scope.getInstances()) {
        Pair<Abstract.ReferableSourceNode, Abstract.ReferableSourceNode> pair = new Pair<>(instance.getClassView().getReferent(), instance.getClassifyingDefinition());
        for (Abstract.ClassViewInstance prev : known.get(pair)) {
          if (prev != instance) {
            reporter.accept(prev, instance);
          }
        }
        known.put(pair, instance);
      }
    }
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return StreamSupport.stream(myScopes.spliterator(), false).flatMap(s -> s.getInstances().stream()).collect(Collectors.toSet());
  }
}
