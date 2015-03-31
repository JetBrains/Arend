package com.jetbrains.jetpad.vclang.editor.expr.arg;

import com.jetbrains.jetpad.vclang.editor.util.Validators;
import com.jetbrains.jetpad.vclang.model.expr.Model;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;

public class TelescopeArgumentMapper extends Mapper<Model.TelescopeArgument, TelescopeArgumentMapper.Cell> {
  public TelescopeArgumentMapper(Model.TelescopeArgument source) {
    super(source, new Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    // TODO: Fix this.
    // conf.add(forPropsTwoWay(getSource().name(), getTarget().variable.text()));
    conf.add(forExpression(this, getSource().type(), getTarget().type, "<type>"));
  }

  public static class Cell extends IndentCell {
    public final TextCell variable = noDelete(new TextCell());
    public final jetbrains.jetpad.cell.Cell type = noDelete(indent());

    public Cell() {
      CellFactory.to(this,
          label("("),
          variable,
          placeHolder(variable, "<no name>"),
          space(),
          label(":"),
          space(),
          type,
          label(")"));

      focusable().set(true);
      variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
    }
  }
}
