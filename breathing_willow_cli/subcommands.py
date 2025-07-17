from __future__ import annotations

import argparse
import subprocess
from warnings import warn
from pathlib import Path

from w_cli import diff
from breathing_willow.willow_viz import WillowGrowth
from .utils import (
    append_shaping_log,
    get_version,
    log_prompt,
    mark_vc_step,
    save_snapshot,
)


def cmd_ccraft(args: argparse.Namespace) -> None:
    from breathing_willow import context_slicer_openoption as cs

    fp = args.file
    print(f"for input file '{fp}'")
    cs.endpoint(fp)
    print("done. 是道")


def cmd_sense(args: argparse.Namespace) -> None:
    if args.diff:
        report = diff.export_diff("/field")
        Path("/field/field-update.md").write_text(report)
    else:
        print("sense")


def cmd_module_prompt133(args: argparse.Namespace) -> None:
    from breathing_willow import module_prompt_setup as mps

    fp_module = args.filepath_module
    fp_output = args.dir_out
    if not fp_module or not fp_output:
        raise SystemExit("must have both -f and -o.")
    mps.generate_codex_prompts(fp_module, fp_output)


def cmd_log_prompt(args: argparse.Namespace) -> None:
    log_prompt(args.title, args.link, args.commit)


def cmd_history(args: argparse.Namespace) -> None:
    from breathing_willow.export_kernel import ChatExportArchiver
    from os.path import join
    from datetime import datetime

    fp = "/l/gds/chatgpt-exports"
    print(f"will write to '{fp}'")
    now = datetime.now()
    fpo = Path(join(fp, now.strftime('%Y-%m-%d')))
    fpi = Path(args.file)
    archiver = ChatExportArchiver(fpi, fpo)
    archiver.run()
    print(f"\nwould have written to '{fp}'")


def cmd_vc_step(args: argparse.Namespace) -> None:
    mark_vc_step(args.note)


def cmd_docs(args: argparse.Namespace) -> None:
    host = args.host
    port = args.port
    url = f"http://{host}:{port}"
    print(f"Serving docs at {url} (live reload). Press Ctrl+C to stop.")
    cmd = ["mkdocs", "serve", "--dev-addr", f"{host}:{port}"]
    subprocess.run(cmd, check=True)


def cmd_update_net(args: argparse.Namespace) -> None:
    src = Path(args.file)
    snap_dir = Path(args.snapshot_dir) if args.snapshot_dir else None
    save_snapshot(src, snap_dir)
    wg = WillowGrowth(graph_path=args.graph)
    wg.submit_document(args.file)
    wg.visualize(args.visual_archive)
    clusters = wg.cluster_terms()
    append_shaping_log(src, clusters)


def cmd_snip_file(args: argparse.Namespace) -> None:
    from breathing_willow import snip_file as sf
    import tiktoken

    fp = Path(args.input_file)
    if not fp.exists():
        raise SystemExit(f"file not found: {fp}")

    enc = tiktoken.encoding_for_model("gpt-4")
    before_text = fp.read_text(encoding="utf-8")
    before_tokens = len(enc.encode(before_text))
    print(f"file '{fp}' has {before_tokens} tokens before snipping.")

    print("snipping file to last practical context...")
    text = sf.snip_file_to_last_tokens(
        str(fp), context_scope="practical", aggressive=False
    )
    fpo = Path(args.output_file)
    fpo.parent.mkdir(parents=True, exist_ok=True)
    with open(fpo, "w") as f:
        f.write(text)

    after_text = fpo.read_text(encoding="utf-8")
    after_tokens = len(enc.encode(after_text))
    print(f"file '{fpo}' now has {after_tokens} tokens after snipping.")
    print(f"wrote '{fpo}'")


