import java.awt.*;
import javax.swing.*;
import java.util.Arrays;
import java.util.BitSet;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Random;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;

public class App {
    static int length = 780;
    static int minDelay = 5;
    static int maxDelay = 1000;
    static Random rand = new Random(System.nanoTime());

    public static void main(String[] args) {
        JFrame mainFrame = new JFrame("Main GUI");
        mainFrame.setSize(1300, length);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MainGUI menu = new MainGUI();
        mainFrame = menu.mainFrame;
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
}

class Simulation {
    int n;
    Point dragStart = null;
    boolean penMode = false;
    int cellSize = 0;
    int xOffset = 0;
    int yOffset = 0;
    int panX = 0;
    int panY = 0;
    String drawState;
    int drawStateindex = 0;
    String[] statesArray;
    int row = 0;
    int column = 0;
    int cellX = 0;
    int cellY = 0;

    Simulation(int n, String[] states) {
        this.n = n;
        statesArray = states;
    }

    void update(int n) {
        this.n = n;
        this.cellSize = App.length / n;
        this.xOffset = (App.length - (this.cellSize * n)) / 2;
        this.yOffset = (App.length - (this.cellSize * n)) / 2;
    }

    void updateCoords(int x, int y) {
        row = Math.floorDiv(y - yOffset - panY, cellSize);
        column = Math.floorDiv(x - xOffset - panX, cellSize);
        cellX = xOffset + column * cellSize + panX;
        cellY = yOffset + row * cellSize + panY;
    }

