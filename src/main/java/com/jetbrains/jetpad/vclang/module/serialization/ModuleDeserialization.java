package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;

public class ModuleDeserialization {
  private final SimpleCallTargetProvider myCallTargetProvider = new SimpleCallTargetProvider();
  private final TypecheckerState myState;
  private final List<Pair<DefinitionProtos.Definition, Definition>> myDefinitions = new ArrayList<>();

  public ModuleDeserialization(TypecheckerState state) {
    myState = state;
  }

  public boolean readModule(ModuleProtos.Module moduleProto, ModuleScopeProvider moduleScopeProvider, Function<ModulePath, Boolean> moduleLoader, ErrorReporter errorReporter) throws DeserializationException {
    for (ModuleProtos.ModuleCallTargets moduleCallTargets : moduleProto.getModuleCallTargetsList()) {
      ModulePath module = new ModulePath(moduleCallTargets.getNameList());
      if (!moduleLoader.apply(module)) {
        return false;
      }

      Scope scope = moduleScopeProvider.forModule(module);
      if (scope == null) {
        errorReporter.report(new ModuleNotFoundError(module));
        return false;
      }

      for (ModuleProtos.CallTargetTree callTargetTree : moduleCallTargets.getCallTargetTreeList()) {
        fillInCallTargetTree(callTargetTree, scope, module);
      }
    }

    DefinitionDeserialization defDeserialization = new DefinitionDeserialization(myCallTargetProvider);
    for (Pair<DefinitionProtos.Definition, Definition> pair : myDefinitions) {
      defDeserialization.fillInDefinition(pair.proj1, pair.proj2);
    }
    myDefinitions.clear();
    return true;
  }

  private void fillInCallTargetTree(ModuleProtos.CallTargetTree callTargetTree, Scope scope, ModulePath module) throws DeserializationException {
    if (callTargetTree.getIndex() > 0) {
      Referable referable = scope.resolveName(callTargetTree.getName());
      if (!(referable instanceof GlobalReferable)) {
        throw new DeserializationException("Cannot resolve reference '" + callTargetTree.getName() + "' in " + module);
      }
      myCallTargetProvider.putCallTarget(callTargetTree.getIndex(), myState.getTypechecked((GlobalReferable) referable));
    }

    Scope subscope = scope.resolveNamespace(callTargetTree.getName(), true);
    if (subscope == null) {
      throw new DeserializationException("Cannot resolve reference '" + callTargetTree.getName() + "' in " + module);
    }

    for (ModuleProtos.CallTargetTree tree : callTargetTree.getSubtreeList()) {
      fillInCallTargetTree(tree, subscope, module);
    }
  }

  @Nonnull
  public Group readGroup(ModuleProtos.Group groupProto) throws DeserializationException {
    return readGroup(groupProto, null);
  }

