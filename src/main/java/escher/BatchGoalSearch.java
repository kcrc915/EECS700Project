package escher;

import escher.Synthesis.IndexValueMap;
import escher.Synthesis.ValueVector;
import escher.Synthesis.splitGoal;
import java.util.*;
import java.util.function.BiConsumer;

public class BatchGoalSearch {
    private interface SearchResult {
        void foreach(BiConsumer<Integer, Term> f);
    }

    private static class NotFoundUnderCost implements SearchResult {
        private final int cost;

        public NotFoundUnderCost(int cost) {
            this.cost = cost;
        }

        @Override
        public void foreach(BiConsumer<Integer, Term> f) {
        }
    }

    private static class FoundAtCost implements SearchResult {
        private final int cost;
        private final Term term;

        public FoundAtCost(int cost, Term term) {
            this.cost = cost;
            this.term = term;
        }

        @Override
        public void foreach(BiConsumer<Integer, Term> f) {
            f.accept(cost, term);
        }
    }

    private static Map<Set<Integer>, SearchResult> emptyBuffer() {
        return new HashMap<>();
    }

    private static Optional<Pair<Pair<Integer, Term>, List<Integer>>> maxSatConditions(IndexValueMap vm,
                                                                                       Function<IndexValueMap, Optional<Pair<Integer, Term>>> boolOfVM) {
        List<Integer> keyList = vm.keySet().stream()
                .filter(i -> vm.get(i) == ValueBool.TRUE)
                .sorted(Comparator.reverseOrder())
                .toList();
        IndexValueMap vm1 = vm;
        while (!keyList.isEmpty()) {
            Optional<Pair<Integer, Term>> result = boolOfVM.apply(vm1);
            if (result.isPresent()) {
                return Optional.of(result.get().mapFirst(x -> x).mapSecond(x -> keyList));
            }
            vm1 = vm1.updated(keyList.get(0), ValueBool.FALSE);
            keyList = keyList.subList(1, keyList.size());
        }
        return Optional.empty();
    }

    private final int maxCompCost;
    private final BiFunction<Integer, IndexValueMap, Optional<Term>> termOfCostAndVM;
    private final Function<Integer, Iterable<Pair<ValueVector, Term>>> termsOfCost;
    private final Function<Integer, Iterable<Pair<ValueVector, Term>>> boolTermsOfCost;
    private final Function<IndexValueMap, Optional<Pair<Integer, Term>>> boolOfVM;
    private final Map<Set<Integer>, SearchResult> buffer;

    public BatchGoalSearch(int maxCompCost,
                           BiFunction<Integer, IndexValueMap, Optional<Term>> termOfCostAndVM,
                           Function<Integer, Iterable<Pair<ValueVector, Term>>> termsOfCost,
                           Function<Integer, Iterable<Pair<ValueVector, Term>>> boolTermsOfCost,
                           Function<IndexValueMap, Optional<Pair<Integer, Term>>> boolOfVM) {
        this.maxCompCost = maxCompCost;
        this.termOfCostAndVM = termOfCostAndVM;
        this.termsOfCost = termsOfCost;
        this.boolTermsOfCost = boolTermsOfCost;
        this.boolOfVM = boolOfVM;
        this.buffer = emptyBuffer();
    }

    public Optional<Pair<Integer, Term>> searchThenFirst(int cost, IndexValueMap currentGoal) {
        Set<Integer> keySet = currentGoal.keySet();
        SearchResult result = buffer.get(keySet);
        if (result != null) {
            result.foreach((c, term) -> {
                if (c <= cost) {
                    return Optional.of(new Pair<>(c, term));
                }
            });
            if (result instanceof NotFoundUnderCost && ((NotFoundUnderCost) result).cost >= cost) {
                return Optional.empty();
            }
        }
        SearchResult buffered = searchResult -> {
            buffer.put(keySet, searchResult);
            if (searchResult instanceof NotFoundUnderCost) {
                return Optional.empty();
            } else if (searchResult instanceof FoundAtCost) {
                FoundAtCost foundAtCost = (FoundAtCost) searchResult;
                return Optional.of(new Pair<>(foundAtCost.cost, foundAtCost.term));
            }
        };
        int maxCost = Math.min(maxCompCost, cost);
        for (int c = 1; c <= maxCost; c++) {
            Optional<Term> term = termOfCostAndVM.apply(c, currentGoal);
            if (term.isPresent()) {
                return buffered.apply(new FoundAtCost(c, term.get()));
            }
        }
        int ifCost = 1;
        Optional<Pair<Integer, Term>> minCostCandidate = Optional.empty();
        for (int cThen = 1; cThen <= maxCost - 1 - ifCost; cThen++) {
            for (Pair<ValueVector, Term> pair : termsOfCost.apply(cThen)) {
                ValueVector thenVec = pair.getFirst();
                Term tThen = pair.getSecond();
                Optional<Triple<IndexValueMap, List<Integer>, List<Integer>>> splitResult = IndexValueMap.splitValueMap(currentGoal, thenVec);
                if (splitResult.isPresent()) {
                    Triple<IndexValueMap, List<Integer>, List<Integer>> triple = splitResult.get();
                    IndexValueMap vm = triple.getFirst();
                    List<Integer> trueKeys = triple.getSecond();
                    Optional<Pair<Pair<Integer, Term>, List<Integer>>> maxSatResult = maxSatConditions(vm, boolOfVM);
                    if (maxSatResult.isPresent()) {
                        Pair<Pair<Integer, Term>, List<Integer>> pair2 = maxSatResult.get();
                        Pair<Integer, Term> pair3 = pair2.getFirst();
                        List<Integer> pair4 = pair2.getSecond();
                        IndexValueMap elseGoal = currentGoal.remove(pair4);
                        int costSoFar = cThen + pair3.getFirst() + ifCost;
                        int maxCostForElse = Math.min(cost, minCostCandidate.map(Pair::getFirst).orElse(Integer.MAX_VALUE) - 1) - costSoFar;
                        Optional<Pair<Integer, Term>> searchResult = searchThenFirst(maxCostForElse, elseGoal);
                        if (searchResult.isPresent()) {
                            int cElse = searchResult.get().getFirst();
                            Term tElse = searchResult.get().getSecond();
                            Term t = DSL.ifThenElse(tCond, tThen, tElse);
                            int totalCost = cElse + costSoFar;
                            minCostCandidate = Optional.of(new Pair<>(totalCost, t));
                        }
                    }
                }
            }
        }
        if (minCostCandidate.isPresent()) {
            return buffered.apply(new FoundAtCost(minCostCandidate.get().getFirst(), minCostCandidate.get().getSecond()));
        } else {
            return buffered.apply(new NotFoundUnderCost(cost));
        }
    }

