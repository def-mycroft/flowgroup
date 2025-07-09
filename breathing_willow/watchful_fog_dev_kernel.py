
from breathing_willow.helpers import load_asset 
from codenamize import codenamize 
from uuid import uuid4 as uuid

from jinja2 import Template


def infer_structure(text):
    x = Template(load_asset('prompt-bootloop-s01', ext='jinja2'))
    return x.render({'content':text})


def generate_surfacing(fp_values='/field/values.md', i='', 
                       fp_prompt='/field/prompt.md', text_excess='',
                       fp_objective='/field/objective.md'):
    x = Template(load_asset('prompt-surfacings-s2', ext='jinja2'))
    with open(fp_values, 'r') as f:
        text_values = f.read()
    with open(fp_objective, 'r') as f:
        text_objective = f.read()
    with open(fp_prompt, 'r') as f:
        text_prompt = f.read()
    if not i:
        i = str(uuid())
    cname = codenamize(i)
    return x.render({
        'content_optional_additional':text_excess, 
        'content_values':text_values,
        'content_objective':text_objective, 
        'content_prompt':text_prompt,
        'cname':cname, 'uuid':i, 
    })



def render_compare_prompt(text_prompt: str, fp_values: str = '/field/values.md',
                           fp_objective: str = '/field/objective.md') -> str:
    """Render a Stage 3 comparison prompt container.

    Parameters
    ----------
    text_prompt : str
        The surfacing prompt text to package.
    fp_values : str
        Path to the values file.
    fp_objective : str
        Path to the fuzzy objective file.

    Returns
    -------
    str
        Rendered markdown text for comparison.
    """
    tmpl = Template(load_asset('prompt-compare-s3', ext='jinja2'))
    with open(fp_values, 'r') as f:
        values = f.read()
    with open(fp_objective, 'r') as f:
        objective = f.read()
    return tmpl.render({
        'text_prompt': text_prompt,
        'value_statements': values,
        'fuzzy_objective': objective,
    })
