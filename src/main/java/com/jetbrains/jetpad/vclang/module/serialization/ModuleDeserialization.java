package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.order.dependency.DependencyListener;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;

public class ModuleDeserialization {
  private final ModuleProtos.Module myModuleProto;
  private final SimpleCallTargetProvider myCallTargetProvider = new SimpleCallTargetProvider();
  private final TypecheckerState myState;
  private final ReferableConverter myReferableConverter;
  private final List<Pair<DefinitionProtos.Definition, Definition>> myDefinitions = new ArrayList<>();

  public ModuleDeserialization(ModuleProtos.Module moduleProto, TypecheckerState state, ReferableConverter referableConverter) {
    myModuleProto = moduleProto;
    myState = state;
    myReferableConverter = referableConverter;
  }

  public ModuleProtos.Module getModuleProto() {
    return myModuleProto;
  }

  public void readModule(ModuleScopeProvider moduleScopeProvider, DependencyListener dependencyListener, boolean typecheckDefinitionsWithErrors) throws DeserializationException {
    for (ModuleProtos.ModuleCallTargets moduleCallTargets : myModuleProto.getModuleCallTargetsList()) {
      ModulePath module = new ModulePath(moduleCallTargets.getNameList());
      Scope scope = moduleScopeProvider.forModule(module);
      if (scope == null) {
        throw new DeserializationException("Cannot find module: " + module);
      }

      for (ModuleProtos.CallTargetTree callTargetTree : moduleCallTargets.getCallTargetTreeList()) {
        fillInCallTargetTree(callTargetTree, scope, module);
      }
    }

    DefinitionDeserialization defDeserialization = new DefinitionDeserialization(myCallTargetProvider, dependencyListener);
    for (Pair<DefinitionProtos.Definition, Definition> pair : myDefinitions) {
      defDeserialization.fillInDefinition(pair.proj1, pair.proj2, typecheckDefinitionsWithErrors);
    }
    myDefinitions.clear();
  }

  private void fillInCallTargetTree(ModuleProtos.CallTargetTree callTargetTree, Scope scope, ModulePath module) throws DeserializationException {
    if (callTargetTree.getIndex() > 0) {
      Referable referable1 = scope.resolveName(callTargetTree.getName());
      TCReferable referable = myReferableConverter == null
        ? (referable1 instanceof TCReferable ? (TCReferable) referable1 : null)
        : (referable1 instanceof LocatedReferable ? myReferableConverter.toDataLocatedReferable((LocatedReferable) referable1) : null);
      if (referable == null) {
        throw new DeserializationException("Cannot resolve reference '" + callTargetTree.getName() + "' in " + module);
      }
      Definition callTarget = myState.getTypechecked(referable);
      if (callTarget == null) {
        throw new DeserializationException("Definition '" + callTargetTree.getName() + "' was not typechecked");
      }
      myCallTargetProvider.putCallTarget(callTargetTree.getIndex(), callTarget);
    }

    List<ModuleProtos.CallTargetTree> subtreeList = callTargetTree.getSubtreeList();
    if (!subtreeList.isEmpty()) {
      Scope subscope = scope.resolveNamespace(callTargetTree.getName());
      if (subscope == null) {
        throw new DeserializationException("Cannot resolve reference '" + callTargetTree.getName() + "' in " + module);
      }

      for (ModuleProtos.CallTargetTree tree : subtreeList) {
        fillInCallTargetTree(tree, subscope, module);
      }
    }
  }

  public void readDefinitions(Group group) throws DeserializationException {
    readDefinitions(myModuleProto.getGroup(), group);
  }

