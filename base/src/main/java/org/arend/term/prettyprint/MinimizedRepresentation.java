package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ProjExpression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.variable.Variable;
import org.arend.extImpl.ConcreteFactoryImpl;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.FunctionArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            @Nullable DefinitionRenamer definitionRenamer) {
        Converter converter = new Converter(definitionRenamer);
        var verboseRepresentation = converter.coreToConcrete(expressionToPrint, true);
        var incompleteRepresentation = converter.coreToConcrete(expressionToPrint, false);
        processBindingTypes(converter);
        List<GeneralError> errorsCollector = new ArrayList<>();

        var groundExpr = generateGroundConcrete(expressionToPrint, converter, incompleteRepresentation, verboseRepresentation);
        var typechecker = generateTypechecker(instanceProvider, errorsCollector);

        int limit = 50;
        while (true) {
            var hadError = tryFixError(typechecker, groundExpr, verboseRepresentation, incompleteRepresentation, errorsCollector);
            if (!hadError) {
                return incompleteRepresentation;
            }
            --limit;
            if (limit == 0) {
                throw new AssertionError("Minimization of expression (" + expressionToPrint + ") is likely diverged. Please report it to maintainers.\n " +
                        "Errors: \n" + errorsCollector);
            }
        }
    }

    private static void processBindingTypes(Converter converter) {
        for (Variable binding : converter.freeVariableBindings.keySet().toArray(new Variable[0])) {
            if (binding instanceof Binding && ((Binding) binding).getTypeExpr() != null) {
                converter.coreToConcrete(((Binding) binding).getTypeExpr(), true);
            }
        }
    }

    private static Concrete.Expression generateGroundConcrete(Expression expressionToPrint, Converter converter, Concrete.Expression incompleteRepresentation, Concrete.Expression completeExpression) {
        var concreteFactory = new ConcreteFactoryImpl(null);
        Map<ArendRef, Expression> refToType = new LinkedHashMap<>();
        Map<String, Expression> nameToType = new LinkedHashMap<>();
        ExpressionVisitor<Void, Void> resolveVariablesVisitor = new VoidExpressionVisitor<>() {
            @Override
            public Void visitReference(ReferenceExpression expr, Void params) {
                if (converter.freeVariableBindings.containsKey(expr.getBinding())) {
                    expr.getType().accept(this, null);
                    refToType.putIfAbsent(converter.freeVariableBindings.get(expr.getBinding()), expr.getBinding().getTypeExpr());
                }
                return null;
            }

            @Override
            public Void visitProj(ProjExpression expr, Void params) {
                if (expr.getExpression() instanceof ReferenceExpression &&
                        converter.freeVariableBindings.containsKey(((ReferenceExpression) expr.getExpression()).getBinding())) {
                    expr.getType().accept(this, null);
                    nameToType.putIfAbsent(expr.toString(), expr.getType());
                }
                return super.visitProj(expr, params);
            }
        };
        expressionToPrint.accept(resolveVariablesVisitor, null);

        Map<Referable, Expression> namedRefToExpression;
        if (!nameToType.isEmpty()) {
            namedRefToExpression = new LinkedHashMap<>();
            Set<Referable> referables = new LinkedHashSet<>();
            var freeReferablesVisitor = new FreeVariableCollectorConcrete(referables);
            incompleteRepresentation.accept(freeReferablesVisitor, null);
            completeExpression.accept(freeReferablesVisitor, null);
            for (var entry : referables) {
                if (nameToType.containsKey(entry.getRefName())) {
                    namedRefToExpression.put(entry, nameToType.get(entry.getRefName()));
                }
            }
        } else {
            namedRefToExpression = new LinkedHashMap<>();
        }

        Stream<Map.Entry<? extends ArendRef, Expression>> entries = Stream.concat(refToType.entrySet().stream(), namedRefToExpression.entrySet().stream());

        List<Concrete.LetClause> clauses = entries
                .map(entry -> (Concrete.LetClause) concreteFactory.letClause(
                        entry.getKey(),
                        Collections.emptyList(),
                        converter.coreToConcrete(entry.getValue(), true),
                        concreteFactory.goal())
                )
                .collect(Collectors.toList());

        return (Concrete.LetExpression) concreteFactory.letExpr(false, false, clauses, incompleteRepresentation);
    }

    // TODO: remove dependency on internals of ToAbstractVisitor.
    // to do this, one should replace all reference expressions from incomplete concrete with corresponding identifiers from complete concrete.
    // then, re-collect free variables in `expressionToPrint` as well as in `completeExpression` and finally generate all let clauses with proper type.
    // it requires to abstract visitor on two concrete expressions
    private static final class Converter {
        private final PrettyPrinterConfig verboseConfig;
        private final PrettyPrinterConfig emptyConfig;
        private final Map<Variable, LocalReferable> freeVariableBindings;

        public Converter(@Nullable DefinitionRenamer definitionRenamer) {
            verboseConfig = new PrettyPrinterConfig() {
                @Override
                public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                    return EnumSet.of(PrettyPrinterFlag.SHOW_TYPES_IN_LAM, PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE, PrettyPrinterFlag.SHOW_CON_PARAMS, PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS, PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS, PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_IMPLICIT_ARGS, PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_TUPLE_TYPE);
                }

                @Override
                public @Nullable DefinitionRenamer getDefinitionRenamer() {
                    return definitionRenamer;
                }
            };
            emptyConfig = new PrettyPrinterConfig() {
                @Override
                public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                    return EnumSet.noneOf(PrettyPrinterFlag.class);
                }

                @Override
                public @Nullable DefinitionRenamer getDefinitionRenamer() {
                    return definitionRenamer;
                }
            };
            freeVariableBindings = new LinkedHashMap<>();
        }

        Concrete.Expression coreToConcrete(Expression core, boolean verbose) {
            var config = verbose ? verboseConfig : emptyConfig;
            return ToAbstractVisitor.convert(core, config, freeVariableBindings);
        }
    }


    private static CheckTypeVisitor generateTypechecker(InstanceProvider instanceProvider, List<GeneralError> errorsCollector) {
        var checkTypeVisitor = new CheckTypeVisitor(error -> {
            if (!(error instanceof GoalError)) errorsCollector.add(error);
        }, null, null);
        checkTypeVisitor.setInstancePool(new GlobalInstancePool(instanceProvider, checkTypeVisitor, new LocalInstancePool(checkTypeVisitor)));
        return checkTypeVisitor;
    }

    private static boolean tryFixError(CheckTypeVisitor checkTypeVisitor, Concrete.Expression groundConcrete, Concrete.Expression completeConcrete, Concrete.Expression minimizedConcrete, List<GeneralError> errorsCollector) {
        checkTypeVisitor.finalCheckExpr(groundConcrete, null);
        if (!errorsCollector.isEmpty()) {
            minimizedConcrete.accept(new ErrorFixingConcreteExpressionVisitor(errorsCollector, new ConcreteFactoryImpl(null)), completeConcrete);
            return true;
        } else {
            return false;
        }
    }
}

