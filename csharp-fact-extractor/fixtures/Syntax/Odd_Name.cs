namespace Syntax;

file class Gadget
{
    public int RunPlain() { return 1; }
}

public class UsesOddPlain
{
    public int Go() { return new Gadget().RunPlain(); }
}
