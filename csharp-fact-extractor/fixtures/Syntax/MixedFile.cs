namespace Syntax;

file class Mixed
{
    public int Hidden() { return 1; }
}

public class UsesHiddenMixed
{
    public int Go() { return new Mixed().Hidden(); }
}
