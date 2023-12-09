package escher;

import java.util.*;
import escher.DynamicGoalSearch.ExecuteHoleException;
import escher.Synthesis.*;

/**
 * Search programs in Ascending Recursive Form
 */
public class AscendRecGoalSearch {
    private int maxCompCost;
    private ComponentSignature signature;
    private Set<ComponentImpl> envComps;
    private BiFunction<ArgList, ArgList, Boolean> argListCompare;
    private List<ArgList> inputVector;
    private List<List<Pair<ValueVector, Term>>> termsWithKnownVV;
    private List<List<Pair<ValueVector, Term>>> nonRecBoolTerms;
    private String holeName;
    private List<Map<String, TermValue>> varMaps;
    private Map<String, ComponentImpl> envCompMap;

    public AscendRecGoalSearch(int maxCompCost,
                               ComponentSignature signature,
                               Set<ComponentImpl> envComps,
                               BiFunction<ArgList, ArgList, Boolean> argListCompare,
                               List<ArgList> inputVector,
                               List<List<Pair<ValueVector, Term>>> termsWithKnownVV,
                               List<List<Pair<ValueVector, Term>>> nonRecBoolTerms) {
        this.maxCompCost = maxCompCost;
        this.signature = signature;
        this.envComps = envComps;
        this.argListCompare = argListCompare;
        this.inputVector = inputVector;
        this.termsWithKnownVV = termsWithKnownVV;
        this.nonRecBoolTerms = nonRecBoolTerms;
        this.holeName = "HOLE";
        this.varMaps = new ArrayList<>();
        for (int i = 0; i < inputVector.size(); i++) {
            Map<String, TermValue> varMap = new HashMap<>();
            for (int j = 0; j < signature.argNames.size(); j++) {
                varMap.put(signature.argNames.get(j), inputVector.get(i).get(j));
            }
            this.varMaps.add(varMap);
        }
        this.envCompMap = new HashMap<>();
        for (ComponentImpl comp : envComps) {
            this.envCompMap.put(comp.name, comp);
        }
        ComponentImpl holeImpl = new ComponentImpl(holeName, new ArrayList<>(), signature.returnType, (ArgList args) -> {
            throw new ExecuteHoleException();
        });
        this.envCompMap.put(holeName, holeImpl);
    }

    public ComponentImpl assembleRecProgram(Term term) {
        return ComponentImpl.recursiveImpl(signature, envCompMap, argListCompare, term);
    }

    public Pair<Boolean, Optional<List<String>>> checkTrigger(Term term, Optional<List<String>> prefixTrigger) {
        if (!prefixTrigger.isPresent()) {
            return new Pair<>(false, Optional.empty());
        }
        List<String> ts = prefixTrigger.get();
        if (ts.isEmpty()) {
            return new Pair<>(false, Optional.empty());
        }
        if (ts.size() == 1) {
            return new Pair<>(ts.get(0).equals(term.show()), Optional.empty());
        }
        String h = ts.get(0);
        List<String> t = ts.subList(1, ts.size());
        if (term.show().equals(h)) {
            return new Pair<>(false, Optional.of(t));
        } else {
            return new Pair<>(false, Optional.empty());
        }
    }

