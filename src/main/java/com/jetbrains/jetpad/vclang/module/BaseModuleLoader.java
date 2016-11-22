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
import java.util.HashMap;
import java.util.Map;

public class BaseModuleLoader<SourceIdT extends SourceId> extends SourceModuleLoader<SourceIdT> {
  private final SourceSupplier<SourceIdT> mySourceSupplier;
  private final ErrorReporter myErrorReporter;
  private final ModuleLoadingListener<SourceIdT> myListener;
  private final Map<SourceIdT, SourceSupplier.Result> myLoadedModules = new HashMap<>();

  public BaseModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ErrorReporter errorReporter, ModuleLoadingListener<SourceIdT> listener) {
    mySourceSupplier = sourceSupplier;
    myErrorReporter = errorReporter;
    myListener = listener;
  }


  @Override
  public SourceIdT locateModule(ModulePath modulePath) {
    return mySourceSupplier.locateModule(modulePath);
  }

  @Override
  public SourceSupplier.Result load(SourceIdT sourceId) {
    SourceSupplier.Result loaded = myLoadedModules.get(sourceId);
    if (loaded != null) {
      return loaded;
    }

    try {
      CountingErrorReporter countingErrorReporter = new CountingErrorReporter(Error.Level.ERROR);
      SourceSupplier.Result result = mySourceSupplier.loadSource(sourceId, new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
      if (result == null) {
        myListener.loadingError(sourceId, new ModuleNotFoundError(sourceId));
        return null;
      }

      int errorCount = countingErrorReporter.getErrorsNumber();
      if (errorCount == 0) {
        assert result.definition != null;
        myLoadedModules.put(sourceId, result);
        myListener.loadingSucceeded(sourceId, result.definition);
      } else {
        ModuleLoadingError error = new ModuleLoadingError(sourceId, "Module  contains " + errorCount + (errorCount == 1 ? " error" : " errors"));
        myListener.loadingError(sourceId, error);
      }
      return result;
    } catch (IOException e) {
      myListener.loadingError(sourceId, new ModuleLoadingError(sourceId, GeneralError.ioError(e)));
    }

    return null;
  }


  public static class ModuleLoadingListener<SourceIdT extends SourceId> {
    protected void loadingSucceeded(SourceIdT module, Abstract.ClassDefinition abstractDefinition) {
    }
    protected void loadingError(SourceIdT module, ModuleLoadingError loadingError) {
    }
  }
}
