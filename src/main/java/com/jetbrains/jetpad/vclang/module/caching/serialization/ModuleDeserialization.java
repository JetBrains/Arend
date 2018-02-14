package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ModuleDeserialization {
  private final TypecheckerState myState;
  private final CallTargetProvider myCallTargetProvider = new SimpleCallTargetProvider();

  public ModuleDeserialization(TypecheckerState state) {
    myState = state;
  }

  public Group readModule(ModuleProtos.Module moduleProto) throws DeserializationError {
    for (Map.Entry<String, DefinitionProtos.Definition> entry : moduleProto.getDefinitionMap().entrySet()) {
      DefinitionProtos.Definition defProto = entry.getValue();
      Pair<Precedence, List<String>> pair = fullNameFromNameId(entry.getKey()); // TODO[library]: add this pair to the group.

      final GlobalReferable abstractDef = getAbstract(id);
      final Definition def;
      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = new ClassDefinition(abstractDef);
          for (DefinitionProtos.Definition.ClassData.Field fieldProto : defProto.getClass_().getPersonalFieldList()) {
            myPersistenceProvider.registerCachedDefinition(mySourceId, fieldProto.getName(), abstractDef);
            GlobalReferable absField = getAbstract(fieldProto.getName());
            ClassField res = new ClassField(absField, classDef);
            res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            state.record(absField, res);
          }
          def = classDef;
          break;
        case DATA:
          DataDefinition dataDef = new DataDefinition(abstractDef);
          for (String constructorId : defProto.getData().getConstructorsMap().keySet()) {
            myPersistenceProvider.registerCachedDefinition(mySourceId, constructorId, abstractDef);
            GlobalReferable absConstructor = getAbstract(constructorId);
            Constructor res = new Constructor(absConstructor, dataDef);
            res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            state.record(absConstructor, res);
          }
          def = dataDef;
          break;
        case FUNCTION:
          def = new FunctionDefinition(getAbstract(id));
          break;
        default:
          throw new DeserializationError("Unknown Definition kind: " + defProto.getDefinitionDataCase());
      }

      def.setStatus(readTcStatus(defProto));
      state.record(abstractDef, def);
    }
  }

  private static Pair<Precedence, List<String>> fullNameFromNameId(String s) {
    boolean isInfix = s.charAt(0) == 'i';
    final Precedence.Associativity assoc;
    switch (s.charAt(1)) {
      case 'l':
        assoc = Precedence.Associativity.LEFT_ASSOC;
        break;
      case 'r':
        assoc = Precedence.Associativity.RIGHT_ASSOC;
        break;
      default:
        assoc = Precedence.Associativity.NON_ASSOC;
    }

    int sepIndex = s.indexOf(';');
    final byte priority = Byte.parseByte(s.substring(2, sepIndex));
    return new Pair<>(new Precedence(assoc, priority, isInfix), Arrays.asList(s.substring(sepIndex + 1).split("\\.")));
  }
}