    public Optional<Pair<Integer, Term>> searchMin(int cost,
                                                   IndexValueMap currentGoal,
                                                   List<List<Pair<Term, ExtendedValueVec>>> recTermsOfReturnType,
                                                   Function<Term, Term> fillTermToHole,
                                                   boolean isFirstBranch,
                                                   Optional<List<String>> prefixTrigger) {
        if (cost <= 0) {
            return Optional.empty();
        }
        Set<String> keySet = currentGoal.keySet();
        int maxCost = Math.min(maxCompCost, cost);
        for (int c = 1; c <= maxCost; c++) {
            for (Pair<ValueVector, Term> pair : termsWithKnownVV.get(c - 1)) {
                ValueVector vv = pair.getKey();
                Term term = pair.getValue();
                if (IndexValueMap.matchVector(currentGoal, vv)) {
                    return Optional.of(new Pair<>(c, term));
                }
            }
            if (!isFirstBranch) {
                for (Pair<Term, ExtendedValueVec> pair : recTermsOfReturnType.get(c - 1)) {
                    Term term = pair.getKey();
                    ExtendedValueVec vv = pair.getValue();
                    ExtendedValueVec.MatchResult matchResult = vv.matchWithIndexValueMap(currentGoal);
                    if (matchResult == ExtendedValueVec.MatchResult.ExactMatch) {
                        return Optional.of(new Pair<>(c, term));
                    } else if (matchResult == ExtendedValueVec.MatchResult.PossibleMatch) {
                        List<Pair<Integer, Value>> leftToCheck = vv.getLeftToCheck();
                        ComponentImpl p = assembleRecProgram(fillTermToHole.apply(term));
                        boolean passCheck = leftToCheck.stream().allMatch(pair2 -> {
                            int i = pair2.getKey();
                            Value desired = pair2.getValue();
                            return p.executeEfficient(inputVector.get(i)).equals(desired);
                        });
                        if (passCheck) {
                            return Optional.of(new Pair<>(c, term));
                        }
                    }
                }
            }
        }
        int ifCost = 1;
        Optional<Pair<Integer, Term>> minCostCandidate = Optional.empty();
        for (int cCond = 1; cCond <= Math.min(maxCompCost, cost - ifCost - 2); cCond++) {
            List<Pair<ValueVector, Term>> nonRecBoolTermList = nonRecBoolTerms.get(cCond - 1);
            for (Pair<ValueVector, Term> pair : nonRecBoolTermList) {
                ValueVector condVec = pair.getKey();
                Term tCond = pair.getValue();
                Pair<IndexValueMap, IndexValueMap> splitResult = splitGoal(condVec, currentGoal);
                IndexValueMap thenGoal = splitResult.getKey();
                IndexValueMap elseGoal = splitResult.getValue();
                Pair<Boolean, Optional<List<String>>> checkTriggerResult = checkTrigger(tCond, prefixTrigger);
                boolean trigCond = checkTriggerResult.getKey();
                Optional<List<String>> prefixTrigger1 = checkTriggerResult.getValue();
                if (trigCond) {
                    System.out.println("trigger cond branch!");
                }
                Optional<Pair<Integer, Term>> thenCandidate = Optional.empty();
                for (int cThen = 1; cThen <= Math.min(maxCompCost, cost - ifCost - cCond - 1); cThen++) {
                    List<Pair<ValueVector, Term>> termsWithKnownVVList = termsWithKnownVV.get(cThen - 1);
                    for (Pair<ValueVector, Term> pair2 : termsWithKnownVVList) {
                        ValueVector vv = pair2.getKey();
                        Term term = pair2.getValue();
                        if (IndexValueMap.matchVector(thenGoal, vv)) {
                            thenCandidate = Optional.of(new Pair<>(cThen, term));
                            break;
                        }
                    }
                    if (thenCandidate.isPresent()) {
                        break;
                    }
                    List<Pair<Term, ExtendedValueVec>> recTermsOfReturnTypeList = recTermsOfReturnType.get(cThen - 1);
                    for (Pair<Term, ExtendedValueVec> pair2 : recTermsOfReturnTypeList) {
                        Term tThen = pair2.getKey();
                        ExtendedValueVec thenEVec = pair2.getValue();
                        ComponentImpl partialImpl = ComponentImpl.recursiveImpl(signature, envCompMap, argListCompare,
                                fillTermToHole.apply(new IfThenElse(tCond, tThen, new Hole())));
                        Map<String, ComponentImpl> envWithPartialImpl = new HashMap<>(envCompMap);
                        envWithPartialImpl.put(signature.name, partialImpl);
                        boolean isCandidateForThen = true;
                        for (Map.Entry<Integer, Value> entry : thenGoal.entrySet()) {
                            int i = entry.getKey();
                            Value v = entry.getValue();
                            Value thenEVecValue = thenEVec.get(i);
                            if (thenEVecValue instanceof ValueUnknown) {
                                Map<String, TermValue> varMap = varMaps.get(i);
                                try {
                                    if (!Term.executeTerm(varMap, envWithPartialImpl).apply(tThen).equals(v)) {
                                        isCandidateForThen = false;
                                        break;
                                    }
                                } catch (ExecuteHoleException e) {
                                    isCandidateForThen = false;
                                    break;
                                }
                            } else if (!thenEVecValue.equals(v)) {
                                isCandidateForThen = false;
                                break;
                            }
                        }
                        if (isCandidateForThen) {
                            thenCandidate = Optional.of(new Pair<>(cThen, tThen));
                            break;
                        }
                    }
                    if (thenCandidate.isPresent()) {
                        break;
                    }
                }
                if (thenCandidate.isPresent()) {
                    Pair<Boolean, Optional<List<String>>> checkTriggerResult2 = checkTrigger(thenCandidate.get().getValue(), prefixTrigger1);
                    boolean trigThen = checkTriggerResult2.getKey();
                    Optional<List<String>> prefixTrigger2 = checkTriggerResult2.getValue();
                    if (trigThen) {
                        System.out.println("trigger then branch!");
                    }
                    Function<Term, Term> assembleTerm = (Term tElse) -> fillTermToHole.apply(new IfThenElse(tCond, thenCandidate.get().getValue(), tElse));
                    int costSoFar = thenCandidate.get().getKey() + cCond + ifCost;
                    int maxCostForElse = Math.min(cost, minCostCandidate.map(Pair::getKey).orElse(Integer.MAX_VALUE) - 1) - costSoFar;
                    ComponentImpl partialImpl = ComponentImpl.recursiveImpl(signature, envCompMap, argListCompare, assembleTerm.apply(new Hole()));
                    Map<String, ComponentImpl> envWithPartialImpl = new HashMap<>(envCompMap);
                    envWithPartialImpl.put(signature.name, partialImpl);
                    List<List<Pair<Term, ExtendedValueVec>>> newRecTermsOfReturnType = recTermsOfReturnType;
                    Optional<Pair<Integer, Term>> elseCandidate = searchMin(maxCostForElse, elseGoal, newRecTermsOfReturnType, assembleTerm,
                            false, prefixTrigger2);
                    if (elseCandidate.isPresent()) {
                        int totalCost = elseCandidate.get().getKey() + costSoFar;
                        Term t = new IfThenElse(tCond, thenCandidate.get().getValue(), elseCandidate.get().getValue());
                        minCostCandidate = Optional.of(new Pair<>(totalCost, t));
                    }
                }
            }
        }
        if (minCostCandidate.isPresent()) {
            return Optional.of(new Pair<>(minCostCandidate.get().getKey(), minCostCandidate.get().getValue()));
        } else {
            return Optional.empty();
        }
    }
}


