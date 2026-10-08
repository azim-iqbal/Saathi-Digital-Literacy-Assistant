"""General changing-claim routing; no service-specific facts or prerequisites."""
import re

_PATTERNS = [(kind, re.compile(pattern, re.I)) for kind, pattern in (
    ('outage', r'outage|service down|not working|error|सेवा बंद|काम नहीं कर|त्रुटि|kaam nahi|kaam nahin|samasya'),
    ('eligibility', r'eligib|qualify|qualification|पात्र|योग्यता|patrata|yogya'),
    ('deadline', r'deadline|last date|अंतिम तारीख|आखिरी तारीख|aakhri tareekh|antim tithi'),
    ('pricing', r'pricing|fees?|cost|price|शुल्क|कीमत|shulk|keemat'),
    ('requirements', r'prerequisite|requirement|required document|documents needed|apply for|application process|ज़रूरी दस्तावेज|जरूरी दस्तावेज|पूर्व शर्त|आवेदन|zaroori dastavez|zaruri dastavez|aavedan|apply kar'),
)]

def research_claim(goal):
    return next((kind for kind, pattern in _PATTERNS if pattern.search(goal[:160])), None)

_PRIVATE_LABEL = re.compile(r'(?i)\b(inbox|conversation|recipient|message[ _-]?(body|thread)|email[ _-]?(body|subject)|compose[ _-]?(mail|message)|personal[ _-]?(details|information)|account[ _-]?details|document[ _-]?(body|editor))\b|निजी जानकारी|व्यक्तिगत जानकारी|संदेश का पाठ|ईमेल|niji jaankari|vyaktigat jaankari')
def private_context(value):
    return bool(_PRIVATE_LABEL.search(value))

_ERROR_STATUS = re.compile(r'(?i)^(service unavailable|temporarily unavailable|something went wrong|try again later|network error|सेवा उपलब्ध नहीं है|कुछ गलत हो गया|बाद में कोशिश करें|service uplabdh nahin|baad mein koshish karein)[.!। ]*$')
def error_status(value):
    return bool(_ERROR_STATUS.fullmatch(value.strip()))

_BROWSER_WARNING = re.compile(r'(?i)your connection is not private|connection (?:is )?not secure|(?:net::)?err_(?:cert|ssl)_[a-z_]+|deceptive site ahead|dangerous site|suspected phishing|आपका कनेक्शन निजी नहीं है|कनेक्शन सुरक्षित नहीं है|धोखाधड़ी वाली साइट|connection surakshit nahin hai')
def browser_warning(value):
    return bool(_BROWSER_WARNING.search(value))
