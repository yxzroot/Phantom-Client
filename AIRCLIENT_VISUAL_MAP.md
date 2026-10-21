# AirClient visual dependency map

Scanned before implementation on 2026-10-19 from C:/Users/johng/Downloads/AirClient-main.zip (source extracted into Downloads/AirClient-main).

Scope update: user requested in-game Minecraft visuals only. ClickGUI, setting widgets and HUD editor are inventoried for completeness but EXCLUDED from adaptation. Phantom GUI classes are not modified.

License: source LICENSE is GPL v3; preserve individual file notices (including GPLv3-or-later where present), attribution, modification dates, no-warranty notice and corresponding source obligations on distribution. See THIRD_PARTY_NOTICES and LICENSE.

Integration: ClientThemesUtils palette -> AirPalette/AirTheme; NewArrayList and Watermark appearance -> AirHudRenderer; RoundedUtil -> AirDraw. PhantomHUD retains its existing event host, settings/config model, fonts and classic renderer. SwordGlow reads the shared visual theme. No LiquidBounce framework, event bus or gameplay modules are imported.

All paths below are relative to src/main/java/net/ccbluex/liquidbounce. Imports are the source's direct dependency edges, not a claim that every imported module will be ported. Wildcards and Kotlin declarations are retained as scanned; platform java/kotlin/Minecraft/LWJGL APIs remain external dependencies.

## features/module/modules/client/ClickGUI.kt

Declarations: ClickGUI.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.clickGui`
- `net.ccbluex.liquidbounce.event.PacketEvent`
- `net.ccbluex.liquidbounce.event.handler`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.astolfo.AstolfoClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.augustus.AugustusClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.flat.FlatClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.moonlight.MoonLightClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.neverlose.NeverloseScreen`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiScreen`
- `net.ccbluex.liquidbounce.ui.client.clickgui.rise.RiseClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.MinimalStyle`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle`
- `net.minecraft.network.play.server.S2EPacketCloseWindow`
- `org.lwjgl.input.Keyboard`
- `java.awt.Color`

## features/module/modules/client/ThemeManager.kt

Declarations: ThemeManager.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.client.theme.ThemeSelector`
- `net.ccbluex.liquidbounce.utils.client.ClientThemesUtils`
- `org.lwjgl.input.Keyboard`
- `java.awt.Color`

## features/module/modules/render/ColorMixer.kt

Declarations: ColorMixer, ColorElementMaterial.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `java.awt.Color`

## features/module/modules/render/HUD.kt

Declarations: HUD.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME`
- `net.ccbluex.liquidbounce.LiquidBounce.hud`
- `net.ccbluex.liquidbounce.event.*`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.Gui`
- `net.minecraft.client.gui.GuiChat`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.shader.Framebuffer`
- `net.minecraft.util.ResourceLocation`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.AnimationUtils`
- `net.minecraftforge.client.event.RenderGameOverlayEvent`
- `net.minecraftforge.common.MinecraftForge`
- `net.minecraftforge.fml.common.eventhandler.EventPriority`
- `net.minecraftforge.fml.common.eventhandler.SubscribeEvent`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `kotlin.math.abs`
- `kotlin.math.ceil`
- `kotlin.math.max`
- `kotlin.math.min`

## file/configs/ClickGuiConfig.kt

Declarations: ClickGuiConfig.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `com.google.gson.JsonObject`
- `net.ccbluex.liquidbounce.LiquidBounce.clickGui`
- `net.ccbluex.liquidbounce.file.FileConfig`
- `net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.io.readJson`
- `java.io.*`

## file/configs/HudConfig.kt

Declarations: HudConfig.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `com.google.gson.JsonArray`
- `com.google.gson.JsonObject`
- `net.ccbluex.liquidbounce.config.FontValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.file.FileConfig`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `net.ccbluex.liquidbounce.utils.io.json`
- `net.ccbluex.liquidbounce.utils.io.jsonArray`
- `net.ccbluex.liquidbounce.utils.io.readJson`
- `net.ccbluex.liquidbounce.utils.io.writeJson`
- `java.io.File`
- `java.io.IOException`

## ui/client/clickgui/ClickGui.kt

Declarations: ClickGui.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `kotlinx.coroutines.launch`
- `net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME`
- `net.ccbluex.liquidbounce.LiquidBounce.moduleManager`
- `net.ccbluex.liquidbounce.api.ClientApi`
- `net.ccbluex.liquidbounce.api.autoSettingsList`
- `net.ccbluex.liquidbounce.api.loadSettings`
- `net.ccbluex.liquidbounce.config.SettingsUtils`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.guiColor`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scale`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scrolls`
- `net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.Style`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.utils.attack.EntityUtils.Targets`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `net.ccbluex.liquidbounce.utils.client.asResourceLocation`
- `net.ccbluex.liquidbounce.utils.client.chat`
- `net.ccbluex.liquidbounce.utils.client.playSound`
- `net.ccbluex.liquidbounce.utils.kotlin.SharedScopes`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.renderer.GlStateManager.disableLighting`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11.glScaled`
- `kotlin.math.roundToInt`

## ui/client/clickgui/Panel.kt

Declarations: Panel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.clickGui`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.fadeSpeed`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.maxElements`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.panelsForcedInBoundaries`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scale`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.Element`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `kotlin.math.max`
- `kotlin.math.min`
- `kotlin.math.roundToInt`

## ui/client/clickgui/airi/AiriClickGui.kt

Declarations: AiriClickGui, MeasuredMsg, ControlBtn, Ctrl.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `kotlinx.coroutines.launch`
- `net.ccbluex.liquidbounce.ai.api.AiriSettings`
- `net.ccbluex.liquidbounce.ai.conversation.Conversation`
- `net.ccbluex.liquidbounce.ai.conversation.ConversationManager`
- `net.ccbluex.liquidbounce.ai.conversation.Message`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `net.ccbluex.liquidbounce.file.FileManager.airiConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.kotlin.SharedScopes`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `java.io.IOException`
- `kotlin.math.abs`
- `kotlin.math.roundToInt`
- `kotlin.math.sqrt`

## ui/client/clickgui/airi/AiriStyles.kt

Declarations: AiriStyleConfig.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.ui.font.GameFontRenderer`
- `java.awt.Color`

