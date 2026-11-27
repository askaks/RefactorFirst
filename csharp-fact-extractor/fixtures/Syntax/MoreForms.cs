namespace Syntax;

public class Line
{
    public Order Inner { get; set; } = new Order();
}

public class FormsUser
{
    public void IndexWriteIfAny(Grid? grid) { grid?[0] = 1; }

    public int IndexInTuple(Grid grid)
    {
        (grid[0], var rest) = (1, 2);
        return rest;
    }

    public void DeepNullConditional(Line? line) { line?.Inner.Total = 5; }

    public void ParenthesizedBump(Order a) { (a.Total)++; }
}

/// Has its own method called `nameof`, so `nameof(a.Total)` here is an ordinary call and `a.Total` is read.
public class Shadow
{
    private string nameof(int value) { return value.ToString(); }

    public string Go(Order a) { return nameof(a.Total); }
}

/// Names Grid only inside `nameof`.
public class NameofOnly
{
    public string Go() { return nameof(Grid); }
}

public record struct Extent(int Wide, int High);
