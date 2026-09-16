import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.Random;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Convolution1D extends JPanel {

    private static final int SAMPLE_COUNT = 200;
    private static final double DEFAULT_FILTER_COEFFICIENT = 0.5;
    private static final Double[] FILTER_COEFFICIENT_OPTIONS = { 0.1, 0.2, 0.5, 1.0, 2.0 };

    private final double[] noiseSignal = new double[SAMPLE_COUNT];
    private final double[] originalSignal = new double[SAMPLE_COUNT];
    private final double[] corruptedSignal = new double[SAMPLE_COUNT];

    private double[] filter;
    private double[] filteredSignal;

    private final JComboBox<Double> coefficientComboBox;
    private final JTable dataTable;
    private final DefaultTableModel tableModel;
    private final SignalPlotPanel plotPanel;

    public Convolution1D() {
        setLayout(new BorderLayout());

        generateSignals();
        updateFilterAndConvolution(DEFAULT_FILTER_COEFFICIENT);

        String[] columns = { "n", "Original", "Noise", "Corrupted", "Filter", "Filtered" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dataTable = new JTable(tableModel);
        dataTable.setFillsViewportHeight(true);
        dataTable.setDefaultRenderer(Object.class, new SignalTableCellRenderer());

        JScrollPane tableScrollPane = new JScrollPane(dataTable);
        plotPanel = new SignalPlotPanel();

        coefficientComboBox = new JComboBox<>(FILTER_COEFFICIENT_OPTIONS);
        coefficientComboBox.setSelectedItem(DEFAULT_FILTER_COEFFICIENT);
        coefficientComboBox.addActionListener(event -> {
            Double selectedCoefficient = (Double) coefficientComboBox.getSelectedItem();
            if (selectedCoefficient != null) {
                updateData(selectedCoefficient);
            }
        });

        JPanel controlPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        controlPanel.add(new JLabel("Filter coefficient 'a':", SwingConstants.RIGHT));
        controlPanel.add(coefficientComboBox);

        add(controlPanel, BorderLayout.NORTH);
        add(plotPanel, BorderLayout.CENTER);
        add(tableScrollPane, BorderLayout.EAST);

        refreshTable();
    }

    private void generateSignals() {
        for (int n = 0; n < SAMPLE_COUNT; n++) {
            originalSignal[n] = Math.sin(0.05 * n) + 0.5 * Math.sin(0.15 * n);
        }

        Random random = new Random();
        for (int n = 0; n < SAMPLE_COUNT; n++) {
            noiseSignal[n] = random.nextDouble() * 0.4 - 0.2;
            corruptedSignal[n] = originalSignal[n] + noiseSignal[n];
        }
    }

    private void updateFilterAndConvolution(double coefficient) {
        if (!Double.isFinite(coefficient)) {
            throw new IllegalArgumentException("The filter coefficient must be finite.");
        }

        filter = new double[SAMPLE_COUNT];
        for (int n = 0; n < SAMPLE_COUNT; n++) {
            filter[n] = (1.0 - coefficient) * Math.pow(coefficient, n);
        }

        filteredSignal = convolve(corruptedSignal, filter);
    }

    private static double[] convolve(double[] signal, double[] impulseResponse) {
        if (signal == null || impulseResponse == null) {
            throw new IllegalArgumentException("Signal and impulse response must not be null.");
        }
        if (signal.length == 0 || impulseResponse.length == 0) {
            throw new IllegalArgumentException("Signal and impulse response must not be empty.");
        }

        int resultLength = signal.length + impulseResponse.length - 1;
        double[] result = new double[resultLength];

        for (int n = 0; n < resultLength; n++) {
            int firstSignalIndex = Math.max(0, n - impulseResponse.length + 1);
            int lastSignalIndex = Math.min(n, signal.length - 1);

            for (int signalIndex = firstSignalIndex; signalIndex <= lastSignalIndex; signalIndex++) {
                int filterIndex = n - signalIndex;
                result[n] += signal[signalIndex] * impulseResponse[filterIndex];
            }
        }

        return result;
    }

    private void updateData(double coefficient) {
        updateFilterAndConvolution(coefficient);
        refreshTable();
        plotPanel.repaint();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        int rowCount = Math.max(SAMPLE_COUNT, filteredSignal.length);

        for (int n = 0; n < rowCount; n++) {
            Object originalValue = n < originalSignal.length ? originalSignal[n] : "";
            Object noiseValue = n < noiseSignal.length ? noiseSignal[n] : "";
            Object corruptedValue = n < corruptedSignal.length ? corruptedSignal[n] : "";
            Object filterValue = n < filter.length ? filter[n] : "";
            Object filteredValue = n < filteredSignal.length ? filteredSignal[n] : "";

            tableModel.addRow(new Object[] {
                    n,
                    originalValue,
                    noiseValue,
                    corruptedValue,
                    filterValue,
                    filteredValue
            });
        }
    }

    private class SignalPlotPanel extends JPanel {
        private static final int LEFT_MARGIN = 50;
        private static final int RIGHT_MARGIN = 20;
        private static final int VERTICAL_MARGIN = 20;
        private static final int Y_SCALE = 100;

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int xAxisStart = LEFT_MARGIN;
            int xAxisEnd = Math.max(xAxisStart, panelWidth - RIGHT_MARGIN);
            int yAxisPosition = panelHeight / 2;
            int graphWidth = Math.max(1, xAxisEnd - xAxisStart);
            int graphSampleCount = Math.max(originalSignal.length, filteredSignal.length);

            graphics.setColor(Color.BLACK);
            graphics.drawLine(xAxisStart, yAxisPosition, xAxisEnd, yAxisPosition);
            graphics.drawLine(xAxisStart, VERTICAL_MARGIN, xAxisStart, panelHeight - VERTICAL_MARGIN);

            graphics.setColor(Color.BLUE);
            drawGraph(graphics, originalSignal, xAxisStart, yAxisPosition, graphWidth, graphSampleCount);

            graphics.setColor(Color.RED);
            drawGraph(graphics, corruptedSignal, xAxisStart, yAxisPosition, graphWidth, graphSampleCount);

            graphics.setColor(Color.GREEN.darker());
            drawGraph(graphics, filteredSignal, xAxisStart, yAxisPosition, graphWidth, graphSampleCount);
        }

        private void drawGraph(
                Graphics graphics,
                double[] data,
                int xAxisStart,
                int yAxisPosition,
                int graphWidth,
                int graphSampleCount) {

            if (data == null || data.length == 0 || graphSampleCount <= 1) {
                return;
            }

            int previousX = xAxisStart;
            int previousY = toScreenY(data[0], yAxisPosition);

            for (int i = 1; i < data.length; i++) {
                int x = xAxisStart + i * graphWidth / (graphSampleCount - 1);
                int y = toScreenY(data[i], yAxisPosition);
                graphics.drawLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
        }

        private int toScreenY(double value, int yAxisPosition) {
            if (!Double.isFinite(value)) {
                return value > 0 ? VERTICAL_MARGIN : getHeight() - VERTICAL_MARGIN;
            }

            double rawY = yAxisPosition - value * Y_SCALE;
            double minimumY = VERTICAL_MARGIN;
            double maximumY = Math.max(VERTICAL_MARGIN, getHeight() - VERTICAL_MARGIN);
            return (int) Math.round(Math.max(minimumY, Math.min(maximumY, rawY)));
        }
    }

    private static class SignalTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component component = super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            if (!isSelected) {
                switch (column) {
                    case 1:
                        component.setForeground(Color.BLUE);
                        break;
                    case 3:
                        component.setForeground(Color.RED);
                        break;
                    case 5:
                        component.setForeground(Color.GREEN.darker());
                        break;
                    default:
                        component.setForeground(Color.BLACK);
                        break;
                }
            }

            return component;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Convolution1D graphPanel = new Convolution1D();

            JFrame frame = new JFrame("1D Convolution - Signals and Data");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(graphPanel);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });
    }
}
