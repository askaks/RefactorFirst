using System.Collections.Generic;

namespace Syntax;

public class LibraryUser
{
    public int ListRead(List<int> list) { return list[0]; }

    public void ListWrite(List<int> list) { list[0] = 1; }

    public char TextRead(string text) { return text[0]; }

    public int MapRead(Dictionary<string, int> map) { return map["a"]; }

    public Dictionary<string, int> MapCreate() { return new Dictionary<string, int> { ["a"] = 1 }; }

    public int ArrayRead(int[] cells) { return cells[0]; }

    public void ArrayWrite(int[] cells) { cells[0] = 1; }
}