def cmd_promptdev_bootstrap(args: argparse.Namespace) -> None:
    from breathing_willow.watchful_fog_dev_kernel import infer_structure
    from breathing_willow.watchful_fog_dev_kernel import generate_surfacing
    from breathing_willow.watchful_fog_dev_kernel import render_compare_prompt
    from breathing_willow.watchful_fog_dev_kernel import alert_if_prompt_too_large
    from breathing_willow.helpers import strip_markdown_formatting
    from breathing_willow.count_tokens import get_token_count_model
    import random
    from uuid import uuid4 as uuid
    from codenamize import codenamize
    from os.path import join

    if args.step01_objvals_draft0:
        fp = args.input_file
        strip_markdown_formatting(fp)
        text = Path(fp).read_text()
        prompt_text = infer_structure(text)
        fpo = args.output_file
        Path(fpo).write_text(prompt_text)

        print('\n'+'#'*80+'\n')
        print(f"wrote '{fpo}', use that to get values, objective, prompt\n")
        print("now update these files:")
        for fn in ("values", "objective", "prompt", "excess"):
            path = join("/field", f"{fn}.md")
            print(f"* {path}")
        print('\n'+'#'*80+'\n')
        alert_if_prompt_too_large(prompt_text, fpo)

        return

    if args.step2_make_surfacing:
        fp_values = args.values_file
        fp_objective = args.objective_file
        fp_prompt = args.input_file
        fp_excess = args.excess_file
        if fp_excess:
            text_excess = Path(fp_excess).read_text()
        else:
            text_excess = "<none>"
        i = str(uuid())
        x = i.split("-")[0]
        fp_output = join("/field", f"surfacing {codenamize(i)} {x}.md")
        text = generate_surfacing(
            fp_values=fp_values,
            text_excess=text_excess,
            fp_objective=fp_objective,
            fp_prompt=fp_prompt,
            i=i,
        )
        Path(fp_output).write_text(text)
        print(f"wrote '{fp_output}'")
        print("now you have a surfacing. if you have at least a few, onto next!")
        return

    if args.step3_package_compare:
        substrings = [k for k in args.keys.split("|") if k]
        if len(substrings) < 2:
            raise SystemExit("need at least two keys")
        root = Path("/field")
        matches: dict[str, Path] = {}
        for s in substrings:
            files = [p for p in root.glob("*.md") if s in p.name]
            if len(files) != 1:
                raise SystemExit(f"uuid substring '{s}' matched {len(files)} files")
            matches[s] = files[0]

        selected_keys = random.sample(list(matches.keys()), 2)
        compare_dir = root / "compare"
        compare_dir.mkdir(exist_ok=True)

        for key in selected_keys:
            fp_surfacing = matches[key]
            text_prompt = fp_surfacing.read_text()
            out_text = render_compare_prompt(
                text_prompt,
                fp_values=args.values_file,
                fp_objective=args.objective_file,
            )
            fp_out = compare_dir / f"{key} version.md"
            fp_out.write_text(out_text)
            print(f"wrote '{fp_out}'")


