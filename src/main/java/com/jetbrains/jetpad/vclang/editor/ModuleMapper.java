package com.jetbrains.jetpad.vclang.editor;

import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.definition.Definition;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.VarExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.ModifierKey;
import jetbrains.jetpad.mapper.Mapper;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forDefinitions;

public class ModuleMapper extends Mapper<Module, ModuleCell> {
  public ModuleMapper(Module source) {
    super(source, new ModuleCell());

    getTarget().addTrait(new CellTrait() {
      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        if (event.is(Key.E, ModifierKey.CONTROL) || event.is(Key.E, ModifierKey.META)) {
          for (Definition def : getSource().definitions) {
            if (def instanceof FunctionDefinition) {
              // TODO: Run type checker
              FunctionDefinition funDef = (FunctionDefinition) def;
              funDef.getTerm().wellTypedExpr().set(new VarExpression("x"));
            }
          }
          event.consume();
        }
        super.onKeyPressed(cell, event);
      }
    });
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forDefinitions(this, getSource().definitions, getTarget().definitions));
  }
}
