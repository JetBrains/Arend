package com.jetbrains.jetpad.vclang.editor.error;

import com.jetbrains.jetpad.vclang.model.error.ErrorMessage;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;

import static jetbrains.jetpad.mapper.Synchronizers.forPropsOneWay;

public class ErrorMessageMapper extends Mapper<ErrorMessage, ErrorMessageMapper.Cell> {
  public ErrorMessageMapper(ErrorMessage source) {
    super(source, new Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsOneWay(getSource().message(), getTarget().message.text()));
  }

  public static class Cell extends IndentCell {
    public final TextCell message = new TextCell();

    public Cell() {
      CellFactory.to(this, message);
      message.focusable().set(true);
    }
  }
}
