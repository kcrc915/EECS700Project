package escher;

import escher.AscendRecSynthesizer.*;
import DSL.*;
import Synthesis.*;
import escher.CommonComps.ReducibleCheck;

/**
 * Run AscendRec on a benchmark suit.
 */
public class RunAscendRec {
    public static void main(String[] args) {
        AscendRecSynthesizer syn = new AscendRecSynthesizer(
                new Config(15, true, false, 3),
                System.out::print
        );

        synthesizeUsingRef(CommonComps.length, IS("xs"), IS(
                argList(listValue(2, 3, 4)),
                argList(listValue(1)),
                argList(listValue())
        ));

        synthesizeUsingRef(CommonComps.reverse, IS("xs"), IS(
                argList(listValue(2, 3, 4)),
                argList(listValue(2, 3)),
                argList(listValue(1)),
                argList(listValue())
        ));

        synthesizeUsingRef(CommonComps.stutter, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(5)),
                argList(listValue(5, 6, 3))
        ));

        synthesizeUsingRef(CommonComps.contains, IS("xs", "x"), IS(
                argList(listValue(1, 2, 3), 1),
                argList(listValue(1, 2, 3), 2),
                argList(listValue(1, 2, 3), 3),
                argList(listValue(1, 2, 3), 4),
                argList(listValue(1, 2, 3), -1),
                argList(listValue(1, 2), 3),
                argList(listValue(), 1)
        ));

        synthesizeUsingRef(CommonComps.insert, IS("xs", "i", "x"), IS(
                argList(listValue(), 0, 5),
                argList(listValue(), 3, 5),
                argList(listValue(3), -1, 1),
                argList(listValue(1, 2, 3), 0, 8),
                argList(listValue(1, 2, 3), 1, 8),
                argList(listValue(1, 2, 3), 2, 8),
                argList(listValue(1, 2, 3), 3, 8),
                argList(listValue(1, 2, 3), 4, 8)
        ));

        synthesizeUsingRef(CommonComps.fib, IS("n"), IS(
                argList(-3),
                argList(0),
                argList(1),
                argList(2),
                argList(3),
                argList(4),
                argList(5),
                argList(6)
        ));

        synthesizeUsingRef(CommonComps.squareList, IS("n"), IS(
                argList(-3),
                argList(0),
                argList(1),
                argList(2),
                argList(3),
                argList(4)
        ));

        import BinaryTree.*;
        synthesizeUsingRef(CommonComps.nodesAtLevel, IS("tree", "level"), IS(
                argList(BinaryLeaf, 1),
                argList(BinaryLeaf, -1),
                argList(singleNode(1), -1),
                argList(singleNode(1), 0),
                argList(singleNode(1), 1),
                argList(singleNode(1), 2),
                argList(BinaryNode(1, singleNode(7), singleNode(9)), 1),
                argList(BinaryNode(1, BinaryNode(15, singleNode(4), BinaryLeaf), singleNode(9)), 3),
                argList(BinaryNode(1, singleNode(2), BinaryNode(3, singleNode(4), BinaryNode(5, singleNode(6), singleNode(7)))), 0),
                argList(BinaryNode(1, singleNode(2), BinaryNode(3, singleNode(4), BinaryNode(5, singleNode(6), singleNode(7)))), 1),
                argList(BinaryNode(1, singleNode(2), BinaryNode(3, singleNode(4), BinaryNode(5, singleNode(6), singleNode(7)))), 2),
                argList(BinaryNode(1, singleNode(2), BinaryNode(3, singleNode(4), BinaryNode(5, singleNode(6), singleNode(7)))), 3)
        ));

        synthesizeUsingRef(CommonComps.dropLast, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(1, 2)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 1, 1, 2, 3, 2))
        ));

        synthesizeUsingRef(CommonComps.evens, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(1, 2)),
                argList(listValue(1, 2, 3, 4)),
                argList(listValue(1, 2, 3, 4, 5, 6))
        ));

        synthesizeUsingRef(CommonComps.lastInList, IS("xs"), IS(
                argList(listValue(1)),
                argList(listValue(1, 2)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 3, 7, 9))
        ));

        synthesizeUsingRef(CommonComps.tConcat, IS("baseTree", "inserted"), IS(
                argList(BinaryLeaf, BinaryLeaf),
                argList(BinaryLeaf, singleNode(1)),
                argList(singleNode(1), BinaryLeaf),
                argList(singleNode(1), BinaryNode(2, singleNode(3), singleNode(4))),
                argList(BinaryNode(1, singleNode(2), singleNode(3)), BinaryNode(4, singleNode(5), singleNode(6))),
                argList(BinaryNode(1, BinaryLeaf, BinaryNode(2, singleNode(3), singleNode(4))), singleNode(5))
        ));

        synthesizeUsingRef(CommonComps.cartesian, IS("xs", "ys"), IS(
                argList(listValue(), listValue(2, 3, 4)),
                argList(listValue(5), listValue()),
                argList(listValue(5), listValue(7, 8, 9)),
                argList(listValue(2, 3), listValue(4, 5))
        ));

        synthesizeUsingRef(CommonComps.times, IS("x", "y"), IS(
                argList(1, 0),
                argList(0, 5),
                argList(2, 7),
                argList(3, 8),
                argList(0, 8),
                argList(7, 5)
        ));

        synthesizeUsingRef(CommonComps.sumUnder, IS("n"), IS(
                argList(0),
                argList(1),
                argList(2),
                argList(3),
                argList(4)
        ));

        synthesizeUsingRef(CommonComps.flattenTree, IS("tree"), IS(
                argList(BinaryLeaf),
                argList(singleNode(1)),
                argList(BinaryNode(1, singleNode(2), singleNode(3)))
        ));

        synthesizeUsingRef(CommonComps.maxInList, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(3)),
                argList(listValue(0, 2, 1)),
                argList(listValue(1, 6, 2, 5)),
                argList(listValue(1, 6, 7, 5)),
                argList(listValue(10, 25, 7, 9, 18)),
                argList(listValue(100, 25, 7, 9, 18))
        ));

        synthesizeUsingRef(CommonComps.lastInList, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 6, 7, 11)),
                argList(listValue(10, 25, 7, 9, 18))
        ));

        synthesizeUsingRef(CommonComps.shiftLeft, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 6, 7, 11)),
                argList(listValue(10, 25, 7, 9, 18))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(2)),
                argList(listValue(2, 3, 3, 9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(3, 3, 9)),
                argList(listValue(3, 3, 9, 9)),
                argList(listValue(3, 9)),
                argList(listValue(3, 9, 9)),
                argList(listValue(7)),
                argList(listValue(9)),
                argList(listValue(9, 2)),
                argList(listValue(9, 9)),
                argList(listValue(9, 9, 2))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));

        synthesizeUsingRef(CommonComps.compress, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(7)),
                argList(listValue(3, 9)),
                argList(listValue(9, 9)),
                argList(listValue(2, 3, 9)),
                argList(listValue(9, 9, 2)),
                argList(listValue(3, 3, 3, 9)),
                argList(listValue(2, 3, 3, 9, 9))
        ));

        synthesizeUsingRef(CommonComps.dedup, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(1)),
                argList(listValue(3, 3)),
                argList(listValue(2, 3)),
                argList(listValue(1, 2, 3)),
                argList(listValue(1, 2, 3, 2)),
                argList(listValue(1, 1, 1, 2, 3, 2)),
                argList(listValue(2, 2, 2, 3, 3, 3)),
                argList(listValue(1, 2, 3, 2, 1))
        ));

        synthesizeUsingRef(CommonComps.sortInts, IS("xs"), IS(
                argList(listValue()),
                argList(listValue(9)),
                argList(listValue(12)),
                argList(listValue(4)),
                argList(listValue(9, 12)),
                argList(listValue(12, 9)),
                argList(listValue(12, 4)),
                argList(listValue(4, 12)),
                argList(listValue(9, 4)),
                argList(listValue(4, 9)),
                argList(listValue(9, 12, 4))
        ));