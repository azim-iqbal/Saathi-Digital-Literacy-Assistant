"""Optional source-cited, user-performed navigation hints; never tool instructions."""
from dataclasses import dataclass
import re
import unicodedata
from backend.live import safe_text, CONSEQUENTIAL, HIGH_RISK_DESTINATION
from backend.request_policy import private_context, browser_warning, error_status

_PRIVATE_FIELD = re.compile(r'(?i)\b(name|email|phone|mobile|address|birth|account|card|identity|document|message|income|salary)\b|नाम|ईमेल|फोन|पता|जन्म|खाता|naam|pata')

@dataclass(frozen=True)
class NavigationHint:
    kind: str
    label: str

    def valid(self, quote):
        if self.kind not in ('READ_OPTION','FIELD_LABEL') or not safe_text(self.label,80): return False
        if unicodedata.normalize('NFKC',self.label) != self.label or any(unicodedata.category(c) in ('Cf','Cc','Cs') for c in self.label): return False
        if re.search(r'(?i)खरीद|जमा|हटाएं|हटाएँ|मंजूर|मंज़ूर|\b(bhugtan|bhejein|bhejo|kharidein|kharido|jama|hatao|manzoor)\b',self.label): return False
        if self.label != self.label.strip() or any(c in self.label for c in ('"',"'",'“','”','‘','’',':','/','\\')): return False
        if CONSEQUENTIAL.search(self.label) or HIGH_RISK_DESTINATION.search(self.label) or private_context(self.label) or browser_warning(self.label) or error_status(self.label): return False
        if self.kind == 'FIELD_LABEL' and _PRIVATE_FIELD.search(self.label): return False
        quoted=next((left+self.label+right for left,right in [('“','”'),('"','"'),('‘','’'),("'","'")] if left+self.label+right in quote),None)
        if quoted is None: return False
        # Only a source's explicit labelled instruction, never merely a mentioned word.
        action=r'(?:select|choose|open|tap|click)' if self.kind=='READ_OPTION' else r'(?:find field|field)'
        suffix=r'(?:चुनें|खोलें|दबाएं|chunein|chuniye|kholein)' if self.kind=='READ_OPTION' else r'(?:फ़ील्ड|फील्ड|field)'
        return bool(re.search(r'(?i)\b'+action+r'\s+'+re.escape(quoted),quote) or
                    re.search(re.escape(quoted)+r'\s+'+suffix,quote,re.I))
