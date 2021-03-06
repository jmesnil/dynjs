package org.dynjs.codegen;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.util.List;

import me.qmx.jitescript.CodeBlock;

import org.dynjs.codegen.AbstractCodeGeneratingVisitor.Arities;
import org.dynjs.parser.ast.Expression;
import org.dynjs.parser.ast.FunctionCallExpression;
import org.dynjs.parser.ast.NewOperatorExpression;
import org.dynjs.runtime.BlockManager;
import org.dynjs.runtime.EnvironmentRecord;
import org.dynjs.runtime.ExecutionContext;
import org.dynjs.runtime.JSFunction;
import org.dynjs.runtime.JSObject;
import org.dynjs.runtime.Reference;
import org.dynjs.runtime.ReferenceContext;
import org.dynjs.runtime.Types;
import org.dynjs.runtime.linker.DynJSBootstrapper;
import org.objectweb.asm.tree.LabelNode;

public class InvokeDynamicBytecodeGeneratingVisitor extends BasicBytecodeGeneratingVisitor {
    
    public InvokeDynamicBytecodeGeneratingVisitor(BlockManager blockManager) {
        super(blockManager);
    }
    
    @Override
    public void visit(ExecutionContext context, NewOperatorExpression expr, boolean strict) {
        LabelNode end = new LabelNode();
        // 11.2.2

        aload(Arities.EXECUTION_CONTEXT);
        // context
        invokevirtual(p(ExecutionContext.class), "incrementPendingConstructorCount", sig(void.class));
        // <empty>

        expr.getExpr().accept(context, this, strict);
        // obj

        aload(Arities.EXECUTION_CONTEXT);
        // obj context
        invokevirtual(p(ExecutionContext.class), "getPendingConstructorCount", sig(int.class));
        // obj count
        iffalse(end);

        // obj
        aload(Arities.EXECUTION_CONTEXT);
        // obj context
        swap();
        // context obj
        append(jsGetValue());
        // context ctor-fn
        swap();
        // ctor-fn context

        bipush(0);
        anewarray(p(Object.class));
        // ctor-fn context array
        
        invokedynamic("dyn:new", sig(Object.class, Object.class, ExecutionContext.class, Object[].class), DynJSBootstrapper.BOOTSTRAP, DynJSBootstrapper.BOOTSTRAP_ARGS);
        // obj

        label(end);
        nop();
    }
    
    @Override
    public void visit(ExecutionContext context, FunctionCallExpression expr, boolean strict) {
        LabelNode propertyRef = new LabelNode();
        LabelNode noSelf = new LabelNode();
        LabelNode doCall = new LabelNode();
        LabelNode isCallable = new LabelNode();
        // 11.2.3
        
        expr.getMemberExpression().accept(context, this, strict);
        // ref
        dup();
        // ref ref
        append(jsGetValue());
        // ref function

        swap();
        // function ref
        dup();
        // function ref ref
        instance_of(p(Reference.class));
        // function ref isref?
        iffalse(noSelf);

        // ----------------------------------------
        // Reference

        // function ref
        checkcast(p(Reference.class));
        dup();
        // function ref ref
        invokevirtual(p(Reference.class), "isPropertyReference", sig(boolean.class));
        // function ref bool(is-prop)

        iftrue(propertyRef);

        // ----------------------------------------
        // Environment Record

        // function ref
        dup();
        // function ref ref
        append(jsGetBase());
        // function ref base
        checkcast(p(EnvironmentRecord.class));
        // function ref env-rec
        invokeinterface(p(EnvironmentRecord.class), "implicitThisValue", sig(Object.class));
        // function ref self
        go_to(doCall);

        // ----------------------------------------
        // Property Reference
        label(propertyRef);
        // function ref
        dup();
        // function ref ref
        append(jsGetBase());
        // function ref self
        go_to(doCall);

        // ------------------------------------------
        // No self
        label(noSelf);
        // function ref
        pop();
        // function
        aconst_null();
        // function ref
        append(jsPushUndefined());
        // function ref UNDEFINED

        // ------------------------------------------
        // call()

        label(doCall);
        // function ref self

        aload(Arities.EXECUTION_CONTEXT);
        // function ref self context
        
        dup_x2();
        // function context ref self context
        
        invokevirtual(p(ExecutionContext.class), "pushCallContext", sig(void.class));
        // function context ref self

        List<Expression> argExprs = expr.getArgumentExpressions();
        int numArgs = argExprs.size();
        bipush(numArgs);
        anewarray(p(Object.class));
        // function context ref self array
        for (int i = 0; i < numArgs; ++i) {
            dup();
            bipush(i);

            argExprs.get(i).accept(context, this, strict);
            append(jsGetValue());
            aastore();
        }
        // function context ref self args

        aload(Arities.EXECUTION_CONTEXT);
        // function context ref self array context
        invokevirtual(p(ExecutionContext.class), "popCallContext", sig(void.class));
        // function context ref self args

        // function context ref self args
        invokedynamic("dyn:call", sig(Object.class, Object.class, ExecutionContext.class, Object.class, Object.class, Object[].class), DynJSBootstrapper.BOOTSTRAP, DynJSBootstrapper.BOOTSTRAP_ARGS);
        
        // call ExecutionContext#call(fn, self, args) -> Object
        //invokevirtual(p(ExecutionContext.class), "call", sig(Object.class, Object.class, JSFunction.class, Object.class, Object[].class));
        
        // dyn:call (fn, context, ref, self, args)obj

        // obj
    }

    @Override
    public CodeBlock jsGetValue(final Class<?> throwIfNot) {
        return new CodeBlock() {
            {
                // IN: reference
                LabelNode end = new LabelNode();
                LabelNode throwRef = new LabelNode();
                
                dup();
                // ref ref
                instance_of( p(Reference.class) );
                // ref isref?
                iffalse(end);
                checkcast(p(Reference.class));
                // ref
                dup();
                // ref ref
                invokevirtual(p(Reference.class), "isUnresolvableReference", sig( boolean.class ) );
                // ref unresolv?
                iftrue( throwRef );
                // ref
                dup();
                // ref ref
                invokevirtual(p(Reference.class), "getBase", sig(Object.class));
                // ref obj
                swap();
                // obj ref
                dup();
                // obj ref ref
                invokevirtual(p(Reference.class), "getReferencedName", sig(String.class));
                // obj ref name
                swap();
                // obj name ref
                aload( Arities.EXECUTION_CONTEXT );
                // obj name ref context
                invokestatic(p(ReferenceContext.class), "create", sig(ReferenceContext.class, Reference.class, ExecutionContext.class) );
                // obj name context
                swap();
                // obj context name
                invokedynamic("dyn:getProp|getElem|getMethod", sig(Object.class, Object.class, ReferenceContext.class, String.class), DynJSBootstrapper.BOOTSTRAP, DynJSBootstrapper.BOOTSTRAP_ARGS);
                // value
                if (throwIfNot != null) {
                    dup();
                    // value value
                    instance_of(p(throwIfNot));
                    // value bool
                    iftrue(end);
                    // value
                    pop();
                    append(jsThrowTypeError("expected " + throwIfNot.getName()));
                }
                // result
                go_to( end );
                
                label( throwRef);
                append( jsThrowReferenceError( "unable to dereference" ) );
                
                label( end );
                // value
                nop();
            }
        };
    }

}
