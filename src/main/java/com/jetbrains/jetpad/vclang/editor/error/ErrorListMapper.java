package com.jetbrains.jetpad.vclang.editor.error;

import com.jetbrains.jetpad.vclang.model.error.ErrorMessage;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.VerticalCell;
import jetbrains.jetpad.cell.util.CellLists;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

public class ErrorListMapper extends Mapper<ObservableArrayList<ErrorMessage>, VerticalCell> {
  public ErrorListMapper() {
    super(new ObservableArrayList<ErrorMessage>(), new VerticalCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(ProjectionalSynchronizers.forRole(this, getSource(), getTarget(), CellLists.newLineSeparated(getTarget().children()), new MapperFactory<ErrorMessage, Cell>() {
      @Override
      public Mapper<? extends ErrorMessage, ? extends Cell> createMapper(ErrorMessage source) {
        return new ErrorMessageMapper(source);
      }
    }));
  }
}
