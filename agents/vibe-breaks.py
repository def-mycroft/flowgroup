#!/usr/bin/env python3
"""vibe-breaks: Print a few random links for a quick break."""

import random

# Editable list of light break activity links
LINKS = [
    "https://online-go.com",
    "https://lumosity.com",
    "https://sudoku.com",
    "https://jigsawplanet.com",
    "https://www.chess.com/play/computer",
    "https://www.wordlewebsite.com/",
]


def main():
    selected = random.sample(LINKS, k=3)
    for link in selected:
        print(link)


if __name__ == "__main__":
    main()