    void updateState() {
        drawStateindex = (drawStateindex + 1 % statesArray.length + statesArray.length)
                % statesArray.length;
        drawState = statesArray[drawStateindex];
    }
}

class Screen1 extends JComponent
        implements CellularAutomations.Conway.Observer, MouseListener, MouseMotionListener, MouseWheelListener {
    BitSet grid;
    BitSet copy;
    Simulation sim;
    String[] statesArray = { "Live", "Dead" };
    int liveCells = 0;
    int numberOfGenerations = 0;
    double growthPopulation = 0;
    protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void setGenerationNumber(int n) {
        CellularAutomations.Conway.setGenerationNumber(n);
    }

    Screen1() {
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public void updateScreen(BitSet t, int i, int j, double k) {
        int oldnumber = this.numberOfGenerations;
        this.numberOfGenerations = i;
        pcs.firePropertyChange("numberOfGenerations", oldnumber, this.numberOfGenerations);
        int oldlive = this.liveCells;
        this.liveCells = j;
        pcs.firePropertyChange("liveCells", oldlive, this.liveCells);
        double oldgrowth = this.growthPopulation;
        this.growthPopulation = k;
        pcs.firePropertyChange("growthPopulation", oldgrowth, this.growthPopulation);
        this.grid = (BitSet) t.clone();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    public void clearStats() {
        liveCells = numberOfGenerations = 0;
        growthPopulation = 0;
        CellularAutomations.Conway.setGenerationNumber(0);
    }

    void randomize(int n) {
        BitSet set = new BitSet(n * n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int centerX = n / 2;
                int centerY = n / 2;
                double distance = Math.sqrt((i - centerX) * (i - centerX) + (j - centerY) * (j - centerY));
                double maxDistance = Math.sqrt((n / 2) * (n / 2) + (n / 2) * (n / 2));
                double probability = 0.4 - 0.2 * (distance / maxDistance);
                set.set(i * n + j, App.rand.nextDouble() < probability);
            }
        }
        setGrid(set, n);
    }

    protected void start() {
        CellularAutomations.Conway conway = new CellularAutomations.Conway(this.grid, this.sim.n);
        CellularAutomations.Conway.addObserver(this);
        this.copy = new BitSet();
        this.copy = (BitSet) this.grid.clone();
        new Thread(new Runnable() {
            @Override
            public void run() {
                conway.start();
            }
        }).start();
    }

    public Screen1(int n) {
        this.sim = new Simulation(n, statesArray);
        this.grid = new BitSet(n * n);
        this.copy = new BitSet(n * n);
        this.copy = (BitSet) this.grid.clone();
        this.sim.drawState = this.statesArray[0];
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void setGrid(BitSet set, int n) {
        this.sim.update(n);
        this.grid = (BitSet) set.clone();
        this.copy = new BitSet();
        this.copy = (BitSet) this.grid.clone();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (CellularAutomations.stop && this.sim.penMode && SwingUtilities.isLeftMouseButton(e)) {
            updateGrid(e);
        }
        if (SwingUtilities.isMiddleMouseButton(e)) {
            this.sim.dragStart = e.getPoint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        this.sim.cellSize = Math.max(1, this.sim.cellSize - notches);
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (CellularAutomations.stop && this.sim.penMode) {
            updateGrid(e);
        }
        if (this.sim.dragStart != null) {
            Point dragEnd = e.getPoint();
            this.sim.panX += (dragEnd.x - this.sim.dragStart.x);
            this.sim.panY += (dragEnd.y - this.sim.dragStart.y);
            this.sim.dragStart = dragEnd;
            repaint();
        }
    }

    protected void updateGrid(MouseEvent e) {
        int x = (int) e.getX();
        int y = (int) e.getY();
        sim.updateCoords(x, y);
        if (sim.row >= 0 && sim.row < this.sim.n && sim.column >= 0 && sim.column < this.sim.n) {
            if (sim.drawState.equals("Live"))
                this.grid.set(sim.row * this.sim.n + sim.column);
            else
                this.grid.clear(sim.row * this.sim.n + sim.column);
            repaint(sim.cellX, sim.cellY, this.sim.cellSize, this.sim.cellSize);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            int oldindex = sim.drawStateindex;
            sim.updateState();
            pcs.firePropertyChange("sim.drawStateIndex", oldindex, sim.drawStateindex);
            return;
        }
        if (!CellularAutomations.stop || this.sim.penMode)
            return;
        int x = (int) e.getX();
        int y = (int) e.getY();
        sim.updateCoords(x, y);
        if (sim.row >= 0 && sim.row < sim.n && sim.column >= 0 && sim.column < sim.n) {
            if (sim.drawState.equals("Live"))
                this.grid.set(sim.row * this.sim.n + sim.column);
            else
                this.grid.clear(sim.row * this.sim.n + sim.column);
            repaint(sim.cellX, sim.cellY, this.sim.cellSize, this.sim.cellSize);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e))
            sim.dragStart = null;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(App.length, App.length);
    }

    Color deadcolor = Color.BLACK;
    Color livecolor = Color.BLACK;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        for (int i = 0; i < sim.n; i++) {
            for (int j = 0; j < sim.n; j++) {
                int x = sim.xOffset + j * sim.cellSize + sim.panX;
                int y = sim.yOffset + i * sim.cellSize + sim.panY;
                if (this.grid.get(i * sim.n + j))
                    g2d.setColor(livecolor);
                else
                    g2d.setColor(deadcolor);
                g2d.fillRect(x, y, sim.cellSize, sim.cellSize);
            }
        }
    }
}

class Screen2 extends JComponent
        implements CellularAutomations.Brian.Observer, MouseListener, MouseMotionListener, MouseWheelListener {
    CellularAutomations.State[][] grid;
    CellularAutomations.State[][] copy;
    String[] statesArray = { "Live", "Dying", "Dead" };
    Simulation sim;
    int liveCells = 0;
    int dyingCells = 0;
    int numberOfGenerations = 0;
    double growthPopulation = 0;
    protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void setGenerationNumber(int n) {
        CellularAutomations.Brian.setGenerationNumber(n);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(App.length, App.length);
    }

    Screen2(int n) {
        this.sim = new Simulation(n, statesArray);
        this.grid = new CellularAutomations.State[sim.n][sim.n];
        this.copy = new CellularAutomations.State[sim.n][sim.n];
        this.sim.drawState = this.statesArray[0];
        for (int i = 0; i < n; i++) {
            Arrays.fill(this.grid[i], CellularAutomations.State.OFF);
            Arrays.fill(this.copy[i], CellularAutomations.State.OFF);
        }
        addMouseListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
    }

    public void clearStats() {
        liveCells = dyingCells = numberOfGenerations = 0;
        growthPopulation = 0;
        CellularAutomations.Brian.setGenerationNumber(0);
    }

    public void setGrid(CellularAutomations.State[][] set, int n) {
        sim.update(n);
        this.grid = new CellularAutomations.State[sim.n][sim.n];
        this.copy = new CellularAutomations.State[sim.n][sim.n];
        for (int i = 0; i < sim.n; i++)
            for (int j = 0; j < sim.n; j++) {
                this.copy[i][j] = this.grid[i][j];
            }
        for (int i = 0; i < sim.n; i++) {
            for (int j = 0; j < sim.n; j++)
                this.grid[i][j] = set[i][j];
        }
        repaint();
    }

    @Override
    public void updateScreen(CellularAutomations.State[][] t, int i, int j, double k, int l, HashSet<String> changes) {
        int oldnumber = this.numberOfGenerations;
        this.numberOfGenerations = i;
        pcs.firePropertyChange("numberOfGenerations", oldnumber, this.numberOfGenerations);
        int oldlive = this.liveCells;
        this.liveCells = j;
        pcs.firePropertyChange("liveCells", oldlive, this.liveCells);
        double oldgrowth = this.growthPopulation;
        this.growthPopulation = k;
        pcs.firePropertyChange("growthPopulation", oldgrowth, this.growthPopulation);
        int oldDying = this.dyingCells;
        this.dyingCells = l;
        pcs.firePropertyChange("dyingCells", oldDying, this.dyingCells);
        for (int p = 0; p < sim.n; p++)
            for (int q = 0; q < sim.n; q++)
                this.grid[p][q] = t[p][q];
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                repaint();
            }
        });
    }

    void randomize(int n) {
        CellularAutomations.State[][] newArr = new CellularAutomations.State[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int r = App.rand.nextInt(3);
                if (r == 2)
                    newArr[i][j] = CellularAutomations.State.ON;
                else if (r == 1)
                    newArr[i][j] = CellularAutomations.State.OFF;
                else
                    newArr[i][j] = CellularAutomations.State.DYING;
            }
        }
        setGrid(newArr, n);
    }

    protected void updateGrid(MouseEvent e) {
        int x = (int) e.getX();
        int y = (int) e.getY();
        sim.updateCoords(x, y);
        if (sim.row >= 0 && sim.row < sim.n && sim.column >= 0 && sim.column < sim.n) {
            if (sim.drawState.equals("Live"))
                this.grid[sim.row][sim.column] = CellularAutomations.State.ON;
            else if (sim.drawState.equals("Dying"))
                this.grid[sim.row][sim.column] = CellularAutomations.State.DYING;
            else if (sim.drawState.equals("Dead"))
                this.grid[sim.row][sim.column] = CellularAutomations.State.OFF;
            repaint(sim.cellX, sim.cellY, this.sim.cellSize, this.sim.cellSize);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (CellularAutomations.stop && this.sim.penMode && SwingUtilities.isLeftMouseButton(e)) {
            updateGrid(e);
        }
        if (SwingUtilities.isMiddleMouseButton(e)) {
            this.sim.dragStart = e.getPoint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        this.sim.cellSize = Math.max(1, this.sim.cellSize - notches);
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (CellularAutomations.stop && this.sim.penMode) {
            updateGrid(e);
        }
        if (this.sim.dragStart != null) {
            Point dragEnd = e.getPoint();
            this.sim.panX += (dragEnd.x - this.sim.dragStart.x);
            this.sim.panY += (dragEnd.y - this.sim.dragStart.y);
            this.sim.dragStart = dragEnd;
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            int oldindex = sim.drawStateindex;
            sim.updateState();
            pcs.firePropertyChange("sim.drawStateIndex", oldindex, sim.drawStateindex);
            return;
        }
        if (!CellularAutomations.stop || this.sim.penMode)
            return;
        int x = (int) e.getX();
        int y = (int) e.getY();
        sim.updateCoords(x, y);
        if (sim.row >= 0 && sim.row < sim.n && sim.column >= 0 && sim.column < sim.n) {
            if (sim.drawState.equals("Live"))
                this.grid[sim.row][sim.column] = CellularAutomations.State.ON;
            else if (sim.drawState.equals("Dying"))
                this.grid[sim.row][sim.column] = CellularAutomations.State.DYING;
            else if (sim.drawState.equals("Dead"))
                this.grid[sim.row][sim.column] = CellularAutomations.State.OFF;
            repaint(sim.cellX, sim.cellY, this.sim.cellSize, this.sim.cellSize);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e))
            sim.dragStart = null;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    Color dyingcolor = Color.BLACK;
    Color deadcolor = Color.BLACK;
    Color livecolor = Color.BLACK;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        for (int i = 0; i < sim.n; i++) {
            for (int j = 0; j < sim.n; j++) {
                int x = sim.xOffset + j * sim.cellSize + sim.panX;
                int y = sim.yOffset + i * sim.cellSize + sim.panY;
                if (this.grid[i][j] == CellularAutomations.State.ON) {
                    g2d.setColor(livecolor);
                } else if (this.grid[i][j] == CellularAutomations.State.DYING) {
                    g2d.setColor(dyingcolor);
                } else if (this.grid[i][j] == CellularAutomations.State.OFF) {
                    g2d.setColor(deadcolor);
                }
                g2d.fillRect(x, y, sim.cellSize, sim.cellSize);
            }
        }
    }

    protected void start() {
        CellularAutomations.Brian.addObserver(this);
        CellularAutomations.Brian brian = new CellularAutomations.Brian(this.grid, sim.n);
        this.copy = new CellularAutomations.State[sim.n][sim.n];
        for (int i = 0; i < sim.n; i++)
            for (int j = 0; j < sim.n; j++) {
                this.copy[i][j] = this.grid[i][j];
            }
        new Thread(new Runnable() {
            @Override
            public void run() {
                brian.start();
            }
        }).start();
    }
}

