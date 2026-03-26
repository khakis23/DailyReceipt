from src.web import app
from src.path.paths import setup_wd
import sys


PORT = 8001


if __name__ == "__main__":
    # get optional args
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        PORT = int(sys.argv[1])
    # setup working directory
    setup_wd()

    app.run(PORT)
