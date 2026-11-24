namespace Syntax;

file class Tool
{
    public int RunTilde() { return 1; }
}

public class UsesTilde
{
    public int Go() { return new Tool().RunTilde(); }
}
