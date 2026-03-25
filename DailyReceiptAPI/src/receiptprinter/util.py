from pathlib import Path
import usb.core, usb.util


CONFIG_FILE = Path("src/receiptprinter/config.txt")   # TODO make this work on all envirements!!!!!!
RECEIPT_NUMBER_FILE = Path("src/receiptprinter/receipt_number.txt")


def setup_endpoint(v, d):
    # find default device
    dev = usb.core.find(idVendor=v, idProduct=d)
    if dev is None:
        raise ValueError(f"Device not found: {v:#06x}, {d:#06x}")

    # default configuration
    dev.set_configuration()
    config = dev.get_active_configuration()
    intf = config[(0, 0)]

    # find the 'out' endpoint
    end_pt = None
    for ep in intf:
        if not (ep.bEndpointAddress & 0x80):
            end_pt = ep
            break

    if end_pt is None:
        raise ValueError('Endpoint not found')

    return end_pt


def get_v_or_d(to_get: str) -> int:
    try:
        with open(CONFIG_FILE, 'r') as f:
            for line in f:
                if line.startswith(to_get):
                    return int(line.split('=')[1], 16)
            else:
                raise ValueError(f'Could not find {to_get} in config file')
    except FileNotFoundError:
        raise ValueError(f'Config file {CONFIG_FILE} not found')


def get_receipt_number() -> int:   # TODO RETURN AN ERROR INSTEAD!
    try:
        with open(RECEIPT_NUMBER_FILE, 'r') as f:
            return int(f.read())
    except FileNotFoundError as e:
        return -1

def set_receipt_number(num: int):
    try:
        with open(RECEIPT_NUMBER_FILE, 'w') as f:
            f.write(str(num))
    except Exception as e:
        print(e)   # TODO LOG