class Screen3 extends Screen1 {
    Screen3(int n) {
        super(n);
    }

    @Override
    protected void start() {
        CellularAutomations.Seeds seeds = new CellularAutomations.Seeds(this.grid, this.sim.n);
        CellularAutomations.Seeds.addObserver(this);
        this.copy = new BitSet();
        this.copy = (BitSet) this.grid.clone();
        new Thread(new Runnable() {
            @Override
            public void run() {
                seeds.start();
            }
        }).start();
    }
}

class Screen4 extends Screen1 {
    Screen4(int n) {
        super(n);
    }

    @Override
    protected void start() {
        CellularAutomations.LifeWithoutDeath lifewithoutdeath = new CellularAutomations.LifeWithoutDeath(this.grid,
                this.sim.n);
        CellularAutomations.LifeWithoutDeath.addObserver(this);
        this.copy = new BitSet();
        this.copy = (BitSet) this.grid.clone();
        new Thread(new Runnable() {
            @Override
            public void run() {
                lifewithoutdeath.start();
            }
        }).start();
    }
}

class Screen5 extends Screen1 {
    Screen5(int n) {
        super(n);
    }

    @Override
    protected void start() {
        CellularAutomations.Diamoeba diamoeba = new CellularAutomations.Diamoeba(this.grid,
                this.sim.n);
        CellularAutomations.Diamoeba.addObserver(this);
        this.copy = new BitSet();
        this.copy = (BitSet) this.grid.clone();
        new Thread(new Runnable() {
            @Override
            public void run() {
                diamoeba.start();
            }
        }).start();
    }
}

class Screen6 extends Screen1 {
    Screen6(int n) {
        super(n);
    }

    @Override
    protected void start() {
        CellularAutomations.DayAndNight dayandnight = new CellularAutomations.DayAndNight(this.grid,
                this.sim.n);
        CellularAutomations.DayAndNight.addObserver(this);
        this.copy = new BitSet();
        this.copy = (BitSet) this.grid.clone();
        new Thread(new Runnable() {
            @Override
            public void run() {
                dayandnight.start();
            }
        }).start();
    }
}

abstract class CellularAutomationsGUI<T extends JComponent, U> extends JComponent {
    T screen;
    JFrame frame = new JFrame("Cellular Automata");
    JPanel leftPanel = new JPanel();
    JPanel rightPanel = new JPanel();

    CellularAutomationsGUI(T scr) {
        this.screen = scr;
        addDefaults();
        addControls();
        frame.setVisible(true);
        frame.pack();
    }

    int order = 0;

    abstract void setGrid(U grid, int n);

    abstract U getGrid();

    abstract int getN();

    abstract void start();

    abstract void randomize(int n);

    abstract void clearStats();

    abstract void setGenerationNumber();

    abstract void clearGrid(int n);

    abstract void resetGrid();

    abstract void togglePenMode();

    abstract String[] getStates();

    abstract void updateState(String n);

