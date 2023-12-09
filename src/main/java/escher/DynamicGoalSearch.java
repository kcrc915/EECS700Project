package escher;

import escher.Synthesis.*;
import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import escher.BatchGoalSearch.*;
import escher.DynamicGoalSearch.ExecuteHoleException;
import escher.AscendRecSynthesizer.ExtendedCompImpl;

public class DynamicGoalSearch {
    private int maxCompCost;
    private ComponentSignature signature;
    private Set<ComponentImpl> envComps;
    private ArgListCompare argListCompare;
    private List<ArgList> inputVector;
    private CostAndVMToTerm termOfCostAndVM;
    private CostToTerms termsOfCost;
    private VMToBool boolOfVM;
    private String holeName;
    private List<Map<String, TermValue>> varMaps;
    private Map<String, ComponentImpl> envCompMap;

    public DynamicGoalSearch(int maxCompCost, ComponentSignature signature, Set<ComponentImpl> envComps,
                             ArgListCompare argListCompare, List<ArgList> inputVector,
                             CostAndVMToTerm termOfCostAndVM, CostToTerms termsOfCost, VMToBool boolOfVM) {
        this.maxCompCost = maxCompCost;
        this.signature = signature;
        this.envComps = envComps;
        this.argListCompare = argListCompare;
        this.inputVector = inputVector;
        this.termOfCostAndVM = termOfCostAndVM;
        this.termsOfCost = termsOfCost;
        this.boolOfVM = boolOfVM;
        this.holeName = "HOLE";
        this.varMaps = new ArrayList<>();
        for (int i = 0; i < inputVector.size(); i++) {
            Map<String, TermValue> varMap = new HashMap<>();
            for (int j = 0; j < signature.argNames.length; j++) {
                varMap.put(signature.argNames[j], inputVector.get(i).get(j));
            }
            this.varMaps.add(varMap);
        }
        this.envCompMap = new HashMap<>();
        for (ComponentImpl comp : envComps) {
            this.envCompMap.put(comp.name, comp);
        }
        ComponentImpl holeImpl = new ComponentImpl(holeName, new ArrayList<>(), signature.returnType, (args) -> {
            throw new ExecuteHoleException();
        });
        this.envCompMap.put(holeName, holeImpl);
    }

    public ComponentImpl assembleRecProgram(Term term) {
        return ComponentImpl.recursiveImpl(signature, envCompMap, argListCompare, term);
    }

