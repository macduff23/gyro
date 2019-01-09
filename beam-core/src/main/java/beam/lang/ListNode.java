package beam.lang;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends ValueNode<List> {

    private List<ValueNode> values;

    public List<ValueNode> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }

        return values;
    }

    @Override
    public void setParentNode(Node parentNode) {
        super.setParentNode(parentNode);

        for (ValueNode value : getValues()) {
            value.setParentNode(parentNode);
        }
    }

    @Override
    public List getValue() {
        List<String> list = new ArrayList();
        for (ValueNode value : getValues()) {
            Object item = value.getValue();
            if (item != null) {
                list.add(item.toString());
            } else {
                list.add(value.toString());
            }
        }

        return list;
    }

    @Override
    public ListNode copy() {
        ListNode listNode = new ListNode();

        for (ValueNode valueNode : getValues()) {
            listNode.getValues().add(valueNode.copy());
        }

        return listNode;
    }

    @Override
    public boolean resolve() {
        for (ValueNode value : getValues()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unabled to resolve configuration.", value);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[\n");

        List<String> out = new ArrayList<>();
        for (ValueNode value : getValues()) {
            out.add("    " + value.toString());
        }

        sb.append(StringUtils.join(out, ",\n"));
        sb.append("\n]\n");

        return sb.toString();
    }

}
