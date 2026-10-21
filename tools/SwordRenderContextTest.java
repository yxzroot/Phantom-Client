import keystrokesmod.client.module.modules.render.swordglow.SwordRenderContext;

/** Reproduces Lunar's observed first-person NONE -> RenderItem NONE call chain. */
public final class SwordRenderContextTest {
    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }
    public static void main(String[] args) {
        SwordRenderContext context = new SwordRenderContext();
        Object local = new Object(), other = new Object();
        context.held(local, SwordRenderContext.NONE, true);
        check(context.owner() == local && context.view() == SwordRenderContext.FIRST_PERSON,
                "Lunar's already-transformed held sword was rejected");
        context.model(local, SwordRenderContext.NONE);
        check(context.owner() == local && context.view() == SwordRenderContext.FIRST_PERSON,
                "Nested model NONE erased the first-person owner");
        context.gui();
        check(context.owner() == null && context.view() == SwordRenderContext.GUI,
                "Hotbar inherited the held-sword owner");
        context.model(local, SwordRenderContext.GUI);
        check(context.view() == SwordRenderContext.GUI, "GUI model inherited a hand transform");
        context.end(); context.end();
        check(context.owner() == local && context.view() == SwordRenderContext.FIRST_PERSON,
                "Exiting nested inventory rendering lost held context");
        context.end(); context.end();
        check(context.owner() == null && context.view() == SwordRenderContext.NONE, "Context leaked after hand rendering");
        context.model(local, SwordRenderContext.NONE);
        check(context.view() == SwordRenderContext.NONE, "Standalone NONE was mistaken for a held sword");
        context.end();
        context.held(other, SwordRenderContext.NONE, false);
        check(context.view() == SwordRenderContext.NONE, "Remote NONE was mistaken for local hand");
        context.clear();
        context.held(other, SwordRenderContext.THIRD_PERSON, false);
        context.model(other, SwordRenderContext.NONE);
        check(context.view() == SwordRenderContext.THIRD_PERSON, "Third person lost render ownership");
        context.clear();
        check(context.owner() == null, "Canceled parent leaked into the next frame");
        for (int i = 0; i < 20; i++) context.held(local, SwordRenderContext.FIRST_PERSON, true);
        check(context.owner() == null, "Overflow must fail closed");
        for (int i = 0; i < 20; i++) context.end();
        check(context.owner() == null, "Overflow unwind left a stale owner");
        System.out.println("PASS: Lunar held NONE and nested NONE, vanilla first/third person, isolated hotbar/GUI, cancellation cleanup and bounded scope overflow");
    }
}