    public SearchResult searchMin(int cost, IndexValueMap currentGoal, List<List<Pair<Term, ExtendedValueVec>>> recTermsOfReturnType,
                                  TermToTerm fillTermToHole, boolean isFirstBranch, Option<List<String>> prefixTrigger) {
        if (cost <= 0) {
            return null;
        }
        Set<Integer> keySet = currentGoal.keySet();
        for (int c = 1; c <= maxCompCost; c++) {
            Option<Term> termOption = termOfCostAndVM.apply(c, currentGoal);
            if (termOption.isDefined()) {
                return new FoundAtCost(c, termOption.get());
            }
            if (!isFirstBranch) {
                for (List<Pair<Term, ExtendedValueVec>> recTerms : recTermsOfReturnType.get(c - 1)) {
                    for (Pair<Term, ExtendedValueVec> pair : recTerms) {
                        Term term = pair.first();
                        ExtendedValueVec vv = pair.second();
                        ExtendedValueVec.MatchResult matchResult = vv.matchWithIndexValueMap(currentGoal);
                        if (matchResult == ExtendedValueVec.MatchResult.ExactMatch) {
                            return new FoundAtCost(c, term);
                        } else if (matchResult == ExtendedValueVec.MatchResult.PossibleMatch) {
                            boolean passCheck = true;
                            for (Pair<Integer, Value> pair2 : matchResult.leftToCheck()) {
                                int i = pair2.first();
                                Value desired = pair2.second();
                                ComponentImpl p = assembleRecProgram(fillTermToHole.apply(term));
                                if (!p.executeEfficient(inputVector.get(i)).equals(desired)) {
                                    passCheck = false;
                                    break;
                                }
                            }
                            if (passCheck) {
                                return new FoundAtCost(c, term);
                            }
                        }
                    }
                }
            }
        }
        int ifCost = 1;
        Pair<Integer, Term> minCostCandidate = null;
        for (int cThen = 1; cThen <= maxCompCost - ifCost - 2; cThen++) {
            for (Pair<Term, ExtendedValueVec> pair : termsOfCost.apply(cThen)) {
                Term tThen = pair.first();
                ExtendedValueVec thenVec = pair.second();
                Pair<Boolean, Option<List<String>>> result = checkTrigger(tThen, prefixTrigger);
                boolean trig = result.first();
                Option<List<String>> prefixTrigger1 = result.second();
                if (trig) {
                    System.out.println("trigger then branch!");
                }
                for (Triple<IndexValueMap, Pair<Integer, Term>, Set<Integer>> triple : IndexValueMap.splitValueMap(currentGoal, thenVec)) {
                    IndexValueMap vm = triple.first();
                    Pair<Integer, Term> pair2 = triple.second();
                    int cCond = pair2.first();
                    Term tCond = pair2.second();
                    Set<Integer> trueKeys = triple.third();
                    Pair<Boolean, Option<List<String>>> result2 = checkTrigger(tCond, prefixTrigger1);
                    boolean trig2 = result2.first();
                    Option<List<String>> prefixTrigger2 = result2.second();
                    if (trig2) {
                        System.out.println("trigger condition!");
                        int costSoFar = cThen + cCond + ifCost;
                        int maxCostForElse = Math.min(cost, minCostCandidate != null ? minCostCandidate.first() : Integer.MAX_VALUE) - costSoFar;
                        String target = "createNode(treeValue(@baseTree), tConcat(treeLeft(@baseTree), @inserted), tConcat(treeRight(@baseTree), @inserted))";
                        boolean found = false;
                        for (List<Pair<Term, ExtendedValueVec>> recTerms : recTermsOfReturnType.subList(0, maxCostForElse)) {
                            for (Pair<Term, ExtendedValueVec> pair3 : recTerms) {
                                if (pair3.first().show().equals(target)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                break;
                            }
                        }
                        System.out.println("found: " + found);
                    }
                    Term assembleTerm = fillTermToHole.apply(new If(tCond, tThen, holeName.$()));
                    int costSoFar = cThen + cCond + ifCost;
                    int maxCostForElse = Math.min(cost, minCostCandidate != null ? minCostCandidate.first() : Integer.MAX_VALUE) - costSoFar;
                    ComponentImpl partialImpl = ComponentImpl.recursiveImpl(signature, compMapWithHole, argListCompare, assembleTerm);
                    Map<String, ComponentImpl> envWithPartialImpl = new HashMap<>(envCompMap);
                    envWithPartialImpl.put(signature.name, partialImpl);
                    List<List<Pair<Term, ExtendedValueVec>>> newRecTermsOfCost = new ArrayList<>();
                    for (List<Pair<Term, ExtendedValueVec>> recTerms : recTermsOfReturnType.subList(0, maxCostForElse)) {
                        List<Pair<Term, ExtendedValueVec>> newRecTerms = new ArrayList<>();
                        for (Pair<Term, ExtendedValueVec> pair3 : recTerms) {
                            Term term = pair3.first();
                            ExtendedValueVec vv = pair3.second();
                            List<Value> newVV = new ArrayList<>();
                            for (int i = 0; i < vv.size(); i++) {
                                Value value = vv.get(i);
                                if (value instanceof ValueUnknown) {
                                    Map<String, TermValue> varMap = varMaps.get(i);
                                    try {
                                        newVV.add(Term.executeTerm(varMap, envWithPartialImpl, term));
                                    } catch (ExecuteHoleException e) {
                                        newVV.add(ValueUnknown.getInstance());
                                    }
                                } else {
                                    newVV.add((TermValue) value);
                                }
                            }
                            newRecTerms.add(new Pair<>(term, new ExtendedValueVec(newVV)));
                        }
                        newRecTermsOfCost.add(newRecTerms);
                    }
                    IndexValueMap elseGoal = currentGoal.remove(trueKeys);
                    Pair<Integer, Term> pair3 = searchMin(maxCostForElse, elseGoal, newRecTermsOfCost, assembleTerm,
                            false, prefixTrigger2);
                    if (pair3 != null) {
                        int totalCost = pair3.first() + costSoFar;
                        Term t = new If(tCond, tThen, pair3.second());
                        minCostCandidate = new Pair<>(totalCost, t);
                    }
                }
            }
        }
        if (minCostCandidate != null) {
            return new FoundAtCost(minCostCandidate.first(), minCostCandidate.second());
        } else {
            return new NotFoundUnderCost(cost);
        }
    }

    public Pair<Boolean, Option<List<String>>> checkTrigger(Term term, Option<List<String>> prefixTrigger) {
        if (prefixTrigger.isEmpty()) {
            return new Pair<>(false, Option.empty());
        } else {
            List<String> ts = prefixTrigger.get();
            if (ts.isEmpty()) {
                return new Pair<>(false, Option.empty());
            } else if (ts.size() == 1) {
                return new Pair<>(ts.get(0).equals(term.show()), Option.empty());
            } else {
                String h = ts.get(0);
                List<String> t = ts.subList(1, ts.size());
                if (term.show().equals(h)) {
                    return new Pair<>(false, Option.of(t));
                } else {
                    return new Pair<>(false, Option.empty());
                }
            }
        }
    }

    public static class ExecuteHoleException extends Exception {
    }
}