## ui/client/clickgui/astolfo/AstolfoClickGui.kt

Declarations: AstolfoClickGui, Window.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.config.TextValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.file.FileManager.modulesConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.file.FileManager.valuesConfig`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.GuiScreen`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `java.awt.Color`
- `java.io.IOException`
- `kotlin.math.max`
- `kotlin.math.roundToInt`

## ui/client/clickgui/augustus/AugustusClickGui.java

Declarations: AugustusClickGui.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.FloatRangeValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.config.IntRangeValue`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.config.TextValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.gui.GuiTextField`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.MathHelper`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11`
- `javax.vecmath.Vector2f`
- `java.awt.Color`
- `java.io.IOException`
- `java.util.HashMap`
- `java.util.List`
- `java.util.Map`
- `java.util.stream.Collectors`

## ui/client/clickgui/elements/ButtonElement.kt

Declarations: ButtonElement.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.clickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`

## ui/client/clickgui/elements/Element.kt

Declarations: Element.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`

## ui/client/clickgui/elements/ModuleElement.kt

Declarations: ModuleElement.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.clickGui`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.spacedModules`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`

## ui/client/clickgui/flat/FlatClickGui.kt

Declarations: FlatClickGui, FlatTheme, GuiEvent.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.moduleManager`
- `net.ccbluex.liquidbounce.config.*`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.ui.font.GameFontRenderer`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.util.MathHelper`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `java.io.IOException`
- `kotlin.math.*`

## ui/client/clickgui/moonlight/MoonLightClickGui.kt

Declarations: MoonLightClickGui, Panel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.config.TextValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.file.FileManager.modulesConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.file.FileManager.valuesConfig`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.minecraft.client.gui.GuiScreen`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `java.awt.Color`
- `java.io.IOException`
- `kotlin.math.roundToInt`

## ui/client/clickgui/neverlose/NeverloseScreen.kt

Declarations: NeverloseScreen.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.BlockValue`
- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.FloatRangeValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.config.FontValue`
- `net.ccbluex.liquidbounce.config.IntRangeValue`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.config.TextValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `net.ccbluex.liquidbounce.file.FileManager.modulesConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.file.FileManager.valuesConfig`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `java.awt.Color`
- `java.io.IOException`
- `java.text.SimpleDateFormat`
- `java.util.Date`
- `kotlin.math.roundToInt`

## ui/client/clickgui/opai/Animation.kt

Declarations: Animation.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `kotlin.math.*`

## ui/client/clickgui/opai/Easing.kt

Declarations: Easing.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- None declared.

## ui/client/clickgui/opai/MD3Theme.kt

Declarations: MD3Theme, TonalPalette.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `java.awt.Color`
- `kotlin.math.abs`
## ui/client/clickgui/opai/OpaiCompat.kt

Declarations: OpaiCompat, LuminRenderSystem, LuminRenderTarget, Constants, TranslateComponent, EpsilonTranslateComponent, StaticFontLoader, IMEFocusHelper.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `net.minecraft.client.Minecraft`

## ui/client/clickgui/opai/OpaiGUI.kt

Declarations: OpaiGUI.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiScreen`
- `org.lwjgl.input.Keyboard`
- `java.awt.Color`

## ui/client/clickgui/opai/OpaiLayoutState.kt

Declarations: OpaiLayoutState.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.component.OpaiPanel`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.Constants`
- `com.google.gson.*`
- `java.io.IOException`
- `java.nio.charset.StandardCharsets`
- `java.nio.file.Files`
- `java.nio.file.Path`
- `java.nio.file.StandardOpenOption`
- `java.util.List`

## ui/client/clickgui/opai/OpaiManagers.kt

Declarations: OpaiManagers, ConfigManager, AddonManager, FriendManager, OpaiModuleManager, SoundManager, SoundKey.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.features.module.ModuleManager`
- `net.ccbluex.liquidbounce.file.FileManager`
- `net.ccbluex.liquidbounce.features.module.Module`

## ui/client/clickgui/opai/OpaiRenderer.kt

Declarations: OpaiRenderer, Slot.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.minecraft.client.gui.ScaledResolution`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.Constants.mc`
- `kotlin.math.roundToInt`

## ui/client/clickgui/opai/OpaiScreen.kt

Declarations: OpaiScreen.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.component.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.OpaiTextField`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.minecraft.client.gui.GuiScreen`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `java.awt.Color`
- `java.io.IOException`
- `java.util.ArrayList`
- `net.ccbluex.liquidbounce.utils.render.InternalBlurShader`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.util.ResourceLocation`

## ui/client/clickgui/opai/OpaiTheme.kt

Declarations: OpaiTheme.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `java.awt.Color`

## ui/client/clickgui/opai/Renderers.kt

Declarations: ShadowRenderer, RoundRectRenderer, RoundRectOutlineRenderer, RectRenderer, TriangleRenderer, TextRenderer, ScissorHelper.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.ui.font.GameFontRenderer`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/clickgui/opai/SettingAdapters.kt

Declarations: SettingAdapters, Setting, BoolSetting, IntSetting, DoubleSetting, IntRangeSetting, DoubleRangeSetting, EnumSetting, ColorSetting, KeybindSetting, StringSetting, ButtonSetting, SettingGroup.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.*`
- `java.awt.Color`

## ui/client/clickgui/opai/component/AbstractOpaiPanel.kt

Declarations: AbstractOpaiPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.TranslateComponent`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.StaticFontLoader`

## ui/client/clickgui/opai/component/AddonOpaiPanel.kt

Declarations: AddonOpaiPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.StaticFontLoader`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.AddonManager`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager`
- `java.util.*`

## ui/client/clickgui/opai/component/CategoryOpaiPanel.kt

Declarations: CategoryOpaiPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.OpaiModuleManager`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `java.util.ArrayList`

## ui/client/clickgui/opai/component/Component.kt

Declarations: Component.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`

## ui/client/clickgui/opai/component/ConfigOpaiPanel.kt

Declarations: ConfigOpaiPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.OpaiTextField`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager`
- `org.lwjgl.input.Keyboard`
- `java.util.Objects`

## ui/client/clickgui/opai/component/FriendOpaiPanel.kt

