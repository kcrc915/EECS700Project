package escher;


import static escher.DSL.listValue;

public class RunTypedEscher {
    public static class TestCase {
        private String name;



    }



        public void reverseSynthesis() {
            String ArgList;
            Object TermValue;
            IS<(ArgList, TermValue)>Object examples = IS(
                    argList(listValue()), listValue(),
                    argList(listValue(1, 2)), listValue(2, 1),
                    argList(listValue(1, 2, 3)), listValue(3, 2, 1)
            );
            ComponentImpl refComp = CommonComps.reverse;
            synthesize(refComp.name, IS(tyList(tyVar(0))), IS("xs"), tyList(tyVar(0)))(envComps = CommonComps.standardComps, examples, oracle = refComp.impl);
        }

    private Object listValue(int i, int i1) {
    }

    public void stutterSynthesis() {
            IS<(ArgList, TermValue)> examples = IS(
                    argList(listValue()), listValue(),
                    argList(listValue(5)), listValue(5, 5),
                    argList(listValue(5, 6, 3)), listValue(5, 5, 6, 6, 3, 3)
            );
            ComponentImpl refComp = CommonComps.stutter;
            synthesize(refComp.name, IS(tyList(tyVar(0))), IS("xs"), tyList(tyVar(0)))(envComps = CommonComps.standardComps, examples, oracle = refComp.impl);
        }

        public void cartesianSynthesis() {
            IS<(ArgList, TermValue)> examples = IS(
                    argList(listValue(), listValue(2, 3, 4)), listValue(),
                    argList(listValue(5), listValue()), listValue(),
                    argList(listValue(5), listValue(7, 8, 9)), listValue((5, 7), (5, 8), (5, 9)),
            argList(listValue(2, 3), listValue(4, 5)), listValue((2, 4), (2, 5), (3, 4), (3, 5))
            );
            ComponentImpl refComp = CommonComps.cartesian;
            synthesize("cartesian", IS(tyList(tyVar(0)), tyList(tyVar(1))), IS("xs", "ys"), tyList(tyPair(tyVar(0), tyVar(1))))(envComps = CommonComps.standardComps + CommonComps.createPair(tyFixVar(0), tyFixVar(1)), examples, oracle = refComp.impl);
        }

        public void squareListSynthesis() {
            IS<(ArgList, TermValue)> examples = IS(
                    argList(-3), listValue(),
                    argList(0), listValue(),
                    argList(1), listValue(1),
                    argList(2), listValue(1, 4),
                    argList(3), listValue(1, 4, 9),
                    argList(4), listValue(1, 4, 9, 16)
            );
            ComponentImpl refComp = CommonComps.squareList;
            synthesize("squareList", IS(tyInt), IS("n"), tyList(tyInt))(envComps = CommonComps.standardComps ++ CommonComps.timesAndDiv, examples, oracle = refComp.impl);
        }
    }


}
