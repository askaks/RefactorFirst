namespace Syntax;

file class Gadget
{
    public int RunCopy() { return 1; }
}

public class UsesCopy
{
    public int Go() { return new Gadget().RunCopy(); }
}
