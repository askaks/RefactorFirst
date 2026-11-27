namespace Syntax;

file class Gadget
{
    public int RunDollar() { return 1; }
}

public class UsesDollar
{
    public int Go() { return new Gadget().RunDollar(); }
}
