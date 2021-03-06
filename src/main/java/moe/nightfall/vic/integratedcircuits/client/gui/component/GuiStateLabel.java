package moe.nightfall.vic.integratedcircuits.client.gui.component;

import java.util.Arrays;
import java.util.List;

import moe.nightfall.vic.integratedcircuits.client.gui.GuiInterfaces.IHoverable;
import moe.nightfall.vic.integratedcircuits.client.gui.GuiInterfaces.IHoverableHandler;
import moe.nightfall.vic.integratedcircuits.misc.MiscUtils;
import moe.nightfall.vic.integratedcircuits.misc.Vec2i;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.client.config.GuiButtonExt;

public class GuiStateLabel extends GuiButtonExt implements IHoverable {
	private Vec2i[] states;
	private String[] desc;
	private int state;
	private ResourceLocation loc;

	public GuiStateLabel(int id, int xPos, int yPos, int width, int height, ResourceLocation loc) {
		super(id, xPos, yPos, "");
		this.width = width;
		this.height = height;
		this.loc = loc;
	}

	public GuiStateLabel addState(Vec2i... states) {
		this.states = states;
		return this;
	}

	public GuiStateLabel addDescription(String... desc) {
		this.desc = desc;
		return this;
	}

	@Override
	public boolean mousePressed(Minecraft mc, int x, int y) {
		boolean bool = super.mousePressed(mc, x, y);
		if (bool)
			state = (state + 1) % states.length;
		return bool;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		if (!this.visible || states == null || state >= states.length)
			return;
		this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
				&& mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
		if (hovered) {
			if (mc.currentScreen instanceof IHoverableHandler)
				((IHoverableHandler) mc.currentScreen).setCurrentItem(this);
			GL11.glColor3f(0.8F, 0.9F, 1F);
		} else
			GL11.glColor3f(1F, 1F, 1F);
		Vec2i uv = states[state];
		mc.getTextureManager().bindTexture(loc);
		drawTexturedModalRect(xPosition, yPosition, uv.x, uv.y, width, height);
	}

	@Override
	public List<String> getHoverInformation() {
		if (desc != null && state < desc.length)
			return Arrays.asList(MiscUtils.stringNewlineSplit(desc[state]));
		return null;
	}

	public int getState() {
		return state;
	}

	public GuiStateLabel setState(int state) {
		this.state = state;
		return this;
	}
}
