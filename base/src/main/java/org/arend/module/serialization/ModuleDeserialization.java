package org.arend.module.serialization;

import org.arend.core.definition.*;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.ext.serialization.DeserializationException;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.extImpl.SerializableKeyRegistryImpl;
import org.arend.module.ModuleLocation;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.term.group.*;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModuleDeserialization {
  private final ModuleProtos.Module myModuleProto;
  private final SimpleCallTargetProvider myCallTargetProvider = new SimpleCallTargetProvider();
  private final ReferableConverter myReferableConverter;
  private final List<Pair<DefinitionProtos.Definition, Definition>> myDefinitions = new ArrayList<>();
  private final SerializableKeyRegistryImpl myKeyRegistry;
  private final DefinitionListener myDefinitionListener;

  public ModuleDeserialization(ModuleProtos.Module moduleProto, ReferableConverter referableConverter, SerializableKeyRegistryImpl keyRegistry, DefinitionListener definitionListener) {
    myModuleProto = moduleProto;
    myReferableConverter = referableConverter;
    myKeyRegistry = keyRegistry;
    myDefinitionListener = definitionListener;
  }

  public ModuleProtos.Module getModuleProto() {
    return myModuleProto;
  }

  public void readModule(ModuleScopeProvider moduleScopeProvider, DependencyListener dependencyListener) throws DeserializationException {
    if (myModuleProto.getVersion() != ModuleSerialization.VERSION) {
      throw new DeserializationException("Version mismatch");
    }

    for (ModuleProtos.ModuleCallTargets moduleCallTargets : myModuleProto.getModuleCallTargetsList()) {
      ModulePath module = new ModulePath(moduleCallTargets.getNameList());
      Scope scope = moduleScopeProvider.forModule(module);
      if (scope == null) {
        throw new DeserializationException("Cannot find module: " + module);
      }

      for (ModuleProtos.CallTargetTree callTargetTree : moduleCallTargets.getCallTargetTreeList()) {
        fillInCallTargetTree(null, callTargetTree, scope, module);
      }
    }

    DefinitionDeserialization defDeserialization = new DefinitionDeserialization(myCallTargetProvider, dependencyListener, myKeyRegistry, myDefinitionListener);
    for (Pair<DefinitionProtos.Definition, Definition> pair : myDefinitions) {
      defDeserialization.fillInDefinition(pair.proj1, pair.proj2);
    }
    myDefinitions.clear();
  }

  private void fillInCallTargetTree(String parentName, ModuleProtos.CallTargetTree callTargetTree, Scope scope, ModulePath module) throws DeserializationException {
    if (callTargetTree.getIndex() > 0) {
      Referable referable1 = scope.resolveName(callTargetTree.getName());
      TCReferable referable = myReferableConverter == null
        ? (referable1 instanceof TCReferable ? (TCReferable) referable1 : null)
        : (referable1 instanceof LocatedReferable ? myReferableConverter.toDataLocatedReferable((LocatedReferable) referable1) : null);
      if (referable == null && module.equals(Prelude.MODULE_PATH) && "Fin".equals(parentName)) {
        if (callTargetTree.getName().equals("zero")) {
          referable = Prelude.FIN_ZERO.getReferable();
        } else if (callTargetTree.getName().equals("suc")) {
          referable = Prelude.FIN_SUC.getReferable();
        }
      }
      if (referable == null) {
        throw new DeserializationException("Cannot resolve reference '" + callTargetTree.getName() + "' in " + module);
      }
      myCallTargetProvider.putCallTarget(callTargetTree.getIndex(), referable);
    }

    List<ModuleProtos.CallTargetTree> subtreeList = callTargetTree.getSubtreeList();
    if (!subtreeList.isEmpty()) {
      Scope subscope = scope.resolveNamespace(callTargetTree.getName(), true);
      if (subscope == null) {
        throw new DeserializationException("Cannot resolve reference '" + callTargetTree.getName() + "' in " + module);
      }

      for (ModuleProtos.CallTargetTree tree : subtreeList) {
        fillInCallTargetTree(callTargetTree.getName(), tree, subscope, module);
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
      if (!(tcReferable instanceof TCDefReferable)) {
        throw new DeserializationException("'" + referable + "' is not a definition");
      }

      TCDefReferable tcDefReferable = (TCDefReferable) tcReferable;
      Definition def = readDefinition(groupProto.getDefinition(), tcDefReferable, false);
      tcDefReferable.setTypechecked(def);
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
          if (!(absField instanceof TCFieldReferable)) {
            throw new DeserializationException("Incorrect field '" + absField.textRepresentation() + "'");
          }

          assert def instanceof ClassDefinition;
          ClassField res = new ClassField((TCFieldReferable) absField, (ClassDefinition) def);
          ((ClassDefinition) def).addPersonalField(res);
          ((TCFieldReferable) absField).setTypechecked(res);
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
          if (!(absConstructor instanceof TCDefReferable)) {
            throw new DeserializationException("'" + constructorRef + "' is not a definition");
          }

          assert def instanceof DataDefinition;
          Constructor res = new Constructor((TCDefReferable) absConstructor, (DataDefinition) def);
          ((DataDefinition) def).addConstructor(res);
          ((TCDefReferable) absConstructor).setTypechecked(res);
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

    Collection<? extends Group> dynSubgroups = group.getDynamicSubgroups();
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

  @NotNull
  public ChildGroup readGroup(ModuleLocation modulePath) throws DeserializationException {
    return readGroup(myModuleProto.getGroup(), null, modulePath);
  }

  private static GlobalReferable.Kind getDefinitionKind(DefinitionProtos.Definition defProto) {
    DefinitionProtos.Definition.DefinitionDataCase kind = defProto.getDefinitionDataCase();
    switch (kind) {
      case CLASS: return GlobalReferable.Kind.CLASS;
      case DATA: return GlobalReferable.Kind.DATA;
      case FUNCTION: return defProto.getFunction().getIsInstance() ? GlobalReferable.Kind.INSTANCE : GlobalReferable.Kind.FUNCTION;
      case CONSTRUCTOR: return GlobalReferable.Kind.DEFINED_CONSTRUCTOR;
      default: return GlobalReferable.Kind.OTHER;
    }
  }

  @NotNull
  private ChildGroup readGroup(ModuleProtos.Group groupProto, ChildGroup parent, ModuleLocation modulePath) throws DeserializationException {
    DefinitionProtos.Referable referableProto = groupProto.getReferable();
    List<TCFieldReferable> fieldReferables;
    LocatedReferable referable;
    GlobalReferable.Kind kind = getDefinitionKind(groupProto.getDefinition());
    List<GlobalReferable> dynamicReferables;
    if (groupProto.hasDefinition() && kind == GlobalReferable.Kind.CLASS) {
      dynamicReferables = new ArrayList<>();
      fieldReferables = new ArrayList<>();
      referable = new ClassReferableImpl(readPrecedence(referableProto.getPrecedence()), referableProto.getName(), groupProto.getDefinition().getClass_().getIsRecord(), new ArrayList<>(), fieldReferables, dynamicReferables, modulePath);
    } else {
      dynamicReferables = null;
      fieldReferables = new ArrayList<>(0);
      if (parent == null) {
        referable = new FullModuleReferable(modulePath);
      } else {
        referable = new LocatedReferableImpl(readPrecedence(referableProto.getPrecedence()), referableProto.getName(), parent.getReferable(), kind);
      }
    }

    Definition def;
    if (referable instanceof TCDefReferable && groupProto.hasDefinition()) {
      def = readDefinition(groupProto.getDefinition(), (TCDefReferable) referable, true);
      ((TCDefReferable) referable).setTypechecked(def);
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
    } else if (referable instanceof ClassReferable && def instanceof ClassDefinition) {
      Set<Definition> invisibleRefs = new HashSet<>();
      for (Integer index : groupProto.getInvisibleInternalReferableList()) {
        invisibleRefs.add(myCallTargetProvider.getCallTarget(index));
      }
      List<Group.InternalReferable> internalReferables = new ArrayList<>();
      for (DefinitionProtos.Definition.ClassData.Field field : groupProto.getDefinition().getClass_().getPersonalFieldList()) {
        ClassField fieldDef = myCallTargetProvider.getCallTarget(field.getReferable().getIndex(), ClassField.class);
        TCFieldReferable fieldReferable = fieldDef.getReferable();
        internalReferables.add(new SimpleInternalReferable(fieldReferable, !invisibleRefs.contains(fieldDef)));
        fieldReferables.add(fieldReferable);
      }

      List<Group> dynamicGroups = new ArrayList<>(groupProto.getDynamicSubgroupCount());
      group = new ClassGroup((ClassReferable) referable, internalReferables, dynamicGroups, subgroups, Collections.emptyList(), parent);
      for (ModuleProtos.Group subgroupProto : groupProto.getDynamicSubgroupList()) {
        Group subgroup = readGroup(subgroupProto, group, modulePath);
        dynamicGroups.add(subgroup);
        dynamicReferables.add(subgroup.getReferable());
      }
    } else {
      throw new IllegalStateException();
    }

    for (ModuleProtos.Group subgroup : groupProto.getSubgroupList()) {
      subgroups.add(readGroup(subgroup, group, modulePath));
    }

    return group;
  }

  private Definition readDefinition(DefinitionProtos.Definition defProto, TCDefReferable referable, boolean fillInternalDefinitions) throws DeserializationException {
    final Definition def;
    switch (defProto.getDefinitionDataCase()) {
      case CLASS:
        ClassDefinition classDef = new ClassDefinition(referable);
        if (fillInternalDefinitions) {
          for (DefinitionProtos.Definition.ClassData.Field fieldProto : defProto.getClass_().getPersonalFieldList()) {
            DefinitionProtos.Referable fieldReferable = fieldProto.getReferable();
            TCFieldReferable absField = new FieldReferableImpl(readPrecedence(fieldReferable.getPrecedence()), fieldReferable.getName(), fieldProto.getIsExplicit(), fieldProto.getIsParameter(), referable);
            ClassField res = new ClassField(absField, classDef);
            classDef.addPersonalField(res);
            absField.setTypechecked(res);
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
            TCDefReferable absConstructor = new LocatedReferableImpl(readPrecedence(conReferable.getPrecedence()), conReferable.getName(), referable, LocatedReferableImpl.Kind.CONSTRUCTOR);
            Constructor res = new Constructor(absConstructor, dataDef);
            dataDef.addConstructor(res);
            absConstructor.setTypechecked(res);
            myCallTargetProvider.putCallTarget(conReferable.getIndex(), res);
          }
        }
        def = dataDef;
        break;
      case FUNCTION:
        def = new FunctionDefinition(referable);
        break;
      case CONSTRUCTOR:
        def = new DConstructor(referable);
        break;
      default:
        throw new DeserializationException("Unknown Definition kind: " + defProto.getDefinitionDataCase());
    }
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
