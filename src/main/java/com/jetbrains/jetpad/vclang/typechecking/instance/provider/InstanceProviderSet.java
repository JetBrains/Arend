package com.jetbrains.jetpad.vclang.typechecking.instance.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceCommandNamespace;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class InstanceProviderSet {
  private final Map<TCReferable, InstanceProvider> myProviders = new HashMap<>();
  private final Set<Group> myCollected = new HashSet<>();

  public void put(TCReferable referable, InstanceProvider provider) {
    myProviders.put(referable, provider);
  }

  public InstanceProvider get(TCReferable referable) {
    return myProviders.get(referable);
  }

  public InstanceProvider computeIfAbsent(TCReferable referable, Function<? super TCReferable, ? extends InstanceProvider> fun) {
    return myProviders.computeIfAbsent(referable, fun);
  }

  private class MyPredicate implements Predicate<Referable> {
    private final ConcreteProvider concreteProvider;
    private SimpleInstanceProvider instanceProvider;
    private boolean used = false;
    boolean recordInstances = false;

    private MyPredicate(ConcreteProvider concreteProvider) {
      this.concreteProvider = concreteProvider;
      this.instanceProvider = new SimpleInstanceProvider();
    }

    @Override
    public boolean test(Referable ref) {
      if (ref instanceof TCReferable) {
        Concrete.Instance instance = concreteProvider.getConcreteInstance((GlobalReferable) ref);
        if (instance != null) {
          Referable classRef = instance.getReferenceInType();
          if (classRef instanceof ClassReferable) {
            if (used) {
              instanceProvider = new SimpleInstanceProvider(instanceProvider);
              used = false;
            }
            instanceProvider.put(((ClassReferable) classRef).getUnderlyingTypecheckable(), instance);
          }
        }

        if (recordInstances) {
          myProviders.put((TCReferable) ref, instanceProvider);
          used = true;
        }
      }
      return false;
    }
  }

  public boolean collectInstances(Group group, Scope parentScope, ConcreteProvider concreteProvider) {
    if (!myCollected.add(group)) {
      return false;
    }

    MyPredicate predicate = new MyPredicate(concreteProvider);
    parentScope.find(predicate);
    predicate.recordInstances = true;
    predicate.test(group.getReferable());
    processGroup(group, parentScope, predicate);
    return true;
  }

  private void processGroup(Group group, Scope parentScope, MyPredicate predicate) {
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();
    Collection<? extends Group> subgroups = group.getSubgroups();
    if (namespaceCommands.isEmpty() && dynamicSubgroups.isEmpty() && subgroups.isEmpty()) {
      return;
    }

    parentScope = CachingScope.make(LexicalScope.insideOf(group, parentScope));
    for (NamespaceCommand command : namespaceCommands) {
      NamespaceCommandNamespace.makeNamespace(Scope.Utils.resolveNamespace(parentScope, command.getPath()), command).find(predicate);
    }
    processSubgroups(parentScope, predicate, dynamicSubgroups);
    processSubgroups(parentScope, predicate, subgroups);
  }

  private void processSubgroups(Scope parentScope, MyPredicate predicate, Collection<? extends Group> subgroups) {
    for (Group subgroup : subgroups) {
      predicate.test(subgroup.getReferable());
      SimpleInstanceProvider instanceProvider = predicate.instanceProvider;
      processGroup(subgroup, parentScope, predicate);
      predicate.used = true;
      predicate.instanceProvider = instanceProvider;
    }
  }
}
