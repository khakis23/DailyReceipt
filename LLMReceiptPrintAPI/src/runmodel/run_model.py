from ollama import chat
import json
from src.path.paths import all_paths


def get_receipt_text(api_data: dict, retry_attempts=5, keep_alive="30h") -> dict[str, str]:
    """
    Feed cleaned API data to the configured LLM, which should return a configured JSON response.

    Configure the response and structure of the JSON response and API data in the config directory.
    API data must match the model's schema, and cleaned API data is preferred over raw API data.

    Args:
        api_data:       (required) cleaned API data to be fed to the model.
        retry_attempts: (optional) number of times to retry the model's response before failing.
        keep_alive:     (optional) how long the model should keep loaded in memory.

    Returns: JSON response from the model.

    Raises:
        - FileNotFoundError if any config files are missing or damaged.
        - KeyError if the model's schema is missing required keys.
        - Exception if the model fails to produce a valid response after retrying.

    """
    # get necessary config values
    try:
        system, user, schema, model_options, model = _get_config(api_data)
    except Exception as e:
        print(e)
        return {"error": str(e)}

    # messages fed to the model
    messages: list = [
        {"role": "system", "content": system},
        {"role": "user", "content": user}
    ]

    # attempt loop
    while retry_attempts > 0:
        retry_attempts -= 1

        # feed messages to the model
        resp = chat(
            model=model,
            messages=messages,
            format=schema,
            options=model_options,
            keep_alive=keep_alive
        )

        # check if the response is valid JSON
        try:
            res = json.loads(resp.message.content)

            # if its valid return
            if _check_json_headers(res, messages, schema.get("required")):
                return res
            else:
                # try again
                print("JSON headers missing. retrying...")
                continue

        # model did not provide a valid JSON response, try again
        except json.JSONDecodeError as je:
            print("Malformed JSON: retrying...")

            # penalize model for invalid JSON response
            messages.append({
                "role": "user",
                "content": (
                    "The previous response was INVALID JSON.\n"
                    "You MUST return ONLY valid JSON matching the schema.\n"
                    "Do NOT include explanations, apologies, or commentary.\n"
                    "Escape all quotes inside strings.\n"
                    "If unsure, output the simplest valid JSON that satisfies the schema."
                )
            })

    raise Exception(f"{model} failed to produced a valid response and exceeded retry limit of {retry_attempts}.")


def model_status():
    """
    Check if the model is active and ready to use.

    Returns:
        - {"sucess": 0} if the model is active.
        - {"error": <err>} otherwise.

    """
    try:
        # get model name
        _, _, _, _, model = _get_config({})
        # send a simple message to the model
        resp = chat(
            model=model,
            messages=[{"role": "system", "content": "status"}],
        )
        # ensure the output actually exists
        if type(resp.message.content) == str: pass
    except Exception as e:
        return {"error": str(e)}
    return {"success": 0}


def _get_config(api_data: dict) -> tuple[str, str, dict, dict, str]:
    """
    Get the system, user, schema, and model options from the config files.

    Args:
        api_data: raw API data from the request

    Returns: system message, user message, schema, model options, model name

    """
    system: str
    user: str
    schema: dict
    model_options: dict
    model: str

    system_path = all_paths.get("SYSTEM", "src/runmodel/config/system.md")
    user_path = all_paths.get("USER", "src/runmodel/config/user.md")
    schema_path = all_paths.get("SCHEMA", "src/runmodel/config/model_config.json")

    # read config files
    try:
        system = system_path.read_text(encoding="utf-8")
        user = user_path.read_text(encoding="utf-8").format(
            api_data=json.dumps(api_data, ensure_ascii=True)
        )
        model_config = json.loads(schema_path.read_text(encoding="utf-8"))
    except FileNotFoundError as e:
        print(f"Error reading config files: {e}")
        raise Exception(f"Error reading config files: {e}")

    # extract config values
    try:
        schema = model_config["schema"]
        model_options = model_config["model_options"]
        model = model_config["model"]
    except KeyError:
        print("Malformed model config, missing schema or model_options key")
        raise Exception("Malformed model config, missing schema or model_options key")

    return system, user, schema, model_options, model


def _check_json_headers(resp_dict: dict, mes_ref: list, required: list) -> bool:
    """
    Check if the required headers are present in the response.

    Args:
        resp_dict:  extracted dictionary response from the model.
        mes_ref:    reference to the system messages list — a penalty will
                    be added to this list if a header is missing.
        required:   list of required headers from the schema (ensure the model has access to this same list).

    Returns: False if headers missing, True otherwise.

    """
    if not required:
        print("No required headers found in schema.")
        return True

    # check to make sure all keys are present
    for key in required:
        if not resp_dict.get(key):   # key does not exist
            # add a penalty to the messages reference
            mes_ref.append({"role": "user",
                             "content":
                                 f"'{key}' key missing from response.\n"
                                 f"Refer to system message or schema for clear directions"})
            print(f"Missing key: {key}. retrying...")
            return False   # key is missing
    # no headers missing
    return True


##### TESTING #####
if __name__ == "__main__":
    # if err := model_status().get("error"):
    #     print(err)
    # else:
    #     print("Model is online and ready to use.")
    #
    # input("Press Enter to continue...")

    data = {
    "today's_date": {
        "iso": "2026-03-08",
        "weekday": "Sunday",
        "timezone": "America/Los_Angeles",
        "human": "Sunday, March 8, 2026"
    },
    "weather": {
        "location": "San Francisco, CA",
        "summary": "Light rain",
        "temp_f": 55,
        "feels_like_f": 52,
        "high_f": 60,
        "low_f": 48,
        "precip_chance_pct": 60,
        "wind_mph": 12,
        "alerts": [
            {
                "type": "small_craft_advisory",
                "details": "High seas expected. Use caution if operating marine vessels."
            }
        ]
    },
    "calendar_events": [
        {
            "title": "Dental Appointment",
            "start_local": "2026-03-10",
            "end_local": "2026-03-10",
            "notes": "Routine cleaning and checkup. Bring insurance card."
        },
        {
            "title": "Project Deadline",
            "start_local": "2026-03-15",
            "end_local": "2026-03-15",
            "notes": "Final revisions due for the Alpha Project."
        }
    ],
    "on_this_day": {
        "date": "March 7",
        "items": [
            {
                "year": 1876,
                "type": "invention",
                "text": "Alexander Graham Bell patents the telephone.",
                "source": "History.com"
            },
            {
                "year": 1936,
                "type": "historical",
                "text": "German troops re-occupy the Rhineland, violating the Treaty of Versailles.",
                "source": "Britannica (On This Day: March 7)"
            }
        ]
    },
    "national_days_top3": [
        {
            "name": "International Women's Day",
            "type": "international",
            "tagline": "A global day celebrating the social, economic, cultural, and political achievements of women.",
            "source": "UN Women"
        },
        {
            "name": "National Peanut Butter Day",
            "type": "food",
            "tagline": "A day to indulge in the creamy, nutty goodness of peanut butter.",
            "source": "National Day Calendar"
        },
        {
            "name": "National Proofreading Day",
            "type": "observance",
            "tagline": "A reminder to double-check your work before submitting it.",
            "source": "National Day Calendar"
        }
    ],
    "ascii art inspiration":
        "bird"
}

    txt = get_receipt_text(data)

    print(txt["title"])
    print(txt["body"])
    print(txt["art_title"])
    for line in txt["ascii_art"]:
        print(line)

