# CPU Scheduling Simulator (JavaFX)

## Overview
This project is a CPU Scheduling Simulator built using JavaFX.  
It implements four scheduling algorithms:
- First Come First Served (FCFS)
- Shortest Job First (SJF)
- Shortest Remaining Time First (SRTF) *(new algorithm)*
- Highest Response Ratio Next (HRRN) *(new algorithm)*

It supports performance evaluation based on:
- Average Waiting Time (AWT)
- Average Turnaround Time (ATT)
- CPU Utilization
- Throughput (Processes/sec)

The app features a simple graphical user interface (GUI) that displays process tables, metrics, and a Gantt chart.

---

## How to Run

1. Ensure you have [JavaFX SDK](https://gluonhq.com/products/javafx/) installed.
2. In IntelliJ IDEA:
    - Mark `src` as Sources Root.
    - Add JavaFX libraries to your project.
    - Set VM options for running:
      ```
      --module-path "C:\path\to\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml
      ```
3. Run `SchedulerApp.java`.

---

## Project Structure
