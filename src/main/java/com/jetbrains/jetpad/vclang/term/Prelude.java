package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.file.FileStorage;
import com.jetbrains.jetpad.vclang.module.source.file.ParseSource;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends SimpleNamespace {
  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;
  public static FunctionDefinition ISO;

  public static DataDefinition PROP_TRUNC;
  public static DataDefinition SET_TRUNC;

  public static Constructor PROP_TRUNC_PATH_CON;
  public static Constructor SET_TRUNC_PATH_CON;

  private Prelude() {
  }

  public static void update(Abstract.Definition abstractDef, Definition definition) {
    if (abstractDef.getName().equals("Nat")) {
      NAT = (DataDefinition) definition;
      ZERO = NAT.getConstructor("zero");
      SUC = NAT.getConstructor("suc");
    } else
    if (abstractDef.getName().equals("I")) {
      INTERVAL = (DataDefinition) definition;
      INTERVAL.setSorts(new SortMax(Sort.PROP));
      INTERVAL.setMatchesOnInterval();
      LEFT = INTERVAL.getConstructor("left");
      RIGHT = INTERVAL.getConstructor("right");
    } else
    if (abstractDef.getName().equals("Path")) {
      PATH = (DataDefinition) definition;
      List<LevelBinding> pathPolyParams = PATH.getPolyParams();
      PATH.getParameters().setType(Pi(Interval(), Universe(new Sort(new Level(pathPolyParams.get(0)), new Level(pathPolyParams.get(1), 1)))));
      PATH_CON = PATH.getConstructor("path");
    } else
    if (abstractDef.getName().equals("=")) {
      PATH_INFIX = (FunctionDefinition) definition;
      List<LevelBinding> infixPolyParams = PATH_INFIX.getPolyParams();
      PATH_INFIX.getParameters().setType(Universe(new Sort(new Level(infixPolyParams.get(0)), new Level(infixPolyParams.get(1), 1))));
    } else
    if (abstractDef.getName().equals("@")) {
      AT = (FunctionDefinition) definition;
      DependentLink param4 = AT.getParameters().getNext().getNext().getNext();
      DependentLink atPath = param("f", PATH_CON.getParameters().getType());
      AT.setElimTree(top(AT.getParameters(), branch(param4.getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(AT.getParameters().getNext())),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(AT.getParameters().getNext().getNext())),
        clause(branch(param4, tail(param4.getNext()),
            clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(param4.getNext()))))))));
      AT.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);
    } else
    if (abstractDef.getName().equals("coe")) {
      COERCE = (FunctionDefinition) definition;
      COERCE.setElimTree(top(COERCE.getParameters(), branch(COERCE.getParameters().getNext().getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(COERCE.getParameters().getNext())))));
      COERCE.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);
    } else
    if (abstractDef.getName().equals("iso")) {
      ISO = (FunctionDefinition) definition;
      ISO.setElimTree(top(ISO.getParameters(), branch(ISO.getParameters().getNext().getNext().getNext().getNext().getNext().getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(ISO.getParameters())),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(ISO.getParameters().getNext())))));
      ISO.setResultType(new PiUniverseType(EmptyDependentLink.getInstance(), new SortMax(new Sort(new Level(ISO.getPolyParams().get(0)), new Level(ISO.getPolyParams().get(1))))));
      ISO.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);
    } else
    if (abstractDef.getName().equals("TrP")) {
      PROP_TRUNC = (DataDefinition) definition;
      PROP_TRUNC.setSorts(new SortMax(Sort.PROP));
      PROP_TRUNC_PATH_CON = PROP_TRUNC.getConstructor("truncP");
    } else
    if (abstractDef.getName().equals("TrS")) {
      SET_TRUNC = (DataDefinition) definition;
      SET_TRUNC.setSorts(new SortMax(Sort.SetOfLevel(new Level(SET_TRUNC.getPolyParams().get(0)))));
      SET_TRUNC_PATH_CON = SET_TRUNC.getConstructor("truncS");
    }
  }

  public static class UpdatePreludeReporter implements TypecheckedReporter {
    private final TypecheckerState state;

    public UpdatePreludeReporter(TypecheckerState state) {
      this.state = state;
    }

    @Override
    public void typecheckingSucceeded(Abstract.Definition definition) {
      update(definition, state.getTypechecked(definition));
    }

    @Override
    public void typecheckingFailed(Abstract.Definition definition) {
      update(definition, state.getTypechecked(definition));
    }
  }


  public static class PreludeStorage implements SourceSupplier<SourceId>, CacheStorageSupplier<SourceId> {
    public static String SOURCE_RESOURCE_PATH = "/lib/Prelude";
    public static ModulePath PRELUDE_MODULE_PATH = new ModulePath("Prelude");
    public final SourceId preludeSourceId = new SourceId();

    @Override
    public InputStream getCacheInputStream(SourceId sourceId) {
      if (sourceId != preludeSourceId) return null;
      return Prelude.class.getResourceAsStream(SOURCE_RESOURCE_PATH + FileStorage.SERIALIZED_EXTENSION);
    }

    @Override
    public OutputStream getCacheOutputStream(SourceId sourceId) {
      // Prelude cache is generated during build and stored as a resource,
      // therefore PreludeStorage does not support serialization of Prelude in runtime.
      return null;
    }

    @Override
    public SourceId locateModule(ModulePath modulePath) {
      if (modulePath.getParent().list().length == 0 && modulePath.getName().equals("Prelude")) {
        return preludeSourceId;
      } else {
        return null;
      }
    }

    @Override
    public boolean isAvailable(SourceId sourceId) {
      return sourceId == preludeSourceId;
    }

    @Override
    public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
      if (sourceId != preludeSourceId) return null;
      InputStream stream = Prelude.class.getResourceAsStream(SOURCE_RESOURCE_PATH + FileStorage.EXTENSION);
      return new ParseSource(preludeSourceId, stream) {}.load(errorReporter);
    }
  }

  public static class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private SourceId() {}

    @Override
    public ModulePath getModulePath() {
      return PreludeStorage.PRELUDE_MODULE_PATH;
    }

    @Override
    public String toString() {
      return "PRELUDE";
    }
  }


}
