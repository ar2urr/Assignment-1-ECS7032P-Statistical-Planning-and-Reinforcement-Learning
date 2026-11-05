# Confirmation execution notes

The completed run reused the frozen Stage 5 framework JAR through `TAG_JAR`.
Its SHA-256 matches the original study manifest exactly:
`a73464d8a7664f7b4445fe9f98752fd7c041ef57e71edcc1a0d7c5ec5860cf81`.
Java 21 and the recorded pinned TAG source revision were used. Two earlier
build attempts stopped in local JAR creation before any games began; neither
produced measured outcomes. The completed run used the same prespecified
protocol and master seed, with no outcome-based stopping or parameter changes.

The groupAG archive check ran sequentially after this study. Its decision times
are retained separately and are not pooled with parallel iteration-run times.
