package com.jetbrains.jetpad.vclang.editor;

import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.definition.Definition;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.visitor.CheckTypeVisitor;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.ModifierKey;
import jetbrains.jetpad.mapper.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

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
              CheckTypeVisitor.Result typeResult = funDef.getResultType().accept(new CheckTypeVisitor(new HashMap<String, com.jetbrains.jetpad.vclang.term.definition.Definition>(), new ArrayList<com.jetbrains.jetpad.vclang.term.definition.Definition>()), new UniverseExpression());
              funDef.getResultType().wellTypedExpr().set(typeResult.expression);
              CheckTypeVisitor.Result exprResult = funDef.getTerm().accept(new CheckTypeVisitor(new HashMap<String, com.jetbrains.jetpad.vclang.term.definition.Definition>(), new ArrayList<com.jetbrains.jetpad.vclang.term.definition.Definition>()), typeResult.expression);
              funDef.getTerm().wellTypedExpr().set(exprResult.expression);
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