    void addDefaults() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, App.length);
        frame.setLayout(new BorderLayout());
        frame.add(leftPanel, BorderLayout.WEST);
        frame.add(rightPanel, BorderLayout.EAST);
        leftPanel.add(screen, BorderLayout.CENTER);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        JPanel sizer = new JPanel(new GridLayout(1, 2));
        JLabel label = new JLabel("Size(NxN): ");
        sizer.add(label);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(100, 5, App.length, 1));
        sizer.add(spinner);
        rightPanel.add(sizer);
        clearGrid((int) spinner.getValue());
        JButton setGrid = new JButton("Set Grid");
        rightPanel.add(setGrid);
        JPanel drawState = new JPanel(new GridLayout(1, 2));
        drawState.add(new JLabel("Draw State: "));
        JComboBox<String> states = new JComboBox<String>(getStates());
        drawState.add(states);
        rightPanel.add(drawState);
        states.addActionListener((e) -> {
            updateState((String) states.getSelectedItem());
        });
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("sim.drawStateIndex")) {
                states.setSelectedIndex((int) e.getNewValue());
            }
        });
        JButton start = new JButton("Start Simulation");
        rightPanel.add(start);
        setGrid.addActionListener((e) -> {
            if (start.getText().equals("Start Simulation")) {
                clearGrid((int) spinner.getValue());
            }
        });
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (start.getText().equals("Start Simulation")) {
                    CellularAutomations.stop = false;
                    start();
                    start.setText("Pause Simulation");
                } else if (start.getText().equals("Pause Simulation")) {
                    start.setText("Continue Simulation");
                    CellularAutomations.stop = true;
                } else if (start.getText().equals("Continue Simulation")) {
                    CellularAutomations.stop = false;
                    setGenerationNumber();
                    start.setText("Pause Simulation");
                    start();
                }
            }
        });
        JButton stop = new JButton("Reset Simulation");
        rightPanel.add(stop);
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CellularAutomations.stop = true;
                clearStats();
                start.setText("Start Simulation");
                resetGrid();
            }
        });
        JButton clear = new JButton("Clear Grid");
        clear.addActionListener((e) -> {
            CellularAutomations.stop = true;
            clearStats();
            start.setText("Start Simulation");
            clearGrid((int) spinner.getValue());
        });
        rightPanel.add(clear);
        JPanel loadArea = new JPanel(new GridLayout(1, 2));
        JTextArea loadName = new JTextArea("Enter pattern name here");
        JTextArea loadResult = new JTextArea();
        loadResult.setEditable(false);
        loadResult.setBackground(frame.getBackground());
        JButton load = new JButton("Load");
        rightPanel.add(loadArea);
        loadArea.add(loadName);
        loadArea.add(load);
        rightPanel.add(loadResult);
        load.addActionListener((e) -> {
            if (start.getText().equals("Start Simulation") || start.getText().equals("Continue Simulation")) {
                String name = loadName.getText();
                if (name.equals("") || name.equals("Enter pattern name here")) {
                    loadResult.setForeground(Color.RED);
                    loadResult.setText("Please enter name\nof pattern to load");
                } else {
                    PatternCollection<U> loader = new PatternCollection<U>(
                            "/mnt/c/Users/ENG Eldeeb/Desktop/CS50/JAVA/configurations.bin");
                    PatternCollection<U>.Pair result = loader.getPattern(name);
                    if (result != null) {
                        loadResult.setForeground(Color.GREEN);
                        loadResult.setText("Loaded: " + name);
                        spinner.setValue(result.n);
                        setGrid((U) result.set, result.n);
                    } else {
                        loadResult.setForeground(Color.RED);
                        loadResult.setText("Pattern not found");
                    }
                }
            } else {
                loadResult.setForeground(Color.RED);
                loadResult.setText("Please pause the simulation first");
            }
        });
        JPanel saveArea = new JPanel(new GridLayout(1, 2));
        JTextArea patternName = new JTextArea("Enter pattern name here");
        JTextArea saveResult = new JTextArea();
        saveResult.setEditable(false);
        saveResult.setBackground(frame.getBackground());
        JButton save = new JButton("Save");
        rightPanel.add(saveArea);
        saveArea.add(patternName);
        saveArea.add(save);
        save.addActionListener((e) -> {
            if (start.getText().equals("Start Simulation") || start.getText().equals("Continue Simulation")) {
                String name = patternName.getText();
                if (name.equals("") || name.equals("Enter pattern name here")) {
                    saveResult.setForeground(Color.RED);
                    saveResult.setText("Please enter name\nof pattern to save");
                } else {
                    PatternCollection<U> saver = new PatternCollection<U>(
                            "/mnt/c/Users/ENG Eldeeb/Desktop/CS50/JAVA/configurations.bin");
                    int overwrite = saver.addPattern(name, getGrid(), getN(), 0);
                    if (overwrite == 3) {
                        saveResult.setForeground(Color.GREEN);
                        saveResult.setText("Saved as: " + name);
                    }
                    if (overwrite == 1) {
                        JDialog overwriteDialog = new JDialog(frame, "Overwrite Pattern?", true);
                        overwriteDialog.setLayout(new GridLayout(3, 1));
                        overwriteDialog.add(new JLabel("There is a pattern with the same name. Overwrite?"));
                        JButton yesButton = new JButton("Yes");
                        yesButton.addActionListener((ee) -> {
                            saver.addPattern(name, getGrid(), getN(), 1);
                            saveResult.setForeground(Color.GREEN);
                            saveResult.setText("Overwritten: " + name);
                            overwriteDialog.dispose();
                        });
                        overwriteDialog.add(yesButton);
                        JButton noButton = new JButton("No");
                        noButton.addActionListener((ee) -> {
                            saver.addPattern(name, getGrid(), getN(), 2);
                            saveResult.setForeground(Color.RED);
                            saveResult.setText("Saving cancelled");
                            overwriteDialog.dispose();
                        });
                        overwriteDialog.add(noButton);
                        overwriteDialog.pack();
                        overwriteDialog.setLocationRelativeTo(frame);
                        overwriteDialog.setVisible(true);
                    }
                }
            } else {
                saveResult.setForeground(Color.RED);
                saveResult.setText("Please pause the simulation first");
            }
        });
        rightPanel.add(saveResult);
        JButton drawMode = new JButton("Pen Mode: Off");
        drawMode.addActionListener((e) -> {
            if (drawMode.getText().equals("Pen Mode: Off")) {
                togglePenMode();
                drawMode.setText("Pen Mode: On");
            } else {
                drawMode.setText("Pen Mode: Off");
                togglePenMode();
            }
        });
        rightPanel.add(drawMode);
        JLabel animationSpeed = new JLabel("Animation Speed");
        rightPanel.add(animationSpeed);
        JSlider slider = new JSlider(App.minDelay, App.maxDelay, 500);
        rightPanel.add(slider);
        slider.addChangeListener((e) -> {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                CellularAutomations.delay = (int) source.getValue();
            }
        });
        JButton randomize = new JButton("Randomize");
        randomize.addActionListener((e) -> {
            if (start.getText().equals("Start Simulation") || start.getText().equals("Continue Simulation")) {
                randomize((int) spinner.getValue());
            } else {
                saveResult.setForeground(Color.RED);
                saveResult.setText("Please pause the simulation first");
            }
        });
        rightPanel.add(randomize);
        JButton returnMenu = new JButton("Return");
        returnMenu.addActionListener((e) -> {
            frame.dispose();
            MainGUI menu = new MainGUI();
            menu.mainFrame.pack();
            menu.mainFrame.setVisible(true);
        });
        rightPanel.add(returnMenu);
    }

    public abstract void addControls();
}

class MainGUI extends JComponent {
    JFrame mainFrame = new JFrame("Main GUI");

