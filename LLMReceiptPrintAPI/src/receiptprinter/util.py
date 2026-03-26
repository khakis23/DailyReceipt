from src.path.paths import all_paths
import usb.core, usb.util


def setup_endpoint(v: int, d: int) -> usb.core.Endpoint:
    """
    Attempts to find and set up the endpoint for the specified vendor and product ID.

    Args:
        v:  vendor ID
        d:  product ID

    Returns:  USB endpoint object

    Raises:  Value Error if device or endpoint not found
    """
    print(type(v), type(d))

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
    """
    Retrieves the vendor or product ID from the config file.

    Args:
        to_get:  name from config.txt (currently: "VENDOR" or "PRODUCT")

    Returns: 16-bit integer ID

    Raises:
        - FileNotFoundError if config file not found
        - ValueError if ID not found in config file
    """
    config_path = all_paths.get("CONFIG_FILE", "src/receiptprinter/config.txt")

    try:
        # open the config file and read each line
        with open(config_path, 'r') as f:
            for line in f:

                # return the ID if the line starts with the specified name
                if line.startswith(to_get):
                    return int(line.split('=')[1], 16)

            else:
                raise ValueError(f'Could not find {to_get} in config file')

    except FileNotFoundError:
        raise ValueError(f'Config file {config_path} not found')


def get_receipt_number() -> int:
    """
    Get stored receipt number.

    Returns:  receipt number

    Raises:  FileNotFoundError if receipt number file not found
    """
    receipt_number_file = all_paths.get("RECEIPT_NUMBER_FILE", "src/receiptprinter/receipt_number.txt")

    with open(receipt_number_file, 'r') as f:
        return int(f.read())

def set_receipt_number(num: int):
    """
    Store the new receipt number.

    Args:
        num:  new receipt number

    Raises:  FileNotFoundError if receipt number file not found
    """
    receipt_number_file = all_paths.get("RECEIPT_NUMBER_FILE", "src/receiptprinter/receipt_number.txt")

    with open(receipt_number_file, 'w') as f:
        f.write(str(num))
