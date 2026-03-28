from flask import Flask, request, jsonify
from src.receiptprinter.receipt_print import ReceiptPrinter, DemoReceiptPrinter
from src.receiptprinter.util import get_receipt_number, set_receipt_number
from src.runmodel.response_formatter import format_response
from src.runmodel.run_model import *


# global objects
app = Flask(__name__)
rp: ReceiptPrinter


@app.route("/", methods=["GET"])
def status():
    """
    Get the status of the model and printer.

    Returns:
        "status": "OK" | "ERROR: <err>
        if status is OK, Printer: "Demo" | "Active"

    """
    if err := model_status().get("error"):
        return {"status": "ERROR: " + err}
    return jsonify({"status": "OK", "Printer": "Demo" if isinstance(rp, DemoReceiptPrinter) else "Active"})


@app.route("/print-llm", methods=["POST"])
def print_receipt_llm():
    """
    Run the core functionality of the app. Feed API data into the configured LLM,
    then print the response on the receipt printer.

    Request:
        - api_data: JSON API data {"title": <body: JSON | str>, ...}

    Returns:
        - "success": receipt_num
        - "error": "err..."
    """
    # minor errors, dont stop execution
    errors = []

    # try to get api data from the request
    try:
        api_data: dict = request.get_json()
    except Exception as e:
        print(e)
        return jsonify({"error": "Could nor part request JSON.\n" + str(e)})

    # try to run the model
    llm_resp: dict
    try:
        llm_resp = get_receipt_text(api_data)
        if err := llm_resp.get("error"):
            print(err)
            return jsonify({"error": "run_model -> get_receipt_text() failed internally.\n" + err})
    except Exception as e:
        print(e)
        return jsonify({"error": "run_model -> get_receipt_text() failed.\n" + str(e)})

    # try to get receipt number (don't stop if this fails)
    try:
        recept_num = get_receipt_number() + 1
    except FileNotFoundError as e:
        errors.append(e)
        recept_num = -1

    # format the response for printing
    try:
        formatted = format_response(llm_resp, recept_num, rp.printer_line_size)
    except Exception as e:
        print(e)
        return jsonify({"error": "run_model -> format_response() failed.\n" + str(e)})

    # log the new receipt number (if getter didnt fail)
    if recept_num != -1:
        try:
            set_receipt_number(recept_num)
        except FileNotFoundError as e:
            errors.append(e)

    # try to print the receipt
    try:
        rp.print_formatted_string(formatted)
    except Exception as e:
        print(e)
        return jsonify({"error": "ReceiptPrinter -> print_formatted_string() failed.\n" + str(e)})

    # let client know demo printer is being used
    if isinstance(rp, DemoReceiptPrinter):
        errors.append("Demo Printer Used.")

    # return JSON
    return_json = {"success": recept_num, "errors": errors} if errors else {"success": recept_num}
    return jsonify(return_json)

@app.route("/feed-llm", methods=["POST"])
def get_llm_output():
    """
    Feed the configured LLM model with the API data, and return the response.
    (Does not print the receipt!)

    Request:
        - api_data: JSON API data {"title": <body: JSON | str>, ...}

    Returns:
        - "response": JSON response from the model.
        - "error": "err..."

    """
    # try to get api data from the request
    try:
        api_data: dict = request.get_json()
    except Exception as e:
        print(e)
        return jsonify({"error": "Could nor part request JSON.\n" + str(e)})

    # try to run the model
    llm_resp: dict
    try:
        llm_resp = get_receipt_text(api_data)
    except Exception as e:
        print(e)
        return jsonify({"error": "run_model —> get_receipt_text() failed.\n" + str(e)})

    # model successfully ran, return the response
    return jsonify({"response": llm_resp})


def run(port: int=8000):
    """
    Run the app on localhost.

    Args:
        port: Desired port to run the app on (default: 8000).
    """
    global app, rp

    # control when RP is setup (due to working directory differences/issues)
    try:
        rp = ReceiptPrinter()
    except Exception as e:
        print(e)
        rp = DemoReceiptPrinter()

    app.run(host="0.0.0.0", port=port)
