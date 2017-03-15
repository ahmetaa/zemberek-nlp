package zemberek.corpus;

import zemberek.core.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EliminateDuplicates {

    private Path corporaRoot;

    private EliminateDuplicates(Path corporaRoot) {
        this.corporaRoot = corporaRoot;
    }

    public static void main(String[] args) throws IOException {
        Path cr = Paths.get("/media/data/corpora/raw3");
        EliminateDuplicates e = new EliminateDuplicates(cr);
        e.doIt();
    }

    private void doIt() throws IOException {
        List<Path> dirs = Files.walk(corporaRoot)
                .filter(s -> s.toFile().isDirectory()).collect(Collectors.toList());
        for (Path dir : dirs) {
            if (dir.equals(corporaRoot)) {
                continue;
            }
            extractLabeledDocuments(dir, corporaRoot.resolve(dir.toFile().getName() + ".corpus"));
        }
    }

    private void extractLabeledDocuments(Path root, Path outFile) throws IOException {
        List<Path> files = Files.walk(root).filter(s -> s.toFile().isFile()).collect(Collectors.toList());
        files.sort(Comparator.comparing(Path::toString));
        WebCorpus corpus = new WebCorpus("c", "c");
        for (Path file : files) {
            if (file.toFile().isDirectory()) {
                continue;
            }
            Log.info("Adding %s", file);
            List<WebDocument> doc = WebCorpus.loadDocuments(file);
            for (WebDocument webDocument : doc) {
                webDocument.removeDuplicateLines();
            }
            List<WebDocument> labeled = doc.stream()
                    .filter(s -> s.getContentAsString().length() > 200)
                    .collect(Collectors.toList());
            corpus.addDocuments(labeled);
        }
        Log.info("Total amount of files = %d", corpus.getDocuments().size());
        WebCorpus noDuplicates = corpus.copyNoDuplicates();
        Log.info("Corpus size = %d, After removing duplicates = %d",
                corpus.documentCount(),
                noDuplicates.documentCount());
        Log.info("Saving corpus to %s", outFile);
        noDuplicates.save(outFile, false);
    }
}
