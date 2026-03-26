from pathlib import Path


# working directory
WD = Path(__file__).resolve().parent.parent

### All Paths ###
all_paths = {
    # receiptprinter.utils
    "CONFIG_FILE": Path("receiptprinter/config.txt"),
    "RECEIPT_NUMBER_FILE": Path("receiptprinter/receipt_number.txt"),
    "SYSTEM": Path("runmodel/config/system.md"),
    # runmodel.run_model
    "USER": Path("runmodel/config/user.md"),
    "SCHEMA": Path("runmodel/config/model_config.json")
}


def setup_wd():
    """
    Setup working directory, so it is the same on all systems. Update all paths accordingly.

    WD = <project directory> (not src!)
    """
    global WD

    # move working directory up one level
    if WD.name == "src":
        WD = WD.parent

    # update all paths
    src = WD / "src"
    for file, path in all_paths.items():
        all_paths[file] = Path(src / path)

    print("Using working directory: ", WD, "\n")
