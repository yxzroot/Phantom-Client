# AspectRatio — Phantom 1.8.9

Open **Render → AspectRatio**. Disabled and unbound by default. Mode defaults to
4:3; other options are Custom (0.5–3.0), 5:4, 16:10, 16:9, 21:9 and 1:1.
The standard slider shows the current custom value, also used by the arraylist suffix.
Settings/bind/enabled state use the existing module/config system.

The completed perspective matrix is retargeted after EntityRenderer's camera setup.
This does not depend on Lunar keeping vanilla's `Project.gluPerspective` invocation:
the old required hand argument hook caused a startup injection failure and is gone.
Optional hooks cover Project/GLU sky/cloud far-plane resets and the hand setup,
plus a final first-person item boundary. HUD/chat/hotbar/crosshair/menu orthographic projections,
window size, viewport and mouse sensitivity are untouched. **Stretch hand** defaults
off, leaving the vanilla hand/item projection intact; opting in applies the same
ratio to that pass. First and third person use the same world hook.

**Keep vertical FOV** defaults on: Minecraft's dynamic vertical FOV argument remains
unchanged and only horizontal scale changes. Off preserves vanilla horizontal FOV
by compensating the vertical angle. No FOV/game settings are overwritten. Picking
and attack rays remain vanilla; a centered symmetric projection keeps the center
ray/crosshair direction invariant. World block outlines use the same projection as
the blocks. This is not a reach or mouse-input modification.

**Smooth transition** defaults on (200ms smoothstep, real time). Mode/custom value
changes and toggles ease between ratios. OFF reaches the exact window ratio at the
end; disabling Smooth snaps. Resizes/fullscreen changes reset directly to the
selected ratio if enabled, or the exact new native ratio if disabled; leaving a
world resets to native. Matrix reads use preallocated buffers, with no temporary
arrays. Retargeting is idempotent and rejects non-perspective matrices.

A preallocated projection snapshot is restored at each world-pass return, frame
return, and before overlay rendering. A frame-start safety cleanup also handles
a previous interrupted render. MODELVIEW is restored; no texture, color, lighting,
blend or viewport state is touched. Failures log once and disable the module.
Optional invocation sites use fail-open hooks; stable method boundaries are required.
There are no required perspective-helper invocations anywhere in this mixin.

Build `tools/build-aspect.ps1 -CheckOpenGL`. Checks verify exact vanilla 1.8.9
MCP/SRG target methods and optional helper-call requirements, matrix aspect/FOV math, preset
config codecs, 60/240Hz fades, FOV math, actual GL horizontal scaling, unchanged
vertical/center projection, unchanged orthographic HUD, idempotence, normal hand ratio,
exact matrix restoration, 240 simulated passes, and
exception cleanup. All unrelated JAR entries are preserved. These are offscreen
correctness checks, not live Lunar compatibility/FPS or a fullscreen/input test;
those still need in-game confirmation, especially with third-party shader packs.
