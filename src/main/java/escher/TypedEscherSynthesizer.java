package escher;

import java.util.*;
import java.util.function.Function;

public class TypedEscherSynthesizer {
    /**
     *
     * @param deleteAllErr whether to delete synthesized components whose value vector consists only of Err
     * @param searchSizeFactor searchSizeFactor = maxProgramCost / level
     */
    public static class Config {
        public int maxCost = Integer.MAX_VALUE;
        public boolean deleteAllErr = true;
        public boolean logGoal = true;
        public boolean logLevels = true;
        public boolean logComponents = true;
        public boolean logTotalMap = true;
        public boolean logReboot = true;
        public RebootStrategy rebootStrategy = RebootStrategy.addSimplestFailedExample;
        public Function<ArgList, ArgList, Boolean> argListCompare = ArgList::anyArgSmaller;
        public int searchSizeFactor = 3;
    }

    public static class SynthesisData {
        public IS<Pair<ArgList, TermValue>> oracleBuffer;
        public int reboots;
        public TimeTools.Nanosecond lastRebootTimeUsed;

        public SynthesisData(IS<Pair<ArgList, TermValue>> oracleBuffer, int reboots, TimeTools.Nanosecond lastRebootTimeUsed) {
            this.oracleBuffer = oracleBuffer;
            this.reboots = reboots;
            this.lastRebootTimeUsed = lastRebootTimeUsed;
        }

        public static SynthesisData init() {
            return new SynthesisData(new IS<>(), 0, 0);
        }
    }

    public static void printResult(TypedEscherSynthesizer syn, int maxExamplesShown, Optional<Triple<SynthesizedComponent, SynthesisState, SynthesisData>> result) {
        result.ifPresent(triple -> {
            SynthesizedComponent program = triple.getFirst();
            SynthesisState state = triple.getSecond();
            SynthesisData synData = triple.getThird();
            List<Pair<ArgList, TermValue>> examples = state.examples;
            System.out.println("------ Synthesis for " + program.signature.name + " Succeeded! (" + synData.reboots + " reboots) ------");
            System.out.println("Time used for the last reboot: " + TimeTools.nanoToMillisString(synData.lastRebootTimeUsed));
            showExamples("Initial Examples", examples, 50);
            showExamples("Additional examples provided", synData.oracleBuffer, maxExamplesShown);
            state.print(examples.size());
            System.out.println("\nProgram found:\n");
            program.print();
            System.out.println();
        });
        if (!result.isPresent()) {
            System.out.println("------- Synthesis Failed. -------");
        }
    }

    private Config config;
    private Function<String, Void> logger;

    public TypedEscherSynthesizer(Config config, Function<String, Void> logger) {
        this.config = config;
        this.logger = logger;
    }

    private void log(boolean condition, String msg) {
        if (condition) {
            logger.apply(msg);
        }
    }

    private void logLn(boolean condition, String msg) {
        if (condition) {
            logger.apply(msg);
            logger.apply("\n");
        }
    }

    private static class TypeMap {
        private Map<Type, ValueTermMap> map;
        private int examples;

        public TypeMap(int examples) {
            this.map = new HashMap<>();
            this.examples = examples;
        }

        public ValueTermMap apply(Type ty) {
            ValueTermMap v = map.getOrDefault(ty, ValueTermMap.empty());
            map.put(ty, v);
            return v;
        }

        public Optional<ValueTermMap> get(Type ty) {
            return Optional.ofNullable(map.get(ty));
        }

        public void registerTerm(Term term, Type ty, ValueVector valueVector) {
            apply(ty).put(valueVector, term);
        }

        public void print(int indentation) {
            String whiteSpace = " ".repeat(indentation);
            map.forEach((k, v) -> {
                logger.apply(whiteSpace);
                logger.apply("(" + map.get(k).size() + ") " + k + "  -> " + v);
                logger.apply("\n");
            });
        }

        public Set<Type> typeSet() {
            return map.keySet();
        }

        public String statString() {
            int totalComponents = map.values().stream().mapToInt(ValueTermMap::size).sum();
            int totalTypes = map.keySet().size();
            return totalComponents + " components, " + totalTypes + " types";
        }
    }

    private static class SynthesisState {
        private IS<Pair<ArgList, TermValue>> examples;
        private TypeMap totalMap;
        private Type returnType;
        private IS<TypeMap> levelMaps;
        private IS<ValueVectorTree<Term>> returnTypeVectorTrees;
        private IS<ValueVectorTree<Term>> boolVectorTrees;

