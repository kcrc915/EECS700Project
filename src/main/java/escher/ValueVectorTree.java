package escher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A tree with fixed depth, each path from root to leaf represents a ValueVector,
 * can be used to efficiently find ValueVectors that match a goal (partial vector)
 */
public class ValueVectorTree<A> {
    private int depth;
    private double thresholdToUseTree;
    private int size;
    private InternalNode<A> root;
    private Map<ValueVector, A> valueTermMap;

    /**
     * A tree with fixed depth, each path from root to leaf represents a ValueVector,
     * can be used to efficiently find ValueVectors that match a goal (partial vector)
     *
     * @param depth              the length of each ValueVector
     * @param thresholdToUseTree
     */
    public ValueVectorTree(int depth, double thresholdToUseTree) {
        this.depth = depth;
        this.thresholdToUseTree = thresholdToUseTree;
        this.size = 0;
        this.root = new InternalNode<>(new HashMap<>());
        this.valueTermMap = new HashMap<>();
    }

    public int getSize() {
        return size;
    }

    public void addTerm(A term, ValueVector valueVector) {
        if (shouldUseTree()) {
            boolean added = root.addTerm(term, valueVector.toList());
            if (added) {
                size++;
                valueTermMap.put(valueVector, term);
            }
        } else {
            if (!valueTermMap.containsKey(valueVector)) {
                root.addTerm(term, valueVector.toList());
                size++;
                valueTermMap.put(valueVector, term);
            }
        }
    }

    public void update(ValueVector valueVector, A term) {
        addTerm(term, valueVector);
    }

    private List<Either<Unit, TermValue>> valueMapToVector(IndexValueMap valueMap) {
        List<Either<Unit, TermValue>> vector = new ArrayList<>();
        for (int i = 0; i < depth; i++) {
            if (valueMap.containsKey(i)) {
                vector.add(Right(valueMap.get(i)));
            } else {
                vector.add(Left(()));
            }
        }
        return vector;
    }

    public Iterator<A> searchTerms(IndexValueMap valueMap) {
        if (shouldUseTree()) {
            List<Either<Unit, TermValue>> vv = valueMapToVector(valueMap);
            return root.searchTerms(vv);
        } else {
            List<A> terms = new ArrayList<>();
            for (Map.Entry<ValueVector, A> entry : valueTermMap.entrySet()) {
                ValueVector vv = entry.getKey();
                A term = entry.getValue();
                if (IndexValueMap.matchVector(valueMap, vv)) {
                    terms.add(term);
                }
            }
            return terms.iterator();
        }
    }

    public Optional<A> searchATerm(IndexValueMap valueMap) {
        if (shouldUseTree()) {
            List<Either<Unit, TermValue>> vv = valueMapToVector(valueMap);
            return root.searchATerm(vv);
        } else {
            for (Map.Entry<ValueVector, A> entry : valueTermMap.entrySet()) {
                ValueVector vv = entry.getKey();
                A term = entry.getValue();
                if (IndexValueMap.matchVector(valueMap, vv)) {
                    return Optional.of(term);
                }
            }
            return Optional.empty();
        }
    }

    public Optional<A> get(ValueVector valueVector) {
        return Optional.ofNullable(valueTermMap.get(valueVector));
    }

    public void printRoot(Function<A, String> show) {
        System.out.println("---Root Status---");
        print(root, show, 0);
    }

    private void print(TreeNode<A> tree, Function<A, String> show, int indent) {
        if (tree instanceof LeafNode) {
            LeafNode<A> leafNode = (LeafNode<A>) tree;
            System.out.println("  ".repeat(indent) + "* " + show.apply(leafNode.getTerm()));
        } else if (tree instanceof InternalNode) {
            InternalNode<A> internalNode = (InternalNode<A>) tree;
            for (Map.Entry<TermValue, TreeNode<A>> entry : internalNode.getChildren().entrySet()) {
                TermValue tv = entry.getKey();
                TreeNode<A> tree1 = entry.getValue();
                System.out.println("  ".repeat(indent) + "- " + tv.show());
                print(tree1, show, indent + 1);
            }
        }
    }

    private boolean shouldUseTree() {
        return (double) size / depth >= thresholdToUseTree;
    }

    private interface TreeNode<A> {
    }

    private static class LeafNode<A> implements TreeNode<A> {
        private A term;

