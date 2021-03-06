package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyArray;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is an internal ruby array generated during multiple assignment expressions.
// FIXME: is this array subject to monkey-patching of Array methods?
// i.e. if I override the elt accessor method [], will multiple-assignment
// semantics change as well?
//
// FIXME: Rename GetArrayInstr to ArrayArefInstr which would be used
// in later passes as well when compiler passes replace ruby-array []
// calls with inlined lookups
public class GetArrayInstr extends OneOperandInstr {
    private final int index;
    private final boolean all;  // If true, returns the rest of the array starting at the index

    public GetArrayInstr(Variable dest, Operand array, int index, boolean getRestOfArray) {
        super(Operation.GET_ARRAY, dest, array);
        this.index = index;
        all = getRestOfArray;
    }

    @Override
    public String toString() {
        return "" + getResult() + " = " + argument + "[" + index + (all ? ":END" : "") + "] (GET_ARRAY)";
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        Operand val = argument.getValue(valueMap);
        return val.fetchCompileTimeArrayElement(index, all);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetArrayInstr(ii.getRenamedVariable(getResult()), argument.cloneForInlining(ii), index, all);
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray array = (RubyArray) getArg().retrieve(interp, context, self);
        Object val;
        
        if (!all) {
            val = array.entry(index);
        } else {
            int n = array.getLength();
            int size = n - index;
            if (size <= 0) {
                val = RubyArray.newEmptyArray(context.getRuntime());
            } else {
                // FIXME: Perf win to use COW between source Array and this new one (remove toJavaArray)
                val = RubyArray.newArrayNoCopy(context.getRuntime(), array.toJavaArray(), index);
            }
        }
        getResult().store(interp, context, self, val);
        return null;
    }
}
