namespace Syntax;

file class Solo
{
    public int Run() { return 1; }
}

public class UsesSolo
{
    public int Go() { return new Solo().Run(); }
}
