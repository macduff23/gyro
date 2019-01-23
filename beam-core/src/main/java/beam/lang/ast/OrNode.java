package beam.lang.ast;

import beam.parser.antlr4.BeamParser;

public class OrNode extends ExpressionNode {

    public OrNode(BeamParser.ExpressionContext context) {
        super(context);
    }

    @Override
    public Object evaluate(Scope scope) {
        Boolean leftValue = toBoolean(getLeftNode().evaluate(scope));
        Boolean rightValue = toBoolean(getRightNode().evaluate(scope));

        return leftValue || rightValue;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" or ");
        builder.append(getRightNode());
    }

}
