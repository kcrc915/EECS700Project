package escher;

import escher.Synthesis.IndexValueMap;
import escher.Synthesis.ValueVector;

public class ImmutableGoalGraph {
    public interface GraphNode {
        String show(int exampleCount);
        boolean isSolved();
        Term toTerm();
    }

    public interface SolvedNode extends GraphNode {}

    public static class Unsolved implements GraphNode {
        private final IndexValueMap valueMap;
        private final Set<Resolver> children;

        public Unsolved(IndexValueMap valueMap, Set<Resolver> children) {
            this.valueMap = valueMap;
            this.children = children;
        }

        public String show(int exampleCount) {
            return "Unsolved(" + IndexValueMap.show(valueMap, exampleCount) + ")";
        }

        public boolean isSolved() {
            return false;
        }

        public Term toTerm() {
            throw new Exception("Unsolved goals can't be convert to Term");
        }
    }

    public static class SolvedByTerm implements SolvedNode {
        private final Term term;

        public SolvedByTerm(Term term) {
            this.term = term;
        }

        public String show(int exampleCount) {
            return "SolvedByTerm(" + term.show() + ")";
        }

        public boolean isSolved() {
            return true;
        }

        public Term toTerm() {
            return term;
        }
    }

    public static class SolvedByResolver implements SolvedNode {
        private final Resolver resolver;

        public SolvedByResolver(Resolver resolver) {
            this.resolver = resolver;
        }

        public String show(int exampleCount) {
            return "SolvedByResolver(" + resolver.show() + ")";
        }

        public boolean isSolved() {
            return true;
        }

        public Term toTerm() {
            return resolver.cond.toTerm() ? resolver.thenBranch.toTerm() : resolver.elseBranch.toTerm();
        }
    }

    public static class Resolver {
        private final IndexValueMap condMap;
        private final GraphNode cond;
        private final SolvedNode thenBranch;
        private final GraphNode elseBranch;

        public Resolver(IndexValueMap condMap, GraphNode cond, SolvedNode thenBranch, GraphNode elseBranch) {
            this.condMap = condMap;
            this.cond = cond;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        public boolean isSolved() {
            return cond.isSolved() && elseBranch.isSolved();
        }

        public List<GraphNode> condAndElse() {
            return Arrays.asList(cond, elseBranch);
        }

        public String show() {
            return "IF " + cond.show() + " THEN " + thenBranch.show() + " ELSE " + elseBranch.show();
        }
    }

    public static class GoalManager {
        private GraphNode root;
        private final IndexValueMap initGoal;
        private final Function<IndexValueMap, Optional<Term>> boolLibrary;
        private final Function<IndexValueMap, Optional<Term>> valueLibrary;
        private final int exampleCount;
        private final BiConsumer<Integer, String> printer;

        public GoalManager(IndexValueMap initGoal, Function<IndexValueMap, Optional<Term>> boolLibrary,
                           Function<IndexValueMap, Optional<Term>> valueLibrary, int exampleCount,
                           BiConsumer<Integer, String> printer) {
            this.initGoal = initGoal;
            this.boolLibrary = boolLibrary;
            this.valueLibrary = valueLibrary;
            this.exampleCount = exampleCount;
            this.printer = printer;
            this.root = new Unsolved(initGoal, new HashSet<>());
        }

        public void insertNewTerm(ValueVector valueVector, Term term) {
            GraphNode closeGoals(Unsolved unsolved) {
                if (IndexValueMap.matchVector(unsolved.valueMap, valueVector)) {
                    return new SolvedByTerm(term);
                } else {
                    Set<Resolver> newChildren = unsolved.children.stream().map(r -> {
                        List<GraphNode> condAndElse = r.condAndElse().stream().map(g -> {
                            if (g instanceof Unsolved) {
                                return closeGoals((Unsolved) g);
                            } else {
                                return g;
                            }
                        }).collect(Collectors.toList());
                        Resolver r1 = new Resolver(r.condMap, condAndElse.get(0), r.thenBranch, condAndElse.get(1));
                        if (r1.isSolved()) {
                            return new SolvedByResolver(r1);
                        } else {
                            return r1;
                        }
                    }).collect(Collectors.toSet());
                    return new Unsolved(unsolved.valueMap, newChildren);
                }
            }

            GraphNode splitGoal(Unsolved unsolved) {
                Optional<Tuple3<IndexValueMap, ValueVector, IndexValueMap>> splitResult = IndexValueMap.splitValueMap(unsolved.valueMap, valueVector);
                if (!splitResult.isPresent()) {
                    return unsolved;
                } else {
                    Tuple3<IndexValueMap, ValueVector, IndexValueMap> split = splitResult.get();
                    if (!unsolved.children.stream().allMatch(r -> r.condMap != split._1())) {
                        return new Unsolved(unsolved.valueMap, new HashSet<>());
                    } else {
                        GraphNode gCond = boolLibrary.apply(split._1()).map(SolvedByTerm::new).orElseGet(() -> new Unsolved(split._1(), new HashSet<>()));
                        SolvedNode gb1 = new SolvedByTerm(term);
                        GraphNode gb2 = valueLibrary.apply(split._3()).map(SolvedByTerm::new).orElseGet(() -> new Unsolved(split._3(), new HashSet<>()));
                        Resolver r = new Resolver(split._1(), gCond, gb1, gb2);
                        if (r.isSolved()) {
                            return new SolvedByResolver(r);
                        } else {
                            Set<Resolver> newChildren = unsolved.children.stream().map(this::splitResolver).collect(Collectors.toSet());
                            newChildren.add(r);
                            return new Unsolved(unsolved.valueMap, newChildren);
                        }
                    }
                }
            }

            Resolver splitResolver(Resolver resolver) {
                if (resolver.elseBranch instanceof Unsolved) {
                    return new Resolver(resolver.condMap, splitGoal((Unsolved) resolver.elseBranch), resolver.thenBranch, resolver.elseBranch);
                } else {
                    return resolver;
                }
            }

            if (root instanceof Unsolved) {
                GraphNode closeGoalsResult = closeGoals((Unsolved) root);
                if (closeGoalsResult instanceof Unsolved) {
                    root = splitGoal((Unsolved) closeGoalsResult);
                } else {
                    root = closeGoalsResult;
                }
            } else {
                throw new Exception("Root goal has already been solved.");
            }
        }

        public void printResolver(Resolver resolver, int indent) {
            printer.accept(indent, "Resolver(condition = " + IndexValueMap.show(resolver.condMap, exampleCount) + ")\n");
            resolver.condAndElse().forEach(g -> printGraphNode(g, indent + 1));
        }

        public void printGraphNode(GraphNode g, int indent) {
            if (g instanceof Unsolved) {
                printer.accept(indent, "Unsolved: " + IndexValueMap.show(((Unsolved) g).valueMap, exampleCount) + "\n");
                ((Unsolved) g).children.forEach(r -> printResolver(r, indent + 1));
            } else if (g instanceof SolvedByTerm) {
                printer.accept(indent, "SolvedByTerm(" + ((SolvedByTerm) g).term.show() + ")\n");
            } else if (g instanceof SolvedByResolver) {
                printer.accept(indent, "SolvedByResolver: \n");
                printResolver(((SolvedByResolver) g).resolver, indent + 1);
            }
        }

        public void printState() {
            printer.accept(0, "Goal Manager State:\n");
            printGraphNode(root, 1);
        }

        public Term synthesizedProgram() {
            return root.toTerm();
        }
    }
}