  @Nonnull
  private Group readGroup(ModuleProtos.Group groupProto, ChildGroup parent) throws DeserializationException {
    DefinitionProtos.Referable referableProto = groupProto.getReferable();
    List<GlobalReferable> fieldReferables;
    GlobalReferable referable;
    if (groupProto.hasDefinition() && groupProto.getDefinition().getDefinitionDataCase() == DefinitionProtos.Definition.DefinitionDataCase.CLASS) {
      fieldReferables = Collections.emptyList();
      referable = new SimpleGlobalReferable(readPrecedence(referableProto.getPrecedence()), referableProto.getName(), null);
    } else {
      fieldReferables = new ArrayList<>();
      referable = new SimpleClassReferable(readPrecedence(referableProto.getPrecedence()), referableProto.getName(), new ArrayList<>(), fieldReferables);
    }

    Definition def;
    if (groupProto.hasDefinition()) {
      def = readDefinition(groupProto.getDefinition(), referable);
      myState.record(referable, def);
      myCallTargetProvider.putCallTarget(referableProto.getIndex(), def);
      myDefinitions.add(new Pair<>(groupProto.getDefinition(), def));
    } else {
      def = null;
    }

    List<Group> subgroups = new ArrayList<>(groupProto.getSubgroupCount());

    ChildGroup group;
    if (def instanceof FunctionDefinition) {
      group = new StaticGroup(referable, subgroups, Collections.emptyList(), parent);
    } else if (def instanceof DataDefinition) {
      List<Constructor> constructors = ((DataDefinition) def).getConstructors();
      Set<Definition> invisibleRefs = new HashSet<>();
      for (Integer index : groupProto.getInvisibleInternalReferableList()) {
        invisibleRefs.add(myCallTargetProvider.getCallTarget(index));
      }
      List<Group.InternalReferable> internalReferables = new ArrayList<>(constructors.size());
      for (Constructor constructor : constructors) {
        internalReferables.add(new SimpleInternalReferable(constructor.getReferable(), invisibleRefs.contains(constructor)));
      }

      group = new DataGroup(referable, internalReferables, subgroups, Collections.emptyList(), parent);
    } else if (def instanceof ClassDefinition) {
      List<? extends ClassField> fields = ((ClassDefinition) def).getPersonalFields();
      Set<Definition> invisibleRefs = new HashSet<>();
      for (Integer index : groupProto.getInvisibleInternalReferableList()) {
        invisibleRefs.add(myCallTargetProvider.getCallTarget(index));
      }
      List<Group.InternalReferable> internalReferables = new ArrayList<>(fields.size());
      for (ClassField field : fields) {
        internalReferables.add(new SimpleInternalReferable(field.getReferable(), invisibleRefs.contains(field)));
        fieldReferables.add(field.getReferable());
      }

      List<Group> dynamicGroups = new ArrayList<>(groupProto.getDynamicSubgroupCount());
      group = new ClassGroup((ClassReferable) referable, internalReferables, dynamicGroups, subgroups, Collections.emptyList(), parent);
      for (ModuleProtos.Group subgroup : groupProto.getDynamicSubgroupList()) {
        dynamicGroups.add(readGroup(subgroup, group));
      }
    } else {
      throw new IllegalStateException();
    }

    for (ModuleProtos.Group subgroup : groupProto.getSubgroupList()) {
      subgroups.add(readGroup(subgroup, group));
    }

    return group;
  }

  private Definition readDefinition(DefinitionProtos.Definition defProto, GlobalReferable referable) throws DeserializationException {
    final Definition def;
    switch (defProto.getDefinitionDataCase()) {
      case CLASS:
        ClassDefinition classDef = new ClassDefinition(referable);
        for (DefinitionProtos.Definition.ClassData.Field fieldProto : defProto.getClass_().getPersonalFieldList()) {
          DefinitionProtos.Referable fieldReferable = fieldProto.getReferable();
          GlobalReferable absField = new SimpleGlobalReferable(readPrecedence(fieldReferable.getPrecedence()), fieldReferable.getName(), referable);
          ClassField res = new ClassField(absField, classDef);
          res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          myState.record(absField, res);
          myCallTargetProvider.putCallTarget(fieldReferable.getIndex(), res);
        }
        def = classDef;
        break;
      case DATA:
        DataDefinition dataDef = new DataDefinition(referable);
        for (DefinitionProtos.Definition.DataData.Constructor constructor : defProto.getData().getConstructorList()) {
          DefinitionProtos.Referable conReferable = constructor.getReferable();
          GlobalReferable absConstructor = new SimpleGlobalReferable(readPrecedence(conReferable.getPrecedence()), conReferable.getName(), referable);
          Constructor res = new Constructor(absConstructor, dataDef);
          res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          myState.record(absConstructor, res);
          myCallTargetProvider.putCallTarget(conReferable.getIndex(), res);
        }
        def = dataDef;
        break;
      case FUNCTION:
        def = new FunctionDefinition(referable);
        break;
      default:
        throw new DeserializationException("Unknown Definition kind: " + defProto.getDefinitionDataCase());
    }
    def.setStatus(readTcStatus(defProto));
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

  private @Nonnull Definition.TypeCheckingStatus readTcStatus(DefinitionProtos.Definition defProto) {
    switch (defProto.getStatus()) {
      case HEADER_HAS_ERRORS:
        return Definition.TypeCheckingStatus.HEADER_HAS_ERRORS;
      case BODY_HAS_ERRORS:
        return Definition.TypeCheckingStatus.BODY_HAS_ERRORS;
      case HEADER_NEEDS_TYPE_CHECKING:
        return Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING;
      case BODY_NEEDS_TYPE_CHECKING:
        return Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING;
      case HAS_ERRORS:
        return Definition.TypeCheckingStatus.HAS_ERRORS;
      case NO_ERRORS:
        return Definition.TypeCheckingStatus.NO_ERRORS;
      default:
        throw new IllegalStateException("Unknown typechecking state");
    }
  }
}