Declarations: FriendOpaiPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.OpaiTextField`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.FriendManager`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager`
- `org.lwjgl.input.Keyboard`
- `java.util.stream.Collectors`

## ui/client/clickgui/opai/component/MainOpaiPanel.kt

Declarations: MainOpaiPanel, Entry.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.Constants`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.minecraft.client.Minecraft`
- `net.minecraft.client.gui.ScaledResolution`
- `kotlin.math.roundToInt`
- `org.lwjgl.BufferUtils`
- `org.lwjgl.opengl.GL11`
- `java.util.ArrayList`
- `java.util.function.BooleanSupplier`
- `java.util.function.Consumer`
- `java.awt.Color`

## ui/client/clickgui/opai/component/ModuleButton.kt

Declarations: ModuleButton, SettingSection.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.StaticFontLoader`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.OpaiModuleManager`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.BoolSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ColorSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleRangeSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.EnumSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntRangeSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.SettingGroup`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.StringSetting`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.config.*`
- `net.minecraft.client.Minecraft`
- `net.minecraft.client.gui.ScaledResolution`
- `kotlin.math.roundToInt`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `java.util.*`
- `org.lwjgl.BufferUtils`

## ui/client/clickgui/opai/component/OpaiPanel.kt

Declarations: OpaiPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`

## ui/client/clickgui/opai/component/SettingsContent.kt

Declarations: SettingsContent, SettingSection.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.BoolSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntRangeSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleRangeSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.EnumSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ColorSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.KeybindSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.StringSetting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.Setting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.SettingGroup`
- `java.util.*`

## ui/client/clickgui/opai/component/SettingsListPanel.kt

Declarations: SettingsListPanel.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.Setting`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.SettingGroup`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.SettingWidget`
- `java.util.*`

## ui/client/clickgui/opai/widget/BoolWidget.kt

Declarations: BoolWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.BoolSetting`
- `net.minecraft.util.MathHelper`

## ui/client/clickgui/opai/widget/ButtonWidget.kt

Declarations: ButtonWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ButtonSetting`

## ui/client/clickgui/opai/widget/ColorWidget.kt

Declarations: ColorWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ColorSetting`
- `net.minecraft.util.MathHelper`
- `java.awt.Color`

## ui/client/clickgui/opai/widget/DoubleRangeWidget.kt

Declarations: DoubleRangeWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.RangeSlider`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleRangeSetting`
- `net.minecraft.util.MathHelper`
- `kotlin.math.abs`
- `kotlin.math.max`
- `kotlin.math.min`

## ui/client/clickgui/opai/widget/DoubleSliderWidget.kt

Declarations: DoubleSliderWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleSetting`
- `net.minecraft.util.MathHelper`
- `org.lwjgl.input.Keyboard`
- `java.text.DecimalFormat`

## ui/client/clickgui/opai/widget/EnumWidget.kt

Declarations: EnumWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.EnumSetting`
- `java.awt.Color`

## ui/client/clickgui/opai/widget/IntRangeWidget.kt

Declarations: IntRangeWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.RangeSlider`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntRangeSetting`
- `net.minecraft.util.MathHelper`
- `kotlin.math.abs`
- `kotlin.math.max`
- `kotlin.math.min`
- `kotlin.math.roundToInt`

## ui/client/clickgui/opai/widget/IntSliderWidget.kt

Declarations: IntSliderWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.*`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntSetting`
- `net.minecraft.util.MathHelper`
- `org.lwjgl.input.Keyboard`
- `kotlin.math.roundToInt`

## ui/client/clickgui/opai/widget/KeybindWidget.kt

Declarations: KeybindWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.KeybindSetting`
- `org.lwjgl.input.Keyboard`

## ui/client/clickgui/opai/widget/OpaiTextField.kt

Declarations: OpaiTextField.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.MD3Theme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.IMEFocusHelper`
- `net.minecraft.client.gui.GuiScreen`
- `org.lwjgl.input.Keyboard`
- `java.util.function.Predicate`
- `kotlin.math.min`

## ui/client/clickgui/opai/widget/SettingWidget.kt

Declarations: SettingWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.component.Component`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.Setting`

## ui/client/clickgui/opai/widget/StringWidget.kt

Declarations: StringWidget.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme`
- `net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.StringSetting`
- `org.lwjgl.input.Keyboard`

## ui/client/clickgui/rise/RiseClickGui.kt

Declarations: RiseClickGui.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.BlockValue`
- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.FloatRangeValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.config.FontValue`
- `net.ccbluex.liquidbounce.config.IntRangeValue`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.config.TextValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI`
- `net.ccbluex.liquidbounce.file.FileManager.modulesConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.file.FileManager.valuesConfig`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.gui.ScaledResolution`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `java.io.IOException`
- `kotlin.math.abs`
- `kotlin.math.roundToInt`

## ui/client/clickgui/style/Style.kt

Declarations: Style.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.file.FileManager.valuesConfig`
- `net.ccbluex.liquidbounce.ui.client.clickgui.Panel`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.client.asResourceLocation`
- `net.ccbluex.liquidbounce.utils.client.playSound`
- `net.ccbluex.liquidbounce.utils.extensions.decimalPlaces`
- `net.ccbluex.liquidbounce.utils.timing.WaitTickUtils`
- `net.ccbluex.liquidbounce.utils.ui.EditableText`
- `org.lwjgl.input.Mouse`
- `java.awt.Color`
- `kotlin.math.max`

## ui/client/clickgui/style/styles/LiquidBounceStyle.kt

Declarations: LiquidBounceStyle.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.*`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.guiColor`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scale`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp`
- `net.ccbluex.liquidbounce.ui.client.clickgui.Panel`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.Style`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35`
- `net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName`
- `net.ccbluex.liquidbounce.utils.extensions.component1`
- `net.ccbluex.liquidbounce.utils.extensions.component2`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.blendColors`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.minecraftRed`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache`
- `net.ccbluex.liquidbounce.utils.ui.EditableText`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.StringUtils`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `java.awt.Color`
- `kotlin.math.abs`
- `kotlin.math.roundToInt`

## ui/client/clickgui/style/styles/MinimalStyle.kt

Declarations: MinimalStyle.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.*`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.guiColor`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scale`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp`
- `net.ccbluex.liquidbounce.ui.client.clickgui.Panel`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.Style`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35`
- `net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName`
- `net.ccbluex.liquidbounce.utils.extensions.component1`
- `net.ccbluex.liquidbounce.utils.extensions.component2`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.blendColors`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.minecraftRed`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache`
- `net.ccbluex.liquidbounce.utils.ui.EditableText`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.StringUtils`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `java.awt.Color`
- `kotlin.math.abs`
- `kotlin.math.roundToInt`

## ui/client/clickgui/style/styles/SlowlyStyle.kt

Declarations: SlowlyStyle.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.*`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scale`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp`
- `net.ccbluex.liquidbounce.ui.client.clickgui.Panel`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.Style`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35`
- `net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName`
- `net.ccbluex.liquidbounce.utils.extensions.component1`
- `net.ccbluex.liquidbounce.utils.extensions.component2`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.blendColors`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledCircle`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache`
- `net.ccbluex.liquidbounce.utils.ui.EditableText`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.StringUtils`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `java.awt.Color`
- `kotlin.math.abs`
- `kotlin.math.roundToInt`

