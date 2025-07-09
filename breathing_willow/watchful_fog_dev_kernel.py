
from breathing_willow.helpers import load_asset 

from jinja2 import Template


def infer_structure(text):
    x = Template(load_asset('prompt-bootloop-s01', ext='jinja2'))
    return x.render({'content':text})

endpoint = infer_structure
