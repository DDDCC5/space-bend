# Review and regression fixes

The independent source review identified five material issues. All were fixed before delivery:

| Finding | Final behavior | Regression coverage |
|---|---|---|
| Primary panels overlapped on the same plane | Menu, scan, levels and result replace one another; other windows remain independent | GameViewModel navigation tests |
| Finite-plane collision ignored object overhang at an edge | Swept sphere tests faces, edge capsules and vertices | Geometry/SweepCollision tests |
| Concave portal fit checked only vertices | Portal chords must stay inside the detected boundary, excluding holes | Geometry containment tests |
| Lost anchor had no recovery path | Pause, show scan, and clear orphaned chain on confirmed scan continuation | GameViewModel tracking recovery tests |
| Remaining time after transfer could skip an obstacle | Recursively sweep the destination remainder of the same fixed step | PuzzleEngine destination-obstacle test |

Additional fixes found during runtime work:

- Preserve local anchor basis across retessellation and weld repeated vertices.
- Continue falling after displaying failure instead of freezing the object in mid-air.
- Use actual payload GLB bounds for conservative collision radii and center the render hierarchy.
- Correct inverted GLB triangle winding: 13,524 triangles, zero remaining inverted faces.
- Use the SDK dp-to-meter converter for attached panels; their mesh bounds are zero and cannot determine hit-test size.
- Keep the two-second deletion timer owned by one controller, so simultaneous B/Y input cannot halve the hold time.
- Transfer at the center-plane crossing only when the entire projected body fits the aperture. Avoid radius offsets that inject gravitational potential at every crossing.
- Apply the near-mouth attraction field on approach and departure. The 30-second closed-orbit regression stays below the room ceiling.
- Revalidate cached audio handles before reuse; native emitter disposal was observed to invalidate a repeated one-shot handle.

The managed scene validator proves source delivery and an Android build. It does not prove physical hand/controller input, haptics, room-scan precision or spatial-audio listening quality.
