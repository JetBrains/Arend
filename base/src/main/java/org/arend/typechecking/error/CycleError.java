package org.arend.typechecking.error;

import org.arend.core.definition.ClassField;
import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.ArendRef;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class CycleError extends GeneralError {
  public final List<? extends GlobalReferable> cycle;
  public final Concrete.SourceNode cause;
  private final GlobalReferable myCauseReferable;
  private final boolean myDoCycle;

  private CycleError(String message, List<? extends GlobalReferable> cycle, boolean doCycle, GlobalReferable causeReferable, Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cycle = cycle;
    this.cause = cause;
    myDoCycle = doCycle;
    myCauseReferable = causeReferable;
  }

  public CycleError(String message, List<? extends GlobalReferable> cycle, boolean doCycle) {
    this(message, cycle, doCycle, null, null);
  }

  public CycleError(List<? extends GlobalReferable> cycle) {
    this("Dependency cycle", cycle, true);
  }

  public static CycleError fieldDependency(List<? extends ClassField> cycle, Concrete.SourceNode cause) {
    List<GlobalReferable> refs = new ArrayList<>(cycle.size());
    for (ClassField field : cycle) {
      refs.add(field.getReferable());
    }
    return new CycleError("Field dependency cycle", refs, true, null, cause);
  }

  @Override
  public Concrete.SourceNode getCauseSourceNode() {
    return cause;
  }

  @Override
  public Object getCause() {
    if (cause != null) {
      Object data = cause.getData();
      if (data != null) {
        return data;
      }
    }
    return myCauseReferable != null ? myCauseReferable : cycle;
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig ppConfig) {
    Doc causeDoc = super.getCauseDoc(ppConfig);
    return causeDoc != null ? causeDoc : refDoc(cycle.get(0));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    Set<ModulePath> modules = new LinkedHashSet<>();
    for (GlobalReferable referable : cycle) {
      if (referable instanceof LocatedReferable) {
        ModuleLocation location = ((LocatedReferable) referable).getLocation();
        if (location != null) {
          modules.add(location.getModulePath());
        }
      }
    }

    List<LineDoc> docs = new ArrayList<>(cycle.size() + 1);
    for (GlobalReferable definition : cycle) {
      docs.add(refDoc(definition));
    }
    if (myDoCycle) {
      docs.add(refDoc(cycle.get(0)));
    }
    Doc result = hSep(text(" - "), docs);
    if (modules.size() > 1) {
      List<LineDoc> modulesDocs = new ArrayList<>(modules.size());
      for (ModulePath module : modules) {
        modulesDocs.add(text(module.toString()));
      }
      result = vList(result, hList(text("Located in modules: "), hSep(text(", "), modulesDocs)));
    }
    return result;
  }

  @Override
  public void forAffectedDefinitions(BiConsumer<ArendRef, GeneralError> consumer) {
    Object causeData = cause != null ? cause.getData() : null;
    if (causeData instanceof GlobalReferable) {
      consumer.accept((GlobalReferable) causeData, this);
    } else {
      for (GlobalReferable ref : cycle) {
        consumer.accept(ref, new CycleError(message, cycle, myDoCycle, ref, cause));
      }
    }
  }

  @NotNull
  @Override
  public Stage getStage() {
    return Stage.TYPECHECKER;
  }
}
