package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.editor.error.ErrorListMapper;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.projectional.util.RootController;

public class ContainerFactory {
  private static CellContainer MAIN_CONTAINER;
  private static CellContainer ERRORS_CONTAINER;
  private static ErrorListMapper ERRORS_ROOT_MAPPER;

  static {
    MAIN_CONTAINER = new CellContainer();
    RootController.install(MAIN_CONTAINER);
  }

  static {
    ERRORS_ROOT_MAPPER = new ErrorListMapper();
    ERRORS_ROOT_MAPPER.attachRoot();

    ERRORS_CONTAINER = new CellContainer();
    ERRORS_CONTAINER.root.children().add(ERRORS_ROOT_MAPPER.getTarget());
    RootController.install(ERRORS_CONTAINER);
  }

  public static CellContainer getMainContainer() {
    return MAIN_CONTAINER;
  }

  public static CellContainer getErrorsContainer() {
    return ERRORS_CONTAINER;
  }

  public static ErrorListMapper getErrorsRootMapper() {
    return ERRORS_ROOT_MAPPER;
  }
}