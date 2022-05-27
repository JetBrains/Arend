package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.FreeVariablesCollector;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.util.Pair;
import org.arend.extImpl.ConcreteFactoryImpl;
import org.arend.naming.reference.DataLocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.SubstConcreteExpressionVisitor;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.FunctionArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.error.local.inference.LambdaInferenceError;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final public class MinimizedRepresentation {
    private MinimizedRepresentation() {
    }

    /**
     * Converts {@code expressionToPrint} to concrete, inserting as little additional information (like implicit arguments)
     * as possible. Resulting concrete expression is intended to be type checkable, but not ground.
     */
    public static @NotNull Concrete.Expression generateMinimizedRepresentation(
            @NotNull Expression expressionToPrint,
            @Nullable InstanceProvider instanceProvider,
            @Nullable DefinitionRenamer definitionRenamer,
            @Nullable Supplier<@NotNull ReferableRenamer> referableRenamer) {
        Expression actualExpression = expressionToPrint.normalize(NormalizationMode.RNF);
        var pair = generateRepresentations(actualExpression, definitionRenamer, referableRenamer);
        Concrete.Expression verboseRepresentation = pair.proj1;
        Concrete.Expression incompleteRepresentation = pair.proj2;
        List<GeneralError> errorsCollector = new ArrayList<>();
        var typechecker = generateTypechecker(instanceProvider, errorsCollector);
        induceContext(typechecker, verboseRepresentation, incompleteRepresentation, actualExpression);

        int limit = 50;
        while (true) {
            var fixedExpression = tryFixError(typechecker, verboseRepresentation, incompleteRepresentation, errorsCollector);
            if (fixedExpression == null) {
                return incompleteRepresentation;
            } else {
                --limit;
                if (limit == 0) {
                    throw new IllegalStateException("Minimization of expression (" + actualExpression + ") is likely diverged. Please report it to maintainers.\n " +
                            "Errors: \n" + errorsCollector);
                }
                incompleteRepresentation = fixedExpression;
            }
        }
    }

    private static void induceContext(CheckTypeVisitor typechecker, Concrete.Expression verboseRepresentation, Concrete.Expression incompleteRepresentation, Expression expressionToPrint) {
        Map<String, List<Referable>> freeReferables = getFreeReferables(verboseRepresentation, incompleteRepresentation);
        Map<String, Binding> freeBindings = collectFreeBindings(expressionToPrint);

        for (var nameToBinding : freeBindings.entrySet()) {
            var referables = freeReferables.get(nameToBinding.getKey());
            if (referables == null) {
                continue;
            }
            for (var referable : referables) {
                typechecker.addBinding(referable, nameToBinding.getValue());
            }
        }
        List<Referable> thisEntries = freeReferables.get("this");
        if (thisEntries != null && thisEntries.size() == 1) {
            var ref = thisEntries.get(0);
            if (ref instanceof DataLocalReferable && ((DataLocalReferable) ref).getData() instanceof ClassCallExpression) {
                typechecker.addBinding(ref, ((ClassCallExpression) ((DataLocalReferable) ref).getData()).getThisBinding());
            }
        }
    }

    @NotNull
    private static Map<String, List<Referable>> getFreeReferables(Concrete.Expression verboseRepresentation, Concrete.Expression incompleteRepresentation) {
        var freeReferables = new LinkedHashSet<Referable>();
        verboseRepresentation.accept(new FreeVariableCollectorConcrete(freeReferables), null);
        incompleteRepresentation.accept(new FreeVariableCollectorConcrete(freeReferables), null);
        Map<String, List<Referable>> mapping = new HashMap<>();
        for (Referable freeReferable : freeReferables) {
            mapping.computeIfAbsent(freeReferable.getRefName(), __ -> new ArrayList<>()).add(freeReferable);
        }
        return mapping;
    }

    private static Concrete.Expression addTrailingImplicitArguments(Concrete.Expression verboseRepresentation, Concrete.Expression incompleteRepresentation) {
        if (!(verboseRepresentation instanceof Concrete.AppExpression)) {
            return incompleteRepresentation;
        }
        List<Concrete.Argument> verboseArguments = ((Concrete.AppExpression) verboseRepresentation).getArguments();
        List<Concrete.Argument> trailingImplicitArguments = new ArrayList<>();
        for (var arg : verboseArguments) {
            if (arg.isExplicit()) {
                trailingImplicitArguments.clear();
            } else {
                trailingImplicitArguments.add(arg);
            }
        }
        return Concrete.AppExpression.make(null, incompleteRepresentation, trailingImplicitArguments);
    }

    private static Pair<Concrete.Expression, Concrete.Expression>
    generateRepresentations(Expression core,
                            @Nullable DefinitionRenamer definitionRenamer,
                            @Nullable Supplier<@NotNull ReferableRenamer> referableRenamer) {
        var verboseConfig = new PrettyPrinterConfig() {
            @Override
            public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                return EnumSet.of(
                        PrettyPrinterFlag.SHOW_TYPES_IN_LAM,
                        PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE,
                        PrettyPrinterFlag.SHOW_CON_PARAMS,
                        PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS,
                        PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS,
                        PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE,
                        PrettyPrinterFlag.SHOW_IMPLICIT_ARGS,
                        PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE,
                        PrettyPrinterFlag.SHOW_TUPLE_TYPE,
                        PrettyPrinterFlag.SHOW_PROOFS);
            }

            @Override
            public @Nullable NormalizationMode getNormalizationMode() {
                return null;
            }

            @Override
            public @Nullable DefinitionRenamer getDefinitionRenamer() {
                return definitionRenamer;
            }
        };
        var emptyConfig = new PrettyPrinterConfig() {
            @Override
            public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                return EnumSet.of(PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS, PrettyPrinterFlag.SHOW_PROOFS);
            }

            @Override
            public @Nullable NormalizationMode getNormalizationMode() {
                return null;
            }

            @Override
            public @Nullable DefinitionRenamer getDefinitionRenamer() {
                return definitionRenamer;
            }
        };

        Supplier<ReferableRenamer> notNullRenamer = referableRenamer == null ? ReferableRenamer::new : referableRenamer;
        var verboseRepresentation = ToAbstractVisitor.convert(core, verboseConfig, notNullRenamer.get());
        var incompleteRepresentation = ToAbstractVisitor.convert(core, emptyConfig, notNullRenamer.get());
        if (verboseRepresentation instanceof Concrete.ClassExtExpression && incompleteRepresentation instanceof Concrete.ClassExtExpression) {
            verboseRepresentation = ((Concrete.ClassExtExpression) verboseRepresentation).getBaseClassExpression();
            incompleteRepresentation = ((Concrete.ClassExtExpression) incompleteRepresentation).getBaseClassExpression();
        }
        incompleteRepresentation = incompleteRepresentation.accept(new BiConcreteVisitor() {
            @Override
            public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Concrete.SourceNode params) {
                if (params instanceof Concrete.AppExpression) {
                    return ((Concrete.AppExpression) params).getFunction();
                } else {
                    return (Concrete.Expression) params;
                }
            }

            @Override
            protected Concrete.Parameter visitParameter(Concrete.Parameter parameter, Concrete.Parameter wideParameter) {
                //noinspection DuplicatedCode
                if (parameter.getType() == null) {
                    return (Concrete.Parameter) myFactory.param(parameter.isExplicit(), wideParameter.getRefList().get(0));
                } else {
                    Concrete.Expression processedType = ((Concrete.TypeParameter) parameter).type.accept(this, ((Concrete.TypeParameter) wideParameter).type);
                    if (wideParameter.getRefList().stream().anyMatch(Objects::nonNull)) {
                        if (parameter instanceof Concrete.SigmaParameter) {
                            return myFactory.sigmaParam(((Concrete.SigmaParameter) parameter).getKind(), wideParameter.getRefList(), processedType);
                        } else {
                            return myFactory.param(parameter.isExplicit(), parameter.getRefList(), processedType);
                        }
                    } else {
                        if (parameter instanceof Concrete.SigmaParameter) {
                            return myFactory.sigmaParam(((Concrete.SigmaParameter) parameter).getKind(), List.of(), processedType);
                        } else {
                            return (Concrete.Parameter) myFactory.param(parameter.isExplicit(), processedType);
                        }
                    }
                }
            }
        }, verboseRepresentation);
        incompleteRepresentation = addTrailingImplicitArguments(verboseRepresentation, incompleteRepresentation);
        return new Pair<>(verboseRepresentation, incompleteRepresentation);
    }

    private static Map<String, Binding> collectFreeBindings(Expression expr) {
        var freeBindings = FreeVariablesCollector.getFreeVariables(expr);
        expr.accept(new VoidExpressionVisitor<Void>() {

            private Binding getBindingDeep(ProjExpression proj) {
                if (proj.getExpression() instanceof ReferenceExpression) {
                    return ((ReferenceExpression) proj.getExpression()).getBinding();
                } else if (proj.getExpression() instanceof ProjExpression) {
                    return getBindingDeep((ProjExpression) proj.getExpression());
                } else {
                    return null;
                }
            }

            @Override
            public Void visitProj(ProjExpression expr, Void params) {
                Binding deepBinding = getBindingDeep(expr);
                if (deepBinding != null && freeBindings.contains(deepBinding)) {
                    expr.getType().accept(this, null);
                    freeBindings.add(new TypedBinding(expr.toString(), expr.getType()));
                }
                return super.visitProj(expr, params);
            }
        }, null);
        Map<String, Binding> bindings = new HashMap<>();
        for (Binding binding : freeBindings) {
            bindings.putIfAbsent(binding.getName(), binding);
        }
        return bindings;
    }

    private static CheckTypeVisitor generateTypechecker(InstanceProvider instanceProvider, List<GeneralError> errorsCollector) {
        var checkTypeVisitor = new CheckTypeVisitor(error -> {
            if (!(error instanceof GoalError)) {
                errorsCollector.add(error);
            }
        }, null, null);
        checkTypeVisitor.setInstancePool(new GlobalInstancePool(instanceProvider, checkTypeVisitor, new LocalInstancePool(checkTypeVisitor)));
        return checkTypeVisitor;
    }

    private static Concrete.Expression tryFixError(CheckTypeVisitor checkTypeVisitor, Concrete.Expression completeConcrete, Concrete.Expression minimizedConcrete, List<GeneralError> errorsCollector) {
        var factory = new ConcreteFactoryImpl(null);
        checkTypeVisitor.finalCheckExpr(minimizedConcrete, null);
        if (!errorsCollector.isEmpty()) {
            return minimizedConcrete.accept(new ErrorFixingConcreteExpressionVisitor(errorsCollector, factory), completeConcrete);
        } else {
            return null;
        }
    }
}