## ui/client/hud/HUD.kt

Declarations: HUD.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.*`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.extensions.component1`
- `net.ccbluex.liquidbounce.utils.extensions.component2`
- `net.minecraft.client.gui.ScaledResolution`
- `org.lwjgl.opengl.GL11.*`
- `java.util.*`
- `java.util.concurrent.CopyOnWriteArrayList`
- `kotlin.math.max`
- `kotlin.math.min`

## ui/client/hud/designer/EditorPanel.kt

Declarations: EditorPanel, ElementEditableText.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.config.*`
- `net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.guiColor`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.file.FileManager.valuesConfig`
- `net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui`
- `net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle.rgbaLabels`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD.ELEMENTS`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.blendColors`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.minecraftRed`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.makeScissorBox`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache`
- `net.ccbluex.liquidbounce.utils.timing.WaitTickUtils`
- `net.ccbluex.liquidbounce.utils.ui.EditableText`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.MathHelper`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `kotlin.math.roundToInt`

## ui/client/hud/designer/GuiHudDesigner.kt

Declarations: GuiHudDesigner.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.file.FileManager.hudConfig`
- `net.ccbluex.liquidbounce.file.FileManager.saveConfig`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.EditorPanel.ElementEditableText`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.minecraft.client.gui.GuiScreen`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `kotlin.math.min`

## ui/client/hud/element/Element.kt

Declarations: Element, ElementInfo, Side, Horizontal, Vertical, Border.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.Configurable`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect`
- `net.minecraft.client.gui.ScaledResolution`
- `java.awt.Color`
- `kotlin.math.max`
- `kotlin.math.min`

## ui/client/hud/element/elements/Armor.kt

Declarations: Armor.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.minecraft.block.material.Material`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting`
- `net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting`
- `org.lwjgl.opengl.GL11.*`

## ui/client/hud/element/elements/Arraylist.kt

Declarations: Arraylist.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.moduleManager`
- `net.ccbluex.liquidbounce.config.Configurable`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.modules.misc.GameDetector`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.extensions.safeDiv`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.render.*`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.EmbeddedStencil`
- `net.ccbluex.liquidbounce.utils.render.InternalBlurShader`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.fade`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader`
- `net.minecraft.client.renderer.GlStateManager.resetColor`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/hud/element/elements/Cooldown.kt

Declarations: Cooldown.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.utils.attack.CooldownHelper.getAttackCooldownProgress`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `java.awt.Color`

## ui/client/hud/element/elements/Effects.kt

Declarations: Effects.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.ui.font.GameFontRenderer`
- `net.minecraft.client.resources.I18n`
- `net.minecraft.potion.Potion`

## ui/client/hud/element/elements/Image.kt

Declarations: Image.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.utils.io.FileFilters`
- `net.ccbluex.liquidbounce.utils.io.MiscUtils`
- `net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.randomNumber`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage`
- `net.minecraft.client.renderer.texture.DynamicTexture`
- `net.minecraft.util.ResourceLocation`
- `java.awt.Color`
- `java.io.File`
- `java.util.*`
- `javax.imageio.ImageIO`

## ui/client/hud/element/elements/Inventory.kt

Declarations: Inventory.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.inventory.inventorySlot`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorder`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect2`
- `net.minecraft.client.gui.FontRenderer`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting`
- `net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`

## ui/client/hud/element/elements/Keystrokes.kt

Declarations: Keystrokes, GridKey.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.ui.font.GameFontRenderer`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.extensions.safeDiv`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.withOutline`
- `java.awt.Color`
- `kotlin.math.nextDown`

## ui/client/hud/element/elements/Model.kt

Declarations: Model.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.client.renderer.OpenGlHelper`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.entity.EntityLivingBase`
- `org.lwjgl.opengl.GL11.*`
- `kotlin.math.abs`
- `kotlin.math.atan`

## ui/client/hud/element/elements/MusicLyric.kt

Declarations: MusicLyric.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader`
- `net.ccbluex.liquidbounce.utils.render.toColorArray`
- `net.ccbluex.liquidbounce.utils.client.ClientThemesUtils`
- `net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`

## ui/client/hud/element/elements/NewArrayList.kt

Declarations: NewArrayList, ItemInfo.

Disposition: Selected visual/data reference, rewritten against Phantom.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.moduleManager`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.RoundedCorners`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.client.ClientThemesUtils`
- `net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/hud/element/elements/Notifications.kt

Declarations: Notifications, SeverityType, Notification, FadeState.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification`
- `net.ccbluex.liquidbounce.ui.client.hud.HUD.notifications`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification.Companion.maxTextLength`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.minecraft.util.ResourceLocation`
- `java.awt.Color`

## ui/client/hud/element/elements/OpaiArrayList.kt

Declarations: OpaiArrayList, ItemInfo.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.moduleManager`
- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.EmbeddedStencil`
- `net.ccbluex.liquidbounce.utils.render.InternalBlurShader`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.RoundedCorners`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/hud/element/elements/OpaiTarget.kt

