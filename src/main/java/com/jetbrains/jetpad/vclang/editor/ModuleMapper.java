package com.jetbrains.jetpad.vclang.editor;

import com.google.common.base.Supplier;
import com.jetbrains.jetpad.vclang.editor.definition.DefinitionMapperFactory;
import com.jetbrains.jetpad.vclang.model.definition.EmptyDefinition;
import jetbrains.jetpad.cell.util.CellLists;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;
import jetbrains.jetpad.projectional.generic.Role;
import jetbrains.jetpad.projectional.generic.RoleCompletion;
import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.definition.Definition;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;

import java.util.ArrayList;
import java.util.List;

public class ModuleMapper extends Mapper<Module, ModuleCell> {
  public ModuleMapper(Module source) {
    super(source, new ModuleCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    ProjectionalRoleSynchronizer<Module, Definition> synchronizer = ProjectionalSynchronizers.forRole(this, getSource().definitions, getTarget().definitions, CellLists.newLineSeparated(getTarget().definitions.children()), new DefinitionMapperFactory());
    synchronizer.setCompletion(new RoleCompletion<Node, Definition>() {
      @Override
      public List<CompletionItem> createRoleCompletion(CompletionParameters ctx, Mapper<?, ?> mapper, Node contextNode, final Role<Definition> target) {
        List<CompletionItem> result = new ArrayList<>();
        result.add(new SimpleCompletionItem("function") {
          @Override
          public Runnable complete(String text) {
            return target.set(new FunctionDefinition());
          }
        });
        result.add(new SimpleCompletionItem("fun ") {
          @Override
          public Runnable complete(String text) {
            return target.set(new FunctionDefinition());
          }
        });
        return result;
      }
    });
    synchronizer.setItemFactory(new Supplier<Definition>() {
      @Override
      public Definition get() {
        return new EmptyDefinition();
      }
    });
    conf.add(synchronizer);
  }
}
