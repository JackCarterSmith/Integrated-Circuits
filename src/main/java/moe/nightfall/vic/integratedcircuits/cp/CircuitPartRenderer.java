package moe.nightfall.vic.integratedcircuits.cp;

import moe.nightfall.vic.integratedcircuits.misc.RenderManager;
import moe.nightfall.vic.integratedcircuits.misc.Vec2i;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import moe.nightfall.vic.integratedcircuits.Config;
import moe.nightfall.vic.integratedcircuits.client.Resources;
import moe.nightfall.vic.integratedcircuits.cp.part.PartCPGate;
import moe.nightfall.vic.integratedcircuits.cp.part.PartIOBit;
import moe.nightfall.vic.integratedcircuits.cp.part.PartNull;
import moe.nightfall.vic.integratedcircuits.cp.part.PartWire;
import moe.nightfall.vic.integratedcircuits.misc.MiscUtils;
import moe.nightfall.vic.integratedcircuits.misc.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;

@SideOnly(Side.CLIENT)
public class CircuitPartRenderer {

	public static final int PART_SIZE = 16;
	
	public enum EnumRenderType {
		GUI, WORLD, WORLD_16x
	}

	public static void renderPart(CircuitRenderWrapper crw, double x, double y) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(Resources.RESOURCE_PCB);
		GL11.glTranslated(x, y, 0);
		RenderManager.getInstance().startDrawQuads(DefaultVertexFormats.POSITION_TEX_COLOR);
		renderPartPayload(crw.getPos(), crw, crw.getPart(), 0, 0, EnumRenderType.GUI);
		RenderManager.getInstance().draw();
		GL11.glTranslated(-x, -y, 0);
	}

	private static void renderPartPayload(Vec2i pos, ICircuit parent, CircuitPart part, double x, double y, EnumRenderType type) {
		if (type == EnumRenderType.WORLD_16x && !(part instanceof PartNull || part instanceof PartWire || part instanceof PartIOBit)) {
			RenderManager.getInstance().addQuad(x, y, 0, 15 * 16, PART_SIZE, PART_SIZE);
		}

		part.renderPart(pos, parent, x, y, type);
	}

	@SideOnly(Side.CLIENT)
	public static int checkConnections(Vec2i pos, ICircuit parent, CircuitPart part) {
		boolean c1 = part.hasConnectionOnSide(pos, parent, EnumFacing.SOUTH);
		boolean c2 = part.hasConnectionOnSide(pos, parent, EnumFacing.NORTH);
		boolean c3 = part.hasConnectionOnSide(pos, parent, EnumFacing.EAST);
		boolean c4 = part.hasConnectionOnSide(pos, parent, EnumFacing.WEST);

		return (c1 ? 1 : 0) << 3 | (c2 ? 1 : 0) << 2 | (c3 ? 1 : 0) << 1 | (c4 ? 1 : 0);
	}

	public static void renderParts(ICircuit circuit) {
		int w = circuit.getCircuitData().getSize();

		Minecraft.getMinecraft().getTextureManager().bindTexture(Resources.RESOURCE_PCB);
		GL11.glPushMatrix();
		GL11.glScalef(1F / PART_SIZE, 1F / PART_SIZE, 1);

		RenderManager renderManager = RenderManager.getInstance();
		renderManager.startDrawQuads(DefaultVertexFormats.POSITION_TEX_COLOR);
		for (int x2 = 0; x2 < w; x2++) {
			for (int y2 = 0; y2 < w; y2++) {
				Vec2i pos = new Vec2i(x2, y2);
				renderPartPayload(pos, circuit, circuit.getCircuitData().getPart(pos), x2 * PART_SIZE, y2 * PART_SIZE, EnumRenderType.GUI);
			}
		}
		renderManager.draw();
		GL11.glPopMatrix();
	}

	public static void renderParts(ICircuit circuit, double offX, double offY, boolean[][] exc, EnumRenderType type) {
		int w = circuit.getCircuitData().getSize();

		GL11.glPushMatrix();
		GL11.glTranslated(offX, offY, 0);
		if (type == EnumRenderType.GUI)
			GL11.glScalef(1F / PART_SIZE, 1F / PART_SIZE, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(Resources.RESOURCE_PCB);

		RenderManager renderManager = RenderManager.getInstance();
		renderManager.startDrawQuads(DefaultVertexFormats.POSITION_TEX_COLOR);
		for (int x2 = 0; x2 < w; x2++) {
			for (int y2 = 0; y2 < w; y2++) {
				Vec2i pos = new Vec2i(x2, y2);
				if (exc[x2][y2])
					renderPartPayload(pos, circuit, circuit.getCircuitData().getPart(pos), x2 * PART_SIZE, y2 * PART_SIZE, type);
			}
		}
		renderManager.draw();
		GL11.glPopMatrix();
	}

	public static void renderPerfboard(CircuitData data) {
		Tessellator tes = Tessellator.getInstance();
		int size = data.getSize();

		Minecraft.getMinecraft().getTextureManager().bindTexture(Resources.RESOURCE_PCB_PERF1);

		RenderManager renderManager = RenderManager.getInstance();
		renderManager.setColor(1, 1, 1, 1);
		renderManager.startDrawQuads(DefaultVertexFormats.POSITION_TEX_COLOR);

		renderManager.addQuad(0, 0, 0, 0, size, size, 16, 16, 16D / size, 16D / size, 0);
		renderManager.draw();

		Minecraft.getMinecraft().getTextureManager().bindTexture(Resources.RESOURCE_PCB_PERF2);

		renderManager.startDrawQuads(DefaultVertexFormats.POSITION_TEX_COLOR);
		renderManager.addQuad(0, 0, 0, 0, 1, size, 16, 16, 16D, 16D / size, 0);
		renderManager.addQuad(size - 1, 0, 0, 0, 1, size, 16, 16, 16, 16D / size, 0);
		renderManager.addQuad(0, 0, 0, 0, size, 1, 16, 16, 16D / size, 16, 0);
		renderManager.addQuad(0, size - 1, 0, 0, size, 1, 16, 16, 16D / size, 16, 0);
		tes.draw();
	}

	public static void renderPartGate(Vec2i pos, ICircuit parent, PartCPGate gate, double x, double y, EnumRenderType type) {
		RenderManager rm = RenderManager.getInstance();
		if (gate.canConnectToSide(pos, parent, EnumFacing.NORTH)) {
			if (type == EnumRenderType.GUI && (
					gate.getOutputToSide(pos, parent, EnumFacing.NORTH)
					|| gate.getInputFromSide(pos, parent, EnumFacing.NORTH)))
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
			else
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen, 0.4F);
			rm.addQuad(x, y, 2 * 16, 0, PART_SIZE, PART_SIZE);
		}

		if (gate.canConnectToSide(pos, parent, EnumFacing.SOUTH)) {
			if (type == EnumRenderType.GUI && (
					gate.getOutputToSide(pos, parent, EnumFacing.SOUTH)
					|| gate.getInputFromSide(pos, parent, EnumFacing.SOUTH)))
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
			else
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen, 0.4F);
			rm.addQuad(x, y, 4 * 16, 0, PART_SIZE, PART_SIZE);
		}

		if (gate.canConnectToSide(pos, parent, EnumFacing.WEST)) {
			if (type == EnumRenderType.GUI && (
					gate.getOutputToSide(pos, parent, EnumFacing.WEST)
					|| gate.getInputFromSide(pos, parent, EnumFacing.WEST)))
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
			else
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen, 0.4F);
			rm.addQuad(x, y, 1 * 16, 0, PART_SIZE, PART_SIZE);
		}

		if (gate.canConnectToSide(pos, parent, EnumFacing.EAST)) {
			if (type == EnumRenderType.GUI && (
					gate.getOutputToSide(pos, parent, EnumFacing.EAST)
					|| gate.getInputFromSide(pos, parent, EnumFacing.EAST)))
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
			else
				RenderUtils.applyColorIRGBA(rm, Config.colorGreen, 0.4F);
			rm.addQuad(x, y, 3 * 16, 0, PART_SIZE, PART_SIZE);
		}

		RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
	}

	@SideOnly(Side.CLIENT)
	public static void renderPartCell(Vec2i pos, ICircuit parent, CircuitPart cell, double x, double y, EnumRenderType type) {
		RenderManager rm = RenderManager.getInstance();

		int rotation = 0;
		if(cell instanceof PartCPGate)
			rotation = ((PartCPGate) cell).getRotation(pos, parent);

		if (type == EnumRenderType.GUI
				&& (cell.getOutputToSide(pos, parent, MiscUtils.rotn(EnumFacing.NORTH, rotation))
				|| cell.getInputFromSide(pos, parent, MiscUtils.rotn(EnumFacing.NORTH, rotation))
				|| cell.getOutputToSide(pos, parent, MiscUtils.rotn(EnumFacing.SOUTH, rotation))
				|| cell.getInputFromSide(pos, parent, MiscUtils.rotn(EnumFacing.SOUTH, rotation))))
			RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
		else {
			RenderUtils.applyColorIRGBA(rm, Config.colorGreen, 0.4f);
		}
		rm.addQuad(x, y, 0, 2 * 16, PART_SIZE, PART_SIZE, rotation);

		if (type == EnumRenderType.GUI
				&& (cell.getOutputToSide(pos, parent, MiscUtils.rotn(EnumFacing.EAST, rotation))
				|| cell.getInputFromSide(pos, parent, MiscUtils.rotn(EnumFacing.EAST, rotation))
				|| cell.getOutputToSide(pos, parent, MiscUtils.rotn(EnumFacing.WEST, rotation))
				|| cell.getInputFromSide(pos, parent, MiscUtils.rotn(EnumFacing.WEST, rotation))))
			RenderUtils.applyColorIRGBA(rm, Config.colorGreen);
		else
			RenderUtils.applyColorIRGBA(rm, Config.colorGreen, 0.4F);
	}

	public static class CircuitRenderWrapper implements ICircuit {
		private final CircuitData data;
		private final CircuitPart part;
		private final Vec2i pos;

		public CircuitRenderWrapper(Class<? extends CircuitPart> clazz) {
			this(clazz, 0);
		}

		public CircuitRenderWrapper(Class<? extends CircuitPart> clazz, int state) {
			this(state, CircuitPart.getPart(clazz));
		}

		public CircuitRenderWrapper(int state, CircuitPart part) {
			this.data = CircuitData.createShallowInstance(state, this);
			this.part = part;
			this.pos = new Vec2i(1, 1);
		}

		public CircuitRenderWrapper(CircuitData data) {
			this(data, null, null);
		}

		public CircuitRenderWrapper(CircuitData data, CircuitPart part, Vec2i pos) {
			this.data = data;
			this.part = part;
			this.pos = pos;
		}

		public CircuitPart getPart() {
			return part;
		}

		@Override
		public CircuitData getCircuitData() {
			return data;
		}

		public Vec2i getPos() {
			return pos;
		}

		public int getState() {
			return getCircuitData().getMeta(getPos());
		}

		public void setState(int state) {
			getCircuitData().setMeta(getPos(), state);
		}

		@Override
		public void setCircuitData(CircuitData data) {
		}

		@Override
		public boolean getInputFromSide(EnumFacing dir, int frequency) {
			return false;
		}

		@Override
		public void setOutputToSide(EnumFacing dir, int frequency, boolean output) {
		}
	}
}
