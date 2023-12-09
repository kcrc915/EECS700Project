package escher;

import escher.Term.Component;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The program synthesizing algorithm
 */
public class Synthesis {
    private static class ValueVector extends ArrayList<AscendRecSynthesizer.TermValue> {
    }

    private static class ArgList extends ArrayList<TermValue> {
    }

    private static class IndexValueMap extends HashMap<Integer, TermValue> {
    }

    private static class ValueTermMap extends HashMap<ValueVector, Term> {
    }

    private static class ExtendedValueVec extends ArrayList<ExtendedValue> {
    }

    private static class IndexValueMap {
        public static boolean matchVector(IndexValueMap valueMap, ValueVector valueVector) {
            for (Map.Entry<Integer, TermValue> entry : valueMap.entrySet()) {
                int k = entry.getKey();
                TermValue v = entry.getValue();
                if (valueVector.get(k) != v) {
                    return false;
                }
            }
            return true;
        }

        public static List<IndexValueMap> splitValueMap(IndexValueMap valueMap, ValueVector valueVector) {
            IndexValueMap thenMap = new IndexValueMap();
            IndexValueMap elseMap = new IndexValueMap();
            IndexValueMap condMap = new IndexValueMap();
            for (Map.Entry<Integer, TermValue> entry : valueMap.entrySet()) {
                int i = entry.getKey();
                TermValue v = entry.getValue();
                boolean match = valueVector.get(i) == v;
                if (match) {
                    thenMap.put(i, v);
                } else {
                    elseMap.put(i, v);
                }
                condMap.put(i, new ValueBool(match));
            }
            if (!thenMap.isEmpty() && !elseMap.isEmpty()) {
                List<IndexValueMap> result = new ArrayList<>();
                result.add(condMap);
                result.add(thenMap);
                result.add(elseMap);
                return result;
            } else {
                return null;
            }
        }

