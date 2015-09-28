package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.error.CycleError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.output.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.module.source.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseModuleLoader implements ModuleLoader {
  private final List<Namespace> myLoadingModules = new ArrayList<>();
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private final boolean myRecompile;

  public BaseModuleLoader(boolean recompile) {
    mySourceSupplier = DummySourceSupplier.getInstance();
    myOutputSupplier = DummyOutputSupplier.getInstance();
    myRecompile = true; // recompile; // TODO: Fix serialization.
  }

  public void setSourceSupplier(SourceSupplier sourceSupplier) {
    mySourceSupplier = sourceSupplier;
  }

  public void setOutputSupplier(OutputSupplier outputSupplier) {
    myOutputSupplier = outputSupplier;
  }

  @Override
  public ModuleLoadingResult load(Namespace parent, String name, boolean tryLoad) {
    DefinitionPair member = parent.getMember(name);
    if (member != null && (member.abstractDefinition != null || member.definition != null)) {
      return null;
    }
    Namespace module = member == null ? parent.getChild(new Utils.Name(name)) : member.namespace;

    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      loadingError(new CycleError(module, new ArrayList<>(myLoadingModules.subList(index, myLoadingModules.size()))));
      return null;
    }

    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);
    boolean compile;
    if (source.isAvailable()) {
      compile = myRecompile || !output.canRead() || source.lastModified() > output.lastModified();
    } else {
      output = myOutputSupplier.locateOutput(module);
      if (!output.canRead()) {
        if (!tryLoad) {
          loadingError(new ModuleNotFoundError(module));
        }
        return null;
      }
      compile = false;
    }

    myLoadingModules.add(module);
    try {
      ModuleLoadingResult result;
      if (compile) {
        result = source.load();
        if (result != null && result.errorsNumber == 0 && result.definition != null && result.definition.definition instanceof ClassDefinition && output.canWrite()) {
          output.write(module, (ClassDefinition) result.definition.definition);
        }
      } else {
        result = output.read();
      }

      if (result == null || result.errorsNumber != 0) {
        GeneralError error = new GeneralError(module, result == null ? "cannot load module" : "module contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
        error.setLevel(GeneralError.Level.INFO);
        loadingError(error);
      } else {
        if (result.definition != null && (result.definition.abstractDefinition != null || result.definition.definition != null)) {
          loadingSucceeded(module, result.definition, result.compiled);
        }
      }

      if (result != null && result.definition != null) {
        parent.addMember(result.definition);
      }
      return result;
    } catch (EOFException e) {
      loadingError(new GeneralError(module, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      loadingError(new GeneralError(module, e.toString()));
    } catch (IOException e) {
      loadingError(new GeneralError(module, GeneralError.ioError(e)));
    } finally {
      myLoadingModules.remove(myLoadingModules.size() - 1);
    }

    return null;
  }
}