  public void readDefinitions(ModuleProtos.Group groupProto, Group group) throws DeserializationException {
    if (groupProto.hasDefinition()) {
      LocatedReferable referable = group.getReferable();
      TCReferable tcReferable = myReferableConverter.toDataLocatedReferable(referable);
      if (tcReferable == null) {
        throw new DeserializationException("Cannot locate '" + referable + "'");
      }

      Definition def = readDefinition(groupProto.getDefinition(), tcReferable, false);
      myState.record(tcReferable, def);
      myCallTargetProvider.putCallTarget(groupProto.getReferable().getIndex(), def);
      myDefinitions.add(new Pair<>(groupProto.getDefinition(), def));

      Collection<? extends Group.InternalReferable> fields = group.getFields();
      if (!fields.isEmpty()) {
        Map<String, DefinitionProtos.Definition.ClassData.Field> fieldMap = new HashMap<>();
        for (DefinitionProtos.Definition.ClassData.Field field : groupProto.getDefinition().getClass_().getPersonalFieldList()) {
          fieldMap.put(field.getReferable().getName(), field);
        }

        for (Group.InternalReferable field : fields) {
          LocatedReferable fieldRef = field.getReferable();
          TCReferable absField = myReferableConverter.toDataLocatedReferable(fieldRef);
          DefinitionProtos.Definition.ClassData.Field fieldProto = fieldMap.get(fieldRef.textRepresentation());
          if (fieldProto == null || absField == null) {
            throw new DeserializationException("Cannot locate '" + fieldRef + "'");
          }

          assert def instanceof ClassDefinition;
          ClassField res = new ClassField(absField, (ClassDefinition) def);
          res.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
          myState.record(absField, res);
          myCallTargetProvider.putCallTarget(fieldProto.getReferable().getIndex(), res);
        }
      }

      Collection<? extends Group.InternalReferable> constructors = group.getConstructors();
      if (!constructors.isEmpty()) {
        Map<String, DefinitionProtos.Definition.DataData.Constructor> constructorMap = new HashMap<>();
        for (DefinitionProtos.Definition.DataData.Constructor constructor : groupProto.getDefinition().getData().getConstructorList()) {
          constructorMap.put(constructor.getReferable().getName(), constructor);
        }

        for (Group.InternalReferable constructor : constructors) {
          LocatedReferable constructorRef = constructor.getReferable();
          TCReferable absConstructor = myReferableConverter.toDataLocatedReferable(constructorRef);
          DefinitionProtos.Definition.DataData.Constructor constructorProto = constructorMap.get(constructorRef.textRepresentation());
          if (constructorProto == null || absConstructor == null) {
            throw new DeserializationException("Cannot locate '" + constructorRef + "'");
          }

          assert def instanceof DataDefinition;
          Constructor res = new Constructor(absConstructor, (DataDefinition) def);
          res.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
          myState.record(absConstructor, res);
          myCallTargetProvider.putCallTarget(constructorProto.getReferable().getIndex(), res);
        }
      }
    }

    Collection<? extends Group> subgroups = group.getSubgroups();
    if (!groupProto.getSubgroupList().isEmpty() && !subgroups.isEmpty()) {
      Map<String, ModuleProtos.Group> subgroupMap = new HashMap<>();
      for (ModuleProtos.Group subgroup : groupProto.getSubgroupList()) {
        subgroupMap.put(subgroup.getReferable().getName(), subgroup);
      }
      for (Group subgroup : subgroups) {
        ModuleProtos.Group subgroupProto = subgroupMap.get(subgroup.getReferable().textRepresentation());
        if (subgroupProto != null) {
          readDefinitions(subgroupProto, subgroup);
        }
      }
    }

    Collection<? extends Group> dynSubgroups = group.getSubgroups();
    if (!groupProto.getDynamicSubgroupList().isEmpty() && !dynSubgroups.isEmpty()) {
      Map<String, ModuleProtos.Group> subgroupMap = new HashMap<>();
      for (ModuleProtos.Group subgroup : groupProto.getDynamicSubgroupList()) {
        subgroupMap.put(subgroup.getReferable().getName(), subgroup);
      }
      for (Group subgroup : dynSubgroups) {
        ModuleProtos.Group subgroupProto = subgroupMap.get(subgroup.getReferable().textRepresentation());
        if (subgroupProto != null) {
          readDefinitions(subgroupProto, subgroup);
        }
      }
    }
  }

  @Nonnull
  public ChildGroup readGroup(ModulePath modulePath) throws DeserializationException {
    return readGroup(myModuleProto.getGroup(), null, modulePath);
  }