def add_subcommands(subparsers: argparse._SubParsersAction) -> None:
    """Register all breathing-willow subcommands."""

    ccraft = subparsers.add_parser(
        "ccraft", help="tools for context crafting ccraft."
    )
    ccraft.add_argument("-f", "--file", required=True, help="input file")
    ccraft.set_defaults(func=cmd_ccraft)

    sense = subparsers.add_parser("sense", help="sense for pulse (diff)")
    sense.add_argument(
        "--diff", action="store_true", help="export conceptual diff report"
    )
    sense.set_defaults(func=cmd_sense)

    dev_man = subparsers.add_parser(
        "module-prompt133", help="setup prompt stubs for 133 process"
    )
    dev_man.add_argument(
        "--filepath-module",
        "-f",
        required=False,
        help="path to module to be developed.",
        default="",
    )
    dev_man.add_argument(
        "--dir-out",
        "-o",
        required=False,
        help="output dir path. will write codex/prompts.",
        default="",
    )
    dev_man.set_defaults(func=cmd_module_prompt133)

    log_p = subparsers.add_parser("log-prompt", help="log a codex prompt")
    log_p.add_argument("--title", required=True, help="prompt title")
    log_p.add_argument("--link", required=True, help="codex task link")
    log_p.add_argument("--commit", help="commit or PR link")
    log_p.set_defaults(func=cmd_log_prompt)

    hist = subparsers.add_parser("history", help="parse chatgpt history")
    hist.add_argument("-f", "--file", required=True, help="input file")
    hist.set_defaults(func=cmd_history)

    step = subparsers.add_parser(
        "vc-step", help="record a quick vc loop step"
    )
    step.add_argument("note", help="short note for the step")
    step.set_defaults(func=cmd_vc_step)

    docs = subparsers.add_parser(
        "docs", help="build and preview the documentation"
    )
    docs.add_argument(
        "--host", default="127.0.0.1", help="host to bind (default: 127.0.0.1)"
    )
    docs.add_argument(
        "--port", default="8000", help="port to serve on (default: 8000)"
    )
    docs.set_defaults(func=cmd_docs)

    update = subparsers.add_parser(
        "update-net", help="add a document to the graph and render"
    )
    update.add_argument(
        "-f", "--file", required=True, help="path to the document to ingest"
    )
    update.add_argument(
        "--visual-archive", required=True, help="path to write the visualization HTML"
    )
    update.add_argument(
        "--graph",
        default="willow_growth_v5.json",
        help="path to persistent graph (default: willow_growth_v5.json)",
    )
    update.add_argument(
        "--snapshot-dir",
        help="directory to save version snapshots (default: alongside file)",
    )
    update.set_defaults(func=cmd_update_net)

    snip = subparsers.add_parser(
        "snip-file", help="truncate file to last practical tokens"
    )
    snip.add_argument(
        "-n",
        "--n-tokens",
        default="0",
        required=False,
        help="arbitrary n of last tokens.",
    )
    snip.add_argument(
        "-f",
        "--input-file",
        default="/field/prompt.md",
        required=False,
        help="path to text file to be snipped.",
    )
    snip.add_argument(
        "-o",
        "--output-file",
        default="/field/prompt-snipped.md",
        required=False,
        help="output file",
    )
    snip.set_defaults(func=cmd_snip_file)

    shape = subparsers.add_parser(
        "promptdev-bootstrap", help="shape from a seed prompt."
    )
    shape.add_argument(
        "-s0",
        "--step01-objvals-draft0",
        action="store_true",
        help="steps 0-1: infer objective-values, write init.",
    )
    shape.add_argument(
        "-s2",
        "--step2-make-surfacing",
        action="store_true",
        help="step 2: given a prompt and values, make surfacing.",
    )
    shape.add_argument(
        "-s3",
        "--step3-package-compare",
        action="store_true",
        help="step 3: package surfacings for comparison.",
    )
    shape.add_argument(
        "--keys", default="", help="pipe-separated uuid substrings for surfacing prompts"
    )
    shape.add_argument(
        "-o",
        "--output-file",
        default="/field/prompt-output.md",
        help="output file. default: /field/prompt-output.md",
    )
    shape.add_argument(
        "-v",
        "--values-file",
        default="/field/values.md",
        help="values that will be used for shaping. default: /field/values.md",
    )
    shape.add_argument(
        "-j",
        "--objective-file",
        default="/field/objective.md",
        help="fuzzy objective statement. default: /field/objective.md",
    )
    shape.add_argument(
        "-f",
        "--input-file",
        default="/field/prompt.md",
        help="input file. default:/field/prompt.md",
    )
    shape.add_argument(
        "-e",
        "--excess-file",
        default="",
        help="any desired shaping context for step2. often /field/excess.md",
    )
    shape.set_defaults(func=cmd_promptdev_bootstrap)