Declarations: OpaiTarget.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.event.AttackEvent`
- `net.ccbluex.liquidbounce.event.Listenable`
- `net.ccbluex.liquidbounce.event.handler`
- `net.ccbluex.liquidbounce.features.module.modules.combat.KillAura`
- `net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `net.minecraft.client.gui.GuiChat`
- `net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting`
- `net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting`
- `net.minecraft.entity.Entity`
- `net.minecraft.entity.EntityLivingBase`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`

## ui/client/hud/element/elements/Radar.kt

Declarations: Radar.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.module.modules.render.ESP`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected`
- `net.ccbluex.liquidbounce.utils.extensions.toRadians`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.MiniMapRegister`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorder`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.makeScissorBox`
- `net.ccbluex.liquidbounce.utils.render.SafeVertexBuffer`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader`
- `net.minecraft.client.renderer.GlStateManager.bindTexture`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.client.renderer.vertex.VertexBuffer`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.util.vector.Vector2f`
- `java.awt.Color`
- `kotlin.math.*`

## ui/client/hud/element/elements/ScoreboardElement.kt

Declarations: ScoreboardElement.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `com.google.common.collect.Iterables`
- `com.google.common.collect.Lists`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.Gui`
- `net.minecraft.scoreboard.ScoreObjective`
- `net.minecraft.scoreboard.ScorePlayerTeam`
- `net.minecraft.util.EnumChatFormatting`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/hud/element/elements/SpeedGraph.kt

Declarations: SpeedGraph.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor`
- `net.minecraft.client.renderer.GlStateManager.resetColor`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `kotlin.math.sqrt`

## ui/client/hud/element/elements/TabGUI.kt

Declarations: TabGUI, Action.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.module.Category`
- `net.ccbluex.liquidbounce.features.module.Module`
- `net.ccbluex.liquidbounce.features.module.ModuleManager`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader`
- `net.minecraft.client.gui.FontRenderer`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.opengl.GL11.glColor4f`
- `java.awt.Color`
- `kotlin.math.abs`

## ui/client/hud/element/elements/Taco.kt

Declarations: Taco.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.command.commands.TacoCommand.tacoToggle`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.ResourceLocation`

## ui/client/hud/element/elements/Target.kt

Declarations: Target.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.event.AttackEvent`
- `net.ccbluex.liquidbounce.event.Listenable`
- `net.ccbluex.liquidbounce.event.handler`
- `net.ccbluex.liquidbounce.features.module.modules.combat.KillAura`
- `net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.EmbeddedStencil`
- `net.ccbluex.liquidbounce.utils.render.InternalBlurShader`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping`
- `net.ccbluex.liquidbounce.utils.render.Stencil`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `net.minecraft.client.gui.GuiChat`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting`
- `net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting`
- `net.minecraft.entity.Entity`
- `net.minecraft.entity.EntityLivingBase`
- `net.minecraft.entity.player.EntityPlayer`
- `net.minecraft.util.EnumChatFormatting.BOLD`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `java.text.DecimalFormat`
- `java.text.DecimalFormatSymbols`
- `java.util.*`
- `kotlin.math.*`

## ui/client/hud/element/elements/Target2.kt

Declarations: Target2.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.config.*`
- `net.ccbluex.liquidbounce.features.module.modules.combat.KillAura`
- `net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl.*`
- `net.ccbluex.liquidbounce.utils.render.*`
- `net.minecraft.client.gui.GuiChat`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/hud/element/elements/Target3.kt

Declarations: Target3.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.event.AttackEvent`
- `net.ccbluex.liquidbounce.event.Listenable`
- `net.ccbluex.liquidbounce.event.handler`
- `net.ccbluex.liquidbounce.features.module.modules.combat.KillAura`
- `net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth`
- `net.ccbluex.liquidbounce.utils.extensions.lerpWith`
- `net.ccbluex.liquidbounce.utils.extensions.safeDiv`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawGradientRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawScaledCustomSizeModalRect`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader`
- `net.minecraft.client.gui.GuiChat`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.Entity`
- `net.minecraft.entity.EntityLivingBase`
- `net.minecraft.entity.player.EntityPlayer`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `java.text.DecimalFormat`
- `java.text.DecimalFormatSymbols`
- `java.util.*`
- `kotlin.math.abs`
- `kotlin.math.max`
- `kotlin.math.pow`

## ui/client/hud/element/elements/Text.kt

Declarations: Text.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.CLIENT_AUTHOR`
- `net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME`
- `net.ccbluex.liquidbounce.LiquidBounce.clientCommit`
- `net.ccbluex.liquidbounce.LiquidBounce.clientVersionText`
- `net.ccbluex.liquidbounce.features.module.modules.combat.KillAura.blockStatus`
- `net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold`
- `net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.ui.font.GameFontRenderer`
- `net.ccbluex.liquidbounce.utils.GlowUtils`
- `net.ccbluex.liquidbounce.utils.attack.CPSCounter`
- `net.ccbluex.liquidbounce.utils.client.PPSCounter`
- `net.ccbluex.liquidbounce.utils.client.ServerUtils`
- `net.ccbluex.liquidbounce.utils.extensions.getPing`
- `net.ccbluex.liquidbounce.utils.inventory.InventoryUtils`
- `net.ccbluex.liquidbounce.utils.inventory.SilentHotbar`
- `net.ccbluex.liquidbounce.utils.movement.BPSUtils`
- `net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed`
- `net.ccbluex.liquidbounce.utils.movement.TimerBalanceUtils`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat`
- `net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader`
- `net.ccbluex.liquidbounce.utils.render.toColorArray`
- `net.minecraft.client.Minecraft`
- `net.minecraft.client.gui.inventory.GuiContainer`
- `net.minecraft.client.gui.inventory.GuiInventory`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting`
- `net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting`
- `net.minecraft.item.ItemBlock`
- `net.minecraft.item.ItemSword`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `java.text.DecimalFormat`
- `java.text.SimpleDateFormat`
- `kotlin.math.max`

## ui/client/hud/element/elements/Watermark.kt

Declarations: Watermark.

Disposition: Selected visual/data reference, rewritten against Phantom.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element`
- `net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Side`
- `net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.extensions.getPing`
- `net.ccbluex.liquidbounce.utils.render.BlurUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.ccbluex.liquidbounce.utils.client.ClientThemesUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.util.EnumChatFormatting`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `java.text.SimpleDateFormat`
- `java.util.*`

## ui/client/hud/element/elements/targets2/TargetStyle.kt

