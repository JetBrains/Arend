package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.NamespaceCommandNamespace;
import org.arend.naming.scope.Scope;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.Group;

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
    private final ReferableConverter referableConverter;
    private SimpleInstanceProvider instanceProvider;
    private boolean used = false;

    private MyPredicate(ReferableConverter referableConverter) {
      this.referableConverter = referableConverter;
      this.instanceProvider = new SimpleInstanceProvider();
    }

    public LocatedReferable recordInstances(LocatedReferable ref) {
      TCReferable tcRef = referableConverter.toDataLocatedReferable(ref);
      if (tcRef != null) {
        myProviders.put(tcRef, instanceProvider);
      }
      return ref;
    }

    @Override
    public boolean test(Referable ref) {
      if (ref instanceof LocatedReferable) {
        TCReferable instance = referableConverter.toDataLocatedReferable((LocatedReferable) ref);
        if (instance != null && instance.getKind() == GlobalReferable.Kind.INSTANCE) {
          if (used) {
            instanceProvider = new SimpleInstanceProvider(instanceProvider);
            used = false;
          }
          instanceProvider.put(instance);
        }
      }
      return false;
    }
  }

  public boolean collectInstances(Group group, Scope parentScope, LocatedReferable referable, ReferableConverter referableConverter) {
    if (!myCollected.add(group)) {
      return false;
    }

    var predicate = new MyPredicate(referableConverter);
    parentScope.find(predicate);
    processGroup(group, parentScope, predicate);
    predicate.recordInstances(referable);
    return true;
  }

  public boolean collectInstances(Group group, Scope parentScope, ReferableConverter referableConverter) {
    return collectInstances(group, parentScope, group.getReferable(), referableConverter);
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
      NamespaceCommandNamespace.resolveNamespace(command.getKind() == NamespaceCommand.Kind.IMPORT ? parentScope.getImportedSubscope() : parentScope, command).find(predicate);
    }
    processSubgroups(parentScope, predicate, dynamicSubgroups);
    processSubgroups(parentScope, predicate, subgroups);
  }

  private void processSubgroups(Scope parentScope, MyPredicate predicate, Collection<? extends Group> subgroups) {
    for (Group subgroup : subgroups) {
      SimpleInstanceProvider instanceProvider = predicate.instanceProvider;
      processGroup(subgroup, parentScope, predicate);
      LocatedReferable ref = predicate.recordInstances(subgroup.getReferable());
      predicate.used = true;
      predicate.instanceProvider = instanceProvider;
      predicate.test(ref);
    }
  }
}