  @Nonnull
  private ChildGroup readGroup(ModuleProtos.Group groupProto, ChildGroup parent, ModulePath modulePath) throws DeserializationException {
    DefinitionProtos.Referable referableProto = groupProto.getReferable();
    List<TCReferable> fieldReferables;
    LocatedReferable referable;
    if (groupProto.hasDefinition() && groupProto.getDefinition().getDefinitionDataCase() == DefinitionProtos.Definition.DefinitionDataCase.CLASS) {
      fieldReferables = new ArrayList<>();
      referable = new ClassReferableImpl(readPrecedence(referableProto.getPrecedence()), referableProto.getName(), new ArrayList<>(), fieldReferables, modulePath);
    } else {
      fieldReferables = new ArrayList<>(0);
      if (parent == null) {
        referable = new ModuleReferable(modulePath);
      } else {
        referable = new DataLocatedReferableImpl(readPrecedence(referableProto.getPrecedence()), referableProto.getName(), parent.getReferable(), null, true);
      }
    }

    Definition def;
    if (groupProto.hasDefinition()) {
      def = readDefinition(groupProto.getDefinition(), (TCReferable) referable, true);
      myState.record((TCReferable) referable, def);
      myCallTargetProvider.putCallTarget(referableProto.getIndex(), def);
      myDefinitions.add(new Pair<>(groupProto.getDefinition(), def));
    } else {
      def = null;
    }

    List<Group> subgroups = new ArrayList<>(groupProto.getSubgroupCount());

    ChildGroup group;
    if (def == null || def instanceof FunctionDefinition) {
      group = new StaticGroup(referable, subgroups, Collections.emptyList(), parent);
    } else if (def instanceof DataDefinition) {
      Set<Definition> invisibleRefs = new HashSet<>();
      for (Integer index : groupProto.getInvisibleInternalReferableList()) {
        invisibleRefs.add(myCallTargetProvider.getCallTarget(index));
      }
      List<Group.InternalReferable> internalReferables = new ArrayList<>();
      for (DefinitionProtos.Definition.DataData.Constructor constructor : groupProto.getDefinition().getData().getConstructorList()) {
        Definition conDef = myCallTargetProvider.getCallTarget(constructor.getReferable().getIndex());
        internalReferables.add(new SimpleInternalReferable(conDef.getReferable(), !invisibleRefs.contains(conDef)));
      }

      group = new DataGroup(referable, internalReferables, subgroups, Collections.emptyList(), parent);
    } else if (def instanceof ClassDefinition) {
      Set<Definition> invisibleRefs = new HashSet<>();
      for (Integer index : groupProto.getInvisibleInternalReferableList()) {
        invisibleRefs.add(myCallTargetProvider.getCallTarget(index));
      }
      List<Group.InternalReferable> internalReferables = new ArrayList<>();
      for (DefinitionProtos.Definition.ClassData.Field field : groupProto.getDefinition().getClass_().getPersonalFieldList()) {
        Definition fieldDef = myCallTargetProvider.getCallTarget(field.getReferable().getIndex());
        TCReferable fieldReferable = fieldDef.getReferable();
        internalReferables.add(new SimpleInternalReferable(fieldReferable, !invisibleRefs.contains(fieldDef)));
        fieldReferables.add(fieldReferable);
      }

      List<Group> dynamicGroups = new ArrayList<>(groupProto.getDynamicSubgroupCount());
      group = new ClassGroup((ClassReferable) referable, internalReferables, dynamicGroups, subgroups, Collections.emptyList(), parent);
      for (ModuleProtos.Group subgroup : groupProto.getDynamicSubgroupList()) {
        dynamicGroups.add(readGroup(subgroup, group, modulePath));
      }
    } else {
      throw new IllegalStateException();
    }

    for (ModuleProtos.Group subgroup : groupProto.getSubgroupList()) {
      subgroups.add(readGroup(subgroup, group, modulePath));
    }

    return group;
  }

  private Definition readDefinition(DefinitionProtos.Definition defProto, TCReferable referable, boolean fillInternalDefinitions) throws DeserializationException {
    final Definition def;
    switch (defProto.getDefinitionDataCase()) {
      case CLASS:
        ClassDefinition classDef = new ClassDefinition((TCClassReferable) referable);
        if (fillInternalDefinitions) {
          for (DefinitionProtos.Definition.ClassData.Field fieldProto : defProto.getClass_().getPersonalFieldList()) {
            DefinitionProtos.Referable fieldReferable = fieldProto.getReferable();
            TCReferable absField = new DataLocatedReferableImpl(readPrecedence(fieldReferable.getPrecedence()), fieldReferable.getName(), referable, null, false);
            ClassField res = new ClassField(absField, classDef);
            res.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
            myState.record(absField, res);
            myCallTargetProvider.putCallTarget(fieldReferable.getIndex(), res);
          }
        }
        def = classDef;
        break;
      case DATA:
        DataDefinition dataDef = new DataDefinition(referable);
        if (fillInternalDefinitions) {
          for (DefinitionProtos.Definition.DataData.Constructor constructor : defProto.getData().getConstructorList()) {
            DefinitionProtos.Referable conReferable = constructor.getReferable();
            TCReferable absConstructor = new LocatedReferableImpl(readPrecedence(conReferable.getPrecedence()), conReferable.getName(), referable, false);
            Constructor res = new Constructor(absConstructor, dataDef);
            res.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
            myState.record(absConstructor, res);
            myCallTargetProvider.putCallTarget(conReferable.getIndex(), res);
          }
        }
        def = dataDef;
        break;
      case FUNCTION:
        def = new FunctionDefinition(referable);
        break;
      default:
        throw new DeserializationException("Unknown Definition kind: " + defProto.getDefinitionDataCase());
    }
    def.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
    return def;
  }

  private static Precedence readPrecedence(DefinitionProtos.Precedence precedenceProto) throws DeserializationException {
    Precedence.Associativity assoc;
    switch (precedenceProto.getAssoc()) {
      case LEFT:
        assoc = Precedence.Associativity.LEFT_ASSOC;
        break;
      case RIGHT:
        assoc = Precedence.Associativity.RIGHT_ASSOC;
        break;
      case NON_ASSOC:
        assoc = Precedence.Associativity.NON_ASSOC;
        break;
      default:
        throw new DeserializationException("Unknown associativity: " + precedenceProto.getAssoc());
    }
    return new Precedence(assoc, (byte) precedenceProto.getPriority(), precedenceProto.getInfix());
  }
}
