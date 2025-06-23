import argparse
from breathing_willow.willow_viz import WillowGrowth


def build_parser():
    parser = argparse.ArgumentParser(description="WillowGrowth CLI")
    parser.add_argument(
        "--graph",
        default="willow_growth_v5.json",
        help="path to persistent graph (default: willow_growth_v5.json)",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    add_p = sub.add_parser("add", help="add a document")
    add_p.add_argument("path", help="path to document")

    vis_p = sub.add_parser("visualize", help="render the graph")

    shape_p = sub.add_parser("shape", help="shape a node")
    shape_p.add_argument("uid", help="node uid")
    shape_p.add_argument("--sentence", default="", help="short sentence")
    shape_p.add_argument("--paragraph", default="", help="paragraph")

    return parser


def main(argv=None):
    parser = build_parser()
    args = parser.parse_args(argv)

    wg = WillowGrowth(graph_path=args.graph)

    if args.command == "add":
        wg.submit_document(args.path)
    elif args.command == "visualize":
        wg.visualize('/l/tmp/willow-net.html')
    elif args.command == "shape":
        wg.shape_node(args.uid, sentence=args.sentence, paragraph=args.paragraph)


if __name__ == "__main__":
    main()