        public SynthesisState(IS<Pair<ArgList, TermValue>> examples, TypeMap totalMap, Type returnType) {
            this.examples = examples;
            this.totalMap = totalMap;
            this.returnType = returnType;
            this.levelMaps = new IS<>();
            this.returnTypeVectorTrees = new IS<>();
            this.boolVectorTrees = new IS<>();
        }

        public int levels() {
            return levelMaps.size();
        }

        public TypeMap getLevelOfCost(int cost) {
            return levelMaps.get(cost - 1);
        }

        public int openNextLevel() {
            levelMaps = levelMaps.append(TypeMap.empty(examples.size()));
            return levelMaps.size();
        }

        public void createLibrariesForThisLevel() {
            int levels = levelMaps.size();
            TypeMap typeMap = getLevelOfCost(levels);
            ValueVectorTree<Term> returnTypeTree = new ValueVectorTree<>(examples.size());
            for (Type ty : typesMatch(typeMap, returnType)) {
                ValueTermMap vt = typeMap.apply(ty);
                vt.forEach((vv, term) -> returnTypeTree.addTerm(term, vv));
            }
            returnTypeVectorTrees = returnTypeVectorTrees.append(returnTypeTree);
            ValueVectorTree<Term> boolTree = new ValueVectorTree<>(examples.size());
            for (Type ty : typesMatch(typeMap, tyBool)) {
                ValueTermMap vt = typeMap.apply(ty);
                vt.forEach((vv, term) -> boolTree.addTerm(term, vv));
            }
            boolVectorTrees = boolVectorTrees.append(boolTree);
        }

        private List<Type> typesMatch(TypeMap typeMap, Type ty) {
            List<Type> types = new ArrayList<>();
            for (Type t : typeMap.typeSet()) {
                if (ty.instanceOf(t)) {
                    types.add(Type.alphaNormalForm(t));
                }
            }
            return types;
        }

        public Optional<Pair<Integer, Term>> boolLibrary(IndexValueMap vm) {
            for (int cost = 1; cost <= levels(); cost++) {
                ValueVectorTree<Term> tree = boolVectorTrees.get(cost - 1);
                Optional<Term> term = tree.searchATerm(vm);
                if (term.isPresent()) {
                    return Optional.of(new Pair<>(cost, term.get()));
                }
            }
            return Optional.empty();
        }

        public Optional<Pair<Integer, Term>> returnTypeLibrary(IndexValueMap vm) {
            for (int cost = 1; cost <= levels(); cost++) {
                ValueVectorTree<Term> tree = returnTypeVectorTrees.get(cost - 1);
                Optional<Term> term = tree.searchATerm(vm);
                if (term.isPresent()) {
                    return Optional.of(new Pair<>(cost, term.get()));
                }
            }
            return Optional.empty();
        }

        public Optional<Term> libraryOfCost(int cost, IndexValueMap vm) {
            ValueVectorTree<Term> tree = returnTypeVectorTrees.get(cost - 1);
            return tree.searchATerm(vm);
        }

        public Iterable<Pair<ValueVector, Term>> termsOfCost(int cost) {
            return returnTypeVectorTrees.get(cost - 1).elements();
        }

        public Iterable<Pair<ValueVector, Term>> boolTermsOfCost(int cost) {
            return boolVectorTrees.get(cost - 1).elements();
        }

        public void openToLevel(int n) {
            for (int i = 0; i < n - levelMaps.size(); i++) {
                openNextLevel();
            }
        }

        public boolean registerTermAtLevel(int cost, Type ty, Term term, ValueVector valueVector) {
            Type ty1 = Type.alphaNormalForm(ty);
            if (totalMap.get(ty1).isPresent()) {
                return false;
            }
            totalMap.apply(ty1).put(valueVector, term);
            getLevelOfCost(cost).apply(ty1).put(valueVector, term);
            return true;
        }

        public void print(int exampleCount) {
            logLn(config.logTotalMap, "TotalMap: (" + totalMap.statString() + ")");
            if (config.logTotalMap && config.logComponents) {
                totalMap.print(4);
            }
            logLn(config.logLevels, "LevelMaps:");
            if (config.logLevels) {
                for (int i = 0; i < levelMaps.size(); i++) {
                    int c = i + 1;
                    TypeMap typeMap = getLevelOfCost(c);
                    logLn(true, "  " + c + ": (" + typeMap.statString() + ")");
                    if (config.logComponents) {
                        typeMap.print(6);
                    }
                }
            }
        }
    }

