package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.error.ModuleCycleError;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.utils.LoadModulesRecursively;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ModuleLoader<ModuleSourceIdT extends ModuleSourceId> {
  private final List<ModulePath> myLoadingModules = new ArrayList<>();
  private final Map<ModulePath, Result> myLoadedModules = new HashMap<>();

  protected abstract ModuleSourceIdT locateModule(ModulePath modulePath);
  protected abstract Source getSource(ModuleSourceIdT moduleSourceId);

  protected abstract void loadingSucceeded(ModuleSourceIdT module, Abstract.ClassDefinition abstractDefinition);
  protected abstract void loadingError(ModuleSourceIdT module, ModuleLoadingError loadingError);

  public Result load(ModulePath module) {
    Result loaded = myLoadedModules.get(module);
    if (loaded != null) {
      return loaded;
    }

    ModuleSourceIdT sourceId = locateModule(module);
    assert sourceId != null;

    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      loadingError(sourceId, new ModuleCycleError(sourceId, new ArrayList<>(myLoadingModules.subList(index, myLoadingModules.size()))));
      return null;
    }

    try {
      Source source = getSource(sourceId);
      if (source == null) {
        loadingError(sourceId, new ModuleNotFoundError(sourceId));
        return null;
      }

      try {
        myLoadingModules.add(module);

        Result result = source.load();
        if (result != null && result.errorsNumber == 0) {
          assert result.abstractDefinition != null;
          new LoadModulesRecursively(this).visitClass(result.abstractDefinition, null);
          loadingSucceeded(sourceId, result.abstractDefinition);
          myLoadedModules.put(module, result);
        } else {
          ModuleLoadingError error = new ModuleLoadingError(sourceId, result == null ? "Cannot load module" : "Module  contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
          loadingError(sourceId, error);
        }

        return result;
      } finally {
        myLoadingModules.remove(myLoadingModules.size() - 1);
      }
    } catch (EOFException e) {
      loadingError(sourceId, new ModuleLoadingError(sourceId, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      loadingError(sourceId, new ModuleLoadingError(sourceId, e.toString()));
    } catch (IOException e) {
      loadingError(sourceId, new ModuleLoadingError(sourceId, GeneralError.ioError(e)));
    }
    return null;
  }


  public static class Result {
    public final Abstract.ClassDefinition abstractDefinition;
    public final int errorsNumber;

    public Result(Abstract.ClassDefinition abstractDefinition, int errorsNumber) {
      this.abstractDefinition = abstractDefinition;
      this.errorsNumber = errorsNumber;
    }
  }
}
