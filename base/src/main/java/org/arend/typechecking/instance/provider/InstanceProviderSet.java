package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.*;
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
  private final Map<TCDefReferable, InstanceProvider> myProviders = new HashMap<>();
  private final Set<Group> myCollected = new HashSet<>();

  public void put(TCDefReferable referable, InstanceProvider provider) {
    myProviders.put(referable, provider);
  }

  public InstanceProvider get(TCReferable referable) {
    return referable instanceof TCDefReferable ? myProviders.get(referable) : null;
  }

  public InstanceProvider computeIfAbsent(TCDefReferable referable, Function<? super TCDefReferable, ? extends InstanceProvider> fun) {
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
      if (instanceProvider.isEmpty()) return ref;
      TCReferable tcRef = referableConverter.toDataLocatedReferable(ref);
      if (tcRef instanceof TCDefReferable) {
        SimpleInstanceProvider instanceProvider = this.instanceProvider;
        if (tcRef.getKind() == GlobalReferable.Kind.INSTANCE) {
          instanceProvider = new SimpleInstanceProvider(instanceProvider);
          instanceProvider.remove((TCDefReferable) tcRef);
        }
        myProviders.put((TCDefReferable) tcRef, instanceProvider);
      }
      return tcRef;
    }

    void test(int index, Referable ref) {
      if (ref instanceof LocatedReferable) {
        TCReferable instance = referableConverter.toDataLocatedReferable((LocatedReferable) ref);
        if (instance instanceof TCDefReferable && instance.getKind() == GlobalReferable.Kind.INSTANCE) {
          if (used) {
            instanceProvider = new SimpleInstanceProvider(instanceProvider);
            used = false;
          }
          instanceProvider.add(index, (TCDefReferable) instance);
        }
      }
    }

    @Override
    public boolean test(Referable ref) {
      test(-1, ref);
      return false;
    }
  }

  public boolean collectInstances(Group group, Scope parentScope, LocatedReferable referable, ReferableConverter referableConverter) {
    if (!myCollected.add(group)) {
      return false;
    }

    var predicate = new MyPredicate(referableConverter);
    parentScope.find(predicate);
    predicate.instanceProvider.reverseFrom(0);
    processGroup(group, parentScope, predicate);
    predicate.recordInstances(referable);
    return true;
  }

  public boolean collectInstances(Group group, Scope parentScope, ReferableConverter referableConverter) {
    return collectInstances(group, parentScope, group.getReferable(), referableConverter);
  }

  private void processGroup(Group group, Scope parentScope, MyPredicate predicate) {
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();
    Collection<? extends Group> subgroups = group.getSubgroups();
    if (namespaceCommands.isEmpty() && subgroups.isEmpty()) {
      return;
    }

    parentScope = CachingScope.make(LexicalScope.insideOf(group, parentScope));
    for (NamespaceCommand command : namespaceCommands) {
      int size = predicate.instanceProvider.getInstances().size();
      NamespaceCommandNamespace.resolveNamespace(command.getKind() == NamespaceCommand.Kind.IMPORT ? parentScope.getImportedSubscope() : parentScope, command).find(predicate);
      predicate.instanceProvider.reverseFrom(size);
    }
    processSubgroups(parentScope, predicate, subgroups);
  }

  private void processSubgroups(Scope parentScope, MyPredicate predicate, Collection<? extends Group> subgroups) {
    int size = predicate.instanceProvider.getInstances().size();
    for (Group subgroup : subgroups) {
      LocatedReferable groupRef = subgroup.getReferable();
      if (groupRef.getKind() == GlobalReferable.Kind.COCLAUSE_FUNCTION) continue;
      predicate.used = true;
      SimpleInstanceProvider instanceProvider = predicate.instanceProvider;
      predicate.test(size, groupRef);
      processGroup(subgroup, parentScope, predicate);

      if (!predicate.instanceProvider.isEmpty()) {
        for (Group subSubgroup : subgroup.getSubgroups()) {
          LocatedReferable subRef = subSubgroup.getReferable();
          if (subRef.getKind() == GlobalReferable.Kind.COCLAUSE_FUNCTION) {
            subRef = predicate.referableConverter.toDataLocatedReferable(subRef);
            if (subRef instanceof TCDefReferable) {
              myProviders.put((TCDefReferable) subRef, predicate.instanceProvider);
            }
          }
        }
      }

      LocatedReferable ref = predicate.recordInstances(groupRef);
      predicate.used = true;
      predicate.instanceProvider = instanceProvider;
      predicate.test(size, ref);

      processSubgroups(parentScope, predicate, subgroup.getDynamicSubgroups());
    }
  }
}
