from flask import Flask, request, jsonify
from receiptprinter.receipt_print import ReceiptPrinter
from receiptprinter.util import get_receipt_number, set_receipt_number
from runmodel.response_formatter import format_response
from runmodel.run_model import *
from web.PrinterError import PrinterError

app = Flask(__name__)
try:
    rp = ReceiptPrinter()
except Exception as e:
    raise PrinterError(str(e))


@app.route("/", methods=["GET"])
def status():
    if err := model_status().get("error"):
        return {"status": "ERROR: " + err}
    return jsonify({"status": "OK"})


@app.route("/print-llm", methods=["POST"])
def print_receipt_llm():
    """

    Request:
        - api_data: JSON API data {"title": <body: JSON | str>, ...}

    Returns:
        - "success": receipt_num
        -"error": "err..."

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
        if err := llm_resp.get("error"):
            print(err)
            return jsonify({"error": "run_model —> get_receipt_text() failed internally.\n" + err})
    except Exception as e:
        print(e)
        return jsonify({"error": "run_model —> get_receipt_text() failed.\n" + str(e)})

    # format llm response for printer, and keep track of receipt number
    recept_num = get_receipt_number() + 1   # TODO handle errors in here!
    formatted = format_response(llm_resp, recept_num, rp.printer_line_size)
    set_receipt_number(recept_num)

    # try to print the receipt
    try:
        rp.print_formatted_string(formatted)
    except Exception as e:
        print(e)
        return jsonify({"error": "ReceiptPrinter -> print_formatted_string() failed.\n" + str(e)})

    return jsonify({"success": recept_num})

@app.route("/feed-llm", methods=["POST"])
def get_llm_output():
    """

    Request:

    Returns:

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
    app.run(host="0.0.0.0", port=port)

