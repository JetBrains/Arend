package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.*;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public class DefaultModuleLoader<SourceIdT extends SourceId> extends SourceModuleLoader<SourceIdT> {
  private final SourceSupplier<SourceIdT> mySourceSupplier;
  protected final ErrorReporter myErrorReporter;
  private final ModuleLoadingListener<SourceIdT> myListener;

  public DefaultModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ErrorReporter errorReporter, ModuleLoadingListener<SourceIdT> listener) {
    mySourceSupplier = sourceSupplier;
    myErrorReporter = errorReporter;
    myListener = listener;
  }


  @Override
  public SourceIdT locateModule(ModulePath modulePath) {
    return mySourceSupplier.locateModule(modulePath);
  }

  @Override
  public boolean isAvailable(SourceIdT sourceId) {
    return mySourceSupplier.isAvailable(sourceId);
  }

  @Override
  public Abstract.ClassDefinition load(SourceIdT sourceId) {
    try {
      CountingErrorReporter countingErrorReporter = new CountingErrorReporter(Error.Level.ERROR);
      Abstract.ClassDefinition result = mySourceSupplier.loadSource(sourceId, new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
      if (result == null) {
        myListener.loadingError(sourceId, new ModuleNotFoundError(sourceId));
        return null;
      }

      int errorCount = countingErrorReporter.getErrorsNumber();
      if (errorCount == 0) {
        myListener.loadingSucceeded(sourceId, result);
        return result;
      } else {
        ModuleLoadingError error = new ModuleLoadingError(sourceId, "Module  contains " + errorCount + (errorCount == 1 ? " error" : " errors"));
        myListener.loadingError(sourceId, error);
        return null;
      }
    } catch (IOException e) {
      myListener.loadingError(sourceId, new ModuleLoadingError(sourceId, GeneralError.ioError(e)));
    }

    return null;
  }


  public static class ModuleLoadingListener<SourceIdT extends SourceId> {
    public void loadingSucceeded(SourceIdT module, Abstract.ClassDefinition abstractDefinition) {
    }
    public void loadingError(SourceIdT module, ModuleLoadingError loadingError) {
    }
  }
}