    MainGUI() {
        mainFrame.setSize(1300, App.length);
        mainFrame.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        mainFrame.add(panel);
        JLabel title = new JLabel("Cellular Automata");
        JButton gameOfLifeButton = new JButton("Game of Life");
        gameOfLifeButton.addActionListener((e) -> {
            GameOfLifeGUI gameOfLifeGUI = new GameOfLifeGUI();
            mainFrame.dispose();
            gameOfLifeGUI.frame.add(gameOfLifeGUI);
            gameOfLifeGUI.frame.pack();
            gameOfLifeGUI.frame.setVisible(true);
        });
        JButton brianBrainButton = new JButton("Brian's Brain");
        brianBrainButton.addActionListener((e) -> {
            BrianGUI brianGUI = new BrianGUI();
            mainFrame.dispose();
            brianGUI.frame.add(brianGUI);
            brianGUI.frame.pack();
            brianGUI.frame.setVisible(true);
        });
        JButton seedsButton = new JButton("Seeds");
        seedsButton.addActionListener((e) -> {
            SeedsGUI seedsGUI = new SeedsGUI();
            mainFrame.dispose();
            seedsGUI.frame.add(seedsGUI);
            seedsGUI.frame.pack();
            seedsGUI.frame.setVisible(true);
        });
        JButton lifeWithoutDeathButton = new JButton("Life Without Death");
        lifeWithoutDeathButton.addActionListener((e) -> {
            LifeWithoutDeathGUI lifeWithoutDeathGUI = new LifeWithoutDeathGUI();
            lifeWithoutDeathGUI.frame.add(lifeWithoutDeathGUI);
            mainFrame.dispose();
            lifeWithoutDeathGUI.frame.pack();
            lifeWithoutDeathGUI.frame.setVisible(true);
        });
        JButton diamoebaButton = new JButton("Diamoeba");
        diamoebaButton.addActionListener((e) -> {
            DiamoebaGUI diamoebaGUI = new DiamoebaGUI();
            diamoebaGUI.frame.add(diamoebaGUI);
            mainFrame.dispose();
            diamoebaGUI.frame.pack();
            diamoebaGUI.frame.setVisible(true);
        });
        JButton dayNightButton = new JButton("Day and Night");
        dayNightButton.addActionListener((e) -> {
            DayAndNightGUI dayNightGUI = new DayAndNightGUI();
            dayNightGUI.frame.add(dayNightGUI);
            mainFrame.dispose();
            dayNightGUI.frame.pack();
            dayNightGUI.frame.setVisible(true);
        });
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        panel.add(title);
        JLabel author = new JLabel("Made by: Bakr Mohamed Bakr");
        panel.add(author);
        panel.add(gameOfLifeButton);
        panel.add(brianBrainButton);
        panel.add(seedsButton);
        panel.add(lifeWithoutDeathButton);
        panel.add(diamoebaButton);
        panel.add(dayNightButton);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
}

class GameOfLifeGUI extends CellularAutomationsGUI<Screen1, BitSet> {
    GameOfLifeGUI() {
        super(new Screen1(5));
        this.screen = (Screen1) super.screen;
    }

    @Override
    void start() {
        this.screen.start();
    }

    @Override
    void updateState(String n) {
        this.screen.sim.drawState = n;
    }

    @Override
    String[] getStates() {
        return this.screen.statesArray;
    }

    @Override
    void togglePenMode() {
        this.screen.sim.penMode = !this.screen.sim.penMode;
    }

    @Override
    void randomize(int n) {
        this.screen.randomize(n);
    }

    @Override
    void clearGrid(int n) {
        BitSet clear = new BitSet(n * n);
        this.screen.setGrid(clear, n);
    }

    @Override
    void setGenerationNumber() {
        this.screen.setGenerationNumber(this.screen.numberOfGenerations);
    }

    @Override
    void clearStats() {
        this.screen.clearStats();
    }

    @Override
    void setGrid(BitSet grid, int n) {
        this.screen.setGrid(grid, n);
    }

    @Override
    void resetGrid() {
        this.screen.setGrid(this.screen.copy, this.screen.sim.n);
    }

    @Override
    BitSet getGrid() {
        return this.screen.grid;
    }

    @Override
    int getN() {
        return this.screen.sim.n;
    }

    @Override
    public void addControls() {
        JLabel title = new JLabel("Game Of Life");
        title.setBackground(frame.getBackground());
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        rightPanel.add(title, order++);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints d = new GridBagConstraints();
        JLabel liveLabel = new JLabel("Live color:");
        JLabel deadLabel = new JLabel("Dead color:");
        Color[] colorings = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY,
                Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED,
                Color.WHITE, Color.YELLOW };
        String[] colorNames = { "Black", "Blue", "Cyan", "Dark Gray", "Gray",
                "Green", "Light Gray", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow" };
        JComboBox<String> liveBox = new JComboBox<>(colorNames);
        JComboBox<String> deadBox = new JComboBox<>(colorNames);
        d.gridx = 0;
        d.gridy = 0;
        panel.add(liveLabel, d);
        d.gridx = 1;
        panel.add(liveBox, d);
        d.gridy = 1;
        d.gridx = 0;
        panel.add(deadLabel, d);
        d.gridx = 1;
        panel.add(deadBox, d);
        rightPanel.add(panel, order++);
        JButton colorizeButton = new JButton("Colorize");
        colorizeButton.addActionListener((e) -> {
            screen.livecolor = colorings[liveBox.getSelectedIndex()];
            screen.deadcolor = colorings[deadBox.getSelectedIndex()];
            screen.repaint();
        });
        rightPanel.add(colorizeButton, order++);
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        JLabel generationNumberLabel = new JLabel("Generation Number:");
        JLabel liveCellsLabel = new JLabel("Live Cells:");
        JLabel growthRateLabel = new JLabel("Growth Rate:");
        JTextField generationNumberField = new JTextField();
        generationNumberField.setEditable(false);
        JTextField liveCellsField = new JTextField();
        liveCellsField.setEditable(false);
        JTextField growthRateField = new JTextField();
        growthRateField.setEditable(false);
        generationNumberField.setText(null);
        liveCellsField.setText(null);
        growthRateField.setText(null);
        statsPanel.add(generationNumberLabel);
        statsPanel.add(generationNumberField);
        statsPanel.add(liveCellsLabel);
        statsPanel.add(liveCellsField);
        statsPanel.add(growthRateLabel);
        statsPanel.add(growthRateField);
        rightPanel.add(statsPanel, order++);
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("numberOfGenerations")) {
                generationNumberField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("liveCells")) {
                liveCellsField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("growthPopulation")) {
                growthRateField.setText(String.format("%.2f%%", (double) e.getNewValue() *
                        100));
            }
        });
    }

}

class BrianGUI extends CellularAutomationsGUI<Screen2, CellularAutomations.State[][]> {
    BrianGUI() {
        super(new Screen2(5));
        this.screen = (Screen2) super.screen;
    }

    @Override
    void updateState(String n) {
        this.screen.sim.drawState = n;
    }

    @Override
    String[] getStates() {
        return this.screen.statesArray;
    }

    @Override
    void togglePenMode() {
        this.screen.sim.penMode = !this.screen.sim.penMode;
    }

