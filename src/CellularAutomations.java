import java.util.*;
import java.lang.Math;

public class CellularAutomations {
    public volatile static boolean stop = true;
    public volatile static int delay = 500;

    public static void delay() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class Conway {
        public interface Observer {
            public void updateScreen(BitSet arr, int generationNumber, int liveCells, double growthRate);
        }

        protected static ArrayList<Observer> observers = new ArrayList<Observer>();

        public static void addObserver(Observer observer) {
            observers.add(observer);
        }

        public static void removeObserver(Observer observer) {
            observers.remove(observer);
        }

        void notifyObservers(BitSet state, ArrayList<Observer> observers, int generationNumber,
                int liveCells,
                double growthRate) {
            for (Observer observer : observers) {
                observer.updateScreen(state, generationNumber, liveCells, growthRate);
            }
        }

        BitSet map;
        int n;
        HashSet<Integer> changes = new HashSet<Integer>();
        static int generationNumber = 0;
        int liveCells = 0;
        double growthRate = 0.0;

        public static void setGenerationNumber(int n) {
            generationNumber = n;
        }

        Conway() {

        }

        Conway(BitSet set, int n) {
            this.n = n;
            this.map = new BitSet();
            this.map = (BitSet) set.clone();
            this.liveCells = set.cardinality();
        }

        protected void check(int bitIndex) {
            int row = Math.floorDiv(bitIndex, n);
            int column = Math.floorMod(bitIndex, n);
            int liveNeighbors = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0)
                        continue;
                    int neighborRow = Math.floorMod(row + i, n);
                    int neighborColumn = Math.floorMod(column + j, n);
                    if (this.map.get(neighborRow * n + neighborColumn))
                        liveNeighbors++;
                }
            }
            if (this.map.get(bitIndex)) {
                if (liveNeighbors < 2 || liveNeighbors > 3)
                    this.changes.add(bitIndex);
            } else if (liveNeighbors == 3)
                this.changes.add(bitIndex);
        }

        protected void update() {
            int bitIndex = 0;
            while (bitIndex < n * n) {
                check(bitIndex);
                bitIndex = this.map.nextSetBit(bitIndex + 1);
                if (bitIndex == -1)
                    break;
            }
            bitIndex = 0;
            while (bitIndex < n * n) {
                check(bitIndex);
                bitIndex = this.map.nextClearBit(bitIndex + 1);
                if (bitIndex == -1)
                    break;
            }
            for (Integer change : changes)
                this.map.flip(change);
            this.changes.clear();
            generationNumber++;
            int newLiveCells = this.map.cardinality();
            this.growthRate = ((double) newLiveCells - this.liveCells) / this.liveCells;
            this.liveCells = newLiveCells;
        }

        public void start() {
            while (!stop) {
                notifyObservers(this.map, observers, generationNumber, this.liveCells,
                        this.growthRate);
                update();
                delay();
            }
        }
    }

    public static class Seeds extends Conway {
        Seeds(BitSet set, int n) {
            super(set, n);
        }

        @Override
        protected void check(int bitIndex) {
            int row = Math.floorDiv(bitIndex, n);
            int column = Math.floorMod(bitIndex, n);
            int liveNeighbors = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0)
                        continue;
                    int neighborRow = Math.floorMod(row + i, n);
                    int neighborColumn = Math.floorMod(column + j, n);
                    if (this.map.get(neighborRow * n + neighborColumn))
                        liveNeighbors++;
                }
            }
            if (!this.map.get(bitIndex)) {
                if (liveNeighbors == 2)
                    this.changes.add(bitIndex);
            } else
                this.changes.add(bitIndex);
        }
    }

    public static class LifeWithoutDeath extends Conway {
        LifeWithoutDeath(BitSet set, int n) {
            super(set, n);
        }

        @Override
        protected void check(int bitIndex) {
            int row = Math.floorDiv(bitIndex, n);
            int column = Math.floorMod(bitIndex, n);
            int liveNeighbors = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0)
                        continue;
                    int neighborRow = Math.floorMod(row + i, n);
                    int neighborColumn = Math.floorMod(column + j, n);
                    if (this.map.get(neighborRow * n + neighborColumn))
                        liveNeighbors++;
                }
            }
            if (!this.map.get(bitIndex) && (liveNeighbors == 3)) {
                this.changes.add(bitIndex);
            }
        }
    }

    public static class Diamoeba extends Conway {
        Diamoeba(BitSet set, int n) {
            super(set, n);
        }

        @Override
        protected void check(int bitIndex) {
            int row = Math.floorDiv(bitIndex, n);
            int column = Math.floorMod(bitIndex, n);
            int liveNeighbors = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0)
                        continue;
                    int neighborRow = Math.floorMod(row + i, n);
                    int neighborColumn = Math.floorMod(column + j, n);
                    if (this.map.get(neighborRow * n + neighborColumn))
                        liveNeighbors++;
                }
            }
            if (!this.map.get(bitIndex)) {
                if (liveNeighbors == 3 || liveNeighbors == 5 || liveNeighbors == 6 || liveNeighbors == 7
                        || liveNeighbors == 8)
                    this.changes.add(bitIndex);
            } else if (liveNeighbors < 5 || liveNeighbors > 8)
                this.changes.add(bitIndex);
        }
    }

    public static class DayAndNight extends Conway {
        DayAndNight(BitSet set, int n) {
            super(set, n);
        }

        @Override
        protected void check(int bitIndex) {
            int row = Math.floorDiv(bitIndex, n);
            int column = Math.floorMod(bitIndex, n);
            int liveNeighbors = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0)
                        continue;
                    int neighborRow = Math.floorMod(row + i, n);
                    int neighborColumn = Math.floorMod(column + j, n);
                    if (this.map.get(neighborRow * n + neighborColumn))
                        liveNeighbors++;
                }
            }
            if (!this.map.get(bitIndex)) {
                if (liveNeighbors == 3 || liveNeighbors == 4 || liveNeighbors == 6 || liveNeighbors == 7
                        || liveNeighbors == 8)
                    this.changes.add(bitIndex);
            } else if (liveNeighbors == 0 || liveNeighbors == 1 || liveNeighbors == 2 || liveNeighbors == 5)
                this.changes.add(bitIndex);
        }
    }

    public static enum State {
        ON,
        OFF,
        DYING;
    }

    public static class Brian {
        public interface Observer {
            public void updateScreen(State[][] arr, int generationNumber, int liveCells, double growthRate,
                    int dyingCells, HashSet<String> changes);
        }

        protected static ArrayList<Observer> observers = new ArrayList<Observer>();

        public static void addObserver(Observer observer) {
            observers.add(observer);
        }

        public static void removeObserver(Observer observer) {
            observers.remove(observer);
        }

        void notifyObservers(State[][] state, ArrayList<Observer> observers, int generationNumber,
                int liveCells,
                double growthRate, int dyingCells, HashSet<String> changes) {
            for (Observer observer : observers) {
                observer.updateScreen(state, generationNumber, liveCells, growthRate, dyingCells, changes);
            }
        }

        State[][] map;
        HashSet<String> changes = new HashSet<String>();
        int n;
        static int generationNumber = 0;
        int liveCells = 0;
        double growthRate = 0.0;
        int dyingCells = 0;

        public static void setGenerationNumber(int n) {
            generationNumber = n;
        }

        Brian(State[][] grid, int n) {
            this.map = new State[n][n];
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    this.map[i][j] = grid[i][j];
            this.n = n;
        }

        protected boolean wrap(int i, int j) {
            return this.map[(i % n + n) % n][(j % n + n) % n] == State.ON;
        }

        protected void check(int i, int j) {
            int counter = 0;
            if (wrap(i - 1, j - 1))
                counter++;
            if (wrap(i - 1, j))
                counter++;
            if (wrap(i - 1, j + 1))
                counter++;
            if (wrap(i, j - 1))
                counter++;
            if (wrap(i, j + 1))
                counter++;
            if (wrap(i + 1, j - 1))
                counter++;
            if (wrap(i + 1, j))
                counter++;
            if (wrap(i + 1, j + 1))
                counter++;
            if ((this.map[i][j] == State.OFF && counter == 2) || this.map[i][j] != State.OFF) {
                this.changes.add(i + "," + j);
            }
        }

        protected void update() {
            int oldCells = liveCells;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (this.map[i][j] == State.ON)
                        liveCells++;
                    else if (this.map[i][j] == State.DYING)
                        dyingCells++;
                    check(i, j);
                }
            }
            for (String change : this.changes) {
                String[] t = change.split(",");
                int i = Integer.parseInt(t[0]);
                int j = Integer.parseInt(t[1]);
                if (this.map[i][j] == State.ON)
                    this.map[i][j] = State.DYING;
                else if (this.map[i][j] == State.OFF)
                    this.map[i][j] = State.ON;
                else if (this.map[i][j] == State.DYING)
                    this.map[i][j] = State.OFF;
            }
            growthRate = ((double) liveCells - oldCells) / oldCells;
            generationNumber++;
            this.changes.clear();
        }

        public void start() {
            while (!stop) {
                notifyObservers(this.map, observers, generationNumber, this.liveCells,
                        this.growthRate, this.dyingCells, this.changes);
                update();
                delay();
            }
        }
    }
}