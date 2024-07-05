package org.c2graph.model.binding;

import lombok.Data;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;

import java.util.Objects;

@Data
public class AstBinding {
    private int initialSize;
    private IASTName name;

    // use prefix sum to check if this is end.
    private int value;
    private String type;

    private Boolean isLiteral;
    private String literalValue;

    private Boolean isCollection;
    // pointer analysis check
    private String otherOperation;

    // high precision check
    private String defineOperationLike = "none";

    public void addValue(int cur) {
        this.value += cur;
    }

    public AstBinding() {
        this.isLiteral = false;
        this.isCollection = false;
    }

    public AstBinding(IASTName name, IASTExpression expr) {
        this.name = name;
        this.value = 0;

        try {
            if (expr != null) {
                this.type = expr.getExpressionType().toString();
            }
        } catch (Exception ignores)  {

        }

        this.isLiteral = false;
        this.isCollection = false;
    }

    public AstBinding(String literalValue, IASTExpression expr) {
        this.literalValue = literalValue;
        this.value = 0;

        try {
            if (expr != null) {
                this.type = expr.getExpressionType().toString();
            }
        } catch (Exception ignores)  {

        }

        this.isLiteral = true;
        this.isCollection = false;
    }

    public AstBinding(IASTName name, int value, IASTExpression expr) {
        this.name = name;
        this.value = value;

        try {
            if (expr != null) {
                this.type = expr.getExpressionType().toString();
            }
        } catch (Exception ignores)  {

        }

        this.isLiteral = false;
        this.isCollection = false;
    }

    public boolean isPointer() {
        if (this.type == null) {
            return false;
        }
        return this.type.endsWith("*") || this.type.endsWith("]");
    }

    @Override
    public String toString() {
        if (name == null) {
            return literalValue;
        }
        return name.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AstBinding that)) return false;
        if (name != null)
            return value == that.value && Objects.equals(name.toString(), that.name.toString())
                    && Objects.equals(type, that.type)
                    && Objects.equals(isLiteral, that.isLiteral)
                    && Objects.equals(literalValue, that.literalValue)
                    && Objects.equals(isCollection, that.isCollection);
        return value == that.value
                && Objects.equals(type, that.type)
                && Objects.equals(isLiteral, that.isLiteral)
                && Objects.equals(literalValue, that.literalValue)
                && Objects.equals(isCollection, that.isCollection);
    }

    @Override
    public int hashCode() {
        if (name != null)
            return Objects.hash(name.toString(), value, type, isLiteral, literalValue, isCollection);
        return Objects.hash(value, type, isLiteral, literalValue, isCollection);
    }
}