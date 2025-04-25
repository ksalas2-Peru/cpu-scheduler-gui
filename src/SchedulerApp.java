import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.*;
import java.util.stream.Collectors;

public class SchedulerApp extends Application {
    private TableView<Process> processTable;
    private TextArea resultArea;
    private Pane ganttPane;
    private ComboBox<String> algorithmBox;
    private final int TIME_UNIT_WIDTH = 20;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CPU Scheduling Simulator");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label titleLabel = new Label("CPU Scheduling Simulator");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        algorithmBox = new ComboBox<>();
        algorithmBox.getItems().addAll("FCFS", "SJF", "SRTF", "HRRN");
        algorithmBox.setValue("FCFS");

        Button generateButton = new Button("Generate Processes (25)");
        Button runButton = new Button("Run Scheduling");

        generateButton.setOnAction(e -> generateProcesses(25));
        runButton.setOnAction(e -> runScheduling());

        HBox controlRow = new HBox(10, new Label("Algorithm:"), algorithmBox, generateButton, runButton);

        processTable = new TableView<>();
        processTable.setItems(FXCollections.observableArrayList());
        processTable.setMinHeight(200);

        TableColumn<Process, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty().asObject());

        TableColumn<Process, Integer> arrivalCol = new TableColumn<>("Arrival Time");
        arrivalCol.setCellValueFactory(data -> data.getValue().arrivalProperty().asObject());

        TableColumn<Process, Integer> burstCol = new TableColumn<>("Burst Time");
        burstCol.setCellValueFactory(data -> data.getValue().burstProperty().asObject());

        TableColumn<Process, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data -> data.getValue().priorityProperty().asObject());

        processTable.getColumns().addAll(idCol, arrivalCol, burstCol, priorityCol);

        Label resultsLabel = new Label("Results:");
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(150);

        Label ganttLabel = new Label("Gantt Chart:");
        ganttPane = new Pane();
        ganttPane.setPrefHeight(100);
        ganttPane.setStyle("-fx-border-color: black;");

        root.getChildren().addAll(titleLabel, controlRow, processTable, resultsLabel, resultArea, ganttLabel, ganttPane);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void generateProcesses(int count) {
        Random rand = new Random();
        List<Process> processes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            int arrival = rand.nextInt(20);
            int burst = rand.nextInt(10) + 1;
            int priority = rand.nextInt(10) + 1;
            processes.add(new Process(i, arrival, burst, priority));
        }
        processTable.setItems(FXCollections.observableArrayList(processes));
    }

    private void runScheduling() {
        String algo = algorithmBox.getValue();
        List<Process> originalList = new ArrayList<>(processTable.getItems());
        Scheduler scheduler;
        switch (algo) {
            case "SJF":
                scheduler = new SJF(); break;
            case "SRTF":
                scheduler = new SRTF(); break;
            case "HRRN":
                scheduler = new HRRN(); break;
            default:
                scheduler = new FCFS(); break;
        }
        List<ScheduledProcess> result = scheduler.schedule(originalList);
        drawGantt(result);
        displayMetrics(result);
    }

    private void drawGantt(List<ScheduledProcess> scheduled) {
        ganttPane.getChildren().clear();
        int currentX = 0;
        for (ScheduledProcess sp : scheduled) {
            Rectangle rect = new Rectangle(currentX, 10, sp.duration * TIME_UNIT_WIDTH, 30);
            rect.setFill(Color.LIGHTBLUE);
            Label label = new Label("P" + sp.id);
            label.setLayoutX(currentX + 5);
            label.setLayoutY(15);
            ganttPane.getChildren().addAll(rect, label);
            currentX += sp.duration * TIME_UNIT_WIDTH;
        }
    }

    private void displayMetrics(List<ScheduledProcess> scheduled) {
        double totalWT = 0, totalTT = 0;
        int lastTime = 0;
        Map<Integer, Integer> arrivalMap = new HashMap<>();
        for (ScheduledProcess sp : scheduled) {
            arrivalMap.putIfAbsent(sp.id, sp.arrival);
            totalWT += sp.start - sp.arrival;
            totalTT += (sp.start + sp.duration) - sp.arrival;
            lastTime = Math.max(lastTime, sp.start + sp.duration);
        }
        int count = (int) scheduled.stream().map(sp -> sp.id).distinct().count();
        double awt = totalWT / count;
        double att = totalTT / count;
        double cpuUtil = 100.0 * scheduled.stream().mapToInt(sp -> sp.duration).sum() / lastTime;
        double throughput = (double) count / lastTime;
        resultArea.setText(String.format("AWT: %.2f\nATT: %.2f\nCPU Utilization: %.2f%%\nThroughput: %.2f proc/sec",
                awt, att, cpuUtil, throughput));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Process {
    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty arrival;
    private final SimpleIntegerProperty burst;
    private final SimpleIntegerProperty priority;

    public Process(int id, int arrival, int burst, int priority) {
        this.id = new SimpleIntegerProperty(id);
        this.arrival = new SimpleIntegerProperty(arrival);
        this.burst = new SimpleIntegerProperty(burst);
        this.priority = new SimpleIntegerProperty(priority);
    }
    public int getId() { return id.get(); }
    public int getArrival() { return arrival.get(); }
    public int getBurst() { return burst.get(); }
    public int getPriority() { return priority.get(); }
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleIntegerProperty arrivalProperty() { return arrival; }
    public SimpleIntegerProperty burstProperty() { return burst; }
    public SimpleIntegerProperty priorityProperty() { return priority; }
}

interface Scheduler {
    List<ScheduledProcess> schedule(List<Process> processes);
}

class ScheduledProcess {
    int id, arrival, start, duration;
    public ScheduledProcess(int id, int arrival, int start, int duration) {
        this.id = id;
        this.arrival = arrival;
        this.start = start;
        this.duration = duration;
    }
}

class FCFS implements Scheduler {
    public List<ScheduledProcess> schedule(List<Process> processes) {
        List<Process> sorted = processes.stream().sorted(Comparator.comparingInt(Process::getArrival)).collect(Collectors.toList());
        List<ScheduledProcess> result = new ArrayList<>();
        int time = 0;
        for (Process p : sorted) {
            time = Math.max(time, p.getArrival());
            result.add(new ScheduledProcess(p.getId(), p.getArrival(), time, p.getBurst()));
            time += p.getBurst();
        }
        return result;
    }
}

class SJF implements Scheduler {
    public List<ScheduledProcess> schedule(List<Process> processes) {
        List<ScheduledProcess> result = new ArrayList<>();
        List<Process> ready = new ArrayList<>();
        int time = 0;
        List<Process> queue = new ArrayList<>(processes);
        while (!queue.isEmpty() || !ready.isEmpty()) {
            for (Iterator<Process> it = queue.iterator(); it.hasNext();) {
                Process p = it.next();
                if (p.getArrival() <= time) {
                    ready.add(p);
                    it.remove();
                }
            }
            if (ready.isEmpty()) {
                time++;
                continue;
            }
            Process next = ready.stream().min(Comparator.comparingInt(Process::getBurst)).get();
            ready.remove(next);
            result.add(new ScheduledProcess(next.getId(), next.getArrival(), time, next.getBurst()));
            time += next.getBurst();
        }
        return result;
    }
}

class SRTF implements Scheduler {
    public List<ScheduledProcess> schedule(List<Process> processes) {
        List<ScheduledProcess> result = new ArrayList<>();
        Map<Integer, Integer> remaining = new HashMap<>();
        for (Process p : processes) remaining.put(p.getId(), p.getBurst());

        int time = 0, completed = 0, n = processes.size();
        Process running = null;

        while (completed < n) {
            List<Process> available = new ArrayList<>();
            for (Process p : processes) {
                if (p.getArrival() <= time && remaining.get(p.getId()) > 0) {
                    available.add(p);
                }
            }

            if (!available.isEmpty()) {
                Process shortest = available.get(0);
                for (Process p : available) {
                    if (remaining.get(p.getId()) < remaining.get(shortest.getId())) {
                        shortest = p;
                    }
                }

                result.add(new ScheduledProcess(shortest.getId(), shortest.getArrival(), time, 1));
                remaining.put(shortest.getId(), remaining.get(shortest.getId()) - 1);

                if (remaining.get(shortest.getId()) == 0) {
                    completed++;
                }
            }
            time++;
        }
        return result;
    }
}
class HRRN implements Scheduler {
    public List<ScheduledProcess> schedule(List<Process> processes) {
        List<ScheduledProcess> result = new ArrayList<>();
        List<Process> queue = new ArrayList<>(processes);
        int time = 0;

        while (!queue.isEmpty()) {
            List<Process> ready = new ArrayList<>();
            for (Process p : queue) {
                if (p.getArrival() <= time) {
                    ready.add(p);
                }
            }

            if (ready.isEmpty()) {
                time++;
                continue;
            }

            Process selected = ready.get(0);
            double highestResponseRatio = -1;

            for (Process p : ready) {
                int waitingTime = time - p.getArrival();
                double responseRatio = (waitingTime + p.getBurst()) / (double) p.getBurst();
                if (responseRatio > highestResponseRatio) {
                    highestResponseRatio = responseRatio;
                    selected = p;
                }
            }

            queue.remove(selected);
            result.add(new ScheduledProcess(selected.getId(), selected.getArrival(), time, selected.getBurst()));
            time += selected.getBurst();
        }

        return result;
    }
}
