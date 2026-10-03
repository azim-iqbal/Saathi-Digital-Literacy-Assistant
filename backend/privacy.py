"""Display-only exemptions shared conceptually with Android SensitiveContent.
Never use these exemptions to approve editable values for upload.
"""
import re

CUES = re.compile(r"(?i)(?<![^\W_])(pin|otp|password|cvv|mpin|passcode|dob|date[ _-]*of[ _-]*birth|birth[ _-]*date|aadhaar|aadhar|passport|account[ _-]*number|card[ _-]*number|security[ _-]*code|recovery[ _-]*code)(?![^\W_])|पासवर्ड|पिन|ओटीपी|ओ[.]टी[.]पी|गुप्त|सुरक्षा[ ]*कोड|सीवीवी")
DIGITS = re.compile(r"(?<![^\W_])\d{4,}(?![^\W_])")
DATE = re.compile(r"(?<![^\W_])(?:[0-3]?[0-9][/-][01]?[0-9][/-](?:19|20)[0-9]{2}|(?:19|20)[0-9]{2}[/-][01]?[0-9][/-][0-3]?[0-9])(?![^\W_])")
PRICE = re.compile(r"(?i)(?:₹|\$|€|£|\bINR|\bRs\.?)\s*(?:[0-9]{1,3}(?:,[0-9]{2,3}){1,2}|[0-9]{1,7})(?:\.[0-9]{1,2})?(?![0-9,.])")


def sensitive(value):
    value = re.sub(r"([a-z])([A-Z])", r"\1 \2", value)
    if CUES.search(value):
        return True
    if "://" not in value and "?" not in value:
        value = PRICE.sub(" ", DATE.sub(" ", value))
    return bool(DIGITS.search(value))