    @Override
    void clearGrid(int n) {
        CellularAutomations.State[][] arr = new CellularAutomations.State[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                arr[i][j] = CellularAutomations.State.OFF;
        this.screen.setGrid(arr, n);
    }

    @Override
    void start() {
        this.screen.start();
    }

    @Override
    void randomize(int n) {
        this.screen.randomize(n);
    }

    @Override
    void setGenerationNumber() {
        this.screen.setGenerationNumber(this.screen.numberOfGenerations);
    }

    @Override
    void clearStats() {
        this.screen.clearStats();
    }

    @Override
    void setGrid(CellularAutomations.State[][] grid, int n) {
        this.screen.setGrid(grid, n);
    }

    @Override
    void resetGrid() {
        this.screen.setGrid(this.screen.copy, this.screen.sim.n);
    }

    @Override
    CellularAutomations.State[][] getGrid() {
        return this.screen.grid;
    }

    @Override
    int getN() {
        return this.screen.sim.n;
    }

    @Override
    public void addControls() {
        JLabel title = new JLabel("Brian's Brain");
        title.setBackground(frame.getBackground());
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        rightPanel.add(title, order++);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints d = new GridBagConstraints();
        JLabel liveLabel = new JLabel("Live color:");
        JLabel dyingLabel = new JLabel("Dying color:");
        JLabel deadLabel = new JLabel("Dead color:");
        Color[] colorings = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY,
                Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED,
                Color.WHITE, Color.YELLOW };
        String[] colorNames = { "Black", "Blue", "Cyan", "Dark Gray", "Gray",
                "Green", "Light Gray", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow" };
        JComboBox<String> liveBox = new JComboBox<>(colorNames);
        JComboBox<String> dyingBox = new JComboBox<>(colorNames);
        JComboBox<String> deadBox = new JComboBox<>(colorNames);
        d.gridx = 0;
        d.gridy = 0;
        panel.add(liveLabel, d);
        d.gridx = 1;
        panel.add(liveBox, d);
        d.gridy = 1;
        d.gridx = 0;
        panel.add(dyingLabel, d);
        d.gridx = 1;
        panel.add(dyingBox, d);
        d.gridy = 2;
        d.gridx = 0;
        panel.add(deadLabel, d);
        d.gridx = 1;
        panel.add(deadBox, d);
        rightPanel.add(panel, order++);
        JButton colorizeButton = new JButton("Colorize");
        colorizeButton.addActionListener((e) -> {
            screen.livecolor = colorings[liveBox.getSelectedIndex()];
            screen.dyingcolor = colorings[dyingBox.getSelectedIndex()];
            screen.deadcolor = colorings[deadBox.getSelectedIndex()];
            screen.repaint();
        });
        rightPanel.add(colorizeButton, order++);
        JPanel statsPanel = new JPanel(new GridLayout(4, 2));
        JLabel generationNumberLabel = new JLabel("Generation Number:");
        JLabel liveCellsLabel = new JLabel("Live Cells:");
        JLabel dyingCellsLabel = new JLabel("Dying Cells:");
        JLabel growthRateLabel = new JLabel("Growth Rate:");
        JTextField generationNumberField = new JTextField();
        generationNumberField.setEditable(false);
        JTextField liveCellsField = new JTextField();
        liveCellsField.setEditable(false);
        JTextField dyingCellsField = new JTextField();
        dyingCellsField.setEditable(false);
        JTextField growthRateField = new JTextField();
        growthRateField.setEditable(false);
        generationNumberField.setText(null);
        liveCellsField.setText(null);
        dyingCellsField.setText(null);
        growthRateField.setText(null);
        statsPanel.add(generationNumberLabel);
        statsPanel.add(generationNumberField);
        statsPanel.add(liveCellsLabel);
        statsPanel.add(liveCellsField);
        statsPanel.add(dyingCellsLabel);
        statsPanel.add(dyingCellsField);
        statsPanel.add(growthRateLabel);
        statsPanel.add(growthRateField);
        rightPanel.add(statsPanel, order++);
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("numberOfGenerations")) {
                generationNumberField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("liveCells")) {
                liveCellsField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("growthPopulation")) {
                growthRateField.setText(String.format("%.2f%%", (double) e.getNewValue() *
                        100));
            } else if (e.getPropertyName().equals("dyingCells")) {
                dyingCellsField.setText("" + e.getNewValue());
            }
        });
    }

}

class SeedsGUI extends CellularAutomationsGUI<Screen3, BitSet> {
    SeedsGUI() {
        super(new Screen3(5));
        this.screen = (Screen3) super.screen;
    }

    @Override
    void start() {
        this.screen.start();
    }

    @Override
    void updateState(String n) {
        this.screen.sim.drawState = n;
    }

    @Override
    String[] getStates() {
        return this.screen.statesArray;
    }

    @Override
    void togglePenMode() {
        this.screen.sim.penMode = !this.screen.sim.penMode;
    }

    @Override
    void randomize(int n) {
        this.screen.randomize(n);
    }

    @Override
    void clearGrid(int n) {
        BitSet clear = new BitSet(n * n);
        this.screen.setGrid(clear, n);
    }

    @Override
    void setGenerationNumber() {
        this.screen.setGenerationNumber(this.screen.numberOfGenerations);
    }

    @Override
    void clearStats() {
        this.screen.clearStats();
    }

    @Override
    void setGrid(BitSet grid, int n) {
        this.screen.setGrid(grid, n);
    }

    @Override
    void resetGrid() {
        this.screen.setGrid(this.screen.copy, this.screen.sim.n);
    }

    @Override
    BitSet getGrid() {
        return this.screen.grid;
    }

    @Override
    int getN() {
        return this.screen.sim.n;
    }

    @Override
    public void addControls() {
        JLabel title = new JLabel("Seeds");
        title.setBackground(frame.getBackground());
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        rightPanel.add(title, order++);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints d = new GridBagConstraints();
        JLabel liveLabel = new JLabel("Live color:");
        JLabel deadLabel = new JLabel("Dead color:");
        Color[] colorings = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY,
                Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED,
                Color.WHITE, Color.YELLOW };
        String[] colorNames = { "Black", "Blue", "Cyan", "Dark Gray", "Gray",
                "Green", "Light Gray", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow" };
        JComboBox<String> liveBox = new JComboBox<>(colorNames);
        JComboBox<String> deadBox = new JComboBox<>(colorNames);
        d.gridx = 0;
        d.gridy = 0;
        panel.add(liveLabel, d);
        d.gridx = 1;
        panel.add(liveBox, d);
        d.gridy = 1;
        d.gridx = 0;
        panel.add(deadLabel, d);
        d.gridx = 1;
        panel.add(deadBox, d);
        rightPanel.add(panel, order++);
        JButton colorizeButton = new JButton("Colorize");
        colorizeButton.addActionListener((e) -> {
            screen.livecolor = colorings[liveBox.getSelectedIndex()];
            screen.deadcolor = colorings[deadBox.getSelectedIndex()];
            screen.repaint();
        });
        rightPanel.add(colorizeButton, order++);
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        JLabel generationNumberLabel = new JLabel("Generation Number:");
        JLabel liveCellsLabel = new JLabel("Live Cells:");
        JLabel growthRateLabel = new JLabel("Growth Rate:");
        JTextField generationNumberField = new JTextField();
        generationNumberField.setEditable(false);
        JTextField liveCellsField = new JTextField();
        liveCellsField.setEditable(false);
        JTextField growthRateField = new JTextField();
        growthRateField.setEditable(false);
        generationNumberField.setText(null);
        liveCellsField.setText(null);
        growthRateField.setText(null);
        statsPanel.add(generationNumberLabel);
        statsPanel.add(generationNumberField);
        statsPanel.add(liveCellsLabel);
        statsPanel.add(liveCellsField);
        statsPanel.add(growthRateLabel);
        statsPanel.add(growthRateField);
        rightPanel.add(statsPanel, order++);
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("numberOfGenerations")) {
                generationNumberField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("liveCells")) {
                liveCellsField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("growthPopulation")) {
                growthRateField.setText(String.format("%.2f%%", (double) e.getNewValue() *
                        100));
            }
        });
    }
}