/**
 * Simultaneously traverses both incomplete and complete concrete expressions, attempting to fix errors encountered during the traverse.
 *
 * This visitor is meant to fix only one error. I consider errors not to be independent,
 * therefore, after fixing one error, the rest may become irrelevant for the newly built expression.
 */
class ErrorFixingConcreteExpressionVisitor extends BaseConcreteExpressionVisitor<Concrete.SourceNode> {

    private final List<GeneralError> myErrors;
    private final ConcreteFactory myFactory;

    public ErrorFixingConcreteExpressionVisitor(List<GeneralError> myErrors, ConcreteFactory myFactory) {
        this.myErrors = myErrors;
        this.myFactory = myFactory;
    }

    private void visitParameters(List<? extends Concrete.Parameter> incompleteParameters,
                                 List<? extends Concrete.Parameter> completeParameters) {
        for (int i = 0; i < incompleteParameters.size(); ++i) {
            var param = incompleteParameters.get(i);
            if (param instanceof Concrete.TypeParameter) {
                ((Concrete.TypeParameter) param).type.accept(this, ((Concrete.TypeParameter) completeParameters.get(i)).type);
            }
        }
    }

    @Override
    public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Concrete.SourceNode verbose) {
        var verboseExpr = (Concrete.SigmaExpression) verbose;
        visitParameters(expr.getParameters(), verboseExpr.getParameters());
        return expr;
    }

    @Override
    public Concrete.Expression visitPi(Concrete.PiExpression expr, Concrete.SourceNode verbose) {
        var verboseExpr = (Concrete.PiExpression) verbose;
        visitParameters(expr.getParameters(), verboseExpr.getParameters());
        expr.codomain.accept(this, verboseExpr.codomain);
        return expr;
    }

    @Override
    public Concrete.Expression visitLam(Concrete.LamExpression expr, Concrete.SourceNode verbose) {
        var verboseExpr = (Concrete.LamExpression) verbose;
        visitParameters(expr.getParameters(), verboseExpr.getParameters());
        expr.body.accept(this, verboseExpr.body);
        return expr;
    }

    @Override
    public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Concrete.SourceNode params) {
        if (expr.expression != null) {
            var verboseExpr = (Concrete.GoalExpression) params;
            expr.expression.accept(this, verboseExpr.expression);
        }
        return super.visitGoal(expr, params);
    }

    @Override
    public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Concrete.SourceNode verbose) {
        var verboseExpr = (Concrete.TupleExpression) verbose;
        for (int i = 0; i < expr.getFields().size(); i++) {
            expr.getFields().set(i, expr.getFields().get(i).accept(this, verboseExpr.getFields().get(i)));
        }
        return expr;
    }

    @Override
    public Concrete.Expression visitApp(Concrete.AppExpression expr, Concrete.SourceNode verbose) {
        Concrete.AppExpression verboseExpr = (Concrete.AppExpression) verbose;
        if (expr.getFunction() instanceof Concrete.ReferenceExpression) {
            var errorList = myErrors.stream().filter(err -> err.getCauseSourceNode() == expr.getFunction()).collect(Collectors.toList());
            if (!errorList.isEmpty()) {
                GeneralError mostImportantError = findMostImportantError(errorList);
                myErrors.clear(); // no errors should be fixed afterwards
                var fixed = fixError(expr, verboseExpr, mostImportantError);
                expr.getArguments().clear();
                expr.setFunction(fixed);
                return expr;
            }
        }
        var allArguments = verboseExpr.getArguments().iterator();
        for (var argument : expr.getArguments()) {
            var currentArgument = allArguments.next();
            while (currentArgument.isExplicit() != argument.isExplicit()) {
                currentArgument = allArguments.next();
            }
            argument.expression = argument.expression.accept(this, currentArgument.expression);
        }

        expr.setFunction(expr.getFunction().accept(this, verboseExpr.getFunction()));

        return expr;
    }

    @Override
    public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Concrete.SourceNode params) {
        var errorList = myErrors.stream().filter(err -> err.getCauseSourceNode() == expr).collect(Collectors.toList());
        if (!errorList.isEmpty()) {
            var verboseExpr = (Concrete.Expression) params;
            myErrors.clear();
            return verboseExpr;
        }
        return super.visitReference(expr, params);
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
