# Checkpointing in Tide

A **checkpoint** is Tide's way of saving your place in an experiment. It captures the current conversation, code snippets, and shaping target so you can jump back to that moment later.

## When and Why to Use a Checkpoint

Checkpointing is handy whenever you reach a milestone or want to preserve your progress before trying something risky. By saving a checkpoint, you ensure you can restore the same context if your next steps don't work out.

## How to Trigger a Checkpoint

You can trigger checkpoints directly from the command line. Here are two quick examples:

```bash
# save a checkpoint with a custom label
$ tide checkpoint save "initial-shape"

# list checkpoints and restore one
$ tide checkpoint list
$ tide checkpoint load "initial-shape"
```

## Why Checkpointing Matters

Flows evolve quickly, and it's easy to lose track of how you got to a certain state. Checkpoints keep your experiments stable, giving you a reliable way to rewind or branch out without starting over.

## Good Moments to Checkpoint

- right after you shape a new goal
- before testing a major code change
- when a conversation delivers key insight
- anytime you want a safe point to return to

With regular checkpoints, you can explore freely and still keep your flow under control.
