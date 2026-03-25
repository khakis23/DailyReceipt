from receiptprinter.util import get_v_or_d, setup_endpoint


class ReceiptPrinter:

    printer_line_size = 42

    def __init__(self, vendor=None, product=None):
        # use config file if not specified
        if vendor is None:
            vendor = get_v_or_d('VENDOR')
        if product is None:
            product = get_v_or_d('PRODUCT')

        print(f"Using vendor: {vendor}, product: {product}")

        # printing endpoint
        self.ep = setup_endpoint(vendor, product)

    def print_from_bytes(self, data: bytes) -> Exception | None:
        # build payload
        payload = b'\x1b\x40\n\n'  # init
        payload += data
        payload += b'\n\n\n\n\n\n\n\x1d\x56\x00'   # cut paper

        # attempt to write to printer
        try:
            self.ep.write(payload)
        except Exception as e:
            return e
        return None

    def print_formatted_string(self, data: str, h_space=2, t_space=2) -> Exception | None:
        final_str = h_space * '\n'
        for line in data.split('\n'):
            final_str += line + '\n'
        final_str += t_space * '\n'
        return self.print_from_bytes(final_str.encode())

    def print_inline_string(self, data: str, h_space=2, t_space=2) -> Exception | None:
        final_str = ""
        count = 0
        current_word = ""

        # add heading vertical space
        final_str += '\n' * h_space

        for char in data:
            # handle new line character normally
            if char == '\n':
                final_str += char
                count = 0

            # got to the end line, start new one
            elif count == self.printer_line_size:
                # case where word is longer than printer line size
                if len(current_word) >= self.printer_line_size:
                    final_str += current_word[:self.printer_line_size]
                    current_word = current_word[self.printer_line_size:]
                # normal case where the word starts on new line
                else:
                    current_word += char

                final_str += '\n'
                count = len(current_word)   # reset count

            # handle space character — start new word
            elif char == ' ':
                final_str += current_word + char
                current_word = ""
                count += 1

            # add character to current word
            else:
                current_word += char
                count += 1

        # leftover word and tailing space
        final_str += current_word + '\n' * t_space
        # convert to bytes and print
        return self.print_from_bytes(final_str.encode())



# TODO TESTING
if __name__ == "__main__":
    data = r"Hello this is your    friendly abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-=+[]{}\|/.,<>`~ printer, Tom, speaking! I hope you enjoy your day! Love your boy, Tom. Receipt printers often have a physical distance between the print head and the manual tear bar or auto-cutter. If you send your text and then immediately stop, the last few lines might stay tucked inside the printer where the user can't see them."
    cat = r"""                              
                         .'\            
                        //  ;           
                       /'   |           
      .----..._    _../ |   \           
       \'---._ `.-'      `  .'          
        `.    '              `.         
          :            _,.    '.        
          |     ,_    (() '    |        
          ;   .'(().  '      _/__..-    
          \ _ '       __  _.-'--._      
          ,'.'...____'::-'  \     `'    
         / |   /         .---.          
   .-.  '  '  / ,---.   (     )         
  / /       ,' (     )---`-`-`-.._      
 : '       /  '-`-`-`..........--'\     
 ' :      /  /                     '.   
 :  \    |  .'         o             \  
  \  '  .' /          o       .       ' 
   \  `.|  :      ,    : _o--'.\      | 
    `. /  '       ))    (   )  \>     | 
      ;   |      ((      \ /    \___  | 
      ;   |      _))      `'.-'. ,-'` ' 
      |    `.   ((`            |/    /  
      \     ).  .))            '    .   
    ----`-'-'  `''.::.________:::--'' ---             
    """

    try:
        rp = ReceiptPrinter()
        rp.print_formatted_string(cat, h_space=0, t_space=0)
    except Exception as e:
        print(e)