    public Optional<Triple<SynthesizedComponent, SynthesisState, SynthesisData>> synthesize(String name, IndexedSeq<Type> inputTypesFree, IndexedSeq<String> inputNames, Type returnTypeFree, Set<ComponentImpl> envComps, IS<Pair<ArgList, TermValue>> examples0, PartialFunction<IS<TermValue>, TermValue> oracle, SynthesisData synData) {
        List<Pair<ArgList, TermValue>> examples = examples0.sortWith(Synthesis.exampleLt);
        IS<ArgList> inputs = examples.map(Pair::getFirst);
        IS<TermValue> outputs = examples.map(Pair::getSecond);
        IndexedSeq<Type> inputTypes = inputTypesFree.map(Type::fixVars);
        Type goalReturnType = returnTypeFree.fixVars();
        ComponentSignature signature = new ComponentSignature(name, inputNames, inputTypes, goalReturnType);
        if (inputTypes.size() != inputNames.size()) {
            throw new IllegalArgumentException("inputTypes and inputNames must have the same size");
        }
        BufferedOracle bufferedOracle = new BufferedOracle(inputs.zip(outputs), oracle, synData.oracleBuffer);
        ComponentImpl recursiveComp = new ComponentImpl(name, inputTypes, goalReturnType, bufferedOracle::evaluate);
        Map<String, ComponentImpl> compMap = new HashMap<>();
        for (ComponentImpl impl : envComps) {
            compMap.put(impl.name, impl);
        }
        compMap.put(name, recursiveComp);
        Function<ComponentImpl, Integer> compCostFunction = impl -> 1;
        Function<IS<ArgList>, IS<ArgList>, Boolean> argDecrease = (arg, exampleId) -> config.argListCompare.apply(arg, inputs.get(exampleId));
        int exampleCount = outputs.size();
        SynthesisState state = new SynthesisState(examples, new TypeMap(exampleCount), goalReturnType);
        state.openNextLevel();
        for (int argId = 0; argId < inputTypes.size(); argId++) {
            ArgList valueMap = new ArgList();
            for (int exId = 0; exId < inputs.size(); exId++) {
                valueMap.add(inputs.get(exId).get(argId));
            }
            state.registerTermAtLevel(1, inputTypes.get(argId), new Term.Variable(inputNames.get(argId)), valueMap);
        }
        Map<Integer, Function<IndexValueMap, Optional<Term>>> termOfCostAndVM = new HashMap<>();
        for (int cost = 1; cost <= config.maxCost; cost++) {
            int finalCost = cost;
            termOfCostAndVM.put(cost, vm -> state.libraryOfCost(finalCost, vm));
        }
        Map<Integer, Function<Integer, Iterable<Pair<ValueVector, Term>>>> termsOfCost = new HashMap<>();
        for (int cost = 1; cost <= config.maxCost; cost++) {
            int finalCost = cost;
            termsOfCost.put(cost, state::termsOfCost);
        }
        Map<Integer, Function<Integer, Iterable<Pair<ValueVector, Term>>>> boolTermsOfCost = new HashMap<>();
        for (int cost = 1; cost <= config.maxCost; cost++) {
            int finalCost = cost;
            boolTermsOfCost.put(cost, state::boolTermsOfCost);
        }
        Map<IndexValueMap, Optional<Pair<Integer, Term>>> boolOfVM = new HashMap<>();
        for (int cost = 1; cost <= config.maxCost; cost++) {
            int finalCost = cost;
            boolOfVM.put(cost, state::boolLibrary);
        }
        for (int level = 1; level <= config.maxCost; level++) {
            synthesizeAtLevel(level, true, state, compMap, compCostFunction, argDecrease);
            searchThenFirst(config.searchSizeFactor * level, state, termOfCostAndVM, termsOfCost, boolTermsOfCost, boolOfVM, outputs, config.logLevels).ifPresent(pair -> {
                SynthesizedComponent comp = new SynthesizedComponent(signature, pair.getSecond(), pair.getFirst(), level);
                ComponentImpl impl = ComponentImpl.recursiveImpl(name, inputNames, inputTypes, goalReturnType, envComps, config.argListCompare, pair.getSecond());
                IS<Pair<ArgList, TermValue>> passed = new IS<>();
                IS<Pair<ArgList, TermValue>> failed = new IS<>();
                bufferedOracle.buffer.forEach(pair2 -> {
                    ArgList a = pair2.getFirst();
                    TermValue r = pair2.getSecond();
                    if (impl.executeEfficient(a).equals(r)) {
                        passed = passed.append(pair2);
                    } else {
                        failed = failed.append(pair2);
                    }
                });
                if (failed.isEmpty()) {
                    long timeUse = System.nanoTime() - startTime;
                    return Optional.of(new Triple<>(comp, state, new SynthesisData(passed, synData.reboots + 1, timeUse)));
                } else {
                    if (config.logReboot) {
                        System.out.println("Failed program found:");
                        comp.print();
                    }
                    if (config.logReboot) {
                        System.out.println("--- Reboot ---");
                    }
                    logLn(config.logReboot, "  which failed at " + showExamples(failed) + "\nNow Reboot...");
                    Pair<IS<Pair<ArgList, TermValue>>, IS<Pair<ArgList, TermValue>>> newExamplesAndOracleBuffer = config.rebootStrategy.newExamplesAndOracleBuffer(examples, failed, passed);
                    IS<Pair<ArgList, TermValue>> newExamples = newExamplesAndOracleBuffer.getFirst();
                    IS<Pair<ArgList, TermValue>> newBuffer = newExamplesAndOracleBuffer.getSecond();
                    System.out.println("New examples: " + showExamples(newExamples));
                    SynthesisData newSynData = new SynthesisData(newBuffer, synData.reboots + 1, 0);
                    return synthesize(name, inputTypes, inputNames, goalReturnType, envComps, newExamples, oracle, newSynData);
                }
            });
            synthesizeAtLevel(level, false, state, compMap, compCostFunction, argDecrease);
            if (config.logLevels) {
                System.out.println("State at level: " + level + "\n");
                state.print(exampleCount);
            }
            state.openNextLevel();
        }
        return Optional.empty();
    }

