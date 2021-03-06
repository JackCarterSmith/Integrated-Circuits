package moe.nightfall.vic.integratedcircuits.cp.part.latch;

import java.util.ArrayList;

import moe.nightfall.vic.integratedcircuits.misc.Vec2i;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import moe.nightfall.vic.integratedcircuits.cp.CircuitPartRenderer;
import moe.nightfall.vic.integratedcircuits.cp.ICircuit;
import moe.nightfall.vic.integratedcircuits.cp.part.PartCPGate;
import moe.nightfall.vic.integratedcircuits.misc.PropertyStitcher.IntProperty;
import moe.nightfall.vic.integratedcircuits.misc.PropertyStitcher.BooleanProperty;
import moe.nightfall.vic.integratedcircuits.tile.TileEntityCAD;
import net.minecraft.client.resources.I18n;

public class PartRSLatch extends PartCPGate {
	public final IntProperty PROP_STATE = new IntProperty("STATE", stitcher, 2);
	public final BooleanProperty PROP_CHECK = new BooleanProperty("CHECK", stitcher);
	public final IntProperty PROP_MODE = new IntProperty("MODE", stitcher, 3);

	@Override
	public void onClick(Vec2i pos, ICircuit parent, int button, boolean ctrl) {
		super.onClick(pos, parent, button, ctrl);
		if (button == 0 && ctrl) {
			cycleProperty(pos, parent, PROP_MODE);
			scheduleInputChange(pos, parent);
			notifyNeighbours(pos, parent);
		}
	}

	private boolean isMirrored(Vec2i pos, ICircuit parent) {
		return (getProperty(pos, parent, PROP_MODE) & 2) != 0;
	}

	private boolean isSpecial(Vec2i pos, ICircuit parent) {
		return (getProperty(pos, parent, PROP_MODE) & 1) != 0;
	}

	@Override
	public void onInputChange(Vec2i pos, ICircuit parent) {
		scheduleTick(pos, parent);
		// A bit of wire-like behavior, for the second mode
		if (isSpecial(pos, parent)) {
			if (getProperty(pos, parent, PROP_CHECK))
				return;
			int st = getProperty(pos, parent, PROP_STATE);
			EnumFacing s1 = toExternal(pos, parent, EnumFacing.NORTH);
			boolean in1 = getInputFromSide(pos, parent, s1);
			boolean in2 = getInputFromSide(pos, parent, s1.getOpposite());
			if ((st != 2 && !in1 && in2) || (st == 2 && !in2 && in1)) {
				// Turn off currently outputting input side,
				// to check if we must switch to other state or break.
				setProperty(pos, parent, PROP_CHECK, true);
			}
			notifyNeighbours(pos, parent);
		}
	}

	@Override
	public void onScheduledTick(Vec2i pos, ICircuit parent) {
		EnumFacing s1 = toExternal(pos, parent, EnumFacing.NORTH);
		boolean in1 = getInputFromSide(pos, parent, s1);
		boolean in2 = getInputFromSide(pos, parent, s1.getOpposite());
		if (in1 != in2)
			setProperty(pos, parent, PROP_STATE, in1 ? 1 : 2);
		else if (in1 || getProperty(pos, parent, PROP_CHECK)) {
			// Either both inputs are high
			//  or they are shorted by unpowered wire in "special" mode.
			setProperty(pos, parent, PROP_STATE, 0); // Switch to broken state
			if (!in1 && !(parent instanceof TileEntityCAD)) {
				// Inputs shorted and circuit is ticked outside CAD.
				// It will never ever be edited again.
				// So just keep this RS latch permanently broken.
				notifyNeighbours(pos, parent);
				return;
			}
		} else if (getProperty(pos, parent, PROP_STATE) == 0)
			// Both inputs off, still in broken state. Switch to "default".
			// Vanilla and P:R RS latches work the same way.
			setProperty(pos, parent, PROP_STATE, 1);
		setProperty(pos, parent, PROP_CHECK, false); 
		notifyNeighbours(pos, parent);
	}

	@Override
	public boolean getOutputToSide(Vec2i pos, ICircuit parent, EnumFacing side) {
		int state = getProperty(pos, parent, PROP_STATE);
		EnumFacing s2 = toInternal(pos, parent, side);
		boolean special = isSpecial(pos, parent);
		boolean mirrored = isMirrored(pos, parent);
		if (s2 == EnumFacing.NORTH) {
			if (special && state != 2)
				// A bit of wire-like behavior:
				// Only output if we are not powered from the same side
				//  and are not checking it.
				return !getProperty(pos, parent, PROP_CHECK)
						&& !getInputFromSide(pos, parent, side);
			return false;
		}
		if (state == 0)
			return false;
		if (s2 == EnumFacing.SOUTH) {
			if (special && state == 2)
				return !getProperty(pos, parent, PROP_CHECK)
						&& !getInputFromSide(pos, parent, side);
			return false;
		}
		if (s2 == EnumFacing.EAST) {
			return (state == 1) != mirrored;
		}
		if (s2 == EnumFacing.WEST) {
			return (state == 1) == mirrored;
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vec2i getTextureOffset(Vec2i pos, ICircuit parent, double x, double y, CircuitPartRenderer.EnumRenderType type) {
		return new Vec2i(7, 1);
	}

	@Override
	public ArrayList<String> getInformation(Vec2i pos, ICircuit parent, boolean edit, boolean ctrlDown) {
		ArrayList<String> text = super.getInformation(pos, parent, edit, ctrlDown);
		text.add(I18n.format("part.integratedcircuits.rslatch.mode") + ": " + (isSpecial(pos, parent) ? 1 : 0));
		if (isMirrored(pos, parent))
			text.add(TextFormatting.ITALIC + I18n.format("part.integratedcircuits.rslatch.mirrored"));
		if (edit && ctrlDown)
			text.add(I18n.format("gui.integratedcircuits.cad.mode"));
		return text;
	}

	@Override
	public Category getCategory() {
		return Category.LATCH;
	}
}