    public Optional<Pair<Integer, Term>> searchCondFirst(int cost, IndexValueMap currentGoal) {
        Set<Integer> keySet = currentGoal.keySet();
        SearchResult result = buffer.get(keySet);
        if (result != null) {
            result.foreach((c, term) -> {
                if (c <= cost) {
                    return Optional.of(new Pair<>(c, term));
                }
            });
            if (result instanceof NotFoundUnderCost && ((NotFoundUnderCost) result).cost >= cost) {
                return Optional.empty();
            }
        }
        SearchResult buffered = searchResult -> {
            buffer.put(keySet, searchResult);
            if (searchResult instanceof NotFoundUnderCost) {
                return Optional.empty();
            } else if (searchResult instanceof FoundAtCost) {
                FoundAtCost foundAtCost = (FoundAtCost) searchResult;
                return Optional.of(new Pair<>(foundAtCost.cost, foundAtCost.term));
            }
        };
        int maxCost = Math.min(maxCompCost, cost);
        for (int c = 1; c <= maxCost; c++) {
            Optional<Term> term = termOfCostAndVM.apply(c, currentGoal);
            if (term.isPresent()) {
                return buffered.apply(new FoundAtCost(c, term.get()));
            }
        }
        int ifCost = 1;
        Optional<Pair<Integer, Term>> minCostCandidate = Optional.empty();
        for (int cCond = 1; cCond <= Math.min(maxCompCost, cost - ifCost - 2); cCond++) {
            for (Pair<ValueVector, Term> pair : boolTermsOfCost.apply(cCond)) {
                ValueVector condVec = pair.getFirst();
                Term tCond = pair.getSecond();
                Optional<Pair<IndexValueMap, IndexValueMap>> splitResult = splitGoal(condVec, currentGoal);
                if (splitResult.isPresent()) {
                    Pair<IndexValueMap, IndexValueMap> pair2 = splitResult.get();
                    IndexValueMap thenGoal = pair2.getFirst();
                    IndexValueMap elseGoal = pair2.getSecond();
                    Optional<Pair<Integer, Term>> thenCandidate = Optional.empty();
                    for (int cThen = 1; cThen <= Math.min(maxCompCost, cost - ifCost - cCond - 1); cThen++) {
                        for (Pair<ValueVector, Term> pair3 : termsOfCost.apply(cThen)) {
                            ValueVector vv = pair3.getFirst();
                            Term term = pair3.getSecond();
                            if (IndexValueMap.matchVector(thenGoal, vv)) {
                                thenCandidate = Optional.of(new Pair<>(cThen, term));
                                break;
                            }
                        }
                        if (thenCandidate.isPresent()) {
                            break;
                        }
                    }
                    if (thenCandidate.isPresent()) {
                        int cThen = thenCandidate.get().getFirst();
                        Term tThen = thenCandidate.get().getSecond();
                        int costSoFar = cThen + cCond + ifCost;
                        int maxCostForElse = Math.min(cost, minCostCandidate.map(Pair::getFirst).orElse(Integer.MAX_VALUE) - 1) - costSoFar;
                        Optional<Pair<Integer, Term>> searchResult = searchCondFirst(maxCostForElse, elseGoal);
                        if (searchResult.isPresent()) {
                            int cElse = searchResult.get().getFirst();
                            Term tElse = searchResult.get().getSecond();
                            Term t = DSL.ifThenElse(tCond, tThen, tElse);
                            int totalCost = cElse + costSoFar;
                            minCostCandidate = Optional.of(new Pair<>(totalCost, t));
                        }
                    }
                }
            }
        }
        if (minCostCandidate.isPresent()) {
            return buffered.apply(new FoundAtCost(minCostCandidate.get().getFirst(), minCostCandidate.get().getSecond()));
        } else {
            return buffered.apply(new NotFoundUnderCost(cost));
        }
    }
}


