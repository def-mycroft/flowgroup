
from breathing_willow.helpers import load_asset 
from codenamize import codenamize 
from uuid import uuid4 as uuid
from breathing_willow.count_tokens import get_token_count_model

from jinja2 import Template


def alert_if_prompt_too_large(text: str, fp: str, threshold: int = 3000) -> None:
    model='gpt-4o'
    n_tokens = get_token_count_model(text, model=model)
    if n_tokens > threshold:
        ex = n_tokens - threshold
        p = ex / threshold
        print(f"\n⚠️  Prompt at '{fp}' has {n_tokens:,} tokens (model={model}).")
        print(f"   This exceeds shaping threshold of ~{threshold:,} by {100*p:.1f}%.")
        print("   Consider trimming for clarity, cost, and shaping alignment.\n")


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
