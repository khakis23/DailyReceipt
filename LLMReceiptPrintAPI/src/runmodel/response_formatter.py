import datetime
import textwrap


def format_response(response: dict, r_num: int, line_len: int=42) -> str:
    """
    Format the response dictionary into a single string for printing.

    Formatting Example:
        +------------------------------+
        | Receipt #123     26 Mar 2026 |
        |                              |
        |            TITLE             |
        |                              |
        |  Today's roast profile is    |
        |  looking consistent with     |
        |  notes of dark chocolate     |
        |  and a bright citrus finish  |
        |                              |
        |             ( (              |
        |              ) )             |
        |           .-------.          |
        |           |   U   |          |
        |           '-------'          |
        |            \_____/           |
        |                              |
        |          ASCII_TITLE         |
        +------------------------------+

    Args:
        response:  Unmodified response dictionary from the model.
            - must contain "title", "body", "ascii_art", and "art_title" keys.
        r_num:     Receipt number — will be displayed as: "Receipt #123"
        line_len:  Max line length needed for the receipt printer.

    Returns: inline (single line) string

    """
    ## assign variables for each string category
    # header
    receipt_num = f"Receipt #{r_num}"
    date = _get_today_date()
    title = _utf8_to_ascii(response["title"])
    # clean and format body
    body = _format_body(
        _utf8_to_ascii("    " + response["body"]),
        line_len)
    # art
    ascii_art = '\n'.join(response["ascii_art"])
    ascii_title = _utf8_to_ascii(response["art_title"])

    # assemble and return
    return (f"{receipt_num}{date:>{line_len - len(receipt_num)}}\n\n"
                 f"{title:^{line_len}}\n"
                 f"{body}\n\n"
                 f"{ascii_art}\n"
                 f"{ascii_title:^{line_len}}")


def _get_today_date() -> str:
    """Get current day in the format "DD MMM YYYY"."""
    return datetime.date.today().strftime("%d %b %Y")


def _format_body(body: str, line_len: int=42) -> str:
    """
    Wrap the body text to fit within the specified line length.

    Args:
        body:      inline string (newlines are accepted)
        line_len:  max line length

    Returns: formatted string wirth newline characters inserted at line breaks.

    """
    paragraphs = body.split('\n')

    para_formatted = []
    for para in paragraphs:
        # empty lines
        if para.strip() == "":
            para_formatted.append("")
            continue

        # text wrap
        para_formatted.append(
            textwrap.fill(para, width=line_len)
        )

    return '\n'.join(para_formatted)


def _utf8_to_ascii(text: str) -> str:
    """
    Convert UTF-8 characters to their ASCII equivalents.

    Args:
        text: string containing (potentially) UTF-8 characters

    Returns:
        string with UTF-8 characters replaced by their ASCII equivalents
    """
    utf8_to_ascii_map = {
        "\u2018": "'", "\u2019": "'",
        "\u201c": '"', "\u201d": '"',
        "\u2013": "-", "\u2014": "-",
        "\u2026": "...",
    }

    for utf8, ascii in utf8_to_ascii_map.items():
        text = text.replace(utf8, ascii)

    return text
