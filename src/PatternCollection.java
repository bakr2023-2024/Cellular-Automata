import java.io.*;
import java.util.HashMap;
import java.io.Serializable;

public class PatternCollection<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    class Pair implements Serializable {
        private static final long serialVersionUID = 1L;
        T set;
        int n;

        Pair(T set, int n) {
            this.set = set;
            this.n = n;
        }
    }

    HashMap<String, Pair> patterns;
    private String filePath;

    public PatternCollection(String filePath) {
        this.filePath = filePath;
        this.patterns = new HashMap<>();
        loadPatterns();
    }

    public int addPattern(String name, T arr, int n, int overwrite) {
        if (!this.patterns.containsKey(name))
            patterns.put(name, new Pair(arr, n));
        else {
            if (overwrite == 0)
                return 1;
            if (overwrite == 1)
                patterns.put(name, new Pair(arr, n));
            else
                return 2;
        }
        savePatterns();
        return 3;
    }

    public Pair getPattern(String name) {
        return patterns.get(name);
    }

    private void savePatterns() {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(patterns);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPatterns() {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                patterns = (HashMap<String, Pair>) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                e.printStackTrace();
            }
        }
    }
}