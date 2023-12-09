package escher;

import java.util.*;
import java.util.function.Function;

public class AscendRecSynthesizer {
    private Config config;
    private Function<String, Void> logger;

    public AscendRecSynthesizer(Config config, Function<String, Void> logger) {
        this.config = config;
        this.logger = logger;
    }

    public SynthesizedComponent synthesize(String name, List<Type> inputTypesFree, List<String> inputNames, Type returnTypeFree,
                                           Set<ComponentImpl> envComps, List<Pair<ArgList, TermValue>> examples0,
                                           Map<ComponentImpl, ReducibleCheck> compReductionRules) {
        List<Pair<ArgList, TermValue>> examples = new ArrayList<>(examples0);
        examples.sort((e1, e2) -> exampleLt(e1, e2));
        List<ArgList> inputs = new ArrayList<>();
        List<TermValue> outputs = new ArrayList<>();
        for (Pair<ArgList, TermValue> example : examples) {
            inputs.add(example.getKey());
            outputs.add(example.getValue());
        }
        List<Type> inputTypes = new ArrayList<>();
        for (Type type : inputTypesFree) {
            inputTypes.add(type.fixVars());
        }
        Type goalReturnType = returnTypeFree.fixVars();
        int exampleCount = outputs.size();
        SynthesisState state = new SynthesisState(examples, new TypeMap(), goalReturnType, compReductionRules);
        ComponentSignature signature = new ComponentSignature(name, inputNames, inputTypes, goalReturnType);
        if (inputTypes.size() != inputNames.size()) {
            throw new IllegalArgumentException("inputTypes and inputNames must have the same length");
        }
        Function<ArgList, Integer> argDecrease = (arg, exampleId) -> config.argListCompare(arg, inputs.get(exampleId));
        Map<ArgList, TermValue> knownMap = new HashMap<>(examples);
        ExtendedCompImpl recursiveComp = ExtendedCompImpl.fromImplOnTermValue(name, inputTypes, goalReturnType, argList -> knownMap.getOrDefault(argList, ValueUnknown));
        Set<ExtendedCompImpl> envExtendedCompSet = new HashSet<>();
        for (ComponentImpl impl : envComps) {
            envExtendedCompSet.add(ExtendedCompImpl.fromImplOnTermValue(impl.name, impl.inputTypes, impl.returnType, impl.executeEfficient));
        }
        Set<ExtendedCompImpl> compSet = new HashSet<>(envExtendedCompSet);
        compSet.add(recursiveComp);
        Function<ExtendedCompImpl, Integer> compCostFunction = impl -> 1;
        Function<Type, Boolean> isInterestingSignature = (argTypes, returnType) -> isInterestingSignature(goalReturnType, inputTypes);
        for (int cost = 1; cost <= config.maxLevel; cost++) {
            for (ExtendedCompImpl impl : compSet) {
                boolean isRecCall = impl.name.equals(name);
                int compCost = compCostFunction.apply(impl);
                if (compCost <= cost) {
                    int arity = impl.inputTypes.size();
                    int costLeft = cost - compCost;
                    if (arity == 0) {
                        if (compCost == cost) {
                            TermValue result = (TermValue) impl.execute(Collections.emptyList());
                            List<TermValue> valueVector = new ArrayList<>(Collections.nCopies(exampleCount, result));
                            Term term = new Term.Component(impl.name, Collections.emptyList());
                            state.registerNonRecAtLevel(cost, impl.returnType, term, valueVector);
                        }
                    } else {
                        List<List<Integer>> costsList = divideNumberAsSum(costLeft, arity, 1);
                        for (List<Integer> costs : costsList) {
                            List<Type> argTypes = new ArrayList<>();
                            Type returnType;
                            try {
                                returnType = typesForCosts(c -> state.getNonRecOfCost(c).typeSet + state.getRecOfCost(c).typeSet, costs, impl.inputTypes, impl.returnType);
                            } catch (IllegalArgumentException e) {
                                continue;
                            }
                            if ((synBoolAndReturnType && (goalReturnType.instanceOf(returnType) || tyBool.instanceOf(returnType))) &&
                                    isInterestingSignature.apply(argTypes, returnType)) {
                                List<List<Term>> nonRecCandidates = new ArrayList<>();
                                for (int argIdx = 0; argIdx < arity; argIdx++) {
                                    int c = costs.get(argIdx);
                                    nonRecCandidates.add(new ArrayList<>(state.getNonRecOfCost(c).get(argTypes.get(argIdx))));
                                }
                                if (isRecCall) {
                                    for (List<Term> product : cartesianProduct(nonRecCandidates)) {
                                        List<TermValue> valueVector = new ArrayList<>();
                                        for (int exId = 0; exId < exampleCount; exId++) {
                                            List<Term> arguments = new ArrayList<>();
                                            for (Pair<Term, Term> p : product) {
                                                arguments.add(p.getKey().get(exId));
                                            }
                                            if (!argDecrease.apply(arguments, exId)) {
                                                valueVector.add(ValueError);
                                            } else {
                                                valueVector.add((TermValue) impl.execute(arguments));
                                            }
                                        }
                                        if (!config.deleteAllErr || notAllErr(valueVector)) {
                                            Term term = new Term.Component(impl.name, product.stream().map(Pair::getValue).collect(Collectors.toList()));
                                            state.registerTermAtLevel(cost, returnType, term, valueVector);
                                        }
                                    }
                                } else {
                                    List<List<Term>> recCandidates = new ArrayList<>();
                                    for (int argIdx = 0; argIdx < arity; argIdx++) {
                                        int c = costs.get(argIdx);
                                        recCandidates.add(state.getRecOfCost(c).get(argTypes.get(argIdx)).stream().map(Pair::swap).collect(Collectors.toList()));
                                    }
                                    List<List<Term>> allCandidates = new ArrayList<>();
                                    for (int i = 0; i < nonRecCandidates.size(); i++) {
                                        allCandidates.add(new ArrayList<>(nonRecCandidates.get(i)));
                                        allCandidates.get(i).addAll(recCandidates.get(i));
                                    }
                                    for (List<Term> product : cartesianProduct(allCandidates)) {
                                        List<TermValue> valueVector = new ArrayList<>();
                                        for (int exId = 0; exId < exampleCount; exId++) {
                                            List<Term> arguments = new ArrayList<>();
                                            for (Pair<Term, Term> p : product) {
                                                arguments.add(p.getKey().get(exId));
                                            }
                                            valueVector.add((TermValue) impl.execute(arguments));
                                        }
                                        if (!config.deleteAllErr || notAllErr(valueVector)) {
                                            Term term = new Term.Component(impl.name, product.stream().map(Pair::getValue).collect(Collectors.toList()));
                                            state.registerTermAtLevel(cost, returnType, term, valueVector);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private int exampleLt(Pair<ArgList, TermValue> e1, Pair<ArgList, TermValue> e2) {
        // TODO: Implement exampleLt
        return 0;
    }

    private List<List<Integer>> divideNumberAsSum(int number, int parts, int minNumber) {
        List<List<Integer>> result = new ArrayList<>();
        divideNumberAsSumHelper(number, parts, minNumber, new ArrayList<>(), result);
        return result;
    }

    private void divideNumberAsSumHelper(int number, int parts, int minNumber, List<Integer> current, List<List<Integer>> result) {
        if (number == 0 && parts == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        if (number <= 0 || parts <= 0) {
            return;
        }
        for (int i = minNumber; i <= number; i++) {
            current.add(i);
            divideNumberAsSumHelper(number - i, parts - 1, i, current, result);
            current.remove(current.size() - 1);
        }
    }

    private Type typesForCosts(Function<Integer, Set<Type>> getTypeSet, List<Integer> costs, List<Type> inputTypes, Type returnType) {
        // TODO: Implement typesForCosts
        return null;
    }

    private boolean isInterestingSignature(Type returnType, List<Type> inputTypes) {
        // TODO: Implement isInterestingSignature
        return false;
    }

    private <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> result = new ArrayList<>();
        cartesianProductHelper(lists, 0, new ArrayList<>(), result);
        return result;
    }

    private <T> void cartesianProductHelper(List<List<T>> lists, int index, List<T> current, List<List<T>> result) {
        if (index == lists.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (T item : lists.get(index)) {
            current.add(item);
            cartesianProductHelper(lists, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private boolean notAllErr(List<TermValue> valueVector) {
        // TODO: Implement notAllErr
        return false;
    }

    public void log(boolean condition, String msg) {
        if (condition) {
            logger.apply(msg);
        }
    }

    public void logLn(boolean condition, String msg) {
        if (condition) {
            logger.apply(msg);
            logger.apply("\n");
        }
    }

    private class RecTypeMap {
        private Map<Type, RecMap> map;

        public RecTypeMap() {
            this.map = new HashMap<>();
        }

        public RecMap get(Type ty) {
            RecMap v = map.getOrDefault(ty, new HashMap<>());
            map.put(ty, v);
            return v;
        }

        public void put(Type ty, Term term, ExtendedValueVec valueVec) {
            map.put(ty, get(ty).put(term, valueVec));
        }

        public Optional<RecMap> get(Type ty) {
            return Optional.ofNullable(map.get(ty));
        }

        public void print(int indentation) {
            String whiteSpace = " ".repeat(indentation);
            map.forEach((k, v) -> {
                logger.apply(whiteSpace);
                logger.apply("(" + v.size() + ") " + k + " -> " + v);
                logger.apply("\n");
            });
        }

        public Set<Type> typeSet() {
            return map.keySet();
        }

        public String statString() {
            int componentCount = map.values().stream().mapToInt(Map::size).sum();
            int typeCount = map.keySet().size();
            return componentCount + " components, " + typeCount + " types";
        }
    }

    private class TypeMap {
        private Map<Type, ValueTermMap> map;

        public TypeMap() {
            this.map = new HashMap<>();
        }

        public ValueTermMap get(Type ty) {
            ValueTermMap v = map.getOrDefault(ty, new ValueTermMap());
            map.put(ty, v);
            return v;
        }

        public Optional<ValueTermMap> get(Type ty) {
            return Optional.ofNullable(map.get(ty));
        }

        public void print(int indentation) {
            String whiteSpace = " ".repeat(indentation);
            map.forEach((k, v) -> {
                logger.apply(whiteSpace);
                logger.apply("(" + v.size() + ") " + k + " -> " + v);
                logger.apply("\n");
            });
        }

        public Set<Type> typeSet() {
            return map.keySet();
        }

        public String statString() {
            int componentCount = map.values().stream().mapToInt(Map::size).sum();
            int typeCount = map.keySet().size();
            return componentCount + " components, " + typeCount + " types";
        }
    }

    private class SynthesisState {
        private List<Pair<ArgList, TermValue>> examples;
        private TypeMap totalNonRec;
        private Type returnType;
        private Map<ComponentImpl, ReducibleCheck> reductionRules;
        private Map<String, ReducibleCheck> reductionRulesMap;
        private List<TypeMap> levelNonRecs;
        private List<RecTypeMap> levelRecComps;
        private List<ValueVectorTree<Term>> returnTypeVectorTrees;
        private List<ValueVectorTree<Term>> boolVectorTrees;
        private List<List<Pair<Term, ExtendedValueVec>>> recTermsOfReturnType;

        public SynthesisState(List<Pair<ArgList, TermValue>> examples, TypeMap totalNonRec, Type returnType,
                              Map<ComponentImpl, ReducibleCheck> reductionRules) {
            this.examples = new ArrayList<>(examples);
            this.totalNonRec = totalNonRec;
            this.returnType = returnType;
            this.reductionRules = reductionRules;
            this.reductionRulesMap = reductionRules.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().name, Map.Entry::getValue));
            this.levelNonRecs = new ArrayList<>();
            this.levelRecComps = new ArrayList<>();
            this.returnTypeVectorTrees = new ArrayList<>();
            this.boolVectorTrees = new ArrayList<>();
            this.recTermsOfReturnType = new ArrayList<>();
        }

        public int levels() {
            return levelNonRecs.size();
        }

        public TypeMap getNonRecOfCost(int cost) {
            return levelNonRecs.get(cost - 1);
        }

        public RecTypeMap getRecOfCost(int cost) {
            return levelRecComps.get(cost - 1);
        }

        public int openNextLevel() {
            levelNonRecs.add(new TypeMap());
            levelRecComps.add(new RecTypeMap());
            return levelNonRecs.size();
        }

        public void createLibrariesForThisLevel() {
            int cost = levels();
            TypeMap typeMap = getNonRecOfCost(cost);
            ValueVectorTree<Term> returnTypeTree = new ValueVectorTree<>(examples.size());
            for (Type ty : typesMatch(typeMap.typeSet(), returnType)) {
                ValueTermMap vt = typeMap.get(ty);
                vt.forEach((vv, term) -> returnTypeTree.addTerm(term, vv));
            }
            returnTypeVectorTrees.add(returnTypeTree);
            ValueVectorTree<Term> boolTree = new ValueVectorTree<>(examples.size());
            for (Type ty : typesMatch(typeMap.typeSet(), tyBool)) {
                ValueTermMap vt = typeMap.get(ty);
                vt.forEach((vv, term) -> boolTree.addTerm(term, vv));
            }
            boolVectorTrees.add(boolTree);
            List<Pair<Term, ExtendedValueVec>> newRecs = new ArrayList<>();
            RecTypeMap recTypeMap = getRecOfCost(cost);
            for (Type ty : typesMatch(recTypeMap.typeSet(), returnType)) {
                RecMap recMap = recTypeMap.get(ty);
                newRecs.addAll(recMap);
            }
            recTermsOfReturnType.add(newRecs);
        }

        private List<Type> typesMatch(Set<Type> types, Type ty) {
            return types.stream()
                    .filter(t -> ty.instanceOf(t))
                    .map(Type::alphaNormalForm)
                    .collect(Collectors.toList());
        }

        public Optional<Term> boolLibrary(IndexValueMap vm) {
            for (int cost = 1; cost <= levels(); cost++) {
                ValueVectorTree<Term> tree = boolVectorTrees.get(cost - 1);
                Optional<Term> term = tree.searchATerm(vm);
                if (term.isPresent()) {
                    return term;
                }
            }
            return Optional.empty();
        }

        public Optional<Term> returnTypeLibrary(IndexValueMap vm) {
            for (int cost = 1; cost <= levels(); cost++) {
                ValueVectorTree<Term> tree = returnTypeVectorTrees.get(cost - 1);
                Optional<Term> term = tree.searchATerm(vm);
                if (term.isPresent()) {
                    return term;
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

        public Iterable<Pair<ValueVector, Term>> nonRecBoolTerms(int cost) {
            return boolVectorTrees.get(cost - 1).elements();
        }

        public void openToLevel(int n) {
            for (int i = 0; i < n - levels(); i++) {
                openNextLevel();
            }
        }

        public boolean registerNonRecAtLevel(int cost, Type ty, Term term, ValueVector valueVector) {
            Type ty1 = Type.alphaNormalForm(ty);
            for (Type t : totalNonRec.typeSet()) {
                if (ty1.instanceOf(t)) {
                    if (totalNonRec.get(t).contains(valueVector)) {
                        return false;
                    }
                    if (valueVector.contains(ValueError)) {
                        Map<Integer, ExtendedValue> indexValueMap = new HashMap<>();
                        for (int i = 0; i < valueVector.size(); i++) {
                            ExtendedValue value = valueVector.get(i);
                            if (value != ValueError) {
                                indexValueMap.put(i, value);
                            }
                        }
                        for (ValueVector vv : totalNonRec.get(t).keySet()) {
                            if (IndexValueMap.matchVector(indexValueMap, vv)) {
                                return false;
                            }
                        }
                    }
                }
            }
            totalNonRec.get(ty1).put(valueVector, term);
            getNonRecOfCost(cost).get(ty1).put(valueVector, term);
            return false;
        }

        public boolean registerRecTermAtLevel(int cost, Type ty, Term term, ExtendedValueVec valueVector) {
            if (config.useReductionRules) {
                if (term instanceof Term.Component && reductionRulesMap.containsKey(((Term.Component) term).name)) {
                    if (reductionRulesMap.get(((Term.Component) term).name).isReducible(((Term.Component) term).terms)) {
                        return false;
                    }
                }
            }
            Type ty1 = Type.alphaNormalForm(ty);
            getRecOfCost(cost).put(ty1, term, valueVector);
            return true;
        }

        public boolean registerTermAtLevel(int cost, Type ty, Term term, ExtendedValueVec valueVector) {
            Optional<ValueVector> x = ExtendedValueVec.toValueVec(valueVector);
            if (x.isPresent()) {
                return registerNonRecAtLevel(cost, ty, term, x.get());
            } else {
                return registerRecTermAtLevel(cost, ty, term, valueVector);
            }
        }

        public void print(int exampleCount) {
            logLn(config.logTotalMap, "TotalMap: (" + totalNonRec.statString() + ")");
            if (config.logTotalMap && config.logComponents) {
                totalNonRec.print(4);
            }
            logLn(config.logLevels, "Non-recursive LevelMaps:");
            if (config.logLevels) {
                for (int i = 0; i < levelNonRecs.size(); i++) {
                    int c = i + 1;
                    TypeMap typeMap = getNonRecOfCost(c);
                    logLn(true, "  " + c + ": (" + typeMap.statString() + ")");
                    if (config.logComponents) {
                        typeMap.print(6);
                    }
                }
            }
            logLn(config.logLevels, "Recursive LevelMaps:");
            if (config.logLevels) {
                for (int i = 0; i < levelRecComps.size(); i++) {
                    int c = i + 1;
                    RecTypeMap recTypeMap = getRecOfCost(c);
                    logLn(true, "  " + c + ": (" + recTypeMap.statString() + ")");
                    if (config.logComponents) {
                        recTypeMap.print(6);
                    }
                }
            }
        }
    }

    public static class Config {
        public int maxLevel;
        public boolean deleteAllErr;
        public boolean logGoal;
        public boolean logLevels;
        public boolean logComponents;
        public boolean logTotalMap;
        public BiFunction<ArgList, ArgList, Boolean> argListCompare;
        public int searchSizeFactor;
        public boolean useReductionRules;
        public boolean onlyForwardSearch;

        public Config() {
            this.maxLevel = Integer.MAX_VALUE;
            this.deleteAllErr = true;
            this.logGoal = true;
            this.logLevels = true;
            this.logComponents = true;
            this.logTotalMap = true;
            this.argListCompare = ArgList::anyArgSmaller;
            this.searchSizeFactor = 3;
            this.useReductionRules = true;
            this.onlyForwardSearch = false;
        }
    }

    public static class ExtendedCompImpl {
        public String name;
        public List<Type> inputTypes;
        public Type returnType;
        public Function<List<ExtendedValue>, ExtendedValue> execute;

        public ExtendedCompImpl(String name, List<Type> inputTypes, Type returnType, Function<List<ExtendedValue>, ExtendedValue> execute) {
            this.name = name;
            this.inputTypes = inputTypes;
            this.returnType = returnType;
            this.execute = execute;
        }

        public static ExtendedCompImpl fromImplOnTermValue(String name, List<Type> inputTypes, Type returnType, Function<ArgList, ExtendedValue> impl) {
            Function<List<ExtendedValue>, ExtendedValue> execute = args -> {
                Optional<ValueVector> knownArgs = ExtendedValueVec.toValueVec(args);
                if (knownArgs.isPresent()) {
                    return impl.apply(knownArgs.get());
                } else {
                    if (args.contains(ValueError)) {
                        return ValueError;
                    } else {
                        return ValueUnknown;
                    }
                }
            };
            return new ExtendedCompImpl(name, inputTypes, returnType, execute);
        }
    }

    public static class SynthesizedComponent {
        public ComponentSignature signature;
        public Term term;
        public int cost;
        public int level;

        public SynthesizedComponent(ComponentSignature signature, Term term, int cost, int level) {
            this.signature = signature;
            this.term = term;
            this.cost = cost;
            this.level = level;
        }
    }

    public static class ComponentSignature {
        public String name;
        public List<String> inputNames;
        public List<Type> inputTypes;
        public Type returnType;

        public ComponentSignature(String name, List<String> inputNames, List<Type> inputTypes, Type returnType) {
            this.name = name;
            this.inputNames = inputNames;
            this.inputTypes = inputTypes;
            this.returnType = returnType;
        }
    }

    public static class ComponentImpl {
        public String name;
        public List<Type> inputTypes;
        public Type returnType;
        public Function<List<Term>, Term> executeEfficient;

        public ComponentImpl(String name, List<Type> inputTypes, Type returnType, Function<List<Term>, Term> executeEfficient) {
            this.name = name;
            this.inputTypes = inputTypes;
            this.returnType = returnType;
            this.executeEfficient = executeEfficient;
        }
    }

    public static class ReducibleCheck {
        public boolean isReducible(List<Term> terms) {
            // TODO: Implement isReducible
            return false;
        }
    }

    public static class ArgList {
        public static boolean anyArgSmaller(ArgList arg1, ArgList arg2) {
            // TODO: Implement anyArgSmaller
            return false;
        }
    }

    public static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    public static class Type {
        public Type fixVars() {
            // TODO: Implement fixVars
            return null;
        }

        public boolean instanceOf(Type t) {
            // TODO: Implement instanceOf
            return false;
        }

        public Type alphaNormalForm() {
            // TODO: Implement alphaNormalForm
            return null;
        }
    }

    public static class Term {
        public static class Component extends Term {
            public String name;
            public List<Term> terms;

            public Component(String name, List<Term> terms) {
                this.name = name;
                this.terms = terms;
            }
        }
    }

    public static class TermValue extends Term {
    }

    public static class ValueUnknown extends TermValue {
    }

    public static class ValueError extends TermValue {
    }

    public static class ExtendedValue {
    }

    public static class ExtendedValueVec {
        public static Optional<ValueVector> toValueVec(ExtendedValueVec valueVector) {
            // TODO: Implement toValueVec
            return Optional.empty();
        }
    }

    public static class ValueVector {
        public int size() {
            // TODO: Implement size
            return 0;
        }

        public ExtendedValue get(int index) {
            // TODO: Implement get
            return null;
        }

        public boolean contains(ExtendedValue value) {
            // TODO: Implement contains
            return false;
        }
    }

    public static class ValueTermMap {
        public int size() {
            // TODO: Implement size
            return 0;
        }

        public ValueTermMap put(ValueVector valueVector, Term term) {
            // TODO: Implement put
            return null;
        }

        public Optional<Term> get(ValueVector valueVector) {
            // TODO: Implement get
            return Optional.empty();
        }
    }

    public static class ValueVectorTree<T> {
        public ValueVectorTree(int size) {
            // TODO: Implement ValueVectorTree
        }

        public void addTerm(T term, ValueVector valueVector) {
            // TODO: Implement addTerm
        }

        public Optional<T> searchATerm(IndexValueMap vm) {
            // TODO: Implement searchATerm
            return Optional.empty();
        }

        public Iterable<Pair<ValueVector, T>> elements() {
            // TODO: Implement elements
            return null;
        }
    }

    public static class IndexValueMap {
        public static boolean matchVector(Map<Integer, ExtendedValue> indexValueMap, ValueVector vv) {
            // TODO: Implement matchVector
            return false;
        }
    }

    public static class TimeTools {
        public static void printTimeUsed(String msg, boolean logLevels) {
            // TODO: Implement printTimeUsed
        }
    }
}