/**
 * Simultaneously traverses both incomplete and complete concrete expressions, attempting to fix errors encountered during the traverse.
 *
 * This visitor is meant to fix only one error. I consider errors not to be independent,
 * therefore, after fixing one error, the rest may become irrelevant for the newly built expression.
 */
class ErrorFixingConcreteExpressionVisitor extends BiConcreteVisitor {

    private final List<GeneralError> myErrors;
    private final ConcreteFactory myFactory;

    public ErrorFixingConcreteExpressionVisitor(List<GeneralError> myErrors, ConcreteFactory myFactory) {
        this.myErrors = myErrors;
        this.myFactory = myFactory;
    }

    private List<GeneralError> getErrorsForNode(Concrete.SourceNode node) {
        return myErrors.stream().filter(err -> err.getCauseSourceNode() == node).collect(Collectors.toList());
    }

    @Override
    public Concrete.Expression visitLam(Concrete.LamExpression expr, Concrete.SourceNode verbose) {
        var errorList = getErrorsForNode(expr);
        if (!errorList.isEmpty()) {
            var error = errorList.get(0);
            myErrors.clear();
            return fixError(expr, (Concrete.LamExpression) verbose, error);
        }
        return super.visitLam(expr, verbose);
    }

    @Override
    public Concrete.Expression visitApp(Concrete.AppExpression expr, Concrete.SourceNode verbose) {
        Concrete.AppExpression verboseExpr = (Concrete.AppExpression) verbose;
        if (expr.getFunction() instanceof Concrete.ReferenceExpression) {
            var errorList = getErrorsForNode(expr.getFunction());
            if (!errorList.isEmpty()) {
                GeneralError mostImportantError = findMostImportantError(errorList);
                myErrors.clear(); // no errors should be fixed afterwards
                return fixError(expr, verboseExpr, mostImportantError);
            }
        }

        return super.visitApp(expr, verboseExpr);
    }

