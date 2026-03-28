# DailyReceipt
### Author: Tyler Black
An "assistant" powered by an LLM that prints a personalized report using a physical receipt printer
on a configured schedule.

## Description
DailyReceiptLLMPrinter is a modular system that integrates a Large Language Model (LLM) and various configured API's to
generate and print personalized daily receipts. The project is split into two primary components: a Java-based Executor 
that handles scheduling, data fetching, and logging, and a Python-based Flask API that manages the LLM interaction 
and physical thermal printing.

## Example Print

```text
        Receipt #14                    27 Mar 2026
        
                 Sticky Springs, Durango          
            Ugh, another day. The weather's...
        tolerable. 56.2 feels like a cruel joke
        when it's supposed to be 72.9. Feels like
        a damp blanket, frankly. High chance of
        precipitation - 6% - isn't exactly
        inspiring.  Looks like we're celebrating
        Exchange Day, Whisky Day, and National Joe
        Day. Honestly, the world is just *too*
        festive.  Easter Sunday's looming, April
        5th.  And a bunch of events scattered
        around. Denver, <personal info removed>,
        birthdays... it's a chaotic mess.  Let's
        hope the Scribble Day inspires some actual
        creativity, not just more doodles.
        Honestly, a quiet day would be nice.
        
        
        
               _.--._          ,---.        ,---. 
              /   _  \        |   |        |   |  
             |   / \  |        |   |        |   | 
             |  |   |  |        '---'        '---'
             \   \_/  /         |   |        |   |
              `-----'          `---'        `---' 
                      Rainy Receipt  
```

## Architecture

The system operates across two distinct codebases communicating via API calls:

### 1. Java Executor (`Executor`)
Acts as the central controller and data dispatcher.
* **App / System**: Contains the main application loop, handles time/scheduling (`HourMin`), and centrally manages logging (`Logger`).
* **API Fetcher**: Interfaces with external services (like Google Calendar) to gather the daily data needed for the receipt.
* **Data Dispatcher**: Packages the fetched data and sends it to the Python API for processing and printing.

### 2. Python API (`LLMReceiptPrintAPI`)
Acts as the processing engine and hardware interface.
* **Web (`web`)**: A Flask application (`app.py`, `main.py`) that receives payloads from the Java Executor.
* **Model Runner (`runmodel`)**: Formats the incoming data, constructs prompts, and queries the LLM to generate the final receipt text.
* **Printer (`receiptprinter`)**: Interfaces directly with the physical thermal printer to output the LLM-generated text.

---

## Directory Structure

    DailyReceiptLLMPrinter/
    ├── Executor/                           # Java Application
    │   ├── build.gradle.kts
    │   └── src/
    │       └── main/
    │           ├── java/
    │           │   └── receiptprint/
    │           │       ├── api/
    │           │       │   └── impl/
    │           │       │       ├── APIFetcher.java
    │           │       │       ├── ApiFetchException.java
    │           │       │       └── IApiFetcher.java
    │           │       ├── app/
    │           │       │   └── Main.java
    │           │       └── system/
    │           │           ├── DataDispatcher.java
    │           │           ├── HourMin.java
    │           │           └── Logger.java
    │           └── resources/              # Ignored files in here
    │               ├── config.json
    │               ├── GoogleCalCred.json  
    │               ├── RandomWords.csv
    │               └── secrets.json
    │
    └── LLMReceiptPrintAPI/                 # Python Flask API
        ├── requirements.txt
        └── src/
            ├── path/
            │   └── paths.py
            ├── receiptprinter/
            │   ├── config.txt
            │   ├── receipt_number.txt      # Tracks current receipt 
            │   ├── receipt_print.py
            │   └── util.py
            ├── runmodel/
            │   ├── config/                 # LLM Prompts & Configs
            │   │   ├── model_config.json
            │   │   ├── system.md
            │   │   └── user.md
            │   ├── response_formatter.py
            │   └── run_model.py
            └── web/
                ├── app.py
                ├── PrinterError.py
                └── main.py

---

## Configuration & Files to Add

Before running the application, you must populate several configuration and secret files that are excluded from version control.

**Java Resources (`Executor/src/main/resources/`):**
* `GoogleCalCred.json`: Your Google Service Account credentials for Calendar API access.
* `secrets.json`: Contains API keys and other sensitive tokens.
* `config.json`: General Java application configuration (timing, endpoints, etc.).

**Python Resources (`LLMReceiptPrintAPI/src/`):**
* `receiptprinter/receipt_number.txt`: A plain text file containing a single integer. The system reads and increments this number with every print job. If this file is missing, create it and write `1` inside it.
* `receiptprinter/config.txt`: Printer-specific configuration settings (baud rate, port, etc.).

---

## Configuring the LLM

The LLM generation parameters and prompt structures are fully modular and configured within the Python project at `src/runmodel/config/`:

* `model_config.json`: Defines the model parameters (e.g., temperature, top_k, max output tokens).
* `system.md`: The system instructions provided to the LLM to dictate its persona and standard rules.
* `user.md`: The template for the user prompt. This is populated dynamically by `response_formatter.py` using the data dispatched from the Java application.

---

## Error Handling

Error handling is intentionally centralized. The Python API catches local exceptions (like `PrinterError.py`), but all major application errors, API failures, and execution interruptions are returned to and logged by the Java `Executor`.

The `Logger.java` class handles recording these events to ensure there is a single, unified trail of failures or state issues.

---

## Running the Application

### Python API
To run the Python Flask API in the background on your server, execute the following command (ensure your path to the virtual environment is correct relative to your working directory):

    nohup ../.venv/bin/python3 -u main.py > nohup.out 2>&1 &

---

## Printer Setup & Hardware

**Printer Used:**
* [TODO: Insert exact make/model of the thermal printer here]

**Hardware Setup:**
* [TODO: Connection method (USB, Serial, Network/IP)]
* [TODO: OS-level driver installations or dependencies required]