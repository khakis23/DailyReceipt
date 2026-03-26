
class PrinterError(Exception):

    def __init__(self, mes):
        super().__init__("\n\nPrinter is not seen by OS! Don't forget to plug in and power on printer.\nError: " + mes)
