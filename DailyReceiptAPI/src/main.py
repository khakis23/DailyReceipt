from web import app
import sys


PORT = 8001


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        PORT = int(sys.argv[1])
    app.run(PORT)