Declarations: TargetStyle.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.Value`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.utils.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.Gui`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.OpenGlHelper`
- `net.minecraft.entity.player.EntityPlayer`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11.*`
- `java.awt.Color`
- `java.text.DecimalFormat`
- `java.text.DecimalFormatSymbols`
- `java.util.*`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Astolfo.kt

Declarations: Astolfo.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.extensions.darker`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.inventory.GuiInventory`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Astolfo2.kt

Declarations: Astolfo2.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityOnScreen`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11`

## ui/client/hud/element/elements/targets2/impl/AsuidBounce.kt

Declarations: AsuidBounce.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Chill.kt

Declarations: Chill.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.utils.CharRenderer`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.extensions.darker`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.ShaderUtils`
- `net.ccbluex.liquidbounce.utils.render.Stencil`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11`

## ui/client/hud/element/elements/targets2/impl/Exhibition.kt

Declarations: Exhibition.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Flux.kt

Declarations: Flux.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Hanabi.kt

Declarations: Hanabi.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/LiquidBounce.kt

Declarations: LiquidBounce.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Lnk.kt

Declarations: Lnk.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Moon.kt

Declarations: Moon.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Moon4.kt

Declarations: Moon4.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Novoline.kt

Declarations: Novoline.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`

## ui/client/hud/element/elements/targets2/impl/Novoline2.kt

Declarations: Novoline2.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Novoline3.kt

Declarations: Novoline3.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Raven.kt

Declarations: Raven.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`

## ui/client/hud/element/elements/targets2/impl/RavenB4.kt

Declarations: RavenB4.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Remix.kt

Declarations: Remix.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Rice.kt

Declarations: Rice.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.BoolValue`
- `net.ccbluex.liquidbounce.config.FloatValue`
- `net.ccbluex.liquidbounce.config.IntValue`
- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.utils.Particle`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.utils.ShapeType`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox`
- `net.ccbluex.liquidbounce.utils.kotlin.RandomUtils`
- `net.ccbluex.liquidbounce.utils.render.BlendUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.Stencil`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `java.util.*`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/RiseModern.kt

Declarations: RiseModern.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.max`
- `kotlin.math.round`

## ui/client/hud/element/elements/targets2/impl/Slowly.kt

Declarations: Slowly.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.entity.player.EntityPlayer`
- `java.awt.Color`
- `kotlin.math.pow`

## ui/client/hud/element/elements/targets2/impl/Tifality.kt

Declarations: Tifality.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Border`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2`
- `net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.render.BlendUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.entity.player.EntityPlayer`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/hud/element/elements/targets2/utils/CharRenderer.kt

Declarations: CharRenderer.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.render.AnimationUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.gui.ScaledResolution`
- `org.lwjgl.opengl.GL11`
- `java.text.DecimalFormat`
- `java.text.DecimalFormatSymbols`
- `java.util.*`

## ui/client/hud/element/elements/targets2/utils/Particle.kt

Declarations: Particle, ShapeType.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.EaseUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## ui/client/theme/ThemeSelector.java

Declarations: ThemeSelector.

Disposition: Excluded after scope update (GUI/editor).

Dependencies:

- `net.ccbluex.liquidbounce.features.module.modules.client.ThemeManager`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.client.ClientThemesUtils`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.ccbluex.liquidbounce.utils.render.RoundedUtil`
- `net.minecraft.client.gui.GuiScreen`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `org.lwjgl.input.Keyboard`
- `org.lwjgl.input.Mouse`
- `org.lwjgl.opengl.GL11`
- `java.awt.*`
- `java.io.IOException`
- `java.util.ArrayList`
- `java.util.List`

## ui/font/AWTFontRenderer.kt

Declarations: AWTFontRenderer, CharLocation, CachedFont.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.event.Listenable`
- `net.ccbluex.liquidbounce.event.Render2DEvent`
- `net.ccbluex.liquidbounce.event.handler`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.kotlin.LruCache`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.GlStateManager.bindTexture`
- `net.minecraft.client.renderer.texture.TextureUtil`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.opengl.GL12.*`
- `java.awt.Color`
- `java.awt.Font`
- `java.awt.RenderingHints`
- `java.awt.image.BufferedImage`
- `kotlin.math.max`
- `kotlin.math.min`
- `kotlin.math.roundToInt`

## ui/font/Fonts.kt

Declarations: FontInfo, CustomFontInfo, Fonts.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `com.google.gson.JsonObject`
- `net.ccbluex.liquidbounce.file.FileManager.fontsDir`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.io.readJson`
- `net.ccbluex.liquidbounce.utils.io.writeJson`
- `net.minecraft.client.gui.FontRenderer`
- `java.awt.Font`
- `java.io.File`
- `kotlin.system.measureTimeMillis`

## ui/font/GameFontRenderer.kt

Declarations: GameFontRenderer.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.hexColors`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.randomMagicText`
- `net.ccbluex.liquidbounce.utils.render.RenderUtils.drawLine`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader`
- `net.minecraft.client.gui.FontRenderer`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.opengl.GL20.glUseProgram`
- `java.awt.Color`
- `java.awt.Font`

## utils/client/ClientThemesUtils.kt

Declarations: ClientThemesUtils.

Disposition: Selected visual/data reference, rewritten against Phantom.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.ColorUtils`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.mixColors`
- `java.awt.Color`

## utils/extensions/ColorExtensions.kt

Declarations: top-level functions/extensions.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `java.awt.Color`

## utils/render/AnimatedValue.kt

Declarations: AnimatedValue.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- None declared.

## utils/render/Animation.kt

Declarations: Animation, EnumAnimationState.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- None declared.

## utils/render/AnimationUtils.kt

Declarations: AnimationUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `kotlin.math.pow`
- `kotlin.math.sin`

## utils/render/AttackParticle.kt

Declarations: AttackParticle.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.block.BlockAir`
- `net.minecraft.block.BlockBush`
- `net.minecraft.block.BlockLiquid`
- `net.minecraft.client.Minecraft`
- `net.minecraft.util.BlockPos`
- `net.minecraft.util.Vec3`

## utils/render/BlendUtils.java

Declarations: top-level functions/extensions.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `java.awt.*`

## utils/render/BlurEffects.kt

Declarations: BlurEffects, BlurMode.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL20`
- `org.lwjgl.opengl.GL11.*`

## utils/render/BlurUtils.kt