    private void synthesizeAtLevel(int cost, boolean synBoolAndReturnType, SynthesisState state, Map<String, ComponentImpl> compMap, Function<ComponentImpl, Integer> compCostFunction, Function<ArgList, ArgList, Boolean> argDecrease) {
        for (Map.Entry<String, ComponentImpl> entry : compMap.entrySet()) {
            String compName = entry.getKey();
            ComponentImpl impl = entry.getValue();
            int compCost = compCostFunction.apply(impl);
            if (compCost <= cost) {
                int arity = impl.inputTypes.size();
                int costLeft = cost - compCost;
                if (arity == 0) {
                    if (compCost == cost) {
                        Term term = new Term.Variable(compName);
                        ValueVector valueVector = new ValueVector();
                        for (int i = 0; i < state.examples.size(); i++) {
                            valueVector.add(impl.executeEfficient(new ArgList()));
                        }
                        state.registerTermAtLevel(cost, impl.returnType, term, valueVector);
                    }
                } else {
                    List<List<Type>> argTypesList = divideNumberAsSum(costLeft, arity, 1);
                    for (List<Type> argTypes : argTypesList) {
                        Type returnType = impl.returnType;
                        if ((synBoolAndReturnType && (returnType.instanceOf(state.returnType) || tyBool.instanceOf(returnType))) && isInterestingSignature(argTypes, returnType)) {
                            List<List<Pair<ValueTermMap, Term>>> candidatesForArgs = new ArrayList<>();
                            for (int argIdx = 0; argIdx < arity; argIdx++) {
                                int c = costLeft - argTypesList.indexOf(argTypes) + 1;
                                candidatesForArgs.add(new ArrayList<>(state.getLevelOfCost(c).apply(argTypes.get(argIdx)).entrySet()));
                            }
                            boolean isRecCall = compName.equals(name);
                            cartesianProduct(candidatesForArgs).forEach(product -> {
                                ValueVector valueVector = new ValueVector();
                                for (int exId = 0; exId < state.examples.size(); exId++) {
                                    List<TermValue> arguments = new ArrayList<>();
                                    for (Pair<ValueTermMap, Term> pair : product) {
                                        arguments.add(pair.getFirst().get(exId));
                                    }
                                    if (isRecCall && !argDecrease.apply(new ArgList(arguments), exId)) {
                                        valueVector.add(ValueError);
                                    } else {
                                        valueVector.add(impl.executeEfficient(new ArgList(arguments)));
                                    }
                                }
                                if (!config.deleteAllErr || !notAllErr(valueVector)) {
                                    Term term = new Term.Variable(compName);
                                    for (Pair<ValueTermMap, Term> pair : product) {
                                        term = new Term.Application(term, pair.getSecond());
                                    }
                                    state.registerTermAtLevel(cost, returnType, term, valueVector);
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}


