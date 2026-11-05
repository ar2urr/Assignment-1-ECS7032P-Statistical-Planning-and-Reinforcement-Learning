# Confirmation execution notes

The run used Java 21, the recorded TAG revision and the saved Stage 5 framework
JAR supplied through `TAG_JAR`. Its SHA-256 matches the Stage 5 manifest:
`a73464d8a7664f7b4445fe9f98752fd7c041ef57e71edcc1a0d7c5ec5860cf81`.

Two earlier attempts stalled while creating the local JAR, before any games
started. Neither produced measured results. The completed run kept the planned
protocol and master seed, with no parameter changes or stopping based on outcomes.

The groupAG archive check ran afterwards, one game at a time. Its decision times
are kept separate from the parallel iteration runs.