Declarations: BlurUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `net.ccbluex.liquidbounce.utils.MinecraftInstance`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.OpenGlHelper`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.client.shader.ShaderGroup`
- `net.minecraft.util.ResourceLocation`

## utils/render/CharRenderer.kt

Declarations: CharRenderer.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.MinecraftInstance`
- `net.minecraft.client.gui.ScaledResolution`
- `org.lwjgl.opengl.GL11`
- `java.text.DecimalFormat`
- `java.text.DecimalFormatSymbols`
- `java.util.*`

## utils/render/ColorSettings.kt

Declarations: ColorSettingsFloat, ColorSettingsInteger.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.config.Configurable`
- `net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS`
- `net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha`
- `java.awt.Color`

## utils/render/ColorUtils.kt

Declarations: ColorUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth`
- `net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt`
- `net.minecraft.entity.EntityLivingBase`
- `java.awt.Color`
- `java.util.regex.Pattern`
- `kotlin.math.abs`
- `kotlin.math.ceil`
- `kotlin.math.max`
- `kotlin.math.min`
- `kotlin.math.sin`

## utils/render/CustomTexture.kt

Declarations: CustomTexture.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.renderer.texture.TextureUtil`
- `org.lwjgl.opengl.GL11.glDeleteTextures`
- `java.awt.image.BufferedImage`

## utils/render/EaseUtils.kt

Declarations: EaseUtils, EnumEasingType, EnumEasingOrder.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.config.ListValue`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `kotlin.math.*`

## utils/render/EasingObject.kt

Declarations: EasingObject.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- None declared.

## utils/render/EmbeddedStencil.kt

Declarations: EmbeddedStencil.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.EXTFramebufferObject`
- `org.lwjgl.opengl.EXTPackedDepthStencil`
- `org.lwjgl.opengl.GL11.*`

## utils/render/IconUtils.kt

Declarations: IconUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils`
- `net.ccbluex.liquidbounce.utils.io.flipSafely`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `org.lwjgl.opengl.Display`
- `java.io.IOException`
- `java.io.InputStream`
- `java.nio.ByteBuffer`
- `java.util.*`
- `javax.imageio.ImageIO`

## utils/render/ImageUtils.java

Declarations: ImageUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `java.awt.*`
- `java.awt.image.BufferedImage`
- `java.nio.ByteBuffer`
- `net.ccbluex.liquidbounce.utils.io.BufferExtensionsKt`

## utils/render/InternalBlurShader.kt

Declarations: InternalBlurShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL20`
- `org.lwjgl.opengl.GL11.*`

## utils/render/LBPPAnimationUtils.java

Declarations: top-level functions/extensions.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `java.awt.*`

## utils/render/MiniMapRegister.kt

Declarations: MiniMapRegister, MiniMapTexture, ChunkLocation.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.event.Listenable`
- `net.ccbluex.liquidbounce.event.Render2DEvent`
- `net.ccbluex.liquidbounce.event.handler`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.minecraft.client.renderer.texture.DynamicTexture`
- `net.minecraft.util.BlockPos`
- `net.minecraft.world.chunk.Chunk`
- `java.util.concurrent.locks.ReentrantReadWriteLock`
- `kotlin.concurrent.read`
- `kotlin.concurrent.write`

## utils/render/ParticleUtils.kt

Declarations: ParticleUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `net.vitox.ParticleGenerator`

## utils/render/RenderExtensions.kt

Declarations: top-level functions/extensions.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.WorldRenderer`

## utils/render/RenderUtils.kt

Declarations: RenderUtils, Corner, RoundedCorners, ColorValueCache.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `co.uk.hexeption.utils.OutlineUtils`
- `net.ccbluex.liquidbounce.config.ColorValue`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.block.block`
- `net.ccbluex.liquidbounce.utils.block.center`
- `net.ccbluex.liquidbounce.utils.block.toVec`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.disableFastRender`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.extensions.*`
- `net.ccbluex.liquidbounce.utils.io.flipSafely`
- `net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil`
- `net.minecraft.client.gui.FontRenderer`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.entity.Entity`
- `net.minecraft.entity.EntityLivingBase`
- `net.minecraft.util.*`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE`
- `org.lwjgl.opengl.GL14.glBlendFuncSeparate`
- `java.awt.Color`
- `java.awt.Graphics2D`
- `java.awt.image.BufferedImage`
- `java.nio.ByteBuffer`
- `javax.vecmath.Point3d`
- `kotlin.math.*`

## utils/render/RenderUtils2.java

Declarations: top-level functions/extensions.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce`
- `net.ccbluex.liquidbounce.features.module.modules.combat.KillAura`
- `net.ccbluex.liquidbounce.features.module.modules.render.Animations`
- `net.ccbluex.liquidbounce.ui.font.Fonts`
- `net.ccbluex.liquidbounce.utils.MinecraftInstance`
- `net.ccbluex.liquidbounce.utils.block.BlockUtils`
- `net.minecraft.block.Block`
- `net.minecraft.client.gui.Gui`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.OpenGlHelper`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.client.renderer.WorldRenderer`
- `net.minecraft.client.renderer.culling.Frustum`
- `net.minecraft.client.renderer.entity.RenderManager`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.client.shader.ShaderGroup`
- `net.minecraft.client.shader.Framebuffer`
- `net.minecraft.entity.Entity`
- `net.minecraft.entity.EntityLivingBase`
- `net.minecraft.entity.player.EntityPlayer`
- `net.minecraft.util.AxisAlignedBB`
- `net.minecraft.util.BlockPos`
- `net.minecraft.util.ResourceLocation`
- `net.minecraft.util.Timer`
- `net.minecraftforge.fml.relauncher.Side`
- `net.minecraftforge.fml.relauncher.SideOnly`
- `org.lwjgl.opengl.GL11`
- `java.awt.*`
- `java.awt.image.BufferedImage`
- `java.util.HashMap`
- `java.util.Map`
- `static java.lang.Math.*`
- `static org.lwjgl.opengl.GL11.*`

## utils/render/RoundedUtil.kt

Declarations: RoundedUtil.

Disposition: Selected visual/data reference, rewritten against Phantom.

Dependencies:

- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`

## utils/render/SafeVertexBuffer.kt

Declarations: SafeVertexBuffer.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.renderer.vertex.VertexBuffer`
- `net.minecraft.client.renderer.vertex.VertexFormat`

