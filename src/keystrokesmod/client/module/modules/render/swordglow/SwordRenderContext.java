package keystrokesmod.client.module.modules.render.swordglow;

/** Scoped ownership of held renders, independent of the camera transform applied by Lunar. */
public final class SwordRenderContext {
    public static final int NONE = 0, FIRST_PERSON = 1, THIRD_PERSON = 2, GUI = 3;
    private final Object[] owners = new Object[16];
    private final int[] views = new int[16];
    private int depth, overflow;

    /** ItemRenderer is a held-item entry point; Lunar's legacy arm path deliberately passes NONE. */
    public void held(Object owner, int transform, boolean localPlayer) {
        push(owner, transform == NONE && localPlayer ? FIRST_PERSON : transform);
    }

    /** A nested RenderItem call may use NONE because its caller already applied the arm transform. */
    public void model(Object owner, int transform) {
        if (transform == NONE && owner != null && owner == owner()) transform = view();
        push(owner, transform);
    }

    /** Inventory icons never inherit a held-player scope, even if another mod cancels a parent. */
    public void gui() { push(null, GUI); }

    private void push(Object owner, int view) {
        if (depth == owners.length || overflow > 0) { overflow++; return; }
        owners[depth] = owner; views[depth++] = view;
    }

    public Object owner() { return overflow > 0 || depth == 0 ? null : owners[depth - 1]; }
    public int view() { return overflow > 0 || depth == 0 ? NONE : views[depth - 1]; }
    public void end() {
        if (overflow > 0) { overflow--; return; }
        if (depth > 0) owners[--depth] = null;
    }
    public void clear() {
        while (depth > 0) owners[--depth] = null;
        overflow = 0;
    }
}