class LifeWithoutDeathGUI extends CellularAutomationsGUI<Screen4, BitSet> {
    LifeWithoutDeathGUI() {
        super(new Screen4(5));
        this.screen = (Screen4) super.screen;
    }

    @Override
    void start() {
        this.screen.start();
    }

    @Override
    void updateState(String n) {
        this.screen.sim.drawState = n;
    }

    @Override
    String[] getStates() {
        return this.screen.statesArray;
    }

    @Override
    void togglePenMode() {
        this.screen.sim.penMode = !this.screen.sim.penMode;
    }

    @Override
    void randomize(int n) {
        this.screen.randomize(n);
    }

    @Override
    void clearGrid(int n) {
        BitSet clear = new BitSet(n * n);
        this.screen.setGrid(clear, n);
    }

    @Override
    void setGenerationNumber() {
        this.screen.setGenerationNumber(this.screen.numberOfGenerations);
    }

    @Override
    void clearStats() {
        this.screen.clearStats();
    }

    @Override
    void setGrid(BitSet grid, int n) {
        this.screen.setGrid(grid, n);
    }

    @Override
    void resetGrid() {
        this.screen.setGrid(this.screen.copy, this.screen.sim.n);
    }

    @Override
    BitSet getGrid() {
        return this.screen.grid;
    }

    @Override
    int getN() {
        return this.screen.sim.n;
    }

    @Override
    public void addControls() {
        JLabel title = new JLabel("Life Without Death");
        title.setBackground(frame.getBackground());
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        rightPanel.add(title, order++);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints d = new GridBagConstraints();
        JLabel liveLabel = new JLabel("Live color:");
        JLabel deadLabel = new JLabel("Dead color:");
        Color[] colorings = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY,
                Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED,
                Color.WHITE, Color.YELLOW };
        String[] colorNames = { "Black", "Blue", "Cyan", "Dark Gray", "Gray",
                "Green", "Light Gray", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow" };
        JComboBox<String> liveBox = new JComboBox<>(colorNames);
        JComboBox<String> deadBox = new JComboBox<>(colorNames);
        d.gridx = 0;
        d.gridy = 0;
        panel.add(liveLabel, d);
        d.gridx = 1;
        panel.add(liveBox, d);
        d.gridy = 1;
        d.gridx = 0;
        panel.add(deadLabel, d);
        d.gridx = 1;
        panel.add(deadBox, d);
        rightPanel.add(panel, order++);
        JButton colorizeButton = new JButton("Colorize");
        colorizeButton.addActionListener((e) -> {
            screen.livecolor = colorings[liveBox.getSelectedIndex()];
            screen.deadcolor = colorings[deadBox.getSelectedIndex()];
            screen.repaint();
        });
        rightPanel.add(colorizeButton, order++);
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        JLabel generationNumberLabel = new JLabel("Generation Number:");
        JLabel liveCellsLabel = new JLabel("Live Cells:");
        JLabel growthRateLabel = new JLabel("Growth Rate:");
        JTextField generationNumberField = new JTextField();
        generationNumberField.setEditable(false);
        JTextField liveCellsField = new JTextField();
        liveCellsField.setEditable(false);
        JTextField growthRateField = new JTextField();
        growthRateField.setEditable(false);
        generationNumberField.setText(null);
        liveCellsField.setText(null);
        growthRateField.setText(null);
        statsPanel.add(generationNumberLabel);
        statsPanel.add(generationNumberField);
        statsPanel.add(liveCellsLabel);
        statsPanel.add(liveCellsField);
        statsPanel.add(growthRateLabel);
        statsPanel.add(growthRateField);
        rightPanel.add(statsPanel, order++);
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("numberOfGenerations")) {
                generationNumberField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("liveCells")) {
                liveCellsField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("growthPopulation")) {
                growthRateField.setText(String.format("%.2f%%", (double) e.getNewValue() *
                        100));
            }
        });
    }
}

class DiamoebaGUI extends CellularAutomationsGUI<Screen5, BitSet> {
    DiamoebaGUI() {
        super(new Screen5(5));
        this.screen = (Screen5) super.screen;
    }

    @Override
    void start() {
        this.screen.start();
    }

    @Override
    void updateState(String n) {
        while (!this.screen.sim.statesArray[screen.sim.drawStateindex].equals(n)) {
            screen.sim.drawStateindex = ((screen.sim.drawStateindex + 1) % screen.sim.statesArray.length
                    + screen.sim.statesArray.length) % screen.sim.statesArray.length;
        }
        this.screen.sim.drawState = n;
    }

    @Override
    String[] getStates() {
        return this.screen.statesArray;
    }

    @Override
    void togglePenMode() {
        this.screen.sim.penMode = !this.screen.sim.penMode;
    }

    @Override
    void randomize(int n) {
        this.screen.randomize(n);
    }

    @Override
    void clearGrid(int n) {
        BitSet clear = new BitSet(n * n);
        this.screen.setGrid(clear, n);
    }

    @Override
    void setGenerationNumber() {
        this.screen.setGenerationNumber(this.screen.numberOfGenerations);
    }

    @Override
    void clearStats() {
        this.screen.clearStats();
    }

    @Override
    void setGrid(BitSet grid, int n) {
        this.screen.setGrid(grid, n);
    }

    @Override
    void resetGrid() {
        this.screen.setGrid(this.screen.copy, this.screen.sim.n);
    }

    @Override
    BitSet getGrid() {
        return this.screen.grid;
    }

    @Override
    int getN() {
        return this.screen.sim.n;
    }