## utils/render/ShaderUtils.kt

Declarations: ShaderUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.shader.shaders.RoundedRectShader`
- `java.awt.Color`

## utils/render/ShadowUtils.kt

Declarations: ShadowUtils.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.MinecraftInstance`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.renderer.OpenGlHelper`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.client.shader.Framebuffer`
- `net.minecraft.client.shader.ShaderGroup`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE`
- `java.io.IOException`

## utils/render/Stencil.java

Declarations: Stencil.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.EXTFramebufferObject`
- `static org.lwjgl.opengl.GL11.*`

## utils/render/WorldToScreen.kt

Declarations: WorldToScreen.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `org.lwjgl.BufferUtils`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL11.glGetFloat`
- `org.lwjgl.util.vector.Matrix4f`
- `org.lwjgl.util.vector.Vector2f`
- `org.lwjgl.util.vector.Vector3f`
- `org.lwjgl.util.vector.Vector4f`
- `kotlin.math.abs`

## utils/render/animation/Animation.kt

Declarations: Animation.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- None declared.

## utils/render/animation/AnimationType.kt

Declarations: AnimationType.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- None declared.

## utils/render/animation/AnimationUtil.kt

Declarations: AnimationUtil.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `kotlin.math.*`

## utils/render/animation/ColorAnimation.kt

Declarations: ColorAnimation.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `java.awt.Color`

## utils/render/animations/Animation.kt

Declarations: Animation.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.timer.TimerUtil`

## utils/render/animations/Direction.kt

Declarations: Direction.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- None declared.

## utils/render/animations/impl/EaseBackIn.kt

Declarations: EaseBackIn.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.animations.Animation`
- `net.ccbluex.liquidbounce.utils.render.animations.Direction`

## utils/render/shader/Background.kt

Declarations: Background, ImageBackground, ShaderBackground.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc`
- `net.ccbluex.liquidbounce.utils.render.drawWithTessellatorWorldRenderer`
- `net.ccbluex.liquidbounce.utils.render.shader.shaders.BackgroundShader`
- `net.minecraft.client.gui.Gui`
- `net.minecraft.client.renderer.GlStateManager.color`
- `net.minecraft.client.renderer.texture.DynamicTexture`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.util.ResourceLocation`
- `java.io.File`
- `java.util.concurrent.CountDownLatch`
- `javax.imageio.ImageIO`

## utils/render/shader/FramebufferShader.kt

Declarations: FramebufferShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.client.renderer.GlStateManager.*`
- `net.minecraft.client.renderer.RenderHelper`
- `net.minecraft.client.renderer.Tessellator`
- `net.minecraft.client.renderer.vertex.DefaultVertexFormats`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.opengl.GL20.glUseProgram`
- `java.awt.Color`
- `kotlin.math.roundToInt`

## utils/render/shader/KawaseBloom.kt

Declarations: KawaseBloom.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL13`
- `org.lwjgl.opengl.GL14`

## utils/render/shader/KawaseBlur.kt

Declarations: KawaseBlur.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.renderer.GlStateManager`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL13`
- `org.lwjgl.opengl.GL14`

## utils/render/shader/Shader.kt

Declarations: Shader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.client.MinecraftInstance`
- `org.apache.commons.io.IOUtils`
- `org.lwjgl.opengl.*`
- `org.lwjgl.opengl.ARBShaderObjects.*`
- `org.lwjgl.opengl.GL11.*`
- `org.lwjgl.opengl.GL20.glGetUniformLocation`
- `org.lwjgl.opengl.GL20.glUseProgram`
- `java.io.File`
- `java.io.IOException`
- `java.nio.file.Files`
- `net.minecraft.util.ResourceLocation`

## utils/render/shader/ShaderElement.kt

Declarations: ShaderElement.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.RenderUtils`
- `net.minecraft.client.Minecraft`
- `net.minecraft.client.shader.Framebuffer`
- `org.lwjgl.opengl.GL11`
- `java.util.ArrayList`

## utils/render/shader/ShaderUtil.kt

Declarations: ShaderUtil.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.minecraft.client.Minecraft`
- `net.minecraft.client.gui.ScaledResolution`
- `net.minecraft.util.ResourceLocation`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL20`
- `java.io.ByteArrayInputStream`
- `java.io.IOException`

## utils/render/shader/shaders/BackgroundShader.kt

Declarations: BackgroundShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime`
- `net.ccbluex.liquidbounce.utils.render.shader.Shader`
- `org.lwjgl.opengl.Display`
- `org.lwjgl.opengl.GL20.*`
- `java.io.File`
- `java.io.IOException`
- `net.minecraft.util.ResourceLocation`

## utils/render/shader/shaders/GlowShader.kt

Declarations: GlowShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader`
- `org.lwjgl.opengl.GL20.*`

## utils/render/shader/shaders/GradientFontShader.kt

Declarations: GradientFontShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.render.shader.Shader`
- `org.lwjgl.opengl.GL20.*`
- `java.io.Closeable`

## utils/render/shader/shaders/GradientShader.kt

Declarations: GradientShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS`
- `net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER`
- `net.ccbluex.liquidbounce.utils.render.shader.Shader`
- `org.lwjgl.opengl.GL20.*`
- `java.io.Closeable`

## utils/render/shader/shaders/RainbowFontShader.kt

Declarations: RainbowFontShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.shader.Shader`
- `org.lwjgl.opengl.GL20.glUniform1f`
- `org.lwjgl.opengl.GL20.glUniform2f`
- `java.io.Closeable`

## utils/render/shader/shaders/RainbowShader.kt

Declarations: RainbowShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.shader.Shader`
- `org.lwjgl.opengl.GL20.glUniform1f`
- `org.lwjgl.opengl.GL20.glUniform2f`
- `java.io.Closeable`

## utils/render/shader/shaders/RoundedRectShader.kt

Declarations: RoundedRectShader.

Disposition: Dependency or alternative reviewed; not imported as a framework.

Dependencies:

- `net.ccbluex.liquidbounce.utils.render.shader.Shader`
- `net.minecraft.client.renderer.GlStateManager`
- `org.lwjgl.opengl.GL11`
- `java.awt.Color`
- `kotlin.math.abs`