    @Override
    public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Concrete.SourceNode params) {
        var errorList = myErrors.stream().filter(err -> err.getCauseSourceNode() == expr).collect(Collectors.toList());
        if (!errorList.isEmpty()) {
            var verboseExpr = (Concrete.Expression) params;
            myErrors.clear();
            return verboseExpr;
        }
        return expr;
    }

    private static GeneralError findMostImportantError(List<GeneralError> errors) {
        return errors
                .stream()
                .filter(err -> err instanceof InstanceInferenceError)
                .findFirst()
                .orElse(errors
                        .stream()
                        .filter(err -> err instanceof ArgInferenceError)
                        .findFirst()
                        .orElse(errors.get(0))
                );
    }

    private Concrete.AppExpression fixError(Concrete.AppExpression incomplete, Concrete.AppExpression complete, GeneralError error) {
        if (error instanceof InstanceInferenceError) {
            return fixInstanceInferenceError(incomplete, complete, (InstanceInferenceError) error);
        } else if (error instanceof FunctionArgInferenceError) {
            return fixFunctionArgInferenceError(incomplete, complete, (FunctionArgInferenceError) error);
        } else {
            return fixUnknownError(incomplete, complete);
        }
    }

    private Concrete.LamExpression fixError(Concrete.LamExpression incomplete, Concrete.LamExpression complete, GeneralError error) {
        if (error instanceof LambdaInferenceError) {
            return fixLambdaInferenceError(incomplete, complete, (LambdaInferenceError)error);
        } else {
            throw new AssertionError("No other errors should have case source node of this kind");
        }
    }

    private Concrete.LamExpression fixLambdaInferenceError(Concrete.LamExpression incomplete, Concrete.LamExpression complete, LambdaInferenceError error) {
        var newParams = new ArrayList<Concrete.Parameter>();
        for (int i = 0; i < incomplete.getParameters().size(); ++i) {
            var incompleteParam = incomplete.getParameters().get(i);
            if (incompleteParam.getRefList().size() != 1) {
                newParams.add(incompleteParam);
            } else if (incompleteParam.getRefList().get(0).equals(error.parameter)) {
                newParams.add(complete.getParameters().get(i));
                incomplete.body = incomplete.body.accept(new SubstConcreteExpressionVisitor(Map.of(incompleteParam.getRefList().get(0), new Concrete.ReferenceExpression(null, complete.getParameters().get(i).getRefList().get(0))), null), null);
            }
        }
        return (Concrete.LamExpression) myFactory.lam(newParams, incomplete.body);
    }

    private Concrete.AppExpression fixUnknownError(Concrete.AppExpression incomplete, Concrete.AppExpression complete) {
        ArrayList<Concrete.Argument> args = new ArrayList<>();
        var incompleteArgumentsIterator = incomplete.getArguments().iterator();
        var completeArgumentsIterator = complete.getArguments().iterator();
        Concrete.Argument currentActualArg = incompleteArgumentsIterator.next();
        while (completeArgumentsIterator.hasNext()) {
            var currentProperArg = completeArgumentsIterator.next();
            if (currentActualArg == null || (currentProperArg.isExplicit() == currentActualArg.isExplicit())) {
                args.add(currentActualArg);
                currentActualArg = incompleteArgumentsIterator.hasNext() ? incompleteArgumentsIterator.next() : null;
            } else {
                args.add(currentProperArg);
            }
        }
        return (Concrete.AppExpression) myFactory.app(incomplete.getFunction(), args);
    }

    private Concrete.AppExpression fixFunctionArgInferenceError(Concrete.AppExpression incomplete, Concrete.AppExpression complete, FunctionArgInferenceError targetError) {
        var definition = targetError.definition;
        var args = new ArrayList<Concrete.Argument>();
        var fullIterator = new ArgumentMappingIterator(definition, complete);
        var incompleteIterator = new ArgumentMappingIterator(definition, incomplete);
        var inserted = false;
        var startIndex = definition instanceof ClassField ? 0 : 1;
        while (fullIterator.hasNext()) {
            var fullArg = fullIterator.next().proj2;
            var incompleteArg = incompleteIterator.next().proj2;
            if (startIndex == targetError.index) {
                args.add((Concrete.Argument) fullArg);
                inserted = true;
            } else if (incompleteArg != null) {
                args.add((Concrete.Argument) incompleteArg);
            } else if (!inserted) {
                args.add((Concrete.Argument) myFactory.arg(myFactory.hole(), fullArg.isExplicit()));
            }
            startIndex += 1;
        }
        return (Concrete.AppExpression) myFactory.app(incomplete.getFunction(), args);
    }

    private Concrete.AppExpression fixInstanceInferenceError(Concrete.AppExpression incomplete, Concrete.AppExpression complete, InstanceInferenceError targetError) {
        var function = (Concrete.ReferenceExpression) incomplete.getFunction();
        var definition = ((TCDefReferable) function.getReferent()).getTypechecked();
        var args = new ArrayList<Concrete.Argument>();
        var fullIterator = new ArgumentMappingIterator(definition, complete);
        var incompleteIterator = new ArgumentMappingIterator(definition, incomplete);
        var inserted = false;
        while (fullIterator.hasNext()) {
            var fullResult = fullIterator.next();
            var incompleteArg = incompleteIterator.next().proj2;
            var param = fullResult.proj1;
            if (param instanceof DependentLink && ((DependentLink) param).getType() instanceof DefCallExpression && ((DefCallExpression) ((DependentLink) param).getType()).getDefinition() == targetError.classRef.getTypechecked()) {
                args.add((Concrete.Argument) fullResult.proj2);
                inserted = true;
            } else if (incompleteArg != null) {
                args.add((Concrete.Argument) incompleteArg);
            } else if (!inserted) {
                args.add((Concrete.Argument) myFactory.arg(myFactory.hole(), Objects.requireNonNull(fullResult.proj2).isExplicit()));
            }
        }
        return (Concrete.AppExpression) myFactory.app(function, args);
    }
}
