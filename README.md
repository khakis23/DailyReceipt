# DailyReceipt
### Author: Tyler Black
An LLM-powered "assistant" that prints personalized scheduled reports for the user on a physical thermal receipt printer.

## Description
_DailyReceipt_ is a modular and customizable system that integrates a local Large Language Model (LLM) and various configured API's to
generate and print personalized receipts on a set schedule. The project is split into two primary components: a Java-based Executor 
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
* **Fetchers**: General purpose API data getters that can easily be implimented for any basic API.
  * Currently implimented: 
    * Google Calendar
    * [CheckiDay](https://www.checkiday.com) (Today's _obscure_ Holidays)
    * [Local Weather](https://www.visualcrossing.com)
    * Random word/object generator (sudo-fetcher: everything is stored locally)
* **System**: Contains the main application loop, which handles configured scheduling, and centrally manages logging.

### 2. Python Flask API (`LLMReceiptPrintAPI`)
Acts as the processing engine and hardware interface.
* **Web**: A Flask application receives payloads from the Java Executor and graceully returns sucess/errors to the Executor's `Logger`.
  * Runs on `localhost` with a default `PORT=8001` 
    * Port configurable as an excution argument 
* **Model Runner**: Handles all LLM-related interactions and configurations.
* **Receipt Printer**: Interfaces directly with the physical thermal printer using the response from the LLM, includes formatting the response for the printer, and configuring printer.



## Directory Structure

    DailyReceiptLLMPrinter/
    ├── Executor/                           # Java Application
    │   ├── build.gradle.kts
    │   ├── LOG.log                         # Default Location 
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
    │           └── resources/              
    │               ├── config.json         # Ignored
    │               ├── GoogleCalCred.json  # Ignored
    │               ├── RandomWords.csv
    │               └── secrets.json        # Ignored
    │
    └── LLMReceiptPrintAPI/                 # Python Flask API
        ├── requirements.txt
        └── src/
            ├── path/
            │   └── paths.py
            ├── receiptprinter/
            │   ├── config.txt
            │   ├── receipt_number.txt      # Ignored
            │   ├── receipt_print.py
            │   ├── response_formatter.py
            │   └── util.py
            ├── runmodel/
            │   ├── config/                 # LLM Prompts & Configs
            │   │   ├── model_config.json
            │   │   ├── system.md
            │   │   └── user.md
            │   └── run_model.py
            └── web/
                ├── app.py
                └── PrinterError.py

---

## Required Files to Add

Before running the application, you must populate several configuration and secret files that are excluded from version control.

### `receipt_number.txt` 

### `config.json`

### `GoogleCalCred.json`

### `secrets.json`



## Configuration

### Configuring the LLM 

The LLM generation parameters and prompt structures are fully modular and configured within the Python project at `src/runmodel/config/`:

* `model_config.json`: Defines the model parameters (e.g., temperature, top_k, max output tokens).
* `system.md`: The system instructions provided to the LLM to dictate its persona and standard rules.
* `user.md`: The template for the user prompt. This is populated dynamically by `response_formatter.py` using the data dispatched from the Java application.

### Configuring Schedule and API Data

TODO


## Printer Setup & Hardware

All printers that support standard **ESC/POS** should be configuabled for this project. The `ReceiptPrinter` class
assumes serial communication, but other forms of communication could be implimented.

**Tested Printer Model:**
* Epson TM-T88V

**Hardware Setup:**
* Connect compatiable printer via serial USB.
* In `src/receiptprinter/config.txt`, configure the `VENDOR` and `PRODUCT` for specific printer (this can easily be found online).
* Add the printer to the operating system.
  * This is different on every OS! There are many guides online for adding a basic POS printer to Mac/Window/Linux. (MacOS can be quite difficult!)
  

## Logging and Error Handling

Error handling is intentionally centralized. The Python API catches and logs local exceptions, but all major application errors or warnings, 
API failures, and execution interruptions are returned to and logged by the Java `Executor's` `Logger`.

The `Logger.java` class handles recording these events to ensure there is a single, unified trail of failures or state issues. 
By default the log is stored here: `Executor/LOG.log`



## Running the Application

### Ubuntu 24 LTS Build & Run Script
Inlcuded in this repository is a script called `BUILD_RUN.sh`. The linux-specific script is a single-command build and run
script, that builds all part of the application and runs the two processes in the backrgound. The script will also
generate a `stop_receipt_printer.sh`, which kills the entire application. The script is test on Ubuntu 24.04, but should
work on other versions and distributions, and it can be addapted to work on MacOS.

### Other systems
TODO
