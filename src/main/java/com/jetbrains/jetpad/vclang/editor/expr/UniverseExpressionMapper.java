package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.UniverseExpression;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.text;

public class UniverseExpressionMapper extends Mapper<UniverseExpression, UniverseExpressionMapper.Cell> {
  public UniverseExpressionMapper(UniverseExpression source) {
    super(source, new Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    // TODO: Add synchronization for level
  }

  public static class Cell extends IndentCell {
    public final TextCell level = noDelete(new TextCell());

    public Cell() {
      CellFactory.to(this,
          text("Type"),
          level);

      focusable().set(true);
      level.addTrait(TextEditing.validTextEditing(Validators.unsignedInteger()));
    }
  }
}
