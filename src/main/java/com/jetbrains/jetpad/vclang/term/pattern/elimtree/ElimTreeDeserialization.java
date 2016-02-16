package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElimTreeDeserialization {
  private final ModuleDeserialization myModuleDeserialization;

  public ElimTreeDeserialization(ModuleDeserialization moduleDeserialization) {
    this.myModuleDeserialization = moduleDeserialization;
  }

  public ElimTreeNode readElimTree(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    switch (stream.readInt()) {
      case 0: {
        Binding binding = myModuleDeserialization.readBinding(stream, definitionMap);
        int contextTailSize = stream.readInt();
        List<Binding> contextTail = new ArrayList<>(contextTailSize);
        for (int i = 0; i < contextTailSize; i++) {
          contextTail.add(myModuleDeserialization.readBinding(stream, definitionMap));
        }
        BranchElimTreeNode elimTree = new BranchElimTreeNode(binding, contextTail);
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
          Constructor constructor = (Constructor) definitionMap.get(stream.readInt());
          DependentLink parameters = myModuleDeserialization.readParameters(stream, definitionMap);
          List<Binding> tailBindings = new ArrayList<>(contextTailSize);
          for (int j = 0; j < contextTailSize; j++) {
            tailBindings.add(myModuleDeserialization.readTypedBinding(stream, definitionMap));
          }
          elimTree.addClause(constructor, parameters, tailBindings, readElimTree(stream, definitionMap));
        }
        if (stream.readBoolean()) {
          elimTree.addOtherwiseClause(readElimTree(stream, definitionMap));
        }
        return elimTree;
      }
      case 1: {
        return new LeafElimTreeNode(stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT, myModuleDeserialization.readExpression(stream, definitionMap));
      }
      case 2: {
        return EmptyElimTreeNode.getInstance();
      }
      default:
        throw new IllegalStateException();
    }
  }
}
