package moe.nightfall.vic.integratedcircuits.cp.part;

import moe.nightfall.vic.integratedcircuits.cp.ICircuit;
import moe.nightfall.vic.integratedcircuits.misc.Vec2i;
import moe.nightfall.vic.integratedcircuits.misc.PropertyStitcher.BooleanProperty;
import net.minecraft.util.EnumFacing;

/** Has only one type of output **/
public abstract class PartSimpleGate extends PartCPGate {
	public final BooleanProperty PROP_OUT = new BooleanProperty("OUT", stitcher);

	protected final boolean getOutput(Vec2i pos, ICircuit parent) {
		return getProperty(pos, parent, PROP_OUT);
	}

	protected final void setOutput(Vec2i pos, ICircuit parent, boolean output) {
		setProperty(pos, parent, PROP_OUT, output);
	}

	protected abstract void calcOutput(Vec2i pos, ICircuit parent);

	/** already rotated **/
	protected abstract boolean hasOutputToSide(Vec2i pos, ICircuit parent, EnumFacing fd);

	@Override
	public Category getCategory() {
		return Category.GATE;
	}

	@Override
	public boolean getOutputToSide(Vec2i pos, ICircuit parent, EnumFacing side) {
		return hasOutputToSide(pos, parent, toInternal(pos, parent, side)) && getProperty(pos, parent, PROP_OUT);
	}

	@Override
	public void onInputChange(Vec2i pos, ICircuit parent) {
		scheduleTick(pos, parent);
	}

	@Override
	public void onScheduledTick(Vec2i pos, ICircuit parent) {
		calcOutput(pos, parent);
		notifyNeighbours(pos, parent);
	}
}