        public static String show(IndexValueMap valueMap, int exampleCount) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < exampleCount; i++) {
                TermValue value = valueMap.get(i);
                if (value != null) {
                    sb.append(value.show());
                } else {
                    sb.append("?");
                }
                if (i < exampleCount - 1) {
                    sb.append(", ");
                }
            }
            return "<" + sb.toString() + ">";
        }
    }

    private static class ArgList {
        public static boolean alphabeticSmaller(ArgList args1, ArgList args2) {
            if (args1.size() != args2.size()) {
                throw new IllegalArgumentException("arg lists must have the same length");
            }
            for (int i = 0; i < args1.size(); i++) {
                TermValue arg1 = args1.get(i);
                TermValue arg2 = args2.get(i);
                if (arg1.greaterThan(arg2)) {
                    return false;
                } else if (arg1.smallerThan(arg2)) {
                    return true;
                }
            }
            return false;
        }

        public static boolean anyArgSmaller(ArgList args1, ArgList args2) {
            if (args1.size() != args2.size()) {
                throw new IllegalArgumentException("arg lists must have the same length");
            }
            boolean smaller = false;
            for (int i = 0; i < args1.size(); i++) {
                TermValue arg1 = args1.get(i);
                TermValue arg2 = args2.get(i);
                if (arg1.greaterThan(arg2)) {
                    return false;
                } else if (arg1.smallerThan(arg2)) {
                    smaller = true;
                }
            }
            return smaller;
        }

        public static String showArgList(ArgList argList) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < argList.size(); i++) {
                TermValue arg = argList.get(i);
                sb.append(arg.show());
                if (i < argList.size() - 1) {
                    sb.append(", ");
                }
            }
            return "(" + sb.toString() + ")";
        }
    }

    private static class ValueVector {
        public static String show(ValueVector valueVector) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < valueVector.size(); i++) {
                TermValue value = valueVector.get(i);
                sb.append(value.show());
                if (i < valueVector.size() - 1) {
                    sb.append(", ");
                }
            }
            return "<" + sb.toString() + ">";
        }
    }

    private static class ExtendedValueVec extends ArrayList<ExtendedValue> {
        public static ValueVector toValueVec(ExtendedValueVec extendedValueVec) {
            ValueVector result = new ValueVector();
            for (ExtendedValue value : extendedValueVec) {
                if (value instanceof ValueUnknown) {
                    return null;
                } else {
                    result.add((TermValue) value);
                }
            }
            return result;
        }

        public static String show(ExtendedValueVec valueVector) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < valueVector.size(); i++) {
                TermValue value = valueVector.get(i);
                sb.append(value.show());
                if (i < valueVector.size() - 1) {
                    sb.append(", ");
                }
            }
            return "<" + sb.toString() + ">";
        }

        public static MatchResult matchWithIndexValueMap(ExtendedValueVec extendedValueVec, IndexValueMap indexValueMap) {
            IndexValueMap leftToCheck = new IndexValueMap();
            for (Map.Entry<Integer, TermValue> entry : indexValueMap.entrySet()) {
                int i = entry.getKey();
                TermValue value = entry.getValue();
                TermValue extendedValue = extendedValueVec.get(i);
                if (extendedValue instanceof ValueUnknown) {
                    leftToCheck.put(i, value);
                } else if (!value.equals(extendedValue)) {
                    return MatchResult.NotMatch;
                }
            }
            if (leftToCheck.isEmpty()) {
                return MatchResult.ExactMatch;
            } else {
                return new MatchResult.PossibleMatch(leftToCheck);
            }
        }
    }

    private static class MatchResult {
        public static final MatchResult NotMatch = new MatchResult();
        public static final MatchResult ExactMatch = new MatchResult();
        public static class PossibleMatch extends MatchResult {
            public IndexValueMap leftToCheck;
        }
    }

    private static String showExamples(List<Pair<ArgList, TermValue>> examples) {
        StringBuilder sb = new StringBuilder();
        for (Pair<ArgList, TermValue> example : examples) {
            ArgList argList = example.getKey();
            TermValue result = example.getValue();
            sb.append(ArgList.showArgList(argList));
            sb.append(" -> ");
            sb.append(result.show());
            sb.append("; ");
        }
        return sb.toString();
    }

    private static List<List<Integer>> divideNumberAsSum(int number, int pieces, int minNumber) {
        List<List<Integer>> result = new ArrayList<>();
        if (number < minNumber) {
            return result;
        }
        if (pieces == 1) {
            List<Integer> list = new ArrayList<>();
            list.add(number);
            result.add(list);
            return result;
        }
        for (int n = minNumber; n <= number; n++) {
            List<List<Integer>> subResult = divideNumberAsSum(number - n, pieces - 1, minNumber);
            for (List<Integer> subList : subResult) {
                List<Integer> list = new ArrayList<>();
                list.add(n);
                list.addAll(subList);
                result.add(list);
            }
        }
        return result;
    }

    private static <A> Iterator<List<A>> cartesianProduct(List<Iterable<A>> listOfSets) {
        if (listOfSets.isEmpty()) {
            List<A> list = new ArrayList<>();
            List<List<A>> result = new ArrayList<>();
            result.add(list);
            return result.iterator();
        }
        Iterable<A> head = listOfSets.get(0);
        List<Iterable<A>> tail = listOfSets.subList(1, listOfSets.size());
        List<List<A>> result = new ArrayList<>();
        for (A v : head) {
            Iterator<List<A>> subResult = cartesianProduct(tail);
            while (subResult.hasNext()) {
                List<A> subList = subResult.next();
                List<A> list = new ArrayList<>();
                list.add(v);
                list.addAll(subList);
                result.add(list);
            }
        }
        return result.iterator();
    }

    private static class ComponentSignature {
        public String name;
        public List<String> argNames;
        public List<Type> inputTypes;
        public Type returnType;

        public ComponentSignature(String name, List<String> argNames, List<Type> inputTypes, Type returnType) {
            this.name = name;
            this.argNames = argNames;
            this.inputTypes = inputTypes;
            this.returnType = returnType;
        }
    }

    private static class SynthesizedComponent {
        public ComponentSignature signature;
        public Term body;
        public int cost;
        public int depth;

        public SynthesizedComponent(ComponentSignature signature, Term body, int cost, int depth) {
            this.signature = signature;
            this.body = body;
            this.cost = cost;
            this.depth = depth;
        }

        public String show() {
            StringBuilder sb = new StringBuilder();
            List<String> paramList = new ArrayList<>();
            for (int i = 0; i < signature.argNames.size(); i++) {
                String argName = signature.argNames.get(i);
                Type ty = signature.inputTypes.get(i);
                paramList.add("@" + argName + ": " + ty);
            }
            sb.append(signature.name);
            sb.append("(");
            sb.append(String.join(", ", paramList));
            sb.append("): ");
            sb.append(signature.returnType);
            sb.append(" =\n  ");
            sb.append(body.show());
            return sb.toString();
        }

        public void print() {
            System.out.println(show());
            Term.printTerm(body, 2);
        }
    }

    private static class BufferedOracle {
        private List<Pair<ArgList, TermValue>> examples;
        private Map<ArgList, TermValue> knownMap;
        private Map<ArgList, TermValue> buffer;

        public BufferedOracle(List<Pair<ArgList, TermValue>> examples, PartialFunction<ArgList, TermValue> oracle, List<Pair<ArgList, TermValue>> initBuffer) {
            this.examples = examples;
            this.knownMap = new HashMap<>();
            for (Pair<ArgList, TermValue> example : examples) {
                ArgList argList = example.getKey();
                TermValue result = example.getValue();
                knownMap.put(argList, result);
            }
            this.buffer = new HashMap<>();
            for (Pair<ArgList, TermValue> example : initBuffer) {
                ArgList argList = example.getKey();
                TermValue result = example.getValue();
                buffer.put(argList, result);
            }
        }

        public Map<List<TermValue>, TermValue> getBuffer() {
            Map<List<TermValue>, TermValue> result = new HashMap<>();
            for (Map.Entry<ArgList, TermValue> entry : buffer.entrySet()) {
                ArgList argList = entry.getKey();
                TermValue resultValue = entry.getValue();
                result.put(argList, resultValue);
            }
            return result;
        }

        public TermValue evaluate(ArgList argList) {
            TermValue result = knownMap.get(argList);
            if (result != null) {
                return result;
            }
            result = buffer.get(argList);
            if (result != null) {
                return result;
            }
            result = oracle.apply(argList);
            buffer.put(argList, result);
            return result;
        }
    }

    private static boolean exampleLt(Pair<ArgList, TermValue> ex1, Pair<ArgList, TermValue> ex2) {
        return ArgList.alphabeticSmaller(ex1.getKey(), ex2.getKey());
    }

    private interface RebootStrategy {
        List<Pair<ArgList, TermValue>> newExamplesAndOracleBuffer(List<Pair<ArgList, TermValue>> examples, List<Pair<ArgList, TermValue>> failed, List<Pair<ArgList, TermValue>> passed);
    }

    private static class AddSimplestFailedExample implements RebootStrategy {
        @Override
        public List<Pair<ArgList, TermValue>> newExamplesAndOracleBuffer(List<Pair<ArgList, TermValue>> examples, List<Pair<ArgList, TermValue>> failed, List<Pair<ArgList, TermValue>> passed) {
            List<Pair<ArgList, TermValue>> failSorted = new ArrayList<>(failed);
            failSorted.sort(Synthesis::exampleLt);
            List<Pair<ArgList, TermValue>> newExamples = new ArrayList<>(examples);
            newExamples.add(failSorted.get(0));
            List<Pair<ArgList, TermValue>> newOracleBuffer = new ArrayList<>(failSorted.subList(1, failSorted.size()));
            newOracleBuffer.addAll(passed);
            return new Pair<>(newExamples, newOracleBuffer);
        }
    }

    private static class AddMostComplicatedFailedExample implements RebootStrategy {
        @Override
        public List<Pair<ArgList, TermValue>> newExamplesAndOracleBuffer(List<Pair<ArgList, TermValue>> examples, List<Pair<ArgList, TermValue>> failed, List<Pair<ArgList, TermValue>> passed) {
            List<Pair<ArgList, TermValue>> failSorted = new ArrayList<>(failed);
            failSorted.sort(Synthesis::exampleLt);
            List<Pair<ArgList, TermValue>> newExamples = new ArrayList<>(examples);
            newExamples.add(failSorted.get(failSorted.size() - 1));
            List<Pair<ArgList, TermValue>> newOracleBuffer = new ArrayList<>(passed);
            newOracleBuffer.addAll(failSorted.subList(0, failSorted.size() - 1));
            return new Pair<>(newExamples, newOracleBuffer);
        }
    }

    private static boolean isInterestingSignature(Type goalReturnType, List<Type> inputTypes) {
        List<Type> goodTypes = new ArrayList<>();
        goodTypes.add(tyBool);
        goodTypes.add(goalReturnType);
        goodTypes.addAll(inputTypes);
        return (List<Type> argTypes, Type returnType) -> {
            for (Type gt : goodTypes) {
                if (returnType.canAppearIn(gt)) {
                    return true;
                }
            }
            for (Type argType : argTypes) {
                boolean interesting = false;
                for (Type gt : goodTypes) {
                    if (argType.canAppearIn(gt)) {
                        interesting = true;
                        break;
                    }
                }
                if (!interesting) {
                    return false;
                }
            }
            return true;
        };
    }

    private static List<Pair<List<Type>, Type>> typesForCosts(Function<Integer, Iterable<Type>> typesOfCost, List<Integer> costs, List<Type> inputTypes, Type returnType) {
        int signatureNextFreeId = Math.max(returnType.nextFreeId, inputTypes.stream().mapToInt(Type::nextFreeId).max().orElse(0));
        List<Pair<List<Type>, Type>> result = new ArrayList<>();
        typesForCostsAux(0, signatureNextFreeId, TypeSubst.empty(), result, typesOfCost, costs, inputTypes, returnType);
        return result;
    }

    private static void typesForCostsAux(int argId, int nextFreeId, TypeSubst subst, List<Pair<List<Type>, Type>> result, Function<Integer, Iterable<Type>> typesOfCost, List<Integer> costs, List<Type> inputTypes, Type returnType) {
        if (argId == costs.size()) {
            List<Type> argTypes = new ArrayList<>();
            result.add(new Pair<>(argTypes, subst.apply(returnType).alphaNormalForm()));
            return;
        }
        int c = costs.get(argId);
        Type requireType = subst.apply(inputTypes.get(argId));
        for (Type t : typesOfCost.apply(c)) {
            Type candidateType = t.shiftId(nextFreeId);
            TypeSubst unifyResult = Type.unify(requireType, candidateType);
            if (unifyResult != null) {
                int newFreeId = nextFreeId + t.nextFreeId();
                TypeSubst newSubst = subst.compose(unifyResult);
                List<Type> newArgTypes = new ArrayList<>();
                newArgTypes.add(t);
                newArgTypes.addAll(argTypes);
                typesForCostsAux(argId + 1, newFreeId, newSubst, result, typesOfCost, costs, inputTypes, returnType);
            }
        }
    }

    private static void showExamples(String tag, List<Pair<ArgList, TermValue>> examples, int maxExamplesShown) {
        System.out.println(tag + " (" + examples.size() + "):");
        for (int i = 0; i < Math.min(examples.size(), maxExamplesShown); i++) {
            Pair<ArgList, TermValue> example = examples.get(i);
            ArgList argList = example.getKey();
            TermValue result = example.getValue();
            System.out.print(ArgList.showArgList(argList));
            System.out.print(" -> ");
            System.out.println(result.show());
        }
        if (examples.size() > maxExamplesShown) {
            System.out.println(" ...(" + (examples.size() - maxExamplesShown) + " more not shown)...");
        }
    }

    private static List<Pair<IndexValueMap, IndexValueMap>> splitGoal(ValueVector boolVector, IndexValueMap goal) {
        IndexValueMap thenGoal = new IndexValueMap();
        IndexValueMap elseGoal = new IndexValueMap();
        IndexValueMap condGoal = new IndexValueMap();
        for (Map.Entry<Integer, TermValue> entry : goal.entrySet()) {
            int i = entry.getKey();
            TermValue tv = entry.getValue();
            if (boolVector.get(i) == ValueError) {
                return null;
            }
            if (((ValueBool) boolVector.get(i)).value) {
                thenGoal.put(i, tv);
            } else {
                elseGoal.put(i, tv);
            }
            condGoal.put(i, new ValueBool(((ValueBool) boolVector.get(i)).value));
        }
        if (!thenGoal.isEmpty() && !elseGoal.isEmpty()) {
            List<Pair<IndexValueMap, IndexValueMap>> result = new ArrayList<>();
            result.add(new Pair<>(condGoal, thenGoal));
            result.add(new Pair<>(condGoal, elseGoal));
            return result;
        } else {
            return null;
        }
    }
}


