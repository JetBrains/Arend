package com.jetbrains.jetpad.vclang.editor.definition;

import com.jetbrains.jetpad.vclang.editor.expr.ExpressionCompletion;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class FunctionDefinitionMapper extends Mapper<FunctionDefinition, FunctionDefinitionMapper.Cell> {
  public FunctionDefinitionMapper(FunctionDefinition source) {
    super(source, new FunctionDefinitionMapper.Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsTwoWay(getSource().name, getTarget().name.text()));
    conf.add(forExpression(this, getSource().resultType, getTarget().type, "<type>", ExpressionCompletion.getGlobalInstance()));
    conf.add(forExpression(this, getSource().term, getTarget().term, "<term>", ExpressionCompletion.getGlobalInstance()));
  }

  public static class Cell extends IndentCell {
    final TextCell name = noDelete(new TextCell());
    final jetbrains.jetpad.cell.Cell type = noDelete(indent());
    final jetbrains.jetpad.cell.Cell term = noDelete(indent());

    Cell() {
      to(this,
          noDelete(keyword("function")),
          newLine(),
          name,
          placeHolder(name, "<no name>"),
          space(),
          text(":"),
          space(),
          type,
          space(),
          text("=>"),
          space(),
          term);

      focusable().set(true);
      name.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(name));
    }
  }
}
