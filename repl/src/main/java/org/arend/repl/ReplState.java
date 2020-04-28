package org.arend.repl;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.error.ListErrorReporter;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.reference.Precedence;
import org.arend.extImpl.DefinitionRequester;
import org.arend.library.Library;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryManager;
import org.arend.library.resolver.LibraryResolver;
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
import org.arend.repl.action.ElaborateExprAction;
import org.arend.repl.action.ReplAction;
import org.arend.repl.action.ReplCommand;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.SyntacticDesugarVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public abstract class ReplState implements ReplApi {
  protected final PrettyPrinterConfig myPpConfig = PrettyPrinterConfig.DEFAULT;
  private final List<Scope> myMergedScopes = new ArrayList<>();
  private final List<ReplAction> myActions = new ArrayList<>();
  private final MergeScope myScope = new MergeScope(myMergedScopes);
  protected final @NotNull ListErrorReporter myErrorReporter;
  protected final @NotNull TypecheckerState myTypecheckerState;
  protected final @NotNull ReplLibrary myReplLibrary;
  protected final @NotNull LibraryManager myLibraryManager;
  protected final @NotNull ConcreteProvider myConcreteProvider;
  protected final @NotNull TypecheckingOrderingListener myTypechecking;

  public static final List<String> definitionEvidence = Arrays.asList(
      "\\import", "\\open", "\\use", "\\func", "\\sfunc", "\\lemma",
      "\\data", "\\module", "\\meta", "\\instance", "\\class");
  private static final @NotNull ReplAction defaultAction = ElaborateExprAction.INSTANCE;

  private final @NotNull PrintStream myStdout;
  private final @NotNull PrintStream myStderr;

  public ReplState(@NotNull ListErrorReporter listErrorReporter,
                   @NotNull LibraryResolver libraryResolver,
                   @NotNull ConcreteProvider concreteProvider,
                   @NotNull PartialComparator<TCReferable> comparator,
                   @NotNull PrintStream stdout,
                   @NotNull PrintStream stderr,
                   @NotNull ReplLibrary replLibrary,
                   @NotNull TypecheckerState typecheckerState) {
    myErrorReporter = listErrorReporter;
    myConcreteProvider = concreteProvider;
    myTypecheckerState = typecheckerState;
    myStdout = stdout;
    myStderr = stderr;
    myReplLibrary = replLibrary;
    var instanceProviders = new InstanceProviderSet();
    myLibraryManager = new LibraryManager(libraryResolver, instanceProviders, this.myErrorReporter, this.myErrorReporter, DefinitionRequester.INSTANCE);
    myTypechecking = new TypecheckingOrderingListener(instanceProviders, myTypecheckerState, myConcreteProvider, IdReferableConverter.INSTANCE, this.myErrorReporter, comparator, new LibraryArendExtensionProvider(myLibraryManager));
  }

  public void loadPreludeLibrary() {
    var preludeLibrary = new PreludeResourceLibrary(myTypecheckerState);
    if (!myLibraryManager.loadLibrary(preludeLibrary, myTypechecking)) {
      myStderr.println("[FATAL] Failed to load Prelude");
    }
    myReplLibrary.addDependency(new LibraryDependency(preludeLibrary.getName()));
    myMergedScopes.add(PreludeLibrary.getPreludeScope());
  }

  private void loadReplLibrary() {
    if (!myLibraryManager.loadLibrary(myReplLibrary, myTypechecking))
      myStderr.println("[FATAL] Failed to load the REPL virtual library");
  }

  private boolean loadLibrary(Library library) {
    if (!myLibraryManager.loadLibrary(library, myTypechecking)) return false;
    myLibraryManager.registerDependency(myReplLibrary, library);
    return true;
  }

  public void runRepl(@NotNull InputStream inputStream) {
    loadPreludeLibrary();
    loadReplLibrary();

    var scanner = new Scanner(inputStream);
    while (scanner.hasNext()) {
      myStdout.print("\u03bb ");
      myStdout.flush();
      String line = scanner.nextLine();
      if (line.startsWith(":load"))
        actionLoad(line.substring(":load".length()));
      else if (line.startsWith(":l"))
        actionLoad(line.substring(":l".length()));
      else if (line.startsWith(":type"))
        actionType(line.substring(":type".length()));
      else if (line.startsWith(":t"))
        actionType(line.substring(":t".length()));
      else if (line.startsWith(":quit") || line.equals(":q"))
        break;
      else if (line.startsWith(":nf")) {
        var result = checkExpr(line.substring(":nf".length()), null);
        if (result == null) continue;
        myStdout.println(result.expression.normalize(NormalizationMode.NF));
      } else if (line.startsWith(":whnf")) {
        var result = checkExpr(line.substring(":whnf".length()), null);
        if (result == null) continue;
        myStdout.println(result.expression.normalize(NormalizationMode.WHNF));
      } else if (line.startsWith(":")) {
        myStderr.println("[ERROR] Unrecognized command: " + line.substring(1) + ".");
      } else if (definitionEvidence.stream().anyMatch(line::contains)) {
        var group = parseStatements(line);
        if (group == null) continue;
        var moduleScopeProvider = myReplLibrary.getModuleScopeProvider();
        Scope scope = CachingScope.make(ScopeFactory.forGroup(group, moduleScopeProvider));
        myMergedScopes.add(scope);
        new DefinitionResolveNameVisitor(myConcreteProvider, myErrorReporter)
            .resolveGroupWithTypes(group, null, myScope);
        if (checkErrors()) {
          myMergedScopes.remove(scope);
          continue;
        }
        myLibraryManager.getInstanceProviderSet().collectInstances(group,
            CachingScope.make(ScopeFactory.parentScopeForGroup(group, moduleScopeProvider, true)),
            myConcreteProvider, null);
        if (checkErrors()) {
          myMergedScopes.remove(scope);
          continue;
        }
        if (!myTypechecking.typecheckModules(Collections.singletonList(group), null)) {
          checkErrors();
          myMergedScopes.remove(scope);
        }
      } else if (defaultAction.isApplicable(line)) {
        defaultAction.invoke(line, this);
      }
    }
  }

  protected abstract @Nullable FileGroup parseStatements(String line);

  protected abstract @Nullable Concrete.Expression parseExpr(@NotNull String text);

  protected void initialize() {
  }

  @Override
  public final void registerAction(@NotNull ReplCommand action) {
    myActions.add(action);
  }

  @Override
  public final boolean unregisterAction(@NotNull ReplAction action) {
    return myActions.remove(action);
  }

  @Override
  public final void clearActions() {
    myActions.clear();
  }

  @Override
  public final @NotNull List<Scope> getMergedScopes() {
    return myMergedScopes;
  }

  private void actionType(String line) {
    var result = checkExpr(line, null);
    if (result == null) return;
    Expression type = result.expression.getType();
    myStdout.println(type == null ? "Cannot synthesize a type, sorry." : type);
  }

  private void actionLoad(String text) {
/* TODO
    var libPath = Paths.get(text);
    if (!loadLibrary(myLibraryResolver.registerLibrary(libPath)))
      myStderr.println("[ERROR] Failed to load the library specified.");
*/
  }

  @Override
  public void println(Object anything) {
    myStdout.println(anything);
  }

  @Override
  public void eprintln(Object anything) {
    myStderr.println(anything);
    myStderr.flush();
  }

  @Override
  public @NotNull StringBuilder prettyExpr(@NotNull StringBuilder builder, @NotNull Expression expression) {
    var abs = ToAbstractVisitor.convert(expression, myPpConfig);
    abs.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder;
  }

  @Override
  public @Nullable TypecheckingResult checkExpr(@NotNull String text, @Nullable Expression expectedType) {
    var expr = preprocessExpr(text);
    if (expr == null || checkErrors()) return null;
    var result = typeChecker().checkExpr(expr, expectedType);
    return checkErrors() ? null : result;
  }

  @NotNull
  private CheckTypeVisitor typeChecker() {
    return new CheckTypeVisitor(myTypecheckerState, myErrorReporter, null, null);
  }

  private @Nullable Concrete.Expression preprocessExpr(@NotNull String text) {
    var expr = parseExpr(text);
    if (expr == null || checkErrors()) return null;
    expr = expr
        .accept(new ExpressionResolveNameVisitor(myConcreteProvider,
            myScope, Collections.emptyList(), myErrorReporter, null), null)
        .accept(new SyntacticDesugarVisitor(myErrorReporter), null);
    if (checkErrors()) return null;
    return expr;
  }

  @Override
  public final boolean checkErrors() {
    var errorList = myErrorReporter.getErrorList();
    for (GeneralError error : errorList)
      (error.isSevere() ? myStderr : myStdout).println(error.getDoc(myPpConfig));
    boolean hasErrors = !errorList.isEmpty();
    errorList.clear();
    return hasErrors;
  }
}
