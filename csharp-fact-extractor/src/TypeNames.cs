using Microsoft.CodeAnalysis;

namespace CSharpFactExtractor;

public static class TypeNames
{
    /// Namespace + name; nested types joined with '$' so the namespace stays the package.
    /// A generic type keeps its plain name unless a type with the same name sits beside it
    /// (Result and Result<T>); then the generic one is named the .NET way, "Result`1".
    public static string Fqn(INamedTypeSymbol type)
    {
        var names = new Stack<string>();
        for (INamedTypeSymbol? t = type.OriginalDefinition; t != null; t = t.ContainingType)
        {
            // An `extension(...) { }` block is not a type of its own: what is in it belongs to the class around it.
            if (t.IsExtension) continue;
            // Only a generic or a file-local type is named differently for having a namesake, so only those are looked up.
            bool hasNamesake = (t.Arity > 0 || t.IsFileLocal) && HasNamesake(t);
            string name = t.Arity > 0 && hasNamesake ? t.Name + "`" + t.Arity : t.Name;
            // Two `file class Helper` in two files are two types, told apart by the file: "Helper@FirstFile".
            names.Push(t.IsFileLocal && hasNamesake ? name + "@" + FileTag(t) : name);
        }
        var ns = Namespace(type);
        return (ns.Length == 0 ? "" : ns + ".") + string.Join("$", names);
    }

    /// The file a file-local type is in, without its extension: just the file's name when that is enough,
    /// with as many folders as it takes when its namesakes sit in files of the same name ("A_Helpers", "B_Helpers").
    /// Anything but a letter, a digit, "_" or "~" becomes "_", so the name still ends at its last dot and a "$"
    /// in a file name is not read as a nested type. Files that read the same after that
    /// ("Dotted.Part" and "Dotted_Part") are numbered in path order: the second is "Dotted_Part~2",
    /// or the next number no file is already called.
    private static string FileTag(INamedTypeSymbol type)
    {
        static string[] Parts(INamedTypeSymbol t) =>
            Path.ChangeExtension(t.Locations.First(l => l.IsInSource).SourceTree!.FilePath, null).Split('/', '\\');
        string[] own = Parts(type);
        var all = ((INamespaceOrTypeSymbol?)type.ContainingType ?? type.ContainingNamespace).GetTypeMembers(type.Name)
            .Where(t => t.IsFileLocal && IsInSource(t)).Select(Parts).ToList();

        string Tag(string[] file)
        {
            int take = 1;
            while (take < file.Length && all.Any(p => !p.SequenceEqual(file) && p.TakeLast(take).SequenceEqual(file.TakeLast(take)))) take++;
            return string.Concat(string.Join("_", file.TakeLast(take)).Select(c => char.IsLetterOrDigit(c) || c is '_' or '~' ? c : '_'));
        }

        // In path order: the first file with a tag keeps it; a later one is numbered, skipping every
        // name that a file's own tag or an earlier number already uses ("Dotted_Part~2.cs" keeps "~2").
        var paths = all.Select(p => string.Join("/", p)).Distinct().OrderBy(p => p, StringComparer.Ordinal).ToList();
        var natural = paths.Select(p => Tag(p.Split('/'))).ToHashSet();
        var given = new Dictionary<string, string>();
        var used = new HashSet<string>();
        foreach (string path in paths)
        {
            string tag = Tag(path.Split('/')), name = tag;
            for (int n = 2; !used.Add(name); n++)
            {
                name = tag + "~" + n;
                if (natural.Contains(name)) name = tag;
            }
            given[path] = name;
        }
        return given[string.Join("/", own)];
    }

    /// True when another type with the same name is declared in the same namespace or outer type of the same project.
    private static bool HasNamesake(INamedTypeSymbol type)
    {
        INamespaceOrTypeSymbol? container = (INamespaceOrTypeSymbol?)type.ContainingType ?? type.ContainingNamespace;
        return container is not null && container.GetTypeMembers(type.Name).Count(IsInSource) > 1;
    }

    public static string Namespace(INamedTypeSymbol type) =>
        type.ContainingNamespace is { IsGlobalNamespace: false } ns ? ns.ToDisplayString() : "";

    public static bool IsInSource(ISymbol symbol) => symbol.Locations.Any(l => l.IsInSource);

    private static readonly string[] GeneratedSuffixes = { ".g.cs", ".g.i.cs", ".designer.cs", ".generated.cs" };

    /// Build output and tool-generated files (Razor, designers, source generators): nobody refactors these.
    public static bool IsGeneratedPath(string? path)
    {
        if (string.IsNullOrEmpty(path)) return true;
        string normalized = path.Replace('\\', '/');
        if (normalized.Contains("/obj/") || normalized.Contains("/bin/")) return true;
        return GeneratedSuffixes.Any(suffix => normalized.EndsWith(suffix, StringComparison.OrdinalIgnoreCase));
    }

    /// The symbol's declarations that are in hand-written files.
    public static IEnumerable<SyntaxReference> SourceReferences(ISymbol symbol) =>
        symbol.DeclaringSyntaxReferences.Where(r => !IsGeneratedPath(r.SyntaxTree.FilePath));

    /// True when the symbol is declared only in generated files.
    public static bool IsGeneratedOnly(ISymbol symbol) =>
        symbol.DeclaringSyntaxReferences.Length > 0 && !SourceReferences(symbol).Any();

    // Nullable annotations ("string?") are left out so a method's own signature and a caller's view of it always match.
    private static readonly SymbolDisplayFormat ParameterFormat = SymbolDisplayFormat.MinimallyQualifiedFormat.WithMiscellaneousOptions(
        SymbolDisplayFormat.MinimallyQualifiedFormat.MiscellaneousOptions & ~SymbolDisplayMiscellaneousOptions.IncludeNullableReferenceTypeModifier);

    /// Type parameters and ref/out/in are part of the name: Save() and Save<T>(), Add(int) and Add(ref int) are different methods.
    /// A member of an `extension(Order order) { }` block is named as the static method it compiles to, `Twice(Order)`,
    /// which is also how a classic `this` extension method is named.
    public static string Signature(IMethodSymbol method)
    {
        method = method.AssociatedExtensionImplementation ?? method;
        string typeParameters = method.Arity > 0 ? "<" + string.Join(",", method.TypeParameters.Select(t => t.Name)) + ">" : "";
        return method.Name + typeParameters + "(" + string.Join(",", method.Parameters.Select(Parameter)) + ")";
    }

    private static string Parameter(IParameterSymbol parameter)
    {
        string passedBy = parameter.RefKind switch
        {
            RefKind.Ref => "ref ",
            RefKind.Out => "out ",
            RefKind.In => "in ",
            RefKind.RefReadOnlyParameter => "ref readonly ",
            _ => "",
        };
        return passedBy + parameter.Type.ToDisplayString(ParameterFormat);
    }
}
