package compiler;

import compiler.lib.*;

public class STentry implements Visitable {
	int nl;
	TypeNode type;
	int offset;
	public STentry(int n, TypeNode t ) { nl = n; type = t;}
	public STentry(int n, TypeNode t, int of ) { nl = n; type = t; offset = of; }
	@Override
	public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {
		return ((BaseEASTVisitor<S,E>) visitor).visitSTentry(this);
	}
}
