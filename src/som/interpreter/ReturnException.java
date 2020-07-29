package som.interpreter;

import com.oracle.truffle.api.nodes.ControlFlowException;
import som.vmobjects.SAbstractObject;

public class ReturnException extends ControlFlowException {

    private static final long serialVersionUID = 6317360573982456256L;
    private final SAbstractObject result;
    private final Frame outerContext;

    public ReturnException(SAbstractObject result, Frame target) {
        this.result = result;
        this.outerContext = target;
    }

    public SAbstractObject getResult() {
        return result;
    }

    public Frame getTarget() { return this.outerContext; }

    public boolean hasReachedTarget(Frame current) {
        return current == outerContext;
    }
}

