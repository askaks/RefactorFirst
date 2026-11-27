namespace Syntax;

public class Mixed
{
    public int Open() { return 1; }
}

public class UsesOpenMixed
{
    public int Go() { return new Mixed().Open(); }
}
