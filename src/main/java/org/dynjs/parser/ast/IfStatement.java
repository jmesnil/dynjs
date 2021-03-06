/**
 *  Copyright 2012 Douglas Campos, and individual contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dynjs.parser.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.Tree;
import org.dynjs.parser.CodeVisitor;
import org.dynjs.parser.Statement;
import org.dynjs.runtime.ExecutionContext;

public class IfStatement extends AbstractStatement {

    private final Expression vbool;
    private final Statement vthen;
    private final Statement velse;

    public IfStatement(final Tree tree, final Expression vbool, final Statement vthen, final Statement velse) {
        super(tree);
        this.vbool = vbool;
        this.vthen = vthen;
        this.velse = velse;
    }

    public Expression getTest() {
        return this.vbool;
    }

    public Statement getThenBlock() {
        return this.vthen;
    }

    public Statement getElseBlock() {
        return this.velse;
    }

    public List<VariableDeclaration> getVariableDeclarations() {
        List<VariableDeclaration> decls = new ArrayList<>();
        if (this.vthen != null) {
            decls.addAll(this.vthen.getVariableDeclarations());
        }
        if (this.velse != null) {
            decls.addAll(this.velse.getVariableDeclarations());
        }
        return decls;
    }

    public String toIndentedString(String indent) {
        StringBuffer buf = new StringBuffer();

        buf.append(indent).append("if (").append(this.vbool.toString()).append(") {\n");
        buf.append(this.vthen.toIndentedString(indent + "  "));
        if (this.velse != null) {
            buf.append(indent).append("} else {\n").append(this.velse.toIndentedString(indent + "  "));
        }
        buf.append(indent).append("}");

        return buf.toString();
    }

    @Override
    public void accept(ExecutionContext context, CodeVisitor visitor, boolean strict) {
        visitor.visit(context, this, strict);
    }
}
