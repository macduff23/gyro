package gyro.lang.ast.control;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.BlockNode;
import gyro.parser.antlr4.GyroParser;

import java.util.List;
import java.util.stream.Collectors;

public class ForNode extends BlockNode {

    private final List<String> variables;
    private final Node value;

    public ForNode(List<String> variables, Node value, List<Node> body) {
        super(body);

        this.variables = ImmutableList.copyOf(Preconditions.checkNotNull(variables));
        this.value = Preconditions.checkNotNull(value);
    }

    public ForNode(GyroParser.ForStatementContext context) {
        this(
            Preconditions.checkNotNull(context)
                .forVariable()
                .stream()
                .map(c -> c.IDENTIFIER().getText())
                .collect(Collectors.toList()),

            Node.create(context.forValue().getChild(0)),

            context.blockBody()
                .blockStatement()
                .stream()
                .map(c -> Node.create(c.getChild(0)))
                .collect(Collectors.toList()));

    }

    public List<String> getVariables() {
        return variables;
    }

    public Node getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitFor(this, context);
    }

}
