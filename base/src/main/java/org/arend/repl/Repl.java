package org.arend.repl;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.error.ListErrorReporter;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.reference.Precedence;
import org.arend.extImpl.DefinitionRequester;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.library.resolver.LibraryResolver;
import org.arend.module.FullModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.MergeScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.prelude.PreludeLibrary;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.repl.action.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.provider.EmptyInstanceProvider;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.SyntacticDesugarVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public abstract class Repl {
  public static final @NotNull FullModulePath replModulePath = new FullModulePath(null, FullModulePath.LocationKind.SOURCE, Collections.singletonList("Repl"));

  private final List<Scope> myMergedScopes = new ArrayList<>();
  private final List<ReplHandler> myHandlers = new ArrayList<>();
  private final Set<ModulePath> myModules;
  private final MergeScope myScope = new MergeScope(myMergedScopes);
  private final SourceLibrary myReplLibrary;
  private final TypecheckerState myTypecheckerState;
  private final ConcreteProvider myConcreteProvider;
  private final TypecheckingOrderingListener myTypechecking;
  protected final @NotNull PrettyPrinterConfig myPpConfig = PrettyPrinterConfig.DEFAULT;
  protected final @NotNull ListErrorReporter myErrorReporter;
  protected final @NotNull LibraryManager myLibraryManager;

  public Repl(@NotNull ListErrorReporter listErrorReporter,
              @NotNull LibraryResolver libraryResolver,
              @NotNull ConcreteProvider concreteProvider,
              @NotNull PartialComparator<TCReferable> comparator,
              @NotNull Set<ModulePath> modules,
              @NotNull SourceLibrary replLibrary,
              @NotNull TypecheckerState typecheckerState) {
    this(listErrorReporter, libraryResolver, concreteProvider, comparator, modules, replLibrary, new InstanceProviderSet(), typecheckerState);
  }

  public Repl(@NotNull ListErrorReporter listErrorReporter,
              @NotNull LibraryResolver libraryResolver,
              @NotNull ConcreteProvider concreteProvider,
              @NotNull PartialComparator<TCReferable> comparator,
              @NotNull Set<ModulePath> modules,
              @NotNull SourceLibrary replLibrary,
              @NotNull InstanceProviderSet instanceProviders,
              @NotNull TypecheckerState typecheckerState) {
    this(listErrorReporter, libraryManager(listErrorReporter, libraryResolver, instanceProviders), concreteProvider, comparator, modules, replLibrary, instanceProviders, typecheckerState);
  }

  private static @NotNull LibraryManager libraryManager(@NotNull ListErrorReporter listErrorReporter, @NotNull LibraryResolver libraryResolver, @NotNull InstanceProviderSet instanceProviders) {
    return new LibraryManager(libraryResolver, instanceProviders, listErrorReporter, listErrorReporter, DefinitionRequester.INSTANCE);
  }

  public Repl(@NotNull ListErrorReporter listErrorReporter,
              @NotNull LibraryManager libraryManager,
              @NotNull ConcreteProvider concreteProvider,
              @NotNull PartialComparator<TCReferable> comparator,
              @NotNull Set<ModulePath> modules,
              @NotNull SourceLibrary replLibrary,
              @NotNull InstanceProviderSet instanceProviders,
              @NotNull TypecheckerState typecheckerState) {
    myErrorReporter = listErrorReporter;
    myConcreteProvider = concreteProvider;
    myModules = modules;
    myTypecheckerState = typecheckerState;
    myReplLibrary = replLibrary;
    myLibraryManager = libraryManager;
    myTypechecking = new TypecheckingOrderingListener(instanceProviders, myTypecheckerState, myConcreteProvider, IdReferableConverter.INSTANCE, myErrorReporter, comparator, new LibraryArendExtensionProvider(myLibraryManager));
  }

  private void loadPreludeLibrary() {
    if (!loadLibrary(new PreludeResourceLibrary(myTypecheckerState)))
      eprintln("[FATAL] Failed to load Prelude");
    else myMergedScopes.add(PreludeLibrary.getPreludeScope());
  }

  private void loadReplLibrary() {
    if (!myLibraryManager.loadLibrary(myReplLibrary, myTypechecking))
      eprintln("[FATAL] Failed to load the REPL virtual library");
  }

  protected final @NotNull List<Referable> getInScopeElements() {
    return myScope.getElements();
  }

  public final boolean loadLibrary(@NotNull Library library) {
    if (!myLibraryManager.loadLibrary(library, myTypechecking)) return false;
    myLibraryManager.registerDependency(myReplLibrary, library);
    return true;
  }

  public final void initialize() {
    loadPreludeLibrary();
    loadReplLibrary();
    loadCommands();
  }

  /**
   * The function executed per main-loop of the REPL.
   *
   * @param lineSupplier in case the command requires more user input,
   *                     use this to acquire more lines
   * @param currentLine  the current user input
   * @return true if the REPL wants to quit
   */
  public final boolean repl(@NotNull Supplier<@NotNull String> lineSupplier, @NotNull String currentLine) {
    if (currentLine.startsWith(":quit") || currentLine.equals(":q"))
      return true;
    for (var action : myHandlers)
      if (action.isApplicable(currentLine))
        action.invoke(currentLine, this, lineSupplier);
    return false;
  }

  /**
   * Load a file under the REPL working directory and get its scope.
   * This will <strong>not</strong> modify the REPL scope.
   */
  public final @Nullable Scope loadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.add(modulePath);
    myLibraryManager.reload(myTypechecking);
    if (checkErrors()) {
      myModules.remove(modulePath);
      return null;
    }
    if (isLoadedBefore) {
      Scope scope = getAvailableModuleScopeProvider().forModule(modulePath);
      if (scope != null) removeScope(scope);
    }
    myTypechecking.typecheckLibrary(myReplLibrary);
    return getAvailableModuleScopeProvider().forModule(modulePath);
  }

  /**
   * Like {@link Repl#loadModule(ModulePath)}, this will
   * <strong>not</strong> modify the REPL scope as well.
   *
   * @return true if the module is already loaded before.
   */
  public final boolean unloadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.remove(modulePath);
    if (isLoadedBefore) {
      myLibraryManager.reload(myTypechecking);
      Scope scope = getAvailableModuleScopeProvider().forModule(modulePath);
      if (scope != null) removeScope(scope);
      myReplLibrary.onGroupLoaded(modulePath, null, true);
      myTypechecking.typecheckLibrary(myReplLibrary);
    }
    return isLoadedBefore;
  }

  public final @NotNull ModuleScopeProvider getAvailableModuleScopeProvider() {
    return module -> {
      for (Library registeredLibrary : myLibraryManager.getRegisteredLibraries()) {
        Scope scope = myLibraryManager.getAvailableModuleScopeProvider(registeredLibrary).forModule(module);
        if (scope != null) return scope;
      }
      return null;
    };
  }

  public @NotNull String prompt() {
    return "\u03bb ";
  }

  protected abstract @Nullable Group parseStatements(@NotNull String line);

  protected abstract @Nullable Concrete.Expression parseExpr(@NotNull String text);

  public final void checkStatements(@NotNull String line) {
    var group = parseStatements(line);
    if (group == null) return;
    var moduleScopeProvider = getAvailableModuleScopeProvider();
    Scope scope = CachingScope.make(ScopeFactory.forGroup(group, moduleScopeProvider));
    myMergedScopes.add(scope);
    new DefinitionResolveNameVisitor(myConcreteProvider, myErrorReporter)
        .resolveGroupWithTypes(group, null, myScope);
    if (checkErrors()) {
      myMergedScopes.remove(scope);
      return;
    }
    myLibraryManager.getInstanceProviderSet().collectInstances(group,
        CachingScope.make(ScopeFactory.parentScopeForGroup(group, moduleScopeProvider, true)),
        myConcreteProvider, null);
    if (checkErrors()) {
      myMergedScopes.remove(scope);
      return;
    }
    if (!myTypechecking.typecheckModules(Collections.singletonList(group), null)) {
      checkErrors();
      myMergedScopes.remove(scope);
    }
  }

  protected void loadCommands() {
    myHandlers.add(CodeParsingHandler.INSTANCE);
    myHandlers.add(CommandHandler.INSTANCE);
    registerAction("unload", UnloadModuleCommand.INSTANCE);
    registerAction("modules", ListLoadedModulesAction.INSTANCE);
    registerAction("type", ShowTypeCommand.INSTANCE);
    registerAction("t", ShowTypeCommand.INSTANCE);
    registerAction("load", LoadModuleCommand.INSTANCE);
    registerAction("l", LoadModuleCommand.INSTANCE);
    registerAction("reload", LoadModuleCommand.ReloadModuleCommand.INSTANCE);
    registerAction("r", LoadModuleCommand.ReloadModuleCommand.INSTANCE);
    for (NormalizationMode normalizationMode : NormalizationMode.values()) {
      var name = normalizationMode.name().toLowerCase();
      registerAction(name, new NormalizeCommand(normalizationMode));
    }
  }

  public final @Nullable ReplCommand registerAction(@NotNull String name, @NotNull ReplCommand action) {
    return CommandHandler.INSTANCE.commandMap.put(name, action);
  }

  public final @Nullable ReplCommand unregisterAction(@NotNull String name) {
    return CommandHandler.INSTANCE.commandMap.remove(name);
  }

  public final void clearActions() {
    CommandHandler.INSTANCE.commandMap.clear();
  }

  public final @NotNull Library getReplLibrary() {
    return myReplLibrary;
  }

  /**
   * Multiplex the scope into the current REPL scope.
   */
  public final void addScope(@NotNull Scope scope) {
    myMergedScopes.add(scope);
  }

  /**
   * Remove a multiplexed scope from the current REPL scope.
   *
   * @return true if there is indeed a scope removed
   */
  public final boolean removeScope(@NotNull Scope scope) {
    for (Referable element : scope.getElements())
      if (element instanceof TCReferable)
        myTypecheckerState.reset((TCReferable) element);
    return myMergedScopes.remove(scope);
  }

  /**
   * A replacement of {@link System#out#println(Object)} where it uses the
   * output stream of the REPL.
   *
   * @param anything whose {@link Object#toString()} is invoked.
   */
  public void println(Object anything) {
    print(anything);
    print(System.lineSeparator());
  }

  public abstract void print(Object anything);

  /**
   * @param toError if true, print to stderr. Otherwise print to stdout
   */
  public void printlnOpt(Object anything, boolean toError) {
    if (toError) eprintln(anything);
    else println(anything);
  }

  /**
   * A replacement of {@link System#err#println(Object)} where it uses the
   * error output stream of the REPL.
   *
   * @param anything whose {@link Object#toString()} is invoked.
   */
  public abstract void eprintln(Object anything);

  public final @NotNull StringBuilder prettyExpr(@NotNull StringBuilder builder, @NotNull Expression expression) {
    var abs = ToAbstractVisitor.convert(expression, myPpConfig);
    abs.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder;
  }

  /**
   * @param expr input concrete expression.
   * @see Repl#preprocessExpr(String)
   */
  public final @Nullable TypecheckingResult checkExpr(@NotNull Concrete.Expression expr, @Nullable Expression expectedType) {
    var typechecker = new CheckTypeVisitor(myTypecheckerState, myErrorReporter, null, null);
    var instancePool = new GlobalInstancePool(EmptyInstanceProvider.getInstance(), typechecker);
    typechecker.setInstancePool(instancePool);
    var result = typechecker.checkExpr(expr, expectedType);
    return checkErrors() ? null : result;
  }

  /**
   * @see Repl#checkExpr(Concrete.Expression, Expression)
   */
  public final @Nullable Concrete.Expression preprocessExpr(@NotNull String text) {
    var expr = parseExpr(text);
    if (expr == null || checkErrors()) return null;
    expr = expr
        .accept(new ExpressionResolveNameVisitor(myConcreteProvider,
            myScope, Collections.emptyList(), myErrorReporter, null), null)
        .accept(new SyntacticDesugarVisitor(myErrorReporter), null);
    if (checkErrors()) return null;
    return expr;
  }

  /**
   * Check and print errors.
   *
   * @return true if there is error(s).
   */
  public final boolean checkErrors() {
    var errorList = myErrorReporter.getErrorList();
    for (GeneralError error : errorList)
      printlnOpt(error.getDoc(myPpConfig), error.isSevere());
    boolean hasErrors = !errorList.isEmpty();
    errorList.clear();
    return hasErrors;
  }
}
