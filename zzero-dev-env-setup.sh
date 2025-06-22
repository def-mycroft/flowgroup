#!/bin/bash

# ==============================================================================
# Script: dev_link_and_mark.sh
#
# Purpose:
#     - Apply a Vim global mark 'C' to cli.py in current project.
#     - Manage /l/m symlink to current project directory, unless already /l/m.
#     - Provide convenient aliases for CLI tool.
#
# Usage:
#     sh dev_link_and_mark.sh
#
# Description:
#     This script automates a small but useful workflow for managing multiple
#     parallel development projects. It allows you to quickly set a global Vim
#     mark for the main CLI file (cli.py), manage a symlink to quickly switch
#     working contexts, and define command aliases for ease of use.
#
# Current Features:
#     - Vim mark 'C' is applied to cli.py to allow fast jump via `'C'` or `:marks`.
#     - Symlink /l/m is removed and relinked to current directory, unless you are
#       already in /l/m, avoiding symlink loops.
#     - Aliases `zcc` and `cli` are defined to run the CLI tool from anywhere.
#
# Future Plans:
#     - Add automatic activation of the correct conda env.
#     - Detect if cli.py exists, warn if not.
#     - Auto-open tmux session with context name.
#     - Print a pretty status summary after running.
#     - Track current project in ~/.dev_link_history for quick switching.
#
# Notes:
#     You can edit this doc block directly — this will always print when run.
#
# ==============================================================================
# Quick Reference ==============================================================
# Vim Marks: C->"cli.py" | 
# ==============================================================================

# Print doc when running
cat "$0" | sed -n '/^# ====/,/^# ====/{ s/^# \?//; p }'


# --- Manage /l/m symlink ---
if [ "$(realpath .)" != "/l/m" ]; then
    if [ -L /l/m ]; then
        rm /l/m
    fi
    ln -s "$(pwd)" /l/m
fi

# --- Apply Vim global mark 'C' to cli.py ---
#vim -c "delmarks C" -c "mark C" -c "wq" cli.py
#vim -c "delmarks A" -c "mark A" -c "wq" "/l/m/docs/00projectanchor-liftsim-burly-concert-gigantic-analysis-e7cf1ccf.md"
#vim -c "delmarks D" -c "mark D" -c "wq" "/l/m/docs/CONTENTS.md"

# --- Set aliases ---
#alias zls=j
#alias cli=

