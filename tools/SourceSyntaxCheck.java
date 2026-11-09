import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class SourceSyntaxCheck {
    public static void main(String[] args) throws IOException { // Iniciar la aplicación
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Se necesita un compilador de Java (JDK).");
        }
        Path project = Path.of(args.length > 0 ? args[0] : ".");
        List<Path> sources;
        try (Stream<Path> paths = Files.walk(project)) {
            sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/"))
                    .toList();
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> files = manager.getJavaFileObjectsFromPaths(sources);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "17", "-proc:none"), null, files);
            task.parse();
        }
        long errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                .peek(diagnostic -> System.err.println(diagnostic.getSource().getName()
                        + ":" + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null)))
                .count();
        if (errors != 0) {
            throw new AssertionError(errors + " errores de sintaxis");
        }
        System.out.println("Correcto: se analizaron " + sources.size() + " archivos Java con sintaxis de Java 17.");
    }
}