    @Override
    public void addControls() {
        JLabel title = new JLabel("Diamoeba");
        title.setBackground(frame.getBackground());
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        rightPanel.add(title, order++);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints d = new GridBagConstraints();
        JLabel liveLabel = new JLabel("Live color:");
        JLabel deadLabel = new JLabel("Dead color:");
        Color[] colorings = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY,
                Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED,
                Color.WHITE, Color.YELLOW };
        String[] colorNames = { "Black", "Blue", "Cyan", "Dark Gray", "Gray",
                "Green", "Light Gray", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow" };
        JComboBox<String> liveBox = new JComboBox<>(colorNames);
        JComboBox<String> deadBox = new JComboBox<>(colorNames);
        d.gridx = 0;
        d.gridy = 0;
        panel.add(liveLabel, d);
        d.gridx = 1;
        panel.add(liveBox, d);
        d.gridy = 1;
        d.gridx = 0;
        panel.add(deadLabel, d);
        d.gridx = 1;
        panel.add(deadBox, d);
        rightPanel.add(panel, order++);
        JButton colorizeButton = new JButton("Colorize");
        colorizeButton.addActionListener((e) -> {
            screen.livecolor = colorings[liveBox.getSelectedIndex()];
            screen.deadcolor = colorings[deadBox.getSelectedIndex()];
            screen.repaint();
        });
        rightPanel.add(colorizeButton, order++);
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        JLabel generationNumberLabel = new JLabel("Generation Number:");
        JLabel liveCellsLabel = new JLabel("Live Cells:");
        JLabel growthRateLabel = new JLabel("Growth Rate:");
        JTextField generationNumberField = new JTextField();
        generationNumberField.setEditable(false);
        JTextField liveCellsField = new JTextField();
        liveCellsField.setEditable(false);
        JTextField growthRateField = new JTextField();
        growthRateField.setEditable(false);
        generationNumberField.setText(null);
        liveCellsField.setText(null);
        growthRateField.setText(null);
        statsPanel.add(generationNumberLabel);
        statsPanel.add(generationNumberField);
        statsPanel.add(liveCellsLabel);
        statsPanel.add(liveCellsField);
        statsPanel.add(growthRateLabel);
        statsPanel.add(growthRateField);
        rightPanel.add(statsPanel, order++);
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("numberOfGenerations")) {
                generationNumberField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("liveCells")) {
                liveCellsField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("growthPopulation")) {
                growthRateField.setText(String.format("%.2f%%", (double) e.getNewValue() *
                        100));
            }
        });
    }
}

class DayAndNightGUI extends CellularAutomationsGUI<Screen6, BitSet> {
    DayAndNightGUI() {
        super(new Screen6(5));
        this.screen = (Screen6) super.screen;
    }

    @Override
    void start() {
        this.screen.start();
    }

    @Override
    void updateState(String n) {
        while (!this.screen.sim.statesArray[screen.sim.drawStateindex].equals(n)) {
            screen.sim.drawStateindex = ((screen.sim.drawStateindex + 1) % screen.sim.statesArray.length
                    + screen.sim.statesArray.length) % screen.sim.statesArray.length;
        }
        this.screen.sim.drawState = n;
    }

    @Override
    String[] getStates() {
        return this.screen.statesArray;
    }

    @Override
    void togglePenMode() {
        this.screen.sim.penMode = !this.screen.sim.penMode;
    }

    @Override
    void randomize(int n) {
        this.screen.randomize(n);
    }

    @Override
    void clearGrid(int n) {
        BitSet clear = new BitSet(n * n);
        this.screen.setGrid(clear, n);
    }

    @Override
    void setGenerationNumber() {
        this.screen.setGenerationNumber(this.screen.numberOfGenerations);
    }

    @Override
    void clearStats() {
        this.screen.clearStats();
    }

    @Override
    void setGrid(BitSet grid, int n) {
        this.screen.setGrid(grid, n);
    }

    @Override
    void resetGrid() {
        this.screen.setGrid(this.screen.copy, this.screen.sim.n);
    }

    @Override
    BitSet getGrid() {
        return this.screen.grid;
    }

    @Override
    int getN() {
        return this.screen.sim.n;
    }

    @Override
    public void addControls() {
        JLabel title = new JLabel("Day and Night");
        title.setBackground(frame.getBackground());
        title.setFont(new Font("Papyrus", Font.BOLD, 25));
        rightPanel.add(title, order++);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints d = new GridBagConstraints();
        JLabel liveLabel = new JLabel("Live color:");
        JLabel deadLabel = new JLabel("Dead color:");
        Color[] colorings = { Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY,
                Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED,
                Color.WHITE, Color.YELLOW };
        String[] colorNames = { "Black", "Blue", "Cyan", "Dark Gray", "Gray",
                "Green", "Light Gray", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow" };
        JComboBox<String> liveBox = new JComboBox<>(colorNames);
        JComboBox<String> deadBox = new JComboBox<>(colorNames);
        d.gridx = 0;
        d.gridy = 0;
        panel.add(liveLabel, d);
        d.gridx = 1;
        panel.add(liveBox, d);
        d.gridy = 1;
        d.gridx = 0;
        panel.add(deadLabel, d);
        d.gridx = 1;
        panel.add(deadBox, d);
        rightPanel.add(panel, order++);
        JButton colorizeButton = new JButton("Colorize");
        colorizeButton.addActionListener((e) -> {
            screen.livecolor = colorings[liveBox.getSelectedIndex()];
            screen.deadcolor = colorings[deadBox.getSelectedIndex()];
            screen.repaint();
        });
        rightPanel.add(colorizeButton, order++);
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        JLabel generationNumberLabel = new JLabel("Generation Number:");
        JLabel liveCellsLabel = new JLabel("Live Cells:");
        JLabel growthRateLabel = new JLabel("Growth Rate:");
        JTextField generationNumberField = new JTextField();
        generationNumberField.setEditable(false);
        JTextField liveCellsField = new JTextField();
        liveCellsField.setEditable(false);
        JTextField growthRateField = new JTextField();
        growthRateField.setEditable(false);
        generationNumberField.setText(null);
        liveCellsField.setText(null);
        growthRateField.setText(null);
        statsPanel.add(generationNumberLabel);
        statsPanel.add(generationNumberField);
        statsPanel.add(liveCellsLabel);
        statsPanel.add(liveCellsField);
        statsPanel.add(growthRateLabel);
        statsPanel.add(growthRateField);
        rightPanel.add(statsPanel, order++);
        screen.addPropertyChangeListener((e) -> {
            if (e.getPropertyName().equals("numberOfGenerations")) {
                generationNumberField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("liveCells")) {
                liveCellsField.setText("" + e.getNewValue());
            } else if (e.getPropertyName().equals("growthPopulation")) {
                growthRateField.setText(String.format("%.2f%%", (double) e.getNewValue() *
                        100));
            }
        });
    }
}