        public LeafNode(A term) {
            this.term = term;
        }

        public A getTerm() {
            return term;
        }
    }

    private static class InternalNode<A> implements TreeNode<A> {
        private Map<TermValue, TreeNode<A>> children;

        public InternalNode(Map<TermValue, TreeNode<A>> children) {
            this.children = children;
        }

        public Map<TermValue, TreeNode<A>> getChildren() {
            return children;
        }

        public boolean addTerm(A term, List<TermValue> valueVector) {
            if (valueVector.isEmpty()) {
                throw new Exception("Empty valueVector!");
            } else if (valueVector.size() == 1) {
                TermValue v = valueVector.get(0);
                if (children.containsKey(v)) {
                    return false;
                } else {
                    children.put(v, new LeafNode<>(term));
                    return true;
                }
            } else {
                TermValue v = valueVector.get(0);
                TreeNode<A> n1 = children.get(v);
                if (n1 instanceof InternalNode) {
                    n1 = (InternalNode<A>) n1;
                } else if (n1 == null) {
                    n1 = new InternalNode<>(new HashMap<>());
                } else {
                    throw new Exception();
                }
                children.put(v, n1);
                return n1.addTerm(term, valueVector.subList(1, valueVector.size()));
            }
        }

        public Iterator<A> searchTerms(List<Either<Unit, TermValue>> valueVector) {
            if (valueVector.isEmpty()) {
                throw new Exception("Empty valueVector!");
            } else if (valueVector.size() == 1) {
                Either<Unit, TermValue> v = valueVector.get(0);
                if (v.isLeft()) {
                    List<A> terms = new ArrayList<>();
                    for (TreeNode<A> child : children.values()) {
                        if (child instanceof LeafNode) {
                            terms.add(((LeafNode<A>) child).getTerm());
                        }
                    }
                    return terms.iterator();
                } else {
                    TermValue tv = v.getRight();
                    TreeNode<A> n1 = children.get(tv);
                    if (n1 instanceof LeafNode) {
                        return Collections.singleton(((LeafNode<A>) n1).getTerm()).iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                }
            } else {
                Either<Unit, TermValue> v = valueVector.get(0);
                if (v.isLeft()) {
                    List<A> terms = new ArrayList<>();
                    for (TreeNode<A> child : children.values()) {
                        if (child instanceof InternalNode) {
                            terms.addAll(((InternalNode<A>) child).searchTerms(valueVector.subList(1, valueVector.size())));
                        }
                    }
                    return terms.iterator();
                } else {
                    TermValue tv = v.getRight();
                    TreeNode<A> n1 = children.get(tv);
                    if (n1 instanceof InternalNode) {
                        return ((InternalNode<A>) n1).searchTerms(valueVector.subList(1, valueVector.size()));
                    } else {
                        return Collections.emptyIterator();
                    }
                }
            }
        }

        public Optional<A> searchATerm(List<Either<Unit, TermValue>> valueVector) {
            if (valueVector.isEmpty()) {
                throw new Exception("Empty valueVector!");
            } else if (valueVector.size() == 1) {
                Either<Unit, TermValue> v = valueVector.get(0);
                if (v.isLeft()) {
                    for (TreeNode<A> child : children.values()) {
                        if (child instanceof LeafNode) {
                            return Optional.of(((LeafNode<A>) child).getTerm());
                        }
                    }
                    return Optional.empty();
                } else {
                    TermValue tv = v.getRight();
                    TreeNode<A> n1 = children.get(tv);
                    if (n1 instanceof LeafNode) {
                        return Optional.of(((LeafNode<A>) n1).getTerm());
                    } else {
                        return Optional.empty();
                    }
                }
            } else {
                Either<Unit, TermValue> v = valueVector.get(0);
                if (v.isLeft()) {
                    for (TreeNode<A> child : children.values()) {
                        if (child instanceof InternalNode) {
                            Optional<A> term = ((InternalNode<A>) child).searchATerm(valueVector.subList(1, valueVector.size()));
                            if (term.isPresent()) {
                                return term;
                            }
                        }
                    }
                    return Optional.empty();
                } else {
                    TermValue tv = v.getRight();
                    TreeNode<A> n1 = children.get(tv);
                    if (n1 instanceof InternalNode) {
                        return ((InternalNode<A>) n1).searchATerm(valueVector.subList(1, valueVector.size()));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }
    }
}


