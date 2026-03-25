import datetime
import textwrap


def format_response(response: dict, r_num: int, line_len: int=42) -> str:
    """

    Args:
        response:
        r_num:
        line_len:

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
    return datetime.date.today().strftime("%d %b %Y")


def _format_body(body: str, line_len: int=42) -> str:
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
    utf8_to_ascii_map = {
        "\u2018": "'", "\u2019": "'",  # Smart single quotes
        "\u201c": '"', "\u201d": '"',  # Smart double quotes
        "\u2013": "-", "\u2014": "-",  # Dashes
        "\u2026": "...",
    }

    for utf8, ascii in utf8_to_ascii_map.items():
        text = text.replace(utf8, ascii)

    return text
